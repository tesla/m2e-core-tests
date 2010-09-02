/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.actions;

import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.apache.maven.model.Dependency;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.index.IIndex;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.ui.dialogs.MavenRepositorySearchDialog;


public class AddDependencyAction extends MavenActionSupport implements IWorkbenchWindowActionDelegate {

  public static final String ID = "org.maven.ide.eclipse.addDependencyAction";

  public void run(IAction action) {
    IFile file = getPomFileFromPomEditorOrViewSelection();

    if(file == null) {
      return;
    }

    MavenPlugin plugin = MavenPlugin.getDefault();

    Set<ArtifactKey> artifacts = getArtifacts(file, plugin);
    MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), "Add Dependency", IIndex.SEARCH_ARTIFACT, artifacts, true);
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
          MavenLogger.log(msg, ex);
          MessageDialog.openError(Display.getCurrent().getActiveShell(), "Add Dependency", msg);
        }
      }
    }
  }

  public void dispose() {
  }

  public void init(IWorkbenchWindow window) {
  }
}
