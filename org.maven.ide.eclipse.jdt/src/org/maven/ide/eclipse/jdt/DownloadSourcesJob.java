/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;


/**
 * DownloadSourcesJob
 * 
 * @author igor
 */
class DownloadSourcesJob extends Job {

  private static class DownloadRequest {

    final IProject project;

    final IPath path;

    final ArrayList<ArtifactKey> artifacts;

    final boolean downloadSources;

    final boolean downloadJavaDoc;

    public DownloadRequest(IProject project, IPath path, Set<ArtifactKey> artifacts, boolean downloadSources,
        boolean downloadJavaDoc) {
      this.project = project;
      this.path = path;
      this.artifacts = new ArrayList<ArtifactKey>(artifacts);
      this.downloadSources = downloadSources;
      this.downloadJavaDoc = downloadJavaDoc;
    }

  }

  private final IMaven maven;

  private final BuildPathManager manager;

  private final MavenConsole console;

  private final MavenProjectManager projectManager;

  private final IndexManager indexManager;

  private final ArrayList<DownloadRequest> queue = new ArrayList<DownloadRequest>();

  public DownloadSourcesJob(BuildPathManager manager) {
    super("Download sources and javadoc");
    this.manager = manager;

    this.maven = MavenPlugin.lookup(IMaven.class);

    MavenPlugin plugin = MavenPlugin.getDefault();
    this.projectManager = plugin.getMavenProjectManager();
    this.indexManager = plugin.getIndexManager();
    this.console = plugin.getConsole();
  }

  public IStatus run(IProgressMonitor monitor) {

    ArrayList<DownloadRequest> requests;

    synchronized(this.queue) {
      requests = new ArrayList<DownloadRequest>(this.queue);
      this.queue.clear();
    }

    ArrayList<IStatus> exceptions = new ArrayList<IStatus>();

    for(DownloadRequest request : requests) {
      try {
        downloadSources(request, monitor);
      } catch(CoreException ex) {
        exceptions.add(ex.getStatus());
      }
    }

    if(!exceptions.isEmpty()) {
      IStatus[] problems = exceptions.toArray(new IStatus[exceptions.size()]);
      return new MultiStatus(MavenJdtPlugin.PLUGIN_ID, -1, problems, "Could not download sources or javadoc", null);
    }

    return Status.OK_STATUS;
  }

  private void downloadSources(DownloadRequest request, IProgressMonitor monitor) throws CoreException {

    List<ArtifactRepository> repositories;

    IMavenProjectFacade projectFacade = projectManager.create(request.project, monitor);

    if(projectFacade != null) {
      repositories = projectFacade.getMavenProject(monitor).getRemoteArtifactRepositories();
    } else {
      repositories = indexManager.getArtifactRepositories(null, null);
    }

    Artifact[] sources = new Artifact[request.artifacts.size()];
    String[] javadocUrls = new String[request.artifacts.size()];

    boolean update = false;

    for(int i = 0; i < request.artifacts.size(); i++ ) {
      ArtifactKey artifact = request.artifacts.get(i);

      if(request.downloadSources) {
        try {
          sources[i] = downloadSources(artifact, repositories, monitor);

          console.logMessage("Downloaded sources for " + artifact.toString());

          update |= true;

        } catch(CoreException e) {
          logMessage("Could not download sources for " + artifact.toString(), e);
        }
      }

      if(request.downloadJavaDoc) {
        try {
          Artifact javadoc = downloadJavadoc(artifact, repositories, monitor);
          javadocUrls[i] = BuildPathManager.getJavaDocUrl(javadoc.getFile());

          console.logMessage("Downloaded javadoc for " + artifact.toString());

          update |= true;

        } catch(CoreException e) {
          logMessage("Could not download javadoc for " + artifact.toString(), e);
        }
      }

    }

    if(update) {
      ISchedulingRule schedulingRule = request.project.getWorkspace().getRuleFactory().buildRule();
      getJobManager().beginRule(schedulingRule, monitor);
      try {
        IPath srcPath = (sources[0] != null) ? Path.fromOSString(sources[0].getFile().getAbsolutePath()) : null;
        manager.setSourcePath(request.project, request.path, srcPath, javadocUrls[0], monitor);
      } finally {
        getJobManager().endRule(schedulingRule);
      }
    }
  }

  private void logMessage(String msg, CoreException e) {
    MavenLogger.log(msg, e);
    console.logMessage(msg);
  }

  private Artifact downloadJavadoc(ArtifactKey artifact, List<ArtifactRepository> repositories, IProgressMonitor monitor)
      throws CoreException {
    return maven.resolve(artifact.getGroupId(), //
        artifact.getArtifactId(), //
        artifact.getVersion(), //
        "jar" /*type*/, //
        BuildPathManager.CLASSIFIER_JAVADOC, // 
        repositories, //
        monitor);
  }

  private Artifact downloadSources(ArtifactKey artifact, List<ArtifactRepository> repositories, IProgressMonitor monitor)
      throws CoreException {
    return maven.resolve(artifact.getGroupId(), //
        artifact.getArtifactId(), //
        artifact.getVersion(), //
        "jar" /*type*/, //
        BuildPathManager.getSourcesClassifier(artifact.getClassifier()), // 
        repositories, //
        monitor);
  }

  public void scheduleDownload(IProject project, IPath path, Set<ArtifactKey> artifacts, boolean downloadSources,
      boolean downloadJavaDoc) {
    synchronized(this.queue) {
      queue.add(new DownloadRequest(project, path, artifacts, downloadSources, downloadJavaDoc));
    }

    schedule(1000L);
  }

}
