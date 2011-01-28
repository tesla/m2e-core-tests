/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

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
    IProject project = createExisting("markerTest", "projects/markers/testWorkflow");
    waitForJobsToComplete();
    assertNotNull("Expected not null project", project);
    IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManagerImpl().create(project, monitor);
    assertNull("Expected null MavenProjectFacade", facade);
    String expectedErrorMessage = "Project build error: Non-readable POM ";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_POM_LOADING_ID, expectedErrorMessage, 1 /*lineNumber*/,
        project);

    // Fix the pom, introduce a configuration problem
    copyContent(project, "pom_badConfiguration.xml", "pom.xml");
    waitForJobsToComplete();
    facade = MavenPlugin.getDefault().getMavenProjectManagerImpl().getProject(project);
    assertNotNull("Expected not null MavenProjectFacade", facade);
    project = facade.getProject();
    expectedErrorMessage = "Unknown or missing lifecycle mapping (project packaging type=\"test-packaging-empty\")";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        7 /*lineNumber of <packaging> element*/, project);
    WorkspaceHelpers.assertLifecyclePackagingErrorMarkerAttributes(project, "test-packaging-empty");

    // Building the project should not remove the marker
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        7 /*lineNumber of <packaging> element*/, project);

    // Fix the current configuration problem, introduce a new one
    copyContent(project, "pom_badConfiguration1.xml", "pom.xml");
    waitForJobsToComplete();
    expectedErrorMessage = "Plugin execution not covered by lifecycle configuration: org.codehaus.modello:modello-maven-plugin:1.1:java (execution: standard, phase: generate-sources)";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_CONFIGURATION_ID, expectedErrorMessage,
        24 /*lineNumber of <goal>standard</goal>*/, project);

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
        project);

    // Building the project should not remove the marker
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    markers = WorkspaceHelpers.findErrorMarkers(project);
    // (jdt) The container 'Maven Dependencies' references non existing library ...missing/missing/0.0.0/missing-0.0.0.jar'
    // (jdt) The project cannot be built until build path errors are resolved
    // (maven) Missing artifact missing:missing:jar:0.0.0:compile
    assertEquals(WorkspaceHelpers.toString(markers), 3, markers.size());
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_DEPENDENCY_ID, expectedErrorMessage, 1 /*lineNumber*/,
        project);

    // Fix the current dependency problem
    copyContent(project, "pom_good.xml", "pom.xml");
    waitForJobsToComplete();
    WorkspaceHelpers.assertErrorMarker("org.eclipse.jdt.core.problem",
        "The project cannot be built until build path errors are resolved", null /*lineNumber*/,
        null /*resourceRelativePath*/, project);

    // Building the project should fix the problem
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(project);

    // Add a fake maven build marker
    project.createMarker(IMavenConstants.MARKER_BUILD_ID);

    // Building the project should remove the marker
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(project);

    // Add a maven build marker based on build participant exception
    copyContent(project, "pom_buildException.xml", "pom.xml");
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(project);
    MavenPlugin.getDefault().getProjectConfigurationManager()
        .updateProjectConfiguration(project, new ResolverConfiguration(), monitor);
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    expectedErrorMessage = "Exception: " + ThrowBuildExceptionProjectConfigurator.ERROR_MESSAGE;
    IMarker marker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_ID, expectedErrorMessage,
        null /*lineNumber*/, project);

    // Verify that the marker is removed by a new build
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    IMarker newMarker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_ID, expectedErrorMessage,
        null /*lineNumber*/, project);
    assertFalse(marker.getId() == newMarker.getId());
  }

  public void testBuildContextWithOneProjectConfigurator() throws Exception {
    IProject project = createExisting("markerTest", "projects/markers/testBuildContextWithOneProjectConfigurator");
    waitForJobsToComplete();
    assertNotNull("Expected not null project", project);
    IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManagerImpl().create(project, monitor);
    assertNotNull("Expected not null MavenProjectFacade", facade);
    WorkspaceHelpers.assertNoErrors(project);

    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    IMarker errorMarker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfiguratorFoo.ERROR_MESSAGE, AddMarkersProjectConfiguratorFoo.ERROR_LINE_NUMBER,
        AddMarkersProjectConfiguratorFoo.FILE_NAME, project);
    IMarker warningMarker = WorkspaceHelpers.assertWarningMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfiguratorFoo.WARNING_MESSAGE, AddMarkersProjectConfiguratorFoo.WARNING_LINE_NUMBER,
        AddMarkersProjectConfiguratorFoo.FILE_NAME, project);

    // An incremental build without interesting changes should not remove the markers
    copyContent(project, AddMarkersProjectConfiguratorFoo.FILE_NAME, "x.txt");
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    IMarker newErrorMarker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfiguratorFoo.ERROR_MESSAGE, AddMarkersProjectConfiguratorFoo.ERROR_LINE_NUMBER,
        AddMarkersProjectConfiguratorFoo.FILE_NAME, project);
    IMarker newWarningMarker = WorkspaceHelpers.assertWarningMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfiguratorFoo.WARNING_MESSAGE, AddMarkersProjectConfiguratorFoo.WARNING_LINE_NUMBER,
        AddMarkersProjectConfiguratorFoo.FILE_NAME, project);
    assertEquals(errorMarker.getId(), newErrorMarker.getId());
    assertEquals(errorMarker.getAttribute(IMarker.MESSAGE, null), newErrorMarker.getAttribute(IMarker.MESSAGE, null));
    assertEquals(warningMarker.getId(), newWarningMarker.getId());
    assertEquals(warningMarker.getAttribute(IMarker.MESSAGE, null),
        newWarningMarker.getAttribute(IMarker.MESSAGE, null));

    // A full build should remove the markers
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    newErrorMarker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfiguratorFoo.ERROR_MESSAGE, AddMarkersProjectConfiguratorFoo.ERROR_LINE_NUMBER,
        AddMarkersProjectConfiguratorFoo.FILE_NAME, project);
    newWarningMarker = WorkspaceHelpers.assertWarningMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfiguratorFoo.WARNING_MESSAGE, AddMarkersProjectConfiguratorFoo.WARNING_LINE_NUMBER,
        AddMarkersProjectConfiguratorFoo.FILE_NAME, project);
    assertFalse(errorMarker.getId() == newErrorMarker.getId());
    assertFalse(warningMarker.getId() == newWarningMarker.getId());

    // An incremental build with interesting changes should remove the old markers
    errorMarker = newErrorMarker;
    warningMarker = newWarningMarker;
    copyContent(project, "x.txt", AddMarkersProjectConfiguratorFoo.FILE_NAME);
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    newErrorMarker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfiguratorFoo.ERROR_MESSAGE, AddMarkersProjectConfiguratorFoo.ERROR_LINE_NUMBER,
        AddMarkersProjectConfiguratorFoo.FILE_NAME, project);
    newWarningMarker = WorkspaceHelpers.assertWarningMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfiguratorFoo.WARNING_MESSAGE, AddMarkersProjectConfiguratorFoo.WARNING_LINE_NUMBER,
        AddMarkersProjectConfiguratorFoo.FILE_NAME, project);
    assertFalse(errorMarker.getId() == newErrorMarker.getId());
    assertFalse(warningMarker.getId() == newWarningMarker.getId());

    // A clean build should remove the markers
    project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(project);
  }

  public void testBuildContextWithTwoProjectConfigurators() throws Exception {
    IProject project = createExisting("markerTest", "projects/markers/testBuildContextWithTwoProjectConfigurators");
    waitForJobsToComplete();
    assertNotNull("Expected not null project", project);
    IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManagerImpl().create(project, monitor);
    assertNotNull("Expected not null MavenProjectFacade", facade);
    WorkspaceHelpers.assertNoErrors(project);

    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    IMarker errorMarker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfiguratorFoo.ERROR_MESSAGE, AddMarkersProjectConfiguratorFoo.ERROR_LINE_NUMBER,
        AddMarkersProjectConfiguratorFoo.FILE_NAME, project);
    IMarker warningMarker = WorkspaceHelpers.assertWarningMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfiguratorFoo.WARNING_MESSAGE, AddMarkersProjectConfiguratorFoo.WARNING_LINE_NUMBER,
        AddMarkersProjectConfiguratorFoo.FILE_NAME, project);

    // An incremental build with interesting changes for only one of the project configurators should not remove the markers created by the other project configurator
    copyContent(project, AddMarkersProjectConfiguratorFoo.FILE_NAME, AddMarkersProjectConfiguratorBar.FILE_NAME);
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertEquals(toString(errorMarkers), 2, errorMarkers.size());
    List<IMarker> warningMarkers = WorkspaceHelpers.findWarningMarkers(project);
    assertEquals(toString(warningMarkers), 2, errorMarkers.size());
    // Verify that the old markers for AddMarkersProjectConfiguratorFoo where not removed
    IMarker newErrorMarker = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfiguratorFoo.ERROR_MESSAGE, AddMarkersProjectConfiguratorFoo.ERROR_LINE_NUMBER,
        AddMarkersProjectConfiguratorFoo.FILE_NAME, project);
    IMarker newWarningMarker = WorkspaceHelpers.assertWarningMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfiguratorFoo.WARNING_MESSAGE, AddMarkersProjectConfiguratorFoo.WARNING_LINE_NUMBER,
        AddMarkersProjectConfiguratorFoo.FILE_NAME, project);
    assertEquals(errorMarker.getId(), newErrorMarker.getId());
    assertEquals(errorMarker.getAttribute(IMarker.MESSAGE, null), newErrorMarker.getAttribute(IMarker.MESSAGE, null));
    assertEquals(warningMarker.getId(), newWarningMarker.getId());
    assertEquals(warningMarker.getAttribute(IMarker.MESSAGE, null),
        newWarningMarker.getAttribute(IMarker.MESSAGE, null));
    // Verify that the new markers for AddMarkersProjectConfiguratorBar where created
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfiguratorBar.ERROR_MESSAGE, AddMarkersProjectConfiguratorBar.ERROR_LINE_NUMBER,
        AddMarkersProjectConfiguratorBar.FILE_NAME, project);
    WorkspaceHelpers.assertWarningMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfiguratorBar.WARNING_MESSAGE, AddMarkersProjectConfiguratorBar.WARNING_LINE_NUMBER,
        AddMarkersProjectConfiguratorBar.FILE_NAME, project);
  }

  public void testBuildContextWithSameProjectConfiguratorTwice() throws Exception {
    IProject project = createExisting("markerTest", "projects/markers/testBuildContextWithSameProjectConfiguratorTwice");
    waitForJobsToComplete();
    assertNotNull("Expected not null project", project);
    IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManagerImpl().create(project, monitor);
    assertNotNull("Expected not null MavenProjectFacade", facade);
    WorkspaceHelpers.assertNoErrors(project);

    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertEquals(toString(errorMarkers), 2, errorMarkers.size());
    List<IMarker> warningMarkers = WorkspaceHelpers.findWarningMarkers(project);
    assertEquals(toString(warningMarkers), 2, errorMarkers.size());

    String mojoExecutionKey0 = "org.apache.maven.plugins:maven-deploy-plugin:2.5:deploy:default-deploy";
    IMarker errorMarker0 = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfigurator.ERROR_MESSAGE + " " + mojoExecutionKey0,
        AddMarkersProjectConfigurator.ERROR_LINE_NUMBER, AddMarkersProjectConfigurator.FILE_NAME, project);
    IMarker warningMarker0 = WorkspaceHelpers.assertWarningMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfigurator.WARNING_MESSAGE + " " + mojoExecutionKey0,
        AddMarkersProjectConfigurator.WARNING_LINE_NUMBER, AddMarkersProjectConfigurator.FILE_NAME, project);

    String mojoExecutionKey1 = "org.apache.maven.plugins:maven-install-plugin:2.3.1:install:default-install";
    IMarker errorMarker1 = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfigurator.ERROR_MESSAGE + " " + mojoExecutionKey1,
        AddMarkersProjectConfigurator.ERROR_LINE_NUMBER, AddMarkersProjectConfigurator.FILE_NAME, project);
    IMarker warningMarker1 = WorkspaceHelpers.assertWarningMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfigurator.WARNING_MESSAGE + " " + mojoExecutionKey1,
        AddMarkersProjectConfigurator.WARNING_LINE_NUMBER, AddMarkersProjectConfigurator.FILE_NAME, project);

    // An incremental build should remove and re-create only the markers for the second mojo execution key
    copyContent(project, AddMarkersProjectConfigurator.FILE_NAME, "x.txt");
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    waitForJobsToComplete();
    errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertEquals(toString(errorMarkers), 2, errorMarkers.size());
    warningMarkers = WorkspaceHelpers.findWarningMarkers(project);
    assertEquals(toString(warningMarkers), 2, errorMarkers.size());

    IMarker newErrorMarker0 = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfigurator.ERROR_MESSAGE + " " + mojoExecutionKey0,
        AddMarkersProjectConfigurator.ERROR_LINE_NUMBER, AddMarkersProjectConfigurator.FILE_NAME, project);
    IMarker newWarningMarker0 = WorkspaceHelpers.assertWarningMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfigurator.WARNING_MESSAGE + " " + mojoExecutionKey0,
        AddMarkersProjectConfigurator.WARNING_LINE_NUMBER, AddMarkersProjectConfigurator.FILE_NAME, project);

    IMarker newErrorMarker1 = WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfigurator.ERROR_MESSAGE + " " + mojoExecutionKey1,
        AddMarkersProjectConfigurator.ERROR_LINE_NUMBER, AddMarkersProjectConfigurator.FILE_NAME, project);
    IMarker newWarningMarker1 = WorkspaceHelpers.assertWarningMarker(IMavenConstants.MARKER_BUILD_PARTICIPANT_ID,
        AddMarkersProjectConfigurator.WARNING_MESSAGE + " " + mojoExecutionKey1,
        AddMarkersProjectConfigurator.WARNING_LINE_NUMBER, AddMarkersProjectConfigurator.FILE_NAME, project);

    assertEquals(errorMarker0.getId(), newErrorMarker0.getId());
    assertEquals(warningMarker0.getId(), newWarningMarker0.getId());
    assertFalse(errorMarker1.getId() == newErrorMarker1.getId());
    assertFalse(warningMarker1.getId() == newWarningMarker1.getId());
  }

//  public void testSearchMarkers() throws Exception {
//    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
//        "testLifecycleMappingSpecifiedInMetadata/pom.xml");
//    assertNotNull("Expected not null MavenProjectFacade", facade);
//    IProject project = facade.getProject();
//    WorkspaceHelpers.assertNoErrors(project);
//    IResource resource = facade.getPom();
//
//    int totalMarkers = 100;
//    int percentForFind = 10;
//    List<String> idsToFind = new ArrayList<String>();
//    long start = System.currentTimeMillis();
//    String markerType = IMavenConstants.MARKER_CONFIGURATION_ID;
//    for(int i = 0; i < totalMarkers; i++ ) {
//      IMarker marker = resource.createMarker(markerType);
//      marker.setAttribute(IMarker.MESSAGE, "Some reasonably error message here. Or maybe a little bit longer.");
//      marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
//      marker.setAttribute(IMarker.TRANSIENT, true);
//      String id = "org.apache.maven.plugins:maven-compiler-plugin:2.0.1:testCompile:standard:org.eclipse.m2e.jdt.internal.JavaProjectConfigurator"
//          + System.currentTimeMillis() + System.nanoTime();
//      marker.setAttribute("buildParticipant", id);
//      if(i % percentForFind == 0) {
//        idsToFind.add(id);
//      }
//    }
//    System.err.println("Time to create " + totalMarkers + " markers: " + (System.currentTimeMillis() - start) + "ms");
//    IMarker[] allMarkers = resource
//        .findMarkers(markerType, false /*includeSubtypes*/, 0 /*depth*/);
//    assertEquals(totalMarkers, allMarkers.length);
//
//    start = System.currentTimeMillis();
//    for(String idToFind : idsToFind) {
//      int found = 0;
//      allMarkers = resource
//          .findMarkers(markerType, false /*includeSubtypes*/, 0 /*depth*/);
//      for(IMarker marker : allMarkers) {
//        String id = marker.getAttribute("buildParticipant", null);
//        if(idToFind.equals(id)) {
//          found++ ;
//        }
//      }
//      assertEquals(1, found);
//    }
//    long elapsed = (System.currentTimeMillis() - start);
//    System.err.println("Time to find " + idsToFind.size() + " markers: " + elapsed + "ms");
//    System.err.println("Average time to find 1 marker: " + (elapsed / idsToFind.size()) + "ms");
//  }
//
//  public void testSearchMarkers1() throws Exception {
//    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping/lifecycleMappingMetadata",
//        "testLifecycleMappingSpecifiedInMetadata/pom.xml");
//    assertNotNull("Expected not null MavenProjectFacade", facade);
//    IProject project = facade.getProject();
//    WorkspaceHelpers.assertNoErrors(project);
//    IResource resource = facade.getPom();
//
//    int totalMarkers = 10000;
//    int percentForFind = 10;
//    List<String> idsToFind = new ArrayList<String>();
//    long start = System.currentTimeMillis();
//    String markerType = IMavenConstants.MARKER_CONFIGURATION_ID;
//    for(int i = 0; i < totalMarkers; i++ ) {
//      String id = "org.apache.maven.plugins:maven-compiler-plugin:2.0.1:testCompile:standard:org.eclipse.m2e.jdt.internal.JavaProjectConfigurator"
//          + System.currentTimeMillis() + System.nanoTime();
//      IMarker marker = resource.createMarker(markerType + "." + id);
//      marker.setAttribute(IMarker.MESSAGE, "Some reasonably error message here. Or maybe a little bit longer.");
//      marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
//      marker.setAttribute(IMarker.TRANSIENT, true);
//      if(i % percentForFind == 0) {
//        idsToFind.add(id);
//      }
//    }
//    System.err.println("Time to create " + totalMarkers + " markers: " + (System.currentTimeMillis() - start) + "ms");
////    IMarker[] allMarkers = resource.findMarkers(markerType, true /*includeSubtypes*/, 0 /*depth*/);
////    assertEquals(totalMarkers, allMarkers.length);
//
//    start = System.currentTimeMillis();
//    for(String idToFind : idsToFind) {
//      IMarker[] allMarkers = resource
//          .findMarkers(markerType + "." + idToFind, false /*includeSubtypes*/, 0 /*depth*/);
//      assertEquals(1, allMarkers.length);
//    }
//    long elapsed = (System.currentTimeMillis() - start);
//    System.err.println("Time to find " + idsToFind.size() + " markers: " + elapsed + "ms");
//    System.err.println("Average time to find 1 marker: " + (elapsed / idsToFind.size()) + "ms");
//  }
//
//  protected IMavenProjectFacade importMavenProject(String basedir, String pomName) throws Exception {
//    ResolverConfiguration configuration = new ResolverConfiguration();
//    IProject[] project = importProjects(basedir, new String[] {pomName}, configuration);
//    waitForJobsToComplete();
//
//    MavenProjectManager mavenProjectManager = MavenPlugin.getDefault().getMavenProjectManager();
//    return mavenProjectManager.create(project[0], monitor);
//  }
}
