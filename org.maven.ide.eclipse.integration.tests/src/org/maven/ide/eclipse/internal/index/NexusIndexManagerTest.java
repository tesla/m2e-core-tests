/*******************************************************************************
 * Copyright (c) 2009 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.index;

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.components.pom.util.PomResourceImpl;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.integration.tests.UIIntegrationTestCase;

/**
 * @author dyocum
 *
 */
public class NexusIndexManagerTest extends UIIntegrationTestCase {

  private static final String TEST_PROJECT_NAME = "editor-tests";
  
  private IProject project;

  private PomResourceImpl resource = null;

  protected void setUp() throws Exception {
    super.setUp();
    
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();
    
    project = root.getProject(TEST_PROJECT_NAME);
    if(!project.exists()) {
      project.create(new NullProgressMonitor());
    }
    if(!project.isOpen()) {
      project.open(new NullProgressMonitor());
    }
  }
  
  protected void tearDown() throws Exception {
    if (resource != null) {
      resource.unload();
    }
    super.tearDown();
  }

  public void testPublicRepo() throws Exception{
    
    File emptySettingsFile = new File("src/org/maven/ide/eclipse/internal/index/empty_settings.xml");
    assertTrue(emptySettingsFile.exists());
//    String publicMirrorPath = "";
//    String publicNonMirroredPath = "";
    

    MavenPlugin.getDefault().getMaven().getMavenConfiguration().setUserSettingsFile(emptySettingsFile.getAbsolutePath());
    assertEquals(MavenPlugin.getDefault().getMaven().getMavenConfiguration().getUserSettingsFile(), emptySettingsFile.getAbsolutePath());
    
    List<ArtifactRepository> remoteRepos = MavenPlugin.getDefault().getRemoteRepositories();
    System.out.println("settings: "+MavenPlugin.getDefault().getMaven().getMavenConfiguration().getUserSettingsFile());
    List<ArtifactRepository> remoteRepositories = MavenPlugin.getDefault().getMaven().getEffectiveRepositories(remoteRepos);
    assertEquals(1, remoteRepositories.size());
  }
  
}

