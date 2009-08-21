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
import org.maven.ide.eclipse.internal.index.IndexedArtifactGroup;
import org.maven.ide.eclipse.internal.index.NexusIndexManager;
import org.maven.ide.eclipse.util.StringUtils;

/**
 * HiddenRepositoryNode
 *
 * @author dyocum
 */
public class HiddenRepositoryNode implements IMavenRepositoryNode{

  private String name;
  private String url;
  private Object[] kids;

  public HiddenRepositoryNode(String name, String url){
    this.name = name;
    this.url = url;
  }
  public boolean isUpdating(){
    if(isEnabledIndex()){
      return ((NexusIndexManager)MavenPlugin.getDefault().getIndexManager()).isUpdatingIndex(getRepoName());  
    }
    return false;
  }
  
  public String getRepoName(){
    return name;
  }
  public String getRepoUrl(){
    return url;
  }
  public boolean isEnabledIndex(){
    return MavenPlugin.getDefault().isEnabledIndex(getRepoName());
  }
  
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#getChildren()
   */
  public Object[] getChildren() {
    if(isEnabledIndex()){
      kids = getEnabledIndexChildren();
      return kids;
    }
    return new Object[0];
  }

  /**
   * @return
   */
  private Object[] getEnabledIndexChildren() {
    try {
      NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();
      IndexedArtifactGroup[] rootGroups = indexManager.getRootGroups(getRepoName());
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
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#getImage()
   */
  public Image getImage() {
    return MavenImages.IMG_INDEX; 
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#getName()
   */
  public String getName() {
    String namePart = name;
    String mirrorStr = " [Disabled by Mirror]";
    if(isEnabledIndex()){
      mirrorStr = " [Enabled by User]";
    }
    return StringUtils.nullOrEmpty(namePart) ? url+mirrorStr : namePart+": "+url+mirrorStr+getUpdatingString();
  }

  private String getUpdatingString(){
    return isUpdating() ? " [updating]" : "";
  }
  
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#hasChildren()
   */
  public boolean hasChildren() {
    if(isEnabledIndex()){
      Object[] kids = getChildren();
      return kids != null && kids.length > 0;
    }
    return false;
  }

}
