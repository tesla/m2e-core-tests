/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.composites;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * @author Eugene Kuleshov
 */
public class ListEditorContentProvider<T> implements IStructuredContentProvider {

  public static final Object[] EMPTY = new Object[0];
  
  @SuppressWarnings("unchecked")
  public Object[] getElements(Object input) {
    if(input instanceof EList) {
      EList<T> list = (EList<T>) input;
      return list.toArray();
    }
    return EMPTY;
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
  }
  
  public void dispose() {
  }

}
