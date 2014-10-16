/*******************************************************************************
 * Copyright (c) 2008-2014 Sonatype, Inc. and others
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.externaltools.internal.model.BuilderCoreUtils;
import org.eclipse.core.externaltools.internal.model.ExternalToolBuilder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.codehaus.plexus.util.FileUtils;

import org.apache.maven.archetype.catalog.Archetype;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.preferences.MavenConfigurationImpl;
import org.eclipse.m2e.core.internal.preferences.ProblemSeverity;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryRefreshJob;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.FilexWagon;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


public class ProjectConfigurationManagerTest extends AbstractMavenProjectTestCase {

  public void testBasedirRenameRequired() throws Exception {
    testBasedirRename(MavenProjectInfo.RENAME_REQUIRED);

    IWorkspaceRoot root = workspace.getRoot();
    IProject project = root.getProject("maven-project");
    assertNotNull(project);
    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getMavenProject("MNGECLIPSE-1793_basedirRename",
        "maven-project", "0.0.1-SNAPSHOT");
    assertNotNull(facade);
    assertEquals(project, facade.getProject());
  }

  public void testBasedirRenameNo() throws Exception {
    testBasedirRename(MavenProjectInfo.RENAME_NO);

    IWorkspaceRoot root = workspace.getRoot();
    IProject project = root.getProject("mavenNNNNNNN");
    assertNotNull(project);
    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getMavenProject("MNGECLIPSE-1793_basedirRename",
        "maven-project", "0.0.1-SNAPSHOT");
    assertNotNull(facade);
    assertEquals(project, facade.getProject());
  }

  private void testBasedirRename(int renameRequired) throws IOException, CoreException {
    IWorkspaceRoot root = workspace.getRoot();

    String pathname = "projects/MNGECLIPSE-1793_basedirRename/mavenNNNNNNN";
    File src = new File(pathname);
    File dst = new File(root.getLocation().toFile(), src.getName());
    copyDir(src, dst);

    final ArrayList<MavenProjectInfo> projectInfos = new ArrayList<MavenProjectInfo>();
    projectInfos.add(new MavenProjectInfo("label", new File(dst, "pom.xml"), null, null));
    projectInfos.get(0).setBasedirRename(renameRequired);

    final ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration(new ResolverConfiguration());

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        MavenPlugin.getProjectConfigurationManager().importProjects(projectInfos, importConfiguration, monitor);
      }
    }, MavenPlugin.getProjectConfigurationManager().getRule(), IWorkspace.AVOID_UPDATE, monitor);
  }

  public void testWorkspaceResolutionOfInterModuleDependenciesDuringImport() throws Exception {
    String oldSettings = mavenConfiguration.getUserSettingsFile();
    try {
      injectFilexWagon();
      FilexWagon.setRequestFilterPattern("test/.*", true);
      IJobChangeListener jobChangeListener = new JobChangeAdapter() {
        public void scheduled(IJobChangeEvent event) {
          if(event.getJob() instanceof ProjectRegistryRefreshJob) {
            // cancel all those concurrent refresh jobs, we want to monitor the main thread only
            event.getJob().cancel();
          }
        }
      };
      Job.getJobManager().addJobChangeListener(jobChangeListener);
      mavenConfiguration.setUserSettingsFile(new File("projects/MNGECLIPSE-1990/settings.xml").getAbsolutePath());
      List<String> requests;
      try {
        importProjects("projects/MNGECLIPSE-1990", new String[] {"pom.xml", "dependent/pom.xml", "dependency/pom.xml",
            "parent/pom.xml"}, new ResolverConfiguration());
        requests = FilexWagon.getRequests();
      } finally {
        Job.getJobManager().removeJobChangeListener(jobChangeListener);
      }
      assertTrue("Dependency resolution was attempted from remote repository: " + requests, requests.isEmpty());
    } finally {
      mavenConfiguration.setUserSettingsFile(oldSettings);
    }
  }

  public void testResolutionOfArchetypeFromRepository() throws Exception {
    String oldSettings = mavenConfiguration.getUserSettingsFile();
    try {
      mavenConfiguration.setUserSettingsFile(new File("settingsWithDefaultRepos.xml").getAbsolutePath());

      Archetype archetype = new Archetype();
      archetype.setGroupId("org.eclipse.m2e.its");
      archetype.setArtifactId("mngeclipse-2110");
      archetype.setVersion("1.0");
      archetype.setRepository("http://bad.host"); // should be mirrored by settings

      IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("mngeclipse-2110");
      ProjectImportConfiguration pic = new ProjectImportConfiguration(new ResolverConfiguration());

      IProjectConfigurationManager manager = MavenPlugin.getProjectConfigurationManager();
      manager.createArchetypeProject(project, null, archetype, "m2e.its", "mngeclipse-2110", "1.0-SNAPSHOT", "jar",
          new Properties(), pic, monitor);

      assertTrue(project.isAccessible());
    } finally {
      mavenConfiguration.setUserSettingsFile(oldSettings);
    }
  }

  public void testExtractionOfCompilerSettingsDespiteErrorsInExecutionPlan() throws Exception {
    IProject[] projects = importProjects("projects/compilerSettingsPluginError", new String[] {"pom.xml"},
        new ResolverConfiguration());
    assertNotNull(projects);
    assertEquals(1, projects.length);
    IProject project = projects[0];
    assertNotNull(project);
    assertNoErrors(project);

    IJavaProject javaProject = JavaCore.create(project);
    assertEquals("1.6", javaProject.getOption(JavaCore.COMPILER_SOURCE, true));
    assertEquals("1.5", javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true));
  }

  public void testStaleProjectConfigurationMarker() throws Exception {
    IProject project = importProject("projects/staleconfiguration/basic/pom.xml");
    assertNoErrors(project);

    final IMavenProjectFacade projectFacade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);

    copyContent(project, new File("projects/staleconfiguration/basic/pom-changed.xml"), "pom.xml");
    WorkspaceHelpers.assertMarker(IMavenConstants.MARKER_CONFIGURATION_ID, IMarker.SEVERITY_ERROR,
        Messages.ProjectConfigurationUpdateRequired, null, null, project);

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(projectFacade.getProject(), monitor);
      }
    }, monitor);
    assertNoErrors(project);
  }

  public void testStaleProjectConfigurationMarkerAfterWorkspaceRestart() throws Exception {
    IProject project = importProject("projects/staleconfiguration/basic/pom.xml");
    assertNoErrors(project);

    final IMavenProjectFacade projectFacade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);

    deserializeFromWorkspaceState(projectFacade);

    copyContent(project, new File("projects/staleconfiguration/basic/pom-changed.xml"), "pom.xml");
    WorkspaceHelpers.assertMarker(IMavenConstants.MARKER_CONFIGURATION_ID, IMarker.SEVERITY_ERROR,
        Messages.ProjectConfigurationUpdateRequired, null, null, project);

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(projectFacade.getProject(), monitor);
      }
    }, monitor);
    assertNoErrors(project);
  }

  public void testStaleProjectConfigurationMarkerAfterFixingMissingBuildExtension() throws Exception {
    IProject project = importProjects("projects/staleconfiguration/missingextension", new String[] {"pom.xml"},
        new ResolverConfiguration(), true)[0];

    WorkspaceHelpers.assertMarker(IMavenConstants.MARKER_POM_LOADING_ID, IMarker.SEVERITY_ERROR, null, null, "pom.xml",
        project);

    copyContent(project, new File("projects/staleconfiguration/missingextension/pom-changed.xml"), "pom.xml");
    WorkspaceHelpers.assertNoErrors(project);
    WorkspaceHelpers.assertNoWarnings(project);

    final IMavenProjectFacade projectFacade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(projectFacade.getProject(), monitor);
      }
    }, monitor);
    assertNoErrors(project);
  }

  public void testImportJavaProjectWithUnknownPackaging() throws Exception {
    IProject project = importProject("projects/detectJavaProject/default/pom.xml");
    assertTrue("default compilerId", project.hasNature(JavaCore.NATURE_ID));

    project = importProject("projects/detectJavaProject/explicitJavac/pom.xml");
    assertTrue("compilerId=javac", project.hasNature(JavaCore.NATURE_ID));

    project = importProject("projects/detectJavaProject/nonJavac/pom.xml");
    assertFalse("compilerId=jikes", project.hasNature(JavaCore.NATURE_ID));
  }

  public void testAddRemoveMavenBuilder() throws Exception {
    IProject project = createExisting("testAddRemoveMavenBuilder",
        "projects/AddRemoveMavenBuilder/testAddRemoveMavenBuilder");
    IProjectDescription projectDescription = project.getDescription();
    // The project has only java builder
    assertEquals(1, projectDescription.getBuildSpec().length);
    assertNotSame(IMavenConstants.BUILDER_ID, projectDescription.getBuildSpec()[0].getBuilderName());

    // Add the maven builder
    MavenPlugin.getProjectConfigurationManager().addMavenBuilder(project, null /*description*/, monitor);
    projectDescription = project.getDescription();
    assertEquals(2, projectDescription.getBuildSpec().length);
    assertEquals(IMavenConstants.BUILDER_ID, projectDescription.getBuildSpec()[1].getBuilderName());

    // Add the maven builder again
    MavenPlugin.getProjectConfigurationManager().addMavenBuilder(project, null /*description*/, monitor);
    projectDescription = project.getDescription();
    assertEquals(2, projectDescription.getBuildSpec().length);
    assertEquals(IMavenConstants.BUILDER_ID, projectDescription.getBuildSpec()[1].getBuilderName());

    // Remove the maven builder
    MavenPlugin.getProjectConfigurationManager().removeMavenBuilder(project, null /*description*/, monitor);
    projectDescription = project.getDescription();
    assertEquals(1, projectDescription.getBuildSpec().length);
    assertNotSame(IMavenConstants.BUILDER_ID, projectDescription.getBuildSpec()[0].getBuilderName());

    // Remove the maven builder again
    MavenPlugin.getProjectConfigurationManager().removeMavenBuilder(project, null /*description*/, monitor);
    projectDescription = project.getDescription();
    assertEquals(1, projectDescription.getBuildSpec().length);
    assertNotSame(IMavenConstants.BUILDER_ID, projectDescription.getBuildSpec()[0].getBuilderName());
  }

  public void testAddRemoveMavenBuilderDisabled() throws Exception {
    IProject project = createExisting("testAddRemoveMavenDisabled",
        "projects/AddRemoveMavenBuilder/testAddRemoveMavenBuilderDisabled");
    IProjectDescription projectDescription = project.getDescription();
    // The project has only java builder
    assertEquals(2, projectDescription.getBuildSpec().length);
    String firstBuilderId = projectDescription.getBuildSpec()[0].getBuilderName();
    assertNotSame(IMavenConstants.BUILDER_ID, firstBuilderId);
    assertEquals(ExternalToolBuilder.ID, projectDescription.getBuildSpec()[1].getBuilderName());
    String launchConfigHandleArg = (String) projectDescription.getBuildSpec()[1].getArguments().get(
        BuilderCoreUtils.LAUNCH_CONFIG_HANDLE);
    assertNotNull(launchConfigHandleArg);
    assertTrue(launchConfigHandleArg.contains(IMavenConstants.BUILDER_ID));

    // Add the maven builder
    // The maven builder is there, but it is disabled - since detecting that would require m2e core to depend on internal m2e classes,
    // we just ignore the disabled maven builder
    MavenPlugin.getProjectConfigurationManager().addMavenBuilder(project, null /*description*/, monitor);
    projectDescription = project.getDescription();
    assertEquals(3, projectDescription.getBuildSpec().length);
    assertEquals(firstBuilderId, projectDescription.getBuildSpec()[0].getBuilderName());
    assertEquals(ExternalToolBuilder.ID, projectDescription.getBuildSpec()[1].getBuilderName());
    assertEquals(IMavenConstants.BUILDER_ID, projectDescription.getBuildSpec()[2].getBuilderName());

    // Remove the maven builder
    MavenPlugin.getProjectConfigurationManager().removeMavenBuilder(project, null /*description*/, monitor);
    projectDescription = project.getDescription();
    assertEquals(2, projectDescription.getBuildSpec().length);
    assertEquals(firstBuilderId, projectDescription.getBuildSpec()[0].getBuilderName());
    assertEquals(ExternalToolBuilder.ID, projectDescription.getBuildSpec()[1].getBuilderName());
  }

  public void testBasicUpdateConfiguration() throws Exception {
    IProject project = importProject("projects/projectimport/p001/pom.xml");
    IProjectConfigurationManager manager = MavenPlugin.getProjectConfigurationManager();
    // make sure #updateProjectConfiguration(MavenUpdateRequest, IProgressMonitor) does not blow up
    manager.updateProjectConfiguration(new MavenUpdateRequest(project, true, false), monitor);
  }

  public void test447460MultipleUpdateConfiguration() throws Exception {
    // the project import already performs a configuration !!!
    IProject project = importProject("projects/447460_MultipleUpdateConfiguration/pom.xml");
    IJavaProject javaProject = JavaCore.create(project);
    // check whether everything has been imported correctly
    List<IClasspathEntry> cpEntries = filterClasspath(javaProject.getRawClasspath(), IClasspathEntry.CPE_SOURCE);
    assertNotNull(cpEntries);
    assertEquals("Invalid number of classpath entries", 4, cpEntries.size());
    for(IClasspathEntry cpEntry : cpEntries) {
      IPath[] exclusions = cpEntry.getExclusionPatterns();
      assertNotNull(exclusions);
      assertEquals("Classpath entry contains an exclusion pattern.", 0, exclusions.length);
    }
  }

  private List<IClasspathEntry> filterClasspath(IClasspathEntry[] candidates, int cptype) {
    List<IClasspathEntry> result = new ArrayList<>();
    if(candidates != null) {
      for(IClasspathEntry entry : candidates) {
        if(entry.getEntryKind() == cptype) {
          result.add(entry);
        }
      }
    }
    return result;
  }

  public void testConfigureProjectEncodingForBasicProject() throws Exception {
    IProject project = importProject("projects/projectEncoding/p001/pom.xml");
    WorkspaceHelpers.assertNoErrors(project);

    String projectEncoding = project.getDefaultCharset();
    assertEquals("Encoding configured through Maven property not set on project", "ISO-8859-1", projectEncoding);
    String testfolderEncoding = project.getFolder(new Path("testfolder")).getDefaultCharset();
    assertEquals("Encoding for folder should have been inherited from project", "ISO-8859-1", testfolderEncoding);

    copyContent(project, new File("projects/projectEncoding/p001/pom2.xml"), "pom.xml");

    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);

    String projectEncodingChanged = project.getDefaultCharset();
    assertEquals("Encoding configured through Maven property not set on project", "UTF-16", projectEncodingChanged);
    String testfolderEncodingChanged = project.getFolder(new Path("testfolder")).getDefaultCharset();
    assertEquals("Encoding for folder should have been inherited from project", "UTF-16", testfolderEncodingChanged);
  }

  public void testRevertToContainerDefinedEncoding() throws Exception {
    IProject project = importProject("projects/projectEncoding/p002/pom.xml");
    WorkspaceHelpers.assertNoErrors(project);

    String containerProjectEncoding = project.getDefaultCharset();
    String containerTestfolderEncoding = project.getFolder(new Path("testfolder")).getDefaultCharset();
    assertEquals("Encoding for folder should be the same as project encoding", containerProjectEncoding,
        containerTestfolderEncoding);

    if(!"ISO-8859-1".equals(containerProjectEncoding)) {
      project.setDefaultCharset("ISO-8859-1", monitor);
    } else {
      project.setDefaultCharset("UTF-8", monitor);
    }
    assertEquals("Encoding for folder should be the same as project encoding", containerProjectEncoding,
        containerTestfolderEncoding);

    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);

    String projectEncodingReverted = project.getDefaultCharset();
    assertEquals("Project encoding not reverted to container defined", containerProjectEncoding,
        projectEncodingReverted);
    String testfolderEncodingReverted = project.getFolder(new Path("testfolder")).getDefaultCharset();
    assertEquals("Folder encoding not reverted to container defined", containerTestfolderEncoding,
        testfolderEncodingReverted);
  }

  public void testKeepEncodingSetByUserForSubfoldersAndFiles() throws Exception {
    IProject project = importProject("projects/projectEncoding/p003/pom.xml");
    WorkspaceHelpers.assertNoErrors(project);

    project.getFolder(new Path("testfolder")).setDefaultCharset("ISO-8859-1", monitor);
    project.getFile(new Path("testfolder/testfile.txt")).setCharset("UTF-16", monitor);

    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);

    String testfolderEncoding = project.getFolder(new Path("testfolder")).getDefaultCharset();
    assertEquals("Folder encoding set by user not kept", "ISO-8859-1", testfolderEncoding);
    String testfileEncoding = project.getFile(new Path("testfolder/testfile.txt")).getCharset();
    assertEquals("File encoding set by user not kept", "UTF-16", testfileEncoding);
  }

  public void test397251_forcePluginResolutionUpdate() throws Exception {
    FileUtils.deleteDirectory(new File("target/397251localrepo"));

    MojoExecutionKey key = new MojoExecutionKey("org.apache.maven.plugins", "maven-compiler-plugin", "2.0.2",
        "compile", "compile", "default-compile");

    mavenConfiguration.setUserSettingsFile("projects/397251_forcePluginResolutionUpdate/settings.xml");
    waitForJobsToComplete();

    injectFilexWagon();
    FilexWagon.setRequestFailPattern("org/apache/maven/plugins/maven-resources-plugin/.*");

    try {
      importProject("projects/397251_forcePluginResolutionUpdate/pom.xml");
      fail();
    } catch(CoreException e) {
      // ignore
    }
    waitForJobsToComplete();

    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("397251_forcePluginResolutionUpdate");

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
    Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mapping = facade.getMojoExecutionMapping();
//    assertTrue(mapping.get(key).isEmpty());

    FilexWagon.setRequestFailPattern(null);

    MavenUpdateRequest request = new MavenUpdateRequest(false, true);
    request.addPomFile(project);
    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(request, monitor);
    facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
    mapping = facade.getMojoExecutionMapping();
    assertFalse(mapping.get(key).isEmpty());
  }

  public void testStaleProjectConfigurationWarningMarker() throws Exception {
    testStaleProjectConfigurationMarker(ProblemSeverity.warning);
  }

  public void testIgnoreStaleProjectConfiguration() throws Exception {
    testStaleProjectConfigurationMarker(ProblemSeverity.ignore);
  }

  protected void testStaleProjectConfigurationMarker(ProblemSeverity problemSeverity) throws Exception {
    IProject project = importProject("projects/staleconfiguration/basic/pom.xml");
    assertNoErrors(project);

    final IMavenProjectFacade projectFacade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);

    String oldSeverity = mavenConfiguration.getOutOfDateProjectSeverity();
    try {
      ((MavenConfigurationImpl) mavenConfiguration).setOutOfDateProjectSeverity(problemSeverity.toString());
      copyContent(project, new File("projects/staleconfiguration/basic/pom-changed.xml"), "pom.xml");
      if(problemSeverity.getSeverity() > -1) {
        WorkspaceHelpers.assertMarker(IMavenConstants.MARKER_CONFIGURATION_ID, problemSeverity.getSeverity(),
            Messages.ProjectConfigurationUpdateRequired, null, null, project);

        workspace.run(new IWorkspaceRunnable() {
          public void run(IProgressMonitor monitor) throws CoreException {
            MavenPlugin.getProjectConfigurationManager()
                .updateProjectConfiguration(projectFacade.getProject(), monitor);
          }
        }, monitor);
      }
      assertNoErrors(project);
    } finally {
      ((MavenConfigurationImpl) mavenConfiguration).setOutOfDateProjectSeverity(oldSeverity);
    }
  }

}
