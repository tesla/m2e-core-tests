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
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;

import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.index.IndexInfo;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.index.IndexedArtifactGroup;
import org.maven.ide.eclipse.index.Indexer;


/**
 * Legacy index manager
 * 
 * @author Eugene Kuleshov
 */
public class LegacyIndexManager extends IndexManager {

  public LegacyIndexManager(File stateDir, MavenConsole console) {
    super(console, stateDir, "index");
  }

  public void addIndex(IndexInfo indexInfo, boolean reindex) {
    String indexName = indexInfo.getIndexName();
    addIndex(indexName, indexInfo);

    // indexInfo.getType() == IndexInfo.Type.LOCAL
    if(indexInfo.getRepositoryDir() != null && (reindex || !isValidIndex(indexName))) {
      scheduleIndexUpdate(IndexManager.LOCAL_INDEX, true, 5000L);
    } else {
      Indexer indexer = new Indexer();
      try {
        indexer.createIndex(workspaceIndexDirectory);
      } catch(IOException ex) {
        String msg = "Unable to create index " + indexInfo.getIndexName();
        console.logError(msg + "; " + ex.getMessage());
        MavenLogger.log(msg, ex);
      }
    }
  }

  public void removeIndex(String indexName, boolean delete) {
    removeIndex(indexName);
  }

  public synchronized void addDocument(String indexName, File pomFile, String name, long size, long date, File jarFile,
      int sourcesExists, int javadocExists) {
    try {
      Directory indexDir = getIndexDirectory(indexName);
      if(indexDir == null) {
        // TODO log
      } else {
        Indexer indexer = new Indexer();
        indexer.addDocument(indexDir, name, size, date, indexer.readNames(jarFile), indexName);
      }
    } catch(IOException ex) {
      String msg = "Unable add " + name;
      console.logError(msg + "; " + ex.getMessage());
      MavenLogger.log(msg, ex);
    }
  }

  public synchronized void removeDocument(String indexName, File pomFile, String name) {
    try {
      Directory indexDir = getIndexDirectory(indexName);
      if(indexDir == null) {
        // TODO log
      } else {
        Indexer indexer = new Indexer();
        indexer.removeDocument(indexDir, name);
      }
    } catch(IOException ex) {
      String msg = "Unable to remove " + name;
      console.logError(msg + "; " + ex.getMessage());
      MavenLogger.log(msg, ex);
    }
  }

  public Map<String, IndexedArtifact> search(String query, String field) throws CoreException {
    try {
      Indexer indexer = new Indexer();
      return indexer.search(getIndexDirs(), query, field);
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Search error", ex));
    }
  }

  public Map<String, IndexedArtifact> search(String indexName, String prefix, String searchGroup) {
    // not supported
    return Collections.emptyMap();
  }
  
  public Map<String, IndexedArtifact> search(String indexName, Query query) {
    // not supported
    return Collections.emptyMap();
  }

  public IndexedArtifactGroup[] getGroups(String indexId) {
    // not supported
    return new IndexedArtifactGroup[0];
  }
  
  public IndexedArtifactGroup[] getRootGroups(String indexId) {
    // not supported
    return new IndexedArtifactGroup[0];
  }

  public Date getIndexArchiveTime(InputStream is) {
    // not supported
    return null;
  }
  
  public Date fetchAndUpdateIndex(String indexId, boolean force, final IProgressMonitor monitor) {
    // not supported
    return null;
  }

  public Properties fetchIndexProperties(String repositoryUrl, String indexUpdateUrl, IProgressMonitor monitor) {
    // not supported
    return null;
  }
  
  public Date replaceIndex(String indexName, InputStream is) {
    // not supported
    return null;
  }

  public Date mergeIndex(String indexName, InputStream is) {
    // not supported
    return null;
  }

  public Date reindex(String indexName, IProgressMonitor monitor) throws CoreException {
    IndexInfo indexInfo = getIndexInfo(indexName);
    if(indexInfo == null) {
      return null;
    }

//    File baseIndexDir = getBaseIndexDir();
//    File indexDir = new File(baseIndexDir, indexName);
//    if(!indexDir.exists()) {
//      indexDir.mkdirs();
//    }

    if(indexInfo.getRepositoryDir() != null) {
      try {
        String repositoryPath = indexInfo.getRepositoryDir().getAbsolutePath();
        Directory indexDir = getIndexDirectory(indexInfo);

        Indexer indexer = new Indexer();
        indexer.reindex(indexDir, repositoryPath, indexName, monitor);
      } catch(IOException ex) {
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Reindexing error", ex));
      }
    }
    return null;
  }

  private Directory[] getIndexDirs() {
    Map<String, IndexInfo> indexMap = getIndexes();
    Directory[] indexDirs = new Directory[indexMap.size()];
    int i = 0;
    for(IndexInfo indexInfo : indexMap.values()) {
      try {
        indexDirs[i++ ] = getIndexDirectory(indexInfo);
      } catch(IOException ex) {
        // TODO log
      }
    }
    return indexDirs;
  }

  private boolean isValidIndex(String indexName) {
    IndexInfo indexInfo = getIndexInfo(indexName);
    if(indexInfo == null) {
      return false;
    }

    File indexDir = new File(getBaseIndexDir(), LOCAL_INDEX);
    if(indexDir.exists()) {
      IndexReader reader = null;
      try {
        reader = IndexReader.open(indexDir);
        return true;
      } catch(RuntimeException ex) {
      } catch(Exception ex) {
        // ignore
      } finally {
        try {
          if(reader != null) {
            reader.close();
          }
        } catch(IOException ex) {
          // ignore
        }
      }
    }
    return false;
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.index.IndexManager#getIndexedArtifactFile(java.lang.String, java.lang.String)
   */
  public IndexedArtifactFile getIndexedArtifactFile(String indexName, String name) {
    return null;
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.index.IndexManager#createQuery(java.lang.String, java.lang.String)
   */
  public Query createQuery(String field, String expression) {
    return null;
  }

}
