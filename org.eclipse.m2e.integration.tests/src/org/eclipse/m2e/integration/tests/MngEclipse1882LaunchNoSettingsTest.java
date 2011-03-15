/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.integration.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

import org.eclipse.m2e.integration.tests.common.SWTBotMenuEx;


/**
 * Verify that Maven can be launched even if the configured settings file does not exist.
 */
public class MngEclipse1882LaunchNoSettingsTest extends M2EUIIntegrationTestCase {

  private static String oldUserSettings;

  @BeforeClass
  public static void setUpBeforeClass() throws CoreException {
    oldUserSettings = setUserSettings("resources/settings-non-existent.xml");
  }

  @AfterClass
  public static void tearDownAfterclass() throws CoreException {
    setUserSettings(oldUserSettings);
  }

  @Test
  public void test() throws Exception {
    IProject project = createSimpleMavenProject("mngeclipse-1882");

    IFile file = project.getFile("target/launch.txt");
    file.create(new ByteArrayInputStream(new byte[0]), true, monitor);
    File path = file.getLocation().toFile();
    assertTrue(path.getAbsolutePath(), path.isFile());

    SWTBotTree tree = bot.viewById(PACKAGE_EXPLORER_VIEW_ID).bot().tree();
    SWTBotTreeItem node = tree.getTreeItem("mngeclipse-1882").expand().getNode("pom.xml");
    new SWTBotMenuEx(node.contextMenu("Run As")).menuContains("Maven clean").click();
    waitForAllBuildsToComplete();
    waitForAllLaunchesToComplete(30 * 1000);

    assertFalse(path.getAbsolutePath(), path.exists());
  }

}
