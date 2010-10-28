/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.pr.internal.data;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
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
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.pr.IDataGatherer;
import org.eclipse.m2e.pr.IDataSource;
import org.eclipse.m2e.pr.IDataTarget;
import org.eclipse.m2e.pr.internal.ProblemReportingPlugin;
import org.eclipse.m2e.pr.internal.sources.StatusSource;
import org.sonatype.plexus.encryptor.PlexusEncryptor;
import org.sonatype.plexus.encryptor.RsaAesPlexusEncryptor;


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

  private final URL publicKey;

  public DataGatherer(IMavenConfiguration mavenConfiguration, MavenProjectManager mavenProjectManager,
      MavenConsole console, IWorkspace workspace, Set<IProject> projects, URL publicKey) {
    this.mavenConfiguration = mavenConfiguration;
    this.projectManager = mavenProjectManager;
    this.console = console;
    this.workspace = workspace;
    this.projects = projects;
    this.publicKey = publicKey;
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

    SubMonitor progress = SubMonitor.convert(monitor, "Gathering problem data", dataSet.size() + dataGatherers.size() + 1);

    // There's a size limit on JIRA attachments, so we split logs, config etc. from the project sources
    Set<Data> set1 = EnumSet.copyOf(dataSet);
    set1.remove(Data.MAVEN_SOURCES);
    Set<Data> set2 = EnumSet.copyOf(dataSet);
    set2.removeAll(set1);

    try {
      if(!set2.isEmpty()) {
        File bundleFile = File.createTempFile("bundle", ".zip", bundleDir);
        bundleFiles.add(bundleFile);

        gather(bundleFile, set2, Collections.<IDataGatherer> emptyList(), false, progress);

        if(bundleFile.length() <= 0) {
          bundleFile.delete();
          bundleFiles.remove(bundleFile);
        }
      }

      // primary bundle with the potential status logs is created last
      {
        File bundleFile = File.createTempFile("bundle", ".zip", bundleDir);
        bundleFiles.add(0, bundleFile);

        gather(bundleFile, set1, dataGatherers, true, progress);
      }
    } catch(IOException e) {
      for(File bundleFile : bundleFiles) {
        bundleFile.delete();
      }
      throw e;
    }

    bundleFiles = encryptBundles(bundleFiles, bundleDir, progress.newChild(1));

    progress.done();

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

  private List<File> encryptBundles(List<File> inputFiles, File bundleDir, IProgressMonitor monitor) throws IOException {
    SubMonitor progress = SubMonitor.convert(monitor, "Encrypting problem data", inputFiles.size());

    if(publicKey == null) {
      return inputFiles;
    }

    List<File> bundleFiles = new ArrayList<File>();

    PlexusEncryptor encryptor = new RsaAesPlexusEncryptor();

    for(File input : inputFiles) {
      File output = File.createTempFile("bundle", ".ezip", bundleDir);
      bundleFiles.add(output);
      try {
        encryptor.encrypt(input, output, publicKey.openStream());
      } catch(GeneralSecurityException ex) {
        throw (IOException) new IOException(ex.getMessage()).initCause(ex);
      }
      input.delete();
      progress.worked(1);
    }

    return bundleFiles;
  }

  // NOTE: This exists solely to allow unit testing
  public List<File> decryptBundles(List<File> inputFiles, File bundleDir, URL privateKey) throws IOException {
    if(privateKey == null) {
      return inputFiles;
    }

    List<File> bundleFiles = new ArrayList<File>();

    PlexusEncryptor encryptor = new RsaAesPlexusEncryptor();

    for(File input : inputFiles) {
      File output = File.createTempFile("bundle", ".zip", bundleDir);
      bundleFiles.add(output);
      InputStream is = null;
      OutputStream os = null;
      try {
        is = new FileInputStream(input);
        os = new FileOutputStream(output);
        encryptor.decrypt(is, os, privateKey.openStream());
      } catch(GeneralSecurityException ex) {
        throw (IOException) new IOException(ex.getMessage()).initCause(ex);
      } finally {
        if(is != null) {
          is.close();
        }
        if(os != null) {
          os.close();
        }
      }
      input.delete();
    }

    return bundleFiles;
  }

}
