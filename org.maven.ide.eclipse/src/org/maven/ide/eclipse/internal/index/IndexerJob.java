/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.index;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.index.IndexManager;


/**
 * IndexerJob
 * 
 * @author Eugene Kuleshov
 */
public class IndexerJob extends Job {
  private final IndexManager indexManager;
  private final MavenConsole console;

  private String indexName;

  public IndexerJob(String indexName, IndexManager indexManager, MavenConsole console) {
    super("Indexing");
    this.indexName = indexName;
    this.indexManager = indexManager;
    this.console = console;
    setPriority(Job.LONG);
  }

  protected IStatus run(IProgressMonitor monitor) {
    monitor.beginTask("Indexing " + indexName, IProgressMonitor.UNKNOWN);
    try {
      indexManager.reindex(indexName, monitor);
      console.logMessage("Updated index " + indexName);
      return Status.OK_STATUS;
    } catch(CoreException ex) {
      console.logError("Unable to reindex local repository");
      return new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Indexing error", ex);
    } finally {
      monitor.done();
    }
  }

}
