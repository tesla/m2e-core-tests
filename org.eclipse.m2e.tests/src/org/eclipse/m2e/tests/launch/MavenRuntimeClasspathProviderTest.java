/*******************************************************************************
 * Copyright (c) 2020 Till Brychcy
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Till Brychcy - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.launch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;

import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.jdt.internal.launch.MavenRuntimeClasspathProvider;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.mocks.MockLaunchConfiguration;
import org.eclipse.m2e.tests.mocks.MockLaunchConfigurationType;


public class MavenRuntimeClasspathProviderTest extends AbstractMavenProjectTestCase {

  @Test
  public void testAddJUnit5DependenciesAggregator() throws Exception {
    runAddJunit5DepsTest("551298_add_junit5_deps_aggregator", //
        "junit-jupiter-5.4.2.jar", //
        "junit-jupiter-api-5.4.2.jar", //
        "apiguardian-api-1.0.0.jar", //
        "opentest4j-1.1.1.jar", //
        "junit-platform-commons-1.4.2.jar", //
        "junit-jupiter-params-5.4.2.jar", //
        "junit-jupiter-engine-5.4.2.jar", //
        "junit-platform-engine-1.4.2.jar", //
        "junit-platform-launcher-1.4.2.jar" //
    );
  }

  @Test
  public void testAddJUnit5DependenciesAPIOnly() throws Exception {
    runAddJunit5DepsTest("551298_add_junit5_deps_apionly", //
        "junit-jupiter-api-5.4.2.jar", //
        "apiguardian-api-1.0.0.jar", //
        "opentest4j-1.1.1.jar", //
        "junit-platform-commons-1.4.2.jar", //
        "junit-platform-launcher-1.4.2.jar", //
        "junit-platform-engine-1.4.2.jar", //
        "junit-jupiter-engine-5.4.2.jar");
  }

  @Test
  public void testAddJUnit5DependenciesAPIOnlyDisabled() throws Exception {
    runAddJunit5DepsTest("551298_add_junit5_deps_apionly_disabled", //
        "junit-jupiter-api-5.4.2.jar", //
        "apiguardian-api-1.0.0.jar", //
        "opentest4j-1.1.1.jar", //
        "junit-platform-commons-1.4.2.jar");
  }

  @Test
  public void testAddJUnit5DependenciesWithEngine() throws Exception {
    runAddJunit5DepsTest("551298_add_junit5_deps_withengine", //
        "junit-jupiter-engine-5.4.2.jar",
        "apiguardian-api-1.0.0.jar", //
        "junit-platform-engine-1.4.2.jar", //
        "opentest4j-1.1.1.jar", //
        "junit-platform-commons-1.4.2.jar", //
        "junit-jupiter-api-5.4.2.jar", //
        "junit-platform-launcher-1.4.2.jar" //
    );
  }

  @Test
  public void testAddJUnit5DependenciesWithLauncher() throws Exception {
    runAddJunit5DepsTest("551298_add_junit5_deps_withlauncher", //
        "junit-platform-launcher-1.4.2.jar", //
        "apiguardian-api-1.0.0.jar", //
        "junit-platform-engine-1.4.2.jar", //
        "junit-jupiter-api-5.4.2.jar", //
        "opentest4j-1.1.1.jar", //
        "junit-platform-commons-1.4.2.jar", //
        "junit-jupiter-engine-5.4.2.jar" //
    );
  }

  private void runAddJunit5DepsTest(String projectName, String... expectedJars)
      throws IOException, CoreException {
    importProject("projects/" + projectName + "/pom.xml");
    MockLaunchConfiguration configuration = getLaunchConfiguration("projects/" + projectName);
    configuration.getAttributes().put("org.eclipse.jdt.junit.TEST_KIND", "org.eclipse.jdt.junit.loader.junit5");
    configuration.getAttributes().put(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
    MavenRuntimeClasspathProvider mavenRuntimeClasspathProvider = new MavenRuntimeClasspathProvider();
    IRuntimeClasspathEntry[] unresolved = mavenRuntimeClasspathProvider.computeUnresolvedClasspath(configuration);
    IRuntimeClasspathEntry[] resolveClasspath = mavenRuntimeClasspathProvider.resolveClasspath(unresolved,
        configuration);
    assertResolveClasspathEndsWithJars(resolveClasspath, expectedJars);
  }

  /**
   * @param resolveClasspath
   * @param jars
   */
  private void assertResolveClasspathEndsWithJars(IRuntimeClasspathEntry[] resolveClasspath, String... jars) {
    int i;
    // start looking after classes directory;
    for(i = resolveClasspath.length; i > 0; i-- ) {
      if(resolveClasspath[i - 1].getLocation().endsWith("classes")) {
        break;
      }
    }
    for(int j = 0; j < Math.min(jars.length, resolveClasspath.length - i); j++ ) {
      String location = resolveClasspath[j + i].getLocation();
      String jar = jars[j];
      assertTrue("got " + location + " but expected something ending with " + jar, location.endsWith(jar));
    }
    assertEquals(jars.length, resolveClasspath.length - i);
  }

  private MockLaunchConfiguration getLaunchConfiguration(String pomDirectory) {
    File file = new File(pomDirectory);
    String absPomDir = file.getAbsolutePath();
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(MavenLaunchConstants.ATTR_POM_DIR, absPomDir);
    return new MockLaunchConfiguration(attributes, new MockLaunchConfigurationType(Collections.singletonMap("id",
        MavenRuntimeClasspathProvider.JDT_JUNIT_TEST)));
  }
}
