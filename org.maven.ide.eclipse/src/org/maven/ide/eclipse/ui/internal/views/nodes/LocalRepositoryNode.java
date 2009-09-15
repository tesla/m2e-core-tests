/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views.nodes;

import org.maven.ide.eclipse.internal.index.NexusIndex;

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
    StringBuilder sb = new StringBuilder();
    sb.append(index.getRepository().toString());
    // TODO local repository path
    return sb.toString();
  }

  public boolean isEnabledIndex(){
    return false;
  }
}
