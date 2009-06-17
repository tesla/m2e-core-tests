/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;

import org.apache.maven.artifact.Artifact;

import org.maven.ide.eclipse.project.IMavenProjectFacade;


/**
 * IClasspathDescriptor
 *
 * @author igor
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IClasspathDescriptor {

  public static interface EntryFilter {
    public boolean accept(IClasspathEntryDescriptor descriptor);
  }

  /**
   * @return true if classpath contains entry with specified path, false otherwise.
   */
  public boolean containsPath(IPath path);

  public void addSourceEntry(IPath sourcePath, IPath outputLocation, boolean generated) throws CoreException;

  public void removeEntry(IPath path);

  public void removeEntry(EntryFilter filter);

  public void addSourceEntry(IPath sourcePath, IPath outputLocation, IPath[] inclusion, IPath[] exclusion,
      boolean generated) throws CoreException;

  public IClasspathEntry[] getEntries();

  public List<IClasspathEntryDescriptor> getEntryDescriptors();

  public void addEntry(IClasspathEntry entry);

  public void addProjectEntry(Artifact artifact, IMavenProjectFacade projectFacade);

  public void addLibraryEntry(Artifact artifact, IPath srcPath, IPath srcRoot, String javaDocUrl);

}
