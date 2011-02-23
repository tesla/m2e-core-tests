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

package org.eclipse.m2e.tests.lifecycle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;

import org.apache.maven.execution.MavenExecutionRequest;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.internal.lifecyclemapping.InvalidLifecycleMapping;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingResult;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.internal.markers.MavenProblemInfo;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractCustomizableLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.jdt.internal.JarLifecycleMapping;
import org.eclipse.m2e.jdt.internal.JavaProjectConfigurator;
import org.eclipse.m2e.tests.common.AbstractLifecycleMappingTest;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;
import org.eclipse.m2e.tests.configurators.TestBuildParticipant;
import org.eclipse.m2e.tests.configurators.TestLifecycleMapping;
import org.eclipse.m2e.tests.configurators.TestProjectConfigurator;
import org.eclipse.m2e.tests.configurators.TestProjectConfigurator2;


@SuppressWarnings("restriction")
public class LifecycleMappingTest extends AbstractLifecycleMappingTest {
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

  public void testMissingLifecycleMappingMetadata() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
        "testMissingLifecycleMappingMetadata/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);

    IProject project = facade.getProject();

    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 2, errorMarkers.size());

    String expectedErrorMessage = "Could not resolve artifact testLifecycleMappingMetadata:missing:xml:lifecycle-mapping-metadata:0.0.1";
    IMarker marker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID,
        expectedErrorMessage, null /*lineNumber*/, project);
    WorkspaceHelpers.assertMarkerLocation(new SourceLocation(17, 11, 25), marker);

    expectedErrorMessage = "Unknown or missing lifecycle mapping (project packaging type=\"jar\")";
    marker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID,
        expectedErrorMessage, null /*lineNumber*/, project);
    WorkspaceHelpers.assertMarkerLocation(new SourceLocation(7, 3, 13), marker);
    WorkspaceHelpers.assertLifecyclePackagingErrorMarkerAttributes(marker, "jar");

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

  public void testUseDefaultLifecycleMappingMetadataSource() throws Exception {
    LifecycleMappingFactory.setUseDefaultLifecycleMappingMetadataSource(false);
    assertNull(LifecycleMappingFactory.getDefaultLifecycleMappingMetadataSource());

    // By default, the use of default lifecycle metadata is disabled for unit tests
    LifecycleMappingFactory.setUseDefaultLifecycleMappingMetadataSource(true);

    assertNotNull(LifecycleMappingFactory.getDefaultLifecycleMappingMetadataSource());
  }

  public void testGetLifecycleMappingMetadata() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
        "testGetLifecycleMappingMetadata/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    MavenExecutionRequest request = mavenProjectManager.createExecutionRequest(facade, monitor);
    List<LifecycleMappingMetadataSource> metadataSources = LifecycleMappingFactory.getPomMappingMetadataSources(
        facade.getMavenProject(), request, monitor);
    assertNotNull(metadataSources);
    assertEquals(1, metadataSources.size());
    LifecycleMappingMetadataSource metadataSource = metadataSources.get(0);
    assertEquals("testLifecycleMappingMetadata", metadataSource.getGroupId());
    assertEquals("testLifecycleMappingMetadata1", metadataSource.getArtifactId());
    assertEquals("0.0.1", metadataSource.getVersion());

    // Assert lifecycle mappings
    assertNotNull(getLifecycleMappingMetadata(metadataSource, "war"));
    assertEquals("fakeid", getLifecycleMappingMetadata(metadataSource, "war").getLifecycleMappingId());
    assertNull(getLifecycleMappingMetadata(metadataSource, "jar"));

    // Assert mojo/plugin executions
    List<PluginExecutionMetadata> pluginExecutions = metadataSources.get(0).getPluginExecutions();
    assertEquals(3, pluginExecutions.size());
    Set<String> goals = new LinkedHashSet<String>();

    goals.add("compile");
    goals.add("testCompile");
    assertPluginExecutionMetadata("org.apache.maven.plugins", "maven-compiler-plugin", "[2.0,)", goals,
        PluginExecutionAction.configurator, "org.eclipse.m2e.jdt.javaConfigurator", pluginExecutions.get(0));

    goals = new LinkedHashSet<String>();
    goals.add("jar");
    assertPluginExecutionMetadata("org.apache.maven.plugins", "maven-jar-plugin", "[2.0,)", goals,
        PluginExecutionAction.ignore, null, pluginExecutions.get(1));

    goals = new LinkedHashSet<String>();
    goals.add("resources");
    assertPluginExecutionMetadata("org.apache.maven.plugins", "maven-resources-plugin", "[2.0,)", goals,
        PluginExecutionAction.execute, null, pluginExecutions.get(2));
  }

  public void testGetLifecycleMappingMetadataOverride() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
        "testGetLifecycleMappingMetadataOverride/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    MavenExecutionRequest request = mavenProjectManager.createExecutionRequest(facade, monitor);
    List<LifecycleMappingMetadataSource> metadata = LifecycleMappingFactory.getPomMappingMetadataSources(
        facade.getMavenProject(), request, monitor);
    assertNotNull(metadata);
    assertEquals(2, metadata.size());
    assertEquals("testLifecycleMappingMetadata", metadata.get(0).getGroupId());
    assertEquals("testLifecycleMappingMetadata2", metadata.get(0).getArtifactId());
    assertEquals("0.0.2", metadata.get(0).getVersion());
    assertEquals("testLifecycleMappingMetadata", metadata.get(1).getGroupId());
    assertEquals("testLifecycleMappingMetadata1", metadata.get(1).getArtifactId());
    assertEquals("0.0.1", metadata.get(1).getVersion());

    List<PluginExecutionMetadata> pluginExecutions = metadata.get(0).getPluginExecutions();
    assertEquals(2, pluginExecutions.size());
    Set<String> goals = new LinkedHashSet<String>();

    goals.add("compile");
    goals.add("testCompile");
    assertPluginExecutionMetadata("org.apache.maven.plugins", "maven-compiler-plugin", "[3.0,)", goals,
        PluginExecutionAction.configurator, "org.eclipse.m2e.jdt.javaConfigurator", pluginExecutions.get(0));

    goals = new LinkedHashSet<String>();
    goals.add("jar");
    assertPluginExecutionMetadata("org.apache.maven.plugins", "maven-jar-plugin", "[3.0,)", goals,
        PluginExecutionAction.ignore, null, pluginExecutions.get(1));
  }

  public void testProfilesAndProperties() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
        "testProfilesAndProperties/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    MavenExecutionRequest request = mavenProjectManager.createExecutionRequest(facade, monitor);
    List<LifecycleMappingMetadataSource> metadata = LifecycleMappingFactory.getPomMappingMetadataSources(
        facade.getMavenProject(), request, monitor);
    assertNotNull(metadata);
    assertEquals(2, metadata.size());
    assertEquals("testLifecycleMappingMetadata", metadata.get(0).getGroupId());
    assertEquals("testLifecycleMappingMetadata2", metadata.get(0).getArtifactId());
    assertEquals("0.0.2", metadata.get(0).getVersion());

    assertEquals("testLifecycleMappingMetadata", metadata.get(1).getGroupId());
    assertEquals("testLifecycleMappingMetadata1", metadata.get(1).getArtifactId());
    assertEquals("0.0.1", metadata.get(1).getVersion());
  }

  public void testGetLifecycleMappingMetadataMerge() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
        "testGetLifecycleMappingMetadataMerge/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    MavenExecutionRequest request = mavenProjectManager.createExecutionRequest(facade, monitor);
    List<LifecycleMappingMetadataSource> metadata = LifecycleMappingFactory.getPomMappingMetadataSources(
        facade.getMavenProject(), request, monitor);
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

    AbstractCustomizableLifecycleMapping lifecycleMapping = (AbstractCustomizableLifecycleMapping) projectConfigurationManager
        .getLifecycleMapping(facade);

    assertTrue(lifecycleMapping instanceof JarLifecycleMapping);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertEquals(configurators.toString(), 1, configurators.size());
    assertTrue(configurators.get(0) instanceof JavaProjectConfigurator);

    List<MojoExecutionKey> notCoveredMojoExecutions = getNotCoveredMojoExecutions(facade);
    assertEquals(notCoveredMojoExecutions.toString(), 0, notCoveredMojoExecutions.size());

    Map<MojoExecutionKey, List<AbstractBuildParticipant>> buildParticipants = lifecycleMapping.getBuildParticipants(
        facade, monitor);
    assertEquals(8, buildParticipants.size());

    assertBuildParticipantType(buildParticipants, "maven-resources-plugin", MojoExecutionBuildParticipant.class);
    // TODO assert all other mojo executions
  }

  private void assertBuildParticipantType(Map<MojoExecutionKey, List<AbstractBuildParticipant>> buildParticipants,
      String artifactId, Class<MojoExecutionBuildParticipant> participantType) {
    for(Map.Entry<MojoExecutionKey, List<AbstractBuildParticipant>> entry : buildParticipants.entrySet()) {
      if(artifactId.equals(entry.getKey().getArtifactId())) {
        for(AbstractBuildParticipant participant : entry.getValue()) {
          assertTrue(participantType.equals(participant.getClass()));
        }
      }
    }
  }

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
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 2, errorMarkers.size());
    String expectedErrorMessage = "Plugin execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-1 (execution: default-test-goal-1, phase: process-resources)";
    IMarker marker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID,
        expectedErrorMessage, 11 /*lineNumber <plugin> of plugin def*/, project);
    WorkspaceHelpers.assertErrorMarkerAttributes(marker, notCoveredMojoExecutions.get(0));
    expectedErrorMessage = "Plugin execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-2 (execution: default-test-goal-2, phase: compile)";
    marker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID, expectedErrorMessage,
        11 /*lineNumber <plugin> of plugin def*/, project);
    WorkspaceHelpers.assertErrorMarkerAttributes(marker, notCoveredMojoExecutions.get(1));
  }

  public void testMissingMapping() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "missing/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 1, errorMarkers.size());
    String expectedErrorMessage = "Lifecycle mapping \"unknown-or-missing\" is not available. To enable full functionality, install the lifecycle mapping and run Maven->Update Project Configuration.";
    IMarker marker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID,
        expectedErrorMessage, null /*lineNumber*/, project);
    WorkspaceHelpers.assertMarkerLocation(new SourceLocation(7, 3, 13), marker);
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

  public void testUnknownPackagingType() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "unknownPackagingType/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    String expectedErrorMessage = "Unknown or missing lifecycle mapping (project packaging type=\"rar\")";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID, expectedErrorMessage,
        7 /*lineNumber for <packaging>*/, project);
    WorkspaceHelpers.assertLifecyclePackagingErrorMarkerAttributes(project, "rar");

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade);
    assertTrue(lifecycleMapping instanceof InvalidLifecycleMapping);

    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 1, errorMarkers.size());
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID, expectedErrorMessage,
        7 /*lineNumber for <packaging>*/, project);
    WorkspaceHelpers.assertLifecyclePackagingErrorMarkerAttributes(project, "rar");
  }

  public void testNotInterestingPhaseConfigurator() throws Exception {
    IMavenProjectFacade facade = importMavenProject(
        "projects/lifecyclemapping/lifecycleMappingMetadata/testNotInterestingPhaseConfigurator", "pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);

    ILifecycleMapping lifecycleMapping = mavenProjectManager.getLifecycleMapping(facade);

    Map<MojoExecutionKey, List<AbstractBuildParticipant>> buildParticipants = lifecycleMapping.getBuildParticipants(
        facade, monitor);
    assertEquals(1, buildParticipants.size());
    MojoExecutionKey executionKey = buildParticipants.keySet().iterator().next();
    assertEquals("package", executionKey.getLifecyclePhase());
    assertEquals("test-lifecyclemapping-plugin", executionKey.getArtifactId());
    assertEquals(1, buildParticipants.get(executionKey).size());
    assertTrue(buildParticipants.get(executionKey).get(0) instanceof MojoExecutionBuildParticipant);
  }

  public void testDuplicatePackagingTypeMetadata() throws Exception {
    MavenProjectFacade facade = newMavenProjectFacade(
        "projects/lifecyclemapping/lifecycleMappingMetadata/DuplicateMetadata/testDuplicatePackagingType", "pom.xml");

    LifecycleMappingResult mappingResult = LifecycleMappingFactory.calculateLifecycleMapping(plugin.getMaven()
        .createExecutionRequest(monitor), facade, monitor);

    List<MavenProblemInfo> problems = mappingResult.getProblems();

    assertEquals(2, problems.size());
    assertEquals(
        "Conflicting lifecycle mapping metadata (project packaging type=\"test-packaging-a\"). To enable full functionality, remove the conflicting mapping and run Maven->Update Project Configuration.",
        problems.get(0).getMessage());
  }

  public void testDuplicatePluginExecution1() throws Exception {
    MavenProjectFacade facade = newMavenProjectFacade(
        "projects/lifecyclemapping/lifecycleMappingMetadata/DuplicateMetadata/testDuplicatePluginExecution1", "pom.xml");

    LifecycleMappingResult mappingResult = LifecycleMappingFactory.calculateLifecycleMapping(plugin.getMaven()
        .createExecutionRequest(monitor), facade, monitor);

    List<MavenProblemInfo> problems = mappingResult.getProblems();

    assertEquals(4, problems.size());
    assertEquals(
        "Conflicting lifecycle mapping (plugin execution \"org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-1 (execution: default-test-goal-1, phase: process-resources)\"). To enable full functionality, remove the conflicting mapping and run Maven->Update Project Configuration.",
        problems.get(0).getMessage());
    // [1] Conflicting lifecycle mapping (plugin execution "org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-2 (execution: default-test-goal-2, phase: compile)"). To enable full functionality, remove the conflicting mapping and run Maven->Update Project Configuration.
    // [2] Plugin execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-1 (execution: default-test-goal-1, phase: process-resources)
    // [3] Plugin execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-2 (execution: default-test-goal-2, phase: compile)
  }

  public void testDuplicatePluginExecution2() throws Exception {
    MavenProjectFacade facade = newMavenProjectFacade(
        "projects/lifecyclemapping/lifecycleMappingMetadata/DuplicateMetadata/testDuplicatePluginExecution2", "pom.xml");

    LifecycleMappingResult mappingResult = LifecycleMappingFactory.calculateLifecycleMapping(plugin.getMaven()
        .createExecutionRequest(monitor), facade, monitor);

    List<MavenProblemInfo> problems = mappingResult.getProblems();

    assertEquals(3, problems.size());
    assertEquals(
        "Conflicting lifecycle mapping (plugin execution \"org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-1 (execution: default-test-goal-1, phase: process-resources)\"). To enable full functionality, remove the conflicting mapping and run Maven->Update Project Configuration.",
        problems.get(0).getMessage());
    // [1] Plugin execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-1 (execution: default-test-goal-1, phase: process-resources)
    // [2] Plugin execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-2 (execution: default-test-goal-2, phase: compile)
  }

  private MavenProjectFacade newMavenProjectFacade(String dirName, String fileName) throws IOException, CoreException {
    File dir = new File(dirName);
    IProject project = createExisting(dir.getName(), dirName);
    return newMavenProjectFacade(project.getFile(fileName));
  }

  public void testSecondaryConfiguratorsCustomizable() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/secondaryConfigurators/customizable",
        "pom.xml");
    assertNoErrors(facade.getProject());

    AbstractLifecycleMapping lifecycleMapping = (AbstractLifecycleMapping) projectConfigurationManager
        .getLifecycleMapping(facade);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertEquals(3, configurators.size());
  }

  public void testSecondaryConfiguratorsCustom() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/secondaryConfigurators/custom",
        "pom.xml");
    assertNoErrors(facade.getProject());

    AbstractLifecycleMapping lifecycleMapping = (AbstractLifecycleMapping) projectConfigurationManager
        .getLifecycleMapping(facade);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertEquals(3, configurators.size());
  }

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

  public void testIncompatibleVersion() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
        "testIncompatibleVersion/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 2, errorMarkers.size());

    String expectedErrorMessage = "Incompatible lifecycle mapping plugin version 1000.0.0";
    IMarker marker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID,
        expectedErrorMessage,
        null /*lineNumber*/, project);
    WorkspaceHelpers.assertMarkerLocation(new SourceLocation(14, 11, 19), marker);

    expectedErrorMessage = "Unknown or missing lifecycle mapping (project packaging type=\"jar\")";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID, expectedErrorMessage,
        2 /*lineNumber of <project> for cases without local <packaging> section.*/, project);
  }

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
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 2, errorMarkers.size());

    String expectedErrorMessage = "Packaging type test-packaging-a configured in embedded lifecycle mapping configuration does not match the packaging type test-packaging-empty of the current project.";
    IMarker marker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID,
        expectedErrorMessage, null /*lineNumber*/, project);
    WorkspaceHelpers.assertMarkerLocation(new SourceLocation(25, 11, 25), marker);

    expectedErrorMessage = "Unknown or missing lifecycle mapping (project packaging type=\"test-packaging-empty\")";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID, expectedErrorMessage,
        7 /*lineNumber*/, project);
  }

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

  public void testNonresolvableExecutionPlan() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping",
        "testNonresolvableExecutionPlan/pom.xml");

    assertNotNull("Expected not null MavenProjectFacade", facade);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade);
    assertTrue(lifecycleMapping instanceof JarLifecycleMapping);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertEquals(configurators.toString(), 0, configurators.size());
  }

  public void testNondeafaultLifecycles() throws Exception {
    MavenProjectFacade facade = (MavenProjectFacade) importMavenProject(
        "projects/lifecyclemapping/lifecycleMappingMetadata/PluginExecutionActionsTest/testNondefaultLifecycles",
        "pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);

    List<Map.Entry<MojoExecutionKey, List<PluginExecutionMetadata>>> executionMapping = new ArrayList<Map.Entry<MojoExecutionKey, List<PluginExecutionMetadata>>>(
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
}
