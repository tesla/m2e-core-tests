/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.internal.DefaultMavenPluginManager;

/**
 * EclipsePluginManager
 *
 * @author igor
 */
public class EclipseMavenPluginManager extends DefaultMavenPluginManager {
  
  public void setupPluginRealm(PluginDescriptor pluginDescriptor, MavenSession session, ClassLoader parent,
      List<String> imports) throws PluginResolutionException, PluginManagerException {
    /*
     * Plugin realms are cached and there is currently no way to purge cached
     * realms due to http://jira.codehaus.org/browse/MNG-4194.
     * 
     * Workspace plugins cannot be cached, so we disable this until MNG-4194 is fixed.
     */
    
    boolean disabled = EclipseWorkspaceArtifactRepository.isDisabled();
    EclipseWorkspaceArtifactRepository.setDisabled(true);
    try {
      super.setupPluginRealm(pluginDescriptor, session, parent, imports);
    } finally {
      EclipseWorkspaceArtifactRepository.setDisabled(disabled);
    }
  }
}
