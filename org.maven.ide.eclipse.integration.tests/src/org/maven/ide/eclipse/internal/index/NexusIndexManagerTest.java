/*******************************************************************************
 * Copyright (c) 2009 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.index;

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.integration.tests.UIIntegrationTestCase;

import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;

/**
 * @author dyocum
 *
 */
public class NexusIndexManagerTest extends UIIntegrationTestCase {  
  protected List<ArtifactRepository> remoteRepos = null;

  public NexusIndexManagerTest(){
    super();
    try{
      super.cancelIndexJobs();
    }catch(Exception e){
      
    }
  }

  /**
   * Authentication was causing a failure for public (non-auth) repos. This test makes sure its ok.
   */
  public void testMngEclipse1621() throws Exception{
    String publicRepoName = "nexus";
    String publicRepoUrl = "http://repository.sonatype.org/content/groups/public";
    final File mirroredRepoFile = new File("src/org/maven/ide/eclipse/internal/index/public_mirror_repo_settings.xml");
    assertTrue(mirroredRepoFile.exists());
    WorkspaceJob job = new WorkspaceJob("setting two public repo file"){
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        try{     
          MavenPlugin.getDefault().getMaven().getMavenConfiguration().setUserSettingsFile(mirroredRepoFile.getAbsolutePath());
          assertEquals(MavenPlugin.getDefault().getMaven().getMavenConfiguration().getUserSettingsFile(), mirroredRepoFile.getAbsolutePath());        
          remoteRepos = MavenPlugin.getDefault().getRemoteRepositories(monitor);
        } catch(Exception e){
          assertTrue("The Maven settings blew up", false);
        }
        return Status.OK_STATUS;
      }
    };
    
    job.schedule();

    getUI().wait(new JobsCompleteCondition(), 120000);

    List<ArtifactRepository> effectiveRepos = MavenPlugin.getDefault().getMaven().getEffectiveRepositories(remoteRepos);

    assertEquals(1, effectiveRepos.size());
    ArtifactRepository publicRepo = effectiveRepos.get(0);
    assertEquals(publicRepo.getId(), publicRepoName);
    assertEquals(publicRepo.getUrl(), publicRepoUrl);
    MavenPlugin.getDefault().reloadSettingsXml();
    //MavenPlugin.getDefault().getIndexManager().scheduleIndexUpdate(publicRepoName, true, 0);

    getUI().wait(new JobsCompleteCondition(), 600000);
    
    //this failed with the bug in authentication (NPE) in NexusIndexManager
    IndexedArtifactGroup[] rootGroups = ((NexusIndexManager)MavenPlugin.getDefault().getIndexManager()).getRootGroups(publicRepoName);
    assertTrue(rootGroups.length > 0);
    
    
  }
  
  public void testNoMirror() throws Exception {
    final File settingsFile = new File("src/org/maven/ide/eclipse/internal/index/no_mirror_settings.xml");
    assertTrue(settingsFile.exists());
    WorkspaceJob job = new WorkspaceJob("settings.xml file has 2 repos but no mirror"){
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        try{     
          MavenPlugin.getDefault().getMaven().getMavenConfiguration().setUserSettingsFile(settingsFile.getAbsolutePath());
          assertEquals(MavenPlugin.getDefault().getMaven().getMavenConfiguration().getUserSettingsFile(), settingsFile.getAbsolutePath());        
          remoteRepos = MavenPlugin.getDefault().getRemoteRepositories(monitor);
        } catch(Exception e){
          assertTrue("The Maven settings blew up", false);
        }
        return Status.OK_STATUS;
      }
    };
    
    job.schedule();

    getUI().wait(new JobsCompleteCondition(), 120000);
    List<ArtifactRepository> effectiveRepos = MavenPlugin.getDefault().getMaven().getEffectiveRepositories(remoteRepos);
    assertEquals(effectiveRepos.size(), 1);
  }
  
  public void testPublicMirror() throws Exception{
    String publicRepoName = "nexus";
    String publicRepoUrl = "http://repository.sonatype.org/content/groups/public";
    final File mirroredRepoFile = new File("src/org/maven/ide/eclipse/internal/index/public_mirror_repo_settings.xml");
    assertTrue(mirroredRepoFile.exists());
    WorkspaceJob job = new WorkspaceJob("setting two public repo file"){
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        try{     
          MavenPlugin.getDefault().getMaven().getMavenConfiguration().setUserSettingsFile(mirroredRepoFile.getAbsolutePath());
          assertEquals(MavenPlugin.getDefault().getMaven().getMavenConfiguration().getUserSettingsFile(), mirroredRepoFile.getAbsolutePath());        
          remoteRepos = MavenPlugin.getDefault().getRemoteRepositories(monitor);
        } catch(Exception e){
          assertTrue("The Maven settings blew up", false);
        }
        return Status.OK_STATUS;
      }
    };
    
    job.schedule();

    getUI().wait(new JobsCompleteCondition(), 120000);

    List<ArtifactRepository> effectiveRepos = MavenPlugin.getDefault().getMaven().getEffectiveRepositories(remoteRepos);

    assertEquals(1, effectiveRepos.size());

  }
  public void testPublicNonMirrored() throws Exception {
    final File nonMirroredRepoFile = new File("src/org/maven/ide/eclipse/internal/index/public_nonmirrored_repo_settings.xml");
    assertTrue(nonMirroredRepoFile.exists());
    WorkspaceJob job = new WorkspaceJob("setting two public repo file"){
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        try{     
          MavenPlugin.getDefault().getMaven().getMavenConfiguration().setUserSettingsFile(nonMirroredRepoFile.getAbsolutePath());
          assertEquals(MavenPlugin.getDefault().getMaven().getMavenConfiguration().getUserSettingsFile(), nonMirroredRepoFile.getAbsolutePath());        
          remoteRepos = MavenPlugin.getDefault().getRemoteRepositories(monitor);
        } catch(Exception e){
          assertTrue("The Maven settings blew up", false);
        }
        return Status.OK_STATUS;
      }
    };
    
    job.schedule();
    getUI().wait(new JobsCompleteCondition(), 120000);
    List<ArtifactRepository> effectiveRepos = MavenPlugin.getDefault().getMaven().getEffectiveRepositories(remoteRepos);

    assertEquals(1, effectiveRepos.size());
  }
 
  
}

