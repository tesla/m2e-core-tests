/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;


/**
 * @author Rich Seddon
 */
@Ignore
public class MEclipse162ImportMavenProjectTest extends M2EUIIntegrationTestCase {

  public MEclipse162ImportMavenProjectTest() {
    super();
  }

  @Test
  public void testSimpleModuleImport() throws Exception {
    try {
      doImport("projects/commons-collections-3.2.1-src.zip");
    } catch(Exception e) {
      throw takeScreenShot(e);
    }
  }

  @Test
  public void testMultiModuleImport() throws Exception {
    try {
      doImport("projects/httpcomponents-core-4.0-beta3-src.zip");
    } catch(Exception e) {
      throw takeScreenShot(e);
    }
  }

  @Test
  public void testMultiModuleImport2() throws Exception {
    try {
      doImport("projects/testMultiModule.zip");
    } catch(Exception e) {
      throw takeScreenShot(e);
    }
  }

  @Test @Ignore
  public void testMNGEclipse1028ImportOrderMatters() throws Exception {
    //this is dependent on WTP tests which are disabled for now
    checkoutProjectsFromSVN("http://svn.sonatype.org/m2eclipse/trunk/org.maven.ide.eclipse.wtp.tests/projects/import-order-matters/");
    assertProjectsHaveNoErrors();
  }

  @After
  public void tearDown() throws Exception {
    clearProjects();
  }

}
