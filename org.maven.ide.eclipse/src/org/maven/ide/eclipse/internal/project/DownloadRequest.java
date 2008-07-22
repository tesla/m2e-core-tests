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

import org.maven.ide.eclipse.embedder.ArtifactKey;


class DownloadRequest {
  final IProject project;
  final IPath path;
  final ArtifactKey artifactKey;
  final boolean downloadSources;
  final boolean downloadJavaDoc;

  public DownloadRequest(IProject project, IPath path, ArtifactKey artifactKey, boolean downloadSources, boolean downloadJavaDoc) {
    this.project = project;
    this.path = path;
    this.artifactKey = artifactKey;
    this.downloadSources = downloadSources;
    this.downloadJavaDoc = downloadJavaDoc;
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
  
  public boolean isDownloadSources() {
    return this.downloadSources;
  }
  
  public boolean isDownloadJavaDoc() {
    return this.downloadJavaDoc;
  }
  
}
