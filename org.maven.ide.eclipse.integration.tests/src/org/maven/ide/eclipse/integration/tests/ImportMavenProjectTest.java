/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import java.io.File;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.SWT;

import com.windowtester.runtime.swt.condition.SWTIdleCondition;
import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.FilteredTreeItemLocator;
import com.windowtester.runtime.swt.locator.MenuItemLocator;


/**
 * @author Rich Seddon
 */
public class ImportMavenProjectTest extends UIIntegrationTestCase {

  private File tempDir;

  public void testSimpleModuleImport() throws Exception {
    doImport("projects/commons-collections-3.2.1-src.zip");
  }

  public void testMultiModuleImport() throws Exception {
    doImport("projects/httpcomponents-core-4.0-beta3-src.zip");
  }

  private void doImport(String projectPath) throws Exception {
    tempDir = unzipProject(projectPath);

    ui.click(new MenuItemLocator("File/Import..."));
    ui.wait(new ShellShowingCondition("Import"));
    ui.click(new FilteredTreeItemLocator("General/Maven Projects"));
    ui.click(new ButtonLocator("&Next >"));
    ui.wait(new SWTIdleCondition());
    ui.enterText(tempDir.getCanonicalPath());
    ui.keyClick(SWT.CR);
    ui.click(new ButtonLocator("&Finish"));
    ui.wait(new ShellDisposedCondition("Checkout as Maven project from SCM"));
    ui.wait(new JobsCompleteCondition(), 300000);

    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for(IProject project : projects) {
      int severity = project.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
      assertTrue(IMarker.SEVERITY_ERROR > severity);
      assertTrue(project.hasNature(JavaCore.NATURE_ID));
      assertTrue(project.hasNature("org.maven.ide.eclipse.maven2Nature"));
    }

  }

  protected void tearDown() throws Exception {
    clearProjects();
    
    if(tempDir != null && tempDir.exists()) {
      deleteDirectory(tempDir);
      tempDir = null;
    }
    super.tearDown();

  
  }

}
