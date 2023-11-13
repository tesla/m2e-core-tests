/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.tests.embedder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.launch.AbstractMavenRuntime;
import org.eclipse.m2e.core.internal.launch.AbstractMavenRuntime.IMavenLauncherConfiguration;
import org.eclipse.m2e.core.internal.launch.MavenEmbeddedRuntime;
import org.eclipse.m2e.core.internal.launch.MavenExternalRuntime;
import org.eclipse.m2e.core.internal.launch.MavenRuntimeManagerImpl;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * @author Eugene Kuleshov
 */
public class MavenRuntimeManagerTest {

  private MavenRuntimeManagerImpl runtimeManager;

  @Before
  public void setUp() throws Exception {
    runtimeManager = MavenPluginActivator.getDefault().getMavenRuntimeManager();
    runtimeManager.reset();
  }

  @Test
  public void testGetRuntime() throws Exception {
    assertEquals(MavenRuntimeManagerImpl.EMBEDDED, runtimeManager.getRuntime(null).getName());
    assertEquals(MavenRuntimeManagerImpl.EMBEDDED, runtimeManager.getRuntime("").getName());
    assertEquals(MavenRuntimeManagerImpl.EMBEDDED,
        runtimeManager.getRuntime(MavenRuntimeManagerImpl.DEFAULT).getName());

    assertEquals(MavenRuntimeManagerImpl.EMBEDDED,
        runtimeManager.getRuntime(MavenRuntimeManagerImpl.EMBEDDED).getLocation());

    assertEquals(MavenRuntimeManagerImpl.WORKSPACE,
        runtimeManager.getRuntime(MavenRuntimeManagerImpl.WORKSPACE).getLocation());
  }

  @Test
  public void testSetDefaultRuntime() throws Exception {
    assertEquals(MavenRuntimeManagerImpl.EMBEDDED,
        runtimeManager.getRuntime(MavenRuntimeManagerImpl.DEFAULT).getName());

    runtimeManager.setDefaultRuntime(null);
    assertEquals(MavenRuntimeManagerImpl.EMBEDDED,
        runtimeManager.getRuntime(MavenRuntimeManagerImpl.DEFAULT).getName());

    runtimeManager.setDefaultRuntime(runtimeManager.getRuntime(MavenRuntimeManagerImpl.WORKSPACE));
    assertEquals(MavenRuntimeManagerImpl.EMBEDDED,
        runtimeManager.getRuntime(MavenRuntimeManagerImpl.DEFAULT).getName());

    assertFalse(runtimeManager.getMavenRuntimes().isEmpty());
  }

  @Test
  public void testDefaultRuntime() throws Exception {
    AbstractMavenRuntime runtime = runtimeManager.getRuntime(MavenRuntimeManagerImpl.DEFAULT);
    assertFalse(runtime.isEditable());
    assertTrue(runtime.isAvailable());
    assertEquals(MavenRuntimeManagerImpl.EMBEDDED, runtime.getLocation());

    MavenPlugin.getMavenConfiguration().setGlobalSettingsFile("settings.xml");
    try {
      assertEquals(new File(MavenPlugin.getMavenConfiguration().getGlobalSettingsFile()).getCanonicalPath(),
          runtime.getSettings());
    } finally {
      MavenPlugin.getMavenConfiguration().setGlobalSettingsFile(null);
    }
    assertEquals(null, runtime.getSettings());

    assertNotNull(runtime);
    assertNotEquals(0, runtime.hashCode());

    DummyLauncherConfig m2conf = new DummyLauncherConfig();
    runtime.createLauncherConfiguration(m2conf, null);

    assertNotNull(m2conf.mainRealm);
    assertNotNull(m2conf.mainType);
    assertTrue(m2conf.realms.get(m2conf.mainRealm).size() > 0);
    assertTrue(m2conf.realms.get(IMavenLauncherConfiguration.LAUNCHER_REALM).size() > 0);
    List<String> classpathEntries = m2conf.realms.get(MavenEmbeddedRuntime.PLEXUS_CLASSWORLD_NAME);
    assertNotNull("Realm " + MavenEmbeddedRuntime.PLEXUS_CLASSWORLD_NAME + " does not exist", classpathEntries);
    assertFalse(classpathEntries.isEmpty());
    assertFalse("plexus-build-api jar is in classpath",
        classpathEntries.stream().anyMatch(e -> e.contains("plexus-build-api")));
    assertTrue("slf4j-api bundle is missing from the classpath",
        classpathEntries.stream().anyMatch(e -> e.contains("slf4j.api") || e.contains("slf4j-api")
        //or in Dev mode:
            || Files.isRegularFile(Path.of(e, "org", "slf4j", "Logger.class"))));
  }

  @Test
  public void testExternalRuntime() throws Exception {
    String location = new File("resources/testRuntime").getCanonicalPath();
    AbstractMavenRuntime runtime = new MavenExternalRuntime(location);
    assertTrue(runtime.isEditable());
    assertFalse(runtime.isAvailable()); // runtime from non-existing folder
    assertEquals(location, runtime.getLocation());
    MavenPlugin.getMavenConfiguration().setGlobalSettingsFile("settings.xml");
    try {
      assertEquals(new File(MavenPlugin.getMavenConfiguration().getGlobalSettingsFile()).getCanonicalPath(),
          runtime.getSettings());
    } finally {
      MavenPlugin.getMavenConfiguration().setGlobalSettingsFile(null);
    }
    assertEquals(null, runtime.getSettings());

    DummyLauncherConfig m2conf = new DummyLauncherConfig();
    runtime.createLauncherConfiguration(m2conf, null);

    assertNotNull(m2conf.mainRealm);
    assertNotNull(m2conf.mainType);
    assertEquals(0, m2conf.realms.get(m2conf.mainRealm).size());
    assertEquals(1, m2conf.realms.get(IMavenLauncherConfiguration.LAUNCHER_REALM).size());

    assertNotNull(runtime);
    assertNotEquals(runtimeManager.getRuntime(MavenRuntimeManagerImpl.DEFAULT), runtime);
    assertNotEquals(0, runtime.hashCode());
  }

  static class DummyLauncherConfig implements IMavenLauncherConfiguration {

    public String mainType;

    public String mainRealm;

    public LinkedHashMap<String, List<String>> realms = new LinkedHashMap<>();

    public ArrayList<String> curRealm;

    @Override
    public void addArchiveEntry(String entry) {
      curRealm.add(entry);
    }

    @Override
    public void addProjectEntry(IMavenProjectFacade facade) {
      curRealm.add(facade.getProject().getName());
    }

    @Override
    public void addRealm(String realm) {
      curRealm = new ArrayList<>();
      realms.put(realm, curRealm);
    }

    @Override
    public void setMainType(String type, String realm) {
      this.mainType = type;
      this.mainRealm = realm;
    }
  }
}
