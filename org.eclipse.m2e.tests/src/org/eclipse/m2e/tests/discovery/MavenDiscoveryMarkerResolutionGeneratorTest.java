
package org.eclipse.m2e.tests.discovery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.internal.discovery.markers.MavenDiscoveryMarkerResolutionGenerator;
import org.eclipse.m2e.tests.common.AbstractLifecycleMappingTest;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


public class MavenDiscoveryMarkerResolutionGeneratorTest extends AbstractLifecycleMappingTest {

  IMarkerResolutionGenerator2 generator;

  public void setUp() throws Exception {
    super.setUp();
    generator = new MavenDiscoveryMarkerResolutionGenerator();
  }

  @Test
  public void testCanResolveConfiguratorMarker() throws Exception {
    LifecycleMappingMetadataSource defaultMetadata = loadLifecycleMappingMetadataSource("projects/discovery/defaultMetadata.xml");
    LifecycleMappingFactory.setDefaultLifecycleMappingMetadataSource(defaultMetadata);

    IMavenProjectFacade facade = importMavenProject("projects/discovery", "configurator/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();

    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertEquals("Error markers", 1, errorMarkers.size());

    String expectedErrorMessage = "Project configurator \"no such project configurator id for test-lifecyclemapping-plugin:test-goal-for-eclipse-extension2 - embedded from pom\" required by plugin execution \"org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-for-eclipse-extension2 (execution: default-test-goal-for-eclipse-extension2, phase: compile)\" is not available. To enable full functionality, install the project configurator and run Maven->Update Project Configuration.";
    IMarker marker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID,
        expectedErrorMessage, null /*lineNumber*/, project);
    WorkspaceHelpers
        .assertConfiguratorErrorMarkerAttributes(marker,
            "no such project configurator id for test-lifecyclemapping-plugin:test-goal-for-eclipse-extension2 - embedded from pom");
    assertTrue("Resolve configurator marker", generator.hasResolutions(marker));
    IMarkerResolution[] resolutions = generator.getResolutions(marker);
    assertEquals(1, resolutions.length);
  }

  @Test
  public void testCanResolveLifecycleIdMarker() throws Exception {
    LifecycleMappingMetadataSource defaultMetadata = loadLifecycleMappingMetadataSource("projects/discovery/defaultMetadata.xml");
    LifecycleMappingFactory.setDefaultLifecycleMappingMetadataSource(defaultMetadata);

    IMavenProjectFacade facade = importMavenProject("projects/discovery", "lifecycleId/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();

    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertEquals("Error markers :" + toString(errorMarkers), 1, errorMarkers.size());

    String expectedErrorMessage = "Lifecycle mapping \"lifecycleId\" is not available. To enable full functionality, install the lifecycle mapping and run Maven->Update Project Configuration.";
    IMarker marker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID,
        expectedErrorMessage, null /*lineNumber*/, project);
    WorkspaceHelpers.assertLifecycleIdErrorMarkerAttributes(marker, "lifecycleId");
    assertTrue("Resolve packaging marker", generator.hasResolutions(marker));
  }

  @Test
  public void testCanResolveMojoExecutionMarker() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/discovery", "mojoExecutions/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();

    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertEquals("Error markers", 2, errorMarkers.size());

    List<MojoExecutionKey> notCoveredMojoExecutions = getNotCoveredMojoExecutions(facade);

    String expectedErrorMessage = "Plugin execution not covered by lifecycle configuration: org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0:test-goal-1 (execution: default-test-goal-1, phase: process-resources)";
    IMarker marker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_LIFECYCLEMAPPING_ID,
        expectedErrorMessage, null /*lineNumber*/, project);
    WorkspaceHelpers.assertErrorMarkerAttributes(marker, notCoveredMojoExecutions.get(0));
    assertTrue("Resolve MojoExecution marker", generator.hasResolutions(marker));
  }

  @Test
  public void testResolveMultipleMarkers() throws Exception {
    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject[] projects = importProjects("projects/discovery", new String[] {"configurator/pom.xml",
        "lifecycleId/pom.xml", "mojoExecutions/pom.xml"}, configuration);
    waitForJobsToComplete();

    List<IMarker> errorMarkers = new ArrayList<IMarker>();
    for(IProject project : projects) {
      IMavenProjectFacade facade = mavenProjectManager.create(project, monitor);
      IProject p = facade.getProject();
      errorMarkers.addAll(WorkspaceHelpers.findErrorMarkers(p));
    }

    IMarkerResolution m = generator.getResolutions(errorMarkers.get(0))[0];

    assertTrue(m instanceof WorkbenchMarkerResolution);

    IMarker[] resolvable = ((WorkbenchMarkerResolution) m).findOtherMarkers(errorMarkers
        .toArray(new IMarker[errorMarkers.size()]));
    //Two fewer marker than the total otherwise the marker used to generate the resolution will be shown twice
    //we have a MavenDiscoveryMarkerResolution instance per resource because of 335299, 335490
    //that means we cannot pinpoint exactly the ONE marker that is associated with the findOtherMarkers() call,
    //so we exclude all associated with the file. thus 6->5 in the assert
    assertEquals(3, resolvable.length);
    assertFalse(Arrays.asList(resolvable).contains(errorMarkers.get(0)));
  }
}
