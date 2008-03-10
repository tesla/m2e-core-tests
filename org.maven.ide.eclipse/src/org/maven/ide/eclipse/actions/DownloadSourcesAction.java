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
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import org.maven.ide.eclipse.MavenPlugin;


public class DownloadSourcesAction implements IObjectActionDelegate {

  static final String ID = "org.maven.ide.eclipse.downloadSourcesAction";

  private IStructuredSelection selection;

  public void run(IAction action) {
    for(Iterator it = selection.iterator(); it.hasNext();) {
      Object element = it.next();
      try {
        if(element instanceof IProject) {
          IProject project = (IProject) element;
          if(project.isAccessible() && project.hasNature(MavenPlugin.NATURE_ID)) {
            MavenPlugin.getDefault().getBuildpathManager().downloadSources(project, null);
          }
        } else if(element instanceof IPackageFragmentRoot) {
          IPackageFragmentRoot fragment = (IPackageFragmentRoot) element;
          IProject project = fragment.getJavaProject().getProject();
          if(project.isAccessible() && fragment.isArchive()) {
            MavenPlugin.getDefault().getBuildpathManager().downloadSources(project, fragment.getPath());
          }
        }
      } catch(CoreException ex) {
        MavenPlugin.log(ex);
      }
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    } else {
      this.selection = null;
    }
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

}
