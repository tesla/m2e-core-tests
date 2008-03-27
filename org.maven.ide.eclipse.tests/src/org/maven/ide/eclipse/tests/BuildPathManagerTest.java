/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.tests;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.model.Model;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.index.IndexInfo;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.project.BuildPathManager;
import org.maven.ide.eclipse.project.MavenUpdateRequest;
import org.maven.ide.eclipse.project.ResolverConfiguration;


/**
 * @author Eugene Kuleshov
 */
public class BuildPathManagerTest extends AsbtractMavenProjectTestCase {

  public void testEnableMavenNature() throws Exception {
    deleteProject("MNGECLIPSE-248parent");
    deleteProject("MNGECLIPSE-248child");

    final IProject project1 = createProject("MNGECLIPSE-248parent", "projects/MNGECLIPSE-248parent/pom.xml");
    final IProject project2 = createProject("MNGECLIPSE-248child", "projects/MNGECLIPSE-248child/pom.xml");

    NullProgressMonitor monitor = new NullProgressMonitor();
    BuildPathManager buildpathManager = MavenPlugin.getDefault().getBuildpathManager();

    ResolverConfiguration configuration = new ResolverConfiguration();
    buildpathManager.enableMavenNature(project1, configuration, monitor);
//    buildpathManager.updateSourceFolders(project1, monitor);

    buildpathManager.enableMavenNature(project2, configuration, monitor);
//    buildpathManager.updateSourceFolders(project2, monitor);

//    waitForJob("Initializing " + project1.getProject().getName());
//    waitForJob("Initializing " + project2.getProject().getName());
    project1.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    project2.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();

    IMarker[] markers1 = project1.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertTrue("Unexpected markers " + Arrays.asList(markers1), markers1.length==0);
    
    IClasspathEntry[] project1entries = getMavenContainerEntries(project1);
    assertEquals(1, project1entries.length);
    assertEquals(IClasspathEntry.CPE_LIBRARY, project1entries[0].getEntryKind());
    assertEquals("junit-4.1.jar", project1entries[0].getPath().lastSegment());

    IClasspathEntry[] project2entries = getMavenContainerEntries(project2);
    assertEquals(2, project2entries.length);
    assertEquals(IClasspathEntry.CPE_PROJECT, project2entries[0].getEntryKind());
    assertEquals("MNGECLIPSE-248parent", project2entries[0].getPath().segment(0));
    assertEquals(IClasspathEntry.CPE_LIBRARY, project2entries[1].getEntryKind());
    assertEquals("junit-4.1.jar", project2entries[1].getPath().lastSegment());
  }

  public void testEnableMavenNatureWithNoWorkspace() throws Exception {
    deleteProject("MNGECLIPSE-248parent");
    deleteProject("MNGECLIPSE-248child");

    final IProject project1 = createProject("MNGECLIPSE-248parent", "projects/MNGECLIPSE-248parent/pom.xml");
    final IProject project2 = createProject("MNGECLIPSE-248child", "projects/MNGECLIPSE-248child/pom.xml");

    NullProgressMonitor monitor = new NullProgressMonitor();
    BuildPathManager buildpathManager = MavenPlugin.getDefault().getBuildpathManager();

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(false);
    configuration.setResolveWorkspaceProjects(false);
    configuration.setFilterResources(false);
    configuration.setUseMavenOutputFolders(false);
    configuration.setActiveProfiles("");
    
    buildpathManager.enableMavenNature(project1, configuration, monitor);
    buildpathManager.enableMavenNature(project2, configuration, monitor);
//    buildpathManager.updateSourceFolders(project1, monitor);
//    buildpathManager.updateSourceFolders(project2, monitor);

//    waitForJob("Initializing " + project1.getProject().getName());
//    waitForJob("Initializing " + project2.getProject().getName());
    waitForJobsToComplete();

    IClasspathEntry[] project1entries = getMavenContainerEntries(project1);
    assertEquals(1, project1entries.length);
    assertEquals(IClasspathEntry.CPE_LIBRARY, project1entries[0].getEntryKind());
    assertTrue(project1entries[0].getPath().lastSegment().equals("junit-4.1.jar"));

    IClasspathEntry[] project2entries = getMavenContainerEntries(project2);
    assertEquals(1, project2entries.length);
    assertEquals(IClasspathEntry.CPE_LIBRARY, project2entries[0].getEntryKind());
    assertEquals("MNGECLIPSE-248parent-1.0.0.jar", project2entries[0].getPath().lastSegment());
  }

  public void testProjectImportDefault() throws Exception {
    deleteProject("MNGECLIPSE-20");
    deleteProject("MNGECLIPSE-20-app");
    deleteProject("MNGECLIPSE-20-ear");
    deleteProject("MNGECLIPSE-20-ejb");
    deleteProject("MNGECLIPSE-20-type");
    deleteProject("MNGECLIPSE-20-web");

    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject project1 = importProject("projects/MNGECLIPSE-20/pom.xml", configuration);
    IProject project2 = importProject("projects/MNGECLIPSE-20/type/pom.xml", configuration);
    IProject project3 = importProject("projects/MNGECLIPSE-20/app/pom.xml", configuration);
    IProject project4 = importProject("projects/MNGECLIPSE-20/web/pom.xml", configuration);
    IProject project5 = importProject("projects/MNGECLIPSE-20/ejb/pom.xml", configuration);
    IProject project6 = importProject("projects/MNGECLIPSE-20/ear/pom.xml", configuration);

    waitForJobsToComplete();
    
    {
      IJavaProject javaProject = JavaCore.create(project1);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(0, classpathEntries.length);

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(2, rawClasspath.length);
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[0].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[1].getPath().toString());

      IMarker[] markers = project1.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project2);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(0, classpathEntries.length);

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(3, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-type/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[2].getPath().toString());

      IMarker[] markers = project2.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project3);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(3, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-type", classpathEntries[0].getPath().lastSegment());
      assertEquals("log4j-1.2.13.jar", classpathEntries[1].getPath().lastSegment());
      assertEquals("junit-3.8.1.jar", classpathEntries[2].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(3, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-app/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[2].getPath().toString());

      IMarker[] markers = project3.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project4);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(3, classpathEntries.length);
      assertEquals(Arrays.asList(classpathEntries).toString(), "MNGECLIPSE-20-app", classpathEntries[0].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type", classpathEntries[1].getPath().lastSegment());
      assertEquals("log4j-1.2.13.jar", classpathEntries[2].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(3, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-web/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[2].getPath().toString());

      IMarker[] markers = project4.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project5);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(3, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-app", classpathEntries[0].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type", classpathEntries[1].getPath().lastSegment());
      assertEquals("log4j-1.2.13.jar", classpathEntries[2].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(4, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-ejb/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("/MNGECLIPSE-20-ejb/src/main/resources", rawClasspath[1].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[2].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[3].getPath().toString());

      IMarker[] markers = project5.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project6);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(4, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-ejb", classpathEntries[0].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-app", classpathEntries[1].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type", classpathEntries[2].getPath().lastSegment());
      assertEquals("log4j-1.2.13.jar", classpathEntries[3].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(2, rawClasspath.length);
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[0].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[1].getPath().toString());

      IMarker[] markers = project6.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }
  }

  public void testProjectImportNoWorkspaceResolution() throws Exception {
    deleteProject("MNGECLIPSE-20");
    deleteProject("MNGECLIPSE-20-app");
    deleteProject("MNGECLIPSE-20-ear");
    deleteProject("MNGECLIPSE-20-ejb");
    deleteProject("MNGECLIPSE-20-type");
    deleteProject("MNGECLIPSE-20-web");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(false);
    configuration.setResolveWorkspaceProjects(false);
    configuration.setFilterResources(false);
    configuration.setUseMavenOutputFolders(false);
    configuration.setActiveProfiles("");
    
    IProject project1 = importProject("projects/MNGECLIPSE-20/pom.xml", configuration);
    IProject project2 = importProject("projects/MNGECLIPSE-20/type/pom.xml", configuration);
    IProject project3 = importProject("projects/MNGECLIPSE-20/app/pom.xml", configuration);
    IProject project4 = importProject("projects/MNGECLIPSE-20/web/pom.xml", configuration);
    IProject project5 = importProject("projects/MNGECLIPSE-20/ejb/pom.xml", configuration);
    IProject project6 = importProject("projects/MNGECLIPSE-20/ear/pom.xml", configuration);

    waitForJobsToComplete();
    
    {
      IJavaProject javaProject = JavaCore.create(project1);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(0, classpathEntries.length);

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(2, rawClasspath.length);
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[0].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER/noworkspace", rawClasspath[1].getPath().toString());

      IMarker[] markers = project1.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project2);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(0, classpathEntries.length);

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(3, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-type/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER/noworkspace", rawClasspath[2].getPath().toString());

      IMarker[] markers = project2.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project3);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(3, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-type-0.0.1-SNAPSHOT.jar", classpathEntries[0].getPath().lastSegment());
      assertEquals("log4j-1.2.13.jar", classpathEntries[1].getPath().lastSegment());
      assertEquals("junit-3.8.1.jar", classpathEntries[2].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(3, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-app/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER/noworkspace", rawClasspath[2].getPath().toString());

      IMarker[] markers = project3.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 3, markers.length);
    }

    {
      project4.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
      IJavaProject javaProject = JavaCore.create(project4);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(2, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-app-0.0.1-SNAPSHOT.jar", classpathEntries[0].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type-0.0.1-SNAPSHOT.jar", classpathEntries[1].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(3, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-web/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER/noworkspace", rawClasspath[2].getPath().toString());

      IMarker[] markers = project4.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 4, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project5);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(2, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-app-0.0.1-SNAPSHOT.jar", classpathEntries[0].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type-0.0.1-SNAPSHOT.jar", classpathEntries[1].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(4, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-ejb/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("/MNGECLIPSE-20-ejb/src/main/resources", rawClasspath[1].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[2].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER/noworkspace", rawClasspath[3].getPath().toString());

      IMarker[] markers = project5.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 4, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(project6);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(1, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-ejb-0.0.1-SNAPSHOT.jar", classpathEntries[0].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(2, rawClasspath.length);
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[0].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER/noworkspace", rawClasspath[1].getPath().toString());

      IMarker[] markers = project6.findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 4, markers.length);
    }
  }

  public void testProjectImportWithModules() throws Exception {
    deleteProject("MNGECLIPSE-20");
    deleteProject("MNGECLIPSE-20-app");
    deleteProject("MNGECLIPSE-20-ear");
    deleteProject("MNGECLIPSE-20-ejb");
    deleteProject("MNGECLIPSE-20-type");
    deleteProject("MNGECLIPSE-20-web");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(true);
    configuration.setResolveWorkspaceProjects(true);
    configuration.setFilterResources(false);
    configuration.setUseMavenOutputFolders(false);
    configuration.setActiveProfiles("");
    
    IProject project = importProject("projects/MNGECLIPSE-20/pom.xml", configuration);

    waitForJobsToComplete();
    
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals(2, classpathEntries.length);
    assertEquals("log4j-1.2.13.jar", classpathEntries[0].getPath().lastSegment());
    assertEquals("junit-3.8.1.jar", classpathEntries[1].getPath().lastSegment());

    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    assertEquals(7, rawClasspath.length);
    assertEquals("/MNGECLIPSE-20/type/src/main/java", rawClasspath[0].getPath().toString());
    assertEquals("/MNGECLIPSE-20/app/src/main/java", rawClasspath[1].getPath().toString());
    assertEquals("/MNGECLIPSE-20/web/src/main/java", rawClasspath[2].getPath().toString());
    assertEquals("/MNGECLIPSE-20/ejb/src/main/java", rawClasspath[3].getPath().toString());
    assertEquals("/MNGECLIPSE-20/ejb/src/main/resources", rawClasspath[4].getPath().toString());
    assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[5].getPath().toString());
    assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER/modules", rawClasspath[6].getPath().toString());

    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(toString(markers), 0, markers.length);
  }

  public void testProjectImportWithModulesNoWorkspaceResolution() throws Exception {
    deleteProject("MNGECLIPSE-20");
    deleteProject("MNGECLIPSE-20-app");
    deleteProject("MNGECLIPSE-20-ear");
    deleteProject("MNGECLIPSE-20-ejb");
    deleteProject("MNGECLIPSE-20-type");
    deleteProject("MNGECLIPSE-20-web");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(true);
    configuration.setResolveWorkspaceProjects(false);
    configuration.setFilterResources(false);
    configuration.setUseMavenOutputFolders(false);
    configuration.setActiveProfiles("");

    IProject project = importProject("projects/MNGECLIPSE-20/pom.xml", configuration);

    waitForJobsToComplete();
    
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals("" + Arrays.asList(classpathEntries), 2, classpathEntries.length);
    assertEquals("log4j-1.2.13.jar", classpathEntries[0].getPath().lastSegment());
    assertEquals("junit-3.8.1.jar", classpathEntries[1].getPath().lastSegment());

    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    assertEquals(7, rawClasspath.length);
    assertEquals("/MNGECLIPSE-20/type/src/main/java", rawClasspath[0].getPath().toString());
    assertEquals("/MNGECLIPSE-20/app/src/main/java", rawClasspath[1].getPath().toString());
    assertEquals("/MNGECLIPSE-20/web/src/main/java", rawClasspath[2].getPath().toString());
    assertEquals("/MNGECLIPSE-20/ejb/src/main/java", rawClasspath[3].getPath().toString());
    assertEquals("/MNGECLIPSE-20/ejb/src/main/resources", rawClasspath[4].getPath().toString());
    assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[5].getPath().toString());
    assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER/modules/noworkspace", rawClasspath[6].getPath()
        .toString());

    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(toString(markers), 0, markers.length);
  }

  public void testUpdateClasspathContainerWithModulesNoWorkspace() throws Exception {
    deleteProject("MNGECLIPSE-20");
    deleteProject("MNGECLIPSE-20-app");
    deleteProject("MNGECLIPSE-20-ear");
    deleteProject("MNGECLIPSE-20-ejb");
    deleteProject("MNGECLIPSE-20-type");
    deleteProject("MNGECLIPSE-20-web");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(true);
    configuration.setResolveWorkspaceProjects(false);
    configuration.setFilterResources(false);
    configuration.setUseMavenOutputFolders(false);
    configuration.setActiveProfiles("");

    IProject project = importProject("projects/MNGECLIPSE-20/pom.xml", configuration);

    waitForJobsToComplete();
    
//    BuildPathManager buildpathManager = MavenPlugin.getDefault().getBuildpathManager();
//    buildpathManager.updateClasspathContainer(project, new NullProgressMonitor());

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals("" + Arrays.asList(classpathEntries), 2, classpathEntries.length);
    assertEquals("log4j-1.2.13.jar", classpathEntries[0].getPath().lastSegment());
    assertEquals("junit-3.8.1.jar", classpathEntries[1].getPath().lastSegment());

    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(toString(markers), 0, markers.length);
  }

  public void testProjectImportWithProfile1() throws Exception {
    deleteProject("MNGECLIPSE-353");
    
    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(false);
    configuration.setResolveWorkspaceProjects(true);
    configuration.setFilterResources(false);
    configuration.setUseMavenOutputFolders(false);
    configuration.setActiveProfiles("jaxb1");

    IProject project = importProject("projects/MNGECLIPSE-353/pom.xml", configuration);

    waitForJobsToComplete();
    
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals("" + Arrays.asList(classpathEntries), 2, classpathEntries.length);
    assertEquals("junit-3.8.1.jar", classpathEntries[0].getPath().lastSegment());
    assertEquals("jaxb-api-1.5.jar", classpathEntries[1].getPath().lastSegment());

    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(toString(markers), 0, markers.length);
  }

  public void testProjectImportWithProfile2() throws Exception {
    deleteProject("MNGECLIPSE-353");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(false);
    configuration.setResolveWorkspaceProjects(true);
    configuration.setFilterResources(false);
    configuration.setUseMavenOutputFolders(false);
    configuration.setActiveProfiles("jaxb20");

    IProject project = importProject("projects/MNGECLIPSE-353/pom.xml", configuration);

    waitForJobsToComplete();
    
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals("" + Arrays.asList(classpathEntries), 4, classpathEntries.length);
    assertEquals("junit-3.8.1.jar", classpathEntries[0].getPath().lastSegment());
    assertEquals("jaxb-api-2.0.jar", classpathEntries[1].getPath().lastSegment());
    assertEquals("jsr173_api-1.0.jar", classpathEntries[2].getPath().lastSegment());
    assertEquals("activation-1.1.jar", classpathEntries[3].getPath().lastSegment());
    
    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(toString(markers), 0, markers.length);
  }

  public void testProjectImport001_separateFolders() throws Exception {
    deleteProject("projectimport-p001");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setUseMavenOutputFolders(false);
    IProject project = importProject("projectimport-p001", "projects/projectimport/p001", configuration);

    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    
    assertEquals(new Path("/projectimport-p001/target-eclipse/classes"), javaProject.getOutputLocation());
    IClasspathEntry[] cp = javaProject.getRawClasspath();

    assertEquals(4, cp.length);
    assertEquals(new Path("/projectimport-p001/src/main/java"), cp[0].getPath());
    assertEquals(new Path("/projectimport-p001/target-eclipse/classes"), cp[0].getOutputLocation());
    assertEquals(0, getAttributeCount(cp[0], MavenPlugin.TYPE_ATTRIBUTE));
    assertEquals(new Path("/projectimport-p001/src/test/java"), cp[1].getPath());
    assertEquals(new Path("/projectimport-p001/target-eclipse/test-classes"), cp[1].getOutputLocation());
    assertEquals(1, getAttributeCount(cp[1], MavenPlugin.TYPE_ATTRIBUTE));
  }

  public void testProjectImport001_useMavenOutputFolders() throws Exception {
    deleteProject("projectimport-p001");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setUseMavenOutputFolders(true);
    IProject project = importProject("projectimport-p001", "projects/projectimport/p001", configuration);

    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    
    assertEquals(new Path("/projectimport-p001/target/classes"), javaProject.getOutputLocation());
    IClasspathEntry[] cp = javaProject.getRawClasspath();

    assertEquals(4, cp.length);
    assertEquals(new Path("/projectimport-p001/src/main/java"), cp[0].getPath());
    assertEquals(new Path("/projectimport-p001/target/classes"), cp[0].getOutputLocation());
    assertEquals(0, getAttributeCount(cp[0], MavenPlugin.TYPE_ATTRIBUTE));
    assertEquals(new Path("/projectimport-p001/src/test/java"), cp[1].getPath());
    assertEquals(new Path("/projectimport-p001/target/test-classes"), cp[1].getOutputLocation());
    assertEquals(1, getAttributeCount(cp[1], MavenPlugin.TYPE_ATTRIBUTE));
  }

  public void testProjectImport002_separateFolders() throws Exception {
    deleteProject("projectimport-p002");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(true);
    configuration.setUseMavenOutputFolders(false);
    IProject project = importProject("projectimport-p002", "projects/projectimport/p002", configuration);

    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);

    assertEquals(new Path("/projectimport-p002/target-eclipse/classes"), javaProject.getOutputLocation());
    IClasspathEntry[] cp = javaProject.getRawClasspath();

    assertEquals(3, cp.length);
    assertEquals(new Path("/projectimport-p002/p002-m1/src/main/java"), cp[0].getPath());
    assertEquals(new Path("/projectimport-p002/target-eclipse/classes"), cp[0].getOutputLocation());
  }

  public void testProjectImport002_useMavenOutputFolders() throws Exception {
    deleteProject("projectimport-p002");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(true);
    configuration.setUseMavenOutputFolders(true);
    IProject project = importProject("projectimport-p002", "projects/projectimport/p002", configuration);

    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);

    assertEquals(new Path("/projectimport-p002/target/classes"), javaProject.getOutputLocation());
    IClasspathEntry[] cp = javaProject.getRawClasspath();

    assertEquals(3, cp.length);
    assertEquals(new Path("/projectimport-p002/p002-m1/src/main/java"), cp[0].getPath());
    assertEquals(new Path("/projectimport-p002/p002-m1/target/classes"), cp[0].getOutputLocation());
  }

  public void testEmbedderException() throws Exception {
    deleteProject("MNGECLIPSE-157parent");

    importProject("projects/MNGECLIPSE-157parent/pom.xml", new ResolverConfiguration());
    IProject project = importProject("projects/MNGECLIPSE-157child/pom.xml", new ResolverConfiguration());
    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    waitForJobsToComplete();

    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(toString(markers), 1, markers.length);
    assertEquals(toString(markers), "pom.xml", markers[0].getResource().getFullPath().lastSegment());
  }

  public void testClasspathOrderWorkspace001() throws Exception {
    deleteProject("p1");
    deleteProject("p2");
    
    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(false);
    configuration.setResolveWorkspaceProjects(true);
    configuration.setFilterResources(false);
    configuration.setUseMavenOutputFolders(false);
    configuration.setActiveProfiles("");

    IProject p1 = importProject("projects/dependencyorder/p1/pom.xml", configuration);
    IProject p2 = importProject("projects/dependencyorder/p2/pom.xml", configuration);
    p1.build(IncrementalProjectBuilder.FULL_BUILD, null);
    p2.build(IncrementalProjectBuilder.FULL_BUILD, null);
    waitForJobsToComplete();
    
//    MavenPlugin.getDefault().getBuildpathManager().updateClasspathContainer(p1, new NullProgressMonitor());

    IJavaProject javaProject = JavaCore.create(p1);
    IClasspathContainer maven2ClasspathContainer = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = maven2ClasspathContainer.getClasspathEntries();

    // order according to mvn -X
    assertEquals(3, cp.length);
    assertEquals(new Path("/p2"),  cp[0].getPath());
    assertEquals("junit-4.0.jar",  cp[1].getPath().lastSegment());
    assertEquals("easymock-1.0.jar",  cp[2].getPath().lastSegment());
  }

  public void testClasspathOrderWorkspace003() throws Exception {
    deleteProject("p3");
    
    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(false);
    configuration.setResolveWorkspaceProjects(true);
    configuration.setFilterResources(false);
    configuration.setUseMavenOutputFolders(false);
    configuration.setActiveProfiles("");

    IProject p3 = importProject("projects/dependencyorder/p3/pom.xml", configuration);
    p3.build(IncrementalProjectBuilder.FULL_BUILD, null);
    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(p3);
    IClasspathContainer maven2ClasspathContainer = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = maven2ClasspathContainer.getClasspathEntries();

    // order according to mvn -X. note that maven 2.0.7 and 2.1-SNAPSHOT produce different order 
    assertEquals(6, cp.length);
    assertEquals("junit-3.8.1.jar",  cp[0].getPath().lastSegment());
    assertEquals("commons-digester-1.6.jar",  cp[1].getPath().lastSegment());
    assertEquals("commons-beanutils-1.6.jar",  cp[2].getPath().lastSegment());
    assertEquals("commons-logging-1.0.jar",  cp[3].getPath().lastSegment());
    assertEquals("commons-collections-2.1.jar",  cp[4].getPath().lastSegment());
    assertEquals("xml-apis-1.0.b2.jar",  cp[5].getPath().lastSegment());
  }

  public void testDownloadSources_001_basic() throws Exception {
    new File(repo, "downloadsources/downloadsources-t001/0.0.1/downloadsources-t001-0.0.1-sources.jar").delete();
    new File(repo, "downloadsources/downloadsources-t002/0.0.1/downloadsources-t002-0.0.1-sources.jar").delete();

    IProject project = createExisting("downloadsources-p001", "projects/downloadsources/p001");
    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);

    // sanity check
    IClasspathEntry[] cp = container.getClasspathEntries();
    assertEquals(2, cp.length);
    assertNull(cp[0].getSourceAttachmentPath());
    assertNull(cp[1].getSourceAttachmentPath());

    // test project
    MavenPlugin.getDefault().getBuildpathManager().downloadSources(project, null);
    waitForJobsToComplete();
    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();
    assertEquals(2, cp.length);
    assertEquals("downloadsources-t001-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
    assertEquals("downloadsources-t002-0.0.1-sources.jar", cp[1].getSourceAttachmentPath().lastSegment());

    {
      // cleanup
      new File(repo, "downloadsources/downloadsources-t001/0.0.1/downloadsources-t001-0.0.1-sources.jar").delete();
      new File(repo, "downloadsources/downloadsources-t002/0.0.1/downloadsources-t002-0.0.1-sources.jar").delete();
      MavenUpdateRequest updateRequest = new MavenUpdateRequest(new IProject[] {project}, false /*offline*/, false);
      MavenPlugin.getDefault().getMavenProjectManager().refresh(updateRequest);
      waitForJobsToComplete();
    }

    // test one entry
    MavenPlugin.getDefault().getBuildpathManager().downloadSources(project, cp[0].getPath());
    waitForJobsToComplete();
    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();
    assertEquals(2, cp.length);
    assertEquals("downloadsources-t001-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
    assertNull(cp[1].getSourceAttachmentPath());
  
    {
      // cleanup
      new File(repo, "downloadsources/downloadsources-t001/0.0.1/downloadsources-t001-0.0.1-sources.jar").delete();
      new File(repo, "downloadsources/downloadsources-t002/0.0.1/downloadsources-t002-0.0.1-sources.jar").delete();
      MavenUpdateRequest updateRequest = new MavenUpdateRequest(new IProject[] {project}, false /*offline*/, false);
      MavenPlugin.getDefault().getMavenProjectManager().refresh(updateRequest);
      waitForJobsToComplete();
    }

    // test two entries
    MavenPlugin.getDefault().getBuildpathManager().downloadSources(project, cp[0].getPath());
    MavenPlugin.getDefault().getBuildpathManager().downloadSources(project, cp[1].getPath());
    waitForJobsToComplete();
    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();
    assertEquals(2, cp.length);
    assertEquals("downloadsources-t001-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
    assertEquals("downloadsources-t002-0.0.1-sources.jar", cp[1].getSourceAttachmentPath().lastSegment());
  }

  public void testDownloadSources_001_sourceAttachment() throws Exception {
    new File(repo, "downloadsources/downloadsources-t001/0.0.1/downloadsources-t001-0.0.1-sources.jar").delete();
    new File(repo, "downloadsources/downloadsources-t002/0.0.1/downloadsources-t002-0.0.1-sources.jar").delete();

    IProject project = createExisting("downloadsources-p001", "projects/downloadsources/p001");
    waitForJobsToComplete();
    
    IJavaProject javaProject = JavaCore.create(project);
    final IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    
    IPath entryPath = container.getClasspathEntries()[0].getPath();

    IPath srcPath = new Path("/a");
    IPath srcRoot = new Path("/b");
    String javaDocUrl = "c";

    IClasspathAttribute attribute = JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javaDocUrl);

    final IClasspathEntry entry = JavaCore.newLibraryEntry(entryPath, //
        srcPath, srcRoot, new IAccessRule[0], //
        new IClasspathAttribute[] {attribute}, // 
        false /*not exported*/);

    BuildPathManager buildpathManager = MavenPlugin.getDefault().getBuildpathManager();
    
    IClasspathContainer containerSuggestion = new IClasspathContainer() {
      public IClasspathEntry[] getClasspathEntries() {
        return new IClasspathEntry[] {entry};
      }
      public String getDescription() {
        return container.getDescription();
      }
      public int getKind() {
        return container.getKind();
      }
      public IPath getPath() {
        return container.getPath();
      }
    };
    buildpathManager.updateClasspathContainer(javaProject, containerSuggestion);
    waitForJobsToComplete();

    // check custom source/javadoc
    IClasspathContainer container2 = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry entry2 = container2.getClasspathEntries()[0];
    assertEquals(entryPath, entry2.getPath());
    assertEquals(srcPath, entry2.getSourceAttachmentPath());
    assertEquals(srcRoot, entry2.getSourceAttachmentRootPath());
    assertEquals(javaDocUrl, buildpathManager.getJavadocLocation(entry2));

    File file = buildpathManager.getSourceAttachmentPropertiesFile(project);
    assertEquals(true, file.canRead());

    // check project delete
    project.delete(true, monitor);
    waitForJobsToComplete();
    assertEquals(false, file.canRead());
  }

  public void testDownloadSources_002_javadoconly() throws Exception {
    new File(repo, "downloadsources/downloadsources-t003/0.0.1/downloadsources-t003-0.0.1-javadoc.jar").delete();

    IProject project = createExisting("downloadsources-p002", "projects/downloadsources/p002");
    waitForJobsToComplete();

    // sanity check
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();
    assertEquals(1, cp.length);
    assertNull(cp[0].getSourceAttachmentPath()); 

    MavenPlugin.getDefault().getBuildpathManager().downloadSources(project, null);
    waitForJobsToComplete();

    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();
    assertEquals(1, cp.length);
    assertNull(cp[0].getSourceAttachmentPath()); // sanity check
    assertEquals("" + cp[0], 1, getAttributeCount(cp[0], IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME));
    
    MavenPlugin.getDefault().getBuildpathManager().downloadSources(project, null);
    waitForJobsToComplete();

    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();
    assertEquals(1, cp.length);
    assertNull(cp[0].getSourceAttachmentPath()); // sanity check
    assertEquals("" + cp[0], 1, getAttributeCount(cp[0], IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME));

  }

  public void testDownloadSources_003_customRenoteRepository() throws Exception {
    new File(repo, "downloadsources/downloadsources-t004/0.0.1/downloadsources-t004-0.0.1-sources.jar").delete();

    IProject project = createExisting("downloadsources-p003", "projects/downloadsources/p003");
    waitForJobsToComplete();

    // sanity check
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();
    assertEquals(1, cp.length);
    assertNull(cp[0].getSourceAttachmentPath()); 

    MavenPlugin.getDefault().getBuildpathManager().downloadSources(project, cp[0].getPath());
    waitForJobsToComplete();

    javaProject = JavaCore.create(project);
    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();
    assertEquals(1, cp.length);
    assertEquals("downloadsources-t004-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment()); 
  }

  private static int getAttributeCount(IClasspathEntry entry, String name) {
    IClasspathAttribute[] attrs = entry.getExtraAttributes();
    int count = 0;
    for (int i = 0; i < attrs.length; i++) {
      if (name.equals(attrs[i].getName())) {
        count++;
      }
    }
    return count;
  }

  public void testDownloadSources_004_testsClassifier() throws Exception {
    new File(repo, "downloadsources/downloadsources-t005/0.0.1/downloadsources-t005-0.0.1-test-sources.jar").delete();
    
    IProject project = createExisting("downloadsources-p004", "projects/downloadsources/p004");
    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();

    // sanity check
    assertEquals("downloadsources-t005-0.0.1-tests.jar", cp[1].getPath().lastSegment());

    MavenPlugin.getDefault().getBuildpathManager().downloadSources(project, cp[1].getPath());
    waitForJobsToComplete();
    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();

    assertEquals(2, cp.length);
    assertEquals("downloadsources-t005-0.0.1-test-sources.jar", cp[1].getSourceAttachmentPath().lastSegment());
  }

  public void testDownloadSources_004_classifier() throws Exception {
    new File(repo, "downloadsources/downloadsources-t006/0.0.1/downloadsources-t006-0.0.1-sources.jar").delete();

    IProject project = createExisting("downloadsources-p005", "projects/downloadsources/p005");
    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();

    // sanity check
    assertEquals("downloadsources-t006-0.0.1-jdk14.jar", cp[0].getPath().lastSegment());

    MavenPlugin.getDefault().getBuildpathManager().downloadSources(project, cp[0].getPath());
    waitForJobsToComplete();
    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();

    assertEquals(1, cp.length);
    assertEquals("downloadsources-t006-0.0.1-sources.jar", cp[0].getSourceAttachmentPath().lastSegment());
  }

  public void testDownloadSources_006_nonMavenProject() throws Exception {
    IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
    IndexInfo indexInfo = new IndexInfo("remoterepo-local", new File("remoterepo"), null, IndexInfo.Type.LOCAL, false);
    indexManager.addIndex(indexInfo, false);
    indexManager.reindex(indexInfo.getIndexName(), monitor);
    indexManager.addIndex(new IndexInfo("remoterepo", null, "file:remoterepo", IndexInfo.Type.REMOTE, false), false);

    IProject project = createExisting("downloadsources-p006", "projects/downloadsources/p006");

    File log4jJar = new File("remoterepo/log4j/log4j/1.2.13/log4j-1.2.13.jar");
    Path log4jPath = new Path(log4jJar.getAbsolutePath());

    File junitJar = new File("remoterepo/junit/junit/3.8.1/junit-3.8.1.jar");
    Path junitPath = new Path(junitJar.getAbsolutePath());
    
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] origCp = javaProject.getRawClasspath();
    IClasspathEntry[] cp = new IClasspathEntry[origCp.length + 2];
    System.arraycopy(origCp, 0, cp, 0, origCp.length);

    cp[cp.length - 2] = JavaCore.newLibraryEntry(log4jPath, null, null, true);
    cp[cp.length - 1] = JavaCore.newLibraryEntry(junitPath, null, null, false);
    
    javaProject.setRawClasspath(cp, monitor);

    MavenPlugin.getDefault().getBuildpathManager().downloadSources(project, log4jPath);
    waitForJobsToComplete();

    cp = javaProject.getRawClasspath();

    assertEquals(log4jPath, cp[cp.length - 2].getPath());
    assertEquals("log4j-1.2.13-sources.jar", cp[cp.length - 2].getSourceAttachmentPath().lastSegment());
    assertEquals(true, cp[cp.length - 2].isExported());

    MavenPlugin.getDefault().getBuildpathManager().downloadSources(project, junitPath);
    waitForJobsToComplete();
    
    assertEquals(junitPath, cp[cp.length - 1].getPath());
    assertEquals("junit-3.8.1-sources.jar", cp[cp.length - 1].getSourceAttachmentPath().lastSegment());
    assertEquals(false, cp[cp.length - 1].isExported());
  }

  public void testClassifiers() throws Exception {
    IProject p1 = createExisting("classifiers-p1", "projects/classifiers/classifiers-p1");
    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(p1);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();

    assertEquals(2, cp.length);
    assertEquals("classifiers-p2-0.0.1.jar", cp[0].getPath().lastSegment());
    assertEquals("classifiers-p2-0.0.1-tests.jar", cp[1].getPath().lastSegment());

    createExisting("classifiers-p2", "projects/classifiers/classifiers-p2");
    waitForJobsToComplete();
  
    container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    cp = container.getClasspathEntries();

    assertEquals(1, cp.length);
    assertEquals("classifiers-p2", cp[0].getPath().lastSegment());
  }

  public void testCreateSimpleProject() throws CoreException {

    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject( "simple-project" );
    final boolean modules = true;
    final boolean mavenFolders = true;

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        Model model = new Model();
        model.setGroupId("simple-project");
        model.setArtifactId("simple-project");
        model.setVersion("0.0.1-SNAPSHOT");

        String[] directories = {};

        ResolverConfiguration resolverConfiguration = new ResolverConfiguration();
        resolverConfiguration.setIncludeModules(modules);
        resolverConfiguration.setUseMavenOutputFolders(mavenFolders);
        
        MavenPlugin.getDefault().getBuildpathManager().createSimpleProject(project, null, model, directories, resolverConfiguration, monitor);
      }
    }, monitor);

    IJavaProject javaProject = JavaCore.create(project);
    assertEquals("ignore", javaProject.getOption(JavaCore.CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER, false));

    ResolverConfiguration resolverConfiguration = BuildPathManager.getResolverConfiguration(javaProject);
    assertEquals(modules, resolverConfiguration.shouldIncludeModules());
    assertEquals(mavenFolders, resolverConfiguration.shouldUseMavenOutputFolders());
  }

  public void test005_dependencyAvailableFromLocalRepoAndWorkspace() throws Exception {
    IProject p1 = createExisting("t005-p1", "resources/t005/t005-p1");
    IProject p2 = createExisting("t005-p2", "resources/t005/t005-p2");
    waitForJobsToComplete();

    IClasspathEntry[] cp = getMavenContainerEntries(p1);
    assertEquals(1, cp.length);
    assertEquals(p2.getFullPath(), cp[0].getPath());

    p2.close(monitor);
    waitForJobsToComplete();

    cp = getMavenContainerEntries(p1);
    assertEquals(1, cp.length);
    assertEquals("t005-p2-0.0.1.jar", cp[0].getPath().lastSegment());

    p2.open(monitor);
    waitForJobsToComplete();
  
    cp = getMavenContainerEntries(p1);
    assertEquals(1, cp.length);
    assertEquals(p2.getFullPath(), cp[0].getPath());
  }
}
