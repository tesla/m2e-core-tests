package org.maven.ide.eclipse.editor.pom;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;

import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WT;
import com.windowtester.runtime.WaitTimedOutException;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.swt.condition.SWTIdleCondition;
import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.TableItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ContributedToolItemLocator;

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
  
  public void testInternalModificationUpdatesModel() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    assertTextValue("version", "1.0.0");
    
    // internally replace file contents
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFile file = root.getFile(new Path(TEST_POM_POM_XML));
    File f = new File(file.getLocation().toOSString());
    String text = getContents(f);
    file.setContents(new ByteArrayInputStream(text.replace("1.0.0", "1.0.1").getBytes()), true, true, null);

    file.refreshLocal(IResource.DEPTH_ZERO, null);
    getUI().wait(new JobsCompleteCondition());
    
    assertTextValue("version", "1.0.1");
    
    selectEditorTab(TAB_POM_XML);
    String editorText = getEditorText();
    assertTrue(editorText, editorText.contains("<version>1.0.1</version>"));
    
  }

  public void testMutlipleMNGEclipse1312() throws Exception {
    
    createArchetypeProjct("maven-archetype-quickstart", "projectA");
    createArchetypeProjct("maven-archetype-quickstart", "projectB");
    
    MavenPomEditor editorA = openPomFile("projectA/pom.xml");
    MavenPomEditor editorB = openPomFile("projectB/pom.xml");
    
    assertFalse(editorA.isDirty());
    assertFalse(editorB.isDirty());
    
    IUIContext ui = getUI();
    
    ui.click(new CTabItemLocator("projectA/pom.xml"));
    replaceText(new NamedWidgetLocator("version"), "0.0.2-SNAPSHOT");
    
    ui.click(new CTabItemLocator("projectB/pom.xml"));
    replaceText(new NamedWidgetLocator("version"), "0.0.2-SNAPSHOT");
    
    assertTrue(editorA.isDirty());
    assertTrue(editorB.isDirty());
    
    ui.keyClick(SWT.MOD1, 's');
    Thread.sleep(5000);
    ui.wait(new JobsCompleteCondition(), 240000);
    
    assertTrue(editorA.isDirty());
    assertFalse(editorB.isDirty());
    
    ui.keyClick(SWT.MOD1|SWT.SHIFT, 's');
    
    ui.wait(new SWTIdleCondition());
    ui.wait(new JobsCompleteCondition());
    
    assertFalse(editorA.isDirty());
    assertFalse(editorB.isDirty());
  }
  
  public void testMNGEclipse1081() throws Exception {
    
    IUIContext ui = getUI();
    
    IProject project = createArchetypeProjct("maven-archetype-quickstart", "aProject");
    openPomFile("aProject/pom.xml");
    
    ui.wait(new SWTIdleCondition());
    ui.click(new ContributedToolItemLocator("org.maven.ide.ecillpse.editor.showEffectivePOMAction"));
    ui.wait(new SWTIdleCondition());
    ui.close(new CTabItemLocator("aProject/pom.xml [effective]"));
    ui.wait(new SWTIdleCondition());
    
    addDependency(project, "commons-collections", "commons-collections", "1.0");
    getUI().click(new CTabItemLocator("Overview"));
    
    ui.click(new ContributedToolItemLocator("org.maven.ide.ecillpse.editor.showEffectivePOMAction"));
    ui.wait(new SWTIdleCondition());
    
    ui.click(new CTabItemLocator("Dependencies"));
    
    ui.click(new TableItemLocator("commons-collections : commons-collections : 1.0.*", new NamedWidgetLocator("list-editor-composite-table")));
    
  }
}
