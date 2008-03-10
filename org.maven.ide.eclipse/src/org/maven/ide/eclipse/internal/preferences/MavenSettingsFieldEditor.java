/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.preferences;

import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import org.maven.ide.eclipse.MavenPlugin;

/**
 * Maven settings <code>FieldEditor</code>
 *
 * @author Eugene Kuleshov
 */
public class MavenSettingsFieldEditor extends FileFieldEditor {
  Button openButton;

  public MavenSettingsFieldEditor(String name, String labelText, Composite parent) {
    super(name, labelText, parent);
  }

  public Text getTextControl(Composite parent) {
    final Text text = super.getTextControl(parent);
    text.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        if(checkState()) {
          openButton.setEnabled(text.getText().length() != 0);
        }
      }
    });
    return text;
  }

  protected Button getChangeControl(Composite parent) {
    Button button = super.getChangeControl(parent);
    
    openButton = new Button(parent, SWT.PUSH);
    openButton.setText("O&pen");
    openButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        
        final String fileName = getTextControl().getText();
        
        IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor("settings.xml");
        
        try {
          final IEditorPart editor = IDE.openEditor(page, new MavenFileEditorInput(fileName), desc.getId());
          editor.addPropertyListener(new IPropertyListener() {
            public void propertyChanged(Object source, int propId) {
              if(!editor.isDirty()) {
                MavenPlugin plugin = MavenPlugin.getDefault();
                plugin.getConsole().logMessage("Refreshing settings " + fileName);
                plugin.getMavenEmbedderManager().invalidateMavenSettings();
              }
            }
          });
          
        } catch(PartInitException ex) {
          MavenPlugin.log(ex);
        }
      }
    });
    
    return button;
  }

  protected void adjustForNumColumns(int numColumns) {
    ((GridData) getTextControl().getLayoutData()).horizontalSpan = numColumns - 3;
  }

  public int getNumberOfControls() {
    return 4;
  }
}
