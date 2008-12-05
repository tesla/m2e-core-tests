package org.maven.ide.eclipse.editor.pom;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.refactoring.exclude.MavenExcludeWizard;

/**
 * This action is intended to be used an in action bar of a Viewer to exclude dependency
 * 
 * @author Anton Kraev
 */
public class DependencyExcludeAction extends Action {
  Artifact selectedArtifact;
  private MavenPomEditor editor;

  public DependencyExcludeAction(final List<Viewer> viewers, MavenPomEditor editor) {
    super("Exclude", MavenEditorImages.EXCLUDE);
    setViewers(viewers);
    this.editor = editor;
  }

  private void setViewers(final List<Viewer> viewers) {
    setEnabled(false);
    for (final Viewer viewer: viewers) {
      viewer.addSelectionChangedListener(new ISelectionChangedListener() {
        public void selectionChanged(SelectionChangedEvent event) {
          IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
          if(!selection.isEmpty()) {
            List<?> sel = selection.toList();
            if (sel.size() == 1)
              if (sel.get(0) instanceof Artifact) {
                selectedArtifact = (Artifact) sel.get(0);
                setEnabled(true);
                return;
              } else if (sel.get(0) instanceof DependencyNode) {
                DependencyNode node = (DependencyNode) sel.get(0);
                selectedArtifact = node.getArtifact();
                setEnabled(true);
                return;
              }
          }
          setEnabled(false);
        }
        
      });
    }
  }

  public DependencyExcludeAction(final Viewer viewer, MavenPomEditor editor) {
    super("Exclude", MavenEditorImages.EXCLUDE);
    List<Viewer> viewers = new ArrayList<Viewer>();
    viewers.add(viewer);
    setViewers(viewers);
    this.editor = editor;
  }

  public void run() {
    if (selectedArtifact != null) {
      Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
      MavenExcludeWizard wizard = new MavenExcludeWizard(editor.getPomFile(), 
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
}
