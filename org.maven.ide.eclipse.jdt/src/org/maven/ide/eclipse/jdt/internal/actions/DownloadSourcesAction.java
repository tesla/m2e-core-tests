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
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.jdt.BuildPathManager;
import org.maven.ide.eclipse.jdt.MavenJdtPlugin;


public class DownloadSourcesAction implements IObjectActionDelegate, IExecutableExtension {

  private static final String ATTR_MENU_ITEM_ID = "id";

  public static final String ID_SOURCES = "org.maven.ide.eclipse.downloadSourcesAction";

  public static final String ID_JAVADOC = "org.maven.ide.eclipse.downloadJavaDocAction";

  private IStructuredSelection selection;

  private String id;
  
  public void run(IAction action) {
    BuildPathManager buildpathManager = MavenJdtPlugin.getDefault().getBuildpathManager();
    for(Iterator<?> it = selection.iterator(); it.hasNext();) {
      Object element = it.next();
      try {
        IProject currentProject = null;
        IPath currentPath = null;
        if(element instanceof IProject) {
          IProject project = (IProject) element;
          if(project.isAccessible() && project.hasNature(IMavenConstants.NATURE_ID)) {
            currentProject = project;
          }
        } else if(element instanceof IPackageFragmentRoot) {
          IPackageFragmentRoot fragment = (IPackageFragmentRoot) element;
          IProject project = fragment.getJavaProject().getProject();
          if(project.isAccessible() && fragment.isArchive()) {
            currentProject = project;
            currentPath = fragment.getPath();
          }
        }
        
        if(currentProject!=null) {
          if(isDownloadSources()) {
            buildpathManager.downloadSources(currentProject, currentPath);
          } else {
            buildpathManager.downloadJavaDoc(currentProject, currentPath);
          }
        }
      } catch(CoreException ex) {
        MavenLogger.log(ex);
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

  private boolean isDownloadSources() {
    return ID_SOURCES.equals(id);
  }

  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    id = config.getAttribute(ATTR_MENU_ITEM_ID);
  }
  
}
