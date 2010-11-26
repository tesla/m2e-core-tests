/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.index;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;

import org.apache.maven.index.MAVEN;
import org.apache.maven.index.SearchType;

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
  public static final String DETAILS_DISABLED = "off"; //$NON-NLS-1$

  /**
   * Only artifact index information is used. Classname index is disabled.
   */
  public static final String DETAILS_MIN = "min"; //$NON-NLS-1$

  /**
   * Both artifact and classname indexes are used.
   */
  public static final String DETAILS_FULL = "full"; //$NON-NLS-1$

  private final NexusIndexManager indexManager;

  private final IRepository repository;

  private final String indexDetails;

  NexusIndex(NexusIndexManager indexManager, IRepository repository, String indexDetails) {
    this.indexManager = indexManager;
    this.repository = repository;
    this.indexDetails = indexDetails;
  }

  public String getRepositoryUrl() {
    return this.repository.getUrl();
  }

  public String getIndexDetails() {
    return this.indexDetails;
  }

  public void addArtifact(File pomFile, ArtifactKey artifactKey) {
    indexManager.addDocument(repository, pomFile, artifactKey);
  }

  public void removeArtifact(File pomFile, ArtifactKey artifactKey) {
    indexManager.removeDocument(repository, pomFile, artifactKey);
  }

  public Collection<IndexedArtifact> find(String groupId, String artifactId, String version, String packaging)
      throws CoreException {
    BooleanQuery query = new BooleanQuery();

    if(!isBlank(packaging)) {
      query.add(indexManager.constructQuery(MAVEN.PACKAGING, packaging, SearchType.EXACT), Occur.MUST);
    }

    if(!isBlank(groupId)) {
      query.add(indexManager.constructQuery(MAVEN.GROUP_ID, groupId, SearchType.SCORED), Occur.MUST);
    }

    if(!isBlank(artifactId)) {
      query.add(indexManager.constructQuery(MAVEN.ARTIFACT_ID, artifactId, SearchType.SCORED), Occur.MUST);
    }

    if(!isBlank(version)) {
      query.add(indexManager.constructQuery(MAVEN.VERSION, version, SearchType.SCORED), Occur.MUST);
    }

    return indexManager.search(repository, query).values();
  }

  private boolean isBlank(String str) {
    return str == null || str.trim().length() == 0;
  }

  public IndexedArtifactFile getIndexedArtifactFile(ArtifactKey artifact) throws CoreException {
    return indexManager.getIndexedArtifactFile(repository, artifact);
  }

  public IndexedArtifactFile identify(File file) throws CoreException {
    return indexManager.identify(repository, file);
  }

  public void updateIndex(boolean force, IProgressMonitor monitor) throws CoreException {
    indexManager.updateIndex(repository, force, monitor);
  }

  public void scheduleIndexUpdate(boolean force) {
    indexManager.scheduleIndexUpdate(repository, force);
  }

  public IndexedArtifactGroup[] getRootIndexedArtifactGroups() throws CoreException {
    return indexManager.getRootIndexedArtifactGroups(repository);
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

  public Map<String, IndexedArtifact> search(String term, String searchType) throws CoreException {
    return indexManager.search(getRepository(), term, searchType);
  }

  public Map<String, IndexedArtifact> search(String term, String searchType, int classifier) throws CoreException {
    return indexManager.search(getRepository(), term, searchType, classifier);
  }
}
