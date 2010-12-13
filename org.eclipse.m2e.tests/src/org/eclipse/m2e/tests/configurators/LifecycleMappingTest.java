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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.internal.lifecycle.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.project.MissingLifecycleMapping;
import org.eclipse.m2e.core.internal.project.MojoExecutionProjectConfigurator;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.CustomizableLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.LifecycleMappingMetadata;
import org.eclipse.m2e.core.project.configurator.MavenResourcesProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.PluginExecutionAction;
import org.eclipse.m2e.core.project.configurator.PluginExecutionMetadata;
import org.eclipse.m2e.jdt.internal.JarLifecycleMapping;
import org.eclipse.m2e.jdt.internal.JavaProjectConfigurator;
import org.eclipse.m2e.tests.common.AbstractLifecycleMappingTest;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


@SuppressWarnings("restriction")
public class LifecycleMappingTest extends AbstractLifecycleMappingTest {
  public void testGetLifecycleMappingMetadata() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
        "testGetLifecycleMappingMetadata/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    List<LifecycleMappingMetadata> metadata = LifecycleMappingFactory.getLifecycleMappingMetadata(facade
        .getMavenProject());
    assertNotNull(metadata);
    assertEquals(1, metadata.size());
    assertEquals("testLifecycleMappingMetadata", metadata.get(0).getGroupId());
    assertEquals("testLifecycleMappingMetadata1", metadata.get(0).getArtifactId());
    assertEquals("0.0.1", metadata.get(0).getVersion());

    List<PluginExecutionMetadata> pluginExecutions = metadata.get(0).getPluginExecutions();
    assertEquals(3, pluginExecutions.size());
    Set<String> goals = new LinkedHashSet<String>();

    goals.add("compile");
    goals.add("testCompile");
    assertPluginExecutionMetadata("org.apache.maven.plugins", "maven-compiler-plugin", "[2.0,)", goals,
        PluginExecutionAction.CONFIGURATOR, "org.eclipse.m2e.jdt.javaConfigurator", pluginExecutions.get(0));

    goals = new LinkedHashSet<String>();
    goals.add("jar");
    assertPluginExecutionMetadata("org.apache.maven.plugins", "maven-jar-plugin", "[2.0,)", goals,
        PluginExecutionAction.IGNORE, null, pluginExecutions.get(1));

    goals = new LinkedHashSet<String>();
    goals.add("resources");
    assertPluginExecutionMetadata("org.apache.maven.plugins", "maven-resources-plugin", "[2.0,)", goals,
        PluginExecutionAction.EXECUTE, null, pluginExecutions.get(2));
  }

  public void testGetLifecycleMappingMetadataOverride() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
        "testGetLifecycleMappingMetadataOverride/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    List<LifecycleMappingMetadata> metadata = LifecycleMappingFactory.getLifecycleMappingMetadata(facade
        .getMavenProject());
    assertNotNull(metadata);
    assertEquals(1, metadata.size());
    assertEquals("testLifecycleMappingMetadata", metadata.get(0).getGroupId());
    assertEquals("testLifecycleMappingMetadata2", metadata.get(0).getArtifactId());
    assertEquals("0.0.2", metadata.get(0).getVersion());

    List<PluginExecutionMetadata> pluginExecutions = metadata.get(0).getPluginExecutions();
    assertEquals(2, pluginExecutions.size());
    Set<String> goals = new LinkedHashSet<String>();

    goals.add("compile");
    goals.add("testCompile");
    assertPluginExecutionMetadata("org.apache.maven.plugins", "maven-compiler-plugin", "[3.0,)", goals,
        PluginExecutionAction.CONFIGURATOR, "org.eclipse.m2e.jdt.javaConfigurator", pluginExecutions.get(0));

    goals = new LinkedHashSet<String>();
    goals.add("jar");
    assertPluginExecutionMetadata("org.apache.maven.plugins", "maven-jar-plugin", "[3.0,)", goals,
        PluginExecutionAction.IGNORE, null, pluginExecutions.get(1));
  }

  public void testGetLifecycleMappingMetadataMerge() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
        "testGetLifecycleMappingMetadataMerge/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    List<LifecycleMappingMetadata> metadata = LifecycleMappingFactory.getLifecycleMappingMetadata(facade
        .getMavenProject());
    assertNotNull(metadata);
    assertEquals(2, metadata.size());
    assertEquals("testLifecycleMappingMetadata", metadata.get(0).getGroupId());
    assertEquals("testLifecycleMappingMetadata2", metadata.get(0).getArtifactId());
    assertEquals("0.0.2", metadata.get(0).getVersion());
    assertEquals("testLifecycleMappingMetadata", metadata.get(1).getGroupId());
    assertEquals("testLifecycleMappingMetadata1", metadata.get(1).getArtifactId());
    assertEquals("0.0.1", metadata.get(1).getVersion());
  }

  private void assertPluginExecutionMetadata(String groupId, String artifactId, String versionRange, Set<String> goals,
      PluginExecutionAction action, String configuratorId, PluginExecutionMetadata metadata) {
    assertEquals(groupId, metadata.getFilter().getGroupId());
    assertEquals(artifactId, metadata.getFilter().getArtifactId());
    assertEquals(versionRange, metadata.getFilter().getVersionRange());
    assertEquals(goals, metadata.getFilter().getGoals());
    assertEquals(action, metadata.getAction());
    if(configuratorId != null) {
      assertEquals(configuratorId, metadata.getConfiguration().getChild("id").getValue());
    }
  }

  public void testDefaultJarLifecycleMapping() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "default-jar/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade, monitor);

    assertTrue(lifecycleMapping instanceof JarLifecycleMapping);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertEquals(configurators.toString(), 2, configurators.size());
    assertTrue(configurators.get(0) instanceof MavenResourcesProjectConfigurator);
    assertTrue(configurators.get(1) instanceof JavaProjectConfigurator);

    List<MojoExecution> notCoveredMojoExecutions = lifecycleMapping.getNotCoveredMojoExecutions(facade, monitor);
    assertEquals(notCoveredMojoExecutions.toString(), 0, lifecycleMapping.getNotCoveredMojoExecutions(facade, monitor)
        .size());
  }

  public void testCustomizableMapping() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "customizable/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade, monitor);

    assertTrue(lifecycleMapping instanceof CustomizableLifecycleMapping);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertEquals(configurators.toString(), 2, configurators.size());
    assertTrue(configurators.get(1) instanceof MojoExecutionProjectConfigurator);

    List<MojoExecution> notCoveredMojoExecutions = lifecycleMapping.getNotCoveredMojoExecutions(facade, monitor);
    assertEquals(notCoveredMojoExecutions.toString(), 0, lifecycleMapping.getNotCoveredMojoExecutions(facade, monitor)
        .size());
  }

  public void testCustomizableMappingNotComplete() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "customizableNotComplete/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 2, errorMarkers.size());
    String expectedErrorMessage = "Mojo execution not covered by lifecycle configuration: org.apache.maven.plugins:maven-resources-plugin:2.4.1:resources {execution: default-resources} (maven lifecycle phase: process-resources)";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(0));

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade, monitor);
    assertTrue(lifecycleMapping instanceof CustomizableLifecycleMapping);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertEquals(configurators.toString(), 1, configurators.size());
    assertTrue(configurators.get(0) instanceof JavaProjectConfigurator);

    List<MojoExecution> notCoveredMojoExecutions = lifecycleMapping.getNotCoveredMojoExecutions(facade, monitor);
    assertEquals(notCoveredMojoExecutions.toString(), 2, notCoveredMojoExecutions.size());
    assertEquals("org.apache.maven.plugins:maven-resources-plugin:2.4.1:resources {execution: default-resources}", notCoveredMojoExecutions
        .get(0).toString());
    assertEquals("org.apache.maven.plugins:maven-resources-plugin:2.4.1:testResources {execution: default-testResources}", notCoveredMojoExecutions
        .get(1).toString());

    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();

    errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(0));
  }

  public void testMissingMapping() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "missing/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    String expectedErrorMessage = "Unknown or missing lifecycle mapping with id=\"MISSING\" (project packaging type=\"jar\")";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, project);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade, monitor);
    assertTrue(lifecycleMapping instanceof MissingLifecycleMapping);
    assertEquals("unknown-or-missing", ((MissingLifecycleMapping) lifecycleMapping).getMissingMappingId());
    assertEquals(0, lifecycleMapping.getNotCoveredMojoExecutions(facade, monitor).size());

    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 1, errorMarkers.size());
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, project);
  }

  public void testUnknownPackagingType() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "unknownPackagingType/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    String expectedErrorMessage = "Unknown or missing lifecycle mapping with id=\"MISSING\" (project packaging type=\"war\")";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, project);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade, monitor);
    assertTrue(lifecycleMapping instanceof MissingLifecycleMapping);
    assertEquals(0, lifecycleMapping.getNotCoveredMojoExecutions(facade, monitor).size());

    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 1, errorMarkers.size());
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, project);
  }
}
