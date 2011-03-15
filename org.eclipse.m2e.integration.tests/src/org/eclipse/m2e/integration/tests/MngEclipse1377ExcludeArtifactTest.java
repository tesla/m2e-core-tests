/*******************************************************************************
 * Copyright (c) 2009 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.integration.tests;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.jdt.internal.BuildPathManager;


/**
 * @author rseddon
 */
public class MngEclipse1377ExcludeArtifactTest extends M2EUIIntegrationTestCase {

  private static final String GROUP_ID = "org.eclipse.m2e.its";

  private static final String ARTIFACT_ID = "m2e-its-a";

  private static final String VERSION = "0.1";

  private static final String JAR = ARTIFACT_ID + "-" + VERSION + ".jar";

  private static String oldUserSettings;

  @BeforeClass
  public static void setUpBeforeClass() throws CoreException {
    oldUserSettings = setUserSettings("resources/settings.xml");
    updateRepositoryRegistry();
    updateIndex("file:resources/remote-repo");
  }

  @AfterClass
  public static void tearDownAfterclass() throws CoreException {
    setUserSettings(oldUserSettings);
  }

  @Test
  public void testEclipseArtifact() throws Exception {
    String projectName = "eclipseArtifactProject";
    IProject project = null;
    try {
      project = createQuickstartProject(projectName);
    } catch(Exception e) {
      project = setupDefaultProject();
    }
    IJavaProject jp = (IJavaProject) project.getNature(JavaCore.NATURE_ID);

    assertMavenCPEntry(jp, JAR, false);

    openPomFile(projectName + "/pom.xml");
    addDependency(project, GROUP_ID, ARTIFACT_ID, VERSION);
    waitForAllBuildsToComplete();

    assertMavenCPEntry(jp, JAR, true);

    excludeArtifact(projectName, JAR, GROUP_ID + ":" + ARTIFACT_ID + ":" + VERSION);
    waitForAllBuildsToComplete();

    assertMavenCPEntry(jp, JAR, false);
  }

  public void testExcludeTransitiveArtifact() throws Exception {
    String projectName = "excludeProject";
    //IProject project = setupDefaultProject();
    IProject project = createQuickstartProject(projectName);
    IJavaProject jp = (IJavaProject) project.getNature(JavaCore.NATURE_ID);

    openPomFile(projectName + "/pom.xml");
    addDependency(project, "org.hibernate", "hibernate", "3.2.4.ga");
    waitForAllBuildsToComplete();

    assertMavenCPEntry(jp, "hibernate-3.2.4.ga.jar", true);
    assertMavenCPEntry(jp, "ehcache-1.2.3.jar", true);

    excludeArtifact(projectName, "ehcache-1.2.3.jar", null);
    waitForAllBuildsToComplete();

    assertMavenCPEntry(jp, "ehcache-1.2.3.jar", false);
    assertMavenCPEntry(jp, "hibernate-3.2.4.ga.jar", true);

    excludeArtifact(projectName, "hibernate-3.2.4.ga.jar", null);
    waitForAllBuildsToComplete();

    assertMavenCPEntry(jp, "hibernate-3.2.4.ga.jar", false);
  }

  private void assertMavenCPEntry(IJavaProject jp, String jarName, boolean shouldExist) throws Exception {
    IClasspathContainer maven2Container = BuildPathManager.getMaven2ClasspathContainer(jp);
    for(IClasspathEntry entry : maven2Container.getClasspathEntries()) {
      if(entry.getPath().toString().endsWith(jarName)) {
        if(shouldExist) {
          return;
        } else {
          Assert.fail("Found jar in maven classpath container: " + jarName);
        }
      }
    }
    if(shouldExist) {
      Assert.fail("Couldn't find jar in maven classpath container: " + jarName);
    }
  }

}
