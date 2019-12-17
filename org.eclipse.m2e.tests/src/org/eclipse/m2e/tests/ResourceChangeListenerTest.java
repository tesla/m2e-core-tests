/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class ResourceChangeListenerTest extends AbstractMavenProjectTestCase {

  IProject project;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    deleteProject("resourcechange");
    project = createProject("resourcechange", "projects/resourcechange/pom.xml");

    IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();
    ResolverConfiguration configuration = new ResolverConfiguration();
    configurationManager.enableMavenNature(project, configuration, monitor);
    configurationManager.updateProjectConfiguration(project, monitor);

    waitForJobsToComplete();
  }

  @After
  public void tearDown() throws Exception {
    try {
      deleteProject("resourcechange");
    } finally {
      super.tearDown();
    }
  }

  //TODO Does this test actually test anything?!
  @Test
  public void testMarkerOnlyChange() throws Exception {
    // modify
    IFile pom = project.getFile("pom.xml");
    pom.createMarker(IMavenConstants.MARKER_ID);
    waitForJobsToComplete();

    // ideally, I need to test that the container did not refresh
  }

  @Test
  public void testPomChanges() throws Exception {
    // modify
    copyContent(project, new File("projects/resourcechange/pom001.xml"), "pom.xml");

    // assert
    IClasspathEntry[] cp = getMavenContainerEntries(project);
    assertEquals(1, cp.length);
    assertEquals("junit-4.1.jar", cp[0].getPath().lastSegment());
  }

  @Test
  public void testPomMove001() throws Exception {
    // setup some more
    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        try {
          InputStream contents = new FileInputStream("projects/resourcechange/pom001.xml");
          IFile pom001 = project.getFile("pom001.xml");
          pom001.create(contents, true, monitor);
        } catch(Exception e) {
          throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, 0, e.getMessage(), e));
        }
      }
    }, null);
    waitForJobsToComplete();

    // modify
    copyContent(project, "pom001.xml", "pom.xml");

    // assert
    IClasspathEntry[] cp = getMavenContainerEntries(project);
    assertEquals(1, cp.length);
    assertEquals("junit-4.1.jar", cp[0].getPath().lastSegment());
  }

  @Test
  public void testPomMove002() throws Exception {
    // setup some more
    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        try {
          InputStream contents = new FileInputStream("projects/resourcechange/pom001.xml");
          IFile pom001 = project.getFile("pom001.xml");
          pom001.create(contents, true, monitor);
        } catch(Exception e) {
          throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, 0, e.getMessage(), e));
        }
      }
    }, null);
    waitForJobsToComplete();

    // modify
    copyContent(project, "pom001.xml", "pom.xml");

    // assert
    waitForJobsToComplete();
    IClasspathEntry[] cp = getMavenContainerEntries(project);
    assertEquals(1, cp.length);
    assertEquals("junit-4.1.jar", cp[0].getPath().lastSegment());
  }

  @Test
  public void testPomDelete() throws Exception {
    // just in case, make sure we imported the right project
    IClasspathEntry[] cp = getMavenContainerEntries(project);
    assertEquals(1, cp.length);
    assertEquals("junit-3.8.1.jar", cp[0].getPath().lastSegment());

    // change
    project.getFile("pom.xml").delete(true, null);
    refreshMavenProject(project);

    // assert
    waitForJobsToComplete();
    assertEquals(0, getMavenContainerEntries(project).length);
  }

  @Test
  public void testPomRename() throws Exception {
    // just in case, make sure we imported the right project
    IClasspathEntry[] cp = getMavenContainerEntries(project);
    assertEquals(1, cp.length);
    assertEquals("junit-3.8.1.jar", cp[0].getPath().lastSegment());

    // change
    project.getFile("pom.xml").move(project.getFullPath().append("backup"), true, null);
    refreshMavenProject(project);

    // assert
    waitForJobsToComplete();
    assertEquals(0, getMavenContainerEntries(project).length);
  }

  @Test
  public void testProjectDelete() throws Exception {
    IClasspathEntry[] cp = getMavenContainerEntries(project);
    assertEquals(1, cp.length);
    assertEquals("junit-3.8.1.jar", cp[0].getPath().lastSegment());
    waitForJobsToComplete();
    assertEquals(1, MavenPlugin.getMavenProjectRegistry().getProjects().length);

    project.delete(true, new NullProgressMonitor());

    waitForJobsToComplete();
    assertEquals(0, MavenPlugin.getMavenProjectRegistry().getProjects().length);
  }
}
