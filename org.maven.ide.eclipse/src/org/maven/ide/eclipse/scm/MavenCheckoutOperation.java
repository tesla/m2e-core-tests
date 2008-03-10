/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.scm;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.MavenProjectScmInfo;


/**
 * Checkout operation
 * 
 * @author Eugene Kuleshov
 */
public class MavenCheckoutOperation implements IRunnableWithProgress {

  private Collection mavenProjects;
  
  private File location;

  private List locations = new ArrayList();
  
  public void run(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
    List flatProjects = new ArrayList();

    // sort nested projects
    for(Iterator it = mavenProjects.iterator(); it.hasNext();) {
      if(monitor.isCanceled()) {
        throw new InterruptedException();
      }

      MavenProjectScmInfo info = (MavenProjectScmInfo) it.next();
      String folderUrl = info.getFolderUrl();
      
      monitor.setTaskName("Scanning " + info.getLabel() + " " + info.getFolderUrl());

      // XXX check if projects already exist
      boolean isNestedPath = false;
      for(Iterator it2 = mavenProjects.iterator(); it2.hasNext();) {
        MavenProjectScmInfo info2 = (MavenProjectScmInfo) it2.next();
        if(info != info2) {
          String path = info2.getFolderUrl().toString();
          if(folderUrl.startsWith(path)) {
            isNestedPath = true;
            break;
          }
        }
      }
      if(!isNestedPath) {
        flatProjects.add(info);
      }
    }

    for(Iterator it = flatProjects.iterator(); it.hasNext();) {
      if(monitor.isCanceled()) {
        throw new InterruptedException();
      }
      
      MavenProjectScmInfo info = (MavenProjectScmInfo) it.next();
      monitor.setTaskName("Checking out " + info.getLabel() + " " + info.getFolderUrl());
      
      try {
        // XXX if location is pointing to workspace folder need to create unique dir too 
        File workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
        File location = this.location;
        if(location == null || location.getAbsolutePath().equals(workspaceRoot.getAbsolutePath())) {
          location = getUniqueDir(workspaceRoot);
        }

        ScmHandler handler = ScmHandlerFactory.getHandler(info.getFolderUrl());
        if(handler == null) {
          String msg = "SCM provider is not available for " + info.getFolderUrl();
          MavenPlugin.getDefault().getConsole().logError(msg);
        } else {
          handler.checkoutProject(info, location, monitor);
          locations.add(location.getAbsolutePath());
        }
        
      } catch(CoreException ex) {
        String msg = "Checkout error; " + (ex.getMessage() == null ? ex.toString() : ex.getMessage());
        MavenPlugin.getDefault().getConsole().logError(msg);
        MavenPlugin.log(msg, ex);
        throw new InvocationTargetException(ex);
      }
    }

    /*
    // update projects and import the missing ones
    for(Iterator it = mavenProjects.iterator(); it.hasNext();) {
      if(monitor.isCanceled()) {
        throw new InterruptedException();
      }

      MavenProjectScmInfo info = (MavenProjectScmInfo) it.next();
      monitor.subTask("Importing " + info.getLabel());

      String folderUrl = info.getFolderUrl().toString();

      try {
        int n = flatProjects.indexOf(info);
        if(n > -1) {
          // project is already in workspace
          MavenProjectScmInfo info2 = (MavenProjectScmInfo) flatProjects.get(n);
          IProject project = configuration.getProject(workspaceRoot, info2.getModel());
          buildpathManager.configureProject(project, configuration.getResolverConfiguration(), monitor);

        } else {
          // module project that need to be imported
          File pomFile = findPomFile(folderUrl, mavenProjects, workspaceRoot);
          if(pomFile == null) {
            MavenPlugin.getDefault().getConsole().logError("Can't find POM file for " + folderUrl);
          } else {
            buildpathManager.importProject(pomFile, info.getModel(), configuration, monitor);
          }
        }
      } catch(CoreException ex) {
        MavenPlugin.getDefault().getConsole().logError(
            "Unable to create project for " + info.getModel().getId() + "; " + ex.toString());
      }
    }
    */
  }

//  private File findPomFile(String folderUrl, Collection mavenProjects, IWorkspaceRoot workspaceRoot) {
//    for(Iterator it = mavenProjects.iterator(); it.hasNext();) {
//      MavenProjectScmInfo info = (MavenProjectScmInfo) it.next();
//      String url = info.getFolderUrl();
//      if(folderUrl.startsWith(url)) {
//        IProject parentProject = configuration.getProject(workspaceRoot, info.getModel());
//        File parentFolder = parentProject.getLocation().toFile();
//        return new File(parentFolder, folderUrl.substring(url.length()) + File.separator + MavenPlugin.POM_FILE_NAME);
//      }
//    }
//    return null;
//  }

//  public void setConfiguration(ProjectImportConfiguration configuration) {
//    this.configuration = configuration;
//  }

  private File getUniqueDir(File baseDir) {
    long suffix = System.currentTimeMillis();
    while(true) {
      File tempDir = new File(baseDir, "maven." + suffix);
      if(!tempDir.exists()) {
        return tempDir;
      }
      suffix++;
    }
  }

  public void setLocation(File location) {
    this.location = location;
  }

  /**
   * @param mavenProjects a collection of {@link MavenProjectScmInfo}
   */
  public void setMavenProjects(Collection mavenProjects) {
    this.mavenProjects = mavenProjects;
  }

  /**
   * @return Returns collection of {@link MavenProjectScmInfo}
   */
  public Collection getMavenProjects() {
    return this.mavenProjects;
  }
  
  /**
   * @return Returns list of <code>String</code> paths for the checked out locations
   */
  public List getLocations() {
    return this.locations;
  }
  
}

