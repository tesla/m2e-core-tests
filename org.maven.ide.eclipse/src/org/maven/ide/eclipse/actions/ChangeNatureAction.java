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
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.BuildPathManager;
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

  private void changeNature(IProject project) {
    try {
      ResolverConfiguration configuration = BuildPathManager.getResolverConfiguration(JavaCore.create(project));

      switch(option) {
        case ENABLE_WORKSPACE:
          configuration.setResolveWorkspaceProjects(true);
          break;
        case DISABLE_WORKSPACE:
          configuration.setResolveWorkspaceProjects(false);
          break;
        case ENABLE_MODULES:
          configuration.setIncludeModules(true);
          break;
        case DISABLE_MODULES:
          configuration.setIncludeModules(false);
          break;
      }
      
      MavenPlugin plugin = MavenPlugin.getDefault();
      plugin.getBuildpathManager().enableMavenNature(project, configuration, new NullProgressMonitor());

    } catch(CoreException ex) {
      MavenPlugin.log(ex);
    }
  }

}
