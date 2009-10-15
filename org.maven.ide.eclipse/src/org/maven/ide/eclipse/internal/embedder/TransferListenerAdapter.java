/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.repository.Repository;

import org.maven.ide.eclipse.core.MavenConsole;

/**
 * TransferListenerAdapter
 * 
 * @author Eugene Kuleshov
 */
public final class TransferListenerAdapter implements TransferListener {
  private final IProgressMonitor monitor;

  private final MavenConsole console;

  private long complete = 0;

  TransferListenerAdapter(IProgressMonitor monitor, MavenConsole console) {
    this.monitor = monitor == null ? new NullProgressMonitor() : monitor;
    this.console = console;
  }

  public void transferInitiated(TransferEvent e) {
    // System.err.println( "init "+e.getWagon().getRepository()+"/"+e.getResource().getName());
    this.complete = 0;
  }

  public void transferStarted(TransferEvent e) {
    StringBuilder sb = new StringBuilder();
    if(e.getWagon() != null && e.getWagon().getRepository() != null) {
      Wagon wagon = e.getWagon();
      Repository repository = wagon.getRepository();
      String repositoryId = repository.getId();
      sb.append(repositoryId).append(" : ");
    }
    sb.append(e.getResource().getName());
    console.logMessage("Downloading " + sb.toString());
    // monitor.beginTask("0% "+e.getWagon().getRepository()+"/"+e.getResource().getName(), IProgressMonitor.UNKNOWN);
    monitor.setTaskName("0% " + sb.toString());
  }

  public void transferProgress(TransferEvent e, byte[] buffer, int length) {
    if(monitor.isCanceled()) {
      throw new OperationCanceledException("Transfer is canceled");
    }
    
    complete += length;

    long total = e.getResource().getContentLength();

    StringBuffer sb = new StringBuffer();

    formatBytes(complete, sb);
    if(total != WagonConstants.UNKNOWN_LENGTH) {
      sb.append('/');
      formatBytes(total, sb);
      sb.append(" (");
      sb.append(100l * complete / total);
      sb.append("%)");
    }
    sb.append(' ');

    monitor.setTaskName(sb.toString() + e.getWagon().getRepository() + "/"
        + e.getResource().getName());
  }

  public void transferCompleted(TransferEvent e) {
    console.logMessage("Downloaded " + e.getWagon().getRepository() + "/" + e.getResource().getName());

    // monitor.subTask("100% "+e.getWagon().getRepository()+"/"+e.getResource().getName());
    monitor.setTaskName("");
  }

  public void transferError(TransferEvent e) {
    console.logMessage("Unable to download " + e.getWagon().getRepository() + "/" + e.getResource().getName() + ": "
        + e.getException());
    monitor.setTaskName("error " + e.getWagon().getRepository() + "/" + e.getResource().getName());
  }

  public void debug(String message) {
    // System.err.println( "debug "+message);
  }
  
  private static final String[] units = {"B","KB","MB"};
  private void formatBytes(long n, StringBuffer sb) {
    int i = 0;
    while (n >= 1024 && ++i <units.length) n >>= 10;

    sb.append(n);
    sb.append(units[i]);
  }
}
