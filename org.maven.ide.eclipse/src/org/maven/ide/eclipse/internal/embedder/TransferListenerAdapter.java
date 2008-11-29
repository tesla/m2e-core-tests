/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;

import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.index.IndexManager;


/**
 * TransferListenerAdapter
 * 
 * @author Eugene Kuleshov
 */
public final class TransferListenerAdapter implements TransferListener {
  private final IProgressMonitor monitor;

  private final MavenConsole console;

  private final IndexManager indexManager;

  private long complete = 0;

  public TransferListenerAdapter(IProgressMonitor monitor, MavenConsole console, IndexManager indexManager) {
    this.monitor = monitor == null ? new NullProgressMonitor() : monitor;
    this.console = console;
    this.indexManager = indexManager;
  }

  public void transferInitiated(TransferEvent e) {
    // System.err.println( "init "+e.getWagon().getRepository()+"/"+e.getResource().getName());
    this.complete = 0;
  }

  public void transferStarted(TransferEvent e) {
    Wagon wagon = e.getWagon();
    Repository repository = wagon.getRepository();
    String repositoryId = repository.getId();
    console.logMessage("Downloading " + repositoryId + " : " + e.getResource().getName());
    // monitor.beginTask("0% "+e.getWagon().getRepository()+"/"+e.getResource().getName(), IProgressMonitor.UNKNOWN);
    monitor.setTaskName("0% " + repositoryId + " : " + e.getResource().getName());

    // TODO register new repository
//    IndexInfo info = indexManager.getIndexInfoByUrl(repositoryUrl);
//    if(info==null) {
//      info = new IndexInfo(repositoryId, null, repositoryUrl, IndexInfo.Type.REMOTE, false);
//      indexManager.addIndex(info, false);
//    }
  }

  public void transferProgress(TransferEvent e, byte[] buffer, int length) {
    if(monitor.isCanceled()) {
      throw new OperationCanceledException("Transfer is canceled");
    }
    
    complete += length;

    long total = e.getResource().getContentLength();

    StringBuffer sb = new StringBuffer();
    if(total >= 1024) {
      sb.append(complete / 1024);
      if(total != WagonConstants.UNKNOWN_LENGTH) {
        sb.append("/").append(total / 1024).append("K");
      }

    } else {
      sb.append(complete);
      if(total != WagonConstants.UNKNOWN_LENGTH) {
        sb.append("/").append(total).append("b");
      }
    }

    monitor.setTaskName((int) (100d * complete / total) + "% " + e.getWagon().getRepository() + "/"
        + e.getResource().getName());
  }

  public void transferCompleted(TransferEvent e) {
    console.logMessage("Downloaded " + e.getWagon().getRepository() + "/" + e.getResource().getName());

    // monitor.subTask("100% "+e.getWagon().getRepository()+"/"+e.getResource().getName());
    monitor.setTaskName("");

    // updating local index
    // String repository = e.getWagon().getRepository().getName();
    Resource resource = e.getResource();

    if(indexManager != null) {
      File file = e.getLocalFile();
      if(file.getName().endsWith(".jar")) {
        indexManager.addDocument(IndexManager.LOCAL_INDEX, file, resource.getName(), //
            resource.getContentLength(), resource.getLastModified(), e.getLocalFile(), //
            IndexManager.NOT_PRESENT, IndexManager.NOT_PRESENT);
      }
    }
  }

  public void transferError(TransferEvent e) {
    console.logMessage("Unable to download " + e.getWagon().getRepository() + "/" + e.getResource().getName() + ": "
        + e.getException());
    monitor.setTaskName("error " + e.getWagon().getRepository() + "/" + e.getResource().getName());
  }

  public void debug(String message) {
    // System.err.println( "debug "+message);
  }
}
