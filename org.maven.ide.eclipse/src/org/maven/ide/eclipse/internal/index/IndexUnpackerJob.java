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
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.index.IndexInfo;
import org.maven.ide.eclipse.index.IndexManager;

/**
 * Index unpacker job
 * 
 * @author Eugene Kuleshov
 */
public class IndexUnpackerJob extends Job {
  private final IndexManager indexManager;
  
  /**
   * <code>Collection</code> of <code>IndexInfo</code>s to unpack 
   */
  private final Collection<IndexInfo> extensionIndexes;
  
  private boolean overwrite;

  public IndexUnpackerJob(IndexManager indexManager, Collection<IndexInfo> extensionIndexes) {
    super("Unpacking indexes");
    this.indexManager = indexManager;
    this.extensionIndexes = extensionIndexes;

    setPriority(Job.LONG);
  }
  
  public void setOverwrite(boolean overwrite) {
    this.overwrite = overwrite;
  }

  protected IStatus run(IProgressMonitor monitor) {
    for(Iterator<IndexInfo> it = extensionIndexes.iterator(); it.hasNext();) {
      IndexInfo extensionIndexInfo = it.next();
      String indexName = extensionIndexInfo.getIndexName();
      
      monitor.setTaskName(indexName);

      IndexInfo indexInfo = indexManager.getIndexInfo(indexName);
      if(indexInfo == null) {
        continue;
      }

      URL indexArchive = extensionIndexInfo.getArchiveUrl();
      if(indexArchive!=null) {
        indexInfo.setArchiveUrl(indexArchive);

        Date extensionIndexTime = null;
        try {
          extensionIndexTime = indexManager.getIndexArchiveTime(indexArchive.openStream());
          extensionIndexInfo.setUpdateTime(extensionIndexTime);
        } catch(IOException ex) {
          MavenLogger.log("Unable to read creation time for index " + indexName, ex);
        }
        
        boolean replace = overwrite || indexInfo.isNew();
        if(!replace) {
          if(extensionIndexTime!=null) {
            Date currentIndexTime = indexInfo.getUpdateTime();
            replace = currentIndexTime==null || extensionIndexTime.after(currentIndexTime);
          }
        }

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
            
            // update index and repository urls
            indexManager.removeIndex(indexName, false);
            indexManager.addIndex(extensionIndexInfo, false);
            
          } catch(Exception ex) {
            MavenLogger.log("Unable to unpack index " + indexName, ex);
          } finally {
            close(is);
          }
        }
      }
    }
    return Status.OK_STATUS;
  }
  
  private void close(InputStream is) {
    try {
      if(is != null) {
        is.close();
      }
    } catch(IOException ex) {
      MavenLogger.log("Unable to close stream", ex);
    }
  }

}
