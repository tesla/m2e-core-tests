/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.refactoring.rename;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionDelegate;
import org.eclipse.ui.internal.ObjectPluginAction;
import org.maven.ide.eclipse.core.MavenLogger;


/**
 * @author Anton Kraev
 */
@SuppressWarnings("restriction")
public class RenameArtifactAction extends ActionDelegate {

  @Override
  public void init(IAction action) {
    super.init(action);
  }

  @Override
  public void run(IAction action) {
    doRun(action);
  }

  @Override
  public void runWithEvent(IAction action, Event event) {
    doRun(action);
  }

  public void doRun(IAction action) {
    Object element = ((IStructuredSelection) ((ObjectPluginAction) action).getSelection()).getFirstElement();
    if(element instanceof IFile) {
      rename((IFile) element);
    } else if (element instanceof IProject) {
      IProject project = (IProject) element;
      IFile file = project.getFile("pom.xml");
      if(file!=null) {
        rename(file);
      }
    }
  }

  private void rename(IFile file) {
    try {
      // get the model from existing file
      MavenRenameWizard wizard = new MavenRenameWizard(file);
      RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
      String titleForFailedChecks = ""; //$NON-NLS-1$
      op.run(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), titleForFailedChecks);
    } catch(Exception e) {
      MavenLogger.log("Unable to rename " + file, e);
    }
  }

}

