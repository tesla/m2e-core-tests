
package org.eclipse.m2e.tests.ui.dialogs;

import org.eclipse.core.resources.IProject;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.m2e.core.ui.dialogs.EditDependencyDialog;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;


public class EditDependencyDialogTest extends AbstractMavenProjectTestCase {
  public static final String GROUP_ID = "groupid"; //$NON-NLS-1$

  public static final String ARTIFACT_ID = "artifactid"; //$NON-NLS-1$

  public static final String VERSION = "1.0.0"; //$NON-NLS-1$

  public static final String CLASSIFIER = "classifier"; //$NON-NLS-1$

  public static final String TYPE = "jar"; //$NON-NLS-1$

  public static final String SCOPE = "compile"; //$NON-NLS-1$

  public static final String PATH = "path"; //$NON-NLS-1$

  @Test
  public void testDialog() throws Exception {
    final IProject project = this.createProject("dependencies", "projects/dependencies/pom.xml"); //$NON-NLS-1$ //$NON-NLS-2$

    final Dependency dependency = PomFactory.eINSTANCE.createDependency();
    dependency.setGroupId(GROUP_ID);
    dependency.setArtifactId(ARTIFACT_ID);
    dependency.setVersion(VERSION);
    dependency.setClassifier(CLASSIFIER);
    dependency.setType(TYPE);
    dependency.setScope(SCOPE);
    dependency.setSystemPath(PATH);
    dependency.setOptional(Boolean.TRUE.toString());

    Display.getDefault().syncExec(new Runnable() {

      public void run() {
        TestDialog dialog = new TestDialog(Display.getCurrent().getActiveShell(), false, null, project) {

        };
        dialog.setDependency(dependency);
        dialog.setBlockOnOpen(false);
        dialog.open();
        dialog.assertValues();
        dialog.close();
      }
    });
  }

  protected class TestDialog extends EditDependencyDialog {
    public TestDialog(Shell parent, boolean dependencyManagement, EditingDomain editingDomain, IProject project) {
      super(parent, dependencyManagement, editingDomain, project);
    }

    protected void assertValues() {
      assertEquals("Group Id", GROUP_ID, groupIdText.getText()); //$NON-NLS-1$
      assertEquals("Artifact Id", ARTIFACT_ID, artifactIdText.getText()); //$NON-NLS-1$
      assertEquals("Version", VERSION, versionText.getText()); //$NON-NLS-1$
      assertEquals("Classifier", CLASSIFIER, classifierText.getText()); //$NON-NLS-1$
      assertEquals("Type", TYPE, typeCombo.getText()); //$NON-NLS-1$
      assertEquals("Scope", SCOPE, scopeCombo.getText()); //$NON-NLS-1$
      assertEquals("System Path", PATH, systemPathText.getText()); //$NON-NLS-1$
      assertEquals("Optional", true, optionalButton.getSelection()); //$NON-NLS-1$
    }
  }
}
