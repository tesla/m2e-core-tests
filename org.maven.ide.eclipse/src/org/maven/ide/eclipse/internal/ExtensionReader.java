/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import org.maven.ide.eclipse.archetype.ArchetypeCatalogFactory;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.index.IndexInfo;
import org.maven.ide.eclipse.internal.index.IndexInfoWriter;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;


/**
 * Extension reader
 * 
 * @author Eugene Kuleshov
 */
public class ExtensionReader {

  public static final String EXTENSION_ARCHETYPES = "org.maven.ide.eclipse.archetypeCatalogs";
  
  public static final String EXTENSION_INDEXES = "org.maven.ide.eclipse.indexes";

  public static final String EXTENSION_PROJECT_CONFIGURATORS = "org.maven.ide.eclipse.projectConfigurators";

  private static final String ELEMENT_INDEX = "index";

  private static final String ATTR_INDEX_ID = "indexId";

  private static final String ATTR_INDEX_ARCHIVE = "archive";

  private static final String ATTR_REPOSITORY_URL = "repositoryUrl";

  private static final String ATTR_UPDATE_URL = "updateUrl";

  private static final String ATTR_IS_SHORT = "isShort";

  private static final String ELEMENT_LOCAL_ARCHETYPE = "local";

  private static final String ELEMENT_REMOTE_ARCHETYPE = "remote";

  private static final String ATTR_NAME = "name";

  private static final String ATTR_URL = "url";
  
  private static final String ATTR_DESCRIPTION = "description";

  private static final String ELEMENT_CONFIGURATOR = "configurator";

  /**
   * @param configFile previously saved indexes configuration
   * @return collection of {@link IndexInfo} loaded from given config
   */
  public static Collection<IndexInfo> readIndexInfoConfig(File configFile) {
    if(configFile != null && configFile.exists()) {
      FileInputStream is = null;
      try {
        is = new FileInputStream(configFile);
        IndexInfoWriter writer = new IndexInfoWriter();
        return writer.readIndexInfo(is);
      } catch(IOException ex) {
        MavenLogger.log("Unable to read index configuration", ex);
      } finally {
        if(is != null) {
          try {
            is.close();
          } catch(IOException ex) {
            MavenLogger.log("Unable to close index config stream", ex);
          }
        }
      }
    }

    return Collections.emptyList();
  }

  /**
   * @param configFile previously saved indexes configuration
   * @return collection of {@link IndexInfo} from the extension points
   */
  public static Map<String, IndexInfo> readIndexInfoExtensions() {
    Map<String, IndexInfo> indexes = new LinkedHashMap<String, IndexInfo>();

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint indexesExtensionPoint = registry.getExtensionPoint(EXTENSION_INDEXES);
    if(indexesExtensionPoint != null) {
      IExtension[] indexesExtensions = indexesExtensionPoint.getExtensions();
      for(IExtension extension : indexesExtensions) {
        IContributor contributor = extension.getContributor();
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_INDEX)) {
            IndexInfo indexInfo = readIndexElement(element, contributor);
            indexes.put(indexInfo.getIndexName(), indexInfo);
          }
        }
      }
    }

    return indexes;
  }

  private static IndexInfo readIndexElement(IConfigurationElement element, IContributor contributor) {
    String indexId = element.getAttribute(ATTR_INDEX_ID);
    String repositoryUrl = element.getAttribute(ATTR_REPOSITORY_URL);
    String indexUpdateUrl = element.getAttribute(ATTR_UPDATE_URL);
    boolean isShort = Boolean.valueOf(element.getAttribute(ATTR_IS_SHORT)).booleanValue();

    IndexInfo indexInfo = new IndexInfo(indexId, null, repositoryUrl, IndexInfo.Type.REMOTE, isShort);
    indexInfo.setIndexUpdateUrl(indexUpdateUrl);

    String archive = element.getAttribute(ATTR_INDEX_ARCHIVE);
    if(archive != null) {
      Bundle[] bundles = Platform.getBundles(contributor.getName(), null);
      URL archiveUrl = null;
      for(int i = 0; i < bundles.length && archiveUrl == null; i++ ) {
        Bundle bundle = bundles[i];
        archiveUrl = bundle.getEntry(archive);
        indexInfo.setArchiveUrl(archiveUrl);
      }
      if(archiveUrl == null) {
        MavenLogger.log("Unable to find index archive " + archive + " in " + contributor.getName(), null);
      }
    }

    return indexInfo;
  }

  public static List<ArchetypeCatalogFactory> readArchetypeExtensions() {
    List<ArchetypeCatalogFactory> archetypeCatalogs = new ArrayList<ArchetypeCatalogFactory>();
    
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint archetypesExtensionPoint = registry.getExtensionPoint(EXTENSION_ARCHETYPES);
    if(archetypesExtensionPoint != null) {
      IExtension[] archetypesExtensions = archetypesExtensionPoint.getExtensions();
      for(IExtension extension : archetypesExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        IContributor contributor = extension.getContributor();
        for(IConfigurationElement element : elements) {
          ArchetypeCatalogFactory factory = readArchetypeCatalogs(element, contributor);
          // archetypeManager.addArchetypeCatalogFactory(factory);
          archetypeCatalogs.add(factory);
        }
      }
    }
    return archetypeCatalogs;
  }

  private static ArchetypeCatalogFactory readArchetypeCatalogs(IConfigurationElement element, IContributor contributor) {
    if(ELEMENT_LOCAL_ARCHETYPE.equals(element.getName())) {
      String name = element.getAttribute(ATTR_NAME);
      if(name!=null) {
        Bundle[] bundles = Platform.getBundles(contributor.getName(), null);
        URL catalogUrl = null;
        for(int i = 0; i < bundles.length; i++ ) {
          Bundle bundle = bundles[i];
          catalogUrl = bundle.getEntry(name);
          if(catalogUrl!=null) {
            String description = element.getAttribute(ATTR_DESCRIPTION);
            String url = catalogUrl.toString();
            // XXX ARCHETYPE-161: RemoteCatalogArchetypeDataSource don't allow to download arbitrary urls
            return new ArchetypeCatalogFactory.RemoteCatalogFactory(url.substring(0, url.lastIndexOf("/")), description, false);
          }
        }
        MavenLogger.log("Unable to find Archetype catalog " + name + " in " + contributor.getName(), null);
      }
    } else if(ELEMENT_REMOTE_ARCHETYPE.equals(element.getName())) {
      String url = element.getAttribute(ATTR_URL);
      if(url!=null) {
        String description = element.getAttribute(ATTR_DESCRIPTION);
        return new ArchetypeCatalogFactory.RemoteCatalogFactory(url, description, false);
      }
    }
    return null;
  }

  public static List<AbstractProjectConfigurator> readProjectConfiguratorExtensions(MavenProjectManager projectManager,
      MavenRuntimeManager runtimeManager, MavenConsole console) {
    ArrayList<AbstractProjectConfigurator> projectConfigurators = new ArrayList<AbstractProjectConfigurator>();
    
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_PROJECT_CONFIGURATORS);
    if(configuratorsExtensionPoint != null) {
      IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
      for(IExtension extension : configuratorExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_CONFIGURATOR)) {
            try {
              Object o = element.createExecutableExtension(AbstractProjectConfigurator.ATTR_CLASS);

              AbstractProjectConfigurator projectConfigurator = (AbstractProjectConfigurator) o;
              projectConfigurator.setProjectManager(projectManager);
              projectConfigurator.setRuntimeManager(runtimeManager);
              projectConfigurator.setConsole(console);
              
              projectConfigurators.add(projectConfigurator);
            } catch(CoreException ex) {
              MavenLogger.log(ex);
            }
          }
        }
      }
    }
    
    return projectConfigurators;
  }
}

