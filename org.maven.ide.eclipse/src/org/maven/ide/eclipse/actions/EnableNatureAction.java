/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.actions;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.wizards.MavenPomWizard;


public class EnableNatureAction implements IObjectActionDelegate, IExecutableExtension {

  public static final String ID = "org.maven.ide.eclipse.enableNatureAction";

  static final String ID_WORKSPACE = "org.maven.ide.eclipse.enableWorkspaceResolutionAction";
  
  static final String ID_MODULES = "org.maven.ide.eclipse.enableModulesAction";
  
  private boolean includeModules = false;

  private boolean workspaceProjects = true;
  
  private ISelection selection;
  
  public EnableNatureAction() {
  }
  
  public EnableNatureAction(String option) {
    setInitializationData(null, null, option);
  }

  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    if(IMavenConstants.INCLUDE_MODULES.equals(data)) {
      this.includeModules = true;
    } else if(IMavenConstants.NO_WORKSPACE_PROJECTS.equals(data)) {
      this.workspaceProjects = false;
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
    this.selection = selection;
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  public void run(IAction action) {
    if(selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      for(Iterator<?> it = structuredSelection.iterator(); it.hasNext();) {
        Object element = it.next();
        IProject project = null;
        if(element instanceof IProject) {
          project = (IProject) element;
        } else if(element instanceof IAdaptable) {
          project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
        }
        if(project != null) {
          enableNature(project, structuredSelection.size() == 1);
        }
      }
    }
  }

  private void enableNature(IProject project, boolean isSingle) {
    try {
      MavenPlugin plugin = MavenPlugin.getDefault();
      IFile pom = project.getFile(IMavenConstants.POM_FILE_NAME);
      if(isSingle && !pom.exists()) {
        // XXX move into AbstractProjectConfigurator and use Eclipse project settings
        IWorkbench workbench = plugin.getWorkbench();

        MavenPomWizard wizard = new MavenPomWizard();
        wizard.init(workbench, (IStructuredSelection) selection);

        Shell shell = workbench.getActiveWorkbenchWindow().getShell();
        WizardDialog wizardDialog = new WizardDialog(shell, wizard);
        wizardDialog.create();
        wizardDialog.getShell().setText("Create new POM");
        if(wizardDialog.open() == Window.CANCEL) {
          return;
        }
      }

      ResolverConfiguration configuration = new ResolverConfiguration();
      configuration.setIncludeModules(includeModules);
      configuration.setResolveWorkspaceProjects(workspaceProjects);
      configuration.setActiveProfiles("");
      
      boolean hasMavenNature = project.hasNature(IMavenConstants.NATURE_ID);
      
      IProjectConfigurationManager configurationManager = plugin.getProjectConfigurationManager();
      
      configurationManager.enableMavenNature(project, configuration, new NullProgressMonitor());
      
      if(!hasMavenNature) {
        configurationManager.updateProjectConfiguration(project, configuration, //
            plugin.getMavenRuntimeManager().getGoalOnUpdate(), new NullProgressMonitor());
      }

    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }
  }

}
