/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.apache.maven.artifact.Artifact;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.container.MavenClasspathContainer;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.internal.project.MavenProjectManagerImpl;
import org.maven.ide.eclipse.internal.project.MavenProjectManagerRefreshJob;


/**
 * This class keeps track of all maven projects present in the workspace and provides mapping between Maven artifacts
 * and Workspace projects.
 */
public class MavenProjectManager {

  private MavenProjectManagerImpl manager;

  private MavenProjectManagerRefreshJob mavenBackgroundJob;

  private IndexManager indexManager;

  public MavenProjectManager(MavenProjectManagerImpl manager, IndexManager indexManager, MavenProjectManagerRefreshJob mavenBackgroundJob) {
    this.manager = manager;
    this.indexManager = indexManager;
    this.mavenBackgroundJob = mavenBackgroundJob;
  }

  // Maven projects    

  public void refresh(IProject[] projects, boolean offline, boolean updateSnapshots) {
    mavenBackgroundJob.refresh(new MavenUpdateRequest(projects, offline, updateSnapshots));
  }
  public void refresh(IProject project, boolean offline, boolean updateSnapshots) {
    mavenBackgroundJob.refresh(new MavenUpdateRequest(project, offline, updateSnapshots));
  }

  public void addMavenProjectChangedListener(IMavenProjectChangedListener listener) {
    manager.addMavenProjectChangedListener(listener);
  }

  public void removeMavenProjectChangedListener(IMavenProjectChangedListener listener) {
    manager.removeMavenProjectChangedListener(listener);
  }

  /**
   * Returns MavenProjectFacade corresponding to the pom. This method first looks in the project cache, then attempts to
   * load the pom if the pom is not found in the cache. In the latter case, workspace resolution is assumed to be
   * enabled for the pom but the pom will not be added to the cache.
   */
  public MavenProjectFacade create(IFile pom, boolean load, IProgressMonitor monitor) {
    return manager.create(pom, load, monitor);
  }

  public MavenProjectFacade create(IProject project, IProgressMonitor monitor) {
    return manager.create(project, monitor);
  }

  public MavenProjectFacade getMavenProject(Artifact artifact) {
    return manager.getMavenProject(artifact);
  }
  
  public ResolverConfiguration getResolverConfiguration(IProject project) {
    MavenProjectFacade projectFacade = create(project, new NullProgressMonitor());
    if(projectFacade!=null) {
      return projectFacade.getResolverConfiguration();
    }
    return manager.readResolverConfiguration(project);
  }

  public boolean setResolverConfiguration(IProject project, ResolverConfiguration configuration) {
    return manager.setResolverConfiguration(project, configuration);
  }
  
  // Downloading sources

  public void downloadSources(IProject project, IPath path, String groupId, String artifactId, String version,
      String classifier) {
    mavenBackgroundJob.downloadSources(project, path, groupId, artifactId, version, classifier);
  }

  public void addDownloadSourceListener(IDownloadSourceListener listener) {
    manager.addDownloadSourceListener(listener);
  }

  public void removeDownloadSourceListener(IDownloadSourceListener listener) {
    manager.removeDownloadSourceListener(listener);
  }

  /**
   * If sources artifact is available in the local repo, return IPath of this artifact. Otherwise, if automatic source
   * download is enabled in the preferences, the method will do the following If sources artifact is available in any
   * remote repo, schedule background download of the sources and return null. DownloadSourceListeners will be notified
   * upon download completion. Otherwise, if javadoc artifact is not available from the local repo but is available from
   * any remote repo, schedule background download of javadoc and return null. DownloadSourceListeners will be notified
   * upon download completion. Otherwise, return null.
   */
  public IPath getSourcePath(IProject project, IPath path, Artifact artifact, boolean forceDownload) {
    IPath srcPath = getArtifactPath(artifact, MavenProjectManagerImpl.ARTIFACT_TYPE_JAVA_SOURCE);

    if(srcPath != null) {
      return srcPath;
    }

    if(!forceDownload) {
      return null;
    }

    IndexedArtifactFile af;
    try {
      af = indexManager.getIndexedArtifactFile(IndexManager.LOCAL_INDEX, indexManager.getDocumentKey(artifact));
    } catch(Exception ex) {
      MavenPlugin.log(ex.getMessage(), ex);
      return null;
    }

    if(af == null) {
      return null;
    }

    // download if sources artifact is available from a remote repo
    boolean shouldDownload = af.sourcesExists != IndexManager.NOT_AVAILABLE;

    // download if javadoc is not available in the local repo
    // but is available from a remote repo
    if(getArtifactPath(artifact, MavenProjectManagerImpl.ARTIFACT_TYPE_JAVADOC) == null) {
      shouldDownload |= af.javadocExists != IndexManager.NOT_AVAILABLE;
    }

    if(shouldDownload) {
      downloadSources(project, path, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact
          .getClassifier());
    }

    return null;
  }

  /**
   * If javadoc artifact is available in the local repo, return URL of this artifact. Otherwise, return null
   */
  public String getJavaDocUrl(Artifact artifact) {
    IPath javadocPath = getArtifactPath(artifact, MavenProjectManagerImpl.ARTIFACT_TYPE_JAVADOC);

    String javadocUrl = null;
    if(javadocPath != null) {
      javadocUrl = MavenClasspathContainer.getJavaDocUrl(javadocPath.toString());
    }

    return javadocUrl;
  }
  
  /**
   * Returns local file system path if the requested artifact/type/classifier
   * is available in the local repository. Returns null if the requested artifact/type/classifier
   * is not available in the local repository. 
   */
  private IPath getArtifactPath(Artifact artifact, String type) {
    File artifactFile = artifact.getFile();
    if(artifactFile == null) {
      return null;
    }

    String classifier = manager.getClassifier(type, artifact.getClassifier());

    StringBuffer filename = new StringBuffer();
    filename.append(artifact.getArtifactId());
    filename.append("-").append(artifact.getVersion());
    filename.append("-").append(classifier);
    filename.append(".jar");
    
    File file = new File(artifactFile.getParent(), filename.toString());

    if(file.exists()) {
      // workaround to not download already existing archive
      return new Path(file.getAbsolutePath());
    }

    return null;
  }

  /**
   * @return MavenProjectFacade[] all maven projects which exist under workspace root 
   */
  public MavenProjectFacade[] getProjects() {
    return manager.getProjects();
  }

}
