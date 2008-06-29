/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.container;

import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;

import org.maven.ide.eclipse.MavenPlugin;


/**
 * MavenClasspathContainer
 * 
 * @author Eugene Kuleshov
 */
public class MavenClasspathContainer implements IClasspathContainer {
  private final IClasspathEntry[] entries;
  private final IPath path;

  
  public MavenClasspathContainer() {
    this.path = new Path(MavenPlugin.CONTAINER_ID);
    this.entries = new IClasspathEntry[0];
  }
  
  public MavenClasspathContainer(IPath path, IClasspathEntry[] entries) {
    this.path = path;
    this.entries = entries;
  }
  
  public MavenClasspathContainer(IPath path, Set<IClasspathEntry> entrySet) {
    this(path, entrySet.toArray(new IClasspathEntry[entrySet.size()]));
  }

  public synchronized IClasspathEntry[] getClasspathEntries() {
    return entries;
  }

  public String getDescription() {
    return "Maven Dependencies";  // TODO move to properties
  }

  public int getKind() {
    return IClasspathContainer.K_APPLICATION;
  }

  public IPath getPath() {
    return path; 
  }
  
}

