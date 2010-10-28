/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.integration.tests;

import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.m2e.core.core.Messages;
import org.eclipse.m2e.integration.tests.common.SwtbotUtil;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.Ignore;
import org.junit.Test;


/**
 * @author dyocum
 */
@Ignore
public class MNGEclipse1687ArchetypeCreationTest extends M2EUIIntegrationTestCase {

  private static final String GROUP_ID = "org.eclipse.m2e.its";

  private static final String ARCHETYPE_ID = "maven-plugin-packaging";

  private static final String VERSION_ID = "1.0";

  private static final String PROJECT_NAME = "archetypeTestProjext";

  @Test()
  public void testArchetypeCreation() throws Exception {
    createProjectFromArchetype(ARCHETYPE_ID, "", "file:resources/remote-repo");
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
    SWTBotShell shell = bot.shell("New Project");
    try {
      shell.activate();
      bot.tree().expandNode("Maven").getNode("Maven Project").doubleClick();
      bot.button("Next >").click();

      bot.checkBox("Show the last version of Archetype only").deselect();

      bot.button("Add Archetype...").click();
      SWTBotShell addShell = bot.shell("Add Archetype");
      try {
        addShell.activate();

        bot.comboBoxWithLabel("Archetype Group Id:").setText(GROUP_ID);
        bot.comboBoxWithLabel("Archetype Artifact Id:").setText(artifactID);
        bot.comboBoxWithLabel("Archetype Version:").setText(VERSION_ID);
        bot.comboBoxWithLabel("Repository URL:").setText(repoUrl);

        bot.button("OK").click();
      } finally {
        SwtbotUtil.waitForClose(addShell);
      }

      waitForAllBuildsToComplete();

      bot.table().select(bot.table().indexOf(artifactID, 1));

      try {
        bot.button("Next >").click();
      } catch(Exception ex) {
        takeScreenShot(ex);
      }

      bot.comboBoxWithLabel(Messages.getString("artifactComponent.groupId")).setText("org.sonatype.test");
      bot.comboBoxWithLabel(Messages.getString("artifactComponent.artifactId")).setText(PROJECT_NAME);
      bot.comboBoxWithLabel(Messages.getString("artifactComponent.package")).setText("org.sonatype.test");
      bot.button("Finish").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }

    waitForAllBuildsToComplete();

    selectProject(PROJECT_NAME);
    bot.menu("File").menu("Refresh").click();

    waitForAllBuildsToComplete();
    assertProjectsHaveNoErrors();
  }
}
