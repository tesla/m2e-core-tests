/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views.nodes;

import org.maven.ide.eclipse.internal.index.NexusIndex;
import org.maven.ide.eclipse.repository.IRepository;

/**
 * LocalRepositoryNode
 *
 * @author igor
 */
public class LocalRepositoryNode extends AbstractIndexedRepositoryNode {

  public LocalRepositoryNode(NexusIndex index) {
    super(index);
  }

  public String getName() {
    IRepository repository = index.getRepository();
    StringBuilder sb = new StringBuilder();
    sb.append("Local Repository");
    if (repository.getBasedir() != null) {
      sb.append(" (").append(repository.getBasedir().getAbsolutePath()).append(')');
    }
    return sb.toString();
  }
}
