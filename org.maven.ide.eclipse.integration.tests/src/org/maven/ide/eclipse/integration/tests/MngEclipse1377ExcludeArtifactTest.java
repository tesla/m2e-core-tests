/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.maven.ide.eclipse.jdt.BuildPathManager;

import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;

/**
 * @author rseddon
 *
 */
public class MngEclipse1377ExcludeArtifactTest extends UIIntegrationTestCase {

 public void testEclipseArtifact() throws Exception {
   IUIContext ui = getUI();
   IProject project = createArchetypeProjct("maven-archetype-quickstart", "project");
   IJavaProject jp = (IJavaProject)project.getNature(JavaCore.NATURE_ID);
   IClasspathContainer maven2Container = BuildPathManager.getMaven2ClasspathContainer(jp);
   
   assertTrue(maven2Container.getClasspathEntries().length == 1);
   
   openPomFile("project/pom.xml");
   addDependency(project, "commons-collections", "commons-collections", "3.2.1");
  
   maven2Container = BuildPathManager.getMaven2ClasspathContainer(jp);
   assertTrue(maven2Container.getClasspathEntries().length == 2);
   IPath path = maven2Container.getClasspathEntries()[1].getPath();
   assertTrue(path.toString().endsWith("commons-collections-3.2.1.jar"));
   ui
       .click(new TreeItemLocator(
           "project/Maven Dependencies/commons-collections-3.2.1.jar.*",
           new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")));
   ui
       .contextClick(
           new TreeItemLocator(
               "project/Maven Dependencies/commons-collections-3.2.1.jar.*",
               new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")), "Maven/Exclude Maven artifact...");
   //ui.wait(new ShellDisposedCondition("Progress Information"));
   ui.wait(new ShellDisposedCondition("Progress Information"));
   ui.wait(new ShellShowingCondition("Exclude Maven Artifact"));
   ui.click(new ButtonLocator("OK"));
   ui.wait(new ShellDisposedCondition("Exclude Maven Artifact"));
   waitForAllBuildsToComplete();
   assertTrue(BuildPathManager.getMaven2ClasspathContainer(jp).getClasspathEntries().length == 1);
  }
}
