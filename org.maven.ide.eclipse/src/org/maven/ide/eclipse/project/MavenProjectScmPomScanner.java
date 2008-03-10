/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.internal.console.MavenConsoleImpl;

/**
 * MavenProjectScmScanner
 *
 * @author Eugene Kuleshov
 */
public class MavenProjectScmPomScanner extends MavenProjectScmScanner {

  private final Dependency[] dependencies;

  private MavenEmbedderManager embedderManager;

  private IndexManager indexManager;

  private MavenConsoleImpl console;

  public MavenProjectScmPomScanner(Dependency[] dependencies, //
      MavenModelManager modelManager,
      MavenEmbedderManager embedderManager, IndexManager indexManager, MavenConsoleImpl console) {
    super(null, modelManager);
    this.dependencies = dependencies;
    this.embedderManager = embedderManager;
    this.indexManager = indexManager;
    this.console = console;
  }

  public String getDescription() {
    if(dependencies.length==1) {
      Dependency d = dependencies[0];
      return d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getVersion() + (d.getClassifier()==null ? "" : ":" + d.getClassifier());
    }
    return "" + dependencies.length + " projects";
  }

  public Scm[] getScms(IProgressMonitor monitor) {
    ArrayList scms = new ArrayList();
    
    MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();

    for(int i = 0; i < dependencies.length; i++ ) {
      try {
        Dependency d = dependencies[i];
        
        Artifact artifact = embedder.createArtifact(d.getGroupId(), //
            d.getArtifactId(), d.getVersion(), null, "pom");
        
        List remoteRepositories = indexManager.getArtifactRepositories(null, null);
        
        embedder.resolve(artifact, remoteRepositories, embedder.getLocalRepository());
        
        File file = artifact.getFile();
        if(file != null) {
          MavenProject project = embedder.readProject(file);
          
          Scm scm = project.getScm();
          if(scm == null) {
            String msg = project.getId() + " doesn't specify SCM info";
            console.logError(msg);
            addError(new Exception(msg));
            continue;
          }
          
          String connection = scm.getConnection();
          String devConnection = scm.getDeveloperConnection();
          String tag = scm.getTag();
          String url = scm.getUrl();

          console.logMessage(project.getArtifactId());
          console.logMessage("Connection: " + connection);
          console.logMessage("       dev: " + devConnection);
          console.logMessage("       url: " + url);
          console.logMessage("       tag: " + tag);
          
          if(connection==null) {
            if(devConnection==null) {
              String msg = project.getId() + " doesn't specify SCM connection";
              console.logError(msg);
              addError(new Exception(msg));
              continue;
            }
            scm.setConnection(devConnection);
          }

          // connection: scm:svn:https://svn.apache.org/repos/asf/incubator/wicket/branches/wicket-1.2.x/wicket
          //        dev: scm:svn:https://svn.apache.org/repos/asf/incubator/wicket/branches/wicket-1.2.x/wicket
          //        url: http://svn.apache.org/viewvc/incubator/wicket/branches/wicket-1.2.x/wicket
          //        tag: HEAD  

          // TODO add an option to select all modules/projects and optimize scan 
          
          scms.add(scm);
          
//          if(!connection.startsWith(SCM_SVN_PROTOCOL)) {
//            String msg = project.getId() + " SCM type is not supported " + connection;
//            console.logError(msg);
//            addError(new Exception(msg));
//          } else {
//            String svnUrl = connection.trim().substring(SCM_SVN_PROTOCOL.length());
//          }
        }

      } catch(Exception ex) {
        addError(ex);
      }
    }
    
    return (Scm[]) scms.toArray(new Scm[scms.size()]);
  }

}
