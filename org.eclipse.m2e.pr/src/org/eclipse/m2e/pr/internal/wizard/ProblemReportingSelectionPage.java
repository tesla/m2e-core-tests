/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.pr.internal.wizard;

import java.util.EnumSet;
import java.util.Set;

import org.eclipse.m2e.core.wizards.AbstractMavenWizardPage;
import org.eclipse.m2e.pr.internal.ProblemReportingImages;
import org.eclipse.m2e.pr.internal.data.Data;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;


/**
 * A problem reporting selection page
 * 
 * @author Eugene Kuleshov
 */
public class ProblemReportingSelectionPage extends AbstractMavenWizardPage {

  String location;

  Set<Data> dataSet;

  protected ProblemReportingSelectionPage() {
    super("problemReportingSelectionPage");
    setTitle("Select data to include");
    setDescription("Select data to include into reporting bundle");
    setImageDescriptor(ProblemReportingImages.REPORT_WIZARD);

    dataSet = EnumSet.allOf(Data.class);
    dataSet.remove(Data.MAVEN_SOURCES);
  }

  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 4;
    composite.setLayout(gridLayout);
    setControl(composite);

    final Button exportAllDataButton = new Button(composite, SWT.CHECK);
    exportAllDataButton.setData("name", "exportAllDataButton");
    exportAllDataButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
    exportAllDataButton.setText("Export &all data");
    exportAllDataButton.setSelection(false);

    final Button mavenUsersSettingsButton = new Button(composite, SWT.CHECK);
    GridData gd_mavenUsersSettingsButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    gd_mavenUsersSettingsButton.horizontalIndent = 15;
    mavenUsersSettingsButton.setLayoutData(gd_mavenUsersSettingsButton);
    mavenUsersSettingsButton.setData("name", "mavenUsersSettingsButton");
    mavenUsersSettingsButton.setText("Maven user's settings.xml");
    mavenUsersSettingsButton.setSelection(dataSet.contains(Data.MAVEN_USER_SETTINGS));
    mavenUsersSettingsButton.addSelectionListener(new SourceSelectionAdapter(mavenUsersSettingsButton, //
        dataSet, Data.MAVEN_USER_SETTINGS));

    final Button eclipseConfigurationButton = new Button(composite, SWT.CHECK);
    GridData gd_eclipseConfigurationButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    gd_eclipseConfigurationButton.horizontalIndent = 25;
    eclipseConfigurationButton.setLayoutData(gd_eclipseConfigurationButton);
    eclipseConfigurationButton.setData("name", "eclipseConfigurationButton");
    eclipseConfigurationButton.setText("Eclipse configuration details");
    eclipseConfigurationButton.setSelection(dataSet.contains(Data.ECLIPSE_CONFIG));
    eclipseConfigurationButton.addSelectionListener(new SourceSelectionAdapter(eclipseConfigurationButton, //
        dataSet, Data.ECLIPSE_CONFIG));

    final Button mavenGlobalSettingsButton = new Button(composite, SWT.CHECK);
    GridData gd_mavenGlobalSettingsButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    gd_mavenGlobalSettingsButton.horizontalIndent = 15;
    mavenGlobalSettingsButton.setLayoutData(gd_mavenGlobalSettingsButton);
    mavenGlobalSettingsButton.setData("name", "mavenGlobalSettingsButton");
    mavenGlobalSettingsButton.setText("Maven global settings.xml");
    mavenGlobalSettingsButton.setSelection(dataSet.contains(Data.MAVEN_GLOBAL_SETTINGS));
    mavenGlobalSettingsButton.addSelectionListener(new SourceSelectionAdapter(mavenGlobalSettingsButton, //
        dataSet, Data.MAVEN_GLOBAL_SETTINGS));

    final Button eclipseLogButton = new Button(composite, SWT.CHECK);
    GridData gd_eclipseLogButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    gd_eclipseLogButton.horizontalIndent = 25;
    eclipseLogButton.setLayoutData(gd_eclipseLogButton);
    eclipseLogButton.setData("name", "eclipseLogButton");
    eclipseLogButton.setText("Eclipse error .log");
    eclipseLogButton.setSelection(dataSet.contains(Data.ECLIPSE_LOG));
    eclipseLogButton.addSelectionListener(new SourceSelectionAdapter(eclipseLogButton, //
        dataSet, Data.ECLIPSE_LOG));

    final Button mavenConsoleButton = new Button(composite, SWT.CHECK);
    GridData gd_mavenConsoleButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1);
    gd_mavenConsoleButton.horizontalIndent = 15;
    mavenConsoleButton.setLayoutData(gd_mavenConsoleButton);
    mavenConsoleButton.setData("name", "mavenConsoleButton");
    mavenConsoleButton.setText("Maven console");
    mavenConsoleButton.setSelection(dataSet.contains(Data.MAVEN_CONSOLE));
    mavenConsoleButton.addSelectionListener(new SourceSelectionAdapter(mavenConsoleButton, //
        dataSet, Data.MAVEN_CONSOLE));

    final Button mavenPomFilesButton = new Button(composite, SWT.CHECK);
    GridData gd_mavenPomFilesButton = new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1);
    gd_mavenPomFilesButton.horizontalIndent = 15;
    mavenPomFilesButton.setLayoutData(gd_mavenPomFilesButton);
    mavenPomFilesButton.setData("name", "mavenPomFilesButton");
    mavenPomFilesButton.setText("Maven pom files");
    mavenPomFilesButton.setSelection(dataSet.contains(Data.MAVEN_POM_FILES));
    mavenPomFilesButton.addSelectionListener(new SourceSelectionAdapter(mavenPomFilesButton, //
        dataSet, Data.MAVEN_POM_FILES));

    final Button mavenSourcesButton = new Button(composite, SWT.CHECK);
    GridData gd_completeProjectSourceButton = new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1);
    gd_completeProjectSourceButton.horizontalIndent = 15;
    mavenSourcesButton.setLayoutData(gd_completeProjectSourceButton);
    mavenSourcesButton.setData("name", "completeProjectSourceButton");
    mavenSourcesButton.setText("Complete project &sources");
    mavenSourcesButton.setSelection(dataSet.contains(Data.MAVEN_SOURCES));
    mavenSourcesButton.addSelectionListener(new SourceSelectionAdapter(mavenSourcesButton, //
        dataSet, Data.MAVEN_SOURCES));

    Label locationLabel = new Label(composite, SWT.NONE);
    GridData gd_locationLabel = new GridData();
    gd_locationLabel.verticalIndent = 15;
    locationLabel.setLayoutData(gd_locationLabel);
    locationLabel.setData("name", "locationLabel");
    locationLabel.setText("&Location:");

    final Combo locationCombo = new Combo(composite, SWT.NONE);
    locationCombo.setData("name", "locationText");
    GridData gd_locationText = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    gd_locationText.verticalIndent = 15;
    locationCombo.setLayoutData(gd_locationText);
    locationCombo.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        location = locationCombo.getText();
      }
    });
    addFieldWithHistory("bundleLocation", locationCombo);

    Button browseButton = new Button(composite, SWT.NONE);
    GridData gd_browseButton = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
    gd_browseButton.verticalIndent = 15;
    browseButton.setLayoutData(gd_browseButton);
    browseButton.setData("name", "browseButton");
    browseButton.setText("&Browse...");
    browseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(final SelectionEvent e) {
        FileDialog dialog = new FileDialog(getContainer().getShell(), SWT.SAVE);
        dialog.setText("Select file name to save problem reporting bundle");
        String fileName = dialog.open();
        if(fileName != null) {
          locationCombo.setText(fileName);
        }
      }
    });

    exportAllDataButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        boolean selectAll = exportAllDataButton.getSelection();
        eclipseConfigurationButton.setEnabled(!selectAll);
        eclipseLogButton.setEnabled(!selectAll);
        mavenUsersSettingsButton.setEnabled(!selectAll);
        mavenGlobalSettingsButton.setEnabled(!selectAll);
        mavenConsoleButton.setEnabled(!selectAll);
        mavenPomFilesButton.setEnabled(!selectAll);
        mavenSourcesButton.setEnabled(!selectAll);

        if(selectAll) {
          dataSet.addAll(EnumSet.allOf(Data.class));
          dataSet.remove(Data.MAVEN_POM_FILES);
        } else {
          dataSet.clear();
          if(eclipseConfigurationButton.getSelection()) {
            dataSet.add(Data.ECLIPSE_CONFIG);
          }
          if(eclipseLogButton.getSelection()) {
            dataSet.add(Data.ECLIPSE_LOG);
          }
          if(mavenUsersSettingsButton.getSelection()) {
            dataSet.add(Data.MAVEN_USER_SETTINGS);
          }
          if(mavenGlobalSettingsButton.getSelection()) {
            dataSet.add(Data.MAVEN_GLOBAL_SETTINGS);
          }
          if(mavenConsoleButton.getSelection()) {
            dataSet.add(Data.MAVEN_CONSOLE);
          }
          if(mavenPomFilesButton.getSelection()) {
            dataSet.add(Data.MAVEN_POM_FILES);
          }
          if(mavenSourcesButton.getSelection()) {
            dataSet.add(Data.MAVEN_SOURCES);
            dataSet.remove(Data.MAVEN_POM_FILES);
          }
        }
      }
    });

  }

  public Set<Data> getDataSet() {
    return dataSet;
  }

  public String getLocation() {
    return location;
  }

  static final class SourceSelectionAdapter extends SelectionAdapter {
    private final Button mavenUsersSettingsButton;

    private final Set<Data> dataSet;

    private final Data data;

    private SourceSelectionAdapter(Button mavenUsersSettingsButton, Set<Data> dataSet, Data data) {
      this.mavenUsersSettingsButton = mavenUsersSettingsButton;
      this.dataSet = dataSet;
      this.data = data;
    }

    public void widgetSelected(SelectionEvent e) {
      if(mavenUsersSettingsButton.getSelection()) {
        dataSet.add(data);
        if(dataSet.contains(Data.MAVEN_SOURCES)) {
          dataSet.remove(Data.MAVEN_POM_FILES);
        }
      } else {
        dataSet.remove(data);
      }
    }
  }

}
