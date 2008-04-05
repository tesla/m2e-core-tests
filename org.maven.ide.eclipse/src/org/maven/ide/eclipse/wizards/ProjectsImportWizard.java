package org.maven.ide.eclipse.wizards;

import org.eclipse.jface.wizard.Wizard;

import org.maven.ide.eclipse.MavenPlugin;

/**
 * WizardExtension
 *
 * @author Eugene Kuleshov
 */
public class ProjectsImportWizard extends Wizard {
  private final String location;

  private ProjectsImportPage mainPage;

  public ProjectsImportWizard(String location) {
    this.location = location;
    setWindowTitle("Import");
    setDefaultPageImageDescriptor(MavenPlugin.getImageDescriptor("icons/import_project.png")); //$NON-NLS-1$
  }

  public void addPages() {
    mainPage = new ProjectsImportPage(this.location);
    addPage(mainPage);
  }

  public boolean performCancel() {
    mainPage.performCancel();
    return true;
  }

  public boolean performFinish() {
    return mainPage.createProjects();
  }
}