/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.apache.maven.embedder.MavenEmbedder;

import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.project.configurator.AbstractBuildParticipant;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.LifecycleMapping;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;


/**
 * DefaultLifecycleMapping
 * 
 * @author igor
 */
public class DefaultLifecycleMapping implements LifecycleMapping {

  private final List<AbstractProjectConfigurator> configurators;

  private final List<AbstractBuildParticipant> buildParticipants = new ArrayList<AbstractBuildParticipant>();

  public DefaultLifecycleMapping(Set<AbstractProjectConfigurator> configurators) {
    this.configurators = new ArrayList<AbstractProjectConfigurator>(configurators);

    this.buildParticipants.add(new DefaultBuildParticipant());
  }

  public void configure(MavenEmbedder embedder, ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    for(AbstractProjectConfigurator configurator : getProjectConfigurators()) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }
      configurator.configure(embedder, request, monitor);
    }

    addMavenBuilder(request.getProject(), monitor);
  }

  private void addMavenBuilder(IProject project, IProgressMonitor monitor) throws CoreException {
    IProjectDescription description = project.getDescription();

    // ensure Maven builder is always the last one
    ICommand mavenBuilder = null;
    ArrayList<ICommand> newSpec = new ArrayList<ICommand>();
    for(ICommand command : description.getBuildSpec()) {
      if(IMavenConstants.BUILDER_ID.equals(command.getBuilderName())) {
        mavenBuilder = command;
      } else {
        newSpec.add(command);
      }
    }
    if(mavenBuilder == null) {
      mavenBuilder = description.newCommand();
      mavenBuilder.setBuilderName(IMavenConstants.BUILDER_ID);
    }
    newSpec.add(mavenBuilder);
    description.setBuildSpec(newSpec.toArray(new ICommand[newSpec.size()]));

    project.setDescription(description, monitor);
  }

  public List<AbstractBuildParticipant> getBuildParticipants() {
    return buildParticipants;
  }

  public void unconfigure(MavenEmbedder embedder, ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    for(AbstractProjectConfigurator configurator : getProjectConfigurators()) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }
      configurator.unconfigure(embedder, request, monitor);
    }
  }

  public List<AbstractProjectConfigurator> getProjectConfigurators() {
    return configurators;
  }

}
