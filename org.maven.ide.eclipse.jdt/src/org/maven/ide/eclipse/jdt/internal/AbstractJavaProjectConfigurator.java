/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.jdt.BuildPathManager;
import org.maven.ide.eclipse.jdt.IClasspathDescriptor;
import org.maven.ide.eclipse.jdt.IClasspathEntryDescriptor;
import org.maven.ide.eclipse.jdt.MavenJdtPlugin;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;
import org.maven.ide.eclipse.util.Util;


/**
 * AbstractJavaProjectConfigurator
 * 
 * @author igor
 */
abstract class AbstractJavaProjectConfigurator extends AbstractProjectConfigurator {

  protected static final List<String> SOURCES = Arrays.asList("1.1,1.2,1.3,1.4,1.5,1.6,1.7".split(","));

  protected static final List<String> TARGETS = Arrays.asList("1.1,1.2,1.3,1.4,jsr14,1.5,1.6,1.7".split(","));

  protected static final LinkedHashMap<String, String> ENVIRONMENTS = new LinkedHashMap<String, String>();

  static {
    ENVIRONMENTS.put("1.1", "JRE-1.1");
    ENVIRONMENTS.put("1.2", "J2SE-1.2");
    ENVIRONMENTS.put("1.3", "J2SE-1.3");
    ENVIRONMENTS.put("1.4", "J2SE-1.4");
    ENVIRONMENTS.put("1.5", "J2SE-1.5");
    ENVIRONMENTS.put("jsr14", "J2SE-1.5");
    ENVIRONMENTS.put("1.6", "JavaSE-1.6");
    ENVIRONMENTS.put("1.7", "JavaSE-1.7");
  }

  protected static final String DEFAULT_COMPILER_LEVEL = "1.4";

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    IProject project = request.getProject();

    monitor.setTaskName("Configuring java project " + project.getName());

    addNature(project, JavaCore.NATURE_ID, monitor);

    IJavaProject javaProject = JavaCore.create(project);

    List<MavenProject> mavenProjects = getMavenProjects(request, monitor);
    
    if (mavenProjects == null) {
      return;
    }

    IClasspathDescriptor classpath = new ClasspathDescriptor(javaProject);

    Map<String, String> options = collectOptions(mavenProjects, request, monitor);

    for(MavenProject mavenProject : mavenProjects) {
      addProjectSourceFolders(classpath, project, mavenProject);
    }

    addClasspathEntries(classpath, request, monitor);

    // classpath containers
    addJREClasspathContainer(classpath, options.get(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM));
    addMavenClasspathContainer(classpath);

    javaProject.setOptions(options);

    IContainer classesFolder;
    if(!mavenProjects.isEmpty()) {
      classesFolder = getFolder(project, //
          mavenProjects.get(0).getBuild().getOutputDirectory());
    } else {
      classesFolder = project;
    }

    javaProject.setRawClasspath(classpath.getEntries(), classesFolder.getFullPath(), monitor);

    MavenJdtPlugin.getDefault().getBuildpathManager().updateClasspath(project, monitor);

  }

  @SuppressWarnings("unused")
  protected void addClasspathEntries(IClasspathDescriptor classpath, ProjectConfigurationRequest request,
      final IProgressMonitor monitor) throws CoreException {
    
  }
  
  private void addJREClasspathContainer(IClasspathDescriptor classpath, String target) {
    // remove existing JRE entry
    classpath.removeEntry(new ClasspathDescriptor.EntryFilter() {
      public boolean accept(IClasspathEntryDescriptor descriptor) {
        return JavaRuntime.JRE_CONTAINER.equals(descriptor.getPath().segment(0));
      }
    });

    IClasspathEntry cpe;
    IExecutionEnvironment executionEnvironment = getExecutionEnvironment(ENVIRONMENTS.get(target));
    if(executionEnvironment == null) {
      cpe = JavaRuntime.getDefaultJREContainerEntry();
    } else {
      IPath containerPath = JavaRuntime.newJREContainerPath(executionEnvironment);
      cpe = JavaCore.newContainerEntry(containerPath);
    }

    classpath.addEntry(cpe);
  }

  private IExecutionEnvironment getExecutionEnvironment(String environmentId) {
    IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
    for(IExecutionEnvironment environment : manager.getExecutionEnvironments()) {
      if(environment.getId().equals(environmentId)) {
        return environment;
      }
    }
    return null;
  }
  
  private void addMavenClasspathContainer(IClasspathDescriptor classpath) {
    // remove any old maven classpath container entries
    classpath.removeEntry(new ClasspathDescriptor.EntryFilter() {
      public boolean accept(IClasspathEntryDescriptor entry) {
        return BuildPathManager.isMaven2ClasspathContainer(entry.getPath());
      }
    });

    // add new entry
    IClasspathEntry cpe = BuildPathManager.getDefaultContainerEntry();
    classpath.addEntry(cpe);
  }

  protected void addProjectSourceFolders(IClasspathDescriptor classpath, IProject project, MavenProject mavenProject)
      throws CoreException {
    IFolder classes = getFolder(project, mavenProject.getBuild().getOutputDirectory());
    IFolder testClasses = getFolder(project, mavenProject.getBuild().getTestOutputDirectory());

    Util.createFolder(classes, true);
    Util.createFolder(testClasses, true);

    addSourceDirs(classpath, project, mavenProject.getCompileSourceRoots(), classes.getFullPath());
    addResourceDirs(classpath, project, mavenProject.getBuild().getResources(), classes.getFullPath());

    addSourceDirs(classpath, project, mavenProject.getTestCompileSourceRoots(), testClasses.getFullPath());
    addResourceDirs(classpath, project, mavenProject.getBuild().getTestResources(), testClasses.getFullPath());
  }

  private void addSourceDirs(IClasspathDescriptor classpath, IProject project, List<String> sourceRoots,
      IPath outputPath) throws CoreException {
    for(String sourceRoot : sourceRoots) {
      IFolder sourceFolder = getFolder(project, sourceRoot);

      if(sourceFolder != null && sourceFolder.exists()) {
        console.logMessage("Adding source folder " + sourceFolder.getFullPath());
        classpath.addSourceEntry(sourceFolder.getFullPath(), outputPath, false);
      } else {
        if(sourceFolder != null) {
          classpath.removeEntry(sourceFolder.getFullPath());
        }
      }
    }
  }

  private void addResourceDirs(IClasspathDescriptor classpath, IProject project, List<Resource> resources,
      IPath outputPath) throws CoreException {

    for(Resource resource : resources) {
      File resourceDirectory = new File(resource.getDirectory());
      if(resourceDirectory.exists() && resourceDirectory.isDirectory()) {
        IPath relativePath = getProjectRelativePath(project, resource.getDirectory());
        IResource r = project.findMember(relativePath);
        if(r == project) {
          /* 
           * Workaround for the Java Model Exception: 
           *   Cannot nest output folder 'xxx/src/main/resources' inside output folder 'xxx'
           * when pom.xml have something like this:
           * 
           * <build>
           *   <resources>
           *     <resource>
           *       <directory>${basedir}</directory>
           *       <targetPath>META-INF</targetPath>
           *       <includes>
           *         <include>LICENSE</include>
           *       </includes>
           *     </resource>
           */
          console.logError("Skipping resource folder " + r.getFullPath());
        } else if(r != null && !classpath.containsPath(r.getFullPath())) {
          console.logMessage("Adding resource folder " + r.getFullPath());
          classpath.addSourceEntry(r.getFullPath(), outputPath, new IPath[0] /*inclusions*/,  
              new IPath[] {new Path("**")} /*exclusion*/, false /*optional*/);
        }
      }
    }
  }

  protected IPath getProjectRelativePath(IProject project, String absolutePath) {
    File basedir = project.getLocation().toFile();
    String relative;
    if(absolutePath.equals(basedir.getAbsolutePath())) {
      relative = ".";
    } else if(absolutePath.startsWith(basedir.getAbsolutePath())) {
      relative = absolutePath.substring(basedir.getAbsolutePath().length() + 1);
    } else {
      relative = absolutePath;
    }
    return new Path(relative.replace('\\', '/')); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private IFolder getFolder(IProject project, String absolutePath) {
    return project.getFolder(getProjectRelativePath(project, absolutePath));
  }

  protected abstract List<MavenProject> getMavenProjects(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException;

  private Map<String, String> collectOptions(List<MavenProject> mavenProjects, ProjectConfigurationRequest request,
      IProgressMonitor monitor) throws CoreException {
    Map<String, String> options = new HashMap<String, String>();

    MavenSession mavenSession = request.getMavenSession();

    String source = null, target = null;

    for(MavenProject mavenProject : mavenProjects) {
      MavenExecutionPlan executionPlan = getExecutionPlan(mavenProject, request, monitor);

      for(MojoExecution mojoExecution : executionPlan.getExecutions()) {
        if(isJavaCompilerExecution(mojoExecution)) {
          source = getCompilerLevel(mavenSession, mojoExecution, "source", source, SOURCES);
          target = getCompilerLevel(mavenSession, mojoExecution, "target", target, TARGETS);
        }
      }
    }

    if(source == null) {
      source = DEFAULT_COMPILER_LEVEL;
      console.logMessage("Could not determine source level, using default " + source);
    }

    if(target == null) {
      target = DEFAULT_COMPILER_LEVEL;
      console.logMessage("Could not determine target level, using default " + source);
    }

    options.put(JavaCore.COMPILER_SOURCE, source);
    options.put(JavaCore.COMPILER_COMPLIANCE, source);
    options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, target);
    options.put(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, "warning");

    return options;
  }

  private MavenExecutionPlan getExecutionPlan(MavenProject mavenProject, ProjectConfigurationRequest request,
      IProgressMonitor monitor) throws CoreException {
    MavenExecutionRequest executionRequest = projectManager.createExecutionRequest(request.getPom(), request
        .getResolverConfiguration(), monitor);
    executionRequest.setGoals(Arrays.asList("package"));
    MavenExecutionPlan executionPlan = maven.calculateExecutionPlan(executionRequest, mavenProject, monitor);
    return executionPlan;
  }

  private String getCompilerLevel(MavenSession mavenSession, MojoExecution mojoExecution, String parameter,
      String source, List<String> levels) throws CoreException {

    int levelIdx = getLevelIndex(source, levels);

    source = getParameterValue(mavenSession, mojoExecution, parameter, String.class);

    int newLevelIdx = getLevelIndex(source, levels);

    if(newLevelIdx > levelIdx) {
      levelIdx = newLevelIdx;
    }

    if(levelIdx < 0) {
      return DEFAULT_COMPILER_LEVEL;
    }

    return levels.get(levelIdx);
  }

  private int getLevelIndex(String level, List<String> levels) {
    return level != null ? levels.indexOf(level) : -1;
  }

  private boolean isJavaCompilerExecution(MojoExecution mojoExecution) {
    return "org.apache.maven.plugins".equals(mojoExecution.getGroupId())
        && "maven-compiler-plugin".equals(mojoExecution.getArtifactId());
  }

  public void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    super.unconfigure(request, monitor);
    removeMavenClasspathContainer(request.getProject());
  }
  
  private void removeMavenClasspathContainer(IProject project) throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    if(javaProject != null) {
      // remove classpatch container from JavaProject
      ArrayList<IClasspathEntry> newEntries = new ArrayList<IClasspathEntry>();
      for(IClasspathEntry entry : javaProject.getRawClasspath()) {
        if(!BuildPathManager.isMaven2ClasspathContainer(entry.getPath())) {
          newEntries.add(entry);
        }
      }
      javaProject.setRawClasspath(newEntries.toArray(new IClasspathEntry[newEntries.size()]), null);
    }
  }

}
