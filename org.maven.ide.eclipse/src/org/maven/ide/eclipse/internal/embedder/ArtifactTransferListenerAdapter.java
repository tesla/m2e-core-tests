/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.repository.ArtifactTransferEvent;
import org.apache.maven.repository.ArtifactTransferListener;

import org.maven.ide.eclipse.core.MavenConsole;


/**
 * ArtifactTransferListenerAdapter
 * 
 * @author igor
 */
public class ArtifactTransferListenerAdapter extends AbstractTransferListenerAdapter implements
    ArtifactTransferListener {

  ArtifactTransferListenerAdapter(MavenImpl maven, IProgressMonitor monitor, MavenConsole console) {
    super(maven, monitor, console);
  }

  public boolean isShowChecksumEvents() {
    return false;
  }

  public void setShowChecksumEvents(boolean showChecksumEvents) {
  }

  public void transferCompleted(ArtifactTransferEvent transferEvent) {
    transferCompleted(transferEvent.getResource().getUrl());
  }

  public void transferInitiated(ArtifactTransferEvent transferEvent) {
    transferInitiated(transferEvent.getResource().getUrl());
  }

  public void transferProgress(ArtifactTransferEvent transferEvent) {
    long total = transferEvent.getResource().getContentLength();
    String artifactUrl = transferEvent.getResource().getUrl();

    transferProgress(artifactUrl, total, transferEvent.getDataLength());
  }

  public void transferStarted(ArtifactTransferEvent transferEvent) {
    transferStarted(transferEvent.getResource().getUrl());
  }

}
