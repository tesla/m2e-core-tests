/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.ui.internal.actions.MavenPropertyTester;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.MavenRunner;
import org.eclipse.m2e.tests.common.RequireMavenExecutionContext;


@RunWith(MavenRunner.class)
public class MavenPropertyTesterTest extends AbstractMavenProjectTestCase {

  private static final String IS_BUILD_DIRECTORY = "isBuildDirectory"; //$NON-NLS-1$

  @RequireMavenExecutionContext
  @Test
  public void testIsBuildDirectory() throws Exception {
    IProject[] projects = importProjects("projects/385422_filterMavenBuildDir/", new String[] {"simple/pom.xml",
        "nonDefaultBuildDir/pom.xml"}, new ResolverConfiguration());

    IProject p1 = projects[0];
    IProject p2 = projects[1];
    IFolder targetDir;
    IFolder buildDir;

    MavenPropertyTester propertyTester = new MavenPropertyTester();

    //check default build directory :
    targetDir = p1.getFolder("target");
    buildDir = p1.getFolder("build");

    assertTrue(targetDir + " should be identified as build dir",
        propertyTester.test(targetDir, IS_BUILD_DIRECTORY, null, null));
    assertFalse(buildDir + " should not be identified as build dir",
        propertyTester.test(buildDir, IS_BUILD_DIRECTORY, null, null));

    //Check for custom build directory
    targetDir = p2.getFolder("target");
    buildDir = p2.getFolder("build");

    assertTrue(buildDir + " should be identified as build dir",
        propertyTester.test(buildDir, IS_BUILD_DIRECTORY, null, null));
    assertFalse(targetDir + " should not be identified as build dir",
        propertyTester.test(targetDir, IS_BUILD_DIRECTORY, null, null));

  }

}
