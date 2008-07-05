/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.archetype;

import java.util.Collection;

import junit.framework.TestCase;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.ArchetypeCatalogFactory;
import org.maven.ide.eclipse.embedder.ArchetypeManager;
import org.maven.ide.eclipse.embedder.ArchetypeCatalogFactory.NexusIndexerCatalogFactory;
import org.maven.ide.eclipse.embedder.ArchetypeCatalogFactory.InternalCatalogFactory;


/**
 * @author Eugene Kuleshov
 */
public class ArchetypeManagerTest extends TestCase {

  public void testArchetypeManager() throws Exception {
    ArchetypeManager archetypeManager = MavenPlugin.getDefault().getArchetypeManager();

    ArchetypeCatalogFactory nexusCatalog = archetypeManager.getArchetypeCatalogFactory(NexusIndexerCatalogFactory.ID);
    assertNotNull(nexusCatalog);

    ArchetypeCatalogFactory internalCatalog = archetypeManager.getArchetypeCatalogFactory(InternalCatalogFactory.ID);
    assertNotNull(internalCatalog);

    Collection<ArchetypeCatalogFactory> catalogs = archetypeManager.getArchetypeCatalogs();
    assertEquals("" + catalogs.toString(), 5, catalogs.size());
  }

}
