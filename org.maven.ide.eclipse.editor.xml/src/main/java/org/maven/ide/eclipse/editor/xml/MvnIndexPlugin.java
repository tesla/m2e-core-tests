/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml;

import java.io.IOException;

import org.osgi.framework.BundleContext;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.editor.xml.search.CompositeSearchEngine;
import org.maven.ide.eclipse.editor.xml.search.SearchEngine;


/**
 * @author Lukas Krecan
 */
public class MvnIndexPlugin extends AbstractUIPlugin {
  public static final String PLUGIN_ID = "org.maven.ide.eclipse.editor.xml";

  private static MvnIndexPlugin defaultInstance;

  private CompositeSearchEngine searchEngine;

  private TemplateStore templateStore;

  private ContributionContextTypeRegistry contextTypeRegistry;

  private static final String TEMPLATES_KEY = PLUGIN_ID + ".templates";

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    defaultInstance = this;
    initSearchEngines();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    defaultInstance = null;
  }

  protected void initSearchEngines() {
    IExtension[] extensions = Platform.getExtensionRegistry().getExtensionPoint(PLUGIN_ID, "searchEngine")
        .getExtensions();
    searchEngine = new CompositeSearchEngine();
    for(IExtension extension : extensions) {
      for(IConfigurationElement configElement : extension.getConfigurationElements()) {
        if("search-engine".equals(configElement.getName())) {
          try {
            searchEngine.addSearchEngine((SearchEngine) configElement.createExecutableExtension("class"));
          } catch(CoreException e) {
            MavenLogger.log("Error when initializing extension", e);
          }
        }
      }
    }
  }

  public static MvnIndexPlugin getDefault() {
    return defaultInstance;
  }

//  public void logError(String message, Throwable e) {
//    getLog().log(new Status(Status.ERROR, PLUGIN_ID, 0, message, e));
//  }
//
//  public void logWarn(String message, Throwable e) {
//    getLog().log(new Status(Status.WARNING, PLUGIN_ID, 0, message, e));
//  }
//
//  public void log(Throwable e) {
//    getLog().log(new Status(Status.ERROR, PLUGIN_ID, 0, "MvnIndex plugin error", e));
//  }
//
  public SearchEngine getSearchEngine() {
    return searchEngine;
  }

  /**
   * Returns the template store.
   * 
   * @return the template store.
   */
  public TemplateStore getTemplateStore() {
    if(templateStore == null) {
      templateStore = new ContributionTemplateStore(getTemplateContextRegistry(), getPreferenceStore(), TEMPLATES_KEY);
      try {
        templateStore.load();
      } catch(IOException ex) {
        MavenLogger.log("Unable to load pom templates", ex);
      }
    }
    return templateStore;
  }

  /**
   * Returns the template context type registry.
   * 
   * @return the template context type registry
   */
  public ContextTypeRegistry getTemplateContextRegistry() {
    if(contextTypeRegistry == null) {
      ContributionContextTypeRegistry registry = new ContributionContextTypeRegistry();
      for(PomTemplateContext contextType : PomTemplateContext.values()) {
        registry.addContextType(contextType.getContextTypeId());
      }
      contextTypeRegistry = registry;
    }
    return contextTypeRegistry;
  }

  public ContextTypeRegistry getContextTypeRegistry() {
    if(contextTypeRegistry == null) {
      contextTypeRegistry = new ContributionContextTypeRegistry();
      // TemplateContextType contextType = new TemplateContextType(CONTEXT_TYPE, "POM XML Editor");
      PomTemplateContextType contextType = new PomTemplateContextType();
      contextTypeRegistry.addContextType(contextType);
    }
    return contextTypeRegistry;
  }
}
