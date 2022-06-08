/*******************************************************************************
 * Copyright (c) 2012 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.jdt;

import java.io.File;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.AbstractSourcesGenerationProjectConfigurator;


public class TestJavaCodeGenerationProjectConfigurator extends AbstractSourcesGenerationProjectConfigurator {

  @Override
  public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade, MojoExecution execution,
      IPluginExecutionMetadata executionMetadata) {
    return new AbstractBuildParticipant() {
      @Override
      public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
        MavenProject mavenProject = getMavenProjectFacade().getMavenProject(monitor);

        new File(mavenProject.getBuild().getDirectory(), "generated-sources/test/test.txt").createNewFile();

        return null;
      }
    };
  }

  @Override
  protected File[] getSourceFolders(ProjectConfigurationRequest request, MojoExecution mojoExecution,
      IProgressMonitor monitor) {
    return new File[] {new File(request.mavenProject().getBuild().getDirectory(), "generated-sources/test")};
  }
}
