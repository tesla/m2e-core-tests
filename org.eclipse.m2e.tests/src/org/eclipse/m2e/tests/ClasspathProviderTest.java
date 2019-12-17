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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.jdt.internal.launch.MavenRuntimeClasspathProvider;
import org.eclipse.m2e.jdt.internal.launch.MavenSourcePathProvider;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


public class ClasspathProviderTest extends AbstractMavenProjectTestCase {

  public void test() throws Exception {
    IProject cptest = createExisting("cptest", "projects/MNGECLIPSE-369/cptest");
    createExisting("cptest2", "projects/MNGECLIPSE-369/cptest2");
    createExisting("testlib", "projects/MNGECLIPSE-369/testlib");
    waitForJobsToComplete();

    workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    WorkspaceHelpers.assertNoErrors(cptest);

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfiguration(cptest.getFile("TestApp.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);

    assertEquals(Arrays.asList(unresolvedClasspath).toString(), 3, unresolvedClasspath.length);

    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 4, userClasspath.length);
    assertEquals(new Path("/cptest/target/classes"), userClasspath[0].getPath());
    assertEquals("testlib-2.0.jar", userClasspath[1].getPath().lastSegment());
    assertEquals("commons-logging-1.0.2.jar", userClasspath[2].getPath().lastSegment());
    assertEquals(new Path("/cptest2/target/classes"), userClasspath[3].getPath());
  }

  public void testNonDefaultTestSource() throws Exception {
    deleteProject("515398");
    IProject project = createExisting("515398", "projects/515398");
    waitForJobsToComplete();
    workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    WorkspaceHelpers.assertNoErrors(project);

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfiguration(project.getFile("TestApp.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    assertEquals(Arrays.asList(unresolvedClasspath).toString(), 3, unresolvedClasspath.length);

    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 2, userClasspath.length);
    assertEquals(new Path("/515398/target/test-classes"), userClasspath[0].getPath());
    assertEquals(new Path("/515398/target/classes"), userClasspath[1].getPath());
  }

  public void testSourcePath() throws Exception {
    IProject cptest = createExisting("cptest", "projects/MNGECLIPSE-369/cptest");
    createExisting("cptest2", "projects/MNGECLIPSE-369/cptest2");
    createExisting("testlib", "projects/MNGECLIPSE-369/testlib");
    waitForJobsToComplete();

    workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfiguration(cptest.getFile("TestApp.launch"));
    MavenSourcePathProvider classpathProvider = new MavenSourcePathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);

    assertEquals(Arrays.asList(unresolvedClasspath).toString(), 3, unresolvedClasspath.length);

    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    // source path contains project entries
    assertEquals(Arrays.asList(userClasspath).toString(), 4, userClasspath.length);
    assertEquals(new Path("/cptest"), userClasspath[0].getPath());
    assertEquals(IRuntimeClasspathEntry.PROJECT, userClasspath[0].getType());
    assertEquals("testlib-2.0.jar", userClasspath[1].getPath().lastSegment());
    assertEquals("commons-logging-1.0.2.jar", userClasspath[2].getPath().lastSegment());
    assertEquals(new Path("/cptest2"), userClasspath[3].getPath());
    assertEquals(IRuntimeClasspathEntry.PROJECT, userClasspath[3].getType());
  }

  IRuntimeClasspathEntry[] getUserClasspathEntries(IRuntimeClasspathEntry[] entries) {
    ArrayList<IRuntimeClasspathEntry> result = new ArrayList<IRuntimeClasspathEntry>();
    for(IRuntimeClasspathEntry entry : entries) {
      if(IRuntimeClasspathEntry.USER_CLASSES == entry.getClasspathProperty()) {
        result.add(entry);
      }
    }
    return result.toArray(new IRuntimeClasspathEntry[result.size()]);
  }

  public void testNoFilterResources() throws Exception {
    IProject cptest = createExisting("runtimeclasspath-nofilterresources",
        "projects/runtimeclasspath/nofilterresources");
    waitForJobsToComplete();

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfiguration(cptest.getFile("runtimeclasspath-nofilterresources.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 1, userClasspath.length);
    assertEquals(new Path("/runtimeclasspath-nofilterresources/target/classes"), userClasspath[0].getPath());
  }

  public void testNotMavenProject() throws Exception {
    IProject project = createExisting("runtimeclasspath-notmaven", "projects/runtimeclasspath/notmaven");
    waitForJobsToComplete();

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfiguration(project.getFile("notmaven.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 0, userClasspath.length);
  }

  public void testJunitClasspathOrder() throws Exception {
    IProject cptest = createExisting("runtimeclasspath-junit", "projects/runtimeclasspath/junit");
    waitForJobsToComplete();

    workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfiguration(cptest.getFile("runtimeclasspath-junit.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 3, userClasspath.length);
    assertEquals(new Path("/runtimeclasspath-junit/target/test-classes"), userClasspath[0].getPath());
    assertEquals(new Path("/runtimeclasspath-junit/target/classes"), userClasspath[1].getPath());
  }

//  This require TestNG plugin to be present  
//  public void testTestNGClasspathOrder() throws Exception {
//    IProject cptest = createExisting("runtimeclasspath-testng", "projects/runtimeclasspath/testng");
//    waitForJobsToComplete();
//    
//    workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
//    
//    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
//    ILaunchConfiguration configuration = launchManager.getLaunchConfiguration(cptest
//        .getFile("runtimeclasspath-testng.launch"));
//    
//    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
//    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
//    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
//    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);
//    
//    assertEquals(Arrays.asList(userClasspath).toString(), 4, userClasspath.length);
//    assertEquals(new Path("/runtimeclasspath-testng/target/test-classes"), userClasspath[0].getPath());
//    assertEquals(new Path("/runtimeclasspath-testng/target/classes"), userClasspath[1].getPath());
//    assertEquals("testng-5.8-jdk15.jar", userClasspath[2].getPath().lastSegment());
//    assertEquals("junit-3.8.1.jar", userClasspath[3].getPath().lastSegment());
//  }

  public void testProvidedScopeApp() throws Exception {
    IProject project = createExisting("runtimeclasspath-providedscope", "projects/runtimeclasspath/providedscope");
    waitForJobsToComplete();

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfiguration(project.getFile("App.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 2, userClasspath.length);
    assertEquals(new Path("/runtimeclasspath-providedscope/target/classes"), userClasspath[0].getPath());
    assertEquals("junit-3.8.1.jar", userClasspath[1].getPath().lastSegment());
  }

  public void testProvidedScopeTestApp() throws Exception {
    IProject project = createExisting("runtimeclasspath-providedscope", "projects/runtimeclasspath/providedscope");
    waitForJobsToComplete();

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfiguration(project.getFile("TestApp.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 3, userClasspath.length);
    assertEquals(new Path("/runtimeclasspath-providedscope/target/test-classes"), userClasspath[0].getPath());
    assertEquals(new Path("/runtimeclasspath-providedscope/target/classes"), userClasspath[1].getPath());
    assertEquals("junit-3.8.1.jar", userClasspath[2].getPath().lastSegment());
  }

  public void testProvidedScopeAppTest() throws Exception {
    IProject project = createExisting("runtimeclasspath-providedscope", "projects/runtimeclasspath/providedscope");
    waitForJobsToComplete();

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfiguration(project.getFile("AppTest.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 3, userClasspath.length);
    assertEquals(new Path("/runtimeclasspath-providedscope/target/test-classes"), userClasspath[0].getPath());
    assertEquals(new Path("/runtimeclasspath-providedscope/target/classes"), userClasspath[1].getPath());
    assertEquals("junit-3.8.1.jar", userClasspath[2].getPath().lastSegment());
  }

  public void testSystemScope() throws Exception {
    IProject project = createExisting("runtimeclasspath-systemscope", "projects/runtimeclasspath/systemscope");

    MavenXpp3Reader reader = new MavenXpp3Reader();
    InputStream is = project.getFile("pom-orig.xml").getContents();
    Model model;
    try {
      model = reader.read(is);
    } finally {
      is.close();
    }

    Dependency d = model.getDependencies().get(0);
    d.setSystemPath(new File("repositories/remoterepo/log4j/log4j/1.2.13/log4j-1.2.13.jar").getCanonicalPath());

    MavenXpp3Writer writer = new MavenXpp3Writer();

    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    OutputStreamWriter out = new OutputStreamWriter(buf);
    writer.write(out, model);

    project.getFile("pom.xml").create(new ByteArrayInputStream(buf.toByteArray()), true, null);

    waitForJobsToComplete();

    workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfiguration(project.getFile("SystemScope.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 2, userClasspath.length);
    assertEquals(new Path("/runtimeclasspath-systemscope/target/classes"), userClasspath[0].getPath());
    assertEquals("log4j-1.2.13.jar", userClasspath[1].getPath().lastSegment());
  }

  public void testCustomClasspath() throws Exception {
    IProject project = createExisting("runtimeclasspath-customentries", "projects/runtimeclasspath/customentries");
    waitForJobsToComplete();

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfiguration(project.getFile("runtimeclasspath-customentries.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 1, userClasspath.length);
    assertEquals("custom.jar", userClasspath[0].getPath().lastSegment());
  }

  public void testCustomProjectEntry() throws Exception {
    IProject project = createExisting("runtimeclasspath-customentries", "projects/runtimeclasspath/customentries");
    IProject javaproject = createExisting("runtimeclasspath-javaproject", "projects/runtimeclasspath/javaproject");
    waitForJobsToComplete();

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfiguration(project.getFile("runtimeclasspath-javaproject.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 1, userClasspath.length);
    assertEquals(javaproject.getFullPath(), userClasspath[0].getPath());
  }

  public void testCustomBuildpath() throws Exception {
    IProject project = createExisting("runtimeclasspath-custombuildpath", "projects/runtimeclasspath/custombuildpath");
    IProject javaproject = createExisting("runtimeclasspath-javaproject", "projects/runtimeclasspath/javaproject");
    waitForJobsToComplete();

    workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfiguration(project.getFile("runtimeclasspath-custombuildpath.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 3, userClasspath.length);
    assertEquals(new Path("/runtimeclasspath-custombuildpath/target/classes"), userClasspath[0].getPath());
    assertEquals(javaproject.getFullPath(), userClasspath[1].getPath());
    assertEquals("custom.jar", userClasspath[2].getPath().lastSegment());
  }

  public void testTestClassesDefaultClassifier() throws Exception {
    createExisting("runtimeclasspath-testscope01", "projects/runtimeclasspath/testscope01");
    IProject p02 = createExisting("runtimeclasspath-testscope02", "projects/runtimeclasspath/testscope02");
    waitForJobsToComplete();

    workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfiguration(p02.getFile("T02.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 3, userClasspath.length);
    assertEquals(new Path("/runtimeclasspath-testscope02/target/test-classes"), userClasspath[0].getPath());
    assertEquals(new Path("/runtimeclasspath-testscope02/target/classes"), userClasspath[1].getPath());
    assertEquals(new Path("/runtimeclasspath-testscope01/target/classes"), userClasspath[2].getPath());
  }

  public void testTestClassesTestsClassifier() throws Exception {
    createExisting("runtimeclasspath-testscope01", "projects/runtimeclasspath/testscope01");
    IProject p03 = createExisting("runtimeclasspath-testscope03", "projects/runtimeclasspath/testscope03");
    waitForJobsToComplete();

    workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfiguration(p03.getFile("T03.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 3, userClasspath.length);
    assertEquals(new Path("/runtimeclasspath-testscope03/target/test-classes"), userClasspath[0].getPath());
    assertEquals(new Path("/runtimeclasspath-testscope03/target/classes"), userClasspath[1].getPath());
    assertEquals(new Path("/runtimeclasspath-testscope01/target/test-classes"), userClasspath[2].getPath());
  }

  public void testTestClassesDefaultAndTestsClassifier() throws Exception {
    createExisting("runtimeclasspath-testscope01", "projects/runtimeclasspath/testscope01");
    IProject p04 = createExisting("runtimeclasspath-testscope04", "projects/runtimeclasspath/testscope04");
    waitForJobsToComplete();

    workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfiguration(p04.getFile("T04.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 4, userClasspath.length);
    assertEquals(new Path("/runtimeclasspath-testscope04/target/test-classes"), userClasspath[0].getPath());
    assertEquals(new Path("/runtimeclasspath-testscope04/target/classes"), userClasspath[1].getPath());
    assertEquals(new Path("/runtimeclasspath-testscope01/target/classes"), userClasspath[2].getPath());
    assertEquals(new Path("/runtimeclasspath-testscope01/target/test-classes"), userClasspath[3].getPath());
  }

  public void testTestClassesTestScopeAndTestType() throws Exception {
    createExisting("runtimeclasspath-testscope01", "projects/runtimeclasspath/testscope01");
    IProject p05 = createExisting("runtimeclasspath-testscope05", "projects/runtimeclasspath/testscope05");
    waitForJobsToComplete();

    workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfiguration(p05.getFile("T05.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 4, userClasspath.length);
    assertEquals(new Path("/runtimeclasspath-testscope05/target/test-classes"), userClasspath[0].getPath());
    assertEquals(new Path("/runtimeclasspath-testscope05/target/classes"), userClasspath[1].getPath());
    assertEquals(new Path("/runtimeclasspath-testscope01/target/classes"), userClasspath[2].getPath());
    assertEquals(new Path("/runtimeclasspath-testscope01/target/test-classes"), userClasspath[3].getPath());
  }

  public void testLaunchConfigListener() throws Exception {
    IProject p01 = createExisting("runtimeclasspath-configlistener01", "projects/runtimeclasspath/configlistener01");
    IProject p02 = createExisting("runtimeclasspath-configlistener02", "projects/runtimeclasspath/configlistener02");
    waitForJobsToComplete();

    assertTrue(hasMavenClasspathProvider(p01, "runtimeclasspath-configlistener01.launch"));
    assertTrue(hasMavenClasspathProvider(p02, "runtimeclasspath-configlistener02.launch"));

    IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();

    configurationManager.disableMavenNature(p01, monitor);
    waitForJobsToComplete();

    assertFalse(hasMavenClasspathProvider(p01, "runtimeclasspath-configlistener01.launch"));
    assertTrue(hasMavenClasspathProvider(p02, "runtimeclasspath-configlistener02.launch"));

    configurationManager.enableMavenNature(p01, new ResolverConfiguration(), monitor);
    waitForJobsToComplete();

    assertTrue(hasMavenClasspathProvider(p01, "runtimeclasspath-configlistener01.launch"));
    assertTrue(hasMavenClasspathProvider(p02, "runtimeclasspath-configlistener02.launch"));

    p01.close(monitor);
    waitForJobsToComplete();

    //assertTrue(hasMavenClasspathProvider(p01, "runtimeclasspath-configlistener01.launch"));
    assertTrue(hasMavenClasspathProvider(p02, "runtimeclasspath-configlistener02.launch"));
  }

  private static boolean hasMavenClasspathProvider(IProject project, String file) throws CoreException {
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfiguration configuration = launchManager.getLaunchConfiguration(project.getFile(file));

    String provider = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER,
        (String) null);
    return MavenRuntimeClasspathProvider.MAVEN_CLASSPATH_PROVIDER.equals(provider);
  }

  public void test368230_FancyClassifier() throws Exception {
    createExisting("runtimeclasspath-testscope01", "projects/runtimeclasspath/testscope01");
    IProject p06 = createExisting("runtimeclasspath-testscope06", "projects/runtimeclasspath/testscope06");
    waitForJobsToComplete();

    workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();

    /* check runtime classpath */{
      ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
          .getLaunchConfiguration(p06.getFile("T06-runtime.launch"));
      IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
      IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath,
          configuration);
      IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

      assertEquals("Invalid runtime classpath :" + Arrays.asList(userClasspath).toString(), 3, userClasspath.length);
      assertEquals(new Path("/runtimeclasspath-testscope06/target/classes"), userClasspath[0].getPath());
      assertEquals(new Path("/runtimeclasspath-testscope01/src/main/java"), userClasspath[1].getPath());
      assertEquals(new Path("/runtimeclasspath-testscope01/src/main/resources"), userClasspath[2].getPath());
    }

    /*check test classpath*/{
      ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
          .getLaunchConfiguration(p06.getFile("T06-test.launch"));
      IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
      IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath,
          configuration);
      IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

      assertEquals("Invalid test classpath :" + Arrays.asList(userClasspath).toString(), 4, userClasspath.length);
      assertEquals(new Path("/runtimeclasspath-testscope06/target/test-classes"), userClasspath[0].getPath());
      assertEquals(new Path("/runtimeclasspath-testscope06/target/classes"), userClasspath[1].getPath());
      assertEquals(new Path("/runtimeclasspath-testscope01/src/test/java"), userClasspath[2].getPath());
      assertEquals(new Path("/runtimeclasspath-testscope01/src/test/resources"), userClasspath[3].getPath());
    }
  }

  public void test368230_unknownClassifier() throws Exception {
    createExisting("runtimeclasspath-testscope01", "projects/runtimeclasspath/testscope01");
    IProject p07 = createExisting("runtimeclasspath-testscope07", "projects/runtimeclasspath/testscope07");
    waitForJobsToComplete();

    workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();

    /* check runtime classpath */{
      ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
          .getLaunchConfiguration(p07.getFile("T07-runtime.launch"));
      IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
      IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath,
          configuration);
      IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

      assertEquals("Invalid runtime classpath :" + Arrays.asList(userClasspath).toString(), 1, userClasspath.length);
      assertEquals(new Path("/runtimeclasspath-testscope07/target/classes"), userClasspath[0].getPath());
    }

    /*check test classpath*/{
      ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager()
          .getLaunchConfiguration(p07.getFile("T07-test.launch"));
      IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
      IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath,
          configuration);
      IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

      assertEquals("Invalid test classpath :" + Arrays.asList(userClasspath).toString(), 2, userClasspath.length);
      assertEquals(new Path("/runtimeclasspath-testscope07/target/test-classes"), userClasspath[0].getPath());
      assertEquals(new Path("/runtimeclasspath-testscope07/target/classes"), userClasspath[1].getPath());
    }
  }

}
