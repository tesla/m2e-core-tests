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
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.IOUtil;

import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.source.ArchetypeDataSource;
import org.apache.maven.archetype.source.ArchetypeDataSourceException;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.internal.embedder.ArchetypeCatalogsWriter;


/**
 * Archetype Manager
 * 
 * @author Eugene Kuleshov
 */
public class ArchetypeManager {

  private final Map catalogs = new LinkedHashMap();

  private final File configFile;
  
  private final ArchetypeCatalogsWriter writer;

  public ArchetypeManager(File configFile) {
    this.configFile = configFile;
    this.writer = new ArchetypeCatalogsWriter();
  }

  /**
   * @return Collection of ArchetypeCatalogFactory
   */
  public Collection getArchetypeCatalogs() {
    return new ArrayList(catalogs.values());
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
    return (ArchetypeCatalogFactory) catalogs.get(catalogId);
  }
  
  public void readCatalogs() throws IOException {
    if(configFile.exists()) {
      InputStream is = null;
      try {
        is = new FileInputStream(configFile);
        Collection catalogs = writer.readArchetypeCatalogs(is);
        for(Iterator it = catalogs.iterator(); it.hasNext();) {
          addArchetypeCatalogFactory((ArchetypeCatalogFactory) it.next());
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

  /**
   * Abstract ArchetypeCatalog factory
   */
  public static abstract class ArchetypeCatalogFactory {
    private final String id;

    private final String description;

    private final boolean editable;

    public ArchetypeCatalogFactory(String id, String description, boolean editable) {
      this.id = id;
      this.description = description;
      this.editable = editable;
    }

    public String getId() {
      return this.id;
    }

    public String getDescription() {
      return this.description;
    }

    public boolean isEditable() {
      return editable;
    }

    public abstract ArchetypeCatalog getArchetypeCatalog(MavenEmbedderManager manager) throws CoreException;

    public String toString() {
      return getId();
    }

  }

  /**
   * Factory for Nexus Indexer ArchetypeCatalog
   */
  public static class NexusIndexerCatalogFactory extends ArchetypeCatalogFactory {
    public static final String ID = "nexusIndexer";

    public NexusIndexerCatalogFactory() {
      super(ID, "Nexus Indexer", false);
    }

    public ArchetypeCatalog getArchetypeCatalog(MavenEmbedderManager manager) throws CoreException {
      PlexusContainer container = manager.getWorkspaceEmbedder().getPlexusContainer();
      try {
        ArchetypeDataSource source = (ArchetypeDataSource) container.lookup(ArchetypeDataSource.class, "nexus");
        return source.getArchetypeCatalog(new Properties());
      } catch(ComponentLookupException ex) {
        String msg = "Error looking up archetype data; " + ex.getMessage();
        MavenPlugin.log(msg, ex);
        throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, ex));
      } catch(ArchetypeDataSourceException ex) {
        String msg = "Error looking up archetype catalog; " + ex.getMessage();
        MavenPlugin.log(msg, ex);
        throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, ex));
      }
    }

  }

  /**
   * Factory for internal ArchetypeCatalog
   */
  public static class InternalCatalogFactory extends ArchetypeCatalogFactory {
    public static final String ID = "internal";

    public InternalCatalogFactory() {
      super(ID, "Internal", false);
    }

    public ArchetypeCatalog getArchetypeCatalog(MavenEmbedderManager manager) throws CoreException {
      return manager.getArchetyper().getInternalCatalog();
    }
  }

  /**
   * Factory for default local ArchetypeCatalog
   */
  public static class DefaultLocalCatalogFactory extends ArchetypeCatalogFactory {
    public static final String ID = "defaultLocal";
    
    public DefaultLocalCatalogFactory() {
      super(ID, "Default Local", false);
    }
    
    public ArchetypeCatalog getArchetypeCatalog(MavenEmbedderManager manager) throws CoreException {
      return manager.getArchetyper().getDefaultLocalCatalog();
    }
  }
  
  /**
   * Factory for local ArchetypeCatalog
   */
  public static class LocalCatalogFactory extends ArchetypeCatalogFactory {

    public LocalCatalogFactory(String path, String description, boolean editable) {
      super(path, description == null || description.trim().length() == 0 ? "Local " + path : description, editable);
    }

    public ArchetypeCatalog getArchetypeCatalog(MavenEmbedderManager manager) throws CoreException {
      return manager.getArchetyper().getLocalCatalog(getId());
    }
  }

  /**
   * Factory for remote ArchetypeCatalog
   */
  public static class RemoteCatalogFactory extends ArchetypeCatalogFactory {

    public RemoteCatalogFactory(String url, String description, boolean editable) {
      super(url, description == null || description.trim().length() == 0 ? "Remote " + url : description, editable);
    }

    public ArchetypeCatalog getArchetypeCatalog(MavenEmbedderManager manager) throws CoreException {
      return manager.getArchetyper().getRemoteCatalog(getId());
    }
  }

}
