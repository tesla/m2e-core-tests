/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.lifecycle.custom;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.m2e.core.internal.project.CustomizableLifecycleMapping;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class CustomLifecycleParamsDialog extends Dialog {

  private Combo templateCombo;
  private Button createFromTemplateButton;
  private Button createEmptyButton;
  private String selectedTemplate;
  
  private String[] mappingIds;
  private String[] mappingNames;

  /**
   * Create the dialog.
   * @param parentShell
   */
  public CustomLifecycleParamsDialog(Shell parentShell, String[] mappingIds, String[] mappingNames) {
    super(parentShell);
    if(mappingIds.length != mappingNames.length) {
      throw new IllegalArgumentException();
    }
    ArrayList<String> filteredIds = new ArrayList<String>(mappingIds.length);
    ArrayList<String> filteredNames = new ArrayList<String>(mappingNames.length);
    for(int i = 0; i < mappingIds.length; i++) {
      if(!CustomizableLifecycleMapping.EXTENSION_ID.equals(mappingIds[i])) {
        filteredIds.add(mappingIds[i]);
        filteredNames.add(mappingNames[i]);
      }
    }
    
    this.mappingIds = filteredIds.toArray(new String[0]);
    this.mappingNames = filteredNames.toArray(new String[0]); 
  }

  /**
   * Create contents of the dialog.
   * @param parent
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);
    container.setLayout(new GridLayout(1, true));
    createEmptyButton = new Button(container, SWT.RADIO);
    createEmptyButton.setSelection(true);
    createEmptyButton.setText(Messages.CustomLifecycleParamsDialog_btnCreateEmpty);
    createEmptyButton.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent event) {
        templateCombo.setEnabled(false);
      }
      
      public void widgetDefaultSelected(SelectionEvent event) {}
    });
    
    createFromTemplateButton = new Button(container, SWT.RADIO);
    createFromTemplateButton.setText(Messages.CustomLifecycleParamsDialog_btnCopyFrom);
    createFromTemplateButton.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent event) {
        templateCombo.setEnabled(true);
      }
      
      public void widgetDefaultSelected(SelectionEvent event) {}
    });
    templateCombo = new Combo(container, SWT.NONE);
    templateCombo.setEnabled(false);
    templateCombo.setItems(mappingNames);
    templateCombo.select(0);
    
    return container;
  }
  
  @Override
  protected void okPressed() {
    if(createEmptyButton.getSelection()) {
      selectedTemplate = null;
    } else {
      int si = templateCombo.getSelectionIndex();
      selectedTemplate = mappingIds[si];
    }
    super.okPressed();
  }

  /**
   * Create contents of the button bar.
   * @param parent
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
  }

  /**
   * Return the initial size of the dialog.
   */
  @Override
  protected Point getInitialSize() {
    return new Point(300, 200);
  }
  
  public String getSelectedTemplate() {
    return selectedTemplate;
  }

}
