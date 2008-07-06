/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import org.apache.maven.archetype.Archetype;
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ContainerCustomizer;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.MavenExecutionRequest;

import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;


/**
 * Maven Embedder manager
 *
 * @author Eugene Kuleshov
 */
public class MavenEmbedderManager {
  private final MavenConsole console;
  private final MavenRuntimeManager runtimeManager;

  private MavenEmbedder workspaceEmbedder;

  private List<AbstractMavenEmbedderListener> listeners = new CopyOnWriteArrayList<AbstractMavenEmbedderListener>(); 
  
  public MavenEmbedderManager(MavenConsole console, MavenRuntimeManager runtimeManager) {
    this.console = console;
    this.runtimeManager = runtimeManager;
  }
  
  public Configuration createDefaultConfiguration(ContainerCustomizer customizer) {
    DefaultConfiguration configuration = new DefaultConfiguration();

    boolean debug = runtimeManager.isDebugOutput();
    String userSettings = runtimeManager.getUserSettingsFile();
    String globalSettings = runtimeManager.getGlobalSettingsFile();

    configuration.setMavenEmbedderLogger(new PluginConsoleMavenEmbeddedLogger(console, debug));

    if(userSettings != null) {
      configuration.setUserSettingsFile(new File(userSettings));
    }

    if(globalSettings != null) {
      configuration.setGlobalSettingsFile(new File(globalSettings));
    }
    
    configuration.setConfigurationCustomizer(customizer);
    
    return configuration;
  }
  
  public synchronized MavenEmbedder createEmbedder(ContainerCustomizer customizer) throws MavenEmbedderException {
    Configuration configuration = createDefaultConfiguration(customizer);
    return EmbedderFactory.createMavenEmbedder(configuration, null);
  }

  public synchronized MavenExecutionRequest createRequest(MavenEmbedder embedder) {
    boolean offline = runtimeManager.isOffline();
    boolean debug = runtimeManager.isDebugOutput();

    return EmbedderFactory.createMavenExecutionRequest(embedder, offline, debug);
  }
  
  public MavenEmbedder getWorkspaceEmbedder() {
    if(this.workspaceEmbedder==null) {
      try {
        this.workspaceEmbedder = createEmbedder(EmbedderFactory.createExecutionCustomizer());
        
        for(AbstractMavenEmbedderListener listener : listeners) {
          listener.workspaceEmbedderCreated();
        }
        
      } catch(MavenEmbedderException ex) {
        String msg = "Can't create workspace embedder";
        console.logError(msg + "; " + ex.getMessage());
        MavenLogger.log(msg, ex);
      } 
    }
    return this.workspaceEmbedder;
  }

  public void invalidateMavenSettings() {
    shutdown();
    
    for(AbstractMavenEmbedderListener listener : listeners) {
      listener.workspaceEmbedderInvalidated();
    }
  }

  public void shutdown() {
    // XXX need to wait when embedder jobs will be completed 
    if(workspaceEmbedder!=null) {
      try {
        workspaceEmbedder.stop();
      } catch(MavenEmbedderException ex) {
        console.logError("Error on stopping project embedder "+ex.getMessage());
      }
      workspaceEmbedder = null;

      for(AbstractMavenEmbedderListener listener : listeners) {
        listener.workspaceEmbedderDestroyed();
      }
    }
  }
  

  public File getLocalRepositoryDir() {
    String localRepository = getWorkspaceEmbedder().getLocalRepository().getBasedir();
    
    File localRepositoryDir = new File(localRepository);
    
  //    if(!localRepositoryDir.exists()) {
  //      console.logError("Created local repository folder "+localRepository);
  //      localRepositoryDir.mkdirs();
  //    }
    
    if(!localRepositoryDir.exists()) {
      boolean created = localRepositoryDir.mkdirs();
      if(!created) {
        console.logError("Can'c create local Maven repository folder " + localRepository);
      }
    }
    if(localRepositoryDir.exists() && !localRepositoryDir.isDirectory()) {
      console.logError("Local repository "+localRepository+" is not a directory");
    }
    
    return localRepositoryDir;
  }

  public Archetype getArchetyper() throws CoreException {
    PlexusContainer container = getWorkspaceEmbedder().getPlexusContainer();

    try {
      return (Archetype) container.lookup(Archetype.class);
    } catch(ComponentLookupException ex) {
      String msg = "Error looking up the archetyper: " + ex.getMessage();
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, msg, ex));
    }
  }
  
  public void addListener(AbstractMavenEmbedderListener listener) {
    listeners.add(listener);
  }

  public void removeListener(AbstractMavenEmbedderListener listener) {
    listeners.remove(listener);
  }
  
}
