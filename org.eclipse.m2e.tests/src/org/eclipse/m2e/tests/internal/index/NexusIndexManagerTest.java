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
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifact;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.index.IndexedArtifactGroup;
import org.eclipse.m2e.core.internal.index.NexusIndex;
import org.eclipse.m2e.core.internal.index.NexusIndexManager;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.internal.repository.RepositoryRegistry;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;
import org.eclipse.m2e.tests.common.FileHelpers;
import org.eclipse.m2e.tests.common.HttpServer;
import org.sonatype.nexus.index.ArtifactInfo;


/**
 * @author dyocum
 */
public class NexusIndexManagerTest extends AbstractNexusIndexManagerTest {
  private static final String SETTINGS_NO_MIRROR = "src/org/eclipse/m2e/tests/internal/index/no_mirror_settings.xml";
  private static final String SETTINGS_PUBLIC_JBOSS_NOTMIRRORED = "src/org/eclipse/m2e/tests/internal/index/public_nonmirrored_repo_settings.xml";
  private static final String SETTINGS_ECLIPSE_REPO = "src/org/eclipse/m2e/tests/internal/index/public_mirror_repo_settings.xml";
  
  private static final String REPO_URL_ECLIPSE = "http://repository.sonatype.org/content/repositories/eclipse";
  private static final String REPO_URL_PUBLIC = "http://repository.sonatype.org/content/groups/public/";
  
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
    updateRepo("http://central", SETTINGS_NO_MIRROR);
    String projectName = "resourcefiltering-p009";
    createExisting(projectName, "projects/resourcefiltering/p009");
    waitForJobsToComplete();
    IProject project = workspace.getRoot().getProject(projectName);
    assertNotNull(project);
    List<IRepository> repositories = repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_PROJECT);
    //assertTrue(repositories.size() > 0);
    String projectRepo = "EclipseProjectRepo";
    boolean hasProjectRepo = false;
    for(IRepository repo : repositories){
      if(projectRepo.equals(repo.getId())){
        hasProjectRepo = true;
      }
    }
    assertTrue(hasProjectRepo);
  }

  public void testProjectSpecificThenInSettings() throws Exception {
    mavenConfiguration.setUserSettingsFile("settings.xml");
    waitForJobsToComplete();

    importProject("projects/customrepo/pom.xml", new ResolverConfiguration());
    waitForJobsToComplete();

    IRepository projectRepository = getRepository("customremote", IRepositoryRegistry.SCOPE_PROJECT);
    assertNotNull(projectRepository);

    NexusIndex index = indexManager.getIndex(projectRepository);
    assertEquals(NexusIndex.DETAILS_DISABLED, index.getIndexDetails());

    assertNull(getRepository("customremote", IRepositoryRegistry.SCOPE_SETTINGS));

    mavenConfiguration.setUserSettingsFile("src/org/eclipse/m2e/tests/internal/index/customremote_settings.xml");
    waitForJobsToComplete();

    IRepository settingsRepository = getRepository("customremote", IRepositoryRegistry.SCOPE_SETTINGS);
    assertNotNull(settingsRepository);
    assertEquals(projectRepository.getUid(), settingsRepository.getUid());

    index = indexManager.getIndex(settingsRepository);
    assertEquals(NexusIndex.DETAILS_MIN, index.getIndexDetails());
  }

  /**
   * @param repositoryId
   * @param scope
   * @return
   */
  private IRepository getRepository(String repositoryId, int scope) {
    IRepository customRepository = null;
    for (IRepository repository : repositoryRegistry.getRepositories(scope)) {
      if (repositoryId.equals(repository.getId())) {
        customRepository = repository;
        break;
      }
    }
    return customRepository;
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
    
    boolean containsResourceFiltering = false;
    for(IndexedArtifactGroup group : rootGroups){
      if("resourcefiltering".equals(group.getPrefix())){
        containsResourceFiltering = true;
      }
    }
    assertTrue(containsResourceFiltering);

    Map<String, IndexedArtifact> search = indexManager.search(workspaceRepository, "p005", IIndex.SEARCH_ARTIFACT, 0);
    assertEquals(1, search.size());
    assertEquals("jar", search.values().iterator().next().getPackaging());

    deleteProject(projectName);
    waitForJobsToComplete();
    waitForJobsToComplete();
    assertTrue(indexManager.search(workspaceRepository, "p005", IIndex.SEARCH_ARTIFACT, 0).isEmpty());
  }
  //you're right. its too painfully slow

  /**
   * Authentication was causing a failure for public (non-auth) repos. This test makes sure its ok.
   */
  public void testMngEclipse1621() throws Exception {
    final File mirroredRepoFile = new File(SETTINGS_ECLIPSE_REPO);
    assertTrue(mirroredRepoFile.exists());

    mavenConfiguration.setUserSettingsFile(mirroredRepoFile.getCanonicalPath());
    waitForJobsToComplete();

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
    waitForJobsToComplete();
    
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
    waitForJobsToComplete();
    
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
    waitForJobsToComplete();

    List<IRepository> repositories = repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_SETTINGS);
    assertEquals(3, repositories.size());
    for(IRepository repo :repositories){
      if("http://repository.sonatype.org/content/repositories/eclipse-snapshots/".equals(repo.getUrl())){
        assertNotNull(indexManager.getIndexingContext(repo));
      } else if(REPO_URL_ECLIPSE.equals(repo.getUrl())){
        assertNotNull(indexManager.getIndexingContext(repo));
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

  public void testMngEclipse1710() throws Exception {
    HttpServer httpServer = new HttpServer();
    httpServer.addResources("/", "");
    httpServer.setProxyAuth("proxyuser", "proxypass");
    httpServer.start();
    try {
      final File settingsFile = new File("target/settings-mngeclipse-1710.xml");
      FileHelpers.filterXmlFile(new File("src/org/eclipse/m2e/tests/internal/index/proxy_settings.xml"), settingsFile,
          Collections.singletonMap("@port.http@", Integer.toString(httpServer.getHttpPort())));
      assertTrue(settingsFile.exists());

      mavenConfiguration.setUserSettingsFile(settingsFile.getCanonicalPath());
      waitForJobsToComplete();

      IndexedArtifactGroup[] rootGroups = indexManager.getRootGroups(getRepository("http://bad.host/remoterepo"));
      assertTrue(rootGroups.length > 0);
    } finally {
      httpServer.stop();
    }
  }

  public void testMngEclipse1907() throws Exception {
    IProject project = importProject("projects/projectimport/p001/pom.xml", new ResolverConfiguration());
    waitForJobsToComplete();

    // make facade shallow as it would be when the workspace was just started and its state deserialized
    IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager().getProject(project);
    Field field = MavenProjectFacade.class.getDeclaredField("mavenProject");
    field.setAccessible(true);
    field.set(facade, null);

    project.delete(true, true, new NullProgressMonitor());

    MavenProjectChangedEvent event = new MavenProjectChangedEvent(facade.getPom(),
        MavenProjectChangedEvent.KIND_REMOVED, MavenProjectChangedEvent.FLAG_NONE, facade, null);
    ((NexusIndexManager) MavenPlugin.getDefault().getIndexManager()).mavenProjectChanged(
        new MavenProjectChangedEvent[] {event}, new NullProgressMonitor());
  }

  public void testFetchIndexFromRepositoryWithAuthentication() throws Exception {
    HttpServer httpServer = new HttpServer();
    httpServer.addResources("/", "");
    httpServer.addUser("testuser", "testpass", "index-reader");
    httpServer.addSecuredRealm("/*", "index-reader");
    httpServer.start();
    try {
      final File settingsFile = new File("target/settings-index-with-auth.xml");
      FileHelpers.filterXmlFile(new File("src/org/eclipse/m2e/tests/internal/index/auth_settings.xml"), settingsFile,
          Collections.singletonMap("@port.http@", Integer.toString(httpServer.getHttpPort())));
      assertTrue(settingsFile.exists());

      mavenConfiguration.setUserSettingsFile(settingsFile.getCanonicalPath());
      waitForJobsToComplete();

      IndexedArtifactGroup[] rootGroups = indexManager.getRootGroups(getRepository(httpServer.getHttpUrl()
          + "/remoterepo"));
      assertTrue(rootGroups.length > 0);
    } finally {
      httpServer.stop();
    }
  }

}
