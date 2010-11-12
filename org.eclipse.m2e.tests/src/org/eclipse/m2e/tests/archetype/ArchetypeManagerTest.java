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

import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.archetype.ArchetypeCatalogFactory;
import org.eclipse.m2e.core.archetype.ArchetypeCatalogFactory.DefaultLocalCatalogFactory;
import org.eclipse.m2e.core.archetype.ArchetypeCatalogFactory.InternalCatalogFactory;
import org.eclipse.m2e.core.archetype.ArchetypeCatalogFactory.LocalCatalogFactory;
import org.eclipse.m2e.core.archetype.ArchetypeCatalogFactory.NexusIndexerCatalogFactory;
import org.eclipse.m2e.core.archetype.ArchetypeCatalogFactory.RemoteCatalogFactory;
import org.eclipse.m2e.core.archetype.ArchetypeManager;


/**
 * @author Eugene Kuleshov
 */
public class ArchetypeManagerTest extends TestCase {

  private ArchetypeManager archetypeManager;

  protected void setUp() throws Exception {
    super.setUp();
    
    archetypeManager = MavenPlugin.getDefault().getArchetypeManager();
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

}
