/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.tests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.index.IndexInfo;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.jdt.BuildPathManager;
import org.maven.ide.eclipse.jdt.MavenJdtPlugin;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.MavenUpdateRequest;
import org.maven.ide.eclipse.project.ProjectImportConfiguration;
import org.maven.ide.eclipse.project.ResolverConfiguration;


/**
 * @author Eugene Kuleshov
 */
public class BuildPathManagerTest extends AsbtractMavenProjectTestCase {

  public void testEnableMavenNature() throws Exception {
    deleteProject("MNGECLIPSE-248parent");
    deleteProject("MNGECLIPSE-248child");

    final IProject project1 = createProject("MNGECLIPSE-248parent", "projects/MNGECLIPSE-248parent/pom.xml");
    final IProject project2 = createProject("MNGECLIPSE-248child", "projects/MNGECLIPSE-248child/pom.xml");

    NullProgressMonitor monitor = new NullProgressMonitor();
    IProjectConfigurationManager configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();

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

    IMarker[] markers1 = project1.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertTrue("Unexpected markers " + Arrays.asList(markers1), markers1.length == 0);

//    IClasspathEntry[] project1entries = getMavenContainerEntries(project1);
//    assertEquals(1, project1entries.length);
//    assertEquals(IClasspathEntry.CPE_LIBRARY, project1entries[0].getEntryKind());
//    assertEquals("junit-4.1.jar", project1entries[0].getPath().lastSegment());

    IClasspathEntry[] project2entries = getMavenContainerEntries(project2);
    assertEquals(2, project2entries.length);
    assertEquals(IClasspathEntry.CPE_PROJECT, project2entries[0].getEntryKind());
    assertEquals("MNGECLIPSE-248parent", project2entries[0].getPath().segment(0));
    assertEquals(IClasspathEntry.CPE_LIBRARY, project2entries[1].getEntryKind());
    assertEquals("junit-4.1.jar", project2entries[1].getPath().lastSegment());
    
    configurationManager.updateProjectConfiguration(project2, configuration, "", monitor);
    waitForJobsToComplete();

    assertMarkers(project2, 0);
  }
  
  public void testDisableMavenNature() throws Exception {
    deleteProject("disablemaven-p001");
    IProject p = createExisting("disablemaven-p001", "projects/disablemaven/p001");
    waitForJobsToComplete();
    
    assertTrue(p.hasNature(IMavenConstants.NATURE_ID));
    assertTrue(p.hasNature(JavaCore.NATURE_ID));
    assertNotNull(BuildPathManager.getMaven2ClasspathContainer(JavaCore.create(p)));
    
    IProjectConfigurationManager configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
    configurationManager.disableMavenNature(p, monitor);
    waitForJobsToComplete();

    assertFalse(p.hasNature(IMavenConstants.NATURE_ID));
    assertFalse(hasBuilder(p, IMavenConstants.BUILDER_ID));
    assertTrue(p.hasNature(JavaCore.NATURE_ID));
    assertNull(BuildPathManager.getMaven2ClasspathContainer(JavaCore.create(p)));
  }

  private boolean hasBuilder(IProject p, String builderId) throws CoreException {
    for (ICommand command : p.getDescription().getBuildSpec()) {
      if (builderId.equals(command.getBuilderName())) {
        return true;
      }
    }
    return false;
  }

  public void testEnableMavenNatureWithNoWorkspace() throws Exception {
    deleteProject("MNGECLIPSE-248parent");
    deleteProject("MNGECLIPSE-248child");

    final IProject project1 = createProject("MNGECLIPSE-248parent", "projects/MNGECLIPSE-248parent/pom.xml");
    final IProject project2 = createProject("MNGECLIPSE-248child", "projects/MNGECLIPSE-248child/pom.xml");

    NullProgressMonitor monitor = new NullProgressMonitor();
    IProjectConfigurationManager importManager = MavenPlugin.getDefault().getProjectConfigurationManager();

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(false);
    configuration.setResolveWorkspaceProjects(false);
    configuration.setActiveProfiles("");

    importManager.enableMavenNature(project1, configuration, monitor);
    importManager.enableMavenNature(project2, configuration, monitor);
//    buildpathManager.updateSourceFolders(project1, monitor);
//    buildpathManager.updateSourceFolders(project2, monitor);

//    waitForJob("Initializing " + project1.getProject().getName());
//    waitForJob("Initializing " + project2.getProject().getName());
    waitForJobsToComplete();

//    IClasspathEntry[] project1entries = getMavenContainerEntries(project1);
//    assertEquals(Arrays.asList(project1entries).toString(), 1, project1entries.length);
//    assertEquals(IClasspathEntry.CPE_LIBRARY, project1entries[0].getEntryKind());
//    assertEquals("junit-4.1.jar", project1entries[0].getPath().lastSegment());

    IClasspathEntry[] project2entries = getMavenContainerEntries(project2);
    assertEquals(Arrays.asList(project2entries).toString(), 1, project2entries.length);
    assertEquals(IClasspathEntry.CPE_LIBRARY, project2entries[0].getEntryKind());
    assertEquals("MNGECLIPSE-248parent-1.0.0.jar", project2entries[0].getPath().lastSegment());
  }

  public void testProjectImportWithProfile1() throws Exception {
    deleteProject("MNGECLIPSE-353");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(false);
    configuration.setResolveWorkspaceProjects(true);
    configuration.setActiveProfiles("jaxb1");

    IProject project = importProject("projects/MNGECLIPSE-353/pom.xml", configuration);

    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals("" + Arrays.asList(classpathEntries), 2, classpathEntries.length);
    assertEquals("junit-3.8.1.jar", classpathEntries[0].getPath().lastSegment());
    assertEquals("jaxb-api-1.5.jar", classpathEntries[1].getPath().lastSegment());

    assertMarkers(project, 0);
  }

  public void testProjectImportWithProfile2() throws Exception {
    deleteProject("MNGECLIPSE-353");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(false);
    configuration.setResolveWorkspaceProjects(true);
    configuration.setActiveProfiles("jaxb20");

    IProject project = importProject("projects/MNGECLIPSE-353/pom.xml", configuration);

    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals("" + Arrays.asList(classpathEntries), 4, classpathEntries.length);
    assertEquals("junit-3.8.1.jar", classpathEntries[0].getPath().lastSegment());
    assertEquals("jaxb-api-2.0.jar", classpathEntries[1].getPath().lastSegment());
    assertEquals("jsr173_api-1.0.jar", classpathEntries[2].getPath().lastSegment());
    assertEquals("activation-1.1.jar", classpathEntries[3].getPath().lastSegment());

    assertMarkers(project, 0);
  }

  public void testProjectImport001_useMavenOutputFolders() throws Exception {
    deleteProject("projectimport-p001");

    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject project = importProject("projectimport-p001", "projects/projectimport/p001", configuration);

    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);

    assertEquals(new Path("/projectimport-p001/target/classes"), javaProject.getOutputLocation());
    IClasspathEntry[] cp = javaProject.getRawClasspath();

    assertEquals(4, cp.length);
    assertEquals(new Path("/projectimport-p001/src/main/java"), cp[0].getPath());
    assertEquals(new Path("/projectimport-p001/target/classes"), cp[0].getOutputLocation());
    assertEquals(new Path("/projectimport-p001/src/test/java"), cp[1].getPath());
    assertEquals(new Path("/projectimport-p001/target/test-classes"), cp[1].getOutputLocation());
  }

  public void testProjectImport002_useMavenOutputFolders() throws Exception {
    deleteProject("projectimport-p002");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(true);
    IProject project = importProject("projectimport-p002", "projects/projectimport/p002", configuration);

    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);

    assertEquals(new Path("/projectimport-p002/target/classes"), javaProject.getOutputLocation());
    IClasspathEntry[] cp = javaProject.getRawClasspath();

    assertEquals(3, cp.length);
    assertEquals(new Path("/projectimport-p002/p002-m1/src/main/java"), cp[0].getPath());
    assertEquals(new Path("/projectimport-p002/p002-m1/target/classes"), cp[0].getOutputLocation());
  }

  public void testEmbedderException() throws Exception {
    deleteProject("MNGECLIPSE-157parent");

    IProject project = importProject("projects/MNGECLIPSE-157parent/pom.xml", new ResolverConfiguration());
    importProject("projects/MNGECLIPSE-157child/pom.xml", new ResolverConfiguration());
    waitForJobsToComplete();

    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(toString(markers), 1, markers.length);
    assertEquals(toString(markers), "pom.xml", markers[0].getResource().getFullPath().lastSegment());
  }

  public void testClasspathOrderWorkspace001() throws Exception {
    deleteProject("p1");
    deleteProject("p2");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(false);
    configuration.setResolveWorkspaceProjects(true);
    configuration.setActiveProfiles("");

    IProject project1 = importProject("projects/dependencyorder/p1/pom.xml", configuration);
    IProject project2 = importProject("projects/dependencyorder/p2/pom.xml", configuration);
    project1.build(IncrementalProjectBuilder.FULL_BUILD, null);
    project2.build(IncrementalProjectBuilder.FULL_BUILD, null);
    waitForJobsToComplete();

//    MavenPlugin.getDefault().getBuildpathManager().updateClasspathContainer(p1, new NullProgressMonitor());

    IJavaProject javaProject = JavaCore.create(project1);
    IClasspathContainer maven2ClasspathContainer = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = maven2ClasspathContainer.getClasspathEntries();

    // order according to mvn -X
    assertEquals(3, cp.length);
    assertEquals(new Path("/p2"), cp[0].getPath());
    assertEquals("junit-4.0.jar", cp[1].getPath().lastSegment());
    assertEquals("easymock-1.0.jar", cp[2].getPath().lastSegment());
  }

  public void testClasspathOrderWorkspace003() throws Exception {
    deleteProject("p3");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(false);
    configuration.setResolveWorkspaceProjects(true);
    configuration.setActiveProfiles("");

    IProject p3 = importProject("projects/dependencyorder/p3/pom.xml", configuration);
    p3.build(IncrementalProjectBuilder.FULL_BUILD, null);
    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(p3);
    IClasspathContainer maven2ClasspathContainer = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = maven2ClasspathContainer.getClasspathEntries();

    // order according to mvn -X. note that maven 2.0.7 and 2.1-SNAPSHOT produce different order 
    assertEquals(6, cp.length);
    assertEquals("junit-3.8.1.jar", cp[0].getPath().lastSegment());
    assertEquals("commons-digester-1.6.jar", cp[1].getPath().lastSegment());
    assertEquals("commons-beanutils-1.6.jar", cp[2].getPath().lastSegment());
    assertEquals("commons-logging-1.0.jar", cp[3].getPath().lastSegment());
    assertEquals("commons-collections-2.1.jar", cp[4].getPath().lastSegment());
    assertEquals("xml-apis-1.0.b2.jar", cp[5].getPath().lastSegment());
  }

  public void testDownloadSources_001_basic() throws Exception {
    new File(repo, "downloadsources/downloadsources-t001/0.0.1/downloadsources-t001-0.0.1-sources.jar").delete();
    new File(repo, "downloadsources/downloadsources-t002/0.0.1/downloadsources-t002-0.0.1-sources.jar").delete();

    IProject project = createExisting("downloadsources-p001", "projects/downloadsources/p001");
    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);

    // sanity check
    IClasspathEntry[] cp = container.getClasspathEntries();
    assertEquals(2, cp.length);
    assertNull(cp[0].getSourceAttachmentPath());
    assertNull(cp[1].getSourceAttachmentPath());

    // test project
    getBuildPathManager().downloadSources(project, null);
    waitForJobsToComplete();
    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();
    assertEquals(2, cp.length);
    assertEquals("downloadsources-t001-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
    assertEquals("downloadsources-t002-0.0.1-sources.jar", cp[1].getSourceAttachmentPath().lastSegment());

    {
      // cleanup
      new File(repo, "downloadsources/downloadsources-t001/0.0.1/downloadsources-t001-0.0.1-sources.jar").delete();
      new File(repo, "downloadsources/downloadsources-t002/0.0.1/downloadsources-t002-0.0.1-sources.jar").delete();
      MavenPlugin.getDefault().getMavenProjectManager().refresh(
          new MavenUpdateRequest(new IProject[] {project}, false /*offline*/, false));
      waitForJobsToComplete();
    }

    // test one entry
    getBuildPathManager().downloadSources(project, cp[0].getPath());
    waitForJobsToComplete();
    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();
    assertEquals(2, cp.length);
    assertEquals("downloadsources-t001-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
    assertNull(cp[1].getSourceAttachmentPath());

    {
      // cleanup
      new File(repo, "downloadsources/downloadsources-t001/0.0.1/downloadsources-t001-0.0.1-sources.jar").delete();
      new File(repo, "downloadsources/downloadsources-t002/0.0.1/downloadsources-t002-0.0.1-sources.jar").delete();
      MavenPlugin.getDefault().getMavenProjectManager().refresh(
          new MavenUpdateRequest(new IProject[] {project}, false /*offline*/, false));
      waitForJobsToComplete();
    }

    // test two entries
    getBuildPathManager().downloadSources(project, cp[0].getPath());
    getBuildPathManager().downloadSources(project, cp[1].getPath());
    waitForJobsToComplete();
    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();
    assertEquals(2, cp.length);
    assertEquals("downloadsources-t001-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
    assertEquals("downloadsources-t002-0.0.1-sources.jar", cp[1].getSourceAttachmentPath().lastSegment());
  }

  public void testDownloadSources_001_sourceAttachment() throws Exception {
    new File(repo, "downloadsources/downloadsources-t001/0.0.1/downloadsources-t001-0.0.1-sources.jar").delete();
    new File(repo, "downloadsources/downloadsources-t002/0.0.1/downloadsources-t002-0.0.1-sources.jar").delete();

    IProject project = createExisting("downloadsources-p001", "projects/downloadsources/p001");
    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    final IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);

    IPath entryPath = container.getClasspathEntries()[0].getPath();

    IPath srcPath = new Path("/a");
    IPath srcRoot = new Path("/b");
    String javaDocUrl = "c";

    IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
        javaDocUrl);

    final IClasspathEntry entry = JavaCore.newLibraryEntry(entryPath, //
        srcPath, srcRoot, new IAccessRule[0], //
        new IClasspathAttribute[] {attribute}, // 
        false /*not exported*/);

    BuildPathManager buildpathManager = getBuildPathManager();

    IClasspathContainer containerSuggestion = new IClasspathContainer() {
      public IClasspathEntry[] getClasspathEntries() {
        return new IClasspathEntry[] {entry};
      }

      public String getDescription() {
        return container.getDescription();
      }

      public int getKind() {
        return container.getKind();
      }

      public IPath getPath() {
        return container.getPath();
      }
    };
    buildpathManager.updateClasspathContainer(javaProject, containerSuggestion, monitor);
    waitForJobsToComplete();

    // check custom source/javadoc
    IClasspathContainer container2 = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry entry2 = container2.getClasspathEntries()[0];
    assertEquals(entryPath, entry2.getPath());
    assertEquals(srcPath, entry2.getSourceAttachmentPath());
    assertEquals(srcRoot, entry2.getSourceAttachmentRootPath());
    assertEquals(javaDocUrl, buildpathManager.getJavadocLocation(entry2));

    File file = buildpathManager.getSourceAttachmentPropertiesFile(project);
    assertEquals(true, file.canRead());

    // check project delete
    project.delete(true, monitor);
    waitForJobsToComplete();
    assertEquals(false, file.canRead());
  }

  public void testDownloadSources_002_javadoconly() throws Exception {
    new File(repo, "downloadsources/downloadsources-t003/0.0.1/downloadsources-t003-0.0.1-javadoc.jar").delete();

    IProject project = createExisting("downloadsources-p002", "projects/downloadsources/p002");
    waitForJobsToComplete();

    // sanity check
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();
    assertEquals(1, cp.length);
    assertNull(cp[0].getSourceAttachmentPath());

    getBuildPathManager().downloadJavaDoc(project, null);
    waitForJobsToComplete();

    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();
    assertEquals(1, cp.length);
    assertNull(cp[0].getSourceAttachmentPath()); // sanity check
    assertEquals("" + cp[0], 1, getAttributeCount(cp[0], IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME));

    getBuildPathManager().downloadJavaDoc(project, null);
    waitForJobsToComplete();

    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();
    assertEquals(1, cp.length);
    assertNull(cp[0].getSourceAttachmentPath()); // sanity check
    assertEquals("" + cp[0], 1, getAttributeCount(cp[0], IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME));

  }

  public void testDownloadSources_003_customRemoteRepository() throws Exception {
    File file = new File(repo, "downloadsources/downloadsources-t004/0.0.1/downloadsources-t004-0.0.1-sources.jar");
    assertTrue(!file.exists() || file.delete());

    IProject project = createExisting("downloadsources-p003", "projects/downloadsources/p003");
    waitForJobsToComplete();

    // sanity check
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();
    assertEquals(1, cp.length);
    assertNull(cp[0].getSourceAttachmentPath());

    getBuildPathManager().downloadSources(project, cp[0].getPath());
    waitForJobsToComplete();

    javaProject = JavaCore.create(project);
    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();
    assertEquals(1, cp.length);
    assertEquals("downloadsources-t004-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
  }

  private static int getAttributeCount(IClasspathEntry entry, String name) {
    IClasspathAttribute[] attrs = entry.getExtraAttributes();
    int count = 0;
    for(int i = 0; i < attrs.length; i++ ) {
      if(name.equals(attrs[i].getName())) {
        count++ ;
      }
    }
    return count;
  }

  public void testDownloadSources_004_testsClassifier() throws Exception {
    File file = new File(repo, "downloadsources/downloadsources-t005/0.0.1/downloadsources-t005-0.0.1-test-sources.jar");
    assertTrue(!file.exists() || file.delete());

    IProject project = createExisting("downloadsources-p004", "projects/downloadsources/p004");
    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();

    // sanity check
    assertEquals("downloadsources-t005-0.0.1-tests.jar", cp[1].getPath().lastSegment());

    getBuildPathManager().downloadSources(project, cp[1].getPath());
    waitForJobsToComplete();
    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();

    assertEquals(2, cp.length);
    assertEquals("downloadsources-t005-0.0.1-test-sources.jar", cp[1].getSourceAttachmentPath().lastSegment());
  }

  public void testDownloadSources_004_classifier() throws Exception {
    File file = new File(repo, "downloadsources/downloadsources-t006/0.0.1/downloadsources-t006-0.0.1-sources.jar");
    assertTrue(!file.exists() || file.delete());

    IProject project = createExisting("downloadsources-p005", "projects/downloadsources/p005");
    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();

    // sanity check
    assertEquals("downloadsources-t006-0.0.1-jdk14.jar", cp[0].getPath().lastSegment());

    getBuildPathManager().downloadSources(project, cp[0].getPath());
    waitForJobsToComplete();
    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();

    assertEquals(1, cp.length);
    assertNotNull(cp[0].getSourceAttachmentPath());
    assertEquals(cp[0].getSourceAttachmentPath().toString(), "downloadsources-t006-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
  }

  public void testDownloadSources_006_nonMavenProject() throws Exception {
    IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
    IndexInfo indexInfo = new IndexInfo("remoterepo-local", new File("remoterepo"), null, IndexInfo.Type.LOCAL, false);
    indexManager.addIndex(indexInfo, false);
    indexManager.reindex(indexInfo.getIndexName(), monitor);
    indexManager.addIndex(new IndexInfo("remoterepo", null, "file:remoterepo", IndexInfo.Type.REMOTE, false), false);

    IProject project = createExisting("downloadsources-p006", "projects/downloadsources/p006");

    File log4jJar = new File("remoterepo/log4j/log4j/1.2.13/log4j-1.2.13.jar");
    Path log4jPath = new Path(log4jJar.getAbsolutePath());

    File junitJar = new File("remoterepo/junit/junit/3.8.1/junit-3.8.1.jar");
    Path junitPath = new Path(junitJar.getAbsolutePath());

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] origCp = javaProject.getRawClasspath();
    IClasspathEntry[] cp = new IClasspathEntry[origCp.length + 2];
    System.arraycopy(origCp, 0, cp, 0, origCp.length);

    cp[cp.length - 2] = JavaCore.newLibraryEntry(log4jPath, null, null, true);
    cp[cp.length - 1] = JavaCore.newLibraryEntry(junitPath, null, null, false);

    javaProject.setRawClasspath(cp, monitor);

    getBuildPathManager().downloadSources(project, log4jPath);
    waitForJobsToComplete();

    cp = javaProject.getRawClasspath();

    assertEquals(log4jPath, cp[cp.length - 2].getPath());
    assertEquals("log4j-1.2.13-sources.jar", cp[cp.length - 2].getSourceAttachmentPath().lastSegment());
    assertEquals(true, cp[cp.length - 2].isExported());

    getBuildPathManager().downloadSources(project, junitPath);
    waitForJobsToComplete();

    assertEquals(junitPath, cp[cp.length - 1].getPath());
    assertEquals("junit-3.8.1-sources.jar", cp[cp.length - 1].getSourceAttachmentPath().lastSegment());
    assertEquals(false, cp[cp.length - 1].isExported());
  }

  private BuildPathManager getBuildPathManager() {
    return MavenJdtPlugin.getDefault().getBuildpathManager();
  }

  public void testClassifiers() throws Exception {
    IProject p1 = createExisting("classifiers-p1", "projects/classifiers/classifiers-p1");
    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(p1);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();

    assertEquals(2, cp.length);
    assertEquals("classifiers-p2-0.0.1.jar", cp[0].getPath().lastSegment());
    assertEquals("classifiers-p2-0.0.1-tests.jar", cp[1].getPath().lastSegment());

    createExisting("classifiers-p2", "projects/classifiers/classifiers-p2");
    waitForJobsToComplete();

    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();

    assertEquals(1, cp.length);
    assertEquals("classifiers-p2", cp[0].getPath().lastSegment());
  }

  public void testCreateSimpleProject() throws CoreException {
    final MavenPlugin plugin = MavenPlugin.getDefault();

    final boolean modules = true;
    IProject project = createSimpleProject("simple-project", null, modules);

    ResolverConfiguration configuration = plugin.getMavenProjectManager().getResolverConfiguration(project);
    assertEquals(modules, configuration.shouldIncludeModules());
    
    IJavaProject javaProject = JavaCore.create(project);

    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    assertEquals(Arrays.toString(rawClasspath), 6, rawClasspath.length);
    assertEquals("/simple-project/src/main/java", rawClasspath[0].getPath().toString());
    assertEquals("/simple-project/src/main/resources", rawClasspath[1].getPath().toString());
    assertEquals("/simple-project/src/test/java", rawClasspath[2].getPath().toString());
    assertEquals("/simple-project/src/test/resources", rawClasspath[3].getPath().toString());
    assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[4].getPath().toString());
    assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[5].getPath().toString());
   
    IClasspathEntry[] entries = getMavenContainerEntries(project);
    assertEquals(Arrays.toString(entries), 1, entries.length);
    assertEquals(IClasspathEntry.CPE_LIBRARY, entries[0].getEntryKind());
    assertEquals("junit-3.8.1.jar", entries[0].getPath().lastSegment());
    
    assertTrue(project.getFile("pom.xml").exists());
    assertTrue(project.getFolder("src/main/java").exists());
    assertTrue(project.getFolder("src/test/java").exists());
    assertTrue(project.getFolder("src/main/resources").exists());
    assertTrue(project.getFolder("src/test/resources").exists());
  }

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
  }

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

  public void testCompilerSettingsJsr14() throws Exception {
    deleteProject("compilerSettingsJsr14");

    ResolverConfiguration configuration = new ResolverConfiguration();
    ProjectImportConfiguration projectImportConfiguration = new ProjectImportConfiguration(configuration);
    importProject("compilerSettingsJsr14", "projects/compilerSettingsJsr14", projectImportConfiguration);

    waitForJobsToComplete();

    IProject project = workspace.getRoot().getProject("compilerSettingsJsr14");
    assertTrue(project.exists());

    assertMarkers(project, 0);

    IJavaProject javaProject = JavaCore.create(project);
    assertEquals("1.5", javaProject.getOption(JavaCore.COMPILER_SOURCE, true));
    assertEquals("1.5", javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true));

    IClasspathEntry jreEntry = getJreContainer(javaProject.getRawClasspath());
    assertEquals("J2SE-1.5", JavaRuntime.getExecutionEnvironmentId(jreEntry.getPath()));
  }

  public void testCompilerSettings14() throws Exception {
    deleteProject("compilerSettings14");

    ResolverConfiguration configuration = new ResolverConfiguration();
    ProjectImportConfiguration projectImportConfiguration = new ProjectImportConfiguration(configuration);
    importProject("compilerSettings14", "projects/compilerSettings14", projectImportConfiguration);

    waitForJobsToComplete();

    IProject project = workspace.getRoot().getProject("compilerSettings14");
    assertTrue(project.exists());

    // Build path specifies execution environment J2SE-1.4. 
    // There are no JREs in the workspace strictly compatible with this environment.
    assertMarkers(project, 0);

    IJavaProject javaProject = JavaCore.create(project);
    assertEquals("1.4", javaProject.getOption(JavaCore.COMPILER_SOURCE, true));
    assertEquals("1.4", javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true));

    IClasspathEntry jreEntry = getJreContainer(javaProject.getRawClasspath());
    assertEquals("J2SE-1.4", JavaRuntime.getExecutionEnvironmentId(jreEntry.getPath()));
  }

  private IClasspathEntry getJreContainer(IClasspathEntry[] entries) {
    for(IClasspathEntry entry : entries) {
      if(JavaRuntime.newDefaultJREContainerPath().isPrefixOf(entry.getPath())) {
        return entry;
      }
    }
    return null;
  }

  public void testMavenBuilderOrder() throws Exception {
    IProject project = createExisting("builderOrder", "projects/builderOrder");
    IProjectDescription description = project.getDescription();

    ICommand[] buildSpec = description.getBuildSpec();
    ICommand javaBuilder = buildSpec[0];
    ICommand mavenBuilder = buildSpec[1];

    verifyNaturesAndBuilders(project);

    ResolverConfiguration configuration = new ResolverConfiguration();
    String goalToExecute = "";

    IProjectConfigurationManager configurationManager = plugin.getProjectConfigurationManager();

    configurationManager.updateProjectConfiguration(project, configuration, goalToExecute, monitor);
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
  public void testUpdateProjectConfigurationWithWorkspace() throws Exception {
    deleteProject("MNGECLIPSE-1133parent");
    deleteProject("MNGECLIPSE-1133child");

    final IProject project1 = createProject("MNGECLIPSE-1133parent", "projects/MNGECLIPSE-1133/parent/pom.xml");
    final IProject project2 = createProject("MNGECLIPSE-1133child", "projects/MNGECLIPSE-1133/child/pom.xml");

    NullProgressMonitor monitor = new NullProgressMonitor();
    IProjectConfigurationManager configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();

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

    assertMarkers(project2, 0);

    // update configuration
    configurationManager.updateProjectConfiguration(project2, configuration, "", monitor);
    waitForJobsToComplete();

    assertMarkers(project2, 0);
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

  private IProject createSimpleProject(final String projectName, final IPath location, final boolean modules)
      throws CoreException {
    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        Model model = new Model();
        model.setGroupId(projectName);
        model.setArtifactId(projectName);
        model.setVersion("0.0.1-SNAPSHOT");
        model.setModelVersion("4.0.0");
        
        Dependency dependency = new Dependency();
        dependency.setGroupId("junit");
        dependency.setArtifactId("junit");
        dependency.setVersion("3.8.1");
        
        model.addDependency(dependency);

        String[] directories = {"src/main/java", "src/test/java", "src/main/resources", "src/test/resources"};

        ProjectImportConfiguration config = new ProjectImportConfiguration();
        config.getResolverConfiguration().setIncludeModules(modules);

        plugin.getProjectConfigurationManager().createSimpleProject(project, location, model, directories,
            config, monitor);
      }
    }, plugin.getProjectConfigurationManager().getRule(), IWorkspace.AVOID_UPDATE, monitor);

    return project;
  }

  public void testSimpleProjectInExternalLocation() throws CoreException, IOException {
    final MavenPlugin plugin = MavenPlugin.getDefault();
    final boolean modules = true;

    File tmp = File.createTempFile("m2eclipse", "test");
    tmp.delete(); //deleting a tmp file so we can use the name for a folder

    final String projectName1 = "external-simple-project-1";
    IProject project1 = createSimpleProject(projectName1, new Path(tmp.getAbsolutePath()).append(projectName1), modules);
    ResolverConfiguration configuration = plugin.getMavenProjectManager().getResolverConfiguration(project1);
    assertEquals(modules, configuration.shouldIncludeModules());

    final String projectName2 = "external-simple-project-2";
    File existingFolder = new File(tmp, projectName2);
    existingFolder.mkdirs();
    new File(existingFolder, IMavenConstants.POM_FILE_NAME).createNewFile();
    try {
      createSimpleProject(projectName2, new Path(tmp.getAbsolutePath()).append(projectName2), modules);
      fail("Project creation should fail if the POM exists in the target folder");
    } catch(CoreException e) {
      final String msg = IMavenConstants.POM_FILE_NAME + " already exists";
      assertTrue("Project creation should throw a \"" + msg + "\" exception if the POM exists in the target folder", e
          .getMessage().indexOf(msg) > 0);
    }

    tmp.delete();
  }

  //FIXME FB 30/10/2008 : Archetype tests are disabled while MNGECLIPSE-948 is not fixed 
  public void XXXtestArchetypeProject() throws CoreException {
    MavenPlugin plugin = MavenPlugin.getDefault();
    boolean modules = true;
    Archetype quickStart = findQuickStartArchetype();

    IProject project = createArchetypeProject("archetype-project", null, quickStart, modules);

    ResolverConfiguration configuration = plugin.getMavenProjectManager().getResolverConfiguration(project);
    assertEquals(modules, configuration.shouldIncludeModules());
  }

  public void XXXtestArchetypeProjectInExternalLocation() throws CoreException, IOException {
    final MavenPlugin plugin = MavenPlugin.getDefault();
    final boolean modules = true;
    Archetype quickStart = findQuickStartArchetype();

    File tmp = File.createTempFile("m2eclipse", "test");
    tmp.delete(); //deleting a tmp file so we can use the name for a folder

    final String projectName1 = "external-archetype-project-1";
    IProject project1 = createArchetypeProject(projectName1, new Path(tmp.getAbsolutePath()).append(projectName1),
        quickStart, modules);
    ResolverConfiguration configuration = plugin.getMavenProjectManager().getResolverConfiguration(project1);
    assertEquals(modules, configuration.shouldIncludeModules());

    final String projectName2 = "external-archetype-project-2";
    File existingFolder = new File(tmp, projectName2);
    existingFolder.mkdirs();
    new File(existingFolder, IMavenConstants.POM_FILE_NAME).createNewFile();
    try {
      createArchetypeProject(projectName2, new Path(tmp.getAbsolutePath()).append(projectName2), quickStart, modules);
      fail("Project creation should fail if the POM exists in the target folder");
    } catch(CoreException e) {
      // this is supposed to happen
    }

    tmp.delete();
  }

  private Archetype findQuickStartArchetype() throws CoreException {
    final MavenPlugin plugin = MavenPlugin.getDefault();

    @SuppressWarnings("unchecked")
    List<Archetype> archetypes = plugin.getArchetypeManager().getArchetypeCatalogFactory("internal")
        .getArchetypeCatalog().getArchetypes();
    for(Archetype archetype : archetypes) {
      if("org.apache.maven.archetypes".equals(archetype.getGroupId())
          && "maven-archetype-quickstart".equals(archetype.getArtifactId()) && "1.0".equals(archetype.getVersion())) {
        return archetype;
      }
    }

    fail("maven-archetype-quickstart archetype not found in the internal catalog");
    return null;
  }

  private IProject createArchetypeProject(final String projectName, final IPath location, final Archetype archetype,
      final boolean modules) throws CoreException {
    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        ResolverConfiguration resolverConfiguration = new ResolverConfiguration();
        resolverConfiguration.setIncludeModules(modules);
        ProjectImportConfiguration pic = new ProjectImportConfiguration(resolverConfiguration);

        plugin.getProjectConfigurationManager().createArchetypeProject(project, location, archetype, //
            projectName, projectName, "0.0.1-SNAPSHOT", "jar", new Properties(), pic, monitor);
      }
    }, plugin.getProjectConfigurationManager().getRule(), IWorkspace.AVOID_UPDATE, monitor);

    return project;
  }

  //Local repo set by the BuildManager is based on a cached version of MavenEmbedder, not the one configured in setup.
  public void XXXtestMNGECLIPSE_1047_localRepoPath() {
   IPath m2_repo = JavaCore.getClasspathVariable(BuildPathManager.M2_REPO);
   assertEquals(repo.toString(), m2_repo.toOSString());
  }
  
  
}
