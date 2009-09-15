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
  private static final String SETTINGS_WITH_PUBLIC = "src/org/maven/ide/eclipse/internal/index/public_settings.xml";
  private static final String SETTINGS_ECLIPSE_REPO = "src/org/maven/ide/eclipse/internal/index/public_mirror_repo_settings.xml";
  
  private static final String REPO_URL_ECLIPSE = "http://repository.sonatype.org/content/repositories/eclipse";
  private static final String REPO_URL_PUBLIC = "http://repository.sonatype.org/content/groups/public/";
  
  private IMavenConfiguration mavenConfiguration = MavenPlugin.lookup(IMavenConfiguration.class);
  private NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();
  private RepositoryRegistry repositoryRegistry = (RepositoryRegistry) MavenPlugin.getDefault().getRepositoryRegistry();

  private void waitForIndexJobToComplete() throws InterruptedException {
    repositoryRegistry.getBackgroundJob().join();
    indexManager.getIndexUpdateJob().join();
  }  
  
  protected void setupPublicMirror(String publicRepoUrl, String settingsFile) throws Exception {
    final File mirroredRepoFile = new File(settingsFile);
    assertTrue(mirroredRepoFile.exists());

    mavenConfiguration.setUserSettingsFile(mirroredRepoFile.getCanonicalPath());
    waitForIndexJobToComplete();
  }


  public void testClassSearch() throws Exception {
    updateRepo(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
    indexManager.scheduleIndexUpdate(getRepository(REPO_URL_ECLIPSE), true);
    waitForIndexJobToComplete();
    Map<String, IndexedArtifact> search = indexManager.search("TestCase", IIndex.SEARCH_CLASS_NAME);
    assertTrue(search.size() > 0);

    Map<String, IndexedArtifact> noResultsSearch = indexManager.search("BeepBeepNoClass", IIndex.SEARCH_CLASS_NAME);
    assertTrue(noResultsSearch.size() == 0);
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
  
  public void testPluginSearch() throws Exception {
    //updateRepo(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
    Map<String, IndexedArtifact> search = indexManager.search("maven-tycho", IIndex.SEARCH_PLUGIN);
    assertTrue(search.size() > 0);
    
    Map<String, IndexedArtifact> noResultsSearch = indexManager.search("maven-fake-plugin", IIndex.SEARCH_PLUGIN);
    assertTrue(noResultsSearch.size() == 0);
  }
  
  public void testGroupSearch() throws Exception {
    //updateRepo(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
    Map<String, IndexedArtifact> search = indexManager.search("org.junit", IIndex.SEARCH_GROUP);
    assertTrue(search.size() > 0);
    
    //TODO: this should probably be returning results, but it seems like there result space is too big,
    //so its not returning results now
    Map<String, IndexedArtifact> noResultsSearch = indexManager.search("orgXX", IIndex.SEARCH_GROUP);
    assertTrue(noResultsSearch.size() == 0);
  }

  
  public void testArchetypeSearch() throws Exception {
    // TODO this goes to the REAL r.s.o/public group, where index .gz > 20M! 
    updateRepo(REPO_URL_PUBLIC, SETTINGS_WITH_PUBLIC);
    //updateRepo(ECLIPSE_PUBLIC_REPO, SETTINGS_ECLIPSE_REPO);
    Map<String, IndexedArtifact> search = indexManager.search("maven-archetype-quickstart", IIndex.SEARCH_ARCHETYPE);
    assertTrue(search.size() == 1);
    
    Map<String, IndexedArtifact> j2eeSearch = indexManager.search("maven-archetype-j2ee-simple", IIndex.SEARCH_ARCHETYPE);
    assertTrue(j2eeSearch.size() == 1);
    
    Map<String, IndexedArtifact> none = indexManager.search("maven-archetype-foobar", IIndex.SEARCH_ARCHETYPE);
    assertTrue(none.size() == 0);
  }
  public void testArtifactSearch() throws Exception {
    //updateRepo(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
    
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

  public void testPackagingSearch() throws Exception {
    //updateRepo(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
    
    Map<String, IndexedArtifact> pomSearch = indexManager.search("pom", IIndex.SEARCH_PACKAGING);
    assertTrue(pomSearch.size() > 0);
    
    pomSearch = indexManager.search("pomXX", IIndex.SEARCH_PACKAGING);
    assertTrue(pomSearch.size() == 0);
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
    //updateRepo(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
    Map<String, IndexedArtifact> search = indexManager.search("commons-logging", "BadSearchType");
    assertTrue(search.size() == 0);
  }

  public void testDisableIndex() throws Exception {
    setupPublicMirror(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);

    assertEquals("Local repo should default to min details", NexusIndex.DETAILS_MIN, indexManager.getIndexDetails(repositoryRegistry.getLocalRepository()));
    assertEquals("Workspace repo should default to min details", NexusIndex.DETAILS_MIN, indexManager.getIndexDetails(repositoryRegistry.getWorkspaceRepository()));

    for(IRepository info : repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_SETTINGS)) {
      String details = indexManager.getIndexDetails(info);
      if(!REPO_URL_ECLIPSE.equals(info.getUrl())) {
        assertEquals("Mirrored should be disabled", NexusIndex.DETAILS_DISABLED, details);
      }
    }
  }  
 
  
  public void testProjectIndexes() throws Exception {
    setupPublicMirror(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
    IProject project = createExisting("resourcefiltering-p005", "projects/resourcefiltering/p005");
    waitForJobsToComplete();
    
    indexManager.updateIndex(getRepository(REPO_URL_ECLIPSE), true, monitor);
    
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
    waitForIndexJobToComplete();
    //not indexed at startup
    IndexedArtifactGroup[] rootGroups = indexManager.getRootGroups(repositoryRegistry.getWorkspaceRepository());
    if(rootGroups != null && rootGroups.length > 0){
      //there should be no files in the workspace after the project delete
      assertTrue(rootGroups[0].getFiles() == null || rootGroups[0].getFiles().size() == 0);
    }
 
    updateRepo(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
    createExisting(projectName, "projects/resourcefiltering/p005");
    waitForJobsToComplete();
    
    //after the project is created, there should be the project root group
    rootGroups = indexManager.getRootGroups(repositoryRegistry.getWorkspaceRepository());
    assertTrue(rootGroups.length > 0);
    assertEquals("resourcefiltering", rootGroups[0].getPrefix());
  }
  
  public void testLocalIndex() throws Exception {
    // TODO this scans real(!) user local repository. is this necessary?
    waitForIndexJobToComplete();
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
    updateRepo(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
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
  

  public void testPublicMirror() throws Exception {
    setupPublicMirror(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);

    List<IRepository> repositories = repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_SETTINGS);
    assertEquals(2, repositories.size());
    assertEquals(REPO_URL_ECLIPSE, repositories.get(0).getUrl());
    assertNotNull(indexManager.getIndexingContext(repositories.get(0)));
    assertNull(indexManager.getIndexingContext(repositories.get(1)));
    
    //make sure that the junit jar can be found in the public repo
    NexusIndex index = indexManager.getIndex(repositories.get(0));
    assertNotNull(index);
    Collection<IndexedArtifact> junitArtifact = index.find("junit", "junit", "3.8.1", "jar");
    assertTrue(junitArtifact.size() > 0);
  }


  public void testPublicNonMirrored() throws Exception {
    final File nonMirroredRepoFile = new File(
        SETTINGS_PUBLIC_JBOSS_NOTMIRRORED);
    assertTrue(nonMirroredRepoFile.exists());

    mavenConfiguration.setUserSettingsFile(nonMirroredRepoFile.getCanonicalPath());
    waitForIndexJobToComplete();

    List<IRepository> repositories = repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_SETTINGS);
    assertEquals(3, repositories.size());
    assertEquals("http://repository.sonatype.org/content/repositories/eclipse-snapshots/", repositories.get(0).getUrl());
    assertNotNull(indexManager.getIndexingContext(repositories.get(0)));
    assertEquals(REPO_URL_ECLIPSE, repositories.get(1).getUrl());
    assertNotNull(indexManager.getIndexingContext(repositories.get(1)));
    assertNull(indexManager.getIndexingContext(repositories.get(2)));
  }

  
}
