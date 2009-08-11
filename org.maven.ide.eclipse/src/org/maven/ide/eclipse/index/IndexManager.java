/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.index;

import java.io.File;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;


public interface IndexManager {

  public static final int MIN_CLASS_QUERY_LENGTH = 6;

  // well-known indexes

  public static final String LOCAL_INDEX = "local";

  public static final String WORKSPACE_INDEX = "workspace";

  public abstract IMutableIndex getWorkspaceIndex();

  public abstract IMutableIndex getLocalIndex();

  /**
   * For Maven projects, returns index of all repositories configured for the project. Index includes repositories
   * defined in the project pom.xml, inherited from parent projects and defined in enabled profiles in settings.xml. If
   * project is null or is not a maven project, returns index that includes repositories defined in profiles enabled by
   * default in settings.xml.
   */
  public abstract IIndex getIndex(IProject project) throws CoreException;

  /**
   * Convenience method to search in all indexes enabled for repositories defined in settings.xml
   */
  public abstract Map<String, IndexedArtifact> search(String term, String searchType) throws CoreException;

  /**
   * Convenience method to search in all indexes enabled for repositories defined in settings.xml
   * 
   * @param term - search term
   * @param searchType - query type. Should be one of the SEARCH_* values.
   * @param classifier - the type of classifiers to search for, SEARCH_ALL, SEARCH_JAVADOCS, SEARCH_SOURCES,
   *          SEARCH_TESTS
   */
  public abstract Map<String, IndexedArtifact> search(String term, String type, int classifier) throws CoreException;

  /**
   * Convenience method to search in all indexes enabled for repositories defined in settings.xml
   */
  public abstract Map<String, IndexedArtifact> search(String indexName, String prefix, String searchGroup)
      throws CoreException;

  /**
   * Convenience method to search in all indexes enabled for repositories defined in settings.xml
   */
  public abstract IndexedArtifactFile identify(File file) throws CoreException;

}
