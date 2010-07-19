/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.core;

import org.eclipse.core.runtime.QualifiedName;

/**
 * Maven Constants
 * 
 * @author Eugene Kuleshov
 */
public interface IMavenConstants {

  public static final String PLUGIN_ID = "org.maven.ide.eclipse"; //$NON-NLS-1$

  public static final String NATURE_ID = PLUGIN_ID + ".maven2Nature"; //$NON-NLS-1$

  public static final String BUILDER_ID = PLUGIN_ID + ".maven2Builder"; //$NON-NLS-1$

  public static final String MARKER_ID = PLUGIN_ID + ".maven2Problem"; //$NON-NLS-1$

  public static final String POM_FILE_NAME = "pom.xml"; //$NON-NLS-1$

  public static final String PREFERENCE_PAGE_ID = PLUGIN_ID + ".MavenProjectPreferencePage";
  
  public static final String NO_WORKSPACE_PROJECTS = "noworkspace"; //$NON-NLS-1$

  public static final String ACTIVE_PROFILES = "profiles"; //$NON-NLS-1$

  public static final String FILTER_RESOURCES = "filterresources"; //$NON-NLS-1$

  public static final String JAVADOC_CLASSIFIER = "javadoc"; //$NON-NLS-1$

  public static final String SOURCES_CLASSIFIER = "sources"; //$NON-NLS-1$

  /** 
   * Session property key used to indicate that full maven build was requested for a project.
   * It is not intended to be used by clients directly.
   */
  public static final QualifiedName FULL_MAVEN_BUILD = new QualifiedName(PLUGIN_ID, "fullBuild");

  /**
   * The name of the folder containing metadata information for the workspace.
   */
  public static final String METADATA_FOLDER = ".metadata"; //$NON-NLS-1$

  public static final String INDEX_UPDATE_PROP = "indexUpdate";

}
