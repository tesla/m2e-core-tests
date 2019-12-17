/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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

    String packaging = "jar";
    participants = manager.getConversionParticipants(p1, packaging);
    assertEquals("Participants found for " + p1.getName() + " : " + participants.toString(), 1, participants.size());

    participants = manager.getConversionParticipants(p2, packaging);
    assertEquals("Participants found for " + p2.getName() + " : " + participants.toString(), 2, participants.size());

    participants = manager.getConversionParticipants(p3, packaging);
    assertEquals("Participants found for " + p3.getName() + " : " + participants.toString(), 0, participants.size());

  }

  public void testRestrictedPackagings() throws Exception {
    IProjectConversionManager manager = MavenPlugin.getProjectConversionManager();
    String packaging = "eclipse-plugin";
    List<AbstractProjectConversionParticipant> participants;

    //Check eclipse-plugin packaging is not supported by default
    IProject pde = createExisting("pde", "projects/conversion/pde");
    participants = manager.getConversionParticipants(pde, packaging);
    checkJdtConverter(participants, false);

    //Check foo packaging contributed by extension point is supported
    IProject foo = createExisting("foo", "projects/conversion/foo");
    packaging = "foo";
    participants = manager.getConversionParticipants(foo, packaging);
    checkJdtConverter(participants, true);
  }

  private void checkJdtConverter(List<AbstractProjectConversionParticipant> participants, boolean expectPresent) {
    if(participants != null) {
      for(AbstractProjectConversionParticipant p : participants) {
        if("org.eclipse.m2e.jdt.javaProjectConversionParticipant".equals(p.getId())) {
          if(expectPresent) {
            return;
          }
          fail("No JDT conversion participant should be found");
        }
      }
    }
    if(expectPresent) {
      fail("JDT conversion participant is missing ");
    }
  }

  public void test393613_SortConversionParticipants() throws Exception {
    IProject project = createExisting("project-needs-test-sort-participant",
        "projects/conversion/project-needs-test-sort-participant");

    IProjectConversionManager manager = MavenPlugin.getProjectConversionManager();
    List<AbstractProjectConversionParticipant> participants;

    String packaging = "jar";
    participants = manager.getConversionParticipants(project, packaging);
    assertEquals("Participants found for " + project.getName() + " : " + participants.toString(), 4,
        participants.size());
    int i = 0;
    String msg = "Participants found for " + project.getName() + " : " + participants.toString();
    assertEquals(msg, "org.eclipse.m2e.tests.conversion.testProjectConversionParticipant2", participants.get(i++ )
        .getId());
    assertEquals(msg, "org.eclipse.m2e.jdt.javaProjectConversionParticipant", participants.get(i++ ).getId());
    assertEquals(msg, "org.eclipse.m2e.tests.conversion.testProjectConversionParticipant4", participants.get(i++ )
        .getId());
    assertEquals(msg, "org.eclipse.m2e.tests.conversion.testProjectConversionParticipant3",
        participants.get(i++ )
        .getId());
  }

}
