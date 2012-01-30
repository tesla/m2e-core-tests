/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.conversion;

import java.util.List;

import org.eclipse.core.resources.IProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.conversion.AbstractProjectConversionParticipant;
import org.eclipse.m2e.core.project.conversion.IProjectConversionManager;


/**
 * ProjectConversionManagerTest
 * 
 * @author Fred Bricon
 */
public class ProjectConversionManagerTest extends AbstractProjectConversionTestCase {

  public void testConversionParticipantsPerProject() throws Exception {
    IProject p1 = createExisting("custom-layout", "projects/conversion/custom-layout");
    IProject p2 = createExisting("project-needs-test-participant", "projects/conversion/project-needs-test-participant");
    IProject p3 = createExisting("no-java-nature", "projects/conversion/no-java-nature");

    IProjectConversionManager manager = MavenPlugin.getProjectConversionManager();
    List<AbstractProjectConversionParticipant> participants;

    participants = manager.getConversionParticipants(p1);
    assertEquals("Participants found for " + p1.getName() + " : " + participants.toString(), 1, participants.size());

    participants = manager.getConversionParticipants(p2);
    assertEquals("Participants found for " + p2.getName() + " : " + participants.toString(), 2, participants.size());

    participants = manager.getConversionParticipants(p3);
    assertEquals("Participants found for " + p3.getName() + " : " + participants.toString(), 0, participants.size());
  }

}
