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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.internal.launch.MavenRuntimeClasspathProvider;


public class DisableNatureAction implements IObjectActionDelegate {
  static final String ID = "org.maven.ide.eclipse.disableAction";

  private ISelection selection;

  /*
   * (non-Javadoc)
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
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
          MavenPlugin plugin = MavenPlugin.getDefault();

          try {
            plugin.getProjectImportManager().disableMavenNature(project, new NullProgressMonitor());

            disableLaunchConfigurations(project);
          } catch(CoreException ex) {
            MavenPlugin.log(ex);
          }
        }
      }
    }
  }

  // TODO request user confirmation
  // TODO launch configs won't be updated if dependency management is changed externally 
  private void disableLaunchConfigurations(IProject project) throws CoreException {
    ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
    for(int i = 0; i < launches.length; i++ ) {
      ILaunchConfiguration config = launches[i].getLaunchConfiguration();
      if(config != null && MavenRuntimeClasspathProvider.isSupportedType(config.getType().getIdentifier())) {
        MavenRuntimeClasspathProvider.disable(config);
      }
    }
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
   *      org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {
    this.selection = selection;
  }

  /*
   * (non-Javadoc) 
   * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
   *      org.eclipse.ui.IWorkbenchPart)
   */
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

}
