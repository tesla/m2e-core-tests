package org.maven.ide.eclipse.editor.pom;

import org.eclipse.swt.SWT;

import com.windowtester.runtime.WT;
import com.windowtester.runtime.WaitTimedOutException;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.locator.XYLocator;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.LabeledTextLocator;
import com.windowtester.runtime.swt.locator.TableItemLocator;

public class PomEditorTest2 extends PomEditorTestBase {

  public void testNewPropertiesSectionModel2XML() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    selectEditorTab(TAB_OVERVIEW);
    
    expandSectionIfRequired("propertiesSection", "Properties");
    collapseSectionIfRequired("modulesSection", "Modules");

    addProperty("p1", "p1");
    addProperty("p2", "p2");
    ui.click(new TableItemLocator("p1 : p1"));
    ui.click(new ButtonLocator("Delete"));
    ui.click(2, new TableItemLocator("p2 : p2"));
    ui.wait(new ShellShowingCondition("Edit property"));
    ui.keyClick(SWT.CTRL, 'a');
    ui.enterText("p12");
    ui.keyClick(WT.TAB);
    ui.keyClick(SWT.CTRL, 'a');
    ui.enterText("p12");
    ui.click(new ButtonLocator("OK"));
    ui.wait(new ShellDisposedCondition("Edit property"));
    
    selectEditorTab(TAB_POM_XML);
    String editorText = getEditorText();
    assertTrue(editorText, editorText.contains("<properties><p12>p12</p12></properties>"));

    ui.keyClick(SWT.CTRL, 'z');
    ui.keyClick(SWT.CTRL, 'z');
    ui.keyClick(SWT.CTRL, 'z');
    editorText = getEditorText();
    assertTrue(editorText, editorText.contains("<properties><p2>p2</p2></properties>"));
    ui.keyClick(SWT.CTRL, 'z');
    
    //TODO: undo should do a complete clean
//    ui.keyClick(SWT.CTRL, 'z');
//    assertFalse(editorText, editorText.contains("<properties>"));
//    ui.keyClick(SWT.CTRL, 'y');
//    assertTrue(editorText, editorText.contains("<properties><p2>p2</p2></properties>"));
    ui.keyClick(SWT.CTRL, 's');
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
