/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import java.io.File;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.IMavenLauncherConfigurationCollector;
import org.maven.ide.eclipse.embedder.MavenRuntime;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;

/**
 * Maven 3.0-SNAPSHOT runtime loaded from the Eclipse Workspace
 * 
 * @author Eugene Kuleshov
 * @author Igor Fedorenko
 */
public class MavenWorkspaceRuntime implements MavenRuntime {

  private static final ArtifactKey MAVEN_DISTRIBUTION = new ArtifactKey("org.apache.maven", "maven-distribution", "3.0-SNAPSHOT", null);

  private static final ArtifactKey PLEXUS_CLASSWORLDS = new ArtifactKey("org.codehaus.plexus", "plexus-classworlds", null, null);

  private static final String MAVEN_EXECUTOR_CLASS = "org.apache.maven.cli.MavenCli";

  private static final String PLEXUS_CLASSWORLD_NAME = "plexus.core";

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

  public boolean isEditable() {
    return false;
  }

  public boolean isAvailable() {
    return projectManager.getMavenProject(MAVEN_DISTRIBUTION.getGroupId(), MAVEN_DISTRIBUTION.getArtifactId(), MAVEN_DISTRIBUTION.getVersion()) != null;
  }

  public void getMavenLauncherConfiguration(IMavenLauncherConfigurationCollector collector, IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade maven = projectManager.getMavenProject(MAVEN_DISTRIBUTION.getGroupId(), MAVEN_DISTRIBUTION.getArtifactId(), MAVEN_DISTRIBUTION.getVersion());
    if (maven != null) {
      MavenProject mavenProject = maven.getMavenProject(monitor);

      collector.setMainType(MAVEN_EXECUTOR_CLASS, PLEXUS_CLASSWORLD_NAME);

      collector.addRealm(PLEXUS_CLASSWORLD_NAME);

      @SuppressWarnings("unchecked")
      Set<Artifact> artifacts = mavenProject.getArtifacts();

      for (Artifact artifact : artifacts) {
        if (Artifact.SCOPE_TEST.equals(artifact.getScope())) {
          continue;
        }

        IMavenProjectFacade facade = projectManager.getMavenProject(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());

        if (facade != null) {
          collector.addProjectEntry(facade);
        } else {
          File file = artifact.getFile();
          if (file != null) {
            collector.addArchiveEntry(file.getAbsolutePath());
          }
        }
        
      }
    }

    // XXX throw something at the caller! 
  }

  public String toString() {
    return "Workspace";
  }

  public String[] getLauncherClasspath() {
    IMavenProjectFacade maven = projectManager.getMavenProject(MAVEN_DISTRIBUTION.getGroupId(), MAVEN_DISTRIBUTION.getArtifactId(), MAVEN_DISTRIBUTION.getVersion());
    if (maven != null) {
      try {
        MavenProject mavenProject;
        mavenProject = maven.getMavenProject(new NullProgressMonitor());
        @SuppressWarnings("unchecked")
        Set<Artifact> artifacts = mavenProject.getArtifacts();
        ArtifactKey key = PLEXUS_CLASSWORLDS;
        for (Artifact artifact : artifacts) {
          if (key.getGroupId().equals(artifact.getGroupId()) && key.getArtifactId().equals(artifact.getArtifactId())) {
            return new String[] {artifact.getFile().getAbsolutePath()};
          }
        }
      } catch(CoreException ex) {
        // should not happen
      }
    }

    // XXX throw something at the caller! 
    return null;
  }

  public String getLauncherType() {
    return LAUNCHER_TYPE;
  }

}

