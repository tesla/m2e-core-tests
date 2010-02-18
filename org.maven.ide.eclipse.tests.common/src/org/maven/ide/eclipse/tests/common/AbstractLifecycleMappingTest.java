/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.tests.common;

import org.eclipse.core.resources.IProject;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.tests.common.AbstractMavenProjectTestCase;

public abstract class AbstractLifecycleMappingTest extends AbstractMavenProjectTestCase {
  protected MavenProjectManager mavenProjectManager;
  protected IProjectConfigurationManager projectConfigurationManager;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    mavenProjectManager = MavenPlugin.getDefault().getMavenProjectManager();
    projectConfigurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
  }
  
  @Override
  protected void tearDown() throws Exception {
    projectConfigurationManager = null;
    mavenProjectManager = null;

    super.tearDown();
  }
  
  protected IMavenProjectFacade importMavenProject(String basedir, String pomName) throws Exception {
    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject[] project = importProjects(basedir, new String[] {pomName}, configuration);
    waitForJobsToComplete();

    return mavenProjectManager.create(project[0], monitor);
  }
}