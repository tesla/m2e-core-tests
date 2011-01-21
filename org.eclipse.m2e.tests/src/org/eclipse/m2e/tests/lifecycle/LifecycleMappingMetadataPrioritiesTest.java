
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


public class LifecycleMappingMetadataPrioritiesTest extends AbstractLifecycleMappingTest {
  // Tests lifecycle mapping declared in default lifecycle mapping metadata
  public void testDefaultMetadataSource() throws Exception {
    LifecycleMappingMetadataSource defaultMetadata = loadLifecycleMappingMetadataSource("projects/lifecyclemapping/lifecycleMappingMetadata/LifecycleMappingMetadataPrioritiesTest/defaultMetadata.xml");
    LifecycleMappingFactory.setDefaultLifecycleMappingMetadataSource(defaultMetadata);

    IMavenProjectFacade facade = importMavenProject(
        "projects/lifecyclemapping/lifecycleMappingMetadata/LifecycleMappingMetadataPrioritiesTest",
        "testDefaultMetadataSource/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 2, errorMarkers.size());

    String expectedErrorMessage = "Lifecycle mapping \"missing default lifecycle mapping id for test-packaging-empty\" is not available. To enable full functionality, install the lifecycle mapping and run Maven->Update Project Configuration.";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(0));
    WorkspaceHelpers.assertLifecycleIdErrorMarkerAttributes(errorMarkers.get(0),
        "missing default lifecycle mapping id for test-packaging-empty");

    expectedErrorMessage = "Unknown or missing lifecycle mapping (project packaging type=\"test-packaging-empty\")";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(1));
    WorkspaceHelpers.assertLifecyclePackagingErrorMarkerAttributes(errorMarkers.get(1), "test-packaging-empty");
  }

  // Tests that a lifecycle mapping declared in an eclipse extension has priority over default lifecycle mapping metadata
  public void testEclipseExtension() throws Exception {
    LifecycleMappingMetadataSource defaultMetadata = loadLifecycleMappingMetadataSource("projects/lifecyclemapping/lifecycleMappingMetadata/LifecycleMappingMetadataPrioritiesTest/defaultMetadata.xml");
    LifecycleMappingFactory.setDefaultLifecycleMappingMetadataSource(defaultMetadata);

    IMavenProjectFacade facade = importMavenProject(
        "projects/lifecyclemapping/lifecycleMappingMetadata/LifecycleMappingMetadataPrioritiesTest",
        "testEclipseExtension/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    ILifecycleMapping lifecycleMapping = facade.getLifecycleMapping(monitor);
    assertNotNull(lifecycleMapping);
    assertTrue(lifecycleMapping.getClass().getCanonicalName(), lifecycleMapping instanceof TestLifecycleMapping);
  }

  // Tests that a lifecycle mapping declared in a metadata source referenced from pom has priority over eclipse extension
  public void testReferencedFromPom() throws Exception {
    LifecycleMappingMetadataSource defaultMetadata = loadLifecycleMappingMetadataSource("projects/lifecyclemapping/lifecycleMappingMetadata/LifecycleMappingMetadataPrioritiesTest/defaultMetadata.xml");
    LifecycleMappingFactory.setDefaultLifecycleMappingMetadataSource(defaultMetadata);

    IMavenProjectFacade facade = importMavenProject(
        "projects/lifecyclemapping/lifecycleMappingMetadata/LifecycleMappingMetadataPrioritiesTest",
        "testReferencedFromPom/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 2, errorMarkers.size());

    String expectedErrorMessage = "Lifecycle mapping \"no such lifecycle mapping for test-packaging-for-eclipse-extension - referenced from pom\" is not available. To enable full functionality, install the lifecycle mapping and run Maven->Update Project Configuration.";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(0));
    WorkspaceHelpers.assertLifecycleIdErrorMarkerAttributes(errorMarkers.get(0),
        "no such lifecycle mapping for test-packaging-for-eclipse-extension - referenced from pom");

    expectedErrorMessage = "Unknown or missing lifecycle mapping (project packaging type=\"test-packaging-for-eclipse-extension\")";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(1));
    WorkspaceHelpers.assertLifecyclePackagingErrorMarkerAttributes(errorMarkers.get(1),
        "test-packaging-for-eclipse-extension");
  }

  // Tests that a lifecycle mapping embedded in pom has priority over lifecycle mapping declared in a metadata source referenced from pom
  public void testEmbeddedInPom() throws Exception {
    LifecycleMappingMetadataSource defaultMetadata = loadLifecycleMappingMetadataSource("projects/lifecyclemapping/lifecycleMappingMetadata/LifecycleMappingMetadataPrioritiesTest/defaultMetadata.xml");
    LifecycleMappingFactory.setDefaultLifecycleMappingMetadataSource(defaultMetadata);

    IMavenProjectFacade facade = importMavenProject(
        "projects/lifecyclemapping/lifecycleMappingMetadata/LifecycleMappingMetadataPrioritiesTest",
        "testEmbeddedInPom/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 2, errorMarkers.size());

    String expectedErrorMessage = "Lifecycle mapping \"no such lifecycle mapping for test-packaging-for-eclipse-extension - embedded in pom\" is not available. To enable full functionality, install the lifecycle mapping and run Maven->Update Project Configuration.";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(0));
    WorkspaceHelpers.assertLifecycleIdErrorMarkerAttributes(errorMarkers.get(0),
        "no such lifecycle mapping for test-packaging-for-eclipse-extension - embedded in pom");

    expectedErrorMessage = "Unknown or missing lifecycle mapping (project packaging type=\"test-packaging-for-eclipse-extension\")";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(1));
    WorkspaceHelpers.assertLifecyclePackagingErrorMarkerAttributes(errorMarkers.get(1),
        "test-packaging-for-eclipse-extension");
  }

  public void testParent() throws Exception {
    IMavenProjectFacade facade = importMavenProject(
        "projects/lifecyclemapping/lifecycleMappingMetadata/LifecycleMappingMetadataPrioritiesTest",
        "testParent/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    facade = importMavenProject(
        "projects/lifecyclemapping/lifecycleMappingMetadata/LifecycleMappingMetadataPrioritiesTest",
        "testParent/useParent/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    project = facade.getProject();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 2, errorMarkers.size());
    String expectedErrorMessage = "Lifecycle mapping \"no such lifecycle mapping for test-packaging-a - parent\" is not available. To enable full functionality, install the lifecycle mapping and run Maven->Update Project Configuration.";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(0));
    WorkspaceHelpers.assertLifecycleIdErrorMarkerAttributes(errorMarkers.get(0),
        "no such lifecycle mapping for test-packaging-a - parent");
    expectedErrorMessage = "Unknown or missing lifecycle mapping (project packaging type=\"test-packaging-a\")";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(1));
    WorkspaceHelpers.assertLifecyclePackagingErrorMarkerAttributes(errorMarkers.get(1), "test-packaging-a");

    facade = importMavenProject(
        "projects/lifecyclemapping/lifecycleMappingMetadata/LifecycleMappingMetadataPrioritiesTest",
        "testParent/overrideParent/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    project = facade.getProject();
    errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 2, errorMarkers.size());
    expectedErrorMessage = "Lifecycle mapping \"no such lifecycle mapping for test-packaging-a - override\" is not available. To enable full functionality, install the lifecycle mapping and run Maven->Update Project Configuration.";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(0));
    WorkspaceHelpers.assertLifecycleIdErrorMarkerAttributes(errorMarkers.get(0),
        "no such lifecycle mapping for test-packaging-a - override");
    expectedErrorMessage = "Unknown or missing lifecycle mapping (project packaging type=\"test-packaging-a\")";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(1));
    WorkspaceHelpers.assertLifecyclePackagingErrorMarkerAttributes(errorMarkers.get(1), "test-packaging-a");
  }
}
