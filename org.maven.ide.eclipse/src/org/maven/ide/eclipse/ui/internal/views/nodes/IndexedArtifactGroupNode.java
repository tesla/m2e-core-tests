/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views.nodes;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.internal.index.IndexedArtifactGroup;
import org.maven.ide.eclipse.internal.index.NexusIndexManager;

/**
 * IndexedArtifactGroupNode
 *
 * @author dyocum
 */
public class IndexedArtifactGroupNode implements IMavenRepositoryNode {

  private IndexedArtifactGroup indexedArtifactGroup;
  private Object[] kids = null;
  public IndexedArtifactGroupNode(IndexedArtifactGroup group){
    this.indexedArtifactGroup = group;
  }
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.IMavenRepositoryNode#getChildren()
   */
  public Object[] getChildren() {
    NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();
    
    IndexedArtifactGroup resolvedGroup = indexManager.resolveGroup(indexedArtifactGroup);
    //IndexedArtifactGroup resolvedGroup = indexedArtifactGroup;
    ArrayList<Object> results = new ArrayList<Object>();
    Collection<IndexedArtifactGroup> groups = resolvedGroup.getNodes().values();
    for(IndexedArtifactGroup group : groups){
     IndexedArtifactGroupNode node = new IndexedArtifactGroupNode(group); 
     results.add(node);
    }
    
    Collection<IndexedArtifact> artifacts = resolvedGroup.getFiles().values(); // IndexedArtifact
    for(IndexedArtifact artifact : artifacts){
      IndexedArtifactNode artifactNode = new IndexedArtifactNode(artifact);
      results.add(artifactNode);
    }
    kids = results.toArray(new Object[results.size()]);
    return kids;
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.IMavenRepositoryNode#getName()
   */
  public String getName() {
    String prefix = indexedArtifactGroup.getPrefix();
    int n = prefix.lastIndexOf('.');
    return n < 0 ? prefix : prefix.substring(n + 1);
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.IMavenRepositoryNode#hasChildren()
   */
  public boolean hasChildren() {
//    if(kids == null){
//      kids = getChildren();
//    }
//    return kids != null && kids.length > 0;
    return true;
  }
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#getImage()
   */
  public Image getImage() {
    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
  }

}
