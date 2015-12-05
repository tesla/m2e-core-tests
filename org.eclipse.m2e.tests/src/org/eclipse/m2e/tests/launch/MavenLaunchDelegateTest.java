/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.launch;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.Launch;
import org.eclipse.jdt.launching.IVMRunner;

import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.internal.launch.MavenLaunchDelegate;
import org.eclipse.m2e.internal.launch.MavenLaunchUtils;
import org.eclipse.m2e.internal.launch.MavenRuntimeLaunchSupport.VMArguments;
import org.eclipse.m2e.internal.launch.MavenSourceLocator;
import org.eclipse.m2e.tests.mocks.MockLaunchConfiguration;
import org.eclipse.m2e.tests.mocks.MockVMRunner;


public class MavenLaunchDelegateTest {

  MockVMRunner runner;

  private MavenLaunchDelegate launcher;

  private VMArguments arguments;

  @Before
  public void setUp() {
    runner = new MockVMRunner();

    launcher = new MavenLaunchDelegate() {
      public IVMRunner getVMRunner(ILaunchConfiguration configuration, String mode) {
        return runner;
      }
    };
    arguments = new VMArguments();
  }

  @After
  public void tearDown() {
    launcher = null;
    arguments = null;
  }

  @Test
  public void testGetVMArguments331() throws Exception {
    ILaunchConfiguration configuration = getLaunchConfiguration("projects/462944/foo/bar/baz");
    launcher.appendRuntimeSpecificArguments("3.3.1", arguments, configuration);
    String expectedVmArgs = "-Dmaven.multiModuleProjectDirectory="
        + MavenLaunchUtils.quote(new File("projects/462944/foo/bar/baz").getAbsolutePath());
    assertEquals(expectedVmArgs, arguments.toString());
  }

  @Test
  public void testGetVMArguments332WithParent() throws Exception {
    ILaunchConfiguration configuration = getLaunchConfiguration("projects/462944/foo/bar/");
    launcher.appendRuntimeSpecificArguments("3.3.2", arguments, configuration);
    String expectedVmArgs = "-Xmx2048m -Xms1024m -XX:MaxPermSize=512m -Djava.awt.headless=true -Dmaven.multiModuleProjectDirectory="
        + MavenLaunchUtils.quote(new File("projects/462944/foo/").getAbsolutePath());
    assertEquals(expectedVmArgs, arguments.toString());
  }

  @Test
  public void testGetVMArguments325() throws Exception {
    ILaunchConfiguration configuration = getLaunchConfiguration("projects/462944/foo/bar/baz");
    launcher.appendRuntimeSpecificArguments("3.2.5", arguments, configuration);
    assertEquals("", arguments.toString());
  }

  @Test
  public void testGetVMArgumentsSubstituteMultiModuleDir() throws Exception {
    Path workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toPath();
    Path mvn = workspaceRoot.resolve(Paths.get("foo", ".mvn"));
    if(!Files.exists(mvn)) {
      Files.createDirectories(mvn);
    }
    ILaunchConfiguration configuration = new MockLaunchConfiguration(
        Collections.singletonMap(MavenLaunchConstants.ATTR_POM_DIR, "${workspace_loc}/foo/bar"));
    launcher.appendRuntimeSpecificArguments("3.3.3", arguments, configuration);
    String expectedVmArgs = "-Dmaven.multiModuleProjectDirectory="
        + MavenLaunchUtils.quote(ResourcesPlugin.getWorkspace().getRoot().getLocation().append("foo").toOSString());
    assertEquals(expectedVmArgs, arguments.toString());
  }

  @Test
  public void testGlobalSettingsProgramArguments() throws Exception {
    IMavenConfiguration mavenConfig = MavenPlugin.getMavenConfiguration();
    MockLaunchConfiguration configuration = getLaunchConfiguration("projects/444262_settings");
    try {
      performDummyLaunch(configuration);
      assertArrayEquals(new String[] {"-B"}, runner.getConfiguration().getProgramArguments());

      // relative preference path to global settings is relative to eclipse home 
      mavenConfig.setGlobalSettingsFile("settings_empty.xml");
      performDummyLaunch(configuration);
      assertArrayEquals(new String[] {"-B", "-gs", new File("settings_empty.xml").getAbsolutePath()},
          runner.getConfiguration().getProgramArguments());

      // specifying -gs within goals overrides global settings from  configuration
      configuration.getAttributes().put(MavenLaunchConstants.ATTR_GOALS, "clean -gs other_settings.xml");
      performDummyLaunch(configuration);
      assertArrayEquals(new String[] {"-B", "clean", "-gs", "other_settings.xml"},
          runner.getConfiguration().getProgramArguments());

    } finally {
      mavenConfig.setGlobalSettingsFile(null);
    }
  }

  @Test
  public void testUserSettingsProgramArguments() throws Exception {
    IMavenConfiguration mavenConfig = MavenPlugin.getMavenConfiguration();
    MockLaunchConfiguration configuration = getLaunchConfiguration("projects/444262_settings");
    try {
      mavenConfig.setUserSettingsFile(new File("settings_empty.xml").getAbsolutePath());
      performDummyLaunch(configuration);
      assertArrayEquals(new String[] {"-B", "-s", mavenConfig.getUserSettingsFile()},
          runner.getConfiguration().getProgramArguments());

      configuration.getAttributes().put(MavenLaunchConstants.ATTR_USER_SETTINGS, "settings.xml");
      performDummyLaunch(configuration);
      assertArrayEquals(new String[] {"-B", "-s", "settings.xml"}, runner.getConfiguration().getProgramArguments());
    } finally {
      mavenConfig.setUserSettingsFile(null);
    }
  }

  private void performDummyLaunch(ILaunchConfiguration configuration) throws Exception {
    Launch launch = new Launch(configuration, "run", new MavenSourceLocator());
    try {
      launcher.launch(configuration, "run", launch, new NullProgressMonitor());
    } finally {
      launch.launchRemoved(launch);
    }
  }

  private MockLaunchConfiguration getLaunchConfiguration(String pomDirectory) {
    File file = new File(pomDirectory);
    String absPomDir = file.getAbsolutePath();
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(MavenLaunchConstants.ATTR_POM_DIR, absPomDir);
    return new MockLaunchConfiguration(attributes);
  }
}
