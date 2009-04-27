/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import java.io.File;
import java.lang.reflect.Method;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;


/**
 * @author rseddon
 */
public class MNGEclipse1326HideSubprojectsTest extends UIIntegrationTestCase {

  private File tempDir;

  public void testHideSubprojectsOnImport() throws Exception {
    tempDir = importMavenProjects("projects/plexus-security-snapshot.zip");

    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("security-realms");

    assertTrue(isHidden(project.getFolder("security-url-realm")));
    assertTrue(isHidden(project.getFolder("plexus-delegating-realm")));
    assertTrue(isHidden(project.getFolder("security-xml-realm")));

    project = ResourcesPlugin.getWorkspace().getRoot().getProject("security-aggregator");

    assertTrue(project.exists());
    
    String[] children = {"security-manager", "security-model", "security-model-xml",
        "security-parent", "security-realms", "security-rest-api", "security-system"};
    
    for(int i = 0; i < children.length; i++ ) {
      IFolder folder = project.getFolder(children[i]);
      assertNotNull("folder exists: " + children[i], folder);
      assertTrue("Child project is hidden: " + children[i], isHidden(folder));
    }

  }

  private boolean isHidden(IResource resource) throws Exception {
    if(!resource.exists()) {
      throw new Exception("Coudn't find resource: " + resource.toString());
    }
    if(isEclipseVersion(3, 3)) {
      return true;
    }
    Method m = IResource.class.getMethod("isHidden", new Class[] {});

    Boolean b = (Boolean) m.invoke(resource);
    return b.booleanValue();
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
