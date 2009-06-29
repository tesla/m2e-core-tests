/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.repository.LocalArtifactRepository;

import org.maven.ide.eclipse.embedder.ArtifactKey;


public final class EclipseWorkspaceArtifactRepository extends LocalArtifactRepository {

  private static final long serialVersionUID = 1018465082844566543L;

  private final transient MavenProjectManagerImpl.Context context;

  private static final ThreadLocal<Boolean> disabled = new ThreadLocal<Boolean>();

  public EclipseWorkspaceArtifactRepository(MavenProjectManagerImpl.Context context) {
    this.context = context;
  }

  protected boolean resolveAsEclipseProject(Artifact artifact) {
    if (isDisabled()) {
      return false;
    }

    if(context == null) { // XXX this is actually a bug 
      return false;
    }

    if(artifact == null) {
      // according to the DefaultArtifactResolver source code, it looks like artifact can be null
      return false;
    }

    // check in the workspace, note that workspace artifacts never have classifiers
    ArtifactKey key = new ArtifactKey(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(), null); 
    IPath pomPath = context.state.getWorkspaceArtifact(key);
    if(pomPath == null) {
      return false;
    }
    if(pomPath.equals(context.pom.getFullPath())) {
      return false;
    }

    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFile pom = root.getFile(pomPath);
    if(pom == null || !pom.isAccessible()) {
      // XXX this is really a bug, need to throw something meaningful
      return false;
    }

//    if(!"pom".equals(artifact.getType())) {
//      return false;
//    }

    if(context.resolverConfiguration.shouldResolveWorkspaceProjects()
        || (context.resolverConfiguration.shouldIncludeModules() && MutableProjectRegistry.isSameProject(context.pom, pom))) {

      IPath file = pom.getLocation();
      if (!"pom".equals(artifact.getType())) {
        MavenProjectFacade facade = context.state.getProjectFacade(pom);
        IFolder outputLocation = root.getFolder(facade.getOutputLocation());
        if (outputLocation.exists()) {
          file = outputLocation.getLocation();
        }
      }

      artifact.setFile(file.toFile());
      artifact.setResolved(true);

      return true;
    }

    return false;
  }

  public Artifact find(Artifact artifact) {
    resolveAsEclipseProject(artifact);
    return artifact;
  }

  public boolean hasLocalMetadata() {
    return false; // XXX
  }

  public boolean isAuthoritative() {
    return true;
  }

  public static void setDisabled(boolean disable) {
    disabled.set(disable? Boolean.TRUE: null);
  }
  
  public static boolean isDisabled() {
    return Boolean.TRUE.equals(disabled.get());
  }

  public int hashCode() {
    return 0; // no state
  }

  public boolean equals(Object obj) {
    return obj instanceof EclipseWorkspaceArtifactRepository;
  }
}
