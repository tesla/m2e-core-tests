/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.MavenModelManager;


/**
 * @author Eugene Kuleshov
 */
public class LocalProjectScanner extends AbstractProjectScanner {
  private final Collection folders;
  private final boolean needsRename;

  public LocalProjectScanner(String folder, boolean needsRename) {
    this(Collections.singletonList(folder), needsRename);
  }

  public LocalProjectScanner(Collection folders, boolean needsRename) {
    this.folders = folders;
    this.needsRename = needsRename;
  }

  public void run(IProgressMonitor monitor) throws InterruptedException {
    monitor.beginTask("Scanning folders", IProgressMonitor.UNKNOWN);
    try {
      for(Iterator it = folders.iterator(); it.hasNext();) {
        String folder = (String) it.next();
        scanFolder(folder, folder, new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
      }
      
    } finally {
      monitor.done();
    }
  }

  private void scanFolder(String baseFolderPath, String folderPath, IProgressMonitor monitor) throws InterruptedException {
    if(monitor.isCanceled()) {
      throw new InterruptedException();
    }

    monitor.subTask(folderPath);
    monitor.worked(1);

    File folderDir = new File(folderPath);
    if(!folderDir.exists() || !folderDir.isDirectory()) {
      return;
    }

    File pomFile = new File(folderDir, MavenPlugin.POM_FILE_NAME);
    MavenProjectInfo mavenProjectInfo = readMavenProjectInfo(baseFolderPath, pomFile, null);
    if(mavenProjectInfo != null) {
      mavenProjectInfo.setNeedsRename(needsRename && baseFolderPath == folderPath);
      addProject(mavenProjectInfo);
      return; // don't scan subfolders of the Maven project
    }

    File[] files = folderDir.listFiles();
    for(int i = 0; i < files.length; i++ ) {
      if(files[i].isDirectory()) {
        scanFolder(baseFolderPath, files[i].getAbsolutePath(), monitor);
      }
    }
  }

  private MavenProjectInfo readMavenProjectInfo(String baseFolderPath, File pomFile, MavenProjectInfo parentInfo) {
    if(!pomFile.exists()) {
      return null;
    }

    MavenModelManager modelManager = MavenPlugin.getDefault().getMavenModelManager();
    try {
      Model model = modelManager.readMavenModel(pomFile);
      String pomName = pomFile.getAbsolutePath().substring(baseFolderPath.length());

      MavenProjectInfo projectInfo = new MavenProjectInfo(pomName, pomFile, model, parentInfo);

      Map modules = new LinkedHashMap();
      for(Iterator it = model.getModules().iterator(); it.hasNext();) {
        String module = (String) it.next();
        modules.put(module, new HashSet());
      }

      for(Iterator it = model.getProfiles().iterator(); it.hasNext();) {
        Profile profile = (Profile) it.next();
        for(Iterator ir = profile.getModules().iterator(); ir.hasNext();) {
          String module = (String) ir.next();
          Set profiles = (Set) modules.get(module);
          if(profiles == null) {
            profiles = new HashSet();
            modules.put(module, profiles);
          }
          profiles.add(profile.getId());
        }
      }

      File baseDir = pomFile.getParentFile();
      for(Iterator it = modules.entrySet().iterator(); it.hasNext();) {
        Map.Entry e = (Map.Entry) it.next();
        String module = (String) e.getKey();
        Set profiles = (Set) e.getValue();
        File modulePom = new File(baseDir, module + "/" + MavenPlugin.POM_FILE_NAME);
        try {
          modulePom = modulePom.getCanonicalFile();
        } catch(IOException ex) {
        }
        
        // MNGECLIPSE-614 skip folders outside of baseFolderPath 
        if(!isSubdir(baseFolderPath, modulePom.getParentFile())) {
          continue;  
        }
        
        MavenProjectInfo moduleInfo = readMavenProjectInfo(baseFolderPath, modulePom, projectInfo);
        if(moduleInfo != null) {
          moduleInfo.addProfiles(profiles);
          projectInfo.add(moduleInfo);
        }
      }

      return projectInfo;

    } catch(CoreException ex) {
      addError(ex);
      MavenPlugin.getDefault().getConsole().logError("Unable to read model " + pomFile.getAbsolutePath());
    }

    return null;
  }

  private boolean isSubdir(String baseFolderPath, File folder) {
    int n = baseFolderPath.length()-1;
    if(baseFolderPath.charAt(n)=='\\' || baseFolderPath.charAt(n)=='/') {
      baseFolderPath = baseFolderPath.substring(0, n);
    }
    
    while(folder != null) {
      if(baseFolderPath.equals(folder.getAbsolutePath())) {
        return true;
      }
      folder = folder.getParentFile();
    }
    return false;
  }

  public String getDescription() {
    return folders.toString();
  }

}
