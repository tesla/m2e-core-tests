/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wizards;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.progress.IProgressConstants;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.actions.OpenMavenConsoleAction;
import org.maven.ide.eclipse.actions.SelectionUtil;
import org.maven.ide.eclipse.project.MavenProjectInfo;
import org.maven.ide.eclipse.project.ProjectImportConfiguration;


/**
 * Maven Import Wizard
 * 
 * @author Eugene Kuleshov
 */
public class MavenImportWizard extends Wizard implements IImportWizard {

  final ProjectImportConfiguration importConfiguration;
  
  private MavenImportWizardPage page;

  private List<String> locations;

  private boolean showLocation = true;
  
  public MavenImportWizard() {
    importConfiguration = new ProjectImportConfiguration();
    setNeedsProgressMonitor(true);
    setWindowTitle("Import Maven projects");
  }

  public MavenImportWizard(ProjectImportConfiguration importConfiguration, List<String> locations) {
    this.importConfiguration = importConfiguration;
    this.locations = locations;
    this.showLocation = false;
    setNeedsProgressMonitor(true);
  }
  
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    if(locations == null || locations.isEmpty()) {
      IPath location = SelectionUtil.getSelectedLocation(selection);
      if(location != null) {
        locations = Collections.singletonList(location.toOSString());
      }
    }
    
    importConfiguration.setWorkingSet(SelectionUtil.getSelectedWorkingSet(selection));
  }

  public void addPages() {
    page = new MavenImportWizardPage(importConfiguration);
    page.setLocations(locations);
    page.setShowLocation(showLocation);
    addPage(page);
  }

  public boolean performFinish() {
    if(!page.isPageComplete()) {
      return false;
    }

    final Collection<MavenProjectInfo> projects = page.getProjects();

    final MavenPlugin plugin = MavenPlugin.getDefault();

    Job job = new WorkspaceJob("Importing Maven projects") {
      public IStatus runInWorkspace(IProgressMonitor monitor) {
        setProperty(IProgressConstants.ACTION_PROPERTY, new OpenMavenConsoleAction());
        try {
          plugin.getProjectConfigurationManager().importProjects(projects, importConfiguration, monitor);
        } catch(CoreException ex) {
          plugin.getConsole().logError("Projects imported with errors");
          return ex.getStatus();
        }

        return Status.OK_STATUS;
      }
    };
    job.setRule(plugin.getProjectConfigurationManager().getRule(new IResource[] {ResourcesPlugin.getWorkspace().getRoot()}));
    job.schedule();

    return true;
  }

}
