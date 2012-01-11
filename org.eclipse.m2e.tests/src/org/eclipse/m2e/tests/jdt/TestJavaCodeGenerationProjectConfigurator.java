/*******************************************************************************
 * Copyright (c) 2012 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.m2e.jdt.AbstractJavaProjectConfigurator;


public class TestJavaCodeGenerationProjectConfigurator extends AbstractJavaProjectConfigurator {

  public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade, MojoExecution execution,
      IPluginExecutionMetadata executionMetadata) {
    return new AbstractBuildParticipant() {
      public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
        MavenProject mavenProject = getMavenProjectFacade().getMavenProject(monitor);

        new File(mavenProject.getBuild().getDirectory(), "generated-sources/test/test.txt").createNewFile();

        return null;
      }
    };
  }

  protected File[] getSourceFolders(ProjectConfigurationRequest request, MojoExecution mojoExecution) {
    return new File[] {new File(request.getMavenProject().getBuild().getDirectory(), "generated-sources/test")};
  }
}
