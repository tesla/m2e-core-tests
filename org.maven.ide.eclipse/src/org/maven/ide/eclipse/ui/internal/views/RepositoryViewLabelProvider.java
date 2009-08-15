/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.maven.ide.eclipse.ui.internal.views.nodes.HiddenRepositoryNode;
import org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode;

/**
 * RepositoryViewLabelProvider
 *
 * @author dyocum
 */
public class RepositoryViewLabelProvider extends LabelProvider implements IColorProvider {

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
  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
   */
  public Color getBackground(Object element) {
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
   */
  public Color getForeground(Object element) {
    if(element instanceof HiddenRepositoryNode){
      return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
    } 
    return Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
  }

}
