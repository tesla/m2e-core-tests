/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project.configurator;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.embedder.MavenEmbedder;

/**
 * LifecycleMapping
 *
 * @author igor
 */
public interface LifecycleMapping {

  void configure(MavenEmbedder embedder, ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException;

  void unconfigure(MavenEmbedder embedder, ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException;

  List<AbstractBuildParticipant> getBuildParticipants();

  List<AbstractProjectConfigurator> getProjectConfigurators();
}
