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

package org.eclipse.m2e.editor.lifecycle;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.model.edit.pom.Model;

/**
 * Represents a contribution to the lifecycle editor page.
 * 
 * @author mpoindexter
 *
 */
public interface ILifecycleMappingEditorContribution {
  /**
   * Called after this object is created to supply data about the objects being edited.
   * @param mapping
   * @param project
   * @param pom
   */
  public void setSiteData(MavenPomEditor editor, IMavenProjectFacade project, Model pom);
  
  /**
   * Called if the lifecycle mapping this contribution manages is being newly added to the project.
   * Perform any custom setup (writing plugins to pom, prompting for creation params, etc.) here.
   */
  public void initializeConfiguration() throws CoreException;
  
  /**
   * Gets the list of project configurators supplied by this lifecycle mapping.
   * @return
   */
  public List<AbstractProjectConfigurator> getProjectConfigurators() throws CoreException;
  
  /**
   * Can this mapping have a configurator added to it?
   * @return
   */
  public boolean canAddProjectConfigurator() throws CoreException;
  
  /**
   * Does the supplied configurator have an edit dialog associated with it.
   * @param configurator
   * @return
   */
  public boolean canEditProjectConfigurator(AbstractProjectConfigurator configurator) throws CoreException;
  
  /**
   * Can the supplied configurator be removed from this mapping?
   * @param configurator
   * @return
   */
  public boolean canRemoveProjectConfigurator(AbstractProjectConfigurator configurator) throws CoreException;
  
  /**
   * Add a new configurator to this mapping.  The contributor should show a dialog
   * to select from the supported configurators.
   */
  public void addProjectConfigurator() throws CoreException;
  
  /**
   * Shows the edit dialog for the given configurator.
   * @param configurator
   */
  public void editProjectConfigurator(AbstractProjectConfigurator configurator) throws CoreException;
  
  /**
   * Removes the given configurator from this mapping.
   * @param configurator
   */
  public void removeProjectConfigurator(AbstractProjectConfigurator configurator) throws CoreException;
  
  
  /**
   * Get all mojos that might run as part of this mapping.
   * @return
   */
  public List<MojoExecutionData> getMojoExecutions() throws CoreException;
  
  /**
   * Can the given mojo be enabled?
   * @param execution
   * @return
   */
  public boolean canEnableMojoExecution(MojoExecutionData execution) throws CoreException;
  /**
   * Can the given mojo be disabled?
   * @param execution
   * @return
   */
  public boolean canDisableMojoExecution(MojoExecutionData execution) throws CoreException;
  
  
  /**
   * Enable the given mojo
   * @param execution
   */
  public void enableMojoExecution(MojoExecutionData execution) throws CoreException;
  
  /**
   * Disable the given mojo
   * @param execution
   */
  public void disableMojoExecution(MojoExecutionData execution) throws CoreException;
  
  /**
   * Can the give mojo be toggled to use incremental execution.
   * @param execution
   * @return
   * @throws CoreException
   */
  public boolean canSetIncremental(MojoExecutionData execution) throws CoreException;
  
  /**
   * Set whether the given mojo will execute incrementally
   * @param execution
   * @throws CoreException
   */
  public void setIncremental(MojoExecutionData execution, boolean incremental) throws CoreException;
}
