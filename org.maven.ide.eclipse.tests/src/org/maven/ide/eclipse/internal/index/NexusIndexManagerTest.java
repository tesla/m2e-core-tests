/*******************************************************************************
 * Copyright (c) 2009 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.index;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.index.IIndex;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.internal.repository.RepositoryRegistry;
import org.maven.ide.eclipse.repository.IRepository;
import org.maven.ide.eclipse.repository.IRepositoryRegistry;
import org.maven.ide.eclipse.tests.AsbtractMavenProjectTestCase;
import org.sonatype.nexus.index.ArtifactInfo;


/**
 * @author dyocum
 */
public class NexusIndexManagerTest extends AsbtractMavenProjectTestCase {
  private static final String SETTINGS_NO_MIRROR = "src/org/maven/ide/eclipse/internal/index/no_mirror_settings.xml";
  private static final String SETTINGS_PUBLIC_JBOSS_NOTMIRRORED = "src/org/maven/ide/eclipse/internal/index/public_nonmirrored_repo_settings.xml";
  private static final String SETTINGS_ECLIPSE_REPO = "src/org/maven/ide/eclipse/internal/index/public_mirror_repo_settings.xml";
  
  private static final String REPO_URL_ECLIPSE = "http://repository.sonatype.org/content/repositories/eclipse";
  private static final String REPO_URL_PUBLIC = "http://repository.sonatype.org/content/groups/public/";
  
  private IMavenConfiguration mavenConfiguration = MavenPlugin.lookup(IMavenConfiguration.class);
  private NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();
  private RepositoryRegistry repositoryRegistry = (RepositoryRegistry) MavenPlugin.getDefault().getRepositoryRegistry();

  private void waitForIndexJobToComplete() throws InterruptedException {
    indexManager.getIndexUpdateJob().join();
    repositoryRegistry.getBackgroundJob().join();
  }  
  
  protected void setUp() throws Exception {
    super.setUp();
    updateRepo(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
  }

  protected void setupPublicMirror(String publicRepoUrl, String settingsFile) throws Exception {
    final File mirroredRepoFile = new File(settingsFile);
    assertTrue(mirroredRepoFile.exists());
    mavenConfiguration.setUserSettingsFile(mirroredRepoFile.getCanonicalPath());
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
    waitForIndexJobToComplete();
    IRepository repository = getRepository(repoUrl);
    indexManager.setIndexDetails(repository, NexusIndex.DETAILS_FULL, monitor);  
    assertEquals(NexusIndex.DETAILS_FULL, indexManager.getIndexDetails(repository));
    
  }
  
  public void testDisableIndex() throws Exception {
    assertEquals("Local repo should default to min details", NexusIndex.DETAILS_MIN, indexManager.getIndexDetails(repositoryRegistry.getLocalRepository()));
    assertEquals("Workspace repo should default to min details", NexusIndex.DETAILS_MIN, indexManager.getIndexDetails(repositoryRegistry.getWorkspaceRepository()));

    for(IRepository info : repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_SETTINGS)) {
      String details = indexManager.getIndexDetails(info);
      if(!REPO_URL_ECLIPSE.equals(info.getUrl())) {
        if(!NexusIndex.DETAILS_DISABLED.equals(details)){
          System.out.println("index not disabled: "+info.getUrl());
        }
        assertEquals("Mirrored should be disabled", NexusIndex.DETAILS_DISABLED, details);
      }
    }
  }
  
  public void testProjectIndexes() throws Exception {
    IProject project = createExisting("resourcefiltering-p005", "projects/resourcefiltering/p005");
    waitForJobsToComplete();
    
    IIndex index = indexManager.getIndex(project);
    assertNotNull(index);
    //there should be some global indices too
    IIndex globalIndices = indexManager.getIndex((IProject)null);
    assertNotNull(globalIndices);
  }
  
  public void testWorkspaceIndex() throws Exception {
    String projectName = "resourcefiltering-p005";
    deleteProject(projectName);
    waitForJobsToComplete();
    //not indexed at startup
    IRepository workspaceRepository = repositoryRegistry.getWorkspaceRepository();
    IndexedArtifactGroup[] rootGroups = indexManager.getRootGroups(workspaceRepository);
    if(rootGroups != null && rootGroups.length > 0){
      //there should be no files in the workspace after the project delete
      assertTrue(rootGroups[0].getFiles() == null || rootGroups[0].getFiles().size() == 0);
    }

    //updateRepo(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
    createExisting(projectName, "projects/resourcefiltering/p005");
    waitForJobsToComplete();

    //after the project is created, there should be the project root group
    rootGroups = indexManager.getRootGroups(workspaceRepository);
    assertTrue(rootGroups.length > 0);
    assertEquals("resourcefiltering", rootGroups[0].getPrefix());

    Map<String, IndexedArtifact> search = indexManager.search(workspaceRepository, "p005", IIndex.SEARCH_ARTIFACT, 0);
    assertEquals(1, search.size());
    assertEquals("jar", search.values().iterator().next().getPackaging());

    deleteProject(projectName);
    waitForJobsToComplete();
    waitForIndexJobToComplete();
    assertTrue(indexManager.search(workspaceRepository, "p005", IIndex.SEARCH_ARTIFACT, 0).isEmpty());
  }
  
  public void testLocalIndex() throws Exception {
    // TODO this scans real(!) user local repository. is this necessary?
    // only if we care if this is covered with a test
    IRepository repository = repositoryRegistry.getLocalRepository();
    indexManager.updateIndex(repository, true, monitor);
    IndexedArtifactGroup[] rootGroups = indexManager.getRootGroups(repository);
    assertTrue(rootGroups.length > 0);
  }

  /**
   * Authentication was causing a failure for public (non-auth) repos. This test makes sure its ok.
   */
  public void testMngEclipse1621() throws Exception {
    final File mirroredRepoFile = new File(SETTINGS_ECLIPSE_REPO);
    assertTrue(mirroredRepoFile.exists());

    mavenConfiguration.setUserSettingsFile(mirroredRepoFile.getCanonicalPath());
    waitForIndexJobToComplete();

    //this failed with the bug in authentication (NPE) in NexusIndexManager
    IndexedArtifactGroup[] rootGroups = indexManager.getRootGroups(getRepository(REPO_URL_ECLIPSE));
    assertTrue(rootGroups.length > 0);
  }

  /**
   * Simply make sure the repositories list comes back for an imported project
   * @throws Exception
   */

  
  public void testIndexedArtifactGroups() throws Exception {
    String publicName = "nexus";
    final File mirroredRepoFile = new File(SETTINGS_ECLIPSE_REPO);
    
    assertTrue(mirroredRepoFile.exists());

    mavenConfiguration.setUserSettingsFile(mirroredRepoFile.getCanonicalPath());
    waitForIndexJobToComplete();
    
    IRepository repository = getRepository(REPO_URL_ECLIPSE);
    IndexedArtifactGroup[] rootGroups = indexManager.getRootGroups(repository);
    assertTrue(rootGroups.length > 0);

    IndexedArtifactGroup iag = new IndexedArtifactGroup(repository, "org.junit");
    IndexedArtifactGroup resolveGroup = indexManager.resolveGroup(iag);
    assertTrue(resolveGroup.getFiles().size() > 0);
    
    IndexedArtifactGroup iag2 = new IndexedArtifactGroup(repository, "org.junit.fizzle");
    IndexedArtifactGroup resolveGroup2 = indexManager.resolveGroup(iag2);
    assertTrue(resolveGroup2.getFiles().size() == 0);

    ArtifactInfo info = new ArtifactInfo(REPO_URL_ECLIPSE, "org.junit", "junit", "3.8.1", "jar");
    IndexedArtifactFile indexedArtifactFile = indexManager.getIndexedArtifactFile(info);
    assertNotNull(indexedArtifactFile);
  }

  public void testIndexedPublicArtifactGroups() throws Exception {
//    updateRepo(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
    Map<String, IndexedArtifact> search = indexManager.search("junit", IIndex.SEARCH_ARTIFACT);
    IndexedArtifact ia = search.get("null : null : org.eclipse : org.eclipse.jdt.junit");
    assertNotNull(ia);
    String version = null;
    String group = null;
    String artifact = null;
    String classifier = null;
    for(IndexedArtifactFile file : ia.getFiles()){  
      if(file.version.startsWith("3.3.1")){
        version = file.version;
        group = file.group;
        artifact = file.artifact;
        classifier = file.classifier;
      }   
    }
    //trying to make sure that search and getIndexedArtifactFile stay consistent - if one
    //finds a result, the other should as well
    ArtifactKey key = new ArtifactKey(group, artifact, version, classifier);
    IndexedArtifactFile indexedArtifactFile = indexManager.getIndexedArtifactFile(getRepository(REPO_URL_ECLIPSE), key);
    assertNotNull(indexedArtifactFile);

  }
  
  public void testPublicMirror() throws Exception {
    List<IRepository> repositories = repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_SETTINGS);
    assertEquals(2, repositories.size());
    IRepository eclipseRepo = null;
    for(IRepository repo : repositories){
      if(REPO_URL_ECLIPSE.equals(repo.getUrl())){
        eclipseRepo = repo;
      } else {
        assertNull(indexManager.getIndexingContext(repo));
      }
    }
    assertNotNull(eclipseRepo);
    assertNotNull(indexManager.getIndexingContext(eclipseRepo));
    
    //make sure that the junit jar can be found in the public repo
    NexusIndex index = indexManager.getIndex(eclipseRepo);
    assertNotNull(index);
    Collection<IndexedArtifact> junitArtifact = index.find("junit", "junit", "3.8.1", "jar");
    assertTrue(junitArtifact.size() > 0);
  }
  public void testNoMirror() throws Exception {
    
    final File settingsFile = new File(SETTINGS_NO_MIRROR);
    assertTrue(settingsFile.exists());

    mavenConfiguration.setUserSettingsFile(settingsFile.getCanonicalPath());
    waitForIndexJobToComplete();
    
    List<IRepository> repositories = repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_SETTINGS);
    assertEquals(3, repositories.size());
    for(IRepository info : repositories){
      assertTrue(info.getMirrorId() == null);
      assertTrue(info.getMirrorOf() == null);
    }

    NexusIndex workspaceIndex = indexManager.getIndex(repositoryRegistry.getWorkspaceRepository());
    assertNotNull(workspaceIndex);
    
    NexusIndex localIndex = indexManager.getIndex(repositoryRegistry.getLocalRepository());
    assertNotNull(localIndex);
  }

  public void testPublicNonMirrored() throws Exception {
    final File nonMirroredRepoFile = new File(
        SETTINGS_PUBLIC_JBOSS_NOTMIRRORED);
    assertTrue(nonMirroredRepoFile.exists());

    mavenConfiguration.setUserSettingsFile(nonMirroredRepoFile.getCanonicalPath());
    waitForIndexJobToComplete();

    List<IRepository> repositories = repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_SETTINGS);
    assertEquals(3, repositories.size());
    for(IRepository repo :repositories){
      if("http://repository.sonatype.org/content/repositories/eclipse-snapshots/".equals(repo.getUrl())){
        assertNotNull(indexManager.getIndexingContext(repo));
      } else if(REPO_URL_ECLIPSE.equals(repo.getUrl())){
        assertNotNull(indexManager.getIndexingContext(repo));
      } else {
        assertNull(indexManager.getIndexingContext(repo));
      }
    }
  }

//  public void testIndexesExtensionPoint() throws Exception {
//    List<IRepository> repositories = repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_UNKNOWN);
//
//    //
//    assertEquals(1, repositories.size());
//    
//    
//    IRepository repository = repositories.get(0);
//    assertEquals("file:testIndex", repository.getUrl());
//    
//    NexusIndex index = indexManager.getIndex(repository);
//    assertEquals(NexusIndex.DETAILS_FULL, index.getIndexDetails());
//  }
  
}
