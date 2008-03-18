/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.tests;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.internal.project.FilterResourcesPathTranslator;

public class MavenBuilderTest extends AsbtractMavenProjectTestCase {

  public void test001_standardLayout() throws Exception {
    deleteProject("resourcefiltering-p001");
    IProject project = createExisting("resourcefiltering-p001", "projects/resourcefiltering/p001");
    waitForJobsToComplete();

    IFolder outputFolder = project.getFolder(runtimeManager.getDefaultOutputFolder());
    IPath resourcesPath = outputFolder.getFolder(FilterResourcesPathTranslator.RESOURCES_FOLDERNAME).getFullPath();
    IPath testResourcesPath = outputFolder.getFolder(FilterResourcesPathTranslator.TEST_RESOURCES_FOLDERNAME).getFullPath();

    IPath aPath = resourcesPath.append("a.properties");
    IPath bPath = testResourcesPath.append("b.properties");

    workspace.getRoot().getFile(aPath).delete(true, new NullProgressMonitor());
    workspace.getRoot().getFile(bPath).delete(true, new NullProgressMonitor());

    project.build(IncrementalProjectBuilder.CLEAN_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();

    Properties properties = loadProperties(aPath);
    assertEquals("Unnamed - resourcefiltering:p001:jar:0.0.1-SNAPSHOT", properties.getProperty("a.name"));
    assertEquals("0.0.1-SNAPSHOT", properties.getProperty("a.version"));

    properties = loadProperties(bPath);
    assertEquals("Unnamed - resourcefiltering:p001:jar:0.0.1-SNAPSHOT", properties.getProperty("b.name"));
    assertEquals("0.0.1-SNAPSHOT", properties.getProperty("b.version"));
  }

  private Properties loadProperties(IPath aPath) throws CoreException, IOException {
    Properties properties = new Properties();
    InputStream contents = workspace.getRoot().getFile(aPath).getContents();
    try {
      properties.load(contents);
    } finally {
      contents.close();
    }
    return properties;
  }

  public void test002_customResourceLocation() throws Exception {
    deleteProject("resourcefiltering-p002");
    IProject project = createExisting("resourcefiltering-p002", "projects/resourcefiltering/p002");
    waitForJobsToComplete();

    IFolder outputFolder = project.getFolder(runtimeManager.getDefaultOutputFolder());
    IPath resourcesPath = outputFolder.getFolder(FilterResourcesPathTranslator.RESOURCES_FOLDERNAME).getFullPath();
    IPath testResourcesPath = outputFolder.getFolder(FilterResourcesPathTranslator.TEST_RESOURCES_FOLDERNAME).getFullPath();

    IPath aPath = resourcesPath.append("a.properties");
    IPath bPath = testResourcesPath.append("b.properties");

    workspace.getRoot().getFile(aPath).delete(true, new NullProgressMonitor());
    workspace.getRoot().getFile(bPath).delete(true, new NullProgressMonitor());

    project.build(IncrementalProjectBuilder.CLEAN_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();

    Properties properties = loadProperties(aPath);
    assertEquals("Unnamed - resourcefiltering:p002:jar:0.0.1-SNAPSHOT", properties.getProperty("a.name"));
    assertEquals("0.0.1-SNAPSHOT", properties.getProperty("a.version"));

    properties = loadProperties(bPath);
    assertEquals("Unnamed - resourcefiltering:p002:jar:0.0.1-SNAPSHOT", properties.getProperty("b.name"));
    assertEquals("0.0.1-SNAPSHOT", properties.getProperty("b.version"));
  }

  public void test003_modules() throws Exception {
    deleteProject("resourcefiltering-p003");
    IProject project = createExisting("resourcefiltering-p003", "projects/resourcefiltering/p003");
    waitForJobsToComplete();

    IFolder outputFolder = project.getFolder(runtimeManager.getDefaultOutputFolder());
    IPath resourcesPath = outputFolder.getFolder(FilterResourcesPathTranslator.RESOURCES_FOLDERNAME).getFullPath();
    IPath testResourcesPath = outputFolder.getFolder(FilterResourcesPathTranslator.TEST_RESOURCES_FOLDERNAME).getFullPath();

    IPath aPath = resourcesPath.append("a.properties");
    IPath bPath = testResourcesPath.append("b.properties");

    workspace.getRoot().getFile(aPath).delete(true, new NullProgressMonitor());
    workspace.getRoot().getFile(bPath).delete(true, new NullProgressMonitor());

    project.build(IncrementalProjectBuilder.CLEAN_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();

    Properties properties = loadProperties(aPath);
    assertEquals("Unnamed - resourcefiltering:p003-m1:jar:0.0.1-SNAPSHOT", properties.getProperty("a.name"));
    assertEquals("0.0.1-SNAPSHOT", properties.getProperty("a.version"));

    properties = loadProperties(bPath);
    assertEquals("Unnamed - resourcefiltering:p003-m1:jar:0.0.1-SNAPSHOT", properties.getProperty("b.name"));
    assertEquals("0.0.1-SNAPSHOT", properties.getProperty("b.version"));
  }

  public void test004_useMavenOutputFolders() throws Exception {
    deleteProject("resourcefiltering-p004");
    IProject project = createExisting("resourcefiltering-p004", "projects/resourcefiltering/p004");
    waitForJobsToComplete();

    IFolder outputFolder = project.getFolder("target/classes"); // XXX
    IPath resourcesPath = outputFolder.getFullPath();

    IPath aPath = resourcesPath.append("a.properties");

    workspace.getRoot().getFile(aPath).delete(true, new NullProgressMonitor());

    project.build(IncrementalProjectBuilder.CLEAN_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();

    Properties properties = loadProperties(aPath);
    assertEquals("Unnamed - resourcefiltering:p004:jar:0.0.1-SNAPSHOT", properties.getProperty("a.name"));
    assertEquals("0.0.1-SNAPSHOT", properties.getProperty("a.version"));
  }

  public void test005_pomChanged() throws Exception {
    deleteProject("resourcefiltering-p005");
    IProject project = createExisting("resourcefiltering-p005", "projects/resourcefiltering/p005");
    waitForJobsToComplete();

    IFolder outputFolder = project.getFolder("target/classes"); // XXX
    IFile a = outputFolder.getFile("a.properties");

    assertEquals(false, a.isAccessible());

    copyContent(project, "pom_changed.xml", "pom.xml");
    waitForJobsToComplete();

    Properties properties = loadProperties(a.getFullPath());
    assertEquals("Unnamed - resourcefiltering:p005:jar:0.0.1-SNAPSHOT", properties.getProperty("a.name"));
    assertEquals("0.0.1-SNAPSHOT", properties.getProperty("a.version"));
  }

  public void test006_testPluginProperties() throws Exception {
    deleteProject("resourcefiltering-p006");
    IProject project = createExisting("resourcefiltering-p006", "projects/resourcefiltering/p006");
    waitForJobsToComplete();

    project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
    waitForJobsToComplete();

    IFolder outputFolder = project.getFolder("target-eclipse/classes"); // XXX
    IFile a = outputFolder.getFile("application.properties");
    Properties properties = loadProperties(a.getFullPath());
    assertEquals("1.0-SNAPSHOT.${timestamp}", properties.getProperty("buildVersion"));

    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(MavenPlugin.PLUGIN_ID);
    // MavenPreferenceConstants.P_GOAL_ON_RESOURCE_FILTER stupid classpath access restrictions!!!
    projectNode.put("eclipse.m2.goalOnResourceFilter", "process-resources resources:testResources");

    project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
    waitForJobsToComplete();

    properties = loadProperties(a.getFullPath());
    assertEquals("1.0-SNAPSHOT.123456789", properties.getProperty("buildVersion"));

  }
}
