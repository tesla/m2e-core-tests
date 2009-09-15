/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.project.IMavenMarkerManager;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.IExtensionLifecycleMapping;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;


/**
 * Extension reader
 * 
 * @author Eugene Kuleshov
 */
public class ExtensionReader {

  public static final String EXTENSION_ARCHETYPES = "org.maven.ide.eclipse.archetypeCatalogs";
  
  public static final String EXTENSION_PROJECT_CONFIGURATORS = "org.maven.ide.eclipse.projectConfigurators";

  public static final String EXTENSION_LIFECYCLE_MAPPINGS = "org.maven.ide.eclipse.lifecycleMappings";

  public static final String EXTENSION_DEFAULT_LIFECYCLE_MAPPINGS = "org.maven.ide.eclipse.defaultLifecycleMappings";

  private static final String ELEMENT_LOCAL_ARCHETYPE = "local";

  private static final String ELEMENT_REMOTE_ARCHETYPE = "remote";

  private static final String ATTR_ID = "id";
  
  private static final String ATTR_NAME = "name";

  private static final String ATTR_URL = "url";
  
  private static final String ATTR_DESCRIPTION = "description";

  private static final String ELEMENT_CONFIGURATOR = "configurator";
  
  private static final String ELEMENT_LIFECYCLE_MAPPING = "lifecycleMapping";

  private static final String ELEMENT_DEFAULT_LIFECYCLE_MAPPING = "defaultLifecycleMapping";

  private static final String ATTR_PACKAGING = "packaging";
  
  private static final String ATTR_LIFECYCLE_MAPPING_ID = "lifecycleMappingId";

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
      IMavenConfiguration runtimeManager, IMavenMarkerManager markerManager, MavenConsole console) {
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
              projectConfigurator.setMavenConfiguration(runtimeManager);
              projectConfigurator.setMarkerManager(markerManager);
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


  
  public static Map<String, ILifecycleMapping> readLifecycleMappingExtensions() {
    Map<String, ILifecycleMapping> lifecycleMappings = new HashMap<String, ILifecycleMapping>();
    
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_LIFECYCLE_MAPPINGS);
    if(configuratorsExtensionPoint != null) {
      IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
      for(IExtension extension : configuratorExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_LIFECYCLE_MAPPING)) {
            try {
              Object o = element.createExecutableExtension(AbstractProjectConfigurator.ATTR_CLASS);
              
              IExtensionLifecycleMapping lifecycleMapping = (IExtensionLifecycleMapping) o;
              String id = element.getAttribute(ATTR_ID);
              lifecycleMapping.setName(element.getAttribute(ATTR_NAME));
              lifecycleMapping.setId(id);

              lifecycleMappings.put(id, lifecycleMapping);

            } catch(CoreException ex) {
              MavenLogger.log(ex);
            }
          }
          
        }
      }
    }
    return lifecycleMappings;
  }

  public static Map<String, String> readDefaultLifecycleMappingExtensions() {
    Map<String, String> defaultLifecycleMappings = new HashMap<String, String>();
    
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint mappingsExtensionPoint = registry.getExtensionPoint(EXTENSION_DEFAULT_LIFECYCLE_MAPPINGS);
    if(mappingsExtensionPoint != null) {
      IExtension[] mappingsExtensions = mappingsExtensionPoint.getExtensions();
      for(IExtension extension : mappingsExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_DEFAULT_LIFECYCLE_MAPPING)) {
            String packaging = element.getAttribute(ATTR_PACKAGING);
            String lifecycleMappingId = element.getAttribute(ATTR_LIFECYCLE_MAPPING_ID);

            if (packaging != null && lifecycleMappingId != null) {
              defaultLifecycleMappings.put(packaging, lifecycleMappingId);
            }
          }
        }
      }
    }
    return defaultLifecycleMappings;
  }

}

