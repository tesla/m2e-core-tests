/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.tests;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.maven.ide.eclipse.launch.MavenRuntimeClasspathProvider;
import org.maven.ide.eclipse.launch.MavenSourcePathProvider;

public class ClasspathProviderTest extends AsbtractMavenProjectTestCase {

  public void test() throws Exception {
    IProject cptest = createExisting("cptest", "projects/MNGECLIPSE-369/cptest");
    createExisting("cptest2", "projects/MNGECLIPSE-369/cptest2");
    createExisting("testlib", "projects/MNGECLIPSE-369/testlib");
    waitForJobsToComplete();

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(cptest.getFile("TestApp.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    
    assertEquals(Arrays.asList(unresolvedClasspath).toString(), 3, unresolvedClasspath.length);

    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 6, userClasspath.length);
    assertEquals(new Path("/cptest/target/classes"), userClasspath[0].getPath());
    assertEquals(new Path("/cptest/src/main/resources"), userClasspath[1].getPath());
    assertEquals("testlib-2.0.jar", userClasspath[2].getPath().lastSegment());
    assertEquals("commons-logging-1.0.2.jar", userClasspath[3].getPath().lastSegment());
    assertEquals(new Path("/cptest2/target/classes"), userClasspath[4].getPath());
    assertEquals(new Path("/cptest2/src/main/resources"), userClasspath[5].getPath());
  }

  public void testSourcePath() throws Exception {
    IProject cptest = createExisting("cptest", "projects/MNGECLIPSE-369/cptest");
    createExisting("cptest2", "projects/MNGECLIPSE-369/cptest2");
    createExisting("testlib", "projects/MNGECLIPSE-369/testlib");
    waitForJobsToComplete();

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(cptest.getFile("TestApp.launch"));
    MavenSourcePathProvider classpathProvider = new MavenSourcePathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    
    assertEquals(Arrays.asList(unresolvedClasspath).toString(), 3, unresolvedClasspath.length);

    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 4, userClasspath.length);
    assertEquals(new Path("/cptest"), userClasspath[0].getPath());
    assertEquals("testlib-2.0.jar", userClasspath[1].getPath().lastSegment());
    assertEquals("commons-logging-1.0.2.jar", userClasspath[2].getPath().lastSegment());
    assertEquals(new Path("/cptest2"), userClasspath[3].getPath());
  }
  
  IRuntimeClasspathEntry[] getUserClasspathEntries(IRuntimeClasspathEntry[] entries) {
    ArrayList result = new ArrayList();
    for (int i = 0; i < entries.length; i++) {
      if (IRuntimeClasspathEntry.USER_CLASSES == entries[i].getClasspathProperty()) {
        result.add(entries[i]);
      }
    }
    return (IRuntimeClasspathEntry[]) result.toArray(new IRuntimeClasspathEntry[result.size()]);
  }

  public void testNoFilterResources() throws Exception {
    IProject cptest = createExisting("runtimeclasspath-nofilterresources", "projects/runtimeclasspath/nofilterresources");
    waitForJobsToComplete();

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(cptest.getFile("runtimeclasspath-nofilterresources.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 1, userClasspath.length);
    assertEquals(new Path("/runtimeclasspath-nofilterresources/src/main/resources"), userClasspath[0].getPath());
  }

  public void testNotMavenProject() throws Exception {
    IProject project = createExisting("runtimeclasspath-notmaven", "projects/runtimeclasspath/notmaven");
    waitForJobsToComplete();

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(project.getFile("notmaven.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 0, userClasspath.length);
  }

  public void testJunitClasspathOrder() throws Exception {
    IProject cptest = createExisting("runtimeclasspath-junit", "projects/runtimeclasspath/junit");
    waitForJobsToComplete();

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(cptest.getFile("runtimeclasspath-junit.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 5, userClasspath.length);
    assertEquals(new Path("/runtimeclasspath-junit/target-eclipse/test-classes"), userClasspath[0].getPath());
    assertEquals(new Path("/runtimeclasspath-junit/src/test/resources"), userClasspath[1].getPath());
    assertEquals(new Path("/runtimeclasspath-junit/target-eclipse/classes"), userClasspath[2].getPath());
    assertEquals(new Path("/runtimeclasspath-junit/src/main/resources"), userClasspath[3].getPath());
  }
  
  public void testGeneratedSources() throws Exception {
    createExisting("runtimeclasspath-gensrc01", "projects/runtimeclasspath/gensrc01");
    IProject gensrc02 = createExisting("runtimeclasspath-gensrc02", "projects/runtimeclasspath/gensrc02");
    waitForJobsToComplete();
    
    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(gensrc02.getFile("gensrc02-junit.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 3, userClasspath.length);
    assertEquals(new Path("/runtimeclasspath-gensrc02/target-eclipse/test-classes"), userClasspath[0].getPath());
    assertEquals(new Path("/runtimeclasspath-gensrc01/target-eclipse/classes"), userClasspath[1].getPath());
    assertEquals("junit-3.8.1.jar", userClasspath[2].getPath().lastSegment());
  }

  public void testProvidedScopeApp() throws Exception {
    IProject project = createExisting("runtimeclasspath-providedscope", "projects/runtimeclasspath/providedscope");
    waitForJobsToComplete();

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(project.getFile("App.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 3, userClasspath.length);
    assertEquals(new Path("/runtimeclasspath-providedscope/target-eclipse/classes"), userClasspath[0].getPath());
    assertEquals(new Path("/runtimeclasspath-providedscope/src/main/resources"), userClasspath[1].getPath());
    assertEquals("junit-3.8.1.jar", userClasspath[2].getPath().lastSegment());
  }

  public void testProvidedScopeTestApp() throws Exception {
    IProject project = createExisting("runtimeclasspath-providedscope", "projects/runtimeclasspath/providedscope");
    waitForJobsToComplete();

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(project.getFile("TestApp.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 5, userClasspath.length);
    assertEquals(new Path("/runtimeclasspath-providedscope/target-eclipse/test-classes"), userClasspath[0].getPath());
    assertEquals(new Path("/runtimeclasspath-providedscope/src/test/resources"), userClasspath[1].getPath());
    assertEquals(new Path("/runtimeclasspath-providedscope/target-eclipse/classes"), userClasspath[2].getPath());
    assertEquals(new Path("/runtimeclasspath-providedscope/src/main/resources"), userClasspath[3].getPath());
    assertEquals("junit-3.8.1.jar", userClasspath[4].getPath().lastSegment());
  }


  public void testProvidedScopeAppTest() throws Exception {
    IProject project = createExisting("runtimeclasspath-providedscope", "projects/runtimeclasspath/providedscope");
    waitForJobsToComplete();

    ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(project.getFile("AppTest.launch"));

    MavenRuntimeClasspathProvider classpathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolvedClasspath = classpathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolvedClasspath = classpathProvider.resolveClasspath(unresolvedClasspath, configuration);
    IRuntimeClasspathEntry[] userClasspath = getUserClasspathEntries(resolvedClasspath);

    assertEquals(Arrays.asList(userClasspath).toString(), 5, userClasspath.length);
    assertEquals(new Path("/runtimeclasspath-providedscope/target-eclipse/test-classes"), userClasspath[0].getPath());
    assertEquals(new Path("/runtimeclasspath-providedscope/src/test/resources"), userClasspath[1].getPath());
    assertEquals(new Path("/runtimeclasspath-providedscope/target-eclipse/classes"), userClasspath[2].getPath());
    assertEquals(new Path("/runtimeclasspath-providedscope/src/main/resources"), userClasspath[3].getPath());
    assertEquals("junit-3.8.1.jar", userClasspath[4].getPath().lastSegment());
  }
}
