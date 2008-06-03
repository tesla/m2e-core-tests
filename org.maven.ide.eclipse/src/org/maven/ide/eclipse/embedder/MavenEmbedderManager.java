/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import org.apache.maven.archetype.Archetype;
import org.apache.maven.embedder.ContainerCustomizer;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.MavenExecutionRequest;

import org.maven.ide.eclipse.MavenConsole;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.internal.embedder.PluginConsoleMavenEmbeddedLogger;


/**
 * Maven Embedder manager
 *
 * @author Eugene Kuleshov
 */
public class MavenEmbedderManager {
  private final MavenConsole console;
  private final MavenRuntimeManager runtimeManager;

  private MavenEmbedder workspaceEmbedder;

  
  public MavenEmbedderManager(MavenConsole console, MavenRuntimeManager runtimeManager) {
    this.console = console;
    this.runtimeManager = runtimeManager;
  }
  
  public synchronized MavenEmbedder createEmbedder(ContainerCustomizer customizer) throws MavenEmbedderException {
    boolean debug = runtimeManager.isDebugOutput();
    String userSettings = runtimeManager.getUserSettingsFile();
    String globalSettings = runtimeManager.getGlobalSettingsFile();

    return EmbedderFactory.createMavenEmbedder(customizer,
        new PluginConsoleMavenEmbeddedLogger(console, debug), globalSettings, userSettings);
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
      } catch(MavenEmbedderException ex) {
        String msg = "Can't create workspace embedder";
        console.logError(msg + "; " + ex.getMessage());
        MavenPlugin.log(msg, ex);
      } 
    }
    return this.workspaceEmbedder;
  }

  public void invalidateMavenSettings() {
    shutdown();
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
      localRepositoryDir.mkdirs();
    }
    if(!localRepositoryDir.isDirectory()) {
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
      MavenPlugin.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, msg, ex));
    }
  }

}
