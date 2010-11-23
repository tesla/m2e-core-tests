/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.project.configurator;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

import org.eclipse.m2e.core.internal.project.MojoExecutionProjectConfigurator;

/**
 * Project configurator for maven-resources-plugin
 */
public class MavenResourcesProjectConfigurator extends MojoExecutionProjectConfigurator {
  private static final String GROUP_ID = "org.apache.maven.plugins";

  private static final String ARTIFACT_ID = "maven-resources-plugin";

  private static final VersionRange VERSION_RANGE;
  static {
    try {
      VERSION_RANGE = VersionRange.createFromVersionSpec("[2.4,)");
    } catch(InvalidVersionSpecificationException ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    }
  }

  public MavenResourcesProjectConfigurator() {
    super(GROUP_ID, ARTIFACT_ID, VERSION_RANGE, null /*goals*/, true /*runOnIncremental*/);
  }
}
