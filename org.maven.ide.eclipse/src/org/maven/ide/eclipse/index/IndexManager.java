/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.index;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;

import org.sonatype.nexus.index.ArtifactInfo;

import org.maven.ide.eclipse.MavenConsole;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.index.IndexInfo.Type;
import org.maven.ide.eclipse.internal.index.IndexUpdaterJob;


/**
 * @author Eugene Kuleshov
 */
public abstract class IndexManager {

  public static final int MIN_CLASS_QUERY_LENGTH = 6;

  // index ids
  
  public static final String LOCAL_INDEX = "local";
  
  public static final String WORKSPACE_INDEX = "workspace";
  
  // search keys 
  
  public static final String SEARCH_GROUP = "groupId";

  public static final String SEARCH_ARTIFACT = "artifact";

  public static final String SEARCH_PLUGIN = "plugin";

  public static final String SEARCH_ARCHETYPE = "archetype";
  
  public static final String SEARCH_CLASS_NAME = "className";

  public static final String SEARCH_PACKAGING = "packaging";

  public static final String SEARCH_MD5 = "md5";
  
  public static final String SEARCH_SHA1 = "sha1";

  // search terms

  public static final String FIELD_GROUP_ID = ArtifactInfo.GROUP_ID;

  public static final String FIELD_ARTIFACT_ID = ArtifactInfo.ARTIFACT_ID;

  public static final String FIELD_VERSION = ArtifactInfo.VERSION;

  public static final String FIELD_PACKAGING = ArtifactInfo.PACKAGING;

  // availability flags
  
  public static int PRESENT = 1;
  
  public static int NOT_PRESENT = 0;
  
  public static int NOT_AVAILABLE = 2;
  
  // local state
  
  protected final Directory workspaceIndexDirectory = new RAMDirectory();
  
  private final ArrayList listeners = new ArrayList();
  
  protected final MavenConsole console;

  protected final File baseIndexDir;

  private final Map indexes = new LinkedHashMap();

  private IndexUpdaterJob updaterJob;

  public IndexManager(MavenConsole console, File stateDir, String baseName) {
    this.console = console;
    this.baseIndexDir = new File(stateDir, baseName);
  }

  public File getBaseIndexDir() {
    return baseIndexDir;
  }

  /**
   * @return map of <code>String</code> to <code>IndexInfo</code>
   */
  public Map getIndexes() {
    synchronized(indexes) {
      return Collections.unmodifiableMap(new HashMap(indexes));
    }
  }
  
  protected void addIndex(String indexName, IndexInfo indexInfo) {
    synchronized(indexes) {
      indexes.put(indexName, indexInfo);
    }
  }
  
  protected void removeIndex(String indexName) {
    synchronized(indexes) {
      indexes.remove(indexName);
    }
  }

  public IndexInfo getIndexInfo(String indexName) {
    synchronized(indexes) {
      return (IndexInfo) indexes.get(indexName);
    }
  }

  protected Directory getIndexDirectory(String indexName) throws IOException {
    IndexInfo indexInfo = getIndexInfo(indexName);
    if(indexInfo == null) {
      return null;
    }
    return getIndexDirectory(indexInfo);
  }

  protected Directory getIndexDirectory(IndexInfo indexInfo) throws IOException {
    if(indexInfo.getType() == IndexInfo.Type.WORKSPACE) {
      return workspaceIndexDirectory;
    }
    return FSDirectory.getDirectory(getIndexDirectoryFile(indexInfo));
  }

  public File getIndexDirectoryFile(IndexInfo indexInfo) {
    return new File(getBaseIndexDir(), indexInfo.getIndexName());
  }

  public void scheduleIndexUpdate(String indexName, boolean force, long delay) {
    IndexInfo indexInfo = getIndexInfo(indexName);
    if(indexInfo!=null) {
      if(updaterJob == null) {
        updaterJob = new IndexUpdaterJob(this, console);
      }
      updaterJob.scheduleUpdate(indexInfo, force, delay);
    }
  }

  public abstract void addIndex(IndexInfo indexInfo, boolean reindex);

  public abstract void removeIndex(String indexName, boolean delete);

  public abstract void addDocument(String indexName, File pomFile, String documentKey, //
      long size, long date, File jarFile, int sourceExists, int javadocExists);

  public abstract void removeDocument(String indexName, File pomFile, String documentKey);

  /**
   * @param term - search term
   * @param searchType - query type. Should be one of the SEARCH_* values.
   * 
   * @return Map&lt;String, IndexedArtifact&gt;
   */
  public abstract Map search(String term, String searchType) throws IOException;

  /**
   * @return Map&lt;String, IndexedArtifact&gt;
   */
  public abstract Map search(String indexName, String prefix, String searchGroup) throws IOException;

  /**
   * @param indexName name of the index to search or null to search in all indexes
   * @param query Lucene query that could use combinations of fields 
   *    {@link IndexManager#FIELD_GROUP_ID}, 
   *    {@link IndexManager#FIELD_ARTIFACT_ID},
   *    {@link IndexManager#FIELD_VERSION}, 
   *    {@link IndexManager#FIELD_PACKAGING}
   * 
   * @return Map&lt;String, IndexedArtifact&gt;
   */
  public abstract Map search(String indexName, Query query) throws IOException;

  /**
   * Creates query for given field and expression
   * 
   * @param field One of 
   *    {@link IndexManager#FIELD_GROUP_ID}, 
   *    {@link IndexManager#FIELD_ARTIFACT_ID},
   *    {@link IndexManager#FIELD_VERSION}, 
   *    {@link IndexManager#FIELD_PACKAGING}
   * @param expression search text   
   */
  public abstract Query createQuery(String field, String expression);
  
  public abstract IndexedArtifactFile getIndexedArtifactFile(String indexName, String documentKey) throws IOException;
  
  public abstract IndexedArtifactGroup[] getGroups(String indexId) throws IOException;

  public abstract IndexedArtifactGroup[] getRootGroups(String indexId) throws IOException;

  public abstract Date reindex(String indexName, IProgressMonitor monitor) throws IOException;

  public abstract Date fetchAndUpdateIndex(String indexName, boolean force, IProgressMonitor monitor) throws IOException;

  public abstract Date replaceIndex(String indexName, InputStream is) throws IOException;

  public abstract Date mergeIndex(String indexName, InputStream is) throws IOException;

  public abstract Date getIndexArchiveTime(InputStream is) throws IOException;

  public void addIndexListener(IndexListener indexListener) {
    synchronized(listeners) {
      listeners.add(indexListener);
    }
  }

  public void fireIndexAdded(IndexInfo indexInfo) {
    synchronized(listeners) {
      for(Iterator it = listeners.iterator(); it.hasNext();) {
        IndexListener listener = (IndexListener) it.next();
        listener.indexAdded(indexInfo);
      }
    }
  }

  public void fireIndexRemoved(IndexInfo indexInfo) {
    synchronized(listeners) {
      for(Iterator it = listeners.iterator(); it.hasNext();) {
        IndexListener listener = (IndexListener) it.next();
        listener.indexRemoved(indexInfo);
      }
    }
  }

  public void fireIndexUpdated(IndexInfo indexInfo) {
    synchronized(listeners) {
      for(Iterator it = listeners.iterator(); it.hasNext();) {
        IndexListener listener = (IndexListener) it.next();
        listener.indexChanged(indexInfo);
      }
    }
  }

  public IndexInfo getIndexInfoByUrl(String url) {
    synchronized(indexes) {
      for(Iterator it = indexes.values().iterator(); it.hasNext();) {
        IndexInfo info = (IndexInfo) it.next();
        if(IndexInfo.Type.REMOTE.equals(info.getType()) && url.equals(info.getRepositoryUrl())) {
          return info;
        }
      }
      return null;
    }
  }

  public IndexedArtifactGroup resolveGroup(IndexedArtifactGroup group) {
    String indexName = group.info.getIndexName();
    String prefix = group.prefix;
  
    try {
      IndexedArtifactGroup g = new IndexedArtifactGroup(group.info, group.prefix);
  
      Map results = search(indexName, prefix, IndexManager.SEARCH_GROUP);
  
      for(Iterator it = results.values().iterator(); it.hasNext();) {
        IndexedArtifact a = (IndexedArtifact) it.next();
        String groupId = a.group;
        if(groupId.equals(prefix)) {
          g.files.put(a.artifact, a);
        } else if(groupId.startsWith(prefix + ".")) {
          int start = prefix.length() + 1;
          int end = groupId.indexOf('.', start);
          String key = end > -1 ? groupId.substring(0, end) : groupId;
          g.nodes.put(key, new IndexedArtifactGroup(group.info, key));
        }
      }
  
      return g;
  
    } catch(IOException ex) {
      MavenPlugin.log("Can't retrieve groups for " + indexName + ":" + prefix, ex);
      return group;
    }
  }

  public List getArtifactRepositories(ArtifactRepositoryPolicy snapshotsPolicy, ArtifactRepositoryPolicy releasesPolicy) {
    DefaultRepositoryLayout layout = new DefaultRepositoryLayout();
    
    List artifactRepositories = new ArrayList();
    for(Iterator it = getIndexes().values().iterator(); it.hasNext();) {
      IndexInfo info = (IndexInfo) it.next();
      if(Type.REMOTE.equals(info.getType())) {
        artifactRepositories.add(new DefaultArtifactRepository( //
            info.getIndexName(), info.getRepositoryUrl(), layout, snapshotsPolicy, releasesPolicy));
      }
    }
    return artifactRepositories;
  }

  public String getDocumentKey(Artifact artifact) {
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
    if(classifier!=null) {
      key += "-" + classifier;
    }
  
    // TODO use artifact handler to retrieve extension
    return key + ".pom";
  }

}
