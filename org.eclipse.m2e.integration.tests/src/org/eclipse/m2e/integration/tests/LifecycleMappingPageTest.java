/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.integration.tests;

import java.io.File;
import java.io.IOException;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.SWTBotTestCase;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.AbstractSWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

import org.eclipse.m2e.integration.tests.common.SonatypeSWTBot;
import org.eclipse.m2e.integration.tests.common.SwtbotUtil;
import org.eclipse.m2e.integration.tests.common.UIIntegrationTestCase;
import org.eclipse.m2e.tests.common.FileHelpers;

public class LifecycleMappingPageTest extends SWTBotTestCase {

  private static final String TEST_PROJECT_ROOT = "projects/mavenImportWizardTests/";

  private SonatypeSWTBot bot;

  @Override
  public void setUp() throws Exception {
    bot = new SonatypeSWTBot();
    SWTBotPreferences.KEYBOARD_LAYOUT = "EN_US";
  }

  /*
   * Simple java project
   */
  public void testSimpleJavaProject() throws Exception {
    doImport(TEST_PROJECT_ROOT + "simpleJar");
    SWTBotShell shell = bot.shell("Import Maven Projects");
    try {
      shell.activate();
      SWTBotTree tree = bot.tree();
      assertEquals("Unexpected number of lifecycle rows", 1, tree.visibleRowCount());

      SWTBotTreeItem item = tree.getTreeItem("maven-jar-plugin:2.3.1:jar");
      assertEquals("Unexpected action", "Resolve Later", item.cell(1));

      item.expand();
      SWTBotTreeItem[] children = item.getItems();
      assertEquals("Expected one child", 1, children.length);

      bot.button("Cancel").click();
    } finally {
      try {
        bot.button("Cancel").click();
      } catch(Exception e) {
        // do nothing
      }
      SwtbotUtil.waitForClose(shell);
    }
  }

  /*
   * Simple project that uses modello
   */
  public void testModello() throws Exception {
    doImport(TEST_PROJECT_ROOT + "modelloReq");
    SWTBotShell shell = bot.shell("Import Maven Projects");
    try {
      shell.activate();
      SWTBotTree tree = bot.tree();
      assertEquals("Unexpected number of lifecycle rows", 4, tree.visibleRowCount());

      SWTBotTreeItem item = tree.getTreeItem("maven-jar-plugin:2.3.1:jar").expand();
      assertEquals("Children " + item.getText(), 2, item.getItems().length);
      item.collapse();
      assertEquals("Action maven-jar-plugin:2.3.1:jar", "Install mavenarchiver pomproperties", item.cell(1));

      item = tree.getTreeItem("modello-maven-plugin:1.5-SNAPSHOT:java").expand();
      assertEquals("Children " + item.getText(), 2, item.getItems().length);
      item.collapse();
      assertEquals("Action modello-maven-plugin:1.5-SNAPSHOT:java", "Install modello", item.cell(1));

      item = tree.getTreeItem("modello-maven-plugin:1.5-SNAPSHOT:xpp3-writer").expand();
      assertEquals("Children " + item.getText(), 2, item.getItems().length);
      item.collapse();
      assertEquals("Action modello-maven-plugin:1.5-SNAPSHOT:xpp3-writer", "Install modello", item.cell(1));

      item = tree.getTreeItem("modello-maven-plugin:1.5-SNAPSHOT:xpp3-reader").expand();
      assertEquals("Children " + item.getText(), 2, item.getItems().length);
      item.collapse();
      assertEquals("Action modello-maven-plugin:1.5-SNAPSHOT:xpp3-reader", "Install modello", item.cell(1));

      bot.button("Cancel").click();
    } finally {
      try {
        bot.button("Cancel").click();
      } catch(Exception e) {
        // do nothing
      }
      SwtbotUtil.waitForClose(shell);
    }
  }

  public void testResolveAllLater() throws Exception {
    doImport(TEST_PROJECT_ROOT + "modelloReq");
    SWTBotShell shell = bot.shell("Import Maven Projects");
    try {
      shell.activate();
      SWTBotTreeItem[] items = bot.tree().getAllItems();
      assertEquals("Unexpected number of lifecycle rows", 4, items.length);

      assertTrue("Unexpected text" + items[0].cell(1), items[0].cell(1).contains("Install"));
      items[1].click(1);
      select(items[1], 1);
      assertEquals("Unexpected text", "Resolve Later", items[1].cell(1));

      items[2].click(1);
      select(items[2], 1);
      assertEquals("Unexpected text", "Do Not Execute (add to pom)", items[2].cell(1));

      items[3].click(1);
      select(items[3], 2);
      assertEquals("Unexpected text", "Do Not Execute (add to parent)", items[3].cell(1));

      bot.button("Resolve All Later").click();
      for(SWTBotTreeItem item : items) {
        assertTrue(item.cell(1).equals("Resolve Later"));
      }

      bot.button("Cancel").click();
    } finally {
      try {
        bot.button("Cancel").click();
      } catch(Exception e) {
        // do nothing
      }
      SwtbotUtil.waitForClose(shell);
    }
  }

  public void testAutoSelect() throws Exception {
    doImport(TEST_PROJECT_ROOT + "modelloReq");
    SWTBotShell shell = bot.shell("Import Maven Projects");
    try {
      shell.activate();

      SWTBotTreeItem[] items = bot.tree().getAllItems();
      assertEquals("Unexpected number of lifecycle rows", 4, items.length);

      assertTrue("Unexpected text" + items[0].cell(1), items[0].cell(1).contains("Install"));
      items[1].click(1);
      select(items[1], 1);
      assertEquals("Unexpected text", "Resolve Later", items[1].cell(1));

      items[2].click(1);
      select(items[2], 1);
      assertEquals("Unexpected text", "Do Not Execute (add to pom)", items[2].cell(1));

      items[3].click(1);
      select(items[3], 2);
      assertEquals("Unexpected text", "Do Not Execute (add to parent)", items[3].cell(1));

      bot.button("Auto Select").click();
      waitForDiscovery();

      for(SWTBotTreeItem item : items) {
        assertTrue(item.cell(1).contains("Install"));
      }

      bot.button("Cancel").click();
    } finally {
      try {
        bot.button("Cancel").click();
      } catch(Exception e) {
        // do nothing
      }
      SwtbotUtil.waitForClose(shell);
    }
  }

  private void select(AbstractSWTBot<?> item, int down) {
    for (int i=0; i<down; i++) {
      item.pressShortcut(Keystrokes.DOWN);
    }
    item.pressShortcut(Keystrokes.TAB);
  }

  /*
   * Stops on the LifecycleMappingPage  
   */
  private void doImport(String rootPath) throws Exception {
    File tempDir = copyProject(rootPath);
    UIIntegrationTestCase.waitForAllBuildsToComplete();

    try {
      bot.menu("File").menu("Import...").click();

      SWTBotShell shell = bot.shell("Import");
      shell.activate();

      bot.tree().expandNode("Maven").select("Existing Maven Projects");
      bot.button("Next >").click();
      SWTBotCombo combo = bot.comboBoxWithLabel("Root Directory:");
      combo.setFocus();
      combo.setText(tempDir.getCanonicalPath());

      bot.button("Refresh").setFocus();
      waitForDiscovery();

      bot.button("Next >").click();
    } finally {
      deleteDirectory(tempDir);
    }
  }

  private File copyProject(String path) throws IOException {
    File temp = createTempDir("sonatype");
    FileHelpers.copyDir(new File(path), temp);
    return temp;
  }

  protected static File createTempDir(String prefix) throws IOException {
    File temp = null;
    temp = File.createTempFile(prefix, "");
    if(!temp.delete()) {
      throw new IOException("Unable to delete temp file:" + temp.getName());
    }
    if(!temp.mkdir()) {
      throw new IOException("Unable to create temp dir:" + temp.getName());
    }
    return temp;
  }

  private void deleteDirectory(File dir) {
    File[] fileArray = dir.listFiles();
    if(fileArray != null) {
      for(int i = 0; i < fileArray.length; i++ ) {
        if(fileArray[i].isDirectory())
          deleteDirectory(fileArray[i]);
        else if(!fileArray[i].delete())
          fileArray[i].deleteOnExit();
      }
    }
    if(!dir.delete()) {
      dir.deleteOnExit();
    }
  }

  private void waitForDiscovery() {
    bot.waitUntil(new ICondition() {
      private SWTBotButton cancel;

      public boolean test() throws Exception {
        return cancel.isEnabled();
      }

      public void init(SWTBot bot) {
        cancel = bot.button("Cancel");
      }

      public String getFailureMessage() {
        return "Next button is not enabled";
      }
    });
  }
}
