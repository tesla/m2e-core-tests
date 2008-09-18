package org.maven.ide.eclipse.editor.pom;

import org.eclipse.swt.SWT;

import com.windowtester.runtime.WT;
import com.windowtester.runtime.WaitTimedOutException;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.TableItemLocator;
import com.windowtester.runtime.util.ScreenCapture;

public class PomEditorTest2 extends PomEditorTestBase {

  public void testNewPropertiesSectionModel2XML() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    selectEditorTab(TAB_OVERVIEW);
    
    expandSectionIfRequired("propertiesSection", "Properties");
    collapseSectionIfRequired("modulesSection", "Modules");

    addProperty("pz", "pz");
    ui.click(2, new TableItemLocator("pz : pz"));
    ui.wait(new ShellShowingCondition("Edit property"));
    ui.keyClick(SWT.CTRL, 'a');
    ui.enterText("p12");
    ui.keyClick(WT.TAB);
    ui.keyClick(SWT.CTRL, 'a');
    ui.enterText("p12");
    ui.click(new ButtonLocator("OK"));
    ui.wait(new ShellDisposedCondition("Edit property"));
    addProperty("p2", "p2");
    ui.click(new TableItemLocator("p12 : p12"));
    ui.click(new ButtonLocator("Delete"));
    
    selectEditorTab(TAB_POM_XML);
    String editorText = getEditorText();
    assertTrue(editorText, editorText.contains("<p2>p2</p2>"));

    ui.keyClick(SWT.CTRL, 'z');
    ui.keyClick(SWT.CTRL, 'z');
    ui.keyClick(SWT.CTRL, 'z');
    editorText = getEditorText();
    assertTrue(editorText, editorText.contains("<pz>pz</pz>"));
    
    ui.keyClick(SWT.CTRL, 'z');
    editorText = getEditorText();
    assertFalse(editorText, editorText.contains("<properties>"));
    ui.keyClick(SWT.CTRL, 'y');
    editorText = getEditorText();
    assertTrue(editorText, editorText.contains("<pz>pz</pz>"));
    ui.keyClick(SWT.CTRL, 's');
  }

  public void testPropertiesSectionXML2Model() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    expandSectionIfRequired("propertiesSection", "Properties");
    collapseSectionIfRequired("modulesSection", "Modules");

    selectEditorTab(TAB_POM_XML);
    
    replaceText("<pz>pz</pz>", "<prop>hoho</prop>");
    replaceText("<prop>hoho</prop>", "<prop>hoho</prop><prop1>hoho1</prop1>");

    selectEditorTab(TAB_OVERVIEW);

    ui.click(2, new TableItemLocator("prop : hoho"));
    ui.wait(new ShellShowingCondition("Edit property"));
    ui.click(new ButtonLocator("OK"));
    ui.wait(new ShellDisposedCondition("Edit property"));
    ui.click(2, new TableItemLocator("prop1 : hoho1"));
    ui.wait(new ShellShowingCondition("Edit property"));
    ui.click(new ButtonLocator("OK"));
    ui.wait(new ShellDisposedCondition("Edit property"));

    selectEditorTab(TAB_POM_XML);
    replaceText("<prop>hoho</prop><prop1>hoho1</prop1>", "<prop>hoho</prop>");
    selectEditorTab(TAB_OVERVIEW);

    ui.click(2, new TableItemLocator("prop : hoho"));
    ui.wait(new ShellShowingCondition("Edit property"));
    ui.click(new ButtonLocator("OK"));
    ui.wait(new ShellDisposedCondition("Edit property"));
  }

  private void addProperty(String name, String value) throws WidgetSearchException, WaitTimedOutException {
    ui.click(new ButtonLocator("Add..."));
    ui.wait(new ShellShowingCondition("Add property"));
    ui.enterText(name);
    ui.keyClick(WT.TAB);
    ui.enterText(value);
    ui.keyClick(WT.CR);
    ui.wait(new ShellDisposedCondition("Add property"));
  }


}
