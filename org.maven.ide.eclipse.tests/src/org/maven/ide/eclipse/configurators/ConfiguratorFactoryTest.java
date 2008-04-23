/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.configurators;

import java.util.Set;

import junit.framework.TestCase;

import org.maven.ide.eclipse.internal.project.ProjectConfiguratorFacotry;
import org.maven.ide.eclipse.project.AbstractProjectConfigurator;


/**
 * @author Eugene Kuleshov
 */
public class ConfiguratorFactoryTest extends TestCase {

  public void testConfiguratorFactory() throws Exception {
    Set configurators = ProjectConfiguratorFacotry.getConfigurators();
    
    AbstractProjectConfigurator[] cc = (AbstractProjectConfigurator[]) configurators.toArray(new AbstractProjectConfigurator[configurators.size()]);

    assertEquals(2, cc.length);

    assertEquals("org.maven.ide.eclipse.configurator.jdt", cc[0].getId());
    assertEquals("JDT", cc[0].getName());
    assertEquals(10, cc[0].getPriority());
    
    assertEquals("org.maven.ide.eclipse.configurator.test", cc[1].getId());
    assertEquals("TEST", cc[1].getName());
    assertEquals(1000, cc[1].getPriority());
  }

}
