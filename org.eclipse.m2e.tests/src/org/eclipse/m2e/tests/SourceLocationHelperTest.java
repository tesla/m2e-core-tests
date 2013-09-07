
package org.eclipse.m2e.tests;

import java.util.List;

import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.DefaultArtifactTypeRegistry;
import org.eclipse.core.resources.IProject;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.internal.markers.SourceLocationHelper;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.RequireMavenExecutionContext;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


@SuppressWarnings("restriction")
public class SourceLocationHelperTest extends AbstractMavenProjectTestCase {
  private IMavenProjectRegistry mavenProjectManager;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    mavenProjectManager = MavenPlugin.getMavenProjectRegistry();
    //projectConfigurationManager = MavenPlugin.getProjectConfigurationManager();
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      //projectConfigurationManager = null;
      mavenProjectManager = null;
    } finally {
      super.tearDown();
    }
  }

  public void testPackagingLocation() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/markers/SourceLocationHelperTest/testPackagingLocation",
        "pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);
    MavenProject mavenProject = facade.getMavenProject(monitor);
    SourceLocation sourceLocation = SourceLocationHelper.findPackagingLocation(mavenProject);
    assertSourceLocation(new SourceLocation(7, 3, 13), sourceLocation);
  }

  public void testPluginAttributeLocation() throws Exception {
    IMavenProjectFacade facade = importMavenProject(
        "projects/markers/SourceLocationHelperTest/testPluginAttributeLocation", "pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);
    MavenProject mavenProject = facade.getMavenProject(monitor);
    String pomPath = mavenProject.getFile().getAbsolutePath();
    String pomId = WorkspaceHelpers.getModelId(mavenProject);
    Plugin plugin = mavenProject.getPlugin("org.apache.maven.plugins:maven-install-plugin");
    SourceLocation sourceLocation = SourceLocationHelper.findLocation(plugin, "version");
    assertSourceLocation(new SourceLocation(pomPath, pomId, 14, 9, 17), sourceLocation);
    sourceLocation = SourceLocationHelper.findLocation(plugin, "xxx");
    assertSourceLocation(new SourceLocation(pomPath, pomId, 11, 7, 14), sourceLocation);
  }

  @RequireMavenExecutionContext
  public void testMojoExecutionLocationWithExecutions() throws Exception {
    IMavenProjectFacade facade = importMavenProject(
        "projects/markers/SourceLocationHelperTest/testMojoExecutionLocationWithExecutions", "parent1/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);

    facade = importMavenProject("projects/markers/SourceLocationHelperTest/testMojoExecutionLocationWithExecutions",
        "parent1/parent2/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);
    MavenProject parent2MavenProject = facade.getMavenProject(monitor);

    facade = importMavenProject("projects/markers/SourceLocationHelperTest/testMojoExecutionLocationWithExecutions",
        "parent1/parent2/child/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);
    MavenProject childMavenProject = facade.getMavenProject(monitor);

    MojoExecutionKey mojoExecutionKey = getMojoExecutionKey("org.eclipse.m2e.test.lifecyclemapping",
        "test-lifecyclemapping-plugin", "default-test-goal-1", ((MavenProjectFacade) facade).getMojoExecutions());
    SourceLocation sourceLocation = SourceLocationHelper.findLocation(childMavenProject, mojoExecutionKey);
    assertSourceLocation(new SourceLocation(16, 7, 14), sourceLocation);
    mojoExecutionKey = getMojoExecutionKey("org.eclipse.m2e.test.lifecyclemapping", "test-lifecyclemapping-plugin",
        "default-test-goal-2", ((MavenProjectFacade) facade).getMojoExecutions());
    sourceLocation = SourceLocationHelper.findLocation(childMavenProject, mojoExecutionKey);
    assertSourceLocation(new SourceLocation(16, 7, 14), sourceLocation);
    mojoExecutionKey = getMojoExecutionKey("org.eclipse.m2e.test.lifecyclemapping", "test-lifecyclemapping-plugin",
        "parent2Execution", ((MavenProjectFacade) facade).getMojoExecutions());
    sourceLocation = SourceLocationHelper.findLocation(childMavenProject, mojoExecutionKey);
    SourceLocation causeLocation = new SourceLocation(parent2MavenProject.getFile().getAbsolutePath(),
        WorkspaceHelpers.getModelId(parent2MavenProject), 21, 11, 21);
    assertSourceLocation(new SourceLocation(5, 3, 10, causeLocation), sourceLocation);
    mojoExecutionKey = getMojoExecutionKey("org.eclipse.m2e.test.lifecyclemapping", "test-lifecyclemapping-plugin",
        "childExecution", ((MavenProjectFacade) facade).getMojoExecutions());
    sourceLocation = SourceLocationHelper.findLocation(childMavenProject, mojoExecutionKey);
    assertSourceLocation(new SourceLocation(21, 11, 21), sourceLocation);
  }

  @RequireMavenExecutionContext
  public void testMojoExecutionLocation() throws Exception {
    IMavenProjectFacade facade = importMavenProject(
        "projects/markers/SourceLocationHelperTest/testMojoExecutionLocation", "parent/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);
    MavenProject parentMavenProject = facade.getMavenProject(monitor);
    // Plugin from maven lifecycle
    MojoExecutionKey mojoExecutionKey = getMojoExecutionKey("org.apache.maven.plugins", "maven-clean-plugin",
        ((MavenProjectFacade) facade).getMojoExecutions());
    SourceLocation sourceLocation = SourceLocationHelper.findLocation(parentMavenProject, mojoExecutionKey);
    assertSourceLocation(new SourceLocation(13, 3, 13), sourceLocation);
    // Plugin from current pom
    mojoExecutionKey = getMojoExecutionKey("org.apache.maven.plugins", "maven-install-plugin",
        ((MavenProjectFacade) facade).getMojoExecutions());
    sourceLocation = SourceLocationHelper.findLocation(parentMavenProject, mojoExecutionKey);
    assertSourceLocation(new SourceLocation(17, 7, 14), sourceLocation);

    facade = importMavenProject("projects/markers/SourceLocationHelperTest/testMojoExecutionLocation",
        "parent/child/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    project = facade.getProject();
    WorkspaceHelpers.assertNoErrors(project);
    MavenProject mavenProject = facade.getMavenProject(monitor);
    // Plugin from maven lifecycle
    mojoExecutionKey = getMojoExecutionKey("org.apache.maven.plugins", "maven-clean-plugin",
        ((MavenProjectFacade) facade).getMojoExecutions());
    sourceLocation = SourceLocationHelper.findLocation(mavenProject, mojoExecutionKey);
    assertSourceLocation(new SourceLocation(2, 1, 9), sourceLocation);
    // Plugin from current pom
    mojoExecutionKey = getMojoExecutionKey("org.apache.maven.plugins", "maven-compiler-plugin",
        ((MavenProjectFacade) facade).getMojoExecutions());
    sourceLocation = SourceLocationHelper.findLocation(mavenProject, mojoExecutionKey);
    assertSourceLocation(new SourceLocation(15, 7, 14), sourceLocation);
    // Plugin from parent pom
    mojoExecutionKey = getMojoExecutionKey("org.apache.maven.plugins", "maven-install-plugin",
        ((MavenProjectFacade) facade).getMojoExecutions());
    sourceLocation = SourceLocationHelper.findLocation(mavenProject, mojoExecutionKey);
    assertSourceLocation(new SourceLocation(5, 3, 10, new SourceLocation(
        parentMavenProject.getFile().getAbsolutePath(), WorkspaceHelpers.getModelId(parentMavenProject), 17, 7, 14)),
        sourceLocation);
  }

  public void testDependencyLocation() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/markers/SourceLocationHelperTest/testDependencyLocation",
        "parent/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    MavenProject parentMavenProject = facade.getMavenProject(monitor);
    // Dependency from current pom
    Dependency dependency = getDependency("missing", "missing-parent1", parentMavenProject);
    SourceLocation sourceLocation = SourceLocationHelper.findLocation(parentMavenProject, dependency);
    assertSourceLocation(new SourceLocation(16, 5, 16), sourceLocation);
    dependency = getDependency("missing", "missing-parent2", parentMavenProject);
    sourceLocation = SourceLocationHelper.findLocation(parentMavenProject, dependency);
    assertSourceLocation(new SourceLocation(21, 5, 16), sourceLocation);

    facade = importMavenProject("projects/markers/SourceLocationHelperTest/testDependencyLocation",
        "parent/child/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    MavenProject mavenProject = facade.getMavenProject(monitor);
    // Dependency from current pom
    dependency = getDependency("missing", "missing-parent2", mavenProject);
    sourceLocation = SourceLocationHelper.findLocation(mavenProject, dependency);
    assertSourceLocation(new SourceLocation(14, 5, 16), sourceLocation);
    dependency = getDependency("missing", "missing-child", mavenProject);
    sourceLocation = SourceLocationHelper.findLocation(mavenProject, dependency);
    assertSourceLocation(new SourceLocation(19, 5, 16), sourceLocation);
    // Dependency from parent pom
    dependency = getDependency("missing", "missing-parent1", mavenProject);
    sourceLocation = SourceLocationHelper.findLocation(mavenProject, dependency);
    SourceLocation cause = new SourceLocation(parentMavenProject.getFile().getAbsolutePath(),
        WorkspaceHelpers.getModelId(parentMavenProject), 16, 5, 16);
    assertSourceLocation(new SourceLocation(5, 3, 10, cause), sourceLocation);
  }

  private void assertSourceLocation(SourceLocation expected, SourceLocation actual) {
    assertNotNull("Expected not null SourceLocation", actual);
    assertEquals("Wrong SourceLocation.resourcePath", expected.getResourcePath(), actual.getResourcePath());
    assertEquals("Wrong SourceLocation.lineNumber", expected.getLineNumber(), actual.getLineNumber());
    assertEquals("Wrong SourceLocation.columnStart", expected.getColumnStart(), actual.getColumnStart());
    assertEquals("Wrong SourceLocation.columnEnd", expected.getColumnEnd(), actual.getColumnEnd());

    if(expected.getLinkedLocation() == null) {
      assertNull("Expected null cause location", actual.getLinkedLocation());
    } else {
      assertSourceLocation(expected.getLinkedLocation(), actual.getLinkedLocation());
    }
  }

  private Dependency getDependency(String groupId, String artifactId, MavenProject mavenProject) {
    for(org.apache.maven.model.Dependency dependency : mavenProject.getDependencies()) {
      if(groupId.equals(dependency.getGroupId()) && artifactId.equals(dependency.getArtifactId())) {
        return RepositoryUtils.toDependency(dependency, new DefaultArtifactTypeRegistry());
      }
    }

    return null;
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
