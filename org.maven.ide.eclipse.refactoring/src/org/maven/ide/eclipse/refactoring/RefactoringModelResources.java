/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.refactoring;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.PropertyPair;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.IMavenProjectFacade;

/**
 * @author Anton Kraev
 * 
 * This class manages all refactoring-related resources for a particular maven project
 *
 */
public class RefactoringModelResources {
  protected IFile pomFile;
  protected IFile tmpFile;
  protected ITextFileBuffer pomBuffer;
  protected ITextFileBuffer tmpBuffer;
  protected Model tmpModel;
  protected org.apache.maven.model.Model effective;
  protected ITextFileBufferManager textFileBufferManager;
  protected Map<String, PropertyInfo> properties;
  protected MavenProject project;
  protected CompoundCommand command;
  
  public static class PropertyInfo {
    protected PropertyPair pair;
    protected RefactoringModelResources resource;
    protected Command newValue;
    
    public Command getNewValue() {
      return newValue;
    }

    public void setNewValue(Command newValue) {
      this.newValue = newValue;
    }
    
    public PropertyPair getPair() {
      return pair;
    }

    public void setPair(PropertyPair pair) {
      this.pair = pair;
    }

    public RefactoringModelResources getResource() {
      return resource;
    }

    public void setResource(RefactoringModelResources resource) {
      this.resource = resource;
    }
  }

  @SuppressWarnings("deprecation")
  public RefactoringModelResources(IMavenProjectFacade projectFacade) throws CoreException, IOException {
    textFileBufferManager = FileBuffers.getTextFileBufferManager();
    project = projectFacade.getMavenProject(null);
    effective = project.getModel();
    pomFile = projectFacade.getPom();
    pomBuffer = getBuffer(pomFile);

    //create temp file
    IProject project = pomFile.getProject();
    IPath wsPrefix = project.getWorkspace().getRoot().getLocation();
    IPath location = pomFile.getProject().getDescription().getLocation();
    String tmpName = File.createTempFile("pom", ".xml").getName();
    if (location != null) {
      //(EMF2DOMSSE has problems with virtual resources from nested projects)
      //so temp file must be created under shell project (1st level)           
      location = location.append(tmpName);
      location = location.removeFirstSegments(wsPrefix.segmentCount()).setDevice(null).makeAbsolute();
      location = new Path(location.removeLastSegments(location.segmentCount() - 1).toString() + "/" +
          location.removeFirstSegments(location.segmentCount() - 1));
    } else {
      location = pomFile.getParent().getFullPath().append(tmpName);
    }
    pomFile.copy(location, true, null);
    tmpFile = pomFile.getWorkspace().getRoot().getFile(location);
    
    tmpModel = loadModel(tmpFile);
    tmpBuffer = getBuffer(tmpFile);
  }

  public static Model loadModel(IFile file) throws CoreException {
    return MavenPlugin.getDefault().getMavenModelManager().loadResource(file).getModel();
  }
  
  public CompoundCommand getCommand() {
    return command;
  }

  public void setCommand(CompoundCommand command) {
    this.command = command;
  }

  public IFile getPomFile() {
    return pomFile;
  }

  public IFile getTmpFile() {
    return tmpFile;
  }

  public ITextFileBuffer getPomBuffer() {
    return pomBuffer;
  }

  public ITextFileBuffer getTmpBuffer() {
    return tmpBuffer;
  }

  public Model getTmpModel() {
    return tmpModel;
  }

  public org.apache.maven.model.Model getEffective() {
    return effective;
  }

  public MavenProject getProject() {
    return project;
  }

  public Map<String, PropertyInfo> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, PropertyInfo> properties) {
    this.properties = properties;
  }

  public void releaseAllResources() throws CoreException {
    releaseBuffer(pomBuffer, pomFile);
    if (tmpFile != null && tmpFile.exists()) {
      releaseBuffer(tmpBuffer, tmpFile);
      tmpFile.delete(true, null);
    }
  }

  protected ITextFileBuffer getBuffer(IFile file) throws CoreException {
    textFileBufferManager.connect(file.getLocation(), LocationKind.NORMALIZE, null);
    return textFileBufferManager.getTextFileBuffer(file.getLocation(), LocationKind.NORMALIZE); 
  }

  protected void releaseBuffer(ITextFileBuffer buffer, IFile file) throws CoreException {
    buffer.revert(null);
    textFileBufferManager.disconnect(file.getLocation(), LocationKind.NORMALIZE, null);
  }

  public String getName() {
    return pomFile.getProject().getName();
  }
}
