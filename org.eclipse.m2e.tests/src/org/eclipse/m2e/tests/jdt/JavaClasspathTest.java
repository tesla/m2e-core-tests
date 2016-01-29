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

import static org.eclipse.m2e.tests.common.ClasspathHelpers.assertClasspath;
import static org.eclipse.m2e.tests.common.ClasspathHelpers.getClasspathAttribute;
import static org.eclipse.m2e.tests.common.ClasspathHelpers.getClasspathEntry;

import org.junit.Assert;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.internal.MavenClasspathHelpers;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


public class JavaClasspathTest extends AbstractMavenProjectTestCase {

  public void testImport() throws Exception {
    IProject project = importProject("customclasspath-p001", "projects/customclasspath/p001",
        new ResolverConfiguration());
    assertNoErrors(project);

    IJavaProject javaProject = JavaCore.create(project);

    IClasspathEntry[] cp = javaProject.getRawClasspath();

    assertEquals(5, cp.length);
    assertEquals("/customclasspath-p001/src/main/java", cp[0].getPath().toPortableString()); // <= main sources
    assertEquals("/customclasspath-p001/src/main/java2", cp[1].getPath().toPortableString()); // <= custom entry
    assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", cp[2].getPath().segment(0));
    assertEquals("org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", cp[3].getPath().segment(0));
    assertEquals("/customclasspath-p001/src/test/java", cp[4].getPath().toPortableString()); // <= test sources
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

    assertEquals(5, cp.length);

    // stale pom-derived classpath entries are removed after new entries have been added to the classpath
    // this results in unexpected classpath order, i.e. compile sources appear *after* test sources in some cases

    assertEquals("/customclasspath-p001/src/main/java2", cp[0].getPath().toPortableString()); // <= custom entry
    assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", cp[1].getPath().segment(0));
    assertEquals("org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", cp[2].getPath().segment(0));
    assertEquals("/customclasspath-p001/src/test/java", cp[3].getPath().toPortableString()); // <= test sources
    assertEquals("/customclasspath-p001/src/main/java3", cp[4].getPath().toPortableString()); // <= main sources
  }

  public void testClasspathContainers() throws Exception {
    IProject project = importProject("customclasspath-classpath-containers",
        "projects/customclasspath/classpath-containers", new ResolverConfiguration());

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] cp = javaProject.getRawClasspath();

    assertEquals(5, cp.length);

    assertClasspath(new String[] {//
        "org.eclipse.jdt.launching.JRE_CONTAINER/.*",//
            "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER",//
            "container1",//
            "/customclasspath-classpath-containers/src/main/java", //
            "/customclasspath-classpath-containers/src/test/java" //
        }, cp);

    Assert
        .assertNull(getClasspathAttribute(getClasspathEntry(cp, "container1"), IClasspathManager.POMDERIVED_ATTRIBUTE));
  }

  public void test370685_PreserveResourcesOnUpdate() throws Exception {
    IProject project = importProject("projects/370685_missingResources/pom.xml");
    //assertNoErrors(project);

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] originalCp = javaProject.getRawClasspath();

    assertEquals(5, originalCp.length);
    assertEquals("/project/src/main/java", originalCp[0].getPath().toPortableString());
    assertEquals("/project/src/main/resources", originalCp[1].getPath().toPortableString());
    assertEquals("/project/src/test/java", originalCp[2].getPath().toPortableString());
    assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", originalCp[3].getPath().segment(0));
    assertEquals("org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", originalCp[4].getPath().segment(0));

    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);
    waitForJobsToComplete();
    //assertNoErrors(project);

    javaProject = JavaCore.create(project);
    IClasspathEntry[] updatedCp = javaProject.getRawClasspath();
    assertEquals("classpath changed on update", originalCp.length, updatedCp.length);
    for(int i = 0; i < originalCp.length; i++ ) {
      assertEquals("classpath entry changed", originalCp[i], updatedCp[i]);
    }
  }

  public void test486739_PreserveExportedOnUpdate() throws Exception {
    IProject project = importProject("projects/486739_exportedContainer/pom.xml");
    //assertNoErrors(project);

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] originalCp = javaProject.getRawClasspath();

    assertEquals(5, originalCp.length);
    assertEquals("org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", originalCp[4].getPath().segment(0));
    assertFalse(originalCp[4].isExported());

    originalCp[4] = MavenClasspathHelpers.getDefaultContainerEntry(true);
    javaProject.setRawClasspath(originalCp, monitor);

    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);
    waitForJobsToComplete();
    //assertNoErrors(project);

    javaProject = JavaCore.create(project);
    IClasspathEntry[] updatedCp = javaProject.getRawClasspath();
    assertEquals("classpath changed on update", originalCp.length, updatedCp.length);
    assertEquals("org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", updatedCp[4].getPath().segment(0));
    assertTrue(updatedCp[4].isExported());
  }

  public void test398121_PreserveOrderOfClasspathContainersOnUpdate() throws Exception {
    IProject project = importProject("orderOf-classpath-containers", "projects/customclasspath/classpath-containers",
        new ResolverConfiguration());

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] cp = javaProject.getRawClasspath();

    assertEquals(5, cp.length);

    assertClasspath(new String[] {//
        "org.eclipse.jdt.launching.JRE_CONTAINER/.*",//
            "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER",//
            "container1",//
            "/orderOf-classpath-containers/src/main/java", //
            "/orderOf-classpath-containers/src/test/java" //
        }, cp);

    // Simulate user changes the order or the classpath entries. The order should be preserved during update if nothing changes in the pom.xml

    IClasspathEntry[] newOrder = new IClasspathEntry[] {cp[2], cp[3], cp[1], cp[0], cp[4]};

    javaProject.setRawClasspath(newOrder, null);

    // Update the project
    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);
    waitForJobsToComplete();

    cp = javaProject.getRawClasspath();

    assertEquals(5, cp.length);

    assertClasspath(new String[] {//
        "container1",//
            "/orderOf-classpath-containers/src/main/java", //
            "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER",//
            "org.eclipse.jdt.launching.JRE_CONTAINER/.*",//
            "/orderOf-classpath-containers/src/test/java" //
        }, cp);

    // Simulate another change to the classpath

    newOrder = new IClasspathEntry[] {cp[0], cp[1], cp[3], cp[2], cp[4]};

    javaProject.setRawClasspath(newOrder, null);

    // Update the project
    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);
    waitForJobsToComplete();

    cp = javaProject.getRawClasspath();

    assertEquals(5, cp.length);

    assertClasspath(new String[] {//
        "container1",//
            "/orderOf-classpath-containers/src/main/java", //
            "org.eclipse.jdt.launching.JRE_CONTAINER/.*",//
            "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER",//
            "/orderOf-classpath-containers/src/test/java" //
        }, cp);

  }

  public void test394042_ClasspathEntry4() throws Exception {
    IProject project = importProject("projects/394042_ClasspathEntry4/pom.xml");
    assertNoErrors(project);

    IJavaProject javaProject = JavaCore.create(project);

    IClasspathEntry[] cp = javaProject.getRawClasspath();

    assertEquals(cp.toString(), 5, cp.length);

    assertClasspath(new String[] {//
        "M2_REPO/junit/junit/3.8.1/junit-3.8.1.jar",//
            "org.eclipse.jdt.launching.JRE_CONTAINER/.*",//
            "/394042_ClasspathEntry4/src/main/java", //
            "/394042_ClasspathEntry4/src/test/java", //
            "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER",//
        }, cp);

    //Check Variable classpath entry attributes/accessrules are preserved
    IClasspathEntry junit = cp[0];

    assertEquals("/foo/bar/sources.jar", junit.getSourceAttachmentPath().toPortableString());

    assertEquals(2, junit.getExtraAttributes().length);
    assertEquals("file:/foo/bar/javadoc/", getClasspathAttribute(junit, "javadoc_location").getValue());
    assertEquals("UTF-8", getClasspathAttribute(junit, "source_encoding").getValue());

    assertEquals(1, junit.getAccessRules().length);
    assertEquals("foo/bar/**", junit.getAccessRules()[0].getPattern().toPortableString());
  }

  public void test466518_classpathJREOrder() throws Exception {
    IProject project = importProject("projects/466518_classpathJREOrder/pom.xml");
    WorkspaceHelpers.assertNoErrors(project);

    IClasspathEntry[] cp = JavaCore.create(project).getRawClasspath();

    assertClasspath(new String[] {//
        "/466518-project/src/main/java", //
            "/466518-project/src/test/java", //
            "org.eclipse.jdt.launching.JRE_CONTAINER/.*",//
            "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER",//
        }, cp);

    copyContent(project, "pom_changed.xml", "pom.xml");
    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(project);

    cp = JavaCore.create(project).getRawClasspath();

    assertClasspath(new String[] {//
        "/466518-project/src/main/java", //
        "/466518-project/src/test/java", //
        "org.eclipse.jdt.launching.JRE_CONTAINER/.*",//
        "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER",//
    }, cp);
  }

  public void test388541_KeepClasspathAttributes() throws Exception {
    IProject project = importProject("projects/388541/pom.xml");
    assertNoErrors(project);

    IJavaProject javaProject = JavaCore.create(project);

    IClasspathEntry[] cp = javaProject.getRawClasspath();

    assertEquals(cp.toString(), 5, cp.length);

    assertClasspath(new String[] {//
        "M2_REPO/junit/junit/3.8.1/junit-3.8.1.jar", //
        "org.eclipse.jdt.launching.JRE_CONTAINER/.*", //
        "/388541/src/main/java", //
        "/388541/src/test/java", //
        "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER",//
    }, cp);

    //Check Variable classpath entry attributes/accessrules are preserved
    IClasspathEntry javaSource = cp[2];

    assertEquals("foo value should not change", "bar", getClasspathAttribute(javaSource, "foo").getValue());
    assertEquals("ignore_option_problems value should not change", "true",
        getClasspathAttribute(javaSource, "ignore_option_problems").getValue());
    assertEquals("maven.pomderived value should change", "true",
        getClasspathAttribute(javaSource, "maven.pomderived").getValue());
    assertNull("foobar value should have been removed", getClasspathAttribute(javaSource, "foobar"));

  }

  public void test480137_addingResourceFoldersAreNotRemovedDuringConfigurationUpdate() throws Exception {
    IProject project = importProject("480137_addingResourceFoldersUnstable",
        "projects/480137_addingResourceFoldersUnstable", new ResolverConfiguration());
    assertNoErrors(project);

    IJavaProject javaProject = JavaCore.create(project);

    assertClasspath(new String[] {//
        "/480137_addingResourceFoldersUnstable/src/main/java", //
        "/480137_addingResourceFoldersUnstable/src/main/js", //
        "/480137_addingResourceFoldersUnstable/src/test/java", //
        "/480137_addingResourceFoldersUnstable/src/test/js", //
        "org.eclipse.jdt.launching.JRE_CONTAINER/.*", //
        "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", //
    }, //
        javaProject.getRawClasspath());

    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);

    assertClasspath(new String[] {//
        "/480137_addingResourceFoldersUnstable/src/main/java", //
        "/480137_addingResourceFoldersUnstable/src/main/js", //
        "/480137_addingResourceFoldersUnstable/src/test/java", //
        "/480137_addingResourceFoldersUnstable/src/test/js", //
        "org.eclipse.jdt.launching.JRE_CONTAINER/.*", //
        "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", //
    }, //
        javaProject.getRawClasspath());
  }
}
