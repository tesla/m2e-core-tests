/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.jdt.BuildPathManager;
import org.maven.ide.eclipse.jdt.MavenJdtPlugin;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.MavenUpdateRequest;


/**
 * MavenClasspathContainerInitializer
 * 
 * @author Eugene Kuleshov
 */
public class MavenClasspathContainerInitializer extends ClasspathContainerInitializer {

  public void initialize(IPath containerPath, IJavaProject project) {
    if(BuildPathManager.isMaven2ClasspathContainer(containerPath)) {
      try {
        IClasspathContainer mavenContainer = getBuildPathManager().getSavedContainer(project.getProject());
        if(mavenContainer != null) {
          JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {project},
              new IClasspathContainer[] {mavenContainer}, new NullProgressMonitor());
          return;
        }
      } catch(CoreException ex) {
        MavenLogger.log("Exception initializing classpath container " + containerPath.toString(), ex);
      }

      // force refresh if can't read persisted state
      MavenUpdateRequest request = new MavenUpdateRequest(project.getProject(), true, false);
      getMavenProjectManager().refresh(request);
    }
  }

  public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
    return true;
  }

  public void requestClasspathContainerUpdate(IPath containerPath, final IJavaProject project,
      final IClasspathContainer containerSuggestion) {
    // one job per request. assumption that users are not going to change hundreds of containers simultaneously.
    new Job("Persist classpath container changes") {
      protected IStatus run(IProgressMonitor monitor) {
        try {
          getBuildPathManager().updateClasspathContainer(project, containerSuggestion, monitor);
        } catch(CoreException ex) {
          MavenLogger.log(ex);
          return new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, 0, "Can't persist classpath container", ex);
        }
        return Status.OK_STATUS;
      }
    }.schedule();
  }

  BuildPathManager getBuildPathManager() {
    return MavenJdtPlugin.getDefault().getBuildpathManager();
  }

  MavenProjectManager getMavenProjectManager() {
    return MavenPlugin.getDefault().getMavenProjectManager();
  }
}
