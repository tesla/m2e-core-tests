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

package org.eclipse.m2e.tests.embedder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenLauncherConfiguration;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.launch.AbstractMavenRuntime;
import org.eclipse.m2e.core.internal.launch.MavenEmbeddedRuntime;
import org.eclipse.m2e.core.internal.launch.MavenExternalRuntime;
import org.eclipse.m2e.core.internal.launch.MavenRuntimeManagerImpl;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * @author Eugene Kuleshov
 */
@SuppressWarnings("deprecation")
public class MavenRuntimeManagerTest extends TestCase {

  private MavenRuntimeManagerImpl runtimeManager;

  protected void setUp() throws Exception {
    super.setUp();
    runtimeManager = MavenPluginActivator.getDefault().getMavenRuntimeManager();
    runtimeManager.reset();
  }

  public void testGetRuntime() throws Exception {
    assertEquals(MavenRuntimeManagerImpl.EMBEDDED, runtimeManager.getRuntime(null).getName());
    assertEquals(MavenRuntimeManagerImpl.EMBEDDED, runtimeManager.getRuntime("").getName());
    assertEquals(MavenRuntimeManagerImpl.EMBEDDED, runtimeManager.getRuntime(MavenRuntimeManagerImpl.DEFAULT).getName());

    assertEquals(MavenRuntimeManagerImpl.EMBEDDED, runtimeManager.getRuntime(MavenRuntimeManagerImpl.EMBEDDED)
        .getLocation());

    assertEquals(MavenRuntimeManagerImpl.WORKSPACE, runtimeManager.getRuntime(MavenRuntimeManagerImpl.WORKSPACE)
        .getLocation());
  }

  public void testSetDefaultRuntime() throws Exception {
    assertEquals(MavenRuntimeManagerImpl.EMBEDDED, runtimeManager.getRuntime(MavenRuntimeManagerImpl.DEFAULT).getName());

    runtimeManager.setDefaultRuntime(null);
    assertEquals(MavenRuntimeManagerImpl.EMBEDDED, runtimeManager.getRuntime(MavenRuntimeManagerImpl.DEFAULT).getName());

    runtimeManager.setDefaultRuntime(runtimeManager.getRuntime(MavenRuntimeManagerImpl.WORKSPACE));
    assertEquals(MavenRuntimeManagerImpl.EMBEDDED, runtimeManager.getRuntime(MavenRuntimeManagerImpl.DEFAULT).getName());

    assertFalse(runtimeManager.getMavenRuntimes().isEmpty());
  }

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

    assertTrue(runtime.equals(runtime));
    assertFalse(runtime.equals(null));
    assertTrue(runtime.hashCode() != 0);

    DummyLauncherConfig m2conf = new DummyLauncherConfig();
    runtime.createLauncherConfiguration(m2conf, null);

    assertNotNull(m2conf.mainRealm);
    assertNotNull(m2conf.mainType);
    assertTrue(m2conf.realms.get(m2conf.mainRealm).size() > 0);
    assertTrue(m2conf.realms.get(IMavenLauncherConfiguration.LAUNCHER_REALM).size() > 0);
    List<String> classpathEntries = m2conf.realms.get(MavenEmbeddedRuntime.PLEXUS_CLASSWORLD_NAME);
    assertNotNull("Realm " + MavenEmbeddedRuntime.PLEXUS_CLASSWORLD_NAME + " does not exist", classpathEntries);
    assertTrue(classpathEntries.size() > 0);
    boolean foundPlexusApi = false;
    boolean foundSlf4j = false;
    for(String classpathEntry : classpathEntries) {
      if(classpathEntry.contains("plexus-build-api")) {
        foundPlexusApi = true;
        break;
      } else if(classpathEntry.contains("org.slf4j.api")
          //or in Dev mode:
          || Files.isRegularFile(Paths.get(classpathEntry, "org", "slf4j", "Logger.class"))) {
        foundSlf4j = true;
      }
    }
    assertFalse("plexus-build-api jar is in classpath", foundPlexusApi);
    assertTrue("slf4j-api bundle is missing from the classpath", foundSlf4j);
  }

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
    assertTrue(m2conf.realms.get(m2conf.mainRealm).size() == 0);
    assertTrue(m2conf.realms.get(IMavenLauncherConfiguration.LAUNCHER_REALM).size() == 1);

    assertTrue(runtime.equals(runtime));
    assertFalse(runtime.equals(null));
    assertFalse(runtime.equals(runtimeManager.getRuntime(MavenRuntimeManagerImpl.DEFAULT)));
    assertTrue(runtime.hashCode() != 0);
  }

  static class DummyLauncherConfig implements IMavenLauncherConfiguration {

    public String mainType;

    public String mainRealm;

    public LinkedHashMap<String, List<String>> realms = new LinkedHashMap<String, List<String>>();

    public ArrayList<String> curRealm;

    public void addArchiveEntry(String entry) {
      curRealm.add(entry);
    }

    public void addProjectEntry(IMavenProjectFacade facade) {
      curRealm.add(facade.getProject().getName());
    }

    public void addRealm(String realm) {
      curRealm = new ArrayList<String>();
      realms.put(realm, curRealm);
    }

    public void setMainType(String type, String realm) {
      this.mainType = type;
      this.mainRealm = realm;
    }
  }
}
