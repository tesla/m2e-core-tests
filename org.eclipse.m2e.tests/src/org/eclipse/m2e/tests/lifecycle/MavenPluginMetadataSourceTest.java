/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.lifecycle;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.tests.common.AbstractLifecycleMappingTest;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


public class MavenPluginMetadataSourceTest extends AbstractLifecycleMappingTest {

  public void testBasic() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/mavenpluginsource/basic", "pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();

    WorkspaceHelpers
        .assertMarker(
            "org.eclipse.m2e.core.maven2Problem.lifecycleMapping",
            IMarker.SEVERITY_ERROR,
            "Plugin execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-1 (execution: not-covered, phase: compile)",
            null, "pom.xml", project);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade);
    assertNotNull(lifecycleMapping);
  }
}
