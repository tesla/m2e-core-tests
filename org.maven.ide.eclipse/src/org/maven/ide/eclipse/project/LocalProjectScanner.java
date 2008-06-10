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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
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
  private final File workspaceRoot;
  private final List<String> folders;
  private final boolean needsRename;

  private Set<File> scannedFolders = new HashSet<File>();

  public LocalProjectScanner(File workspaceRoot, String folder, boolean needsRename) {
    this(workspaceRoot, Collections.singletonList(folder), needsRename);
  }

  public LocalProjectScanner(File workspaceRoot, List<String> folders, boolean needsRename) {
    this.workspaceRoot = workspaceRoot;
    this.folders = folders;
    this.needsRename = needsRename;
  }

  public void run(IProgressMonitor monitor) throws InterruptedException {
    monitor.beginTask("Scanning folders", IProgressMonitor.UNKNOWN);
    try {
      for(Iterator it = folders.iterator(); it.hasNext();) {
        File folder;
        try {
          folder = new File((String) it.next()).getCanonicalFile();
          scanFolder(folder, new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
        } catch(IOException ex) {
          addError(ex);
        }
      }
    } finally {
      monitor.done();
    }
  }

  private void scanFolder(File baseDir, IProgressMonitor monitor) throws InterruptedException {
    if(monitor.isCanceled()) {
      throw new InterruptedException();
    }

    monitor.subTask(baseDir.toString());
    monitor.worked(1);

    if(!baseDir.exists() || !baseDir.isDirectory()) {
      return;
    }

    MavenProjectInfo projectInfo = readMavenProjectInfo(baseDir, "", null);
    if(projectInfo != null) {
      addProject(projectInfo);
      return; // don't scan subfolders of the Maven project
    }

    File[] files = baseDir.listFiles();
    for(int i = 0; i < files.length; i++ ) {
      File file;
      try {
        file = files[i].getCanonicalFile();
        if(file.isDirectory()) {
          scanFolder(file, monitor);
        }
      } catch(IOException ex) {
        addError(ex);
      }
    }
  }

  private MavenProjectInfo readMavenProjectInfo(File baseDir, String modulePath, MavenProjectInfo parentInfo) {

    MavenModelManager modelManager = MavenPlugin.getDefault().getMavenModelManager();

    File pomFile = new File(baseDir, MavenPlugin.POM_FILE_NAME);
    try {
      Model model = modelManager.readMavenModel(pomFile);

      if (!scannedFolders.add(baseDir.getCanonicalFile())) {
        return null; // we already know this project
      }

      String pomName = modulePath + "/" + MavenPlugin.POM_FILE_NAME;

      MavenProjectInfo projectInfo = new MavenProjectInfo(pomName , pomFile, model, parentInfo);
      projectInfo.setNeedsRename(getNeedsRename(projectInfo));

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

      for(Iterator it = modules.entrySet().iterator(); it.hasNext();) {
        Map.Entry e = (Map.Entry) it.next();
        String module = (String) e.getKey();
        Set profiles = (Set) e.getValue();

        File moduleBaseDir = new File(baseDir, module);
        MavenProjectInfo moduleInfo = readMavenProjectInfo(moduleBaseDir, module, projectInfo);
        if(moduleInfo != null) {
          moduleInfo.addProfiles(profiles);
          projectInfo.add(moduleInfo);
        }
      }

      return projectInfo;

    } catch(CoreException ex) {
      addError(ex);
      MavenPlugin.getDefault().getConsole().logError("Unable to read model " + pomFile.getAbsolutePath());
    } catch(IOException ex) {
      addError(ex);
      MavenPlugin.getDefault().getConsole().logError("Unable to read model " + pomFile.getAbsolutePath());
    }

    return null;
  }

  public String getDescription() {
    return folders.toString();
  }

  private boolean getNeedsRename(MavenProjectInfo mavenProjectInfo) throws IOException {
    File cannonical = mavenProjectInfo.getPomFile().getParentFile().getParentFile().getCanonicalFile();
    return needsRename && cannonical.equals(workspaceRoot.getCanonicalFile());
  }
}
