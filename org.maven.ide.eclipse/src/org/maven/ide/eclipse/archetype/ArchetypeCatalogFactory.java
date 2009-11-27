/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.archetype;

import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.apache.maven.archetype.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.source.ArchetypeDataSource;
import org.apache.maven.archetype.source.ArchetypeDataSourceException;

import org.maven.ide.eclipse.MavenPlugin;
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

  public abstract ArchetypeCatalog getArchetypeCatalog() throws CoreException;

  public String toString() {
    return getId();
  }

  protected Archetype getArchetyper() {
    return MavenPlugin.lookup(Archetype.class);
  }

  /**
   * Factory for Nexus Indexer ArchetypeCatalog
   */
  public static class NexusIndexerCatalogFactory extends ArchetypeCatalogFactory {
    public static final String ID = "nexusIndexer";

    public NexusIndexerCatalogFactory() {
      super(ID, "Nexus Indexer", false);
    }

    public ArchetypeCatalog getArchetypeCatalog() throws CoreException {
      try {
        ArchetypeDataSource source = MavenPlugin.lookup(ArchetypeDataSource.class, "nexus");
        return source.getArchetypeCatalog(new Properties());
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

    public ArchetypeCatalog getArchetypeCatalog() {
      return getArchetyper().getInternalCatalog();
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

    public ArchetypeCatalog getArchetypeCatalog() {
      return getArchetyper().getDefaultLocalCatalog();
    }
  }

  /**
   * Factory for local ArchetypeCatalog
   */
  public static class LocalCatalogFactory extends ArchetypeCatalogFactory {

    public LocalCatalogFactory(String path, String description, boolean editable) {
      super(path, description == null || description.trim().length() == 0 ? "Local " + path : description, editable);
    }

    public ArchetypeCatalog getArchetypeCatalog() {
      return getArchetyper().getLocalCatalog(getId());
    }
  }

  /**
   * Factory for remote ArchetypeCatalog
   */
  public static class RemoteCatalogFactory extends ArchetypeCatalogFactory {

    private String repositoryUrl = null;
    
    public RemoteCatalogFactory(String url, String description, boolean editable) {
      super(url, description == null || description.trim().length() == 0 ? "Remote " + url : description, editable);
      repositoryUrl = parseCatalogUrl(url);
    }

    /**
     * @param url
     * @return
     
     */
    private String parseCatalogUrl(String url) {
      if (url == null) {
        return null;
      }
      int length = url.length();
      if (length > 1 && url.endsWith("/"))
      {
        return url.substring(0, url.length()-1);
      }
      int idx = url.lastIndexOf("/");
      idx = (idx>0)?idx:0; 
      if (url.lastIndexOf(".") >= idx) {
        //Assume last fragment of the url is a file, let's keep its parent folder
        return url.substring(0, idx);
      }
      return url;
    }

    public ArchetypeCatalog getArchetypeCatalog() {
      return getArchetyper().getRemoteCatalog(getId());
    }

    
    /**
     * @return the url of the remote repository hosting the catalog
     */
    public String getRepositoryUrl() {
      return repositoryUrl;
    }
  }

}
