/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.DirectoryScanner;
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
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.index.IMutableIndex;
import org.eclipse.m2e.core.internal.index.NexusIndex;
import org.eclipse.m2e.core.internal.index.NexusIndexManager;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.internal.repository.RepositoryRegistry;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;
import org.eclipse.m2e.jdt.BuildPathManager;
import org.eclipse.m2e.jdt.MavenJdtPlugin;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


/**
 * @author Eugene Kuleshov
 */
public class BuildPathManagerTest extends AbstractMavenProjectTestCase {

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

    WorkspaceHelpers.assertNoErrors(project1);

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

    WorkspaceHelpers.assertMarkers(project2, 0);
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
    for(ICommand command : p.getDescription().getBuildSpec()) {
      if(builderId.equals(command.getBuilderName())) {
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

    IProjectConfigurationManager importManager = MavenPlugin.getDefault().getProjectConfigurationManager();

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setResolveWorkspaceProjects(false);
    configuration.setActiveProfiles("");

    importManager.enableMavenNature(project1, configuration, monitor);
    importManager.enableMavenNature(project2, configuration, monitor);
    waitForJobsToComplete();

    IClasspathEntry[] project2entries = getMavenContainerEntries(project2);
    assertEquals(Arrays.asList(project2entries).toString(), 1, project2entries.length);
    assertEquals(IClasspathEntry.CPE_LIBRARY, project2entries[0].getEntryKind());
    assertEquals("MNGECLIPSE-248parent-1.0.0.jar", project2entries[0].getPath().lastSegment());
  }

  public void testProjectImportWithProfile1() throws Exception {
    deleteProject("MNGECLIPSE-353");

    ResolverConfiguration configuration = new ResolverConfiguration();
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

    WorkspaceHelpers.assertMarkers(project, 0);
  }

  public void testProjectImportWithProfile2() throws Exception {
    deleteProject("MNGECLIPSE-353");

    ResolverConfiguration configuration = new ResolverConfiguration();
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

    WorkspaceHelpers.assertMarkers(project, 0);
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

  // disabled nested modules tests 
  public void _testProjectImport002_useMavenOutputFolders() throws Exception {
    deleteProject("projectimport-p002");

    ResolverConfiguration configuration = new ResolverConfiguration();
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
    assertEquals(WorkspaceHelpers.toString(markers), 1, markers.length);
    assertEquals(WorkspaceHelpers.toString(markers), "pom.xml", markers[0].getResource().getFullPath().lastSegment());
  }

  public void testClasspathOrderWorkspace001() throws Exception {
    deleteProject("p1");
    deleteProject("p2");

    ResolverConfiguration configuration = new ResolverConfiguration();
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
    assertEquals("junit-4.0.jar", cp[2].getPath().lastSegment());
    assertEquals("easymock-1.0.jar", cp[1].getPath().lastSegment());
  }

  public void testClasspathOrderWorkspace003() throws Exception {
    deleteProject("p3");

    ResolverConfiguration configuration = new ResolverConfiguration();
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
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t001/0.0.1/"));
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t002/0.0.1/"));

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
    getBuildPathManager().scheduleDownload(project, true, false);
    waitForJobsToComplete();
    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();
    assertEquals(2, cp.length);
    assertEquals("downloadsources-t001-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
    assertEquals("downloadsources-t002-0.0.1-sources.jar", cp[1].getSourceAttachmentPath().lastSegment());

    {
      // cleanup
      deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t001/0.0.1/"));
      deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t002/0.0.1/"));
      MavenPlugin.getDefault().getMavenProjectManager().refresh(
          new MavenUpdateRequest(new IProject[] {project}, false /*offline*/, false));
      waitForJobsToComplete();
    }

    // test one entry
    getBuildPathManager().scheduleDownload(getPackageFragmentRoot(javaProject, cp[0]), true, false);
    waitForJobsToComplete();
    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();
    assertEquals(2, cp.length);
    assertEquals("downloadsources-t001-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
    assertNull(cp[1].getSourceAttachmentPath());

    {
      // cleanup
      deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t001/0.0.1/"));
      deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t002/0.0.1/"));
      MavenPlugin.getDefault().getMavenProjectManager().refresh(
          new MavenUpdateRequest(new IProject[] {project}, false /*offline*/, false));
      waitForJobsToComplete();
    }

    // test two entries
    getBuildPathManager().scheduleDownload(getPackageFragmentRoot(javaProject, cp[0]), true, false);
    getBuildPathManager().scheduleDownload(getPackageFragmentRoot(javaProject, cp[1]), true, false);
    waitForJobsToComplete();
    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();
    assertEquals(2, cp.length);
    assertEquals("downloadsources-t001-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
    assertEquals("downloadsources-t002-0.0.1-sources.jar", cp[1].getSourceAttachmentPath().lastSegment());
  }

  private IPackageFragmentRoot getPackageFragmentRoot(IJavaProject javaProject, IClasspathEntry cp)
      throws JavaModelException {
    return javaProject.findPackageFragmentRoot(cp.getPath());
  }

  private void deleteSourcesAndJavadoc(File basedir) {
    if(!basedir.exists()){
      return;
    }
    DirectoryScanner ds = new DirectoryScanner();
    ds.setBasedir(basedir);
    ds.setIncludes(new String[] {"*-sources.jar", "*-javadoc.jar", "m2e-lastUpdated.properties"});
    ds.scan();

    for(String path : ds.getIncludedFiles()) {
      new File(basedir, path).delete();
    }
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
    buildpathManager.persistAttachedSourcesAndJavadoc(javaProject, containerSuggestion, monitor);
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
    deleteProject(project);
    waitForJobsToComplete();
    assertEquals(false, file.canRead());
  }

  public void testDownloadSources_002_javadoconly() throws Exception {
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t003/0.0.1"));
    
    IProject project = createExisting("downloadsources-p002", "projects/downloadsources/p002");
    waitForJobsToComplete();

    // sanity check
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();
    assertEquals(1, cp.length);
    assertNull(cp[0].getSourceAttachmentPath());

    getBuildPathManager().scheduleDownload(project, false, true);
    waitForJobsToComplete();

    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();
    assertEquals(1, cp.length);
    assertNull(cp[0].getSourceAttachmentPath()); // sanity check
    assertEquals("" + cp[0], 1, getAttributeCount(cp[0], IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME));
  }

  public void testDownloadSources_003_customRemoteRepository() throws Exception {
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t004/0.0.1"));

    IProject project = createExisting("downloadsources-p003", "projects/downloadsources/p003");
    waitForJobsToComplete();

    // sanity check
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();
    assertEquals(1, cp.length);
    assertNull(cp[0].getSourceAttachmentPath());

    getBuildPathManager().scheduleDownload(project, true, false);
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
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t005/0.0.1"));

    IProject project = createExisting("downloadsources-p004", "projects/downloadsources/p004");
    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();

    // sanity check
    assertEquals("downloadsources-t005-0.0.1-tests.jar", cp[1].getPath().lastSegment());

    getBuildPathManager().scheduleDownload(project, true, false);
    waitForJobsToComplete();
    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();

    assertEquals(2, cp.length);
    assertEquals("downloadsources-t005-0.0.1-test-sources.jar", cp[1].getSourceAttachmentPath().lastSegment());
  }

  public void testDownloadSources_005_classifier() throws Exception {
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t006/0.0.1"));

    IProject project = createExisting("downloadsources-p005", "projects/downloadsources/p005");
    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();

    // sanity check
    assertEquals("downloadsources-t006-0.0.1-jdk14.jar", cp[0].getPath().lastSegment());

    getBuildPathManager().scheduleDownload(project, true, false);
    waitForJobsToComplete();
    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();

    assertEquals(1, cp.length);
    assertNotNull(cp[0].getSourceAttachmentPath());
    assertEquals(cp[0].getSourceAttachmentPath().toString(), "downloadsources-t006-0.0.1-sources.jar", cp[0]
        .getSourceAttachmentPath().lastSegment());
  }

  public void testDownloadSources_006_nonMavenProject() throws Exception {
    RepositoryRegistry repositoryRegistry = (RepositoryRegistry) MavenPlugin.getDefault().getRepositoryRegistry();
    repositoryRegistry.updateRegistry(monitor);

    NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();
    indexManager.getIndexUpdateJob().join();
    IMutableIndex localIndex = indexManager.getLocalIndex();
    localIndex.updateIndex(true, monitor);

    for (IRepository repository : repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_SETTINGS)) {
      if ("file:remoterepo".equals(repository.getUrl())) {
        NexusIndex remoteIndex = indexManager.getIndex(repository);
        remoteIndex.updateIndex(true, monitor); // actually scan the repo
      }
    }

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

    getBuildPathManager().scheduleDownload(javaProject.findPackageFragmentRoot(log4jPath), true, false);
    waitForJobsToComplete();

    cp = javaProject.getRawClasspath();

    assertEquals(log4jJar.getAbsoluteFile(), cp[cp.length - 2].getPath().toFile());
    assertEquals("log4j-1.2.13-sources.jar", cp[cp.length - 2].getSourceAttachmentPath().lastSegment());
    assertEquals(true, cp[cp.length - 2].isExported());

    getBuildPathManager().scheduleDownload(javaProject.findPackageFragmentRoot(junitPath), true, false);
    waitForJobsToComplete();

    assertEquals(junitJar.getAbsoluteFile(), cp[cp.length - 1].getPath().toFile());
    assertEquals("junit-3.8.1-sources.jar", cp[cp.length - 1].getSourceAttachmentPath().lastSegment());
    assertEquals(false, cp[cp.length - 1].isExported());
  }

  public void testDownloadSources_007_missingTestsSources() throws Exception {
    // see MNGECLIPSE-1777
    deleteSourcesAndJavadoc(new File(repo, "downloadsources/downloadsources-t007/0.0.1"));

    IProject project = createExisting("downloadsources-p007", "projects/downloadsources/p007");
    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();

    // sanity check
    assertEquals(1, cp.length);
    assertEquals("downloadsources-t007-0.0.1-tests.jar", cp[0].getPath().lastSegment());
    assertNull(cp[0].getSourceAttachmentPath());

    IPreferenceStore preferenceStore = plugin.getPreferenceStore();
    boolean oldDownloadSources = preferenceStore.getBoolean(MavenPreferenceConstants.P_DOWNLOAD_SOURCES);
    try {
      preferenceStore.setValue(MavenPreferenceConstants.P_DOWNLOAD_SOURCES, true);
      MavenUpdateRequest request = new MavenUpdateRequest(project, false/*offline*/, false/*updateSnapshots*/);
      plugin.getMavenProjectManager().refresh(request );
      waitForJobsToComplete();
      container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
      cp = container.getClasspathEntries();
  
      assertEquals(1, cp.length);
      assertEquals("downloadsources-t007-0.0.1-tests.jar", cp[0].getPath().lastSegment());
      assertNull(cp[0].getSourceAttachmentPath());
    } finally {
      preferenceStore.setValue(MavenPreferenceConstants.P_DOWNLOAD_SOURCES, oldDownloadSources);
    }
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

    WorkspaceHelpers.assertMarkers(project, 0);

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
    WorkspaceHelpers.assertMarkers(project, 0);

    IJavaProject javaProject = JavaCore.create(project);
    assertEquals("1.4", javaProject.getOption(JavaCore.COMPILER_SOURCE, true));
    assertEquals("1.4", javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true));
    assertEquals("123", javaProject.getOption(JavaCore.COMPILER_PB_MAX_PER_UNIT, true));

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

    WorkspaceHelpers.assertMarkers(project2, 0);

    // update configuration
    configurationManager.updateProjectConfiguration(project2, configuration, "", monitor);
    waitForJobsToComplete();

    WorkspaceHelpers.assertMarkers(project2, 0);
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

  private IProject createSimpleProject(final String projectName, final IPath location)
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

        // used to derive the source/target options so lock it down
        Plugin buildPlugin = new Plugin();
        buildPlugin.setArtifactId("maven-compiler-plugin");
        buildPlugin.setVersion("2.3");

        model.setBuild(new Build());
        model.getBuild().addPlugin(buildPlugin);

        String[] directories = {"src/main/java", "src/test/java", "src/main/resources", "src/test/resources"};

        ProjectImportConfiguration config = new ProjectImportConfiguration();

        plugin.getProjectConfigurationManager().createSimpleProject(project, location, model, directories, config,
            monitor);
      }
    }, plugin.getProjectConfigurationManager().getRule(), IWorkspace.AVOID_UPDATE, monitor);

    return project;
  }

  public void testSimpleProjectInExternalLocation() throws CoreException, IOException {
    final MavenPlugin plugin = MavenPlugin.getDefault();

    File tmp = File.createTempFile("m2eclipse", "test");
    tmp.delete(); //deleting a tmp file so we can use the name for a folder

    final String projectName1 = "external-simple-project-1";
    IProject project1 = createSimpleProject(projectName1, new Path(tmp.getAbsolutePath()).append(projectName1));

    final String projectName2 = "external-simple-project-2";
    File existingFolder = new File(tmp, projectName2);
    existingFolder.mkdirs();
    new File(existingFolder, IMavenConstants.POM_FILE_NAME).createNewFile();
    try {
      createSimpleProject(projectName2, new Path(tmp.getAbsolutePath()).append(projectName2));
      fail("Project creation should fail if the POM exists in the target folder");
    } catch(CoreException e) {
      final String msg = IMavenConstants.POM_FILE_NAME + " already exists";
      assertTrue("Project creation should throw a \"" + msg + "\" exception if the POM exists in the target folder", e
          .getMessage().indexOf(msg) > 0);
    }

    tmp.delete();
  }

  public void testArchetypeProject() throws CoreException {
    Archetype quickStart = findQuickStartArchetype();

    IProject project = createArchetypeProject("archetype-project", null, quickStart);

    assertNotNull(JavaCore.create(project)); // TODO more meaningful assertion 
  }

  public void testArchetypeProjectInExternalLocation() throws CoreException, IOException {
    final MavenPlugin plugin = MavenPlugin.getDefault();
    Archetype quickStart = findQuickStartArchetype();

    File tmp = File.createTempFile("m2eclipse", "test");
    tmp.delete(); //deleting a tmp file so we can use the name for a folder

    final String projectName1 = "external-archetype-project-1";
    IProject project1 = createArchetypeProject(projectName1, new Path(tmp.getAbsolutePath()).append(projectName1),
        quickStart);
    assertNotNull(JavaCore.create(project1)); // TODO more meaningful assertion 

    final String projectName2 = "external-archetype-project-2";
    File existingFolder = new File(tmp, projectName2);
    existingFolder.mkdirs();
    new File(existingFolder, IMavenConstants.POM_FILE_NAME).createNewFile();
    try {
      createArchetypeProject(projectName2, new Path(tmp.getAbsolutePath()).append(projectName2), quickStart);
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

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        ResolverConfiguration resolverConfiguration = new ResolverConfiguration();
        ProjectImportConfiguration pic = new ProjectImportConfiguration(resolverConfiguration);

        plugin.getProjectConfigurationManager().createArchetypeProject(project, location, archetype, //
            projectName, projectName, "0.0.1-SNAPSHOT", "jar", new Properties(), pic, monitor);
      }
    }, plugin.getProjectConfigurationManager().getRule(), IWorkspace.AVOID_UPDATE, monitor);

    return project;
  }

  public void testMNGECLIPSE_1047_localRepoPath() {
    IPath m2_repo = JavaCore.getClasspathVariable(BuildPathManager.M2_REPO);
    //Local repo set by the BuildManager was based on a cached version of MavenEmbedder, not the one configured in setup.
    assertEquals(repo.toString(), m2_repo.toOSString());
  }

  public void testMNGECLIPSE_696_compiler_includes_excludes() throws Exception {
    final String projectName = "MNGECLIPSE-696";

    deleteProject(projectName);

    final ResolverConfiguration configuration = new ResolverConfiguration();
    final IProject project = importProject("projects/" + projectName + "/pom.xml", configuration);
    waitForJobsToComplete();

    WorkspaceHelpers.assertMarkers(project, 0);

    final IJavaProject javaProject = JavaCore.create(project);
    final IClasspathEntry[] cp = javaProject.getRawClasspath();
    final IClasspathEntry cpMain = cp[0];
    final IClasspathEntry cpTest = cp[1];

    assertEquals(new Path("/" + projectName + "/src/main/java"), cpMain.getPath());
    assertEquals(new Path("/" + projectName + "/src/test/java"), cpTest.getPath());

    final IPath[] inclusionsMain = cpMain.getInclusionPatterns();
    assertEquals(2, inclusionsMain.length);
    assertEquals(new Path("org/apache/maven/"), inclusionsMain[0]);
    assertEquals(new Path("org/maven/ide/eclipse/"), inclusionsMain[1]);

    final IPath[] exclusionsMain = cpMain.getExclusionPatterns();
    assertEquals(1, exclusionsMain.length);
    assertEquals(new Path("org/maven/ide/eclipse/tests/"), exclusionsMain[0]);

    final IPath[] inclusionsTest = cpTest.getInclusionPatterns();
    assertEquals(1, inclusionsTest.length);
    assertEquals(new Path("org/apache/maven/tests/"), inclusionsTest[0]);

    final IPath[] exclusionsTest = cpTest.getExclusionPatterns();
    assertEquals(1, exclusionsTest.length);
    assertEquals(new Path("org/apache/maven/tests/Excluded.java"), exclusionsTest[0]);
  }

  public void testMNGECLIPSE_2367_same_sources_resources() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-2367_sourcesResourcesOverlap/project01/pom.xml");
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

  public void testMNGECLIPSE_2367_sources_encloses_resources() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-2367_sourcesResourcesOverlap/project02/pom.xml");
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

  public void testMNGECLIPSE_2367_testSources_encloses_resources() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-2367_sourcesResourcesOverlap/project03/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    // this project has resources and test-sources folders overlap. m2e does not support this properly
    // so it is good enough if we don't fail with nasty exceptions
    // see https://issues.sonatype.org/browse/MNGECLIPSE-2367

    // make sure resources do get copied to target/classes folder
    assertTrue(project.getFile("target/classes/test.properties").isAccessible());
  }

  public void testMNGECLIPSE_2433_resourcesOutsideBasdir() throws Exception {
    IProject[] projects = importProjects("projects/MNGECLIPSE-2433_resourcesOutsideBasdir", new String[] {
        "project01/pom.xml", "project02/pom.xml"}, new ResolverConfiguration());
    projects[1].build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    // project02 has resources folder outside of it's basedir. m2e does not support this and probably never will.
    // so it is good enough we don't fail with nasty exceptions 
    // ideally we need to add a warning marker on the offending pom.xml element
    // https://issues.sonatype.org/browse/MNGECLIPSE-2433
    
    IJavaProject javaProject = JavaCore.create(projects[1]);
    IClasspathEntry[] cp = javaProject.getRawClasspath();

    assertEquals(2, cp.length);
  }

}
