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

package org.eclipse.m2e.core.project.configurator;

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

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.ExtensionReader;
import org.eclipse.m2e.core.project.IMavenMarkerManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;


/**
 * AbstractLifecycleMapping
 *
 * @author igor
 */
public abstract class AbstractLifecycleMapping implements IExtensionLifecycleMapping {

  private static List<AbstractProjectConfigurator> configurators;
  private String name;
  private String id;
  private boolean showConfigurators;

  /**
   * Calls #configure method of all registered project configurators
   */
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    for(AbstractProjectConfigurator configurator : getProjectConfigurators(request.getMavenProjectFacade(), monitor)) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }
      configurator.configure(request, monitor);
    }
  }

  public void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    for(AbstractProjectConfigurator configurator : getProjectConfigurators(request.getMavenProjectFacade(), monitor)) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }
      configurator.unconfigure(request, monitor);
    }
  }
  
  public static List<AbstractProjectConfigurator> getProjectConfigurators(){
    synchronized(AbstractLifecycleMapping.class) {
      if(configurators == null) {
        MavenPlugin plugin = MavenPlugin.getDefault();
        MavenProjectManager projectManager = plugin.getMavenProjectManager();
        IMavenConfiguration mavenConfiguration;
        mavenConfiguration = MavenPlugin.getDefault().getMavenConfiguration();
        IMavenMarkerManager mavenMarkerManager = plugin.getMavenMarkerManager();
        MavenConsole console = plugin.getConsole();
        configurators = new ArrayList<AbstractProjectConfigurator>(ExtensionReader
            .readProjectConfiguratorExtensions(projectManager, mavenConfiguration, mavenMarkerManager, console));
        Collections.sort(configurators, new ProjectConfiguratorComparator());
      }
    }
    return Collections.unmodifiableList(configurators);
  }
  
  public static List<AbstractProjectConfigurator> getProjectConfigurators(boolean generic) {
    ArrayList<AbstractProjectConfigurator> result = new ArrayList<AbstractProjectConfigurator>();
    for (AbstractProjectConfigurator configurator : getProjectConfigurators()) {
      if (generic == configurator.isGeneric()) {
        result.add(configurator);
      }
    }
    return result;
  }

  public static AbstractProjectConfigurator getProjectConfigurator(String id) {
    if(id == null) {
      return null;
    }
    for(AbstractProjectConfigurator configurator : getProjectConfigurators()) {
      if(id.equals(configurator.getId())) {
        return configurator;
      }
    }
    return null;
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

  protected static void addMavenBuilder(IProject project, IProgressMonitor monitor) throws CoreException {
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

  /**
   * @return Returns the name.
   */
  public String getName() {
    return this.name;
  }

  /**
   * @param name The name to set.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return Returns the id.
   */
  public String getId() {
    return this.id;
  }

  /**
   * @param id The id to set.
   */
  public void setId(String id) {
    this.id = id;
  }
  
  /**
   * @param show Set whether the project configurators should show. Default is true.
   */
  public void setShowConfigurators(boolean show){
    this.showConfigurators = show;
  }
  
  /**
   * Returns whether the project configurators will be shown in the UI. Default is true.
   */
  public boolean showConfigurators(){
    return this.showConfigurators;
  }

  protected List<AbstractBuildParticipant> getBuildParticipants(IMavenProjectFacade facade,
      List<AbstractProjectConfigurator> configurators, IProgressMonitor monitor) throws CoreException {
    List<AbstractBuildParticipant> participants = new ArrayList<AbstractBuildParticipant>();

    for (MojoExecution execution : facade.getExecutionPlan(monitor).getExecutions()) {
      for (AbstractProjectConfigurator configurator : configurators) {
        AbstractBuildParticipant participant = configurator.getBuildParticipant(execution);
        if (participant != null) {
          participants.add(participant);
        }
      }
    }

    return participants;
  }

}
