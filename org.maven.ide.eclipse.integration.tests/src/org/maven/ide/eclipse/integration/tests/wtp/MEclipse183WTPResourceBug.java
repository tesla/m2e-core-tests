/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests.wtp;

import junit.framework.ComparisonFailure;

import org.maven.ide.eclipse.integration.tests.UIIntegrationTestCase;


/**
 * @author rseddon
 */
public class MEclipse183WTPResourceBug extends UIIntegrationTestCase {

  private static final String SERVER_URL = "http://localhost:8080/bug.MNGECLIPSE-1189--war-dep/Test";

  public void testResourceInDependentJar() throws Exception {

    installTomcat6();

    importZippedProject("projects/resourcebug.zip");

    assertProjectsHaveNoErrors();

    deployProjectsIntoTomcat();

    String s = retrieveWebPage(SERVER_URL);
    try {
      assertEquals("SUCCESS", s);
    } catch(ComparisonFailure cf) {
      System.out.println("Test has failed, catching exception: " + cf.getMessage());
      cf.printStackTrace();
    }

  }

  protected void tearDown() throws Exception {

    try {
      shutdownTomcat();
    } catch(Exception ex) {
      ex.printStackTrace();
    }

    super.tearDown();

  }

}
