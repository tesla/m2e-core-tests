/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;


/**
 * A Dialog whose primary goal is to allow the user to select a dependency, either by entering the GAV coordinates
 * manually, or by search through a repository index.
 * 
 * @author rgould
 */
public class AddDependencyDialog extends AbstractMavenDialog {

  private static final String[] SCOPES = new String[] { "compile", "provided", "runtime", "test", "system" };
  /*
   * dependencies under dependencyManagement are permitted to use an the extra "import" scope
   */
  private static final String[] DEP_MANAGEMENT_SCOPES = new String[] { "compile", "provided", "runtime", "test", "system", "import" };
  private static final String DIALOG_SETTINGS = AddDependencyDialog.class.getName();
  private String[] scopes;

  /**
   * The AddDependencyDialog differs slightly in behaviour depending on
   * context. If it is being used to apply a dependency under the 
   * "dependencyManagement" context, the extra "import" scope is available. 
   * Set @param isForDependencyManagement to true if this is case.
   * 
   * @param parent
   * @param isForDependencyManagement
   */
  public AddDependencyDialog(Shell parent, boolean isForDependencyManagement) {
    super(parent, DIALOG_SETTINGS);

    setShellStyle(getShellStyle() | SWT.RESIZE);
    setTitle("Add Dependency");
    
    if (!isForDependencyManagement) {
      this.scopes = SCOPES;
    } else {
      this.scopes = DEP_MANAGEMENT_SCOPES;
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.Dialog#createDialogArea()
   */
  protected Control createDialogArea(Composite parent) {
    readSettings();
    
    Composite composite = (Composite) super.createDialogArea(parent);
    
    Composite gavControls = createGAVControls(composite);
    gavControls.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    
    new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    
    Composite searchControls = createSearchControls(composite);
    searchControls.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    
    return composite;
  }

  /**
   * Sets the up group-artifact-version controls
   */
  private Composite createGAVControls(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridData gridData = null;
    
    GridLayout gridLayout = new GridLayout(4, false);
    composite.setLayout(gridLayout);
    
    Label groupIDlabel = new Label(composite, SWT.NONE);
    groupIDlabel.setText("Group ID:");
    
    Text groupIDtext = new Text(composite, SWT.BORDER);
    groupIDtext.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    
    Label scopeLabel = new Label(composite, SWT.NONE);
    scopeLabel.setText("Scope: ");
    gridData = new GridData();
    gridData.verticalSpan = 3;
    gridData.verticalAlignment = SWT.TOP;
    scopeLabel.setLayoutData(gridData);
    
    List scopeList = new List(composite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
    scopeList.setItems(scopes);
    gridData = new GridData();
    gridData.grabExcessVerticalSpace = true;
    gridData.verticalAlignment = SWT.TOP;
    gridData.verticalSpan = 3;
    scopeList.setLayoutData(gridData);
    
    Label artifactIDlabel = new Label(composite, SWT.NONE);
    artifactIDlabel.setText("Artifact ID:");
    
    Text artifactIDtext = new Text(composite, SWT.BORDER);
    artifactIDtext.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    Label versionLabel = new Label(composite, SWT.NONE);
    versionLabel.setText("Version: ");
    versionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
    
    Text version = new Text(composite, SWT.BORDER);
    version.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    
    return composite;
  }

  private Composite createSearchControls(Composite parent) {
    SashForm sashForm = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);
    sashForm.setLayout(new FillLayout());

    Composite resultsComposite = new Composite(sashForm, SWT.NONE);
    FormData data = null;
    
    resultsComposite.setLayout(new FormLayout());
    
    Label queryLabel = new Label(resultsComposite, SWT.NONE);
    queryLabel.setText("Query:");
    data = new FormData();
    data.left = new FormAttachment(0, 0);
    queryLabel.setLayoutData(data);
    
    Text queryText = new Text(resultsComposite, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH);
    data = new FormData();
    data.left = new FormAttachment(10, 0);
    data.right = new FormAttachment(100, -5);
    queryText.setLayoutData(data);
    
    Label hint = new Label(resultsComposite, SWT.NONE);
    hint.setText("(coordinate, sha1 prefix, project name)");
    data = new FormData();
    data.left = new FormAttachment(10, 0);
    data.top = new FormAttachment(queryText, 5);
    hint.setLayoutData(data);
    
    Label resultsLabel = new Label(resultsComposite, SWT.NONE);
    resultsLabel.setText("Results:");
    data = new FormData();
    data.left = new FormAttachment(0, 0);
    data.top = new FormAttachment(hint, 5);
    resultsLabel.setLayoutData(data);
    
    Tree resultsTree = new Tree(resultsComposite, SWT.MULTI | SWT.BORDER);
    data = new FormData();
    data.left = new FormAttachment(10, 0);
    data.top = new FormAttachment(hint, 5);
    data.right = new FormAttachment(100, -5);
    data.bottom = new FormAttachment(100, -5);
    resultsTree.setLayoutData(data);
    
    Composite infoComposite = new Composite(sashForm, SWT.NONE);
    infoComposite.setLayout(new FormLayout());
    
    Label infoLabel = new Label(infoComposite, SWT.NONE);
    FormData formData = new FormData();
    formData.left = new FormAttachment(0, 0);
    infoLabel.setLayoutData(formData);
    infoLabel.setText("Info: ");
    
    Text infoTextarea = new Text(infoComposite, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER );
    formData = new FormData();
    formData.left = new FormAttachment(10, 0);
    formData.bottom = new FormAttachment(100, -5);
    formData.top = new FormAttachment(0, 0);
    formData.right = new FormAttachment(100, -5);
    infoTextarea.setLayoutData(formData);
    
    sashForm.setWeights(new int[] { 70, 30 } );
    
    return sashForm;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
   * This is called when OK is pressed. There's no obligation to do anything.
   */
  protected void computeResult() {
    // TODO Auto-generated method computeResult

  }

}
