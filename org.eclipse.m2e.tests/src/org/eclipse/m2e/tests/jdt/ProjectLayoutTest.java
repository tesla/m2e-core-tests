/*******************************************************************************
 * Copyright (c) 2015 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.jdt;

import org.junit.Test;

import org.eclipse.core.resources.IProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.ClasspathHelpers;


public class ProjectLayoutTest extends AbstractMavenProjectTestCase {
  @Test
  public void test472726_basedirResources() throws Exception {
    IProject project = importProject("projects/472726_basedirResources/pom.xml");
    waitForJobsToComplete();
    assertNoErrors(project);
    ClasspathHelpers.assertClasspath(project //
        , "/472726_basedirResources/src/main/java" //
        , "/472726_basedirResources/src/main/resources" //
        , "/472726_basedirResources/src/test/java" //
        , "/472726_basedirResources/src/test/resources" //
        , "org.eclipse.jdt.launching.JRE_CONTAINER/.*" //
        , "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER" //
    );

    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);
    assertNoErrors(project);

  }
}
