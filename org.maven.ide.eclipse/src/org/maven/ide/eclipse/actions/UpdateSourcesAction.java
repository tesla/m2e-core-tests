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
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.progress.IProgressConstants;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.project.IMavenProjectFacade;


public class UpdateSourcesAction implements IObjectActionDelegate {
  
  public static final String ID = "org.maven.ide.eclipse.updateSourcesAction";
  
  private IStructuredSelection selection;

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    } else {
      this.selection = null;
    }
  }

  public void run(IAction action) {
    final Set<IProject> projects = getProjects();
    final MavenPlugin plugin = MavenPlugin.getDefault();
    WorkspaceJob job = new WorkspaceJob("Updating Maven Configuration") {
      public IStatus runInWorkspace(IProgressMonitor monitor) {
        setProperty(IProgressConstants.ACTION_PROPERTY, new OpenMavenConsoleAction());
        monitor.beginTask(getName(), projects.size());
        
        MavenConsole console = plugin.getConsole();
        
        long l1 = System.currentTimeMillis();
        console.logMessage("Update started");
        
        MultiStatus status = null;
        for(IProject project : projects) {
          if (monitor.isCanceled()) {
            throw new OperationCanceledException();
          }

          monitor.subTask(project.getName());
          IMavenProjectFacade projectFacade = plugin.getMavenProjectManager().create(project, monitor);
          if(projectFacade != null) {
            try {
              plugin.getProjectConfigurationManager().updateProjectConfiguration(project, //
                  projectFacade.getResolverConfiguration(), //
                  plugin.getMavenRuntimeManager().getGoalOnUpdate(), //
                  new SubProgressMonitor(monitor, 1));
            } catch(CoreException ex) {
              if (status == null) {
                status = new MultiStatus(IMavenConstants.PLUGIN_ID, IStatus.ERROR, //
                    "Can't update Maven configuration", null);
              }
              status.add(ex.getStatus());
            }
          }
        }
        
        long l2 = System.currentTimeMillis();
        console.logMessage("Update completed: " + ((l2 - l1) / 1000) + " sec");

        return status != null? status: Status.OK_STATUS;
      }
    };
    job.setRule(plugin.getProjectConfigurationManager().getRule(projects.toArray(new IProject[]{})));
    job.schedule();
  }

  private Set<IProject> getProjects() {
    Set<IProject> projects = new LinkedHashSet<IProject>();
    if(selection != null) {
      for(Iterator<?> it = selection.iterator(); it.hasNext();) {
        Object element = it.next();
        if(element instanceof IProject) {
          projects.add((IProject) element);
        } else if(element instanceof IWorkingSet) {
          IWorkingSet workingSet = (IWorkingSet) element;
          for(IAdaptable adaptable : workingSet.getElements()) {
            IProject project = (IProject) adaptable.getAdapter(IProject.class);
            try {
              if(project != null && project.isAccessible() && project.hasNature(IMavenConstants.NATURE_ID)) {
                projects.add(project);
              }
            } catch(CoreException ex) {
              MavenLogger.log(ex);
            }
          }
        } else if(element instanceof IAdaptable) {
          IProject project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
          if(project != null) {
            projects.add(project);
          }
        }
      }
    }
    return projects;
  }

}
