/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.internal.embedder;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.apache.maven.wagon.WagonConstants;

import org.eclipse.m2e.core.core.MavenConsole;


/**
 * AbstractTransferListenerAdapter
 * 
 * @author igor
 */
abstract class AbstractTransferListenerAdapter {

  protected final MavenImpl maven;

  protected final IProgressMonitor monitor;

  protected final MavenConsole console;

  protected long complete = 0;

  private static final String[] units = {"B", "KB", "MB"};

  protected AbstractTransferListenerAdapter(MavenImpl maven, IProgressMonitor monitor, MavenConsole console) {
    this.maven = maven;
    this.monitor = monitor == null ? new NullProgressMonitor() : monitor;
    this.console = console;
  }

  protected void formatBytes(long n, StringBuffer sb) {
    int i = 0;
    while(n >= 1024 && ++i < units.length)
      n >>= 10;

    sb.append(n);
    sb.append(units[i]);
  }

  protected void transferInitiated(String artifactUrl) {
    this.complete = 0;
    
    if (artifactUrl != null) {
      monitor.subTask(artifactUrl);
    }
  }

  protected void transferStarted(String artifactUrl) {
    console.logMessage("Downloading " + artifactUrl);
    // monitor.beginTask("0% "+e.getWagon().getRepository()+"/"+e.getResource().getName(), IProgressMonitor.UNKNOWN);
    monitor.subTask("0% " + artifactUrl);
  }

  protected void transferProgress(String artifactUrl, long total, int length) {
    if(monitor.isCanceled()) {
      throw new OperationCanceledException("Transfer is canceled");
    }

    complete += length;

    StringBuffer sb = new StringBuffer();

    formatBytes(complete, sb);
    if(total != WagonConstants.UNKNOWN_LENGTH) {
      sb.append('/');
      formatBytes(total, sb);
      if (total > 0) {
        sb.append(" (");
        sb.append(100l * complete / total);
        sb.append("%)");
      }
    }
    sb.append(' ');

    monitor.subTask(sb.toString() + artifactUrl);
  }

  protected void transferCompleted(String artifactUrl) {
    console.logMessage("Downloaded " + artifactUrl);

    // monitor.subTask("100% "+e.getWagon().getRepository()+"/"+e.getResource().getName());
    monitor.subTask("");
  }

  protected void transferError(String artifactUrl, Exception exception) {
    console.logMessage("Unable to download " + artifactUrl + ": " + exception);
    monitor.subTask("error " + artifactUrl);
  }

}
