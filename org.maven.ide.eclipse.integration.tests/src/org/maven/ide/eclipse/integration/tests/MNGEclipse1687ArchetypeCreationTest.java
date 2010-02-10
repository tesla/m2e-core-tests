/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.junit.Test;
import org.maven.ide.eclipse.core.Messages;


/**
 * @author dyocum
 */
public class MNGEclipse1687ArchetypeCreationTest extends M2EUIIntegrationTestCase {

  private static final String GROUP_ID = "org.jboss.portletbridge.archetypes";
  
  private static final String ARCHETYPE_ID = "seam-basic";

  private static final String VERSION_ID = "2.0.0.ALPHA";
  
  private static final String PROJECT_NAME = "archetypeTestProjext";

  @Test
  public void testArchetypeCreation() throws Exception {

    URL url = FileLocator.find(Platform.getBundle(PLUGIN_ID), new Path("/projects/seam-basic-2.0.0.ALPHA.jar"), null);

    createProjectFromArchetype(ARCHETYPE_ID, "", url.toString());
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
    assertTrue("The project is expected to exist", project.exists());
  }

  /**
   * The jboss archetype has a packaging type of maven-plugin, so its a good test to make sure we find an archetype and
   * create it with the right packaging type. This was originally filed as MNG-Eclipse1687
   * 
   * @param artifactID
   * @param extraConfig
   * @param repoUrl
   * @throws Exception
   */
  public void createProjectFromArchetype(String artifactID, String extraConfig, String repoUrl) throws Exception {
    bot.menu("File").menu("New").menu("Project...").click();
    bot.shell("New Project").activate();
    bot.tree().expandNode("Maven").getNode("Maven Project").doubleClick();
    bot.button("Next >").click();

    bot.button("Add Archetype...").click();
    bot.shell("Add Archetype").activate();

    bot.comboBoxWithLabel("Archetype Group Id:").setText(GROUP_ID);
    bot.comboBoxWithLabel("Archetype Artifact Id:").setText(artifactID);
    bot.comboBoxWithLabel("Archetype Version:").setText(VERSION_ID);
    bot.comboBoxWithLabel("Repository URL:").setText(repoUrl);

    bot.button("OK").click();
    waitForAllBuildsToComplete();
    assertWizardError(null);
    assertWizardMessage("Select an Archetype");

    bot.button("Next >").click();

    bot.comboBoxWithLabel(Messages.getString("artifactComponent.groupId")).setText("org.sonatype.test");
    bot.comboBoxWithLabel(Messages.getString("artifactComponent.artifactId")).setText(PROJECT_NAME);
    bot.comboBoxWithLabel(Messages.getString("artifactComponent.package")).setText("org.sonatype.test");
    bot.button("Finish").click();

    waitForAllBuildsToComplete();

    selectProject(PROJECT_NAME);
    bot.menu("File").menu("Refresh").click();

    waitForAllBuildsToComplete();
    assertProjectsHaveNoErrors();
  }
}
