/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.ui.ide.IDE;

import com.windowtester.runtime.WT;
import com.windowtester.runtime.swt.condition.SWTIdleCondition;
import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;

/**
 * @author Rich Seddon
 */
public class ResolveDependenciesTest extends UIIntegrationTestCase {

  public void testResolveDependencies() throws Exception {
    importZippedProject("projects/resolve_deps_test.zip");
    assertTrue(ResourcesPlugin.getWorkspace().getRoot().getProject("project").exists());

    
    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("project");
    
    final IFile f = project.getFile("src/main/java/org/sonatype/test/project/App.java");
    assertTrue(f.exists());
    executeOnEventQueue(new Task() {

      public Object runEx() throws Exception {
        IDE.openEditor(getActivePage(), f);
        return null;
      }});
    
    ui.wait(new JobsCompleteCondition(), 60000);
    
    // there should be compile errors
    int severity = project.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
    assertEquals(IMarker.SEVERITY_ERROR, severity);

    //launch quick fix for SessionFactory dependency
    ui.keyClick(SWT.MOD1, 'f');
    ui.wait(new ShellShowingCondition("Find/Replace"));

    ui.enterText("SessionF");
    ui.keyClick(WT.CR);
    ui.click(new ButtonLocator("Close"));
    ui.wait(new ShellDisposedCondition("Find/Replace"));

    ui.keyClick(SWT.MOD1, '1');
    ui.wait(new ShellShowingCondition(""));
    ui.keyClick(SWT.END);
    ui.keyClick(SWT.ARROW_UP);

    ui.keyClick(SWT.CR);
    ui.wait(new ShellShowingCondition("Search in Maven repositories"));
    ui.wait(new SWTIdleCondition());

    ui.click(new TreeItemLocator("SessionFactory   org.hibernate   hibernate   hibernate", new NamedWidgetLocator(
        "searchResultTree")));
    ui.click(new TreeItemLocator(
        "SessionFactory   org.hibernate   hibernate   hibernate/3.0.5 - hibernate-3.0.5.jar .*",
        new NamedWidgetLocator("searchResultTree")));
    ui.click(new ButtonLocator("OK"));

    ui.wait(new ShellDisposedCondition("Search in Maven repositories"));

    Thread.sleep(5000);
    ui.wait(new JobsCompleteCondition());

    // Organize imports to resolve other missing dependencies
    ui.keyClick(SWT.MOD1 | SWT.SHIFT, 'o');
    ui.wait(new ShellShowingCondition("Organize Imports"));
    ui.enterText("org.hibernate.cfg.Configuration");
    ui.click(new ButtonLocator("&Finish"));
    ui.wait(new ShellDisposedCondition("Organize Imports"));
    ui.keyClick(SWT.MOD1, 's');
    ui.wait(new JobsCompleteCondition());
    ui.wait(new SWTIdleCondition());
    severity = project.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);

    // All errors fixed.
    assertTrue(IMarker.SEVERITY_ERROR > severity);

  }

}
