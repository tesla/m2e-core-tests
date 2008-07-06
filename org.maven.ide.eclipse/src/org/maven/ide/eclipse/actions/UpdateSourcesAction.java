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
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.project.MavenProjectFacade;


public class UpdateSourcesAction implements IObjectActionDelegate {
  private ISelection selection;
  static final String ID = "org.maven.ide.eclipse.updateSourcesAction";

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  public void selectionChanged(IAction action, ISelection selection) {
    this.selection = selection;
  }

  public void run(IAction action) {
    final Set<IProject> projects = new LinkedHashSet<IProject>();
    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
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

    final MavenPlugin plugin = MavenPlugin.getDefault();
    WorkspaceJob job = new WorkspaceJob("Updating Maven Configuration") {
      public IStatus runInWorkspace(IProgressMonitor monitor) {
        monitor.beginTask(getName(), projects.size());
        
        MultiStatus status = null;
        for(IProject project : projects) {
          if (monitor.isCanceled()) {
            throw new OperationCanceledException();
          }

          monitor.subTask(project.getName());
          MavenProjectFacade projectFacade = plugin.getMavenProjectManager().create(project, monitor);
          if(projectFacade != null) {
            try {
              plugin.getProjectConfigurationManager().updateProjectConfiguration(project, //
                  projectFacade.getResolverConfiguration(), //
                  plugin.getMavenRuntimeManager().getGoalOnUpdate(), //
                  new SubProgressMonitor(monitor, 1));
            } catch(CoreException ex) {
              if (status == null) {
                status = new MultiStatus(IMavenConstants.PLUGIN_ID, IStatus.ERROR, "Can't update maven configuration", null);
              }
              status.add(ex.getStatus());
            }
          }
        }

        return status != null? status: Status.OK_STATUS;
      }
    };
    job.setRule(plugin.getProjectConfigurationManager().getRule());
    job.schedule();
  }

}
