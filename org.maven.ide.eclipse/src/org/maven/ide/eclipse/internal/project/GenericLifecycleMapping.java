/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.project.configurator.AbstractBuildParticipant;
import org.maven.ide.eclipse.project.configurator.AbstractLifecycleMapping;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;


/**
 * 
 * @author igor
 */
public class GenericLifecycleMapping extends AbstractLifecycleMapping implements ILifecycleMapping {

  private final DefaultBuildParticipant buildParticipant = new DefaultBuildParticipant();

  public GenericLifecycleMapping() {
  }

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    super.configure(request, monitor);

    addMavenBuilder(request.getProject(), monitor);
  }

  public List<AbstractProjectConfigurator> getProjectConfigurators(IMavenProjectFacade facade, IProgressMonitor monitor) {
    return getProjectConfigurators(true);
  }

  public List<AbstractBuildParticipant> getBuildParticipants(IMavenProjectFacade facade, IProgressMonitor monitor) {
    return Collections.singletonList((AbstractBuildParticipant)buildParticipant);
  }
  
  public List<String> getPotentialMojoExecutionsForBuildKind(IMavenProjectFacade projectFacade, int kind, IProgressMonitor progressMonitor) {
    List<String> goals = buildParticipant.getPossibleGoalsForBuildKind(projectFacade, kind);
    List<String> mojos = new LinkedList<String>();
    if(!goals.isEmpty()) {
      try {
        MavenProjectManager manager = MavenPlugin.lookup(MavenProjectManager.class);
        IMaven maven = MavenPlugin.lookup(IMaven.class);
        ResolverConfiguration configuration = projectFacade.getResolverConfiguration();
        MavenExecutionRequest request = manager.createExecutionRequest(projectFacade.getPom(), configuration, progressMonitor);
        request.setGoals(goals);
        MavenExecutionPlan plan = maven.calculateExecutionPlan(request, projectFacade.getMavenProject(progressMonitor), progressMonitor);
        for(MojoExecution mojo : plan.getExecutions()) {
          if("compile:compile".equals(mojo.getMojoDescriptor().getFullGoalName()) && configuration.isSkipCompiler()) {
            continue;
          }
          mojos.add(MojoExecutionUtils.getExecutionKey(mojo));
        }
      } catch(CoreException ex) {
        MavenLogger.log(ex);
      }
    }
    return mojos;
  }

}
