/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Scm;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.scm.ScmHandler;
import org.maven.ide.eclipse.scm.ScmHandlerFactory;

/**
 * Maven project scanner based on information from SCM
 *
 * @author Eugene Kuleshov
 * 
 * @see http://maven.apache.org/scm/
 * @see http://docs.codehaus.org/display/SCM/SCM+Matrix
 */
public class MavenProjectScmScanner extends AbstractProjectScanner {

  protected final MavenModelManager modelManager;
  
  private Scm[] scms;

  public MavenProjectScmScanner(Scm[] scms, MavenModelManager modelManager) {
    this.scms = scms;
    this.modelManager = modelManager;
  }

  public String getDescription() {
    if(scms.length == 1) {
      return scms[0].getConnection();
    }
    return "" + scms.length + " projects";
  }
  
  /**
   * @return SCMs in format recognized by maven-scm
   * 
   * @see http://maven.apache.org/scm/scm-url-format.html
   */
  public Scm[] getScms(IProgressMonitor monitor) {
    return scms;
  }
  
  public void run(IProgressMonitor monitor) throws InterruptedException {
    Scm[] scms = getScms(monitor);
    for(int i = 0; i < scms.length; i++ ) {
      if(monitor.isCanceled()) {
        throw new InterruptedException();
      }

      String url = scms[i].getConnection();
      String revision = scms[i].getTag();

      try {
        MavenProjectInfo info = readMavenProjectInfo(null, null, revision, url, url, monitor);
        if(info != null) {
          addProject(info);
        }
      } catch(Exception ex) {
        addError(ex);
        MavenPlugin.log("Error reading project " + url, ex);
      }
    }
  }

  private MavenProjectInfo readMavenProjectInfo(MavenProjectInfo parent, String profileId, //
      String revision, String url, String repositoryUrl, IProgressMonitor monitor) throws CoreException {
    if(monitor.isCanceled()) {
      return null;
    }

    if(url.endsWith("/")) {
      url = url.substring(0, url.length()-1);
    }
    
    monitor.setTaskName("Reading " + url);
    monitor.worked(1);

//    ISVNResource[] members = remoteFolder.members(monitor, ISVNFolder.FILE_MEMBERS | ISVNFolder.EXISTING_MEMBERS);
//    ISVNRemoteFile remotePomFile = null;
//    for(int i = 0; i < members.length; i++ ) {
//      if(members[i].getName().equals(MavenPlugin.POM_FILE_NAME)) {
//        remotePomFile = (ISVNRemoteFile) members[i];
//        break;
//      }
//    }
//    if(remotePomFile == null) {
//      throw new CoreException(new Status(IStatus.ERROR, MavenSubclipsePlugin.PLUGIN_ID, 0, //
//          "Folder " + remoteFolder.getRepositoryRelativePath() + " don't have Maven project", null));
//    }
//
//    IStorage storage = remotePomFile.getStorage(monitor);
//    InputStream is = storage.getContents();

//    String label = remoteFolder.getName() + "/" + MavenPlugin.POM_FILE_NAME;
    
    InputStream is = open(url, revision);
    if(is==null) {
      return null;
    }
    
    // label = folderUrl.getLastPathSegment() + "/" + MavenPlugin.POM_FILE_NAME; 
    Model model;
    try {
      model = modelManager.readMavenModel(new BufferedReader(new InputStreamReader(is)));
    } finally {
      try {
        is.close();
      } catch(IOException ex) {
        MavenPlugin.log("Unable to close stream", ex);
      }
    }
    
    int n = url.lastIndexOf("/");
    String label = (n == -1 ? url : url.substring(n)) + "/" + MavenPlugin.POM_FILE_NAME;
    MavenProjectInfo projectInfo = new MavenProjectScmInfo(label, model, //
        parent, revision, url, repositoryUrl);
    projectInfo.addProfile(profileId);

    readMavenProjectModules(projectInfo, null, revision, model.getModules(), url, repositoryUrl, monitor);
    
    for(Iterator it = model.getProfiles().iterator(); it.hasNext();) {
      Profile profile = (Profile) it.next();
      readMavenProjectModules(projectInfo, profile.getId(), revision, profile.getModules(), url, repositoryUrl, monitor);
    }

    return projectInfo;
  }


  private void readMavenProjectModules(MavenProjectInfo project, String profileId, String revision, //
      List modules, String url, String repositoryUrl, IProgressMonitor monitor) {
    for(Iterator it = modules.iterator(); it.hasNext();) {
      String module = (String) it.next();
      String moduleUrl = url.endsWith("/") ? url + module : url + "/" + module;
      try {
        MavenProjectInfo moduleInfo = readMavenProjectInfo(project, profileId, revision, moduleUrl, repositoryUrl, monitor);
        if(moduleInfo != null) {
          project.add(moduleInfo);
        }
      } catch(CoreException ex) {
        addError(ex);
        MavenPlugin.log(ex);
      }
    }
  }
  
  /**
   * Opens resource from SCM
   * 
   * @param url an url in maven-scm format for the resource to open
   * @param revision a resource revision to open 
   *  
   * @see http://maven.apache.org/scm/scm-url-format.html
   */
  private InputStream open(String url, String revision) throws CoreException {
    ScmHandler handler = ScmHandlerFactory.getHandler(url);
    if(handler==null) {
      addError(new Exception("Not supported SCM type " + url));
      return null;
    }
    return handler.open(url, revision);
  }
  
}
