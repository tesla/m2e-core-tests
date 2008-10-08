
/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt;

import java.io.File;

import org.osgi.framework.BundleContext;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.embedder.AbstractMavenEmbedderListener;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.jdt.internal.launch.MavenLaunchConfigurationListener;
import org.maven.ide.eclipse.project.MavenProjectManager;


public class MavenJdtPlugin extends AbstractUIPlugin {

  public static String PLUGIN_ID = "org.maven.ide.eclipse.jdt";
  
  private static MavenJdtPlugin instance;

  MavenLaunchConfigurationListener launchConfigurationListener;
  BuildPathManager buildpathManager;
  
  public MavenJdtPlugin() {
    instance = this;

    if(Boolean.parseBoolean(Platform.getDebugOption(PLUGIN_ID + "/debug/initialization"))) {
      System.err.println("### executing constructor " + PLUGIN_ID); //$NON-NLS-1$
      new Throwable().printStackTrace();
    }
  }

  public void start(BundleContext bundleContext) throws Exception {
    if(Boolean.parseBoolean(Platform.getDebugOption(PLUGIN_ID + "/debug/initialization"))) {
      System.err.println("### executing start() " + PLUGIN_ID); //$NON-NLS-1$
      new Throwable().printStackTrace();
    }
    
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    MavenPlugin mavenPlugin = MavenPlugin.getDefault();

    MavenProjectManager projectManager = mavenPlugin.getMavenProjectManager();
    MavenEmbedderManager embedderManager = mavenPlugin.getMavenEmbedderManager();
    MavenConsole console = mavenPlugin.getConsole();
    IndexManager indexManager = mavenPlugin.getIndexManager();
    MavenModelManager modelManager = mavenPlugin.getMavenModelManager();
    MavenRuntimeManager runtimeManager = mavenPlugin.getMavenRuntimeManager();

    File stateLocationDir = mavenPlugin.getStateLocation().toFile(); // TODO migrate JDT settings to this plugin's store

    this.buildpathManager = new BuildPathManager(embedderManager, console, projectManager, indexManager, modelManager,
        runtimeManager, bundleContext, stateLocationDir);
    workspace.addResourceChangeListener(buildpathManager, IResourceChangeEvent.PRE_DELETE);

    projectManager.addMavenProjectChangedListener(this.buildpathManager);
    projectManager.addDownloadSourceListener(this.buildpathManager);

    embedderManager.addListener(new AbstractMavenEmbedderListener() {
      public void workspaceEmbedderInvalidated() {
        buildpathManager.setupVariables();
      }
    });

    this.launchConfigurationListener = new MavenLaunchConfigurationListener();
    DebugPlugin.getDefault().getLaunchManager().addLaunchConfigurationListener(launchConfigurationListener);
    projectManager.addMavenProjectChangedListener(launchConfigurationListener);
  }

  public void stop(BundleContext context) throws Exception {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    MavenPlugin mavenPlugin = MavenPlugin.getDefault();

    MavenProjectManager projectManager = mavenPlugin.getMavenProjectManager();
    projectManager.removeMavenProjectChangedListener(buildpathManager);
    projectManager.removeDownloadSourceListener(this.buildpathManager);

    workspace.removeResourceChangeListener(this.buildpathManager);

    DebugPlugin.getDefault().getLaunchManager().removeLaunchConfigurationListener(launchConfigurationListener);
    projectManager.removeMavenProjectChangedListener(launchConfigurationListener);

    this.buildpathManager = null;
    this.launchConfigurationListener = null;
  }

  public static MavenJdtPlugin getDefault() {
    return instance;
  }

  public BuildPathManager getBuildpathManager() {
    return buildpathManager;
  }
}
