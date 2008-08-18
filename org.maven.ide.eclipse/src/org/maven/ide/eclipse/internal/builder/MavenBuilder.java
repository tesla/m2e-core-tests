/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.builder;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.codehaus.plexus.util.StringUtils;

import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.MavenProjectManager;


public class MavenBuilder extends IncrementalProjectBuilder {

  public static boolean DEBUG = MavenPlugin.getDefault().isDebugging()
      & Boolean.parseBoolean(Platform.getDebugOption(IMavenConstants.PLUGIN_ID + "/debug/builder"));
  
  private final MavenConsole console;
  private MavenProjectManager projectManager;

  public MavenBuilder() {
    MavenPlugin plugin = MavenPlugin.getDefault();
    console = plugin.getConsole();
    this.projectManager = plugin.getMavenProjectManager();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
   *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
   */
  @SuppressWarnings("unchecked")
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
    IProject project = getProject();
    if(project.hasNature(IMavenConstants.NATURE_ID)) {
      IFile pomResource = project.getFile(IMavenConstants.POM_FILE_NAME);
      if(pomResource == null) {
        console.logError("Project " + project.getName() + " does not have pom.xml");
        return null;
      }

      IMavenProjectFacade mavenProject = projectManager.create(getProject(), monitor);
      if (mavenProject == null) {
        // XXX is this really possible? should we warn the user?
        return null;
      }

      if(DEBUG) {
        System.out.println("\nStarting Maven build for " + project.getName() //$NON-NLS-1$
            + " kind:" + kind + " requestedFullBuild:" + getRequireFullBuild(project) //$NON-NLS-1$
            + " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$
      }
      
      if (FULL_BUILD == kind || CLEAN_BUILD == kind || getRequireFullBuild(getProject())) {
        try {
          executePostBuild(mavenProject, monitor);
        } finally {
          resetRequireFullBuild(getProject());
        }
      } else {
        // if( kind == AUTO_BUILD || kind == INCREMENTAL_BUILD ) {
        processResources(mavenProject, monitor);
      }
    }
    return null;
  }

  private void resetRequireFullBuild(IProject project) throws CoreException {
    project.setSessionProperty(IMavenConstants.FULL_MAVEN_BUILD, null);
  }

  private boolean getRequireFullBuild(IProject project) throws CoreException {
    return false;
    // return project.getSessionProperty(IMavenConstants.FULL_MAVEN_BUILD) != null;
  }

  private void processResources(IMavenProjectFacade projectFacade, final IProgressMonitor monitor) throws CoreException {
    final IResourceDelta delta = getDelta(projectFacade.getProject());

    projectFacade.accept(new IMavenProjectVisitor() {
      public boolean visit(IMavenProjectFacade projectFacade) throws CoreException {
        MavenExecutionResult result = null;
        if (hasChangedResources(projectFacade, delta, true, monitor)) {
          result = projectFacade.filterResources(monitor);
        } else if (hasChangedResources(projectFacade, delta, false, monitor)) {
          // XXX optimize! no filtering, just copy the changed resources
          result = projectFacade.filterResources(monitor);
        }
        if(result!=null) {
          logErrors(result, projectFacade.getProject().getName());
        }
        return true;
      }
    }, IMavenProjectVisitor.NESTED_MODULES);
  }

  boolean hasChangedResources(IMavenProjectFacade facade, IResourceDelta delta, boolean filteredOnly, IProgressMonitor monitor) throws CoreException {
    MavenProject mavenProject = facade.getMavenProject(monitor);
    @SuppressWarnings("unchecked")
    List<Resource> resources = mavenProject.getBuild().getResources();
    @SuppressWarnings("unchecked")
    List<Resource> testResources = mavenProject.getBuild().getTestResources();
    
    Set<IPath> folders = new HashSet<IPath>();
    folders.addAll(getResourceFolders(facade, resources, filteredOnly));
    folders.addAll(getResourceFolders(facade, testResources, filteredOnly));

    if (delta == null) {
      return !folders.isEmpty();
    }

    for(IPath folderPath : folders) {
      IResourceDelta member = delta.findMember(folderPath);
      // XXX deal with member kind/flags
      if (member != null) {
        if(DEBUG) {
          System.out.println("  found changed folder " + folderPath); //$NON-NLS-1$
        }
        return true;
      }
    }

    return false;
  }

  private Set<IPath> getResourceFolders(IMavenProjectFacade facade, List<Resource> resources, boolean filteredOnly) {
    Set<IPath> folders = new LinkedHashSet<IPath>();
    for(Resource resource : resources) {
      if (!filteredOnly || resource.isFiltering()) {
        if(DEBUG) {
          System.out.println("  included folder " + resource.getDirectory()); //$NON-NLS-1$
        }
        folders.add(facade.getProjectRelativePath(resource.getDirectory()));
      }
    }
    return folders;
  }

  private void executePostBuild(IMavenProjectFacade projectFacade, IProgressMonitor monitor) throws CoreException {
    String goalsStr = projectFacade.getResolverConfiguration().getFullBuildGoals();
    List<String> goals = Arrays.asList(StringUtils.split(goalsStr));
    MavenExecutionResult result = projectFacade.execute(goals, monitor);
    logErrors(result, projectFacade.getProject().getName());
  }
  
  void logErrors(MavenExecutionResult result, String projectNname) {
    if(result.hasExceptions()) {
      String msg = "Build errors for " + projectNname;
      @SuppressWarnings("unchecked")
      List<Exception> exceptions = result.getExceptions();
      for(Exception ex : exceptions) {
        console.logError(msg + "; " + ex.toString());
        MavenLogger.log(msg, ex);
      }
      
      // XXX add error markers
    }

    // TODO log artifact resolution errors 
    // ArtifactResolutionResult resolutionResult = result.getArtifactResolutionResult();
    // resolutionResult.getCircularDependencyExceptions();
    // resolutionResult.getErrorArtifactExceptions();
    // resolutionResult.getMetadataResolutionExceptions();
    // resolutionResult.getVersionRangeViolations();
  }
  
}

