/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Andrew Eisenberg - Work on Bug 350414
 *******************************************************************************/

package org.eclipse.m2e.tests.lifecycle;

import org.eclipse.m2e.tests.common.AbstractLifecycleMappingTest;


/**
 * Tests for changing the workspace lifecycle mappings file
 * WorkspaceMappingsFileTest
 *
 * @author andrew
 */
public class WorkspaceMappingsFileTest extends AbstractLifecycleMappingTest {

  protected void tearDown() throws Exception {
    mavenConfiguration.setWorkspaceMappingsFile(null);
    super.tearDown();
  }
  
  public void testIsDefault() throws Exception {
    assertEquals(mavenConfiguration.getDefaultWorkspaceMappingsFile(), 
        mavenConfiguration.getWorkspaceMappingsFile());
  }
  
  public void testChangeFromDefault() throws Exception {
    String newFile = "foo";
    mavenConfiguration.setWorkspaceMappingsFile(newFile);
    assertEquals(newFile, 
        mavenConfiguration.getWorkspaceMappingsFile());
  }
  public void testChangeToDefault() throws Exception {
    String newFile = "foo";
    mavenConfiguration.setWorkspaceMappingsFile(newFile);
    mavenConfiguration.setWorkspaceMappingsFile(null);
    assertEquals(mavenConfiguration.getDefaultWorkspaceMappingsFile(), 
        mavenConfiguration.getWorkspaceMappingsFile());
  }

  public void testChangeText() throws Exception {
    String origText =  mavenConfiguration.getWorkspaceMappings();
    String newText = "blah!";
    mavenConfiguration.setWorkspaceMappings(newText);
    assertEquals(newText + "\n", mavenConfiguration.getWorkspaceMappings());
    mavenConfiguration.setWorkspaceMappings(origText);
    assertEquals(origText, mavenConfiguration.getWorkspaceMappings());
  }
}
