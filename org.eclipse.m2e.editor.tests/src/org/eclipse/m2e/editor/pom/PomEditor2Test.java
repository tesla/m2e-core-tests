
package org.eclipse.m2e.editor.pom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import junit.framework.Assert;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.m2e.integration.tests.common.SwtbotUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class PomEditor2Test extends PomEditorTestBase {

  ///MNGECLIPSE-912
  @Test
  public void testCloseAllAndSave() throws Exception {
    createArchetypeProject("maven-archetype-quickstart", "projectC");
    SWTBotEclipseEditor editor = bot.editorByTitle(openPomFile("projectC/pom.xml").getTitle()).toTextEditor();
    SWTBot ebot = editor.bot();
    ebot.cTabItem("pom.xml").activate();

    replaceText("org.sonatype.test", "org.sonatype.test1");
    ebot.cTabItem("Overview").activate();

    editor.saveAndClose();
    waitForAllEditorsToSave();

    editor = bot.editorByTitle(openPomFile("projectC/pom.xml").getTitle()).toTextEditor();
    editor.bot().cTabItem("Overview").activate();
    assertTextValue("groupId", "org.sonatype.test1");

    editor.saveAndClose();
  }

  @Test
  public void testNewPropertiesSectionModel2XML() throws Exception {
    SWTBotEclipseEditor editor = bot.editorByTitle(openPomFile(TEST_POM_POM_XML).getTitle()).toTextEditor();
    editor.bot().cTabItem("Overview").activate();

    expandSectionIfRequired("propertiesSection", "Properties");
    collapseSectionIfRequired("modulesSection", "Modules");

    SWTBotTable properties = bot.table();
    assertEquals(0, properties.rowCount());

    addProperty("pz", "pz");

    properties.select(0);
    properties.pressShortcut(KeyStroke.getInstance(SWT.LF));

    SWTBotShell shell = bot.shell("Edit property");
    try {
      bot.textWithLabel("Name:").setText("p12");
      bot.textWithLabel("Value:").setText("p12");

      bot.button("OK").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }
    addProperty("p2", "p2");

    properties.select(0);
    bot.button("Delete").click();

    editor.bot().cTabItem("pom.xml").activate();

    String editorText = editor.getText();
    assertTrue(editorText, editorText.contains("<p2>p2</p2>"));

    editor.pressShortcut(SwtbotUtil.getUndoShortcut());
    editor.pressShortcut(SwtbotUtil.getUndoShortcut());
    editor.pressShortcut(SwtbotUtil.getUndoShortcut());

    editorText = editor.getText();
    assertTrue(editorText, editorText.contains("<pz>pz</pz>"));

    editor.pressShortcut(SwtbotUtil.getUndoShortcut());
    editorText = editor.getText();
    assertFalse(editorText, editorText.contains("<properties>"));
    editor.pressShortcut(SwtbotUtil.getRedoShortcut());
    editorText = editor.getText();
    assertTrue(editorText, editorText.contains("<pz>pz</pz>"));

    editor.saveAndClose();
  }

  @Test
  public void testPropertiesSectionXML2Model() throws Exception {
    SWTBotEclipseEditor editor = bot.editorByTitle(openPomFile(TEST_POM_POM_XML).getTitle()).toTextEditor();

    expandSectionIfRequired("propertiesSection", "Properties");
    collapseSectionIfRequired("modulesSection", "Modules");

    SWTBotTable table = editor.bot().table();

    editor.bot().cTabItem("Overview").activate();
    validateProperty(table, "pz", "pz");

    editor.bot().cTabItem("pom.xml").activate();

    replaceText("<pz>pz</pz>", "<prop>hoho</prop>");
    replaceText("<prop>hoho</prop>", "<prop>hoho</prop><prop1>hoho1</prop1>");

    editor.bot().cTabItem("Overview").activate();
    validateProperty(table, "prop", "hoho");
    validateProperty(table, "prop1", "hoho1");

    editor.bot().cTabItem("pom.xml").activate();
    replaceText("<prop>hoho</prop><prop1>hoho1</prop1>", "<prop>hoho</prop>");
    editor.bot().cTabItem("Overview").activate();

    validateProperty(table, "prop", "hoho");
    editor.saveAndClose();
  }

  private void validateProperty(SWTBotTable table, String key, String value) {
    if(table.rowCount() == 0) {
      return;
    }

    table.select(table.indexOf(key));
    table.pressShortcut(KeyStroke.getInstance(SWT.LF));

    SWTBotShell shell = bot.shell("Edit property");
    try {
      Assert.assertEquals(key, bot.textWithLabel("Name:").getText());
      Assert.assertEquals(value, bot.textWithLabel("Value:").getText());

      bot.button("OK").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }
  }

  private void addProperty(String name, String value) {
    bot.button("Create...").click();

    SWTBotShell shell = bot.shell("Add property");
    try {
      shell.activate();

      bot.textWithLabel("Name:").setText(name);
      bot.textWithLabel("Value:").setText(value);

      bot.button("OK").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }
  }

  @Test
  public void testInternalModificationUpdatesModel() throws Exception {
    SWTBotEclipseEditor editor = bot.editorByTitle(openPomFile(TEST_POM_POM_XML).getTitle()).toTextEditor();

    assertTextValue("version", "1.0.0");

    // internally replace file contents
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFile file = root.getFile(new Path(TEST_POM_POM_XML));
    File f = new File(file.getLocation().toOSString());
    String text = getContents(f);
    file.setContents(new ByteArrayInputStream(text.replace("1.0.0", "1.0.1").getBytes()), true, true, null);

    file.refreshLocal(IResource.DEPTH_ZERO, null);
    waitForAllBuildsToComplete();

    assertTextValue("version", "1.0.1");

    selectEditorTab(TAB_POM_XML);
    String editorText = getEditorText();
    assertTrue(editorText, editorText.contains("<version>1.0.1</version>"));

  }

  @Test
  public void testMutlipleMNGEclipse1312() throws Exception {
    clearProjects();

    createArchetypeProject("maven-archetype-quickstart", "projectA");
    createArchetypeProject("maven-archetype-quickstart", "projectB");

    SWTBotEclipseEditor editorA = bot.editorByTitle(openPomFile("projectA/pom.xml").getTitle()).toTextEditor();
    SWTBotEclipseEditor editorB = bot.editorByTitle(openPomFile("projectB/pom.xml").getTitle()).toTextEditor();

    assertFalse(editorA.isDirty());
    assertFalse(editorB.isDirty());

    editorA.show();
    editorA.bot().textWithLabel("Version:").setText("0.0.2-SNAPSHOT");
    editorB.show();
    editorB.bot().textWithLabel("Version:").setText("0.0.2-SNAPSHOT");

    assertTrue(editorA.isDirty());
    assertTrue(editorB.isDirty());

    editorB.save();

    waitForAllBuildsToComplete();

    assertTrue(editorA.isDirty());
    assertFalse(editorB.isDirty());

    editorA.save();

    waitForAllBuildsToComplete();

    assertFalse(editorA.isDirty());
    assertFalse(editorB.isDirty());
  }

}
