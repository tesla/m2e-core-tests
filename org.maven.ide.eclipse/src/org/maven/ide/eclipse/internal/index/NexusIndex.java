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

import org.eclipse.core.runtime.CoreException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.index.IIndex;
import org.maven.ide.eclipse.index.IMutableIndex;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;


/**
 * NexusIndex
 * 
 * @author igor
 */
public class NexusIndex implements IIndex, IMutableIndex {

  private NexusIndexManager indexManager;

  private String repositoryUrl;
  
  private String indexName;

  public NexusIndex(NexusIndexManager indexManager, String indexName, String repositoryUrl) {
    this.indexName = indexName;
    this.indexManager = indexManager;
    this.repositoryUrl = repositoryUrl;
    
  }

  public String getIndexName(){
    return indexName;
  }
  
  public void addArtifact(File pomFile, ArtifactKey artifactKey, long size, long date, File jarFile, int sourceExists,
      int javadocExists) {
    indexManager.addDocument(repositoryUrl, pomFile, NexusIndexManager.getDocumentKey(artifactKey), size, date, jarFile, sourceExists,
        javadocExists);
  }

  public void removeArtifact(File pomFile, ArtifactKey artifactKey) {
    indexManager.removeDocument(repositoryUrl, pomFile, NexusIndexManager.getDocumentKey(artifactKey));
  }

  public Collection<IndexedArtifact> find(String groupId, String artifactId, String version, String packaging)
      throws CoreException {
    BooleanQuery query = new BooleanQuery();

    if(packaging != null) {
      query.add(new TermQuery(new Term(NexusIndexManager.FIELD_PACKAGING, packaging)), Occur.MUST);
    }

    if(groupId!=null) {
      // TODO remove '-' workaround once Nexus indexer is fixed
      query.add(indexManager.createQuery(NexusIndexManager.FIELD_GROUP_ID, groupId.replace('-', '*')), Occur.MUST);
    }

    if(artifactId != null) {
      query.add(indexManager.createQuery(NexusIndexManager.FIELD_ARTIFACT_ID, artifactId), Occur.MUST);
    }
 
    if(version != null) {
      query.add(indexManager.createQuery(NexusIndexManager.FIELD_VERSION, version), Occur.MUST);
    }
 
    return indexManager.search(repositoryUrl, query).values();
  }

  public IndexedArtifactFile getIndexedArtifactFile(ArtifactKey artifact) throws CoreException {
    return indexManager.getIndexedArtifactFile(repositoryUrl, artifact);
  }

  public IndexedArtifactFile identify(File file) throws CoreException {
    // TODO identify in this index only
    return indexManager.identify(file);
  }

  public void scheduleIndexUpdate(boolean force, long delay) {
    // TODO Auto-generated method scheduleIndexUpdate
    
  }
}
