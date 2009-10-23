/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.SWT;
import org.maven.ide.eclipse.jdt.BuildPathManager;

import abbot.finder.swt.WidgetSearchException;

import com.windowtester.internal.runtime.locator.ContextMenuItemLocator;
import com.windowtester.runtime.WT;
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.locator.MenuItemLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.TextLocator;

/**
 * @author dyocum
 *
 */
public class MNGEclipse1674PluginCodeCompletionTest extends UIIntegrationTestCase {

  
  public void testSchemaCodeAssist() throws Exception {
    
    doImport("projects/cc_sample2.zip", false);
    
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("cc_sample2");
    IJavaProject jp = (IJavaProject)project.getNature(JavaCore.NATURE_ID);
    assertTrue(project.exists());
    
    // Add a dependency.
    openFile(project, "pom.xml");
    waitForAllBuildsToComplete();
    getUI().click(new CTabItemLocator("pom.xml"));
    
    findTextWithWrap("<project>", true);
    
    getUI().keyClick(WT.CTRL, '1');
    getUI().keyClick(WT.CR);
    waitForAllBuildsToComplete();
    assertProjectsHaveNoErrors();
  }
  public void testPluginCodeCompletion() throws Exception {
    
    doImport("projects/cc_sample.zip");
    
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("cc_sample");
    IJavaProject jp = (IJavaProject)project.getNature(JavaCore.NATURE_ID);
    
    assertTrue(project.exists());

    // Sample project has no maven2 dependencies initially
    assertTrue(BuildPathManager.getMaven2ClasspathContainer(jp).getClasspathEntries().length == 0);
    
    // Add a dependency.
    openFile(project, "pom.xml");
    waitForAllBuildsToComplete();
    getUI().click(new CTabItemLocator("pom.xml"));
    findText("</plugins");
    
    getUI().keyClick(SWT.ARROW_LEFT);
    getUI().enterText("<plugin></");
    
    findTextWithWrap("</plugin", true);
    findTextWithWrap("</plugin", false); 
    getUI().keyClick(SWT.ARROW_LEFT);
    //control space
    getUI().keyClick(WT.CTRL, '\u0020');
    getUI().keyClick(WT.ARROW_DOWN);
    getUI().keyClick(WT.CR);
    getUI().keyClick(SWT.MOD1, 's');
    
    assertTrue(searchForText("configuration", true));

  }
}
