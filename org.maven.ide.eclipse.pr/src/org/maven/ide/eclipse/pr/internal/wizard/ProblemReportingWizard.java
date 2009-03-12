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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import org.codehaus.plexus.swizzle.IssueSubmissionRequest;
import org.codehaus.plexus.swizzle.IssueSubmissionResult;
import org.codehaus.plexus.swizzle.IssueSubmitter;
import org.codehaus.plexus.swizzle.JiraIssueSubmitter;
import org.codehaus.plexus.swizzle.jira.authentication.DefaultAuthenticationSource;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;


/**
 * @author Eugene Kuleshov
 */
@SuppressWarnings("restriction")
public class ProblemReportingWizard extends Wizard implements IImportWizard {

  private IStructuredSelection selection;

  private static final String HOSTNAME = "jiratest.sonatype.org/";

  private static final String URL = "https://" + HOSTNAME;

  private static final String USERNAME = "sonatype_problem_reporting";

  private static final String PASSWORD = "sonatype_problem_reporting";

  private static final String PROJECT = "PR";

  protected static String TITLE = "Problem Reporting";

  //private ProblemReportingSelectionPage selectionPage;

  private ProblemDescriptionPage descriptionPage;

  public ProblemReportingWizard() {
    setWindowTitle("Reporting Problem");
  }

  @Override
  public void addPages() {
    descriptionPage = new ProblemDescriptionPage(selection);
    addPage(descriptionPage);
    //selectionPage = new ProblemReportingSelectionPage();
    //addPage(selectionPage);
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.selection = selection;
  }

  public boolean performFinish() {
    final Set<Data> dataSet = new HashSet<Data>();//selectionPage.getDataSet();
    dataSet.addAll(EnumSet.allOf(Data.class));
    dataSet.remove(Data.MAVEN_POM_FILES);
//    if(locationFile.exists()) {
//      if(!MessageDialog.openQuestion(getShell(), "File already exists", //
//          "File " + location + " already exists.\nDo you want to overwrite?")) {
//        return false;
//      }
//      if(!locationFile.delete()) {
//        MavenLogger.log("Can't delete file " + location, null);
//      }
//    }

    new Job("Gathering Information") {
      protected IStatus run(IProgressMonitor monitor) {
        File locationFile = null;
        try {
          String tmpPath = ResourcesPlugin.getPlugin().getStateLocation().toOSString();
          File tmpDir = new File(tmpPath);
          String location = File.createTempFile("bundle", ".zip", tmpDir).getAbsolutePath();//final String location = selectionPage.getLocation();
          locationFile = new File(location);
          IStatus status = saveData(location, dataSet, monitor);
          if(status.isOK()) {
            //showMessage("The problem reporting bundle is saved to " + location);
            String username = MavenPlugin.getDefault().getMavenRuntimeManager().getJiraUsername();
            String password = MavenPlugin.getDefault().getMavenRuntimeManager().getJiraPassword();
            if(username == null || username.trim().equals("")) {
              username = USERNAME;
              password = PASSWORD;
            }

            IssueSubmitter is = new JiraIssueSubmitter(URL, new DefaultAuthenticationSource(username, password));

            IssueSubmissionRequest r = new IssueSubmissionRequest();
            r.setProjectId(PROJECT);
            r.setSummary(descriptionPage.getProblemSummary());
            r.setDescription(descriptionPage.getProblemDescription());
            r.setAssignee(username);
            r.setReporter(username);
            r.setProblemReportBundle(locationFile);
            r.setEnvironment(getEnvironment());

            IssueSubmissionResult res = is.submitIssue(r);

            showHyperlink("Successfully submitted issue to:", res.getIssueUrl());
          } else {
            MavenLogger.log(new CoreException(status));
            showError(status.getMessage());
          }
        } catch(Exception ex) {
          MavenLogger.log("Failed to generate problem report", ex);
          showError(ex.getMessage());
        } finally {
          if(locationFile != null && locationFile.exists()) {
            locationFile.delete();
          }
        }

        return Status.OK_STATUS;
      }
      

      private void showError(final String msg) {
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            MessageDialog.openError(Display.getCurrent().getActiveShell(), //
                TITLE, msg);
          }
        });
      }

      private void showHyperlink(final String msg, final String url) {
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            HyperlinkDialog dialog = new HyperlinkDialog(Display.getCurrent().getActiveShell(), TITLE, null, msg,
                MessageDialog.INFORMATION, new String[] {IDialogConstants.OK_LABEL}, 0, url);
            dialog.open();
          }
        });
      }
    }.schedule();

    return true;
  }

  private static final String [] PROPERTIES=new String[]{"java.vendor","java.version","os.name","os.version","os.arch","osgi.arch","osgi.nl"};
  
  private String getEnvironment() {
    StringBuffer sb = new StringBuffer();
    
    String sep = System.getProperty("line.separator");
    
    sb.append("M2E Version: ").append(getBundleVersion(MavenPlugin.getDefault().getBundle())).append(sep);
    sb.append("Eclipse Version: ").append(getBundleVersion(ResourcesPlugin.getPlugin().getBundle())).append(sep);
    
    for(int i = 0; i < PROPERTIES.length; i++ ) {
      sb.append(PROPERTIES[i]).append(": ").append(System.getProperty(PROPERTIES[i])).append(sep);
    }
    
    
    return sb.toString();
  }
  
  private String getBundleVersion(Bundle bundle) {
    String version = (String) bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
    Version v = org.osgi.framework.Version.parseVersion(version);
    return v.toString();
  }
  
  IStatus saveData(String location, Set<Data> dataSet, IProgressMonitor monitor) throws IOException {
    if(!location.endsWith(".zip")) {
      location = location + ".zip";
    }

    Set<IProject> projects = new LinkedHashSet<IProject>();
    for(Iterator<?> i = descriptionPage.getSelectedProjects().iterator(); i.hasNext();) {
      Object o = i.next();
      if(o instanceof JavaProject) {
        projects.add(((JavaProject) o).getProject());
      }
      if(o instanceof IProject) {
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
  }

}
