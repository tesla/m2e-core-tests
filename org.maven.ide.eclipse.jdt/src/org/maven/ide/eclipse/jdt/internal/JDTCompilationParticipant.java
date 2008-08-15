/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt.internal;

import java.util.Date;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CompilationParticipant;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.jdt.MavenJdtPlugin;
import org.maven.ide.eclipse.project.MavenProjectManager;


public class JDTCompilationParticipant extends CompilationParticipant {
  
  public static boolean DEBUG = MavenPlugin.getDefault().isDebugging()
      & Boolean.parseBoolean(Platform.getDebugOption(MavenJdtPlugin.PLUGIN_ID + "/debug/compilationParticipant"));
  
  private final MavenProjectManager mavenProjectManager;

  public JDTCompilationParticipant() {
    this.mavenProjectManager = MavenPlugin.getDefault().getMavenProjectManager();
  }
  
  @Override
  public synchronized void cleanStarting(IJavaProject project) {
    if(DEBUG) {
      System.out.println("\nRequested clean for " + project.getProject() //$NON-NLS-1$
          + " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$
    }
    
    super.cleanStarting(project);

    try {
      mavenProjectManager.requestFullMavenBuild(project.getProject());
    } catch(CoreException ex) {
      MavenLogger.log("Exception requesting full Maven build", ex);
    }
  }

  public boolean isActive(IJavaProject project) {
    return true;
  }
  
}
