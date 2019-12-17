/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.conversion;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.tests.common.FileHelpers;


/**
 * ProjectConversionTest
 * 
 * @author Fred Bricon
 */
public class ProjectConversionTest extends AbstractProjectConversionTestCase {

  @Before
  public void setUp() throws Exception {
    super.setUp();
    System.setProperty("org.eclipse.m2e.jdt.conversion.compiler.version", "2.3.2");
  }

  @After
  public void tearDown() throws Exception {
    System.clearProperty("org.eclipse.m2e.jdt.conversion.compiler.version");
    super.tearDown();
  }

  public void testMavenLayoutProjectConversion() throws Exception {
    //Checks a project with Maven layout doesn't create extra configuration 
    //for source and resource folders 
    testProjectConversion("maven-layout");
  }

  public void testJavaMixedWithResourcesProjectConversion() throws Exception {
    //Checks a source directory having mixed java and non-java files also 
    // generates a resource
    testProjectConversion("mixed-resources");
  }

  public void testNoCustomizationNeededProjectConversion() throws Exception {
    //Checks a project with maven layout and Java 1.5 produces a minimal pom.xml
    testProjectConversion("no-customization-needed");
  }

  public void testMultipleSourcesProjectConversion() throws Exception {
    //Checks a project having multiple source directories (main and test) 
    // only configures the first one (because the maven model, by default can only support
    //one sourceDirectory and one testSourceDirectory)
    testProjectConversion("multiple-sources");
  }

  public void testOptionalSourceProjectConversion() throws Exception {
    //Checks a project having optional and missing source directories doesn't crash
    // during conversion
    testProjectConversion("missing-source-folder");
  }

  public void testInheritJavaSettingsDuringConversion() throws Exception {
    //Checks a project with no specific compiler settings inherits workspace compiler settings
    // during conversion
    Hashtable<String, String> options = JavaCore.getOptions();
    try {
      String version = "1.7";
      Hashtable<String, String> newOptions = new Hashtable<>(options);
      newOptions.put(JavaCore.COMPILER_SOURCE, version);
      newOptions.put(JavaCore.COMPILER_COMPLIANCE, version);
      newOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, version);
      JavaCore.setOptions(newOptions);
      testProjectConversion("inherit-workspace-settings");
    } finally {
      JavaCore.setOptions(options);
    }
  }

  public void testProjectConversionWithSvn() throws Exception {
    //.svn folders are not copied with the default createExisting(...) method
    //so we add some extra boilerplate to check .svn folders are ignored during conversion
    //and we don't end up with mixed Java / Resource folders
    String projectName = "custom-layout";
    deleteProject(projectName);
    String srcDir = "projects/conversion/" + projectName;
    IProject project = createExisting(projectName, srcDir);
    assertTrue(projectName + " was not created!", project.exists());
    assertNoErrors(project);
    String svnDir = "JavaSource/foo/.svn";
    FileHelpers.copyDir(new File(srcDir, svnDir), project.getFolder(svnDir).getLocation().toFile(), new FileFilter() {
      public boolean accept(File pathname) {
        return true;
      }
    });
    project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    waitForJobsToComplete();
    assertTrue(project.getFolder(svnDir).getFile("hidden/index.properties").exists());
    assertConvertsAndBuilds(project);
  }

  public IProject testProjectConversion(String projectName) throws Exception {
    deleteProject(projectName);

    //Import existing regular Eclipse project
    IProject project = createExisting(projectName, "projects/conversion/" + projectName);
    assertTrue(projectName + " was not created!", project.exists());
    assertNoErrors(project);

    //Check the project converts and builds correctly
    assertConvertsAndBuilds(project);

    return project;
  }

  /**
   * @param project
   * @throws CoreException
   * @throws InterruptedException
   * @throws Exception
   */
  protected void assertConvertsAndBuilds(IProject project) throws CoreException, InterruptedException, Exception {
    //Convert the project to a Maven project (generates pom.xml, enables Maven nature)
    convert(project);

    //Checks the generated pom.xml is identical to /<projectName>/expectedPom.xml
    verifyGeneratedPom(project);

    //Checks the Maven project builds without errors
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);
  }

}
