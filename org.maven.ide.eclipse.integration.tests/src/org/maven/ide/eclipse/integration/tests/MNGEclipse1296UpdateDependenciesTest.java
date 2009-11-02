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
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.SWT;
import org.maven.ide.eclipse.jdt.BuildPathManager;

import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;
import com.windowtester.runtime.swt.locator.CTabItemLocator;

/**
 * @author rseddon
 *
 */
public class MNGEclipse1296UpdateDependenciesTest extends UIIntegrationTestCase {

  
  public void testUpdateDependencies() throws Exception {
    setXmlPrefs();
    
    doImport("projects/update_deps.zip");
    
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("sample");
    IJavaProject jp = (IJavaProject)project.getNature(JavaCore.NATURE_ID);
    
    assertTrue(project.exists());

    // Sample project has no maven2 dependencies initially
    assertTrue(BuildPathManager.getMaven2ClasspathContainer(jp).getClasspathEntries().length == 0);
    
    // Add a dependency.
    openFile(project, "pom.xml");
    waitForAllBuildsToComplete();
    getUI().click(new CTabItemLocator("pom.xml"));
    findText("</dependencies");
    getUI().keyClick(SWT.ARROW_LEFT);
    getUI().enterText(
        "<dependency><groupId>commons-lang</<artifactId>commons-lang</<version>2.3</<scope>provided</</");
    getUI().keyClick(SWT.MOD1, 's');
    
    waitForAllBuildsToComplete();
    assertProjectsHaveNoErrors();
    
    // Assert that project now has corresponding dependency in it's maven2 container.
    IClasspathContainer maven2Container = BuildPathManager.getMaven2ClasspathContainer(jp);
    IPath path = maven2Container.getClasspathEntries()[0].getPath();
    assertTrue(path.toString().endsWith("commons-lang-2.3.jar"));
    
    // Remove dependency.
    getUI().click(new CTabItemLocator("pom.xml"));
    getUI().keyClick(SWT.MOD1, 'z');
    getUI().keyClick(SWT.MOD1, 's');
    
    waitForAllBuildsToComplete();
    assertProjectsHaveNoErrors();
    
    // Assert there are no longer any maven2 dependencies
    assertTrue(BuildPathManager.getMaven2ClasspathContainer(jp).getClasspathEntries().length == 0);
  }
}
