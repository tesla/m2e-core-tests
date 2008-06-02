/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;

import org.maven.ide.eclipse.internal.preferences.MavenPreferenceConstants;

/**
 * Maven runtime manager
 *
 * @author Eugene Kuleshov
 */
public class MavenRuntimeManager {
  
  public static final String DEFAULT = "DEFAULT";
  
  public static final String EMBEDDED = "EMBEDDED"; 

  public static final String WORKSPACE = "WORKSPACE";

  private final IPreferenceStore preferenceStore;

  private Map<String, MavenRuntime> runtimes = new LinkedHashMap<String, MavenRuntime>();
  
  private MavenRuntime defaultRuntime;

  
  public MavenRuntimeManager(IPreferenceStore preferenceStore) {
    this.preferenceStore = preferenceStore;
    initRuntimes();
  }

  public MavenRuntime getRuntime(String location) {
    if(location==null || location.length()==0 || DEFAULT.equals(location)) {
      return getDefaultRuntime();
    }
    if(EMBEDDED.equals(location)) {
      return MavenRuntime.EMBEDDED;
    }
    if(WORKSPACE.equals(location)) {
      return MavenRuntime.WORKSPACE;
    }
    return runtimes.get(location);
  }
  
  public MavenRuntime getDefaultRuntime() {
    if(defaultRuntime==null || !defaultRuntime.isAvailable()) {
      return MavenRuntime.EMBEDDED;
    }
    return this.defaultRuntime;
  }
  
  public List<MavenRuntime> getMavenRuntimes() {
    ArrayList<MavenRuntime> runtimes = new ArrayList<MavenRuntime>();

    runtimes.add(MavenRuntime.EMBEDDED);

//    // XXX discover all runtimes available from the Workspace
//    if(MavenRuntime.WORKSPACE.isAvailable()) {
//      runtimes.add(MavenRuntime.WORKSPACE);
//    }
    
    for(Iterator<MavenRuntime> it = this.runtimes.values().iterator(); it.hasNext();) {
      MavenRuntime runtime = it.next();
      if(runtime.isAvailable()) {
        runtimes.add(runtime);
      }
    }
    return runtimes;
  }

  public void reset() {
    preferenceStore.setToDefault(MavenPreferenceConstants.P_RUNTIMES);
    preferenceStore.setToDefault(MavenPreferenceConstants.P_DEFAULT_RUNTIME);
    
    initRuntimes();
  }
  
  public void setDefaultRuntime(MavenRuntime runtime) {
    this.defaultRuntime = runtime;
    
    preferenceStore.setValue(MavenPreferenceConstants.P_DEFAULT_RUNTIME, runtime.getLocation());
  }
  
  public void setRuntimes(List<MavenRuntime> runtimes) {
    this.runtimes.clear();

    String separator = "";
    StringBuffer sb = new StringBuffer();
    for(MavenRuntime runtime : runtimes) {
      if(runtime.isEditable()) {
        this.runtimes.put(runtime.getLocation(), runtime);
        sb.append(separator).append(runtime.getLocation());
        separator = "|";
      }
    }
    preferenceStore.setValue(MavenPreferenceConstants.P_RUNTIMES, sb.toString());
  }

  private void initRuntimes() {
    runtimes.clear();

    defaultRuntime = null;
    
    String selected = preferenceStore.getString(MavenPreferenceConstants.P_DEFAULT_RUNTIME);
    
    String runtimesPreference = preferenceStore.getString(MavenPreferenceConstants.P_RUNTIMES);
    if(runtimesPreference!=null && runtimesPreference.length()>0) {
      String[] locations = runtimesPreference.split("\\|");
      for(int i = 0; i < locations.length; i++ ) {
        MavenRuntime runtime = MavenRuntime.createExternalRuntime(locations[i]);
        runtimes.put(runtime.getLocation(), runtime);
        if(runtime.getLocation().equals(selected)) {
          defaultRuntime = runtime;
        }
      }
    }
    
    if(defaultRuntime==null) {
      defaultRuntime = MavenRuntime.EMBEDDED;
    }
  }
  
  // Maven preferences
  
  public boolean isOffline() {
    return preferenceStore.getBoolean(MavenPreferenceConstants.P_OFFLINE);
  }

  public boolean isDebugOutput() {
    return preferenceStore.getBoolean(MavenPreferenceConstants.P_DEBUG_OUTPUT);
  }

  public boolean isDownloadSources() {
    return preferenceStore.getBoolean(MavenPreferenceConstants.P_DOWNLOAD_SOURCES);
  }
  
  public boolean isDownloadJavadoc() {
    return preferenceStore.getBoolean(MavenPreferenceConstants.P_DOWNLOAD_JAVADOC);
  }
  
  public boolean isUpdateIndexesOnStartup() {
    return preferenceStore.getBoolean(MavenPreferenceConstants.P_UPDATE_INDEXES);
  }
  
  public String getGoalOnImport() {
    return preferenceStore.getString(MavenPreferenceConstants.P_GOAL_ON_IMPORT);
  }
  
  public String getGoalOnUpdate() {
    return preferenceStore.getString(MavenPreferenceConstants.P_GOAL_ON_UPDATE);
  }
  
  public String getUserSettingsFile() {
    return preferenceStore.getString(MavenPreferenceConstants.P_USER_SETTINGS_FILE);
  }
  
  public String getGlobalSettingsFile() {
//    if(defaultRuntime.isEditable()) {
//      return defaultRuntime.getSettings();  // settings for external Maven runtime
//    }
//    String globalSettings = preferenceStore.getString(MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE);
//    return globalSettings.trim().length()==0 ? null : globalSettings;
    
    return defaultRuntime.getSettings();
  }
  
  
  public void setOffline(boolean offline) {
    preferenceStore.setValue(MavenPreferenceConstants.P_OFFLINE, offline);
  }
  
  public void setDebugOutput(String debugOutput) {
    preferenceStore.setValue(MavenPreferenceConstants.P_DEBUG_OUTPUT, debugOutput);
  }

  public void setDownloadSources(boolean downloadSources) {
    preferenceStore.setValue(MavenPreferenceConstants.P_DOWNLOAD_SOURCES, downloadSources);
  }
  
  public void setDownloadJavadoc(boolean downloadJavaDoc) {
    preferenceStore.setValue(MavenPreferenceConstants.P_DOWNLOAD_JAVADOC, downloadJavaDoc);
  }
  
  public void setUpdateIndexesOnStartup(boolean updateIndexesOnStartup) {
    preferenceStore.setValue(MavenPreferenceConstants.P_UPDATE_INDEXES, updateIndexesOnStartup);
  }
  
  public void setDefaultOutputFolder(String name) {
    preferenceStore.setValue(MavenPreferenceConstants.P_OUTPUT_FOLDER, name);
  }
  
  public void setGoalOnImport(String goalName) {
    preferenceStore.setValue(MavenPreferenceConstants.P_GOAL_ON_IMPORT, goalName);
  }
  
  public void setGoalOnUpdate(String goalName) {
    preferenceStore.setValue(MavenPreferenceConstants.P_GOAL_ON_UPDATE, goalName);
  }
  
  public void setUserSettingsFile(String fileName) {
    preferenceStore.setValue(MavenPreferenceConstants.P_USER_SETTINGS_FILE, fileName);
  }
  
  public void setGlobalSettingsFile(String fileName) {
    preferenceStore.setValue(MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE, fileName);
  }
  
}
