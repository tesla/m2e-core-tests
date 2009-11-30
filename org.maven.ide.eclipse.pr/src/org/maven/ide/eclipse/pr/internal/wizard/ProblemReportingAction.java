/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr.internal.wizard;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.maven.ide.eclipse.MavenPlugin;


/**
 * Gather information about user .
 */
public class ProblemReportingAction implements IWorkbenchWindowActionDelegate, IObjectActionDelegate {

  public static final String ID = "org.maven.ide.eclipse.pr.action.ProblemReportingAction";
  private IStructuredSelection selection;

  public void dispose() {
  }

  public void init(IWorkbenchWindow window) {
  }

  public void run(IAction action) {
    ProblemReportingWizard wizard = new ProblemReportingWizard();
    wizard.init(null, selection);
    WizardDialog dialog = new WizardDialog(getShell(), wizard);
    dialog.open();
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    } else {
      this.selection = null;
    }
  }

  protected Shell getShell() {
    IWorkbench workbench = MavenPlugin.getDefault().getWorkbench();
    if(workbench == null) {
      return null;
    }
    
    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    return window == null ? null : window.getShell();
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    //don't need
  }
}
