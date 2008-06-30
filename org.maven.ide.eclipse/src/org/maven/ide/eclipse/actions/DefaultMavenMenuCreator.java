/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Menu;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.Messages;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.util.SelectionUtil;

/**
 * Default Maven menu creator
 *
 * @author Eugene Kuleshov
 */
public class DefaultMavenMenuCreator extends AbstractMavenMenuCreator {

  protected void createMenu(Menu menu) {
    int selectionType = SelectionUtil.getSelectionType(selection);
    if(selectionType == SelectionUtil.UNSUPPORTED) {
      return;
    }
    
    if(selection.size() == 1 && selectionType == SelectionUtil.POM_FILE) {
      addMenu(AddDependencyAction.ID, "Add Dependency", new AddDependencyAction(), menu);
      addMenu(AddPluginAction.ID, "Add Plugin", new AddPluginAction(), menu);
      addMenu(ModuleProjectWizardAction.ID, Messages.getString("action.moduleProjectWizardAction"),
          new ModuleProjectWizardAction(), menu);
    }
  
    if(selectionType == SelectionUtil.PROJECT_WITHOUT_NATURE) {
      addMenu(EnableNatureAction.ID, "Enable Dependency Management", new EnableNatureAction(), menu);
    }
  
    if(selectionType == SelectionUtil.PROJECT_WITH_NATURE) {
      if(selection.size() == 1) {
        addMenu(AddDependencyAction.ID, "Add Dependency", new AddDependencyAction(), menu);
        addMenu(AddPluginAction.ID, "Add Plugin", new AddPluginAction(), menu);
        addMenu(ModuleProjectWizardAction.ID, Messages.getString("action.moduleProjectWizardAction"),
            new ModuleProjectWizardAction(), menu);
        new Separator().fill(menu, -1);
      }
  
      addMenu(RefreshMavenModelsAction.ID, "Update Dependencies", new RefreshMavenModelsAction(), menu, "icons/update_dependencies.gif");
      addMenu(RefreshMavenModelsAction.ID_SNAPSHOTS, "Update Snapshots", new RefreshMavenModelsAction(true), menu);
      addMenu(UpdateSourcesAction.ID, "Update Project Configuration", new UpdateSourcesAction(), menu, "icons/update_source_folders.gif");
      addMenu(DownloadSourcesAction.ID_SOURCES, "Download Sources", new DownloadSourcesAction(DownloadSourcesAction.ID_SOURCES), menu);
      addMenu(DownloadSourcesAction.ID_JAVADOC, "Download JavaDoc", new DownloadSourcesAction(DownloadSourcesAction.ID_JAVADOC), menu);
      new Separator().fill(menu, -1);

      // addMenu(OpenPomAction.ID, "Open POM", new OpenPomAction(), menu);
      addMenu(OpenUrlAction.ID_PROJECT, "Open Project Page", new OpenUrlAction(OpenUrlAction.ID_PROJECT), menu, "icons/web.gif");
      addMenu(OpenUrlAction.ID_ISSUES, "Open Issue Tracker", new OpenUrlAction(OpenUrlAction.ID_ISSUES), menu);
      addMenu(OpenUrlAction.ID_SCM, "Open Source Control", new OpenUrlAction(OpenUrlAction.ID_SCM), menu);
      addMenu(OpenUrlAction.ID_CI, "Open Continuous Integration", new OpenUrlAction(OpenUrlAction.ID_CI), menu);
      new Separator().fill(menu, -1);

      boolean enableWorkspaceResolution = true;
      boolean enableNestedModules = true;
      if(selection.size() == 1) {
        IProject project = SelectionUtil.getType(selection.getFirstElement(), IProject.class);
        if(project!=null) {
          MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
          MavenProjectFacade projectFacade = projectManager.create(project, new NullProgressMonitor());
          if(projectFacade!=null) {
            ResolverConfiguration configuration = projectFacade.getResolverConfiguration();
            enableWorkspaceResolution = !configuration.shouldResolveWorkspaceProjects();
            enableNestedModules = !configuration.shouldIncludeModules();
          }
        }
      }
      
      if(enableWorkspaceResolution) {
        addMenu(ChangeNatureAction.ID_ENABLE_WORKSPACE, "Enable Workspace Resolution", 
            new ChangeNatureAction(ChangeNatureAction.ENABLE_WORKSPACE), menu);
      } else {
        addMenu(ChangeNatureAction.ID_DISABLE_WORKSPACE, "Disable Workspace Resolution", 
            new ChangeNatureAction(ChangeNatureAction.DISABLE_WORKSPACE), menu);
      }
      
      if(enableNestedModules) {
        addMenu(ChangeNatureAction.ID_ENABLE_MODULES, "Enable Nested Modules", //
            new ChangeNatureAction(ChangeNatureAction.ENABLE_MODULES), menu);
      } else {
        addMenu(ChangeNatureAction.ID_DISABLE_MODULES, "Disable Nested Modules", //
            new ChangeNatureAction(ChangeNatureAction.DISABLE_MODULES), menu);
      }
      
      addMenu(DisableNatureAction.ID, "Disable Dependency Management", new DisableNatureAction(), menu);
    }
  
    if(selectionType == SelectionUtil.JAR_FILE) {
      addMenu(DownloadSourcesAction.ID_SOURCES, "Download Sources", new DownloadSourcesAction(DownloadSourcesAction.ID_SOURCES), menu);
      addMenu(DownloadSourcesAction.ID_JAVADOC, "Download JavaDoc", new DownloadSourcesAction(DownloadSourcesAction.ID_JAVADOC), menu);
      addMenu(OpenPomAction.ID, "Open POM", new OpenPomAction(), menu);
      addMenu(OpenUrlAction.ID_PROJECT, "Open Project Page", new OpenUrlAction(OpenUrlAction.ID_PROJECT), menu, "icons/web.gif");
      addMenu(OpenUrlAction.ID_ISSUES, "Open Issue Tracker", new OpenUrlAction(OpenUrlAction.ID_ISSUES), menu);
      addMenu(OpenUrlAction.ID_SCM, "Open Source Control", new OpenUrlAction(OpenUrlAction.ID_SCM), menu);
      addMenu(OpenUrlAction.ID_CI, "Open Continuous Integration", new OpenUrlAction(OpenUrlAction.ID_CI), menu);
      new Separator().fill(menu, -1);
      addMenu(MaterializeAction.ID, //
          selection.size() == 1 ? "Import Project" : "Import Projects", //
          new MaterializeAction(), menu, "icons/import_m2_project.gif");
    }
  }
  
}