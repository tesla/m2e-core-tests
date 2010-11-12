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

package org.eclipse.m2e.editor.lifecycle.generic;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.editor.lifecycle.ILifecycleMappingEditorContribution;
import org.eclipse.m2e.editor.lifecycle.LifecycleEditorUtils;
import org.eclipse.m2e.editor.lifecycle.MojoExecutionData;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.model.edit.pom.Model;

public class GenericLifecycleMappingEditorContribution implements ILifecycleMappingEditorContribution {
  private ILifecycleMapping mapping;
  private String mappingId;
  private IMavenProjectFacade facade;
  private Model pom;
  
  public GenericLifecycleMappingEditorContribution(ILifecycleMapping mapping, String mappingId) {
    super();
    this.mapping = mapping;
    this.mappingId = mappingId;
  }

  public void setSiteData(MavenPomEditor editor, IMavenProjectFacade project, Model pom) {
    this.facade = project;
    this.pom = pom;
  }
  
  public void initializeConfiguration()  throws CoreException {
    LifecycleEditorUtils.getOrCreateLifecycleMappingPlugin(pom).getConfiguration().setStringValue("mappingId", mappingId); //$NON-NLS-1$
  }
  
  public List<AbstractProjectConfigurator> getProjectConfigurators() throws CoreException {
    return mapping.getProjectConfigurators(facade, new NullProgressMonitor());
  }
  
  public boolean canAddProjectConfigurator() throws CoreException {
    return false;
  }
  
  public boolean canEditProjectConfigurator(AbstractProjectConfigurator configurator) {
    return false;
  }
  
  public boolean canRemoveProjectConfigurator(AbstractProjectConfigurator configurator) {
    return false;
  }
  
  public void addProjectConfigurator() {
    throw new UnsupportedOperationException();
  }
  
  public void editProjectConfigurator(AbstractProjectConfigurator configurator) throws CoreException {
    throw new UnsupportedOperationException();
  }
  
  public void removeProjectConfigurator(AbstractProjectConfigurator configurator) throws CoreException {
    throw new UnsupportedOperationException();
  }
  
  public List<MojoExecutionData> getMojoExecutions() throws CoreException {
    List<MojoExecutionData> mojos = new LinkedList<MojoExecutionData>();
    
    Set<String> all = new LinkedHashSet<String>(mapping.getPotentialMojoExecutionsForBuildKind(facade, IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor()));
    Set<String> incr = new LinkedHashSet<String>(mapping.getPotentialMojoExecutionsForBuildKind(facade, IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor()));
    for(String s : all) {
      mojos.add(new MojoExecutionData(s, s, true, incr.contains(s)));
    }
    return mojos;
  }
  
  public boolean canEnableMojoExecution(MojoExecutionData execution) throws CoreException {
    return false;
  }
  
  public boolean canDisableMojoExecution(MojoExecutionData execution) throws CoreException {
    return false;
  }
  
  public void enableMojoExecution(MojoExecutionData execution) throws CoreException {
    throw new UnsupportedOperationException();
  }
  
  public void disableMojoExecution(MojoExecutionData execution) throws CoreException {
    throw new UnsupportedOperationException();
  }
  
  public boolean canSetIncremental(MojoExecutionData execution) throws CoreException {
    return false;
  }
  
  public void setIncremental(MojoExecutionData execution, boolean incremental) throws CoreException {
    throw new UnsupportedOperationException();
  }

}
