/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.index;

import java.io.File;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;

import org.maven.ide.eclipse.embedder.ArtifactKey;

/**
 * @author igor
 */
public interface IIndex {

  // search keys 

  public static final String SEARCH_GROUP = "groupId";

  public static final String SEARCH_ARTIFACT = "artifact";

  public static final String SEARCH_PLUGIN = "plugin";

  public static final String SEARCH_ARCHETYPE = "archetype";

  public static final String SEARCH_CLASS_NAME = "className";

  public static final String SEARCH_PACKAGING = "packaging";

  public static final String SEARCH_MD5 = "md5";

  public static final String SEARCH_SHA1 = "sha1";

  // search classifiers

  public static final int SEARCH_JARS = 1 << 0;

  public static final int SEARCH_JAVADOCS = 1 << 1;

  public static final int SEARCH_SOURCES = 1 << 2;

  public static final int SEARCH_TESTS = 1 << 3;

  public static final int SEARCH_ALL = 15;

  // availability flags
  
  public static final int PRESENT = 1;
  
  public static final int NOT_PRESENT = 0;
  
  public static final int NOT_AVAILABLE = 2;

  // index queries

  public IndexedArtifactFile getIndexedArtifactFile(ArtifactKey artifact) throws CoreException;

  public IndexedArtifactFile identify(File file) throws CoreException;

  public Collection<IndexedArtifact> find(String groupId, String artifactId, String version,
      String packaging) throws CoreException;
}
