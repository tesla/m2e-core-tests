/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import org.eclipse.core.resources.IFile;

import org.maven.ide.eclipse.embedder.ArtifactKey;

/**
 * IManagedCache
 *
 * @author igor
 */
public interface IManagedCache {

  /**
   * @param pom
   * @param mavenProject
   */
  void removeProject(IFile pom, ArtifactKey mavenProject);

}
