/*******************************************************************************
 * Copyright (c) 2009 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests.wtp;

import java.io.File;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.SWT;
import org.maven.ide.eclipse.integration.tests.UIIntegrationTestCase;

import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.MenuItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;


public class MEclipse173SimpleWebAppTest extends UIIntegrationTestCase {

  private static final String DEPLOYED_URL = "http://localhost:8080/simple-webapp/weather.x?zip=94038";

  private File tempDir;

  public MEclipse173SimpleWebAppTest(){
    super();
    this.setUseExternalMaven(true);
  }
  public void testSimpleWebApp() throws Exception {
    setXmlPrefs();
    installTomcat6();

    // Import the test project
    tempDir = doImport("projects/ch07project.zip");

    // Add the hsqldb.jar to the dependencies so it can be found at runtime
    IProject simpleWebAppProject = ResourcesPlugin.getWorkspace().getRoot().getProject("simple-webapp");
    openFile(simpleWebAppProject, "pom.xml");
    getUI().click(new CTabItemLocator("pom.xml"));
    getUI().wait(new JobsCompleteCondition(), 120000);
    findText("</dependencies");
    getUI().keyClick(SWT.ARROW_LEFT);
    getUI().enterText("<dependency><groupId>hsqldb</<artifactId>hsqldb</<version>1.8.0.7</</");
    getUI().keyClick(SWT.MOD1, 's');
    Thread.sleep(5000);
    getUI().wait(new JobsCompleteCondition(), 120000);

    // Generate the database using maven goal hibernate3:hbm2ddl
    getUI().click(new TreeItemLocator("simple-webapp", new ViewLocator(PACKAGE_EXPLORER_VIEW_ID)));
    getUI().click(new MenuItemLocator("Run/Run As/.*Maven build..."));
    String shellName = isEclipseVersion(3,3) ? "simple-webapp" : "Edit Configuration";
    getUI().wait(new ShellShowingCondition(shellName));
    getUI().click(new NamedWidgetLocator("goalsText"));
    getUI().enterText("org.codehaus.mojo:hibernate3-maven-plugin:2.1:hbm2ddl");
    getUI().click(new NamedWidgetLocator("enableWorkspaceResolution"));
    getUI().click(new ButtonLocator("&Run"));
    getUI().wait(new ShellDisposedCondition(shellName));
    getUI().wait(new JobsCompleteCondition(), 60000);
    getUI().click(new TreeItemLocator("simple-webapp", new ViewLocator(PACKAGE_EXPLORER_VIEW_ID)));
    getUI().keyClick(SWT.F5);
    getUI().wait(new JobsCompleteCondition());

    assertProjectsHaveNoErrors();

    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for(IProject project : projects) {
      if("Servers".equals(project.getName())) {
        continue;
      }
      assertTrue("project:" + project.getName() + " should have maven nature", project
          .hasNature("org.maven.ide.eclipse.maven2Nature"));
    }

    // Put the correct dababase URL in applicationContext-persist.xml
    IFolder data = simpleWebAppProject.getFolder("data");

    IProject simplePersistProject = ResourcesPlugin.getWorkspace().getRoot().getProject("simple-persist");
    openFile(simplePersistProject, "src/main/resources/applicationContext-persist.xml");

    getUI().click(new CTabItemLocator("Source"));
    getUI().keyClick(SWT.MOD1, 'f');
    getUI().wait(new ShellShowingCondition("Find/Replace"));
    getUI().keyClick(SWT.MOD1, 'a');
    getUI().enterText("data/weather");
    getUI().keyClick(SWT.TAB);
    getUI().enterText(data.getLocation().toFile().getAbsolutePath() + "/weather");
    getUI().click(new ButtonLocator("Replace &All"));
    getUI().click(new ButtonLocator("Close"));
    getUI().wait(new ShellDisposedCondition("Find/Replace"));
    getUI().keyClick(SWT.MOD1, 's');

    getUI().wait(new JobsCompleteCondition(), 120000);

    deployProjectsIntoTomcat();

    // Verify deployment worked (attempt to get weather forcast for Moss Beach CA)
    String s = retrieveWebPage(DEPLOYED_URL);
//    System.out.println(DEPLOYED_URL);
//    Thread.sleep(20000);
    int dex = s.indexOf("Moss Beach");
//    System.out.println("page: "+s);
//    System.out.println("==================================");
//    System.out.println("!!!!!!!!!!!!!!!!index is: "+dex);
    assertTrue("Couldn't find Moss Beach weather in web page", (dex >=0));

  }

  protected void tearDown() throws Exception {
   
    try {
      shutdownTomcat();
    } catch(Exception ex) {
      ex.printStackTrace();
    }

    super.tearDown();
    
    if(tempDir != null && tempDir.exists()) {
      deleteDirectory(tempDir);
      tempDir = null;
    }

  }

}
