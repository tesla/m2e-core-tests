/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * @author Eugene Kuleshov
 */
@SuppressWarnings("unchecked")
public class MavenAdapterFactory implements IAdapterFactory {

  private static final Class[] ADAPTER_TYPES = new Class[] { IActionFilter.class };

  public Class[] getAdapterList() {
    return ADAPTER_TYPES;
  }

  public Object getAdapter(final Object adaptable, Class adapterType) {
    return new IActionFilter() {
        public boolean testAttribute(Object target, String name, String value) {
          IWorkbenchAdapter wa = (IWorkbenchAdapter) ((IAdaptable) adaptable).getAdapter(IWorkbenchAdapter.class);
          if(wa!=null) {
            if("label".equals(name)) {
              String label = wa.getLabel(adaptable);
              return value.equals(label);
            }
          }
            
          return false;
        }
      };
  }

}
