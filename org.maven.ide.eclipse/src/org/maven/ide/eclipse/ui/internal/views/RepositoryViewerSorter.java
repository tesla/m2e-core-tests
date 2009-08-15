/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import org.maven.ide.eclipse.ui.internal.views.nodes.HiddenRepositoryNode;

/**
 * RepositoryViewerSorter
 *
 * @author dyocum
 */
public class RepositoryViewerSorter extends ViewerSorter {

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ViewerComparator#category(java.lang.Object)
   */
  public int category(Object element) {
    if(element instanceof HiddenRepositoryNode){
      return 2;
    }
    return 1;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
   */
  public int compare(Viewer viewer, Object e1, Object e2) {
    
    return super.compare(viewer, e1, e2);
    
  }

}
