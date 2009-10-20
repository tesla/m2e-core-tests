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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.repository.Repository;

import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.artifact.GavCalculator;
import org.sonatype.nexus.artifact.M2GavCalculator;

import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.ILocalRepositoryListener;

/**
 * @author Eugene Kuleshov
 */
final class WagonTransferListenerAdapter implements TransferListener {
  private final MavenImpl maven;

  private final IProgressMonitor monitor;

  private final MavenConsole console;

  private long complete = 0;

  // TODO this is just wrong!
  private final GavCalculator gavCalculator = new M2GavCalculator();

  WagonTransferListenerAdapter(MavenImpl maven, IProgressMonitor monitor, MavenConsole console) {
    this.maven = maven;
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
    
    notifyLocalRepositoryListeners(e);
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

  private void notifyLocalRepositoryListeners(TransferEvent e) {
    try {
      ArtifactRepository localRepository = maven.getLocalRepository();
  
      if (!(localRepository.getLayout() instanceof DefaultRepositoryLayout)) {
        return;
      }
  
      String repoBasepath = new File(localRepository.getBasedir()).getCanonicalPath();
  
      File artifactFile = e.getLocalFile();
  
      if (artifactFile == null) {
        return;
      }
  
      String artifactPath = artifactFile.getCanonicalPath();
      if (!artifactPath.startsWith(repoBasepath)) {
        return;
      }
  
      artifactPath = artifactPath.substring(0, artifactPath.length());
      Gav gav = gavCalculator.pathToGav(artifactPath);
      ArtifactKey artifactKey = new ArtifactKey(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), gav.getClassifier());
  
      File repoBasedir = new File(localRepository.getBasedir()).getCanonicalFile();
  
      for (ILocalRepositoryListener listener : maven.getLocalRepositoryListeners()) {
        listener.artifactInstalled(repoBasedir, artifactKey, artifactPath);
      }
    } catch (Exception ex) {
      MavenLogger.log("Could not notify local repository listeners", ex);
    }
  }

}
