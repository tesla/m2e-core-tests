/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode;

/**
 * RepositoryViewLabelProvider
 *
 * @author dyocum
 */
public class RepositoryViewLabelProvider extends LabelProvider {

  public String getText(Object obj) {
    if(obj instanceof IMavenRepositoryNode){
      return ((IMavenRepositoryNode)obj).getName();
    }
    return obj.toString();
  }

  public Image getImage(Object obj) {
    if(obj instanceof IMavenRepositoryNode){
      return ((IMavenRepositoryNode)obj).getImage();
    }
    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
  }

}
