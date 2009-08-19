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
import org.maven.ide.eclipse.util.StringUtils;

/**
 * LocalRepsoitoryNode
 *
 * @author dyocum
 */
public class IndexNode implements IMavenRepositoryNode {

//  private IndexInfo indexInfo;
  private String displayName;

  private boolean isMirror;

  private boolean updating;
  /**
   * @return Returns the isMirror.
   */
  public boolean isMirror() {
    return this.isMirror;
  }

  public void setIsUpdating(boolean updating){
    this.updating = updating;
  }
  
  public boolean isUpdating(){
    return ((NexusIndexManager)MavenPlugin.getDefault().getIndexManager()).isUpdatingIndex(getIndexName());
  }
  
  public boolean isWorkspace(){
    return IndexManager.WORKSPACE_INDEX.equals(index.getIndexName());
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
  
  public String getIndexName(){
    return index.getIndexName();
  }
  public String getRepositoryUrl(){
    return index.getRepositoryUrl();
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
  
  private String getUpdatingStatus(){
    return isUpdating() ? " [updating]" : "";
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.IMavenRepositoryNode#getName()
   */
  public String getName() {
    String name = "";
    if(IndexManager.LOCAL_INDEX.equals(index.getIndexName())){
      name =  index.getIndexName();
      try{
        String basedir = MavenPlugin.getDefault().getMaven().getLocalRepository().getBasedir();
        if(!StringUtils.nullOrEmpty(basedir)){
          name = name+": "+basedir;
        }
      } catch(CoreException ce){
        //do nothing, just leave name
      }
    } else if(IndexManager.WORKSPACE_INDEX.equals(index.getIndexName())){
      name = index.getIndexName();
    } else {
      name = index.getIndexName() +": "+ index.getRepositoryUrl();
    }
    return name + getUpdatingStatus();
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
