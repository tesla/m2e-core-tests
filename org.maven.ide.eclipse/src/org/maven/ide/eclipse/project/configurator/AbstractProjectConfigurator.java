/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project.configurator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.embedder.MavenEmbedder;

import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.project.IMavenProjectChangedListener;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.MavenProjectManager;


/**
 * Used to configure maven projects.
 *
 * XXX investigate using existing maven infrastructure to read plugin configuration
 * org.apache.maven.plugin.DefaultPluginManager.getConfiguredMojo(MavenSession, Xpp3Dom, MavenProject, boolean, MojoExecution, List) 
 * 
 * @author Igor Fedorenko
 */
public abstract class AbstractProjectConfigurator implements IExecutableExtension, IMavenProjectChangedListener {

  public static final String ATTR_ID = "id";
  
  public static final String ATTR_PRIORITY = "priority";

  public static final String ATTR_NAME = "name";
  
  public static final String ATTR_CLASS = "class";
  
  private int priority;
  private String id;
  private String name;

  protected MavenProjectManager projectManager;
  protected MavenRuntimeManager runtimeManager;
  protected MavenConsole console;

  
  public void setProjectManager(MavenProjectManager projectManager) {
    this.projectManager = projectManager;
  }
  
  public void setRuntimeManager(MavenRuntimeManager runtimeManager) {
    this.runtimeManager = runtimeManager;
  }
  
  public void setConsole(MavenConsole console) {
    this.console = console;
  }
  
  
  /**
   * Configures Eclipse project passed in ProjectConfigurationRequest, using information
   * from Maven project and other configuration request parameters
   * 
   * <p><i>Should be implemented by subclass</i> 
   * 
   * @param embedder a Maven embedder instance that can be reused in current project configuration session
   * @param request a project configuration request
   * @param monitor a progress monitor
   */
  public abstract void configure(MavenEmbedder embedder, ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException;

  /**
   * Removes Maven specific configuration from the project passed in ProjectConfigurationRequest
   * 
   * @param embedder a Maven embedder instance that can be reused in current project configuration session
   * @param request a project un-configuration request
   * @param monitor a progress monitor
   */
  @SuppressWarnings("unused")
  public void unconfigure(MavenEmbedder embedder, ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
  }

  /**
   * Updates project configuration according project changes. 
   * 
   * <p><i>Can be overwritten by subclass</i>
   * 
   * @param event a project change event
   * @param monitor a progress monitor
   */
  @SuppressWarnings("unused")
  protected void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
  }

  // IMavenProjectChangedListener
  
  public final void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    for (int i = 0; i < events.length; i++) {
      try {
        mavenProjectChanged(events[i], monitor);
      } catch(CoreException ex) {
        MavenLogger.log(ex);
      }
    }
  }

  public int getPriority() {
    return priority;
  }
  
  public String getId() {
    return id;
  }
  
  public String getName() {
    return name;
  }
  
  // IExecutableExtension  
  
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    this.id = config.getAttribute(ATTR_ID);
    this.name = config.getAttribute(ATTR_NAME);
    String priorityString = config.getAttribute(ATTR_PRIORITY);
    try {
      priority = Integer.parseInt(priorityString);
    } catch (Exception ex) {
      priority = Integer.MAX_VALUE;
    }
  }

  protected void addNature(IProject project, String natureId, IProgressMonitor monitor) throws CoreException {
    if (!project.hasNature(natureId)) {
      IProjectDescription description = project.getDescription();
      String[] prevNatures = description.getNatureIds();
      String[] newNatures = new String[prevNatures.length + 1];
      System.arraycopy(prevNatures, 0, newNatures, 1, prevNatures.length);
      newNatures[0] = natureId;
      description.setNatureIds(newNatures);
      project.setDescription(description, monitor);
    }
  }

  @Override
  public String toString() {
    return id + ":" + name + "(" + priority + ")";
  }
  
}
