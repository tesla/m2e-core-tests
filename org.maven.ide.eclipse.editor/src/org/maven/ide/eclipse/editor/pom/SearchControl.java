/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.editor.MavenEditorImages;

/**
 * @author Eugene Kuleshov
 */
public class SearchControl extends ControlContribution {
  private final IManagedForm managedForm;
  private Text searchText;

  public SearchControl(String id, IManagedForm managedForm) {
    super(id);
    this.managedForm = managedForm;
  }

  public Text getSearchText() {
    return searchText;
  }
  
  protected Control createControl(Composite parent) {
    FormToolkit toolkit = managedForm.getToolkit();
    Composite composite = toolkit.createComposite(parent);

    GridLayout layout = new GridLayout(3, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.verticalSpacing = 0;

    composite.setLayout(layout);
    composite.setBackground(null);

    Control label = toolkit.createLabel(composite, "Search:");
    label.setBackground(null);

    searchText = toolkit.createText(composite, "", SWT.FLAT);
    searchText.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.TRUE);
    searchText.setLayoutData(new GridData(200, -1));

    ToolBar cancelBar = new ToolBar(composite, SWT.FLAT);

    final ToolItem clearToolItem = new ToolItem(cancelBar, SWT.NONE);
    clearToolItem.setEnabled(false);
    clearToolItem.setImage(MavenEditorImages.IMG_CLEAR);
    clearToolItem.setDisabledImage(MavenEditorImages.IMG_CLEAR_DISABLED);
    clearToolItem.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        searchText.setText("");
      }
    });
    
    searchText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        clearToolItem.setEnabled(searchText.getText().length() > 0);
      }
    });

    toolkit.paintBordersFor(composite);

    return composite;
  }
  
}
