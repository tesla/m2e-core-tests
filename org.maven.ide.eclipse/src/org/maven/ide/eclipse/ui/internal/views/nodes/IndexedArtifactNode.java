/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views.nodes;

import java.util.ArrayList;
import java.util.Set;

import org.eclipse.swt.graphics.Image;

import org.maven.ide.eclipse.MavenImages;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;

/**
 * IndexedArtifactNode
 *
 * @author dyocum
 */
public class IndexedArtifactNode implements IMavenRepositoryNode {

  private IndexedArtifact artifact;

  public IndexedArtifactNode(IndexedArtifact artifact){
    this.artifact = artifact;
  }
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.IMavenRepositoryNode#getChildren()
   */
  public Object[] getChildren() {
    Set<IndexedArtifactFile> files = artifact.files;
    if(files == null){
      return new Object[0];
    }
    ArrayList<Object> fileList = new ArrayList<Object>();
    for(IndexedArtifactFile iaf : files){
      fileList.add(new IndexedArtifactFileNode(iaf));
    }
    return fileList.toArray(new IndexedArtifactFileNode[fileList.size()]);
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.IMavenRepositoryNode#getName()
   */
  public String getName() {
    // return a.group + ":" + a.artifact;
    return artifact.artifact + " - " + artifact.packaging;
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.IMavenRepositoryNode#hasChildren()
   */
  public boolean hasChildren() {
    return getChildren() != null && getChildren().length > 0;
  }
  
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#getImage()
   */
  public Image getImage() {
    return MavenImages.IMG_JAR;
  }

}
