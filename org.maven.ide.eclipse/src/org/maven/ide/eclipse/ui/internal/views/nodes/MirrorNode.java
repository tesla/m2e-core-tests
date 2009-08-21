/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views.nodes;

import org.eclipse.swt.graphics.Image;

import org.apache.maven.settings.Mirror;

/**
 * MirrorNode
 *
 * @author dyocum
 */
public class MirrorNode implements IMavenRepositoryNode {

  private Mirror mirror;

  public MirrorNode(Mirror mirror){
    this.mirror = mirror;
  }
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#getChildren()
   */
  public Object[] getChildren() {
    return new Object[0];
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#getImage()
   */
  public Image getImage() {
    return null;
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#getName()
   */
  public String getName() {
    return mirror.getUrl() + " (mirror)";
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#hasChildren()
   */
  public boolean hasChildren() {
    Object[] kids = getChildren();
    return kids != null && kids.length > 0;
  }
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#isUpdating()
   */
  public boolean isUpdating() {
    // TODO Auto-generated method isUpdating
    return false;
  }

}
