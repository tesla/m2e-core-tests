/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

public class DownloadSourceEvent {

  private final IProject source;
  private final IPath path;
  private final IPath srcPath;
  private final String javadocUrl;

  public DownloadSourceEvent(IProject source, IPath path, IPath srcPath, String javadocUrl) {
    this.source = source;
    this.path = path;
    this.srcPath = srcPath;
    this.javadocUrl = javadocUrl;
  }

  /**
   * IClasspathEntry#getPath
   */
  public IPath getPath() {
    return path;
  }

  public IPath getSourcePath() {
    return srcPath;
  }

  public String getJavadocUrl() {
    return javadocUrl;
  }

  public IProject getSource() {
    return source;
  }
  
}
