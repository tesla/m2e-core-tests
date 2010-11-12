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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.archetype.catalog.Archetype;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.FilexWagon;

public class ProjectConfigurationManagerTest extends AbstractMavenProjectTestCase {

  public void testBasedirRenameRequired() throws Exception {
    testBasedirRename(MavenProjectInfo.RENAME_REQUIRED);

    IWorkspaceRoot root = workspace.getRoot();
    IProject project = root.getProject("maven-project");
    assertNotNull(project);
    IMavenProjectFacade facade = plugin.getMavenProjectManager().getMavenProject("MNGECLIPSE-1793_basedirRename", "maven-project", "0.0.1-SNAPSHOT");
    assertNotNull(facade);
    assertEquals(project, facade.getProject());
  }

  public void testBasedirRenameNo() throws Exception {
    testBasedirRename(MavenProjectInfo.RENAME_NO);

    IWorkspaceRoot root = workspace.getRoot();
    IProject project = root.getProject("mavenNNNNNNN");
    assertNotNull(project);
    IMavenProjectFacade facade = plugin.getMavenProjectManager().getMavenProject("MNGECLIPSE-1793_basedirRename", "maven-project", "0.0.1-SNAPSHOT");
    assertNotNull(facade);
    assertEquals(project, facade.getProject());
  }

  private void testBasedirRename(int renameRequired) throws IOException, CoreException {
    IWorkspaceRoot root = workspace.getRoot();
    final MavenPlugin plugin = MavenPlugin.getDefault();

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
        plugin.getProjectConfigurationManager().importProjects(projectInfos, importConfiguration, monitor);
      }
    }, plugin.getProjectConfigurationManager().getRule(), IWorkspace.AVOID_UPDATE, monitor);
  }

  public void testWorkspaceResolutionOfInterModuleDependenciesDuringImport() throws Exception {
    String oldSettings = mavenConfiguration.getUserSettingsFile();
    try {
      injectFilexWagon();
      FilexWagon.setRequestFilterPattern("test/.*", true);
      mavenConfiguration.setUserSettingsFile(new File("projects/MNGECLIPSE-1990/settings.xml").getAbsolutePath());
      IJobChangeListener jobChangeListener = new JobChangeAdapter() {
        public void scheduled(IJobChangeEvent event) {
          if(event.getJob().getClass().getName().endsWith("MavenProjectManagerRefreshJob")) {
            // cancel all those concurrent refresh jobs, we want to monitor the main thread only
            event.getJob().cancel();
          }
        }
      };
      Job.getJobManager().addJobChangeListener(jobChangeListener);
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

      IProjectConfigurationManager manager = MavenPlugin.getDefault().getProjectConfigurationManager();
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

    IJavaProject javaProject = JavaCore.create(project);
    assertEquals("1.6", javaProject.getOption(JavaCore.COMPILER_SOURCE, true));
    assertEquals("1.5", javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true));
  }

}
