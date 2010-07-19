/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project.registry;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.internal.DefaultPluginDependenciesResolver;



public class EclipsePluginDependenciesResolver extends DefaultPluginDependenciesResolver {

  /*
   * Plugin realms are cached and there is currently no way to purge cached
   * realms due to http://jira.codehaus.org/browse/MNG-4194.
   * 
   * Workspace plugins cannot be cached, so we disable this until MNG-4194 is fixed.
   * 
   * Corresponding m2e JIRA https://issues.sonatype.org/browse/MNGECLIPSE-1448
   */

  @Override
  public Artifact resolve(Plugin plugin, ArtifactResolutionRequest request) throws PluginResolutionException {
    boolean disabled = EclipseWorkspaceArtifactRepository.isDisabled();
    EclipseWorkspaceArtifactRepository.setDisabled(true);
    try {
      return super.resolve(plugin, request);
    } finally {
      EclipseWorkspaceArtifactRepository.setDisabled(disabled);
    }
  }

  @Override
  public List<Artifact> resolve(Plugin plugin, Artifact pluginArtifact, ArtifactResolutionRequest request,
      ArtifactFilter dependencyFilter) throws PluginResolutionException {
    boolean disabled = EclipseWorkspaceArtifactRepository.isDisabled();
    EclipseWorkspaceArtifactRepository.setDisabled(true);
    try {
      return super.resolve(plugin, pluginArtifact, request, dependencyFilter);
    } finally {
      EclipseWorkspaceArtifactRepository.setDisabled(disabled);
    }
  }

}
