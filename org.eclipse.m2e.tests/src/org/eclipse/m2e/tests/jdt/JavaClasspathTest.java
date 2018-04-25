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

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.internal.BuildPathManager;
import org.eclipse.m2e.jdt.internal.ClasspathDescriptor;
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

    updateProjectConfiguration(project);
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

    updateProjectConfiguration(project);

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

    updateProjectConfiguration(project);

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

    updateProjectConfiguration(project);

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

    updateProjectConfiguration(project);

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
    updateProjectConfiguration(project);
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

  public void test525880_JavaAccessRulesArePreservedOnUpdate() throws Exception {
    IProject project = importProject("projects/525880_java_pom_with_access_rules/pom.xml");
    addAccessRulesToClasspathContainers(JavaCore.create(project));
    assertContainersHaveAccessRules(JavaCore.create(project));

    updateProjectConfiguration(project);

    assertContainersHaveAccessRules(JavaCore.create(project));
  }

  public void test518218_configureMethodParametersGenerationWithParameters() throws Exception {
    testMethodParametersGeneration("projects/518218/parameters/pom.xml");
  }

  public void test518218_configureMethodParametersGenerationWithCompilerArgs() throws Exception {
    testMethodParametersGeneration("projects/518218/compilerArgs/pom.xml");
  }

  public void test518218_configureMethodParametersGenerationWithCompilerArgument() throws Exception {
    testMethodParametersGeneration("projects/518218/compilerArgument/pom.xml");
  }

  private void testMethodParametersGeneration(String projectPath) throws Exception {
    IProject project = importProject(projectPath);
    IJavaProject javaProject = JavaCore.create(project);
    assertEquals(JavaCore.GENERATE, javaProject.getOption(JavaCore.COMPILER_CODEGEN_METHOD_PARAMETERS_ATTR, false));
  }

  public void test534049_SetReleaseFlag() throws Exception {
    IProject project = importProject("projects/release-flag/pom.xml");
    IJavaProject javaProject = JavaCore.create(project);
    assertEquals(JavaCore.DISABLED, javaProject.getOption(JavaCore.COMPILER_RELEASE, false));

    copyContent(project, "pom-release.xml", "pom.xml");
    updateProjectConfiguration(project);
    javaProject = JavaCore.create(project);
    assertEquals(JavaCore.ENABLED, javaProject.getOption(JavaCore.COMPILER_RELEASE, false));
  }

  public void test526858_TestClasspathAttributeOnJar() throws Exception {
    IProject project = importProject("projects/526858-test-classpath/jar-dependencies/pom.xml");

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] cp = javaProject.getRawClasspath();

    assertEquals(4, cp.length);

    assertClasspath(new String[] {//
        "/jar-dependencies/src/main/java", //
        "/jar-dependencies/src/test/java", //
        "org.eclipse.jdt.launching.JRE_CONTAINER/.*", //
        "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER" //
    }, cp);
    assertNotTest(cp[0]);
    assertTest(cp[1]);

    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals("" + Arrays.asList(classpathEntries), 5, classpathEntries.length);
    //test dependency
    assertEquals("junit-3.8.1.jar", classpathEntries[0].getPath().lastSegment());
    assertTest(classpathEntries[0]);
    //compile dependency
    assertEquals("commons-io-2.5.jar", classpathEntries[1].getPath().lastSegment());
    assertNotTest(classpathEntries[1]);
    //test + transitive test dependencies
    assertEquals("commons-beanutils-1.6.jar", classpathEntries[2].getPath().lastSegment());
    assertTest(classpathEntries[2]);
    assertEquals("commons-logging-1.0.jar", classpathEntries[3].getPath().lastSegment());
    assertTest(classpathEntries[3]);
    assertEquals("commons-collections-2.0.jar", classpathEntries[4].getPath().lastSegment());
    assertTest(classpathEntries[4]);
  }

  public void test526858_TestClasspathAttributeOnProject() throws Exception {
    IProject[] projects = importProjects("projects/526858-test-classpath/",
        new String[] {"jar-dependencies/pom.xml", "project-dependencies-1/pom.xml"}, new ResolverConfiguration());

    IJavaProject javaProject = JavaCore.create(projects[1]);

    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals("" + Arrays.asList(classpathEntries), 2, classpathEntries.length);
    //project dependency
    assertEquals("jar-dependencies", classpathEntries[0].getPath().lastSegment());
    assertTest(classpathEntries[0]);
    assertDoesntExportTests(classpathEntries[0]);

    //transitive test dependency
    assertEquals("commons-io-2.5.jar", classpathEntries[1].getPath().lastSegment());
    assertTest(classpathEntries[1]);
  }

  public void test526858_ProjectExportingTests() throws Exception {
    IProject[] projects = importProjects("projects/526858-test-classpath/",
        new String[] {"jar-dependencies/pom.xml", "project-dependencies-2/pom.xml"}, new ResolverConfiguration());

    IJavaProject javaProject = JavaCore.create(projects[1]);

    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals("" + Arrays.asList(classpathEntries), 2, classpathEntries.length);
    //project dependency
    assertEquals("jar-dependencies", classpathEntries[0].getPath().lastSegment());
    assertNotTest(classpathEntries[0]);
    assertExportsTests(classpathEntries[0]);

    //transitive dependency
    assertEquals("commons-io-2.5.jar", classpathEntries[1].getPath().lastSegment());
    assertNotTest(classpathEntries[1]);
  }

  private void assertTest(IClasspathEntry entry) {
    IClasspathAttribute cpAttr = getClasspathAttribute(entry, IClasspathManager.TEST_ATTRIBUTE);
    assertNotNull(entry.getPath().lastSegment() + " is missing the test attribute", cpAttr);
    assertEquals(entry.getPath().lastSegment() + " is missing the test attribute", "true", cpAttr.getValue());
  }

  private void assertNotTest(IClasspathEntry entry) {
    IClasspathAttribute cpAttr = getClasspathAttribute(entry, IClasspathManager.TEST_ATTRIBUTE);
    assertNull(entry.getPath().lastSegment() + " should not have the test attribute", cpAttr);
  }

  private void assertExportsTests(IClasspathEntry entry) {
    IClasspathAttribute cpAttr = getClasspathAttribute(entry, IClasspathManager.WITHOUT_TEST_CODE);
    assertNull(entry.getPath().lastSegment() + " should not have the without_test_code attribute", cpAttr);
  }

  private void assertDoesntExportTests(IClasspathEntry entry) {
    IClasspathAttribute cpAttr = getClasspathAttribute(entry, IClasspathManager.WITHOUT_TEST_CODE);
    assertNotNull(entry.getPath().lastSegment() + " is missing the without_test_code attribute", cpAttr);
    assertEquals(entry.getPath().lastSegment() + " is missing the without_test_code attribute", "true",
        cpAttr.getValue());
  }

  private void updateProjectConfiguration(IProject project) throws CoreException, InterruptedException {
    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);
    waitForJobsToComplete();
  }

  private void assertContainersHaveAccessRules(IJavaProject javaProject) throws JavaModelException {
    List<IClasspathEntry> containerEntries = Stream.of(javaProject.getRawClasspath())
        .filter(mavenClasspathEntry()).collect(Collectors.toList());
    assertEquals(1, containerEntries.size());

    containerEntries.forEach(entry -> {
      assertNotNull(entry.getAccessRules());
      List<IAccessRule> accessRules = Arrays.asList(entry.getAccessRules());
      assertEquals(1, accessRules.size());
      accessRules.forEach(accessRule -> {
        assertEquals(IAccessRule.K_DISCOURAGED, accessRule.getKind());
        assertEquals("**/internal/**", accessRule.getPattern().toPortableString());
      });
    });
  }

  private void addAccessRulesToClasspathContainers(IJavaProject javaProject)
      throws JavaModelException, InterruptedException, CoreException {
    IClasspathDescriptor classpath = new ClasspathDescriptor(javaProject);

    List<IClasspathEntryDescriptor> containerEntries = classpath.getEntryDescriptors().stream()
        .filter(mavenClasspathEntryDescriptor())
        .collect(Collectors.toList());
    assertEquals(1, containerEntries.size());

    containerEntries.forEach(this::addAccessRule);

    List<IClasspathEntry> updatedEntries = classpath.getEntryDescriptors().stream().map(e -> e.toClasspathEntry())
        .collect(Collectors.toList());
    javaProject.setRawClasspath(updatedEntries.toArray(new IClasspathEntry[0]), monitor);
    waitForJobsToComplete();
  }

  private Predicate<IClasspathEntry> mavenClasspathEntry() {
    return entry -> entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER
        && entry.getPath().toPortableString().equals(IClasspathManager.CONTAINER_ID);
  }

  private Predicate<IClasspathEntryDescriptor> mavenClasspathEntryDescriptor() {
    return entry -> entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER
        && entry.getPath().toPortableString().equals(IClasspathManager.CONTAINER_ID);
  }

  private void addAccessRule(IClasspathEntryDescriptor entry) {
    entry.addAccessRule(JavaCore.newAccessRule(Path.fromPortableString("**/internal/**"), IAccessRule.K_DISCOURAGED));
  }
}
