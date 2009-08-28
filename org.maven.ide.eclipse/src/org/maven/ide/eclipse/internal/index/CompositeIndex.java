/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.index;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.index.IIndex;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;


/**
 * CompositeIndex
 * 
 * @author igor
 */
public class CompositeIndex implements IIndex {

  private List<IIndex> indexes;

  public CompositeIndex(List<IIndex> indexes) {
    this.indexes = indexes;
  }

  public IndexedArtifactFile getIndexedArtifactFile(ArtifactKey artifact) throws CoreException {
    for(IIndex index : indexes) {
      IndexedArtifactFile aif = index.getIndexedArtifactFile(artifact);
      if(aif != null) {
        // first one wins
        return aif;
      }
    }

    // did not find anything
    return null;
  }

  public IndexedArtifactFile identify(File file) throws CoreException {
    for(IIndex index : indexes) {
      IndexedArtifactFile aif = index.identify(file);
      if(aif != null) {
        // first one wins
        return aif;
      }
    }

    // did not find anything
    return null;
  }

  public Collection<IndexedArtifact> find(String groupId, String artifactId, String version, String packaging)
      throws CoreException {
    Set<IndexedArtifact> result = new LinkedHashSet<IndexedArtifact>();
    for(IIndex index : indexes) {
      Collection<IndexedArtifact> findResults = index.find(groupId, artifactId, version, packaging);
      if(findResults != null){
        result.addAll(findResults);
      }
    }
    return result;
  }

}
