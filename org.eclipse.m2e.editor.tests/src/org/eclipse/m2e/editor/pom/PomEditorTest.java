/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.editor.pom;

import static org.junit.Assert.*;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.integration.tests.common.ContextMenuHelper;
import org.eclipse.m2e.integration.tests.common.SwtbotUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.junit.Ignore;
import org.junit.Test;


/**
 * @author Eugene Kuleshov
 * @author Anton Kraev
 */
@Ignore
public class PomEditorTest extends PomEditorTestBase {

  @Test
  public void testUpdatingArtifactIdInXmlPropagatedToForm() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    selectEditorTab(TAB_POM_XML);
    replaceText("test-pom", "test-pom1");

    selectEditorTab(TAB_OVERVIEW);
    assertTextValue("artifactId", "test-pom1");
  }

  @Test
  public void testFormToXmlAndXmlToFormInParentArtifactId() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    // test FORM->XML and XML->FORM update of parentArtifactId
    selectEditorTab(TAB_OVERVIEW);

    bot.section("Parent").expand();
    setTextValue("parentArtifactId", "parent2");

    selectEditorTab(TAB_POM_XML);
    replaceTextWithWrap("parent2", "parent3", true);

    selectEditorTab(TAB_OVERVIEW);
    assertTextValue("parentArtifactId", "parent3");
  }

  @Test
  public void testNewSectionCreation() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    bot.section("Organization").expand();

    setTextValue("organizationName", "org.foo");

    selectEditorTab(TAB_POM_XML);
    replaceTextWithWrap("org.foo", "orgfoo1", true);

    selectEditorTab(TAB_OVERVIEW);
    assertTextValue("organizationName", "orgfoo1");
  }

  @Test
  public void testUndoRedo() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    bot.sectionWithName("organizationSection").expand();
    SWTBotText text = bot.textWithName("organizationName");
    text.setFocus();
    text.setText("");
    text.typeText("orgfoo");
    bot.textWithName("organizationUrl").setFocus();
    text.setFocus();
    text.typeText("1");

    // test undo
    text.pressShortcut(SwtbotUtil.getUndoShortcut());
    assertTextValue("organizationName", "orgfoo");
    // test redo
    text.pressShortcut(SwtbotUtil.getRedoShortcut());
    assertTextValue("organizationName", "orgfoo1");
  }

  @Test
  public void testDeletingScmSectionInXmlPropagatedToForm() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    selectEditorTab(TAB_OVERVIEW);
    bot.section("SCM").expand();

    setTextValue("scmUrl", "http://m2eclipse");
    assertTextValue("scmUrl", "http://m2eclipse");
    selectEditorTab(TAB_POM_XML);
    delete("<scm>", "</scm>");
    selectEditorTab(TAB_OVERVIEW);
    assertTextValue("scmUrl", "");
    selectEditorTab(TAB_POM_XML);
    delete("<organization>", "</organization>");
    selectEditorTab(TAB_OVERVIEW);
    bot.sectionWithName("organizationSection").expand();
    assertTextValue("organizationName", "");
    setTextValue("scmUrl", "http://m2eclipse");
    assertTextValue("scmUrl", "http://m2eclipse");
  }

  @Test
  public void testExternalModification() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    // externally replace file contents
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFile file = root.getFile(new Path(TEST_POM_POM_XML));
    File f = new File(file.getLocation().toOSString());
    String text = getEditorText();
    assertFalse(text.contains("PASSED"));
    text = text.replace("Test-Name", "PASSED");
    assertTrue(text.contains("PASSED"));
    setContents(f, text);

    // reload the file
    selectProject(PROJECT_NAME).expandNode(PROJECT_NAME).getNode("pom.xml").doubleClick();

    bot.shell("File Changed").activate();
    bot.button("Yes").click();

    bot.sectionWithName("projectSection").expand();
    assertTextValue("projectName", "PASSED");

    // verify that value changed in xml and in the form
    selectEditorTab(TAB_POM_XML);
    String editorText = getEditorText();
    assertTrue(editorText, editorText.contains("<name>PASSED</name>"));

    // XXX verify that value changed on a page haven't been active before
  }

  @Test
  public void testNewEditorIsClean() throws Exception {
    MavenPomEditor editor = openPomFile(TEST_POM_POM_XML);

    // close/open the file 
    bot.editorByTitle(TEST_POM_POM_XML).close();

    openPomFile(TEST_POM_POM_XML);

    // test the editor is clean
    waitForEditorDirtyState(editor, false);
  }

  //MNGECLIPSE-874
  @Test
  public void testUndoAfterSave() throws Exception {
    MavenPomEditor editor = openPomFile(TEST_POM_POM_XML);

    // make a change 
    selectEditorTab(TAB_POM_XML);
    replaceText("Test-Name", "FAILED");
    selectEditorTab(TAB_OVERVIEW);

    //save file
    save();

    // test the editor is clean
    waitForEditorDirtyState(editor, false);

    assertTextValue("projectName", "FAILED");

    // undo change
    bot.textWithName("projectName").pressShortcut(SwtbotUtil.getUndoShortcut());

    // test the editor is dirty
    waitForEditorDirtyState(editor, true);

    //test the value
    assertTextValue("projectName", "Test-Name");

    //save file
    save();
  }

  @Test
  public void testAfterUndoEditorIsClean() throws Exception {
    MavenPomEditor editor = openPomFile(TEST_POM_POM_XML);

    // make a change 
    selectEditorTab(TAB_POM_XML);
    replaceText("parent4", "parent7");
    selectEditorTab(TAB_OVERVIEW);
    // undo change
    bot.activeEditor().toTextEditor().pressShortcut(SwtbotUtil.getUndoShortcut());

    // test the editor is clean
    waitForEditorDirtyState(editor, false);
  }

  @Test
  public void testEmptyFile() throws Exception {
    String name = PROJECT_NAME + "/test.pom";
    createFile(name, "");
    openPomFile(name);

    assertTextValue("artifactId", "");
    setTextValue("artifactId", "artf1");
    selectEditorTab(TAB_POM_XML);
    replaceText("artf1", "artf2");
    selectEditorTab(TAB_OVERVIEW);
    assertTextValue("artifactId", "artf2");

  }

  @Test
  public void testDiscardedFileDeletion() throws Exception {
    String name = PROJECT_NAME + "/another.pom";
    createFile(name, "");
    openPomFile(name);

    bot.editorByTitle(name).close();

    openPomFile(name);
    setTextValue("groupId", "abc");

    bot.activeEditor().toTextEditor().pressShortcut(SwtbotUtil.getCloseShortcut());
    bot.shell("Save Resource").activate();
    bot.button("No").click();

    selectProject(PROJECT_NAME).expandNode(PROJECT_NAME).getNode("another.pom").select();
    ContextMenuHelper.clickContextMenu(bot.tree(), "Delete");
    bot.shell("Confirm Delete").activate();
    bot.button("OK").click();

    IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(name));
    waitForFileToDisappear(file);
  }

  // MNGECLIPSE-833
  @Test
  public void testSaveAfterPaste() throws Exception {
    String name = PROJECT_NAME + "/another2.pom";
    String str = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" " //
        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
        + "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">" //
        + "<modelVersion>4.0.0</modelVersion>" //
        + "<groupId>test</groupId>" //
        + "<artifactId>parent</artifactId>" //
        + "<packaging>pom</packaging>" //
        + "<version>0.0.1-SNAPSHOT</version>" //
        + "</project>";
    createFile(name, str);

    MavenPomEditor editor = openPomFile(name);

    selectEditorTab(TAB_POM_XML);
    waitForEditorDirtyState(editor, false);
    findText("</project>");
    bot.activeEditor().toTextEditor().pressShortcut(SWT.NONE, SWT.ARROW_LEFT, (char) 0);

    copy("<properties><sample>sample</sample></properties>");
    bot.activeEditor().toTextEditor().pressShortcut(SwtbotUtil.getPasteShortcut());
    waitForEditorDirtyState(editor, true);

    save();
    waitForEditorDirtyState(editor, false);
    bot.activeEditor().toTextEditor().pressShortcut(SwtbotUtil.getCloseShortcut());
  }

  // MNGECLIPSE-835
  @Test
  public void testModulesEditorActivation() throws Exception {
    MavenPomEditor editor = openPomFile(TEST_POM_POM_XML);

    bot.activeEditor().toTextEditor().pressShortcut(SwtbotUtil.getMaximizeEditorShortcut());

    bot.section("Parent").expand();
    // getUI().click(new SWTWidgetLocator(Label.class, "Properties"));

    selectEditorTab(TAB_OVERVIEW);

    bot.button("Create...").click();
    bot.table().getTableItem("?").select();

    selectEditorTab(TAB_POM_XML);
    replaceTextWithWrap(">?<", ">foo1<", true);

    save();

    selectEditorTab(TAB_OVERVIEW);
    bot.table().getTableItem("foo1").select();

    bot.activeEditor().toTextEditor().pressShortcut(SwtbotUtil.getMaximizeEditorShortcut());

    // test the editor is clean
    waitForEditorDirtyState(editor, false);
  }

  /*
   * Verify that the POM XML editor is smart enough to offer proper content assist even if the POM does not explicitly
   * declare a schema (MNGECLIPSE-1770).
   */
  @Test
  public void testContentAssistWithoutSchema() throws Exception {
    String name = PROJECT_NAME + "/ca.pom";
    String str = "<project>\n" //
        + "<modelVersion>4.0.0</modelVersion>\n" //
        + "<groupId>test</groupId>\n" //
        + "<artifactId>ca</artifactId>\n" //
        + "<packaging>jar</packaging>\n" //
        + "<version>0.0.1-SNAPSHOT</version>\n" //
        + "<build>\n" //
        + "</build>\n" //
        + "</project>\n";
    createFile(name, str);

    openPomFile(name);

    selectEditorTab(TAB_POM_XML);
    findText("</build>");

    SWTBotEclipseEditor editor = bot.activeEditor().toTextEditor();
    editor.pressShortcut(KeyStroke.getInstance(SWT.ARROW_LEFT));
    editor.pressShortcut(SWT.CTRL, ' ');
    editor.pressShortcut(KeyStroke.getInstance(SWT.LF));
    String text = editor.getText();
    assertTrue(text, text.contains("<defaultGoal>"));
  }

  private void waitForEditorDirtyState(MavenPomEditor editor, boolean dirtyState) {
    for(int n = 0; n < 100; n++ ) {
      if(dirtyState == editor.isDirty()) {
        return;
      }
      bot.sleep(100);
    }
    fail("Timed out waiting for editor dirty state: " + dirtyState);
  }

  private void waitForFileToDisappear(IFile file) {
    for(int n = 0; n < 100; n++ ) {
      if(!file.exists()) {
        return;
      }
      bot.sleep(100);
    }
    fail("Timed out waiting for file to be deleted: " + file);
  }
}
