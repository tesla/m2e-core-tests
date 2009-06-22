/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project.configurator;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.plugin.MojoExecution;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.IMaven;


/**
 * MojoExecutionBuildParticipant
 * 
 * @author igor
 */
public class MojoExecutionBuildParticipant extends AbstractBuildParticipant {

  private final MojoExecution execution;
  private final boolean runOnIncremental;
  private final boolean runOnClean;
  
  public MojoExecutionBuildParticipant(MojoExecution execution, boolean runOnIncremental, boolean runOnClean) {
    this.execution = execution;
    this.runOnClean = runOnClean;
    this.runOnIncremental = runOnIncremental;
  }

  public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
    if(appliesToBuildKind(kind)) {
      IMaven maven = MavenPlugin.lookup(IMaven.class);
  
      maven.execute(getSession(), getMojoExecution(), monitor);
    }
    return null;
  }
  
  public boolean appliesToBuildKind(int kind) {
    if(IncrementalProjectBuilder.FULL_BUILD == kind || IncrementalProjectBuilder.CLEAN_BUILD == kind) {
      return runOnClean;
    } else {
      return runOnIncremental;
    }
  }

  public MojoExecution getMojoExecution() {
    return execution;
  }

}
