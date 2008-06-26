/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.container;

import java.io.File;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ClasspathVariableInitializer;
import org.eclipse.jdt.core.JavaCore;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;

/**
 * Maven classpath variable initializer is used to handle M2_REPO variable.
 *
 * @author Eugene Kuleshov
 */
public class MavenClasspathVariableInitializer extends ClasspathVariableInitializer {

  public MavenClasspathVariableInitializer() {
  }

  public void initialize(String variable) {
    init();
  }

  public static void init() {
    MavenEmbedderManager embedderManager = MavenPlugin.getDefault().getMavenEmbedderManager();
    File localRepositoryDir = embedderManager.getLocalRepositoryDir();

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceDescription wsDescription = workspace.getDescription();        
    boolean autobuild = wsDescription.isAutoBuilding();

    try {
      setAutobuild(workspace, false);
      JavaCore.setClasspathVariable(MavenPlugin.M2_REPO, //
          new Path(localRepositoryDir.getAbsolutePath()), //
          new NullProgressMonitor());
    } catch(CoreException ex) {
      MavenPlugin.log(ex);
    } finally {
      try {
        setAutobuild(workspace, autobuild);
      } catch (CoreException ex2) {
        MavenPlugin.log(ex2);
      }
    }

  }

  private static boolean setAutobuild(IWorkspace workspace, boolean newState) throws CoreException {
    IWorkspaceDescription description = workspace.getDescription();
    boolean oldState = description.isAutoBuilding();
    if(oldState != newState) {
      description.setAutoBuilding(newState);
      workspace.setDescription(description);
    }
    return oldState;
  }
  
}
