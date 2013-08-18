/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClasspathEntry;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


/**
 * Tests for profile related behavior.
 * 
 * @author Fred Bricon
 */
public class ProfileSupportTest extends AbstractMavenProjectTestCase {

  /**
   * Checks the ! prefix deactivates a default activated profile
   * 
   * @throws Exception
   */
  @Test
  public void test337353_deactivateProfile() throws IOException, Exception {
    ResolverConfiguration configuration = new ResolverConfiguration();
    //Deactivate the test profile on import
    configuration.setSelectedProfiles(",!test,    ,,");
    IProject project = importProject("projects/profiles/337353-deactivate-profile/pom.xml", configuration);
    assertNoErrors(project);

    IClasspathEntry[] entries = getMavenContainerEntries(project);
    assertEquals("Profile is deactivated, no classpath entries should be present", 0, entries.length);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
    List<String> inactiveProfiles = facade.getResolverConfiguration().getInactiveProfileList();
    assertEquals(1, inactiveProfiles.size());
    assertEquals("test", inactiveProfiles.get(0));
  }

}
