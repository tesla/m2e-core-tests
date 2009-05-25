/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr.internal.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.pr.internal.ProblemReportingPlugin;
import org.maven.ide.eclipse.project.MavenProjectManager;


/**
 * Gather various resources to aid in problem determination.
 */
public class DataGatherer {

  private final IMavenConfiguration mavenConfiguration;

  private final MavenProjectManager projectManager;

  private final MavenConsole console;

  private final IWorkspace workspace;

  private final Set<IProject> projects;

  private final List<IStatus> statuses = new ArrayList<IStatus>();

  public DataGatherer(IMavenConfiguration mavenConfiguration, MavenProjectManager mavenProjectManager,
      MavenConsole console, IWorkspace workspace, Set<IProject> projects) {
    this.mavenConfiguration = mavenConfiguration;
    this.projectManager = mavenProjectManager;
    this.console = console;
    this.workspace = workspace;
    this.projects = projects;
  }

  public IMavenConfiguration getMavenConfiguration() {
    return mavenConfiguration;
  }

  public MavenProjectManager getProjectManager() {
    return projectManager;
  }

  public MavenConsole getConsole() {
    return console;
  }

  public IWorkspace getWorkspace() {
    return workspace;
  }

  public Set<IProject> getProjects() {
    return projects;
  }

  public IStatus getStatus() {
    if(statuses.size() == 0) {
      return Status.OK_STATUS;
    }
    return new MultiStatus(ProblemReportingPlugin.PLUGIN_ID, -1, statuses.toArray(new IStatus[statuses.size()]),
        "Error gathering data", null);
  }

  public void gather(IDataTarget target, Set<Data> dataSet, IProgressMonitor monitor) {
    monitor.beginTask("Gathering", dataSet.size());
    for(Data data : dataSet) {
      data.gather(this, target, monitor);
      monitor.worked(1);
    }
    monitor.done();
  }

  void gather(String folderName, IDataTarget target, IDataSource source) {
    try {
      target.consume(folderName, source);
    } catch(CoreException ex) {
      MavenLogger.log(ex);
      addStatus(ex.getStatus());
    }
  }

  public void addStatus(IStatus status) {
    statuses.add(status);
  }
  
}
