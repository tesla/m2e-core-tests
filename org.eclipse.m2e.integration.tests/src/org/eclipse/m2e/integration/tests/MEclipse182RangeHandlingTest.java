/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.integration.tests;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withMnemonic;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.m2e.integration.tests.common.ContextMenuHelper;
import org.eclipse.m2e.integration.tests.common.matchers.ContainsMnemonic;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Administrator
 */
public class MEclipse182RangeHandlingTest extends M2EUIIntegrationTestCase {

  @SuppressWarnings("unchecked")
  @Test
  public void testRangeHandling() throws Exception {
    String project1Name = "versionProject1";
    String project2Name = "versionProject2";
    importZippedProject("projects/versionRange.zip");

    //Install version 1.0-SNAPSHOT of project2
    ContextMenuHelper.clickContextMenu(selectProject(project2Name),  withMnemonic("Run As"), ContainsMnemonic.containsMnemonic("Maven install"));

    waitForAllLaunchesToComplete(60 * 1000);
    waitForAllBuildsToComplete();
    assertProjectsHaveNoErrors();

    // Change version of project2 to 1.1-SNAPSHOT
    SWTBotEditor editor = bot.editorByTitle(openPomFile(project2Name + "/pom.xml").getTitle());
    editor.bot().textWithLabel("Version:").setText("1.1-SNAPSHOT");
    editor.saveAndClose();

    waitForAllBuildsToComplete();

    // Change method signature referenced by original project
    IProject project2 = ResourcesPlugin.getWorkspace().getRoot().getProject(project2Name);
    editor = bot.editorByTitle(openFile(project2, "src/main/java/org/sonatype/test/versionProject2/Simple.java")
        .getTitle());
    replaceText("add(", "add2(");
    editor.saveAndClose();
    
    // There should be no compile errors, project2:1.0-SNAPSHOT should come from local repository.
    waitForAllBuildsToComplete();
    assertProjectsHaveNoErrors();

    // Change original project to depend on version range which includes 1.1-SNAPSHOT
    editor = bot.editorByTitle(openPomFile(project1Name + "/pom.xml").getTitle());
    editor.bot().cTabItem("pom.xml").activate();
    replaceText("1.0-SNAPSHOT", "[1.0-SNAPSHOT,1.1-SNAPSHOT]");
    editor.saveAndClose();

    waitForAllBuildsToComplete();

    // Original project should now be using workspace project, this should cause a compile error.
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(project1Name);
    int problemSeverity = project.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
    
    bot.viewByTitle("Problems").show();

    Assert.assertEquals("project should have compile errors: " + takeScreenShot("compile"), IMarker.SEVERITY_ERROR, problemSeverity);

  }
}
