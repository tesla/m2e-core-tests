package org.maven.ide.eclipse.refactoring.exclude;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer.RequiredProjectWrapper;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.actions.SelectionUtil;
import org.maven.ide.eclipse.embedder.ArtifactKey;

/**
 * This action is intended to be used in popup menus
 * 
 * @author Anton Kraev
 */
@SuppressWarnings("restriction")
public class DependencyExcludeAction implements IEditorActionDelegate {
  private Artifact artifact;
  private IFile file;
  private ArtifactKey key;
  private Model model;

  public void run(IAction action) {
    if ((artifact != null || key != null || model != null) && file != null) {
      String artifactId;
      String groupId;
      if (artifact != null) {
        groupId = artifact.getGroupId();
        artifactId = artifact.getArtifactId(); 
      } else if (key != null) {
        groupId = key.getGroupId();
        artifactId = key.getArtifactId(); 
      } else {
        groupId = model.getGroupId();
        artifactId = model.getArtifactId(); 
      }
      Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
      MavenExcludeWizard wizard = new MavenExcludeWizard(file, groupId, artifactId);
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
    //init
    file = null;
    key = null;
    artifact = null;
    model = null;
    
    //get artifact
    if (selection instanceof IStructuredSelection) {
      IStructuredSelection ssel = (IStructuredSelection) selection;
      if(!ssel.isEmpty()) {
        List<?> sel = ssel.toList();
        if (sel.size() == 1) {
          Object selected = sel.get(0);
          if (selected instanceof Artifact) {
            artifact = (Artifact) selected;
            file = getFileFromEditor();
          } else if (selected instanceof DependencyNode) {
            artifact = ((DependencyNode) selected).getArtifact();
            file = getFileFromEditor();
          } else if (selected instanceof RequiredProjectWrapper) {
            RequiredProjectWrapper w = (RequiredProjectWrapper) selected;
            IFile pomFile = w.getProject().getProject().getFile("pom.xml");
            try {
              model = MavenPlugin.getDefault().getMavenModelManager().readMavenModel(pomFile);
            } catch(CoreException ex) {
              ex.printStackTrace();
            }
            file = getFileFromProject(w.getParentClassPathContainer().getJavaProject());
          } else {
            key = SelectionUtil.getType(selected, ArtifactKey.class);
            if (selected instanceof IJavaElement) {
              IJavaElement el = (IJavaElement) selected;
              file = getFileFromProject(el.getParent().getJavaProject());
            }
          }
        }
      }
    }
    
    if ((artifact != null || key != null || model != null) && file != null) {
      action.setEnabled(true);
    } else {
      action.setEnabled(false);
    }
  }

  /**
   * @param javaProject
   */
  private IFile getFileFromProject(IJavaProject javaProject) {
    return javaProject.getProject().getFile("pom.xml");
  }

  private IFile getFileFromEditor() {
    IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    if (part != null && part.getEditorInput() instanceof IFileEditorInput) {
      IFileEditorInput input = (IFileEditorInput) part.getEditorInput();
      return input.getFile();
    }
    return null;
  }

  public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    //ignore
  }
}
