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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.apache.lucene.store.RAMDirectory;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.proxy.ProxyInfo;

import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.artifact.M2GavCalculator;
import org.sonatype.nexus.index.ArtifactAvailablility;
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.ArtifactScanningListener;
import org.sonatype.nexus.index.IndexUtils;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.context.IndexContextInInconsistentStateException;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.context.UnsupportedExistingLuceneIndexException;
import org.sonatype.nexus.index.creator.AbstractIndexCreator;
import org.sonatype.nexus.index.locator.ArtifactLocator;
import org.sonatype.nexus.index.locator.PomLocator;
import org.sonatype.nexus.index.scan.ScanningResult;
import org.sonatype.nexus.index.updater.IndexUpdater;

import org.maven.ide.eclipse.MavenConsole;
import org.maven.ide.eclipse.MavenPlugin;
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

  private final NexusIndexer indexer;

  private final IndexUpdater updater;

  private final MavenEmbedderManager embedderManager;

  public NexusIndexManager(MavenEmbedderManager embedderManager, MavenConsole console, File stateDir)
      throws CoreException {
    super(console, stateDir, "nexus");
    this.embedderManager = embedderManager;

    PlexusContainer plexus = embedderManager.getWorkspaceEmbedder().getPlexusContainer();
    try {
      indexer = (NexusIndexer) plexus.lookup(NexusIndexer.class);
      updater = (IndexUpdater) plexus.lookup(IndexUpdater.class);
    } catch(ComponentLookupException ex) {
      Status status = new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, 0, ex.toString(), ex);
      throw new CoreException(status);
    }
  }

  public void addIndex(IndexInfo indexInfo, boolean reindex) {
    String indexName = indexInfo.getIndexName();
    if(getIndexInfo(indexName) != null) {
      return;
    }
    
    try {
      indexInfo.setNew(!getIndexDirectoryFile(indexInfo).exists());
      
      Directory directory = getIndexDirectory(indexInfo);
      indexer.addIndexingContext(indexName, indexName, indexInfo.getRepositoryDir(), directory, indexInfo
          .getRepositoryUrl(), indexInfo.getIndexUpdateUrl(), //
          (indexInfo.isShort() ? NexusIndexer.MINIMAL_INDEX : NexusIndexer.FULL_INDEX), false);
      addIndex(indexName, indexInfo);
      fireIndexAdded(indexInfo);

    } catch(IOException ex) {
      // XXX how to recover from this?
      String msg = "Error on adding indexing context " + indexName;
      console.logError(msg + "; " + ex.getMessage());
      MavenPlugin.log(msg, ex);

    } catch(UnsupportedExistingLuceneIndexException ex) {
      // XXX how to recover from this?
      String msg = "Unsupported existing index " + indexName;
      console.logError(msg + "; " + ex.getMessage());
      MavenPlugin.log(msg, ex);

    }
  }

  public void removeIndex(String indexName, boolean delete) {
    IndexingContext context = getIndexingContext(indexName);
    if(context != null) {
      try {
        indexer.removeIndexingContext(context, delete);
        removeIndex(indexName);
        fireIndexRemoved(getIndexInfo(indexName));
      } catch(IOException ex) {
        String msg = "Error on removing indexing context " + indexName;
        console.logError(msg + "; " + ex.getMessage());
        MavenPlugin.log(msg, ex);
      }
    }
  }

  public IndexedArtifactFile getIndexedArtifactFile(String indexName, String documentKey) throws IOException {
    Gav gav = M2GavCalculator.calculate(documentKey);
    
    String key = AbstractIndexCreator.getGAV(gav.getGroupId(), //
        gav.getArtifactId(), gav.getVersion(), gav.getClassifier());
    
    TermQuery query = new TermQuery(new Term(ArtifactInfo.UINFO, key));
    try {
      ArtifactInfo artifactInfo = indexer.identify(query, Collections.singleton(getIndexingContext(indexName)));
      if(artifactInfo != null) {
        return getIndexedArtifactFile(artifactInfo);
      }
    } catch(IndexContextInInconsistentStateException ex) {
      String msg = "Inconsistent index context state " + ex.getMessage();
      console.logError(msg);
      MavenPlugin.log(msg, ex);
    }
    return null;
  }
  
  public Query createQuery(String field, String expression) {
    return indexer.constructQuery(field, expression);
  }

  public Map search(String term, String type) throws IOException {
    return search(null, term, type);
  }

  /**
   * @return Map<String, IndexedArtifact>
   */
  public Map search(String indexName, String term, String type) throws IOException {
    Query query;
    if(IndexManager.SEARCH_CLASS_NAME.equals(type)) {
      query = new WildcardQuery(new Term(ArtifactInfo.NAMES, term + "*"));

    } else if(IndexManager.SEARCH_GROUP.equals(type)) {
      query = new PrefixQuery(new Term(ArtifactInfo.GROUP_ID, term));

    } else if(IndexManager.SEARCH_ARTIFACT.equals(type)) {
      BooleanQuery bq = new BooleanQuery();
      
      
      bq.add(indexer.constructQuery(ArtifactInfo.GROUP_ID, term), Occur.SHOULD);
      bq.add(indexer.constructQuery(ArtifactInfo.ARTIFACT_ID, term), Occur.SHOULD);
      bq.add(new PrefixQuery(new Term(ArtifactInfo.MD5, term)), Occur.SHOULD);
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

    } else if(IndexManager.SEARCH_MD5.equals(type)) {
      query = new WildcardQuery(new Term(ArtifactInfo.MD5, term + "*"));

    } else if(IndexManager.SEARCH_SHA1.equals(type)) {
      query = new WildcardQuery(new Term(ArtifactInfo.SHA1, term + "*"));
      
    } else {
      return Collections.EMPTY_MAP;

    }

    Map result = new TreeMap();

    try {
      IndexingContext context = getIndexingContext(indexName);
      Collection artifacts;
      if(context == null) {
        artifacts = indexer.searchFlat(ArtifactInfo.VERSION_COMPARATOR, query);
      } else {
        artifacts = indexer.searchFlat(ArtifactInfo.VERSION_COMPARATOR, query, context);
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

      for(Iterator it = artifacts.iterator(); it.hasNext();) {
        ArtifactInfo artifactInfo = (ArtifactInfo) it.next();
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
      MavenPlugin.log(msg, ex);
    }

    return result;
  }
  
  /**
   * @return Map<String, IndexedArtifact>
   */
  public Map search(String indexName, Query query) throws IOException {
    Map result = new TreeMap();
    try {
      IndexingContext context = getIndexingContext(indexName);
      Collection artifacts;
      if(context == null) {
        artifacts = indexer.searchFlat(ArtifactInfo.VERSION_COMPARATOR, query);
      } else {
        artifacts = indexer.searchFlat(ArtifactInfo.VERSION_COMPARATOR, query, context);
      }
      
      for(Iterator it = artifacts.iterator(); it.hasNext();) {
        ArtifactInfo artifactInfo = (ArtifactInfo) it.next();
        IndexedArtifactFile af = getIndexedArtifactFile(artifactInfo);
        addArtifactFile(result, af, null, null, artifactInfo.packaging);
      }
      
    } catch(IndexContextInInconsistentStateException ex) {
      String msg = "Inconsistent index context state " + ex.getMessage();
      console.logError(msg);
      MavenPlugin.log(msg, ex);
    }
    return result;
  }  

  private void addArtifactFile(Map result, IndexedArtifactFile af, String className, String packageName,
      String packaging) {
    String key = className + " : " + packageName + " : " + af.group + " : " + af.artifact;
    IndexedArtifact indexedArtifact = (IndexedArtifact) result.get(key);
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
    String fname = artifactInfo.fname;
    if(fname == null) {
      fname = artifactId + '-' + version + (classifier != null ? '-' + classifier : "") + ".jar";
    }

    long size = artifactInfo.size;
    Date date = new Date(artifactInfo.lastModified);

    int sourcesExists = artifactInfo.sourcesExists.ordinal();
    int javadocExists = artifactInfo.javadocExists.ordinal();

    String prefix = artifactInfo.prefix;
    List goals = artifactInfo.goals;
    
    return new IndexedArtifactFile(repository, groupId, artifactId, version, classifier, fname, size, date,
        sourcesExists, javadocExists, prefix, goals);
  }

  public Date reindex(String indexName, final IProgressMonitor monitor) throws IOException {
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

    indexer.scan(context, new ArtifactScanningMonitor(indexInfo, monitor, console));
    
    Date indexTime = context.getTimestamp();
    indexInfo.setUpdateTime(indexTime);
    
    fireIndexUpdated(indexInfo);
    
    return indexTime;
  }

  public void addDocument(String indexName, File file, String documentKey, long size, long date, File jarFile,
      int sourceExists, int javadocExists) {
    IndexingContext context = getIndexingContext(indexName);
    if(context == null) {
      // TODO log
      return;
    }

    try {
      ArtifactContext artifactContext = getArtifactContext(file, documentKey, size, date, //
          sourceExists, javadocExists, context.getRepositoryId());
      indexer.addArtifactToIndex(artifactContext, context);
      fireIndexUpdated(getIndexInfo(indexName));
    } catch(Exception ex) {
      String msg = "Unable to add " + documentKey;
      console.logError(msg + "; " + ex.getMessage());
      MavenPlugin.log(msg, ex);
    }
  }

  public void removeDocument(String indexName, File file, String documentKey) {
    IndexingContext context = getIndexingContext(indexName);
    if(context == null) {
      // TODO log
      return;
    }

    try {
      ArtifactContext artifactContext = getArtifactContext(null, documentKey, -1, -1, //
          IndexManager.NOT_AVAILABLE, IndexManager.NOT_AVAILABLE, context.getRepositoryId());
      indexer.deleteArtifactFromIndex(artifactContext, context);
      fireIndexUpdated(getIndexInfo(indexName));
    } catch(Exception ex) {
      String msg = "Unable to remove " + documentKey;
      console.logError(msg + "; " + ex.getMessage());
      MavenPlugin.log(msg, ex);
    }
  }

  private ArtifactContext getArtifactContext(File file, String documentKey, long size, long date, int sourceExists,
      int javadocExists, String repository) {
    Gav gav = M2GavCalculator.calculate(documentKey);

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
      artifactFile = new ArtifactLocator().locate( file, gav );
    
    } else {
      pomFile = new PomLocator().locate( file, gav );
      artifactFile = file;
    }

    return new ArtifactContext(pomFile, artifactFile, null, ai);
  }

  public Date getIndexArchiveTime(InputStream is) throws IOException {
    return IndexUtils.getIndexArchiveTime(is);
  }

  
  public Date fetchAndUpdateIndex(String indexName, boolean force, final IProgressMonitor monitor) throws IOException {
    IndexingContext context = getIndexingContext(indexName);
    if(context != null) {
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

      try {
        Date indexTime = updater.fetchAndUpdateIndex(context, new TransferListenerAdapter(monitor, console, null), proxyInfo);
        if(indexTime!=null) {
          IndexInfo indexInfo = getIndexInfo(indexName);
          indexInfo.setUpdateTime(indexTime);
          fireIndexUpdated(indexInfo);
          return indexTime;
        }
      } catch(UnsupportedExistingLuceneIndexException ex) {
        console.logError("Unsupported index format; " + ex.getMessage());
      }
    }
    return null;
  }

  public Date mergeIndex(String indexName, InputStream is) throws IOException {
    Date indexTime = null;

    IndexingContext context = getIndexingContext(indexName);
    if(context != null) {
      Directory tempDirectory = new RAMDirectory();
      indexTime = IndexUtils.unpackIndexArchive(is, tempDirectory);
      
      context.merge(tempDirectory);
      
      // TODO only update time if current index is older then merged
      IndexInfo indexInfo = getIndexInfo(indexName);
      indexInfo.setUpdateTime(indexTime);
      
      fireIndexUpdated(indexInfo);
    }
    
    return indexTime;
  }

  public Date replaceIndex(String indexName, InputStream is) throws IOException {
    Date indexTime = null;
    
    IndexingContext context = getIndexingContext(indexName);
    if(context != null) {
      Directory tempDirectory = new RAMDirectory();
      indexTime = IndexUtils.unpackIndexArchive(is, tempDirectory);

      context.replace(tempDirectory);
      
      IndexInfo indexInfo = getIndexInfo(indexName);
      indexInfo.setUpdateTime(indexTime);
      
      fireIndexUpdated(indexInfo);
    }
    
    return indexTime;
  }

  public IndexedArtifactGroup[] getGroups(String indexId) throws IOException {
    IndexingContext context = getIndexingContext(indexId);
    if(context != null) {
      Set allGroups = indexer.getAllGroups(context);
      IndexedArtifactGroup[] groups = new IndexedArtifactGroup[allGroups.size()];
      int i = 0;
      for(Iterator it = allGroups.iterator(); it.hasNext();) {
        groups[i++ ] = new IndexedArtifactGroup(getIndexInfo(indexId), ((String) it.next()));
      }
      return groups;
    }
    return new IndexedArtifactGroup[0];
  }
  
  public IndexedArtifactGroup[] getRootGroups(String indexId) throws IOException {
    IndexingContext context = getIndexingContext(indexId);
    if(context != null) {
      Set rootGroups = indexer.getRootGroups(context);
      IndexedArtifactGroup[] groups = new IndexedArtifactGroup[rootGroups.size()];
      int i = 0;
      for(Iterator it = rootGroups.iterator(); it.hasNext();) {
        groups[i++ ] = new IndexedArtifactGroup(getIndexInfo(indexId), ((String) it.next()));
      }
      return groups;
    }
    return new IndexedArtifactGroup[0];
  }

  // 

  private IndexingContext getIndexingContext(String indexName) {
    return (IndexingContext) indexer.getIndexingContexts().get(indexName);
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
