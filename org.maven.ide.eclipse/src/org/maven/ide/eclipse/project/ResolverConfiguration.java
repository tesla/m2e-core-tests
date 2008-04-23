/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project;

import java.util.Arrays;
import java.util.List;

/**
 * Resolver configuration holder
 *
 * @author Eugene Kuleshov
 */
public class ResolverConfiguration {

  public static final String DEFAULT_FILTERING_GOALS = "process-resources resources:testResources";

  private boolean includeModules = false;
  private boolean resolveWorkspaceProjects = true;
  private String activeProfiles = "";
  private boolean filterResources = false;
  private boolean useMavenOutputFolders = false;
  private String resourceFilteringGoals = DEFAULT_FILTERING_GOALS;

//  public ResolverConfiguration() {
//  }
//  
//  private ResolverConfiguration(boolean includeModules, boolean resolveWorkspaceProjects, String activeProfiles, boolean filterResources, boolean useMavenOutputFolders) {
//    this.includeModules = includeModules;
//    this.resolveWorkspaceProjects = resolveWorkspaceProjects;
//    this.activeProfiles = activeProfiles;
//    this.filterResources = filterResources;
//    this.useMavenOutputFolders = useMavenOutputFolders;
//  }

  public boolean shouldIncludeModules() {
    return this.includeModules;
  }
  
  public boolean shouldResolveWorkspaceProjects() {
    return this.resolveWorkspaceProjects;
  }

  public String getActiveProfiles() {
    return this.activeProfiles;
  }
  
  public List getActiveProfileList() {
    return Arrays.asList(activeProfiles.split(",\\s\\|"));
  }

  public boolean shouldFilterResources() {
    return this.filterResources;
  }
  
  public boolean shouldUseMavenOutputFolders() {
    return useMavenOutputFolders;
  }

  public void setResolveWorkspaceProjects(boolean resolveWorkspaceProjects) {
    this.resolveWorkspaceProjects = resolveWorkspaceProjects;
  }
  
  public void setIncludeModules(boolean includeModules) {
    this.includeModules = includeModules;
  }
  
  public void setActiveProfiles(String activeProfiles) {
    this.activeProfiles = activeProfiles;
  }
  
  public void setFilterResources(boolean filterResources) {
    this.filterResources = filterResources;
  }

  public void setUseMavenOutputFolders(boolean useMavenOutputFolders) {
    this.useMavenOutputFolders = useMavenOutputFolders;
  }

  public String getResourceFilteringGoals() {
    return resourceFilteringGoals;
  }
  
  public void setResourceFilteringGoals(String resourceFilteringGoals) {
    this.resourceFilteringGoals = resourceFilteringGoals;
  }
}
