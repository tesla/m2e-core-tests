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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;

import org.apache.maven.artifact.Artifact;

import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingResult;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
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

    LifecycleMappingResult mappingResult = calculateLifecycleMapping((MavenProjectFacade) facade);
    MojoExecutionKey executionKey = new MojoExecutionKey("org.eclipse.m2e.test.lifecyclemapping",
        "test-embeddedmapping-plugin", "1.0.0", "test-goal-1", "compile", "mapping-without-plugin-gav");
    List<IPluginExecutionMetadata> executionMapping = mappingResult.getMojoExecutionMapping().get(executionKey);
    assertEquals(1, executionMapping.size());
    LifecycleMappingMetadataSource metadata = ((PluginExecutionMetadata) executionMapping.get(0)).getSource();
    assertNotNull(metadata);
    Artifact artifact = (Artifact) metadata.getSource();
    assertEquals(executionKey.getGroupId(), artifact.getGroupId());
    assertEquals(executionKey.getArtifactId(), artifact.getArtifactId());
    assertEquals(executionKey.getVersion(), artifact.getVersion());
  }

  public void test422135_mavenPluginSourceWorkspaceResolution() throws Exception {
    // the point of this test is to assert that workspace projects are not used as maven plugin metadata source
    // 'plugin' project shadows test-embeddedmapping-plugin plugin available from repositories
    // 'project' uses the plugin and gets an error marker if workspace project is used as metadats source
    importMavenProject(
        "projects/lifecyclemapping/mavenpluginsource/422135_mavenPluginSourceWorkspaceResolution/plugin", "pom.xml");
    IMavenProjectFacade project = importMavenProject(
        "projects/lifecyclemapping/mavenpluginsource/422135_mavenPluginSourceWorkspaceResolution/project", "pom.xml");
    assertNoErrors(project.getProject());
  }
}
