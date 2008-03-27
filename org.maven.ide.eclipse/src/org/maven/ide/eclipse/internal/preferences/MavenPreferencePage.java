/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.preferences;

import java.io.File;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.Messages;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.internal.embedder.PluginConsoleMavenEmbeddedLogger;


public class MavenPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  final MavenPlugin plugin;
  
  final File localRepositoryDir;

//  private final String globalSettings;
//
//  private String userSettings;
  
  FileFieldEditor globalSettingsEditor;
  
  FileFieldEditor userSettingsEditor;

  StringFieldEditor localRepositoryEditor;

  public MavenPreferencePage() {
    super(GRID);
    setPreferenceStore(MavenPlugin.getDefault().getPreferenceStore());

    plugin = MavenPlugin.getDefault();
    localRepositoryDir = plugin.getMavenEmbedderManager().getLocalRepositoryDir();
//    globalSettings = getPreferenceStore().getString(MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE);
//    userSettings = getPreferenceStore().getString(MavenPreferenceConstants.P_USER_SETTINGS_FILE);
  }

  /*
   * Creates the field editors. Field editors are abstractions of the common GUI
   * blocks needed to manipulate various types of preferences. Each field editor
   * knows how to save and restore itself.
   */
  public void createFieldEditors() {
    //    addField(new DirectoryFieldEditor(MavenPreferenceConstants.P_LOCAL_REPOSITORY_DIR, 
    //        Messages.getString("preferences.localRepositoryFolder"), //$NON-NLS-1$
    //        getFieldEditorParent()));

    // addField( new BooleanFieldEditor( MavenPreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION, 
    //     Messages.getString( "preferences.checkLastPluginVersions" ), //$NON-NLS-1$
    //     getFieldEditorParent() ) );

    addField(new BooleanFieldEditor(MavenPreferenceConstants.P_OFFLINE, Messages.getString("preferences.offline"), //$NON-NLS-1$
        getFieldEditorParent()));
    //    addField(new RadioGroupFieldEditor(MavenPreferenceConstants.P_GLOBAL_CHECKSUM_POLICY, 
    //        Messages.getString("preferences.globalChecksumPolicy"), 1, //$NON-NLS-1$
    //        new String[][] {
    //            {Messages.getString("preferences.checksumPolicyFail"), ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL}, //$NON-NLS-1$
    //            {Messages.getString("preferences.checksumPolicyIgnore"), ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE}, //$NON-NLS-1$
    //            {Messages.getString("preferences.checksumPolicyWarn"), ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN}}, //$NON-NLS-1$  // DEFAULT
    //        getFieldEditorParent(), true));
    // addField( new StringFieldEditor( MavenPreferenceConstants.P_OFFLINE,
    // "A &text preference:",
    // getFieldEditorParent()));
    
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
    
    /*
     * public static final String CHECKSUM_POLICY_FAIL = "fail"; 
     * public static final String CHECKSUM_POLICY_WARN = "warn"; 
     * public static final String CHECKSUM_POLICY_IGNORE = "ignore";
     */

    addSeparator();

    String[] goals = new String[] { //
        MavenPreferenceConstants.NO_GOAL, //
        "generate-sources", //
        "process-sources", //
        "generate-resources", //
        "process-resources", //
        "process-classes", //
        "generate-test-sources", // 
        "process-test-sources", // 
        "generate-test-resources", // 
        "process-test-resources"};

    GridData comboCompositeGridData = new GridData();
    // comboCompositeGridData.verticalIndent = 15;
    comboCompositeGridData.horizontalSpan = 3;
    comboCompositeGridData.grabExcessHorizontalSpace = true;
    comboCompositeGridData.horizontalAlignment = GridData.FILL;

    Composite comboComposite = new Composite(getFieldEditorParent(), SWT.NONE);
    comboComposite.setLayoutData(comboCompositeGridData);
    comboComposite.setLayout(new GridLayout(2, false));
    
    addField(new ComboFieldEditor(MavenPreferenceConstants.P_GOAL_ON_IMPORT, //
        Messages.getString("preferences.goalOnImport"), //$NON-NLS-1$
        goals, comboComposite));
    
    addField(new ComboFieldEditor(MavenPreferenceConstants.P_GOAL_ON_UPDATE, //
        Messages.getString("preferences.goalOnUpdate"), //$NON-NLS-1$
        goals, comboComposite));

    addSeparator();
    
    globalSettingsEditor = new MavenSettingsFieldEditor(MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE, //
        Messages.getString("preferences.globalSettingsFile"), getFieldEditorParent()) { //$NON-NLS-1$
      protected boolean checkState() {
        return checkSettings();
      }
    };
    globalSettingsEditor.setChangeButtonText("&Browse...");
    
    userSettingsEditor = new MavenSettingsFieldEditor(MavenPreferenceConstants.P_USER_SETTINGS_FILE, //
        Messages.getString("preferences.userSettingsFile"), getFieldEditorParent()) {
      protected boolean checkState() {
        return checkSettings();
      }
    };
    userSettingsEditor.setChangeButtonText("Bro&wse...");

    
//    addField(new StringFieldEditor("", Messages.getString("preferences.userSettingsFile"), getFieldEditorParent()) { //$NON-NLS-1$
//      protected void doLoad() {
//        getTextControl().setEditable(false);
//        getTextControl().setText(MavenEmbedder.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath());
//      }
//      
//      protected void doLoadDefault() {
//        getTextControl().setEditable(false);
//        getTextControl().setText(MavenEmbedder.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath());
//      }
//      
//      protected void doStore() {
//      }
//      
//      protected boolean doCheckState() {
//        return true;
//      }
//    });
    
    localRepositoryEditor = new StringFieldEditor("", Messages.getString("preferences.localRepository"), getFieldEditorParent()) { //$NON-NLS-1$
      protected void doLoad() {
        getTextControl().setEditable(false);
        getTextControl().setText(plugin.getMavenEmbedderManager().getLocalRepositoryDir().getAbsolutePath());
      }

      protected void doLoadDefault() {
        getTextControl().setEditable(false);
        getTextControl().setText(plugin.getMavenEmbedderManager().getLocalRepositoryDir().getAbsolutePath());
      }

      protected void doStore() {
      }

      protected boolean doCheckState() {
        return true;
      }
    };
    
    addField(localRepositoryEditor);
    addField(globalSettingsEditor);
    addField(userSettingsEditor);

    GridData buttonsCompositeGridData = new GridData();
    buttonsCompositeGridData.verticalIndent = 15;
    buttonsCompositeGridData.horizontalSpan = 4;

    Composite buttonsComposite = new Composite(getFieldEditorParent(), SWT.NONE);
    buttonsComposite.setLayout(new RowLayout());
    buttonsComposite.setLayoutData(buttonsCompositeGridData);

    Button reindexButton = new Button(buttonsComposite, SWT.NONE);
    reindexButton.setText(Messages.getString("preferences.reindexButton"));
    reindexButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        plugin.getMavenEmbedderManager().invalidateMavenSettings();
        plugin.getIndexManager().scheduleIndexUpdate(IndexManager.LOCAL_INDEX, true, 0L);
      }
    });

    Button refreshButton = new Button(buttonsComposite, SWT.NONE);
    refreshButton.setText(Messages.getString("preferences.refreshButton"));
    refreshButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        localRepositoryEditor.load();
        plugin.getMavenEmbedderManager().invalidateMavenSettings();
      }
    });
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

  protected void contributeButtons(Composite parent) {
    super.contributeButtons(parent);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  public void init(IWorkbench workbench) {
  }

  public boolean performOk() {
    boolean res = checkSettings() && super.performOk();
    if(res) {
//      String newGlobalSettings = getPreferenceStore().getString(MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE);
//      String newUserSettings = getPreferenceStore().getString(MavenPreferenceConstants.P_USER_SETTINGS_FILE);
//      if(newGlobalSettings == null ? globalSettings == null : !newGlobalSettings.equals(globalSettings) ||
//          newUserSettings == null ? userSettings == null : !newUserSettings.equals(userSettings)) {
//      }
      plugin.getMavenEmbedderManager().invalidateMavenSettings();

      localRepositoryEditor.load();

      File newRepositoryDir = plugin.getMavenEmbedderManager().getLocalRepositoryDir();
      if(!newRepositoryDir.equals(localRepositoryDir)) {
        plugin.getIndexManager().scheduleIndexUpdate(IndexManager.LOCAL_INDEX, true, 0L);
      }
    }
    return res;
  }

  boolean checkSettings() {
    setErrorMessage(null);
    setMessage(null);

    Configuration configuration = new DefaultConfiguration() //
        .setClassLoader(Thread.currentThread().getContextClassLoader()) //
        .setMavenEmbedderLogger(new PluginConsoleMavenEmbeddedLogger(plugin.getConsole(), false));

    String globalSettingsFileName = globalSettingsEditor.getStringValue();
    if(globalSettingsFileName != null && globalSettingsFileName.length() > 0) {
      File globalSettingsFile = new File(globalSettingsFileName);
      if(globalSettingsFile.exists()) {
        configuration.setGlobalSettingsFile(globalSettingsFile);
      } else {
        setMessage("Global settings file don't exists", IMessageProvider.WARNING);
      }
    }
    
    String userSettingsFileName = userSettingsEditor.getStringValue();
    if(userSettingsFileName != null && userSettingsFileName.length() > 0) { 
      File userSettingsFile = new File(userSettingsFileName);
      if(userSettingsFile.exists()) {
        configuration.setUserSettingsFile(userSettingsFile);
      } else {
        setMessage("User settings file don't exists", IMessageProvider.WARNING);
      }
    }

    ConfigurationValidationResult result = MavenEmbedder.validateConfiguration(configuration);
    if(!result.isValid()) {
      Exception uex = result.getUserSettingsException();
      Exception gex = result.getGlobalSettingsException();
      if(uex!=null) {
        setMessage("Unable to parse user settings file; " + uex.toString(), IMessageProvider.WARNING);
        return false;
      } else if(gex!=null) {
        setMessage("Unable to parse global settings file; " + gex.toString(), IMessageProvider.WARNING);
        return false;
      } else {
        setMessage("User configuration is invalid", IMessageProvider.WARNING);
        return false;
      }
    }

    return true;
  }

}
