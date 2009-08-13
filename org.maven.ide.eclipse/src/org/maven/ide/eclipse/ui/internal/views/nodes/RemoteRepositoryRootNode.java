/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views.nodes;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Settings;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IMaven;

/**
 * RemoteRepositoryNode
 *
 * @author dyocum
 */
public class RemoteRepositoryRootNode implements IMavenRepositoryNode{

  public List<ArtifactRepository> getRemoteRepositories() throws Exception{
    IMaven maven = MavenPlugin.getDefault().getMaven();
    
    ArrayList<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
    repositories.addAll(maven.getArtifactRepositories());
    repositories.addAll(maven.getPluginArtifactRepository());

    return repositories;
  }

  public List<Mirror> getMirrors(){
    try{
      IMaven maven = MavenPlugin.getDefault().getMaven();
      Settings settings = maven.getSettings();
      List<Mirror> mirrors = settings.getMirrors();
      
      return mirrors;
    } catch(CoreException core){
      MavenLogger.log(core);
    }
    return null;
  }
  
  public boolean hasMirror(){
    List<Mirror> mirrors = getMirrors();
    return mirrors != null && mirrors.size() > 0;
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.IMavenRepositoryRootNode#getElements()
   */
  public Object[] getChildren() {
//    List<Mirror> mirrors = getMirrors();
//    ArrayList<Object> repoList = new ArrayList<Object>();
//    if(mirrors != null && mirrors.size() > 0){
//      for(Mirror mirror : mirrors){
//        MirrorNode node = new MirrorNode(mirror);
//        repoList.add(node);
//      }
//    }
    ArrayList<Object> repoList = new ArrayList<Object>();
    try{
       List<ArtifactRepository> repos = getRemoteRepositories();
       if(repos != null){
        for(ArtifactRepository repo : repos){
          ArtifactRepositoryNode node = new ArtifactRepositoryNode(repo);
          repoList.add(node);
          if(isMirror(repo)){
            node.setIsMirror(true);
          }
        }
      }
    } catch(Exception e){
      e.printStackTrace();
      MavenLogger.log("Unable to load remote repositories", e);
    }
    return repoList.toArray(new Object[repoList.size()]);
  }

  private boolean isMirror(ArtifactRepository repo){
    List<Mirror> mirrors = getMirrors();
    for(Mirror mirror : mirrors){
      if(mirror.getId().equals(repo.getId())){
        return true;
      }
    }
    return false;
  }
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.IMavenRepositoryRootNode#getName()
   */
  public String getName() {
    return "Remote Repositories";
  }
  
  public String toString(){
    return getName();
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.IMavenRepositoryNode#hasChildren()
   */
  public boolean hasChildren() {
    Object[] kids = getChildren();
    return kids != null && kids.length > 0;
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#getImage()
   */
  public Image getImage() {
    // TODO Auto-generated method getImage
    return null;
  }
  
  
}
