/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import static org.maven.ide.eclipse.integration.tests.UIIntegrationTestCase.PLUGIN_ID;

import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.SWT;
import org.maven.ide.eclipse.jdt.BuildPathManager;

import abbot.finder.swt.WidgetSearchException;

import com.windowtester.internal.runtime.locator.ContextMenuItemLocator;
import com.windowtester.runtime.WT;
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.locator.MenuItemLocator;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.FilteredTreeItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.TextLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;

/**
 * @author dyocum
 *
 */
public class MNGEclipse1687ArchetypeCreationTest extends UIIntegrationTestCase {

  private static final String GROUP_ID = "org.jboss.portletbridge.archetypes";
  private static final String VERSION_ID = "2.0.0.ALPHA";
  
  public void testArchetypeCreation() throws Exception {
    
    URL url = FileLocator.find(Platform.getBundle(PLUGIN_ID), new Path("/projects/seam-basic-2.0.0.ALPHA.jar"), null);
    
    createProjectFromArchetype("seam-basic", "", url.toString());
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("project");
    assertTrue(project.exists());
  }
  
  /**
   * The jboss archetype has a packaging type of maven-plugin, so its a good test to make sure we find 
   * an archetype and create it with the right packaging type. This was originally filed as MNG-Eclipse1687
   * @param artifactID
   * @param extraConfig
   * @param repoUrl
   * @throws Exception
   */
  public void createProjectFromArchetype(String artifactID, String extraConfig, String repoUrl) throws Exception {
    getUI().click(new MenuItemLocator("File/New/Project..."));
    getUI().wait(new ShellShowingCondition("New Project"));
    getUI().click(new FilteredTreeItemLocator("Maven/Maven Project"));
    getUI().click(new ButtonLocator("&Next >"));
    getUI().click(new ButtonLocator("&Next >"));
    getUI().click(new NamedWidgetLocator("addArchetypeButton"));

    getUI().wait(new ShellShowingCondition("Add Archetype"));

    replaceText(new NamedWidgetLocator("archetypeGroupId"), GROUP_ID);
    replaceText(new NamedWidgetLocator("archetypeArtifactId"), artifactID);
    replaceText(new NamedWidgetLocator("archetypeVersion"), VERSION_ID);
    replaceText(new NamedWidgetLocator("repository"), repoUrl);

    getUI().click(new ButtonLocator("OK"));
    getUI().wait(new ShellDisposedCondition("Add Archetype"));
    
    waitForAllBuildsToComplete();
    getUI().click(new ButtonLocator("&Next >"));

    replaceText(new NamedWidgetLocator("groupId"), "org.sonatype.test");
    replaceText(new NamedWidgetLocator("artifactId"), "project");
    replaceText(new NamedWidgetLocator("package"), "org.sonatype.test");
    
    getUI().click(new ButtonLocator("&Finish"));
    getUI().wait(new ShellDisposedCondition("New Maven Project"));

    waitForAllBuildsToComplete();
    
    getUI().click(new TreeItemLocator("project", new ViewLocator(PACKAGE_EXPLORER_VIEW_ID)));
    getUI().keyClick(SWT.F5);
    
    waitForAllBuildsToComplete();
    assertProjectsHaveNoErrors();
  }
}
