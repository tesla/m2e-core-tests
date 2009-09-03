/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.ArtifactKey;

/**
 * BasicProjectRegistry
 *
 * @author igor
 */
class BasicProjectRegistry implements Serializable {

  private static final long serialVersionUID = 2476327375565147378L;

  private final String m2e_version = MavenPlugin.getQualifiedVersion();

  /**
   * Map<ArtifactKey, IPath> 
   * Maps ArtifactKey to full workspace IPath of the POM file that defines this artifact. 
   */
  protected final Map<ArtifactKey, IPath> workspaceArtifacts = new HashMap<ArtifactKey, IPath>();

  /**
   * Map<ArtifactKey, Set<IPath>> 
   * Maps ArtifactKey to Set of IPath of poms that depend on the artifact.
   * This map only includes dependencies between different (eclipse) projects.
   */
  protected final Map<ArtifactKey, Set<IPath>> workspaceDependencies = new HashMap<ArtifactKey, Set<IPath>>();

  /**
   * Map<ArtifactKey, Set<IPath>> 
   * Maps ArtifactKey to Set of IPath of poms that depend on the artifact.
   * This map only includes dependencies within the same (eclipse) projects.
   */
  protected final Map<ArtifactKey, Set<IPath>> inprojectDependencies = new HashMap<ArtifactKey, Set<IPath>>();

  /**
   * Maps parent ArtifactKey to Set of module poms IPath. This map only includes
   * module defined in eclipse projects other than project that defines parent pom. 
   */
  protected final Map<ArtifactKey, Set<IPath>> workspaceModules = new HashMap<ArtifactKey, Set<IPath>>();

  /**
   * Maps full pom IPath to MavenProjectFacade
   */
  protected final Map<IPath, MavenProjectFacade> workspacePoms = new HashMap<IPath, MavenProjectFacade>();

  protected BasicProjectRegistry() {
  }

  protected BasicProjectRegistry(BasicProjectRegistry other) {
    replaceWith(other);
  }

  protected final void replaceWith(BasicProjectRegistry other) {
    clear();

    workspaceArtifacts.putAll(other.workspaceArtifacts);
    workspaceDependencies.putAll(other.workspaceDependencies);
    inprojectDependencies.putAll(other.inprojectDependencies);
    workspaceModules.putAll(other.workspaceModules);
    workspacePoms.putAll(other.workspacePoms);
  }

  public MavenProjectFacade getProjectFacade(IFile pom) {
    return workspacePoms.get(pom.getFullPath());
  }

  public MavenProjectFacade getProjectFacade(String groupId, String artifactId, String version) {
    IPath path = workspaceArtifacts.get(new ArtifactKey(groupId, artifactId, version, null));
    if (path == null) {
      return null;
    }
    return workspacePoms.get(path);
  }

  public MavenProjectFacade[] getProjects() {
    return workspacePoms.values().toArray(new MavenProjectFacade[workspacePoms.size()]);
  }

  public IPath getWorkspaceArtifact(ArtifactKey key) {
    return workspaceArtifacts.get(key);
  }

  protected void clear() {
    workspaceArtifacts.clear();
    workspaceDependencies.clear();
    inprojectDependencies.clear();
    workspaceModules.clear();
    workspacePoms.clear();
  }

  public boolean isValid() {
    return MavenPlugin.getQualifiedVersion().equals(m2e_version) //
        && workspaceArtifacts != null //
        && workspaceDependencies != null //
        && inprojectDependencies != null //
        && workspaceModules != null //
        && workspacePoms != null;
  }

}
