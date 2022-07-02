/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.core.internal.events.ResourceChangeEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;
import org.eclipse.m2e.tests.mocks.ResourceDeltaStub;


/**
 * TODO this test assumes that Maven uses repository id only (i.e. without repository url) to determine if local
 * artifact copy is up-to-date with remote contents. This cache behaviour is not documented and may change. This tests
 * should be reworked to "deploy" new artifact versions to the "remote" repository, in order to properly simulate
 * desired test scenario.
 */
public class ProjectRegistryRefreshJobTest extends AbstractMavenProjectTestCase {

  private static final String SETTINGS_ONE = "settings_updateRepo.xml";

  private static final String SETTINGS_TWO = "settings_updateRepo2.xml";

  private static final File LOCAL_REPO = new File("target/updateTestLocalRepo").getAbsoluteFile();

  private static final File LOCAL_ARTIFACT = new File(
      "target/updateTestLocalRepo/updateTest/b/1.0-SNAPSHOT/b-1.0-SNAPSHOT.jar").getAbsoluteFile();

  private IProject project;

  ProjectRegistryManager manager;

  ArrayList<MavenProjectChangedEvent> events;

  IMavenProjectChangedListener listener = (event, monitor) -> events.addAll(event);

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    // clear local repo
    delete(LOCAL_REPO);
    mavenConfiguration.setUserSettingsFile(new File(SETTINGS_ONE).getAbsolutePath());
    waitForJobsToComplete();

    manager = MavenPluginActivator.getDefault().getMavenProjectManagerImpl();
    events = new ArrayList<>();
    manager.addMavenProjectChangedListener(listener);
  }

  @Override
  @After
  public void tearDown() throws Exception {
    try {
      manager.removeMavenProjectChangedListener(listener);
      listener = null;
      events = null;
      manager = null;
    } finally {
      super.tearDown();
    }
  }

  @Test
  public void testUpdateNotForced() throws Exception {
    // import project
    project = importProject("projects/updateProject/simple/pom.xml");
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(project);

    mavenConfiguration.setUserSettingsFile(new File(SETTINGS_TWO).getAbsolutePath());
    waitForJobsToComplete();

    MavenUpdateRequest request = new MavenUpdateRequest(project, false, false);
    projectRefreshJob.refresh(request);
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(project);

    assertEquals(338, LOCAL_ARTIFACT.length()); // still from updateRepo1
  }

  @Test
  public void testUpdateForced() throws Exception {
    // import project
    project = importProject("projects/updateProject/simple/pom.xml");
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(project);
    assertEquals(338, LOCAL_ARTIFACT.length()); // from updateRepo1

    mavenConfiguration.setUserSettingsFile(new File(SETTINGS_TWO).getAbsolutePath());
    waitForJobsToComplete();

    projectRefreshJob.refresh(new MavenUpdateRequest(project, false, true));
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(project);

    assertEquals(785, LOCAL_ARTIFACT.length()); // from updateRepo2
  }

  /*
   * Adding a new dependency should not force an update of the original
   */
  @Test
  public void testDependencyAdded_NoUpdate() throws Exception {
    // import project
    project = importProject("projects/updateProject/simple/pom.xml");
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(project);

    mavenConfiguration.setUserSettingsFile(new File(SETTINGS_TWO).getAbsolutePath());
    waitForJobsToComplete();

    copyContent(project, "pomWithSecondDependency.xml", "pom.xml");
    WorkspaceHelpers.assertNoErrors(project);

    assertEquals(338, LOCAL_ARTIFACT.length());
  }

  @Test
  public void testMultiProject() throws Exception {
    // import project
    IProject[] projects = importProjects("projects/updateProject/multiProject/", new String[] {"projectA/pom.xml",
        "projectB/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(projects[0]);
    WorkspaceHelpers.assertNoErrors(projects[1]);

    mavenConfiguration.setUserSettingsFile(new File(SETTINGS_TWO).getAbsolutePath());
    waitForJobsToComplete();

    assertEquals(338, LOCAL_ARTIFACT.length());

    // Add dependency to original project
    copyContent(projects[0], "pomWithDependency.xml", "pom.xml");
    WorkspaceHelpers.assertNoErrors(projects[0]);
    WorkspaceHelpers.assertNoErrors(projects[1]);

    assertEquals(338, LOCAL_ARTIFACT.length());
  }

  @Test
  public void testRefreshAfterOpen() throws Exception {
    setAutoBuilding(true);
    ProjectRegistryManager manager = MavenPluginActivator.getDefault().getMavenProjectManagerImpl();

    IProject p1 = importProject("projects/updateProject/simple/pom.xml");
    waitForJobsToComplete();
    assertNotNull(manager.getProject(p1));

    p1.close(monitor);
    waitForJobsToComplete();
    assertNull(manager.getProject(p1));

    p1.open(monitor);
    waitForJobsToComplete();
    assertNotNull(manager.getProject(p1));
  }

  @Test
  public void test436679_NoRefreshWhenImport() throws Exception {
    IProject p1 = importProject("projects/updateProject/simple/pom.xml");
    waitForJobsToComplete();
    assertNotNull(manager.getProject(p1));

    assertEquals(1, events.size());
  }

  @Test
  public void test437493_NoRefreshWhenInitClasspathContainer() throws Exception {

    String origSettings = mavenConfiguration.getUserSettingsFile();
    try {
      mavenConfiguration.setUserSettingsFile(new File("settings.xml").getCanonicalPath());
      waitForJobsToComplete();

      events.clear();
      IProject p1 = importProject("customclasspath-classpath-containers",
          "projects/customclasspath/classpath-containers", new ResolverConfiguration());

      waitForJobsToComplete();
      assertNotNull(manager.getProject(p1));

      assertEquals(1, events.size());

    } finally {
      mavenConfiguration.setUserSettingsFile(origSettings);
    }

  }

  @Test
  public void test416050_ignoreNonMavenProjectChanges() throws Exception {
    // import project
    project = createExisting("416050_ignoreNonMavenProjectChanges", "projects/416050_ignoreNonMavenProjectChanges");
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(project);

    ResourceDeltaStub delta = new ResourceDeltaStub(workspace.getRoot());
    ResourceDeltaStub child = delta.addChild(new ResourceDeltaStub(project));
    IFile changedFile = project.getFile(".project");
    child.addChild(new ResourceDeltaStub(changedFile));
    IResourceChangeEvent event;

    event = new ResourceChangeEvent(changedFile, IResourceChangeEvent.POST_CHANGE,
        IncrementalProjectBuilder.AUTO_BUILD, delta);
    projectRefreshJob.resourceChanged(event);
    assertTrue(projectRefreshJob.isEmpty());

    event = new ResourceChangeEvent(project, IResourceChangeEvent.PRE_CLOSE, IncrementalProjectBuilder.FULL_BUILD,
        delta) {
      private static final long serialVersionUID = 1L;

      @Override
      public IResource getResource() {
        return project;
      }
    };
    projectRefreshJob.resourceChanged(event);
    assertTrue(projectRefreshJob.isEmpty());
  }

  @Test
  public void test445675_autobuild() throws Exception {
    // import with autobuild off
    project = importProject("projects/updateProject/simple/pom.xml");
    assertContainsOnly(getProjectsFromEvents(events), project);

    copyContent(project, "pom.xml", "pomWithOneDependency.xml"); // save for later

    // same as testRefreshAfterOpen() but with autobuild off
    project.close(monitor);
    waitForJobsToComplete();
    assertNull(manager.getProject(project));

    project.open(monitor);
    waitForJobsToComplete();
    assertNotNull(manager.getProject(project));

    // change pom with autobuild off
    events.clear();
    copyContent(project, "pomWithSecondDependency.xml", "pom.xml", false);
    waitForJobsToComplete();
    assertEquals(0, events.size());

    // project will get build when autobuild gets enabled
    setAutoBuilding(true);
    waitForJobsToComplete();
    assertContainsOnly(getProjectsFromEvents(events), project);

    // change pom with autobuild on
    events.clear();
    copyContent(project, "pomWithOneDependency.xml", "pom.xml", false);
    waitForJobsToComplete();
    assertContainsOnly(getProjectsFromEvents(events), project);
  }

  @Test
  public void testXXXX_projectRenameRefresh() throws Exception {
    IProject[] projects = importProjects("projects/updateProject/multiProject/", new String[] {"projectA/pom.xml",
        "projectB/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();

    IProject p1 = projects[0];
    IProject p2 = projects[1];

    WorkspaceHelpers.assertNoErrors(p1);
    WorkspaceHelpers.assertNoErrors(p2);

    setAutoBuilding(true);
    waitForJobsToComplete();

    IProjectDescription description = p1.getDescription();
    String newName = description.getName() + "_renamed";
    description.setName(newName);
    p1.move(description, IResource.FORCE | IResource.SHALLOW, new NullProgressMonitor());
    waitForJobsToComplete();

    p1 = ResourcesPlugin.getWorkspace().getRoot().getProject(newName);

    WorkspaceHelpers.assertNoErrors(p1);
    WorkspaceHelpers.assertNoErrors(p2);
  }

  private static void delete(File file) {
    if(file.isDirectory()) {
      for(File child : file.listFiles()) {
        delete(child);
      }
    }
    file.delete();
  }

}
