/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.launch;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.artifact.Artifact;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.IMavenProjectChangedListener;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;

/**
 * Maintains map file of maven artifacts present in workspace.   
 */
public class WorkspaceStateWriter implements IMavenProjectChangedListener {

  public static final String STATE_FILENAME = "workspacestate.properties";

  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    try {
      Properties state = new Properties();

      MavenProjectManager mpm = MavenPlugin.getDefault().getMavenProjectManager();
      MavenProjectFacade[] projects = mpm.getProjects();

      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

      for (int i = 0; i < projects.length; i++) {
        MavenProjectFacade facade = projects[i];
        Artifact artifact = facade.getMavenProject().getArtifact();
        File pom = facade.getPom().getLocation().toFile();
        if (pom.canRead()) {
          String key = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":pom:" + artifact.getBaseVersion();
          state.put(key, pom.getCanonicalPath());
        }
        IResource outputLocation = root.findMember(facade.getOutputLocation());
        if (!"pom".equals(artifact.getType()) && outputLocation != null && outputLocation.exists()) {
          String key = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType() + ":" + artifact.getBaseVersion();
          state.put(key, outputLocation.getLocation().toFile().getCanonicalPath());
        }
      }

      OutputStream buf = new BufferedOutputStream(new FileOutputStream(getStateFile()));
      try {
        state.store(buf, null);
      } finally {
        buf.close();
      }
      
    } catch(IOException ex) {
      MavenPlugin.log("Error writing workspace state file", ex);
    }
  }

  static File getStateFile() {
    File location = MavenPlugin.getDefault().getStateLocation().toFile();
    return new File(location, STATE_FILENAME);
  }

}
