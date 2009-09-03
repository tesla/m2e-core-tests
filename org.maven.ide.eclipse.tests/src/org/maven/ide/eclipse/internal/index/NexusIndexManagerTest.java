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

import junit.framework.TestCase;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.internal.index.IndexedArtifactGroup;
import org.maven.ide.eclipse.internal.index.NexusIndexManager;
import org.maven.ide.eclipse.internal.index.RepositoryInfo;


/**
 * @author dyocum
 */
public class NexusIndexManagerTest extends TestCase {

  private IMavenConfiguration mavenConfiguration = MavenPlugin.lookup(IMavenConfiguration.class);

  private NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();

  private void waitForIndexJobToComplete() throws InterruptedException {
    indexManager.getIndexUpdateJob().join();
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
  }

  public void testPublicMirror() throws Exception {
    String publicRepoUrl = "http://repository.sonatype.org/content/repositories/eclipse";
    final File mirroredRepoFile = new File("src/org/maven/ide/eclipse/internal/index/public_mirror_repo_settings.xml");
    assertTrue(mirroredRepoFile.exists());

    mavenConfiguration.setUserSettingsFile(mirroredRepoFile.getCanonicalPath());
    waitForIndexJobToComplete();

    ArrayList<RepositoryInfo> repositories = new ArrayList<RepositoryInfo>(indexManager.getRepositories());
    assertEquals(2, repositories.size());
    assertEquals(publicRepoUrl, repositories.get(0).getUrl());
    assertNotNull(indexManager.getIndexingContext(publicRepoUrl));
    assertNull(indexManager.getIndexingContext(repositories.get(1).getUrl()));
  }

  public void testPublicNonMirrored() throws Exception {
    final File nonMirroredRepoFile = new File(
        "src/org/maven/ide/eclipse/internal/index/public_nonmirrored_repo_settings.xml");
    assertTrue(nonMirroredRepoFile.exists());

    mavenConfiguration.setUserSettingsFile(nonMirroredRepoFile.getCanonicalPath());
    waitForIndexJobToComplete();

    ArrayList<RepositoryInfo> repositories = new ArrayList<RepositoryInfo>(indexManager.getRepositories());
    assertEquals(3, repositories.size());
    assertEquals("http://repository.sonatype.org/content/repositories/eclipse", repositories.get(0).getUrl());
    assertNotNull(indexManager.getIndexingContext(repositories.get(0).getUrl()));
    assertEquals("http://repository.sonatype.org/content/repositories/eclipse-snapshots/", repositories.get(1).getUrl());
    assertNotNull(indexManager.getIndexingContext(repositories.get(1).getUrl()));
    assertNull(indexManager.getIndexingContext(repositories.get(2).getUrl()));
  }

}
