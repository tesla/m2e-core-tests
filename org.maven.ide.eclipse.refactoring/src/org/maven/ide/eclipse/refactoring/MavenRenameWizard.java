package org.maven.ide.eclipse.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.maven.ide.components.pom.Model;

public class MavenRenameWizard extends RefactoringWizard {

  private static MavenRenameWizardPage page1 = new MavenRenameWizardPage();
  
  public MavenRenameWizard(IFile file) {
    super(new RenameRefactoring(file, page1), DIALOG_BASED_USER_INTERFACE);
  }

  @Override
  protected void addUserInputPages() {
    setDefaultPageTitle( getRefactoring().getName() );
    addPage( page1 );
    Model model = ((PomRefactoring) getRefactoring()).getModel();
    page1.setModel(model);
  }

}
