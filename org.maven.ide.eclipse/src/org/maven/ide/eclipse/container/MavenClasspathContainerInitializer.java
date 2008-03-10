/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.container;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.BuildPathManager;


/**
 * MavenClasspathContainerInitializer
 * 
 * @author Eugene Kuleshov
 */
public class MavenClasspathContainerInitializer extends ClasspathContainerInitializer {
  
  public void initialize(IPath containerPath, final IJavaProject project) {
    if(BuildPathManager.isMaven2ClasspathContainer(containerPath)) {
      try {
        IClasspathContainer container = JavaCore.getClasspathContainer(containerPath, project);
        if (container != null) {
          MavenClasspathContainer mavenContainer = new MavenClasspathContainer(containerPath, container.getClasspathEntries());
          JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {project},
              new IClasspathContainer[] {mavenContainer}, new NullProgressMonitor());
        }
      } catch(JavaModelException ex) {
        MavenPlugin.log("Exception initializing classpath container " + containerPath.toString(), ex);
        return;
      }
    }
  }

  public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
    return true;
  }

  public void requestClasspathContainerUpdate(IPath containerPath, final IJavaProject project,
      final IClasspathContainer containerSuggestion) throws CoreException 
  {
    MavenPlugin.getDefault().getBuildpathManager().updateClasspathContainer(project, containerSuggestion);
  }

}

