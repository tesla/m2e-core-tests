/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.jdt.internal;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;


public class GenericJavaProjectConfigurator extends AbstractJavaProjectConfigurator {

  @Override
  protected MavenProject getMavenProject(ProjectConfigurationRequest request, final IProgressMonitor monitor)
      throws CoreException {

    IProject project = request.getProject();
    IFile pomResource = request.getPom();
    ResolverConfiguration configuration = request.getResolverConfiguration();

    console.logMessage("Generating sources " + pomResource.getFullPath());

    monitor.subTask(NLS.bind(Messages.GenericJavaProjectConfigurator_subtask, pomResource.getFullPath()));
    if(mavenConfiguration.isDebugOutput()) {
      console.logMessage("Reading " + pomResource.getFullPath());
    }

    String goalsToExecute = ""; //$NON-NLS-1$
    if(request.isProjectConfigure()) {
      goalsToExecute = mavenConfiguration.getGoalOnUpdate();
    } else if(request.isProjectImport()) {
      goalsToExecute = mavenConfiguration.getGoalOnImport();
    }

    if(goalsToExecute == null || goalsToExecute.trim().length() <= 0) {
      return request.getMavenProject();
    }

    MavenExecutionRequest executionRequest = projectManager.createExecutionRequest(pomResource, configuration, monitor);
    executionRequest.setGoals(Arrays.asList(goalsToExecute.split("[\\s,]+"))); //$NON-NLS-1$
    MavenExecutionResult result = maven.execute(executionRequest, monitor);

    if(result.hasExceptions()) {
      String msg = "Build error for " + pomResource.getFullPath();
      List<Throwable> exceptions = result.getExceptions();
      for(Throwable ex : exceptions) {
        console.logError(msg + "; " + ex.toString());
        MavenLogger.log(msg, ex);
      }
      markerManager.addMarkers(project, result);
    }

    // TODO optimize project refresh
    monitor.subTask(Messages.GenericJavaProjectConfigurator_subtask_refreshing);
    // project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));
    project.getFolder("target").refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1)); //$NON-NLS-1$

    List<MavenProject> mavenProjects = result.getTopologicallySortedProjects();

    if(mavenProjects == null) {
      return request.getMavenProject();
    }

    return mavenProjects.get(0);
  }

  @Override
  protected void invokeJavaProjectConfigurators(IClasspathDescriptor classpath, ProjectConfigurationRequest request,
      final IProgressMonitor monitor) {
  }

}
