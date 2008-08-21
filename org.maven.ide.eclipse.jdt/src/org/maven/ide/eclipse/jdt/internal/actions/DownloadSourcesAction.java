/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt.internal.actions;

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;

import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.jdt.BuildPathManager;
import org.maven.ide.eclipse.jdt.MavenJdtPlugin;


public class DownloadSourcesAction implements IObjectActionDelegate {

  public static final String ID_SOURCES = "org.maven.ide.eclipse.downloadSourcesAction";

  public static final String ID_JAVADOC = "org.maven.ide.eclipse.downloadJavaDocAction";
  
  private IStructuredSelection selection;

  private String id;
  
  public DownloadSourcesAction(String id) {
    this.id = id;
  }

  public void run(IAction action) {
    if(selection != null) {
      BuildPathManager buildpathManager = MavenJdtPlugin.getDefault().getBuildpathManager();
      for(Iterator<?> it = selection.iterator(); it.hasNext();) {
        Object element = it.next();
        try {
          if(element instanceof IProject) {
            IProject project = (IProject) element;
            download(buildpathManager, project, null);
          } else if(element instanceof IPackageFragmentRoot) {
            IPackageFragmentRoot fragment = (IPackageFragmentRoot) element;
            IProject project = fragment.getJavaProject().getProject();
            if(project.isAccessible() && fragment.isArchive()) {
              download(buildpathManager, project, fragment.getPath());
            }
          } else if(element instanceof IWorkingSet) {
            IWorkingSet workingSet = (IWorkingSet) element;
            for(IAdaptable adaptable : workingSet.getElements()) {
              IProject project = (IProject) adaptable.getAdapter(IProject.class);
              download(buildpathManager, project, null);
            }
          }
        } catch(CoreException ex) {
          MavenLogger.log(ex);
        }
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

  private void download(BuildPathManager buildpathManager, IProject project, IPath currentPath) throws CoreException {
    if(project!=null && project.isAccessible() && project.hasNature(IMavenConstants.NATURE_ID)) {
      if(ID_SOURCES.equals(id)) {
        buildpathManager.downloadSources(project, currentPath);
      } else {
        buildpathManager.downloadJavaDoc(project, currentPath);
      }
    }
  }

}
