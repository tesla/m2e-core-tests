/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.maven.ide.eclipse.internal.ExtensionReader;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;

/**
 * ProjectConfigurator Facotry
 *
 * @author Eugene Kuleshov
 */
public class ProjectConfiguratorFactory {

  private static Set<AbstractProjectConfigurator> configurators;

  public static void addProjectConfigurator(AbstractProjectConfigurator configurator) {
    configurators.add(configurator);
  }
  
  public static synchronized Set<AbstractProjectConfigurator> getConfigurators() {
    if(configurators == null) {
      configurators = new TreeSet<AbstractProjectConfigurator>(new ProjectConfiguratorComparator());
      configurators.addAll(ExtensionReader.readProjectConfiguratorExtensions());
    }
    return Collections.unmodifiableSet(configurators);
  }
  
  /**
   * ProjectConfigurator comparator
   */
  static class ProjectConfiguratorComparator implements Comparator<AbstractProjectConfigurator>, Serializable {
    private static final long serialVersionUID = 1L;

    public int compare(AbstractProjectConfigurator c1, AbstractProjectConfigurator c2) {
      int res = c1.getPriority() - c2.getPriority();
      return res==0 ? c1.getId().compareTo(c2.getId()) : res;
    }
  }
  
}
