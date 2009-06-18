/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import org.maven.ide.eclipse.core.IMavenConstants;

/**
 * Maven project update request
 *
 * @author Eugene Kuleshov
 */
public class MavenUpdateRequest {
  private boolean offline = false;
  private boolean updateSnapshots = false;
  private boolean force = true;
  
  /**
   * Set of {@link IFile}
   */
  private final Set<IFile> pomFiles = new LinkedHashSet<IFile>();

  public MavenUpdateRequest(boolean offline, boolean updateSnapshots) {
    this.offline = offline;
    this.updateSnapshots = updateSnapshots;
  }
  
  public MavenUpdateRequest(IProject project, boolean offline, boolean updateSnapshots) {
    this(offline, updateSnapshots);
    addPomFile(project);
  }
  
  public MavenUpdateRequest(IProject[] projects, boolean offline, boolean updateSnapshots) {
    this(offline, updateSnapshots);
    
    for(int i = 0; i < projects.length; i++ ) {
      addPomFile(projects[i]);
    }
  }

  public boolean isOffline() {
    return this.offline;
  }
  
  public boolean isUpdateSnapshots() {
    return this.updateSnapshots;
  }

  public void addPomFiles(Set<IFile> pomFiles) {
    for (IFile pomFile : pomFiles) {
      addPomFile(pomFile);
    }
  }

  public void addPomFile(IFile pomFile) {
    pomFiles.add(pomFile);
  }
  
  public void addPomFile(IProject project) {
    pomFiles.add(project.getFile(IMavenConstants.POM_FILE_NAME));
    
  }

  public void removePomFile(IFile pomFile) {
    pomFiles.remove(pomFile);
  }
  
  /**
   * Returns Set of {@link IFile}
   */
  public Set<IFile> getPomFiles() {
    return this.pomFiles;
  }

  public boolean isEmpty() {
    return this.pomFiles.isEmpty();
  }

  public boolean isForce() {
    return force;
  }

  public void setForce(boolean force) {
    this.force = force;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder("[");
    String sep = "";
    for(IFile pomFile : pomFiles) {
      sb.append(sep);
      sb.append(pomFile.getFullPath());
      sep = ", ";
    }
    sb.append("]");
    
    if(offline) {
      sb.append(" offline");
    }
    if(updateSnapshots) {
      sb.append(" updateSnapshots");
    }
    if(force) {
      sb.append(" force");
    }
    
    return sb.toString();
  }

}
