/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr.internal.wizard;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Anton Kraev
 */
public class HyperlinkDialog extends MessageDialog {

  private String url;

  public HyperlinkDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage,
      int dialogImageType, String[] dialogButtonLabels, int defaultIndex, String url) {
    super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
    this.url = url;
  }

  protected Control createCustomArea(Composite parent) {
    Link link = new Link(parent, SWT.None);
    link.setText("<a href=\"#\">" + url + "</a>");
    link.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        Program.launch(url);
      }      
    });
    link.setToolTipText(url); 
    return link;
  }

}
