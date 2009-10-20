/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.scm;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.project.MavenProjectScmInfo;


/**
 * Checkout operation
 * 
 * @author Eugene Kuleshov
 */
public class MavenCheckoutOperation {

  private final MavenConsole console;
  
  private final File location;
  
  private final Collection<MavenProjectScmInfo> mavenProjects;
  
  private final List<String> locations = new ArrayList<String>();

  public MavenCheckoutOperation(File location, Collection<MavenProjectScmInfo> mavenProjects, MavenConsole console) {
    this.location = location;
    this.mavenProjects = mavenProjects;
    this.console = console;
  }

  public void run(IProgressMonitor monitor) throws InterruptedException, CoreException {
    List<MavenProjectScmInfo> flatProjects = new ArrayList<MavenProjectScmInfo>();

    // sort nested projects
    for(MavenProjectScmInfo info : mavenProjects) {
      if(monitor.isCanceled()) {
        throw new InterruptedException();
      }

      String folderUrl = info.getFolderUrl();
      
      monitor.setTaskName("Scanning " + info.getLabel() + " " + info.getFolderUrl());

      // XXX check if projects already exist
      boolean isNestedPath = false;
      for(MavenProjectScmInfo info2 : mavenProjects) {
        if(info != info2) {
          String path = info2.getFolderUrl();
          if(folderUrl.startsWith(path + "/")) {
            isNestedPath = true;
            break;
          }
        }
      }
      if(!isNestedPath) {
        flatProjects.add(info);
      }
    }

    for(MavenProjectScmInfo info : flatProjects) {
      if(monitor.isCanceled()) {
        throw new InterruptedException();
      }

      monitor.setTaskName("Checking out " + info.getLabel() + " " + info.getFolderUrl());

      // XXX if location is pointing to workspace folder need to create unique dir too 
      File workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
      File location = getUniqueDir(this.location == null ? workspaceRoot : this.location);

      ScmHandler handler = ScmHandlerFactory.getHandler(info.getFolderUrl());
      if(handler == null) {
        String msg = "SCM provider is not available for " + info.getFolderUrl();
        console.logError(msg);
      } else {
        handler.checkoutProject(info, location, monitor);
        locations.add(location.getAbsolutePath());
      }

    }

  }

  protected File getUniqueDir(File baseDir) {
    long suffix = System.currentTimeMillis();
    while(true) {
      File tempDir = new File(baseDir, "maven." + suffix);
      if(!tempDir.exists()) {
        return tempDir;
      }
      suffix++;
    }
  }

  /**
   * @return Returns collection of {@link MavenProjectScmInfo}
   */
  public Collection<MavenProjectScmInfo> getMavenProjects() {
    return this.mavenProjects;
  }
  
  /**
   * @return Returns list of <code>String</code> paths for the checked out locations
   */
  public List<String> getLocations() {
    return this.locations;
  }
  
}

