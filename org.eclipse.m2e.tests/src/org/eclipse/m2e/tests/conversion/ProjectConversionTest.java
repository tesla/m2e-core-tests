/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.tests.conversion;

import java.io.File;
import java.io.FileFilter;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.m2e.tests.common.FileHelpers;


/**
 * ProjectConversionTest
 *
 * @author Fred Bricon
 */
public class ProjectConversionTest extends AbstractProjectConversionTestCase {

  public void testMavenLayoutProjectConversion() throws Exception {
    //Checks a project with Maven layout don't create extra configuration 
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


  public void testProjectConversionWithSvn() throws Exception {
    //.svn folders are not copied with the default createExisting(...) method
    //so we add some extra boilerplate to check .svn folders are ignored during conversion
    //and we don't end up with mixed Java / Resource folders
    String projectName = "custom-layout";
    deleteProject(projectName );
    String srcDir = "projects/conversion/"+projectName;
    IProject project = createExisting(projectName, srcDir);
    assertTrue(projectName + " was not created!", project.exists());
    assertNoErrors(project);
    String svnDir = "JavaSource/foo/.svn";
    FileHelpers.copyDir(new File(srcDir, svnDir), 
                        project.getFolder(svnDir).getLocation().toFile(), 
                        new FileFilter() {
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
    IProject project = createExisting(projectName, "projects/conversion/"+projectName);
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
