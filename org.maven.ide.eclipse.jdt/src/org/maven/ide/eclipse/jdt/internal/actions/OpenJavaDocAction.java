/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt.internal.actions;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionDelegate;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.actions.SelectionUtil;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.IMaven;

/**
 * Open JavaDoc action
 *
 * @author Eugene Kuleshov
 */
public class OpenJavaDocAction extends ActionDelegate {

  public static final String ID = "org.maven.ide.eclipse.openJavaDocAction";
  
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
      try {
        final ArtifactKey ak = SelectionUtil.getArtifactKey(this.selection.getFirstElement());
        if(ak == null) {
          openDialog("Unable to identify Maven artifact");
          return;
        }

        new Job("Opening JavaDoc for " + ak) {
          protected IStatus run(IProgressMonitor monitor) {
            openJavaDoc(ak.getGroupId(), ak.getArtifactId(), ak.getVersion(), monitor);
            return Status.OK_STATUS;
          }
        }.schedule();
        
      } catch(CoreException ex) {
        MavenLogger.log(ex);
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
          public void run() {
            MessageDialog.openInformation(Display.getDefault().getActiveShell(), //
                "Open JavaDoc", "Unable to read Maven project");
          }
        });
      }
    }
  }

  protected void openJavaDoc(String groupId, String artifactId, String version, IProgressMonitor monitor) {
    final String name = groupId + ":" + artifactId + ":" + version + ":javadoc";

    try {
      IMaven maven = MavenPlugin.getDefault().getMaven();

      List<ArtifactRepository> artifactRepositories = maven.getArtifactRepositories();
      
      Artifact artifact = maven.resolve(groupId, artifactId, version, "javadoc", "javadoc", artifactRepositories, monitor);
      
      final File file = artifact.getFile();
      if(file == null) {
        openDialog("Can't download JavaDoc for " + name);
        return;
      }

      PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
        public void run() {
          try {
            String url = "jar:" + file.toURI().toString() + "!/index.html";
            URL helpUrl = PlatformUI.getWorkbench().getHelpSystem().resolve(url, true);
            
            IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
            IWebBrowser browser = browserSupport.createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR, //
                name, name, name);
            browser.openURL(helpUrl);
          } catch(PartInitException ex) {
            MavenLogger.log(ex);
          }
        }
      });
      
    } catch(CoreException ex) {
      MavenLogger.log("Can't download JavaDoc for " + name, ex);
      openDialog("Can't download JavaDoc for " + name);
      // TODO search index and offer to select other version
    }    

  }

  private static void openDialog(final String msg) {
    PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
      public void run() {
        MessageDialog.openInformation(Display.getDefault().getActiveShell(), //
            "Show JavaDoc", msg);
      }
    });
  }
  
}
