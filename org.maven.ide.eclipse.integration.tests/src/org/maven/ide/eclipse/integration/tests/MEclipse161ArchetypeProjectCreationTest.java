/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.widgets.Composite;

import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.swt.condition.SWTIdleCondition;
import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.FilteredTreeItemLocator;
import com.windowtester.runtime.swt.locator.MenuItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.SWTWidgetLocator;
import com.windowtester.runtime.swt.locator.TableCellLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;


/**
 * @author rseddon
 */
public class MEclipse161ArchetypeProjectCreationTest extends UIIntegrationTestCase {

  private IProject createArchetypeProjct(String archetypeName) throws Exception {
    return createArchetypeProjct(archetypeName, "project");
  }

  public void testQuickStartCreate() throws Exception {
    IProject project = createArchetypeProjct("maven-archetype-quickstart");
    assertTrue(project.hasNature(JavaCore.NATURE_ID));
    IFile f = project.getFile("src/main/java/org/sonatype/test/project/App.java");
    assertTrue(f.exists());
    f = project.getFile("pom.xml");
    assertTrue(f.exists());
    f = project.getFile("src/test/java/org/sonatype/test/project/AppTest.java");
    assertTrue(f.exists());

  }

  public void testCreateMojo() throws Exception {
    createArchetypeProjct("maven-archetype-mojo");
  }

  public void testCreatePortlet() throws Exception {
    createArchetypeProjct("maven-archetype-portlet");
  }

  public void testCreateProfiles() throws Exception {
    createArchetypeProjct("maven-archetype-profiles");
  }

  public void testCreateSite() throws Exception {
    createArchetypeProjct("maven-archetype-site");
  }

  public void testCreateSiteSimple() throws Exception {
    createArchetypeProjct("maven-archetype-site-simple");
  }

  public void testCreateSiteWebapp() throws Exception {
    createArchetypeProjct("maven-archetype-webapp");
  }
  
  public void testCreateStrutsStarter() throws Exception {
    createArchetypeProjct("struts2-archetype-starter");
  }
  
  public void testCreateSpringWS() throws Exception {
    createArchetypeProjct("spring-ws-archetype");
  }

  public void testCreateJ2EESimple() throws Exception {
    if (isEclipseVersion(3, 3)) {
     // maven-archetype-j2ee-simple produces an empty ear deployment descriptor, which makes Eclipse 3.3 WTP blow up
      return;
    }
    String archetypeName = "maven-archetype-j2ee-simple";
    String projectName = "project";
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    assertFalse(project.exists());

    IUIContext ui = getUI();
    ui.click(new SWTWidgetLocator(ViewForm.class, new SWTWidgetLocator(CTabFolder.class, 0, new SWTWidgetLocator(
        Composite.class))));
    ui.click(new MenuItemLocator("File/New/Project..."));
    ui.wait(new ShellShowingCondition("New Project"));
    ui.click(new FilteredTreeItemLocator("Plug-in Project"));
    ui.click(new FilteredTreeItemLocator("Maven/Maven Project"));
    ui.click(new ButtonLocator("&Next >"));
    ui.click(new ButtonLocator("&Next >"));
    ui.click(new TableCellLocator(archetypeName, 2));
    // NamedWidgetLocator table = new NamedWidgetLocator("archetypesTable");

    ui.click(new ButtonLocator("&Next >"));
    ui.wait(new SWTIdleCondition());
    IWidgetLocator groupCombo = ui.find(new NamedWidgetLocator("groupId"));
    ui.setFocus(groupCombo);
    ui.enterText("org.sonatype.test");
    ui.setFocus(ui.find(new NamedWidgetLocator("artifactId")));
    ui.enterText(projectName);
    ui.click(new ButtonLocator("&Finish"));
    ui.wait(new ShellDisposedCondition("New Maven Project"));

    waitForAllBuildsToComplete();

    project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    assertTrue(project.exists());

    // Bump compliance level to 1.5
    openFile(project, "pom.xml");
    waitForAllBuildsToComplete();
    getUI().click(new CTabItemLocator("pom.xml"));
    findText("</plugins");
    getUI().keyClick(SWT.ARROW_LEFT);
    getUI().enterText(
        "<plugin><artifactId>maven-compiler-plugin</<version>2.0.2</<configuration><source>1.5</<target>1.5</</</");
    getUI().keyClick(SWT.MOD1, 's');
    Thread.sleep(5000);
    getUI().wait(new JobsCompleteCondition(), 120000);
    getUI().click(new TreeItemLocator("servlet*", new ViewLocator(PACKAGE_EXPLORER_VIEW_ID)));
    getUI().contextClick(new TreeItemLocator("servlet", new ViewLocator(PACKAGE_EXPLORER_VIEW_ID)),
        "Maven/Update Project Configuration");
    
    waitForAllBuildsToComplete();
    
    getUI().click(new TreeItemLocator("ear*", new ViewLocator(PACKAGE_EXPLORER_VIEW_ID)));
    getUI().contextClick(new TreeItemLocator("ear", new ViewLocator(PACKAGE_EXPLORER_VIEW_ID)),
        "Maven/Update Project Configuration");
    
    waitForAllBuildsToComplete();
    
    assertProjectsHaveNoErrors();
    assertTrue("archtype project \"" + archetypeName + "\" created without Maven nature", project
        .hasNature("org.maven.ide.eclipse.maven2Nature"));

    ui.click(new TreeItemLocator(projectName + ".*", new ViewLocator(PACKAGE_EXPLORER_VIEW_ID)));

  }
}
