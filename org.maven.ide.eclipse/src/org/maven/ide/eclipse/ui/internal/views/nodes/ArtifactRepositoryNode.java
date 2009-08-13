/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views.nodes;

import org.eclipse.swt.graphics.Image;

import org.apache.maven.artifact.repository.ArtifactRepository;

import org.maven.ide.eclipse.MavenImages;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.internal.index.NexusIndex;
import org.maven.ide.eclipse.internal.index.NexusIndexManager;

/**
 * ArtifactRepositoryNode
 *
 * @author dyocum
 */
public class ArtifactRepositoryNode implements IMavenRepositoryNode {

  /**
   * 
   */
  private static final String MIRROR = " (mirror)";
  private ArtifactRepository repository;
  private boolean isMirror;

  public ArtifactRepositoryNode(ArtifactRepository repository){
    this.repository = repository;
  }
  
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#getChildren()
   */
  public Object[] getChildren() {
    NexusIndex index = new NexusIndex(((NexusIndexManager)MavenPlugin.getDefault().getIndexManager()), repository.getUrl());
    IndexNode node = new IndexNode(index);
    return new Object[]{node};
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
    return repository.getUrl() + getMirrorString();
  }

  private String getMirrorString(){
    return ( isMirror() ? MIRROR : "");
  }
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#hasChildren()
   */
  public boolean hasChildren() {
    return true;
  }

  public boolean isMirror(){
    return isMirror;
  }
  /**
   * @param b
   */
  public void setIsMirror(boolean mirror) {
    this.isMirror = mirror;
  }

}
