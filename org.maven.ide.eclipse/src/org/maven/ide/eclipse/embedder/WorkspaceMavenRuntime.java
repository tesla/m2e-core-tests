/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.embedder.MavenEmbedder;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;

/**
 * WorkspaceMavenRuntime
 *
 * @author igor
 */
public class WorkspaceMavenRuntime extends MavenRuntime {

  public String[] getClasspath(String[] forcedComponents) {
    List cp = new ArrayList();
    MavenProjectFacade maven = getMavenDistributionArtifact();
    if (maven != null) {
      Set artifacts = maven.getMavenProject().getArtifacts();
      if (forcedComponents != null) {
        for (int i = 0; i < forcedComponents.length; i++) {
          cp.add(forcedComponents[i]);
        }
      }

      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
      MavenEmbedderManager embedderManager = MavenPlugin.getDefault().getMavenEmbedderManager();
      MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();

      for (Iterator it = artifacts.iterator(); it.hasNext(); ) {
        Artifact artifact = (Artifact) it.next();
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
            embedder.resolve(artifact, new ArrayList(), embedder.getLocalRepository());
          } catch(ArtifactResolutionException ex) {
          } catch(ArtifactNotFoundException ex) {
          }
          file = artifact.getFile();
        }
        
        if (file != null) {
          cp.add(file.getAbsolutePath());
        }
      }
    }
    return (String[]) cp.toArray(new String[cp.size()]);
  }

  private MavenProjectFacade getMavenDistributionArtifact() {
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    return projectManager.getMavenProject("org.apache.maven", "maven-distribution", "2.1-SNAPSHOT");
  }

  public String getLocation() {
    return MavenRuntimeManager.EMBEDDED;
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
    return getMavenDistributionArtifact() != null;
  }

  public String toString() {
    return "Workspace";
  }
}
