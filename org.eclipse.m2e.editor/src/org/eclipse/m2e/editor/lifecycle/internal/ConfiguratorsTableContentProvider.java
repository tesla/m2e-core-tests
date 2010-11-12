/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.lifecycle.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.editor.lifecycle.ILifecycleMappingEditorContribution;
import org.eclipse.swt.graphics.Image;

public class ConfiguratorsTableContentProvider implements IStructuredContentProvider, ITableLabelProvider {
  ILifecycleMappingEditorContribution contributor;
  
  public void dispose() { }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    contributor = (ILifecycleMappingEditorContribution)newInput;
  }

  public Image getColumnImage(Object arg0, int arg1) {
    return null;
  }

  public String getColumnText(Object object, int column) {
    return ((AbstractProjectConfigurator)object).getName();
  }

  public void addListener(ILabelProviderListener arg0) { }

  public boolean isLabelProperty(Object arg0, String arg1) {
    return false;
  }

  public void removeListener(ILabelProviderListener arg0) { }
  
  public Object[] getElements(Object parent) {
    try {
      return contributor.getProjectConfigurators().toArray();
    } catch(CoreException e) {
      MavenLogger.log(e);
      return null;
    }
  }

}
