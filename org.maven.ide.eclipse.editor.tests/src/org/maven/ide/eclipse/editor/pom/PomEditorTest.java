/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;

import org.apache.maven.model.Model;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;

import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WT;
import com.windowtester.runtime.WaitTimedOutException;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.condition.HasTextCondition;
import com.windowtester.runtime.condition.IConditionMonitor;
import com.windowtester.runtime.condition.IHandler;
import com.windowtester.runtime.locator.WidgetReference;
import com.windowtester.runtime.swt.UITestCaseSWT;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.internal.condition.NotCondition;
import com.windowtester.runtime.swt.internal.condition.eclipse.DirtyEditorCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.SWTWidgetLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;


/**
 * @author Eugene Kuleshov
 * @author Anton Kraev 
 */
// XXX fix significance of the order of tests. cannot just comment out a failed test, further test will not work
public class PomEditorTest extends UITestCaseSWT {

  private static final String TEST_POM_POM_XML = "test-pom/pom.xml";

  private static final String TAB_POM_XML_TAB = "pom.xml";

  private static final String TAB_OVERVIEW = "Overview";

  IUIContext ui;

  IWorkspaceRoot root;

  IWorkspace workspace;

  protected void setUp() throws Exception {
    super.setUp();
    
    ui = getUI();
    workspace = ResourcesPlugin.getWorkspace();
    root = workspace.getRoot();
  }

  protected void oneTimeSetup() throws Exception {
    super.oneTimeSetup();

    if("Welcome".equals(getActivePage().getActivePart().getTitle())) {
      getUI().close(new CTabItemLocator("Welcome"));
    }
    
    IConditionMonitor monitor = (IConditionMonitor) getUI().getAdapter(IConditionMonitor.class);
    monitor.add(new ShellShowingCondition("Save Resource"), //
        new IHandler() {
          public void handle(IUIContext ui) {
            try {
              ui.click(new ButtonLocator("&Yes"));
            } catch(WidgetSearchException ex) {
              // ignore
            }
          }
        });

//    ui.click(new MenuItemLocator("Window/Open Perspective/Other..."));
//    ui.wait(new ShellShowingCondition("Open Perspective"));
//    ui.click(2, new TableItemLocator("Java( \\(default\\))?"));
//    ui.wait(new ShellDisposedCondition("Open Perspective"));
    IPerspectiveRegistry perspectiveRegistry = PlatformUI.getWorkbench().getPerspectiveRegistry();
    IPerspectiveDescriptor perspective = perspectiveRegistry
        .findPerspectiveWithId("org.eclipse.jdt.ui.JavaPerspective");
    getActivePage().setPerspective(perspective);

    createTestProject();
    
    openPomFile();
  }

  protected void oneTimeTearDown() throws Exception {
    super.oneTimeTearDown();

//    ui.keyClick(SWT.CTRL | SWT.SHIFT, 's');  // save all to prevent "Save" confirmation dialog
//    
//    Display.getDefault().syncExec(new Runnable() {
//      public void run() {
//        getActivePage().closeAllEditors(false);
//      }
//    });
  }

  public void testUpdatingArtifactIdInXmlPropagatedToForm() throws Exception {
    ui.keyClick(SWT.CTRL, 'm');
    ui.click(new CTabItemLocator(TAB_POM_XML_TAB));
    replaceText("test-pom", "test-pom1");

    ui.click(new CTabItemLocator(TAB_OVERVIEW));
    assertTextValue("artifactId", "test-pom1");
  }

  public void testFormToXmlAndXmlToFormInParentArtifactId() throws Exception {
    // test FORM->XML and XML->FORM update of parentArtifactId
    ui.click(new CTabItemLocator(TAB_OVERVIEW));
    ui.click(new SWTWidgetLocator(Label.class, "Parent"));
    setTextValue("parentArtifactId", "parent2");

    ui.click(new CTabItemLocator(TAB_POM_XML_TAB));
    replaceText("parent2", "parent3");
    
    ui.click(new CTabItemLocator(TAB_OVERVIEW));
    assertTextValue("parentArtifactId", "parent3");
  }

  public void testDeletingScmSectionInXmlPropagatedToForm() throws Exception {
    ui.click(new CTabItemLocator(TAB_OVERVIEW));
    ui.click(new SWTWidgetLocator(Label.class, "SCM"));
    setTextValue("scmUrl", "http://svn.sonatype.org/m2eclipse");

    ui.click(new CTabItemLocator(TAB_POM_XML_TAB));
    setSelection("<scm>", "</scm>");
    ui.keyClick(SWT.BS);

    ui.click(new CTabItemLocator(TAB_OVERVIEW));
    assertTextValue("scmUrl", "");
  }

  public void testExternalModificationEditorClean() throws Exception {
    // save editor
    ui.keyClick(SWT.CTRL, 's');
    Thread.sleep(2000);

    // externally replace file contents
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFile file = root.getFile(new Path(TEST_POM_POM_XML));
    File f = new File(file.getLocation().toOSString());
    String text = getContents(f);
    setContents(f, text.replace("parent3", "parent4"));

    // reload the file
    ui.keyClick(SWT.CTRL, 'm');
    ui.click(new CTabItemLocator("Package Explorer"));
    ui.click(new CTabItemLocator(TEST_POM_POM_XML));
    // ui.contextClick(new TreeItemLocator(TEST_POM_POM_XML, new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")), "Refresh");
    
    ui.wait(new ShellShowingCondition("File Changed"));
    ui.click(new ButtonLocator("&Yes"));
    
    assertTextValue("parentArtifactId", "parent4");

    // verify that value changed in xml and in the form
    try {
      ui.keyClick(SWT.CTRL, 'm');
      ui.click(new CTabItemLocator(TAB_POM_XML_TAB));
      String editorText = getEditorText();
      assertTrue(editorText, editorText.contains("<artifactId>parent4</artifactId>"));
    } finally {
      ui.keyClick(SWT.CTRL, 'm');
    }
    
    // XXX verify that value changed on a page haven't been active before
  }

  // test that form and xml is not updated when refused to pickup external changes
  public void testExternalModificationNotUpdate() throws Exception {
    // XXX test that form and xml are not updated when refused to pickup external changes
  }
  
  // XXX update for new modification code 
  public void testExternalModificationEditorDirty() throws Exception {
    // make editor dirty
    ui.click(new CTabItemLocator(TEST_POM_POM_XML));
    ui.keyClick(SWT.CTRL, 'm');
    ui.click(new CTabItemLocator(TAB_POM_XML_TAB));
    replaceText("parent4", "parent5");
    ui.click(new CTabItemLocator(TAB_OVERVIEW));

    // externally replace file contents
    ui.keyClick(SWT.CTRL, 'm');
    IFile file = root.getFile(new Path(TEST_POM_POM_XML));
    File f = new File(file.getLocation().toOSString());
    String text = getContents(f);
    setContents(f, text.replace("parent4", "parent6"));

    // reload the file
    ui.click(new CTabItemLocator("Package Explorer"));
    // ui.click(new CTabItemLocator("*" + TEST_POM_POM_XML));  // take dirty state into the account
    ui.keyClick(SWT.F12);
    
    // ui.contextClick(new TreeItemLocator(TEST_POM_POM_XML, new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")), "Refresh");
    
    ui.wait(new ShellShowingCondition("File Changed"));
    ui.click(new ButtonLocator("&Yes"));
    
    assertTextValue("parentArtifactId", "parent6");
    
    // verify that value changed in xml and in the form
    try {
      ui.keyClick(SWT.CTRL, 'm');
      ui.click(new CTabItemLocator(TAB_POM_XML_TAB));
      String editorText = getEditorText();
      assertTrue(editorText, editorText.contains("<artifactId>parent6</artifactId>"));
    } finally {
      ui.keyClick(SWT.CTRL, 'm');
    }

    // XXX verify that value changed on a page haven't been active before
  }

  public void testEditorIsClosedWhenProjectIsClosed() throws Exception {
    // XXX test editor is closed when project is closed
    
  }
  
  public void testEditorIsClosedWhenProjectIsDeleted() throws Exception {
    // XXX test editor is closed when project is deleted
  
  }
  
  public void testNewEditorIsClean() throws Exception {
    // close/open the file 
    ui.close(new CTabItemLocator(TEST_POM_POM_XML));
    ui.click(2, new TreeItemLocator(TEST_POM_POM_XML, new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")));

    // test the editor is clean
    ui.assertThat(new NotCondition(new DirtyEditorCondition()));
  }

  public void testAfterUndoEditorIsClean() throws Exception {
    // make a change 
    ui.click(new CTabItemLocator(TEST_POM_POM_XML));
    ui.keyClick(SWT.CTRL, 'm');
    ui.click(new CTabItemLocator(TAB_POM_XML_TAB));
    replaceText("parent6", "parent7");
    ui.click(new CTabItemLocator(TAB_OVERVIEW));
    // undo change
    ui.keyClick(SWT.CTRL, 'z');

    // test the editor is clean
    ui.assertThat(new NotCondition(new DirtyEditorCondition()));
  }

  private void createTestProject() throws CoreException {
    // create new project with POM using new project wizard
    // ui.contextClick(new SWTWidgetLocator(Tree.class, new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")),
    //      "Ne&w/Maven Project");
    // ui.wait(new ShellShowingCondition("New Maven Project"));
    // ui.click(new ButtonLocator("Create a &simple project (skip archetype selection)"));
    // ui.click(new ButtonLocator("&Next >"));
    // ui.enterText("org.foo");
    // ui.keyClick(WT.TAB);
    // ui.enterText("test-pom");
    // ui.click(new ButtonLocator("&Finish"));
    // ui.wait(new ShellDisposedCondition("New Maven Project"));

    IProjectConfigurationManager configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
    
    Model model = new Model();
    model.setModelVersion("4.0.0");
    model.setGroupId("org.foo");
    model.setArtifactId("test-pom");
    model.setVersion("1.0.0");
    
    String[] folders = new String[0];
    ResolverConfiguration config = new ResolverConfiguration();
    
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("test-pom");
    IPath location = null;
    configurationManager.createSimpleProject(project, location, model, folders, config, new NullProgressMonitor());
  }

  private String openPomFile() {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFile file = root.getFile(new Path(TEST_POM_POM_XML));

    final IEditorInput editorInput = new FileEditorInput(file);
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        try {
          getActivePage().openEditor(editorInput, "org.maven.ide.eclipse.editor.MavenPomEditor", true);
        } catch(PartInitException ex) {
          throw new RuntimeException(ex);
        }
      }
    });
    return file.getLocation().toOSString();
  }

  private String getContents(File aFile) throws Exception {
    StringBuilder contents = new StringBuilder();

    BufferedReader input = new BufferedReader(new FileReader(aFile));
    String line = null; //not declared within while loop
    while((line = input.readLine()) != null) {
      contents.append(line);
      contents.append(System.getProperty("line.separator"));
    }
    return contents.toString();
  }

  static public void setContents(File aFile, String aContents) throws Exception {
    Writer output = new BufferedWriter(new FileWriter(aFile));
    output.write(aContents);
    output.flush();
    output.close();
  }

  private void setSelection(String startMarker, String endMarker) throws WaitTimedOutException, WidgetSearchException {
    // read text directly instead of using "Find" dialog
    findText(startMarker);
    final int offset1 = getSelection().getOffset();
    // Display.getDefault().syncExec(null);

    findText(endMarker);
    final int offset2 = getSelection().getOffset();

    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        getSelectionProvider().setSelection(new TextSelection(offset1, offset2 - offset1 + 6));
      }
    });
  }

  private void assertTextValue(String id, String value) {
    ui.assertThat(new HasTextCondition(new NamedWidgetLocator(id), value));
  }

  private void setTextValue(String id, String value) throws WidgetSearchException {
    ui.setFocus(new NamedWidgetLocator(id));
    ui.keyClick(SWT.CTRL, 'a');
    ui.enterText(value);
  }

  private void replaceText(String src, String target) throws WaitTimedOutException, WidgetSearchException {
    ui.keyClick(SWT.CTRL, 'f');
    ui.wait(new ShellShowingCondition("Find/Replace"));

    ui.enterText(src);
    ui.keyClick(WT.TAB);
    ui.enterText(target);
    ui.keyClick(SWT.ALT, 'a'); // "replace all"
    ui.close(new SWTWidgetLocator(Shell.class, "Find/Replace"));
    ui.wait(new ShellDisposedCondition("Find/Replace"));
  }

  private void findText(String src) throws WaitTimedOutException, WidgetSearchException {
    ui.keyClick(SWT.CTRL, 'f');
    ui.wait(new ShellShowingCondition("Find/Replace"));

    ui.enterText(src);
    ui.keyClick(WT.TAB);
    ui.keyClick(SWT.ALT, 'n'); // "find"
    ui.close(new SWTWidgetLocator(Shell.class, "Find/Replace"));
    ui.wait(new ShellDisposedCondition("Find/Replace"));
  }

  private ITextSelection getSelection() {
    final ITextSelection[] selection = new ITextSelection[1];
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        selection[0] = (ITextSelection) getSelectionProvider().getSelection();
      }
    });
    return selection[0];
  }

  ISelectionProvider getSelectionProvider() {
    return getEditorSite().getSelectionProvider();
  }

  private IEditorSite getEditorSite() {
    IWorkbenchPage activePage = getActivePage();
    IEditorPart editor = activePage.getActiveEditor();
    IEditorSite editorSite = editor.getEditorSite();
    return editorSite;
  }

  IWorkbenchPage getActivePage() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow window = workbench.getWorkbenchWindows()[0];
    return window.getActivePage();
  }

  private String getEditorText() {
    final String[] texts = new String[1];
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        try {
          WidgetReference ref = (WidgetReference) ui.find(new SWTWidgetLocator(StyledText.class));
          texts[0] = ((StyledText) ref.getWidget()).getText();
        } catch(WidgetSearchException ex) {
          // ignore
        }
      }
    });
    return texts[0];
  }

}
