/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.embedder.MavenEmbedder;

import org.maven.ide.eclipse.project.MavenProjectFacade;

/**
 * Used to configure maven projects.
 *
 * Work in progress.
 */
public interface IMavenProjectConfigurator {

  public void configure(MavenEmbedder embedder, MavenProjectFacade facade, IProgressMonitor monitor);
}
