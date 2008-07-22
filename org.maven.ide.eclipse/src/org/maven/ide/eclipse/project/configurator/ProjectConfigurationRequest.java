/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project.configurator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.project.ResolverConfiguration;

/**
 * ProjectConfigurationRequest
 *
 * @author igor
 */
public class ProjectConfigurationRequest {
  private final IProject project;
  private final IFile pom;
  private final ResolverConfiguration resolverConfiguration;
  private final MavenProject mavenProject;
  private final boolean updateSources;

  public ProjectConfigurationRequest(IProject project, IFile pom, MavenProject mavenProject, ResolverConfiguration configuration, boolean updateSources) {
    this.project = project;
    this.pom = pom;
    this.mavenProject = mavenProject;
    this.resolverConfiguration = configuration;
    this.updateSources = updateSources;
  }

  public IProject getProject() {
    return project;
  }

  public ResolverConfiguration getResolverConfiguration() {
    return resolverConfiguration;
  }

  public boolean isProjectConfigure() {
    return updateSources;
  }

  public boolean isProjectImport() {
    return !updateSources;
  }

  public MavenProject getMavenProject() {
    return mavenProject;
  }

  public IFile getPom() {
    return pom;
  }

}
