/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views.nodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.graphics.Image;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;

import org.maven.ide.eclipse.MavenImages;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.internal.index.NexusIndex;
import org.maven.ide.eclipse.internal.index.NexusIndexManager;

/**
 * RemoteRepositoryNode
 *
 * @author dyocum
 */
public class RemoteRepositoryRootNode implements IMavenRepositoryNode{

  public List<ArtifactRepository> getRemoteRepositories() throws Exception{
    IMaven maven = MavenPlugin.getDefault().getMaven();
    
    ArrayList<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
    List<ArtifactRepository> artifactRepositories = maven.getArtifactRepositories(new NullProgressMonitor());
    List<ArtifactRepository> pluginArtifactRepository = maven.getPluginArtifactRepository(new NullProgressMonitor());
    
    repositories.addAll(artifactRepositories);
    repositories.addAll(pluginArtifactRepository);
    
    List<ArtifactRepository> effectiveRepositories = maven.getEffectiveRepositories(repositories);

    return effectiveRepositories;
  }

  public List<HiddenRepositoryNode> getHiddenRepositories() throws CoreException{
   
    IMaven maven = MavenPlugin.getDefault().getMaven();
    List<Mirror> mirrorUrls = MavenPlugin.getDefault().getMirrors();
    if(mirrorUrls == null || mirrorUrls.size() == 0){
      return null;
    }
    List<HiddenRepositoryNode> repoNodes = new ArrayList<HiddenRepositoryNode>();
    Map repos = maven.getSettings().getProfilesAsMap();
    List<String> active = maven.getSettings().getActiveProfiles();
    Set keys = repos.keySet();
    Collection values = repos.values();
    ArrayList<Profile> profileList = new ArrayList<Profile>();
    for(Object key : keys){
      Profile profile = (Profile)repos.get(key);
      if(active.contains(profile.getId())){
        profileList.add(profile);
      }
    }
    for(Profile profile : profileList){
      //List<Repository> pluginRepositories = profile.getPluginRepositories();
      List<Repository> repositories = profile.getRepositories();
      for(Repository rep : repositories){
        String name = rep.getId();
        String url = rep.getUrl();
        HiddenRepositoryNode node = new HiddenRepositoryNode(name, url);
        repoNodes.add(node);
      }
    }
    return repoNodes;
  }

  
  public boolean hasMirror(){
    List<Mirror> mirrors = MavenPlugin.getDefault().getMirrors();
    return mirrors != null && mirrors.size() > 0;
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.IMavenRepositoryRootNode#getElements()
   */
  public Object[] getChildren() {
    ArrayList<Object> repoList = new ArrayList<Object>();
    try{
       List<ArtifactRepository> repos = getRemoteRepositories();
       if(repos != null){
        for(ArtifactRepository repo : repos){
          NexusIndex index = new NexusIndex(((NexusIndexManager)MavenPlugin.getDefault().getIndexManager()), repo.getId(), repo.getUrl());
          IndexNode node = new IndexNode(index);
          repoList.add(node);
          if(isMirror(repo)){
            node.setMirror(true);
          }
        }
        List<HiddenRepositoryNode> hiddenRepos = getHiddenRepositories();
        if(hiddenRepos != null){
          repoList.addAll(hiddenRepos);
        }
      }
    } catch(Exception e){
      MavenLogger.log("Unable to load remote repositories", e);
    }
    return repoList.toArray(new Object[repoList.size()]);
  }

  private boolean isMirror(ArtifactRepository repo){
    List<Mirror> mirrors = MavenPlugin.getDefault().getMirrors();
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
    return MavenImages.IMG_INDEXES;
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.ui.internal.views.nodes.IMavenRepositoryNode#isUpdating()
   */
  public boolean isUpdating() {
    // TODO Auto-generated method isUpdating
    return false;
  }
  
  
}
