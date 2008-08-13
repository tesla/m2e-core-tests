/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.builder;

import java.util.Arrays;
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

import org.codehaus.plexus.util.StringUtils;

import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.MavenProjectManager;


public class MavenBuilder extends IncrementalProjectBuilder {

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
    return false; // project.getSessionProperty(IMavenConstants.FULL_MAVEN_BUILD) != null;
  }

  private void processResources(IMavenProjectFacade mavenProject, final IProgressMonitor monitor) throws CoreException {
    final IResourceDelta delta = getDelta(mavenProject.getProject());

    mavenProject.accept(new IMavenProjectVisitor() {
      public boolean visit(IMavenProjectFacade projectFacade) throws CoreException {
        if (hasChangedResources(projectFacade, delta, true, monitor)) {
          projectFacade.filterResources(monitor);
        } else if (hasChangedResources(projectFacade, delta, false, monitor)) {
          // XXX optimize! no filtering, just copy the changed resources
          projectFacade.filterResources(monitor);
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
        return true;
      }
    }

    return false;
  }

  private Set<IPath> getResourceFolders(IMavenProjectFacade facade, List<Resource> resources, boolean filteredOnly) {
    Set<IPath> folders = new LinkedHashSet<IPath>();
    for(Resource resource : resources) {
      if (!filteredOnly || resource.isFiltering()) {
        folders.add(facade.getProjectRelativePath(resource.getDirectory()));
      }
    }
    return folders;
  }

  private void executePostBuild(IMavenProjectFacade mavenProject, IProgressMonitor monitor) throws CoreException {
    String goalsStr = mavenProject.getResolverConfiguration().getFullBuildGoals();
    List<String> goals = Arrays.asList(StringUtils.split(goalsStr));
    mavenProject.execute(goals, monitor);
  }
}
