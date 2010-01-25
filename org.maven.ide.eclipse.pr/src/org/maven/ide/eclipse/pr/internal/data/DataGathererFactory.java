/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr.internal.data;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.pr.IDataGatherer;


/**
 * Provides access to installed data gatherers.
 */
public class DataGathererFactory {

  private static final String EXTENSION_POINT_ID = "org.maven.ide.eclipse.pr.dataGatherers";

  private static final String ELEMENT_DATA_GATHERER = "gatherer";

  private static final String ATTRIBUTE_CLASS = "class";

  public static List<IDataGatherer> getDataGatherers() {
    List<IDataGatherer> dataGatherers = new ArrayList<IDataGatherer>();

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint dataGatherersExtensionPoint = registry.getExtensionPoint(EXTENSION_POINT_ID);
    if(dataGatherersExtensionPoint != null) {
      IExtension[] dataGatherersExtensions = dataGatherersExtensionPoint.getExtensions();
      for(IExtension extension : dataGatherersExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(ELEMENT_DATA_GATHERER.equals(element.getName())) {
            try {
              dataGatherers.add((IDataGatherer) element.createExecutableExtension(ATTRIBUTE_CLASS));
            } catch(CoreException ex) {
              MavenLogger.log(ex);
            }
          }
        }
      }
    }

    return dataGatherers;
  }

}
