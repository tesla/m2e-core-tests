/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.tests;

import java.io.File;
import java.util.List;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.internal.project.MavenProjectManagerImpl;
import org.maven.ide.eclipse.project.MavenUpdateRequest;
import org.maven.ide.eclipse.project.ResolverConfiguration;


/**
 * @author Eugene Kuleshov
 */
public class MavenModelManagerTest extends AsbtractMavenProjectTestCase {

  private MavenPlugin plugin;
  private ResolverConfiguration resolverConfiguration;
  private MavenProjectManagerImpl manager;

  protected void setUp() throws Exception {
    super.setUp();

    resolverConfiguration = new ResolverConfiguration();
    
    plugin = MavenPlugin.getDefault();
    manager = new MavenProjectManagerImpl(plugin.getConsole(), plugin.getIndexManager(), //
        plugin.getMavenEmbedderManager(), null, false, plugin.getMavenRuntimeManager(), plugin.getMavenMarkerManager());
  }
  
  protected void tearDown() throws Exception {
    super.tearDown();
    
    deleteProject("MNGECLIPSE-418a");
    deleteProject("MNGECLIPSE-418b");
  }
  
  public void testReadMavenProjectWithWebdavExtension() throws Exception {
    IProject project = createExisting("MNGECLIPSE-418a", "projects/poms/MNGECLIPSE-418a");
    IFile pomFile = project.getFile(IMavenConstants.POM_FILE_NAME);
    
    MavenUpdateRequest updateRequest = new MavenUpdateRequest(false, false);
    MavenExecutionResult result = manager.readProjectWithDependencies(pomFile, resolverConfiguration, updateRequest,
        new NullProgressMonitor());

    assertFalse(result.getExceptions().toString(), result.hasExceptions());

    MavenProject mavenProject = result.getProject();
    assertNotNull(mavenProject);
  }

  public void testReadMavenProjectWithSshExtension() throws Exception {
    IProject project = createExisting("MNGECLIPSE-418b", "projects/poms/MNGECLIPSE-418b");
    IFile pomFile = project.getFile(IMavenConstants.POM_FILE_NAME);

    MavenUpdateRequest updateRequest = new MavenUpdateRequest(false, false);
    MavenExecutionResult result = manager.readProjectWithDependencies(pomFile, resolverConfiguration, updateRequest,
        new NullProgressMonitor());

    assertFalse(result.getExceptions().toString(), result.hasExceptions());

    MavenProject mavenProject = result.getProject();
    assertNotNull(mavenProject);
  }

  // XXX MAVEN-14: pom parsing error regression https://issues.sonatype.com/browse/MAVEN-14
  public void XXXtestReadMavenProjectWithTwoNames() throws Exception {
    File pomFile = new File("projects/poms/pomWithTwoNames.xml");

    {
      MavenModelManager modelManager = plugin.getMavenModelManager();
      Model model = modelManager.readMavenModel(pomFile);
      assertEquals("JT400", model.getName());
      @SuppressWarnings("unchecked")
      List<License> licenses = model.getLicenses();
      License license = licenses.get(0);
      assertEquals("IBM Public License Version 1.0", license.getName());
    }
    
    {
      MavenEmbedder embedder = plugin.getMavenEmbedderManager().getWorkspaceEmbedder();
      MavenProject project = embedder.readProject(pomFile);
      assertEquals("JT400", project.getName());
      @SuppressWarnings("unchecked")
      List<License> licenses = project.getLicenses();
      License license = licenses.get(0);
      assertEquals("IBM Public License Version 1.0", license.getName());
    }
  }
  
}
