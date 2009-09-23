/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Resolver configuration holder.
 * 
 * TODO need a better name, this configures all aspects of maven project in eclipse, 
 *      not just dependency resolution.
 *
 * @author Eugene Kuleshov
 */
public class ResolverConfiguration implements Serializable {
  private static final long serialVersionUID = 1258510761534886581L;

  public static final String DEFAULT_FILTERING_GOALS = "process-resources resources:testResources";
  public static final String DEFAULT_FULL_BUILD_GOALS = "process-test-resources";

  private boolean includeModules = false;
  private boolean resolveWorkspaceProjects = true;
  private String activeProfiles = "";
  private String resourceFilteringGoals = DEFAULT_FILTERING_GOALS;
  private String fullBuildGoals = DEFAULT_FULL_BUILD_GOALS;

  /**
   * Skip execution of Maven compiler plugin
   */
  private boolean skipCompiler = true;

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

  /**
   * @deprecated see {@link #setIncludeModules(boolean)}
   */
  public boolean shouldIncludeModules() {
    return this.includeModules;
  }

  public boolean shouldResolveWorkspaceProjects() {
    return this.resolveWorkspaceProjects;
  }

  public String getActiveProfiles() {
    return this.activeProfiles;
  }
  
  public List<String> getActiveProfileList() {
    if (activeProfiles.trim().length() > 0) {
      return Arrays.asList(activeProfiles.split("[,\\s\\|]"));
    }
    return new ArrayList<String>();
  }

  public void setResolveWorkspaceProjects(boolean resolveWorkspaceProjects) {
    this.resolveWorkspaceProjects = resolveWorkspaceProjects;
  }
  
  /**
   * @deprecated ability to map multiple Maven modules to a single workspace project
   *    is unsupported and will likely be removed in future m2e versions
   */
  public void setIncludeModules(boolean includeModules) {
    this.includeModules = includeModules;
  }
  
  public void setActiveProfiles(String activeProfiles) {
    this.activeProfiles = activeProfiles;
  }
  
  /**
   * @deprecated only applies to GenericLifecycleMapping 
   */
  public String getResourceFilteringGoals() {
    return resourceFilteringGoals;
  }
  
  /**
   * @deprecated only applies to GenericLifecycleMapping 
   */
  public void setResourceFilteringGoals(String resourceFilteringGoals) {
    this.resourceFilteringGoals = resourceFilteringGoals;
  }

  /**
   * @deprecated only applies to GenericLifecycleMapping 
   */
  public String getFullBuildGoals() {
    return fullBuildGoals;
  }

  /**
   * @deprecated only applies to GenericLifecycleMapping 
   */
  public void setFullBuildGoals(String fullBuildGoals) {
    this.fullBuildGoals = fullBuildGoals;
  }

  /**
   * @deprecated only applies to GenericLifecycleMapping 
   */
  public boolean isSkipCompiler() {
    return this.skipCompiler;
  }
  
  /**
   * @deprecated only applies to GenericLifecycleMapping 
   */
  public void setSkipCompiler(boolean skipCompiler) {
    this.skipCompiler = skipCompiler;
  }
  
  public String getLifecycleMapping() {
    return null;
  }
}
