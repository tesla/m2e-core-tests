/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Model;

/**
 * @author Eugene Kuleshov
 */
public class MavenProjectInfo {

  private final String label;

  private File pomFile;

  private Model model;

  private final MavenProjectInfo parent;

  /**
   * Map of MavenProjectInfo
   */
  private final Map projects = new LinkedHashMap();

  private final Set profiles = new HashSet();


  public MavenProjectInfo(String label, File pomFile, Model model, MavenProjectInfo parent) {
    this.label = label;
    this.pomFile = pomFile;
    this.model = model;
    this.parent = parent;
  }

  public void setPomFile(File pomFile) {
    File oldDir = this.pomFile.getParentFile();
    File newDir = pomFile.getParentFile();
    
    for(Iterator it = projects.values().iterator(); it.hasNext();) {
      MavenProjectInfo projectInfo = (MavenProjectInfo) it.next();
      File childPom = projectInfo.getPomFile();
      if(isSubDir(oldDir, childPom.getParentFile())) {
        String oldPath = oldDir.getAbsolutePath();
        String path = childPom.getAbsolutePath().substring(oldPath.length());
        projectInfo.setPomFile(new File(newDir, path));
      }
    }
    
    this.pomFile = pomFile;
  }

  private boolean isSubDir(File parentDir, File subDir) {
    if(parentDir.equals(subDir)) {
      return true;
    }
    
    if(subDir.getParentFile()!=null) {
      return isSubDir(parentDir, subDir.getParentFile());
    }
    
    return false;
  }

  public void add(MavenProjectInfo info) {
    String key = info.getLabel();
    MavenProjectInfo i = (MavenProjectInfo) projects.get(key);
    if(i==null) {
      projects.put(key, info);
    } else {
      for(Iterator it = info.getProfiles().iterator(); it.hasNext();) {
        i.addProfile((String) it.next());
      }
    }
  }
  
  public void addProfile(String profileId) {
    if(profileId!=null) {
      profiles.add(profileId);
    }
  }
  
  public void addProfiles(Collection profiles) {
    profiles.addAll(profiles);
  }
  
  public String getLabel() {
    return this.label;
  }
  
  public File getPomFile() {
    return this.pomFile;
  }
  
  public Model getModel() {
    return this.model;
  }
  
  public void setModel(Model model) {
    this.model = model;
  }
  
  public Collection getProjects() {
    return this.projects.values();
  }
 
  public MavenProjectInfo getParent() {
    return this.parent;
  }
  
  public Set getProfiles() {
    return this.profiles;
  }
  
  public String toString() {
    return label + " " + pomFile.getAbsolutePath();
  }

}
