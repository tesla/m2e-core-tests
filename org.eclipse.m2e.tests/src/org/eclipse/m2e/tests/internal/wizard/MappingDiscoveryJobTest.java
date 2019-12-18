/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
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

package org.eclipse.m2e.tests.internal.wizard;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Collections;

import org.junit.After;
import org.junit.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingDiscoveryRequest;
import org.eclipse.m2e.core.ui.internal.wizards.MappingDiscoveryJob;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.JobHelpers;


/**
 * MappingDiscoveryJobTest
 *
 * @author Fred Bricon
 */
public class MappingDiscoveryJobTest extends AbstractMavenProjectTestCase {

  MappingDiscoveryJobNoUI discoveryJob;

  @Override
  @After
  public void tearDown() throws Exception {
    discoveryJob = null;
    super.tearDown();
  }

  @Test
  public void testOpensMappingDiscoveryWizard() throws Exception {
    IProject project = importProject("projects/discovery/projectConfigurator/pom.xml");
    checkOpensMappingDiscoveryWizard(project, true);
  }

  @Test
  public void testDontOpenMappingDiscoveryWizard() throws Exception {
    IProject project = importProject("projects/projectimport/p001/pom.xml");
    checkOpensMappingDiscoveryWizard(project, false);
  }

  private void checkOpensMappingDiscoveryWizard(IProject project, boolean expectedResult) throws Exception {
    discoveryJob = new MappingDiscoveryJobNoUI(Collections.singleton(project));
    discoveryJob.schedule();
    JobHelpers.waitForJobs(job -> discoveryJob == job, 100);
    assertEquals("MappingDiscoveryJob was " + (expectedResult ? "" : "not ") + "supposed to open", expectedResult,
        discoveryJob.openedMappingWizard);
  }

  private static class MappingDiscoveryJobNoUI extends MappingDiscoveryJob {

    boolean openedMappingWizard;

    public MappingDiscoveryJobNoUI(Collection<IProject> projects) {
      super(projects);
    }

    @Override
    protected void openProposalWizard(Collection<IProject> projects, LifecycleMappingDiscoveryRequest discoveryRequest) {
      openedMappingWizard = true;
    }

    @Override
    protected void discoverProposals(LifecycleMappingDiscoveryRequest discoveryRequest, IProgressMonitor monitor) {
      //keep it between us
    }

  }

}
