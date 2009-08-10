/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views.nodes;

import org.eclipse.swt.graphics.Image;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.index.IndexInfo;
import org.maven.ide.eclipse.index.IndexManager;

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
    IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
    IndexInfo localInfo = indexManager.getIndexInfo("local");
    IndexInfo workspaceInfo = indexManager.getIndexInfo("workspace");
    return new Object[]{new LocalRepositoryNode(localInfo), new LocalRepositoryNode(workspaceInfo)};
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
    // TODO Auto-generated method getImage
    return null;
  }
}
