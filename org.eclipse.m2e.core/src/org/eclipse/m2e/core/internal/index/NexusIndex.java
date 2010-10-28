/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.internal.index;

import java.io.File;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;

import org.sonatype.nexus.index.ArtifactInfo;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IMutableIndex;
import org.eclipse.m2e.core.index.IndexedArtifact;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.repository.IRepository;


/**
 * NexusIndex
 * 
 * @author igor
 */
public class NexusIndex implements IIndex, IMutableIndex {

  /** 
   * Repository index is disabled.
   */
  public static final String DETAILS_DISABLED = "off";

  /**
   * Only artifact index information is used. Classname index is disabled. 
   */
  public static final String DETAILS_MIN = "min";

  /**
   * Both artifact and classname indexes are used.
   */
  public static final String DETAILS_FULL = "full";

  private final NexusIndexManager indexManager;

  private final IRepository repository;

  private final String indexDetails;
  
  NexusIndex(NexusIndexManager indexManager, IRepository repository, String indexDetails) {
    this.indexManager = indexManager;
    this.repository = repository;
    this.indexDetails = indexDetails;
  }

  public String getRepositoryUrl(){
    return this.repository.getUrl();
  }

  public String getIndexDetails() {
    return this.indexDetails;
  }

  public void addArtifact(File pomFile, ArtifactKey artifactKey, long size, long date, File jarFile, int sourceExists,
      int javadocExists) {
    indexManager.addDocument(repository, pomFile, NexusIndexManager.getDocumentKey(artifactKey), size, date, jarFile, sourceExists,
        javadocExists);
  }

  public void removeArtifact(File pomFile, ArtifactKey artifactKey) {
    indexManager.removeDocument(repository, pomFile, NexusIndexManager.getDocumentKey(artifactKey));
  }

  public Collection<IndexedArtifact> find(String groupId, String artifactId, String version, String packaging)
      throws CoreException {
    BooleanQuery query = new BooleanQuery();

    if(packaging != null) {
      query.add(new TermQuery(new Term(ArtifactInfo.PACKAGING, packaging)), Occur.MUST);
    }

    if(groupId!=null) {
      // TODO remove '-' workaround once Nexus indexer is fixed
      query.add(indexManager.createQuery(ArtifactInfo.GROUP_ID, groupId.replace('-', '*')), Occur.MUST);
    }

    if(artifactId != null) {
      query.add(indexManager.createQuery(ArtifactInfo.ARTIFACT_ID, artifactId), Occur.MUST);
    }
 
    if(version != null) {
      query.add(indexManager.createQuery(ArtifactInfo.VERSION, version), Occur.MUST);
    }
 
    return indexManager.search(repository, query).values();
  }

  public IndexedArtifactFile getIndexedArtifactFile(ArtifactKey artifact) throws CoreException {
    return indexManager.getIndexedArtifactFile(repository, artifact);
  }

  public IndexedArtifactFile identify(File file) throws CoreException {
    // TODO identify in this index only
    return indexManager.identify(file);
  }

  public void updateIndex(boolean force, IProgressMonitor monitor) throws CoreException {
    indexManager.updateIndex(repository, force, monitor);
  }
  
  public void scheduleIndexUpdate(boolean force) {
    indexManager.scheduleIndexUpdate(repository, force);
  }

  public IndexedArtifactGroup[] getRootGroups() throws CoreException {
    return indexManager.getRootGroups(repository);
  }

  public boolean isUpdating() {
    return indexManager.isUpdatingIndex(repository);
  }

  public IRepository getRepository() {
    return repository;
  }

  public boolean isEnabled() {
    return DETAILS_MIN.equals(indexDetails) || DETAILS_FULL.equals(indexDetails);
  }

  public void setIndexDetails(String details) throws CoreException {
    indexManager.setIndexDetails(repository, details, null/*async*/);
  }
}
