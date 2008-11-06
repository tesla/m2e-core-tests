/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.refactoring;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.ReflectiveItemProviderAdapterFactory;
import org.eclipse.emf.edit.provider.resource.ResourceItemProviderAdapterFactory;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.maven.ide.components.pom.Model;
import org.maven.ide.eclipse.MavenPlugin;
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

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    //calculate the list of affected files
    CompositeChange res = new CompositeChange("Renaming " + file.getParent().getName());
    for(IMavenProjectFacade projectFacade : mavenPlugin.getMavenProjectManager().getProjects()) {
      IFile file = projectFacade.getPom();
      IFile tmpFile = null;
      try {
        ITextFileBuffer buffer = getBuffer(file);
        ByteArrayInputStream contents = new ByteArrayInputStream(buffer.getDocument().get().getBytes());
        //create temp file
        String tmpName = file.getName();
        int pos = tmpName.lastIndexOf('.');
        if (pos > 0) {
          tmpName = tmpName.substring(0, pos) + "_refactored" + tmpName.substring(pos); 
        } else {
          tmpName = "_" + tmpName;
        }
        tmpFile = file.getParent().getFile(new Path(tmpName));
        if (tmpFile.exists())
          tmpFile.setContents(contents, 0, pm);
        else
          tmpFile.create(contents, true, pm);
        
        //scan it
        Model current = mavenModelManager.loadResource(tmpFile).getModel();
        List<EObject> affected = getVisitor().scanModel(tmpFile, current);
        if (!affected.isEmpty()) {
          //apply changes to temp file
          ITextFileBuffer tmpBuffer = getBuffer(tmpFile);
          Command command = getVisitor().applyModel(editingDomain, affected);
          editingDomain.getCommandStack().execute(command);
          
          //create text change comparing temp file and real file
          TextFileChange change = new ChangeCreator(file, buffer.getDocument(), tmpBuffer.getDocument(), file.getParent().getName()).createChange();
          res.add(change);
        }
      } finally {
        releaseBuffer(file);
        if (tmpFile != null && tmpFile.exists()) {
          releaseBuffer(tmpFile);
          tmpFile.delete(true, pm);
        }
      }
    }
    return res;
  }

  protected ITextFileBuffer getBuffer(IFile file) throws CoreException {
    textFileBufferManager.connect(file.getLocation(), LocationKind.NORMALIZE, null);
    return textFileBufferManager.getTextFileBuffer(file.getLocation(), LocationKind.NORMALIZE); 
  }

  protected void releaseBuffer(IFile file) throws CoreException {
    textFileBufferManager.disconnect(file.getLocation(), LocationKind.NORMALIZE, null);
  }
  
  // TODO modelManager.loadResource(file) should be cached!!!
  public Model getModel() {
    try {
      return mavenModelManager.loadResource(file).getModel();
    } catch(CoreException e) {
      return null;
    }
  }

  protected IFile getFile() {
    return file;
  }
}
