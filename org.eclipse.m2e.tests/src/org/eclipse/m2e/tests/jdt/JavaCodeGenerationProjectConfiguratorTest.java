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
import static org.eclipse.m2e.tests.common.ClasspathHelpers.getClasspathEntry;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class JavaCodeGenerationProjectConfiguratorTest extends AbstractMavenProjectTestCase {

  public void test368333_generatedSourcesFoldersAreNotRemovedDuringConfigurationUpdate() throws Exception {
    IProject project = importProject("368333_missingGeneratedSourceFolders",
        "projects/368333_missingGeneratedSourceFolders", new ResolverConfiguration());
    assertNoErrors(project);

    IJavaProject javaProject = JavaCore.create(project);

    assertClasspath(new String[] {//
        "/368333_missingGeneratedSourceFolders/src/main/java", //
            "/368333_missingGeneratedSourceFolders/src/test/java", //
            "org.eclipse.jdt.launching.JRE_CONTAINER/.*", //
            "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", //
            "/368333_missingGeneratedSourceFolders/target/generated-sources/test",//
        }, //
        javaProject.getRawClasspath());

    assertClasspathAttribute(javaProject.getRawClasspath(), "/368333_missingGeneratedSourceFolders/src/main/java",
        IClasspathAttribute.IGNORE_OPTIONAL_PROBLEMS, null);
    assertClasspathAttribute(javaProject.getRawClasspath(),
        "/368333_missingGeneratedSourceFolders/target/generated-sources/test",
        IClasspathAttribute.IGNORE_OPTIONAL_PROBLEMS, "true");

    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);

    assertClasspath(new String[] {//
        "/368333_missingGeneratedSourceFolders/src/main/java", //
            "/368333_missingGeneratedSourceFolders/src/test/java", //
            "org.eclipse.jdt.launching.JRE_CONTAINER/.*", //
            "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", //
            "/368333_missingGeneratedSourceFolders/target/generated-sources/test",//
        }, //
        javaProject.getRawClasspath());

    assertClasspathAttribute(javaProject.getRawClasspath(), "/368333_missingGeneratedSourceFolders/src/main/java",
        IClasspathAttribute.IGNORE_OPTIONAL_PROBLEMS, null);
    assertClasspathAttribute(javaProject.getRawClasspath(),
        "/368333_missingGeneratedSourceFolders/target/generated-sources/test",
        IClasspathAttribute.IGNORE_OPTIONAL_PROBLEMS, "true");
  }

  private void assertClasspathAttribute(IClasspathEntry[] cp, String path, String name, String expectedValue) {
    IClasspathAttribute attribute = getClasspathAttribute(getClasspathEntry(cp, path), name);
    String value = attribute != null ? attribute.getValue() : null;
    assertEquals(expectedValue, value);
  }
}
