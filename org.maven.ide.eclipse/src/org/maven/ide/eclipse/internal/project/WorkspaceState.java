/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.project.MavenProjectFacade;

/**
 * WorkspaceState
 *
 * @author igor
 */
public class WorkspaceState {
  /**
   * Map<ArtifactKey, IPath> 
   * Maps ArtifactKey to full workspace IPath of the POM file that defines this artifact. 
   */
  private final Map/*<ArtifactKey, IPath>*/ workspaceArtifacts = new HashMap/*<ArtifactKey, IPath>*/();

  /**
   * Map<ArtifactKey, Set<IPath>> 
   * Maps ArtifactKey to Set of IPath of poms that depend on the artifact.
   * This map only includes dependencies between different (eclipse) projects.
   */
  private final Map/*<ArtifactKey, Set<IPath>>*/ workspaceDependencies = new HashMap/*<ArtifactKey, Set<IPath>>*/();

  /**
   * Map<ArtifactKey, Set<IPath>> 
   * Maps ArtifactKey to Set of IPath of poms that depend on the artifact.
   * This map only includes dependencies within the same (eclipse) projects.
   */
  private final Map/*<ArtifactKey, Set<IPath>>*/ inprojectDependencies = new HashMap/*<ArtifactKey, Set<IPath>>*/();

  /**
   * Maps parent ArtifactKey to Set of module poms IPath. This map only includes
   * module defined in eclipse projects other than project that defines parent pom. 
   */
  private final Map/*<ArtifactKey, Set<IPath>>*/ workspaceModules = new HashMap/*<ArtifactKey, Set<IPath>>*/();

  /**
   * Maps full pom IPath to MavenProjectFacade
   */
  private final Map/*<IPath, MavenProjectFacade>*/ workspacePoms = new HashMap/*<IPath, MavenProjectFacade>*/();

  /**
   * @param state
   */
  public WorkspaceState(WorkspaceState state) {
    // TODO Auto-generated constructor stub
  }

  public synchronized MavenProjectFacade getProjectFacade(IFile pom) {
    return (MavenProjectFacade) workspacePoms.get(pom.getFullPath());
  }

  public synchronized MavenProjectFacade getMavenProject(Artifact artifact) {
    IPath path = (IPath) workspaceArtifacts.get(new ArtifactKey(artifact));
    if (path == null) {
      return null;
    }
    return (MavenProjectFacade) workspacePoms.get(path);
  }

  public synchronized MavenProjectFacade[] getProjects() {
    return (MavenProjectFacade[]) workspacePoms.values().toArray(new MavenProjectFacade[workspacePoms.size()]);
  }

  public synchronized IPath getWorkspaceArtifact(ArtifactKey key) {
    return (IPath) workspaceArtifacts.get(key);
  }

  public synchronized void addProjectDependency(IFile pom, ArtifactKey dependencyKey, boolean workspace) {
    Map/*<ArtifactKey, Set<IPath>>*/ dependencies = workspace? workspaceDependencies: inprojectDependencies;
    Set/*<IPath>*/ dependentProjects = (Set) dependencies.get(dependencyKey);
    if (dependentProjects == null) {
      dependentProjects = new HashSet/*<IPath>*/();
      dependencies.put(dependencyKey, dependentProjects);
    }
    dependentProjects.add(pom.getFullPath());
  }

  public synchronized void addWorkspaceModules(IFile pom, MavenProject mavenProject) {
    ArtifactKey parentArtifactKey = new ArtifactKey(mavenProject.getParentArtifact());
    Set/*<IPath>*/ children = (Set) workspaceModules.get(parentArtifactKey);
    if (children == null) {
      children = new HashSet/*<IPath>*/();
      workspaceModules.put(parentArtifactKey, children);
    }
    children.add(pom.getFullPath());
  }

  public synchronized void addProject(IFile pom, MavenProjectFacade facade) {
    // Add the project to workspaceProjects map
    workspacePoms.put(pom.getFullPath(), facade);

    // Add the project to workspaceArtifacts map
    ArtifactKey artifactKey = new ArtifactKey(facade.getMavenProject().getArtifact());
    workspaceArtifacts.put(artifactKey, pom.getFullPath());
  }

  public synchronized void removeProject(IFile pom, MavenProject mavenProject) {
    // Remove the project from workspaceDependents and inprojectDependenys maps
    removeDependents(pom, workspaceDependencies);
    removeDependents(pom, inprojectDependencies);

    // Remove the project from workspaceProjects map
    workspacePoms.remove(pom.getFullPath());

    // Remove the project from workspaceArtifacts map
    if (mavenProject != null) {
      ArtifactKey artifactKey = new ArtifactKey(mavenProject.getArtifact());
      workspaceArtifacts.remove(artifactKey);
    }
  }

  private void removeDependents(IFile pom, Map/*<ArtifactKey, Set<IPath>>*/ dependencies) {
    // XXX may not be fast enough
    for (Iterator it = dependencies.values().iterator(); it.hasNext(); ) {
      Set dependents = (Set) it.next();
      dependents.remove(pom.getFullPath());
    }
  }

  private static final Set EMPTY_IFILE_SET = Collections.EMPTY_SET;

  private Set/*<IFile>*/ getDependents(IFile pom, MavenProject mavenProject, Map/*<ArtifactKey, Set<IPath>>*/ dependencies) {
    if (mavenProject == null) {
      return EMPTY_IFILE_SET;
    }

    IWorkspaceRoot root = pom.getWorkspace().getRoot();
    // Map dependencies = workspace ? workspaceDependencies : inprojectDependencies;
    Set dependents = (Set) dependencies.get(new ArtifactKey(mavenProject.getArtifact()));
    if (dependents == null) {
      return EMPTY_IFILE_SET;
    }

    Set pomSet = new LinkedHashSet();
    for(Iterator it = dependents.iterator(); it.hasNext();) {
      IFile dependentPom = root.getFile((IPath) it.next());
      if(dependentPom == null || dependentPom.equals(pom)) {
        continue;
      }
      if(dependencies == workspaceDependencies || isSameProject(pom, dependentPom)) {
        pomSet.add(dependentPom);
      }
    }
    return pomSet;
  }

  public synchronized Set/*<IFile>*/ getDependents(IFile pom, MavenProject mavenProject, boolean includeNestedModules) {
    Set dependents = new HashSet();
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

  public synchronized Set/*<IPath>*/ removeWorkspaceModules(IFile pom, MavenProject mavenProject) {
    ArtifactKey key = new ArtifactKey(mavenProject.getArtifact());
    return (Set) workspaceModules.remove(key);
  }

}
