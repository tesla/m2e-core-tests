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
        final IProject p = project;
        new Job("Updating " + project.getName() + " Sources") {
          protected IStatus run(IProgressMonitor monitor) {
            MavenPlugin plugin = MavenPlugin.getDefault();
            MavenProjectFacade projectFacade = plugin.getMavenProjectManager().create(p, monitor);
            if(projectFacade != null) {
              try {
                plugin.getProjectImportManager().updateSourceFolders(p, //
                    projectFacade.getResolverConfiguration(), //
                    plugin.getMavenRuntimeManager().getGoalOnUpdate(), monitor);
              } catch(CoreException ex) {
                return ex.getStatus();
              }
            }

            return Status.OK_STATUS;
          }
        }.schedule();
      }
    }
  }

}
