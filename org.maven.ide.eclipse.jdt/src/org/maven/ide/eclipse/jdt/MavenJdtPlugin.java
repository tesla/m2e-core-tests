
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
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.embedder.AbstractMavenConfigurationChangeListener;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.embedder.MavenConfigurationChangeEvent;
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
    MavenConsole console = mavenPlugin.getConsole();
    IndexManager indexManager = mavenPlugin.getIndexManager();
    IMavenConfiguration mavenConfiguration = MavenPlugin.getDefault().getMavenConfiguration();

    File stateLocationDir = mavenPlugin.getStateLocation().toFile(); // TODO migrate JDT settings to this plugin's store

    this.buildpathManager = new BuildPathManager(console, projectManager, indexManager, bundleContext, stateLocationDir);
    workspace.addResourceChangeListener(buildpathManager, IResourceChangeEvent.PRE_DELETE);

    projectManager.addMavenProjectChangedListener(this.buildpathManager);

    mavenConfiguration.addConfigurationChangeListener(new AbstractMavenConfigurationChangeListener() {
      public void mavenConfigutationChange(MavenConfigurationChangeEvent event) {
        if (!MavenConfigurationChangeEvent.P_USER_SETTINGS_FILE.equals(event.getKey())) {
          return;
        }

        if (buildpathManager.setupVariables() && buildpathManager.variablesAreInUse()) {
          WorkspaceJob job = new WorkspaceJob("Building...") {

            public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
              ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
              return Status.OK_STATUS;
            }
            
          };
          job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
          job.schedule();
      }}});

    this.launchConfigurationListener = new MavenLaunchConfigurationListener();
    DebugPlugin.getDefault().getLaunchManager().addLaunchConfigurationListener(launchConfigurationListener);
    projectManager.addMavenProjectChangedListener(launchConfigurationListener);
  }

  public void stop(BundleContext context) throws Exception {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    MavenPlugin mavenPlugin = MavenPlugin.getDefault();

    MavenProjectManager projectManager = mavenPlugin.getMavenProjectManager();
    projectManager.removeMavenProjectChangedListener(buildpathManager);

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
