/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.lifecycle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.lifecyclemapping.AnnotationMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.DefaultLifecycleMapping;
import org.eclipse.m2e.core.internal.lifecyclemapping.InvalidLifecycleMapping;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingResult;
import org.eclipse.m2e.core.internal.lifecyclemapping.MappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.SimpleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.internal.preferences.MavenConfigurationImpl;
import org.eclipse.m2e.core.internal.preferences.ProblemSeverity;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractCustomizableLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.NoopLifecycleMapping;
import org.eclipse.m2e.jdt.internal.JarLifecycleMapping;
import org.eclipse.m2e.jdt.internal.JavaProjectConfigurator;
import org.eclipse.m2e.tests.common.AbstractLifecycleMappingTest;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;
import org.eclipse.m2e.tests.configurators.TestBuildParticipant;
import org.eclipse.m2e.tests.configurators.TestLifecycleMapping;
import org.eclipse.m2e.tests.configurators.TestProjectConfigurator;
import org.eclipse.m2e.tests.configurators.TestProjectConfigurator2;


public class LifecycleMappingTest extends AbstractLifecycleMappingTest {
  @Test
  public void testLifecycleMappingSpecifiedInMetadata() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
        "testLifecycleMappingSpecifiedInMetadata/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade);
    assertNotNull(lifecycleMapping);
    assertTrue(lifecycleMapping.getClass().getCanonicalName(), lifecycleMapping instanceof JarLifecycleMapping);
  }

  @Test
  public void testMissingLifecycleMappingMetadata() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
        "testMissingLifecycleMappingMetadata/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);

    IProject project = facade.getProject();

    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 1, errorMarkers.size());

    String expectedErrorMessage = "Could not resolve artifact testLifecycleMappingMetadata:missing:xml:lifecycle-mapping-metadata:0.0.1";
    IMarker marker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID,
        expectedErrorMessage, null /*lineNumber*/, project);
    WorkspaceHelpers.assertMarkerLocation(new SourceLocation(17, 11, 25), marker);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade);
    assertTrue(lifecycleMapping instanceof InvalidLifecycleMapping);
  }

  private LifecycleMappingMetadata getLifecycleMappingMetadata(
      LifecycleMappingMetadataSource lifecycleMappingMetadataSource, String packagingType) {
    for(LifecycleMappingMetadata lifecycleMappingMetadata : lifecycleMappingMetadataSource.getLifecycleMappings()) {
      if(packagingType.equals(lifecycleMappingMetadata.getPackagingType())) {
        return lifecycleMappingMetadata;
      }
    }
    return null;
  }

  @Test
  public void testUseDefaultLifecycleMappingMetadataSource() throws Exception {
    LifecycleMappingFactory.setUseDefaultLifecycleMappingMetadataSource(false);
    assertNull(LifecycleMappingFactory.getDefaultLifecycleMappingMetadataSource());

    // By default, the use of default lifecycle metadata is disabled for unit tests
    LifecycleMappingFactory.setUseDefaultLifecycleMappingMetadataSource(true);

    assertNotNull(LifecycleMappingFactory.getDefaultLifecycleMappingMetadataSource());
  }

  @Test
  public void testGetLifecycleMappingMetadata() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
        "testGetLifecycleMappingMetadata/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    List<MappingMetadataSource> metadataSources = LifecycleMappingFactory
        .getPomMappingMetadataSources(facade.getMavenProject(monitor), monitor);
    assertNotNull(metadataSources);
    assertEquals(1, metadataSources.size());

    assertEquals(SimpleMappingMetadataSource.class, metadataSources.get(0).getClass());
    SimpleMappingMetadataSource metadataSource = (SimpleMappingMetadataSource) metadataSources.get(0);
    assertEquals(1, metadataSource.getSources().size());

    LifecycleMappingMetadataSource source = metadataSource.getSources().get(0);
    assertEquals("testLifecycleMappingMetadata", source.getGroupId());
    assertEquals("testLifecycleMappingMetadata1", source.getArtifactId());
    assertEquals("0.0.1", source.getVersion());

    // Assert lifecycle mappings
    assertNotNull(getLifecycleMappingMetadata(source, "war"));
    assertEquals("fakeid", getLifecycleMappingMetadata(source, "war").getLifecycleMappingId());
    assertNull(getLifecycleMappingMetadata(source, "jar"));

    // Assert mojo/plugin executions
    List<PluginExecutionMetadata> pluginExecutions = source.getPluginExecutions();
    assertEquals(3, pluginExecutions.size());
    Set<String> goals = new LinkedHashSet<>();

    goals.add("compile");
    goals.add("testCompile");
    assertPluginExecutionMetadata("org.apache.maven.plugins", "maven-compiler-plugin", "[2.0,)", goals,
        PluginExecutionAction.configurator, "org.eclipse.m2e.jdt.javaConfigurator", pluginExecutions.get(0));

    goals = new LinkedHashSet<>();
    goals.add("jar");
    assertPluginExecutionMetadata("org.apache.maven.plugins", "maven-jar-plugin", "[2.0,)", goals,
        PluginExecutionAction.ignore, null, pluginExecutions.get(1));

    goals = new LinkedHashSet<>();
    goals.add("resources");
    assertPluginExecutionMetadata("org.apache.maven.plugins", "maven-resources-plugin", "[2.0,)", goals,
        PluginExecutionAction.execute, null, pluginExecutions.get(2));
  }

  @Test
  public void testGetLifecycleMappingMetadataOverride() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
        "testGetLifecycleMappingMetadataOverride/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    List<MappingMetadataSource> metadata = LifecycleMappingFactory
        .getPomMappingMetadataSources(facade.getMavenProject(monitor), monitor);
    assertNotNull(metadata);
    assertEquals(2, metadata.size());

    assertEquals(SimpleMappingMetadataSource.class, metadata.get(0).getClass());
    assertEquals(SimpleMappingMetadataSource.class, metadata.get(1).getClass());
    SimpleMappingMetadataSource metadataSource1 = (SimpleMappingMetadataSource) metadata.get(0);
    SimpleMappingMetadataSource metadataSource2 = (SimpleMappingMetadataSource) metadata.get(1);
    assertEquals(1, metadataSource1.getSources().size());
    assertEquals(1, metadataSource2.getSources().size());

    LifecycleMappingMetadataSource source1 = metadataSource1.getSources().get(0);
    assertEquals("testLifecycleMappingMetadata", source1.getGroupId());
    assertEquals("testLifecycleMappingMetadata2", source1.getArtifactId());
    assertEquals("0.0.2", source1.getVersion());

    LifecycleMappingMetadataSource source2 = metadataSource2.getSources().get(0);
    assertEquals("testLifecycleMappingMetadata", source2.getGroupId());
    assertEquals("testLifecycleMappingMetadata1", source2.getArtifactId());
    assertEquals("0.0.1", source2.getVersion());

    List<PluginExecutionMetadata> pluginExecutions = source1.getPluginExecutions();
    assertEquals(2, pluginExecutions.size());
    Set<String> goals = new LinkedHashSet<>();

    goals.add("compile");
    goals.add("testCompile");
    assertPluginExecutionMetadata("org.apache.maven.plugins", "maven-compiler-plugin", "[3.0,)", goals,
        PluginExecutionAction.configurator, "org.eclipse.m2e.jdt.javaConfigurator", pluginExecutions.get(0));

    goals = new LinkedHashSet<>();
    goals.add("jar");
    assertPluginExecutionMetadata("org.apache.maven.plugins", "maven-jar-plugin", "[3.0,)", goals,
        PluginExecutionAction.ignore, null, pluginExecutions.get(1));
  }

  @Test
  public void testProfilesAndProperties() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
        "testProfilesAndProperties/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    List<MappingMetadataSource> metadata = LifecycleMappingFactory
        .getPomMappingMetadataSources(facade.getMavenProject(monitor), monitor);
    assertNotNull(metadata);
    assertEquals(2, metadata.size());

    assertEquals(SimpleMappingMetadataSource.class, metadata.get(0).getClass());
    assertEquals(SimpleMappingMetadataSource.class, metadata.get(1).getClass());
    SimpleMappingMetadataSource metadataSource1 = (SimpleMappingMetadataSource) metadata.get(0);
    SimpleMappingMetadataSource metadataSource2 = (SimpleMappingMetadataSource) metadata.get(1);

    assertEquals(1, metadataSource1.getSources().size());
    assertEquals(1, metadataSource2.getSources().size());

    LifecycleMappingMetadataSource source1 = metadataSource1.getSources().get(0);
    assertEquals("testLifecycleMappingMetadata", source1.getGroupId());
    assertEquals("testLifecycleMappingMetadata2", source1.getArtifactId());
    assertEquals("0.0.2", source1.getVersion());

    LifecycleMappingMetadataSource source2 = metadataSource2.getSources().get(0);
    assertEquals("testLifecycleMappingMetadata", source2.getGroupId());
    assertEquals("testLifecycleMappingMetadata1", source2.getArtifactId());
    assertEquals("0.0.1", source2.getVersion());
  }

  @Test
  public void testGetLifecycleMappingMetadataMerge() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
        "testGetLifecycleMappingMetadataMerge/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    List<MappingMetadataSource> metadata = LifecycleMappingFactory
        .getPomMappingMetadataSources(facade.getMavenProject(monitor), monitor);
    assertNotNull(metadata);
    assertEquals(2, metadata.size());

    assertEquals(SimpleMappingMetadataSource.class, metadata.get(0).getClass());
    assertEquals(SimpleMappingMetadataSource.class, metadata.get(1).getClass());
    SimpleMappingMetadataSource metadataSource1 = (SimpleMappingMetadataSource) metadata.get(0);
    SimpleMappingMetadataSource metadataSource2 = (SimpleMappingMetadataSource) metadata.get(1);

    assertEquals(1, metadataSource1.getSources().size());
    assertEquals(1, metadataSource2.getSources().size());

    LifecycleMappingMetadataSource source1 = metadataSource1.getSources().get(0);
    assertEquals("testLifecycleMappingMetadata", source1.getGroupId());
    assertEquals("testLifecycleMappingMetadata2", source1.getArtifactId());
    assertEquals("0.0.2", source1.getVersion());

    LifecycleMappingMetadataSource source2 = metadataSource2.getSources().get(0);
    assertEquals("testLifecycleMappingMetadata", source2.getGroupId());
    assertEquals("testLifecycleMappingMetadata1", source2.getArtifactId());
    assertEquals("0.0.1", source2.getVersion());
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

  @Test
  public void testDefaultJarLifecycleMapping() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "default-jar/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    AbstractCustomizableLifecycleMapping lifecycleMapping = (AbstractCustomizableLifecycleMapping) projectConfigurationManager
        .getLifecycleMapping(facade);

    assertTrue(lifecycleMapping instanceof JarLifecycleMapping);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertHasJavaConfigurator(configurators);

    List<MojoExecutionKey> notCoveredMojoExecutions = getNotCoveredMojoExecutions(facade);
    assertEquals(notCoveredMojoExecutions.toString(), 0, notCoveredMojoExecutions.size());

    Map<MojoExecutionKey, List<AbstractBuildParticipant>> buildParticipants = lifecycleMapping.getBuildParticipants(
        facade, monitor);
    assertEquals(8, buildParticipants.size());

    assertBuildParticipantType(buildParticipants, "maven-resources-plugin", MojoExecutionBuildParticipant.class);
    // TODO assert all other mojo executions
  }

  private void assertHasJavaConfigurator(List<AbstractProjectConfigurator> configurators) {
    for(AbstractProjectConfigurator configurator : configurators) {
      if(configurator instanceof JavaProjectConfigurator) {
        return;
      }
    }
    fail("no java configurator found: " + configurators.toString());
  }

  private void assertBuildParticipantType(Map<MojoExecutionKey, List<AbstractBuildParticipant>> buildParticipants,
      String artifactId, Class<MojoExecutionBuildParticipant> participantType) {
    for(Map.Entry<MojoExecutionKey, List<AbstractBuildParticipant>> entry : buildParticipants.entrySet()) {
      if(artifactId.equals(entry.getKey().artifactId())) {
        for(AbstractBuildParticipant participant : entry.getValue()) {
          assertEquals(participantType, participant.getClass());
        }
      }
    }
  }

  @Test
  public void testNotCoveredMojoExecutions() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "notCoveredMojoExecutions/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    assertNotNull("Expected not null project", project);

    AbstractCustomizableLifecycleMapping lifecycleMapping = (AbstractCustomizableLifecycleMapping) projectConfigurationManager
        .getLifecycleMapping(facade);

    assertTrue("Unexpected lifecycle mapping type:" + lifecycleMapping.getClass().getName(),
        lifecycleMapping instanceof TestLifecycleMapping);
    assertNotNull("Expected not null lifecycle mapping", lifecycleMapping);
    List<MojoExecutionKey> notCoveredMojoExecutions = getNotCoveredMojoExecutions(facade);
    assertEquals(notCoveredMojoExecutions.toString(), 2, notCoveredMojoExecutions.size());
    assertEquals(
        "org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-1 (execution: default-test-goal-1, phase: process-resources)",
        notCoveredMojoExecutions.get(0).toString());
    assertEquals(
        "org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-2 (execution: default-test-goal-2, phase: compile)",
        notCoveredMojoExecutions.get(1).toString());

    // Also verify that we get the expected markers
    IMarker[] errorMarkers = project.findMarkers(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID, true,
        IResource.DEPTH_INFINITE);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 2, errorMarkers.length);
    String expectedErrorMessage = "Plugin execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-1 (execution: default-test-goal-1, phase: process-resources)";
    IMarker marker = WorkspaceHelpers.assertMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID, -1, expectedErrorMessage,
        11 /*lineNumber <plugin> of plugin def*/, "pom.xml", project);
    WorkspaceHelpers.assertErrorMarkerAttributes(marker, notCoveredMojoExecutions.get(0));
    expectedErrorMessage = "Plugin execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-2 (execution: default-test-goal-2, phase: compile)";
    marker = WorkspaceHelpers.assertMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID, -1, expectedErrorMessage,
        11 /*lineNumber <plugin> of plugin def*/, "pom.xml", project);
    WorkspaceHelpers.assertErrorMarkerAttributes(marker, notCoveredMojoExecutions.get(1));
  }

  @Test
  public void testMissingMapping() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "missing/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 1, errorMarkers.size());
    String expectedErrorMessage = "Lifecycle mapping \"unknown-or-missing\" is not available. To enable full functionality, install the lifecycle mapping and run Maven->Update Project Configuration.";
    IMarker marker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID,
        expectedErrorMessage, null /*lineNumber*/, project);
    WorkspaceHelpers.assertMarkerLocation(new SourceLocation(14, 3, 13), marker);
    WorkspaceHelpers.assertLifecycleIdErrorMarkerAttributes(marker, "unknown-or-missing");

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade);
    assertTrue(lifecycleMapping instanceof InvalidLifecycleMapping);

    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 1, errorMarkers.size());
    expectedErrorMessage = "Lifecycle mapping \"unknown-or-missing\" is not available. To enable full functionality, install the lifecycle mapping and run Maven->Update Project Configuration.";
    marker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID, expectedErrorMessage,
        null /*lineNumber*/, project);
    WorkspaceHelpers.assertLifecycleIdErrorMarkerAttributes(marker, "unknown-or-missing");
  }

  @Test
  public void testUnknownPackagingType() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "unknownPackagingType/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade);
    assertTrue(lifecycleMapping instanceof DefaultLifecycleMapping);

    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(project);
  }

  @Test
  public void testNotInterestingPhaseConfigurator() throws Exception {
    IMavenProjectFacade facade = importMavenProject(
        "projects/lifecyclemapping/lifecycleMappingMetadata/testNotInterestingPhaseConfigurator", "pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);

    ILifecycleMapping lifecycleMapping = LifecycleMappingFactory.getLifecycleMapping(facade);

    Map<MojoExecutionKey, List<AbstractBuildParticipant>> buildParticipants = lifecycleMapping.getBuildParticipants(
        facade, monitor);
    assertEquals(1, buildParticipants.size());
    MojoExecutionKey executionKey = buildParticipants.keySet().iterator().next();
    assertEquals("package", executionKey.lifecyclePhase());
    assertEquals("test-lifecyclemapping-plugin", executionKey.artifactId());
    assertEquals(1, buildParticipants.get(executionKey).size());
    assertTrue(buildParticipants.get(executionKey).get(0) instanceof MojoExecutionBuildParticipant);
  }

  @Test
  public void testDuplicatePackagingTypeMetadata() throws Exception {
    MavenProjectFacade facade = newMavenProjectFacade(
        "projects/lifecyclemapping/lifecycleMappingMetadata/DuplicateMetadata/testDuplicatePackagingType", "pom.xml");

    LifecycleMappingResult mappingResult = calculateLifecycleMapping(facade);

    List<MavenProblemInfo> problems = mappingResult.getProblems();

    assertEquals(problems.toString(), 1, problems.size());
    assertEquals(
        "Conflicting lifecycle mapping metadata (project packaging type=\"test-packaging-a\"): Mapping defined in 'MavenProject: lifecycleMappingMetadataTests:testDuplicatePackagingType:0.0.1-SNAPSHOT @ "
            + facade.getPomFile()
            + "' and 'MavenProject: lifecycleMappingMetadataTests:testDuplicatePackagingType:0.0.1-SNAPSHOT @ "
            + facade.getPomFile()
            + "'. To enable full functionality, remove the conflicting mapping and run Maven->Update Project Configuration.",
        problems.get(0).getMessage());

    assertNull(mappingResult.getLifecycleMapping());
  }

  @Test
  public void testDuplicatePluginExecution1() throws Exception {
    MavenProjectFacade facade = newMavenProjectFacade(
        "projects/lifecyclemapping/lifecycleMappingMetadata/DuplicateMetadata/testDuplicatePluginExecution1", "pom.xml");

    LifecycleMappingResult mappingResult = calculateLifecycleMapping(facade);

    List<MavenProblemInfo> problems = mappingResult.getProblems();

    assertEquals(4, problems.size());
    assertEquals(
        "Conflicting lifecycle mapping (plugin execution \"org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-1 (execution: default-test-goal-1, phase: process-resources)\"): Mapping defined in 'MavenProject: lifecycleMappingMetadataTests:testDuplicatePluginExecution1:0.0.1-SNAPSHOT @ "
            + facade.getPomFile()
            + "' and 'MavenProject: lifecycleMappingMetadataTests:testDuplicatePluginExecution1:0.0.1-SNAPSHOT @ "
            + facade.getPomFile()
            + "'. To enable full functionality, remove the conflicting mapping and run Maven->Update Project Configuration.",
        problems.get(0).getMessage());
    // [1] Conflicting lifecycle mapping (plugin execution "org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-2 (execution: default-test-goal-2, phase: compile)"). ...
    // [2] Plugin execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-1 (execution: default-test-goal-1, phase: process-resources)
    // [3] Plugin execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-2 (execution: default-test-goal-2, phase: compile)
  }

  @Test
  public void testDuplicatePluginExecution2() throws Exception {
    MavenProjectFacade facade = newMavenProjectFacade(
        "projects/lifecyclemapping/lifecycleMappingMetadata/DuplicateMetadata/testDuplicatePluginExecution2", "pom.xml");

    LifecycleMappingResult mappingResult = calculateLifecycleMapping(facade);

    List<MavenProblemInfo> problems = mappingResult.getProblems();

    assertEquals(3, problems.size());
    assertEquals(
        "Conflicting lifecycle mapping (plugin execution \"org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-1 (execution: default-test-goal-1, phase: process-resources)\"): Mapping defined in 'MavenProject: lifecycleMappingMetadataTests:testDuplicatePluginExecution2:0.0.1-SNAPSHOT @ "
            + facade.getPomFile()
            + "' and 'MavenProject: lifecycleMappingMetadataTests:testDuplicatePluginExecution2:0.0.1-SNAPSHOT @ "
            + facade.getPomFile()
            + "'. To enable full functionality, remove the conflicting mapping and run Maven->Update Project Configuration.",
        problems.get(0).getMessage());
    // [1] Plugin execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-1 (execution: default-test-goal-1, phase: process-resources)
    // [2] Plugin execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-2 (execution: default-test-goal-2, phase: compile)
  }

  private MavenProjectFacade newMavenProjectFacade(String dirName, String fileName) throws IOException, CoreException {
    File dir = new File(dirName);
    IProject project = createExisting(dir.getName(), dirName);
    return newMavenProjectFacade(project.getFile(fileName));
  }

  @Test
  public void testSecondaryConfiguratorsCustomizable() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/secondaryConfigurators/customizable",
        "pom.xml");
    assertNoErrors(facade.getProject());

    AbstractLifecycleMapping lifecycleMapping = (AbstractLifecycleMapping) projectConfigurationManager
        .getLifecycleMapping(facade);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertEquals(3, configurators.size());
  }

  @Test
  public void testSecondaryConfiguratorsCustom() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/secondaryConfigurators/custom",
        "pom.xml");
    assertNoErrors(facade.getProject());

    AbstractLifecycleMapping lifecycleMapping = (AbstractLifecycleMapping) projectConfigurationManager
        .getLifecycleMapping(facade);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertEquals(3, configurators.size());
  }

  @Test
  public void testCompatibleVersion() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
        "testCompatibleVersion/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade);
    assertNotNull(lifecycleMapping);
    assertTrue(lifecycleMapping.getClass().getCanonicalName(), lifecycleMapping instanceof JarLifecycleMapping);
  }

  @Test
  public void testIncompatibleVersion() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
        "testIncompatibleVersion/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 1, errorMarkers.size());

    String expectedErrorMessage = "Incompatible lifecycle mapping plugin version 1000.0.0";
    IMarker marker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID,
        expectedErrorMessage, null /*lineNumber*/, project);
    WorkspaceHelpers.assertMarkerLocation(new SourceLocation(14, 11, 19), marker);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade);
    assertTrue(lifecycleMapping instanceof InvalidLifecycleMapping);
  }

  @Test
  public void testPackagingTypeMismatch() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping",
        "testPackagingTypeMismatch/pom/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    assertNotNull("Expected not null project", project);
    WorkspaceHelpers.assertNoErrors(project);

    facade = importMavenProject("projects/lifecyclemapping", "testPackagingTypeMismatch/other/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    project = facade.getProject();
    assertNotNull("Expected not null project", project);
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 1, errorMarkers.size());

    String expectedErrorMessage = "Packaging type test-packaging-a configured in embedded lifecycle mapping configuration does not match the packaging type test-packaging-empty of the current project.";
    IMarker marker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID,
        expectedErrorMessage, null /*lineNumber*/, project);
    WorkspaceHelpers.assertMarkerLocation(new SourceLocation(25, 11, 25), marker);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade);
    assertTrue(lifecycleMapping instanceof InvalidLifecycleMapping);
  }

  @Test
  public void testSameConfiguratorUsedTwice() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
        "testSameConfiguratorUsedTwice/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);
    WorkspaceHelpers.assertNoWarnings(project);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade);
    assertNotNull(lifecycleMapping);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertEquals(configurators.toString(), 1, configurators.size());
    assertTrue(configurators.get(0) instanceof TestProjectConfigurator);

    Map<MojoExecutionKey, List<AbstractBuildParticipant>> buildParticipants = lifecycleMapping.getBuildParticipants(
        facade, monitor);
    assertEquals(buildParticipants.toString(), 2, buildParticipants.size());
    for(MojoExecutionKey mojoExecutionKey : buildParticipants.keySet()) {
      List<AbstractBuildParticipant> buildParticipantList = buildParticipants.get(mojoExecutionKey);
      assertNotNull(mojoExecutionKey.toString(), buildParticipantList);
      assertEquals(mojoExecutionKey.toString(), 1, buildParticipantList.size());
      AbstractBuildParticipant buildParticipant = buildParticipantList.get(0);
      assertTrue(mojoExecutionKey.toString(), buildParticipant instanceof TestBuildParticipant);
      assertEquals(mojoExecutionKey, ((TestBuildParticipant) buildParticipant).mojoExecutionKey);
    }
  }

  @Test
  public void testNonresolvableExecutionPlan() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping",
        "testNonresolvableExecutionPlan/pom.xml");

    assertNotNull("Expected not null MavenProjectFacade", facade);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade);
    assertTrue(lifecycleMapping instanceof JarLifecycleMapping);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertEquals(configurators.toString(), 0, configurators.size());
  }

  @Test
  public void testNondeafaultLifecycles() throws Exception {
    MavenProjectFacade facade = (MavenProjectFacade) importMavenProject(
        "projects/lifecyclemapping/lifecycleMappingMetadata/PluginExecutionActionsTest/testNondefaultLifecycles",
        "pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);

    List<Map.Entry<MojoExecutionKey, List<IPluginExecutionMetadata>>> executionMapping = new ArrayList<>(
        facade.getMojoExecutionMapping().entrySet());
    assertEquals(4, executionMapping.size());
    assertEquals(0, executionMapping.get(0).getValue().size()); // maven-clean-plugin
    assertEquals(1, executionMapping.get(1).getValue().size()); // test-lifecyclemapping-plugin:test-goal-1
    assertEquals(0, executionMapping.get(2).getValue().size()); // maven-site-plugin
    assertEquals(1, executionMapping.get(3).getValue().size()); // test-lifecyclemapping-plugin:test-goal-2

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade);
    assertNotNull("Expected not null lifecycle mapping", lifecycleMapping);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertEquals(2, configurators.size());
    assertTrue(configurators.get(0) instanceof TestProjectConfigurator);
    assertTrue(configurators.get(1) instanceof TestProjectConfigurator2);

    // assert non-default lifecycle mojos are not included in execution plan
    assertEquals(2, facade.getExecutionPlan(ProjectRegistryManager.LIFECYCLE_CLEAN, null).size());
    assertEquals(0, facade.getExecutionPlan(ProjectRegistryManager.LIFECYCLE_DEFAULT, null).size());
    assertEquals(2, facade.getExecutionPlan(ProjectRegistryManager.LIFECYCLE_SITE, null).size());

    // assert configurators activated by non-default lifecycle mojos are not considered during the build
    assertEquals(0, lifecycleMapping.getBuildParticipants(facade, monitor).size());
  }

  @Test
  public void testPomPackagingType() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "pomPackagingType/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade);

    assertTrue(lifecycleMapping instanceof NoopLifecycleMapping);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertNotNull("Expected not null configurators", configurators);
    assertEquals(configurators.toString(), 0, configurators.size());

    List<MojoExecutionKey> notCoveredMojoExecutions = getNotCoveredMojoExecutions(facade);
    assertEquals(notCoveredMojoExecutions.toString(), 0, notCoveredMojoExecutions.size());

    Map<MojoExecutionKey, List<AbstractBuildParticipant>> buildParticipants = lifecycleMapping.getBuildParticipants(
        facade, monitor);
    assertEquals(0, buildParticipants.size());
  }

  @Test
  public void testFailToGetPluginParameterValue() throws Exception {
    withDefaultLifecycleMapping(PluginExecutionAction.warn, () -> {
      MavenProjectFacade facade = (MavenProjectFacade) importMavenProject("projects/lifecyclemapping",
          "testFailToGetPluginParameterValue/pom.xml");
      assertNotNull("Expected not null MavenProjectFacade", facade);

      LifecycleMappingResult mappingResult = calculateLifecycleMapping(facade);

      List<MavenProblemInfo> problems = mappingResult.getProblems();

      assertEquals(problems.toString(), 2, problems.size());
      assertTrue(problems.get(0).toString(), problems.get(0).getMessage()
          .contains("Plugin missing:missing:1.0.0 or one of its dependencies could not be resolved"));
      assertEquals(
          "Plugin execution not covered by lifecycle configuration: missing:missing:1.0.0:run (execution: test, phase: compile)",
          problems.get(1).getMessage());
      // [0] CoreException: Could not calculate build plan: Plugin missing:missing:1.0.0 or one of its dependencies could not be resolved: Failed to read artifact descriptor for missing:missing:jar:1.0.0: ArtifactResolutionException: Failure to find missing:missing:pom:1.0.0 in file:repositories/testrepo was cached in the local repository, resolution will not be reattempted until the update interval of testrepo has elapsed or updates are forced
      // [1] Plugin execution not covered by lifecycle configuration: missing:missing:1.0.0:run (execution: test, phase: compile))
      return null;
    });
  }

  @Test
  public void testSetNoopMappingDuringImport() throws Exception {
    ResolverConfiguration resolverConfig = new ResolverConfiguration();
    resolverConfig.setLifecycleMappingId(NoopLifecycleMapping.LIFECYCLE_MAPPING_ID);
    IProject project = importProject("projects/lifecyclemapping/default-jar/pom.xml", resolverConfig);

    assertFalse(project.hasNature(JavaCore.NATURE_ID));

    IMavenProjectFacade facade = mavenProjectManager.create(project, monitor);

    assertEquals(NoopLifecycleMapping.LIFECYCLE_MAPPING_ID, facade.getLifecycleMappingId());

    assertTrue(LifecycleMappingFactory.getLifecycleMapping(facade) instanceof NoopLifecycleMapping);
    assertTrue(LifecycleMappingFactory.getProjectConfigurators(facade).isEmpty());

    // force reload and check again
    facade.setSessionProperty(MavenProjectFacade.PROP_LIFECYCLE_MAPPING, null);
    facade.setSessionProperty(MavenProjectFacade.PROP_CONFIGURATORS, null);
    assertTrue(LifecycleMappingFactory.getLifecycleMapping(facade) instanceof NoopLifecycleMapping);
    assertTrue(LifecycleMappingFactory.getProjectConfigurators(facade).isEmpty());
  }

  @Test
  public void testSetCustomizableMappingDuringImport() throws Exception {
    ResolverConfiguration resolverConfig = new ResolverConfiguration();
    resolverConfig.setLifecycleMappingId(TestLifecycleMapping.LIFECYCLE_MAPPING_ID);
    IProject project = importProject("projects/lifecyclemapping/default-jar/pom.xml", resolverConfig);

    assertTrue(project.hasNature(JavaCore.NATURE_ID));

    IMavenProjectFacade facade = mavenProjectManager.create(project, monitor);

    assertEquals(TestLifecycleMapping.LIFECYCLE_MAPPING_ID, facade.getLifecycleMappingId());

    assertTrue(LifecycleMappingFactory.getLifecycleMapping(facade) instanceof TestLifecycleMapping);
    assertFalse(LifecycleMappingFactory.getProjectConfigurators(facade).isEmpty());

    // force reload and check again
    facade.setSessionProperty(MavenProjectFacade.PROP_LIFECYCLE_MAPPING, null);
    facade.setSessionProperty(MavenProjectFacade.PROP_CONFIGURATORS, null);
    assertTrue(LifecycleMappingFactory.getLifecycleMapping(facade) instanceof TestLifecycleMapping);
    assertFalse(LifecycleMappingFactory.getProjectConfigurators(facade).isEmpty());
  }

  @Test
  public void testMissingVersionRangeInPluginExecutionFilter() throws Exception {
    try {
      importMavenProject("projects/lifecyclemapping", "testMissingVersionRangeInPluginExecutionFilter/pom.xml");
      fail("Expected exception not thrown.");
    } catch(IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("Missing parameter for pluginExecutionFilter"));
    }
  }

  @Test
  public void testNotCoveredMojoExecutionWarnings() throws Exception {
    withDefaultLifecycleMapping(PluginExecutionAction.warn, () -> {
      String originalSeverity = mavenConfiguration.getNotCoveredMojoExecutionSeverity();
      try {
        ((MavenConfigurationImpl) mavenConfiguration)
            .setNotCoveredMojoExecutionSeverity(ProblemSeverity.warning.toString());
        IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping",
            "notCoveredMojoExecutions/pom.xml");
        assertNotNull("Expected not null MavenProjectFacade", facade);
        IProject project = facade.getProject();
        assertNotNull("Expected not null project", project);
        assertNoErrors(project);

        // Also verify that we get the expected markers
        List<IMarker> errorMarkers = WorkspaceHelpers.findMarkers(project, IMarker.SEVERITY_WARNING);
        assertEquals(WorkspaceHelpers.toString(errorMarkers), 2, errorMarkers.size());

        List<MojoExecutionKey> notCoveredMojoExecutions = getNotCoveredMojoExecutions(facade);
        assertEquals(notCoveredMojoExecutions.toString(), 2, notCoveredMojoExecutions.size());

        String expectedErrorMessage = "Plugin execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-1 (execution: default-test-goal-1, phase: process-resources)";
        IMarker marker = WorkspaceHelpers.assertWarningMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID,
            expectedErrorMessage, 11 /*lineNumber <plugin> of plugin def*/, project);
        WorkspaceHelpers.assertErrorMarkerAttributes(marker, notCoveredMojoExecutions.get(0));

        expectedErrorMessage = "Plugin execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-2 (execution: default-test-goal-2, phase: compile)";
        marker = WorkspaceHelpers.assertWarningMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID, expectedErrorMessage,
            11 /*lineNumber <plugin> of plugin def*/, project);
        WorkspaceHelpers.assertErrorMarkerAttributes(marker, notCoveredMojoExecutions.get(1));

      } finally {
        ((MavenConfigurationImpl) mavenConfiguration).setNotCoveredMojoExecutionSeverity(originalSeverity);
      }
      return null;
    });
  }

  @Test
  public void testNotCoveredMojoExecutionIgnored() throws Exception {
    String originalSeverity = mavenConfiguration.getNotCoveredMojoExecutionSeverity();
    try {
      ((MavenConfigurationImpl) mavenConfiguration).setNotCoveredMojoExecutionSeverity(ProblemSeverity.ignore
          .toString());
      IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "notCoveredMojoExecutions/pom.xml");
      assertNotNull("Expected not null MavenProjectFacade", facade);
      IProject project = facade.getProject();
      assertNotNull("Expected not null project", project);
      assertNoErrors(project);

      // verify we have no warning markers either
      List<IMarker> errorMarkers = WorkspaceHelpers.findMarkers(project, IMarker.SEVERITY_WARNING);
      assertEquals(WorkspaceHelpers.toString(errorMarkers), 0, errorMarkers.size());

    } finally {
      ((MavenConfigurationImpl) mavenConfiguration).setNotCoveredMojoExecutionSeverity(originalSeverity);
    }

  }

  @Test
  public void testOrder() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata/sorting/",
        "testConfiguratorOrder/pom.xml");
    WorkspaceHelpers.assertNoErrors(facade.getProject());

    IFile log = facade.getProject().getFile("target/configurator-log.txt");
    assertTrue(log.getFullPath() + " is missing", log.exists());
    String order = IOUtil.toString(log.getContents(true));
    assertEquals("TEST_SECONDARY3,TEST_SECONDARY4,TEST_SECONDARY6,TEST_SECONDARY5", order);
  }

  @Test
  public void test371618_NullLifecycleMappingPluginVersion() throws Exception {
    IProject[] projects = importProjects("projects/lifecyclemapping/371618",
        new String[] {"pom.xml", "testchild/pom.xml"}, new ResolverConfiguration());
    WorkspaceHelpers.assertNoErrors(projects[0]);
    WorkspaceHelpers.assertNoErrors(projects[1]);
  }

  @Test
  public void testConfiguratorsFollowPhaseOrder() throws Exception {
    MavenProjectFacade facade = (MavenProjectFacade) importMavenProject(
        "projects/lifecyclemapping/lifecycleMappingMetadata/PluginExecutionActionsTest/testConfiguratorsOrder",
        "pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade);
    assertNotNull("Expected not null lifecycle mapping", lifecycleMapping);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertEquals(2, configurators.size());
    assertEquals(TestProjectConfigurator2.class, configurators.get(0).getClass());
    assertEquals(TestProjectConfigurator.class, configurators.get(1).getClass());
  }

  @Test
  public void testWorkspaceArtifacts() throws Exception {
    MavenProjectFacade facade = (MavenProjectFacade) importMavenProject(
        "projects/lifecyclemapping/lifecycleMappingMetadata/PluginExecutionActionsTest/testConfiguratorsOrder",
        "pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    ProjectRegistryManager projectRegistry = MavenPluginActivator.getDefault().getMavenProjectManagerImpl();
    IMavenExecutionContext executionContext = projectRegistry.createExecutionContext(facade.getPom(),
        facade.getConfiguration());
    WorkspaceReader workspaceReader = executionContext.getExecutionRequest().getWorkspaceReader();
    ArtifactKey a = facade.getArtifactKey();
    org.eclipse.aether.artifact.Artifact artifact = new org.eclipse.aether.artifact.DefaultArtifact(a.groupId(),
        a.artifactId(), a.classifier(), "pom", a.version());
    File file = workspaceReader.findArtifact(artifact);
    assertNotNull(file);
    artifact = new org.eclipse.aether.artifact.DefaultArtifact(a.groupId(), a.artifactId(), a.classifier(),
        facade.getPackaging(), a.version());
    file = workspaceReader.findArtifact(artifact);
    assertNull(file);
  }

  @Test
  public void test494858ProcessingInstructions() throws Exception {
    MavenProjectFacade facade = (MavenProjectFacade) importMavenProject(
        "projects/lifecyclemapping/lifecycleMappingMetadata/ProcessingInstructions", "pom.xml");

    assertNotNull("Expected not null MavenProjectFacade", facade);
    WorkspaceHelpers.assertNoErrors(facade.getProject());

    MavenProject project = facade.getMavenProject(monitor);
    MavenProject parent = project.getParent();

    List<MappingMetadataSource> metadata = LifecycleMappingFactory
        .getPomMappingMetadataSources(facade.getMavenProject(monitor), monitor);
    assertNotNull(metadata);
    assertEquals(2, metadata.size());

    assertEquals(AnnotationMappingMetadataSource.class, metadata.get(0).getClass());
    assertEquals(AnnotationMappingMetadataSource.class, metadata.get(1).getClass());

    LifecycleMappingResult mappingResult = calculateLifecycleMapping(facade);
    Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mapping = mappingResult.getMojoExecutionMapping();

    List<MojoExecution> validations = facade.getMojoExecutions("org.eclipse.m2e.test.lifecyclemapping",
        "test-buildhelper-plugin", monitor, "validate");
    assertEquals(3, validations.size());

    assertExecution(validations.get(0), mapping, project, "test1", PluginExecutionAction.ignore, "<ignore/>");
    assertExecution(validations.get(1), mapping, parent, "test2", PluginExecutionAction.ignore, "<ignore/>");
    assertExecution(validations.get(2), mapping, project, "test3", PluginExecutionAction.configurator,
        "<configurator><id>org.eclipse.m2e.configurator.test</id></configurator>");

    validations = facade.getMojoExecutions("org.eclipse.m2e.test.lifecyclemapping", "test-embeddedmapping-plugin",
        monitor, "test-goal-1", "test-goal-2");
    assertEquals(4, validations.size());

    String exec = "<execute><runOnConfiguration>true</runOnConfiguration><runOnIncremental>true</runOnIncremental></execute>";

    assertExecution(validations.get(0), mapping, parent, "test4", PluginExecutionAction.execute, exec);
    assertExecution(validations.get(1), mapping, parent, "test5", PluginExecutionAction.execute, exec);
    assertExecution(validations.get(2), mapping, parent, "test6", PluginExecutionAction.execute, exec);
    assertExecution(validations.get(3), mapping, project, "test7", PluginExecutionAction.execute,
        "<execute><runOnConfiguration>true</runOnConfiguration></execute>");
  }

  private void assertExecution(MojoExecution execution, Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mapping,
      MavenProject sourceProject, String executionId, PluginExecutionAction action, String domContent)
          throws Exception {
    assertEquals(executionId, execution.getExecutionId());
    List<IPluginExecutionMetadata> metadatas = mapping.get(new MojoExecutionKey(execution));
    assertNotNull(metadatas);
    assertEquals(1, metadatas.size());
    PluginExecutionMetadata metadata = (PluginExecutionMetadata) metadatas.get(0);
    assertEquals(sourceProject, metadata.getSource().getSource());
    assertEquals(action, metadata.getAction());
    Xpp3Dom actionDom = new Xpp3Dom("action");
    actionDom.addChild(Xpp3DomBuilder.build(new StringReader(domContent)));
    assertEquals(actionDom, metadata.getActionDom());

  }

  @Test
  public void test387736_missingAction() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/387736", "pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    assertNotNull("Expected not null project", project);

    // the mapping is ignored because of the missing action
    List<MojoExecutionKey> notCoveredMojoExecutions = getNotCoveredMojoExecutions(facade);
    assertEquals(notCoveredMojoExecutions.toString(), 1, notCoveredMojoExecutions.size());
    assertEquals(
        "org.eclipse.m2e.test.lifecyclemapping:test-buildhelper-plugin:1.0.0:publish (execution: add-source, phase: generate-sources)",
        notCoveredMojoExecutions.get(0).toString());
  }
}
