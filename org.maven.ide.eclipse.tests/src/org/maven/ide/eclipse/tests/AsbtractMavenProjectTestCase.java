/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.tests;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.internal.project.MavenProjectManagerRefreshJob;
import org.maven.ide.eclipse.jdt.BuildPathManager;
import org.maven.ide.eclipse.jdt.MavenJdtPlugin;
import org.maven.ide.eclipse.jobs.IBackgroundProcessingQueue;
import org.maven.ide.eclipse.project.IMavenProjectImportResult;
import org.maven.ide.eclipse.project.MavenProjectInfo;
import org.maven.ide.eclipse.project.ProjectImportConfiguration;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.util.FileHelpers;

public abstract class AsbtractMavenProjectTestCase extends TestCase {
  
  public static final int DELETE_RETRY_COUNT = 10;
  public static final long DELETE_RETRY_DELAY = 6000L;

  protected static final IProgressMonitor monitor = new NullProgressMonitor();
  
  protected IWorkspace workspace;
  protected File repo;
  
  protected MavenProjectManagerRefreshJob projectRefreshJob;
  protected Job downloadSourcesJob;

  protected MavenPlugin plugin;

  protected IMavenConfiguration mavenConfiguration;

  @SuppressWarnings("unchecked")
  protected void setUp() throws Exception {
    super.setUp();
    workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceDescription description = workspace.getDescription();
    description.setAutoBuilding(false);
    workspace.setDescription(description);

    // lets not assume we've got subversion in the target platform 
    Hashtable<String, String> options = JavaCore.getOptions();
    options.put(JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER, ".svn/");
    JavaCore.setOptions(options);

    plugin = MavenPlugin.getDefault();

    projectRefreshJob = plugin.getProjectManagerRefreshJob();
    projectRefreshJob.sleep();

    downloadSourcesJob = MavenJdtPlugin.getDefault().getBuildpathManager().getDownloadSourcesJob();
    downloadSourcesJob.sleep();

    mavenConfiguration = MavenPlugin.lookup(IMavenConfiguration.class);

    File settings = new File("settings.xml").getCanonicalFile();
    if (settings.canRead()) {
      mavenConfiguration.setUserSettingsFile(settings.getAbsolutePath());
    }

    ArtifactRepository localRepository = MavenPlugin.lookup(IMaven.class).getLocalRepository();
    if(localRepository != null) {
      repo =  new File(localRepository.getBasedir());
    } else {
      fail("Cannot determine local repository path");
    }

    cleanWorkspace();
  }

  protected void tearDown() throws Exception {
    super.tearDown();

    cleanWorkspace();

    projectRefreshJob.wakeUp();
    IWorkspaceDescription description = workspace.getDescription();
    description.setAutoBuilding(true);
    workspace.setDescription(description);
  }

  private void cleanWorkspace() throws Exception {
    Exception cause = null;
    for (int  i = 0; i < DELETE_RETRY_COUNT; i++) {
      try {
        doCleanWorkspace();
      } catch (InterruptedException e) {
        throw e;
      } catch (OperationCanceledException e) {
        throw e;
      } catch (Exception e) {
        cause = e;
        Thread.sleep(DELETE_RETRY_DELAY);
        continue;
      }

      // all clear
      return;
    }

    // must be a timeout
    throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, "Could not delete workspace resources", cause));
  }

  private void doCleanWorkspace() throws InterruptedException, CoreException, IOException {
    waitForJobsToComplete();

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        IProject[] projects = workspace.getRoot().getProjects();
        for(int i = 0; i < projects.length; i++ ) {
          projects[i].delete(true, true, monitor);
        }
      }
    }, new NullProgressMonitor());

    waitForJobsToComplete();

    File[] files = workspace.getRoot().getLocation().toFile().listFiles();
    if (files != null) {
      for (File file : files) {
        if (!".metadata".equals(file.getName())) {
          if (file.isDirectory()) {
            FileUtils.deleteDirectory(file);
          } else {
            if (!file.delete()) {
              throw new IOException("Could not delete file " + file.getCanonicalPath());
            }
          }
        }
      }
    }
  }

  protected void deleteProject(String projectName) throws CoreException, InterruptedException {
    IProject project = workspace.getRoot().getProject(projectName);

    deleteProject(project);
  }

  protected void deleteProject(IProject project) throws InterruptedException, CoreException {
    Exception cause = null;
    for (int  i = 0; i < DELETE_RETRY_COUNT; i++) {
      try {
        doDeleteProject(project);
      } catch (InterruptedException e) {
        throw e;
      } catch (OperationCanceledException e) {
        throw e;
      } catch (Exception e) {
        cause = e;
        Thread.sleep(DELETE_RETRY_DELAY);
        continue;
      }

      // all clear
      return;
    }

    // must be a timeout
    throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, "Could not delete project", cause));
  }

  private void doDeleteProject(final IProject project) throws CoreException, InterruptedException {
    waitForJobsToComplete(monitor);

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        if(project.exists()) {
          deleteMember(".classpath", project, monitor);
          deleteMember(".project", project, monitor);
          project.delete(false, true, monitor);
        }
      }
  
      private void deleteMember(String name, final IProject project, IProgressMonitor monitor) throws CoreException {
        IResource member = project.findMember(name);
        if(member.exists()) {
          member.delete(true, monitor);
        }
      }
    }, new NullProgressMonitor());
  }

  protected IProject createProject(String projectName, final String pomResource) throws CoreException {
    final IProject project = workspace.getRoot().getProject(projectName);
  
    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        project.create(monitor);
  
        if(!project.isOpen()) {
          project.open(monitor);
        }
  
        IFile pomFile = project.getFile("pom.xml");
        if(!pomFile.exists()) {
          InputStream is = null;
          try {
            is = new FileInputStream(pomResource);
            pomFile.create(is, true, monitor);
          } catch(FileNotFoundException ex) {
            throw new CoreException(new Status(IStatus.ERROR, "", 0, ex.toString(), ex));
          } finally {
            IOUtil.close(is);
          }
        }
      }
    }, null);
  
    return project;
  }

  protected IProject createExisting(String projectName, String projectLocation) throws IOException, CoreException {
    File dir = new File(workspace.getRoot().getLocation().toFile(), projectName);
    copyDir(new File(projectLocation), dir);

    final IProject project = workspace.getRoot().getProject(projectName);

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        if (!project.exists()) {
          IProjectDescription projectDescription = workspace.newProjectDescription(project.getName());
          projectDescription.setLocation(null); 
          project.create(projectDescription, monitor);
          project.open(IResource.NONE, monitor);
        } else {
          project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
      }
    }, null);
    return project;
  }

  /**
   * Import a test project into the Eclipse workspace
   * 
   * @param pomName - a relative location of the pom file for the project to import
   * @param configuration - a resolver configuration to be used to configure imported project 
   * @return created project
   */
  protected IProject importProject(String pomName, ResolverConfiguration configuration) throws IOException, CoreException {
    File pomFile = new File(pomName);
    return importProjects(pomFile.getParentFile().getCanonicalPath(), new String[] {pomFile.getName()}, configuration)[0];
  }

  /**
   * Import test projects into the Eclipse workspace
   * 
   * @param basedir - a base directory for all projects to import
   * @param pomNames - a relative locations of the pom files for the projects to import
   * @param configuration - a resolver configuration to be used to configure imported projects 
   * @return created projects
   */
  protected IProject[] importProjects(String basedir, String[] pomNames, ResolverConfiguration configuration) throws IOException, CoreException {
    final MavenPlugin plugin = MavenPlugin.getDefault();
    MavenModelManager mavenModelManager = plugin.getMavenModelManager();
    IWorkspaceRoot root = workspace.getRoot();
    
    File src = new File(basedir);
    File dst = new File(root.getLocation().toFile(), src.getName());
    copyDir(src, dst);

    final ArrayList<MavenProjectInfo> projectInfos = new ArrayList<MavenProjectInfo>();
    for(String pomName : pomNames) {
      File pomFile = new File(dst, pomName);
      Model model = mavenModelManager.readMavenModel(pomFile);
      MavenProjectInfo projectInfo = new MavenProjectInfo(pomName, pomFile, model, null);
      setBasedirRename(projectInfo);
      projectInfos.add(projectInfo);
    }

    final ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration(configuration);
    
    final ArrayList<IMavenProjectImportResult> importResults = new ArrayList<IMavenProjectImportResult>();

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        importResults.addAll(plugin.getProjectConfigurationManager().importProjects(projectInfos, importConfiguration, monitor));
      }
    }, plugin.getProjectConfigurationManager().getRule(), IWorkspace.AVOID_UPDATE, monitor);

    IProject[] projects = new IProject[projectInfos.size()];
    for (int i = 0; i < projectInfos.size(); i++) {
      IMavenProjectImportResult importResult = importResults.get(i);
      assertSame(projectInfos.get(i), importResult.getMavenProjectInfo());
      projects[i] = importResult.getProject();
      assertNotNull("Failed to import project " + projectInfos, projects[i]);
    }

    return projects;
  }

  private void setBasedirRename(MavenProjectInfo projectInfo) throws IOException {
    File workspaceRoot = workspace.getRoot().getLocation().toFile();
    File basedir = projectInfo.getPomFile().getParentFile().getCanonicalFile();

    projectInfo.setBasedirRename(basedir.getParentFile().equals(workspaceRoot)? MavenProjectInfo.RENAME_REQUIRED: MavenProjectInfo.RENAME_NO);
  }

  protected IProject importProject(String projectName, String projectLocation, ResolverConfiguration configuration) throws IOException, CoreException {
    ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration(configuration);
    importConfiguration.setProjectNameTemplate(projectName);
    return importProject(projectName, projectLocation, importConfiguration);
  }

  protected IProject importProject(String projectName, String projectLocation, final ProjectImportConfiguration importConfiguration) throws IOException, CoreException {
    File dir = new File(workspace.getRoot().getLocation().toFile(), projectName);
    copyDir(new File(projectLocation), dir);

    File pomFile = new File(dir, IMavenConstants.POM_FILE_NAME);
    Model model = MavenPlugin.getDefault().getMavenModelManager().readMavenModel(pomFile);
    final MavenProjectInfo projectInfo = new MavenProjectInfo(projectName, pomFile, model, null);
    setBasedirRename(projectInfo);

    final MavenPlugin plugin = MavenPlugin.getDefault();
    
    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        plugin.getProjectConfigurationManager().importProjects(Collections.singleton(projectInfo), importConfiguration, monitor);
        IProject project = workspace.getRoot().getProject(importConfiguration.getProjectName(projectInfo.getModel()));
        assertNotNull("Failed to import project " + projectInfo, project);
      }
    }, plugin.getProjectConfigurationManager().getRule(), IWorkspace.AVOID_UPDATE, monitor);

    return workspace.getRoot().getProject(projectName);
  }

  protected void waitForJobsToComplete() throws InterruptedException, CoreException {
    waitForJobsToComplete(monitor);
  }

  public static void waitForJobsToComplete(IProgressMonitor monitor) throws InterruptedException, CoreException {
    /*
     * First, make sure refresh job gets all resource change events
     * 
     * Resource change events are delivered after WorkspaceJob#runInWorkspace returns
     * and during IWorkspace#run. Each change notification is delivered by
     * only one thread/job, so we make sure no other workspaceJob is running then
     * call IWorkspace#run from this thread. 
     * 
     * Unfortunately, this does not catch other jobs and threads that call IWorkspace#run
     * so we have to hard-code workarounds
     *  
     * See http://www.eclipse.org/articles/Article-Resource-deltas/resource-deltas.html
     */
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IJobManager jobManager = Job.getJobManager();
    jobManager.suspend();
    try {
      Job[] jobs = jobManager.find(null);
      for (int i = 0; i < jobs.length; i++) {
        if(jobs[i] instanceof WorkspaceJob || jobs[i].getClass().getName().endsWith("JREUpdateJob")) {
          jobs[i].join();
        }
      }
      workspace.run(new IWorkspaceRunnable() {
        public void run(IProgressMonitor monitor) throws CoreException {
        }
      }, workspace.getRoot(), 0, monitor);

      // Now we flush all background processing queues
      boolean processed = flushProcessingQueues(jobManager, monitor);
      for (int i = 0; i < 10 && processed; i++) {
        processed = flushProcessingQueues(jobManager, monitor);
      }

      assertFalse("Could not flush background processing queues: " + getProcessingQueues(jobManager), processed);
    } finally {
      jobManager.resume();
    }

  }

  private static boolean flushProcessingQueues(IJobManager jobManager, IProgressMonitor monitor) throws InterruptedException {
    boolean processed = false;
    for (IBackgroundProcessingQueue queue : getProcessingQueues(jobManager)) {
      queue.join();
      if (!queue.isEmpty()) {
        queue.run(monitor);
        processed = true;
      }
      if (queue.isEmpty()) {
        queue.cancel();
      }
    }
    return processed;
  }

  private static List<IBackgroundProcessingQueue> getProcessingQueues(IJobManager jobManager) {
    ArrayList<IBackgroundProcessingQueue> queues = new ArrayList<IBackgroundProcessingQueue>();
    for (Job job : jobManager.find(null)) {
      if (job instanceof IBackgroundProcessingQueue) {
        queues.add((IBackgroundProcessingQueue) job);
      }
    }
    return queues;
  }

  protected IClasspathEntry[] getMavenContainerEntries(IProject project) throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    return container.getClasspathEntries();
  }


  protected String toString(IMarker[] markers) {
    if (markers != null) {
      return toString(Arrays.asList(markers));  
    }
    return "";  
  }

  protected String toString(List<IMarker> markers) {
    String sep = "";
    StringBuilder sb = new StringBuilder();
    if (markers != null) {
      for(IMarker marker : markers) {
        try { 
          sb.append(sep).append(marker.getType()+":"+marker.getAttribute(IMarker.MESSAGE));
        } catch(CoreException ex) {
          // ignore
        }
        sep = ", ";
      }
    }
    return sb.toString();
  }

protected void copyContent(IProject project, String from, String to) throws Exception {
    InputStream contents = project.getFile(from).getContents();
    try {
      IFile file = project.getFile(to);
      if (!file.exists()) {
        file.create(contents, IResource.FORCE, monitor);
      } else {
        file.setContents(contents, IResource.FORCE, monitor);
      }
    } finally {
      contents.close();
    }
    waitForJobsToComplete();
  }

  public static void copyDir(File src, File dst) throws IOException {
    FileHelpers.copyDir(src, dst);
  }

  public static void copyDir(File src, File dst, FileFilter filter) throws IOException {
    FileHelpers.copyDir(src, dst, filter);
  }

  protected List<IMarker> findErrorMarkers(IProject project) throws CoreException {
    return findMarkers(project, IMarker.SEVERITY_ERROR);
  }

  protected List<IMarker> findMarkers(IProject project, int targetSeverity) throws CoreException {
    ArrayList<IMarker> errors = new ArrayList<IMarker>();
    for(IMarker marker : project.findMarkers(null /* all markers */, true /* subtypes */, IResource.DEPTH_INFINITE)) {
      int severity = marker.getAttribute(IMarker.SEVERITY, 0);
      if(severity==targetSeverity) {
        errors.add(marker);
      }
    }
    return errors;
  }

  protected void assertMarkers(IProject project, int expected) throws CoreException {
    List<IMarker> markers = findErrorMarkers(project);
    assertEquals(project.getName() + " : " + toString(markers.toArray(new IMarker[markers.size()])), //
        expected, markers.size());
  }

  protected void assertNoErrors(IProject project) throws CoreException {
    int severity = project.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertTrue("Unexpected error markers " + toString(markers), severity < IMarker.SEVERITY_ERROR);
  }

}
