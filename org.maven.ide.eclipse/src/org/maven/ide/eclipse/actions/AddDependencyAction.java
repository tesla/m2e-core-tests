/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.actions;

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;


public class AddDependencyAction implements IObjectActionDelegate {
  private IStructuredSelection selection;

  private IWorkbenchPart targetPart;

  static final String ID = "org.maven.ide.eclipse.addDependencyAction";

  public void run(IAction action) {
    Object o = selection.iterator().next();
    IFile file;
    IProject project;
    if(o instanceof IProject) {
      project = (IProject) o;
      file = project.getFile(MavenPlugin.POM_FILE_NAME);
    } else if(o instanceof IFile) {
      file = (IFile) o;
      project = file.getProject();
    } else {
      return;
    }

    MavenPlugin plugin = MavenPlugin.getDefault();
    
    MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
        "Add Dependency", IndexManager.SEARCH_ARTIFACT, getArtifacts(file, plugin), true);
    if(dialog.open() == Window.OK) {
      IndexedArtifactFile indexedArtifactFile = (IndexedArtifactFile) dialog.getFirstResult();
      if(indexedArtifactFile != null) {
        try {
          MavenModelManager modelManager = plugin.getMavenModelManager();
          Dependency dependency = indexedArtifactFile.getDependency();
          String selectedScope = dialog.getSelectedScope();
          dependency.setScope(selectedScope);
          modelManager.addDependency(file, dependency);
        } catch(Exception ex) {
          String msg = "Can't add dependency to " + file;
          MavenPlugin.log(msg, ex);
          MessageDialog.openError(Display.getCurrent().getActiveShell(), "Add Dependency", msg);
        }
      }
    }
  }

  private Set<Artifact> getArtifacts(IFile file, MavenPlugin plugin) {
    try {
      MavenProjectManager projectManager = plugin.getMavenProjectManager();
      MavenProjectFacade projectFacade = projectManager.create(file, true, new NullProgressMonitor());
      if(projectFacade!=null) {
        return projectFacade.getMavenProjectArtifacts();
      }
    } catch(Exception ex) {
      String msg = "Can't read Maven project";
      MavenPlugin.log(msg, ex);
      plugin.getConsole().logError(msg + "; " + ex.toString());
    }
    return Collections.emptySet();
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    this.targetPart = targetPart;
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    }
  }

  protected Shell getShell() {
    Shell shell = null;
    if(targetPart != null) {
      shell = targetPart.getSite().getShell();
    }
    if(shell != null) {
      return shell;
    }

    IWorkbench workbench = MavenPlugin.getDefault().getWorkbench();
    if(workbench == null) {
      return null;
    }

    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    return window == null ? null : window.getShell();
  }

}
