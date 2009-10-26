/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import java.io.File;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.repository.LocalRepositoryMaintainer;
import org.apache.maven.repository.LocalRepositoryMaintainerEvent;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.ILocalRepositoryListener;

/**
 * EclipseLocalRepositoryMaintainer
 *
 * @author igor
 */
public class EclipseLocalRepositoryMaintainer implements LocalRepositoryMaintainer {

  public void artifactDownloaded(LocalRepositoryMaintainerEvent event) {
    notifyListeners(event);
  }

  public void artifactInstalled(LocalRepositoryMaintainerEvent event) {
    notifyListeners(event);
  }

  private void notifyListeners(LocalRepositoryMaintainerEvent event) {
    MavenImpl maven = (MavenImpl) MavenPlugin.getDefault().getMaven();

    ArtifactRepository repository = event.getLocalRepository();
    File basedir = new File(repository.getBasedir());
    ArtifactKey key = new ArtifactKey(event.getGroupId(), event.getArtifactId(), event.getVersion(), event.getClassifier());
    for (ILocalRepositoryListener listener : maven.getLocalRepositoryListeners()) {
      listener.artifactInstalled(basedir, key, event.getFile());
    }
  }

}
