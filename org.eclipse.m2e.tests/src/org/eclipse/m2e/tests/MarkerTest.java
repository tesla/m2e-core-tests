package org.eclipse.m2e.tests;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


public class MarkerTest extends AbstractMavenProjectTestCase {
  @SuppressWarnings("restriction")
  public void test() throws Exception {
    // Import a project with bad pom.xml
    IProject project = createExisting("markerTest", "projects/markers");
    waitForJobsToComplete();
    assertNotNull("Expected not null project", project);
    IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManagerImpl().create(project, monitor);
    assertNull("Expected null MavenProjectFacade", facade);
    String expectedErrorMessage = "Project build error: Non-readable POM ";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_POM_LOADING_ID, expectedErrorMessage,
        1 /*lineNumber*/, project);

    // Fix the pom, introduce a configuration problem
    copyContent(project, "pom_badConfiguration.xml", "pom.xml");
    waitForJobsToComplete();
    facade = MavenPlugin.getDefault().getMavenProjectManagerImpl().getProject(project);
    assertNotNull("Expected not null MavenProjectFacade", facade);
    project = facade.getProject();
    expectedErrorMessage = "Unknown or missing lifecycle mapping with id=\"MISSING\" (project packaging type=\"war\")";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, project);

    // Building the project should not remove the marker
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, project);

    // Fix the current configuration problem, introduce a new one
    copyContent(project, "pom_badConfiguration1.xml", "pom.xml");
    waitForJobsToComplete();
    expectedErrorMessage = "Mojo execution not covered by lifecycle configuration: org.codehaus.modello:modello-maven-plugin:1.1:java {execution: standard} (maven lifecycle phase: generate-sources)";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        1 /*lineNumber*/, project);

    // Fix the current configuration problem, introduce a dependency problem
    copyContent(project, "pom_badDependency.xml", "pom.xml");
    waitForJobsToComplete();
    MavenPlugin.getDefault().getProjectConfigurationManager()
        .updateProjectConfiguration(project, new ResolverConfiguration(), monitor);
    expectedErrorMessage = "Missing artifact missing:missing:jar:0.0.0:compile";
    List<IMarker> markers = WorkspaceHelpers.findErrorMarkers(project);
    // (jdt) The container 'Maven Dependencies' references non existing library ...missing/missing/0.0.0/missing-0.0.0.jar'
    // (maven) Missing artifact missing:missing:jar:0.0.0:compile
    assertEquals(WorkspaceHelpers.toString(markers), 2, markers.size());
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_DEPENDENCY_ID, expectedErrorMessage, 1 /*lineNumber*/,
        markers.get(1));

    // Building the project should not remove the marker
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    markers = WorkspaceHelpers.findErrorMarkers(project);
    // (jdt) The container 'Maven Dependencies' references non existing library ...missing/missing/0.0.0/missing-0.0.0.jar'
    // (jdt) The project cannot be built until build path errors are resolved
    // (maven) Missing artifact missing:missing:jar:0.0.0:compile
    assertEquals(WorkspaceHelpers.toString(markers), 3, markers.size());
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_DEPENDENCY_ID, expectedErrorMessage, 1 /*lineNumber*/,
        markers.get(2));

    // Fix the current dependency problem
    copyContent(project, "pom_good.xml", "pom.xml");
    waitForJobsToComplete();
    WorkspaceHelpers.assertErrorMarker("org.eclipse.jdt.core.problem",
        "The project cannot be built until build path errors are resolved", null /*lineNumber*/,
        project);

    // Building the project should fix the problem
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(project);

    // Add a maven build marker
    project.createMarker(IMavenConstants.MARKER_BUILD_ID);

    // Building the project should remove the marker
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(project);
  }

  protected IMavenProjectFacade importMavenProject(String basedir, String pomName) throws Exception {
    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject[] project = importProjects(basedir, new String[] {pomName}, configuration);
    waitForJobsToComplete();

    return MavenPlugin.getDefault().getMavenProjectManager().create(project[0], monitor);
  }
}
