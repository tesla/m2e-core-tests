/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.DefaultArtifactResolver;

import org.maven.ide.eclipse.embedder.ArtifactKey;



public class EclipseArtifactResolver extends DefaultArtifactResolver {

  public void resolve(Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository)
      throws ArtifactResolutionException, ArtifactNotFoundException {
    if(!resolveAsEclipseProject(artifact)) {
      super.resolve(artifact, remoteRepositories, localRepository);
    }
  }

  public void resolveAlways(Artifact artifact, List<ArtifactRepository > remoteRepositories, ArtifactRepository localRepository)
      throws ArtifactResolutionException, ArtifactNotFoundException {
    if(!resolveAsEclipseProject(artifact)) {
      super.resolveAlways(artifact, remoteRepositories, localRepository);
    }
  }

  protected static boolean resolveAsEclipseProject(Artifact artifact) {
    MavenProjectManagerImpl.Context context = MavenProjectManagerImpl.getContext();
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
        || (context.resolverConfiguration.shouldIncludeModules() && WorkspaceState.isSameProject(context.pom, pom))) {

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

}
