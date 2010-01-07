/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import org.eclipse.swt.SWT;

import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WaitTimedOutException;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.FilteredTreeItemLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;


/**
 * MNGECLIPSE-1964
 * <P/>
 * Creating new maven module while parent's pom.xml is open in editor results in a "file changed on disk" message.
 * <P/>
 * M2E 0.9.9.200912221003<BR/>
 * 1. Create a project with packaging of "pom"<BR/>
 * 2. Open up the pom.xml file, and go to the "pom.xml"<BR/>
 * 3. Create a new module, with the first project as it's parent.<BR/>
 * You get a message saying the file has changed on disk.
 * <P/>
 * This should not be happening.
 */
public class MngEclipse1964PomChangeTest extends M2EUIIntegrationTestCase {

  public void testPomChange() throws Exception {
    IUIContext ui = getUI();

    // new project
    ui.keyClick(SWT.CTRL, 'N');
    ui.wait(new ShellShowingCondition("New"));
    // maven project
    ui.click(new FilteredTreeItemLocator("Maven/Maven Project"));
    ui.click(new ButtonLocator("Next >"));
    // parent project
    ui.click(new ButtonLocator("Create a simple project (skip archetype selection)"));
    ui.click(new ButtonLocator("Next >"));
    ui.enterText("group");
    ui.keyClick(SWT.TAB);
    ui.enterText("parent");
    ui.keyClick(SWT.TAB);
    ui.keyClick(SWT.TAB);
    ui.enterText("pom");
    ui.click(new ButtonLocator("Finish"));
    waitForAllBuildsToComplete();

    // open pom
    ui.click(new TreeItemLocator("parent/pom.xml", new ViewLocator(PACKAGE_EXPLORER_VIEW_ID)));
    ui.keyClick(SWT.F3);
    ui.click(new CTabItemLocator("pom.xml"));

    // new project
    ui.keyClick(SWT.CTRL, 'N');
    ui.wait(new ShellShowingCondition("New"));
    // maven module
    ui.click(new FilteredTreeItemLocator("Maven/Maven Module"));
    ui.click(new ButtonLocator("Next >"));
    // child project
    ui.click(new ButtonLocator("Create a simple project (skip archetype selection)"));
    ui.keyClick(SWT.TAB);
    ui.enterText("child");
    ui.keyClick(SWT.TAB);
    ui.keyClick(SWT.TAB);
    ui.keyClick(SWT.CR);
    ui.wait(new ShellShowingCondition("Select a Maven project"));
    ui.click(new TreeItemLocator("parent"));
    ui.click(new ButtonLocator("OK"));
    ui.click(new ButtonLocator("Finish"));
    waitForAllBuildsToComplete();

    try {
      ui.wait(new ShellShowingCondition("File Changed"), 1000);
      fail();
    } catch(WaitTimedOutException e) {
      //no dialog should be displayed
    }
  }
}
