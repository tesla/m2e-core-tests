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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.debug.core.ILaunchConfiguration;

import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.internal.launch.MavenLaunchDelegate;
import org.eclipse.m2e.internal.launch.MavenLaunchUtils;
import org.eclipse.m2e.internal.launch.MavenRuntimeLaunchSupport.VMArguments;
import org.eclipse.m2e.tests.mocks.MockLaunchConfiguration;


public class MavenLaunchDelegateTest {

  private MavenLaunchDelegate launcher;

  private VMArguments arguments;

  @Before
  public void setUp() {
    launcher = new MavenLaunchDelegate();
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

  private ILaunchConfiguration getLaunchConfiguration(String pomDirectory) {
    File file = new File(pomDirectory);
    String absPomDir = file.getAbsolutePath();
    return new MockLaunchConfiguration(Collections.singletonMap(MavenLaunchConstants.ATTR_POM_DIR, absPomDir));
  }
}
