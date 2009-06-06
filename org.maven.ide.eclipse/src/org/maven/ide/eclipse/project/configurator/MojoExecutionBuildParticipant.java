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

  public MojoExecutionBuildParticipant(MojoExecution execution) {
    this.execution = execution;
  }

  public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
    IMaven maven = MavenPlugin.lookup(IMaven.class);

    maven.execute(getSession(), execution, monitor);
    
    return null;
  }

}
