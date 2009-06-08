/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.refactoring.rename;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;


/**
 * @author Anton Kraev
 */
public class MavenRenameWizardPage extends UserInputWizardPage {
  private Text groupIdText;
  private Text artifactIdText;
  private Text versionText;
  private Button renameCheckbox;
  
  private String groupId;
  private String artifactId;
  private String version;
  private String newGroupId = "";
  private String newArtifactId = "";
  private String newVersion = "";
  private boolean renamed;

  protected MavenRenameWizardPage() {
    super("MavenRenameWizardPage");
    setDescription("Specify new group Id, artifact Id or version");
    setTitle("Rename Maven Artifact");
  }

  public void initialize(String groupId, String artifactID, String version) {
    this.groupId = newGroupId = nvl(groupId);
    this.artifactId = newArtifactId = nvl(artifactID);
    this.version = newVersion = nvl(version);
  }

  public String getNewGroupId() {
    return newGroupId;
  }

  public String getNewArtifactId() {
    return newArtifactId;
  }

  public String getNewVersion() {
    return newVersion;
  }

  @Override
  public boolean isPageComplete() {
    boolean renamedArtifact = !newArtifactId.equals(artifactId);
    renameCheckbox.setEnabled(renamedArtifact);
    if (!renamedArtifact) {
      renameCheckbox.setSelection(false);
      renamed = false;
    }
    return !newGroupId.equals(groupId) //
        || renamedArtifact //
        || !newVersion.equals(version) //
        || !isCurrentPage();
  }

  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginWidth = 10;
    gridLayout.marginHeight = 10;
    composite.setLayout(gridLayout);
    initializeDialogUnits(composite);
    Dialog.applyDialogFont(composite);
    setControl(composite);

    Label groupIdLabel = new Label(composite, SWT.NONE);
    groupIdLabel.setLayoutData(new GridData());
    groupIdLabel.setText("&Group Id:");

    groupIdText = new Text(composite, SWT.BORDER);
    groupIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    groupIdText.setData("name", "groupId");

    Label artifactIdLabel = new Label(composite, SWT.NONE);
    artifactIdLabel.setLayoutData(new GridData());
    artifactIdLabel.setText("&Artifact Id:");

    artifactIdText = new Text(composite, SWT.BORDER);
    artifactIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    artifactIdText.setData("name", "artifactId");

    Label versionLabel = new Label(composite, SWT.NONE);
    versionLabel.setLayoutData(new GridData());
    versionLabel.setText("&Version:");

    versionText = new Text(composite, SWT.BORDER);
    versionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    versionText.setData("name", "version");
    
    new Label(composite, SWT.NONE);

    renameCheckbox = new Button(composite, SWT.CHECK);
    renameCheckbox.setText("&Rename Eclipse project in Workspace");
    renameCheckbox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    renameCheckbox.setData("name", "rename");
    renameCheckbox.setEnabled(false);
    renameCheckbox.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        renamed = renameCheckbox.getSelection();
        getWizard().getContainer().updateButtons();
      }
    });
    
    ModifyListener listener = new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        newGroupId = groupIdText.getText();
        newArtifactId = artifactIdText.getText();
        newVersion = versionText.getText();
        getWizard().getContainer().updateButtons();
      }
    };

    groupIdText.setText(groupId);
    artifactIdText.setText(artifactId);
    versionText.setText(version);

    groupIdText.addModifyListener(listener);
    artifactIdText.addModifyListener(listener);
    versionText.addModifyListener(listener);
  }
  
  private String nvl(String str) {
    return str == null ? "" : str;
  }

  public boolean getRenameEclipseProject() {
    return renamed;
  }

}
