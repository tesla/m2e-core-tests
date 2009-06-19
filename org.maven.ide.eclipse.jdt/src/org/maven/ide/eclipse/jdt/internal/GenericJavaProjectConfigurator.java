/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.JavaCore;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.jdt.IClasspathDescriptor;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;


public class GenericJavaProjectConfigurator extends AbstractJavaProjectConfigurator {
  
  @Override
  protected List<MavenProject> getMavenProjects(ProjectConfigurationRequest request, final IProgressMonitor monitor)
      throws CoreException {

    IProject project = request.getProject();
    IFile pomResource = request.getPom();
    ResolverConfiguration configuration = request.getResolverConfiguration();

    console.logMessage("Generating sources " + pomResource.getFullPath());

    monitor.subTask("reading " + pomResource.getFullPath());
    if(mavenConfiguration.isDebugOutput()) {
      console.logMessage("Reading " + pomResource.getFullPath());
    }
    
    String goalsToExecute = "";
    if (request.isProjectConfigure()) {
      goalsToExecute = mavenConfiguration.getGoalOnUpdate();
    } else if (request.isProjectImport()) {
      goalsToExecute = mavenConfiguration.getGoalOnImport();
    }

    if (goalsToExecute == null || goalsToExecute.trim().length() <= 0) {
      final ArrayList<MavenProject> result = new ArrayList<MavenProject>();
      request.getMavenProjectFacade().accept(new IMavenProjectVisitor() {
        public boolean visit(IMavenProjectFacade projectFacade) throws CoreException {
          result.add(projectFacade.getMavenProject(monitor));
          return true; // keep visiting
        }
      }, IMavenProjectVisitor.NESTED_MODULES);
      return result;
    }

    MavenExecutionRequest executionRequest = projectManager.createExecutionRequest(pomResource, configuration);
    executionRequest.setGoals(Arrays.asList(goalsToExecute.split("[\\s,]+")));
    MavenExecutionResult result = maven.execute(executionRequest, monitor);

    if(result.hasExceptions()) {
      String msg = "Build error for " + pomResource.getFullPath();
      List<Exception> exceptions = result.getExceptions();
      for(Exception ex : exceptions) {
        console.logError(msg + "; " + ex.toString());
        MavenLogger.log(msg, ex);
      }
      markerManager.addMarkers(project, result);
    }

    // TODO optimize project refresh
    monitor.subTask("refreshing");
    // project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));
    project.getFolder("target").refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));

    List<MavenProject> mavenProjects = result.getTopologicallySortedProjects();
    
    if (mavenProjects == null) {
      mavenProjects = new ArrayList<MavenProject>();
      try {
        mavenProjects.add(maven.readProject(pomResource.getLocation().toFile(), monitor));
      } catch (CoreException e) {
        String msg = "Unable to read project " + pomResource.getFullPath();
        console.logError(msg + "; " + e.toString());
        MavenLogger.log(msg, e);
        markerManager.addErrorMarkers(pomResource, e);
        return null;
      }
    }

    return mavenProjects;
  }

  @Override
  protected void addProjectSourceFolders(IClasspathDescriptor classpath, IProject project, MavenProject mavenProject) throws CoreException {

    super.addProjectSourceFolders(classpath, project, mavenProject);

    // HACK to support xmlbeans generated classes MNGECLIPSE-374
    File generatedClassesDir = new File(mavenProject.getBuild().getDirectory(), //
        "generated-classes" + File.separator + "xmlbeans");
    IResource generatedClasses = project.findMember(getProjectRelativePath(project, //
        generatedClassesDir.getAbsolutePath()));
    if(generatedClasses != null && generatedClasses.isAccessible() && generatedClasses.getType() == IResource.FOLDER) {
      classpath.addEntry(JavaCore.newLibraryEntry(generatedClasses.getFullPath(), null, null));
    }
  }


/*
 * XXX move to project configuration manager
 * 
    if(mavenProject != null && !configuration.shouldIncludeModules()) {
      List<String> modules = mavenProject.getModules();
      for(String module : modules) {
        if(!module.startsWith("..")) {
          IFolder moduleDir = project.getFolder(module);
          if(moduleDir.isAccessible()) {
            // TODO don't set derived on modules that are not in Eclipse workspace
            moduleDir.setDerived(true);
          }
        }
      }
    }
*/
}
