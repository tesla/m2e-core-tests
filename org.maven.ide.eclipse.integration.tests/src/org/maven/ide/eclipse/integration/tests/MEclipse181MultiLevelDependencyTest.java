/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.widgets.Tree;

import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.LabeledLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;
import com.windowtester.runtime.util.ScreenCapture;


/**
 * @author Administrator
 */
public class MEclipse181MultiLevelDependencyTest extends UIIntegrationTestCase {

  private IProject createDependentProject(IProject parent, String childName) throws Exception {
    IProject childProject = createArchetypeProject("maven-archetype-quickstart", childName);

    waitForAllBuildsToComplete();
    getUI().click(new TreeItemLocator(parent.getName(), new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")));
    getUI().contextClick(new TreeItemLocator(parent.getName(), new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")),
        "Maven/Add Dependency");
    getUI().wait(new ShellShowingCondition("Add Dependency"));
    getUI().enterText(childName);
    getUI().click(
        new TreeItemLocator("org.sonatype.test   " + childName, new LabeledLocator(Tree.class, "&Search Results:")));
    ScreenCapture.createScreenCapture();
    getUI().click(
        new TreeItemLocator("org.sonatype.test   " + childName + "/0.0.1-SNAPSHOT.*", new LabeledLocator(Tree.class,
            "&Search Results:")));
    getUI().click(new ButtonLocator("OK"));

    waitForAllBuildsToComplete();
    IJavaProject jp = (IJavaProject) parent.getNature(JavaCore.NATURE_ID);
    assertTrue("classpath dependency for "+ childName + " not found", childName.equals(jp.getRequiredProjectNames()[0]));
    return childProject;
  }
  
  public void testMultiLevelDependencies() throws Exception {
    IProject project = createArchetypeProject("maven-archetype-quickstart", "multiProject0");
    //dropped it to three to speed things up
    for (int i = 1; i < 4; i++) {
      project = createDependentProject(project, "multiProject" + i);
    }
  }
}
