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
 * WorkspaceRepositoryNode
 *
 * @author igor
 */
public class WorkspaceRepositoryNode extends AbstractIndexedRepositoryNode {

  public WorkspaceRepositoryNode(NexusIndex index) {
    super(index);
  }

  public String getName() {
    return "Workspace Projects";
  }

}
