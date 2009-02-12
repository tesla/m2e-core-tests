/*******************************************************************************
 * Copyright (c) 2009 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.SWT;

import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.MenuItemLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;


public class MEclipse173SimpleWebAppTest extends UIIntegrationTestCase {

  private static final String SERVER_NAME = "Tomcat.*";

  private static final String DEPLOYED_URL = "http://localhost:8080/simple-webapp/weather.x?zip=94038";

  private File tempDir;

  public void testSimpleWebApp() throws Exception {

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
    getUI().click(new TreeItemLocator("simple-webapp", new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")));
    getUI().click(new MenuItemLocator("Run/Run As/.*Maven build..."));
    getUI().wait(new ShellShowingCondition("Edit Configuration"));
    getUI().enterText("hibernate3:hbm2ddl");
    getUI().click(new ButtonLocator("&Run"));
    getUI().wait(new ShellDisposedCondition("Edit Configuration"));
    getUI().wait(new JobsCompleteCondition(), 60000);
    getUI().click(new TreeItemLocator("simple-webapp", new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")));
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

    // Deploy the test project into tomcat
    getUI().click(new CTabItemLocator("Servers"));
    getUI().contextClick(new TreeItemLocator(SERVER_NAME, new ViewLocator("org.eclipse.wst.server.ui.ServersView")),
        "Add and Remove Projects...");
    getUI().wait(new ShellShowingCondition("Add and Remove Projects"));
    getUI().click(new ButtonLocator("Add A&ll >>"));
    getUI().click(new ButtonLocator("&Finish"));
    getUI().wait(new ShellDisposedCondition("Add and Remove Projects"));

    Thread.sleep(3000);

    // Start the server
    getUI().click(new TreeItemLocator(SERVER_NAME, new ViewLocator("org.eclipse.wst.server.ui.ServersView")));
    getUI().keyClick(SWT.MOD1 | SWT.ALT, 'r');
    getUI().wait(new JobsCompleteCondition(), 120000);
    Thread.sleep(5000);

    // Verify deployment worked (attempt to get weather forcast for Moss Beach CA)

    URL url = new URL(DEPLOYED_URL);
    URLConnection conn = url.openConnection();
    conn.setDoInput(true);
    conn.connect();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IOUtil.copy(conn.getInputStream(), out);
    conn.getInputStream().close();

    String s = new String(out.toByteArray(), "UTF-8");

    assertTrue("Couldn't find Moss Beach weather in web page", s.indexOf("Moss Beach") > 0);

  }

  protected void tearDown() throws Exception {
   
    try {
      // Stop the server
      getUI().click(new CTabItemLocator("Servers"));
      getUI().click(new TreeItemLocator(SERVER_NAME, new ViewLocator("org.eclipse.wst.server.ui.ServersView")));
      getUI().keyClick(SWT.MOD1 | SWT.ALT, 's');
      getUI().wait(new JobsCompleteCondition(), 120000);
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
