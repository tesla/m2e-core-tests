/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.configurator.AbstractBuildParticipant;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;


/**
 * MissingLifecycleMapping
 * 
 * @author igor
 */
public class MissingLifecycleMapping implements ILifecycleMapping {

  /**
   * Lifecycle mapping id. Must match id of properties page defined in plugin.xml
   */
  public static final String ID = "MISSING";

  private final String missingMappingId;

  public MissingLifecycleMapping(String mappingId) {
    this.missingMappingId = mappingId;
  }

  public String getId() {
    return ID;
  }

  public String getName() {
    return "Unknown or missing lifecycle mapping";
  }

  public List<String> getPotentialMojoExecutionsForBuildKind(IMavenProjectFacade projectFacade, int kind,
      IProgressMonitor progressMonitor) {
    return Collections.emptyList();
  }

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
  }

  public void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
  }

  public List<AbstractBuildParticipant> getBuildParticipants(IMavenProjectFacade facade, IProgressMonitor monitor) {
    return Collections.emptyList();
  }

  public List<AbstractProjectConfigurator> getProjectConfigurators(IMavenProjectFacade facade, IProgressMonitor monitor) {
    return Collections.emptyList();
  }

  public String getMissingMappingId() {
    return missingMappingId;
  }

}
