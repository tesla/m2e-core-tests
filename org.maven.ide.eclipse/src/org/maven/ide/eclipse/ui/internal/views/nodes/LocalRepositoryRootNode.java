/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views.nodes;

import org.eclipse.swt.graphics.Image;

import org.maven.ide.eclipse.MavenImages;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.index.IMutableIndex;
import org.maven.ide.eclipse.internal.index.NexusIndexManager;

/**
 * LocalRepositoryNode
 *
 * @author dyocum
 */
public class LocalRepositoryRootNode implements IMavenRepositoryNode{

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.IMavenRepositoryRootNode#getElements()
   */
  public Object[] getChildren() {
    NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();
    IMutableIndex localIndex = indexManager.getLocalIndex();
    IMutableIndex workspaceIndex = indexManager.getWorkspaceIndex();
    //IndexInfo localInfo = indexManager.getIndexInfo(IndexManager.LOCAL_INDEX);
    //IndexInfo workspaceInfo = indexManager.getIndexInfo(IndexManager.WORKSPACE_INDEX);
    return new Object[]{new IndexNode(localIndex), new IndexNode(workspaceIndex)};
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.IMavenRepositoryRootNode#getName()
   */
  public String getName() {
    return "Local Repositories";
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.IMavenRepositoryNode#hasChildren()
   */
  public boolean hasChildren() {
    return true;
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#getImage()
   */
  public Image getImage() {
    return MavenImages.IMG_INDEXES;
  }
  
}
