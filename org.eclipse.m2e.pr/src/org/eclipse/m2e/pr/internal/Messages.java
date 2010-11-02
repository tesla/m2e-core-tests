/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.pr.internal;

import org.eclipse.osgi.util.NLS;


/**
 * @author mkleint
 *
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.m2e.pr.internal.messages"; //$NON-NLS-1$

  public static String ConfigurationDetailsSource_date;

  public static String ConfigurationDetailsSource_error;

  public static String Data_console;

  public static String Data_eclipse_config;

  public static String Data_global_settings;

  public static String Data_project_files;

  public static String Data_project_sources;

  public static String Data_user_settings;

  public static String DataGatherer_error;

  public static String DataGatherer_monitor_encrypting;

  public static String DataGatherer_monitor_gather;

  public static String EffectivePomSource_error1;

  public static String EffectivePomSource_error2;

  public static String EffectivePomSource_error3;

  public static String ProblemDescriptionPage_btnScreenshot;

  public static String ProblemDescriptionPage_description;

  public static String ProblemDescriptionPage_error_empty_desc;

  public static String ProblemDescriptionPage_error_empty_summary;

  public static String ProblemDescriptionPage_error_empty_summary_desc;

  public static String ProblemDescriptionPage_lblDesc;

  public static String ProblemDescriptionPage_lblLink;

  public static String ProblemDescriptionPage_lblProjects;

  public static String ProblemDescriptionPage_lblSummary;

  public static String ProblemDescriptionPage_title;

  public static String ProblemReportingMenuCreator_action_report;
  public static String ProblemReportingSelectionPage_btnBrowse;

  public static String ProblemReportingSelectionPage_btnConsole;

  public static String ProblemReportingSelectionPage_btnEclipseConfig;

  public static String ProblemReportingSelectionPage_btnErrorLog;

  public static String ProblemReportingSelectionPage_btnExportAll;

  public static String ProblemReportingSelectionPage_btnGlobalSettings;

  public static String ProblemReportingSelectionPage_btnPomFiles;

  public static String ProblemReportingSelectionPage_btnSources;

  public static String ProblemReportingSelectionPage_btnUserSettings;

  public static String ProblemReportingSelectionPage_desc;

  public static String ProblemReportingSelectionPage_fileDialog_text;

  public static String ProblemReportingSelectionPage_lblLocation;

  public static String ProblemReportingSelectionPage_title;
  public static String ProblemReportingWizard_job_gathering;

  public static String ProblemReportingWizard_link_success;

  public static String ProblemReportingWizard_monitor_reading;

  public static String ProblemReportingWizard_window_title;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
