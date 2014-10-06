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

package org.eclipse.m2e.editor.xml;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


/**
 * Hello fellow tester: everytime this test finds a regression add an 'x' here: everytime you do mindless test update
 * add an 'y' here: y
 * 
 * @author mkleint
 */

public class ManagedArtifactMarkerTest extends AbstractMavenProjectTestCase {

  public void testMNGEclipse2559() throws Exception {
    ResolverConfiguration config = new ResolverConfiguration();
    IProject[] projects = importProjects("projects/MNGECLIPSE-2559", new String[] {"pom.xml"}, config);
    waitForJobsToComplete();

    IProject project = projects[0];
    IMarker[] markers = XmlEditorHelpers.findEditorHintWarningMarkers(project).toArray(new IMarker[0]);
    assertEquals(2, markers.length);
    XmlEditorHelpers.assertEditorHintWarningMarker(IMavenConstants.MARKER_POM_LOADING_ID,
        IMavenConstants.EDITOR_HINT_MANAGED_DEPENDENCY_OVERRIDE, null /* message */, 18 /* lineNumber */,
        2 /* resolutions */, markers[0]);
    XmlEditorHelpers.assertEditorHintWarningMarker(IMavenConstants.MARKER_POM_LOADING_ID,
        IMavenConstants.EDITOR_HINT_MANAGED_PLUGIN_OVERRIDE, null /* message */, 47 /* lineNumber */,
        2 /* resolutions */, markers[1]);

    assertEquals("org.apache.maven.plugins", markers[1].getAttribute("groupId"));
    assertEquals("maven-compiler-plugin", markers[1].getAttribute("artifactId"));
    // not defined in profile
    assertEquals(null, markers[1].getAttribute("profile"));

    assertEquals("ant", markers[0].getAttribute("groupId"));
    assertEquals("ant-apache-oro", markers[0].getAttribute("artifactId"));
    // not defined in profile
    assertEquals(null, markers[0].getAttribute("profile"));

    // Fix the problem - the marker should be removed
    copyContent(project, "pom_good.xml", "pom.xml");
    XmlEditorHelpers.assertNoEditorHintWarningMarkers(project);
  }

  // splitted the test in two as both projects failed to load together!!!! why? shall I bother?
  public void testMNGEclipse2559Second() throws Exception {
    ResolverConfiguration config = new ResolverConfiguration();
    config.setSelectedProfiles("plug,depend");
    IProject[] projects = importProjects("projects/MNGECLIPSE-2559", new String[] {"withProfileActivated/pom.xml"},
        config);
    waitForJobsToComplete();

    IProject project = projects[0];
    IMarker[] markers = XmlEditorHelpers.findEditorHintWarningMarkers(project).toArray(new IMarker[0]);
    assertEquals(2, markers.length);

    XmlEditorHelpers.assertEditorHintWarningMarker(IMavenConstants.MARKER_POM_LOADING_ID,
        IMavenConstants.EDITOR_HINT_MANAGED_DEPENDENCY_OVERRIDE, null /* message */, 21 /* lineNumber */,
        2 /* resolutions */, markers[0]);
    XmlEditorHelpers.assertEditorHintWarningMarker(IMavenConstants.MARKER_POM_LOADING_ID,
        IMavenConstants.EDITOR_HINT_MANAGED_PLUGIN_OVERRIDE, null /* message */, 41 /* lineNumber */,
        2 /* resolutions */, markers[1]);

    assertEquals("org.apache.maven.plugins", markers[1].getAttribute("groupId"));
    assertEquals("maven-compiler-plugin", markers[1].getAttribute("artifactId"));
    assertEquals("plug", markers[1].getAttribute("profile"));

    assertEquals("ant", markers[0].getAttribute("groupId"));
    assertEquals("ant-apache-oro", markers[0].getAttribute("artifactId"));
    assertEquals("depend", markers[0].getAttribute("profile"));

    // Fix the problem - the marker should be removed
    copyContent(project, "pom_good.xml", "pom.xml");
    XmlEditorHelpers.assertNoEditorHintWarningMarkers(project);
  }
}
