/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.plexus.util.IOUtil;

import org.maven.ide.eclipse.internal.archetype.ArchetypeCatalogsWriter;


/**
 * Archetype Manager
 * 
 * @author Eugene Kuleshov
 */
public class ArchetypeManager {

  private final Map<String, ArchetypeCatalogFactory> catalogs = new LinkedHashMap<String, ArchetypeCatalogFactory>();

  private final File configFile;
  
  private final ArchetypeCatalogsWriter writer;

  public ArchetypeManager(File configFile) {
    this.configFile = configFile;
    this.writer = new ArchetypeCatalogsWriter();
  }

  /**
   * @return Collection of ArchetypeCatalogFactory
   */
  public Collection<ArchetypeCatalogFactory> getArchetypeCatalogs() {
    return new ArrayList<ArchetypeCatalogFactory>(catalogs.values());
  }

  public void addArchetypeCatalogFactory(ArchetypeCatalogFactory factory) {
    if(factory != null) {
      catalogs.put(factory.getId(), factory);
    }
  }

  public void removeArchetypeCatalogFactory(String catalogId) {
    catalogs.remove(catalogId);
  }
  
  public ArchetypeCatalogFactory getArchetypeCatalogFactory(String catalogId) {
    return catalogs.get(catalogId);
  }
  
  public void readCatalogs() throws IOException {
    if(configFile.exists()) {
      InputStream is = null;
      try {
        is = new FileInputStream(configFile);
        Collection<ArchetypeCatalogFactory> catalogs = writer.readArchetypeCatalogs(is);
        for(Iterator<ArchetypeCatalogFactory> it = catalogs.iterator(); it.hasNext();) {
          addArchetypeCatalogFactory(it.next());
        }
      } finally {
        IOUtil.close(is);
      }
    }
  }
  
  public void saveCatalogs() throws IOException {
    OutputStream os = null;
    try {
      os = new FileOutputStream(configFile);
      writer.writeArchetypeCatalogs(getArchetypeCatalogs(), os);
    } finally {
      IOUtil.close(os);
    }
  }

}
