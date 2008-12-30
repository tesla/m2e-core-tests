/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr.internal.sources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.maven.ide.eclipse.pr.internal.ProblemReportingPlugin;
import org.maven.ide.eclipse.pr.internal.data.IDataSource;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;


/**
 * Returns the effective POM for a project.
 */
public class EffectivePomSource implements IDataSource {

  private final MavenProjectManager mavenProjectManager;

  private final IFile file;

  private final IProgressMonitor monitor;

  public EffectivePomSource(MavenProjectManager mavenProjectManager, IFile file, IProgressMonitor monitor) {
    this.mavenProjectManager = mavenProjectManager;
    this.file = file;
    this.monitor = monitor;
  }

  public InputStream getInputStream() throws CoreException {
    IMavenProjectFacade projectFacade;
    try {
      projectFacade = mavenProjectManager.create(file, true, monitor);
    } catch(Exception ex) {
      throw new CoreException(new Status(IStatus.ERROR, ProblemReportingPlugin.PLUGIN_ID, -1, //
          "Can't get project facade for " + file.getLocation(), ex));
    }
    if(projectFacade == null) {
      throw new CoreException(new Status(IStatus.ERROR, ProblemReportingPlugin.PLUGIN_ID, -1, //
          "Can't get project facade for " + file.getLocation(), null));
    }

    StringWriter sw = new StringWriter();
    try {
      new MavenXpp3Writer().write(sw, projectFacade.getMavenProject(monitor).getModel());
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, ProblemReportingPlugin.PLUGIN_ID, -1, //
          "Can't write effective pom source for " + file.getLocation(), ex));
    }

    return new ByteArrayInputStream(sw.getBuffer().toString().getBytes());
  }

  public String getName() {
    return "pom-effective.pom";
  }

}
