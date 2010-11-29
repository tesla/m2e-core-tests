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

import java.io.IOException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

/**
 * Hello fellow tester:
 * everytime this test finds a regression add an 'x' here:
 * everytime you do mindless test update add an 'y' here:
 * @author mkleint
 *
 */

public class WarnIfGroupSameThanParentTest extends AbstractMavenProjectTestCase {

  public void testMNGEclipse2552() throws IOException, CoreException, InterruptedException {
    IProject[] projects = importProjects("projects/MNGECLIPSE-2552", new String[] {
        "child2552withDuplicateGroupAndVersion/pom.xml", 
        "child2552withDuplicateGroup/pom.xml",
        "child2552withDuplicateVersion/pom.xml", 
        "parent2552/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
    MavenMarkerResolutionGenerator generator = new MavenMarkerResolutionGenerator();

    {
      //"child2552withDuplicateGroupAndVersion/pom.xml"
      IMarker[] markers = projects[0].findMember("pom.xml").findMarkers(IMavenConstants.MARKER_HINT_ID, true, IResource.DEPTH_INFINITE);
      assertEquals(2, markers.length);
      assertEquals(IMarker.SEVERITY_WARNING, markers[0].getAttribute(IMarker.SEVERITY));
      assertEquals("parent_groupid", markers[0].getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT));
      assertEquals("parent_version", markers[1].getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT));
      
      //this sort of testing just asks for trouble and endless updates of the test, but well..
      assertEquals(1, generator.getResolutions(markers[0]).length);
      assertEquals(1, generator.getResolutions(markers[1]).length);
    }

    {
      //"child2552withDuplicateGroup/pom.xml", 
      IMarker[] markers = projects[1].findMember("pom.xml").findMarkers(IMavenConstants.MARKER_HINT_ID, true, IResource.DEPTH_INFINITE);
      assertEquals(1, markers.length);
      assertEquals(IMarker.SEVERITY_WARNING, markers[0].getAttribute(IMarker.SEVERITY));
      assertEquals("parent_groupid", markers[0].getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT));

      //this sort of testing just asks for trouble and endless updates of the test, but well..
      assertEquals(1, generator.getResolutions(markers[0]).length);
    }
    
    {
      //"child2552withDuplicateVersion/pom.xml"
      IMarker[] markers = projects[2].findMember("pom.xml").findMarkers(IMavenConstants.MARKER_HINT_ID, true, IResource.DEPTH_INFINITE);
      assertEquals(1, markers.length);
      assertEquals(IMarker.SEVERITY_WARNING, markers[0].getAttribute(IMarker.SEVERITY));
      assertEquals("parent_version", markers[0].getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT));
      
      //this sort of testing just asks for trouble and endless updates of the test, but well..
      assertEquals(1, generator.getResolutions(markers[0]).length);
    }
  }
}
