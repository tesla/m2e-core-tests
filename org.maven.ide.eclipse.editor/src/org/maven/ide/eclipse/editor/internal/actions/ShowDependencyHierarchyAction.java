/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.internal.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionDelegate;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.actions.OpenPomAction;
import org.maven.ide.eclipse.actions.SelectionUtil;
import org.maven.ide.eclipse.editor.pom.MavenPomEditor;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;


/**
 * @author Eugene Kuleshov
 */
public class ShowDependencyHierarchyAction extends ActionDelegate {

  public static final String ID = "org.maven.ide.eclipse.ShowDependencyHierarchy";

  private IStructuredSelection selection;

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    } else {
      this.selection = null;
    }
  }

  public void run(IAction action) {
    if(selection != null) {
      Object element = this.selection.getFirstElement();
      if(element instanceof IPackageFragmentRoot) {
        IPackageFragmentRoot fragment = (IPackageFragmentRoot) element;
        IProject project = fragment.getJavaProject().getProject();
        if(project.isAccessible() && fragment.isArchive()) {
          MavenPlugin plugin = MavenPlugin.getDefault();
          MavenProjectManager projectManager = plugin.getMavenProjectManager();
          IMavenProjectFacade projectFacade = projectManager.create(project, new NullProgressMonitor());
          if(projectFacade == null) {
            plugin.getConsole().logMessage("Unable to get Maven project for " + project.getName());
          } else {
            ArtifactKey artifactKey = SelectionUtil.getType(element, ArtifactKey.class);
            if(artifactKey != null) {
              ArtifactKey projectKey = projectFacade.getArtifactKey();
              IEditorPart editor = OpenPomAction.openEditor(projectKey.getGroupId(), //
                  projectKey.getArtifactId(), projectKey.getVersion());
              if(editor instanceof MavenPomEditor) {
                ((MavenPomEditor) editor).showDependencyHierarchy(artifactKey);
              }
            }
          }
        }
      }
    }
  }

}
