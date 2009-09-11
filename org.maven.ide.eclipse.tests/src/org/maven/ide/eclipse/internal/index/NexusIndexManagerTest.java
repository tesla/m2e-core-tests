/*******************************************************************************
 * Copyright (c) 2009 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.index;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.index.IIndex;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
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

  
  private void waitForIndexJobToComplete() throws InterruptedException {
    indexManager.getIndexUpdateJob().join();
  }  
  
  protected void setupPublicMirror(String publicRepoUrl, String settingsFile) throws Exception {
    final File mirroredRepoFile = new File(settingsFile);
    assertTrue(mirroredRepoFile.exists());

    mavenConfiguration.setUserSettingsFile(mirroredRepoFile.getCanonicalPath());
    waitForIndexJobToComplete();
  }

  public void testDisableIndex() throws Exception{
    setupPublicMirror(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO); 
    Collection<RepositoryInfo> repositories = indexManager.getRepositories();
    for(RepositoryInfo info : repositories){
      String url = info.getUrl();
      String details = indexManager.getIndexDetails(url);
      if("local".equals(url)){
        assertEquals("Local repo should default to full details", RepositoryInfo.DETAILS_FULL, details);
      } else if("workspace".equals(url)){
        assertEquals("workspace repo should default to full details", RepositoryInfo.DETAILS_MIN, details);
      } else {
       if(REPO_URL_ECLIPSE.equals(url)){
         assertEquals("Mirror should be min details", RepositoryInfo.DETAILS_MIN, details);
       } else {
         assertEquals("Mirrored should be disabled", RepositoryInfo.DETAILS_DISABLED, details);
       }
      }
    }
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
    IndexedArtifactGroup[] rootGroups = indexManager.getRootGroups(REPO_URL_ECLIPSE);
    assertTrue(rootGroups.length > 0);
  }

  public void testPublicMirror() throws Exception {
    setupPublicMirror(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);

    ArrayList<RepositoryInfo> repositories = new ArrayList<RepositoryInfo>(indexManager.getRepositories());
    assertEquals(2, repositories.size());
    assertEquals(REPO_URL_ECLIPSE, repositories.get(0).getUrl());
    assertNotNull(indexManager.getIndexingContext(REPO_URL_ECLIPSE));
    assertNull(indexManager.getIndexingContext(repositories.get(1).getUrl()));
    
    //make sure that the junit jar can be found in the public repo
    NexusIndex index = indexManager.getIndex(REPO_URL_ECLIPSE);
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

    ArrayList<RepositoryInfo> repositories = new ArrayList<RepositoryInfo>(indexManager.getRepositories());
    assertEquals(3, repositories.size());
    assertEquals("http://repository.sonatype.org/content/repositories/eclipse-snapshots/", repositories.get(0).getUrl());
    assertNotNull(indexManager.getIndexingContext(repositories.get(0).getUrl()));
    assertEquals(REPO_URL_ECLIPSE, repositories.get(1).getUrl());
    assertNotNull(indexManager.getIndexingContext(repositories.get(1).getUrl()));
    assertNull(indexManager.getIndexingContext(repositories.get(2).getUrl()));
  }

  public void testClassSearch() throws Exception {
    updateRepo(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
    
    Map<String, IndexedArtifact> search = indexManager.search("TestCase", IIndex.SEARCH_CLASS_NAME);
    assertTrue(search.size() > 0);

    Map<String, IndexedArtifact> noResultsSearch = indexManager.search("BeepBeepNoClass", IIndex.SEARCH_CLASS_NAME);
    assertTrue(noResultsSearch.size() == 0);
  }

  
  protected void updateRepo(String repoUrl, String settingsFile) throws Exception{
    setupPublicMirror(repoUrl, settingsFile);
    waitForIndexJobToComplete();
    indexManager.setIndexDetails(repoUrl, RepositoryInfo.DETAILS_FULL);  
    indexManager.scheduleIndexUpdate(repoUrl, true);
    waitForIndexJobToComplete();
    assertEquals(RepositoryInfo.DETAILS_FULL, indexManager.getIndexDetails(repoUrl));
  }
  
  public void testPluginSearch() throws Exception {
    updateRepo(REPO_URL_PUBLIC, SETTINGS_WITH_PUBLIC);
    Map<String, IndexedArtifact> search = indexManager.search("maven-war", IIndex.SEARCH_PLUGIN);
    assertTrue(search.size() > 0);
    
    
    Map<String, IndexedArtifact> noResultsSearch = indexManager.search("TestCase", IIndex.SEARCH_PLUGIN);
    assertTrue(noResultsSearch.size() == 0);
  }
  
  public void testGroupSearch() throws Exception {
    updateRepo(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
    Map<String, IndexedArtifact> search = indexManager.search("org.junit", IIndex.SEARCH_GROUP);
    assertTrue(search.size() > 0);
    
    //TODO: this should probably be returning results, but it seems like there result space is too big,
    //so its not returning results now
    Map<String, IndexedArtifact> noResultsSearch = indexManager.search("org", IIndex.SEARCH_GROUP);
    assertTrue(noResultsSearch.size() == 0);
  }
  
  public void testArchetypeSearch() throws Exception {
    updateRepo(REPO_URL_PUBLIC, SETTINGS_WITH_PUBLIC);
    //updateRepo(ECLIPSE_PUBLIC_REPO, SETTINGS_ECLIPSE_REPO);
    Map<String, IndexedArtifact> search = indexManager.search("maven-archetype-quickstart", IIndex.SEARCH_ARCHETYPE);
    assertTrue(search.size() == 1);
    
    //TODO: this should pass. add it back in when archetypes are working
//    Map<String, IndexedArtifact> j2eeSearch = indexManager.search("maven-archetype-j2ee-simple", IIndex.SEARCH_ARCHETYPE);
//    assertTrue(j2eeSearch.size() == 1);
    
    Map<String, IndexedArtifact> none = indexManager.search("maven-archetype-foobar", IIndex.SEARCH_ARCHETYPE);
    assertTrue(none.size() == 0);
  }
  
  public void testPackagingSearch() throws Exception {
    updateRepo(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
    
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
    updateRepo(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
    Map<String, IndexedArtifact> search = indexManager.search("commons-logging", "BadSearchType");
    assertTrue(search.size() == 0);
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
    
    IndexedArtifactGroup[] rootGroups = indexManager.getRootGroups(REPO_URL_ECLIPSE);
    assertTrue(rootGroups.length > 0);

    IndexedArtifactGroup iag = new IndexedArtifactGroup(publicName, REPO_URL_ECLIPSE, "org.junit");
    IndexedArtifactGroup resolveGroup = indexManager.resolveGroup(iag);
    assertTrue(resolveGroup.getFiles().size() > 0);
    
    IndexedArtifactGroup iag2 = new IndexedArtifactGroup(publicName, REPO_URL_ECLIPSE, "org.junit.fizzle");
    IndexedArtifactGroup resolveGroup2 = indexManager.resolveGroup(iag2);
    assertTrue(resolveGroup2.getFiles().size() == 0);

    ArtifactInfo info = new ArtifactInfo(REPO_URL_ECLIPSE, "org.junit", "junit", "3.8.1", "jar");
    IndexedArtifactFile indexedArtifactFile = indexManager.getIndexedArtifactFile(info);
    assertNotNull(indexedArtifactFile);
  }
  

  
  public void testProjectIndexes() throws Exception {
    setupPublicMirror(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
    IProject project = createExisting("resourcefiltering-p005", "projects/resourcefiltering/p005");
    waitForJobsToComplete();
    
    indexManager.scheduleIndexUpdate(REPO_URL_ECLIPSE, true);
    waitForJobsToComplete();
    
    IIndex index = indexManager.getIndex(project);
    assertNotNull(index);
    //there should be some global indices too
    IIndex globalIndices = indexManager.getIndex((IProject)null);
    assertNotNull(globalIndices);

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
    IndexedArtifactFile indexedArtifactFile = indexManager.getIndexedArtifactFile(REPO_URL_ECLIPSE, key);
    assertNotNull(indexedArtifactFile);

  }
  
  public void testNoMirror() throws Exception {
    
    final File settingsFile = new File(SETTINGS_NO_MIRROR);
    assertTrue(settingsFile.exists());

    mavenConfiguration.setUserSettingsFile(settingsFile.getCanonicalPath());
    waitForIndexJobToComplete();
    
    ArrayList<RepositoryInfo> repositories = new ArrayList<RepositoryInfo>(indexManager.getRepositories());
    assertEquals(3, repositories.size());
    for(RepositoryInfo info : repositories){
      assertTrue(info.getMirrorId() == null);
      assertTrue(info.getMirrorOf() == null);
    }
    
    NexusIndex workspaceIndex = indexManager.getIndex("workspace");
    assertNotNull(workspaceIndex);
    
    NexusIndex localIndex = indexManager.getIndex("local");
    assertNotNull(localIndex);
  }
  
  public void testArtifactSearch() throws Exception {
    updateRepo(REPO_URL_ECLIPSE, SETTINGS_ECLIPSE_REPO);
    
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
}
