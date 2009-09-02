/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views.nodes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.graphics.Image;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.settings.Mirror;

import org.maven.ide.eclipse.MavenImages;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.internal.index.NexusIndex;
import org.maven.ide.eclipse.internal.index.NexusIndexManager;

/**
 * Parent node for all configured artifact repositories and their mirrors.
 * 
 * @author dyocum
 */
public class RemoteRepositoryRootNode implements IMavenRepositoryNode {

  IMaven maven = MavenPlugin.getDefault().getMaven();
  NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();

  public List<ArtifactRepository> getRemoteRepositories() throws Exception {
    LinkedHashSet<ArtifactRepository> repositories = new LinkedHashSet<ArtifactRepository>();
    repositories.addAll(maven.getArtifactRepositories(new NullProgressMonitor()));
    repositories.addAll(maven.getPluginArtifactRepository(new NullProgressMonitor()));
    return new ArrayList<ArtifactRepository>(repositories);
  }

  public Object[] getChildren() {

    ArrayList<Object> repoList = new ArrayList<Object>();
    try {
      // mirrors
      MavenExecutionRequest request = maven.createExecutionRequest(null);
      maven.populateDefaults(request);
      for (Mirror mirror : request.getMirrors()) {
        NexusIndex index = new NexusIndex(indexManager, mirror.getUrl());
        MirrorNode mirrorNode = new MirrorNode(indexManager, index, mirror);
        repoList.add(mirrorNode);
      }

      // repositories
      for(ArtifactRepository repo : getRemoteRepositories()) {
        Mirror mirror = maven.getMirror(repo);
        NexusIndex index = mirror == null? new NexusIndex(indexManager, repo.getUrl()) : null;
        RepositoryNode repoNode = new RepositoryNode(indexManager, index, repo, mirror);
        repoList.add(repoNode);
      }
    } catch(Exception e) {
      MavenLogger.log("Unable to load remote repositories", e);
    }
    return repoList.toArray(new Object[repoList.size()]);
  }

  public String getName() {
    return "Remote Repositories";
  }

  public String toString() {
    return getName();
  }

  public boolean hasChildren() {
    Object[] kids = getChildren();
    return kids != null && kids.length > 0;
  }

  public Image getImage() {
    return MavenImages.IMG_INDEXES;
  }

  public boolean isUpdating() {
    return false;
  }

}
