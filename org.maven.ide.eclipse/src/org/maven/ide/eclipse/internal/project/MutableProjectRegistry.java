/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;

import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.embedder.ArtifactKey;

/**
 * WorkspaceStateDelta
 * 
 * @author igor
 */
public class MutableProjectRegistry extends BasicProjectRegistry implements IProjectRegistry {

  public MutableProjectRegistry(ProjectRegistry state) {
    super(state);
  }

  public void addProjectDependency(IFile pom, ArtifactKey dependencyKey, boolean workspace) {
    Map<ArtifactKey, Set<IPath>> dependencies = workspace ? workspaceDependencies : inprojectDependencies;
    Set<IPath> dependentProjects = dependencies.get(dependencyKey);
    if (dependentProjects == null) {
      dependentProjects = new HashSet<IPath>();
      dependencies.put(dependencyKey, dependentProjects);
    }
    dependentProjects.add(pom.getFullPath());
  }

  public void addWorkspaceModule(IFile pom, MavenProject mavenProject) {
    ArtifactKey parentArtifactKey = new ArtifactKey(mavenProject.getParentArtifact());
    addWorkspaceModule(pom, parentArtifactKey);
  }

  public void addWorkspaceModule(IFile pom, ArtifactKey parentArtifactKey) {
    Set<IPath> children = workspaceModules.get(parentArtifactKey);
    if (children == null) {
      children = new HashSet<IPath>();
      workspaceModules.put(parentArtifactKey, children);
    }
    children.add(pom.getFullPath());
  }

  public void addProject(IFile pom, MavenProjectFacade facade) {
    // Add the project to workspaceProjects map
    workspacePoms.put(pom.getFullPath(), facade);

    // Add the project to workspaceArtifacts map
    workspaceArtifacts.put(facade.getArtifactKey(), pom.getFullPath());
  }

  public void removeProject(IFile pom, ArtifactKey mavenProject) {
    // Remove the project from workspaceDependents and inprojectDependenys maps
    removeDependents(pom, workspaceDependencies);
    removeDependents(pom, inprojectDependencies);
    
    // Remove the project from workspaceModules map
    removeDependents(pom, workspaceModules);

    // Remove the project from workspaceProjects map
    workspacePoms.remove(pom.getFullPath());

    // Remove the project from workspaceArtifacts map
    if (mavenProject != null) {
      workspaceArtifacts.remove(mavenProject);
    }
  }

  private void removeDependents(IFile pom, Map<ArtifactKey, Set<IPath>> dependencies) {
    // XXX may not be fast enough
    for(Set<IPath> dependents : dependencies.values()) {
      dependents.remove(pom.getFullPath());
    }
  }

  private Set<IFile> getDependents(IFile pom, ArtifactKey mavenProject, Map<ArtifactKey, Set<IPath>> dependencies) {
    if (mavenProject == null) {
      return Collections.emptySet();
    }

    IWorkspaceRoot root = pom.getWorkspace().getRoot();
    // Map dependencies = workspace ? workspaceDependencies : inprojectDependencies;
    Set<IPath> dependents = dependencies.get(mavenProject);
    if (dependents == null) {
      return Collections.emptySet();
    }

    Set<IFile> pomSet = new LinkedHashSet<IFile>();
    for(IPath dependent : dependents) {
      IFile dependentPom = root.getFile(dependent);
      if(dependentPom == null || dependentPom.equals(pom)) {
        continue;
      }
      if(dependencies == workspaceDependencies || isSameProject(pom, dependentPom)) {
        pomSet.add(dependentPom);
      }
    }
    return pomSet;
  }

  public Set<IFile> getDependents(IFile pom, ArtifactKey mavenProject, boolean includeNestedModules) {
    Set<IFile> dependents = new HashSet<IFile>();
    dependents.addAll(getDependents(pom, mavenProject, workspaceDependencies));
    if (includeNestedModules) {
      dependents.addAll(getDependents(pom, mavenProject, inprojectDependencies));
    }
    return dependents;
  }

  static boolean isSameProject(IResource r1, IResource r2) {
    if (r1 == null || r2 == null) {
      return false;
    }
    return r1.getProject().equals(r2.getProject());
  }

  public Set<IPath> removeWorkspaceModules(IFile pom, ArtifactKey mavenProject) {
    return workspaceModules.remove(mavenProject);
  }

}
