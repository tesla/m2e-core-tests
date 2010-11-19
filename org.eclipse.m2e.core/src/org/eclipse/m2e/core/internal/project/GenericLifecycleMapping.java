/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;


/**
 * 
 * @author igor
 */
public class GenericLifecycleMapping extends AbstractLifecycleMapping implements ILifecycleMapping {

  private final GenericBuildParticipant buildParticipant = new GenericBuildParticipant();

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

  public List<AbstractBuildParticipant> getBuildParticipants(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
    ArrayList<AbstractBuildParticipant> participants = new ArrayList<AbstractBuildParticipant>();
    participants.add(buildParticipant);
    participants.addAll(getBuildParticipants(facade, getProjectConfigurators(facade, monitor), monitor));
    return participants;
  }

  public List<String> getPotentialMojoExecutionsForBuildKind(IMavenProjectFacade projectFacade, int kind, IProgressMonitor progressMonitor) {
    List<String> goals = buildParticipant.getPossibleGoalsForBuildKind(projectFacade, kind);
    List<String> mojos = new LinkedList<String>();
    if(!goals.isEmpty()) {
      try {
        MavenProjectManager manager = MavenPlugin.getDefault().getMavenProjectManager();
        IMaven maven = MavenPlugin.getDefault().getMaven();
        ResolverConfiguration configuration = projectFacade.getResolverConfiguration();
        MavenExecutionRequest request = manager.createExecutionRequest(projectFacade.getPom(), configuration, progressMonitor);
        request.setGoals(goals);
        MavenExecutionPlan plan = maven.calculateExecutionPlan(request, projectFacade.getMavenProject(progressMonitor), progressMonitor);
        for(MojoExecution mojo : plan.getMojoExecutions()) {
          if("compile:compile".equals(mojo.getMojoDescriptor().getFullGoalName()) && configuration.isSkipCompiler()) { //$NON-NLS-1$
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

  @Override
  public List<MojoExecution> getNotCoveredMojoExecutions(IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor) throws CoreException {
    return Collections.EMPTY_LIST;
  }

  public boolean isInterestingPhase(String phase) {
    return true;
  }
}
