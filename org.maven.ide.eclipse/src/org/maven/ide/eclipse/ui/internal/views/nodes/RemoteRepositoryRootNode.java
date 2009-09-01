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

  public List<ArtifactRepository> getRemoteRepositories() throws Exception {
    LinkedHashSet<ArtifactRepository> repositories = new LinkedHashSet<ArtifactRepository>();
    repositories.addAll(maven.getArtifactRepositories(new NullProgressMonitor()));
    repositories.addAll(maven.getPluginArtifactRepository(new NullProgressMonitor()));
    return new ArrayList<ArtifactRepository>(repositories);
  }

  public Object[] getChildren() {
    NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();

    LinkedHashSet<ArtifactRepository> mirrors = new LinkedHashSet<ArtifactRepository>();

    ArrayList<Object> repoList = new ArrayList<Object>();
    try {
      for(ArtifactRepository repo : getRemoteRepositories()) {
        ArtifactRepository mirror = maven.getMirror(repo);

        NexusIndex index;
        if (mirror == null) {
          index = new NexusIndex(indexManager, repo.getUrl());
        } else {
          index = null;
          mirrors.add(mirror);
        }

        RepositoryNode repoNode = new RepositoryNode(indexManager, index, repo, mirror);

        repoList.add(repoNode);
      }

      for (ArtifactRepository mirror : mirrors) {
        NexusIndex index = new NexusIndex(indexManager, mirror.getUrl());
        RepositoryNode mirrorNode = new RepositoryNode(indexManager, index, mirror, null);
        repoList.add(0, mirrorNode);
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
    // TODO Auto-generated method isUpdating
    return false;
  }

}
