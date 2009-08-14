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
import org.maven.ide.eclipse.index.IMutableIndex;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.internal.index.IndexedArtifactGroup;
import org.maven.ide.eclipse.internal.index.NexusIndexManager;

/**
 * LocalRepsoitoryNode
 *
 * @author dyocum
 */
public class IndexNode implements IMavenRepositoryNode {

//  private IndexInfo indexInfo;
  private String displayName;

  private boolean isMirror;
  /**
   * @return Returns the isMirror.
   */
  public boolean isMirror() {
    return this.isMirror;
  }

  /**
   * @param isMirror The isMirror to set.
   */
  public void setMirror(boolean isMirror) {
    this.isMirror = isMirror;
  }

  private IMutableIndex index;
  public IndexNode(IMutableIndex index){
    this.index = index;
  }
  
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.IMavenRepositoryNode#getChildren()
   */
  public Object[] getChildren() {
    
    try {
      NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();
      IndexedArtifactGroup[] rootGroups = indexManager.getRootGroups(index.getIndexName());
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
    if(IndexManager.LOCAL_INDEX.equals(index.getIndexName())){
      return index.getIndexName();// + " : " + indexInfo.getRepositoryDir().getAbsolutePath();
    } else if(IndexManager.WORKSPACE_INDEX.equals(index.getIndexName())){
      return index.getIndexName();
    } else {
      return index.getIndexName();
    }
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
