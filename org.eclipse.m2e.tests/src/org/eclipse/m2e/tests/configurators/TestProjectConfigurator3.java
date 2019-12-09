/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.tests.configurators;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;


/**
 * @author Fred Bricon
 */
public class TestProjectConfigurator3 extends AbstractProjectConfigurator {

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {

    System.err.println("running " + getName());

    IPath logPath = new Path("target/configurator-log.txt");

    IFile file = request.getProject().getFile(logPath);
    String text = null;
    if(file.exists()) {
      try (InputStream contents = file.getContents(true)) {
        text = IOUtil.toString(contents) + ",";
      } catch(IOException ex) {
        throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.m2e.tests", ex.getLocalizedMessage()));
      }
    }
    text = StringUtils.defaultString(text) + getName();
    InputStream contents = new ByteArrayInputStream(text.getBytes());
    if(!file.exists()) {
      file.create(contents, IResource.FORCE, monitor);
    } else {
      file.setContents(contents, IResource.FORCE, monitor);
    }
  }

  @Override
  public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade, MojoExecution mojoExecution,
      IPluginExecutionMetadata executionMetadata) {
    MojoExecutionKey mojoExecutionKey = new MojoExecutionKey(mojoExecution);
    return new TestBuildParticipant(mojoExecutionKey);
  }
}
