/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.refactoring;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.ReflectiveItemProviderAdapterFactory;
import org.eclipse.emf.edit.provider.resource.ResourceItemProviderAdapterFactory;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameJavaProjectChange;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.maven.ide.components.pom.Model;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.project.IMavenProjectFacade;

/**
 * Base class for all pom.xml refactorings in workspace
 * 
 * @author Anton Kraev
 */
public abstract class AbstractPomRefactoring extends Refactoring {

  // main file that is being refactored
  protected IFile file;
  
  // file buffer manager
  protected ITextFileBufferManager textFileBufferManager;

  // maven plugin
  MavenPlugin mavenPlugin;

  // maven model manager
  protected MavenModelManager mavenModelManager;
  
  // editing domain
  private AdapterFactoryEditingDomain editingDomain;

  public AbstractPomRefactoring(IFile file) {
    this.file = file;
    
    this.textFileBufferManager = FileBuffers.getTextFileBufferManager();
    this.mavenPlugin = MavenPlugin.getDefault();
    this.mavenModelManager = MavenPlugin.getDefault().getMavenModelManager();

    List<AdapterFactoryImpl> factories = new ArrayList<AdapterFactoryImpl>();
    factories.add(new ResourceItemProviderAdapterFactory());
    factories.add(new ReflectiveItemProviderAdapterFactory());

    ComposedAdapterFactory adapterFactory = new ComposedAdapterFactory(factories);
    BasicCommandStack commandStack = new BasicCommandStack();
    this.editingDomain = new AdapterFactoryEditingDomain(adapterFactory, //
        commandStack, new HashMap<Resource, Boolean>());
  }

  //this gets actual refactoring visitor
  public abstract PomVisitor getVisitor();
  
  @Override
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    return new RefactoringStatus();
  }

  @SuppressWarnings("deprecation")
  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    //calculate the list of affected files
    CompositeChange res = new CompositeChange("Renaming " + file.getParent().getName());
    IMavenProjectFacade[] projects = mavenPlugin.getMavenProjectManager().getProjects();
    pm.beginTask("Refactoring", projects.length);
    for(IMavenProjectFacade projectFacade : projects) {
      MavenProject prj = projectFacade.getMavenProject(null);
      org.apache.maven.model.Model effective = prj.getModel();
      pm.setTaskName("Scanning " + prj.getBasedir().getName());

      IFile file = projectFacade.getPom();
      ITextFileBuffer buffer = getBuffer(file);
      ITextFileBuffer tmpBuffer = null;
      IFile tmpFile = null;
      try {
        //create temp file
        IProject project = file.getProject();
        IPath wsPrefix = project.getWorkspace().getRoot().getLocation();
        IPath location = file.getProject().getDescription().getLocation();
        String tmpName = File.createTempFile("pom", ".xml").getName();
        if (location != null) {
          //(EMF2DOMSSE has problems with virtual resources from nested projects)
          //so temp file must be created under shell project (1st level)           
          location = location.append(tmpName);
          location = location.removeFirstSegments(wsPrefix.segmentCount()).setDevice(null).makeAbsolute();
          location = new Path(location.removeLastSegments(location.segmentCount() - 1).toString() + "/" +
              location.removeFirstSegments(location.segmentCount() - 1));
        } else {
          location = file.getParent().getFullPath().append(tmpName);
        }
        file.copy(location, true, null);
        //System.out.println("location: " + location + " for project " + project.getName());
        tmpFile = file.getWorkspace().getRoot().getFile(location);
        
        //scan it
        Model current = mavenModelManager.loadResource(tmpFile).getModel();
        tmpBuffer = getBuffer(tmpFile);
        Command command = getVisitor().applyChanges(editingDomain, file, effective, current);
        //System.out.println("resulting command: " + command);
        if (command == null)
          continue;
        if (command.canExecute()) {
          //apply changes to temp file
          editingDomain.getCommandStack().execute(command);
          //create text change comparing temp file and real file
          TextFileChange change = new ChangeCreator(file, buffer.getDocument(), tmpBuffer.getDocument(), file.getParent().getName()).createChange();
          res.add(change);
          //System.out.println("resulting change: " + change);
        }
      } catch (Exception e) {
        MavenLogger.log("Problems during refactoring", e);
      } finally {
        releaseBuffer(buffer, file);
        if (tmpFile != null && tmpFile.exists()) {
          releaseBuffer(tmpBuffer, tmpFile);
          tmpFile.delete(true, null);
        }
        pm.worked(1);
      }
    }
    
    //rename project
    String newName = getNewProjectName(); 
    if (newName != null) {
      res.add(new RenameJavaProjectChange(JavaCore.create(file.getProject()), newName, true));
    }
    
    return res;
  }

  //returns new eclipse project name or null if no change
  public String getNewProjectName() {
    return null;
  }
  
  protected ITextFileBuffer getBuffer(IFile file) throws CoreException {
    textFileBufferManager.connect(file.getLocation(), LocationKind.NORMALIZE, null);
    return textFileBufferManager.getTextFileBuffer(file.getLocation(), LocationKind.NORMALIZE); 
  }

  protected void releaseBuffer(ITextFileBuffer buffer, IFile file) throws CoreException {
    buffer.revert(null);
    textFileBufferManager.disconnect(file.getLocation(), LocationKind.NORMALIZE, null);
  }
  
  public Model getModel() {
    try {
      return mavenModelManager.loadResource(file).getModel();
    } catch(CoreException e) {
      MavenLogger.log("Cannot load model for refactoring", e);
      return null;
    }
  }

  protected IFile getFile() {
    return file;
  }
}
