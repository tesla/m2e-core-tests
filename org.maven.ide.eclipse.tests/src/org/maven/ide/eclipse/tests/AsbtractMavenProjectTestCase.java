/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.tests;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.apache.maven.model.Model;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.project.BuildPathManager;
import org.maven.ide.eclipse.project.MavenProjectInfo;
import org.maven.ide.eclipse.project.ProjectImportConfiguration;
import org.maven.ide.eclipse.project.ResolverConfiguration;

public abstract class AsbtractMavenProjectTestCase extends TestCase {
  
  protected static final IProgressMonitor monitor = new NullProgressMonitor();
  
  protected IWorkspace workspace;
  protected File repo;
  
  protected MavenRuntimeManager runtimeManager;

  protected void setUp() throws Exception {
    super.setUp();
    workspace = ResourcesPlugin.getWorkspace();

    // lets not assume we've got subversion in the target platform 
    Hashtable options = JavaCore.getOptions();
    options.put(JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER, ".svn/");
    JavaCore.setOptions(options);

    MavenPlugin plugin = MavenPlugin.getDefault();

    // early start does not seem to take place, lets force m2plugin activation
    waitForJobsToComplete();

    File settings = new File("settings.xml").getCanonicalFile();

    runtimeManager = plugin.getMavenRuntimeManager();
    runtimeManager.setUserSettingsFile(settings.getAbsolutePath());

    repo = new File("localrepo").getCanonicalFile();
  }

  protected void tearDown() throws Exception {
    super.tearDown();

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        IProject[] projects = workspace.getRoot().getProjects();
        for(int i = 0; i < projects.length; i++ ) {
          projects[i].delete(false, true, monitor);
        }
      }
    }, new NullProgressMonitor());

    waitForJobsToComplete();
  }

  protected void deleteProject(String projectName) throws CoreException {
    final IProject project = workspace.getRoot().getProject(projectName);
  
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
            try {
              is.close();
            } catch(IOException ex) {
              // ignore
            }
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

  protected IProject importProject(String pomName, ResolverConfiguration configuration) throws IOException, CoreException {
    final MavenPlugin plugin = MavenPlugin.getDefault();
    File pomFile = new File(pomName);

    Model model = plugin.getMavenModelManager().readMavenModel(pomFile);
    final MavenProjectInfo projectInfo = new MavenProjectInfo(pomName, pomFile, model, null);
    
    final ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration(configuration);
    
    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        BuildPathManager buildpathManager = plugin.getBuildpathManager();
        IProject project = buildpathManager.importProject(projectInfo, importConfiguration, new NullProgressMonitor());
        assertNotNull("Failed to import project " + projectInfo, project);
      }
    }, monitor);

    return importConfiguration.getProject(workspace.getRoot(), model);
  }

  protected IProject importProject(String projectName, String projectLocation, ResolverConfiguration configuration) throws IOException, CoreException {
    ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration(configuration);
    importConfiguration.setProjectNameTemplate(projectName);
    return importProject(projectName, projectLocation, importConfiguration);
  }

  protected IProject importProject(String projectName, String projectLocation, final ProjectImportConfiguration importConfiguration) throws IOException, CoreException {
    File dir = new File(workspace.getRoot().getLocation().toFile(), projectName);
    copyDir(new File(projectLocation), dir);

    File pomFile = new File(dir, MavenPlugin.POM_FILE_NAME);
    Model model = MavenPlugin.getDefault().getMavenModelManager().readMavenModel(pomFile);
    final MavenProjectInfo projectInfo = new MavenProjectInfo(projectName, pomFile, model, null);
    
    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        BuildPathManager buildpathManager = MavenPlugin.getDefault().getBuildpathManager();
        IProject project = buildpathManager.importProject(projectInfo, importConfiguration, new NullProgressMonitor());
        assertNotNull("Failed to import project " + projectInfo, project);
      }
    }, monitor);

    return workspace.getRoot().getProject(projectName);
  }

  protected void waitForJobsToComplete() throws InterruptedException {
    IJobManager jobManager = Job.getJobManager();
    boolean running;
    do {
      running = false;
      Job[] jobs = jobManager.find(null);
      for(int i = 0; i < jobs.length; i++ ) {
        Job job = jobs[i];
        if(!job.isSystem()) {
          while(job.getState() != Job.NONE) {
            running = true;
            job.join();
            Thread.sleep(50L);
          }
        }
      }
    } while (running);
  }

  protected IClasspathEntry[] getMavenContainerEntries(IProject project) throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    return container.getClasspathEntries();
  }

  protected String toString(IMarker[] markers) {
    String sep = "";
    StringBuffer sb = new StringBuffer();
    for(int i = 0; i < markers.length; i++ ) {
      IMarker marker = markers[i];
      try {
        sb.append(sep).append(marker.getType()+":"+marker.getAttribute(IMarker.MESSAGE));
      } catch(CoreException ex) {
        // ignore
      }
      sep = ", ";
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

  protected static void copyDir(File src, File dst) throws IOException {
    copyDir(src, dst, new FileFilter() {
      public boolean accept(File pathname) {
        return !".svn".equals(pathname.getName());
      }
    });
  }

  public static void copyDir(File src, File dst, FileFilter filter) throws IOException {
    dst.mkdirs();
    File[] files = src.listFiles(filter);
    if (files != null) {
      for (int i = 0; i < files.length; i++) {
        File file = files[i];
        if (file.canRead()) {
          File dstChild = new File(dst, file.getName());
          if (file.isDirectory()) {
            copyDir(file, dstChild, filter);
          } else {
            copyFile(file, dstChild);
          }
        }
      }
    }
  }

  private static void copyFile(File src, File dst) throws IOException {
    BufferedInputStream in = new BufferedInputStream(new FileInputStream(src));
    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dst));

    byte[] buf = new byte[10240];
    int len;
    while ( (len = in.read(buf)) != -1 ) {
      out.write(buf, 0, len);
    }

    out.close();
    in.close();
  }
}
