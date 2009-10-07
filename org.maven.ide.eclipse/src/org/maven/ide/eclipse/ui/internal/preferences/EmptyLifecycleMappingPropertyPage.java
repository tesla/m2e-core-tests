/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import org.maven.ide.eclipse.internal.lifecycle.AbstractLifecyclePropertyPage;

/**
 * EmptyLifecycleMappingPropertyPage
 *
 * @author dyocum
 */
public class EmptyLifecycleMappingPropertyPage extends AbstractLifecyclePropertyPage{

  /**
   * 
   */
  private static final String DEFAULT_MSG = "No lifecycle mapping info to display for the Empty Lifecycle Mapping";
  private static final String ERR_MSG = "Unable to load lifecycle mapping for this project.";
  private boolean isError;
  private String errorMessage;

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.lifecycle.AbstractLifecyclePropertyPage#createContents(org.eclipse.swt.widgets.Composite)
   */
  public Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));
    composite.setLayoutData(new GridData(GridData.FILL));
    Label noInfoLabel = new Label(composite, SWT.NONE);
    noInfoLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
    noInfoLabel.setAlignment(SWT.CENTER);
    noInfoLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
    noInfoLabel.setData("name", "noInfoLabel");
    noInfoLabel.setText(getNoLifecycleInfoMsg());
    return composite;
  }
  /**
   * @return
   */
  private String getNoLifecycleInfoMsg() {
   return this.isError() ? getErrorMessage() : DEFAULT_MSG;
  }


  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.lifecycle.AbstractLifecyclePropertyPage#performDefaults()
   */
  public void performDefaults() {
    //do nothing
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.lifecycle.AbstractLifecyclePropertyPage#performOk()
   */
  public boolean performOk() {
    return true;
  }
  
  /**
   * @param isError The isError to set.
   */
  public void setError(boolean isError) {
    this.isError = isError;
  }
  
  /**
   * @return Returns the isError.
   */
  public boolean isError() {
    return isError;
  }
  
  /**
   * @param message The message to set.
   */
  public void setErrorMessage(String message) {
    this.isError = true;
    this.errorMessage = message;
  }
  
  public String getErrorMessage(){
    return this.errorMessage == null ? ERR_MSG : this.errorMessage;
  }

}
