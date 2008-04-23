/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project;

import java.util.Collection;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.model.Model;

public interface IProjectImportManager {
  
  ISchedulingRule getRule();

  void importProjects(Collection/*<MavenProjectInfo>*/ projects, ProjectImportConfiguration importConfiguration, IProgressMonitor monitor) throws CoreException;

  void createSimpleProject(IProject project, IPath append, Model model, String[] folders,
      ResolverConfiguration resolverConfiguration, IProgressMonitor monitor) throws CoreException;

  void createArchetypeProject(IProject project, IPath location, Archetype archetype, String groupId, String artifactId,
      String version, String packaging, ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException;

  Set collectProjects(Collection projects, boolean includeModules);

  void enableMavenNature(IProject project, ResolverConfiguration configuration, IProgressMonitor monitor) throws CoreException;

  void disableMavenNature(IProject project, IProgressMonitor monitor) throws CoreException;

  void updateProjectConfiguration(IProject project, ResolverConfiguration configuration, String goalToExecute,
      IProgressMonitor monitor) throws CoreException;

}
