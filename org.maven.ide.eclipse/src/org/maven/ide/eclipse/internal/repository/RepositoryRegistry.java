/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.repository;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.authentication.AuthenticationInfo;

import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.ArtifactRepositoryRef;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.embedder.ISettingsChangeListener;
import org.maven.ide.eclipse.project.IMavenProjectChangedListener;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.repository.IRepository;
import org.maven.ide.eclipse.repository.IRepositoryRegistry;

/**
 * RepositoryRegistry
 *
 * @author igor
 */
public class RepositoryRegistry implements IRepositoryRegistry, IMavenProjectChangedListener, ISettingsChangeListener {

  private final IMaven maven;

  private final MavenProjectManager projectManager;

  /**
   * Maps repositoryUrl to IndexInfo of repository index
   */
  private final Map<String, RepositoryInfo> repositories = new HashMap<String, RepositoryInfo>();

  private RepositoryInfo localRepository;

  private RepositoryInfo workspaceRepository;

  private ArrayList<IRepositoryIndexer> indexers = new ArrayList<IRepositoryIndexer>();

  private ArrayList<IRepositoryDiscoverer> discoverers = new ArrayList<IRepositoryDiscoverer>();

  private Job job = new Job("Repository registry initialization") {
    protected IStatus run(IProgressMonitor monitor) {
      try {
        updateRegistry(monitor);
      } catch(CoreException ex) {
        return ex.getStatus();
      }
      return Status.OK_STATUS;
    }
  };
  
  public RepositoryRegistry(IMaven maven, MavenProjectManager projectManager) throws CoreException {
    this.maven = maven;
    this.projectManager = projectManager;

    File localBasedir = new File(maven.getLocalRepository().getBasedir());
    try {
      localBasedir = localBasedir.getCanonicalFile();
    } catch (IOException e) {
      // will never happen
      localBasedir = localBasedir.getAbsoluteFile();
    }

    String localUrl;
    try {
      localUrl = localBasedir.toURL().toExternalForm();
    } catch(MalformedURLException ex) {
      MavenLogger.log("Could not parse local repository path", ex);
      localUrl = "file://" + localBasedir.getAbsolutePath();
    }

    // initialize local and workspace repositories
    this.localRepository = new RepositoryInfo(null/*id*/, localUrl, localBasedir, SCOPE_LOCAL, null/*auth*/);
    this.workspaceRepository = new RepositoryInfo(null/*id*/, "workspace://"/*url*/, null/*basedir*/, SCOPE_WORKSPACE, null/*auth*/);
  }

  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    /*
     * This method is called while holding workspace lock. Avoid long-running operations if possible. 
     */

    Settings settings = null;
    try {
      settings = maven.getSettings();
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }

    for(MavenProjectChangedEvent event : events) {
      IMavenProjectFacade oldFacade = event.getOldMavenProject();
      if (oldFacade != null) {
        removeProjectRepositories(oldFacade, monitor);
      }
      IMavenProjectFacade facade = event.getMavenProject();
      if(facade != null) {
        addProjectRepositories(settings, facade, null /*asyncUpdate*/);
      }
    }
  }

  private void addProjectRepositories(Settings settings, IMavenProjectFacade facade, IProgressMonitor monitor) {
    ArrayList<ArtifactRepositoryRef> repositories = getProjectRepositories(facade);

    for (ArtifactRepositoryRef repo : repositories) {
      RepositoryInfo repository = getRepository(repo);
      if (repository != null) {
        repository.addProject(facade.getPom().getFullPath());
        continue;
      }
      AuthenticationInfo auth = getAuthenticationInfo(settings, repo.getId());
      repository = new RepositoryInfo(repo.getId(), repo.getUrl(), SCOPE_PROJECT, auth);
      repository.addProject(facade.getPom().getFullPath());

      addRepository(repository, monitor);
    }
  }

  public void addRepository(RepositoryInfo repository, IProgressMonitor monitor) {
    repositories.put(repository.getUid(), repository);

    for (IRepositoryIndexer indexer : indexers) {
      try {
        indexer.repositoryAdded(repository, monitor);
      } catch (CoreException e) {
        MavenLogger.log(e);
      }
    }
  }

  private void removeProjectRepositories(IMavenProjectFacade facade, IProgressMonitor monitor) {
    ArrayList<ArtifactRepositoryRef> repositories = getProjectRepositories(facade);

    for (ArtifactRepositoryRef repo : repositories) {
      RepositoryInfo repository = getRepository(repo);
      if (repository != null && repository.isScope(SCOPE_PROJECT)) {
        repository.removeProject(facade.getPom().getFullPath());
        if (repository.getProjects().isEmpty()) {
          removeRepository(repository, monitor);
        }
      }
    }
  }

  private void removeRepository(RepositoryInfo repository, IProgressMonitor monitor) {
    repositories.remove(repository.getUid());

    for (IRepositoryIndexer indexer : indexers) {
      try {
        indexer.repositoryRemoved(repository, monitor);
      } catch (CoreException e) {
        MavenLogger.log(e);
      }
    }
  }

  private ArrayList<ArtifactRepositoryRef> getProjectRepositories(IMavenProjectFacade facade) {
    ArrayList<ArtifactRepositoryRef> repositories = new ArrayList<ArtifactRepositoryRef>();
    repositories.addAll(facade.getArtifactRepositoryRefs());
    repositories.addAll(facade.getPluginArtifactRepositoryRefs());
    return repositories;
  }


  private AuthenticationInfo getAuthenticationInfo(Settings settings, String id) {
    if (settings == null) {
      return null;
    }

    Server server = settings.getServer(id);
    if (server == null || server.getUsername() == null) {
      return null;
    }

    AuthenticationInfo info = new AuthenticationInfo();
    info.setUserName(server.getUsername());
    info.setPassword(server.getPassword());
    return info;
  }

  public void updateRegistry(IProgressMonitor monitor) throws CoreException {
    Settings settings = maven.getSettings();
    List<Mirror> mirrors = maven.getMirrors();

    // initialize indexers
    for (IRepositoryIndexer indexer : indexers) {
      indexer.initialize(monitor);
    }

    // process configured repositories

    Map<String, RepositoryInfo> oldRepositories = new HashMap<String, RepositoryInfo>(repositories);
    repositories.clear();

    addRepository(this.workspaceRepository, monitor);
    addRepository(this.localRepository, monitor);

    // mirrors
    for(Mirror mirror : mirrors) {
      AuthenticationInfo auth = getAuthenticationInfo(settings, mirror.getId());
      RepositoryInfo repository = new RepositoryInfo(mirror.getId(), mirror.getUrl(), SCOPE_SETTINGS, auth);
      repository.setMirrorOf(mirror.getMirrorOf());
      addRepository(repository, monitor);
    }

    // repositories from settings.xml
    ArrayList<ArtifactRepository> repos = new ArrayList<ArtifactRepository>();
    repos.addAll(maven.getArtifactRepositories(false));
    repos.addAll(maven.getPluginArtifactRepositories(false));

    for(ArtifactRepository repo : repos) {
      Mirror mirror = maven.getMirror(repo);
      AuthenticationInfo auth = getAuthenticationInfo(settings, repo.getId());
      RepositoryInfo repository = new RepositoryInfo(repo.getId(), repo.getUrl(), SCOPE_SETTINGS, auth);
      if (mirror != null) {
        repository.setMirrorId(mirror.getId());
      }
      addRepository(repository, monitor);
    }

    // project-specific repositories
    for (IMavenProjectFacade facade : projectManager.getProjects()) {
      addProjectRepositories(settings, facade, monitor);
    }

    // custom repositories
    for (IRepositoryDiscoverer discoverer : discoverers) {
      discoverer.addRepositories(this, monitor);
    }

    oldRepositories.keySet().removeAll(repositories.keySet());
    for (RepositoryInfo repository : oldRepositories.values()) {
      removeRepository(repository, monitor);
    }
  }

  public List<IRepository> getRepositories(int scope) {
    ArrayList<IRepository> result = new ArrayList<IRepository>();
    for (RepositoryInfo repository : repositories.values()) {
      if (repository.isScope(scope)) {
        result.add(repository);
      }
    }
    return result;
  }

  public void updateRegistry() {
    job.schedule(1000L);
  }

  public void addRepositoryIndexer(IRepositoryIndexer indexer) {
    this.indexers.add(indexer);
  }

  public void addRepositoryDiscoverer(IRepositoryDiscoverer discoverer) {
    this.discoverers.add(discoverer);
  }

  public RepositoryInfo getRepository(ArtifactRepositoryRef ref) {
    String uid = RepositoryInfo.getUid(ref.getId(), ref.getUrl(), ref.getUsername());
    return repositories.get(uid);
  }

  public IRepository getWorkspaceRepository() {
    return workspaceRepository;
  }

  public IRepository getLocalRepository() {
    return localRepository;
  }
  
  public void settingsChanged(Settings settings) {
    updateRegistry();
  }

  public Job getBackgroundJob() {
    return job;
  }
}
