/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import java.io.File;
import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.maven.ide.eclipse.integration.tests.common.UIIntegrationTestCase;


/**
 * @author rseddon
 */

public abstract class M2EUIIntegrationTestCase extends UIIntegrationTestCase {

  private static final String DEFAULT_PROJECT_ZIP = "projects/someproject.zip";
  private static final String DEFAULT_PROJECT_ARTIFACT = "someproject";

  protected static final String PLUGIN_ID = "org.maven.ide.eclipse.integration.tests";

  public void importZippedMavenProjects(URL url) throws Exception {
    File f = copyPluginResourceToTempFile(PLUGIN_ID, url);
    importZippedProject(f);
  }
  
  public void importZippedMavenProjects(String pluginPath) throws Exception {
    File f = copyPluginResourceToTempFile(PLUGIN_ID, pluginPath);
    importZippedProject(f);
  }

  
  public void importZippedProject(String pluginPath) throws Exception {
    importZippedProject(PLUGIN_ID, pluginPath);
  }

  public static File unzipProject(String pluginPath) throws Exception {
    return unzipProject(PLUGIN_ID, pluginPath);
  }
  
 
  public IProject setupDefaultProject() throws Exception{
    importMavenProjects(PLUGIN_ID, DEFAULT_PROJECT_ZIP);
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for(IProject project : projects) {
      if(DEFAULT_PROJECT_ARTIFACT.equals(project.getName())) {
        return project;
      }
    }
    return null;
  }


  protected File importMavenProjects(String projectPath) throws Exception {
    return importMavenProjects(PLUGIN_ID, projectPath);
  }
  
  protected File doImport(String projectPath) throws Exception {
    return doImport(PLUGIN_ID, projectPath);
  }

}
