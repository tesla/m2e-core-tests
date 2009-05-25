/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.archetype;

import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.archetype.ArchetypeCatalogFactory.DefaultLocalCatalogFactory;
import org.maven.ide.eclipse.archetype.ArchetypeCatalogFactory.InternalCatalogFactory;
import org.maven.ide.eclipse.archetype.ArchetypeCatalogFactory.LocalCatalogFactory;
import org.maven.ide.eclipse.archetype.ArchetypeCatalogFactory.NexusIndexerCatalogFactory;
import org.maven.ide.eclipse.archetype.ArchetypeCatalogFactory.RemoteCatalogFactory;


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
