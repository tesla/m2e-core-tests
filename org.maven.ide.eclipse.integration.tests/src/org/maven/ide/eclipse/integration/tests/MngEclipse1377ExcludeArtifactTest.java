/*******************************************************************************
 * Copyright (c) 2009 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Assert;
import org.junit.Test;
import org.maven.ide.eclipse.jdt.BuildPathManager;


/**
 * @author rseddon
 */
public class MngEclipse1377ExcludeArtifactTest extends M2EUIIntegrationTestCase {

  @Test
  public void testEclipseArtifact() throws Exception {
    setXmlPrefs();
    String projectName = "eclipseArtifactProject";
    IProject project = null;
    try {
      project = createQuickstartProject(projectName);
    } catch(Exception e) {
      project = setupDefaultProject();
    }
    IJavaProject jp = (IJavaProject) project.getNature(JavaCore.NATURE_ID);

    assertMavenCPEntry(jp, "commons-collections-3.2.1.jar", false);

    openPomFile(projectName + "/pom.xml");
    addDependency(project, "commons-collections", "commons-collections", "3.2.1");

    assertMavenCPEntry(jp, "commons-collections-3.2.1.jar", true);

    excludeArtifact(projectName, "commons-collections-3.2.1.jar");

    assertMavenCPEntry(jp, "commons-collections-3.2.1.jar", false);
  }

  public void testExcludeTransitiveArtifact() throws Exception {
    setXmlPrefs();
    String projectName = "excludeProject";
    //IProject project = setupDefaultProject();
    IProject project = createQuickstartProject(projectName);
    IJavaProject jp = (IJavaProject) project.getNature(JavaCore.NATURE_ID);

    openPomFile(projectName + "/pom.xml");
    addDependency(project, "org.hibernate", "hibernate", "3.2.4.ga");

    assertMavenCPEntry(jp, "hibernate-3.2.4.ga.jar", true);
    assertMavenCPEntry(jp, "ehcache-1.2.3.jar", true);

    excludeArtifact(projectName, "ehcache-1.2.3.jar");

    assertMavenCPEntry(jp, "ehcache-1.2.3.jar", false);
    assertMavenCPEntry(jp, "hibernate-3.2.4.ga.jar", true);

    excludeArtifact(projectName, "hibernate-3.2.4.ga.jar");
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
