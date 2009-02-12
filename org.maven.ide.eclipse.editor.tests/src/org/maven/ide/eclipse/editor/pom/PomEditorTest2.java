package org.maven.ide.eclipse.editor.pom;

import org.eclipse.swt.SWT;

import com.windowtester.runtime.WT;
import com.windowtester.runtime.WaitTimedOutException;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.TableItemLocator;

public class PomEditorTest2 extends PomEditorTestBase {

  public void testNewPropertiesSectionModel2XML() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    selectEditorTab(TAB_OVERVIEW);
    
    expandSectionIfRequired("propertiesSection", "Properties");
    collapseSectionIfRequired("modulesSection", "Modules");

    addProperty("pz", "pz");
    getUI().click(2, new TableItemLocator("pz : pz"));
    getUI().wait(new ShellShowingCondition("Edit property"));
    getUI().keyClick(SWT.CTRL, 'a');
    getUI().enterText("p12");
    getUI().keyClick(WT.TAB);
    getUI().keyClick(SWT.CTRL, 'a');
    getUI().enterText("p12");
    getUI().click(new ButtonLocator("OK"));
    getUI().wait(new ShellDisposedCondition("Edit property"));
    addProperty("p2", "p2");
    getUI().click(new TableItemLocator("p12 : p12"));
    getUI().click(new ButtonLocator("Delete"));
    
    selectEditorTab(TAB_POM_XML);
    String editorText = getEditorText();
    assertTrue(editorText, editorText.contains("<p2>p2</p2>"));

    getUI().keyClick(SWT.CTRL, 'z');
    getUI().keyClick(SWT.CTRL, 'z');
    getUI().keyClick(SWT.CTRL, 'z');
    editorText = getEditorText();
    assertTrue(editorText, editorText.contains("<pz>pz</pz>"));
    
    getUI().keyClick(SWT.CTRL, 'z');
    editorText = getEditorText();
    assertFalse(editorText, editorText.contains("<properties>"));
    getUI().keyClick(SWT.CTRL, 'y');
    editorText = getEditorText();
    assertTrue(editorText, editorText.contains("<pz>pz</pz>"));
    getUI().keyClick(SWT.CTRL, 's');
  }

  public void testPropertiesSectionXML2Model() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    expandSectionIfRequired("propertiesSection", "Properties");
    collapseSectionIfRequired("modulesSection", "Modules");

    selectEditorTab(TAB_POM_XML);
    
    replaceText("<pz>pz</pz>", "<prop>hoho</prop>");
    replaceText("<prop>hoho</prop>", "<prop>hoho</prop><prop1>hoho1</prop1>");

    selectEditorTab(TAB_OVERVIEW);

    getUI().click(2, new TableItemLocator("prop : hoho"));
    getUI().wait(new ShellShowingCondition("Edit property"));
    getUI().click(new ButtonLocator("OK"));
    getUI().wait(new ShellDisposedCondition("Edit property"));
    getUI().click(2, new TableItemLocator("prop1 : hoho1"));
    getUI().wait(new ShellShowingCondition("Edit property"));
    getUI().click(new ButtonLocator("OK"));
    getUI().wait(new ShellDisposedCondition("Edit property"));

    selectEditorTab(TAB_POM_XML);
    replaceText("<prop>hoho</prop><prop1>hoho1</prop1>", "<prop>hoho</prop>");
    selectEditorTab(TAB_OVERVIEW);

    getUI().click(2, new TableItemLocator("prop : hoho"));
    getUI().wait(new ShellShowingCondition("Edit property"));
    getUI().click(new ButtonLocator("OK"));
    getUI().wait(new ShellDisposedCondition("Edit property"));
  }

  ///MNGECLIPSE-912
  public void testCloseAllAndSave() throws Exception {
    openPomFile(TEST_POM_POM_XML);
    
    getUI().click(new CTabItemLocator(TEST_POM_POM_XML));
    selectEditorTab(TAB_POM_XML);
    replaceText("org.foo", "org.foo1");
    selectEditorTab(TAB_OVERVIEW);
    getUI().contextClick(new CTabItemLocator("*" + TEST_POM_POM_XML), "Close &All");
    getUI().wait(new ShellDisposedCondition("Progress Information"));
    getUI().wait(new ShellShowingCondition("Save Resource"));
    getUI().click(new ButtonLocator("&Yes"));
    getUI().wait(new ShellDisposedCondition("Save Resource"));

    openPomFile(TEST_POM_POM_XML);
    selectEditorTab(TAB_OVERVIEW);
    assertTextValue("groupId", "org.foo1");
  }
  
  private void addProperty(String name, String value) throws WidgetSearchException, WaitTimedOutException {
    getUI().click(new ButtonLocator("Add..."));
    getUI().wait(new ShellShowingCondition("Add property"));
    getUI().enterText(name);
    getUI().keyClick(WT.TAB);
    getUI().enterText(value);
    getUI().keyClick(WT.CR);
    getUI().wait(new ShellDisposedCondition("Add property"));
  }


}
