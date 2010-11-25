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

package org.eclipse.m2e.jdt.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.project.CustomizableLifecycleMapping;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;


public class JarLifecycleMapping extends CustomizableLifecycleMapping implements ILifecycleMapping {
  public static final String EXTENSION_ID = "jar"; //$NON-NLS-1$

  public JarLifecycleMapping() {
  }

  public List<AbstractProjectConfigurator> getProjectConfigurators(IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor)
      throws CoreException {
    List<AbstractProjectConfigurator> configurators = new ArrayList<AbstractProjectConfigurator>();

    for(AbstractProjectConfigurator configurator : getProjectConfigurators()) {
      // Add JavaProjectConfigurator by default
      if(configurator instanceof JavaProjectConfigurator) {
        configurators.add(configurator);
        continue;
      }

      // Ignore project configurators that require explicit enablement/configuration
      if(configurator.requiresExplicitEnablement()) {
        continue;
      }

      // Does this configurator support any of the mojo executions in the maven build plan?
      MavenExecutionPlan mavenExecutionPlan = mavenProjectFacade.getExecutionPlan(monitor);
      List<MojoExecution> allMojoExecutions = mavenExecutionPlan.getMojoExecutions();
      for(MojoExecution mojoExecution : allMojoExecutions) {
        if(!isInterestingPhase(mojoExecution.getLifecyclePhase())) {
          continue;
        }
        if(configurator.isSupportedExecution(mojoExecution)) {
          configurators.add(configurator);
          break;
        }
      }
    }

    // Any project configurators configured explicitly?
    MavenProject mavenProject = mavenProjectFacade.getMavenProject(monitor);
    Plugin plugin = mavenProject.getPlugin("org.eclipse.m2e:lifecycle-mapping"); //$NON-NLS-1$
    if(plugin != null) {
      Xpp3Dom config = (Xpp3Dom) plugin.getConfiguration();
      if(config != null) {
        List<AbstractProjectConfigurator> configuredProjectConfigurators = super.getProjectConfigurators(
            mavenProjectFacade, monitor);
        for(AbstractProjectConfigurator configuredProjectConfigurator : configuredProjectConfigurators) {
          if(!configurators.contains(configuredProjectConfigurator)) {
            configurators.add(configuredProjectConfigurator);
          }
        }
      }
    }

    return configurators;
  }
}
