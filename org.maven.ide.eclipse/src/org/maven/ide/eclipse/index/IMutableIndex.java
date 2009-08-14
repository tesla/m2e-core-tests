/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.index;

import java.io.File;

import org.maven.ide.eclipse.embedder.ArtifactKey;

/**
 * @author igor
 */
public interface IMutableIndex extends IIndex {

  // index content manipulation

  public void addArtifact(File pomFile, ArtifactKey artifactKey, //
      long size, long date, File jarFile, int sourceExists, int javadocExists);

  public void removeArtifact(File pomFile, ArtifactKey artifactKey);

  // reindexing
  
  public void scheduleIndexUpdate(boolean force, long delay);

  public String getIndexName();
  
  public String getRepositoryUrl();
}
