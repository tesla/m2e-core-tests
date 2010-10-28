/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.views.nodes;

import org.eclipse.m2e.core.internal.index.NexusIndex;
import org.eclipse.m2e.core.repository.IRepository;

/**
 * LocalRepsoitoryNode
 *
 * @author dyocum
 */
public class RepositoryNode extends AbstractIndexedRepositoryNode {

  private final IRepository repository;

  public RepositoryNode(NexusIndex index){
    super(index);
    this.repository = index.getRepository();
  }

  public String getName() {
    StringBuilder sb = new StringBuilder();
    sb.append(repository.getId());
    sb.append(" (").append(repository.getUrl()).append(")");
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
    return repository.getUrl();
  }

  public String getRepoName() {
    return repository.toString();
  }

}
