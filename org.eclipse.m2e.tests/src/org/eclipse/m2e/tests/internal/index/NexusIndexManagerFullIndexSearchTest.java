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

package org.eclipse.m2e.tests.internal.index;

import java.io.File;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifact;
import org.eclipse.m2e.core.internal.index.NexusIndex;
import org.eclipse.m2e.core.internal.index.NexusIndexManager;
import org.eclipse.m2e.core.internal.repository.RepositoryRegistry;
import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;


/**
 * Tests search functionality within the NexusIndexManager with repos that 
 * need full indexing details.
 * @author dyocum
 */
public class NexusIndexManagerFullIndexSearchTest extends AbstractNexusIndexManagerTest {
  private static final String SETTINGS_ECLIPSE_REPO = "src/org/eclipse/m2e/tests/internal/index/public_mirror_repo_settings.xml";
  
  private static final String REPO_URL_ECLIPSE = "http://repository.sonatype.org/content/repositories/eclipse";
  
  private IMavenConfiguration mavenConfiguration = MavenPlugin.getDefault().getMavenConfiguration();
  private NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();
  private RepositoryRegistry repositoryRegistry = (RepositoryRegistry) MavenPlugin.getDefault().getRepositoryRegistry();

  protected void setUp() throws Exception {
    super.setUp();
    updateRepo(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
  }
  
  private IRepository getRepository(String repoUrl) {
    for (IRepository repository : repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_SETTINGS)) {
      if (repoUrl.equals(repository.getUrl())) {
        return repository;
      }
    }
    throw new IllegalArgumentException("Repository registry does not have repository with url=" + repoUrl);
  }

  protected void updateRepo(String repoUrl, String settingsFile) throws Exception{
    setupPublicMirror(repoUrl, settingsFile);
    waitForJobsToComplete();
    IRepository repository = getRepository(repoUrl);
    if(!NexusIndex.DETAILS_FULL.equals(indexManager.getIndexDetails(repository))){
      indexManager.setIndexDetails(repository, NexusIndex.DETAILS_FULL, new NullProgressMonitor());  
      assertEquals(NexusIndex.DETAILS_FULL, indexManager.getIndexDetails(repository));
    }
    NexusIndex index = indexManager.getIndex(repository);
    if(index != null){
      index.updateIndex(true, null);
    }
    waitForJobsToComplete();
  }
  
  protected void setupPublicMirror(String publicRepoUrl, String settingsFile) throws Exception {
    final File mirroredRepoFile = new File(settingsFile);
    assertTrue(mirroredRepoFile.exists());

    mavenConfiguration.setUserSettingsFile(mirroredRepoFile.getCanonicalPath());
    waitForJobsToComplete();
  }

  public void testClassSearch() throws Exception {
    Map<String, IndexedArtifact> search = indexManager.search("TestCase", IIndex.SEARCH_CLASS_NAME);
    assertTrue(search.size() > 0);

    Map<String, IndexedArtifact> noResultsSearch = indexManager.search("BeepBeepNoClass", IIndex.SEARCH_CLASS_NAME);
    assertTrue(noResultsSearch.size() == 0);
  }



}
