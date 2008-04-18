/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wizards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.ProjectImportConfiguration;


/**
 * AbstractMavenImportWizardPage
 * 
 * @author Eugene Kuleshov
 */
public abstract class AbstractMavenWizardPage extends WizardPage {

  /** The resolver configuration panel */
  protected ResolverConfigurationComponent resolverConfigurationComponent;
  
  /** dialog settings to store input history */
  protected IDialogSettings dialogSettings;

  /** the Map of field ids to List of comboboxes that share the same history */
  private Map fieldsWithHistory;

  /** the history limit */
  protected static final int MAX_HISTORY = 15;

  /**
   * The resolver configuration
   */
  protected ProjectImportConfiguration projectImportConfiguration;

  protected AbstractMavenWizardPage(String pageName) {
    this(pageName, null);
  }

  /**
   * Creates a page. This constructor should be used for the wizards where you need to have the advanced settings box on
   * each page. Pass the same bean to each page so they can share the data.
   */
  protected AbstractMavenWizardPage(String pageName, ProjectImportConfiguration projectImportConfiguration) {
    super(pageName);
    this.projectImportConfiguration = projectImportConfiguration;

    fieldsWithHistory = new HashMap();
    
    initDialogSettings();
  }

  /** Creates an advanced settings panel. */
  protected void createAdvancedSettings(Composite composite, GridData gridData) {
    if(projectImportConfiguration != null) {
      resolverConfigurationComponent = new ResolverConfigurationComponent(composite, projectImportConfiguration, true);
      resolverConfigurationComponent.setLayoutData(gridData);
      addFieldWithHistory("projectNameTemplate", resolverConfigurationComponent.template);
    }
  }

//  /** Returns the resolver configuration based on the advanced settings. */
//  public ResolverConfiguration getResolverConfiguration() {
//    return resolverConfiguration == null ? new ResolverConfiguration() : //
//        resolverConfigurationComponent.getResolverConfiguration();
//  }

  /** Loads the advanced settings data when the page is displayed. */
  public void setVisible(boolean visible) {
    if(visible) {
      if(dialogSettings == null) {
        initDialogSettings();
        loadInputHistory();
      }
      if(resolverConfigurationComponent != null) {
        resolverConfigurationComponent.loadData();
      }
    }
    super.setVisible(visible);
  }

  /** Saves the history when the page is disposed. */
  public void dispose() {
    saveInputHistory();
    super.dispose();
  }

  /** Loads the dialog settings using the page name as a section name. */
  private void initDialogSettings() {
    IDialogSettings pluginSettings = MavenPlugin.getDefault().getDialogSettings();
    dialogSettings = pluginSettings.getSection(getName());
    if(dialogSettings == null) {
      dialogSettings = pluginSettings.addNewSection(getName());
      pluginSettings.addSection(dialogSettings);
    }
  }

  /** Loads the input history from the dialog settings. */
  private void loadInputHistory() {
    for(Iterator i = fieldsWithHistory.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Map.Entry) i.next();
      String id = (String) e.getKey();
      String[] items = dialogSettings.getArray(id);
      if(items != null) {
        for(Iterator it = ((List) e.getValue()).iterator(); it.hasNext();) {
          Combo combo = (Combo) it.next();
          String text = combo.getText();
          combo.setItems(items);
          if (text.length() > 0) {
            // setItems() clears the text input, so we need to restore it
            combo.setText(text);
          }
        }
      }
    }
  }

  /** Saves the input history into the dialog settings. */
  private void saveInputHistory() {
    for(Iterator i = fieldsWithHistory.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Map.Entry) i.next();
      String id = (String) e.getKey();
      
      Set history = new LinkedHashSet(MAX_HISTORY);
      
      for(Iterator it = ((List) e.getValue()).iterator(); it.hasNext();) {
        Combo combo = (Combo) it.next();
        
        String lastValue = combo.getText();
        if ( lastValue!=null && lastValue.trim().length() > 0 ) {
          history.add(lastValue);
        }
      }

      Combo combo = (Combo) ((List) e.getValue()).iterator().next();
      String[] items = combo.getItems();
      for(int j = 0; j < items.length && history.size() < MAX_HISTORY; j++ ) {
        history.add(items[j]);
      }
      
      dialogSettings.put(id, (String[]) history.toArray(new String[history.size()]));
    }
  }

  /** Adds an input control to the list of fields to save. */
  protected void addFieldWithHistory(String id, Combo combo) {
    if(combo!=null) {
      List combos = (List) fieldsWithHistory.get(id);
      if(combos==null) {
        combos = new ArrayList();
        fieldsWithHistory.put(id, combos);
      }
      combos.add(combo);
    }
  }
}
