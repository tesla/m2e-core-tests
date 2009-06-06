/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.internal.ExtensionReader;
import org.maven.ide.eclipse.internal.project.DefaultLifecycleMapping.ProjectConfiguratorComparator;
import org.maven.ide.eclipse.project.IMavenMarkerManager;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;


/**
 * AbstractLifecycleMapping
 *
 * @author igor
 */
public abstract class AbstractLifecycleMapping implements ILifecycleMapping {

  private static List<AbstractProjectConfigurator> configurators;

  public List<AbstractProjectConfigurator> getProjectConfigurators() {
    synchronized(AbstractLifecycleMapping.class) {
      if(configurators == null) {
        MavenPlugin plugin = MavenPlugin.getDefault();
        MavenProjectManager projectManager = plugin.getMavenProjectManager();
        IMavenConfiguration mavenConfiguration;
        mavenConfiguration = MavenPlugin.lookup(IMavenConfiguration.class);
        IMavenMarkerManager mavenMarkerManager = plugin.getMavenMarkerManager();
        MavenConsole console = plugin.getConsole();
        configurators = new ArrayList<AbstractProjectConfigurator>(ExtensionReader
            .readProjectConfiguratorExtensions(projectManager, mavenConfiguration, mavenMarkerManager, console));
        Collections.sort(configurators, new ProjectConfiguratorComparator());
      }
      return configurators;
    }
  }

}
