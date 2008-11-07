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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.components.pom.Model;


/**
 * @author Anton Kraev
 */
public class MavenRenameWizardPage extends UserInputWizardPage {
  private Text groupIdText;
  private Text artifactIdText;
  private Text versionText;
  
  private Model model;
  
  private String groupId = "";
  private String artifactId = "";
  private String version = "";

  protected MavenRenameWizardPage() {
    super("MavenRenameWizardPage");
    setDescription("Specify new group Id, artifact Id or version");
    setTitle("Rename Maven Artifact");
  }

  public void setModel(Model model) {
    this.model = model;
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

  @Override
  public boolean isPageComplete() {
    return !groupId.equals(groupIdText.getText()) //
        || !artifactId.equals(artifactIdText.getText()) //
        || !version.equals(versionText.getText()) //
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
    groupIdLabel.setText("Group Id:");

    groupIdText = new Text(composite, SWT.BORDER);
    groupIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    groupIdText.setData("name", "groupId");

    Label artifactIdLabel = new Label(composite, SWT.NONE);
    artifactIdLabel.setLayoutData(new GridData());
    artifactIdLabel.setText("Artifact Id:");

    artifactIdText = new Text(composite, SWT.BORDER);
    artifactIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    artifactIdText.setData("name", "artifactId");

    Label versionLabel = new Label(composite, SWT.NONE);
    versionLabel.setLayoutData(new GridData());
    versionLabel.setText("Version:");

    versionText = new Text(composite, SWT.BORDER);
    versionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    versionText.setData("name", "version");
    
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
    
    groupIdText.setText(model.getGroupId());
    artifactIdText.setText(model.getArtifactId());
    versionText.setText(model.getVersion());
  }

}
