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


package org.eclipse.m2e.tests.configurators;

import java.util.List;

import org.eclipse.m2e.core.internal.project.CustomizableLifecycleMapping;
import org.eclipse.m2e.core.internal.project.GenericLifecycleMapping;
import org.eclipse.m2e.core.internal.project.MissingLifecycleMapping;
import org.eclipse.m2e.core.internal.project.MojoExecutionProjectConfigurator;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.tests.common.AbstractLifecycleMappingTest;

public class LifecycleMappingTest extends AbstractLifecycleMappingTest {
  public void testGenericMapping() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "generic/pom.xml");

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade, monitor);

    assertTrue( lifecycleMapping instanceof GenericLifecycleMapping );
  }

  public void testCustomizableMapping() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "customizable/pom.xml");

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade, monitor);

    assertTrue(lifecycleMapping instanceof CustomizableLifecycleMapping);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertEquals(2, configurators.size());
    assertTrue(configurators.get(1) instanceof MojoExecutionProjectConfigurator);
  }

  public void testMissingMapping() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "missing/pom.xml");

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade, monitor);

    assertTrue(lifecycleMapping instanceof MissingLifecycleMapping);
    assertEquals("unknown-or-missing", ((MissingLifecycleMapping) lifecycleMapping).getMissingMappingId());
  }
}
