/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt;

import java.util.Set;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;

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
  public Set<IClasspathAttribute> getAttributes(Artifact artifact, int kind) {
    // no attributes added by defauilt
    return null;
  }

  /**
   * Returns project classpath. Default implementation simply return parameter.
   * 
   * @param entries Set<IClasspathEntry> may contain entries that only differ in 
   *    extended attribute values
   * @return Set<IClasspathEntry> classpath entries 
   */
  public Set<IClasspathEntry> configureClasspath(Set<IClasspathEntry> entries) {
    return entries;
  }

}
