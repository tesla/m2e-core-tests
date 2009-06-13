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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.project.IMavenMarkerManager;
import org.maven.ide.eclipse.project.IMavenProjectChangedListener;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.MavenProjectManager;


/**
 * Used to configure maven projects.
 *
 * @author Igor Fedorenko
 */
public abstract class AbstractProjectConfigurator implements IExecutableExtension, IMavenProjectChangedListener {

  public static final String ATTR_ID = "id";
  
  public static final String ATTR_PRIORITY = "priority";
  
  public static final String ATTR_GENERIC = "generic";

  public static final String ATTR_NAME = "name";
  
  public static final String ATTR_CLASS = "class";
  
  private int priority;
  private String id;
  private String name;
  private boolean generic;

  protected MavenProjectManager projectManager;
  protected IMavenConfiguration mavenConfiguration;
  protected IMavenMarkerManager markerManager; 
  protected MavenConsole console;
  protected IMaven maven = MavenPlugin.lookup(IMaven.class);

  
  public void setProjectManager(MavenProjectManager projectManager) {
    this.projectManager = projectManager;
  }
  
  public void setMavenConfiguration(IMavenConfiguration mavenConfiguration) {
    this.mavenConfiguration = mavenConfiguration;
  }

  public void setMarkerManager(IMavenMarkerManager markerManager) {
    this.markerManager = markerManager;
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
  public abstract void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException;

  /**
   * Removes Maven specific configuration from the project passed in ProjectConfigurationRequest
   * 
   * @param embedder a Maven embedder instance that can be reused in current project configuration session
   * @param request a project un-configuration request
   * @param monitor a progress monitor
   */
  @SuppressWarnings("unused")
  public void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
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

  public boolean isGeneric() {
    return generic;
  }

  // IExecutableExtension  
  
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    this.id = config.getAttribute(ATTR_ID);
    this.name = config.getAttribute(ATTR_NAME);
    this.generic = parseBoolean(config.getAttribute(ATTR_GENERIC), true);
    String priorityString = config.getAttribute(ATTR_PRIORITY);
    try {
      priority = Integer.parseInt(priorityString);
    } catch (Exception ex) {
      priority = Integer.MAX_VALUE;
    }
  }

  private boolean parseBoolean(String value, boolean defaultValue) {
    return value != null? Boolean.parseBoolean(value): defaultValue;
  }

  // TODO move to a helper
  public static void addNature(IProject project, String natureId, IProgressMonitor monitor) throws CoreException {
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

  protected <T> T getParameterValue(MavenSession session, MojoExecution execution, String parameter, Class<T> asType) throws CoreException {
    return maven.getMojoParameterValue(session, execution, parameter, asType);
  }

  protected void assertHasNature(IProject project, String natureId) throws CoreException {
    if (project.getNature(natureId) == null) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Project does not have required natute " + natureId, null));
    }
  }

  @Override
  public String toString() {
    return id + ":" + name + "(" + priority + ")";
  }

  public AbstractBuildParticipant getBuildParticipant(MojoExecution execution) {
    return null;
  }
}
