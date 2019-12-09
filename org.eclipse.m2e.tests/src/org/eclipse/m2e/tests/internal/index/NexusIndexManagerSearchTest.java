/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.internal.index;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.index.IIndex;
import org.eclipse.m2e.core.internal.index.IndexedArtifact;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.index.SearchExpression;
import org.eclipse.m2e.core.internal.index.SourcedSearchExpression;
import org.eclipse.m2e.core.internal.index.UserInputSearchExpression;
import org.eclipse.m2e.core.internal.index.nexus.NexusIndex;
import org.eclipse.m2e.core.internal.index.nexus.NexusIndexManager;
import org.eclipse.m2e.core.internal.repository.RepositoryRegistry;
import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;


/**
 * Tests search functionality within the NexusIndexManager with repos that only need minimum indexing details.
 * 
 * @author dyocum
 */
@SuppressWarnings("restriction")
public class NexusIndexManagerSearchTest extends AbstractNexusIndexManagerTest {
  private static final String SETTINGS_ECLIPSE_REPO = "src/org/eclipse/m2e/tests/internal/index/public_mirror_repo_settings.xml";

  private static final String REPO_URL_ECLIPSE = "http://repository.sonatype.org/content/repositories/eclipse";

  private IMavenConfiguration mavenConfiguration = MavenPlugin.getMavenConfiguration();

  private NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getIndexManager();

  private RepositoryRegistry repositoryRegistry = (RepositoryRegistry) MavenPlugin.getRepositoryRegistry();

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
    for(IRepository repository : repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_SETTINGS)) {
      if(repoUrl.equals(repository.getUrl())) {
        return repository;
      }
    }
    throw new IllegalArgumentException("Repository registry does not have repository with url=" + repoUrl);
  }

  protected void updateRepo(String repoUrl, String settingsFile) throws Exception {
    setupPublicMirror(repoUrl, settingsFile);
    waitForJobsToComplete();
    IRepository repository = getRepository(repoUrl);
    NexusIndex index = indexManager.getIndex(repository);
    if(index != null) {
      index.updateIndex(true, null);
    }
    waitForJobsToComplete();
  }

  public void testPluginSearch() throws Exception {
    Map<String, IndexedArtifact> search = indexManager.search(new UserInputSearchExpression("maven-tycho"),
        IIndex.SEARCH_PLUGIN);
    assertTrue(search.size() > 0);

    Map<String, IndexedArtifact> noResultsSearch = indexManager.search(
        new SourcedSearchExpression("maven-fake-plugin"), IIndex.SEARCH_PLUGIN);
    assertTrue(noResultsSearch.size() == 0);
  }

  public void testGroupSearch() throws Exception {
    Map<String, IndexedArtifact> search = indexManager.search(new SourcedSearchExpression("org.junit"),
        IIndex.SEARCH_GROUP);
    assertTrue(search.size() > 0);

    //TODO: this should probably be returning results, but it seems like there result space is too big,
    //so its not returning results now
    Map<String, IndexedArtifact> noResultsSearch = indexManager.search(new UserInputSearchExpression("orgXX"),
        IIndex.SEARCH_GROUP);
    assertTrue(noResultsSearch.size() == 0);
  }

  public void testArtifactSearch() throws Exception {
    Map<String, IndexedArtifact> search = indexManager.search(new UserInputSearchExpression("junit"),
        IIndex.SEARCH_ARTIFACT);
    IndexedArtifact ia = search.get("null : null : org.eclipse : org.eclipse.jdt.junit");
    assertNotNull(ia);
    boolean hasVersion = false;
    for(IndexedArtifactFile file : ia.getFiles()) {
      if(file.version.startsWith("3.3.1")) {
        hasVersion = true;
        break;
      }
    }
    assertTrue(hasVersion);

    search = indexManager
        .search(new UserInputSearchExpression("junit"), IIndex.SEARCH_ARTIFACT, IIndex.SEARCH_JAVADOCS);
    assertTrue(search.size() > 0);
    search = indexManager.search(new UserInputSearchExpression("junit"), IIndex.SEARCH_ARTIFACT, IIndex.SEARCH_SOURCES);
    assertTrue(search.size() > 0);
    search = indexManager.search(new UserInputSearchExpression("junit"), IIndex.SEARCH_ARTIFACT, IIndex.SEARCH_TESTS);
    assertTrue(search.size() > 0);

    Map<String, IndexedArtifact> noResultsSearch = indexManager.search(
        new UserInputSearchExpression("beepbeep-nothing"), IIndex.SEARCH_ARTIFACT);
    assertTrue(noResultsSearch.size() == 0);
  }

  /**
   * TODO: Stupid test at this point. Not sure what a valid sha1 search should look like.
   * 
   * @throws Exception
   */
  public void testSha1Search() throws Exception {
    Map<String, IndexedArtifact> search = indexManager.search(new SourcedSearchExpression("what-should-this-be"),
        IIndex.SEARCH_SHA1);
    assertTrue(search.size() == 0);
  }

  public void testBogusSearchType() throws Exception {
    Map<String, IndexedArtifact> search = indexManager.search(new SourcedSearchExpression("commons-logging"),
        "BadSearchType");
    assertTrue(search.size() == 0);
  }

  public void testSearchGroups() throws CoreException {
    try {
      Collection<IndexedArtifact> result = indexManager.getIndex((IProject) null).find(new SourcedSearchExpression(""),
          null, null, null);
//      assertTrue(String.format("Wrong result set returned! (size=%s)", new Object[] {result.size()}),
//          result.size() == 0);
      fail("We should not get here!");
    } catch(RuntimeException e) {
      if(!"The expression cannot be empty!".equals(e.getMessage())) {
        throw e;
      }
    }
  }

  public void testSearchGroups2() throws CoreException {
    Collection<IndexedArtifact> result = indexManager.getIndex((IProject) null).find((SearchExpression) null,
        (SearchExpression) null, (SearchExpression) null, (SearchExpression) null);
    assertTrue(String.format("Wrong result set returned! (size=%s)", new Object[] {result.size()}), result.size() == 0);
  }

  public void testSearchGroups3() throws CoreException {
    Collection<IndexedArtifact> result = indexManager.getIndex((IProject) null).find(
        new UserInputSearchExpression("org.ju"), null, null, null);
    assertTrue(String.format("Wrong result set returned! (size=%s)", new Object[] {result.size()}), result.size() > 0);
  }

  public void testSorting() throws CoreException {
    Collection<IndexedArtifact> result = indexManager.getIndex((IProject) null).find(
        new UserInputSearchExpression("junit"), null, null, null);
    assertTrue(result.size() > 1);

    Iterator<IndexedArtifact> iterator = result.iterator();
    IndexedArtifact previous = iterator.next();
    while(iterator.hasNext()) {
      IndexedArtifact indexedArtifact = iterator.next();
      assertTrue(previous.compareTo(indexedArtifact) < 0);
      previous = indexedArtifact;
    }
  }
}
