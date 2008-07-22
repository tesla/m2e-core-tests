/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project;

import org.eclipse.core.runtime.CoreException;

/**
 * This interface is implemented by clients that visit MavenProject tree.
 */
public interface IMavenProjectVisitor {

  public static int NONE = 0;

  public static int LOAD = 1 << 0;
  
  public static int NESTED_MODULES = 1 << 1;

  public static int FORCE_MODULES = 1 << 2;

  /**
   * Visit Maven project or project module
   * 
   * @param projectFacade a facade for visited Maven project
   * @return true if nested artifacts and modules should be visited
   */
  public boolean visit(IMavenProjectFacade projectFacade) throws CoreException;

  /**
   * Visit Maven project dependency/artifact
   * 
   * @param projectFacade a facade for visited Maven project
   * @param artifact an artifact for project dependency
   */
//  public void visit(IMavenProjectFacade projectFacade, Artifact artifact);

}
