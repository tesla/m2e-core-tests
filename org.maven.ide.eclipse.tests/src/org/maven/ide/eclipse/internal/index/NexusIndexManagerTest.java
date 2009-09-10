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

  private IMavenConfiguration mavenConfiguration = MavenPlugin.lookup(IMavenConfiguration.class);

  private NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();

  private void waitForIndexJobToComplete() throws InterruptedException {
    indexManager.getIndexUpdateJob().join();
  }  
  
  public void testDisableIndex() throws Exception{
    String publicRepoUrl = "http://repository.sonatype.org/content/repositories/eclipse";
    setupPublicMirror(publicRepoUrl); 
    Collection<RepositoryInfo> repositories = indexManager.getRepositories();
    for(RepositoryInfo info : repositories){
      String url = info.getUrl();
      String details = indexManager.getIndexDetails(url);
      if("local".equals(url)){
        assertEquals("Local repo should default to full details", RepositoryInfo.DETAILS_FULL, details);
      } else if("workspace".equals(url)){
        assertEquals("workspace repo should default to full details", RepositoryInfo.DETAILS_MIN, details);
      } else {
       if(publicRepoUrl.equals(url)){
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
    String publicRepoUrl = "http://repository.sonatype.org/content/repositories/eclipse";
    final File mirroredRepoFile = new File("src/org/maven/ide/eclipse/internal/index/public_mirror_repo_settings.xml");
    assertTrue(mirroredRepoFile.exists());

    mavenConfiguration.setUserSettingsFile(mirroredRepoFile.getCanonicalPath());
    waitForIndexJobToComplete();

    //this failed with the bug in authentication (NPE) in NexusIndexManager
    IndexedArtifactGroup[] rootGroups = indexManager.getRootGroups(publicRepoUrl);
    assertTrue(rootGroups.length > 0);
  }

  public void testNoMirror() throws Exception {
    final File settingsFile = new File("src/org/maven/ide/eclipse/internal/index/no_mirror_settings.xml");
    assertTrue(settingsFile.exists());

    mavenConfiguration.setUserSettingsFile(settingsFile.getCanonicalPath());
    waitForIndexJobToComplete();

    ArrayList<RepositoryInfo> repositories = new ArrayList<RepositoryInfo>(indexManager.getRepositories());
    assertEquals(3, repositories.size());
    assertNotNull(indexManager.getIndexingContext(repositories.get(0).getUrl()));
    assertNotNull(indexManager.getIndexingContext(repositories.get(1).getUrl()));
    assertNotNull(indexManager.getIndexingContext(repositories.get(2).getUrl()));
    
    NexusIndex workspaceIndex = indexManager.getIndex("workspace");
    assertNotNull(workspaceIndex);
    
    NexusIndex localIndex = indexManager.getIndex("local");
    assertNotNull(localIndex);
  }

  protected void setupPublicMirror(String publicRepoUrl) throws Exception {
    final File mirroredRepoFile = new File("src/org/maven/ide/eclipse/internal/index/public_mirror_repo_settings.xml");
    assertTrue(mirroredRepoFile.exists());

    mavenConfiguration.setUserSettingsFile(mirroredRepoFile.getCanonicalPath());
    waitForIndexJobToComplete();
  }
  
  public void testPublicMirror() throws Exception {
    String publicRepoUrl = "http://repository.sonatype.org/content/repositories/eclipse";
    setupPublicMirror(publicRepoUrl);

    ArrayList<RepositoryInfo> repositories = new ArrayList<RepositoryInfo>(indexManager.getRepositories());
    assertEquals(2, repositories.size());
    assertEquals(publicRepoUrl, repositories.get(0).getUrl());
    assertNotNull(indexManager.getIndexingContext(publicRepoUrl));
    assertNull(indexManager.getIndexingContext(repositories.get(1).getUrl()));
    
    //make sure that the junit jar can be found in the public repo
    NexusIndex index = indexManager.getIndex(publicRepoUrl);
    assertNotNull(index);
    Collection<IndexedArtifact> junitArtifact = index.find("junit", "junit", "3.8.1", "jar");
    assertTrue(junitArtifact.size() > 0);
  }


  public void testPublicNonMirrored() throws Exception {
    final File nonMirroredRepoFile = new File(
        "src/org/maven/ide/eclipse/internal/index/public_nonmirrored_repo_settings.xml");
    assertTrue(nonMirroredRepoFile.exists());

    mavenConfiguration.setUserSettingsFile(nonMirroredRepoFile.getCanonicalPath());
    waitForIndexJobToComplete();

    ArrayList<RepositoryInfo> repositories = new ArrayList<RepositoryInfo>(indexManager.getRepositories());
    assertEquals(3, repositories.size());
    assertEquals("http://repository.sonatype.org/content/repositories/eclipse-snapshots/", repositories.get(0).getUrl());
    assertNotNull(indexManager.getIndexingContext(repositories.get(0).getUrl()));
    assertEquals("http://repository.sonatype.org/content/repositories/eclipse", repositories.get(1).getUrl());
    assertNotNull(indexManager.getIndexingContext(repositories.get(1).getUrl()));
    assertNull(indexManager.getIndexingContext(repositories.get(2).getUrl()));
  }
  
  public void testArtifactSearch() throws Exception {
    final File settingsFile = new File("src/org/maven/ide/eclipse/internal/index/no_mirror_settings.xml");
    assertTrue(settingsFile.exists());

    mavenConfiguration.setUserSettingsFile(settingsFile.getCanonicalPath());
    waitForIndexJobToComplete();
    
    Map<String, IndexedArtifact> search = indexManager.search("commons-logging", IIndex.SEARCH_ARTIFACT);
    IndexedArtifact ia = search.get("null : null : commons-logging : commons-logging");
    String gav = NexusIndexManager.getGAV(ia.getGroupId(), ia.getArtifactId(), "1.1.1", "jar");
    assertNotNull(ia);
    boolean hasVersion = false;
    for(IndexedArtifactFile file : ia.getFiles()){
      if("1.1.1".equals(file.version)){
        hasVersion = true;
        break;
      }   
    }
    assertTrue(hasVersion);
    
    //TODO: do something with these
    Map<String, IndexedArtifact> search2 = indexManager.search(gav, IIndex.SEARCH_ARTIFACT);  
    search = indexManager.search("commons-logging-1.1.1", IIndex.SEARCH_ARTIFACT, IIndex.SEARCH_JAVADOCS);
    search = indexManager.search("commons-logging-1.1.1", IIndex.SEARCH_ARTIFACT, IIndex.SEARCH_SOURCES);
    search = indexManager.search("commons-logging-1.1.1", IIndex.SEARCH_ARTIFACT, IIndex.SEARCH_TESTS);
    
    Map<String, IndexedArtifact> noResultsSearch = indexManager.search("beepbeep-nothing", IIndex.SEARCH_ARTIFACT);
    assertTrue(noResultsSearch.size() == 0);
  }

  
  public void testClassSearch() throws Exception {
    Map<String, IndexedArtifact> search = indexManager.search("TestCase", IIndex.SEARCH_CLASS_NAME);
    assertTrue(search.size() > 0);

    Map<String, IndexedArtifact> noResultsSearch = indexManager.search("BeepBeepNoClass", IIndex.SEARCH_CLASS_NAME);
    assertTrue(noResultsSearch.size() == 0);
  }

  public void testPluginSearch() throws Exception {
    Map<String, IndexedArtifact> search = indexManager.search("maven-war", IIndex.SEARCH_PLUGIN);
    assertTrue(search.size() > 0);
    
    Map<String, IndexedArtifact> all = indexManager.search("*", IIndex.SEARCH_PLUGIN);
    assertTrue(all.size() > 0);
    
    Map<String, IndexedArtifact> noResultsSearch = indexManager.search("TestCase", IIndex.SEARCH_PLUGIN);
    assertTrue(noResultsSearch.size() == 0);
  }
  
  public void testGroupSearch() throws Exception {
    Map<String, IndexedArtifact> search = indexManager.search("org", IIndex.SEARCH_GROUP);
    assertTrue(search.size() > 0);
    
    Map<String, IndexedArtifact> noResultsSearch = indexManager.search("org", IIndex.SEARCH_GROUP);
    assertTrue(noResultsSearch.size() > 0);
  }
  
  public void testArchetypeSearch() throws Exception {
    Map<String, IndexedArtifact> search = indexManager.search("maven-archetype-quickstart", IIndex.SEARCH_ARCHETYPE);
    assertTrue(search.size() == 1);
    
    //TODO: this should pass. add it back in when archetypes are working
//    Map<String, IndexedArtifact> j2eeSearch = indexManager.search("maven-archetype-j2ee-simple", IIndex.SEARCH_ARCHETYPE);
//    assertTrue(j2eeSearch.size() == 1);
    
    Map<String, IndexedArtifact> none = indexManager.search("maven-archetype-foobar", IIndex.SEARCH_ARCHETYPE);
    assertTrue(none.size() == 0);
  }
  
  public void testPackagingSearch() throws Exception {
    Map<String, IndexedArtifact> search = indexManager.search("jar", IIndex.SEARCH_PACKAGING);
    assertTrue(search.size() > 0);
    
    Map<String, IndexedArtifact> pomSearch = indexManager.search("pom", IIndex.SEARCH_PACKAGING);
    assertTrue(pomSearch.size() > 0);
    
    Map<String, IndexedArtifact> noPackaging = indexManager.search("pomX", IIndex.SEARCH_PACKAGING);
    assertTrue(noPackaging.size() == 0);
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
  
  /**
   * Simply make sure the repositories list comes back for an imported project
   * @throws Exception
   */

  
  public void testIndexedArtifactGroups() throws Exception {
    String publicRepoUrl = "http://repository.sonatype.org/content/repositories/eclipse";
    String publicName = "nexus";
    final File mirroredRepoFile = new File("src/org/maven/ide/eclipse/internal/index/public_mirror_repo_settings.xml");
    assertTrue(mirroredRepoFile.exists());

    IndexedArtifactGroup[] rootGroups = indexManager.getRootGroups(publicRepoUrl);
    assertTrue(rootGroups.length == 0);
    
    assertTrue(mirroredRepoFile.exists());

    mavenConfiguration.setUserSettingsFile(mirroredRepoFile.getCanonicalPath());
    waitForIndexJobToComplete();
    rootGroups = indexManager.getRootGroups(publicRepoUrl);
    assertTrue(rootGroups.length > 0);

    IndexedArtifactGroup iag = new IndexedArtifactGroup(publicName, publicRepoUrl, "org.junit");
    IndexedArtifactGroup resolveGroup = indexManager.resolveGroup(iag);
    assertTrue(resolveGroup.getFiles().size() > 0);
    
    IndexedArtifactGroup iag2 = new IndexedArtifactGroup(publicName, publicRepoUrl, "org.junit.fizzle");
    IndexedArtifactGroup resolveGroup2 = indexManager.resolveGroup(iag2);
    assertTrue(resolveGroup2.getFiles().size() == 0);

    ArtifactInfo info = new ArtifactInfo(publicRepoUrl, "org.junit", "junit", "3.8.1", "jar");
    IndexedArtifactFile indexedArtifactFile = indexManager.getIndexedArtifactFile(info);
    assertNotNull(indexedArtifactFile);
  }
  
  public void testIndexedPublicArtifactGroups() throws Exception {
    String publicRepoUrl = "http://repository.sonatype.org/content/groups/public/";

    final File mirroredRepoFile = new File("src/org/maven/ide/eclipse/internal/index/public_settings.xml");
    assertTrue(mirroredRepoFile.exists());

    mavenConfiguration.setUserSettingsFile(mirroredRepoFile.getCanonicalPath());
    waitForIndexJobToComplete();

    ArtifactKey key = new ArtifactKey("org.junit", "junit", "3.8.1", "jar");
    
    IndexedArtifactFile indexedArtifactFile = indexManager.getIndexedArtifactFile(publicRepoUrl, key);
    //TODO: figure out why this is failing to return an indexedArtifactFile
    //assertNotNull(indexedArtifactFile);
  }
  
  public void testProjectIndexes() throws Exception {
    String publicRepoUrl = "http://repository.sonatype.org/content/repositories/eclipse";
    setupPublicMirror(publicRepoUrl);
    IProject project = createExisting("resourcefiltering-p005", "projects/resourcefiltering/p005");
    waitForJobsToComplete();
    
    indexManager.scheduleIndexUpdate(publicRepoUrl, true);
    waitForJobsToComplete();
    
    IIndex index = indexManager.getIndex(project);
    assertNotNull(index);
    //there should be some global indices too
    IIndex globalIndices = indexManager.getIndex((IProject)null);
    assertNotNull(globalIndices);

  }
  

}
