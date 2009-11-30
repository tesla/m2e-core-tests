/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.Messages;
import org.maven.ide.eclipse.internal.preferences.MavenPreferenceConstants;


public class ProblemReportingPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  final MavenPlugin plugin;
  private Composite parent;
  
  public ProblemReportingPreferencePage() {
    super(GRID);
    setPreferenceStore(MavenPlugin.getDefault().getPreferenceStore());

    plugin = MavenPlugin.getDefault();
  }

  public void init(IWorkbench workbench) {
  }

  /*
   * Creates the field editors. Field editors are abstractions of the common GUI
   * blocks needed to manipulate various types of preferences. Each field editor
   * knows how to save and restore itself.
   */
  public void createFieldEditors() {
    parent = getFieldEditorParent();
    
    addField(new StringFieldEditor(MavenPreferenceConstants.P_JIRA_USERNAME, Messages.getString("jira.username"), parent));
    
    StringFieldEditor passwordEditor = new StringFieldEditor(MavenPreferenceConstants.P_JIRA_PASSWORD, Messages.getString("jira.password"), parent);
    
    addField(passwordEditor);
    Text passwordField = passwordEditor.getTextControl(parent);
    passwordField.setEchoChar('*');
  }
}
