/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project.configurator;

import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;

/**
 * AbstractClasspathConfigurator
 *
 * @author igor
 */
public class AbstractClasspathConfigurator {

  /**
   * @return Set<IClasspathAttribute> or null
   */
  public Set getAttributes(Artifact artifact, int kind) {
    // no attributes added by defauilt
    return null;
  }

  /**
   * @param entries Map<IPath, IClasspathEntry> maps entry path to IClasspathEntry
   *    Map is ordered.
   */
  public void configureClasspath(Map entries) {
    // TODO Auto-generated method configureClasspath
  }

}
