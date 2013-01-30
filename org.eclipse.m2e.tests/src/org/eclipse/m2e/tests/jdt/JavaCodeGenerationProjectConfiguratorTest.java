/*******************************************************************************
 * Copyright (c) 2012 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.jdt;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.ClasspathHelpers;


public class JavaCodeGenerationProjectConfiguratorTest extends AbstractMavenProjectTestCase {

  public void test368333_generatedSourcesFoldersAreNotRemovedDuringConfigurationUpdate() throws Exception {
    IProject project = importProject("368333_missingGeneratedSourceFolders",
        "projects/368333_missingGeneratedSourceFolders", new ResolverConfiguration());
    assertNoErrors(project);

    IJavaProject javaProject = JavaCore.create(project);

    ClasspathHelpers.assertClasspath(new String[] {//
        "/368333_missingGeneratedSourceFolders/src/main/java", //
            "/368333_missingGeneratedSourceFolders/src/test/java", //
            "org.eclipse.jdt.launching.JRE_CONTAINER/.*", //
            "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", //
            "/368333_missingGeneratedSourceFolders/target/generated-sources/test",//
        }, //
        javaProject.getRawClasspath());

    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);

    ClasspathHelpers.assertClasspath(new String[] {//
        "/368333_missingGeneratedSourceFolders/src/main/java", //
            "/368333_missingGeneratedSourceFolders/src/test/java", //
            "org.eclipse.jdt.launching.JRE_CONTAINER/.*", //
            "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", //
            "/368333_missingGeneratedSourceFolders/target/generated-sources/test",//
        }, //
        javaProject.getRawClasspath());
  }

}
