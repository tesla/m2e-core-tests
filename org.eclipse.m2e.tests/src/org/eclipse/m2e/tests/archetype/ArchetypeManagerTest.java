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

package org.eclipse.m2e.tests.archetype;

import java.io.File;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.artifact.repository.ArtifactRepository;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.archetype.ArchetypeCatalogFactory;
import org.eclipse.m2e.core.internal.archetype.ArchetypeCatalogFactory.DefaultLocalCatalogFactory;
import org.eclipse.m2e.core.internal.archetype.ArchetypeCatalogFactory.InternalCatalogFactory;
import org.eclipse.m2e.core.internal.archetype.ArchetypeCatalogFactory.LocalCatalogFactory;
import org.eclipse.m2e.core.internal.archetype.ArchetypeCatalogFactory.NexusIndexerCatalogFactory;
import org.eclipse.m2e.core.internal.archetype.ArchetypeCatalogFactory.RemoteCatalogFactory;
import org.eclipse.m2e.core.internal.archetype.ArchetypeManager;
import org.eclipse.m2e.tests.common.FileHelpers;
import org.eclipse.m2e.tests.common.HttpServer;


/**
 * @author Eugene Kuleshov
 */
public class ArchetypeManagerTest extends TestCase {

  private static final String ARCHETYPE_REPOS_SETTINGS = "src/org/eclipse/m2e/tests/archetype/settings_archetypes.xml";

  private ArchetypeManager archetypeManager;

  protected void setUp() throws Exception {
    super.setUp();
    
    archetypeManager = MavenPluginActivator.getDefault().getArchetypeManager();
  }
  
  public void testArchetypeManager() throws Exception {
    {
      ArchetypeCatalogFactory catalog = archetypeManager.getArchetypeCatalogFactory(NexusIndexerCatalogFactory.ID);
      assertNotNull(catalog);
      ArchetypeCatalog archetypeCatalog = catalog.getArchetypeCatalog();
      assertNotNull(archetypeCatalog);
      List<?> archetypes = archetypeCatalog.getArchetypes();
      assertNotNull(archetypes);
      // assertFalse(nexusArchetypes.isEmpty());  central index is not available here
    }
    
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
    
    Collection<ArchetypeCatalogFactory> catalogs = archetypeManager.getArchetypeCatalogs();
    assertEquals("" + catalogs.toString(), 5, catalogs.size());
  }
  
  public void testLocalArchetypeCatalogFactory() throws Exception {
    LocalCatalogFactory catalogFactory = new LocalCatalogFactory("archetype-catalog.xml", "local", true);
    ArchetypeCatalog catalog = catalogFactory.getArchetypeCatalog();
    assertNotNull(catalog);
    assertEquals(1, catalog.getArchetypes().size());
  }
  
  public void testRemoteArchetypeCatalogFactory() throws Exception {
    assertEquals("http://server/repo", new RemoteCatalogFactory("http://server/repo/archetype-catalog.xml", null, true).getRepositoryUrl());
    assertEquals("http://server/repo", new RemoteCatalogFactory("http://server/repo/archetype.catalog.xml", null, true).getRepositoryUrl());
    assertEquals("http://server/repo", new RemoteCatalogFactory("http://server/repo/archetype-catalog.txt", null, true).getRepositoryUrl());
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
  
  
  public void testArchetypeManagerSaveRestore() throws Exception {
    
    ArchetypeCatalogFactory catalogFactory = new RemoteCatalogFactory("http://www.sonatype.org/", "test", true);
    assertEquals("test", catalogFactory.getDescription());
    assertNotNull(catalogFactory.getArchetypeCatalog());
    
    Collection<ArchetypeCatalogFactory> catalogs = archetypeManager.getArchetypeCatalogs();
    
    archetypeManager.addArchetypeCatalogFactory(catalogFactory);
    archetypeManager.saveCatalogs();
    
    archetypeManager.readCatalogs();
    assertEquals(catalogs.size()+1, archetypeManager.getArchetypeCatalogs().size());
    assertNotNull(archetypeManager.getArchetypeCatalogFactory(catalogFactory.getId()));
    
    archetypeManager.removeArchetypeCatalogFactory(catalogFactory.getId());
    archetypeManager.saveCatalogs();

    archetypeManager.readCatalogs();
    assertEquals(catalogs.size(), archetypeManager.getArchetypeCatalogs().size());
    assertNull(archetypeManager.getArchetypeCatalogFactory(catalogFactory.getId()));
  }
  
  

  public void testAddRemoteCatalog() throws Exception {
    HttpServer httpServer = new HttpServer();
    httpServer.addResources("/", "");
    httpServer.start();
    try {
      final RemoteCatalogFactory factory = new RemoteCatalogFactory(httpServer.getHttpUrl() + "/archetype-catalog.xml", null, true);
      ArchetypeCatalog catalog = factory.getArchetypeCatalog();
      assertEquals(1, catalog.getArchetypes().size());
    } finally {
      httpServer.stop();
    }
  }
  
  public void test371775_archetypeRepoAuthentication() throws Exception {
    
    IMavenConfiguration configuration = MavenPlugin.getMavenConfiguration();
    
    String userSettings = configuration.getUserSettingsFile();

    ArtifactRepository repo; 
    try {
      
      configuration.setUserSettingsFile(new File(ARCHETYPE_REPOS_SETTINGS).getCanonicalPath());
      
      Archetype archetype = new Archetype();
      archetype.setRepository("http://localhost/");
      archetype.setArtifactId("my-archetype");
      repo = archetypeManager.getArchetypeRepository(archetype);
      
    } finally {
      
      configuration.setUserSettingsFile(userSettings);
    }
    assertEquals("my-archetype-repo", repo.getId());
    assertNotNull("Repo Authentication is null!", repo.getAuthentication());
    assertEquals("m2e", repo.getAuthentication().getUsername());
    assertEquals("371775", repo.getAuthentication().getPassword());
  }
  
  public void test359855_localArchetypeWithProperties() throws Exception {
    File sourceFolder = new File("resources/359855_localArchetype/");
    
    File localRepo = new File("target/localrepo-archetypes");
    
    FileHelpers.deleteDirectory(localRepo);
    
    FileHelpers.copyDir(sourceFolder, localRepo );

    Archetype archetype = new Archetype();
    archetype.setGroupId("foo.bar");
    archetype.setArtifactId("someproject-archetype");
    archetype.setVersion("1.0-SNAPSHOT");
    
    IMavenConfiguration configuration = MavenPlugin.getMavenConfiguration();
    
    String userSettings = configuration.getUserSettingsFile();

    try {
      
      configuration.setUserSettingsFile(new File(ARCHETYPE_REPOS_SETTINGS).getCanonicalPath());
      
      List<?> properties = archetypeManager.getRequiredProperties(archetype , null, null);
      assertNotNull("Required Properties are null!", properties);
      
      assertEquals("Unexpected required properties "+ properties.toString(), 1, properties.size());
      
    } finally {
      
      configuration.setUserSettingsFile(userSettings);
    }

    
  }
  
}
