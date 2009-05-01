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
import org.maven.ide.eclipse.MavenPlugin;


/**
 * @author rseddon
 */
public class MNGEclipse1326HideSubprojectsTest extends UIIntegrationTestCase {

  private File tempDir;

  private void importAsHidden(boolean hide) throws Exception {
    if(isEclipseVersion(3, 3)) {
      return;
    }
    MavenPlugin.getDefault().getMavenRuntimeManager().setHideFoldersOfNestedProjects(hide);
    tempDir = importMavenProjects("projects/plexus-security-snapshot.zip");

    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("security-realms");

    assertEquals(hide, isHidden(project.getFolder("security-url-realm")));
    assertEquals(hide, isHidden(project.getFolder("plexus-delegating-realm")));
    assertEquals(hide, isHidden(project.getFolder("security-xml-realm")));

    project = ResourcesPlugin.getWorkspace().getRoot().getProject("security-aggregator");

    assertTrue(project.exists());

    String[] children = {"security-manager", "security-model", "security-model-xml", "security-parent",
        "security-realms", "security-rest-api", "security-system"};

    for(int i = 0; i < children.length; i++ ) {
      IFolder folder = project.getFolder(children[i]);
      assertNotNull("folder exists: " + children[i], folder);
      assertEquals("Child project : " + children[i], hide, isHidden(folder));
    }
  }

  public void testHideSubprojectsOnImport() throws Exception {
    importAsHidden(true);
  }

  public void testDontHideSubprojectsOnImport() throws Exception {
    importAsHidden(false);
  }

  private boolean isHidden(IResource resource) throws Exception {
    if(!resource.exists()) {
      throw new Exception("Coudn't find resource: " + resource.toString());
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
    MavenPlugin.getDefault().getMavenRuntimeManager().setHideFoldersOfNestedProjects(false);
    super.tearDown();

  }
}
