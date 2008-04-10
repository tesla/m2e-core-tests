/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.launch;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
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
        if (facade.getResolverConfiguration().shouldUseMavenOutputFolders()) {
          Artifact artifact = facade.getMavenProject().getArtifact();
          File pom = facade.getPom().getLocation().toFile();
          if (pom.canRead()) {
            String key = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":pom:" + artifact.getBaseVersion();
            state.put(key, pom.getCanonicalPath());
          }
          File location = root.getLocation().append(facade.getOutputLocation()).toFile();
          if (!"pom".equals(artifact.getType()) && location.exists()) {
            String key = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType() + ":" + artifact.getBaseVersion();
            state.put(key, location.getCanonicalPath());
          }
        }
      }

      File location = MavenPlugin.getDefault().getStateLocation().toFile();
      OutputStream buf = new BufferedOutputStream(new FileOutputStream(new File(location, STATE_FILENAME)));
      try {
        state.store(buf, null);
      } finally {
        buf.close();
      }
      
      write_m2conf(location);
    } catch(IOException ex) {
      MavenPlugin.log("Error writing workspace state file", ex);
    }
  }

  // XXX this code does not belong here
  private void write_m2conf(File location) throws IOException {
    BufferedWriter out = new BufferedWriter(new FileWriter(new File(location, "m2.conf")));
    try {
      out.write("main is org.apache.maven.cli.MavenCli from plexus.core\n");
      // we always set maven.home
//      out.write("set maven.home default ${user.home}/m2\n");
      out.write("[plexus.core]\n");
      URL url = MavenPlugin.getDefault().getBundle().getEntry("org.maven.ide.eclipse.cliresolver.jar");
      out.write("load " + FileLocator.toFileURL(url).toExternalForm() + "\n");
      out.write("load ${maven.home}/lib/*.jar\n");
    } finally {
      out.close();
    }
  }
}
