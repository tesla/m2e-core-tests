
package org.eclipse.m2e.tests.lifecycle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.internal.lifecycle.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecycle.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecycle.model.io.xpp3.LifecycleMappingMetadataSourceXpp3Reader;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.jdt.internal.JarLifecycleMapping;
import org.eclipse.m2e.tests.common.AbstractLifecycleMappingTest;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


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

    String expectedErrorMessage = "Lifecycle mapping \"no such lifecycle mapping for rar\" is not available. To enable full functionality, install the lifecycle mapping and run Maven->Update Project Configuration.";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(0));
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
    assertTrue(lifecycleMapping.getClass().getCanonicalName(), lifecycleMapping instanceof JarLifecycleMapping);
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

    String expectedErrorMessage = "Lifecycle mapping \"no such lifecycle mapping for jar - referenced from pom\" is not available. To enable full functionality, install the lifecycle mapping and run Maven->Update Project Configuration.";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(0));
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

    String expectedErrorMessage = "Lifecycle mapping \"no such lifecycle mapping for jar - embedded in pom\" is not available. To enable full functionality, install the lifecycle mapping and run Maven->Update Project Configuration.";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, errorMarkers.get(0));
  }

  private LifecycleMappingMetadataSource loadLifecycleMappingMetadataSource(String metadataFilename)
      throws IOException, XmlPullParserException {
    File metadataFile = new File(metadataFilename);
    assertTrue("File does not exist:" + metadataFile.getAbsolutePath(), metadataFile.exists());
    InputStream in = new FileInputStream(metadataFile);
    try {
      LifecycleMappingMetadataSource lifecycleMappingMetadataSource = new LifecycleMappingMetadataSourceXpp3Reader()
          .read(in);
      return lifecycleMappingMetadataSource;
    } finally {
      IOUtil.close(in);
    }
  }
}
