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
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.embedder.MavenEmbedder;

import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.embedder.MavenRuntime;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;

/**
 * Maven 2.1-SNAPSHOT runtime loaded from the Eclipse Workspace
 */
public class MavenWorkspaceRuntime extends MavenRuntime {

  private MavenProjectManager projectManager;
  private MavenEmbedderManager embedderManager;

  public MavenWorkspaceRuntime(MavenProjectManager projectManager, MavenEmbedderManager embedderManager) {
    this.projectManager = projectManager;
    this.embedderManager = embedderManager;
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
    return projectManager.getMavenProject("org.apache.maven", "maven-distribution", "2.1-SNAPSHOT") != null;
  }

  public String[] getClasspath(String[] forcedComponents) {
    List<String> cp = new ArrayList<String>();

    MavenProjectFacade maven = projectManager.getMavenProject("org.apache.maven", "maven-distribution", "2.1-SNAPSHOT");
    if (maven != null) {
      if (forcedComponents != null) {
        for (int i = 0; i < forcedComponents.length; i++) {
          cp.add(forcedComponents[i]);
        }
      }

      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();

      for (Artifact artifact : maven.getMavenProjectArtifacts()) {
        if (Artifact.SCOPE_TEST.equals(artifact.getScope())) {
          continue;
        }

        MavenProjectFacade facade = projectManager.getMavenProject(artifact);

        File file = null;
        if (facade != null) {
          IFolder output = root.getFolder(facade.getOutputLocation());
          if (output.isAccessible()) {
            file = output.getLocation().toFile();
          }
        } else {
          try {
            embedder.resolve(artifact, Collections.EMPTY_LIST, embedder.getLocalRepository());
          } catch(ArtifactResolutionException ex) {
            MavenLogger.log("Artifact resolution error " + artifact, ex);
          } catch(ArtifactNotFoundException ex) {
            MavenLogger.log("Artifact not found " + artifact, ex);
          }
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

