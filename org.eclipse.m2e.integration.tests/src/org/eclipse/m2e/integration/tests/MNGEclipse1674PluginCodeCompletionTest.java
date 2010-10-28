/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.integration.tests;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.m2e.jdt.BuildPathManager;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author dyocum
 */
public class MNGEclipse1674PluginCodeCompletionTest extends M2EUIIntegrationTestCase {

  @Test
  public void testSchemaCodeAssist() throws Exception {
    doImport(PLUGIN_ID, "projects/cc_sample2.zip", false);

    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("cc_sample2");
    project.getNature(JavaCore.NATURE_ID);
    Assert.assertTrue(project.exists());

    // Add a dependency.
    SWTBotEclipseEditor editor = bot.editorByTitle(openFile(project, "pom.xml").getTitle()).toTextEditor();
    waitForAllBuildsToComplete();
    bot.cTabItem("pom.xml").activate();

    Assert.assertTrue(editor.getText().contains("<project>"));

    findTextWithWrap("<project>", true);

    editor.pressShortcut(SWT.CTRL, '1');
    editor.pressShortcut(KeyStroke.getInstance(SWT.LF));
    editor.saveAndClose();

    waitForAllBuildsToComplete();
    assertProjectsHaveNoErrors();

    Assert.assertFalse(editor.getText(), editor.getText().contains("<project>"));
  }

  @Test
  public void testPluginCodeCompletion() throws Exception {

    doImport("projects/cc_sample.zip");

    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("cc_sample");
    IJavaProject jp = (IJavaProject) project.getNature(JavaCore.NATURE_ID);

    Assert.assertTrue(project.exists());

    // Sample project has no maven2 dependencies initially
    Assert.assertTrue(BuildPathManager.getMaven2ClasspathContainer(jp).getClasspathEntries().length == 0);

    // Add a dependency.
    SWTBotEclipseEditor editor = bot.editorByTitle(openFile(project, "pom.xml").getTitle()).toTextEditor();
    waitForAllBuildsToComplete();
    bot.cTabItem("pom.xml").activate();

    findText("</plugins>");
//    editor.pressShortcut(KeyStroke.getInstance(SWT.ARROW_LEFT));
//    
//    editor.insertText("<plugin></plugin>");
//
//    findText("</plugin>");
    editor.pressShortcut(KeyStroke.getInstance(SWT.ARROW_LEFT));
    editor.pressShortcut(SWT.CTRL, ' ');
    editor.pressShortcut(KeyStroke.getInstance(SWT.ARROW_DOWN));
    editor.pressShortcut(KeyStroke.getInstance(SWT.LF));

    editor.saveAndClose();
  }

}
