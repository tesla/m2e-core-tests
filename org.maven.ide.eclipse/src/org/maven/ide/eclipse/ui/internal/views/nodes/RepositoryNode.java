/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views.nodes;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.settings.Mirror;

import org.maven.ide.eclipse.internal.index.NexusIndex;
import org.maven.ide.eclipse.internal.index.NexusIndexManager;

/**
 * LocalRepsoitoryNode
 *
 * @author dyocum
 */
public class RepositoryNode extends AbstractIndexedRepositoryNode {

  private final ArtifactRepository repository;
  private final Mirror mirror;

  public RepositoryNode(NexusIndexManager indexManager, NexusIndex index, ArtifactRepository repository, Mirror mirror){
    super(indexManager, index);
    this.repository = repository;
    this.mirror = mirror;
  }

  public String getName() {
    StringBuilder sb = new StringBuilder();
    sb.append(repository.getId());
    sb.append(" (").append(repository.getUrl()).append(")");
    if (mirror != null) {
      sb.append(" [mirrored by ").append(mirror.getId()).append("]");
    }
    if (isUpdating()) {
      sb.append(" [updating]");
    }
    return sb.toString();
  }

  public String getRepositoryUrl() {
    return repository.getUrl();
  }

  public String getRepoName() {
    return repository.getId();
  }

  public boolean isEnabledIndex() {
    return index != null;
  }

}
