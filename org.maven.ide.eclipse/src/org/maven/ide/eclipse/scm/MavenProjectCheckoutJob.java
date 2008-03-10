/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.scm;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import org.apache.maven.model.Model;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.project.BuildPathManager;
import org.maven.ide.eclipse.project.LocalProjectScanner;
import org.maven.ide.eclipse.project.MavenProjectInfo;
import org.maven.ide.eclipse.project.ProjectImportConfiguration;
import org.maven.ide.eclipse.wizards.MavenImportWizard;


/**
 * Maven project checkout Job
 * 
 * @author Eugene Kuleshov
 */
public abstract class MavenProjectCheckoutJob extends WorkspaceJob {
  final MavenCheckoutOperation operation = new MavenCheckoutOperation();

  final ProjectImportConfiguration configuration;
  
  boolean checkoutAllProjects;

  Collection projects;

  public MavenProjectCheckoutJob(ProjectImportConfiguration importConfiguration, boolean checkoutAllProjects) {
    super("Checking out Maven projects");
    this.configuration = importConfiguration;
    this.checkoutAllProjects = checkoutAllProjects;

    addJobChangeListener(new CheckoutJobChangeListener());
  }
  
  public void setLocation(File location) {
    operation.setLocation(location);
  }
  
  protected abstract Collection getProjects(IProgressMonitor monitor) throws InterruptedException;
  
  
  // WorkspaceJob
  
  public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
    try {
      operation.setMavenProjects(getProjects(monitor));
      operation.run(monitor);

      LocalProjectScanner scanner = new LocalProjectScanner(operation.getLocations());
      scanner.run(monitor);

      MavenPlugin plugin = MavenPlugin.getDefault();
      BuildPathManager buildpathManager = plugin.getBuildpathManager();
      MavenModelManager modelManager = plugin.getMavenModelManager();

      boolean includeModules = configuration.getResolverConfiguration().shouldIncludeModules();
      this.projects = buildpathManager.collectProjects(scanner.getProjects(), includeModules);

      if(checkoutAllProjects) {
        // check if there any project name conflicts 
        IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
        for(Iterator it = projects.iterator(); it.hasNext();) {
          MavenProjectInfo projectInfo = (MavenProjectInfo) it.next();
          Model model = projectInfo.getModel();
          if(model == null) {
            model = modelManager.readMavenModel(projectInfo.getPomFile());
            projectInfo.setModel(model);
          }

          String projectName = configuration.getProjectName(model);
          IProject project = workspace.getProject(projectName);
          if(project.exists()) {
            checkoutAllProjects = false;
            break;
          }
        }
      }

      return Status.OK_STATUS;

    } catch(InterruptedException ex) {
      return Status.CANCEL_STATUS;

    } catch(InvocationTargetException ex) {
      Throwable cause = ex.getTargetException() == null ? ex : ex.getTargetException();
      if(cause instanceof CoreException) {
        return ((CoreException) cause).getStatus();
      }
      return new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, 0, cause.toString(), cause);
    }
  }

  /**
   * Checkout job listener
   */
  final class CheckoutJobChangeListener extends JobChangeAdapter {

    public void done(IJobChangeEvent event) {
      IStatus result = event.getResult();
      if(result.getSeverity() == IStatus.CANCEL) {
        return;
      } else if(!result.isOK()) {
        // XXX report errors
        return;
      }

      if(projects.isEmpty()) {
        MavenPlugin.getDefault().getConsole().logError("No Maven projects to import");
        return;
      }
      
      if(checkoutAllProjects) {
        new WorkspaceJob("Importing Maven projects") {
          public IStatus runInWorkspace(IProgressMonitor monitor) {
            MavenPlugin plugin = MavenPlugin.getDefault();

            BuildPathManager buildpathManager = plugin.getBuildpathManager();

            Set projectSet = buildpathManager.collectProjects(projects, //
                configuration.getResolverConfiguration().shouldIncludeModules());

            IStatus status = buildpathManager.importProjects(projectSet, configuration, monitor);
            if(!status.isOK()) {
              plugin.getConsole().logError("Projects imported with errors");
            }

            return status;
          }
        }.schedule();

      } else {
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            MavenImportWizard wizard = new MavenImportWizard(configuration, operation.getLocations());

            WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
            int res = dialog.open();
            if(res == Window.CANCEL) {
              // XXX offer to delete checkout folder
            }
          }
        });
      }
    }
  }
  
}
