/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.PlatformUI;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.actions.AbstractMavenMenuCreator;
import org.maven.ide.eclipse.actions.AddDependencyAction;
import org.maven.ide.eclipse.actions.AddPluginAction;
import org.maven.ide.eclipse.actions.ChangeNatureAction;
import org.maven.ide.eclipse.actions.DisableNatureAction;
import org.maven.ide.eclipse.actions.EnableNatureAction;
import org.maven.ide.eclipse.actions.ModuleProjectWizardAction;
import org.maven.ide.eclipse.actions.OpenPomAction;
import org.maven.ide.eclipse.actions.OpenUrlAction;
import org.maven.ide.eclipse.actions.RefreshMavenModelsAction;
import org.maven.ide.eclipse.actions.SelectionUtil;
import org.maven.ide.eclipse.actions.UpdateSourcesAction;
import org.maven.ide.eclipse.core.Messages;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;


/**
 * Default Maven menu creator
 * 
 * @author Eugene Kuleshov
 */
public class DefaultMavenMenuCreator extends AbstractMavenMenuCreator {

  public void createMenu(IMenuManager mgr) {
    int selectionType = SelectionUtil.getSelectionType(selection);
    if(selectionType == SelectionUtil.UNSUPPORTED) {
      return;
    }

    if(selection.size() == 1 && selectionType == SelectionUtil.POM_FILE) {
      mgr.appendToGroup(NEW, getAction(new AddDependencyAction(), //
          AddDependencyAction.ID, "Add Dependency"));
      mgr.appendToGroup(NEW, getAction(new AddPluginAction(), AddPluginAction.ID, "Add Plugin"));
      mgr.appendToGroup(NEW, getAction(new ModuleProjectWizardAction(), //
          ModuleProjectWizardAction.ID, Messages.getString("action.moduleProjectWizardAction")));

      mgr.prependToGroup(OPEN, new Separator());
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_PROJECT), //
          OpenUrlAction.ID_PROJECT, "Open Project Page", "icons/web.gif"));
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_ISSUES), //
          OpenUrlAction.ID_ISSUES, "Open Issue Tracker"));
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_SCM), //
          OpenUrlAction.ID_SCM, "Open Source Control"));
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_CI), // 
          OpenUrlAction.ID_CI, "Open Continuous Integration"));
    }

    if(selectionType == SelectionUtil.PROJECT_WITHOUT_NATURE) {
      mgr.appendToGroup(NATURE, getAction(new EnableNatureAction(), //
          EnableNatureAction.ID, "Enable Dependency Management"));
    }

    if(selectionType == SelectionUtil.PROJECT_WITH_NATURE) {
      if(selection.size() == 1) {
        mgr.appendToGroup(NEW, getAction(new AddDependencyAction(), AddDependencyAction.ID, "Add Dependency"));
        mgr.appendToGroup(NEW, getAction(new AddPluginAction(), AddPluginAction.ID, "Add Plugin"));
        mgr.appendToGroup(NEW, getAction(new ModuleProjectWizardAction(), //
            ModuleProjectWizardAction.ID, Messages.getString("action.moduleProjectWizardAction")));
        mgr.prependToGroup(UPDATE, new Separator());
      }
     

      mgr.appendToGroup(UPDATE, getAction(new RefreshMavenModelsAction(), RefreshMavenModelsAction.ID,
          "Update Dependencies", "icons/update_dependencies.gif"));
      mgr.appendToGroup(UPDATE, getAction(new RefreshMavenModelsAction(true), RefreshMavenModelsAction.ID_SNAPSHOTS,
          "Update Snapshots"));
      mgr.appendToGroup(UPDATE, getAction(new UpdateSourcesAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()), //
          UpdateSourcesAction.ID, "Update Project Configuration", "icons/update_source_folders.gif"));

      mgr.prependToGroup(OPEN, new Separator());
      mgr.appendToGroup(OPEN, getAction(new OpenPomAction(), OpenPomAction.ID, "Open POM"));
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_PROJECT), //
          OpenUrlAction.ID_PROJECT, "Open Project Page", "icons/web.gif"));
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_ISSUES), OpenUrlAction.ID_ISSUES,
          "Open Issue Tracker"));
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_SCM), OpenUrlAction.ID_SCM,
          "Open Source Control"));
      mgr.appendToGroup(OPEN, getAction(new OpenUrlAction(OpenUrlAction.ID_CI), OpenUrlAction.ID_CI,
          "Open Continuous Integration"));

      boolean enableWorkspaceResolution = true;
      if(selection.size() == 1) {
        IProject project = SelectionUtil.getType(selection.getFirstElement(), IProject.class);
        if(project != null) {
          MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
          IMavenProjectFacade projectFacade = projectManager.create(project, new NullProgressMonitor());
          if(projectFacade != null) {
            ResolverConfiguration configuration = projectFacade.getResolverConfiguration();
            enableWorkspaceResolution = !configuration.shouldResolveWorkspaceProjects();
          }
        }
      }

      mgr.prependToGroup(NATURE, new Separator());
      if(enableWorkspaceResolution) {
        mgr.appendToGroup(NATURE, getAction(new ChangeNatureAction(ChangeNatureAction.ENABLE_WORKSPACE),
            ChangeNatureAction.ID_ENABLE_WORKSPACE, "Enable Workspace Resolution"));
      } else {
        mgr.appendToGroup(NATURE, getAction(new ChangeNatureAction(ChangeNatureAction.DISABLE_WORKSPACE),
            ChangeNatureAction.ID_DISABLE_WORKSPACE, "Disable Workspace Resolution"));
      }

      mgr.appendToGroup(NATURE, getAction(new DisableNatureAction(), //
          DisableNatureAction.ID, "Disable Dependency Management"));
    }
    
    if(selectionType == SelectionUtil.WORKING_SET) {
      mgr.appendToGroup(UPDATE, getAction(new RefreshMavenModelsAction(), RefreshMavenModelsAction.ID,
          "Update Dependencies", "icons/update_dependencies.gif"));
      mgr.appendToGroup(UPDATE, getAction(new RefreshMavenModelsAction(true), RefreshMavenModelsAction.ID_SNAPSHOTS,
          "Update Snapshots"));
      mgr.appendToGroup(UPDATE, getAction(new UpdateSourcesAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()), //
          UpdateSourcesAction.ID, "Update Project Configuration", "icons/update_source_folders.gif"));
    }
  }

}
