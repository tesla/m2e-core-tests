/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.internal.wizard;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingDiscoveryRequest;
import org.eclipse.m2e.core.ui.internal.wizards.MappingDiscoveryJob;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.JobHelpers;
import org.eclipse.m2e.tests.common.JobHelpers.IJobMatcher;


/**
 * MappingDiscoveryJobTest
 *
 * @author Fred Bricon
 */
public class MappingDiscoveryJobTest extends AbstractMavenProjectTestCase {

  MappingDiscoveryJobNoUI discoveryJob;

  protected void tearDown() throws Exception {
    discoveryJob = null;
    super.tearDown();
  }

  public void testOpensMappingDiscoveryWizard() throws Exception {
    IProject project = importProject("projects/discovery/projectConfigurator/pom.xml");
    checkOpensMappingDiscoveryWizard(project, true);
  }

  public void testDontOpenMappingDiscoveryWizard() throws Exception {
    IProject project = importProject("projects/projectimport/p001/pom.xml");
    checkOpensMappingDiscoveryWizard(project, false);
  }

  private void checkOpensMappingDiscoveryWizard(IProject project, boolean expectedResult) throws Exception {
    discoveryJob = new MappingDiscoveryJobNoUI(Collections.singleton(project));
    discoveryJob.schedule();
    JobHelpers.waitForJobs(new IJobMatcher() {
      public boolean matches(Job job) {
        return discoveryJob == job;
      }
    }, 100);
    assertEquals("MappingDiscoveryJob was " + (expectedResult ? "" : "not ") + "supposed to open", expectedResult,
        discoveryJob.openedMappingWizard);
  }

  private static class MappingDiscoveryJobNoUI extends MappingDiscoveryJob {

    boolean openedMappingWizard;

    public MappingDiscoveryJobNoUI(Collection<IProject> projects) {
      super(projects);
    }

    protected void openProposalWizard(Collection<IProject> projects, LifecycleMappingDiscoveryRequest discoveryRequest) {
      openedMappingWizard = true;
    }

    protected void discoverProposals(LifecycleMappingDiscoveryRequest discoveryRequest, IProgressMonitor monitor) {
      //keep it between us
    }

  }

}
