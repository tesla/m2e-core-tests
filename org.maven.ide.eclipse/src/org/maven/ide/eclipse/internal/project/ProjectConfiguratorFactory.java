/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.maven.ide.eclipse.internal.ExtensionReader;
import org.maven.ide.eclipse.project.AbstractProjectConfigurator;

/**
 * ProjectConfigurator Facotry
 *
 * @author Eugene Kuleshov
 */
public class ProjectConfiguratorFactory {

  private static Set configurators;

  public static void addProjectConfigurator(AbstractProjectConfigurator configurator) {
    configurators.add(configurator);
  }
  
  public static synchronized Set getConfigurators() {
    if(configurators==null) {
      configurators = new TreeSet(new ProjectConfiguratorComparator());
      ExtensionReader.readProjectConfiguratorExtensions();
    }
    return Collections.unmodifiableSet(configurators);
  }
  
  /**
   * ProjectConfigurator comparator
   */
  public static class ProjectConfiguratorComparator implements Comparator {
    
    public int compare(Object o1, Object o2) {
      AbstractProjectConfigurator c1 = (AbstractProjectConfigurator) o1;
      AbstractProjectConfigurator c2 = (AbstractProjectConfigurator) o2;
      return c1.getPriority() - c2.getPriority();
    }
    
  }
  
}
