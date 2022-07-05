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

package org.eclipse.m2e.tests.discovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.internal.Workbench;

import org.eclipse.m2e.internal.discovery.DiscoveryActivator;
import org.eclipse.m2e.internal.discovery.startup.UpdateConfigurationStartup;
import org.eclipse.m2e.tests.common.AbstractLifecycleMappingTest;


public class ConfigurationUpdateStartupTest extends AbstractLifecycleMappingTest {

  private static final String BASE_DIR = "projects/updateConfigurationStartupTest/";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    UpdateConfigurationStartup.enableStartup();
    UpdateConfigurationStartup.clearSavedProjects();
  }

  @Override
  @After
  public void tearDown() throws Exception {
    super.tearDown();
    UpdateConfigurationStartup.clearSavedProjects();
    UpdateConfigurationStartup.enableStartup();
  }

  @Test
  public void testEmptyWorkspace() {
    assertEquals("Expected no projects", 0, UpdateConfigurationStartup.getSavedProjects().size());
    UpdateConfigurationStartup.saveMarkedProjects();
    assertEquals("Expected no projects", 0, UpdateConfigurationStartup.getSavedProjects().size());
  }

  @Test
  public void testNoMavenProjects() throws Exception {
    // Java errors (not selected)
    importProject("javaError", BASE_DIR);
    // Java no error
    importProject("javaNoError", BASE_DIR);
    waitForJobsToComplete();

    assertEquals("Expected no projects", 0, UpdateConfigurationStartup.getSavedProjects().size());
    UpdateConfigurationStartup.saveMarkedProjects();
    assertEquals("Expected no projects", 0, UpdateConfigurationStartup.getSavedProjects().size());
  }

  @Test
  public void testMavenProjectsNoErrors() throws Exception {
    // Simple maven
    importMavenProject(BASE_DIR, "simple-pom/pom.xml");
    // Java errors (not selected)
    importProject("javaError", BASE_DIR);
    // Java no error
    importProject("javaNoError", BASE_DIR);
    waitForJobsToComplete();

    assertEquals("Expected no projects", 0, UpdateConfigurationStartup.getSavedProjects().size());
    UpdateConfigurationStartup.saveMarkedProjects();
    assertEquals("Expected no projects", 0, UpdateConfigurationStartup.getSavedProjects().size());
  }

  @Test
  public void testMavenProjectsErrors() throws Exception {
    assertEquals("Expected no projects", 0, UpdateConfigurationStartup.getSavedProjects().size());

    // Simple maven
    importMavenProject(BASE_DIR, "simple-pom/pom.xml");
    // Configuration Errors
    importMavenProject(BASE_DIR, "notCoveredMojoExecutions/pom.xml");
    // Other Maven errors (not selected)
    importMavenProject(BASE_DIR, "buildError/pom.xml");

    // Java errors (not selected)
    importProject("javaError", BASE_DIR);
    // Java (not selected)
    importProject("javaNoError", BASE_DIR);
    waitForJobsToComplete();

    UpdateConfigurationStartup.saveMarkedProjects();
    assertEquals("Expected no projects", 1, UpdateConfigurationStartup.getSavedProjects().size());
  }

  @Test
  public void testConfigureSpecifiedProjects() throws Exception {
    assertEquals("Expected no projects", 0, UpdateConfigurationStartup.getSavedProjects().size());

    String[] projects = new String[] {"projectA", "projectB", "projectC"};
    UpdateConfigurationStartup.enableStartup(Arrays.asList(projects));

    List<IProject> persisted = UpdateConfigurationStartup.getSavedProjects();
    assertEquals("Expected no projects", projects.length, persisted.size());

    for(String projectName : projects) {
      assertTrue("Missing Project:" + projectName, hasProject(persisted, projectName));
    }
  }

  @Test
  public void testToggleEarlyStartup() {
    assertFalse("Plugin Disabled", pluginDisabled());
    UpdateConfigurationStartup.disableStartup();
    assertTrue("Plugin Enabled", pluginDisabled());
    UpdateConfigurationStartup.enableStartup();
    assertFalse("Plugin Disabled", pluginDisabled());
  }

  private static boolean hasProject(List<IProject> projects, String name) {
    return projects.stream().anyMatch(p -> name.equals(p.getName()));
  }

  private static void importProject(String name, String base) throws Exception {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    // copy
    File src = new File(base);
    File dst = new File(root.getLocation().toFile(), name);
    copyDir(src, dst);

    IProject project = root.getProject(name);
    project.create(monitor);

    if(!project.isOpen()) {
      project.open(monitor);
    }
  }

  private static boolean pluginDisabled() {
    for(String disabled : Workbench.getInstance().getDisabledEarlyActivatedPlugins()) {
      if(DiscoveryActivator.PLUGIN_ID.equals(disabled)) {
        return true;
      }
    }
    return false;
  }
}
