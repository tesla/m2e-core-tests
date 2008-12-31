/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.index;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

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
import org.apache.lucene.store.RAMDirectory;

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
import org.sonatype.nexus.index.IndexUtils;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.context.DefaultIndexingContext;
import org.sonatype.nexus.index.context.IndexContextInInconsistentStateException;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.context.UnsupportedExistingLuceneIndexException;
import org.sonatype.nexus.index.creator.AbstractIndexCreator;
import org.sonatype.nexus.index.locator.PomLocator;
import org.sonatype.nexus.index.scan.ScanningResult;
import org.sonatype.nexus.index.updater.IndexUpdater;

import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.AbstractMavenEmbedderListener;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.index.IndexInfo;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.index.IndexedArtifactGroup;
import org.maven.ide.eclipse.internal.embedder.TransferListenerAdapter;


/**
 * @author Eugene Kuleshov
 */
public class NexusIndexManager extends IndexManager {

  private final GavCalculator gavCalculator = new M2GavCalculator();
  
  private final MavenEmbedderManager embedderManager;
  
  private NexusIndexer indexer;

  private volatile boolean creatingIndexer = false;
  
  public NexusIndexManager(MavenEmbedderManager embedderManager, MavenConsole console, File stateDir) {
    super(console, stateDir, "nexus");
    
    this.embedderManager = embedderManager;
    this.embedderManager.addListener(new AbstractMavenEmbedderListener() {
      public void workspaceEmbedderCreated() {
        invalidateIndexer();
      }
    });
  }

  public void addIndex(IndexInfo indexInfo, boolean reindex) {
    String indexName = indexInfo.getIndexName();
    if(getIndexInfo(indexName) != null) {
      return;
    }
    
    addIndex(indexName, indexInfo);
    addIndexingContext(indexInfo);
  }

  private void addIndexingContext(IndexInfo indexInfo) {
    String indexName = indexInfo.getIndexName();
    try {
      indexInfo.setNew(!getIndexDirectoryFile(indexInfo).exists());
      
      Directory directory = getIndexDirectory(indexInfo);
      getIndexer().addIndexingContext(indexName, indexName, indexInfo.getRepositoryDir(), directory, //
          indexInfo.getRepositoryUrl(), indexInfo.getIndexUpdateUrl(), //
          (indexInfo.isShort() ? NexusIndexer.MINIMAL_INDEX : NexusIndexer.FULL_INDEX));
      fireIndexAdded(indexInfo);

    } catch(IOException ex) {
      // XXX how to recover from this?
      String msg = "Error on adding indexing context " + indexName;
      console.logError(msg + "; " + ex.getMessage());
      MavenLogger.log(msg, ex);

    } catch(UnsupportedExistingLuceneIndexException ex) {
      // XXX how to recover from this?
      String msg = "Unsupported existing index " + indexName;
      console.logError(msg + "; " + ex.getMessage());
      MavenLogger.log(msg, ex);

    } catch(CoreException ex) {
      // XXX how to recover from this?
      MavenLogger.log(ex);
      
    }
  }

  public void removeIndex(String indexName, boolean delete) {
    removeIndex(indexName);
    removeIndexingContext(indexName, delete);
  }

  private void removeIndexingContext(String indexName, boolean delete) {
    if(indexer != null) {
      try {
        IndexingContext context = indexer.getIndexingContexts().get(indexName);
        if(context != null) {
          indexer.removeIndexingContext(context, delete);
          fireIndexRemoved(getIndexInfo(indexName));
        }
      } catch(IOException ex) {
        String msg = "Error on removing indexing context " + indexName;
        console.logError(msg + "; " + ex.getMessage());
        MavenLogger.log(msg, ex);
      }
    }
  }

  public IndexedArtifactFile getIndexedArtifactFile(String indexName, String documentKey) throws CoreException {
    Gav gav = gavCalculator.pathToGav(documentKey);
    
    String key = AbstractIndexCreator.getGAV(gav.getGroupId(), //
        gav.getArtifactId(), gav.getVersion(), gav.getClassifier());
    
    TermQuery query = new TermQuery(new Term(ArtifactInfo.UINFO, key));
    try {
      ArtifactInfo artifactInfo = getIndexer().identify(query, Collections.singleton(getIndexingContext(indexName)));
      if(artifactInfo != null) {
        return getIndexedArtifactFile(artifactInfo);
      }
    } catch(IndexContextInInconsistentStateException ex) {
      String msg = "Inconsistent index context state " + ex.getMessage();
      console.logError(msg);
      MavenLogger.log(msg, ex);
    } catch(IOException ex) {
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
    } catch(IndexContextInInconsistentStateException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Search error", ex));
    }
  }
  
  public Query createQuery(String field, String expression) throws CoreException {
    return getIndexer().constructQuery(field, expression);
  }

  public Map<String, IndexedArtifact> search(String term, String type) throws CoreException {
    return search(null, term, type);
  }

  /**
   * @return Map<String, IndexedArtifact>
   */
  public Map<String, IndexedArtifact> search(String indexName, String term, String type) throws CoreException {
    Query query;
    if(IndexManager.SEARCH_CLASS_NAME.equals(type)) {
      query = new WildcardQuery(new Term(ArtifactInfo.NAMES, term + "*"));

    } else if(IndexManager.SEARCH_GROUP.equals(type)) {
      query = new PrefixQuery(new Term(ArtifactInfo.GROUP_ID, term));

    } else if(IndexManager.SEARCH_ARTIFACT.equals(type)) {
      BooleanQuery bq = new BooleanQuery();
      
      
      bq.add(getIndexer().constructQuery(ArtifactInfo.GROUP_ID, term), Occur.SHOULD);
      bq.add(getIndexer().constructQuery(ArtifactInfo.ARTIFACT_ID, term), Occur.SHOULD);
      bq.add(new PrefixQuery(new Term(ArtifactInfo.SHA1, term)), Occur.SHOULD);
      query = bq;

    } else if(IndexManager.SEARCH_PLUGIN.equals(type)) {
      if("*".equals(term)) {
        query = new TermQuery(new Term(ArtifactInfo.PACKAGING, "maven-plugin"));
      } else {
        BooleanQuery bq = new BooleanQuery();
        bq.add(new WildcardQuery(new Term(ArtifactInfo.GROUP_ID, term + "*")), Occur.SHOULD);
        bq.add(new WildcardQuery(new Term(ArtifactInfo.ARTIFACT_ID, term + "*")), Occur.SHOULD);
        TermQuery tq = new TermQuery(new Term(ArtifactInfo.PACKAGING, "maven-plugin"));
        query = new FilteredQuery(tq, new QueryWrapperFilter(bq));
      }
      
    } else if(IndexManager.SEARCH_ARCHETYPE.equals(type)) {
      BooleanQuery bq = new BooleanQuery();
      bq.add(new WildcardQuery(new Term(ArtifactInfo.GROUP_ID, term + "*")), Occur.SHOULD);
      bq.add(new WildcardQuery(new Term(ArtifactInfo.ARTIFACT_ID, term + "*")), Occur.SHOULD);
      TermQuery tq = new TermQuery(new Term(ArtifactInfo.PACKAGING, "maven-archetype"));
      query = new FilteredQuery(tq, new QueryWrapperFilter(bq));
      
    } else if(IndexManager.SEARCH_PACKAGING.equals(type)) {
      query = new TermQuery(new Term(ArtifactInfo.PACKAGING, term));

    } else if(IndexManager.SEARCH_SHA1.equals(type)) {
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

//      IndexingContext context = (IndexingContext) indexer.getIndexingContexts().get("local");
//      IndexReader reader = context.getIndexSearcher().getIndexReader();
//      for(int i = 0; i < reader.numDocs(); i++ ) {
//        Document doc = reader.document(i);
//        String uinfo = doc.get(ArtifactInfo.UINFO);
//        if(uinfo!=null && uinfo.startsWith("org.apache.maven")) {
//          System.err.println(i + " " + uinfo);
//        }
//      }

      String regex = "^(.*?" + term.replaceAll("\\*", ".+?") + ".*?)$";
      Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

      for(ArtifactInfo artifactInfo : response.getResults()) {
        IndexedArtifactFile af = getIndexedArtifactFile(artifactInfo);

        if(!IndexManager.SEARCH_CLASS_NAME.equals(type) || term.length() < IndexManager.MIN_CLASS_QUERY_LENGTH) {
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

    } catch(IndexContextInInconsistentStateException ex) {
      String msg = "Inconsistent index context state " + ex.getMessage();
      console.logError(msg);
      MavenLogger.log(msg, ex);
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Search error", ex));
    }

    return result;
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
      
    } catch(IndexContextInInconsistentStateException ex) {
      String msg = "Inconsistent index context state " + ex.getMessage();
      console.logError(msg);
      MavenLogger.log(msg, ex);
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
      fname = artifactId + '-' + version + (classifier != null ? '-' + classifier : "") + '.' + packaging;
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
    final IndexInfo indexInfo = getIndexInfo(indexName);
    if(indexInfo == null) {
      // TODO log this
      return null;
    }

    removeIndex(indexName, true);

    addIndex(indexInfo, false);

    IndexingContext context = getIndexingContext(indexName);
    if(context == null) {
      // TODO log this
      return null;
    }

    try {
      getIndexer().scan(context, new ArtifactScanningMonitor(indexInfo, monitor, console), false);
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Reindexing error", ex));
    }
    
    Date indexTime = context.getTimestamp();
    indexInfo.setUpdateTime(indexTime);
    
    fireIndexUpdated(indexInfo);
    
    return indexTime;
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
      fireIndexUpdated(getIndexInfo(indexName));
      
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
          IndexManager.NOT_AVAILABLE, IndexManager.NOT_AVAILABLE, context.getRepositoryId());
      getIndexer().deleteArtifactFromIndex(artifactContext, context);
      fireIndexUpdated(getIndexInfo(indexName));
      
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
    return IndexUtils.getIndexArchiveTime(is);
  }

  
  public Date fetchAndUpdateIndex(String indexName, boolean force, IProgressMonitor monitor) throws CoreException {
    IndexingContext context = getIndexingContext(indexName);
    if(context != null) {
      try {
        @SuppressWarnings("deprecation")
        Date indexTime = getUpdater().fetchAndUpdateIndex(context, //
            new TransferListenerAdapter(monitor, console, null), getProxyInfo());
        if(indexTime!=null) {
          IndexInfo indexInfo = getIndexInfo(indexName);
          indexInfo.setUpdateTime(indexTime);
          fireIndexUpdated(indexInfo);
          return indexTime;
        }
      } catch(UnsupportedExistingLuceneIndexException ex) {
        console.logError("Unsupported index format; " + ex.getMessage());
      } catch(IOException ex) {
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Error updateing index", ex));
      }
    }
    return null;
  }

  public Properties fetchIndexProperties(final String repositoryUrl, final String indexUpdateUrl,
      IProgressMonitor monitor) throws CoreException {
    IndexingContext context;
    try {
      context = new DefaultIndexingContext("unknown", "unknown", null, new RAMDirectory(), repositoryUrl,
          indexUpdateUrl, null, false);
    } catch(UnsupportedExistingLuceneIndexException ex) {
      throw new CoreException(
          new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Unsupported existing index", ex));
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          "Error creating temporary indexing context", ex));
    }
    try {
      @SuppressWarnings("deprecation")
      Properties properties = getUpdater().fetchIndexProperties(context, //
          new TransferListenerAdapter(monitor, console, null), getProxyInfo());
      return properties;
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          "Error fetching index properties", ex));
    }
  }

  private IndexUpdater getUpdater() throws CoreException {
    return embedderManager.getComponent(IndexUpdater.class, null);
  }

  private ProxyInfo getProxyInfo() throws CoreException {
    Settings settings = embedderManager.getWorkspaceEmbedder().getSettings();
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
  
  
  public Date mergeIndex(String indexName, InputStream is) throws CoreException {
    Date indexTime = null;

    IndexingContext context = getIndexingContext(indexName);
    if(context != null) {
      Directory tempDirectory = new RAMDirectory();
      
      try {
        indexTime = IndexUtils.unpackIndexArchive(is, tempDirectory);
        context.merge(tempDirectory);
      } catch(IOException ex) {
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Error merging index", ex));
      }
      
      // TODO only update time if current index is older then merged
      IndexInfo indexInfo = getIndexInfo(indexName);
      indexInfo.setUpdateTime(indexTime);
      
      fireIndexUpdated(indexInfo);
    }
    
    return indexTime;
  }

  public Date replaceIndex(String indexName, InputStream is) throws CoreException {
    Date indexTime = null;
    
    IndexingContext context = getIndexingContext(indexName);
    if(context != null) {
      Directory tempDirectory = new RAMDirectory();
      
      try {
        indexTime = IndexUtils.unpackIndexArchive(is, tempDirectory);
        context.replace(tempDirectory);
      } catch(IOException ex) {
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Error replacing index", ex));
      }
      
      IndexInfo indexInfo = getIndexInfo(indexName);
      indexInfo.setUpdateTime(indexTime);
      
      fireIndexUpdated(indexInfo);
    }
    
    return indexTime;
  }

  public IndexedArtifactGroup[] getGroups(String indexId) throws CoreException {
    IndexingContext context = getIndexingContext(indexId);
    if(context != null) {
      try {
        Set<String> allGroups = getIndexer().getAllGroups(context);
        IndexedArtifactGroup[] groups = new IndexedArtifactGroup[allGroups.size()];
        int i = 0;
        for(String group : allGroups) {
          groups[i++] = new IndexedArtifactGroup(getIndexInfo(indexId), group);
        }
        return groups;
      } catch(IOException ex) {
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, //
            "Can't get groups for " + indexId, ex));
      }
    }
    return new IndexedArtifactGroup[0];
  }
  
  public IndexedArtifactGroup[] getRootGroups(String indexId) throws CoreException {
    IndexingContext context = getIndexingContext(indexId);
    if(context != null) {
      try {
        Set<String> rootGroups = getIndexer().getRootGroups(context);
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

  // 

  private IndexingContext getIndexingContext(String indexName) throws CoreException {
    return indexName == null ? null : getIndexer().getIndexingContexts().get(indexName);
  }

  private synchronized NexusIndexer getIndexer() throws CoreException {
    if(indexer == null) {
      try {
        creatingIndexer = true; 
        indexer = embedderManager.getComponent(NexusIndexer.class, null);
      } finally {
        creatingIndexer = false; 
      }
    }
    return indexer;
  }
  
  synchronized void invalidateIndexer() {
    if(creatingIndexer) {
      return;
    }
    
    if(indexer != null) {
      RAMDirectory directory = new RAMDirectory();
      try {
        Directory.copy(workspaceIndexDirectory, directory, false);
      } catch(IOException ex) {
        MavenLogger.log("Error copying workspace index", ex);
      }
      
      removeIndex(IndexManager.LOCAL_INDEX);
      
      for(IndexInfo info : getIndexes().values()) {
        removeIndexingContext(info.getIndexName(), false);
      }
      indexer = null;
      
      workspaceIndexDirectory = directory;
    }

    try {
      addIndex(new IndexInfo(IndexManager.LOCAL_INDEX, //
          embedderManager.getLocalRepositoryDir(), null, IndexInfo.Type.LOCAL, false), false);
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }
    
    for(IndexInfo info : getIndexes().values()) {
      addIndexingContext(info);
    }
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

}
