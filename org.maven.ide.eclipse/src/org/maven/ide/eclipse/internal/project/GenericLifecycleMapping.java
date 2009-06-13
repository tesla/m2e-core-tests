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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.maven.ide.eclipse.project.IMavenProjectFacade;
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

  private final List<AbstractBuildParticipant> buildParticipants = new ArrayList<AbstractBuildParticipant>();

  public GenericLifecycleMapping() {
    this.buildParticipants.add(new DefaultBuildParticipant());
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
    return buildParticipants;
  }

}
