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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.core.MavenLogger;
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
      addProjectSourceFolders(classpath, project, mavenProject, request, monitor);
    }

    addClasspathEntries(classpath, request, monitor);

    // classpath containers
    addJREClasspathContainer(classpath, options.get(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM));
    addMavenClasspathContainer(classpath);

    // A single setOptions call erases everything else from an existing settings file.
    // Must invoke setOption individually to preserve previous options. 
    for (Map.Entry<String, String> option : options.entrySet()) {
      javaProject.setOption(option.getKey(), option.getValue());
    }

    IContainer classesFolder;
    if(!mavenProjects.isEmpty()) {
      classesFolder = getFolder(project, //
          mavenProjects.get(0).getBuild().getOutputDirectory());
    } else {
      classesFolder = project;
    }

    //Preserve existing libraries and classpath order (sort of) 
    // as other containers would have been added AFTER the JRE and M2 ones anyway 
    IClasspathEntry[] cpEntries = javaProject.getRawClasspath();
    if (cpEntries != null && cpEntries.length>0){
      for (IClasspathEntry entry : cpEntries){
        if (IClasspathEntry.CPE_CONTAINER == entry.getEntryKind() && 
            !JavaRuntime.JRE_CONTAINER.equals(entry.getPath().segment(0)) &&
            !BuildPathManager.isMaven2ClasspathContainer(entry.getPath())){
          classpath.addEntry(entry);
        }
      }
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

  protected void addProjectSourceFolders(IClasspathDescriptor classpath, IProject project, MavenProject mavenProject,
                                          ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    IFolder classes = getFolder(project, mavenProject.getBuild().getOutputDirectory());
    IFolder testClasses = getFolder(project, mavenProject.getBuild().getTestOutputDirectory());

    Util.createFolder(classes, true);
    Util.createFolder(testClasses, true);

    IPath[] inclusion = new IPath[0];
    IPath[] exclusion = new IPath[0];

    IPath[] inclusionTest = new IPath[0];
    IPath[] exclusionTest = new IPath[0];

    for(Plugin plugin : mavenProject.getBuildPlugins()) {
      if(isJavaCompilerExecution(plugin)) {
        for(PluginExecution execution : plugin.getExecutions()) {
          for(String goal : execution.getGoals()) {
            if("compile".equals(goal)) {
              try {
                inclusion = toPaths(maven.getMojoParameterValue("includes", String[].class, request.getMavenSession(),
                    plugin, execution, goal));
              } catch(CoreException ex) {
                MavenLogger.log(ex);
                console.logError("Failed to determine compiler inclusions, assuming defaults");
              }
              try {
                exclusion = toPaths(maven.getMojoParameterValue("excludes", String[].class, request.getMavenSession(),
                    plugin, execution, goal));
              } catch(CoreException ex) {
                MavenLogger.log(ex);
                console.logError("Failed to determine compiler exclusions, assuming defaults");
              }
            } else if("testCompile".equals(goal)) {
              try {
                inclusionTest = toPaths(maven.getMojoParameterValue("testIncludes", String[].class, request
                    .getMavenSession(), plugin, execution, goal));
              } catch(CoreException ex) {
                MavenLogger.log(ex);
                console.logError("Failed to determine compiler test inclusions, assuming defaults");
              }
              try {
                exclusionTest = toPaths(maven.getMojoParameterValue("testExcludes", String[].class, request
                    .getMavenSession(), plugin, execution, goal));
              } catch(CoreException ex) {
                MavenLogger.log(ex);
                console.logError("Failed to determine compiler test exclusions, assuming defaults");
              }
            }
          }
        }
      }
    }

    addSourceDirs(classpath, project, mavenProject.getCompileSourceRoots(), classes.getFullPath(), inclusion, exclusion);
    addResourceDirs(classpath, project, mavenProject.getBuild().getResources(), classes.getFullPath());

    addSourceDirs(classpath, project, mavenProject.getTestCompileSourceRoots(), testClasses.getFullPath(), inclusionTest, exclusionTest);
    addResourceDirs(classpath, project, mavenProject.getBuild().getTestResources(), testClasses.getFullPath());
  }

  private IPath[] toPaths(String[] values) {
    if(values == null) {
      return new IPath[0];
    }
    IPath[] paths = new IPath[values.length];
    for(int i = 0; i < values.length; i++ ) {
      paths[i] = new Path(values[i]);
    }
    return paths;
  }

  private void addSourceDirs(IClasspathDescriptor classpath, IProject project, List<String> sourceRoots,
      IPath outputPath, IPath[] inclusion, IPath[] exclusion) throws CoreException {
    for(String sourceRoot : sourceRoots) {
      IFolder sourceFolder = getFolder(project, sourceRoot);

      if(sourceFolder != null && sourceFolder.exists()) {
        console.logMessage("Adding source folder " + sourceFolder.getFullPath());
        classpath.addSourceEntry(sourceFolder.getFullPath(), outputPath, inclusion, exclusion, false);
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
      IProgressMonitor monitor) {
    Map<String, String> options = new HashMap<String, String>();

    MavenSession mavenSession = request.getMavenSession();

    String source = null, target = null;

    for(MavenProject mavenProject : mavenProjects) {
      for(Plugin plugin : mavenProject.getBuildPlugins()) {
        if(isJavaCompilerExecution(plugin)) {
          for(PluginExecution execution : plugin.getExecutions()) {
            for(String goal : execution.getGoals()) {
              source = getCompilerLevel(mavenSession, plugin, execution, goal, "source", source, SOURCES);
              target = getCompilerLevel(mavenSession, plugin, execution, goal, "target", target, TARGETS);
            }
          }
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

  private String getCompilerLevel(MavenSession session, Plugin plugin, PluginExecution execution, String goal,
      String parameter, String source, List<String> levels) {

    int levelIdx = getLevelIndex(source, levels);

    try {
      source = maven.getMojoParameterValue(parameter, String.class, session, plugin, execution, goal);
    } catch(CoreException ex) {
      MavenLogger.log(ex);
      console.logError("Failed to determine compiler " + parameter + " setting, assuming default");
    }

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

  private boolean isJavaCompilerExecution(Plugin plugin) {
    return isJavaCompilerPlugin(plugin.getGroupId(), plugin.getArtifactId());
  }

  private boolean isJavaCompilerPlugin(String groupId, String artifactId) {
    return "org.apache.maven.plugins".equals(groupId) && "maven-compiler-plugin".equals(artifactId);
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
