package org.maven.ide.eclipse.refactoring.exclude;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;

/**
 * This action is intended to be used in popup menus
 * 
 * @author Anton Kraev
 */
public class DependencyExcludeAction implements IEditorActionDelegate {
  Artifact selectedArtifact;
  private IFile pomFile;

  public void run(IAction action) {
    if (selectedArtifact != null) {
      Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
      MavenExcludeWizard wizard = new MavenExcludeWizard(pomFile, 
          selectedArtifact.getGroupId(), selectedArtifact.getArtifactId());
      RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
      String titleForFailedChecks = ""; //$NON-NLS-1$
      try {
        op.run(shell, titleForFailedChecks);
      } catch(InterruptedException e) {
        // XXX
      }
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
    //get file
    IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    if (part != null && part.getEditorInput() instanceof IFileEditorInput) {
      IFileEditorInput input = (IFileEditorInput) part.getEditorInput();
      pomFile = input.getFile();
    }

    //get artifact
    if (selection instanceof IStructuredSelection) {
      IStructuredSelection ssel = (IStructuredSelection) selection;
      if(!ssel.isEmpty()) {
        List<?> sel = ssel.toList();
        if (sel.size() == 1) {
          Object selected = sel.get(0);
          if (selected instanceof Artifact) {
            selectedArtifact = (Artifact) selected;
            return;
          } else if (selected instanceof IPackageFragmentRoot) {
            //IPackageFragmentRoot root = (IPackageFragmentRoot) selected;
          } else if (selected.getClass().getName().endsWith("DependencyNode")) {
            Object dependencyNode = selected;
            try {
              Method m = dependencyNode.getClass().getMethod("getArtifact", new Class[] {});
              selectedArtifact = (Artifact) m.invoke(dependencyNode, new Object[] {});
            } catch(Exception ex) {
              //ignore
            }
            return;
          }
        }
      }
    }
  }

  public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    //ignore
  }
}
