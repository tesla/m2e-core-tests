/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import java.io.File;

import com.windowtester.runtime.util.ScreenCapture;


/**
 * @author Rich Seddon
 */
public class MEclipse162ImportMavenProjectTest extends UIIntegrationTestCase {

  private File tempDir;

  public MEclipse162ImportMavenProjectTest(){
    super();
    super.setSkipIndexes(true);
  }
  
  public void testSimpleModuleImport() throws Exception {
    try{
      tempDir = doImport("projects/commons-collections-3.2.1-src.zip");
    } catch(Exception e){
      ScreenCapture.createScreenCapture();
      throw e;
    }
  }

  public void testMultiModuleImport() throws Exception {
    try{
      tempDir = doImport("projects/httpcomponents-core-4.0-beta3-src.zip");
    } catch(Exception e){
      ScreenCapture.createScreenCapture();
      throw e;
    }
  }

  public void testMultiModuleImport2() throws Exception {
    try{
      tempDir = doImport("projects/testMultiModule.zip");
    } catch(Exception e){
      ScreenCapture.createScreenCapture();
      throw e;
    }
    
    
  }
  public void testMNGEclipse1028ImportOrderMatters() throws Exception {
    //this is dependent on WTP tests which are disabled for now
    checkoutProjectsFromSVN("http://svn.sonatype.org/m2eclipse/trunk/org.maven.ide.eclipse.wtp.tests/projects/import-order-matters/");
    assertProjectsHaveNoErrors();
  }
  
  protected void tearDown() throws Exception {
    clearProjects();

    if(tempDir != null && tempDir.exists()) {
      deleteDirectory(tempDir);
      tempDir = null;
    }
    super.tearDown();

  }

}
