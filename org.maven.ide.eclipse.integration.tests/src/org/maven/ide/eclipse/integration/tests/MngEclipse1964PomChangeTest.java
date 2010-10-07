/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import static org.junit.Assert.fail;

import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.junit.Test;
import org.maven.ide.eclipse.core.Messages;


/**
 * MNGECLIPSE-1964
 * <P/>
 * Creating new maven module while parent's pom.xml is open in editor results in a "file changed on disk" message.
 * <P/>
 * M2E 0.9.9.200912221003<BR/>
 * 1. Create a project with packaging of "pom"<BR/>
 * 2. Open up the pom.xml file, and go to the "pom.xml"<BR/>
 * 3. Create a new module, with the first project as it's parent.<BR/>
 * You get a message saying the file has changed on disk.
 * <P/>
 * This should not be happening.
 */
public class MngEclipse1964PomChangeTest extends M2EUIIntegrationTestCase {

  @Test
  public void testPomChange() throws Exception {

    // new parent project
    bot.menu("File").menu("New").menu("Project...").click();
    bot.shell("New Project").activate();
    bot.tree().expandNode("Maven").getNode("Maven Project").doubleClick();

    bot.checkBox(Messages.getString("wizard.project.page.project.simpleProject")).select();
    bot.button("Next >").click();

    bot.comboBoxWithLabel(Messages.getString("artifactComponent.groupId")).setText("group");
    bot.comboBoxWithLabel(Messages.getString("artifactComponent.artifactId")).setText("parent");
    bot.comboBoxWithLabel(Messages.getString("artifactComponent.packaging")).setText("pom");
    bot.button("Finish").click();

    waitForAllBuildsToComplete();

    // open pom in the editor
    bot.viewById(PACKAGE_EXPLORER_VIEW_ID).setFocus();
    selectProject("parent").expandNode("parent").getNode("pom.xml").doubleClick();
    bot.editorByTitle("parent/pom.xml").setFocus();
    bot.cTabItem("pom.xml").activate();

    // new child project
    bot.menu("File").menu("New").menu("Project...").click();
    bot.shell("New Project").activate();
    bot.tree().expandNode("Maven").getNode("Maven Module").doubleClick();

    // child project
    bot.checkBox(Messages.getString("wizard.project.page.project.simpleProject")).select();
    bot.comboBoxWithLabel(Messages.getString("wizard.module.page.parent.moduleName")).setText("child");
    bot.button(Messages.getString("wizard.module.page.parent.browse")).click();
    bot.shell(Messages.getString("projectSelectionDialog.title")).activate();
    bot.tree().getTreeItem("parent").doubleClick();
    bot.button("Finish").click();

    waitForAllBuildsToComplete();

    try {
      bot.shell("File Changed");
      fail("The \"File has changed on disk\" message should not be displayed");
    } catch(WidgetNotFoundException e) {
      //no dialog should be displayed
    }
  }
}
