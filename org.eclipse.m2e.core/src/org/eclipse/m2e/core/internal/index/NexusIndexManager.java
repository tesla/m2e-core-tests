/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.internal.index;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

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
import org.sonatype.nexus.index.creator.JarFileContentsIndexCreator;
import org.sonatype.nexus.index.creator.MavenArchetypeArtifactInfoIndexCreator;
import org.sonatype.nexus.index.creator.MavenPluginArtifactInfoIndexCreator;
import org.sonatype.nexus.index.creator.MinimalArtifactInfoIndexCreator;
import org.sonatype.nexus.index.fs.Lock;
import org.sonatype.nexus.index.locator.PomLocator;
import org.sonatype.nexus.index.updater.IndexUpdateRequest;
import org.sonatype.nexus.index.updater.IndexUpdateResult;
import org.sonatype.nexus.index.updater.IndexUpdater;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ArtifactRepositoryRef;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexListener;
import org.eclipse.m2e.core.index.IndexManager;
import org.eclipse.m2e.core.index.IndexedArtifact;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.index.IndexUpdaterJob.IndexCommand;
import org.eclipse.m2e.core.internal.repository.IRepositoryIndexer;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;


/**
 * @author Eugene Kuleshov
 */
public class NexusIndexManager implements IndexManager, IMavenProjectChangedListener, IRepositoryIndexer {

  /** Field separator */
  public static final String FS = "|"; //$NON-NLS-1$

  public static final Pattern FS_PATTERN = Pattern.compile( Pattern.quote( "|" ) ); //$NON-NLS-1$

  /** Non available value */
  public static final String NA = "NA"; //$NON-NLS-1$

  private final GavCalculator gavCalculator = new M2GavCalculator();

  /**
   * Lazy instantiated nexus indexer instance.
   */
  private NexusIndexer indexer;

  /**
   * Lock guarding lazy instantiation of indexerLock instance
   */
  private final Object indexerLock = new Object();

  private IMaven maven;

  private MavenProjectManager projectManager;
  
  private IRepositoryRegistry repositoryRegistry;

  private ArrayList<IndexCreator> fullCreators = null;

  private ArrayList<IndexCreator> minCreators = null;

  private final MavenConsole console;

  private final File baseIndexDir;

  private final List<IndexListener> indexListeners = new ArrayList<IndexListener>();

  private NexusIndex localIndex;

  private final NexusIndex workspaceIndex;

  private final IndexUpdaterJob updaterJob;

  private Properties indexDetails = new Properties();

  private Set<String> updatingIndexes = new HashSet<String>();

  private IndexUpdater indexUpdater;

  private static final EquinoxLocker locker = new EquinoxLocker();

  /**
   * Maps repository UID to the lock object associated with the repository. Entries are only added
   * but never directly removed from the map, although jvm garbage collector may remove otherwise
   * unused entries to reclaim the little memory they use. 
   * 
   * Never access this map directly. #getIndexLock must be used to get repository lock object.
   */
  private final Map<String, Object> indexLocks = new WeakHashMap<String, Object>();

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

  public NexusIndexManager(MavenConsole console, MavenProjectManager projectManager, IRepositoryRegistry repositoryRegistry, File stateDir) {
    this.console = console;
    this.projectManager = projectManager;
    this.repositoryRegistry = repositoryRegistry;
    this.baseIndexDir = new File(stateDir, "nexus"); //$NON-NLS-1$

    this.maven = MavenPlugin.getDefault().getMaven();
    this.indexUpdater = MavenPlugin.getDefault().getIndexUpdater();

    this.updaterJob = new IndexUpdaterJob(this, console);

    this.workspaceIndex = new NexusIndex(this, repositoryRegistry.getWorkspaceRepository(), NexusIndex.DETAILS_MIN);
  }

  private NexusIndex newLocalIndex(IRepository localRepository) {
    return new NexusIndex(this, localRepository, NexusIndex.DETAILS_FULL);
  }

  private ArrayList<IndexCreator> getFullCreator() {
    if(fullCreators == null) {
      try {
        PlexusContainer container = MavenPlugin.getDefault().getPlexusContainer();
        IndexCreator min = container.lookup( IndexCreator.class, MinimalArtifactInfoIndexCreator.ID );
        IndexCreator mavenPlugin = container.lookup( IndexCreator.class, MavenPluginArtifactInfoIndexCreator.ID );
        IndexCreator mavenArchetype = container.lookup( IndexCreator.class, MavenArchetypeArtifactInfoIndexCreator.ID );
        IndexCreator jar = container.lookup( IndexCreator.class, JarFileContentsIndexCreator.ID );
        
        fullCreators = new ArrayList<IndexCreator>();
        fullCreators.add(min);
        fullCreators.add(jar);
        fullCreators.add(mavenPlugin);      
        fullCreators.add(mavenArchetype);
      } catch(ComponentLookupException ce) {
        String msg = "Error looking up component ";
        console.logError(msg + "; " + ce.getMessage()); //$NON-NLS-1$
        MavenLogger.log(msg, ce);

      }
    }
    return fullCreators;
  }
  
  private ArrayList<IndexCreator> getMinCreator() {
    if(minCreators == null) {
      try {
        PlexusContainer container = MavenPlugin.getDefault().getPlexusContainer();
        IndexCreator min = container.lookup( IndexCreator.class, MinimalArtifactInfoIndexCreator.ID );
        IndexCreator mavenArchetype = container.lookup( IndexCreator.class, MavenArchetypeArtifactInfoIndexCreator.ID );
        minCreators = new ArrayList<IndexCreator>();
        minCreators.add(min);
        minCreators.add(mavenArchetype);
      } catch(ComponentLookupException ce) {
        String msg = "Error looking up component ";
        MavenLogger.log(msg, ce);
      }

    }
    return minCreators;
  }


  public IndexedArtifactFile getIndexedArtifactFile(IRepository repository, ArtifactKey gav) throws CoreException {

    try {
      BooleanQuery query = new BooleanQuery();
      query.add( new TermQuery(new Term(ArtifactInfo.GROUP_ID, gav.getGroupId())), BooleanClause.Occur.MUST );
      query.add( new TermQuery(new Term(ArtifactInfo.ARTIFACT_ID, gav.getArtifactId())), BooleanClause.Occur.MUST );
      query.add( new TermQuery(new Term(ArtifactInfo.VERSION, gav.getVersion())), BooleanClause.Occur.MUST );
      if (gav.getClassifier() != null) {
        query.add( new TermQuery(new Term(ArtifactInfo.CLASSIFIER, gav.getClassifier())), BooleanClause.Occur.MUST );
      }

      synchronized(getIndexLock(repository)) {
        ArtifactInfo artifactInfo = getIndexer().identify(query, Collections.singleton(getIndexingContext(repository)));
        if(artifactInfo != null) {
          return getIndexedArtifactFile(artifactInfo);
        }
      }
    } catch(Exception ex) {
      String msg = "Illegal artifact coordinate " + ex.getMessage();
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, Messages.NexusIndexManager_error_search, ex));
    } 
    return null;
  }
  
  public IndexedArtifactFile identify(File file) throws CoreException {
    try {
      ArtifactInfo artifactInfo = getIndexer().identify(file);
      return artifactInfo==null ? null : getIndexedArtifactFile(artifactInfo);
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, Messages.NexusIndexManager_error_search, ex));
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
      tq = new TermQuery(new Term(ArtifactInfo.CLASSIFIER, "javadoc")); //$NON-NLS-1$
      bq.add(tq, Occur.MUST_NOT);
    }
    boolean includeSources = (classifier & IIndex.SEARCH_SOURCES) > 0;
    if(!includeSources){
      tq = new TermQuery(new Term(ArtifactInfo.CLASSIFIER, "sources")); //$NON-NLS-1$
      bq.add(tq, Occur.MUST_NOT);
    }
    boolean includeTests = (classifier & IIndex.SEARCH_TESTS) > 0;
    if(!includeTests){
      tq = new TermQuery(new Term(ArtifactInfo.CLASSIFIER, "tests")); //$NON-NLS-1$
      bq.add(tq, Occur.MUST_NOT);
    }
  }
  /**
   * @return Map<String, IndexedArtifact>
   */
  public Map<String, IndexedArtifact> search(IRepository repository, String term, String type, int classifier) throws CoreException {
    Query query;
    if(IIndex.SEARCH_CLASS_NAME.equals(type)) {
      query = getIndexer().constructQuery(ArtifactInfo.NAMES, term + "$"); //$NON-NLS-1$
      
    } else if(IIndex.SEARCH_GROUP.equals(type)) {
      query = new TermQuery(new Term(ArtifactInfo.GROUP_ID, term));
      //query = new PrefixQuery(new Term(ArtifactInfo.GROUP_ID, term));

    } else if(IIndex.SEARCH_ARTIFACT.equals(type)) {
      BooleanQuery bq = new BooleanQuery();
      
      bq.add(getIndexer().constructQuery(ArtifactInfo.GROUP_ID, term), Occur.SHOULD);
      bq.add(getIndexer().constructQuery(ArtifactInfo.ARTIFACT_ID, term), Occur.SHOULD);
      bq.add(new PrefixQuery(new Term(ArtifactInfo.SHA1, term)), Occur.SHOULD);
      addClassifiersToQuery(bq, classifier);
      query = bq;

    } else if(IIndex.SEARCH_PARENTS.equals(type)) {
      if( term == null || "*".equals(term) || "".equals(term)) { //$NON-NLS-1$ //$NON-NLS-2$
        query = new TermQuery(new Term(ArtifactInfo.PACKAGING, "pom")); //$NON-NLS-1$
      } else {
        BooleanQuery bq = new BooleanQuery();
        bq.add(getIndexer().constructQuery(ArtifactInfo.GROUP_ID, term), Occur.SHOULD);
        bq.add(getIndexer().constructQuery(ArtifactInfo.ARTIFACT_ID, term), Occur.SHOULD);
        bq.add(new PrefixQuery(new Term(ArtifactInfo.SHA1, term)), Occur.SHOULD);
        TermQuery tq = new TermQuery(new Term(ArtifactInfo.PACKAGING, "pom")); //$NON-NLS-1$
        query = new FilteredQuery(tq, new QueryWrapperFilter(bq));
      }
      
    } else if(IIndex.SEARCH_PLUGIN.equals(type)) {
      if("*".equals(term)) { //$NON-NLS-1$
        query = new TermQuery(new Term(ArtifactInfo.PACKAGING, "maven-plugin")); //$NON-NLS-1$
      } else {
        BooleanQuery bq = new BooleanQuery();
        bq.add(new WildcardQuery(new Term(ArtifactInfo.GROUP_ID, term + "*")), Occur.SHOULD); //$NON-NLS-1$
        bq.add(new WildcardQuery(new Term(ArtifactInfo.ARTIFACT_ID, term + "*")), Occur.SHOULD); //$NON-NLS-1$
        TermQuery tq = new TermQuery(new Term(ArtifactInfo.PACKAGING, "maven-plugin")); //$NON-NLS-1$
        query = new FilteredQuery(tq, new QueryWrapperFilter(bq));
      }
      
    } else if(IIndex.SEARCH_ARCHETYPE.equals(type)) {
      BooleanQuery bq = new BooleanQuery();
      bq.add(new WildcardQuery(new Term(ArtifactInfo.GROUP_ID, term + "*")), Occur.SHOULD); //$NON-NLS-1$
      bq.add(new WildcardQuery(new Term(ArtifactInfo.ARTIFACT_ID, term + "*")), Occur.SHOULD); //$NON-NLS-1$
      TermQuery tq = new TermQuery(new Term(ArtifactInfo.PACKAGING, "maven-archetype")); //$NON-NLS-1$
      query = new FilteredQuery(tq, new QueryWrapperFilter(bq));
      
    } else if(IIndex.SEARCH_PACKAGING.equals(type)) {
      query = new TermQuery(new Term(ArtifactInfo.PACKAGING, term));

    } else if(IIndex.SEARCH_SHA1.equals(type)) {
      query = new WildcardQuery(new Term(ArtifactInfo.SHA1, term + "*")); //$NON-NLS-1$
      
    } else {
      return Collections.emptyMap();

    }

    Map<String, IndexedArtifact> result = new TreeMap<String, IndexedArtifact>();

    try {
      FlatSearchResponse response;

      synchronized(getIndexLock(repository)) {
        IndexingContext context = getIndexingContext(repository);
        if(context == null) {
          response = getIndexer().searchFlat(new FlatSearchRequest(query));
        } else {
          response = getIndexer().searchFlat(new FlatSearchRequest(query, context));
        }

        String regex = "^(.*?" + term.replaceAll("\\*", ".+?") + ".*?)$"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
                packageName = ""; //$NON-NLS-1$
                className = value;
              } else {
                packageName = value.substring(0, n).replace('/', '.');
                className = value.substring(n + 1);
              }
              addArtifactFile(result, af, className, packageName, artifactInfo.packaging);
            }
          }
        }

        // https://issues.sonatype.org/browse/MNGECLIPSE-1630
        // lucene can't handle prefix queries that match many index entries.
        // to workaround, use term query to locate group artifacts and manually
        // match subgroups
        if(IIndex.SEARCH_GROUP.equals(type) && context != null) {
          Set<String> groups = context.getAllGroups();
          for(String group : groups) {
            if(group.startsWith(term) && !group.equals(term)) {
              String key = getArtifactFileKey(group, group, null, null);
              result.put(key, new IndexedArtifact(group, group, null, null, null));
            }
          }
        }
      }
    }catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, Messages.NexusIndexManager_error_search, ex));
    }

    return result;
  }
  
  /**
   * @return Map<String, IndexedArtifact>
   */
  public Map<String, IndexedArtifact> search(IRepository repository, String term, String type) throws CoreException {
    return search(repository, term, type, IIndex.SEARCH_ALL);
  }
  
  /**
   * @return Map<String, IndexedArtifact>
   */
  public Map<String, IndexedArtifact> search(IRepository repository, Query query) throws CoreException {
    Map<String, IndexedArtifact> result = new TreeMap<String, IndexedArtifact>();
    try {
      FlatSearchResponse response;

      synchronized(getIndexLock(repository)) {
        IndexingContext context = getIndexingContext(repository);
        if(context == null) {
          response = getIndexer().searchFlat(new FlatSearchRequest(query));
        } else {
          response = getIndexer().searchFlat(new FlatSearchRequest(query, context));
        }
      }

      for(ArtifactInfo artifactInfo : response.getResults()) {
        IndexedArtifactFile af = getIndexedArtifactFile(artifactInfo);
        addArtifactFile(result, af, null, null, artifactInfo.packaging);
      }
      
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, Messages.NexusIndexManager_error_search, ex));
    }
    return result;
  }  

  private void addArtifactFile(Map<String, IndexedArtifact> result, IndexedArtifactFile af, String className, String packageName,
      String packaging) {
    String group = af.group;
    String artifact = af.artifact;
    String key = getArtifactFileKey(group, artifact, packageName, className);
    IndexedArtifact indexedArtifact = result.get(key);
    if(indexedArtifact == null) {
      indexedArtifact = new IndexedArtifact(group, artifact, packageName, className, packaging);
      result.put(key, indexedArtifact);
    }
    indexedArtifact.addFile(af);
  }

  protected String getArtifactFileKey(String group, String artifact, String packageName, String className) {
    return className + " : " + packageName + " : " + group + " : " + artifact; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
  }

  public IndexedArtifactFile getIndexedArtifactFile(ArtifactInfo artifactInfo) {
    String groupId = artifactInfo.groupId;
    String artifactId = artifactInfo.artifactId;
    String repository = artifactInfo.repository;
    String version = artifactInfo.version;
    String classifier = artifactInfo.classifier;
    String packaging = artifactInfo.packaging;
    String fname = artifactInfo.fname;
    if(fname == null) {
      fname = artifactId + '-' + version + (classifier != null ? '-' + classifier : "") + (packaging != null ? ('.' + packaging) : ""); //$NON-NLS-1$ //$NON-NLS-2$
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

  private void reindexLocalRepository(IRepository repository, boolean force, final IProgressMonitor monitor) throws CoreException {
    try {
      if (force) {
        fireIndexUpdating(repository);
        //IndexInfo indexInfo = getIndexInfo(indexName);
        IndexingContext context = getIndexer().getIndexingContexts().get(repository.getUid());
        getIndexer().scan(context, new ArtifactScanningMonitor(context.getRepository(), monitor, console), false);
        fireIndexChanged(repository);
        console.logMessage("Updated local repository index");
      }
      
    } catch(Exception ex) {
      MavenLogger.log("Unable to re-index "+repository.toString(), ex);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, Messages.NexusIndexManager_error_reindexing, ex));
    } finally {
      fireIndexChanged(repository);
    }
  }

  private void reindexWorkspace(IProgressMonitor monitor) {
    // TODO clean current index
    
    IRepository workspaceRepository = repositoryRegistry.getWorkspaceRepository();
    for (IMavenProjectFacade facade : projectManager.getProjects()) {
      addDocument(workspaceRepository, facade.getPomFile(), //
          getDocumentKey(facade.getArtifactKey()), -1, -1, null, 0, 0);
    }

    fireIndexChanged(workspaceRepository);
  }

  public void addDocument(IRepository repository, File file, String documentKey, long size, long date, File jarFile,
      int sourceExists, int javadocExists) {
    synchronized(getIndexLock(repository)) {
      IndexingContext context = getIndexingContext(repository);
      if(context == null) {
        // TODO log
        return;
      }

      try {
        ArtifactContext artifactContext = getArtifactContext(file, documentKey, size, date, //
            sourceExists, javadocExists, context.getRepositoryId());
        addArtifactToIndex(context, artifactContext);
      } catch(Exception ex) {
        String msg = "Unable to add " + documentKey;
        console.logError(msg + "; " + ex.getMessage()); //$NON-NLS-1$
        MavenLogger.log(msg, ex);
      }
    }
  }

  private void addArtifactToIndex(IndexingContext context, ArtifactContext artifactContext) throws IOException {
    getIndexer().addArtifactToIndex(artifactContext, context);
  }
  
  public void removeDocument(IRepository repository, File file, String documentKey) {
    synchronized(getIndexLock(repository)) {
      try {
        IndexingContext context = getIndexingContext(repository);
        if(context == null) {
          String msg = "Unable to find document to remove"+documentKey;
          MavenLogger.log(new Status(IStatus.ERROR,"org.eclipse.m2e", msg)); //$NON-NLS-1$
          return;
        }
        ArtifactContext artifactContext = getArtifactContext(null, documentKey, -1, -1, //
            IIndex.NOT_AVAILABLE, IIndex.NOT_AVAILABLE, context.getRepositoryId());
        getIndexer().deleteArtifactFromIndex(artifactContext, context);
      } catch(Exception ex) {
        String msg = "Unable to remove " + documentKey;
        console.logError(msg + "; " + ex.getMessage()); //$NON-NLS-1$
        MavenLogger.log(msg, ex);
      }
    }

    fireIndexChanged(repository);
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
    
    if(file==null || "pom.xml".equals(file.getName())) { //$NON-NLS-1$
      pomFile = file;
      artifactFile = null;
      // TODO set ai.classNames
    
    } else if(file.getName().endsWith(".pom")) { //$NON-NLS-1$
      pomFile = file;
      String path = file.getAbsolutePath();
      artifactFile = new File(path.substring(0, path.length()-4) + ".jar");   //$NON-NLS-1$
    } else {
      pomFile = new PomLocator().locate( file, gavCalculator, gav );
      artifactFile = file;
    }

    return new ArtifactContext(pomFile, artifactFile, null, ai, gav);
  }

  private ArtifactContext getArtifactContext(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
    IRepository workspaceRepository = repositoryRegistry.getWorkspaceRepository();
    ArtifactKey key = facade.getArtifactKey();
    ArtifactInfo ai = new ArtifactInfo(workspaceRepository.getUid(), key.getGroupId(), key.getArtifactId(), key.getVersion(), null);
    ai.packaging = facade.getPackaging();
    File pomFile = facade.getPomFile();
    File artifactFile = (pomFile != null) ? pomFile.getParentFile() : null;
    try {
      Gav gav = new Gav(key.getGroupId(), key.getArtifactId(), key.getVersion());
      return new ArtifactContext(pomFile, artifactFile, null, ai, gav );
    } catch(IllegalArtifactCoordinateException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, Messages.NexusIndexManager_error_unexpected, ex));
    }
  }

  public void scheduleIndexUpdate(final IRepository repository, final boolean force) {
    if(repository != null) {
      IndexCommand command = new IndexUpdaterJob.IndexCommand() {
        public void run(IProgressMonitor monitor) throws CoreException {
          updateIndex(repository, force, monitor);
        }
      };
      updaterJob.addCommand(command);
      updaterJob.schedule(1000L);
    }
  }

  public IndexedArtifactGroup[] getRootGroups(IRepository repository) throws CoreException {
    synchronized(getIndexLock(repository)) {
      IndexingContext context = getIndexingContext(repository);
      if(context != null) {
        try {
          Set<String> rootGroups = context.getRootGroups();
          IndexedArtifactGroup[] groups = new IndexedArtifactGroup[rootGroups.size()];
          int i = 0;
          for(String group : rootGroups) {
            groups[i++ ] = new IndexedArtifactGroup(repository, group);
          }
          return groups;
        } catch(IOException ex) {
          throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, //
              NLS.bind(Messages.NexusIndexManager_error_root_grp, repository.toString()), ex));
        }
      }
      return new IndexedArtifactGroup[0];
    }
  }

  /** public for unit tests only! */
  public IndexingContext getIndexingContext(IRepository repository) {
    return repository == null ? null : getIndexer().getIndexingContexts().get(repository.getUid());
  }

  private NexusIndexer getIndexer() {
    synchronized(indexerLock) {
      if(indexer == null) {
        indexer = MavenPlugin.getDefault().getNexusIndexer();
      }
    }
    return indexer;
  }

  public static String getDocumentKey(ArtifactKey artifact) {
    String groupId = artifact.getGroupId();
    if(groupId == null) {
      groupId = Messages.NexusIndexManager_inherited;
    }

    String artifactId = artifact.getArtifactId();

    String version = artifact.getVersion();
    if(version == null) {
      version = Messages.NexusIndexManager_inherited;
    }

    String key = groupId.replace('.', '/') + '/' + artifactId + '/' + version + '/' + artifactId + "-" + version; //$NON-NLS-1$

    String classifier = artifact.getClassifier();
    if(classifier != null) {
      key += "-" + classifier; //$NON-NLS-1$
    }

    // TODO use artifact handler to retrieve extension
    return key + ".pom"; //$NON-NLS-1$
  }

  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    /*
     * This method is called while holding workspace lock. Avoid long-running operations if possible. 
     */

    synchronized(getIndexLock(repositoryRegistry.getWorkspaceRepository())) {
      IndexingContext context = getIndexingContext(repositoryRegistry.getWorkspaceRepository());
      
      if (context != null) {
        // workspace indexing context can by null during startup due to MNGECLIPSE-1633
        for(MavenProjectChangedEvent event : events) {
          try {
            IMavenProjectFacade oldFacade = event.getOldMavenProject();
            if (oldFacade != null) {
              ArtifactContext artifactContext = getArtifactContext(oldFacade, monitor);
              getIndexer().deleteArtifactFromIndex(artifactContext, context);
              fireIndexRemoved(repositoryRegistry.getWorkspaceRepository());
            }
            IMavenProjectFacade facade = event.getMavenProject();
            if(facade != null) {
              ArtifactContext artifactContext = getArtifactContext(facade, monitor);
              getIndexer().addArtifactToIndex(artifactContext, context);
              fireIndexAdded(repositoryRegistry.getWorkspaceRepository());
            }
          } catch (CoreException e) {
            MavenLogger.log(e);
          } catch(IOException ex) {
            MavenLogger.log("Could not update workspace index", ex);
          }
        }
      }
    }
  }


  public NexusIndex getWorkspaceIndex() {
    return workspaceIndex;
  }

  public NexusIndex getLocalIndex() {
    IRepository localRepository = repositoryRegistry.getLocalRepository();
    synchronized(getIndexLock(localRepository)) {
      if(localIndex == null) {
        localIndex = newLocalIndex(localRepository);
      }
    }
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
        IRepository repository = repositoryRegistry.getRepository(repositoryRef);
        indexes.add(getIndex(repository));
      }
    } else {
      for (IRepository repository : repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_SETTINGS)) {
        indexes.add(getIndex(repository));
      }
    }

    return new CompositeIndex(indexes);
  }

  public NexusIndex getIndex(IRepository repository) {
    String details = getIndexDetails(repository);
    return new NexusIndex(this, repository, details);
  }

  public File getIndexDirectoryFile(IRepository repository) {
    return new File(baseIndexDir, repository.getUid());
  }

  protected Directory getIndexDirectory(IRepository repository) throws IOException {
    return FSDirectory.getDirectory(getIndexDirectoryFile(repository));
  }
  
  public IndexedArtifactGroup resolveGroup(IndexedArtifactGroup group) {
    IRepository repository = group.getRepository();
    String prefix = group.getPrefix();
    try {
      IndexedArtifactGroup g = new IndexedArtifactGroup(repository, prefix);
      for(IndexedArtifact a : search(repository, prefix, IIndex.SEARCH_GROUP).values()) {
        String groupId = a.getGroupId();
        if(groupId.equals(prefix)) {
          g.getFiles().put(a.getArtifactId(), a);
        } else if(groupId.startsWith(prefix + ".")) { //$NON-NLS-1$
          int start = prefix.length() + 1;
          int end = groupId.indexOf('.', start);
          String key = end > -1 ? groupId.substring(0, end) : groupId;
          g.getNodes().put(key, new IndexedArtifactGroup(repository, key));
        }
      }
  
      return g;
  
    } catch(CoreException ex) {
      MavenLogger.log("Can't retrieve groups for " + repository.toString() + ":" + prefix, ex); //$NON-NLS-2$
      return group;
    }
  }

  public void repositoryAdded(IRepository repository, IProgressMonitor monitor) throws CoreException {
    String details = getIndexDetails(repository);

    // for consistency, always process indexes using our background thread
    setIndexDetails(repository, null, details, null/*async*/);
  }

  public String getIndexDetails(IRepository repository) {
    String details = indexDetails.getProperty(repository.getUid());

    if (details == null) {
      if (repository.isScope(IRepositoryRegistry.SCOPE_SETTINGS) && repository.getMirrorId() == null) {
        details = NexusIndex.DETAILS_MIN;
      } else if (repository.isScope(IRepositoryRegistry.SCOPE_LOCAL)) {
        details = NexusIndex.DETAILS_MIN;
      } else if (repository.isScope(IRepositoryRegistry.SCOPE_WORKSPACE)) {
        details = NexusIndex.DETAILS_MIN;
      } else {
        details = NexusIndex.DETAILS_DISABLED;
      }
    }

    return details;
  }

  /**
   * Updates index synchronously if  monitor!=null. Schedules index update otherwise.
   * ... and yes, I know this ain't kosher.
   * 
   * Public for unit tests only!
   */
  public void setIndexDetails(IRepository repository, String details, IProgressMonitor monitor) throws CoreException {
    setIndexDetails(repository, details, details, monitor);
  }

  private void setIndexDetails(IRepository repository, String details, String defaultDetails, IProgressMonitor monitor) throws CoreException {
    if (details != null) {
      indexDetails.setProperty(repository.getUid(), details);

      writeIndexDetails();
    } else {
      details = defaultDetails;
    }

    synchronized(getIndexLock(repository)) {
      IndexingContext indexingContext = getIndexingContext(repository);

      try {
        if (NexusIndex.DETAILS_DISABLED.equals(details)) {
          if (indexingContext != null) {
            getIndexer().removeIndexingContext(indexingContext, false /*removeFiles*/);
            fireIndexRemoved(repository);
          }
        } else {
          if(indexingContext != null) {
            getIndexer().removeIndexingContext(indexingContext, false);
          }

          createIndexingContext(repository, details);

          fireIndexAdded(repository);

          
          if (monitor != null) {
            updateIndex(repository, false, monitor);
          } else {
            scheduleIndexUpdate(repository, false);
          }
        }
      } catch(IOException ex) {
        String msg = "Error changing index details " + repository.toString();
        console.logError(msg + "; " + ex.getMessage()); //$NON-NLS-1$
        MavenLogger.log(msg, ex);
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, Messages.NexusIndexManager_error_add_repo, ex));
      }

      if (repository.isScope(IRepositoryRegistry.SCOPE_LOCAL)) {
        // note that we are still synchronized on repository lock at this point
        this.localIndex = newLocalIndex(repositoryRegistry.getLocalRepository());
      }
    }
  }

  protected IndexingContext createIndexingContext(IRepository repository, String details) throws IOException {
    IndexingContext indexingContext;
    Directory directory = getIndexDirectory(repository);

    File repositoryPath = null;
    if (repository.getBasedir() != null) {
      repositoryPath = repository.getBasedir().getCanonicalFile();
    }

    ArrayList<IndexCreator> indexers = getIndexers(details);

    indexingContext = getIndexer().addIndexingContextForced(
        repository.getUid(), //
        repository.getUrl(), //
        repositoryPath, //
        directory, //
        repository.getUrl(),
        null, //
        indexers);
    
    indexingContext.setSearchable(false);
    
    return indexingContext;
  }

  protected ArrayList<IndexCreator> getIndexers(String details) {
    boolean fullIndex = NexusIndex.DETAILS_FULL.equals(details);
    ArrayList<IndexCreator> indexers = fullIndex ? getFullCreator() : getMinCreator();
    return indexers;
  }

  public void repositoryRemoved(IRepository repository, IProgressMonitor monitor) {
    synchronized(getIndexLock(repository)) {
      try {
        IndexingContext context = getIndexingContext(repository);
        if(context == null) {
          return;
        }
        getIndexer().removeIndexingContext(context, false);
      } catch(IOException ie) {
        String msg = "Unable to delete files for index";
        MavenLogger.log(msg, ie);
      }
    }

    fireIndexRemoved(repository);
  }

  public void fireIndexAdded(IRepository repository){
    synchronized(indexListeners){
      for(IndexListener listener : indexListeners){
        listener.indexAdded(repository);
      }
    }
  }
  public void fireIndexRemoved(IRepository repository){
    synchronized(updatingIndexes){
      if(repository != null){
        //since workspace index can be null at startup, guard against nulls
        updatingIndexes.remove(repository.getUid());
      }
    }
    synchronized(indexListeners){
      for(IndexListener listener : indexListeners){
        listener.indexRemoved(repository);
      }
    }
  }

  public boolean isUpdatingIndex(IRepository repository){
    synchronized(updatingIndexes){
      return updatingIndexes.contains(repository.getUid());
    }
  }

  public void fireIndexUpdating(IRepository repository){
    synchronized(updatingIndexes){
      if(repository != null){
        //since workspace index can be null at startup, guard against nulls
        updatingIndexes.add(repository.getUid());
      }
    }    
    synchronized(indexListeners){
      for(IndexListener listener : indexListeners){
        listener.indexUpdating(repository);
      }
    }
  }
  public void fireIndexChanged(IRepository repository){
    synchronized(updatingIndexes){
      if(repository != null){
        //since workspace index can be null at startup, guard against nulls
        updatingIndexes.remove(repository.getUid());
      }
      
    }
    synchronized(indexListeners){
      for(IndexListener listener : indexListeners){
        listener.indexChanged(repository);
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

  public void updateIndex(IRepository repository, boolean force, IProgressMonitor monitor) throws CoreException {
    synchronized(getIndexLock(repository)) {
      if (repository.isScope(IRepositoryRegistry.SCOPE_WORKSPACE)) {
        reindexWorkspace(monitor);
      } else {
        IndexingContext context = getIndexingContext(repository);
        if (context != null) {
          if (context.getRepository() != null) {
            reindexLocalRepository(repository, force, monitor);
          } else {
            if(!force){
              //if 'force' is not set, then only do the remote update if this value is set
              IMavenConfiguration mavenConfig = MavenPlugin.getDefault().getMavenConfiguration();
              if(mavenConfig.isUpdateIndexesOnStartup()){
                updateRemoteIndex(repository, force, monitor);
              }
            } else {
              updateRemoteIndex(repository, force, monitor);
            }
          }
        }
      }
      IndexingContext context = getIndexingContext(repository);
      if(context != null) {
        context.setSearchable(true);
      }
    }
  }

  /*
   * Callers must hold repository access synchronisation lock 
   */
  private void updateRemoteIndex(IRepository repository, boolean force, IProgressMonitor monitor) {
    if (repository == null) {
      return;
    }

    if(monitor != null){
      monitor.setTaskName(NLS.bind(Messages.NexusIndexManager_task_updating, repository.toString()));
    }
    console.logMessage("Updating index " + repository.toString());
    try {
      fireIndexUpdating(repository);

      IndexingContext context = getIndexingContext(repository);

      if (context != null) {
        IndexUpdateRequest request = newIndexUpdateRequest(repository, context);

        ProxyInfo proxyInfo = maven.getProxyInfo(repository.getProtocol());
        AuthenticationInfo authenticationInfo = repository.getAuthenticationInfo();
        request.setProxyInfo(proxyInfo);
        request.setAuthenticationInfo(authenticationInfo);
        request.setForceFullUpdate(force);
        request.setResourceFetcher(new AsyncFetcher(authenticationInfo, proxyInfo, monitor));

        Lock cacheLock = locker.lock(request.getLocalIndexCacheDir());
        try {
          boolean updated;

          request.setCacheOnly(true);
          IndexUpdateResult result = indexUpdater.fetchAndUpdateIndex(request);
          if(result.isFullUpdate() || !context.isSearchable()) {
            // need to fully recreate index

            // 1. process index gz into cached/shared lucene index. this can be a noop if cache is uptodate
            String details = getIndexDetails(repository);
            String id = repository.getUid() + "-cache"; //$NON-NLS-1$
            File luceneCache = new File(request.getLocalIndexCacheDir(), details);
            Directory directory = FSDirectory.getDirectory(luceneCache);
            IndexingContext cacheCtx = getIndexer().addIndexingContextForced(id, id, null, directory, null, null,
                getIndexers(details));
            request=newIndexUpdateRequest(repository, cacheCtx);
            request.setOffline(true);
            indexUpdater.fetchAndUpdateIndex(request);

            // 2. copy cached/shared (lets play dirty here!)
            getIndexer().removeIndexingContext(context, true); // nuke workspace index files
            getIndexer().removeIndexingContext(cacheCtx, false); // keep the cache!
            FileUtils.cleanDirectory(context.getIndexDirectoryFile());
            FileUtils.copyDirectory(luceneCache, context.getIndexDirectoryFile()); // copy cached lucene index
            context=createIndexingContext(repository, details); // re-create indexing context

            updated=true;
          } else {
            // incremental change
            request = newIndexUpdateRequest(repository, context);
            request.setOffline(true); // local cache is already uptodate, no need to
            result = indexUpdater.fetchAndUpdateIndex(request);
            updated=result.getTimestamp()!=null;
          }

          if(updated) {
            console.logMessage("Updated index for " + repository.toString());
          } else {
            console.logMessage("No index update available for " + repository.toString());
          }

        } finally {
          cacheLock.release();
        }
      }
    } catch (FileNotFoundException e) {
      String msg = "Unable to update index for " + repository.toString() + ": " + e.getMessage(); //$NON-NLS-2$
      console.logError(msg);
    } catch (Exception ie){
      String msg = "Unable to update index for " + repository.toString();
      MavenLogger.log(msg, ie);
      console.logError(msg);
    } finally {
      fireIndexChanged(repository);
    }
  }

  protected IndexUpdateRequest newIndexUpdateRequest(IRepository repository, IndexingContext context)
      throws IOException {
    IndexUpdateRequest request = new IndexUpdateRequest(context);
    File localRepo = repositoryRegistry.getLocalRepository().getBasedir();
    File indexCacheBasedir = new File(localRepo, ".cache/m2e/" + MavenPlugin.getVersion()).getCanonicalFile(); //$NON-NLS-1$
    File indexCacheDir = new File(indexCacheBasedir, repository.getUid());
    indexCacheDir.mkdirs();
    request.setLocalIndexCacheDir(indexCacheDir);
    return request;
  }

  public void initialize(IProgressMonitor monitor) throws CoreException {
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
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, Messages.NexusIndexManager_error_read_index, e));
    }
  }

  protected void writeIndexDetails() throws CoreException {
    try {
      File indexDetailsFile = getIndexDetailsFile();
      indexDetailsFile.getParentFile().mkdirs();
      BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(indexDetailsFile));
      try {
        indexDetails.store(os, null);
      } finally {
        os.close();
      }
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, Messages.NexusIndexManager_error_write_index, e));
    }
  }

  private File getIndexDetailsFile() {
    return new File(baseIndexDir, "indexDetails.properties"); //$NON-NLS-1$
  }

  /** for unit tests only */
  public Job getIndexUpdateJob() {
    return updaterJob;
  }
  
  public String getIndexerId() {
    return Messages.NexusIndexManager_78;
  }

  private Object getIndexLock(IRepository repository) {
    if(repository == null) {
      return new Object();
    }
    // NOTE: We ultimately want to prevent concurrent access to the IndexingContext so we sync on the repo UID and not on the repo instance.
    synchronized(indexLocks) {
      Object lock = indexLocks.get(repository.getUid());
      if(lock == null) {
        lock = new Object();
        indexLocks.put(repository.getUid(), lock);
      }
      return lock;
    }
  }

}
