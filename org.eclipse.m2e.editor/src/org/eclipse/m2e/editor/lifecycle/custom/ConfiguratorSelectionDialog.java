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

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class ConfiguratorSelectionDialog extends Dialog {

  private Combo configuratorCombo;
  private String selectedConfigurator;
  
  private String[] configuratorIds;
  private String[] configuratorNames;

  /**
   * Create the dialog.
   * @param parentShell
   */
  public ConfiguratorSelectionDialog(Shell parentShell, List<AbstractProjectConfigurator> configurators) {
    super(parentShell);
    configuratorIds = new String[configurators.size()];
    configuratorNames = new String[configurators.size()];
    int i = 0;
    for(AbstractProjectConfigurator configurator : configurators) {
      configuratorIds[i] = configurator.getId();
      configuratorNames[i] = configurator.getName();
      i++;
    }
  }

  /**
   * Create contents of the dialog.
   * @param parent
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);
    container.setLayout(new GridLayout(1, true));
    configuratorCombo = new Combo(container, SWT.NONE);
    configuratorCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
    configuratorCombo.setEnabled(true);
    configuratorCombo.setItems(configuratorNames);
    configuratorCombo.select(0);
    
    return container;
  }
  
  @Override
  protected void okPressed() {
    selectedConfigurator = configuratorIds[configuratorCombo.getSelectionIndex()];
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
    return new Point(300, 125);
  }
  
  public String getSelectedConfigurator() {
    return selectedConfigurator;
  }

}
