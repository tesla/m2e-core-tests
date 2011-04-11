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

import java.io.File;

import org.eclipse.core.resources.IProject;

import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


public class ProjectRegistryRefreshJobTest extends AbstractMavenProjectTestCase {

  private static final String SETTINGS_ONE = "settings_updateRepo.xml";

  private static final String SETTINGS_TWO = "settings_updateRepo2.xml";

  private static final File LOCAL_REPO = new File("target/updateTestLocalRepo").getAbsoluteFile();

  private static final File LOCAL_ARTIFACT = new File(
      "target/updateTestLocalRepo/updateTest/b/1.0-SNAPSHOT/b-1.0-SNAPSHOT.jar").getAbsoluteFile();

  private IProject project;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // clear local repo
    delete(LOCAL_REPO);
    mavenConfiguration.setUserSettingsFile(new File(SETTINGS_ONE).getAbsolutePath());
    waitForJobsToComplete();

    // import project
    project = importProject("projects/updateProject/pom.xml");
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(project);

    mavenConfiguration.setUserSettingsFile(new File(SETTINGS_TWO).getAbsolutePath());
    waitForJobsToComplete();
  }

  public void testUpdateNotForced() throws Exception {
    projectRefreshJob.refresh(new MavenUpdateRequest(project, false, false));
    Thread.sleep(1000); // Refresh job is delayed
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(project);

    assertEquals(338, LOCAL_ARTIFACT.length());
  }

  public void testUpdateForced() throws Exception {
    projectRefreshJob.refresh(new MavenUpdateRequest(project, false, true));
    Thread.sleep(1000); // Refresh job is delayed
    waitForJobsToComplete();
    WorkspaceHelpers.assertNoErrors(project);

    assertEquals(785, LOCAL_ARTIFACT.length());
  }

  private static void delete(File file) {
    if(file.isDirectory()) {
      for(File child : file.listFiles()) {
        delete(child);
      }
    }
    file.delete();
  }
}