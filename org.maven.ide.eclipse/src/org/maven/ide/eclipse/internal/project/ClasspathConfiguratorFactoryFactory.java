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
import org.maven.ide.eclipse.project.configurator.AbstractClasspathConfiguratorFactory;

/**
 * ClasspathConfiguratorFactoryFactory
 *
 * @author igor
 */
public class ClasspathConfiguratorFactoryFactory {

  private static Set factories;

  public static synchronized Set getFactories() {
    if(factories==null) {
      Set tmp = new TreeSet(new FactoryComparator());
      tmp.addAll(ExtensionReader.readClasspathConfiguratorFactoryExtensions());
      factories = Collections.unmodifiableSet(tmp);
    }
    return factories;
  }

  static class FactoryComparator implements Comparator {

    public int compare(Object o1, Object o2) {
      AbstractClasspathConfiguratorFactory c1 = (AbstractClasspathConfiguratorFactory) o1;
      AbstractClasspathConfiguratorFactory c2 = (AbstractClasspathConfiguratorFactory) o2;

      return c1.getPriority() - c2.getPriority();
    }
    
  }
}
