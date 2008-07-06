/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.source.ArchetypeDataSource;
import org.apache.maven.archetype.source.ArchetypeDataSourceException;

import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;

/**
 * Abstract ArchetypeCatalog factory
 */
public abstract class ArchetypeCatalogFactory {
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

  
  /**
   * Factory for Nexus Indexer ArchetypeCatalog
   */
  public static class NexusIndexerCatalogFactory extends ArchetypeCatalogFactory {
    public static final String ID = "nexusIndexer";

    public NexusIndexerCatalogFactory() {
      super(ID, "Nexus Indexer", false);
    }

    public ArchetypeCatalog getArchetypeCatalog(MavenEmbedderManager manager) throws CoreException {
//      ArchetypeCatalog catalog = new ArchetypeCatalog();
//
//      try {
//        IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
//        Map search = indexManager.search("maven-archetype", IndexManager.SEARCH_PACKAGING);
//
//        for(Iterator it = search.values().iterator(); it.hasNext();) {
//          IndexedArtifact artifact = (IndexedArtifact) it.next();
//
//          for(Iterator it2 = artifact.files.iterator(); it2.hasNext();) {
//            IndexedArtifactFile af = (IndexedArtifactFile) it2.next();
//            Archetype archetype = new Archetype();
//            archetype.setGroupId(af.group);
//            archetype.setArtifactId(af.artifact);
//            archetype.setVersion(af.version);
//            // archetype.setDescription(af.description);
//            IndexInfo indexInfo = indexManager.getIndexInfo(af.repository);
//            if(indexInfo.getType()==IndexInfo.Type.REMOTE) {
//              archetype.setRepository(indexInfo.getRepositoryUrl());
//            }
//            
//            catalog.addArchetype(archetype);
//          }
//        }
//        
//      } catch(Exception ex) {
//        MavenPlugin.log("Unable to retrieve archetypes", ex);
//      }
//
//      return catalog;
      
      PlexusContainer container = manager.getWorkspaceEmbedder().getPlexusContainer();
      try {
        ArchetypeDataSource source = (ArchetypeDataSource) container.lookup(ArchetypeDataSource.class, "nexus");
        return source.getArchetypeCatalog(new Properties());
      } catch(ComponentLookupException ex) {
        String msg = "Error looking up archetype data; " + ex.getMessage();
        MavenLogger.log(msg, ex);
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
      } catch(ArchetypeDataSourceException ex) {
        String msg = "Error looking up archetype catalog; " + ex.getMessage();
        MavenLogger.log(msg, ex);
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
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

