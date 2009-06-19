/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.jdt.IClasspathDescriptor;
import org.maven.ide.eclipse.jdt.IJavaProjectConfigurator;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;


/**
 * DefaultJavaConfigurator
 * 
 * @author igor
 */
public class JavaProjectConfigurator extends AbstractJavaProjectConfigurator {

  @Override
  protected void addClasspathEntries(IClasspathDescriptor classpath, ProjectConfigurationRequest request,
      final IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade facade = request.getMavenProjectFacade();
    IProjectConfigurationManager configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
    ILifecycleMapping lifecycleMapping = configurationManager.getLifecycleMapping(facade, monitor);
    for (AbstractProjectConfigurator configurator : lifecycleMapping.getProjectConfigurators(facade, monitor)) {
      if (configurator instanceof IJavaProjectConfigurator) {
        ((IJavaProjectConfigurator) configurator).configureRawClasspath(request, classpath, monitor);
      }
    }
  }

  @Override
  protected List<MavenProject> getMavenProjects(ProjectConfigurationRequest request, final IProgressMonitor monitor)
      throws CoreException {
    
    final ArrayList<MavenProject> projects = new ArrayList<MavenProject>();
    
    IMavenProjectFacade facade = request.getMavenProjectFacade();
    facade.accept(new IMavenProjectVisitor() {

      public boolean visit(IMavenProjectFacade projectFacade) throws CoreException {
        projects.add(projectFacade.getMavenProject(monitor));
        return true; // keep visiting
      }
      
    }, IMavenProjectVisitor.NESTED_MODULES);

    return projects;
  }


}
