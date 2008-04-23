/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.actions;

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.project.IProjectImportManager;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.MavenUpdateRequest;
import org.maven.ide.eclipse.project.ResolverConfiguration;


public class ChangeNatureAction implements IObjectActionDelegate {

  static final String ID_ENABLE_WORKSPACE = "org.maven.ide.eclipse.enableWorkspaceResolutionAction";
  
  static final String ID_ENABLE_MODULES = "org.maven.ide.eclipse.enableModulesAction";
  
  static final String ID_DISABLE_WORKSPACE = "org.maven.ide.eclipse.disableWorkspaceResolutionAction";
  
  static final String ID_DISABLE_MODULES = "org.maven.ide.eclipse.disableModulesAction";

  public static final int ENABLE_WORKSPACE = 1;
  public static final int DISABLE_WORKSPACE = 2;
  public static final int ENABLE_MODULES = 3;
  public static final int DISABLE_MODULES = 4;
  
  private ISelection selection;
  
  private int option;
  
  public ChangeNatureAction(int option) {
    this.option = option;
  }

  public void selectionChanged(IAction action, ISelection selection) {
    this.selection = selection;
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  public void run(IAction action) {
    if(selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      for(Iterator it = structuredSelection.iterator(); it.hasNext();) {
        Object element = it.next();
        IProject project = null;
        if(element instanceof IProject) {
          project = (IProject) element;
        } else if(element instanceof IAdaptable) {
          project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
        }
        if(project != null) {
          changeNature(project);
        }
      }
    }
  }

  private void changeNature(final IProject project) {
    MavenPlugin plugin = MavenPlugin.getDefault();
    MavenProjectManager projectManager = plugin.getMavenProjectManager();
    
    final ResolverConfiguration configuration = projectManager.getResolverConfiguration(project);
    
    boolean updateSourceFolders = false;

    switch(option) {
      case ENABLE_WORKSPACE:
        configuration.setResolveWorkspaceProjects(true);
        break;
      case DISABLE_WORKSPACE:
        configuration.setResolveWorkspaceProjects(false);
        break;
      case ENABLE_MODULES:
        configuration.setIncludeModules(true);
        updateSourceFolders = true;
        break;
      case DISABLE_MODULES:
        configuration.setIncludeModules(false);
        updateSourceFolders = true;
        break;
    }

    projectManager.setResolverConfiguration(project, configuration);
    
    new UpdateJob(project, updateSourceFolders, configuration, //
        plugin.getMavenRuntimeManager(), //
        plugin.getProjectImportManager(), //
        projectManager).schedule();
  }

  static class UpdateJob extends Job {
    private final IProject project;
    private final boolean updateSourceFolders;
    private final ResolverConfiguration configuration;
    private final MavenRuntimeManager runtimeManager;
    private final IProjectImportManager importManager;
    private final MavenProjectManager projectManager;
    
    public UpdateJob(IProject project, boolean updateSourceFolders, ResolverConfiguration configuration, //
        MavenRuntimeManager runtimeManager, IProjectImportManager importManager, MavenProjectManager projectManager) {
      super("Updating " + project.getName() + " sources");
      this.project = project;
      this.updateSourceFolders = updateSourceFolders;
      this.configuration = configuration;
      this.runtimeManager = runtimeManager;
      this.importManager = importManager;
      this.projectManager = projectManager;
    }
    
    protected IStatus run(IProgressMonitor monitor) {
      if(updateSourceFolders) {
        try {
          importManager.updateSourceFolders(project, //
              configuration, runtimeManager.getGoalOnUpdate(), monitor);
        } catch(CoreException ex) {
          return ex.getStatus();
        }
      }
      
      boolean offline = runtimeManager.isOffline();
      boolean updateSnapshots = false;
      projectManager.refresh(new MavenUpdateRequest(project, offline, updateSnapshots));
      
      return Status.OK_STATUS;
    }
  }

}
