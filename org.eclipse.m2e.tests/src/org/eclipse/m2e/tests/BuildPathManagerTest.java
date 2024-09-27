/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype, Inc.
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

package org.eclipse.m2e.tests;

import static org.eclipse.m2e.tests.common.ClasspathHelpers.assertClasspath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.preferences.MavenConfigurationImpl;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.archetype.MavenArchetype;
import org.eclipse.m2e.jdt.MavenJdtPlugin;
import org.eclipse.m2e.jdt.internal.BuildPathManager;
import org.eclipse.m2e.jdt.internal.MavenClasspathHelpers;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.ClasspathHelpers;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


/**
 * @author Eugene Kuleshov
 */
public class BuildPathManagerTest extends AbstractMavenProjectTestCase {

  private ProjectRegistryManager manager;

  private boolean initialDownloadSources;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    manager = MavenPluginActivator.getDefault().getMavenProjectManagerImpl();
    initialDownloadSources = mavenConfiguration.isDownloadSources();
    ((MavenConfigurationImpl) mavenConfiguration).setDownloadSources(false);
  }

  @Override
  @After
  public void tearDown() throws Exception {
    ((MavenConfigurationImpl) mavenConfiguration).setDownloadSources(initialDownloadSources);
    manager = null;
    super.tearDown();
  }

  @Test
  public void testEnableMavenNature() throws Exception {
    deleteProject("MNGECLIPSE-248parent");
    deleteProject("MNGECLIPSE-248child");

    final IProject project1 = createProject("MNGECLIPSE-248parent", "projects/MNGECLIPSE-248parent/pom.xml");
    final IProject project2 = createProject("MNGECLIPSE-248child", "projects/MNGECLIPSE-248child/pom.xml");

    NullProgressMonitor monitor = new NullProgressMonitor();
    IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();

    ResolverConfiguration configuration = new ResolverConfiguration();
    configurationManager.enableMavenNature(project1, configuration, monitor);
//    buildpathManager.updateSourceFolders(project1, monitor);

    configurationManager.enableMavenNature(project2, configuration, monitor);
//    buildpathManager.updateSourceFolders(project2, monitor);

//    waitForJob("Initializing " + project1.getProject().getName());
//    waitForJob("Initializing " + project2.getProject().getName());

    try {
      project1.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
      project2.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    } catch(Exception ex) {
      throw ex;
    }
    waitForJobsToComplete();

    WorkspaceHelpers.assertNoErrors(project1);

//    IClasspathEntry[] project1entries = getMavenContainerEntries(project1);
//    assertEquals(1, project1entries.length);
//    assertEquals(IClasspathEntry.CPE_LIBRARY, project1entries[0].getEntryKind());
//    assertEquals("junit-4.1.jar", project1entries[0].getPath().lastSegment());

    IClasspathEntry[] project2entries = getMavenContainerEntries(project2);
    assertEquals(3, project2entries.length);
    assertEquals(IClasspathEntry.CPE_PROJECT, project2entries[0].getEntryKind());
    assertEquals("MNGECLIPSE-248parent", project2entries[0].getPath().segment(0));
    assertEquals(IClasspathEntry.CPE_LIBRARY, project2entries[1].getEntryKind());
    assertEquals("junit-4.13.1.jar", project2entries[1].getPath().lastSegment());

    configurationManager.updateProjectConfiguration(project2, monitor);
    waitForJobsToComplete();

    WorkspaceHelpers.assertNoErrors(project2);
  }

  @Test
  public void testDisableMavenNature() throws Exception {
    deleteProject("disablemaven-p001");
    IProject p = createExisting("disablemaven-p001", "projects/disablemaven/p001");
    waitForJobsToComplete();

    assertTrue(p.hasNature(IMavenConstants.NATURE_ID));
    assertTrue(p.hasNature(JavaCore.NATURE_ID));
    assertNotNull(BuildPathManager.getMaven2ClasspathContainer(JavaCore.create(p)));

    IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();
    configurationManager.disableMavenNature(p, monitor);
    waitForJobsToComplete();

    assertFalse(p.hasNature(IMavenConstants.NATURE_ID));
    assertFalse(hasBuilder(p, IMavenConstants.BUILDER_ID));
    assertTrue(p.hasNature(JavaCore.NATURE_ID));
    assertNull(BuildPathManager.getMaven2ClasspathContainer(JavaCore.create(p)));
  }

  private boolean hasBuilder(IProject p, String builderId) throws CoreException {
    for(ICommand command : p.getDescription().getBuildSpec()) {
      if(builderId.equals(command.getBuilderName())) {
        return true;
      }
    }
    return false;
  }

  @Test
  public void testEnableMavenNatureWithNoWorkspace() throws Exception {
    deleteProject("MNGECLIPSE-248parent");
    deleteProject("MNGECLIPSE-248child");

    final IProject project1 = createProject("MNGECLIPSE-248parent", "projects/MNGECLIPSE-248parent/pom.xml");
    final IProject project2 = createProject("MNGECLIPSE-248child", "projects/MNGECLIPSE-248child/pom.xml");

    IProjectConfigurationManager importManager = MavenPlugin.getProjectConfigurationManager();

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setResolveWorkspaceProjects(false);
    configuration.setSelectedProfiles("");

    importManager.enableMavenNature(project1, configuration, monitor);
    importManager.enableMavenNature(project2, configuration, monitor);
    waitForJobsToComplete();

    IClasspathEntry[] project2entries = getMavenContainerEntries(project2);
    assertEquals(Arrays.asList(project2entries).toString(), 1, project2entries.length);
    assertEquals(IClasspathEntry.CPE_LIBRARY, project2entries[0].getEntryKind());
    assertEquals("MNGECLIPSE-248parent-1.0.0.jar", project2entries[0].getPath().lastSegment());
  }

  @Test
  public void testProjectImportWithProfile1() throws Exception {
    deleteProject("MNGECLIPSE-353");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setResolveWorkspaceProjects(true);
    configuration.setSelectedProfiles("jaxb1");

    IProject project = importProject("projects/MNGECLIPSE-353/pom.xml", configuration);

    waitForJobsToComplete();

    IClasspathEntry[] classpathEntries = getClasspathEntries(project);
    assertEquals("" + Arrays.asList(classpathEntries), 3, classpathEntries.length);
    assertEquals("junit-4.13.1.jar", classpathEntries[0].getPath().lastSegment());
    assertEquals("jaxb-api-1.5.jar", classpathEntries[2].getPath().lastSegment());

    WorkspaceHelpers.assertNoErrors(project);
  }

  @Test
  public void testProjectImportWithProfile2() throws Exception {
    deleteProject("MNGECLIPSE-353");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setResolveWorkspaceProjects(true);
    configuration.setSelectedProfiles("jaxb20");

    IProject project = importProject("projects/MNGECLIPSE-353/pom.xml", configuration);

    waitForJobsToComplete();

    IClasspathEntry[] classpathEntries = getClasspathEntries(project);
    assertEquals("" + Arrays.asList(classpathEntries), 5, classpathEntries.length);
    assertEquals("junit-4.13.1.jar", classpathEntries[0].getPath().lastSegment());
    assertEquals("jaxb-api-2.0.jar", classpathEntries[2].getPath().lastSegment());
    assertEquals("jsr173_api-1.0.jar", classpathEntries[3].getPath().lastSegment());
    assertEquals("activation-1.1.jar", classpathEntries[4].getPath().lastSegment());

    WorkspaceHelpers.assertNoErrors(project);
  }

  @Test
  public void testProjectImport001_useMavenOutputFolders() throws Exception {
    deleteProject("projectimport-p001");

    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject project = importProject("projectimport-p001", "projects/projectimport/p001", configuration);

    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);

    String outputPath = "/projectimport-p001/target/classes";
    assertEquals(IPath.fromOSString(outputPath), javaProject.getOutputLocation());

    String srcMain = "/projectimport-p001/src/main/java";
    String srcTest = "/projectimport-p001/src/test/java";
    Map<String, IClasspathEntry> map = ClasspathHelpers.assertClasspath(project, srcMain, srcTest);
    IClasspathEntry cp0 = map.get(srcMain);
    IClasspathEntry cp1 = map.get(srcTest);

    assertEquals(IPath.fromOSString(outputPath), cp0.getOutputLocation());
    assertEquals(IPath.fromOSString("/projectimport-p001/target/test-classes"), cp1.getOutputLocation());
  }

  // disabled nested modules tests 
  @Test
  @Ignore
  public void _testProjectImport002_useMavenOutputFolders() throws Exception {
    deleteProject("projectimport-p002");

    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject project = importProject("projectimport-p002", "projects/projectimport/p002", configuration);

    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);

    assertEquals(IPath.fromOSString("/projectimport-p002/target/classes"), javaProject.getOutputLocation());
    IClasspathEntry[] cp = javaProject.getRawClasspath();

    assertEquals(3, cp.length);
    assertEquals(IPath.fromOSString("/projectimport-p002/p002-m1/src/main/java"), cp[0].getPath());
    assertEquals(IPath.fromOSString("/projectimport-p002/p002-m1/target/classes"), cp[0].getOutputLocation());
  }

  @Test
  public void testClasspathOrderWorkspace001() throws Exception {
    deleteProject("p1");
    deleteProject("p2");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setResolveWorkspaceProjects(true);
    configuration.setSelectedProfiles("");

    IProject project1 = importProject("projects/dependencyorder/p1/pom.xml", configuration);
    IProject project2 = importProject("projects/dependencyorder/p2/pom.xml", configuration);
    project1.build(IncrementalProjectBuilder.FULL_BUILD, null);
    project2.build(IncrementalProjectBuilder.FULL_BUILD, null);
    waitForJobsToComplete();

//    MavenPlugin.getBuildpathManager().updateClasspathContainer(p1, new NullProgressMonitor());

    IClasspathEntry[] cp = getClasspathEntries(project1);

    // order according to mvn -X
    assertEquals(4, cp.length);
    assertEquals(IPath.fromOSString("/p2"), cp[0].getPath());
    assertEquals("junit-4.13.1.jar", cp[2].getPath().lastSegment());
    assertEquals("easymock-1.0.jar", cp[1].getPath().lastSegment());

    Set<Artifact> artifacts = getMavenProjectArtifacts(project1);
    assertEquals(4, artifacts.size());
    Artifact a1 = artifacts.iterator().next();
    assertEquals(project2.getFile("pom.xml").getLocation().toFile(), a1.getFile());
  }

  @Test
  public void testClasspathOrderWorkspace003() throws Exception {
    deleteProject("p3");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setResolveWorkspaceProjects(true);
    configuration.setSelectedProfiles("");

    IProject p3 = importProject("projects/dependencyorder/p3/pom.xml", configuration);
    p3.build(IncrementalProjectBuilder.FULL_BUILD, null);
    waitForJobsToComplete();

    IClasspathEntry[] cp = getClasspathEntries(p3);

    // order according to mvn -X. note that maven 2.0.7 and 2.1-SNAPSHOT produce different order 
    assertEquals(7, cp.length);
    assertEquals("junit-4.13.1.jar", cp[0].getPath().lastSegment());
    assertEquals("commons-digester-1.6.jar", cp[2].getPath().lastSegment());
    assertEquals("commons-beanutils-1.6.jar", cp[3].getPath().lastSegment());
    assertEquals("commons-logging-1.0.jar", cp[4].getPath().lastSegment());
    assertEquals("commons-collections-2.1.jar", cp[5].getPath().lastSegment());
    assertEquals("xml-apis-1.0.b2.jar", cp[6].getPath().lastSegment());
  }

  @Test
  public void testDownloadSources_001_basic() throws Exception {
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t001/0.0.1/"));
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t002/0.0.1/"));

    IProject project = createExisting("downloadsources-p001", "projects/downloadsources/p001");
    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);

    // sanity check
    IClasspathEntry[] cp = getClasspathEntries(project);
    assertNullSourceAttachmentPaths(2, cp);

    // test project
    getBuildPathManager().scheduleDownload(project, true, false);
    waitForJobsToComplete();
    cp = getClasspathEntries(project);
    assertEquals(2, cp.length);
    assertEquals("downloadsources-t001-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
    assertEquals("downloadsources-t002-0.0.1-sources.jar", cp[1].getSourceAttachmentPath().lastSegment());

    {
      // cleanup
      deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t001/0.0.1/"));
      deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t002/0.0.1/"));
      MavenPlugin.getMavenProjectRegistry().refresh(new MavenUpdateRequest(project, false /*offline*/, false));
      waitForJobsToComplete();
    }

    // test one entry
    getBuildPathManager().scheduleDownload(getPackageFragmentRoot(javaProject, cp[0]), true, false);
    waitForJobsToComplete();
    cp = getClasspathEntries(project);
    assertEquals(2, cp.length);
    assertEquals("downloadsources-t001-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
    assertNull(cp[1].getSourceAttachmentPath());

    {
      // cleanup
      deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t001/0.0.1/"));
      deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t002/0.0.1/"));
      MavenPlugin.getMavenProjectRegistry()
          .refresh(new MavenUpdateRequest(List.of(project), false /*offline*/, false));
      waitForJobsToComplete();
    }

    // test two entries
    getBuildPathManager().scheduleDownload(getPackageFragmentRoot(javaProject, cp[0]), true, false);
    getBuildPathManager().scheduleDownload(getPackageFragmentRoot(javaProject, cp[1]), true, false);
    waitForJobsToComplete();
    cp = getClasspathEntries(project);
    assertEquals(2, cp.length);
    assertEquals("downloadsources-t001-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
    assertEquals("downloadsources-t002-0.0.1-sources.jar", cp[1].getSourceAttachmentPath().lastSegment());
  }

  private IPackageFragmentRoot getPackageFragmentRoot(IJavaProject javaProject, IClasspathEntry cp)
      throws JavaModelException {
    return javaProject.findPackageFragmentRoot(cp.getPath());
  }

  @Test
  public void testDownloadSources_001_workspaceRestart() throws Exception {
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t001/0.0.1/"));
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t002/0.0.1/"));

    IProject project = createExisting("downloadsources-p001", "projects/downloadsources/p001");
    waitForJobsToComplete();

    // sanity check
    IClasspathEntry[] cp = getClasspathEntries(project);
    assertNullSourceAttachmentPaths(2, cp);

    // purge MavenProject cache to simulate workspace restart
    deserializeFromWorkspaceState(MavenPlugin.getMavenProjectRegistry().getProject(project));

    // test project
    getBuildPathManager().scheduleDownload(project, true, false);
    waitForJobsToComplete();
    cp = getClasspathEntries(project);
    assertEquals(2, cp.length);
    assertEquals("downloadsources-t001-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
    assertEquals("downloadsources-t002-0.0.1-sources.jar", cp[1].getSourceAttachmentPath().lastSegment());
  }

  @Test
  public void testDownloadSources_001_sourceAttachment() throws Exception {
    new File(repo, "downloadsources/downloadsources-t001/0.0.1/downloadsources-t001-0.0.1-sources.jar").delete();
    new File(repo, "downloadsources/downloadsources-t002/0.0.1/downloadsources-t002-0.0.1-sources.jar").delete();

    IProject project = createExisting("downloadsources-p001", "projects/downloadsources/p001");
    waitForJobsToComplete();

    IPath entryPath = getClasspathEntries(project)[0].getPath();

    IPath srcPath = IPath.fromOSString("/a");
    IPath srcRoot = IPath.fromOSString("/b");
    String javaDocUrl = "c";

    IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
        javaDocUrl);

    final IClasspathEntry entry = JavaCore.newLibraryEntry(entryPath, //
        srcPath, srcRoot, new IAccessRule[0], //
        new IClasspathAttribute[] {attribute}, // 
        false /*not exported*/);

    BuildPathManager buildpathManager = persistAttachedSourcesAndJavadoc(project, entry);

    // check custom source/javadoc
    IClasspathEntry entry2 = getClasspathEntries(project)[0];
    assertEquals(entryPath, entry2.getPath());
    assertEquals(srcPath, entry2.getSourceAttachmentPath());
    assertEquals(srcRoot, entry2.getSourceAttachmentRootPath());
    assertEquals(javaDocUrl, buildpathManager.getJavadocLocation(entry2));

    File file = buildpathManager.getSourceAttachmentPropertiesFile(project);
    assertEquals(true, file.canRead());

    // check project delete
    deleteProject(project);
    waitForJobsToComplete();
    assertEquals(false, file.canRead());
  }

  @Test
  public void testDownloadSources_001_redownloadRelease() throws Exception {
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t001/0.0.1/"));
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t002/0.0.1/"));

    IProject project = createExisting("downloadsources-p001", "projects/downloadsources/p001");
    waitForJobsToComplete();

    // sanity check
    IClasspathEntry[] cp = getClasspathEntries(project);
    assertNullSourceAttachmentPaths(2, cp);

    // download sources the first time
    getBuildPathManager().scheduleDownload(project, true, false);
    waitForJobsToComplete();
    cp = getClasspathEntries(project);
    assertEquals(2, cp.length);
    assertEquals("downloadsources-t001-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
    assertEquals("downloadsources-t002-0.0.1-sources.jar", cp[1].getSourceAttachmentPath().lastSegment());

    // download sources a second time time
    getBuildPathManager().scheduleDownload(project, true, false);
    waitForJobsToComplete();
    cp = getClasspathEntries(project);
    assertEquals(2, cp.length);
    assertEquals("downloadsources-t001-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
    assertEquals("downloadsources-t002-0.0.1-sources.jar", cp[1].getSourceAttachmentPath().lastSegment());
  }

  @Test
  public void testDownloadSources_002_javadoconly() throws Exception {
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t003/0.0.1"));

    IProject project = createExisting("downloadsources-p002", "projects/downloadsources/p002");
    waitForJobsToComplete();

    // sanity check
    IClasspathEntry[] cp = getClasspathEntries(project);
    assertNullSourceAttachmentPaths(1, cp);

    getBuildPathManager().scheduleDownload(project, false, true);
    waitForJobsToComplete();

    cp = getClasspathEntries(project);
    assertNullSourceAttachmentPaths(1, cp);
    assertEquals("" + cp[0], 1, getAttributeCount(cp[0], IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME));
  }

  @Test
  public void testDownloadSources_003_customRemoteRepository() throws Exception {
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t004/0.0.1"));

    IProject project = createExisting("downloadsources-p003", "projects/downloadsources/p003");
    waitForJobsToComplete();

    // sanity check
    IClasspathEntry[] cp = getClasspathEntries(project);
    assertNullSourceAttachmentPaths(1, cp);

    getBuildPathManager().scheduleDownload(project, true, false);
    waitForJobsToComplete();

    cp = getClasspathEntries(project);
    assertEquals(1, cp.length);
    assertEquals("downloadsources-t004-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
  }

  private static long getAttributeCount(IClasspathEntry entry, String name) {
    IClasspathAttribute[] attrs = entry.getExtraAttributes();
    return Arrays.stream(attrs).map(IClasspathAttribute::getName).filter(name::equals).count();
  }

  @Test
  public void testDownloadSources_004_testsClassifier() throws Exception {
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t005/0.0.1"));

    IProject project = createExisting("downloadsources-p004", "projects/downloadsources/p004");
    waitForJobsToComplete();

    IClasspathEntry[] cp = getClasspathEntries(project);

    // sanity check
    assertEquals("downloadsources-t005-0.0.1-tests.jar", cp[1].getPath().lastSegment());

    getBuildPathManager().scheduleDownload(project, true, false);
    waitForJobsToComplete();
    cp = getClasspathEntries(project);

    assertEquals(2, cp.length);
    assertEquals("downloadsources-t005-0.0.1-test-sources.jar", cp[1].getSourceAttachmentPath().lastSegment());
  }

  @Test
  public void testDownloadSources_005_classifier() throws Exception {
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t006/0.0.1"));

    IProject project = createExisting("downloadsources-p005", "projects/downloadsources/p005");
    waitForJobsToComplete();

    IClasspathEntry[] cp = getClasspathEntries(project);

    // sanity check
    assertEquals("downloadsources-t006-0.0.1-jdk14.jar", cp[0].getPath().lastSegment());

    getBuildPathManager().scheduleDownload(project, true, false);
    waitForJobsToComplete();
    cp = getClasspathEntries(project);

    assertEquals(1, cp.length);
    assertNotNull(cp[0].getSourceAttachmentPath());
    assertEquals(cp[0].getSourceAttachmentPath().toString(), "downloadsources-t006-0.0.1-sources.jar",
        cp[0].getSourceAttachmentPath().lastSegment());
  }

  @Test
  public void testDownloadSources_007_missingTestsSources() throws Exception {
    // see MNGECLIPSE-1777
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t007/0.0.1"));

    IProject project = createExisting("downloadsources-p007", "projects/downloadsources/p007");
    waitForJobsToComplete();

    IClasspathEntry[] cp = getClasspathEntries(project);

    // sanity check
    assertNullSourceAttachmentPaths(1, cp);
    assertEquals("downloadsources-t007-0.0.1-tests.jar", cp[0].getPath().lastSegment());

    boolean oldDownloadSources = mavenConfiguration.isDownloadSources();
    try {
      ((MavenConfigurationImpl) mavenConfiguration).setDownloadSources(true);
      MavenUpdateRequest request = new MavenUpdateRequest(project, false/*offline*/, false/*updateSnapshots*/);
      MavenPlugin.getMavenProjectRegistry().refresh(request);
      waitForJobsToComplete();
      cp = getClasspathEntries(project);

      assertNullSourceAttachmentPaths(1, cp);
      assertEquals("downloadsources-t007-0.0.1-tests.jar", cp[0].getPath().lastSegment());
    } finally {
      ((MavenConfigurationImpl) mavenConfiguration).setDownloadSources(oldDownloadSources);
    }
  }

  @Test
  public void testDownloadSources_008_fallbackToJavadocWhenMissingSources() throws Exception {
    boolean oldDownloadSources = mavenConfiguration.isDownloadSources();
    boolean oldDownloadJavadoc = mavenConfiguration.isDownloadJavaDoc();
    try {
      ((MavenConfigurationImpl) mavenConfiguration).setDownloadSources(true);
      ((MavenConfigurationImpl) mavenConfiguration).setDownloadJavadoc(false);
      deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t008/0.0.1"));

      IProject project = createExisting("downloadsources-p008", "projects/downloadsources/p008");
      waitForJobsToComplete();

      // sanity check
      IClasspathEntry[] cp = getClasspathEntries(project);
      assertNullSourceAttachmentPaths(1, cp);
      assertEquals("" + cp[0], 0, getAttributeCount(cp[0], IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME));

      MavenUpdateRequest request = new MavenUpdateRequest(project, false/*offline*/, false/*updateSnapshots*/);
      //when sources are missing, we expect the javadoc to be downloaded if available.
      MavenPlugin.getMavenProjectRegistry().refresh(request);
      waitForJobsToComplete();
      cp = getClasspathEntries(project);

      assertNullSourceAttachmentPaths(1, cp);
      assertEquals("" + cp[0], 1, getAttributeCount(cp[0], IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME));
    } finally {
      ((MavenConfigurationImpl) mavenConfiguration).setDownloadSources(oldDownloadSources);
      ((MavenConfigurationImpl) mavenConfiguration).setDownloadJavadoc(oldDownloadJavadoc);
    }
  }

  @Test
  public void testDownloadSources_009_downloadSnapshot() throws Exception {
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t009/0.0.1-SNAPSHOT/"));

    IProject project = createExisting("downloadsources-t009", "projects/downloadsources/p009");
    waitForJobsToComplete();

    // sanity check
    IClasspathEntry[] cp = getClasspathEntries(project);
    assertNullSourceAttachmentPaths(1, cp);

    // download sources the first time
    getBuildPathManager().scheduleDownload(project, true, false);
    waitForJobsToComplete();
    cp = getClasspathEntries(project);
    assertEquals(1, cp.length);
    assertEquals("downloadsources-t009-0.0.1-SNAPSHOT-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
  }

  @Test
  public void testDownloadSources_009_redownloadSnapshot() throws Exception {
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t009/0.0.1-SNAPSHOT/"));

    IProject project = createExisting("downloadsources-t009", "projects/downloadsources/p009");
    waitForJobsToComplete();

    // sanity check
    IClasspathEntry[] cp = getClasspathEntries(project);
    assertNullSourceAttachmentPaths(1, cp);

    // download sources the first time
    getBuildPathManager().scheduleDownload(project, true, false);
    waitForJobsToComplete();
    cp = getClasspathEntries(project);
    assertEquals(1, cp.length);
    assertEquals("downloadsources-t009-0.0.1-SNAPSHOT-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());

    // download sources a second time time
    getBuildPathManager().scheduleDownload(project, true, false);
    waitForJobsToComplete();
    cp = getClasspathEntries(project);
    assertEquals(1, cp.length);
    assertEquals("downloadsources-t009-0.0.1-SNAPSHOT-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
  }

  @Test
  public void testDownloadSources_009_redownloadSnapshotWithOlderSources() throws Exception {
    File localSnapshotRepoMirror = new File(repo, "downloadsources/downloadsources-t009/0.0.1-SNAPSHOT/");
    deleteSourcesAndJavadoc(localSnapshotRepoMirror);

    IProject project = createExisting("downloadsources-t009", "projects/downloadsources/p009");
    waitForJobsToComplete();

    // sanity check
    IClasspathEntry[] cp = getClasspathEntries(project);
    assertNullSourceAttachmentPaths(1, cp);

    // download sources the first time
    getBuildPathManager().scheduleDownload(project, true, false);
    waitForJobsToComplete();
    cp = getClasspathEntries(project);
    assertEquals(1, cp.length);
    assertEquals("downloadsources-t009-0.0.1-SNAPSHOT-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());

    // Pretend artifact was last-modified after its sources
    // The sources artifact is (sometimes and sometimes not) touched during source download, so the main-artifact is touched.
    File mainArtifact = new File(localSnapshotRepoMirror, "downloadsources-t009-0.0.1-SNAPSHOT.jar");
    File mainArtifactSources = new File(localSnapshotRepoMirror, "downloadsources-t009-0.0.1-SNAPSHOT-sources.jar");
    mainArtifact.setLastModified(mainArtifactSources.lastModified() + 10_000);

    // download sources a second time time
    getBuildPathManager().scheduleDownload(project, true, false);
    waitForJobsToComplete();
    cp = getClasspathEntries(project);
    assertEquals(1, cp.length);
    assertEquals("downloadsources-t009-0.0.1-SNAPSHOT-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());

    // Afterwards sanity check: For this test-case setup it is mandatory that the main-artifact is last-modified after its sources.
    // Because the setup of this situation is a bit fragile, this afterwards check makes sure the test setup was not pointless.
    assertTrue("Main artifact not last-modified after sources",
        mainArtifactSources.lastModified() < mainArtifact.lastModified());
  }

  private BuildPathManager getBuildPathManager() {
    return (BuildPathManager) MavenJdtPlugin.getDefault().getBuildpathManager();
  }

  @Test
  public void testClassifiers() throws Exception {
    IProject p1 = createExisting("classifiers-p1", "projects/classifiers/classifiers-p1");
    waitForJobsToComplete();

    IClasspathEntry[] cp = getClasspathEntries(p1);

    assertEquals(2, cp.length);
    assertEquals("classifiers-p2-0.0.1.jar", cp[0].getPath().lastSegment());
    assertEquals("classifiers-p2-0.0.1-tests.jar", cp[1].getPath().lastSegment());

    IProject p2 = createExisting("classifiers-p2", "projects/classifiers/classifiers-p2");
    waitForJobsToComplete();

    cp = getClasspathEntries(p1);

    assertEquals(1, cp.length);
    assertEquals("classifiers-p2", cp[0].getPath().lastSegment());

    Set<Artifact> artifacts = getMavenProjectArtifacts(p1);
    assertEquals(2, artifacts.size());

    Iterator<Artifact> it = artifacts.iterator();
    assertEquals(p2.getFolder("target/classes").getLocation().toFile(), it.next().getFile());
    assertEquals(p2.getFolder("target/test-classes").getLocation().toFile(), it.next().getFile());
  }

  @Test
  public void testCreateSimpleProject() throws CoreException {
    IProject project = createSimpleProject("simple-project", null);

    IJavaProject javaProject = JavaCore.create(project);

    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    assertEquals(Arrays.toString(rawClasspath), 6, rawClasspath.length);
    assertEquals("/simple-project/src/main/java", rawClasspath[0].getPath().toString());
    assertEquals("/simple-project/src/main/resources", rawClasspath[1].getPath().toString());
    assertEquals("/simple-project/src/test/java", rawClasspath[2].getPath().toString());
    assertEquals("/simple-project/src/test/resources", rawClasspath[3].getPath().toString());
    assertEquals(
        "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/J2SE-1.5",
        rawClasspath[4].getPath().toString());
    assertEquals("org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[5].getPath().toString());

    IClasspathEntry[] entries = getMavenContainerEntries(project);
    assertEquals(Arrays.toString(entries), 2, entries.length);
    assertEquals(IClasspathEntry.CPE_LIBRARY, entries[0].getEntryKind());
    assertEquals("junit-4.13.1.jar", entries[0].getPath().lastSegment());

    assertTrue(project.getFile("pom.xml").exists());
    assertTrue(project.getFolder("src/main/java").exists());
    assertTrue(project.getFolder("src/test/java").exists());
    assertTrue(project.getFolder("src/main/resources").exists());
    assertTrue(project.getFolder("src/test/resources").exists());
  }

  @Test
  public void test005_dependencyAvailableFromLocalRepoAndWorkspace() throws Exception {
    IProject p1 = createExisting("t005-p1", "resources/t005/t005-p1");
    IProject p2 = createExisting("t005-p2", "resources/t005/t005-p2");
    waitForJobsToComplete();

    IClasspathEntry[] cp = getMavenContainerEntries(p1);
    assertEquals(1, cp.length);
    assertEquals(p2.getFullPath(), cp[0].getPath());

    p2.close(monitor);
    waitForJobsToComplete();

    cp = getMavenContainerEntries(p1);
    assertEquals(1, cp.length);
    assertEquals("t005-p2-0.0.1.jar", cp[0].getPath().lastSegment());

    p2.open(monitor);
    waitForJobsToComplete();

    cp = getMavenContainerEntries(p1);
    assertEquals(1, cp.length);
    assertEquals(p2.getFullPath(), cp[0].getPath());

    Set<Artifact> artifacts = getMavenProjectArtifacts(p1);
    assertEquals(1, artifacts.size());

    Iterator<Artifact> it = artifacts.iterator();
    assertEquals(p2.getFolder("target/classes").getLocation().toFile(), it.next().getFile());
  }

  @Test
  public void testProjectNameTemplate() throws Exception {
    deleteProject("p003");
    deleteProject("projectimport.p003-2.0");

    ResolverConfiguration configuration = new ResolverConfiguration();
    ProjectImportConfiguration projectImportConfiguration = new ProjectImportConfiguration(configuration);
    importProject("p003version1", "projects/projectimport/p003version1", projectImportConfiguration);

    projectImportConfiguration.setProjectNameTemplate("[groupId].[artifactId]-[version]");
    importProject("p003version2", "projects/projectimport/p003version2", projectImportConfiguration);

    waitForJobsToComplete();

    assertTrue(workspace.getRoot().getProject("p003").exists());
    assertTrue(workspace.getRoot().getProject("projectimport.p003-2.0").exists());
  }

  @Test
  public void testProjectNameTemplateWithProperties() throws Exception {
    deleteProject("project..import.p004.-2.0-");

    ResolverConfiguration configuration = new ResolverConfiguration();
    ProjectImportConfiguration projectImportConfiguration = new ProjectImportConfiguration(configuration);
    projectImportConfiguration.setProjectNameTemplate("[groupId].[artifactId]-[version]");
    importProject("project..import.p004.-2.0-", "projects/projectimport/p004", projectImportConfiguration);

    waitForJobsToComplete();

    assertTrue(workspace.getRoot().getProject("project..import.p004.-2.0-").exists());
  }

  @Test
  public void testProjectNameWithNameTemplate() throws Exception {
    deleteProject("343038");

    ResolverConfiguration configuration = new ResolverConfiguration();
    ProjectImportConfiguration projectImportConfiguration = new ProjectImportConfiguration(configuration);
    projectImportConfiguration.setProjectNameTemplate("[name]-[version]");
    importProject("343038", "projects/projectimport/343038", projectImportConfiguration);

    waitForJobsToComplete();

    assertTrue(workspace.getRoot().getProject("My super awesome project-0.0.1-SNAPSHOT").exists());
  }

  @Test
  public void testMavenBuilderOrder() throws Exception {
    IProject project = createExisting("builderOrder", "projects/builderOrder");
    IProjectDescription description = project.getDescription();

    ICommand[] buildSpec = description.getBuildSpec();
    ICommand javaBuilder = buildSpec[0];
    ICommand mavenBuilder = buildSpec[1];

    verifyNaturesAndBuilders(project);

    ResolverConfiguration configuration = new ResolverConfiguration();

    IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();

    configurationManager.updateProjectConfiguration(project, monitor);
    verifyNaturesAndBuilders(project);

    description.setNatureIds(new String[] {JavaCore.NATURE_ID});
    description.setBuildSpec(new ICommand[] {javaBuilder});
    project.setDescription(description, monitor);
    // can't update configuration of non-maven project
    configurationManager.enableMavenNature(project, configuration, monitor);
    verifyNaturesAndBuilders(project);

    description.setNatureIds(new String[] {});
    description.setBuildSpec(new ICommand[] {mavenBuilder, javaBuilder});
    project.setDescription(description, monitor);
    // can't update configuration of non-maven project
    configurationManager.enableMavenNature(project, configuration, monitor);
    verifyNaturesAndBuilders(project);

    description.setNatureIds(new String[] {IMavenConstants.NATURE_ID, JavaCore.NATURE_ID});
    description.setBuildSpec(new ICommand[] {mavenBuilder, javaBuilder});
    project.setDescription(description, monitor);
    // can't update configuration of non-maven project
    configurationManager.enableMavenNature(project, configuration, monitor);
    verifyNaturesAndBuilders(project);
  }

  // MNGECLIPSE-1133
  @Test
  public void testUpdateProjectConfigurationWithWorkspace() throws Exception {
    deleteProject("MNGECLIPSE-1133parent");
    deleteProject("MNGECLIPSE-1133child");

    final IProject project1 = createProject("MNGECLIPSE-1133parent", "projects/MNGECLIPSE-1133/parent/pom.xml");
    final IProject project2 = createProject("MNGECLIPSE-1133child", "projects/MNGECLIPSE-1133/child/pom.xml");

    NullProgressMonitor monitor = new NullProgressMonitor();
    IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();

    ResolverConfiguration configuration = new ResolverConfiguration();
    configurationManager.enableMavenNature(project1, configuration, monitor);
//    buildpathManager.updateSourceFolders(project1, monitor);

    configurationManager.enableMavenNature(project2, configuration, monitor);
//    buildpathManager.updateSourceFolders(project2, monitor);

//    waitForJob("Initializing " + project1.getProject().getName());
//    waitForJob("Initializing " + project2.getProject().getName());

    try {
      project1.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
      project2.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    } catch(Exception ex) {
      throw ex;
    }
    waitForJobsToComplete();

    WorkspaceHelpers.assertNoErrors(project2);

    // update configuration
    configurationManager.updateProjectConfiguration(project2, monitor);
    waitForJobsToComplete();

    WorkspaceHelpers.assertNoErrors(project2);
  }

  private void verifyNaturesAndBuilders(IProject project) throws CoreException {

    assertTrue(project.hasNature(JavaCore.NATURE_ID));
    assertTrue(project.hasNature(IMavenConstants.NATURE_ID));

    IProjectDescription description = project.getDescription();
    {
      ICommand[] buildSpec = description.getBuildSpec();
      assertEquals(2, buildSpec.length);
      assertEquals(JavaCore.BUILDER_ID, buildSpec[0].getBuilderName());
      assertEquals(IMavenConstants.BUILDER_ID, buildSpec[1].getBuilderName());
    }
  }

  private IProject createSimpleProject(final String projectName, final IPath location) throws CoreException {
    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

    workspace.run((IWorkspaceRunnable) monitor -> {
      Model model = new Model();
      model.setGroupId(projectName);
      model.setArtifactId(projectName);
      model.setVersion("0.0.1-SNAPSHOT");
      model.setModelVersion("4.0.0");

      Dependency dependency = new Dependency();
      dependency.setGroupId("junit");
      dependency.setArtifactId("junit");
      dependency.setVersion("4.13.1");

      model.addDependency(dependency);

      // used to derive the source/target options so lock it down
      Plugin buildPlugin = new Plugin();
      buildPlugin.setArtifactId("maven-compiler-plugin");
      buildPlugin.setVersion("2.3");

      model.setBuild(new Build());
      model.getBuild().addPlugin(buildPlugin);

      List<String> directories = List.of("src/main/java", "src/test/java", "src/main/resources", "src/test/resources");

      ProjectImportConfiguration config = new ProjectImportConfiguration();

      MavenPlugin.getProjectConfigurationManager().createSimpleProject(project, location, model, directories, config,
          monitor);
    }, MavenPlugin.getProjectConfigurationManager().getRule(), IWorkspace.AVOID_UPDATE, monitor);

    return project;
  }

  @Test
  public void testSimpleProjectInExternalLocation() throws CoreException, IOException {
    File tmp = File.createTempFile("m2eclipse", "test");
    tmp.delete(); //deleting a tmp file so we can use the name for a folder

    final String projectName1 = "external-simple-project-1";
    createSimpleProject(projectName1, IPath.fromOSString(tmp.getAbsolutePath()).append(projectName1));

    final String projectName2 = "external-simple-project-2";
    File existingFolder = new File(tmp, projectName2);
    existingFolder.mkdirs();
    new File(existingFolder, IMavenConstants.POM_FILE_NAME).createNewFile();
    try {
      createSimpleProject(projectName2, IPath.fromOSString(tmp.getAbsolutePath()).append(projectName2));
      fail("Project creation should fail if the POM exists in the target folder");
    } catch(CoreException e) {
      final String msg = IMavenConstants.POM_FILE_NAME + " already exists";
      assertTrue("Project creation should throw a \"" + msg + "\" exception if the POM exists in the target folder",
          e.getMessage().indexOf(msg) > 0);
    }

    tmp.delete();
  }

  @Test
  public void testArchetypeProject() throws CoreException, IOException {
    useSettings("settings2.xml");
    Archetype quickStart = findQuickStartArchetype();
    IPath location = null;
    Collection<IProject> projects = createProjectsFromArchetype("archetype-project", new MavenArchetype(quickStart),
        location);
    assertEquals(1, projects.size());
    IProject project = projects.iterator().next();
    assertMavenNature(project);
    assertNotNull(JavaCore.create(project));
  }



  @Test
  public void testArchetypeProjectInExternalLocation() throws CoreException, IOException {
    useSettings("settings2.xml");
    Archetype quickStart = findQuickStartArchetype();

    File tmp = File.createTempFile("m2eclipse", "test");
    tmp.delete(); //deleting a tmp file so we can use the name for a folder

    final String projectName1 = "external-archetype-project-1";
    IProject project1 = createArchetypeProject(projectName1, IPath.fromOSString(tmp.getAbsolutePath()).append(projectName1),
        quickStart);
    assertNotNull(JavaCore.create(project1)); // TODO more meaningful assertion 

    final String projectName2 = "external-archetype-project-2";
    File existingFolder = new File(tmp, projectName2);
    existingFolder.mkdirs();
    new File(existingFolder, IMavenConstants.POM_FILE_NAME).createNewFile();
    try {
      createArchetypeProject(projectName2, IPath.fromOSString(tmp.getAbsolutePath()).append(projectName2), quickStart);
      fail("Project creation should fail if the POM exists in the target folder");
    } catch(CoreException e) {
      // this is supposed to happen
    }

    tmp.delete();
  }

  private Archetype findQuickStartArchetype() throws CoreException {
    List<Archetype> archetypes = M2EUIPluginActivator.getDefault().getArchetypePlugin()
        .getArchetypeCatalogFactory("internal").getArchetypeCatalog().getArchetypes();
    for(Archetype archetype : archetypes) {
      if("org.apache.maven.archetypes".equals(archetype.getGroupId())
          && "maven-archetype-quickstart".equals(archetype.getArtifactId())) {
        return archetype;
      }
    }

    fail("maven-archetype-quickstart archetype not found in the internal catalog");
    return null;
  }

  private IProject createArchetypeProject(final String projectName, final IPath location, final Archetype archetype)
      throws CoreException {
    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    workspace.run((IWorkspaceRunnable) m -> {
      M2EUIPluginActivator.getDefault().getArchetypePlugin().getGenerator().createArchetypeProjects(location,
          new MavenArchetype(archetype), projectName, projectName, "0.0.1-SNAPSHOT", "jar", Map.of(), monitor);
    }, MavenPlugin.getProjectConfigurationManager().getRule(), IWorkspace.AVOID_UPDATE, monitor);
    return project;
  }

  @Test
  public void testMNGECLIPSE_1047_localRepoPath() {
    IPath m2_repo = JavaCore.getClasspathVariable(BuildPathManager.M2_REPO);
    //Local repo set by the BuildManager was based on a cached version of MavenEmbedder, not the one configured in setup.
    assertEquals(repo.toString(), m2_repo.toOSString());
  }

  @Test
  public void testMNGECLIPSE_696_compiler_includes_excludes() throws Exception {
    final String projectName = "MNGECLIPSE-696";

    deleteProject(projectName);

    final ResolverConfiguration configuration = new ResolverConfiguration();
    final IProject project = importProject("projects/" + projectName + "/pom.xml", configuration);
    waitForJobsToComplete();

    WorkspaceHelpers.assertNoErrors(project);

    String srcMain = "/" + projectName + "/src/main/java";
    String srcTest = "/" + projectName + "/src/test/java";
    Map<String, IClasspathEntry> map = assertClasspath(project, srcMain, srcTest);

    final IClasspathEntry cpMain = map.get(srcMain);
    final IClasspathEntry cpTest = map.get(srcTest);

    final IPath[] inclusionsMain = cpMain.getInclusionPatterns();
    assertEquals(2, inclusionsMain.length);
    assertEquals(IPath.fromOSString("org/apache/maven/"), inclusionsMain[0]);
    assertEquals(IPath.fromOSString("org/maven/ide/eclipse/"), inclusionsMain[1]);

    final IPath[] exclusionsMain = cpMain.getExclusionPatterns();
    assertEquals(1, exclusionsMain.length);
    assertEquals(IPath.fromOSString("org/maven/ide/eclipse/tests/"), exclusionsMain[0]);

    final IPath[] inclusionsTest = cpTest.getInclusionPatterns();
    assertEquals(1, inclusionsTest.length);
    assertEquals(IPath.fromOSString("org/apache/maven/tests/"), inclusionsTest[0]);

    final IPath[] exclusionsTest = cpTest.getExclusionPatterns();
    assertEquals(1, exclusionsTest.length);
    assertEquals(IPath.fromOSString("org/apache/maven/tests/Excluded.java"), exclusionsTest[0]);
  }

  @Test
  public void testMNGECLIPSE_2367_same_sources_resources() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-2367_sourcesResourcesOverlap/project01/pom.xml");
    waitForJobsToComplete();
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] cp = javaProject.getRawClasspath();

    // maven-resources-plugin handles resources, make sure JDT only deals with java sources.
    IPath[] incl = cp[0].getInclusionPatterns();
    assertEquals(1, incl.length);
    assertEquals("**/*.java", incl[0].toPortableString());
    assertEquals(0, cp[0].getExclusionPatterns().length);

    // make sure resources do get copied to target/classes folder
    assertTrue(project.getFile("target/classes/test.properties").isAccessible());
  }

  @Test
  public void testMNGECLIPSE_2367_sources_encloses_resources() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-2367_sourcesResourcesOverlap/project02/pom.xml");
    waitForJobsToComplete();
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] cp = javaProject.getRawClasspath();

    // maven-resources-plugin handles resources, make sure JDT only deals with java sources.
    IPath[] incl = cp[0].getInclusionPatterns();
    assertEquals(1, incl.length);
    assertEquals("**/*.java", incl[0].toPortableString());
    assertEquals(0, cp[0].getExclusionPatterns().length);

    // make sure resources do get copied to target/classes folder
    assertTrue(project.getFile("target/classes/test.properties").isAccessible());
  }

  @Test
  public void testMNGECLIPSE_2367_testSources_encloses_resources() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-2367_sourcesResourcesOverlap/project03/pom.xml");
    waitForJobsToComplete();
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    // this project has resources and test-sources folders overlap. m2e does not support this properly
    // so it is good enough if we don't fail with nasty exceptions
    // see https://issues.sonatype.org/browse/MNGECLIPSE-2367

    // make sure resources do get copied to target/classes folder
    assertTrue(project.getFile("target/classes/test.properties").isAccessible());
  }

  @Test
  public void test486721_sourceAndResourceUnderSameNonDefaultFolder() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-2367_sourcesResourcesOverlap/project04/pom.xml");
    assertNoErrors(project);

    String srcJava = "/project04/src/main/java";
    String srcImpl = "/project04/src/main_impl/java";
    String srcTest = "/project04/src/test/java";
    Map<String, IClasspathEntry> map = assertClasspath(project,
        srcJava, //
        srcImpl, //
        srcTest, //
        "org.eclipse.jdt.launching.JRE_CONTAINER/.*", //
        "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER" //
    );

    assertEquals(0, map.get(srcJava).getExclusionPatterns().length);
    assertEquals(0, map.get(srcImpl).getExclusionPatterns().length);
    assertEquals(1, map.get(srcImpl).getInclusionPatterns().length);
    assertEquals("**/*.java", map.get(srcImpl).getInclusionPatterns()[0].toString());
    assertEquals(0, map.get(srcTest).getExclusionPatterns().length);
  }

  @Test
  public void test486721_overlappingResourceFolders() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-2367_sourcesResourcesOverlap/project05/pom.xml");
    assertNoErrors(project);

    String resourcePath = "/project05/src/main/resources";
    IClasspathEntry entry = assertClasspath(project,
        "/project05/src/main/java", //
        resourcePath, //
        "/project05/src/test/java", //
        "org.eclipse.jdt.launching.JRE_CONTAINER/.*", //
        "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER" //
    ).get(resourcePath);
    assertNotNull(entry);
    assertEquals(0, entry.getInclusionPatterns().length);
    assertEquals(1, entry.getExclusionPatterns().length);
    assertEquals("**", entry.getExclusionPatterns()[0].toString());
  }

  @Test
  public void testMNGECLIPSE_2433_resourcesOutsideBasdir() throws Exception {
    IProject[] projects = importProjects("projects/MNGECLIPSE-2433_resourcesOutsideBasdir",
        new String[] {"project01/pom.xml", "project02/pom.xml"}, new ResolverConfiguration());
    projects[1].build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    // project02 has resources folder outside of it's basedir. m2e does not support this and probably never will.
    // so it is good enough we don't fail with nasty exceptions 
    // ideally we need to add a warning marker on the offending pom.xml element
    // https://issues.sonatype.org/browse/MNGECLIPSE-2433

    ClasspathHelpers.assertClasspath(projects[1], "/project02/src/main/java", "/project02/src/test/java",
        "org.eclipse.jdt.launching.JRE_CONTAINER.*", "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER");
  }

  @Test
  public void test359725_resourcesWorkspaceRoot() throws Exception {
    // m2e does not support resources outside of project basedir.
    // the point of this test is to verify m2e can import such unsupported projects

    IProject project = importProject("projects/359725_resourcesWorkspaceRoot/pom.xml");
    waitForJobsToComplete();

    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    assertNoErrors(project);
  }

  @Test
  @Ignore("See https://github.com/eclipse-m2e/m2e-core/issues/1362")
  public void test431080_flatDirectoryLayout() throws Exception {
    IProject project = importProject("projects/431080_flatDirectoryLayout/pom.xml");
    waitForJobsToComplete();

    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    assertNoErrors(project);
  }

  @Test
  public void test360962_forbiddenReferencePreference() throws Exception {
    IProject project = importProject("projects/360962_forbiddenReferencePreference/custom/pom.xml");
    waitForJobsToComplete();
    IJavaProject jproject = JavaCore.create(project);
    assertEquals("error", jproject.getOption(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, false));

    project = importProject("projects/360962_forbiddenReferencePreference/default/pom.xml");
    waitForJobsToComplete();
    jproject = JavaCore.create(project);
    assertEquals("warning", jproject.getOption(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, false));
  }

  @Test
  public void test388596_expandedSnapshotDependency() throws Exception {
    IProject[] projects = importProjects("projects/388596_expandedSnapshotDependency",
        new String[] {"a/pom.xml", "b/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();

    IClasspathEntry[] cp = getClasspathEntries(projects[1]);

    assertEquals(1, cp.length);
    ClasspathHelpers.assertClasspathEntry(cp, projects[0].getFullPath());
  }

  @Test
  public void test385391_keepCustomSourceEncoding() throws Exception {
    IProject project = createSimpleProject("simple-project", null);

    IClasspathEntry[] entries = getMavenContainerEntries(project);
    assertEquals(Arrays.toString(entries), 2, entries.length);
    assertEquals(IClasspathEntry.CPE_LIBRARY, entries[0].getEntryKind());
    assertEquals("junit-4.13.1.jar", entries[0].getPath().lastSegment());
    assertNull(MavenClasspathHelpers.getAttribute(entries[0], IClasspathAttribute.SOURCE_ATTACHMENT_ENCODING));

    IPath entryPath = getClasspathEntries(project)[0].getPath();

    String encoding = "ISO-8859-1";

    IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.SOURCE_ATTACHMENT_ENCODING,
        encoding);

    final IClasspathEntry entry = JavaCore.newLibraryEntry(entryPath, //
        entries[0].getSourceAttachmentPath(), entries[0].getSourceAttachmentRootPath(), new IAccessRule[0], //
        new IClasspathAttribute[] {attribute}, // 
        false /*not exported*/);

    persistAttachedSourcesAndJavadoc(project, entry);

    // check custom source encoding
    IClasspathEntry entry2 = getClasspathEntries(project)[0];
    assertEquals(entryPath, entry2.getPath());
    assertEquals(entry.getSourceAttachmentPath(), entry2.getSourceAttachmentPath());
    assertEquals(entry.getSourceAttachmentRootPath(), entry2.getSourceAttachmentRootPath());
    assertEquals(encoding, MavenClasspathHelpers.getAttribute(entry2, IClasspathAttribute.SOURCE_ATTACHMENT_ENCODING));
  }

  // --- utility methods ---

  private Set<Artifact> getMavenProjectArtifacts(IProject p) throws CoreException {
    MavenProject mavenProject = manager.create(p, monitor).getMavenProject(monitor);
    return mavenProject.getArtifacts();
  }

  private void deleteSourcesAndJavadoc(File basedir) throws IOException {
    if(!basedir.exists()) {
      return;
    }
    try (var files = Files.newDirectoryStream(basedir.toPath());) {
      for(java.nio.file.Path file : files) {
        if(file.toString().endsWith("-sources.jar") || file.toString().endsWith("-javadoc.jar")
            || file.endsWith("m2e-lastUpdated.properties")) {
          Files.delete(file);
        }
      }
    }
  }

  private static IClasspathEntry[] getClasspathEntries(IProject project) throws JavaModelException {
    IJavaProject javaProject2 = JavaCore.create(project);
    IClasspathContainer container2 = BuildPathManager.getMaven2ClasspathContainer(javaProject2);
    return container2.getClasspathEntries();
  }

  private void assertNullSourceAttachmentPaths(int expectedLength, IClasspathEntry[] entries) {
    assertEquals(expectedLength, entries.length);
    for(IClasspathEntry entry : entries) {
      assertNull(entry.getSourceAttachmentPath());
    }
  }

  private BuildPathManager persistAttachedSourcesAndJavadoc(IProject project, final IClasspathEntry entry)
      throws CoreException, InterruptedException {
    IJavaProject javaProject = JavaCore.create(project);
    BuildPathManager buildpathManager = getBuildPathManager();
    final IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathContainer containerSuggestion = new IClasspathContainer() {
      @Override
      public IClasspathEntry[] getClasspathEntries() {
        return new IClasspathEntry[] {entry};
      }

      @Override
      public String getDescription() {
        return container.getDescription();
      }

      @Override
      public int getKind() {
        return container.getKind();
      }

      @Override
      public IPath getPath() {
        return container.getPath();
      }
    };
    buildpathManager.persistAttachedSourcesAndJavadoc(javaProject, containerSuggestion, monitor);
    waitForJobsToComplete();
    return buildpathManager;
  }
}
