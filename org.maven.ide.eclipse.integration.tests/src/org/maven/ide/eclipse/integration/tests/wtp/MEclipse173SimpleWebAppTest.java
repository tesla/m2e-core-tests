/*******************************************************************************
 * Copyright (c) 2009 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests.wtp;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.maven.ide.eclipse.integration.tests.M2EUIIntegrationTestCase;
import org.maven.ide.eclipse.integration.tests.common.ContextMenuHelper;
import org.maven.ide.eclipse.integration.tests.common.SwtbotUtil;


public class MEclipse173SimpleWebAppTest extends M2EUIIntegrationTestCase {

  private static final String DEPLOYED_URL = "http://localhost:8080/simple-webapp/weather.x?zip=94038";

  @Test
  public void testSimpleWebApp() throws Exception {
    installTomcat6();

    // Import the test project
    doImport("projects/ch07project.zip");

    // Add the hsqldb.jar to the dependencies so it can be found at runtime
    IProject simpleWebAppProject = ResourcesPlugin.getWorkspace().getRoot().getProject("simple-webapp");
    SWTBotEclipseEditor editor = bot.editorByTitle(openFile(simpleWebAppProject, "pom.xml").getTitle()).toTextEditor();
    editor.bot().cTabItem("pom.xml").activate();

    replaceText("</dependencies>","<dependency>" + //
        "<groupId>hsqldb</groupId>" + //
        "<artifactId>hsqldb</artifactId>" + //
        "<version>1.8.0.7</version>" + //
        "</dependency>" +//
        "</dependencies>");

    editor.saveAndClose();
    waitForAllBuildsToComplete();

    // Generate the database using maven goal hibernate3:hbm2ddl
    ContextMenuHelper.clickContextMenu(selectProject("simple-webapp"), "Run As", "8 Maven build...");
    String shellName = isEclipseVersion(3, 3) ? "simple-webapp" : "Edit Configuration";
    SWTBotShell shell = bot.shell(shellName);
    try {
      bot.textWithName("goalsText").setText("org.codehaus.mojo:hibernate3-maven-plugin:2.1:hbm2ddl");
      bot.checkBoxWithName("enableWorkspaceResolution").select();
      bot.button("Run").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }

    waitForAllBuildsToComplete();
    ContextMenuHelper.clickContextMenu(selectProject("simple-webapp"), "Refresh");
    waitForAllBuildsToComplete();

    assertProjectsHaveNoErrors();

    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for(IProject project : projects) {
      if("Servers".equals(project.getName())) {
        continue;
      }
      Assert.assertTrue("project:" + project.getName() + " should have maven nature", project
          .hasNature("org.maven.ide.eclipse.maven2Nature"));
    }

    // Put the correct dababase URL in applicationContext-persist.xml
    IFolder data = simpleWebAppProject.getFolder("data");

    IProject simplePersistProject = ResourcesPlugin.getWorkspace().getRoot().getProject("simple-persist");
    editor = bot.editorByTitle(
        openFile(simplePersistProject, "src/main/resources/applicationContext-persist.xml").getTitle()).toTextEditor();
    editor.bot().cTabItem("Source").activate();

    editor.setFocus();
    replaceText("data/weather", data.getLocation().toFile().getAbsolutePath() + "/weather");

    editor.saveAndClose();

    waitForAllBuildsToComplete();
    deployProjectsIntoTomcat();

    // Verify deployment worked (attempt to get weather forcast for Moss Beach CA)
    String s = retrieveWebPage(DEPLOYED_URL);
//    System.out.println(DEPLOYED_URL);
//    Thread.sleep(20000);
    int dex = s.indexOf("Moss Beach");
//    System.out.println("page: "+s);
//    System.out.println("==================================");
//    System.out.println("!!!!!!!!!!!!!!!!index is: "+dex);
    Assert.assertTrue("Couldn't find Moss Beach weather in web page", (dex >= 0));

  }

  @After
  public void tearDown() throws Exception {
    shutdownServer();
  }

}
