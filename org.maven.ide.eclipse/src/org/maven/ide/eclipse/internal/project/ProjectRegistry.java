/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;

import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;


/**
 * Workspace state
 * 
 * @author Igor Fedorenko
 */
public class ProjectRegistry extends BasicProjectRegistry implements Serializable, IProjectRegistry {

  private static final long serialVersionUID = -8747318065115176241L;

  private int version;

  public synchronized MavenProjectFacade getProjectFacade(IFile pom) {
    return super.getProjectFacade(pom);
  }

  public synchronized MavenProjectFacade getProjectFacade(String groupId, String artifactId, String version) {
    return super.getProjectFacade(groupId, artifactId, version);
  }

  public synchronized MavenProjectFacade[] getProjects() {
    return super.getProjects();
  }

  public synchronized IPath getWorkspaceArtifact(ArtifactKey key) {
    return super.getWorkspaceArtifact(key);
  }

  public synchronized List<MavenProjectChangedEvent> apply(MutableProjectRegistry newState) throws StaleMutableProjectRegistryException {
    if (newState.isStale()) {
      throw new StaleMutableProjectRegistryException();
    }
    
    ArrayList<MavenProjectChangedEvent> events = new ArrayList<MavenProjectChangedEvent>();

    // removed projects
    for(MavenProjectFacade facade : workspacePoms.values()) {
      IPath pomFullPath = facade.getPom().getFullPath();
      if(!newState.workspacePoms.containsKey(pomFullPath)) {
        MavenProjectChangedEvent event = new MavenProjectChangedEvent( //
            facade.getPom(), //
            MavenProjectChangedEvent.KIND_REMOVED, //
            MavenProjectChangedEvent.FLAG_NONE, //
            facade /*old*/, //
            null /*new*/);
        events.add(event);
      }
    }

    // changed and new projects
    for(MavenProjectFacade facade : newState.workspacePoms.values()) {
      IPath pomFullPath = facade.getPom().getFullPath();
      MavenProjectFacade old = workspacePoms.get(pomFullPath);
      if(facade != old) { // not the same instance!
        MavenProjectChangedEvent event;
        if(old != null) {
          int flags = MavenProjectManagerImpl.hasDependencyChange(old.getMavenProject(), facade.getMavenProject()) ? MavenProjectChangedEvent.FLAG_DEPENDENCIES
              : MavenProjectChangedEvent.FLAG_NONE;
          event = new MavenProjectChangedEvent(facade.getPom(), //
              MavenProjectChangedEvent.KIND_CHANGED, //
              flags, //
              old /*old*/, //
              facade /*new*/);

        } else {
          event = new MavenProjectChangedEvent(facade.getPom(), //
              MavenProjectChangedEvent.KIND_ADDED, //
              MavenProjectChangedEvent.FLAG_NONE, //
              null /*old*/, //
              facade /*new*/);
        }
        events.add(event);
      }
    }

    replaceWith(newState);

    version++;

    return events;
  }

  public synchronized int getVersion() {
    return version;
  }
}
