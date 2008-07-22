/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.MavenRuntime;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;

/**
 * Maven 2.1-SNAPSHOT runtime loaded from the Eclipse Workspace
 */
public class MavenWorkspaceRuntime extends MavenRuntime {

  private static final ArtifactKey MAVEN_DISTRIBUTION = new ArtifactKey("org.apache.maven", "maven-distribution", "2.1-SNAPSHOT", null);

  private MavenProjectManager projectManager;

  public MavenWorkspaceRuntime(MavenProjectManager projectManager) {
    this.projectManager = projectManager;
  }
  
  public String getLocation() {
    return MavenRuntimeManager.WORKSPACE;
  }
  
  public String getSettings() {
    return null;
  }

  public String getMainTypeName() {
    return "org.apache.maven.cli.MavenCli";
  }

  public String getOptions(File tpmfolder, String[] forcedComponents) {
    return "";
  }

  public boolean isEditable() {
    return false;
  }

  public boolean isAvailable() {
    return projectManager.getMavenProject(MAVEN_DISTRIBUTION.getGroupId(), MAVEN_DISTRIBUTION.getArtifactId(), MAVEN_DISTRIBUTION.getVersion()) != null;
  }

  public String[] getClasspath(String[] forcedComponents) throws CoreException {
    List<String> cp = new ArrayList<String>();

    IMavenProjectFacade maven = projectManager.getMavenProject(MAVEN_DISTRIBUTION.getGroupId(), MAVEN_DISTRIBUTION.getArtifactId(), MAVEN_DISTRIBUTION.getVersion());
    if (maven != null) {
      if (forcedComponents != null) {
        for (int i = 0; i < forcedComponents.length; i++) {
          cp.add(forcedComponents[i]);
        }
      }

      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

      IProgressMonitor monitor = new NullProgressMonitor();

      MavenProject mavenProject = maven.getMavenProject(monitor);

      @SuppressWarnings("unchecked")
      Set<Artifact> artifacts = mavenProject.getArtifacts();

      for (Artifact artifact : artifacts) {
        if (Artifact.SCOPE_TEST.equals(artifact.getScope())) {
          continue;
        }

        IMavenProjectFacade facade = projectManager.getMavenProject(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());

        File file = null;
        if (facade != null) {
          IFolder output = root.getFolder(facade.getOutputLocation());
          if (output.isAccessible()) {
            file = output.getLocation().toFile();
          }
        } else {
            file = artifact.getFile();
        }
        
        if (file != null) {
          cp.add(file.getAbsolutePath());
        }
      }
    }
    
    return cp.toArray(new String[cp.size()]);
  }

  public String toString() {
    return "Workspace";
  }

}

