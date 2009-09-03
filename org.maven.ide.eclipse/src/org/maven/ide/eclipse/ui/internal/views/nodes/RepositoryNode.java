/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views.nodes;

import org.maven.ide.eclipse.internal.index.NexusIndex;
import org.maven.ide.eclipse.internal.index.NexusIndexManager;
import org.maven.ide.eclipse.internal.index.RepositoryInfo;

/**
 * LocalRepsoitoryNode
 *
 * @author dyocum
 */
public class RepositoryNode extends AbstractIndexedRepositoryNode {

  private final RepositoryInfo repository;

  public RepositoryNode(NexusIndexManager indexManager, RepositoryInfo repository, NexusIndex index){
    super(indexManager, index);
    this.repository = repository;
  }

  public String getName() {
    StringBuilder sb = new StringBuilder();
    sb.append(repository.getId());
    sb.append(" (").append(repository.getRepositoryUrl()).append(")");
    if (repository.getMirrorOf() != null) {
      sb.append(" [mirrorOf=").append(repository.getMirrorOf()).append("]");
    }
    if (repository.getMirrorId() != null) {
      sb.append(" [mirrored by ").append(repository.getMirrorId()).append("]");
    }
    if (isUpdating()) {
      sb.append(" [updating]");
    }
    return sb.toString();
  }

  public String getRepositoryUrl() {
    return repository.getRepositoryUrl();
  }

  public String getRepoName() {
    return repository.getId();
  }

  public boolean isEnabledIndex() {
    return index != null;
  }

}
