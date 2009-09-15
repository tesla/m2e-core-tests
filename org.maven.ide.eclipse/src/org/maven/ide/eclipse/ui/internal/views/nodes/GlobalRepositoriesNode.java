/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views.nodes;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Image;

import org.maven.ide.eclipse.MavenImages;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.internal.index.NexusIndex;
import org.maven.ide.eclipse.internal.index.NexusIndexManager;
import org.maven.ide.eclipse.repository.IRepository;
import org.maven.ide.eclipse.repository.IRepositoryRegistry;

/**
 * Parent node for all artifact repositories and mirrors defined in settings.xml.
 * 
 * @author dyocum
 */
public class GlobalRepositoriesNode implements IMavenRepositoryNode {

  private NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();
  private IRepositoryRegistry repositoryRegistry = MavenPlugin.getDefault().getRepositoryRegistry();

  public Object[] getChildren() {

    ArrayList<Object> mirrorNodes = new ArrayList<Object>();
    ArrayList<Object> globalRepoNodes = new ArrayList<Object>();

    for (IRepository repo : repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_SETTINGS)) {
      NexusIndex index = indexManager.getIndex(repo);
      RepositoryNode node = new RepositoryNode(index);
      if (repo.getMirrorOf() != null) {
        mirrorNodes.add(node); 
      } else {
        globalRepoNodes.add(node);
      }
    }

    ArrayList<Object> nodes = new ArrayList<Object>();
    nodes.addAll(mirrorNodes);
    nodes.addAll(globalRepoNodes);

    return nodes.toArray(new Object[nodes.size()]);
  }

  public String getName() {
    return "Global Repositories";
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
