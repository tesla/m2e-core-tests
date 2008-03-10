/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wizards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

import org.maven.ide.eclipse.Messages;


/**
 * Wizard page component showing panel project properties.
 */
public class MavenParentComponent extends Composite {

  /** parent artifact id input field */
  private Combo artifactIdCombo;

  /** parent group id input field */
  private Combo groupIdCombo;

  /** parent version input field */
  private Combo versionCombo;

  /** the "clear parent section" button */
  private Button clearButton;

  /** the "browse..." button */
  private Button browseButton;

  private Label groupIdLabel;

  private Label artifactIdLabel;

  private Label versionLabel;
  
  /** Creates a new panel with parent controls. */
  public MavenParentComponent(Composite parent, int style) {
    super(parent, SWT.NONE);

    boolean readonly = (style & SWT.READ_ONLY) != 0;

    GridLayout topLayout = new GridLayout();
    topLayout.marginHeight = 0;
    topLayout.marginWidth = 0;
    setLayout(topLayout);

    Group group = new Group(this, SWT.NONE);
    group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    group.setText(Messages.getString("wizard.project.page.artifact.parent.title"));

    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    group.setLayout(gridLayout);

    groupIdLabel = new Label(group, SWT.NONE);
    groupIdLabel.setText(Messages.getString("wizard.project.page.artifact.parent.groupId"));

    groupIdCombo = new Combo(group, SWT.NONE);
    groupIdCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    groupIdCombo.setEnabled(!readonly);

    artifactIdLabel = new Label(group, SWT.NONE);
    artifactIdLabel.setText(Messages.getString("wizard.project.page.artifact.parent.artifactId"));

    artifactIdCombo = new Combo(group, SWT.NONE);
    artifactIdCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    artifactIdCombo.setEnabled(!readonly);

    versionLabel = new Label(group, SWT.NONE);
    versionLabel.setText(Messages.getString("wizard.project.page.artifact.parent.version"));

    versionCombo = new Combo(group, SWT.NONE);
    GridData gd_versionCombo = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    gd_versionCombo.widthHint = 150;
    versionCombo.setLayoutData(gd_versionCombo);
    versionCombo.setEnabled(!readonly);

    if(!readonly) {
      Composite buttonPanel = new Composite(group, SWT.NONE);
      RowLayout rowLayout = new RowLayout();
      rowLayout.pack = false;
      rowLayout.marginTop = 0;
      rowLayout.marginRight = 0;
      rowLayout.marginLeft = 0;
      rowLayout.marginBottom = 0;
      buttonPanel.setLayout(rowLayout);
      buttonPanel.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));

      browseButton = new Button(buttonPanel, SWT.NONE);
      browseButton.setText(Messages.getString("wizard.project.page.artifact.parent.browse"));

      clearButton = new Button(buttonPanel, SWT.NONE);
      clearButton.setText(Messages.getString("wizard.project.page.artifact.parent.clear"));
      clearButton.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          setValues("", "", "");
        }
      });
    }
  }
  
  public Combo getGroupIdCombo() {
    return groupIdCombo;
  }

  public Combo getArtifactIdCombo() {
    return this.artifactIdCombo;
  }
  
  public Combo getVersionCombo() {
    return this.versionCombo;
  }
  
  public void setWidthGroup(WidthGroup widthGroup) {
    widthGroup.addControl(this.groupIdLabel);
    widthGroup.addControl(this.artifactIdLabel);
    widthGroup.addControl(this.versionLabel);
  }

  /** Adds modify listener to the input controls. */
  public void addModifyListener(ModifyListener listener) {
    artifactIdCombo.addModifyListener(listener);
    groupIdCombo.addModifyListener(listener);
    versionCombo.addModifyListener(listener);
  }

  /** Removes the listener from the input controls. */
  public void removeModifyListener(ModifyListener listener) {
    artifactIdCombo.removeModifyListener(listener);
    groupIdCombo.removeModifyListener(listener);
    versionCombo.removeModifyListener(listener);
  }

  /** Adds selection listener to the "browse" button. */
  public void addBrowseButtonListener(SelectionListener listener) {
    if(browseButton != null) {
      browseButton.addSelectionListener(listener);
    }
  }

  /** Removes the selection listener from the "browse" button. */
  public void removeBrowseButtonListener(SelectionListener listener) {
    if(browseButton != null) {
      browseButton.removeSelectionListener(listener);
    }
  }

  /** Enables the "clear" button. */
  public void setClearButtonEnabled(boolean enabled) {
    if(clearButton != null) {
      clearButton.setEnabled(enabled);
    }
  }

  /** Sets the parent group values. */
  public void setValues(String groupId, String artifactId, String version) {
    groupIdCombo.setText(groupId==null ? "" : groupId);
    artifactIdCombo.setText(artifactId==null ? "" : artifactId);
    versionCombo.setText(version==null ? "" : version);
  }

  /** Updates a Maven model. */
  public void updateModel(Model model) {
    String groupId = groupIdCombo.getText().trim();
    if(groupId.length() > 0) {
      Parent parent = new Parent();
      parent.setGroupId(groupId);
      parent.setArtifactId(artifactIdCombo.getText().trim());
      parent.setVersion(versionCombo.getText().trim());
      model.setParent(parent);
    }
  }

  /**
   * Validates the inputs to make sure all three fields are present in the same time, or none at all.
   */
  public boolean validate() {
    int parentCheck = 0;
    if(groupIdCombo.getText().trim().length() > 0) {
      parentCheck++ ;
    }
    if(artifactIdCombo.getText().trim().length() > 0) {
      parentCheck++ ;
    }
    if(versionCombo.getText().trim().length() > 0) {
      parentCheck++ ;
    }

    setClearButtonEnabled(parentCheck > 0);

    return parentCheck == 0 || parentCheck == 3;
  }

}
