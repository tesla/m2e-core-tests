/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.io.File;

import org.maven.ide.eclipse.MavenPlugin;

import junit.framework.TestCase;


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
    assertNotNull(runtime.getMainTypeName());
    assertFalse(runtime.isEditable());
    assertTrue(runtime.isAvailable());
    assertEquals(MavenRuntimeManager.EMBEDDED, runtime.getLocation());
    assertNull(runtime.getSettings());

    File location = new File(MavenPlugin.getDefault().getStateLocation().toFile(), "test");
    assertEquals("", runtime.getOptions(location, new String[0]));

    assertTrue(runtime.getClasspath(null).length > 0);
    assertTrue(runtime.getClasspath(new String[0]).length > 0);
    assertTrue(runtime.getClasspath(new String[] {"foo.jar"}).length > 0);
    assertTrue(runtime.equals(runtime));
    assertFalse(runtime.equals(null));
    assertTrue(runtime.hashCode()!=0);
    assertEquals("Embedded", runtime.toString());
  }

  public void testExternalRuntime() throws Exception {
    MavenRuntime runtime = MavenRuntime.createExternalRuntime("testRuntime");
    assertNotNull(runtime.getMainTypeName());
    assertTrue(runtime.isEditable());
    assertFalse(runtime.isAvailable()); // runtime from non-existing folder
    assertEquals("testRuntime", runtime.getLocation());
    assertNotNull(runtime.getSettings());

    File location = new File(MavenPlugin.getDefault().getStateLocation().toFile(), "test");
    assertTrue(runtime.getOptions(location, new String[] {"foo.jar"}).length() > 0);

    assertTrue(runtime.getClasspath(null).length == 0);
    assertTrue(runtime.getClasspath(new String[0]).length == 0);
    assertTrue(runtime.getClasspath(new String[] {"foo.jar"}).length == 0);
    assertTrue(runtime.equals(runtime));
    assertFalse(runtime.equals(null));
    assertFalse(runtime.equals(runtimeManager.getDefaultRuntime()));
    assertTrue(runtime.hashCode()!=0);
    assertTrue(runtime.toString().startsWith("External"));
  }

}
