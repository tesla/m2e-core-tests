/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.SWT;

import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.MenuItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;


/**
 * @author Administrator
 */
public class MEclipse182RangeHandlingTest extends UIIntegrationTestCase {

  public void testRangeHandling() throws Exception {
    importZippedProject("projects/versionRange.zip");

    IUIContext ui = getUI();

    //Install version 1.0-SNAPSHOT of project2
    ui.click(new TreeItemLocator("project2", new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")));
    ui.click(new MenuItemLocator("Run/Run As/Maven install"));
    ui.wait(new JobsCompleteCondition(), 240000);
    assertProjectsHaveNoErrors();

    // Change version of project2 to 1.1-SNAPSHOT
    openPomFile("project2/pom.xml");
    ui.click(new CTabItemLocator("project2/pom.xml"));
    replaceText(new NamedWidgetLocator("version"), "1.1-SNAPSHOT");
    ui.keyClick(SWT.MOD1, 's');
    Thread.sleep(5000);
    ui.wait(new JobsCompleteCondition(), 240000);

    // Change method signature referenced by original project
    IProject project2 = ResourcesPlugin.getWorkspace().getRoot().getProject("project2");
    openFile(project2, "src/main/java/org/sonatype/test/project2/Simple.java");
    ui.click(new CTabItemLocator("Simple.java"));
    replaceText("add(", "add2(");
    ui.keyClick(SWT.MOD1, 's');
    Thread.sleep(5000);
    ui.wait(new JobsCompleteCondition(), 240000);

    // There should be no compile errors, project2:1.1-SNAPSHOT should come from local repository.
    assertProjectsHaveNoErrors();

    // Change original project to depend on version range which includes 1.1-SNAPSHOT
    openPomFile("project/pom.xml");
    ui.click(new CTabItemLocator("project/pom.xml"));
    ui.click(new CTabItemLocator("pom.xml"));
    replaceText("1.0-SNAPSHOT", "[1.0-SNAPSHOT,2.0-SNAPSHOT)");

    ui.keyClick(SWT.MOD1, 's');
    waitForAllBuildsToComplete();

    // Original project should now be using workspace project, this should cause a compile error.
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("project");
    int problemSeverity = project.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);

    assertEquals("project should have compile errors", IMarker.SEVERITY_ERROR, problemSeverity);

  }
}
