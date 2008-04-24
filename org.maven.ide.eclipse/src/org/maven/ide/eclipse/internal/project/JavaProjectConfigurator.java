/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

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
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.MavenConsole;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.internal.preferences.MavenPreferenceConstants;
import org.maven.ide.eclipse.project.BuildPathManager;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.MavenRunnable;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;
import org.maven.ide.eclipse.util.Util;


public class JavaProjectConfigurator extends AbstractProjectConfigurator {
  
  private static final List VERSIONS = Arrays.asList("1.1,1.2,1.3,1.4,1.5,1.6,1.7".split(","));
  
  final MavenProjectManager projectManager;

  final MavenRuntimeManager runtimeManager;

  final MavenConsole console;

  public JavaProjectConfigurator() {
    MavenPlugin plugin = MavenPlugin.getDefault();
    projectManager = plugin.getMavenProjectManager();
    runtimeManager = plugin.getMavenRuntimeManager();
    console = plugin.getConsole();
  }

  public void configure(MavenEmbedder embedder, ProjectConfigurationRequest request, IProgressMonitor monitor) {
    IProject project = request.getProject();
    ResolverConfiguration configuration = request.getResolverConfiguration();

    MavenRuntimeManager runtimeManager = MavenPlugin.getDefault().getMavenRuntimeManager();

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
    monitor.beginTask("Updating sources " + project.getName(), IProgressMonitor.UNKNOWN);
    monitor.setTaskName("Updating sources " + project.getName());

    long t1 = System.currentTimeMillis();
    try {
      Set sources = new LinkedHashSet();
      Set entries = new LinkedHashSet();

      MavenProject mavenProject = collectSourceEntries(embedder, project, entries, sources, configuration,
          goalToExecute, monitor);

      monitor.subTask("Configuring Build Path");
      IJavaProject javaProject = JavaCore.create(project);

      if(mavenProject != null) {
        Map options = collectOptions(mavenProject);
        setOption(javaProject, options, JavaCore.COMPILER_COMPLIANCE);
        setOption(javaProject, options, JavaCore.COMPILER_SOURCE);
        setOption(javaProject, options, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM);

        String source = (String) options.get(JavaCore.COMPILER_SOURCE);
        if(source == null) {
          entries.add(JavaRuntime.getDefaultJREContainerEntry());
        } else {
          entries.add(getJREContainer(source));
        }
      }

      if(configuration.shouldUseMavenOutputFolders()) {
        javaProject.setOption(JavaCore.CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER, "ignore");
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
      if(configuration.shouldUseMavenOutputFolders()) {
        if(mavenProject != null) {
          String outputDirectory = toRelativeAndFixSeparator(project, //
              mavenProject.getBuild().getOutputDirectory());
          classesFolder = project.getFolder(outputDirectory);
        } else {
          classesFolder = project;
        }
      } else {
        IFolder outputFolder = project.getFolder(runtimeManager.getDefaultOutputFolder());
        classesFolder = outputFolder.getFolder(BuildPathManager.CLASSES_FOLDERNAME);
      }

      if(classesFolder instanceof IFolder) {
        Util.createFolder((IFolder) classesFolder);
      }
      javaProject.setRawClasspath((IClasspathEntry[]) entries.toArray(new IClasspathEntry[entries.size()]),
          classesFolder.getFullPath(), monitor);

      long t2 = System.currentTimeMillis();
      console.logMessage("Updated source folders for project " + project.getName() + " " + (t2 - t1) / 1000 + "sec");

    } catch(Exception ex) {
      String msg = "Unable to update source folders " + project.getName() + "; " + ex.toString();
      console.logMessage(msg);
      MavenPlugin.log(msg, ex);

    } finally {
      monitor.done();
    }
  }

  private Map collectOptions(MavenProject mavenProject) {
    Map options = new HashMap();

    String source = getBuildOption(mavenProject, "maven-compiler-plugin", "source");
    if(source != null) {
      if(VERSIONS.contains(source)) {
        console.logMessage("Setting source compatibility: " + source);
        setVersion(options, JavaCore.COMPILER_SOURCE, source);
        setVersion(options, JavaCore.COMPILER_COMPLIANCE, source);
      } else {
        console.logError("Invalid compiler source " + source + ". Using default");
      }
    }

    String target = getBuildOption(mavenProject, "maven-compiler-plugin", "target");
    if(target != null) {
      if("jsr14".equals(target)) {
        // see info about this at http://www.masutti.ch/eel/
        console.logMessage("Setting target compatibility: jsr14");
        setVersion(options, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "1.4");
      } else if(VERSIONS.contains(target)) {
        console.logMessage("Setting target compatibility: " + target);
        setVersion(options, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, target);
      } else {
        console.logError("Invalid compiler target " + target + ". Using default");
      }
    }

    return options;
  }

  void addDirs(IProject project, ResolverConfiguration resolverConfiguration, Set sources, Set entries,
      MavenProject mavenProject) throws CoreException {
    IFolder classes;
    IFolder testClasses;

    if(resolverConfiguration.shouldUseMavenOutputFolders()) {
      classes = project.getFolder(toRelativeAndFixSeparator(project, mavenProject.getBuild().getOutputDirectory()));
      testClasses = project.getFolder(toRelativeAndFixSeparator(project, mavenProject.getBuild()
          .getTestOutputDirectory()));
    } else {
      IFolder outputFolder = project.getFolder(runtimeManager.getDefaultOutputFolder());
      classes = outputFolder.getFolder(BuildPathManager.CLASSES_FOLDERNAME);
      testClasses = outputFolder.getFolder(BuildPathManager.TEST_CLASSES_FOLDERNAME);
    }

    Util.createFolder(classes);
    Util.createFolder(testClasses);

    addSourceDirs(project, sources, entries, mavenProject.getCompileSourceRoots(), classes.getFullPath(), null);
    addSourceDirs(project, sources, entries, mavenProject.getTestCompileSourceRoots(), testClasses.getFullPath(),
        BuildPathManager.TEST_TYPE);

    addResourceDirs(project, sources, entries, mavenProject.getBuild().getResources());
    addResourceDirs(project, sources, entries, mavenProject.getBuild().getTestResources());

    // HACK to support xmlbeans generated classes MNGECLIPSE-374
    File generatedClassesDir = new File(mavenProject.getBuild().getDirectory(), //
        "generated-classes" + File.separator + "xmlbeans");
    IResource generatedClasses = project.findMember(toRelativeAndFixSeparator(project, //
        generatedClassesDir.getAbsolutePath()));
    if(generatedClasses != null && generatedClasses.isAccessible() && generatedClasses.getType() == IResource.FOLDER) {
      entries.add(JavaCore.newLibraryEntry(generatedClasses.getFullPath(), null, null));
    }
  }

  private void addSourceDirs(IProject project, Set sources, Set entries, List sourceRoots, IPath output,
      String scope) {
    for(Iterator it = sourceRoots.iterator(); it.hasNext();) {
      String sourceRoot = (String) it.next();
      if(new File(sourceRoot).isDirectory()) {
        IResource r = project.findMember(toRelativeAndFixSeparator(project, sourceRoot));
        if(r != null && sources.add(r.getFullPath().toString())) {
          console.logMessage("Adding source folder " + r.getFullPath());
          IClasspathAttribute[] attrs = new IClasspathAttribute[0];
          if(scope != null) {
            attrs = new IClasspathAttribute[1];
            attrs[0] = JavaCore.newClasspathAttribute(MavenPlugin.TYPE_ATTRIBUTE, scope);
          }
          entries.add(JavaCore.newSourceEntry(r.getFullPath(), //
              new IPath[0] /*inclusion*/, new IPath[0] /*exclusion*/, output, attrs));
        }
      }
    }
  }

  private void addResourceDirs(IProject project, Set sources, Set entries, List resources) {
    for(Iterator it = resources.iterator(); it.hasNext();) {
      Resource resource = (Resource) it.next();
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
              new IPath[] {new Path("**")} /*exclusion*/, r.getFullPath())); //, new IPath[] { new Path( "**"+"/.svn/"+"**")} ) );
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
      final Set entries, final Set sources, final ResolverConfiguration configuration,
      final String goalToExecute, IProgressMonitor monitor) throws CoreException {
    IFile pomResource = project.getFile(MavenPlugin.POM_FILE_NAME);

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
            request.setGoals(Collections.singletonList(goalToExecute));
            return embedder.execute(request);
          }
        }, monitor);

    // TODO optimize project refresh
    monitor.subTask("refreshing");
    project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));

    MavenProject mavenProject = result.getProject();

    ReactorManager reactorManager = result.getReactorManager();
    if(reactorManager != null && reactorManager.getSortedProjects() != null) {
      if(configuration.shouldIncludeModules()) {
        for(Iterator it = reactorManager.getSortedProjects().iterator(); it.hasNext();) {
          addDirs(project, configuration, sources, entries, (MavenProject) it.next());
        }
      } else {
        addDirs(project, configuration, sources, entries, //
            (MavenProject) reactorManager.getSortedProjects().iterator().next());
      }
    }

    if(result.hasExceptions()) {
      for(Iterator it = result.getExceptions().iterator(); it.hasNext();) {
        Exception ex = (Exception) it.next();
        console.logError("Build error for " + pomResource.getFullPath() + "; " + ex.toString());
      }
    }

    return mavenProject;
  }

  private MavenProject collectSourceEntries(MavenEmbedder mavenEmbedder, final IProject project,
      final Set entries, final Set sources, final ResolverConfiguration configuration,
      final String goalToExecute, IProgressMonitor monitor) throws CoreException {

    if(monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    IFile pomResource = project.getFile(MavenPlugin.POM_FILE_NAME);

    MavenProject mavenProject = null;

    if(!MavenPreferenceConstants.NO_GOAL.equals(goalToExecute)) {
      mavenProject = generateSourceEntries(mavenEmbedder, project, entries, sources, configuration, goalToExecute,
          monitor);
    }

    if(mavenProject == null) {
      MavenProjectFacade facade = projectManager.create(pomResource, false, monitor);

      if(facade == null) {
        return null;
      }

      facade.accept(new IMavenProjectVisitor() {
        public boolean visit(MavenProjectFacade projectFacade) throws CoreException {
          addDirs(project, configuration, sources, entries, projectFacade.getMavenProject());
          return true;
        }

        public void visit(MavenProjectFacade projectFacade, Artifact artifact) {
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
        console.logError("Unable to read project " + pomResource.getFullPath() + "; " + ex2.toString());
        return null;
      }
    }

    return mavenProject;
  }

  private IClasspathEntry getJREContainer(String version) {
    int n = VERSIONS.indexOf(version);
    if(n > -1) {
      Map jreContainers = getJREContainers();
      for(int i = n; i < VERSIONS.size(); i++ ) {
        IClasspathEntry entry = (IClasspathEntry) jreContainers.get(version);
        if(entry != null) {
          console.logMessage("JRE compliant to " + version + ". " + entry);
          return entry;
        }
      }
    }
    IClasspathEntry entry = JavaRuntime.getDefaultJREContainerEntry();
    console.logMessage("No JRE compliant to " + version + ". Using default JRE container " + entry);
    return entry;
  }

  private Map getJREContainers() {
    Map jreContainers = new HashMap();

    jreContainers.put(getJREVersion(JavaRuntime.getDefaultVMInstall()), JavaRuntime.getDefaultJREContainerEntry());

    IVMInstallType[] installTypes = JavaRuntime.getVMInstallTypes();
    for(int i = 0; i < installTypes.length; i++ ) {
      IVMInstall[] installs = installTypes[i].getVMInstalls();
      for(int j = 0; j < installs.length; j++ ) {
        IVMInstall install = installs[j];
        String version = getJREVersion(install);
        if(!jreContainers.containsKey(version)) {
          // in Eclipse 3.2 one could use JavaRuntime.newJREContainerPath(install)
          IPath jreContainerPath = new Path(JavaRuntime.JRE_CONTAINER).append(install.getVMInstallType().getId())
              .append(install.getName());
          jreContainers.put(version, JavaCore.newContainerEntry(jreContainerPath));
        }
      }
    }

    return jreContainers;
  }

  private static void setVersion(Map options, String name, String value) {
    if(value == null) {
      return;
    }
    String current = (String) options.get(name);
    if(current == null) {
      options.put(name, value);
    } else {
      int oldIndex = VERSIONS.indexOf(current);
      int newIndex = VERSIONS.indexOf(value.trim());
      if(newIndex > oldIndex) {
        options.put(name, value);
      }
    }
  }

  private void setOption(IJavaProject javaProject, Map options, String name) {
    String newValue = (String) options.get(name);
    if(newValue == null) {
      newValue = (String) JavaCore.getDefaultOptions().get(name);
    }

    String currentValue = javaProject.getOption(name, false);
    if(!newValue.equals(currentValue)) {
      javaProject.setOption(name, newValue);
    }
  }

  private String getJREVersion(IVMInstall install) {
    LibraryLocation[] libraryLocations = JavaRuntime.getLibraryLocations(install);
    if(libraryLocations != null) {
      for(int k = 0; k < libraryLocations.length; k++ ) {
        IPath path = libraryLocations[k].getSystemLibraryPath();
        String jarName = path.lastSegment();
        // MNGECLIPSE-478 handle Sun and Apple JRE
        if("rt.jar".equals(jarName) || "classes.jar".equals(jarName)) {
          JarFile jarFile = null;
          try {
            jarFile = new JarFile(path.toFile());
            Manifest manifest = jarFile.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            return attributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
          } catch(Exception ex) {
            console.logError("Unable to read " + path + " " + ex.getMessage());
          } finally {
            if(jarFile != null) {
              try {
                jarFile.close();
              } catch(IOException ex) {
                console.logError("Unable to close " + path + " " + ex.getMessage());
              }
            }
          }
        }
      }
    }
    return null;
  }

  private static String getBuildOption(MavenProject project, String artifactId, String optionName) {
    String option = getBuildOption(project.getBuild().getPlugins(), artifactId, optionName);
    if(option != null) {
      return option;
    }
    PluginManagement pluginManagement = project.getBuild().getPluginManagement();
    if(pluginManagement != null) {
      return getBuildOption(pluginManagement.getPlugins(), artifactId, optionName);
    }
    return null;
  }

  private static String getBuildOption(List plugins, String artifactId, String optionName) {
    for(Iterator it = plugins.iterator(); it.hasNext();) {
      Plugin plugin = (Plugin) it.next();
      if(artifactId.equals(plugin.getArtifactId())) {
        Xpp3Dom o = (Xpp3Dom) plugin.getConfiguration();
        if(o != null && o.getChild(optionName) != null) {
          return o.getChild(optionName).getValue();
        }
      }
    }
    return null;
  }

}
