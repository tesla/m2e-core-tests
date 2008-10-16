/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.actions;

import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.ui.dialogs.MavenRepositorySearchDialog;


public class AddPluginAction implements IObjectActionDelegate {
  public static final String ID = "org.maven.ide.eclipse.addPluginAction";
  
  private IStructuredSelection selection;

  private IWorkbenchPart targetPart;

  public void run(IAction action) {
    Object o = selection.iterator().next();
    IFile file;
    if(o instanceof IProject) {
      file = ((IProject) o).getFile(IMavenConstants.POM_FILE_NAME);
    } else if(o instanceof IFile) {
      file = (IFile) o;
    } else {
      return;
    }

    MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
        "Add Plugin", IndexManager.SEARCH_PLUGIN, Collections.<ArtifactKey>emptySet());
    if(dialog.open() == Window.OK) {
      final IndexedArtifactFile indexedArtifactFile = (IndexedArtifactFile) dialog.getFirstResult();
      if(indexedArtifactFile != null) {
        try {
          MavenModelManager modelManager = MavenPlugin.getDefault().getMavenModelManager();
          modelManager.updateProject(file, new MavenModelManager.PluginAdder( //
              indexedArtifactFile.group, //
              indexedArtifactFile.artifact, //
              indexedArtifactFile.version)); 
        } catch(Exception ex) {
          MavenLogger.log("Can't add dependency to " + file, ex);
        }
      }
    }
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
