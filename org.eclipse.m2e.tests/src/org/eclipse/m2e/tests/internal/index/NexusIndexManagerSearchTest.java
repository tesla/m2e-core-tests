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

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifact;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.index.NexusIndex;
import org.eclipse.m2e.core.internal.index.NexusIndexManager;
import org.eclipse.m2e.core.internal.repository.RepositoryRegistry;
import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;


/**
 * Tests search functionality within the NexusIndexManager with repos that only
 * need minimum indexing details.
 * @author dyocum
 */
public class NexusIndexManagerSearchTest extends AbstractNexusIndexManagerTest {
  private static final String SETTINGS_ECLIPSE_REPO = "src/org/eclipse/m2e/tests/internal/index/public_mirror_repo_settings.xml";
  private static final String REPO_URL_ECLIPSE = "http://repository.sonatype.org/content/repositories/eclipse";
  
  private IMavenConfiguration mavenConfiguration = MavenPlugin.getDefault().getMavenConfiguration();
  private NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();
  private RepositoryRegistry repositoryRegistry = (RepositoryRegistry) MavenPlugin.getDefault().getRepositoryRegistry();
  
  protected void setUp() throws Exception {
    super.setUp();
    updateRepo(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
  }

  protected void setupPublicMirror(String publicRepoUrl, String settingsFile) throws Exception {
    final File mirroredRepoFile = new File(settingsFile);
    assertTrue(mirroredRepoFile.exists());

    mavenConfiguration.setUserSettingsFile(mirroredRepoFile.getCanonicalPath());
    waitForJobsToComplete();
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
    NexusIndex index = indexManager.getIndex(repository);
    if(index != null){
      index.updateIndex(true, null);
    }
    waitForJobsToComplete();
  }
  
  public void testPluginSearch() throws Exception {
    Map<String, IndexedArtifact> search = indexManager.search("maven-tycho", IIndex.SEARCH_PLUGIN);
    assertTrue(search.size() > 0);
    
    Map<String, IndexedArtifact> noResultsSearch = indexManager.search("maven-fake-plugin", IIndex.SEARCH_PLUGIN);
    assertTrue(noResultsSearch.size() == 0);
  }
  
  public void testGroupSearch() throws Exception {
    Map<String, IndexedArtifact> search = indexManager.search("org.junit", IIndex.SEARCH_GROUP);
    assertTrue(search.size() > 0);
    
    //TODO: this should probably be returning results, but it seems like there result space is too big,
    //so its not returning results now
    Map<String, IndexedArtifact> noResultsSearch = indexManager.search("orgXX", IIndex.SEARCH_GROUP);
    assertTrue(noResultsSearch.size() == 0);
  }

  public void testArtifactSearch() throws Exception {
    Map<String, IndexedArtifact> search = indexManager.search("junit", IIndex.SEARCH_ARTIFACT);
    IndexedArtifact ia = search.get("null : null : org.eclipse : org.eclipse.jdt.junit");
    assertNotNull(ia);
    boolean hasVersion = false;
    for(IndexedArtifactFile file : ia.getFiles()){
      if(file.version.startsWith("3.3.1")){
        hasVersion = true;
        break;
      }   
    }
    assertTrue(hasVersion);
    
    search = indexManager.search("junit", IIndex.SEARCH_ARTIFACT, IIndex.SEARCH_JAVADOCS);
    assertTrue(search.size()>0);
    search = indexManager.search("junit", IIndex.SEARCH_ARTIFACT, IIndex.SEARCH_SOURCES);
    assertTrue(search.size()>0);
    search = indexManager.search("junit", IIndex.SEARCH_ARTIFACT, IIndex.SEARCH_TESTS);
    assertTrue(search.size()>0);
    
    Map<String, IndexedArtifact> noResultsSearch = indexManager.search("beepbeep-nothing", IIndex.SEARCH_ARTIFACT);
    assertTrue(noResultsSearch.size() == 0);
  }

  /**
   * TODO: Stupid test at this point. Not sure what a valid sha1 search should look like.
   * @throws Exception
   */
  public void testSha1Search() throws Exception {
    Map<String, IndexedArtifact> search = indexManager.search("what-should-this-be", IIndex.SEARCH_SHA1);
    assertTrue(search.size() == 0);
  }
  
  public void testBogusSearchType() throws Exception {
    Map<String, IndexedArtifact> search = indexManager.search("commons-logging", "BadSearchType");
    assertTrue(search.size() == 0);
  }

}
