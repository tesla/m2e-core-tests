/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views.nodes;

import org.apache.maven.settings.Mirror;

import org.maven.ide.eclipse.internal.index.NexusIndex;
import org.maven.ide.eclipse.internal.index.NexusIndexManager;


/**
 * MirrorNode
 * 
 * @author dyocum
 */
public class MirrorNode extends AbstractIndexedRepositoryNode {

  private Mirror mirror;

  public MirrorNode(NexusIndexManager indexManager, NexusIndex index, Mirror mirror) {
    super(indexManager, index);
    this.mirror = mirror;
  }

  public String getName() {
    StringBuilder sb = new StringBuilder();
    sb.append(mirror.getId());
    sb.append(" (").append(mirror.getUrl()).append(")");
    sb.append(" [mirrorOf=").append(mirror.getMirrorOf()).append("]");
    return sb.toString();
  }

  public String getRepositoryUrl() {
    return mirror.getUrl();
  }
}
