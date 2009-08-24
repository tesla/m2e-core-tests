/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.Messages;
import org.maven.ide.eclipse.internal.preferences.MavenPreferenceConstants;


public class MavenPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  final MavenPlugin plugin;
  
  public MavenPreferencePage() {
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

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_OFFLINE, Messages.getString("preferences.offline"), //$NON-NLS-1$
        getFieldEditorParent()));
    
    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_DEBUG_OUTPUT, //
        Messages.getString("preferences.debugOutput"), //$NON-NLS-1$
        getFieldEditorParent()));

    // addField( new BooleanFieldEditor( MavenPreferenceConstants.P_UPDATE_SNAPSHOTS, 
    //     Messages.getString( "preferences.updateSnapshots" ), //$NON-NLS-1$
    //     getFieldEditorParent() ) );

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_DOWNLOAD_SOURCES, //
        Messages.getString("preferences.downloadSources"), //$NON-NLS-1$
        getFieldEditorParent()));

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_DOWNLOAD_JAVADOC, //
        Messages.getString("preferences.downloadJavadoc"), //$NON-NLS-1$
        getFieldEditorParent()));

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_UPDATE_INDEXES, //
        "Download repository index updates on startup", //
        getFieldEditorParent()));
    
    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_UPDATE_PROJECTS, //
        "Update Maven projects on startup", //
        getFieldEditorParent()));
    
    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_HIDE_FOLDERS_OF_NESTED_PROJECTS, //
        "Hide folders of physically nested modules (experimental)", getFieldEditorParent()));
    
    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_SUPPORT_SEPARATE_PROJECTS_FOR_MODULES, //
        Messages.getString("preferences.separateProjectsForModules"), getFieldEditorParent()));

    GridData comboCompositeGridData = new GridData();
    comboCompositeGridData.verticalIndent = 25;
    comboCompositeGridData.horizontalSpan = 3;
    comboCompositeGridData.grabExcessHorizontalSpace = true;
    comboCompositeGridData.horizontalAlignment = GridData.FILL;

    Composite comboComposite = new Composite(getFieldEditorParent(), SWT.NONE);
    comboComposite.setLayoutData(comboCompositeGridData);
    comboComposite.setLayout(new GridLayout(2, false));
    
    addField(new GoalsFieldEditor(MavenPreferenceConstants.P_GOAL_ON_IMPORT, //
        Messages.getString("preferences.goalOnImport"), "&Select...", comboComposite)); //$NON-NLS-1$

    addField(new GoalsFieldEditor(MavenPreferenceConstants.P_GOAL_ON_UPDATE, //
        Messages.getString("preferences.goalOnUpdate"), "S&elect...", comboComposite)); //$NON-NLS-1$

    // addSeparator();
  }

  private void addSeparator() {
    Label separator = new Label(getFieldEditorParent(), SWT.HORIZONTAL | SWT.SEPARATOR);
    // separator.setVisible(false);
    GridData separatorGridData = new GridData();
    separatorGridData.horizontalSpan = 4;
    separatorGridData.grabExcessHorizontalSpace = true;
    separatorGridData.horizontalAlignment = GridData.FILL;
    separatorGridData.verticalIndent = 10;
    separator.setLayoutData(separatorGridData);
  }

}
