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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.proxy.ProxyInfo;

import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.artifact.GavCalculator;
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
import org.sonatype.nexus.index.updater.IndexUpdater;

import org.maven.ide.eclipse.MavenPlugin;
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
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.internal.index.IndexInfo.Type;
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

  private MavenProjectManager projectManager;

  private ArrayList<IndexCreator> fullCreators = null;

  private ArrayList<IndexCreator> minCreators = null;

  private final MavenConsole console;

  private final File baseIndexDir;

  /**
   * URL->IndexInfo. 
   */
  private Map<String, IndexInfo> configuredIndexes = new HashMap<String, IndexInfo>();

  private IMutableIndex localIndex = new NexusIndex(this, LOCAL_INDEX);
  
  private IMutableIndex workspaceIndex = new NexusIndex(this, WORKSPACE_INDEX);

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
        ce.printStackTrace();
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
        ce.printStackTrace();
      }

    }
    return minCreators;
  }

  private void addIndexingContext(IndexInfo indexInfo) {
    String indexName = indexInfo.getIndexName();
    String displayName = indexInfo.getRepositoryUrl();
    try {
      indexInfo.setNew(!getIndexDirectoryFile(indexInfo).exists());

      Directory directory = getIndexDirectory(indexInfo);
      getIndexer().addIndexingContextForced(indexName, indexName, indexInfo.getRepositoryDir(), directory, //
          indexInfo.getRepositoryUrl(), indexInfo.getIndexUpdateUrl(), //
          (indexInfo.isShort() ? getMinCreator() : getFullCreator()));
//      fireIndexAdded(indexInfo);

    } catch(IOException ex) {
      // XXX how to recover from this?
      String msg = "Error on adding indexing context " + displayName;
      console.logError(msg + "; " + ex.getMessage());
      MavenLogger.log(msg, ex);
    } 
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
      
    } /*catch(IndexContextInInconsistentStateException ex) {
      String msg = "Inconsistent index context state " + ex.getMessage();
      console.logError(msg);
      MavenLogger.log(msg, ex);
    } */catch(IOException ex) {
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

  public Date reindex(String indexName, final IProgressMonitor monitor) throws CoreException {
    try {
      IndexInfo indexInfo = getIndexInfo(indexName);
      IndexingContext context = getIndexer().getIndexingContexts().get(indexName);
      getIndexer().scan(context, new ArtifactScanningMonitor(indexInfo, monitor, console), false);
      Date indexTime = context.getTimestamp();
      indexInfo.setUpdateTime(indexTime);
//      fireIndexUpdated(indexInfo);
      return indexTime;
    } catch(Exception ex) {
      MavenLogger.log("Unable to re-index "+indexName, ex);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Reindexing error", ex));
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
        // TODO log
        return;
      }
      
      ArtifactContext artifactContext = getArtifactContext(null, documentKey, -1, -1, //
          IIndex.NOT_AVAILABLE, IIndex.NOT_AVAILABLE, context.getRepositoryId());
      getIndexer().deleteArtifactFromIndex(artifactContext, context);
//      fireIndexUpdated(getIndexInfo(indexName));
      
    } catch(Exception ex) {
      String msg = "Unable to remove " + documentKey;
      console.logError(msg + "; " + ex.getMessage());
      MavenLogger.log(msg, ex);
    }
  }

  private ArtifactContext getArtifactContext(File file, String documentKey, long size, long date, int sourceExists,
      int javadocExists, String repository) {
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

  
//  public Date fetchAndUpdateIndex(String indexName, boolean force, IProgressMonitor monitor) throws CoreException {
//    IndexingContext context = getIndexingContext(indexName);
//    
//    if(context != null) {
//      try {
//        IndexUpdateRequest request = new IndexUpdateRequest(context);
//        request.setProxyInfo(getProxyInfo());
//        request.setTransferListener(new TransferListenerAdapter(monitor, console, null));
//        request.setForceFullUpdate(force);
//        Date indexTime = getUpdater().fetchAndUpdateIndex(request);
//        if(indexTime!=null) {
//          IndexInfo indexInfo = getIndexInfo(indexName);
//          indexInfo.setUpdateTime(indexTime);
//          fireIndexUpdated(indexInfo);
//          return indexTime;
//        }
//      } catch(Throwable ex) {
//        IndexInfo info = this.getIndexInfo(indexName);
//        if(info == null){
//          //it was probably removed by the user, log the problem and move along.
//          MavenLogger.log("Error updating index, but index is no longer around (probably removed by user).",ex);
//          return null;
//        } 
//        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Error updating index", ex));
//      }
//    }
//    return null;
//  }

//  public Properties fetchIndexProperties(final String repositoryUrl, final String indexUpdateUrl,
//      IProgressMonitor monitor) throws CoreException {
//    IndexingContext context;
//    try {
//      context = new DefaultIndexingContext("unknown", "unknown", null, new RAMDirectory(), repositoryUrl,
//          indexUpdateUrl, null, false);
//    } catch(UnsupportedExistingLuceneIndexException ex) {
//      throw new CoreException(
//          new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Unsupported existing index", ex));
//    } catch(IOException ex) {
//      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
//          "Error creating temporary indexing context", ex));
//    }
//    try {
//      @SuppressWarnings("deprecation")
//      Properties properties = getUpdater().fetchIndexProperties(context, //
//          new TransferListenerAdapter(monitor, console, null), getProxyInfo());
//      return properties;
//    } catch(IOException ex) {
//      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
//          "Error fetching index properties", ex));
//    }
//  }

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
  
//  public Date mergeIndex(String indexName, InputStream is) throws CoreException {
//    Date indexTime = null;
//
//    IndexingContext context = getIndexingContext(indexName);
//    if(context != null) {
//      Directory tempDirectory = new RAMDirectory();
//      
//      try {
//        indexTime = unpackIndexArchive(is, tempDirectory);
//        context.merge(tempDirectory);
//      } catch(IOException ex) {
//        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Error merging index", ex));
//      }
//      
//      // TODO only update time if current index is older then merged
//      IndexInfo indexInfo = getIndexInfo(indexName);
//      indexInfo.setUpdateTime(indexTime);
//      
//      fireIndexUpdated(indexInfo);
//    }
//    
//    return indexTime;
//  }

//  public Date replaceIndex(String indexName, InputStream is) throws CoreException {
//    Date indexTime = null;
//    
//    IndexingContext context = getIndexingContext(indexName);
//    if(context != null) {
//      Directory tempDirectory = new RAMDirectory();
//      
//      try {
//        indexTime = unpackIndexArchive(is, tempDirectory);
//        context.replace(tempDirectory);
//      } catch(IOException ex) {
//        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Error replacing index", ex));
//      }
//      
//      IndexInfo indexInfo = getIndexInfo(indexName);
//      indexInfo.setUpdateTime(indexTime);
//      
//      fireIndexUpdated(indexInfo);
//    }
//    
//    return indexTime;
//  }
  
  public IndexedArtifactGroup[] getRootGroups(String indexId) throws CoreException {
    IndexingContext context = getIndexingContext(indexId);
    if(context != null) {
      try {
        Set<String> rootGroups = context.getRootGroups();
        IndexedArtifactGroup[] groups = new IndexedArtifactGroup[rootGroups.size()];
        int i = 0;
        for(String group : rootGroups) {
          groups[i++] = new IndexedArtifactGroup(getIndexInfo(indexId), group);
        }
        return groups;
      } catch(IOException ex) {
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, //
            "Can't get root groups for " + indexId, ex));
      }
    }
    return new IndexedArtifactGroup[0];
  }

  private IndexingContext getIndexingContext(String indexName) {
    return indexName == null ? null : getIndexer().getIndexingContexts().get(indexName);
  }

  private synchronized NexusIndexer getIndexer() {
    if(indexer == null) {
      indexer = MavenPlugin.lookup(NexusIndexer.class);
    }
    return indexer;
  }

  synchronized void invalidateIndexer() throws CoreException {
    if(indexer != null) {
      for (IndexingContext context : indexer.getIndexingContexts().values()) {
        try {
          indexer.removeIndexingContext(context, false);
        } catch(IOException ex) {
          MavenLogger.log("Could not remove indexing context", ex);
        }
      }
      indexer = null;
    }

//    addIndex(new IndexInfo(IndexManager.LOCAL_INDEX, //
//        new File(maven.getLocalRepository().getBasedir()), null, IndexInfo.Type.LOCAL, false), false);

    // global repositories and mirrors
    LinkedHashSet<String> repositories = new LinkedHashSet<String>();
    repositories.addAll(getRepositoryUrls());

    // project-specific repositories
    for (IMavenProjectFacade project : projectManager.getProjects()) {
      repositories.addAll(project.getArtifactRepositoryUrls());
      repositories.addAll(project.getPluginArtifactRepositoryUrls());
    }

    for (String url : repositories) {
      IndexInfo info = getIndexInfo(url);
      addIndexingContext(info);
    }
  }

  private LinkedHashSet<String> getRepositoryUrls() throws CoreException {
    ArrayList<ArtifactRepository> configured = new ArrayList<ArtifactRepository>();
    configured.addAll(maven.getArtifactRepositories());
    configured.addAll(maven.getPluginArtifactRepository());

    List<ArtifactRepository> effective = maven.getEffectiveRepositories(configured);

    LinkedHashSet<String> urls = new LinkedHashSet<String>();
    for (ArtifactRepository repository : effective) {
      urls.add(repository.getUrl());
    }

    return urls;
  }

  public IndexInfo getIndexInfo(String url) {
    // look for explicitly configured repository indexes first
    // if not found, create&return default repository index configuration

    IndexInfo info = configuredIndexes.get(url);

    if (info == null) {
      info = new IndexInfo(url, getIndexDirectoryFile(url), url, Type.REMOTE, true /*short*/);
    }

    return info;
  }

  private static final class ArtifactScanningMonitor implements ArtifactScanningListener {

    private static final long THRESHOLD = 1 * 1000L;

    private final IndexInfo indexInfo;

    private final IProgressMonitor monitor;

    private final MavenConsole console;
    
    private long timestamp = System.currentTimeMillis();

    ArtifactScanningMonitor(IndexInfo indexInfo, IProgressMonitor monitor, MavenConsole console) {
      this.indexInfo = indexInfo;
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
            this.indexInfo.getRepositoryDir().getAbsolutePath().length());
        this.monitor.setTaskName(id);
        this.timestamp = current;
      }
    }

    public void artifactError(ArtifactContext ac, Exception e) {
      String id = ac.getPom().getAbsolutePath().substring(this.indexInfo.getRepositoryDir().getAbsolutePath().length());
      console.logError(id + " " + e.getMessage());
    }
  }

  static String getDocumentKey(ArtifactKey artifact) {
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

          MavenProject mavenProject = event.getMavenProject().getMavenProject(monitor);

          List<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
          repositories.addAll(mavenProject.getRemoteArtifactRepositories());
          repositories.addAll(mavenProject.getPluginArtifactRepositories());
          repositories = maven.getEffectiveRepositories(repositories);

          for(ArtifactRepository repository : repositories) {
            IndexInfo info = getIndexInfo(repository.getUrl());
            addIndexingContext(info);
          }
        }
      } catch (CoreException e) {
        MavenLogger.log(e);
      }
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
      indexes.add(new NexusIndex(this, repository));
    }

    return new CompositeIndex(indexes);
  }

  public File getIndexDirectoryFile(IndexInfo indexInfo) {
    return getIndexDirectoryFile(indexInfo.getIndexName());
  }

  private File getIndexDirectoryFile(String repositoryUrl) {
    repositoryUrl = repositoryUrl.replace(':', '_').replace('/', '_');
    return new File(getBaseIndexDir(), repositoryUrl);
  }

  protected Directory getIndexDirectory(IndexInfo indexInfo) throws IOException {
    return FSDirectory.getDirectory(getIndexDirectoryFile(indexInfo));
  }

  public File getBaseIndexDir() {
    return baseIndexDir;
  }

  public IndexedArtifactGroup resolveGroup(IndexedArtifactGroup group) {
    IndexInfo info = group.getIndexInfo();
    String indexName = info.getIndexName();
    String prefix = group.getPrefix();

    try {
      IndexedArtifactGroup g = new IndexedArtifactGroup(info, prefix);
      for(IndexedArtifact a : search(indexName, prefix, IIndex.SEARCH_GROUP).values()) {
        String groupId = a.getGroupId();
        if(groupId.equals(prefix)) {
          g.getFiles().put(a.getArtifactId(), a);
        } else if(groupId.startsWith(prefix + ".")) {
          int start = prefix.length() + 1;
          int end = groupId.indexOf('.', start);
          String key = end > -1 ? groupId.substring(0, end) : groupId;
          g.getNodes().put(key, new IndexedArtifactGroup(info, key));
        }
      }
  
      return g;
  
    } catch(CoreException ex) {
      MavenLogger.log("Can't retrieve groups for " + indexName + ":" + prefix, ex);
      return group;
    }
  }

}
