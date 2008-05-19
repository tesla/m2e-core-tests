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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;

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
    MavenProjectFacade facade = getMavenDistributionArtifact();
    Set artifacts = facade.getMavenProject().getArtifacts();
    List cp = new ArrayList();
    if (forcedComponents != null) {
      for (int i = 0; i < forcedComponents.length; i++) {
        cp.add(forcedComponents[i]);
      }
    }
    for (Iterator it = artifacts.iterator(); it.hasNext(); ) {
      Artifact a = (Artifact) it.next();
      cp.add(a.getFile().getAbsolutePath());
    }
    return (String[]) cp.toArray(new String[cp.size()]);
  }

  private MavenProjectFacade getMavenDistributionArtifact() {
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    Artifact artifact = new DefaultArtifact(
        "org.apache.maven", 
        "maven-distribution", 
        VersionRange.createFromVersion("2.1-SNAPSHOT"), 
        null /*scope*/, 
        "jar" /*type*/, 
        null /*classifier*/, 
        new DefaultArtifactHandler());
    return projectManager.getMavenProject(artifact);
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

  public boolean isEnabled() {
    return getMavenDistributionArtifact() != null;
  }

  public String toString() {
    return "Workspace";
  }
}
