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
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.junit.Assert;
import org.junit.Test;
import org.maven.ide.eclipse.jdt.BuildPathManager;


/**
 * @author rseddono
 */
public class MNGEclipse1296UpdateDependenciesTest extends M2EUIIntegrationTestCase {

  @Test
  public void testUpdateDependencies() throws Exception {
    doImport("projects/update_deps.zip");

    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("sample");
    IJavaProject jp = (IJavaProject) project.getNature(JavaCore.NATURE_ID);

    Assert.assertTrue(project.exists());

    // Sample project has no maven2 dependencies initially
    Assert.assertTrue(BuildPathManager.getMaven2ClasspathContainer(jp).getClasspathEntries().length == 0);

    // Add a dependency.
    SWTBotEditor editor = bot.editorByTitle(openFile(project, "pom.xml").getTitle());
    editor.bot().cTabItem("pom.xml").activate();

    String dep = "<dependency>" + //
        "<groupId>commons-lang</groupId>" + //
        "<artifactId>commons-lang</artifactId>" + //
        "<version>2.3</version>" + //
        "<scope>provided</scope>" + //
        "</dependency>" + //
        "</dependencies>";
    replaceText("</dependencies>", dep);
    editor.save();

    waitForAllBuildsToComplete();
    assertProjectsHaveNoErrors();

    // Assert that project now has corresponding dependency in it's maven2 container.
    IClasspathContainer maven2Container = BuildPathManager.getMaven2ClasspathContainer(jp);
    Assert.assertEquals(1, maven2Container.getClasspathEntries().length);
    IPath path = maven2Container.getClasspathEntries()[0].getPath();
    Assert.assertTrue(path.toString().endsWith("commons-lang-2.3.jar"));

    // Remove dependency.
    replaceText(dep, "</dependencies>");
    editor.save();

    waitForAllBuildsToComplete();
    assertProjectsHaveNoErrors();

    // Assert there are no longer any maven2 dependencies
    Assert.assertEquals(0, BuildPathManager.getMaven2ClasspathContainer(jp).getClasspathEntries().length);
  }
}
