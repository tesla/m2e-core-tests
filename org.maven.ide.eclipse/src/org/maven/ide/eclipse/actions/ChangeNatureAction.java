/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.actions;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.MavenUpdateRequest;
import org.maven.ide.eclipse.project.ResolverConfiguration;


public class ChangeNatureAction implements IObjectActionDelegate {

  public static final String ID_ENABLE_WORKSPACE = "org.maven.ide.eclipse.enableWorkspaceResolutionAction";
  
  public static final String ID_ENABLE_MODULES = "org.maven.ide.eclipse.enableModulesAction";
  
  public static final String ID_DISABLE_WORKSPACE = "org.maven.ide.eclipse.disableWorkspaceResolutionAction";
  
  public static final String ID_DISABLE_MODULES = "org.maven.ide.eclipse.disableModulesAction";

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
      Set<IProject> projects = new LinkedHashSet<IProject>();
      for(Iterator<?> it = structuredSelection.iterator(); it.hasNext();) {
        Object element = it.next();
        IProject project = null;
        if(element instanceof IProject) {
          project = (IProject) element;
        } else if(element instanceof IAdaptable) {
          project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
        }
        if(project != null) {
          projects.add(project);
        }
      }

      new UpdateJob(projects, option).schedule();
    }
  }

  static class UpdateJob extends WorkspaceJob {
    private final Set<IProject> projects;
    private final int option;

    private final MavenRuntimeManager runtimeManager;
    private final IProjectConfigurationManager importManager;
    private final MavenProjectManager projectManager;
    
    public UpdateJob(Set<IProject> projects, int option) {
      super("Changing nature");
      this.projects = projects;
      this.option = option;

      MavenPlugin plugin = MavenPlugin.getDefault();
      this.runtimeManager = plugin.getMavenRuntimeManager();
      this.importManager = plugin.getProjectConfigurationManager();
      this.projectManager = plugin.getMavenProjectManager();

      setRule(importManager.getRule());
    }
    
    public IStatus runInWorkspace(IProgressMonitor monitor) {
      MultiStatus status = null;
      for(IProject project : projects) {
        if (monitor.isCanceled()) {
          throw new OperationCanceledException();
        }

        monitor.subTask(project.getName());

        try {
          changeNature(project, monitor);
        } catch (CoreException ex) {
          if (status == null) {
            status = new MultiStatus(IMavenConstants.PLUGIN_ID, IStatus.ERROR, "Can't change nature", null);
          }
          status.add(ex.getStatus());
        }
      }

      boolean offline = runtimeManager.isOffline();
      boolean updateSnapshots = false;
      projectManager.refresh(new MavenUpdateRequest(projects.toArray(new IProject[projects.size()]), //
          offline, updateSnapshots));
      
      return status != null? status: Status.OK_STATUS;
    }

    private void changeNature(final IProject project, IProgressMonitor monitor) throws CoreException {
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
      
      if (updateSourceFolders) {
        importManager.updateProjectConfiguration(project, //
            configuration, runtimeManager.getGoalOnUpdate(), monitor);
      }
    }
  }

}
