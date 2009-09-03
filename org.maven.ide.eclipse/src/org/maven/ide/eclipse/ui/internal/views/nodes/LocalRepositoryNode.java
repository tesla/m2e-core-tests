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

/**
 * LocalRepositoryNode
 *
 * @author igor
 */
public class LocalRepositoryNode extends AbstractIndexedRepositoryNode {

  private final String repositoryUrl;

  public LocalRepositoryNode(NexusIndexManager indexManager, String repositoryUrl, NexusIndex index) {
    super(indexManager, index);
    this.repositoryUrl = repositoryUrl;
  }

  public String getName() {
    StringBuilder sb = new StringBuilder();
    sb.append(index.getIndexName());
    // TODO local repository path
    return sb.toString();
  }

  public String getRepositoryUrl() {
    return repositoryUrl;
  }
}
