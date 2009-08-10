/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views.nodes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;

import org.maven.ide.eclipse.MavenImages;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.index.IndexInfo;
import org.maven.ide.eclipse.index.IndexedArtifactGroup;

/**
 * LocalRepsoitoryNode
 *
 * @author dyocum
 */
public class LocalRepositoryNode implements IMavenRepositoryNode {

  private IndexInfo indexInfo;
  private String displayName;

  public LocalRepositoryNode(IndexInfo info){
    this.indexInfo = info;
  }
  
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.IMavenRepositoryNode#getChildren()
   */
  public Object[] getChildren() {
    String indexName = indexInfo.getIndexName();
    try {
      IndexedArtifactGroup[] rootGroups = MavenPlugin.getDefault().getIndexManager().getRootGroups(indexName);
      if(rootGroups == null){
        return new Object[0];
      }
      IndexedArtifactGroupNode[] children = new IndexedArtifactGroupNode[rootGroups.length];
      for(int i=0;i<rootGroups.length;i++){
        children[i] = new IndexedArtifactGroupNode(rootGroups[i]);
      }
      return children;
    } catch(CoreException ex) {
      MavenLogger.log(ex);
      return new Object[0];
    }
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.IMavenRepositoryNode#getName()
   */
  public String getName() {
    if(IndexInfo.Type.LOCAL.equals(indexInfo.getType())){
      return indexInfo.getIndexName() + " : " + indexInfo.getRepositoryDir().getAbsolutePath();
    } else if(IndexInfo.Type.WORKSPACE.equals(indexInfo.getType())){
      return indexInfo.getIndexName();
    }
    return displayName;
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.IMavenRepositoryNode#hasChildren()
   */
  public boolean hasChildren() {
    return getChildren().length > 0;
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#getImage()
   */
  public Image getImage() {
    return MavenImages.IMG_INDEX; 
  }

}
