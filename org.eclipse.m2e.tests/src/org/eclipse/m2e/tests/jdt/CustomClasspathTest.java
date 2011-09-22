/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class CustomClasspathTest extends AbstractMavenProjectTestCase {

  public void testImport() throws Exception {
    IProject project = importProject("customclasspath-p001", "projects/customclasspath/p001",
        new ResolverConfiguration());
    assertNoErrors(project);

    IJavaProject javaProject = JavaCore.create(project);

    IClasspathEntry[] cp = javaProject.getRawClasspath();

    assertEquals(4, cp.length);
    assertEquals("/customclasspath-p001/src/main/java", cp[0].getPath().toPortableString());
    assertEquals("/customclasspath-p001/src/main/java2", cp[1].getPath().toPortableString());
    assertEquals(
        "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/J2SE-1.4",
        cp[2].getPath().toPortableString());
    assertEquals("org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", cp[3].getPath().toPortableString());
  }

  public void testStaleDerivedEntries() throws Exception {
    IProject project = importProject("customclasspath-p001", "projects/customclasspath/p001",
        new ResolverConfiguration());
    assertNoErrors(project);
    copyContent(project, "pom-custom-source-root.xml", "pom.xml");

    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);

    IJavaProject javaProject = JavaCore.create(project);

    IClasspathEntry[] cp = javaProject.getRawClasspath();

    assertEquals(4, cp.length);
    assertEquals("/customclasspath-p001/src/main/java2", cp[0].getPath().toPortableString());
    assertEquals("/customclasspath-p001/src/main/java3", cp[1].getPath().toPortableString());
    assertEquals(
        "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/J2SE-1.4",
        cp[2].getPath().toPortableString());
    assertEquals("org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", cp[3].getPath().toPortableString());
  }
}
