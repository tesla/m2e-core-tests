/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.util;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.maven.ide.eclipse.core.IMavenConstants;


/**
 * Helper methods to deal with workspace resources passed as navigator selection to actions and wizards.
 */
public class SelectionUtil {

  public static final int UNSUPPORTED = 0;

  public static final int PROJECT_WITH_NATURE = 1;

  public static final int PROJECT_WITHOUT_NATURE = 2;

  public static final int POM_FILE = 4;

  public static final int JAR_FILE = 8;

  /** Checks which type the given selection belongs to. */
  public static int getSelectionType(IStructuredSelection selection) {
    int type = UNSUPPORTED;
    for(Iterator<?> it = selection.iterator(); it.hasNext();) {
      int elementType = getElementType(it.next());
      if(elementType == UNSUPPORTED) {
        return UNSUPPORTED;
      }
      type |= elementType;
    }
    return type;
  }

  /** Checks which type the given element belongs to. */
  public static int getElementType(Object element) {
    IProject project = getType(element, IProject.class);
    if(project != null) {
      try {
        if(project.hasNature(IMavenConstants.NATURE_ID)) {
          return PROJECT_WITH_NATURE;
        }
        return PROJECT_WITHOUT_NATURE;
      } catch(CoreException e) {
        // ignored
      }
    }

    IFile file = getType(element, IFile.class);
    if(file != null) {
      if(IMavenConstants.POM_FILE_NAME.equals(file.getFullPath().lastSegment())) {
        return POM_FILE;
      }
    }

    IPackageFragmentRoot fragment = getType(element, IPackageFragmentRoot.class);
    if(fragment != null) {
      if(/*fragment.isExternal() && */fragment.isArchive()) {
        return JAR_FILE;
      }
    }

    return UNSUPPORTED;
  }

  /**
   * Checks if the object belongs to a given type and returns it or a suitable adapter.
   */
  @SuppressWarnings("unchecked")
  public static <T> T getType(Object element, Class<T> type) {
    if(type.isInstance(element)) {
      return (T) element;
    } else if(element instanceof IAdaptable) {
      return (T) ((IAdaptable) element).getAdapter(type);
    }
    return (T) Platform.getAdapterManager().getAdapter(element, type);
  }
}
