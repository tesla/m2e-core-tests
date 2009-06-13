/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import org.codehaus.plexus.classworlds.realm.ClassRealm;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.DefaultPluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;

/**
 * EclipsePluginManager
 *
 * @author igor
 */
public class EclipsePluginManager extends DefaultPluginManager {

  public synchronized ClassRealm getPluginRealm(MavenSession session, PluginDescriptor pluginDescriptor)
      throws PluginManagerException {
    /*
     * Plugin realms are cached and there is currently no way to purge cached
     * realms due to http://jira.codehaus.org/browse/MNG-4194.
     * 
     * Workspace plugins cannot be cached, so we disable this until MNG-4194 is fixed.
     */
    
    boolean disabled = EclipseWorkspaceArtifactRepository.isDisabled();
    EclipseWorkspaceArtifactRepository.setDisabled(true);
    try {
      return super.getPluginRealm(session, pluginDescriptor);
    } finally {
      EclipseWorkspaceArtifactRepository.setDisabled(disabled);
    }
  }
}
