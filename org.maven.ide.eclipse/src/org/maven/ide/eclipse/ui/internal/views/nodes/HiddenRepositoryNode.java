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
import org.maven.ide.eclipse.util.StringUtils;

/**
 * HiddenRepositoryNode
 *
 * @author dyocum
 */
public class HiddenRepositoryNode implements IMavenRepositoryNode{

  private String name;
  private String url;

  public HiddenRepositoryNode(String name, String url){
    this.name = name;
    this.url = url;
  }
  
  public String getRepoName(){
    return name;
  }
  public String getRepoUrl(){
    return url;
  }
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#getChildren()
   */
  public Object[] getChildren() {
//    ArrayList<Object> repoList = new ArrayList<Object>();
//    try{
//       List<ArtifactRepository> repos = getRemoteRepositories();
//       if(repos != null){
//        for(ArtifactRepository repo : repos){
//          NexusIndex index = new NexusIndex(((NexusIndexManager)MavenPlugin.getDefault().getIndexManager()), repo.getId(), repo.getUrl());
//          IndexNode node = new IndexNode(index);
//          repoList.add(node);
//          
//        }
//
//      }
//    } catch(Exception e){
//      MavenLogger.log("Unable to load remote repositories", e);
//    }
//    return repoList.toArray(new Object[repoList.size()]);
    return null;
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
    return StringUtils.nullOrEmpty(namePart) ? url+mirrorStr : namePart+": "+url+mirrorStr;
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#hasChildren()
   */
  public boolean hasChildren() {
    return false;
  }

}
