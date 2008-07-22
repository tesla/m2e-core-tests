/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project;

import org.apache.maven.artifact.Artifact;

/**
 * IMavenProjectVisitor2
 *
 * @author igor
 */
public interface IMavenProjectVisitor2 extends IMavenProjectVisitor {

  /**
   * @param mavenProjectFacade
   * @param artifact
   */
  void visit(IMavenProjectFacade mavenProjectFacade, Artifact artifact);

}
