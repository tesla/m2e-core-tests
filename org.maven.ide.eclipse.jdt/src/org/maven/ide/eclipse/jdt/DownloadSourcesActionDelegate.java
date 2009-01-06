/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/package org.maven.ide.eclipse.jdt;

import java.io.File;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.index.IndexedArtifactFile;

/**
 * 
 * DownloadSourcesActionDelegate
 *
 * @author Anton Kraev
 */

@SuppressWarnings("restriction")
public class DownloadSourcesActionDelegate implements IEditorActionDelegate {

  public void setActiveEditor(IAction action, IEditorPart part) {
    if (MavenPlugin.getDefault().getMavenRuntimeManager().isOffline()) {
      return;
    }

    if (part != null) {
      try {
        IClassFileEditorInput input = (IClassFileEditorInput) part.getEditorInput();
        IJavaElement element = input.getClassFile();
        while (element.getParent() != null) {
          element = element.getParent();
          if (element instanceof JarPackageFragmentRoot) {
            JarPackageFragmentRoot root = (JarPackageFragmentRoot) element;
            File f = root.getPath().toFile();
            IndexedArtifactFile af = MavenPlugin.getDefault().getIndexManager().identify(f);
            if (af != null) {
              ArtifactKey a = af.getArtifactKey();
              IJavaProject project = (IJavaProject) root.getParent();
              MavenPlugin.getDefault().getMavenProjectManager().downloadSources(project.getProject(), 
                  project.getPath(), a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getClassifier(), true, false);
              break;
            }
          }
        }
      } catch(Exception ex) {
        MavenLogger.log("Cannot initiate source download", ex);
      }
    }
  }

  public void run(IAction action) {
    // no need to do anything
  }

  public void selectionChanged(IAction action, ISelection selection) {
    // no need to do anything
  }

}
