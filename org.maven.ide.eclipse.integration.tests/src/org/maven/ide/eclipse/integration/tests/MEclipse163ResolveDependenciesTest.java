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

import com.windowtester.runtime.swt.condition.SWTIdleCondition;
import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;


/**
 * @author Rich Seddon
 */
public class MEclipse163ResolveDependenciesTest extends UIIntegrationTestCase {

  public void testResolveDependencies() throws Exception {
    importZippedProject("projects/resolve_deps_test.zip");
    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("project");
    assertTrue(project.exists());

    openFile(project, "src/main/java/org/sonatype/test/project/App.java");

    // there should be compile errors
    int severity = project.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
    assertEquals(IMarker.SEVERITY_ERROR, severity);

    //Workaround for Window tester bug, close & reopen tab to prevent editor from being in invalid state.
    getUI().close(new CTabItemLocator("App.java"));
    openFile(project, "src/main/java/org/sonatype/test/project/App.java");

    //launch quick fix for SessionFactory dependency
    getUI().click(new TreeItemLocator("project.*", new ViewLocator(PACKAGE_EXPLORER_VIEW_ID)));
    getUI().keyClick(SWT.MOD1 | SWT.SHIFT, 't');
    getUI().wait(new ShellShowingCondition("Open Type"));
    getUI().enterText("app");
    getUI().wait(new SWTIdleCondition());
    getUI().click(new ButtonLocator("OK"));
    getUI().wait(new ShellDisposedCondition("Open Type"));
    getUI().wait(new JobsCompleteCondition(), 60000);

    getUI().keyClick(SWT.MOD1, '.'); // next annotation

    getUI().keyClick(SWT.MOD1, '1');
    getUI().wait(new ShellShowingCondition(""));
    getUI().keyClick(SWT.END);
    getUI().keyClick(SWT.ARROW_UP);

    getUI().keyClick(SWT.CR);
    getUI().wait(new ShellShowingCondition("Search in Maven repositories"));
    getUI().wait(new SWTIdleCondition());

    getUI().click(new TreeItemLocator("JFreeChart   org.jfree.chart   com.google.gwt   gwt-benchmark-viewer",
        new NamedWidgetLocator("searchResultTree")));
    getUI().click(new TreeItemLocator(
            "JFreeChart   org.jfree.chart   jfree   jfreechart/1.0.7 - jfreechart-1.0.7.jar .*",
            new NamedWidgetLocator("searchResultTree")));

    getUI().wait(new SWTIdleCondition());
    getUI().keyClick(SWT.CR);

    getUI().wait(new ShellDisposedCondition("Search in Maven repositories"));

    waitForAllBuildsToComplete();

    assertProjectsHaveNoErrors();
    
  }

}
