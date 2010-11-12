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
import java.util.ArrayList;
import java.util.LinkedHashMap;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenLauncherConfiguration;
import org.eclipse.m2e.core.embedder.MavenRuntime;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * @author Eugene Kuleshov
 */
public class MavenRuntimeManagerTest extends TestCase {

  private MavenRuntimeManager runtimeManager;

  protected void setUp() throws Exception {
    super.setUp();
    runtimeManager = MavenPlugin.getDefault().getMavenRuntimeManager();
    runtimeManager.reset();
  }

  public void testGetRuntime() throws Exception {
    assertEquals(MavenRuntimeManager.EMBEDDED, runtimeManager.getRuntime(null).getLocation());
    assertEquals(MavenRuntimeManager.EMBEDDED, runtimeManager.getRuntime("").getLocation());
    assertEquals(MavenRuntimeManager.EMBEDDED, runtimeManager.getRuntime(MavenRuntimeManager.DEFAULT).getLocation());

    assertEquals(MavenRuntimeManager.EMBEDDED, runtimeManager.getRuntime(MavenRuntimeManager.EMBEDDED).getLocation());

    assertEquals(MavenRuntimeManager.WORKSPACE, runtimeManager.getRuntime(MavenRuntimeManager.WORKSPACE).getLocation());
  }

  public void testSetDefaultRuntime() throws Exception {
    assertEquals(MavenRuntimeManager.EMBEDDED, runtimeManager.getDefaultRuntime().getLocation());

    runtimeManager.setDefaultRuntime(null);
    assertEquals(MavenRuntimeManager.EMBEDDED, runtimeManager.getDefaultRuntime().getLocation());

    runtimeManager.setDefaultRuntime(runtimeManager.getRuntime(MavenRuntimeManager.WORKSPACE));
    assertEquals(MavenRuntimeManager.EMBEDDED, runtimeManager.getDefaultRuntime().getLocation());

    assertFalse(runtimeManager.getMavenRuntimes().isEmpty());
  }

  public void testDefaultRuntime() throws Exception {
     MavenRuntime runtime = runtimeManager.getDefaultRuntime();
    assertFalse(runtime.isEditable());
    assertTrue(runtime.isAvailable());
    assertEquals(MavenRuntimeManager.EMBEDDED, runtime.getLocation());
    assertNull(runtime.getSettings());

    DummyLauncherConfig m2conf = new DummyLauncherConfig();
    runtime.createLauncherConfiguration(m2conf, null);
    
    assertNotNull(m2conf.mainRealm);
    assertNotNull(m2conf.mainType);
    assertTrue(m2conf.realms.get(m2conf.mainRealm).size() > 0);
    assertTrue(m2conf.realms.get(IMavenLauncherConfiguration.LAUNCHER_REALM).size() > 0);

    assertTrue(runtime.equals(runtime));
    assertFalse(runtime.equals(null));
    assertTrue(runtime.hashCode()!=0);
    assertTrue(runtime.toString().startsWith("Embedded"));
  }

  public void testExternalRuntime() throws Exception {
    String location = new File("resources/testRuntime").getCanonicalPath();
    MavenRuntime runtime = MavenRuntimeManager.createExternalRuntime(location);
    assertTrue(runtime.isEditable());
    assertFalse(runtime.isAvailable()); // runtime from non-existing folder
    assertEquals(location, runtime.getLocation());
    assertNotNull(runtime.getSettings());

    DummyLauncherConfig m2conf = new DummyLauncherConfig();
    runtime.createLauncherConfiguration(m2conf, null);

    assertNotNull(m2conf.mainRealm);
    assertNotNull(m2conf.mainType);
    assertTrue(m2conf.realms.get(m2conf.mainRealm).size() == 0);
    assertTrue(m2conf.realms.get(IMavenLauncherConfiguration.LAUNCHER_REALM).size() == 1);

    assertTrue(runtime.equals(runtime));
    assertFalse(runtime.equals(null));
    assertFalse(runtime.equals(runtimeManager.getDefaultRuntime()));
    assertTrue(runtime.hashCode()!=0);
    assertTrue(runtime.toString().startsWith("External"));
  }

  static class DummyLauncherConfig implements IMavenLauncherConfiguration {
    
    public String mainType;
    public String mainRealm;
    public LinkedHashMap<String, ArrayList<String>> realms = new LinkedHashMap<String, ArrayList<String>>();
    public ArrayList<String> curRealm;
    
    public void addArchiveEntry(String entry) throws CoreException {
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
