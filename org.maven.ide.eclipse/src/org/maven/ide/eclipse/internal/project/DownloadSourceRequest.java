/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;


class DownloadSourceRequest {
  final IProject project;
  final IPath path;
  final ArtifactKey artifactKey;

  public DownloadSourceRequest(IProject project, IPath path, ArtifactKey artifactKey) {
    this.project = project;
    this.path = path;
    this.artifactKey = artifactKey;
  }

  public ArtifactKey getArtifactKey() {
    return artifactKey;
  }

  public IProject getProject() {
    return project;
  }

  public IPath getPath() {
    return path;
  }
}
