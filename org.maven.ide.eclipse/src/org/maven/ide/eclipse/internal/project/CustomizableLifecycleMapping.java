/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.configurator.AbstractBuildParticipant;
import org.maven.ide.eclipse.project.configurator.AbstractLifecycleMapping;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;


/**
 * CustomizableLifecycleMapping
 * 
 * @author igor
 */
public class CustomizableLifecycleMapping extends AbstractLifecycleMapping implements ILifecycleMapping {

  public List<AbstractProjectConfigurator> getProjectConfigurators(IMavenProjectFacade facade, IProgressMonitor monitor)
      throws CoreException {
    MavenProject mavenProject = facade.getMavenProject(monitor);
    Plugin plugin = mavenProject.getPlugin("org.maven.ide.eclipse:lifecycle-mapping");

    if(plugin == null) {
      throw new IllegalArgumentException("no mapping");
    }

    // TODO assert version

    Map<String, AbstractProjectConfigurator> configuratorsMap = new LinkedHashMap<String, AbstractProjectConfigurator>();
    for(AbstractProjectConfigurator configurator : getProjectConfigurators(false)) {
      configuratorsMap.put(configurator.getId(), configurator);
    }

    Xpp3Dom config = (Xpp3Dom) plugin.getConfiguration();

    if(config == null) {
      throw new IllegalArgumentException("Empty lifecycle mapping configuration");
    }

    Xpp3Dom configuratorsDom = config.getChild("configurators");
    Xpp3Dom executionsDom = config.getChild("mojoExecutions");

    List<AbstractProjectConfigurator> configurators = new ArrayList<AbstractProjectConfigurator>();
    
    if (configuratorsDom != null) {
      for(String configuratorId : getListElements(configuratorsDom, "configurator")) {
        AbstractProjectConfigurator configurator = configuratorsMap.get(configuratorId);
        if(configurator == null) {
          throw new IllegalArgumentException("Unknown configurator id=" + configuratorId);
        }
        
        configurators.add(configurator);
      }
    }
    
    if (executionsDom != null) {
      for(String executionPattern : getListElements(executionsDom, "mojoExecution")) {
        configurators.add(MojoExecutionProjectConfigurator.fromString(executionPattern));
      }
    }

    return configurators;
  }

  private Set<String> getListElements(Xpp3Dom listDom, String elementName) {
    Set<String> elements = new LinkedHashSet<String>();
    if (listDom == null) {
      return elements;
    }

    for (Xpp3Dom elementDom : listDom.getChildren(elementName)) {
      elements.add(elementDom.getValue());
    }

    return elements;
  }

  public List<AbstractBuildParticipant> getBuildParticipants(IMavenProjectFacade facade, IProgressMonitor monitor)
      throws CoreException {

    List<AbstractProjectConfigurator> configurators = getProjectConfigurators(facade, monitor);

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
  
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    super.configure(request, monitor);

    addMavenBuilder(request.getProject(), monitor);
  }

}
