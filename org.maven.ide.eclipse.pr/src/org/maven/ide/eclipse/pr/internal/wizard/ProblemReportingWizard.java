/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr.internal.wizard;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.pr.internal.data.ArchiveTarget;
import org.maven.ide.eclipse.pr.internal.data.Data;
import org.maven.ide.eclipse.pr.internal.data.DataGatherer;

/**
 *
 * @author Eugene Kuleshov
 */
public class ProblemReportingWizard extends Wizard implements IImportWizard {

  private IStructuredSelection selection;
  
  private ProblemReportingSelectionPage selectionPage;

  private ProblemDescriptionPage descriptionPage;
  
  public ProblemReportingWizard() {
    setWindowTitle("Reporting Problem");
  }
  
  @Override
  public void addPages() {
    descriptionPage = new ProblemDescriptionPage();
    addPage(descriptionPage);
    selectionPage = new ProblemReportingSelectionPage();
    addPage(selectionPage);
  }
  
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.selection = selection;
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.jface.wizard.Wizard#performFinish()
   */
  public boolean performFinish() {
    final Set<Data> dataSet = selectionPage.getDataSet();
    final String location = selectionPage.getLocation();
    File locationFile = new File(location);
    if(locationFile.exists()) {
      if(!MessageDialog.openQuestion(getShell(), "File already exists", //
          "File " + location + " already exists.\nDo you want to overwrite?")) {
        return false;
      }
      if(!locationFile.delete()) {
        MavenLogger.log("Can't delete file " + location, null);
      }
    }
    
    new Job("Gathering Information") {
      protected IStatus run(IProgressMonitor monitor) {
        try {
          IStatus status = saveData(location, dataSet, monitor);
          if(status.isOK()) {
            showMessage(location);
          } else {
            MavenLogger.log(new CoreException(status));
            showError();
          }
          
        } catch(IOException ex) {
          MavenLogger.log("Failed generate errorto report issue", ex);
          showError();
        }
        
        return Status.OK_STATUS;
      }

      private void showError() {
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            MessageDialog.openError(Display.getCurrent().getActiveShell(), //
                "Problem Reporting", "Reporting bundle generated with errors.\n"
                    + "See Eclipse error log for more details");
          }
        });
      }
      
      private void showMessage(final String fileName) {
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            MessageDialog.openInformation(Display.getCurrent().getActiveShell(), //
                "Problem Reporting", "The problem reporting bundle is saved to " + fileName);
          }
        });
      }
    }.schedule();
    
    return true;
  }

  IStatus saveData(String location, Set<Data> dataSet, IProgressMonitor monitor) throws IOException {
    if(!location.endsWith(".zip")) {
      location = location + ".zip";
    }

    Set<IProject> projects = new LinkedHashSet<IProject>(); 
    for(Iterator<?> i = selection.iterator(); i.hasNext();) {
      Object o = i.next();
      if (o instanceof IProject) {
        projects.add((IProject) o);
      }
    }
    
    MavenPlugin mavenPlugin = MavenPlugin.getDefault();
    DataGatherer gatherer = new DataGatherer(mavenPlugin.getMavenRuntimeManager(), //
        mavenPlugin.getMavenProjectManager(), mavenPlugin.getConsole(), //
        ResourcesPlugin.getWorkspace(), projects);

    ZipOutputStream zos = null;
    try {
      zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(location))); 
      gatherer.gather(new ArchiveTarget(zos), dataSet, monitor);
      zos.flush();
    } finally {
      IOUtil.close(zos);
    }
      
    return gatherer.getStatus();

//      DataDeliverer deliverer = new DataDeliverer(outputFile);
//      MessageDialog.openInformation(targetPart.getSite().getWorkbenchWindow().getShell(),
//          "Maven Integration: Problem Determination", "Problem diagnostics information has been written to "
//              + outputFile.getPath());
  }
  
}
