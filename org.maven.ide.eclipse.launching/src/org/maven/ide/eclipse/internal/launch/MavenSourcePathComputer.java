/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;


/**
 * @author Eugene Kuleshov
 */
public class MavenSourcePathComputer implements ISourcePathComputer {

  public String getId() {
    return "org.maven.ide.eclipse.launching.MavenSourceComputer";
  }

  public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration, IProgressMonitor monitor)
      throws CoreException {
    List<IRuntimeClasspathEntry> entries = new ArrayList<IRuntimeClasspathEntry>();

    IRuntimeClasspathEntry jreEntry = JavaRuntime.computeJREEntry(configuration);
    if(jreEntry != null) {
      entries.add(jreEntry);
    }

    for(IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      IJavaProject javaProject = JavaCore.create(project);
      if(javaProject != null) {
        entries.add(JavaRuntime.newDefaultProjectClasspathEntry(javaProject));
      }
    }

    IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveSourceLookupPath( //
        entries.toArray(new IRuntimeClasspathEntry[entries.size()]), configuration);
    return JavaRuntime.getSourceContainers(resolved);
  }

}
