/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.internal.ExtensionReader;
import org.maven.ide.eclipse.project.IMavenMarkerManager;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.configurator.AbstractBuildParticipant;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;


/**
 * DefaultLifecycleMapping
 * 
 * @author igor
 */
public class DefaultLifecycleMapping implements ILifecycleMapping {

  private static List<AbstractProjectConfigurator> configurators;

  private final List<AbstractBuildParticipant> buildParticipants = new ArrayList<AbstractBuildParticipant>();

  public DefaultLifecycleMapping() {
    this.buildParticipants.add(new DefaultBuildParticipant());
  }

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    for(AbstractProjectConfigurator configurator : getProjectConfigurators()) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }
      configurator.configure(request, monitor);
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

  public void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    for(AbstractProjectConfigurator configurator : getProjectConfigurators()) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }
      configurator.unconfigure(request, monitor);
    }
  }

  public List<AbstractProjectConfigurator> getProjectConfigurators() {
    List<AbstractProjectConfigurator> configurators;
    synchronized(DefaultLifecycleMapping.class) {
      if(DefaultLifecycleMapping.configurators == null) {
        MavenPlugin plugin = MavenPlugin.getDefault();
        MavenProjectManager projectManager = plugin.getMavenProjectManager();
        IMavenConfiguration mavenConfiguration;
        mavenConfiguration = MavenPlugin.lookup(IMavenConfiguration.class);
        IMavenMarkerManager mavenMarkerManager = plugin.getMavenMarkerManager();
        MavenConsole console = plugin.getConsole();
        DefaultLifecycleMapping.configurators = new ArrayList<AbstractProjectConfigurator>(ExtensionReader
            .readProjectConfiguratorExtensions(projectManager, mavenConfiguration, mavenMarkerManager, console));
        Collections.sort(DefaultLifecycleMapping.configurators, new ProjectConfiguratorComparator());
      }
      configurators = DefaultLifecycleMapping.configurators;
    }
    return configurators;
  }

  /**
   * ProjectConfigurator comparator
   */
  static class ProjectConfiguratorComparator implements Comparator<AbstractProjectConfigurator>, Serializable {
    private static final long serialVersionUID = 1L;

    public int compare(AbstractProjectConfigurator c1, AbstractProjectConfigurator c2) {
      int res = c1.getPriority() - c2.getPriority();
      return res == 0 ? c1.getId().compareTo(c2.getId()) : res;
    }
  }

}
