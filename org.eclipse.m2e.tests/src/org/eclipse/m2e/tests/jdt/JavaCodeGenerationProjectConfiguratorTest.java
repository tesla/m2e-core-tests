/*******************************************************************************
 * Copyright (c) 2012 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.jdt;

import static org.eclipse.m2e.tests.common.ClasspathHelpers.assertClasspath;
import static org.eclipse.m2e.tests.common.ClasspathHelpers.getClasspathAttribute;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class JavaCodeGenerationProjectConfiguratorTest extends AbstractMavenProjectTestCase {
  @Test
  public void test368333_generatedSourcesFoldersAreNotRemovedDuringConfigurationUpdate() throws Exception {
    IProject project = importProject("368333_missingGeneratedSourceFolders",
        "projects/368333_missingGeneratedSourceFolders", new ResolverConfiguration());
    assertNoErrors(project);

    String srcMain = "/368333_missingGeneratedSourceFolders/src/main/java";
    String srcGeneratedTest = "/368333_missingGeneratedSourceFolders/target/generated-sources/test";
    Map<String, IClasspathEntry> map = assertClasspath(project,
        srcMain, //
            "/368333_missingGeneratedSourceFolders/src/test/java", //
            "org.eclipse.jdt.launching.JRE_CONTAINER/.*", //
            "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", //
        srcGeneratedTest);

    assertClasspathAttribute(map, srcMain,
        IClasspathAttribute.IGNORE_OPTIONAL_PROBLEMS, null);
    assertClasspathAttribute(map,
        srcGeneratedTest,
        IClasspathAttribute.IGNORE_OPTIONAL_PROBLEMS, "true");

    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);

    map = assertClasspath(project,
        srcMain, //
            "/368333_missingGeneratedSourceFolders/src/test/java", //
            "org.eclipse.jdt.launching.JRE_CONTAINER/.*", //
            "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", //
        srcGeneratedTest);

    assertClasspathAttribute(map, srcMain,
        IClasspathAttribute.IGNORE_OPTIONAL_PROBLEMS, null);
    assertClasspathAttribute(map,
        srcGeneratedTest,
        IClasspathAttribute.IGNORE_OPTIONAL_PROBLEMS, "true");
  }

  private void assertClasspathAttribute(Map<String, IClasspathEntry> map, String path, String name,
      String expectedValue) {
    IClasspathAttribute attribute = getClasspathAttribute(map.get(path), name);
    String value = attribute != null ? attribute.getValue() : null;
    assertEquals(expectedValue, value);
  }
}
