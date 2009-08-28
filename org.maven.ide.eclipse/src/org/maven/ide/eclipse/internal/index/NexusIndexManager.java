/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.index;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.progress.IProgressConstants;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import org.apache.lucene.index.Term;
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
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.RAMDirectory;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.settings.Proxy;
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
import org.sonatype.nexus.index.ArtifactScanningListener;
import org.sonatype.nexus.index.FlatSearchRequest;
import org.sonatype.nexus.index.FlatSearchResponse;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.ScanningResult;
import org.sonatype.nexus.index.context.IndexCreator;
import org.sonatype.nexus.index.context.IndexUtils;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.locator.PomLocator;
import org.sonatype.nexus.index.updater.IndexUpdateRequest;
import org.sonatype.nexus.index.updater.IndexUpdater;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.actions.OpenMavenConsoleAction;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.AbstractMavenConfigurationChangeListener;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.embedder.MavenConfigurationChangeEvent;
import org.maven.ide.eclipse.index.IIndex;
import org.maven.ide.eclipse.index.IMutableIndex;
import org.maven.ide.eclipse.index.IndexListener;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.internal.embedder.TransferListenerAdapter;
import org.maven.ide.eclipse.internal.preferences.MavenPreferenceConstants;
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

  // TODO do we need to redefine index fields here?

  public static final String FIELD_GROUP_ID = ArtifactInfo.GROUP_ID;

  public static final String FIELD_ARTIFACT_ID = ArtifactInfo.ARTIFACT_ID;

  public static final String FIELD_VERSION = ArtifactInfo.VERSION;

  public static final String FIELD_PACKAGING = ArtifactInfo.PACKAGING;

  public static final String FIELD_SHA1 = ArtifactInfo.SHA1;

  public static final String FIELD_NAMES = ArtifactInfo.NAMES;

  private final GavCalculator gavCalculator = new M2GavCalculator();

  private NexusIndexer indexer;

  private IMaven maven;

  private IMavenConfiguration mavenConfiguration;

  MavenProjectManager projectManager;

  private ArrayList<IndexCreator> fullCreators = null;

  private ArrayList<IndexCreator> minCreators = null;

  private final MavenConsole console;

  private final File baseIndexDir;

  private List<IndexListener> indexListeners = new ArrayList<IndexListener>();
//  /**
//   * URL->IndexInfo. 
//   */
//  private Map<String, IndexInfo> configuredIndexes = new HashMap<String, IndexInfo>();

  private IMutableIndex localIndex = new NexusIndex(this, LOCAL_INDEX, LOCAL_INDEX);
  
  private IMutableIndex workspaceIndex = new NexusIndex(this, WORKSPACE_INDEX, WORKSPACE_INDEX);

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
    this.mavenConfiguration = MavenPlugin.lookup(IMavenConfiguration.class);

    // TODO what should trigger index invalidation?
    this.mavenConfiguration.addConfigurationChangeListener(new AbstractMavenConfigurationChangeListener() {
      public void mavenConfigutationChange(MavenConfigurationChangeEvent event) throws CoreException {
        if(MavenConfigurationChangeEvent.P_USER_SETTINGS_FILE.equals(event.getKey()) || MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE.equals(event.getKey())) {
          invalidateIndexer();
        }
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


  public IndexedArtifactFile getIndexedArtifactFile(String indexName, ArtifactKey gav) throws CoreException {

    try {
//      Gav gav = gavCalculator.pathToGav(documentKey);
      
      String key = getGAV(gav.getGroupId(), //
          gav.getArtifactId(), gav.getVersion(), gav.getClassifier());
      
      TermQuery query = new TermQuery(new Term(ArtifactInfo.UINFO, key));
      ArtifactInfo artifactInfo = getIndexer().identify(query, Collections.singleton(getIndexingContext(indexName)));
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

  public void reindex(String indexName, final IProgressMonitor monitor) throws CoreException {
    try {
      fireIndexUpdating(indexName);
      //IndexInfo indexInfo = getIndexInfo(indexName);
      IndexingContext context = getIndexer().getIndexingContexts().get(indexName);
      getIndexer().scan(context, new ArtifactScanningMonitor(context.getRepository(), monitor, console), false);
      fireIndexChanged(indexName);
    } catch(Exception ex) {
      MavenLogger.log("Unable to re-index "+indexName, ex);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Reindexing error", ex));
    } finally {
      fireIndexChanged(indexName);
    }
  }

  public void addDocument(String indexName, File file, String documentKey, long size, long date, File jarFile,
      int sourceExists, int javadocExists) {
    try {
      IndexingContext context = getIndexingContext(indexName);
      if(context == null) {
        // TODO log
        return;
      }

      ArtifactContext artifactContext = getArtifactContext(file, documentKey, size, date, //
          sourceExists, javadocExists, context.getRepositoryId());
      getIndexer().addArtifactToIndex(artifactContext, context);
//      fireIndexUpdated(getIndexInfo(indexName));
      
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
        MavenLogger.log(new Status(Status.ERROR,"org.maven.ide.eclipse", msg));
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

  public ArtifactContext getArtifactContext(String documentKey, String repository) throws IllegalArtifactCoordinateException{
    Gav gav = gavCalculator.pathToGav(documentKey);
    ArtifactInfo ai = new ArtifactInfo(repository, gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), gav.getClassifier());

    File pomFile = null;
    File artifactFile = null;

    return new ArtifactContext(pomFile, artifactFile, null, ai, gav);
   
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

  public Date getIndexArchiveTime(InputStream is) throws IOException {
    ZipInputStream zis = null;
    try
    {
        zis = new ZipInputStream( is );

        long timestamp = -1;

        ZipEntry entry;
        while ( ( entry = zis.getNextEntry() ) != null )
        {
            if ( entry.getName() == IndexUtils.TIMESTAMP_FILE )
            {
                return new Date( new DataInputStream( zis ).readLong() );
            }
            timestamp = entry.getTime();
        }

        return timestamp == -1 ? null : new Date( timestamp );
    }
    finally
    {
        zis.close();
        is.close();
    }
  }
  public static class IndexUpdaterRule implements ISchedulingRule {

    public boolean contains(ISchedulingRule rule) {
      return rule == this;
    }

    public boolean isConflicting(ISchedulingRule rule) {
      return rule == this;
    }
    
  }
  static class IndexUpdaterJob extends Job {

    private final NexusIndexManager indexManager;
    
    private final MavenConsole console;

    private final Stack<IndexCommand> updateQueue = new Stack<IndexCommand>(); 
    
    public IndexUpdaterJob(NexusIndexManager indexManager, MavenConsole console) {
      super("Updating indexes");
      this.indexManager = indexManager;
      this.console = console;
      setProperty(IProgressConstants.ACTION_PROPERTY, new OpenMavenConsoleAction());
      setRule(new IndexUpdaterRule());
    }

    public void addCommand(IndexCommand indexCommand) {
      updateQueue.add(indexCommand);
    }
    
    protected IStatus run(IProgressMonitor monitor) {
      monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
      
      while(!updateQueue.isEmpty()) {
        IndexCommand command = updateQueue.pop();
        command.run(indexManager, console, monitor);
      }
      
      monitor.done();
      
      return Status.OK_STATUS;
    }
    
  }  
  
  private IndexUpdaterJob updaterJob;
  
  public void scheduleIndexUpdate(String indexName, boolean force, long delay) {
    if(indexName!=null) {
      if(updaterJob == null) {
        updaterJob = new IndexUpdaterJob(this, console);
      }
      if(IndexManager.LOCAL_INDEX.equals(indexName)){
        updaterJob.addCommand(new ReindexCommand(indexName));
      }  else if(IndexManager.WORKSPACE_INDEX.equals(indexName)) {
        updaterJob.addCommand(new ReindexWorkspaceCommand());
      } else {
        updaterJob.addCommand(new UpdateCommand(indexName, force));
        URL archiveURL = null;
        updaterJob.addCommand(new UnpackCommand(indexName, archiveURL, force));
      }  
      updaterJob.schedule(delay);
    }
  }
 
  private IndexUpdater getUpdater() {
    return MavenPlugin.lookup(IndexUpdater.class);
  }

  private ProxyInfo getProxyInfo() throws CoreException {
    Settings settings = maven.getSettings();
    Proxy proxy = settings.getActiveProxy();
    ProxyInfo proxyInfo = null;
    if(proxy != null) {
      proxyInfo = new ProxyInfo();
      proxyInfo.setHost(proxy.getHost());
      proxyInfo.setPort(proxy.getPort());
      proxyInfo.setNonProxyHosts(proxy.getNonProxyHosts());
      proxyInfo.setUserName(proxy.getUsername());
      proxyInfo.setPassword(proxy.getPassword());
    }
    return proxyInfo;
  }
  
  private List<ArtifactRepository> getEffectiveRepositories() throws CoreException{
    List<ArtifactRepository> allRepos = new ArrayList<ArtifactRepository>();
    List<ArtifactRepository> artifactRepositories = maven.getArtifactRepositories(new NullProgressMonitor());
    List<ArtifactRepository> pluginArtifactRepository = maven.getPluginArtifactRepository(new NullProgressMonitor());
    allRepos.addAll(artifactRepositories);
    allRepos.addAll(pluginArtifactRepository);
    return maven.getEffectiveRepositories(allRepos);
  }
  
  private AuthenticationInfo getAuthenticationInfo(String indexName) throws CoreException{
    AuthenticationInfo info = new AuthenticationInfo();
    List<ArtifactRepository> effectiveRepositories = getEffectiveRepositories();
    ArtifactRepository activeRepo = null;
    for(ArtifactRepository repo : effectiveRepositories){
      if(repo.getId().equals(indexName)){
        activeRepo = repo;
      }
    }
    if(activeRepo != null){
      Authentication authentication = activeRepo.getAuthentication();
      //a public repo, no auth needed
      if(authentication == null){
        return null;
      }
      info.setUserName(authentication.getUsername());
      info.setPassword(authentication.getPassword());
      return info;
    }
    return null;
  }
  
  public Date unpackIndexArchive(InputStream is, Directory directory) throws IOException{
    ZipInputStream zis = new ZipInputStream( is );
    try
    {
        byte[] buf = new byte[4096];

        ZipEntry entry;

        while ( ( entry = zis.getNextEntry() ) != null )
        {
            if ( entry.isDirectory() || entry.getName().indexOf( '/' ) > -1 )
            {
                continue;
            }

            IndexOutput io = directory.createOutput( entry.getName() );

            try
            {
                int n = 0;

                while ( ( n = zis.read( buf ) ) != -1 )
                {
                    io.writeBytes( buf, n );
                }
            }
            finally
            {
                io.close(  );
            }
        }
    }
    finally
    {
        zis.close(  );
    }
    return IndexUtils.getTimestamp( directory );    
  }
  
  public IndexedArtifactGroup[] getRootGroups(String indexName) throws CoreException {
    IndexingContext context = getIndexingContext(indexName);
    if(context != null) {
      try {
        Set<String> rootGroups = context.getRootGroups();
        IndexedArtifactGroup[] groups = new IndexedArtifactGroup[rootGroups.size()];
        int i = 0;
        for(String group : rootGroups) {
          groups[i++] = new IndexedArtifactGroup(indexName, context.getRepositoryUrl(), group);
        }
        return groups;
      } catch(IOException ex) {
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, //
            "Can't get root groups for " + indexName, ex));
      }
    }
    return new IndexedArtifactGroup[0];
  }

  protected IndexingContext getIndexingContext(String indexName) {
    return indexName == null ? null : getIndexer().getIndexingContexts().get(indexName);
  }

  private synchronized NexusIndexer getIndexer() {
    if(indexer == null) {
      indexer = MavenPlugin.lookup(NexusIndexer.class);
    }
    return indexer;
  }

  synchronized void invalidateIndexer() throws CoreException {
    //TODO: is this invalidate Indexer call even needed still
//    if(indexer != null) {
//      for (IndexingContext context : indexer.getIndexingContexts().values()) {
//        try {
//          indexer.removeIndexingContext(context, false);
//        } catch(IOException ex) {
//          MavenLogger.log("Could not remove indexing context", ex);
//        }
//      }
//      indexer = null;
//    }
//
//    // global repositories and mirrors
//    LinkedHashSet<String> repositories = new LinkedHashSet<String>();
//    repositories.addAll(getRepositoryUrls());
//
//    // project-specific repositories
//    for (IMavenProjectFacade project : projectManager.getProjects()) {
//      repositories.addAll(project.getArtifactRepositoryUrls());
//      repositories.addAll(project.getPluginArtifactRepositoryUrls());
//    }
//
//    for (String url : repositories) {
//      IndexInfo info = getIndexInfo(url);
//      addIndex(info, false);
//    }
  }

  private LinkedHashSet<String> getRepositoryUrls() throws CoreException {

    List<ArtifactRepository> effective = getEffectiveRepositories();

    LinkedHashSet<String> urls = new LinkedHashSet<String>();
    for (ArtifactRepository repository : effective) {
      urls.add(repository.getUrl());
    }

    return urls;
  }


  private static final class ArtifactScanningMonitor implements ArtifactScanningListener {

    private static final long THRESHOLD = 1 * 1000L;

    //private final IndexInfo indexInfo;

    private final IProgressMonitor monitor;

    private final MavenConsole console;
    
    private long timestamp = System.currentTimeMillis();

    private File repositoryDir;

    ArtifactScanningMonitor(File repositoryDir, IProgressMonitor monitor, MavenConsole console) {
      //this.indexInfo = indexInfo;
      this.repositoryDir = repositoryDir;
      this.monitor = monitor;
      this.console = console;
    }

    public void scanningStarted(IndexingContext ctx) {
    }

    public void scanningFinished(IndexingContext ctx, ScanningResult result) {
    }

    public void artifactDiscovered(ArtifactContext ac) {
      long current = System.currentTimeMillis();
      if((current - timestamp) > THRESHOLD) {
        // String id = info.groupId + ":" + info.artifactId + ":" + info.version;
        String id = ac.getPom().getAbsolutePath().substring(
            this.repositoryDir.getAbsolutePath().length());
        this.monitor.setTaskName(id);
        this.timestamp = current;
      }
    }

    public void artifactError(ArtifactContext ac, Exception e) {
      String id = ac.getPom().getAbsolutePath().substring(repositoryDir.getAbsolutePath().length());
      console.logError(id + " " + e.getMessage());
    }
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
    for(MavenProjectChangedEvent event : events) {
      try {
      if (event.getOldMavenProject() != null) {
          File file = event.getOldMavenProject().getMavenProject(monitor).getBasedir();
          String key = getDocumentKey(event.getOldMavenProject().getArtifactKey());
          removeDocument(WORKSPACE_INDEX, file, key);
        }
        if(event.getMavenProject() != null) {
          File file = event.getMavenProject().getMavenProject(monitor).getBasedir();
          String key = getDocumentKey(event.getMavenProject().getArtifactKey());
          addDocument(WORKSPACE_INDEX, file, key, -1, -1, null, IIndex.NOT_PRESENT, IIndex.NOT_PRESENT);

          // TODO project-specific indexes
        }
      } catch (CoreException e) {
        MavenLogger.log(e);
      }
    }
  }

  public void createWorkspaceIndex() {
      IndexInfo workspaceIndex = new IndexInfo(IndexManager.WORKSPACE_INDEX, //
          null, null, IndexInfo.Type.WORKSPACE, false);
      addIndex(workspaceIndex);
  }
  
  public void createLocalIndex(boolean forceUpdate){
    try {
      IndexInfo local = new IndexInfo(IndexManager.LOCAL_INDEX, //
          new File(maven.getLocalRepository().getBasedir()), null, IndexInfo.Type.LOCAL, false);
      addIndex(local);
      scheduleIndexUpdate(local.getIndexName(), forceUpdate, 30);
      
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }
  }
  
  public IMutableIndex getWorkspaceIndex() {
    return workspaceIndex;
  }

  public IMutableIndex getLocalIndex() {
    return localIndex;
  }

  public IIndex getIndex(IProject project) throws CoreException {
    IMavenProjectFacade projectFacade = project != null? projectManager.getProject(project): null;

    LinkedHashSet<String> repositories = new LinkedHashSet<String>();
    if (projectFacade != null) {
      repositories.addAll(projectFacade.getArtifactRepositoryUrls());
      repositories.addAll(projectFacade.getPluginArtifactRepositoryUrls());
    } else {
      repositories.addAll(getRepositoryUrls());
    }

    ArrayList<IIndex> indexes = new ArrayList<IIndex>();
    indexes.add(getWorkspaceIndex());
    indexes.add(getLocalIndex());
    for (String repository : repositories) {
      indexes.add(new NexusIndex(this, repository, repository));
    }

    return new CompositeIndex(indexes);
  }

  public File getIndexDirectoryFile(IndexInfo indexInfo) {
    return getIndexDirectoryFile(indexInfo.getIndexName());
  }

  public File getIndexDirectoryFile(String repositoryUrl) {
    repositoryUrl = repositoryUrl.replace(':', '_').replace('/', '_');
    return new File(getBaseIndexDir(), repositoryUrl);
  }

  protected Directory getIndexDirectory(String repositoryUrl) throws IOException{
    return FSDirectory.getDirectory(getIndexDirectoryFile(repositoryUrl));
  }
  protected Directory getIndexDirectory(IndexInfo indexInfo) throws IOException {
    return FSDirectory.getDirectory(getIndexDirectoryFile(indexInfo));
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

  public void doIndexAdd(String indexName, String url, File repoDir, Directory dir, String indexUpdateUrl, boolean fullIndex) throws IOException{
    //boolean fullPrefs = !MavenPlugin.getDefault().getPreferenceStore().getBoolean(MavenPreferenceConstants.P_FULL_INDEX);
    getIndexer().addIndexingContextForced(indexName, indexName, repoDir, dir, //
        url, indexUpdateUrl, (fullIndex ? getFullCreator() : getMinCreator()));
    fireIndexAdded(indexName); 
  }
  
  public void addIndexForRemote(String indexName, String url) throws IOException{
    Directory dir = getIndexDirectory(indexName);
    doIndexAdd(indexName, url, null, dir, null, true);
  }
  
  public void addIndex(IndexInfo indexInfo){
    String indexName = indexInfo.getIndexName();
    try {
      Directory directory = getIndexDirectory(indexInfo);
      doIndexAdd(indexName, indexInfo.getRepositoryUrl(), indexInfo.getRepositoryDir(), directory, indexInfo.getIndexUpdateUrl(), !indexInfo.isShort());
    } catch(IOException ex) {
      String msg = "Error on adding indexing context " + indexName;
      console.logError(msg + "; " + ex.getMessage());
      MavenLogger.log(msg, ex);
    } 
  }
  
  public void removeIndex(String indexName, boolean deleteFiles){
    try{
      IndexingContext context = getIndexingContext(indexName);
      if(context == null) {
        return;
      }

      getIndexer().removeIndexingContext(context, deleteFiles);
      fireIndexRemoved(indexName);
      
    } catch(IOException ie){
      String msg = "Unable to delete files for index";
      MavenLogger.log(msg, ie);
    }
  }

  public void fireIndexAdded(String indexName){
    synchronized(indexListeners){
      for(IndexListener listener : indexListeners){
        listener.indexAdded(indexName);
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

  public Map<String, IndexingContext> getIndexes(){
    return indexer.getIndexingContexts();
  }
  
  public Date replaceIndex(String indexName, InputStream is) throws CoreException {
    Date indexTime = null;
    
    IndexingContext context = getIndexingContext(indexName);
    if(context != null) {
      Directory tempDirectory = new RAMDirectory();
      
      try {
        indexTime = unpackIndexArchive(is, tempDirectory);
        context.replace(tempDirectory);
      } catch(IOException ex) {
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Error replacing index", ex));
      }
      
      //IndexInfo indexInfo = getIndexInfo(indexName);
      //indexInfo.setUpdateTime(indexTime);
      fireIndexChanged(indexName);
    }
    return indexTime;
  }
  

  
  /**
   * Abstract index command
   */
  abstract static class IndexCommand {
    
    protected String indexName;
    
    abstract void run(NexusIndexManager indexManager, MavenConsole console, IProgressMonitor monitor);
    protected String getIndexName(){
      return this.indexName;
    }
  }

  /**
   * Reindex command
   */
   class ReindexCommand extends IndexCommand {

    ReindexCommand(String indexName) {
      this.indexName = indexName;
    }

    public void run(NexusIndexManager indexManager, MavenConsole console, IProgressMonitor monitor) {
      monitor.setTaskName("Reindexing local repository");
      try {
        indexManager.reindex(getIndexName(), monitor);
        fireIndexChanged(getIndexName());
        console.logMessage("Updated local repository index");
      } catch(CoreException ex) {
        console.logError("Unable to reindex local repository");
      }
    }
  }

  /**
   * Update command
   */
   class UpdateCommand extends IndexCommand {
    private final boolean force;

    UpdateCommand(String indexName, boolean force) {
      this.indexName = indexName;
      this.force = force;
    }

    public void run(NexusIndexManager indexManager, MavenConsole console, IProgressMonitor monitor) {
      String displayName = getIndexName();
      monitor.setTaskName("Updating index " + displayName);
      console.logMessage("Updating index " + displayName);
      try {
        
        fireIndexUpdating(getIndexName());
        IndexingContext context = getIndexingContext(displayName);
        IndexUpdateRequest request = new IndexUpdateRequest(context);
        request.setProxyInfo(getProxyInfo());
        AuthenticationInfo authInfo = getAuthenticationInfo(displayName);
        if(authInfo != null){
          request.setAuthenticationInfo(authInfo);
        }
        request.setTransferListener(new TransferListenerAdapter(monitor, console, null));
        request.setForceFullUpdate(force);
        Date indexTime = getUpdater().fetchAndUpdateIndex(request);
        if(indexTime==null) {
          console.logMessage("No index update available for " + displayName);
        } else {
          console.logMessage("Updated index for " + displayName + " " + indexTime);
        }
      } catch(CoreException ex) {
        String msg = "Unable to update index for " + displayName;
        MavenLogger.log(msg, ex);
        console.logError(msg);
      } catch (OperationCanceledException ex) {
        console.logMessage("Updating index " + displayName + " is canceled");
      } catch(IOException ie){
        String msg = "Unable to update index for " + displayName;
        MavenLogger.log(msg, ie);
        console.logError(msg);
      } catch(Exception e){
      } finally {
        fireIndexChanged(getIndexName());
      }
    }
  }

  static class ReindexWorkspaceCommand extends IndexCommand {
    void run(NexusIndexManager indexManager, MavenConsole console, IProgressMonitor monitor) {
      for (IMavenProjectFacade facade : indexManager.projectManager.getProjects()) {
        indexManager.addDocument(IndexManager.WORKSPACE_INDEX, facade.getPomFile(), //
            getDocumentKey(facade.getArtifactKey()), -1, -1, null, 0, 0);
      }
    }
  }

  /**
   * Unpack command
   */
  static class UnpackCommand extends IndexCommand {

    private final boolean force;
    private URL archiveUrl;
    
    UnpackCommand(String indexName, URL archiveURL, boolean force) {
      this.indexName = indexName;
      this.archiveUrl = archiveURL;
      this.force = force;
    }
    public void run(NexusIndexManager indexManager, MavenConsole console, IProgressMonitor monitor) {
      URL indexArchive = this.archiveUrl;
      if(indexArchive==null) {
        return;
      }

      String indexName = getIndexName();
      String displayName = getIndexName();
      monitor.setTaskName("Unpacking " + displayName);
      
      Date archiveIndexTime = null;

      boolean replace = force;

      if(replace) {
        File index = new File(indexManager.getBaseIndexDir(), indexName);
        if(!index.exists()) {
          if(!index.mkdirs()) {
            MavenLogger.log("Can't create index folder " + index.getAbsolutePath(), null);
          }
        } else {
          File[] files = index.listFiles();
          for(int j = 0; j < files.length; j++ ) {
            if(!files[j].delete()) {
              MavenLogger.log("Can't delete " + files[j].getAbsolutePath(), null);
            }
          }
        }
        
        InputStream is = null;
        try {
          is = indexArchive.openStream();
          indexManager.replaceIndex(indexName, is);

          console.logMessage("Unpacked index for " + displayName + " " + archiveIndexTime);
          
          // XXX update index and repository urls
          // indexManager.removeIndex(indexName, false);
          // indexManager.addIndex(extensionIndexInfo, false);
          
        } catch(Exception ex) {
          MavenLogger.log("Unable to unpack index " + displayName, ex);
        } finally {
          try {
            if(is != null) {
              is.close();
            }
          } catch(IOException ex) {
            MavenLogger.log("Unable to close stream", ex);
          }
        }
      }
    }
    
  }

  
}
