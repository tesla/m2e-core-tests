/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
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

import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WT;
import com.windowtester.runtime.WaitTimedOutException;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.condition.HasTextCondition;
import com.windowtester.runtime.swt.UITestCaseSWT;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.SWTWidgetLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;


/**
 * @author Eugene Kuleshov
 * @author Anton Kraev
 */
public class PomEditorTest extends UITestCaseSWT {

  private static final String POM_XML_TAB = "pom.xml";

  private static final String OVERVIEW_TAB = "Overview";

  private IUIContext ui;

  protected void setUp() throws Exception {
    ui = getUI();
  }

  protected void oneTimeSetup() throws Exception {
    super.oneTimeSetup();

    ui = getUI();
    
    if("Welcome".equals(getActivePage().getActivePart().getTitle())) {
      ui.close(new CTabItemLocator("Welcome"));
    }

//    ui.click(new MenuItemLocator("Window/Open Perspective/Other..."));
//    ui.wait(new ShellShowingCondition("Open Perspective"));
//    ui.click(2, new TableItemLocator("Java( \\(default\\))?"));
//    ui.wait(new ShellDisposedCondition("Open Perspective"));
    IPerspectiveRegistry perspectiveRegistry = PlatformUI.getWorkbench().getPerspectiveRegistry();
    IPerspectiveDescriptor perspective = perspectiveRegistry.findPerspectiveWithId("org.eclipse.jdt.ui.JavaPerspective");
    getActivePage().setPerspective(perspective);
    
    //create new project with POM
    ui.contextClick(new SWTWidgetLocator(Tree.class, new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")),
        "Ne&w/Maven Project");
    ui.wait(new ShellShowingCondition("New Maven Project"));
    ui.click(new ButtonLocator("Create a &simple project (skip archetype selection)"));
    ui.click(new ButtonLocator("&Next >"));
    ui.enterText("org.foo");
    ui.keyClick(WT.TAB);
    ui.enterText("test-pom");
    ui.click(new ButtonLocator("&Finish"));
    ui.wait(new ShellDisposedCondition("New Maven Project"));
    
    // open pom editor
    // ui.click(2, new TreeItemLocator("test-pom/pom.xml", new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")));
    
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFile file = root.getFile(new Path("test-pom/pom.xml"));
    
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
    
    ui.keyClick(SWT.CTRL, 'm');
  }
  
  protected void oneTimeTearDown() throws Exception {
    super.oneTimeTearDown();

    // ui.keyClick(SWT.CTRL | SWT.SHIFT, 's');  // save all to prevent "Save" confirmation dialog
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        getActivePage().closeAllEditors(false);
      }
    });
  }

  public void testUpdatingArtifactIdInXmlPropagatedToForm() throws Exception {
    ui.click(new CTabItemLocator(POM_XML_TAB));
    replaceText("test-pom", "test-pom1");

    ui.click(new CTabItemLocator(OVERVIEW_TAB));
    testTextValue("artifactId", "test-pom1");
  }

  public void testFormToXmlAndXmlToFormInParentArtifactId() throws Exception {
    // test FORM->XML and XML->FORM update of parentArtifactId
    ui.click(new CTabItemLocator(OVERVIEW_TAB));
    ui.click(new SWTWidgetLocator(Label.class, "Parent"));
    setTextValue("parentArtifactId", "parent2");
  
    ui.click(new CTabItemLocator(POM_XML_TAB));
    replaceText("parent2", "parent3");
    ui.click(new CTabItemLocator(OVERVIEW_TAB));
    testTextValue("parentArtifactId", "parent3");
  }

  public void testDeletingScmSectionInXmlPropagatedToForm() throws Exception {
    ui.click(new CTabItemLocator(OVERVIEW_TAB));
    ui.click(new SWTWidgetLocator(Label.class, "SCM"));
    setTextValue("scmUrl", "http://svn.sonatype.org/m2eclipse");

    ui.click(new CTabItemLocator(POM_XML_TAB));
    setSelection("<scm>", "</scm>");
    ui.keyClick(SWT.DEL);

    ui.click(new CTabItemLocator(OVERVIEW_TAB));
    testTextValue("scmUrl", "");
  }

  private void setSelection(String startMarker, String endMarker) throws WaitTimedOutException, WidgetSearchException {
    // read text directly instead of using "Find" dialog
    findText(startMarker);
    final int offset1 = getSelection().getOffset();
    Display.getDefault().syncExec(null);

    findText(endMarker);
    final int offset2 = getSelection().getOffset();
    
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        getSelectionProvider().setSelection(new TextSelection(offset1, offset2 - offset1 + 6));
      }
    });
  }

  private void testTextValue(String id, String value) {
    NamedWidgetLocator locator = new NamedWidgetLocator(id);
    ui.assertThat(new HasTextCondition(locator, value));
  }

  private void setTextValue(String id, String value) throws WidgetSearchException {
    NamedWidgetLocator locator = new NamedWidgetLocator(id);
    ui.setFocus(locator);
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
    IWorkbenchPage activePage = getActivePage();
    IEditorPart editor = activePage.getActiveEditor();
    IEditorSite editorSite = editor.getEditorSite();
    return editorSite.getSelectionProvider();
  }

  IWorkbenchPage getActivePage() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow window = workbench.getWorkbenchWindows()[0];
    return window.getActivePage();
  }
  
}
