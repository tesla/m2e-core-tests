/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.integration.tests;

import junit.framework.TestCase;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.m2e.core.ui.dialogs.InputHistory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;


public class InputHistoryTest extends TestCase {
  private static final String COMBO_NAME = "combo";

  private static final String COMBO_TEXT = "combo text";

  private static final String COMBO_TEXT2 = "combo text 2";

  private static final String CCOMBO_NAME = "ccombo";

  private static final String CCOMBO_TEXT = "ccombo text";

  private static final String CCOMBO_TEXT2 = "ccombo text 2";

  @Test
  public void testInputHistory() {

    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        // open the dialog once, set values, close
        Dialog d = new Dialog(new Shell());
        d.open();
        d.combo.setText(COMBO_TEXT);
        d.ccombo.setText(CCOMBO_TEXT);
        d.close();

        // open the dialog again, make sure the history is there
        d = new Dialog(new Shell());
        d.open();
        try {
          assertEquals("Combo default value", COMBO_TEXT, d.combo.getText());
          assertEquals("Combo history length", 1, d.combo.getItemCount());
          assertEquals("Combo history value", COMBO_TEXT, d.combo.getItem(0));
          assertEquals("CCombo default value", "", d.ccombo.getText());
          assertEquals("CCombo history length", 1, d.ccombo.getItemCount());
          assertEquals("CCombo history value", CCOMBO_TEXT, d.ccombo.getItem(0));

          d.combo.setText(COMBO_TEXT2);
          d.ccombo.setText(CCOMBO_TEXT2);
        } finally {
          d.close();
        }

        // open the dialog the third time, make sure the history is updated
        d = new Dialog(new Shell());
        d.open();
        try {
          assertEquals("Combo default value", COMBO_TEXT2, d.combo.getText());
          assertEquals("Combo history length", 2, d.combo.getItemCount());
          assertEquals("Combo history value (new)", COMBO_TEXT2, d.combo.getItem(0));
          assertEquals("Combo history value (old)", COMBO_TEXT, d.combo.getItem(1));
          assertEquals("CCombo default value", "", d.ccombo.getText());
          assertEquals("CCombo history length", 2, d.ccombo.getItemCount());
          assertEquals("CCombo history value (new)", CCOMBO_TEXT2, d.ccombo.getItem(0));
          assertEquals("CCombo history value (old)", CCOMBO_TEXT, d.ccombo.getItem(1));
        } finally {
          d.close();
        }
      }
    });
  }

  protected class Dialog extends TitleAreaDialog {
    private InputHistory inputHistory;

    protected Combo combo;

    protected CCombo ccombo;

    public Dialog(Shell parentShell) {
      super(parentShell);
      setBlockOnOpen(false);
      inputHistory = new InputHistory(Dialog.class.getName());
    }

    public boolean close() {
      inputHistory.save();
      return super.close();
    }

    protected Control createDialogArea(Composite parent) {
      Composite dialogArea = (Composite) super.createDialogArea(parent);

      Composite composite = new Composite(dialogArea, SWT.NONE);
      composite.setLayout(new GridLayout());

      combo = new Combo(composite, SWT.NONE);
      combo.setData("name", COMBO_NAME);
      ccombo = new CCombo(composite, SWT.BORDER);
      ccombo.setData("name", CCOMBO_NAME);

      inputHistory.add(combo);
      inputHistory.add(ccombo);

      inputHistory.load();

      return dialogArea;
    }
  }
}
