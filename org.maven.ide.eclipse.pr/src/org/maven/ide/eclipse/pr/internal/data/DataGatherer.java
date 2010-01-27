/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr.internal.data;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.pr.IDataGatherer;
import org.maven.ide.eclipse.pr.IDataSource;
import org.maven.ide.eclipse.pr.IDataTarget;
import org.maven.ide.eclipse.pr.internal.ProblemReportingPlugin;
import org.maven.ide.eclipse.pr.internal.sources.StatusSource;
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

  public List<File> gather(File bundleDir, Set<Data> dataSet, IProgressMonitor monitor) throws IOException {
    // project sources go into second bundle (if any), all the rest goes into first/primary bundle
    List<File> bundleFiles = new ArrayList<File>();

    List<IDataGatherer> dataGatherers = DataGathererFactory.getDataGatherers();

    monitor.beginTask("Gathering", dataSet.size() + dataGatherers.size());

    // There's a size limit on JIRA attachments, so we split logs, config etc. from the project sources
    Set<Data> set1 = EnumSet.copyOf(dataSet);
    set1.remove(Data.MAVEN_SOURCES);
    Set<Data> set2 = EnumSet.copyOf(dataSet);
    set2.removeAll(set1);

    try {
      if(!set2.isEmpty()) {
        File bundleFile = File.createTempFile("bundle", ".zip", bundleDir);
        bundleFiles.add(bundleFile);

        gather(bundleFile, set2, Collections.<IDataGatherer> emptyList(), false, monitor);

        if(bundleFile.length() <= 0) {
          bundleFile.delete();
          bundleFiles.remove(bundleFile);
        }
      }

      // primary bundle with the potential status logs is created last
      {
        File bundleFile = File.createTempFile("bundle", ".zip", bundleDir);
        bundleFiles.add(0, bundleFile);

        gather(bundleFile, set1, dataGatherers, true, monitor);
      }
    } catch(IOException e) {
      for(File bundleFile : bundleFiles) {
        bundleFile.delete();
      }
      throw e;
    }

    monitor.done();

    return bundleFiles;
  }

  private void gather(File bundleFile, Set<Data> dataSet, List<IDataGatherer> dataGatherers, boolean status,
      IProgressMonitor monitor) throws IOException {
    ZipOutputStream zos = null;
    try {
      zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(bundleFile)));
      IDataTarget target = new ArchiveTarget(zos);
      gather(target, dataSet, dataGatherers, monitor);
      if(status) {
        gatherStatus(target);
      }
      zos.flush();
    } finally {
      IOUtil.close(zos);
    }
  }

  private void gather(IDataTarget target, Set<Data> dataSet, List<IDataGatherer> dataGatherers, IProgressMonitor monitor) {
    for(Data data : dataSet) {
      try {
        data.gather(this, target, monitor);
      } catch(Exception e) {
        addStatus(new Status(IStatus.ERROR, ProblemReportingPlugin.PLUGIN_ID,
            "Failure while gathering problem report data: " + e.getMessage(), e));
      }
      monitor.worked(1);
    }

    DataGatheringContext context = new DataGatheringContext(target, monitor);
    for(IDataGatherer dataGatherer : dataGatherers) {
      try {
        dataGatherer.gather(context);
      } catch(CoreException ex) {
        MavenLogger.log(ex);
        addStatus(ex.getStatus());
      } catch(Exception e) {
        addStatus(new Status(IStatus.ERROR, ProblemReportingPlugin.PLUGIN_ID,
            "Failure while gathering problem report data: " + e.getMessage(), e));
      }
      monitor.worked(1);
    }
  }

  void gather(String folderName, IDataTarget target, IDataSource source) {
    try {
      target.consume(folderName, source);
    } catch(CoreException ex) {
      MavenLogger.log(ex);
      addStatus(ex.getStatus());
    } catch(Exception e) {
      addStatus(new Status(IStatus.ERROR, ProblemReportingPlugin.PLUGIN_ID,
          "Failure while gathering problem report data: " + e.getMessage(), e));
    }
  }

  private void gatherStatus(IDataTarget target) {
    if(!statuses.isEmpty()) {
      int index = 0;
      for(IStatus status : statuses) {
        try {
          target.consume("pr", new StatusSource(status, "status-" + index + ".txt"));
        } catch(Exception e) {
          MavenLogger.log("Failed to save status to problem report", e);
        }
        index++ ;
      }
    }
  }

  public void addStatus(IStatus status) {
    statuses.add(status);
  }
  
}
