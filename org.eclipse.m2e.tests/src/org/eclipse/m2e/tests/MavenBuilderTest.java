/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

public class MavenBuilderTest extends AbstractMavenProjectTestCase {

  public void test001_standardLayout() throws Exception {
    deleteProject("resourcefiltering-p001");
    IProject project = createExisting("resourcefiltering-p001", "projects/resourcefiltering/p001");
    waitForJobsToComplete();

    IPath resourcesPath = project.getFolder("target/classes").getFullPath();
    IPath testResourcesPath = project.getFolder("target/test-classes").getFullPath();

    IPath aPath = resourcesPath.append("a.properties");
    IPath bPath = testResourcesPath.append("b.properties");

    workspace.getRoot().getFile(aPath).delete(true, new NullProgressMonitor());
    workspace.getRoot().getFile(bPath).delete(true, new NullProgressMonitor());

    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();

    Properties properties = loadProperties(aPath);
    assertEquals("p001", properties.getProperty("a.name"));
    assertEquals("0.0.1-SNAPSHOT", properties.getProperty("a.version"));

    properties = loadProperties(bPath);
    assertEquals("p001", properties.getProperty("b.name"));
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

    IPath resourcesPath = project.getFolder("target/classes").getFullPath();
    IPath testResourcesPath = project.getFolder("target/test-classes").getFullPath();

    IPath aPath = resourcesPath.append("a.properties");
    IPath bPath = testResourcesPath.append("b.properties");

    workspace.getRoot().getFile(aPath).delete(true, new NullProgressMonitor());
    workspace.getRoot().getFile(bPath).delete(true, new NullProgressMonitor());

    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();

    Properties properties = loadProperties(aPath);
    assertEquals("p002", properties.getProperty("a.name"));
    assertEquals("0.0.1-SNAPSHOT", properties.getProperty("a.version"));

    properties = loadProperties(bPath);
    assertEquals("p002", properties.getProperty("b.name"));
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

    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();

    Properties properties = loadProperties(aPath);
    assertEquals("p004", properties.getProperty("a.name"));
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

    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

    Properties properties = loadProperties(a.getFullPath());
    assertEquals("p005", properties.getProperty("a.name"));
    assertEquals("0.0.1-SNAPSHOT", properties.getProperty("a.version"));
  }

  public void _test006_testPluginProperties() throws Exception {
    deleteProject("resourcefiltering-p006");
    IProject project = createExisting("resourcefiltering-p006", "projects/resourcefiltering/p006");
    waitForJobsToComplete();

    project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
    waitForJobsToComplete();

    IFolder outputFolder = project.getFolder("target/classes"); // XXX
    IFile a = outputFolder.getFile("application.properties");
    Properties properties = loadProperties(a.getFullPath());
    assertEquals("1.0-SNAPSHOT.${timestamp}", properties.getProperty("buildVersion"));

    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(IMavenConstants.PLUGIN_ID);
    // MavenPreferenceConstants.P_GOAL_ON_RESOURCE_FILTER stupid classpath access restrictions!!!
    projectNode.put("eclipse.m2.goalOnResourceFilter", "process-resources resources:testResources");

    project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
    waitForJobsToComplete();

    properties = loadProperties(a.getFullPath());
    assertEquals("1.0-SNAPSHOT.123456789", properties.getProperty("buildVersion"));

  }

  public void test007_fullBuild() throws Exception {
    deleteProject("resourcefiltering-p007");
    IProject project = createExisting("resourcefiltering-p007", "projects/resourcefiltering/p007");
    waitForJobsToComplete();

    project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
    waitForJobsToComplete();

    IFolder outputFolder = project.getFolder("target/classes");
    assertNull(outputFolder.findMember("META-INF/plexus/components.xml"));

    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    IResource componentsXML = outputFolder.findMember("META-INF/plexus/components.xml");
    assertTrue(componentsXML.isAccessible());
  }

  // the test is disabled due to http://jira.codehaus.org/browse/MNGECLIPSE-839
  public void _test008_classpathChange() throws Exception {
    deleteProject("resourcefiltering-p008");
    IProject project = createExisting("resourcefiltering-p008", "projects/resourcefiltering/p008");
    waitForJobsToComplete();

    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();

    IFile aFile = project.getFile("target/classes/a.properties");
    assertTrue(aFile.isAccessible());

    copyContent(project, "pom-changed.xml", "pom.xml"); // waits for jobs to complete
    project.build(IncrementalProjectBuilder.AUTO_BUILD, monitor);
    assertTrue(aFile.isAccessible());
  }
  
  public void test010_properties() throws Exception {
    IProject project = createExisting("resourcefiltering-p010", "projects/resourcefiltering/p010");
    waitForJobsToComplete();
    
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    
    IFile aFile = project.getFile("target/test-classes/b.properties");
    assertTrue(aFile.exists());
    assertTrue(aFile.isAccessible());
  }
  
}
