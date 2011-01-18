
package org.eclipse.m2e.tests.lifecycle;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.internal.lifecycle.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecycle.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.tests.common.AbstractLifecycleMappingTest;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;
import org.eclipse.m2e.tests.configurators.TestLifecycleMapping;


public class PluginExecutionMetadataPrioritiesTest extends AbstractLifecycleMappingTest {
  public void testDefaultMetadataSource() throws Exception {
    LifecycleMappingMetadataSource defaultMetadata = loadLifecycleMappingMetadataSource("projects/lifecyclemapping/lifecycleMappingMetadata/PluginExecutionMetadataPrioritiesTest/defaultMetadata.xml");
    LifecycleMappingFactory.setDefaultLifecycleMappingMetadataSource(defaultMetadata);

    IMavenProjectFacade facade = importMavenProject(
        "projects/lifecyclemapping/lifecycleMappingMetadata/PluginExecutionMetadataPrioritiesTest",
        "testDefaultMetadataSource/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 2, errorMarkers.size());

    String expectedErrorMessage = "Mojo execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-1 {execution: default-test-goal-1} (maven lifecycle phase: process-resources)";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(0));

    expectedErrorMessage = "Project configurator \"missing default project configurator id for test-lifecyclemapping-plugin:test-goal-1\" is not available. To enable full functionality, install the project configurator and run Maven->Update Project Configuration.";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(1));
  }

  /**
   * This test verifies that pluginExecution mapping metadata contributed via eclipse extension point takes preference
   * over default metadata.
   */
  public void testEclipseExtension() throws Exception {
    LifecycleMappingMetadataSource defaultMetadata = loadLifecycleMappingMetadataSource("projects/lifecyclemapping/lifecycleMappingMetadata/PluginExecutionMetadataPrioritiesTest/defaultMetadata.xml");
    LifecycleMappingFactory.setDefaultLifecycleMappingMetadataSource(defaultMetadata);

    IMavenProjectFacade facade = importMavenProject(
        "projects/lifecyclemapping/lifecycleMappingMetadata/PluginExecutionMetadataPrioritiesTest",
        "testEclipseExtension/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    ILifecycleMapping lifecycleMapping = facade.getLifecycleMapping(monitor);
    assertNotNull(lifecycleMapping);
    assertTrue(lifecycleMapping.getClass().getCanonicalName(), lifecycleMapping instanceof TestLifecycleMapping);
  }

  // Referenced metadata has priority over eclipse extensions
  public void testReferencedFromPom() throws Exception {
    LifecycleMappingMetadataSource defaultMetadata = loadLifecycleMappingMetadataSource("projects/lifecyclemapping/lifecycleMappingMetadata/PluginExecutionMetadataPrioritiesTest/defaultMetadata.xml");
    LifecycleMappingFactory.setDefaultLifecycleMappingMetadataSource(defaultMetadata);

    IMavenProjectFacade facade = importMavenProject(
        "projects/lifecyclemapping/lifecycleMappingMetadata/PluginExecutionMetadataPrioritiesTest",
        "testReferencedFromPom/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 2, errorMarkers.size());

    String expectedErrorMessage = "Mojo execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-for-eclipse-extension2 {execution: default-test-goal-for-eclipse-extension2} (maven lifecycle phase: compile)";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(0));

    expectedErrorMessage = "Project configurator \"no such project configurator id for test-lifecyclemapping-plugin:test-goal-for-eclipse-extension2 - referenced from pom\" is not available. To enable full functionality, install the project configurator and run Maven->Update Project Configuration.";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(1));
  }

  // Embedded metadata has priority over referenced metadata
  public void testEmbeddedInPom() throws Exception {
    LifecycleMappingMetadataSource defaultMetadata = loadLifecycleMappingMetadataSource("projects/lifecyclemapping/lifecycleMappingMetadata/PluginExecutionMetadataPrioritiesTest/defaultMetadata.xml");
    LifecycleMappingFactory.setDefaultLifecycleMappingMetadataSource(defaultMetadata);

    IMavenProjectFacade facade = importMavenProject(
        "projects/lifecyclemapping/lifecycleMappingMetadata/PluginExecutionMetadataPrioritiesTest",
        "testEmbeddedInPom/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 2, errorMarkers.size());

    String expectedErrorMessage = "Mojo execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-for-eclipse-extension2 {execution: default-test-goal-for-eclipse-extension2} (maven lifecycle phase: compile)";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(0));

    expectedErrorMessage = "Project configurator \"no such project configurator id for test-lifecyclemapping-plugin:test-goal-for-eclipse-extension2 - embedded from pom\" is not available. To enable full functionality, install the project configurator and run Maven->Update Project Configuration.";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(1));
  }
}
