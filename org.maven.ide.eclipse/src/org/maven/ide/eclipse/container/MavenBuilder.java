/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.container;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.MavenProjectFacade;
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

      MavenProjectFacade mavenProject = projectManager.create(getProject(), monitor);
      if (mavenProject == null) {
        // XXX is this really possible? should we warn the user?
        return null;
      }

      if (FULL_BUILD == kind) {
        executePostBuild(mavenProject, monitor);
      } else {
        // if( kind == AUTO_BUILD || kind == INCREMENTAL_BUILD ) {
        processResources(mavenProject, monitor);
      }
    }
    return null;
  }

  /**
   * 
   */
  private void processResources(MavenProjectFacade mavenProject, final IProgressMonitor monitor) throws CoreException {
    final IResourceDelta delta = getDelta(mavenProject.getProject());

    mavenProject.accept(new IMavenProjectVisitor() {
      public boolean visit(MavenProjectFacade projectFacade) throws CoreException {
        if (hasChangedResources(projectFacade, delta, true)) {
          projectFacade.filterResources(monitor);
        } else if (hasChangedResources(projectFacade, delta, false)) {
          // XXX optimize! no filtering, just copy the changed resources
          projectFacade.filterResources(monitor);
        }
        return true;
      }
      public void visit(MavenProjectFacade projectFacade, Artifact artifact) {
      }
    }, IMavenProjectVisitor.NESTED_MODULES);
  }

  boolean hasChangedResources(MavenProjectFacade facade, IResourceDelta delta, boolean filteredOnly) {
    Set<IPath> folders = new HashSet<IPath>();
    folders.addAll(getResourceFolders(facade, facade.getMavenProjectBuildResources(), filteredOnly));
    folders.addAll(getResourceFolders(facade, facade.getMavenProjectBuildTestResources(), filteredOnly));

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

  private Set<IPath> getResourceFolders(MavenProjectFacade facade, List<Resource> resources, boolean filteredOnly) {
    Set<IPath> folders = new LinkedHashSet<IPath>();
    for(Resource resource : resources) {
      if (!filteredOnly || resource.isFiltering()) {
        folders.add(facade.getProjectRelativePath(resource.getDirectory()));
      }
    }
    return folders;
  }

  private void executePostBuild(MavenProjectFacade mavenProject, IProgressMonitor monitor) throws CoreException {
    String goalsStr = mavenProject.getResolverConfiguration().getFullBuildGoals();
    List<String> goals = Arrays.asList(StringUtils.split(goalsStr));
    mavenProject.execute(goals, monitor);
  }
}
