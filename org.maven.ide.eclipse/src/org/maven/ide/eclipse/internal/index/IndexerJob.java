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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.index.IndexManager;


/**
 * IndexerJob
 * 
 * @author Eugene Kuleshov
 */
public class IndexerJob extends Job {
  private final IndexManager indexManager;

  private String indexName;

  public IndexerJob(String indexName, File indexDir, IndexManager indexManager) {
    super("Indexing " + indexName);
    this.indexManager = indexManager;
    setPriority(Job.LONG);
  }

  public void reindex(String indexName, long delay) {
    this.indexName = indexName;

    if(getState() == Job.NONE) {
      schedule(delay);
    }
  }

  protected IStatus run(IProgressMonitor monitor) {
    try {
      indexManager.reindex(indexName, monitor);
      return Status.OK_STATUS;

    } catch(IOException ex) {
      return new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, "Indexing error", ex);

    }
  }

}
