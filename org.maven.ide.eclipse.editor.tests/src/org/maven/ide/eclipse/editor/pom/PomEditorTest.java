/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WT;
import com.windowtester.runtime.condition.HasTextCondition;
import com.windowtester.runtime.locator.WidgetReference;
import com.windowtester.runtime.swt.UITestCaseSWT;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.MenuItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.SWTWidgetLocator;
import com.windowtester.runtime.swt.locator.TableItemLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;

/**
 * @author Eugene Kuleshov
 */
public class PomEditorTest extends UITestCaseSWT {

  private IUIContext ui;

  protected void setUp() throws Exception {
    super.setUp();
    
    ui = getUI();
    
    ui.close(new CTabItemLocator("Welcome"));

    ui.click(new MenuItemLocator("Window/Open Perspective/Other..."));
    ui.wait(new ShellShowingCondition("Open Perspective"));
    ui.click(2, new TableItemLocator("Java( \\(default\\))?"));
    ui.wait(new ShellDisposedCondition("Open Perspective"));
  }
  
  public void testSamplePomEditor2() throws Exception {
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

    ui.click(2, new TreeItemLocator("test-pom/pom.xml", new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")));

    // ui.click(new MenuItemLocator("Window/Navigation/Maximize Active View or Editor"));
    ui.keyClick(SWT.CTRL, 'm');
    
    ui.click(new CTabItemLocator("pom.xml"));
    
    // ui.click(new MenuItemLocator("Edit/Find\\\\/Replace\\.\\.\\.(.*)"));
    ui.keyClick(SWT.CTRL, 'f');
    ui.wait(new ShellShowingCondition("Find/Replace"));

    ui.enterText("test-pom");
    ui.keyClick(WT.TAB);
    ui.enterText("test-pom1");
    ui.keyClick(SWT.ALT, 'a');  // "replace all"
    ui.close(new SWTWidgetLocator(Shell.class, "Find/Replace"));
    ui.wait(new ShellDisposedCondition("Find/Replace"));
    
    ui.click(new CTabItemLocator("Overview"));

    NamedWidgetLocator artifactIdLocator = new NamedWidgetLocator("artifactId");
    Text artifactIdText = (Text) ((WidgetReference) ui.find(artifactIdLocator)).getWidget();
    assertNotNull("Can't find artifactIdText", artifactIdText);
    ui.assertThat(new HasTextCondition(artifactIdLocator, "test-pom1"));
  }

}

