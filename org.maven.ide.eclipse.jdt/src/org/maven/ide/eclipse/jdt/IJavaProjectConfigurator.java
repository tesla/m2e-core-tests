/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;


/**
 * IJavaProjectConfigurator
 * 
 * @author igor
 */
public interface IJavaProjectConfigurator {

  /**
   * Configures *Maven* project classpath, i.e. content of Maven Dependencies classpath container.
   */
  public void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor) throws CoreException;

  /**
   * Configures *JDT* project classpath, i.e. project-level entries like source folders, JRE and Maven Dependencies
   * classpath container.
   */
  public void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException;
}
