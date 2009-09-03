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
import org.maven.ide.eclipse.internal.index.RepositoryInfo;


/**
 * Parent node for all artifact repositories configured in pom.xml files.
 */
public class ProjectRepositoriesNode implements IMavenRepositoryNode {

  NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();

  public Object[] getChildren() {
    ArrayList<Object> nodes = new ArrayList<Object>();
    for(RepositoryInfo repo : indexManager.getRepositories()) {
      if(!repo.isGlobal()) {
        NexusIndex index = indexManager.getIndex(repo.getUrl());
        RepositoryNode node = new RepositoryNode(indexManager, repo, index);
        nodes.add(node);
      }
    }
    return nodes.toArray(new Object[nodes.size()]);
  }

  public Image getImage() {
    return MavenImages.IMG_INDEXES;
  }

  public String getName() {
    return "Project Repositories";
  }

  public String toString() {
    return getName();
  }

  public boolean hasChildren() {
    Object[] kids = getChildren();
    return kids != null && kids.length > 0;
  }

  public boolean isUpdating() {
    return false;
  }

}
