/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.configurator.AbstractBuildParticipant;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;
import org.maven.ide.eclipse.project.configurator.MojoExecutionBuildParticipant;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;



/**
 * CustomizableLifecycleMapping
 * 
 * @author igor
 */
public class CustomizableLifecycleMapping extends AbstractLifecycleMapping implements ILifecycleMapping {

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    
  }

  public List<AbstractBuildParticipant> getBuildParticipants(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
    IMaven maven = MavenPlugin.lookup(IMaven.class);
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    
    MavenExecutionRequest request = projectManager.createExecutionRequest(facade.getPom(), facade.getResolverConfiguration());

    // TODO should phase be configurable?
    request.setGoals(Collections.singletonList("package"));
    MavenExecutionPlan plan = maven.calculateExecutionPlan(request, facade.getMavenProject(monitor), monitor); 

    ArrayList<AbstractBuildParticipant> participants = new ArrayList<AbstractBuildParticipant>();
    
    for (MojoExecution exec : plan.getExecutions()) {
      if ("org.apache.maven.plugins".equals(exec.getGroupId()) && "maven-resources-plugin".equals(exec.getArtifactId())) {
        participants.add(new MojoExecutionBuildParticipant(exec));
      }
    }

    return participants;
  }

  public List<AbstractProjectConfigurator> getProjectConfigurators() {
    // TODO Auto-generated method getProjectConfigurators
    return super.getProjectConfigurators();
  }

  public void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    // TODO Auto-generated method unconfigure

  }
}
