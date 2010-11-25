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

package org.eclipse.m2e.tests.configurators;

import java.util.List;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.m2e.core.internal.project.CustomizableLifecycleMapping;
import org.eclipse.m2e.core.internal.project.MissingLifecycleMapping;
import org.eclipse.m2e.core.internal.project.MojoExecutionProjectConfigurator;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.MavenResourcesProjectConfigurator;
import org.eclipse.m2e.jdt.internal.JarLifecycleMapping;
import org.eclipse.m2e.jdt.internal.JavaProjectConfigurator;
import org.eclipse.m2e.tests.common.AbstractLifecycleMappingTest;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;

@SuppressWarnings("restriction")
public class LifecycleMappingTest extends AbstractLifecycleMappingTest {
  public void testJarLifecycleMapping() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "jar/pom.xml");

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade, monitor);

    assertTrue(lifecycleMapping instanceof JarLifecycleMapping);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertEquals(2, configurators.size());
    assertTrue(configurators.get(0) instanceof MavenResourcesProjectConfigurator);
    assertTrue(configurators.get(1) instanceof JavaProjectConfigurator);

    List<MojoExecution> notCoveredMojoExecutions = lifecycleMapping.getNotCoveredMojoExecutions(facade, monitor);
    assertEquals(notCoveredMojoExecutions.toString(), 0, lifecycleMapping.getNotCoveredMojoExecutions(facade, monitor)
        .size());
  }

  public void testCustomizableMapping() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "customizable/pom.xml");

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade, monitor);

    assertTrue(lifecycleMapping instanceof CustomizableLifecycleMapping);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertEquals(2, configurators.size());
    assertTrue(configurators.get(1) instanceof MojoExecutionProjectConfigurator);

    List<MojoExecution> notCoveredMojoExecutions = lifecycleMapping.getNotCoveredMojoExecutions(facade, monitor);
    assertEquals(notCoveredMojoExecutions.toString(), 0, lifecycleMapping.getNotCoveredMojoExecutions(facade, monitor)
        .size());
  }

  public void testCustomizableMappingNotComplete() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "customizableNotComplete/pom.xml");

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade, monitor);

    assertTrue(lifecycleMapping instanceof CustomizableLifecycleMapping);

    List<AbstractProjectConfigurator> configurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertEquals(1, configurators.size());
    assertTrue(configurators.get(0) instanceof JavaProjectConfigurator);

    List<MojoExecution> notCoveredMojoExecutions = lifecycleMapping.getNotCoveredMojoExecutions(facade, monitor);
    assertEquals(notCoveredMojoExecutions.toString(), 1, notCoveredMojoExecutions.size());
    assertEquals("org.codehaus.modello:modello-maven-plugin:1.1:java {execution: standard}", notCoveredMojoExecutions
        .get(0).toString());

    IProject project = facade.getProject();
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 1, errorMarkers.size());
    assertEquals(
        "Mojo execution not covered by lifecycle configuration: org.codehaus.modello:modello-maven-plugin:1.1:java {execution: standard} (maven lifecycle phase: generate-sources)",
        errorMarkers.get(0).getAttribute(IMarker.MESSAGE));
  }

  public void testMissingMapping() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "missing/pom.xml");

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade, monitor);

    assertTrue(lifecycleMapping instanceof MissingLifecycleMapping);
    assertEquals("unknown-or-missing", ((MissingLifecycleMapping) lifecycleMapping).getMissingMappingId());

    assertEquals(0, lifecycleMapping.getNotCoveredMojoExecutions(facade, monitor).size());

    IProject project = facade.getProject();
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 1, errorMarkers.size());
    assertEquals("Unknown or missing lifecycle mapping with id=\"MISSING\" (project packaging type=\"jar\")",
        errorMarkers.get(0).getAttribute(IMarker.MESSAGE));
  }

  public void testUnknownPackagingType() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/lifecyclemapping", "unknownPackagingType/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade, monitor);

    assertTrue(lifecycleMapping instanceof MissingLifecycleMapping);

    assertEquals(0, lifecycleMapping.getNotCoveredMojoExecutions(facade, monitor).size());

    IProject project = facade.getProject();
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertNotNull(errorMarkers);
    assertEquals(WorkspaceHelpers.toString(errorMarkers), 1, errorMarkers.size());
    assertEquals("Unknown or missing lifecycle mapping with id=\"MISSING\" (project packaging type=\"war\")",
        errorMarkers.get(0).getAttribute(IMarker.MESSAGE));
  }
}
