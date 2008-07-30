/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.project.BuildPathManager;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.MavenRunnable;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;
import org.maven.ide.eclipse.util.Util;


public class JavaProjectConfigurator extends AbstractProjectConfigurator {
  
  private static final List<String> SOURCES = Arrays.asList("1.1,1.2,1.3,1.4,1.5,1.6,1.7".split(","));
  private static final List<String> TARGETS = Arrays.asList("1.1,1.2,1.3,1.4,jsr14,1.5,1.6,1.7".split(","));
  private static final LinkedHashMap<String, String> ENVIRONMENTS = new LinkedHashMap<String, String>();
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

  // XXX make sure to configure only Java projects
  public void configure(MavenEmbedder embedder, ProjectConfigurationRequest request, IProgressMonitor monitor) {
    IProject project = request.getProject();
    ResolverConfiguration configuration = request.getResolverConfiguration();

    String goalOnImport = "";
    if (request.isProjectConfigure()) {
      goalOnImport = runtimeManager.getGoalOnUpdate();
    } else if (request.isProjectImport()) {
      goalOnImport = runtimeManager.getGoalOnImport();
    }

    updateSourceFolders(embedder, project, configuration, goalOnImport, monitor);
  }
  
  private void updateSourceFolders(MavenEmbedder embedder, IProject project, ResolverConfiguration configuration,
      String goalToExecute, IProgressMonitor monitor) {
    // monitor.beginTask("Updating sources " + project.getName(), IProgressMonitor.UNKNOWN);
    monitor.setTaskName("Updating sources " + project.getName());

    long t1 = System.currentTimeMillis();
    try {
      Set<String> sources = new LinkedHashSet<String>();
      Set<IClasspathEntry> entries = new LinkedHashSet<IClasspathEntry>();

      MavenProject mavenProject = collectSourceEntries(embedder, project, entries, sources, configuration,
          goalToExecute, monitor);

      monitor.subTask("Configuring Build Path");
      IJavaProject javaProject = JavaCore.create(project);

      if(mavenProject != null) {
        Map<String, String> options = collectOptions(mavenProject, configuration);
        setOption(javaProject, options, JavaCore.COMPILER_COMPLIANCE);
        setOption(javaProject, options, JavaCore.COMPILER_SOURCE);
        setOption(javaProject, options, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM);

        String source = options.get(JavaCore.COMPILER_SOURCE);
        if(source == null) {
          entries.add(JavaRuntime.getDefaultJREContainerEntry());
        } else {
          entries.add(getJREContainer(source));
        }
      }

      IClasspathEntry[] currentClasspath = javaProject.getRawClasspath();
      for(int i = 0; i < currentClasspath.length; i++ ) {
        // Delete all non container (e.g. JRE library) entries. See MNGECLIPSE-9 
        IClasspathEntry entry = currentClasspath[i];
        if(entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
          if(!JavaRuntime.JRE_CONTAINER.equals(entry.getPath().segment(0))) {
            entries.add(entry);
          }
        }
      }

      IContainer classesFolder;
      if(mavenProject != null) {
        String outputDirectory = toRelativeAndFixSeparator(project, //
            mavenProject.getBuild().getOutputDirectory());
        classesFolder = project.getFolder(outputDirectory);
      } else {
        classesFolder = project;
      }

      if(classesFolder instanceof IFolder) {
        Util.createFolder((IFolder) classesFolder, true);
      }
      javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]),
          classesFolder.getFullPath(), monitor);

      buildPathManager.updateClasspath(project, monitor);

      long t2 = System.currentTimeMillis();
      console.logMessage("Updated source folders for project " + project.getName() + " " + (t2 - t1) / 1000 + "sec");

    } catch(Exception ex) {
      String msg = "Unable to update source folders " + project.getName() + "; " + ex.toString();
      console.logMessage(msg);
      MavenLogger.log(msg, ex);

    } finally {
      // monitor.done();
    }
  }

  private Map<String, String> collectOptions(MavenProject mavenProject, ResolverConfiguration configuration) {
    Map<String, String> options = new HashMap<String, String>();

    // XXX find best match when importing multi-module project 
    String source = getBuildOption(mavenProject, "source", SOURCES);
    if(source != null) {
      if(SOURCES.contains(source)) {
        console.logMessage("Setting source compatibility: " + source);
        setVersion(options, JavaCore.COMPILER_SOURCE, source);
        setVersion(options, JavaCore.COMPILER_COMPLIANCE, source);
      } else {
        console.logError("Invalid compiler source " + source + ". Using default");
      }
    }

    // XXX find best match when importing multi-module project 
    String target = getBuildOption(mavenProject, "target", TARGETS);
    if(target != null) {
      if("jsr14".equals(target)) {
        // see info about this at http://www.masutti.ch/eel/
        console.logMessage("Setting target compatibility: jsr14 (1.4)");
        setVersion(options, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "1.4");
      } else if(TARGETS.contains(target)) {
        console.logMessage("Setting target compatibility: " + target);
        setVersion(options, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, target);
      } else {
        console.logError("Invalid compiler target " + target + ". Using default");
      }
    }

    return options;
  }

  @SuppressWarnings("unchecked")
  void addDirs(IProject project, ResolverConfiguration resolverConfiguration, Set<String> sources, Set<IClasspathEntry> entries,
      MavenProject mavenProject) throws CoreException {
    IFolder classes;
    IFolder testClasses;

    classes = project.getFolder(toRelativeAndFixSeparator(project, mavenProject.getBuild().getOutputDirectory()));
    testClasses = project.getFolder(toRelativeAndFixSeparator(project, mavenProject.getBuild()
        .getTestOutputDirectory()));

    Util.createFolder(classes, true);
    Util.createFolder(testClasses, true);

    
    addSourceDirs(project, sources, entries, mavenProject.getCompileSourceRoots(), classes.getFullPath(), null);
    addSourceDirs(project, sources, entries, mavenProject.getTestCompileSourceRoots(), testClasses.getFullPath(),
        BuildPathManager.TEST_TYPE);

    addResourceDirs(project, sources, entries, mavenProject.getBuild().getResources(), classes.getFullPath());
    addResourceDirs(project, sources, entries, mavenProject.getBuild().getTestResources(), testClasses.getFullPath());

    // HACK to support xmlbeans generated classes MNGECLIPSE-374
    File generatedClassesDir = new File(mavenProject.getBuild().getDirectory(), //
        "generated-classes" + File.separator + "xmlbeans");
    IResource generatedClasses = project.findMember(toRelativeAndFixSeparator(project, //
        generatedClassesDir.getAbsolutePath()));
    if(generatedClasses != null && generatedClasses.isAccessible() && generatedClasses.getType() == IResource.FOLDER) {
      entries.add(JavaCore.newLibraryEntry(generatedClasses.getFullPath(), null, null));
    }
  }

  private void addSourceDirs(IProject project, Set<String> sources, Set<IClasspathEntry> entries, List<String> sourceRoots, IPath output,
      String scope) {
    for(String sourceRoot : sourceRoots) {
      if(new File(sourceRoot).isDirectory()) {
        IResource r = project.findMember(toRelativeAndFixSeparator(project, sourceRoot));
        if(r != null && sources.add(r.getFullPath().toString())) {
          console.logMessage("Adding source folder " + r.getFullPath());
          IClasspathAttribute[] attrs = new IClasspathAttribute[0];
          entries.add(JavaCore.newSourceEntry(r.getFullPath(), //
              new IPath[0] /*inclusion*/, new IPath[0] /*exclusion*/, output, attrs));
        }
      }
    }
  }

  private void addResourceDirs(IProject project, Set<String> sources, Set<IClasspathEntry> entries, List<Resource> resources, IPath output) {
    for(Resource resource : resources) {
      File resourceDirectory = new File(resource.getDirectory());
      if(resourceDirectory.exists() && resourceDirectory.isDirectory()) {
        String relativePath = toRelativeAndFixSeparator(project, resource.getDirectory());
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
        } else if(r != null && sources.add(r.getFullPath().toString())) {
          entries.add(JavaCore.newSourceEntry(r.getFullPath(), //
              new IPath[] {new Path("**")} /*exclusion*/, output)); //, new IPath[] { new Path( "**"+"/.svn/"+"**")} ) );
          console.logMessage("Adding resource folder " + r.getFullPath());
        }
      }
    }
  }

  private String toRelativeAndFixSeparator(IProject project, String absolutePath) {
    File basedir = project.getLocation().toFile();
    String relative;
    if(absolutePath.equals(basedir.getAbsolutePath())) {
      relative = ".";
    } else if(absolutePath.startsWith(basedir.getAbsolutePath())) {
      relative = absolutePath.substring(basedir.getAbsolutePath().length() + 1);
    } else {
      relative = absolutePath;
    }
    return relative.replace('\\', '/'); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private MavenProject generateSourceEntries(MavenEmbedder mavenEmbedder, final IProject project,
      final Set<IClasspathEntry> entries, final Set<String> sources, final ResolverConfiguration configuration,
      final String goalsToExecute, IProgressMonitor monitor) throws CoreException {
    IFile pomResource = project.getFile(IMavenConstants.POM_FILE_NAME);

    console.logMessage("Generating sources " + pomResource.getFullPath());

    monitor.subTask("reading " + pomResource.getFullPath());
    if(runtimeManager.isDebugOutput()) {
      console.logMessage("Reading " + pomResource.getFullPath());
    }

    MavenExecutionResult result = projectManager.execute(mavenEmbedder, pomResource, configuration,
        new MavenRunnable() {
          public MavenExecutionResult execute(MavenEmbedder embedder, MavenExecutionRequest request) {
            request.setUseReactor(false);
            request.setRecursive(configuration.shouldIncludeModules());
            request.setGoals(Arrays.asList(goalsToExecute.split(", ")));
            return embedder.execute(request);
          }
        }, monitor);

    // TODO optimize project refresh
    monitor.subTask("refreshing");
    project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));

    MavenProject mavenProject = result.getProject();

    ReactorManager reactorManager = result.getReactorManager();
    if(reactorManager != null) {
      @SuppressWarnings("unchecked")
      List<MavenProject> projects = reactorManager.getSortedProjects();
      if(projects != null) {
        if(configuration.shouldIncludeModules()) {
          for(MavenProject p : projects) {
            addDirs(project, configuration, sources, entries, p);
          }
        } else {
          addDirs(project, configuration, sources, entries, projects.iterator().next());
        }
      }
    }

    if(result.hasExceptions()) {
      @SuppressWarnings("unchecked")
      List<Exception> exceptions = result.getExceptions();
      for(Exception ex : exceptions) {
        String msg = "Build error for " + pomResource.getFullPath();
        console.logError(msg + "; " + ex.toString());
        MavenLogger.log(msg, ex);
      }
    }

    return mavenProject;
  }

  private MavenProject collectSourceEntries(MavenEmbedder mavenEmbedder, final IProject project,
      final Set<IClasspathEntry> entries, final Set<String> sources, final ResolverConfiguration configuration,
      final String goalsToExecute, final IProgressMonitor monitor) throws CoreException {

    if(monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    IFile pomResource = project.getFile(IMavenConstants.POM_FILE_NAME);

    MavenProject mavenProject = null;

    if(goalsToExecute.trim().length() > 0) {
      mavenProject = generateSourceEntries(mavenEmbedder, project, entries, sources, configuration, goalsToExecute,
          monitor);
    }

    if(mavenProject == null) {
      IMavenProjectFacade facade = projectManager.create(pomResource, false, monitor);
      if(facade == null) {
        return null;
      }

      facade.accept(new IMavenProjectVisitor() {
        public boolean visit(IMavenProjectFacade projectFacade) throws CoreException {
          addDirs(project, configuration, sources, entries, projectFacade.getMavenProject(monitor));
          return true;
        }
      }, IMavenProjectVisitor.NESTED_MODULES);
    }

    if(mavenProject == null) {
      try {
        mavenProject = mavenEmbedder.readProject(pomResource.getLocation().toFile());
        if(mavenProject != null) {
          addDirs(project, configuration, sources, entries, mavenProject);
        }
      } catch(Exception ex2) {
        String msg = "Unable to read project " + pomResource.getFullPath();
        console.logError(msg + "; " + ex2.toString());
        MavenLogger.log(msg, ex2);
        return null;
      }
    }
    
    if(mavenProject != null && !configuration.shouldIncludeModules()) {
      @SuppressWarnings("unchecked")
      List<String> modules = mavenProject.getModules();
      for(String module : modules) {
        IFolder moduleDir = project.getFolder(module);
        if(moduleDir.isAccessible()) {
          // TODO don't set derived on modules that are not in Eclipse workspace
          moduleDir.setDerived(true);
        }
      }
    }

    return mavenProject;
  }

  private IClasspathEntry getJREContainer(String version) {
    IExecutionEnvironment executionEnvironment = getExecutionEnvironment(ENVIRONMENTS.get(version));
    if(executionEnvironment == null) {
      return JavaRuntime.getDefaultJREContainerEntry();
    }
    IPath containerPath = JavaRuntime.newJREContainerPath(executionEnvironment);
    return JavaCore.newContainerEntry(containerPath);
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

//  private Map<String, IClasspathEntry> getJREContainers() {
//    Map<String, IClasspathEntry> jreContainers = new HashMap<String, IClasspathEntry>();
//
//    jreContainers.put(getJREVersion(JavaRuntime.getDefaultVMInstall()), JavaRuntime.getDefaultJREContainerEntry());
//
//    IVMInstallType[] installTypes = JavaRuntime.getVMInstallTypes();
//    for(int i = 0; i < installTypes.length; i++ ) {
//      IVMInstall[] installs = installTypes[i].getVMInstalls();
//      for(int j = 0; j < installs.length; j++ ) {
//        IVMInstall install = installs[j];
//        String version = getJREVersion(install);
//        if(!jreContainers.containsKey(version)) {
//          // in Eclipse 3.2 one could use JavaRuntime.newJREContainerPath(install)
//          IPath jreContainerPath = new Path(JavaRuntime.JRE_CONTAINER).append(install.getVMInstallType().getId())
//              .append(install.getName());
//          jreContainers.put(version, JavaCore.newContainerEntry(jreContainerPath));
//        }
//      }
//    }
//
//    return jreContainers;
//  }

  private static void setVersion(Map<String, String> options, String name, String value) {
    if(value == null) {
      return;
    }
    String current = options.get(name);
    if(current == null) {
      options.put(name, value);
    } else {
      if(!current.equals(value)) {
        options.put(name, value);
      }
    }
  }

  private void setOption(IJavaProject javaProject, Map<String, String> options, String name) {
    String newValue = options.get(name);
    if(newValue == null) {
      newValue = (String) JavaCore.getDefaultOptions().get(name);
    }

    String currentValue = javaProject.getOption(name, false);
    if(!newValue.equals(currentValue)) {
      javaProject.setOption(name, newValue);
    }
  }

//  private String getJREVersion(IVMInstall install) {
//    LibraryLocation[] libraryLocations = JavaRuntime.getLibraryLocations(install);
//    if(libraryLocations != null) {
//      for(int k = 0; k < libraryLocations.length; k++ ) {
//        IPath path = libraryLocations[k].getSystemLibraryPath();
//        String jarName = path.lastSegment();
//        // MNGECLIPSE-478 handle Sun and Apple JRE
//        if("rt.jar".equals(jarName) || "classes.jar".equals(jarName)) {
//          JarFile jarFile = null;
//          try {
//            jarFile = new JarFile(path.toFile());
//            Manifest manifest = jarFile.getManifest();
//            Attributes attributes = manifest.getMainAttributes();
//            return attributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
//          } catch(Exception ex) {
//            console.logError("Unable to read " + path + " " + ex.getMessage());
//          } finally {
//            if(jarFile != null) {
//              try {
//                jarFile.close();
//              } catch(IOException ex) {
//                console.logError("Unable to close " + path + " " + ex.getMessage());
//              }
//            }
//          }
//        }
//      }
//    }
//    return null;
//  }

  @SuppressWarnings("unchecked")
  private String getBuildOption(MavenProject project, String optionName, List<String> values) {
    LinkedHashSet<String> options = new LinkedHashSet<String>();
    addBuildOptions(options, project.getBuild().getPluginsAsMap(), optionName);
    if(options.isEmpty()) {
      PluginManagement pluginManagement = project.getBuild().getPluginManagement();
      if(pluginManagement != null) {
        addBuildOptions(options, pluginManagement.getPluginsAsMap(), optionName);
      }
    }
    
    String option = null;
    for(Iterator<String> it = options.iterator(); it.hasNext();) {
      String o = it.next();
      if(option==null) {
        if(values.indexOf(o)>-1) {
          option = o;
        }
      } else {
        int n = values.indexOf(o);
        if(n>values.indexOf(option)) {
          option = o;
        }
      }
    }
    return option;
  }

  private void addBuildOptions(Set<String> options, Map<String, Plugin> plugins, String optionName) {
    Plugin plugin = plugins.get("org.apache.maven.plugins:maven-compiler-plugin");
    if(plugin!=null) {
      addOption(options, (Xpp3Dom) plugin.getConfiguration(), optionName);

      @SuppressWarnings("unchecked")
      List<PluginExecution> executions = plugin.getExecutions();
      if(executions!=null) {
        for(PluginExecution execution : executions) {
          addOption(options, (Xpp3Dom) execution.getConfiguration(), optionName);
        }
      }
    }
  }

  private void addOption(Set<String> options, Xpp3Dom configuration, String optionName) {
    if(configuration != null && configuration.getChild(optionName) != null) {
      options.add(configuration.getChild(optionName).getValue().trim());
    }
  }
  
}
