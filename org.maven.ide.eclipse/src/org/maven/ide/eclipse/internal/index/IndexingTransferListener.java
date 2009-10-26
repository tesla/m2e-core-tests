/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.index;

import java.io.File;

import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.ILocalRepositoryListener;
import org.maven.ide.eclipse.index.IIndex;

public class IndexingTransferListener implements ILocalRepositoryListener {

  private final NexusIndexManager indexManager;

  public IndexingTransferListener(NexusIndexManager indexManager) {
    this.indexManager = indexManager;
  }

  public void artifactInstalled(File repositoryBasedir, ArtifactKey artifact, File artifactFile) {
    NexusIndex localIndex = indexManager.getLocalIndex();
    if(artifactFile.getName().endsWith(".jar")) {
      localIndex.addArtifact(artifactFile, artifact, //
          artifactFile.length(), artifactFile.lastModified(), artifactFile, //
          IIndex.NOT_PRESENT, IIndex.NOT_PRESENT);
    }
  }

}
