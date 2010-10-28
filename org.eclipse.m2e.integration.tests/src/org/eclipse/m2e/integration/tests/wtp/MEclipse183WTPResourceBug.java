/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.integration.tests.wtp;

import org.eclipse.m2e.integration.tests.M2EUIIntegrationTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author rseddon
 */
public class MEclipse183WTPResourceBug extends M2EUIIntegrationTestCase {

  private static final String SERVER_URL = "http://localhost:8080/bug.MNGECLIPSE-1189--war-dep/Test";

  @Test
  public void testResourceInDependentJar() throws Exception {

    installTomcat6();

    importZippedProject("projects/resourcebug.zip");

    assertProjectsHaveNoErrors();

    deployProjectsIntoTomcat();

    String s = retrieveWebPage(SERVER_URL);

    Assert.assertEquals("SUCCESS", s);
  }

  @After
  public void tearDown() throws Exception {
    shutdownServer();
  }

}
