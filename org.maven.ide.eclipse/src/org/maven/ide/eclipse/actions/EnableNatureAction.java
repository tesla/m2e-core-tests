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
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
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
import org.maven.ide.eclipse.launch.MavenRuntimeClasspathProvider;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.wizards.MavenPomWizard;


public class EnableNatureAction implements IObjectActionDelegate, IExecutableExtension {

  static final String ID = "org.maven.ide.eclipse.enableNatureAction";

  static final String ID_WORKSPACE = "org.maven.ide.eclipse.enableWorkspaceResolutionAction";
  
  static final String ID_MODULES = "org.maven.ide.eclipse.enableModulesAction";
  
  private boolean includeModules = false;

  private boolean workspaceProjects = true;
  
  private boolean filterResources = false;
  
  private boolean useMavenOutputFolders = false;

  private ISelection selection;
  
  public EnableNatureAction() {
  }
  
  public EnableNatureAction(String option) {
    setInitializationData(null, null, option);
  }

  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    if(MavenPlugin.INCLUDE_MODULES.equals(data)) {
      this.includeModules = true;
    } else if(MavenPlugin.NO_WORKSPACE_PROJECTS.equals(data)) {
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
      for(Iterator it = structuredSelection.iterator(); it.hasNext();) {
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
      IFile pom = project.getFile(MavenPlugin.POM_FILE_NAME);
      if(isSingle && !pom.exists()) {
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
      configuration.setFilterResources(filterResources);
      configuration.setUseMavenOutputFolders(useMavenOutputFolders);
      configuration.setActiveProfiles("");
      
      plugin.getBuildpathManager().enableMavenNature(project, //
          configuration, //
          new NullProgressMonitor());
      
      enableLaunchLonfigurations(project);

    } catch(CoreException ex) {
      MavenPlugin.log(ex);
    }
  }

  // TODO request user confirmation
  // TODO launch configs won't be updated if dependency management is changed externally 
  private void enableLaunchLonfigurations(IProject project) throws CoreException {
    ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
    for(int i = 0; i < launches.length; i++ ) {
      ILaunchConfiguration config = launches[i].getLaunchConfiguration();
      if(config != null && MavenRuntimeClasspathProvider.isSupportedType(config.getType().getIdentifier())) {
        MavenRuntimeClasspathProvider.enable(config);
      }
    }
  }

}
