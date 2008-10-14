package org.maven.ide.eclipse.refactoring;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.components.pom.Model;

public class MavenRenameWizardPage extends UserInputWizardPage {

  private String groupId = "";
  private String artifactId = "";
  private String version = "";
  private Text groupIdText;
  private Text artifactIdText;
  private Text versionText;

  protected MavenRenameWizardPage() {
    super("MavenRenameWizardPage");
    setDescription("Here you can rename artifact");
  }

  public void setVisible(boolean visible) {
  	if (visible) {
      groupIdText.setText(groupId);
      artifactIdText.setText(artifactId);
      versionText.setText(version);
      ModifyListener listener = new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          getWizard().getContainer().updateButtons();
          groupId = groupIdText.getText();
          artifactId = artifactIdText.getText();
          version = versionText.getText();
        }
      };
      
      groupIdText.addModifyListener(listener);
      artifactIdText.addModifyListener(listener);
      versionText.addModifyListener(listener);
  	}
  }
  
  public void setModel(Model model) {
    this.groupId = model.getGroupId();
    this.artifactId = model.getArtifactId();
    this.version = model.getVersion();
  }
  
  @Override
	public boolean isPageComplete() {
    return !groupId.equals(groupIdText.getText())
    || !artifactId.equals(artifactIdText.getText())
    || !version.equals(versionText.getText())
    		|| !isCurrentPage();
	}

  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout( 2, false );
    gridLayout.marginWidth = 10;
    gridLayout.marginHeight = 10;
    composite.setLayout( gridLayout );
    initializeDialogUnits( composite );
    Dialog.applyDialogFont( composite );
    setControl(composite);

    final Label groupIdLabel = new Label(composite, SWT.NONE);
    groupIdLabel.setLayoutData(new GridData());
    groupIdLabel.setText("Group ID");
    groupIdText = new Text(composite, SWT.None);
    groupIdText.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
    
    final Label artifactIdLabel = new Label(composite, SWT.NONE);
    artifactIdLabel.setLayoutData(new GridData());
    artifactIdLabel.setText("Artifact ID");
    artifactIdText = new Text(composite, SWT.None);
    artifactIdText.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

    final Label versionLabel = new Label(composite, SWT.NONE);
    versionLabel.setLayoutData(new GridData());
    versionLabel.setText("Version");
    versionText = new Text(composite, SWT.None);
    versionText.setLayoutData(new GridData( GridData.FILL_HORIZONTAL ) );
  }
  
  public String getNewGroupId() {
    return groupId;
  }

  public String getNewArtifactId() {
    return artifactId;
  }

  public String getNewVersion() {
    return version;
  }

}
