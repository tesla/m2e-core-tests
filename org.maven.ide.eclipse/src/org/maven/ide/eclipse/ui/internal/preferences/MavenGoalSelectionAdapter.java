/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.preferences;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.maven.ide.eclipse.ui.dialogs.MavenGoalSelectionDialog;

public class MavenGoalSelectionAdapter extends SelectionAdapter {
    private Shell shell;
    private Text text;

    public MavenGoalSelectionAdapter(Text text, Shell shell) {
      this.text = text;
      this.shell = shell;
    }

    public void widgetSelected(SelectionEvent e) {
//        String fileName = Util.substituteVar(fPomDirName.getText());
//        if(!isDirectoryExist(fileName)) {
//          MessageDialog.openError(getShell(), Messages.getString("launch.errorPomMissing"), 
//              Messages.getString("launch.errorSelectPom")); //$NON-NLS-1$ //$NON-NLS-2$
//          return;
//        }
      MavenGoalSelectionDialog dialog = new MavenGoalSelectionDialog(shell);
      int rc = dialog.open();
      if(rc == IDialogConstants.OK_ID) {
        text.insert("");  // clear selected text
        
        String txt = text.getText();
        int len = txt.length();
        int pos = text.getCaretPosition();
        
        StringBuffer sb = new StringBuffer();
        if((pos > 0 && txt.charAt(pos - 1) != ' ')) {
          sb.append(' ');
        }

        String sep = "";
        Object[] o = dialog.getResult();
        for(int i = 0; i < o.length; i++ ) {
          if(o[i] instanceof MavenGoalSelectionDialog.Entry) {
            if(dialog.isQualifiedName()) {
              sb.append(sep).append(((MavenGoalSelectionDialog.Entry) o[i]).getQualifiedName());
            } else {
              sb.append(sep).append(((MavenGoalSelectionDialog.Entry) o[i]).getName());
            }
          }
          sep = " ";
        }
        
        if(pos < len && txt.charAt(pos) != ' ') {
          sb.append(' ');
        }
        
        text.insert(sb.toString());
        text.setFocus();
      }
    }
  }