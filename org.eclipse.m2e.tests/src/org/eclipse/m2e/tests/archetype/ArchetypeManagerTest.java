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

package org.eclipse.m2e.tests.archetype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

import org.eclipse.core.runtime.Platform;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.artifact.repository.ArtifactRepository;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.archetype.ArchetypeCatalogFactory;
import org.eclipse.m2e.core.ui.internal.archetype.ArchetypeManager;
import org.eclipse.m2e.core.ui.internal.archetype.MavenArchetype;
import org.eclipse.m2e.core.ui.internal.archetype.ArchetypeCatalogFactory.DefaultLocalCatalogFactory;
import org.eclipse.m2e.core.ui.internal.archetype.ArchetypeCatalogFactory.InternalCatalogFactory;
import org.eclipse.m2e.core.ui.internal.archetype.ArchetypeCatalogFactory.LocalCatalogFactory;
import org.eclipse.m2e.core.ui.internal.archetype.ArchetypeCatalogFactory.RemoteCatalogFactory;
import org.eclipse.m2e.tests.common.FileHelpers;
import org.eclipse.m2e.tests.common.HttpServer;


/**
 * @author Eugene Kuleshov
 */
public class ArchetypeManagerTest {

  private static final String M2E_TEST_PLUGIN_ID = "org.eclipse.m2e.tests";

  private static final String ARCHETYPE_REPOS_SETTINGS = "src/org/eclipse/m2e/tests/archetype/settings_archetypes.xml";

  private ArchetypeManager archetypeManager;

  @Before
  public void setUp() throws Exception {
    archetypeManager = M2EUIPluginActivator.getDefault().getArchetypeManager();
  }

  @Test
  public void testArchetypeManager() throws Exception {
    {
      ArchetypeCatalogFactory catalog = archetypeManager.getArchetypeCatalogFactory(InternalCatalogFactory.ID);
      assertNotNull(catalog);

      ArchetypeCatalog archetypeCatalog = catalog.getArchetypeCatalog();
      assertNotNull(archetypeCatalog);
      List<?> archetypes = archetypeCatalog.getArchetypes();
      assertNotNull(archetypes);
      assertFalse(archetypes.isEmpty());
    }

    {
      ArchetypeCatalogFactory catalog = archetypeManager.getArchetypeCatalogFactory(DefaultLocalCatalogFactory.ID);
      assertNotNull(catalog);

      ArchetypeCatalog archetypeCatalog = catalog.getArchetypeCatalog();
      assertNotNull(archetypeCatalog);
      List<?> archetypes = archetypeCatalog.getArchetypes();
      assertNotNull(archetypes);
      // assertFalse(archetypes.isEmpty());  empty because no catalog created for the local repository
    }

    {
      ArchetypeCatalogFactory catalog = archetypeManager
          .getArchetypeCatalogFactory("https://repo1.maven.org/maven2/archetype-catalog.xml");
      assertNotNull(catalog);
      assertEquals("Maven Central", catalog.getDescription());
      ArchetypeCatalog archetypeCatalog = catalog.getArchetypeCatalog();
      assertNotNull(archetypeCatalog);
      List<?> archetypes = archetypeCatalog.getArchetypes();
      assertNotNull(archetypes);
      assertFalse(archetypes.isEmpty());
    }

    Collection<ArchetypeCatalogFactory> catalogs = archetypeManager.getArchetypeCatalogs();
    assertEquals("" + catalogs.toString(), 4, catalogs.size());
  }

  @Test
  public void testLocalArchetypeCatalogFactory() throws Exception {
    LocalCatalogFactory catalogFactory = new LocalCatalogFactory("archetype-catalog.xml", "local", true);
    ArchetypeCatalog catalog = catalogFactory.getArchetypeCatalog();
    assertNotNull(catalog);
    assertEquals(2, catalog.getArchetypes().size());
  }

  @Test
  public void testEmbeddedLocalArchetypeCatalogFactory() throws Exception {
    Bundle bundle = Platform.getBundle(M2E_TEST_PLUGIN_ID);
    assertNotNull(bundle);
    URL embeddedCatalog = bundle.getEntry("/resources/490230_embedded_archetype_catalog/my-catalog.xml");
    LocalCatalogFactory catalogFactory = new LocalCatalogFactory(embeddedCatalog.toString(), "embedded", false);
    ArchetypeCatalog catalog = catalogFactory.getArchetypeCatalog();
    assertNotNull(catalog);
    assertEquals(1, catalog.getArchetypes().size());
  }

  @Test
  public void testBadLocalArchetypeCatalogFactory() throws Exception {
    asserEmptyCatalog("crap://nope", "bad-local");
    asserEmptyCatalog("bundleentry://nooope", "missing-embedded");
  }

  public void asserEmptyCatalog(String url, String description) throws Exception {
    LocalCatalogFactory catalogFactory = new LocalCatalogFactory(url, description, true);
    ArchetypeCatalog catalog = catalogFactory.getArchetypeCatalog();
    assertNotNull(catalog);
    assertEquals(0, catalog.getArchetypes().size());
  }

  @Test
  public void testRemoteArchetypeCatalogFactory() throws Exception {
    assertEquals("http://server/repo",
        new RemoteCatalogFactory("http://server/repo/archetype-catalog.xml", null, true).getRepositoryUrl());
    assertEquals("http://server/repo",
        new RemoteCatalogFactory("http://server/repo/archetype.catalog.xml", null, true).getRepositoryUrl());
    assertEquals("http://server/repo",
        new RemoteCatalogFactory("http://server/repo/archetype-catalog.txt", null, true).getRepositoryUrl());
    assertEquals("http://server/repo", new RemoteCatalogFactory("http://server/repo/", null, true).getRepositoryUrl());
    assertEquals("http://server/repo", new RemoteCatalogFactory("http://server/repo", null, true).getRepositoryUrl());
    assertEquals("", new RemoteCatalogFactory("catalog.xml", null, true).getRepositoryUrl());
    assertEquals("/", new RemoteCatalogFactory("/", null, true).getRepositoryUrl());

    //Ok these don't make sense
    assertEquals("", new RemoteCatalogFactory("", null, true).getRepositoryUrl());
    assertEquals("", new RemoteCatalogFactory("/.", null, true).getRepositoryUrl());
    assertEquals("", new RemoteCatalogFactory(".", null, true).getRepositoryUrl());
    assertEquals(".", new RemoteCatalogFactory("./", null, true).getRepositoryUrl());

    assertEquals(null, new RemoteCatalogFactory(null, null, true).getRepositoryUrl());
  }

  @Test
  public void testArchetypeManagerSaveRestore() throws Exception {

    ArchetypeCatalogFactory catalogFactory = new RemoteCatalogFactory("http://www.sonatype.org/", "test", true);
    assertEquals("test", catalogFactory.getDescription());
    assertNotNull(catalogFactory.getArchetypeCatalog());

    Collection<ArchetypeCatalogFactory> catalogs = archetypeManager.getArchetypeCatalogs();

    archetypeManager.addArchetypeCatalogFactory(catalogFactory);
    archetypeManager.saveCatalogs();

    archetypeManager.readCatalogs();
    assertEquals(catalogs.size() + 1, archetypeManager.getArchetypeCatalogs().size());
    assertNotNull(archetypeManager.getArchetypeCatalogFactory(catalogFactory.getId()));

    archetypeManager.removeArchetypeCatalogFactory(catalogFactory.getId());
    archetypeManager.saveCatalogs();

    archetypeManager.readCatalogs();
    assertEquals(catalogs.size(), archetypeManager.getArchetypeCatalogs().size());
    assertNull(archetypeManager.getArchetypeCatalogFactory(catalogFactory.getId()));
  }

  @Test
  public void testActiveArchetypeCatalogs() throws Exception {
    Collection<ArchetypeCatalogFactory> catalogs = archetypeManager.getArchetypeCatalogs();
    //disable catalogs
    catalogs.forEach(c -> c.setEnabled(false));
    //save/reload
    archetypeManager.saveCatalogs();
    archetypeManager.readCatalogs();
    assertEquals(0, archetypeManager.getActiveArchetypeCatalogs().size());

    //check all catalogs are disabled and re-enable them
    Collection<ArchetypeCatalogFactory> catalogs2 = archetypeManager.getArchetypeCatalogs();
    for(ArchetypeCatalogFactory c : catalogs2) {
      assertFalse(c.isEnabled());
      c.setEnabled(true);
    }
    //save/reload
    archetypeManager.saveCatalogs();
    archetypeManager.readCatalogs();

    //check all catalogs are now enabled
    assertEquals(catalogs.size(), archetypeManager.getActiveArchetypeCatalogs().size());

  }

  @Test
  public void testAddRemoteCatalog() throws Exception {
    HttpServer httpServer = new HttpServer();
    httpServer.addResources("/", "");
    httpServer.start();
    try {
      final RemoteCatalogFactory factory = new RemoteCatalogFactory(httpServer.getHttpUrl() + "/archetype-catalog.xml",
          null, true);
      ArchetypeCatalog catalog = factory.getArchetypeCatalog();
      assertEquals(2, catalog.getArchetypes().size());
    } finally {
      httpServer.stop();
    }
  }

  @Test
  public void test371775_archetypeRepoAuthentication() throws Exception {

    IMavenConfiguration configuration = MavenPlugin.getMavenConfiguration();

    String userSettings = configuration.getUserSettingsFile();

    ArtifactRepository repo;
    try {

      configuration.setUserSettingsFile(new File(ARCHETYPE_REPOS_SETTINGS).getCanonicalPath());

      Archetype archetype = new Archetype();
      archetype.setRepository("http://localhost/");
      archetype.setArtifactId("my-archetype");
      repo = archetypeManager.getArchetypeRepository(new MavenArchetype(archetype));

    } finally {

      configuration.setUserSettingsFile(userSettings);
    }
    assertEquals("my-archetype-repo", repo.getId());
    assertNotNull("Repo Authentication is null!", repo.getAuthentication());
    assertEquals("m2e", repo.getAuthentication().getUsername());
    assertEquals("371775", repo.getAuthentication().getPassword());
  }

  @Test
  public void test359855_localArchetypeWithProperties() throws Exception {
    File sourceFolder = new File("resources/359855_localArchetype/");

    File localRepo = new File("target/localrepo-archetypes");

    FileHelpers.deleteDirectory(localRepo);
    assertFalse(localRepo.exists());

    FileHelpers.copyDir(sourceFolder, localRepo);

    Archetype archetype = new Archetype();
    archetype.setGroupId("foo.bar");
    archetype.setArtifactId("someproject-archetype");
    archetype.setVersion("1.0-SNAPSHOT");

    IMavenConfiguration configuration = MavenPlugin.getMavenConfiguration();

    String userSettings = configuration.getUserSettingsFile();

    try {

      configuration.setUserSettingsFile(new File(ARCHETYPE_REPOS_SETTINGS).getCanonicalPath());

      List<?> properties = archetypeManager.getRequiredProperties(new MavenArchetype(archetype), null, null);
      assertNotNull("Required Properties are null!", properties);

      assertEquals("Unexpected required properties " + properties.toString(), 1, properties.size());

    } finally {

      configuration.setUserSettingsFile(userSettings);
    }

  }

  @Test
  public void test387784_remoteArchetypeWithProperties() throws Exception {

    File localRepo = new File("target/localrepo-archetypes");

    FileHelpers.deleteDirectory(localRepo);
    assertFalse(localRepo.exists());

    Archetype archetype = new Archetype();
    archetype.setGroupId("foo.bar");
    archetype.setArtifactId("someproject-archetype");
    archetype.setVersion("1.0");
    archetype.setRepository("file:repositories/customrepo");

    IMavenConfiguration configuration = MavenPlugin.getMavenConfiguration();

    String userSettings = configuration.getUserSettingsFile();

    try {

      configuration.setUserSettingsFile(new File(ARCHETYPE_REPOS_SETTINGS).getCanonicalPath());

      ArtifactRepository remoteArchetypeRepository = archetypeManager
          .getArchetypeRepository(new MavenArchetype(archetype));

      List<?> properties = archetypeManager.getRequiredProperties(new MavenArchetype(archetype),
          remoteArchetypeRepository, null);
      assertNotNull("Required Properties are null!", properties);

      assertEquals("Unexpected required properties " + properties.toString(), 1, properties.size());

    } finally {

      configuration.setUserSettingsFile(userSettings);
    }

  }

  @Test
  public void testRepositoryUrlFromRemoteArchetypeCatalogs() throws Exception {

    HttpServer httpServer = new HttpServer();
    httpServer.addResources("/", "");
    httpServer.start();
    String url = httpServer.getHttpUrl() + "/archetype-catalog.xml";
    try {
      RemoteCatalogFactory remoteFactory = new RemoteCatalogFactory(url, null, true);
      archetypeManager.addArchetypeCatalogFactory(remoteFactory);

      List<Archetype> archetypes = remoteFactory.getArchetypeCatalog().getArchetypes();
      assertEquals(2, archetypes.size());
      Archetype appfuse = archetypes.get(0);
      assertEquals("appfuse-basic-jsf", appfuse.getArtifactId());
      assertEquals("http://repo1.maven.org/maven2/", appfuse.getRepository());

      Archetype someArchetype = archetypes.get(1);
      assertEquals("some-archetype", someArchetype.getArtifactId());
      assertEquals(httpServer.getHttpUrl(), someArchetype.getRepository());

    } finally {
      httpServer.stop();
      archetypeManager.removeArchetypeCatalogFactory(url);
    }
  }
}
