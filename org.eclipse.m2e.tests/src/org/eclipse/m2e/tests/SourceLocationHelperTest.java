
package org.eclipse.m2e.tests;

import java.util.List;

import org.eclipse.core.resources.IProject;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.internal.markers.SourceLocationHelper;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


@SuppressWarnings("restriction")
public class SourceLocationHelperTest extends AbstractMavenProjectTestCase {
  private MavenProjectManager mavenProjectManager;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    mavenProjectManager = MavenPlugin.getDefault().getMavenProjectManager();
    //projectConfigurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
  }

  @Override
  protected void tearDown() throws Exception {
    //projectConfigurationManager = null;
    mavenProjectManager = null;

    super.tearDown();
  }

  public void testPackagingLocation() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/markers/SourceLocationHelperTest/testPackagingLocation",
        "pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);
    MavenProject mavenProject = facade.getMavenProject();
    SourceLocation markerLocation = SourceLocationHelper.findPackagingLocation(mavenProject);
    assertMarkerLocation(new SourceLocation(7, 3, 13), markerLocation);
  }

  public void testPluginAttributeLocation() throws Exception {
    IMavenProjectFacade facade = importMavenProject(
        "projects/markers/SourceLocationHelperTest/testPluginAttributeLocation",
        "pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);
    MavenProject mavenProject = facade.getMavenProject();
    String pomPath = mavenProject.getFile().getAbsolutePath();
    String pomId = WorkspaceHelpers.getModelId(mavenProject);
    Plugin plugin = mavenProject.getPlugin("org.apache.maven.plugins:maven-install-plugin");
    SourceLocation markerLocation = SourceLocationHelper.findLocation(plugin, "version");
    assertMarkerLocation(new SourceLocation(pomPath, pomId, 14, 9, 17), markerLocation);
    markerLocation = SourceLocationHelper.findLocation(plugin, "xxx");
    assertMarkerLocation(new SourceLocation(pomPath, pomId, 11, 7, 14), markerLocation);
  }

  public void testMojoExecutionLocationWithExecutions() throws Exception {
    IMavenProjectFacade facade = importMavenProject(
        "projects/markers/SourceLocationHelperTest/testMojoExecutionLocationWithExecutions", "parent1/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);
    MavenProject parent1MavenProject = facade.getMavenProject();

    facade = importMavenProject("projects/markers/SourceLocationHelperTest/testMojoExecutionLocationWithExecutions",
        "parent1/parent2/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);
    MavenProject parent2MavenProject = facade.getMavenProject();

    facade = importMavenProject("projects/markers/SourceLocationHelperTest/testMojoExecutionLocationWithExecutions",
        "parent1/parent2/child/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);
    MavenProject childMavenProject = facade.getMavenProject();

    MojoExecutionKey mojoExecutionKey = getMojoExecutionKey("org.eclipse.m2e.test.lifecyclemapping",
        "test-lifecyclemapping-plugin", "default-test-goal-1", ((MavenProjectFacade) facade).getMojoExecutions());
    SourceLocation markerLocation = SourceLocationHelper.findLocation(childMavenProject, mojoExecutionKey);
    assertMarkerLocation(new SourceLocation(16, 7, 14), markerLocation);
    mojoExecutionKey = getMojoExecutionKey("org.eclipse.m2e.test.lifecyclemapping", "test-lifecyclemapping-plugin",
        "default-test-goal-2", ((MavenProjectFacade) facade).getMojoExecutions());
    markerLocation = SourceLocationHelper.findLocation(childMavenProject, mojoExecutionKey);
    assertMarkerLocation(new SourceLocation(16, 7, 14), markerLocation);
    mojoExecutionKey = getMojoExecutionKey("org.eclipse.m2e.test.lifecyclemapping", "test-lifecyclemapping-plugin",
        "parent2Execution", ((MavenProjectFacade) facade).getMojoExecutions());
    markerLocation = SourceLocationHelper.findLocation(childMavenProject, mojoExecutionKey);
    SourceLocation causeLocation = new SourceLocation(parent2MavenProject.getFile().getAbsolutePath(),
        WorkspaceHelpers.getModelId(parent2MavenProject), 21, 11, 21);
    assertMarkerLocation(new SourceLocation(5, 3, 10, causeLocation), markerLocation);
    mojoExecutionKey = getMojoExecutionKey("org.eclipse.m2e.test.lifecyclemapping", "test-lifecyclemapping-plugin",
        "childExecution", ((MavenProjectFacade) facade).getMojoExecutions());
    markerLocation = SourceLocationHelper.findLocation(childMavenProject, mojoExecutionKey);
    assertMarkerLocation(new SourceLocation(21, 11, 21), markerLocation);
  }

  public void testMojoExecutionLocation() throws Exception {
    IMavenProjectFacade facade = importMavenProject(
        "projects/markers/SourceLocationHelperTest/testMojoExecutionLocation", "parent/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);
    MavenProject parentMavenProject = facade.getMavenProject();
    // Plugin from maven lifecycle
    MojoExecutionKey mojoExecutionKey = getMojoExecutionKey("org.apache.maven.plugins", "maven-clean-plugin",
        ((MavenProjectFacade) facade).getMojoExecutions());
    SourceLocation markerLocation = SourceLocationHelper.findLocation(parentMavenProject, mojoExecutionKey);
    assertMarkerLocation(new SourceLocation(7, 3, 13), markerLocation);
    // Plugin from current pom
    mojoExecutionKey = getMojoExecutionKey("org.apache.maven.plugins", "maven-install-plugin",
        ((MavenProjectFacade) facade).getMojoExecutions());
    markerLocation = SourceLocationHelper.findLocation(parentMavenProject, mojoExecutionKey);
    assertMarkerLocation(new SourceLocation(11, 7, 14), markerLocation);

    facade = importMavenProject("projects/markers/SourceLocationHelperTest/testMojoExecutionLocation",
        "parent/child/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);
    MavenProject mavenProject = facade.getMavenProject();
    // Plugin from maven lifecycle
    mojoExecutionKey = getMojoExecutionKey("org.apache.maven.plugins", "maven-clean-plugin",
        ((MavenProjectFacade) facade).getMojoExecutions());
    markerLocation = SourceLocationHelper.findLocation(mavenProject, mojoExecutionKey);
    assertMarkerLocation(new SourceLocation(2, 1, 9), markerLocation);
    // Plugin from current pom
    mojoExecutionKey = getMojoExecutionKey("org.apache.maven.plugins", "maven-compiler-plugin",
        ((MavenProjectFacade) facade).getMojoExecutions());
    markerLocation = SourceLocationHelper.findLocation(mavenProject, mojoExecutionKey);
    assertMarkerLocation(new SourceLocation(15, 7, 14), markerLocation);
    // Plugin from parent pom
    mojoExecutionKey = getMojoExecutionKey("org.apache.maven.plugins", "maven-install-plugin",
        ((MavenProjectFacade) facade).getMojoExecutions());
    markerLocation = SourceLocationHelper.findLocation(mavenProject, mojoExecutionKey);
    assertMarkerLocation(new SourceLocation(5, 3, 10, new SourceLocation(
        parentMavenProject.getFile().getAbsolutePath(), WorkspaceHelpers.getModelId(parentMavenProject), 11, 7, 14)),
        markerLocation);
  }

  private void assertMarkerLocation(SourceLocation expected, SourceLocation actual) {
    assertNotNull("Expected not null MarkerLocation", actual);
    assertEquals("Wrong MarkerLocation.resourcePath", expected.getResourcePath(), actual.getResourcePath());
    assertEquals("Wrong MarkerLocation.lineNumber", expected.getLineNumber(), actual.getLineNumber());
    assertEquals("Wrong MarkerLocation.columnStart", expected.getColumnStart(), actual.getColumnStart());
    assertEquals("Wrong MarkerLocation.columnEnd", expected.getColumnEnd(), actual.getColumnEnd());

    if(expected.getLinkedLocation() == null) {
      assertNull("Expected null cause location", actual.getLinkedLocation());
    } else {
      assertMarkerLocation(expected.getLinkedLocation(), actual.getLinkedLocation());
    }
  }

  private MojoExecutionKey getMojoExecutionKey(String groupId, String artifactId, List<MojoExecution> mojoExecutions) {
    return getMojoExecutionKey(groupId, artifactId, null /*executionId*/, mojoExecutions);
  }

  private MojoExecutionKey getMojoExecutionKey(String groupId, String artifactId, String executionId,
      List<MojoExecution> mojoExecutions) {
    for(MojoExecution mojoExecution : mojoExecutions) {
      if(groupId.equals(mojoExecution.getGroupId()) && artifactId.equals(mojoExecution.getArtifactId())) {
        if(executionId == null || executionId.equals(mojoExecution.getExecutionId())) {
          return new MojoExecutionKey(mojoExecution);
        }
      }
    }
    return null;
  }

  private IMavenProjectFacade importMavenProject(String basedir, String pomName) throws Exception {
    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject[] project = importProjects(basedir, new String[] {pomName}, configuration);
    waitForJobsToComplete();

    return mavenProjectManager.create(project[0], monitor);
  }
}
