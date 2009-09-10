/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.index;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;

import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.artifact.GavCalculator;
import org.sonatype.nexus.artifact.IllegalArtifactCoordinateException;
import org.sonatype.nexus.artifact.M2GavCalculator;
import org.sonatype.nexus.index.ArtifactAvailablility;
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.FlatSearchRequest;
import org.sonatype.nexus.index.FlatSearchResponse;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.context.IndexCreator;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.locator.PomLocator;
import org.sonatype.nexus.index.updater.IndexUpdateRequest;
import org.sonatype.nexus.index.updater.IndexUpdater;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.ArtifactRepositoryRef;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.embedder.ISettingsChangeListener;
import org.maven.ide.eclipse.index.IIndex;
import org.maven.ide.eclipse.index.IndexListener;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.internal.index.IndexUpdaterJob.IndexCommand;
import org.maven.ide.eclipse.project.IMavenProjectChangedListener;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.MavenProjectManager;


/**
 * @author Eugene Kuleshov
 */
public class NexusIndexManager implements IndexManager, IMavenProjectChangedListener {

  /** Field separator */
  public static final String FS = "|";

  public static final Pattern FS_PATTERN = Pattern.compile( Pattern.quote( "|" ) );

  /** Non available value */
  public static final String NA = "NA";

  private final GavCalculator gavCalculator = new M2GavCalculator();

  private NexusIndexer indexer;
  
  private IMaven maven;

  private MavenProjectManager projectManager;

  private ArrayList<IndexCreator> fullCreators = null;

  private ArrayList<IndexCreator> minCreators = null;

  private final MavenConsole console;

  private final File baseIndexDir;

  private List<IndexListener> indexListeners = new ArrayList<IndexListener>();

  private NexusIndex localIndex = new NexusIndex(this, LOCAL_INDEX, RepositoryInfo.DETAILS_MIN);

  private NexusIndex workspaceIndex = new NexusIndex(this, WORKSPACE_INDEX, RepositoryInfo.DETAILS_MIN);

  private final IndexUpdaterJob updaterJob;

  /**
   * Maps repositoryUrl to IndexInfo of repository index
   */
  private final Map<String, RepositoryInfo> repositories = new HashMap<String, RepositoryInfo>();

  private Properties indexDetails = new Properties();

  public static String nvl( String v )
  {
      return v == null ? NA : v;
  }

  public static String getGAV( String groupId, String artifactId, String version, String classifier)
  {
      return new StringBuilder() //
          .append( groupId ).append( FS ) //
          .append( artifactId ).append( FS ) //
          .append( version ).append( FS ) //
          .append( nvl( classifier ) )
          .toString();
  }

  public NexusIndexManager(MavenConsole console, MavenProjectManager projectManager, File stateDir) {
    this.console = console;
    this.projectManager = projectManager;
    this.baseIndexDir = new File(stateDir, "nexus");

    this.maven = MavenPlugin.lookup(IMaven.class);

    this.updaterJob = new IndexUpdaterJob(this, console);

    this.maven.addSettingsChangeListener(new ISettingsChangeListener() {
      public void settingsChanged(Settings settings) {
        initialize();
      }
    });
  }

  private ArrayList<IndexCreator> getFullCreator() {
    if(fullCreators == null) {
      try {
        PlexusContainer container = MavenPlugin.getDefault().getPlexusContainer();
        IndexCreator min = container.lookup(IndexCreator.class, "min");
        IndexCreator jar = container.lookup(IndexCreator.class, "jarContent");
        fullCreators = new ArrayList<IndexCreator>();
        fullCreators.add(min);
        fullCreators.add(jar);
      } catch(ComponentLookupException ce) {
        String msg = "Error looking up component ";
        console.logError(msg + "; " + ce.getMessage());
        MavenLogger.log(msg, ce);

      }
    }
    return fullCreators;
  }
  private ArrayList<IndexCreator> getMinCreator() {
    if(minCreators == null) {
      try {
        PlexusContainer container = MavenPlugin.getDefault().getPlexusContainer();
        IndexCreator min = container.lookup(IndexCreator.class, "min");
        minCreators = new ArrayList<IndexCreator>();
        minCreators.add(min);
      } catch(ComponentLookupException ce) {
        String msg = "Error looking up component ";
        MavenLogger.log(msg, ce);
      }

    }
    return minCreators;
  }


  public IndexedArtifactFile getIndexedArtifactFile(String repositoryUrl, ArtifactKey gav) throws CoreException {

    try {
      BooleanQuery query = new BooleanQuery();
      query.add( new TermQuery(new Term(ArtifactInfo.GROUP_ID, gav.getGroupId())), BooleanClause.Occur.MUST );
      query.add( new TermQuery(new Term(ArtifactInfo.ARTIFACT_ID, gav.getArtifactId())), BooleanClause.Occur.MUST );
      query.add( new TermQuery(new Term(ArtifactInfo.VERSION, gav.getVersion())), BooleanClause.Occur.MUST );
      if (gav.getClassifier() != null) {
        query.add( new TermQuery(new Term(ArtifactInfo.CLASSIFIER, gav.getClassifier())), BooleanClause.Occur.MUST );
      }

      ArtifactInfo artifactInfo = getIndexer().identify(query, Collections.singleton(getIndexingContext(repositoryUrl)));
      if(artifactInfo != null) {
        return getIndexedArtifactFile(artifactInfo);
      }
    } catch(Exception ex) {
      String msg = "Illegal artifact coordinate " + ex.getMessage();
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Search error", ex));
    } 
    return null;
  }
  
  public IndexedArtifactFile identify(File file) throws CoreException {
    try {
      ArtifactInfo artifactInfo = getIndexer().identify(file);
      return artifactInfo==null ? null : getIndexedArtifactFile(artifactInfo);
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Search error", ex));
    } 
  }
  
  public Query createQuery(String field, String expression) {
    return getIndexer().constructQuery(field, expression);
  }

  public Map<String, IndexedArtifact> search(String term, String type) throws CoreException {
    return search(null, term, type, IIndex.SEARCH_ALL);
  }
  
  public Map<String, IndexedArtifact> search(String term, String type, int classifier) throws CoreException {
    return search(null, term, type, classifier);
  }
  
  private void addClassifiersToQuery(BooleanQuery bq, int classifier){
    boolean includeJavaDocs = (classifier & IIndex.SEARCH_JAVADOCS) > 0;
    TermQuery tq = null;
    if(!includeJavaDocs){
      tq = new TermQuery(new Term(ArtifactInfo.CLASSIFIER, "javadoc"));
      bq.add(tq, Occur.MUST_NOT);
    }
    boolean includeSources = (classifier & IIndex.SEARCH_SOURCES) > 0;
    if(!includeSources){
      tq = new TermQuery(new Term(ArtifactInfo.CLASSIFIER, "sources"));
      bq.add(tq, Occur.MUST_NOT);
    }
    boolean includeTests = (classifier & IIndex.SEARCH_TESTS) > 0;
    if(!includeTests){
      tq = new TermQuery(new Term(ArtifactInfo.CLASSIFIER, "tests"));
      bq.add(tq, Occur.MUST_NOT);
    }
  }
  /**
   * @return Map<String, IndexedArtifact>
   */
  public Map<String, IndexedArtifact> search(String indexName, String term, String type, int classifier) throws CoreException {
    Query query;
    if(IIndex.SEARCH_CLASS_NAME.equals(type)) {
      query = getIndexer().constructQuery(ArtifactInfo.NAMES, term + "$");
      
    } else if(IIndex.SEARCH_GROUP.equals(type)) {
      query = new PrefixQuery(new Term(ArtifactInfo.GROUP_ID, term));

    } else if(IIndex.SEARCH_ARTIFACT.equals(type)) {
      BooleanQuery bq = new BooleanQuery();
      
      
      bq.add(getIndexer().constructQuery(ArtifactInfo.GROUP_ID, term), Occur.SHOULD);
      bq.add(getIndexer().constructQuery(ArtifactInfo.ARTIFACT_ID, term), Occur.SHOULD);
      bq.add(new PrefixQuery(new Term(ArtifactInfo.SHA1, term)), Occur.SHOULD);
      addClassifiersToQuery(bq, classifier);
      query = bq;

    } else if(IIndex.SEARCH_PLUGIN.equals(type)) {
      if("*".equals(term)) {
        query = new TermQuery(new Term(ArtifactInfo.PACKAGING, "maven-plugin"));
      } else {
        BooleanQuery bq = new BooleanQuery();
        bq.add(new WildcardQuery(new Term(ArtifactInfo.GROUP_ID, term + "*")), Occur.SHOULD);
        bq.add(new WildcardQuery(new Term(ArtifactInfo.ARTIFACT_ID, term + "*")), Occur.SHOULD);
        TermQuery tq = new TermQuery(new Term(ArtifactInfo.PACKAGING, "maven-plugin"));
        query = new FilteredQuery(tq, new QueryWrapperFilter(bq));
      }
      
    } else if(IIndex.SEARCH_ARCHETYPE.equals(type)) {
      BooleanQuery bq = new BooleanQuery();
      bq.add(new WildcardQuery(new Term(ArtifactInfo.GROUP_ID, term + "*")), Occur.SHOULD);
      bq.add(new WildcardQuery(new Term(ArtifactInfo.ARTIFACT_ID, term + "*")), Occur.SHOULD);
      TermQuery tq = new TermQuery(new Term(ArtifactInfo.PACKAGING, "maven-archetype"));
      query = new FilteredQuery(tq, new QueryWrapperFilter(bq));
      
    } else if(IIndex.SEARCH_PACKAGING.equals(type)) {
      query = new TermQuery(new Term(ArtifactInfo.PACKAGING, term));

    } else if(IIndex.SEARCH_SHA1.equals(type)) {
      query = new WildcardQuery(new Term(ArtifactInfo.SHA1, term + "*"));
      
    } else {
      return Collections.emptyMap();

    }

    Map<String, IndexedArtifact> result = new TreeMap<String, IndexedArtifact>();

    try {
      FlatSearchResponse response;
      IndexingContext context = getIndexingContext(indexName);
      if(context == null) {
        response = getIndexer().searchFlat(new FlatSearchRequest(query));
      } else {
        response = getIndexer().searchFlat(new FlatSearchRequest(query, context));
      }

      String regex = "^(.*?" + term.replaceAll("\\*", ".+?") + ".*?)$";
      Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

      for(ArtifactInfo artifactInfo : response.getResults()) {
        IndexedArtifactFile af = getIndexedArtifactFile(artifactInfo);

        if(!IIndex.SEARCH_CLASS_NAME.equals(type) || term.length() < IndexManager.MIN_CLASS_QUERY_LENGTH) {
          addArtifactFile(result, af, null, null, artifactInfo.packaging);

        } else {
          String classNames = artifactInfo.classNames;

          Matcher matcher = p.matcher(classNames);
          while(matcher.find()) {
            String value = matcher.group();
            int n = value.lastIndexOf('/');
            String className;
            String packageName;
            if(n < 0) {
              packageName = "";
              className = value;
            } else {
              packageName = value.substring(0, n).replace('/', '.');
              className = value.substring(n + 1);
            }
            addArtifactFile(result, af, className, packageName, artifactInfo.packaging);
          }
        }
      }

    }catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Search error", ex));
    }

    return result;
  }
  
  /**
   * @return Map<String, IndexedArtifact>
   */
  public Map<String, IndexedArtifact> search(String indexName, String term, String type) throws CoreException {
    return search(indexName, term, type, IIndex.SEARCH_ALL);
  }
  
  /**
   * @return Map<String, IndexedArtifact>
   */
  public Map<String, IndexedArtifact> search(String indexName, Query query) throws CoreException {
    Map<String, IndexedArtifact> result = new TreeMap<String, IndexedArtifact>();
    try {
      IndexingContext context = getIndexingContext(indexName);
      FlatSearchResponse response;
      if(context == null) {
        response = getIndexer().searchFlat(new FlatSearchRequest(query));
      } else {
        response = getIndexer().searchFlat(new FlatSearchRequest(query, context));
      }
      
      for(ArtifactInfo artifactInfo : response.getResults()) {
        IndexedArtifactFile af = getIndexedArtifactFile(artifactInfo);
        addArtifactFile(result, af, null, null, artifactInfo.packaging);
      }
      
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Search error", ex));
    }
    return result;
  }  

  private void addArtifactFile(Map<String, IndexedArtifact> result, IndexedArtifactFile af, String className, String packageName,
      String packaging) {
    String key = className + " : " + packageName + " : " + af.group + " : " + af.artifact;
    IndexedArtifact indexedArtifact = result.get(key);
    if(indexedArtifact == null) {
      indexedArtifact = new IndexedArtifact(af.group, af.artifact, packageName, className, packaging);
      result.put(key, indexedArtifact);
    }
    indexedArtifact.addFile(af);
  }

  IndexedArtifactFile getIndexedArtifactFile(ArtifactInfo artifactInfo) {
    String groupId = artifactInfo.groupId;
    String artifactId = artifactInfo.artifactId;
    String repository = artifactInfo.repository;
    String version = artifactInfo.version;
    String classifier = artifactInfo.classifier;
    String packaging = artifactInfo.packaging;
    String fname = artifactInfo.fname;
    if(fname == null) {
      fname = artifactId + '-' + version + (classifier != null ? '-' + classifier : "") + (packaging != null ? ('.' + packaging) : "");
    }

    long size = artifactInfo.size;
    Date date = new Date(artifactInfo.lastModified);

    int sourcesExists = artifactInfo.sourcesExists.ordinal();
    int javadocExists = artifactInfo.javadocExists.ordinal();

    String prefix = artifactInfo.prefix;
    List<String> goals = artifactInfo.goals;
    
    return new IndexedArtifactFile(repository, groupId, artifactId, version, packaging, classifier, fname, size, date,
        sourcesExists, javadocExists, prefix, goals);
  }

  private void reindexLocalRepository(String repositoryUrl, final IProgressMonitor monitor) throws CoreException {
    try {
      fireIndexUpdating(repositoryUrl);
      //IndexInfo indexInfo = getIndexInfo(indexName);
      IndexingContext context = getIndexer().getIndexingContexts().get(repositoryUrl);
      getIndexer().scan(context, new ArtifactScanningMonitor(context.getRepository(), monitor, console), false);
      fireIndexChanged(repositoryUrl);
      console.logMessage("Updated local repository index");
    } catch(Exception ex) {
      MavenLogger.log("Unable to re-index "+repositoryUrl, ex);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Reindexing error", ex));
    } finally {
      fireIndexChanged(repositoryUrl);
    }
  }

  private void reindexWorkspace(IProgressMonitor monitor) {
    // TODO clean current index
    for (IMavenProjectFacade facade : projectManager.getProjects()) {
      addDocument(IndexManager.WORKSPACE_INDEX, facade.getPomFile(), //
          getDocumentKey(facade.getArtifactKey()), -1, -1, null, 0, 0);
    }
  }

  public void addDocument(String repositoryUrl, File file, String documentKey, long size, long date, File jarFile,
      int sourceExists, int javadocExists) {
    try {
      IndexingContext context = getIndexingContext(repositoryUrl);
      if(context == null) {
        // TODO log
        return;
      }

      ArtifactContext artifactContext = getArtifactContext(file, documentKey, size, date, //
          sourceExists, javadocExists, context.getRepositoryId());

      getIndexer().addArtifactToIndex(artifactContext, context);

    } catch(Exception ex) {
      String msg = "Unable to add " + documentKey;
      console.logError(msg + "; " + ex.getMessage());
      MavenLogger.log(msg, ex);
    }
  }
  
  public void removeDocument(String indexName, File file, String documentKey) {
    try {
      IndexingContext context = getIndexingContext(indexName);
      if(context == null) {
        String msg = "Unable to find document to remove"+documentKey;
        MavenLogger.log(new Status(IStatus.ERROR,"org.maven.ide.eclipse", msg));
        return;
      }
      ArtifactContext artifactContext = getArtifactContext(null, documentKey, -1, -1, //
          IIndex.NOT_AVAILABLE, IIndex.NOT_AVAILABLE, context.getRepositoryId());
      getIndexer().deleteArtifactFromIndex(artifactContext, context);
     fireIndexChanged(indexName);
      
    } catch(Exception ex) {
      String msg = "Unable to remove " + documentKey;
      console.logError(msg + "; " + ex.getMessage());
      MavenLogger.log(msg, ex);
    }
  }

  private ArtifactContext getArtifactContext(File file, String documentKey, long size, long date, int sourceExists,
      int javadocExists, String repository) throws IllegalArtifactCoordinateException {
    Gav gav = gavCalculator.pathToGav(documentKey);

    String groupId = gav.getGroupId();
    String artifactId = gav.getArtifactId();
    String version = gav.getVersion();
    String classifier = gav.getClassifier();

    ArtifactInfo ai = new ArtifactInfo(repository, groupId, artifactId, version, classifier);
    ai.sourcesExists = ArtifactAvailablility.fromString(Integer.toString(sourceExists));
    ai.javadocExists = ArtifactAvailablility.fromString(Integer.toString(javadocExists));
    ai.size = size;
    ai.lastModified = date;

    File pomFile;
    File artifactFile;
    
    if(file==null || "pom.xml".equals(file.getName())) {
      pomFile = file;
      artifactFile = null;
      // TODO set ai.classNames
    
    } else if(file.getName().endsWith(".pom")) {
      pomFile = file;
      String path = file.getAbsolutePath();
      artifactFile = new File(path.substring(0, path.length()-4) + ".jar");
    
    } else {
      pomFile = new PomLocator().locate( file, gavCalculator, gav );
      artifactFile = file;
    }

    return new ArtifactContext(pomFile, artifactFile, null, ai, gav);
  }

  public void scheduleIndexUpdate(final String repositoryUrl, final boolean force) {
    if(repositoryUrl != null) {
      IndexCommand command = new IndexUpdaterJob.IndexCommand() {
        public void run(IProgressMonitor monitor) throws CoreException {
          updateIndex(repositoryUrl, force, monitor);
        }
      };
      updaterJob.addCommand(command);
      updaterJob.schedule(1000L);
    }
  }

  IndexUpdater getUpdater() {
    return MavenPlugin.lookup(IndexUpdater.class);
  }

  public IndexedArtifactGroup[] getRootGroups(String repositoryUrl) throws CoreException {
    IndexingContext context = getIndexingContext(repositoryUrl);
    if(context != null) {
      try {
        Set<String> rootGroups = context.getRootGroups();
        IndexedArtifactGroup[] groups = new IndexedArtifactGroup[rootGroups.size()];
        int i = 0;
        for(String group : rootGroups) {
          groups[i++] = new IndexedArtifactGroup(repositoryUrl, context.getRepositoryUrl(), group);
        }
        return groups;
      } catch(IOException ex) {
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, //
            "Can't get root groups for " + repositoryUrl, ex));
      }
    }
    return new IndexedArtifactGroup[0];
  }

  /** public for unit tests only! */
  public IndexingContext getIndexingContext(String repositoryUrl) {
    return repositoryUrl == null ? null : getIndexer().getIndexingContexts().get(repositoryUrl);
  }

  public synchronized NexusIndexer getIndexer() {
    if(indexer == null) {
      indexer = MavenPlugin.lookup(NexusIndexer.class);
    }
    return indexer;
  }

  public static String getDocumentKey(ArtifactKey artifact) {
    String groupId = artifact.getGroupId();
    if(groupId == null) {
      groupId = "[inherited]";
    }

    String artifactId = artifact.getArtifactId();

    String version = artifact.getVersion();
    if(version == null) {
      version = "[inherited]";
    }

    String key = groupId.replace('.', '/') + '/' + artifactId + '/' + version + '/' + artifactId + "-" + version;

    String classifier = artifact.getClassifier();
    if(classifier != null) {
      key += "-" + classifier;
    }

    // TODO use artifact handler to retrieve extension
    return key + ".pom";
  }

  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    /*
     * This method is called while holding workspace lock. Avoid long-running operations if possible. 
     */

    Settings settings = null;
    try {
      settings = maven.getSettings();
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }

    for(MavenProjectChangedEvent event : events) {
      try {
        IMavenProjectFacade oldFacade = event.getOldMavenProject();
        if (oldFacade != null) {
          File file = oldFacade.getMavenProject(monitor).getBasedir();
          String key = getDocumentKey(oldFacade.getArtifactKey());
          removeDocument(WORKSPACE_INDEX, file, key);

          removeProjectRepositories(oldFacade);
        }
        IMavenProjectFacade facade = event.getMavenProject();
        if(facade != null) {
          File file = facade.getMavenProject(monitor).getBasedir();
          String key = getDocumentKey(facade.getArtifactKey());
          addDocument(WORKSPACE_INDEX, file, key, -1, -1, null, IIndex.NOT_PRESENT, IIndex.NOT_PRESENT);

          // project-specific indexes
          addProjectRepositories(settings, facade, null /*asyncUpdate*/);
        }
      } catch (CoreException e) {
        MavenLogger.log(e);
      }
    }
  }

  private void removeProjectRepositories(IMavenProjectFacade facade) {
    ArrayList<ArtifactRepositoryRef> repositories = getProjectRepositories(facade);

    for (ArtifactRepositoryRef repo : repositories) {
      RepositoryInfo repository = this.repositories.get(repo.getUrl());
      if (repository != null && !repository.isGlobal()) {
        repository.removeProject(facade.getPom().getFullPath());
        if (repository.getProjects().isEmpty()) {
          removeRepository(repository.getUrl(), false);
        }
      }
    }
    
  }

  private Set<String> addProjectRepositories(Settings settings,
      IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
    Set<String> repositoryUrls = new HashSet<String>();
    
    ArrayList<ArtifactRepositoryRef> repositories = getProjectRepositories(facade);

    for (ArtifactRepositoryRef repo : repositories) {
      RepositoryInfo repository = this.repositories.get(repo.getUrl());
      if (repository != null) {
        repository.addProject(facade.getPom().getFullPath());
        continue;
      }
      AuthenticationInfo auth = getAuthenticationInfo(settings, repo.getId());
      ProxyInfo proxy = getProxyInfo(settings, repo.getUrl());
      repository = new RepositoryInfo(repo.getId(), repo.getUrl(), false /*global*/, auth, proxy);
      repository.addProject(facade.getPom().getFullPath());

      addRepository(repository, //
          RepositoryInfo.DETAILS_MIN, //
          monitor);
      repositoryUrls.add(repository.getUrl());
    }
    return repositoryUrls;
  }

  private ArrayList<ArtifactRepositoryRef> getProjectRepositories(IMavenProjectFacade facade) {
    ArrayList<ArtifactRepositoryRef> repositories = new ArrayList<ArtifactRepositoryRef>();
    repositories.addAll(facade.getArtifactRepositoryRefs());
    repositories.addAll(facade.getPluginArtifactRepositoryRefs());
    return repositories;
  }

  public NexusIndex getWorkspaceIndex() {
    return workspaceIndex;
  }

  public NexusIndex getLocalIndex() {
    return localIndex;
  }

  public IIndex getIndex(IProject project) {
    IMavenProjectFacade projectFacade = project != null? projectManager.getProject(project): null;

    ArrayList<IIndex> indexes = new ArrayList<IIndex>();
    indexes.add(getWorkspaceIndex());
    indexes.add(getLocalIndex());

    if (projectFacade != null) {
      LinkedHashSet<ArtifactRepositoryRef> repositories = new LinkedHashSet<ArtifactRepositoryRef>();
      repositories.addAll(projectFacade.getArtifactRepositoryRefs());
      repositories.addAll(projectFacade.getPluginArtifactRepositoryRefs());

      for (ArtifactRepositoryRef repositoryRef : repositories) {
        indexes.add(getIndex(repositoryRef.getUrl()));
      }
    } else {
      for (RepositoryInfo repoInfo : this.repositories.values()) {
        if (repoInfo.isGlobal()) {
          indexes.add(getIndex(repoInfo.getUrl()));
        }
      }
    }

    return new CompositeIndex(indexes);
  }

  public NexusIndex getIndex(String repositoryUrl) {
    String details = indexDetails.getProperty(repositoryUrl, RepositoryInfo.DETAILS_DISABLED);
    return new NexusIndex(this, repositoryUrl, details);
  }

  public File getIndexDirectoryFile(String repositoryUrl) {
    String indexName;
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      digest.update(repositoryUrl.getBytes());
      byte messageDigest[] = digest.digest();
      StringBuffer hexString = new StringBuffer();
      for (int i = 0; i < messageDigest.length; i++)
      {
          String hex = Integer.toHexString(0xFF & messageDigest[i]);
          if (hex.length() == 1)
          {
              hexString.append('0');
          }
          hexString.append(hex);
      }
      indexName = hexString.toString();
    } catch(NoSuchAlgorithmException ex) {
      //this shouldn't happen with MD5
      indexName = repositoryUrl.replace(':', '_').replace('/', '_');
    }
    
    return new File(getBaseIndexDir(), indexName);
  }

  protected Directory getIndexDirectory(String repositoryPath) throws IOException {
    return FSDirectory.getDirectory(getIndexDirectoryFile(repositoryPath));
  }
  

  public File getBaseIndexDir() {
    return baseIndexDir;
  }

  public IndexedArtifactGroup resolveGroup(IndexedArtifactGroup group) {
    //IndexInfo info = group.getIndexInfo();
    String indexName = group.getIndexName();
    String prefix = group.getPrefix();
    try {
      IndexedArtifactGroup g = new IndexedArtifactGroup(group.getIndexName(), group.getRepositoryUrl(), prefix);
      for(IndexedArtifact a : search(indexName, prefix, IIndex.SEARCH_GROUP).values()) {
        String groupId = a.getGroupId();
        if(groupId.equals(prefix)) {
          g.getFiles().put(a.getArtifactId(), a);
        } else if(groupId.startsWith(prefix + ".")) {
          int start = prefix.length() + 1;
          int end = groupId.indexOf('.', start);
          String key = end > -1 ? groupId.substring(0, end) : groupId;
          g.getNodes().put(key, new IndexedArtifactGroup(group.getIndexName(), group.getRepositoryUrl(), key));
        }
      }
  
      return g;
  
    } catch(CoreException ex) {
      MavenLogger.log("Can't retrieve groups for " + indexName + ":" + prefix, ex);
      return group;
    }
  }

  private void addRepository(RepositoryInfo repository, String defaultIndexDetails, IProgressMonitor monitor) throws CoreException {
    String repositoryUrl = repository.getUrl();

    String details = indexDetails.getProperty(repositoryUrl, defaultIndexDetails);

    repositories.put(repositoryUrl, repository);

    setIndexDetails(repositoryUrl, null, details, monitor);
  }

  /**
   * Updates index synchronously if  monitor!=null. Schedules index update otherwise.
   * ... and yes, I know this ain't kosher.
   */
  private void setIndexDetails(String repositoryUrl, File repositoryPath, String details, IProgressMonitor monitor) throws CoreException {
    IndexingContext indexingContext = getIndexer().getIndexingContexts().get(repositoryUrl);

    try {
      if (RepositoryInfo.DETAILS_DISABLED.equals(details)) {
        if (indexingContext != null) {
          getIndexer().removeIndexingContext(indexingContext, false /*removeFiles*/);
          fireIndexRemoved(repositoryUrl);
        }
      } else {
          Directory directory = getIndexDirectory(repositoryUrl);

          if (repositoryPath == null && repositoryUrl.startsWith("file:")) {
            repositoryPath = new File(getBasedir(repositoryUrl)).getCanonicalFile();
          }

          boolean fullIndex = RepositoryInfo.DETAILS_FULL.equals(details);
  
          indexingContext = getIndexer().addIndexingContextForced(
              repositoryUrl, //
              repositoryUrl, //
              repositoryPath, //
              directory, //
              repositoryUrl,
              null, //
              (fullIndex ? getFullCreator() : getMinCreator()));

        fireIndexAdded(repositoryUrl);

        boolean forceUpdate = false;
        if (monitor != null) {
          updateIndex(repositoryUrl, forceUpdate, monitor);
        } else {
          scheduleIndexUpdate(repositoryUrl, forceUpdate);
        }
      }
    } catch(IOException ex) {
      String msg = "Error changing index details " + repositoryUrl;
      console.logError(msg + "; " + ex.getMessage());
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Could not add repository index", ex));
    }
  }

  private void removeRepository(String repositoryUrl, boolean deleteFiles){
    try {
      repositories.remove(repositoryUrl);
      
      IndexingContext context = getIndexingContext(repositoryUrl);
      if(context == null) {
        return;
      }

      getIndexer().removeIndexingContext(context, deleteFiles);
      fireIndexRemoved(repositoryUrl);
      
    } catch(IOException ie){
      String msg = "Unable to delete files for index";
      MavenLogger.log(msg, ie);
    }
  }

  public void fireIndexAdded(String repositoryUrl){
    synchronized(indexListeners){
      for(IndexListener listener : indexListeners){
        listener.indexAdded(repositoryUrl);
      }
    }
  }
  public void fireIndexRemoved(String indexName){
    synchronized(updatingIndexes){
      updatingIndexes.remove(indexName);
    }
    synchronized(indexListeners){
      for(IndexListener listener : indexListeners){
        listener.indexRemoved(indexName);
      }
    }
  }
  
  private Set<String> updatingIndexes = new HashSet<String>();
  public boolean isUpdatingIndex(String indexName){
    synchronized(updatingIndexes){
      return updatingIndexes.contains(indexName);
    }
  }
  
  public void fireIndexUpdating(String indexName){
    synchronized(updatingIndexes){
      updatingIndexes.add(indexName);
    }    
    synchronized(indexListeners){
      for(IndexListener listener : indexListeners){
        listener.indexUpdating(indexName);
      }
    }
  }
  public void fireIndexChanged(String indexName){
    synchronized(updatingIndexes){
      updatingIndexes.remove(indexName);
    }
    synchronized(indexListeners){
      for(IndexListener listener : indexListeners){
        listener.indexChanged(indexName);
      }
    }
  }
  
  
  public void removeIndexListener(IndexListener listener){
    synchronized(indexListeners){
      indexListeners.remove(listener);
    }
  }
  
  public void addIndexListener(IndexListener listener){
    synchronized(indexListeners){
      if(!indexListeners.contains(listener)){
        indexListeners.add(listener);
      }
    }
  }

  void updateIndex(String repositoryUrl, boolean force, IProgressMonitor monitor) throws CoreException {
    if (IndexManager.WORKSPACE_INDEX.equals(repositoryUrl)) {
      reindexWorkspace(monitor);
    } else {
      IndexingContext context = getIndexingContext(repositoryUrl);
      if (context.getRepository() != null) {
        reindexLocalRepository(repositoryUrl, monitor);
      } else {
        updateRemoteIndex(repositoryUrl, force, monitor);
      }
    }
  }

  private void updateRemoteIndex(String repositoryUrl, boolean force, IProgressMonitor monitor) {
    RepositoryInfo repository = repositories.get(repositoryUrl);
    if (repository == null) {
      return;
    }

    monitor.setTaskName("Updating index " + repositoryUrl);
    console.logMessage("Updating index " + repositoryUrl);
    try {
      fireIndexUpdating(repositoryUrl);

      IndexingContext context = getIndexingContext(repositoryUrl);

      IndexUpdateRequest request = new IndexUpdateRequest(context);
      request.setProxyInfo(repository.getProxyInfo());
      request.setAuthenticationInfo(repository.getAuthenticationInfo());
      request.setTransferListener(maven.createTransferListener(monitor));
      request.setForceFullUpdate(force);
      Date indexTime = getUpdater().fetchAndUpdateIndex(request);
      if(indexTime==null) {
        console.logMessage("No index update available for " + repositoryUrl);
      } else {
        console.logMessage("Updated index for " + repositoryUrl + " " + indexTime);
      }
//    } catch(CoreException ex) {
//      String msg = "Unable to update index for " + repositoryUrl;
//      MavenLogger.log(msg, ex);
//      console.logError(msg);
//    } catch (OperationCanceledException ex) {
//      console.logMessage("Updating index " + repositoryUrl + " is canceled");
    } catch(IOException ie){
      String msg = "Unable to update index for " + repositoryUrl;
      MavenLogger.log(msg, ie);
      console.logError(msg);
//    } catch(Exception e){
    } finally {
      fireIndexChanged(repositoryUrl);
    }
  }

  /**
   * This method is called from bundle startup thread and so it should do
   * as little as possible to avoid classloader timeouts.
   */
  public void initialize() {
    updaterJob.addCommand(new IndexCommand() {
      public void run(IProgressMonitor monitor) throws CoreException {
        initialize(monitor);
      }
    });
    updaterJob.schedule(1000L);
  }

  void initialize(IProgressMonitor monitor) throws CoreException {
    try {
      BufferedInputStream is = new BufferedInputStream(new FileInputStream(getIndexDetailsFile()));
      try {
        indexDetails.load(is);
      } finally {
        is.close();
      }
    } catch (FileNotFoundException e) {
      // that's quite alright
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Could not read index details file", e));
    }

    // create workspace and localRepo indexing contexts
    setIndexDetails(IndexManager.WORKSPACE_INDEX, null /*repositoryPath*/, RepositoryInfo.DETAILS_MIN, monitor);
    ArtifactRepository localRepository = maven.getLocalRepository();
    String localRepositoryDetails = indexDetails.getProperty(IndexManager.LOCAL_INDEX, RepositoryInfo.DETAILS_FULL);
    setIndexDetails(IndexManager.LOCAL_INDEX, new File(localRepository.getBasedir()) /*repositoryPath*/, localRepositoryDetails, monitor);

    // process configured repositories

    Set<String> oldRepositoryUrls = new HashSet<String>(repositories.keySet());

    // mirrors
    Settings settings = maven.getSettings();
    List<Mirror> mirrors = maven.getMirrors();
    for(Mirror mirror : mirrors) {
      AuthenticationInfo auth = getAuthenticationInfo(settings, mirror.getId());
      ProxyInfo proxy = getProxyInfo(settings, mirror.getUrl());
      RepositoryInfo repository = new RepositoryInfo(mirror.getId(), mirror.getUrl(), true /*global*/, auth, proxy);
      repository.setMirrorOf(mirror.getMirrorOf());
      addRepository(repository, RepositoryInfo.DETAILS_MIN, monitor);
      oldRepositoryUrls.remove(repository.getUrl());
    }

    // repositories from settings.xml
    ArrayList<ArtifactRepository> repos = new ArrayList<ArtifactRepository>();
    repos.addAll(maven.getArtifactRepositories(false));
    repos.addAll(maven.getPluginArtifactRepositories(false));

    for(ArtifactRepository repo : repos) {
      Mirror mirror = maven.getMirror(repo);
      AuthenticationInfo auth = getAuthenticationInfo(settings, repo.getId());
      ProxyInfo proxy = getProxyInfo(settings, repo.getUrl());
      RepositoryInfo repository = new RepositoryInfo(repo.getId(), repo.getUrl(), true /*global*/, auth, proxy);
      if (mirror != null) {
        repository.setMirrorId(mirror.getId());
      }
      addRepository(repository, //
          mirror != null ? RepositoryInfo.DETAILS_DISABLED : RepositoryInfo.DETAILS_MIN, //
          monitor);
      oldRepositoryUrls.remove(repository.getUrl());
    }

    // project-specific repositories
    for (IMavenProjectFacade facade : projectManager.getProjects()) {
      oldRepositoryUrls.removeAll(addProjectRepositories(settings, facade, monitor));
    }

    for (String repositoryUrl : oldRepositoryUrls) {
      removeRepository(repositoryUrl, false);
    }
  }

  private ProxyInfo getProxyInfo(Settings settings, String url) {
    if (settings == null) {
      return null;
    }

    String protocol = getProtocol(url); // can't be null
    for (Proxy proxy : settings.getProxies()) {
      if (proxy.isActive() && protocol.equalsIgnoreCase(proxy.getProtocol())) {
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setHost(proxy.getHost());
        proxyInfo.setPort(proxy.getPort());
        proxyInfo.setNonProxyHosts(proxy.getNonProxyHosts());
        proxyInfo.setUserName(proxy.getUsername());
        proxyInfo.setPassword(proxy.getPassword());
        return proxyInfo;
      }
    }
    return null;
  }

  private AuthenticationInfo getAuthenticationInfo(Settings settings, String id) {
    if (settings == null) {
      return null;
    }

    Server server = settings.getServer(id);
    if (server == null || server.getUsername() == null) {
      return null;
    }

    AuthenticationInfo info = new AuthenticationInfo();
    info.setUserName(server.getUsername());
    info.setPassword(server.getPassword());
    return info;
  }

  public void setIndexDetails(String repositoryUrl, String details) throws CoreException {
    indexDetails.setProperty(repositoryUrl, details);
    try {
      BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(getIndexDetailsFile()));
      try {
        indexDetails.store(os, null);
      } finally {
        os.close();
      }
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Could not write index details file", e));
    }

    setIndexDetails(repositoryUrl, null, details, null /*asyncUpdate*/);
  }

  private File getIndexDetailsFile() {
    return new File(baseIndexDir, "indexDetails.properties");
  }

  // copy&paste from MavenArtifactRepository#protocol
  private static String getProtocol( final String url )
  {
      final int pos = url.indexOf( ":" );

      if ( pos == -1 )
      {
          return "";
      }
      return url.substring( 0, pos ).trim();
  }

  private static String getBasedir(String repositoryUrl) {
    // absolute hack
    MavenArtifactRepository hack = new MavenArtifactRepository();
    hack.setUrl(repositoryUrl);
    return hack.getBasedir();
  }
  
  public Collection<RepositoryInfo> getRepositories() {
    return repositories.values();
  }

  /** for unit tests only */
  public Job getIndexUpdateJob() {
    return updaterJob;
  }
}
