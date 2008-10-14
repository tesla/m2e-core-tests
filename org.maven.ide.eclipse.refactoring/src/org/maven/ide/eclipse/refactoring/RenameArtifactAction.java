package org.maven.ide.eclipse.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionDelegate;
import org.eclipse.ui.internal.ObjectPluginAction;

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
    try {
      //get the model from existing file
      IFile file = (IFile) ((IStructuredSelection) ((ObjectPluginAction) action).getSelection()).getFirstElement();
      MavenRenameWizard wizard = new MavenRenameWizard(file);
      RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation( wizard );
      String titleForFailedChecks = ""; //$NON-NLS-1$
      op.run( PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), titleForFailedChecks );
    } catch(Exception e) {
      e.printStackTrace();
    }
  }


}
