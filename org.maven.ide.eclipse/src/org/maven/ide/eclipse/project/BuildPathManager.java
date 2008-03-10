/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.DeltaProcessingState;
import org.eclipse.jdt.internal.core.JavaElementDelta;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;

import org.codehaus.plexus.digest.DigesterException;
import org.codehaus.plexus.digest.Sha1Digester;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.archetype.ArchetypeGenerationResult;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.MavenConsole;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.container.MavenClasspathContainer;
import org.maven.ide.eclipse.embedder.EmbedderFactory;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.internal.embedder.PluginConsoleEventMonitor;
import org.maven.ide.eclipse.internal.embedder.TransferListenerAdapter;
import org.maven.ide.eclipse.internal.preferences.MavenPreferenceConstants;
import org.maven.ide.eclipse.util.Util;

/**
 * This class is responsible for mapping Maven classpath to JDT and back.
 * 
 * XXX take project import code into a separate class (ProjectImportManager?)
 */
public class BuildPathManager implements IMavenProjectChangedListener, IDownloadSourceListener, IResourceChangeListener {

  private static final String PROPERTY_SRC_ROOT = ".srcRoot";

  private static final String PROPERTY_SRC_PATH = ".srcPath";

  private static final String PROPERTY_JAVADOC_URL = ".javadoc"; 

  public static final String TEST_CLASSES_FOLDERNAME = "test-classes";

  public static final String CLASSES_FOLDERNAME = "classes";

  public static final String TEST_TYPE = "test";

  public static final String CLASSPATH_COMPONENT_DEPENDENCY = "org.eclipse.jst.component.dependency";

  public static final String CLASSPATH_COMPONENT_NON_DEPENDENCY = "org.eclipse.jst.component.nondependency";

  public static final String PACKAGING_WAR = "war";
  
  public static final int CLASSPATH_TEST = 0;

  public static final int CLASSPATH_RUNTIME = 1;

  //test is the widest possible scope, and this is what we need by default
  public static final int CLASSPATH_DEFAULT = CLASSPATH_TEST;
  
  static final ArtifactFilter SCOPE_FILTER_RUNTIME = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME); 
  static final ArtifactFilter SCOPE_FILTER_TEST = new ScopeArtifactFilter(Artifact.SCOPE_TEST);
  
  final MavenEmbedderManager embedderManager;

  final MavenConsole console;

  final MavenProjectManager projectManager;

  final IndexManager indexManager;

  final MavenModelManager modelManager;
  
  final MavenRuntimeManager runtimeManager;
  
  private String jdtVersion;

  public BuildPathManager(MavenEmbedderManager embedderManager, MavenConsole console,
      MavenProjectManager projectManager, IndexManager indexManager, MavenModelManager modelManager,
      MavenRuntimeManager runtimeManager) {
    this.embedderManager = embedderManager;
    this.console = console;
    this.projectManager = projectManager;
    this.indexManager = indexManager;
    this.modelManager = modelManager;
    this.runtimeManager = runtimeManager;
  }

  public static IClasspathEntry getDefaultContainerEntry() {
    return JavaCore.newContainerEntry(new Path(MavenPlugin.CONTAINER_ID));
  }

  public static ResolverConfiguration getResolverConfiguration(IJavaProject javaProject) {
    return getResolverConfiguration(getMavenContainerEntry(javaProject));
  }

  public static ResolverConfiguration getResolverConfiguration(IClasspathEntry entry) {
    if(entry == null) {
      return new ResolverConfiguration();
    }

    String containerPath = entry.getPath().toString();

    boolean includeModules = containerPath.indexOf("/" + MavenPlugin.INCLUDE_MODULES) > -1;

    boolean resolveWorkspaceProjects = containerPath.indexOf("/" + MavenPlugin.NO_WORKSPACE_PROJECTS) == -1;

    boolean filterResources = containerPath.indexOf("/" + MavenPlugin.FILTER_RESOURCES) != -1;

    boolean useMavenOutputFolders = containerPath.indexOf("/" + MavenPlugin.USE_MAVEN_OUPUT_FOLDERS) != -1;

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(includeModules);
    configuration.setResolveWorkspaceProjects(resolveWorkspaceProjects);
    configuration.setFilterResources(filterResources);
    configuration.setUseMavenOutputFolders(useMavenOutputFolders);
    configuration.setActiveProfiles(getActiveProfiles(entry));
    return configuration;
  }

  public static IClasspathEntry createContainerEntry(ResolverConfiguration configuration) {
    IPath newPath = new Path(MavenPlugin.CONTAINER_ID);
    if(configuration.shouldIncludeModules()) {
      newPath = newPath.append(MavenPlugin.INCLUDE_MODULES);
    }
    if(!configuration.shouldResolveWorkspaceProjects()) {
      newPath = newPath.append(MavenPlugin.NO_WORKSPACE_PROJECTS);
    }
    if(configuration.shouldFilterResources()) {
      newPath = newPath.append(MavenPlugin.FILTER_RESOURCES);
    }
    if (configuration.shouldUseMavenOutputFolders()) {
      newPath = newPath.append(MavenPlugin.USE_MAVEN_OUPUT_FOLDERS);
    }
    if(configuration.getActiveProfiles().length() > 0) {
      newPath = newPath.append(MavenPlugin.ACTIVE_PROFILES + "[" + configuration.getActiveProfiles().trim() + "]");
    }

    return JavaCore.newContainerEntry(newPath);
  }

  public static IClasspathEntry getMavenContainerEntry(IJavaProject javaProject) {
    if(javaProject == null) {
      return null;
    }
    
    IClasspathEntry[] classpath;
    try {
      classpath = javaProject.getRawClasspath();
    } catch(JavaModelException ex) {
      return null;
    }
    for(int i = 0; i < classpath.length; i++ ) {
      IClasspathEntry entry = classpath[i];
      if(isMaven2ClasspathContainer(entry.getPath())) {
        return entry;
      }
    }
    return null;
  }

  private static String getActiveProfiles(IClasspathEntry entry) {
    String path = entry.getPath().toString();
    String prefix = "/" + MavenPlugin.ACTIVE_PROFILES + "[";
    int n = path.indexOf(prefix);
    if(n == -1) {
      return "";
    }

    return path.substring(n + prefix.length(), path.indexOf("]", n));
  }

  /**
     XXX In Eclipse 3.3, changes to resolved classpath are not announced by JDT Core
     and PackageExplorer does not properly refresh when we update Maven
     classpath container.
     As a temporary workaround, send F_CLASSPATH_CHANGED notifications
     to all PackageExplorerContentProvider instances listening to
     java ElementChangedEvent. 
     Note that even with this hack, build clean is sometimes necessary to
     reconcile PackageExplorer with actual classpath
     See https://bugs.eclipse.org/bugs/show_bug.cgi?id=154071
   */
  private void forcePackageExplorerRefresh(IJavaProject javaProject) {
    if(getJDTVersion().startsWith("3.3")) {
      DeltaProcessingState state = JavaModelManager.getJavaModelManager().deltaState;
      synchronized(state) {
        IElementChangedListener[] listeners = state.elementChangedListeners;
        for(int i = 0; i < listeners.length; i++ ) {
          if(listeners[i] instanceof PackageExplorerContentProvider) {
            JavaElementDelta delta = new JavaElementDelta(javaProject);
            delta.changed(IJavaElementDelta.F_CLASSPATH_CHANGED);
            listeners[i].elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));
          }
        }
      }
    }
  }
  
  // XXX should inject version instead of looking it up 
  private synchronized String getJDTVersion() {
    if(jdtVersion==null) {
      Bundle[] bundles = MavenPlugin.getDefault().getBundleContext().getBundles();
      for(int i = 0; i < bundles.length; i++ ) {
        if(JavaCore.PLUGIN_ID.equals(bundles[i].getSymbolicName())) {
          jdtVersion = (String) bundles[i].getHeaders().get(Constants.BUNDLE_VERSION);
          break;
        }
      }
    }
    return jdtVersion;
  }

  public void updateSourceFolders(IProject project, ResolverConfiguration configuration, String goalToExecute, IProgressMonitor monitor) {
    IFile pom = project.getFile(MavenPlugin.POM_FILE_NAME);
    if(!pom.exists()) {
      return;
    }

    monitor.beginTask("Updating sources " + project.getName(), IProgressMonitor.UNKNOWN);
    monitor.setTaskName("Updating sources " + project.getName());
    long t1 = System.currentTimeMillis();
    try {
      Set sources = new LinkedHashSet();
      List entries = new ArrayList();

      MavenProject mavenProject = collectSourceEntries(project, entries, sources, configuration, goalToExecute, monitor);

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

      javaProject.setOption(JavaCore.CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER, "ignore");

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
      if (configuration.shouldUseMavenOutputFolders()) {
        if (mavenProject != null) {
          String outputDirectory = toRelativeAndFixSeparator(project, //
              mavenProject.getBuild().getOutputDirectory());
          classesFolder = project.getFolder(outputDirectory);
        } else {
          classesFolder = project;
        }
      } else {
        IFolder outputFolder = project.getFolder(runtimeManager.getDefaultOutputFolder());
        classesFolder = outputFolder.getFolder(CLASSES_FOLDERNAME);
      }

      if (classesFolder instanceof IFolder) {
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

  private String getJREVersion(IVMInstall install) {
    LibraryLocation[] libraryLocations = JavaRuntime.getLibraryLocations(install);
    if(libraryLocations != null) {
      for(int k = 0; k < libraryLocations.length; k++ ) {
        IPath path = libraryLocations[k].getSystemLibraryPath();
        String jarName = path.lastSegment();
        // TODO that won't be the case on Mac
        if("rt.jar".equals(jarName)) {
          JarFile jarFile = null;
          try {
            jarFile = new JarFile(path.toFile());
            Manifest manifest = jarFile.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            return attributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
          } catch(Exception ex) {
            console.logError("Unable to read " + path + " " + ex.getMessage());
          } finally {
            if(jarFile!=null) {
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

  private MavenProject collectSourceEntries(IProject project, List sourceEntries, Set sources,
      ResolverConfiguration configuration, String goalToExecute, IProgressMonitor monitor) throws CoreException {
    if(monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    MavenEmbedder mavenEmbedder;
    try {
      mavenEmbedder = embedderManager.createEmbedder(EmbedderFactory.createExecutionCustomizer());
    } catch(MavenEmbedderException ex) {
      console.logError("Unable to create embedder; " + ex.toString());
      return null;
    }

    IFile pomResource = project.getFile(MavenPlugin.POM_FILE_NAME);

    monitor.subTask("reading " + pomResource.getFullPath());
    if(runtimeManager.isDebugOutput()) {
      console.logMessage("Reading " + pomResource.getFullPath());
    }

    File pomFile = pomResource.getLocation().toFile();

    MavenProject mavenProject = null;
//    try {
//      mavenProject = mavenEmbedder.readProject(pomFile);
//    } catch(Exception ex) {
//      console.logError("Unable to read project " + pomResource.getFullPath() + "; " + ex.toString());
//      return null;
//    }

    monitor.subTask("generating sources " + pomResource.getFullPath());
    try {
      if(!MavenPreferenceConstants.NO_GOAL.equals(goalToExecute)) {
        console.logMessage("Generating sources " + pomResource.getFullPath());
  
        MavenExecutionRequest request = embedderManager.createRequest(mavenEmbedder);
  
        request.setUseReactor(false);
        request.setRecursive(configuration.shouldIncludeModules());
  
        request.setBaseDirectory(pomFile.getParentFile());
        request.setGoals(Collections.singletonList(goalToExecute));
        request.addEventMonitor(new PluginConsoleEventMonitor(console));
        request.setTransferListener(new TransferListenerAdapter(monitor, console, indexManager));
        // request.setPomFile(pomFile.getAbsolutePath());
        // request.setGoals(Arrays.asList("generate-sources,generate-resources,generate-test-sources,generate-test-resources".split(",")));
        // request.setProfiles(...);
        // request.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_AT_END);
  
        MavenExecutionResult result = mavenEmbedder.execute(request);
  
        // TODO optimize project refresh
        monitor.subTask("refreshing");
        project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));
  
        mavenProject = result.getProject();
  
        ReactorManager reactorManager = result.getReactorManager();
        if(reactorManager != null && reactorManager.getSortedProjects() != null) {
          if(configuration.shouldIncludeModules()) {
            for(Iterator it = reactorManager.getSortedProjects().iterator(); it.hasNext();) {
              addDirs(project, configuration, sources, sourceEntries, (MavenProject) it.next());
            }
          } else {
            addDirs(project, configuration, sources, sourceEntries, //
                (MavenProject) reactorManager.getSortedProjects().iterator().next());
          }
        }
  
        if(result.hasExceptions()) {
          for(Iterator it = result.getExceptions().iterator(); it.hasNext();) {
            Exception ex = (Exception) it.next();
            console.logError("Build error for " + pomResource.getFullPath() + "; " + ex.toString());
          }
        }
      }

      if(mavenProject == null) {
        try {
          mavenProject = mavenEmbedder.readProject(pomFile);
          if(mavenProject!=null) {
            addDirs(project, configuration, sources, sourceEntries, mavenProject);
          }
        } catch(Exception ex2) {
          console.logError("Unable to read project " + pomResource.getFullPath() + "; " + ex2.toString());
          return null;
        }
      }

    } catch(Throwable ex) {
      String msg = "Build error for " + pomResource.getFullPath();
      console.logError(msg + "; " + ex.toString());
      MavenPlugin.log(msg, ex);

      try {
        mavenProject = mavenEmbedder.readProject(pomFile);
      } catch(Exception ex2) {
        console.logError("Unable to read project " + pomResource.getFullPath() + "; " + ex.toString());
        return null;
      }

      addDirs(project, configuration, sources, sourceEntries, mavenProject);
    }

    return mavenProject;
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
      if(VERSIONS.contains(target)) {
        console.logMessage("Setting target compatibility: " + source);
        setVersion(options, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, target);
      } else {
        console.logError("Invalid compiler target " + target + ". Using default");
      }
    }

    return options;
  }

  private void addDirs(IProject project, ResolverConfiguration resolverConfiguration, 
      Set sources, List sourceEntries, MavenProject mavenProject) throws CoreException 
  {
    IFolder classes;
    IFolder testClasses;

    if (resolverConfiguration.shouldUseMavenOutputFolders()) {
      classes = project.getFolder(toRelativeAndFixSeparator(project, mavenProject.getBuild().getOutputDirectory()));
      testClasses = project.getFolder(toRelativeAndFixSeparator(project, mavenProject.getBuild().getTestOutputDirectory()));
    } else {
      IFolder outputFolder = project.getFolder(runtimeManager.getDefaultOutputFolder());
      classes = outputFolder.getFolder(CLASSES_FOLDERNAME);
      testClasses = outputFolder.getFolder(TEST_CLASSES_FOLDERNAME);
    }

    Util.createFolder(testClasses);

    addSourceDirs(project, sources, sourceEntries, mavenProject.getCompileSourceRoots(), classes.getFullPath(), null);
    addSourceDirs(project, sources, sourceEntries, mavenProject.getTestCompileSourceRoots(), testClasses.getFullPath(), TEST_TYPE);

    addResourceDirs(project, sources, sourceEntries, mavenProject.getBuild().getResources());
    addResourceDirs(project, sources, sourceEntries, mavenProject.getBuild().getTestResources());

    // HACK to support xmlbeans generated classes MNGECLIPSE-374
    File generatedClassesDir = new File(mavenProject.getBuild().getDirectory(), //
        "generated-classes" + File.separator + "xmlbeans");
    IResource generatedClasses = project.findMember(toRelativeAndFixSeparator(project, //
        generatedClassesDir.getAbsolutePath()));
    if(generatedClasses != null && generatedClasses.isAccessible() && generatedClasses.getType() == IResource.FOLDER) {
      sourceEntries.add(JavaCore.newLibraryEntry(generatedClasses.getFullPath(), null, null));
    }
  }

  private void addSourceDirs(IProject project, Set sources, List sourceEntries, List sourceRoots, IPath output, String scope) {
    for(Iterator it = sourceRoots.iterator(); it.hasNext();) {
      String sourceRoot = (String) it.next();
      if(new File(sourceRoot).isDirectory()) {
        IResource r = project.findMember(toRelativeAndFixSeparator(project, sourceRoot));
        if(r != null && sources.add(r.getFullPath().toString())) {
          console.logMessage("Adding source folder " + r.getFullPath());
          IClasspathAttribute[] attrs = new IClasspathAttribute[0];
          if (scope != null) {
            attrs = new IClasspathAttribute[1]; 
            attrs[0] = JavaCore.newClasspathAttribute(MavenPlugin.TYPE_ATTRIBUTE, scope);
          }
          sourceEntries.add(JavaCore.newSourceEntry(r.getFullPath(), //
              new IPath[0] /*inclusion*/, new IPath[0] /*exclusion*/, output, attrs));
        }
      }
    }
  }

  private void addResourceDirs(IProject project, Set sources, List sourceEntries, List resources) {
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
          sourceEntries.add(JavaCore.newSourceEntry(r.getFullPath(), //
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

  public void enableMavenNature(IProject project, ResolverConfiguration configuration, IProgressMonitor monitor)
      throws CoreException {
    monitor.subTask("Enable Maven nature");

    ArrayList newNatures = new ArrayList();
    newNatures.add(JavaCore.NATURE_ID);
    newNatures.add(MavenPlugin.NATURE_ID);

    IProjectDescription description = project.getDescription();
    String[] natures = description.getNatureIds();
    for(int i = 0; i < natures.length; ++i) {
      String id = natures[i];
      if(!MavenPlugin.NATURE_ID.equals(id) && !JavaCore.NATURE_ID.equals(natures[i])) {
        newNatures.add(natures[i]);
      }
    }
    description.setNatureIds((String[]) newNatures.toArray(new String[newNatures.size()]));
    project.setDescription(description, monitor);

    IJavaProject javaProject = JavaCore.create(project);
    if(javaProject != null) {
      Set containerEntrySet = new LinkedHashSet();
      IClasspathContainer container = getMaven2ClasspathContainer(javaProject);
      if(container != null) {
        IClasspathEntry[] entries = container.getClasspathEntries();
        for(int i = 0; i < entries.length; i++ ) {
          containerEntrySet.add(entries[i].getPath().toString());
        }
      }

      // remove classpath container from JavaProject
      IClasspathEntry[] entries = javaProject.getRawClasspath();
      ArrayList newEntries = new ArrayList();
      for(int i = 0; i < entries.length; i++ ) {
        IClasspathEntry entry = entries[i];
        if(!isMaven2ClasspathContainer(entry.getPath()) && !containerEntrySet.contains(entry.getPath().toString())) {
          newEntries.add(entry);
        }
      }

      newEntries.add(createContainerEntry(configuration));

      javaProject.setRawClasspath((IClasspathEntry[]) newEntries.toArray(new IClasspathEntry[newEntries.size()]),
          monitor);
    }
  }

  public void disableMavenNature(IProject project, IProgressMonitor monitor) {
    monitor.subTask("Disable Maven nature");

    try {
      project.deleteMarkers(MavenPlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);

      IProjectDescription description = project.getDescription();
      String[] natures = description.getNatureIds();
      ArrayList newNatures = new ArrayList();
      for(int i = 0; i < natures.length; ++i) {
        if(!MavenPlugin.NATURE_ID.equals(natures[i])) {
          newNatures.add(natures[i]);
        }
      }
      description.setNatureIds((String[]) newNatures.toArray(new String[newNatures.size()]));
      project.setDescription(description, null);

      IJavaProject javaProject = JavaCore.create(project);
      if(javaProject != null) {
        // remove classpatch container from JavaProject
        IClasspathEntry[] entries = javaProject.getRawClasspath();
        ArrayList newEntries = new ArrayList();
        for(int i = 0; i < entries.length; i++ ) {
          if(!isMaven2ClasspathContainer(entries[i].getPath())) {
            newEntries.add(entries[i]);
          }
        }
        javaProject.setRawClasspath((IClasspathEntry[]) newEntries.toArray(new IClasspathEntry[newEntries.size()]),
            null);
      }

    } catch(CoreException ex) {
      MavenPlugin.log(ex);
    }
  }

  public static boolean isMaven2ClasspathContainer(IPath containerPath) {
    return containerPath != null && containerPath.segmentCount() > 0
        && MavenPlugin.CONTAINER_ID.equals(containerPath.segment(0));
  }

  public static IClasspathContainer getMaven2ClasspathContainer(IJavaProject project) throws JavaModelException {
    IClasspathEntry[] entries = project.getRawClasspath();
    for(int i = 0; i < entries.length; i++ ) {
      IClasspathEntry entry = entries[i];
      if(entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER && isMaven2ClasspathContainer(entry.getPath())) {
        return JavaCore.getClasspathContainer(entry.getPath(), project);
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

  private static final List VERSIONS = Arrays.asList("1.1,1.2,1.3,1.4,1.5,1.6,1.7".split(","));

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

  /**
   * @param projects a flat collection of {@link MavenProjectInfo} to import
   * @param configuration a project import configuration
   * @param monitor a progress monitor
   */
  public IStatus importProjects(Collection projects, ProjectImportConfiguration configuration, IProgressMonitor monitor) {
    MultiStatus status = new MultiStatus(MavenPlugin.PLUGIN_ID, -1, "Maven Project Import", null);
    
    for(Iterator it = projects.iterator(); it.hasNext();) {
      MavenProjectInfo projectInfo = (MavenProjectInfo) it.next();
      monitor.subTask(projectInfo.getLabel());
      try {
        importProject(projectInfo, configuration, monitor);
      } catch(CoreException ex) {
        MavenPlugin.log(ex);
        MavenPlugin.getDefault().getConsole().logError("Import error for " + projectInfo.getLabel());
        
        logStatus(status);
        
        status.add(ex.getStatus());
      }
    }

    return status; 
  }

  private void logStatus(IStatus status) {
    if(status.isMultiStatus()) {
      IStatus[] children = status.getChildren();
      for(int i = 0; i < children.length; i++ ) {
        IStatus child = children[i];
        if(!child.isOK()) {
          logStatus(child);
        }
      }
    } else {
      MavenPlugin.getDefault().getConsole().logError("  " + status.getMessage());
    }
  }
  
  public IProject importProject(MavenProjectInfo projectInfo, ProjectImportConfiguration configuration, IProgressMonitor monitor)
      throws CoreException {
    File pomFile = projectInfo.getPomFile(); 
    Model model = projectInfo.getModel();
    if(model == null) {
      model = modelManager.readMavenModel(pomFile);
      projectInfo.setModel(model);
    }
    
    String projectName = configuration.getProjectName(model);

    monitor.subTask("Importing project " + projectName);
    
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();
    IProject project = configuration.getProject(root, model);
    if(project.exists()) {
      console.logError("Project " + projectName + " already exists");
      return null;
    }

    File projectDir = pomFile.getParentFile();
    String projectParent = projectDir.getParentFile().getAbsolutePath();

    if(projectDir.equals(root.getLocation().toFile())) {
      console.logError("Can't create project " + projectName + " at Workspace folder");
      return null;
    } else if(projectParent.equals(root.getLocation().toFile().getAbsolutePath())) {
      // rename dir in workspace to match expected project name
      if(!projectDir.equals(root.getLocation().append(project.getName()).toFile())) {
        File newProject = new File(projectDir.getParent(), projectName);
        projectDir.renameTo(newProject);
        projectInfo.setPomFile(new File(newProject, MavenPlugin.PLUGIN_ID));
      }
      project.create(monitor);
    } else {
      IProjectDescription description = workspace.newProjectDescription(projectName);
      description.setLocation(new Path(projectDir.getAbsolutePath()));
      project.create(description, monitor);
    }

    if(!project.isOpen()) {
      project.open(monitor);
    }

    configureProject(project, configuration.getResolverConfiguration(), monitor);

    return project;
  }

  public void configureProject(IProject project, ResolverConfiguration configuration, IProgressMonitor monitor)
      throws CoreException {
    enableMavenNature(project, configuration, monitor);
    updateSourceFolders(project, configuration, runtimeManager.getGoalOnImport(), monitor);
  }

  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    Set projects = new HashSet();
    monitor.setTaskName("Setting classpath containers");
    for (int i = 0; i < events.length; i++) {
      MavenProjectChangedEvent event = events[i];
      IFile pom = (IFile) event.getSource();
      IProject project = pom.getProject();
      if (project.isAccessible() && projects.add(project)) {
        updateClasspath(project, monitor);
      }
    }
  }

  private void updateClasspath(IProject project, IProgressMonitor monitor) {
    IJavaProject javaProject = JavaCore.create(project);
    if(javaProject != null) {
      try {
        IClasspathEntry containerEntry = getMavenContainerEntry(javaProject);
        IPath path = containerEntry != null ? containerEntry.getPath() : new Path(MavenPlugin.CONTAINER_ID);
        IClasspathEntry[] classpath = getClasspath(project, monitor);
        IClasspathContainer container = new MavenClasspathContainer(path, classpath);
        JavaCore.setClasspathContainer(container.getPath(), new IJavaProject[] {javaProject},
            new IClasspathContainer[] {container}, monitor);
        forcePackageExplorerRefresh(javaProject);
      } catch(CoreException ex) {
        // TODO Auto-generated catch block
        MavenPlugin.log(ex);
      }
    }
  }

  private IClasspathEntry[] getClasspath(MavenProjectFacade projectFacade, final int kind, final Properties sourceAttachment) throws CoreException {
    IProject project = projectFacade.getProject();

    // From MNGECLIPSE-105
    // If the current project is a WAR project AND it has
    // a dynamic web project nature, make sure that any workspace
    // projects that it depends on are NOT included in any way
    // in the container (neither as projects nor as artifacts).
    // The idea is that the inclusion is controlled explicitly
    // by a developer via WTP UI.
    final boolean skipWorkspaceProjectsForWeb = PACKAGING_WAR.equals(projectFacade.getPackaging())
        && hasDynamicWebProjectNature(project);

    // maps entry path to entry to avoid dups caused by different entry attributes
    final Map entries = new LinkedHashMap();

    projectFacade.accept(new IMavenProjectVisitor() {
      public boolean visit(MavenProjectFacade mavenProject) {
        addClasspathEntries(entries, mavenProject, kind, skipWorkspaceProjectsForWeb, sourceAttachment);
        return true; // continue traversal
      }
      
      public void visit(MavenProjectFacade mavenProject, Artifact artifact) {
      }
    }, IMavenProjectVisitor.NESTED_MODULES);

    return (IClasspathEntry[]) entries.values().toArray(new IClasspathEntry[entries.size()]);
  }

  void addClasspathEntries(Map entries, MavenProjectFacade mavenProject, int kind, boolean skipWorkspaceProjectsForWeb, Properties sourceAttachment) {
    ArtifactFilter scopeFilter;
    if(CLASSPATH_RUNTIME == kind) {
      // ECLIPSE-33: runtime+provided scope
      scopeFilter = new ArtifactFilter() {
        public boolean include(Artifact artifact) {
          return SCOPE_FILTER_RUNTIME.include(artifact) || Artifact.SCOPE_PROVIDED.equals( artifact.getScope() );
        }
      };
    } else {
      // ECLIPSE-33: test scope (already includes provided)
      scopeFilter = SCOPE_FILTER_TEST;
    }

    for(Iterator it = mavenProject.getMavenProject().getArtifacts().iterator(); it.hasNext();) {
      Artifact a = (Artifact) it.next();

      if (!scopeFilter.include(a) || !a.getArtifactHandler().isAddedToClasspath()) {
        continue;
      }

      ArrayList attributes = new ArrayList();

      String scope = a.getScope();
      // Check the scope & set WTP non-dependency as appropriate
      if(Artifact.SCOPE_PROVIDED.equals(scope) || Artifact.SCOPE_TEST.equals(scope)
          || Artifact.SCOPE_SYSTEM.equals(scope)) {
        attributes.add(JavaCore.newClasspathAttribute(CLASSPATH_COMPONENT_NON_DEPENDENCY, ""));
      }

      // project
      MavenProjectFacade dependency = projectManager.getMavenProject(a);
      if (dependency != null && dependency.getProject().equals(mavenProject.getProject())) {
        continue;
      }

      if (dependency != null && dependency.getFullPath(a.getFile()) != null) {
        if(skipWorkspaceProjectsForWeb) {
          // From MNGECLIPSE-105
          // Leave it out so that the user can handle it the WTP way
          continue;
        }
        entries.put(dependency.getFullPath(), JavaCore.newProjectEntry(dependency.getFullPath(), false));
        continue;
      }

      File artifactFile = a.getFile();
      if(artifactFile != null) {
        String artifactLocation = artifactFile.getAbsolutePath();
        IPath entryPath = new Path(artifactLocation);

        attributes.add(JavaCore.newClasspathAttribute(MavenPlugin.GROUP_ID_ATTRIBUTE, a.getGroupId()));
        attributes.add(JavaCore.newClasspathAttribute(MavenPlugin.ARTIFACT_ID_ATTRIBUTE, a.getArtifactId()));
        attributes.add(JavaCore.newClasspathAttribute(MavenPlugin.VERSION_ATTRIBUTE, a.getVersion()));
        if (a.getClassifier() != null) {
          attributes.add(JavaCore.newClasspathAttribute(MavenPlugin.CLASSIFIER_ATTRIBUTE, a.getClassifier()));
        }

        String key = entryPath.toPortableString();

        IPath srcPath = null, srcRoot = null;
        if (sourceAttachment != null && sourceAttachment.containsKey(key + PROPERTY_SRC_PATH)) {
          srcPath = Path.fromPortableString((String) sourceAttachment.get(key + PROPERTY_SRC_PATH));
          if (sourceAttachment.containsKey(key + PROPERTY_SRC_ROOT)) {
            srcRoot = Path.fromPortableString((String) sourceAttachment.get(key + PROPERTY_SRC_ROOT));
          }
        }
        if (srcPath == null) {
          srcPath = projectManager.getSourcePath(mavenProject.getProject(), null, a, runtimeManager.isDownloadSources());
        }

        // configure javadocs if available
        String javaDocUrl = null;
        if (sourceAttachment != null && sourceAttachment.containsKey(key + PROPERTY_JAVADOC_URL)) {
          javaDocUrl = (String) sourceAttachment.get(key + PROPERTY_JAVADOC_URL);
        }
        if (javaDocUrl == null) {
          javaDocUrl = projectManager.getJavaDocUrl(a);
        }
        if(javaDocUrl != null) {
          attributes.add(JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
              javaDocUrl));
        }

        entries.put(entryPath, JavaCore.newLibraryEntry(entryPath, //
            srcPath, srcRoot, new IAccessRule[0], //
            (IClasspathAttribute[]) attributes.toArray(new IClasspathAttribute[attributes.size()]), // 
            false /*not exported*/));
      }
    }
    
  }

  private boolean hasDynamicWebProjectNature(IProject project) {
    try {
      if(project.hasNature("org.eclipse.wst.common.modulecore.ModuleCoreNature")
          || project.hasNature("org.eclipse.wst.common.project.facet.core.nature")) {
        return true;
      }
    } catch(Exception e) {
      console.logError("Unable to inspect nature: " + e);
    }
    return false;
  }

  public IClasspathEntry[] getClasspath(IProject project, int scope, IProgressMonitor monitor) throws CoreException {
    MavenProjectFacade facade = projectManager.create(project, monitor);
    if (facade == null) {
      return new IClasspathEntry[0];
    }
    try {
      Properties props = new Properties();
      File file = getSourceAttachmentPropertiesFile(project);
      if (file.canRead()) {
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        try {
          props.load(is);
        } finally {
          is.close();
        }
      }
      return getClasspath(facade, scope, props);
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, "Can't save classpath container changes", e));
    }
  }

  public IClasspathEntry[] getClasspath(IProject project, IProgressMonitor monitor) throws CoreException {
    return getClasspath(project, CLASSPATH_DEFAULT, monitor);
  }

  /**
   * Downloads sources for the given project using background job.
   * 
   * If path is null, downloads sources for all classpath entries of the project,
   * otherwise downloads sources for the first classpath entry with the
   * given path.
   */
  public void downloadSources(IProject project, IPath path) throws CoreException {
    Set artifacts = findArtifacts(project, path);
    for (Iterator it = artifacts.iterator(); it.hasNext(); ) {
      Artifact artifact = (Artifact) it.next();
      projectManager.downloadSources(project, path, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getClassifier());
    }
  }

  public Artifact findArtifact(IProject project, IPath path) throws CoreException {
    if (path != null) {
      Set artifacts = findArtifacts(project, path);
      // it is not possible to have more than one classpath entry with the same path
      if (artifacts.size() > 0) {
        return (Artifact) artifacts.iterator().next();
      }
    }
    return null;
  }

  private Set findArtifacts(IProject project, IPath path) throws CoreException {
    ArrayList entries = findClasspathEntries(project, path);

    Set artifacts = new LinkedHashSet();

    for(Iterator it = entries.iterator(); it.hasNext();) {
      IClasspathEntry entry = (IClasspathEntry) it.next();

      Artifact artifact = findArtifactByArtifactKey(entry);

      if(artifact == null) {
        artifact = findArtifactInIndex(project, entry);
        if(artifact == null) {
          console.logError("Can't find artifact for " + entry.getPath());
        } else {
          console.logMessage("Found indexed artifact " + artifact + " for " + entry.getPath());
          artifacts.add(artifact);
        }
      } else {
        console.logMessage("Found artifact " + artifact + " for " + entry.getPath());
        artifacts.add(artifact);
      }
    }

    return artifacts;
  }

  private Artifact findArtifactByArtifactKey(IClasspathEntry entry) {
    IClasspathAttribute[] attributes = entry.getExtraAttributes();
    String groupId = null;
    String artifactId = null;
    String version = null;
    String classifier = null;
    for(int j = 0; j < attributes.length; j++ ) {
      if(MavenPlugin.GROUP_ID_ATTRIBUTE.equals(attributes[j].getName())) {
        groupId = attributes[j].getValue();
      } else if(MavenPlugin.ARTIFACT_ID_ATTRIBUTE.equals(attributes[j].getName())) {
        artifactId = attributes[j].getValue();
      } else if(MavenPlugin.VERSION_ATTRIBUTE.equals(attributes[j].getName())) {
        version = attributes[j].getValue();
      } else if(MavenPlugin.CLASSIFIER_ATTRIBUTE.equals(attributes[j].getName())) {
        classifier = attributes[j].getValue();
      }
    }

    if(groupId != null && artifactId != null && version != null) {
      if (classifier != null) {
        return embedderManager.getWorkspaceEmbedder().createArtifactWithClassifier(groupId, artifactId, version, "jar", classifier);
      } 
      return embedderManager.getWorkspaceEmbedder().createArtifact(groupId, artifactId, version, null, "jar");
    }
    return null;
  }

  private Artifact findArtifactInIndex(IProject project, IClasspathEntry entry) throws CoreException {
    // calculate md5
    try {
      IFile jarFile = project.getWorkspace().getRoot().getFile(entry.getPath());
      File file = jarFile==null || jarFile.getLocation()==null ? entry.getPath().toFile() : jarFile.getLocation().toFile();

      Sha1Digester digester = new Sha1Digester();
      String sha1 = digester.calc(file);
      console.logMessage("Artifact digest " + sha1 + " for " + entry.getPath());

      Map result = indexManager.search(sha1, IndexManager.SEARCH_SHA1);
      if(result.size()==1) {
        IndexedArtifact ia = (IndexedArtifact) result.values().iterator().next();
        IndexedArtifactFile iaf = (IndexedArtifactFile) ia.files.iterator().next();
        return embedderManager.getWorkspaceEmbedder().createArtifact(iaf.group, iaf.artifact, iaf.version, null, "jar");
      }
      
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, 0, "Search error", ex));
    } catch(DigesterException ex) {
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, 0, "MD5 calculation error", ex));
    }
    
    return null;
  }

  // TODO should it be just one entry?
  private ArrayList findClasspathEntries(IProject project, IPath path) throws JavaModelException {
    ArrayList entries = new ArrayList();

    IJavaProject javaProject = JavaCore.create(project);
    addEntries(entries, javaProject.getRawClasspath(), path);

    IClasspathContainer container = getMaven2ClasspathContainer(javaProject);
    if(container!=null) {
      addEntries(entries, container.getClasspathEntries(), path);
    }
    return entries;
  }

  private void addEntries(Collection collection, IClasspathEntry[] entries, IPath path) {
    for(int i = 0; i < entries.length; i++ ) {
      IClasspathEntry entry = entries[i];
      if(entry.getEntryKind()==IClasspathEntry.CPE_LIBRARY && (path==null || path.equals(entry.getPath()))) {
        collection.add(entry);
      }
    }
  }

  public void sourcesDownloaded(DownloadSourceEvent event, IProgressMonitor monitor) throws CoreException {
    IProject project = (IProject) event.getSource();
    IJavaProject javaProject = JavaCore.create(project);

    MavenProjectFacade mavenProject = projectManager.create(project, monitor);

    if (mavenProject != null) {
      // maven project, refresh classpath container
      updateClasspath(project, monitor);
    } else {
      // non-maven project, modify specific classpath entry
      IPath path = event.getPath();

      IClasspathEntry[] cp = javaProject.getRawClasspath();
      for (int i = 0; i < cp.length; i++) {
        if (IClasspathEntry.CPE_LIBRARY == cp[i].getEntryKind() && path.equals(cp[i].getPath())) {

          List attributes = new ArrayList(Arrays.asList(cp[i].getExtraAttributes()));
          IPath srcPath = event.getSourcePath();

          if(srcPath == null) {
            // configure javadocs if available
            String javaDocUrl = event.getJavadocUrl();
            if(javaDocUrl != null) {
              attributes.add(JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
                  javaDocUrl));
            }
          }

          cp[i] = JavaCore.newLibraryEntry(cp[i].getPath(), //
              srcPath, null, cp[i].getAccessRules(), //
              (IClasspathAttribute[]) attributes.toArray(new IClasspathAttribute[attributes.size()]), // 
              cp[i].isExported());
        }
      }

      javaProject.setRawClasspath(cp, monitor);
    }

  }
  
  // project creation

  /**
   * Creates simple Maven project
   * <p>
   * The following steps are executed in the given order:
   * <ul>
   * <li>Creates the workspace project</li>
   * <li>Creates the required folders</li>
   * <li>Creates the POM</li>
   * <li>Configures project</li>
   * </ul>
   * </p>
   */
  // XXX should use Maven plugin configurations instead of manually specifying folders
  public void createSimpleProject(IProject project, IPath location, Model model, String[] directories,
      ResolverConfiguration resolverConfiguration, IProgressMonitor monitor) throws CoreException {
    String projectName = project.getName();
    monitor.beginTask("Creating project " + projectName, 4);

    monitor.subTask("Creating workspace project...");
    IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
    description.setLocation(location);
    project.create(description, monitor);
    project.open(monitor);
    monitor.worked(1);

    monitor.subTask("Creating the POM file...");
    IFile pomFile = project.getFile(MavenPlugin.POM_FILE_NAME);    
    modelManager.createMavenModel(pomFile, model);
    monitor.worked(1);
    
    monitor.subTask("Creating project folders...");
    for(int i = 0; i < directories.length; i++ ) {
      Util.createFolder(project.getFolder(directories[i]));
    }
    monitor.worked(1);
  
    monitor.subTask("Configuring project...");
    configureProject(project, resolverConfiguration, monitor);

    monitor.worked(1);
  }
  
  /**
   * Creates project structure using Archetype and then imports created project
   */
  public void createArchetypeProject(IProject project, IPath location, Archetype archetype, String groupId,
      String artifactId, String version, String javaPackage, ProjectImportConfiguration configuration, IProgressMonitor monitor) throws CoreException, InterruptedException {
    monitor.beginTask("Creating project " + project.getName(), 2);
  
    monitor.subTask("Executing Archetype " + archetype.getGroupId() + ":" + archetype.getArtifactId());
    if(location == null) {
      // if the project should be created in the workspace, figure out the path
      location = ResourcesPlugin.getWorkspace().getRoot().getLocation();
    }

    ArchetypeGenerationRequest request = new ArchetypeGenerationRequest() //
        .setTransferListener(new TransferListenerAdapter(new NullProgressMonitor(), console, indexManager)) //
        .setArchetypeGroupId(archetype.getGroupId()) //
        .setArchetypeArtifactId(archetype.getArtifactId()) //
        .setArchetypeVersion(archetype.getVersion()) //
        .setArchetypeRepository(archetype.getRepository()) //
        .setGroupId(groupId) //
        .setArtifactId(artifactId) //
        .setVersion(version) //
        .setPackage(javaPackage) // the model does not have a package field
        .setLocalRepository(embedderManager.getWorkspaceEmbedder().getLocalRepository()) //
        .setOutputDirectory(location.toPortableString());
    
    ArchetypeGenerationResult result = embedderManager.getArchetyper().generateProjectFromArchetype(request);
    Exception cause = result.getCause();
    if(cause != null) {
      String msg = "Unable to create project from archetype " + archetype.toString();
      MavenPlugin.log(msg, cause);
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, 0, msg, cause));
    }
    monitor.worked(1);
    
    // XXX Archetyper don't allow to specify project folder
    String projectFolder = location.append(artifactId).toFile().getAbsolutePath();
    
    LocalProjectScanner scanner = new LocalProjectScanner(projectFolder);
    scanner.run(monitor);
    // XXX handle scanner errors

    Set projectSet = collectProjects(scanner.getProjects(), configuration.getResolverConfiguration().shouldIncludeModules());
    
    IStatus status = importProjects(projectSet, configuration, monitor);
    if(!status.isOK()) {
      console.logError("Projects imported with errors");
    }

    monitor.worked(1);
  }

  public void updateClasspathContainer(IJavaProject project, IClasspathContainer containerSuggestion) throws CoreException {
    IFile pom = project.getProject().getFile(MavenPlugin.POM_FILE_NAME);
    MavenProjectFacade facade = projectManager.create(pom, false, null);
    if (facade == null) {
      return;
    }

    Properties props = new Properties();
    IClasspathEntry[] entries = containerSuggestion.getClasspathEntries();
    for (int i = 0; i < entries.length; i++) {
      IClasspathEntry entry = entries[i];
      if (IClasspathEntry.CPE_LIBRARY == entry.getEntryKind()) {
        String path = entry.getPath().toPortableString();
        if (entry.getSourceAttachmentPath() != null) {
          props.put(path + PROPERTY_SRC_PATH, entry.getSourceAttachmentPath().toPortableString());
        }
        if (entry.getSourceAttachmentRootPath() != null) {
          props.put(path + PROPERTY_SRC_ROOT, entry.getSourceAttachmentRootPath().toPortableString());
        }
        String javadocUrl = getJavadocLocation(entry);
        if (javadocUrl != null) {
          props.put(path + PROPERTY_JAVADOC_URL, javadocUrl);
        }
      }
    }

    entries = getClasspath(facade, CLASSPATH_DEFAULT, null);
    for (int i = 0; i < entries.length; i++) {
      IClasspathEntry entry = entries[i];
      if (IClasspathEntry.CPE_LIBRARY == entry.getEntryKind()) {
        String path = entry.getPath().toPortableString();
        String value = (String) props.get(path + PROPERTY_SRC_PATH);
        if (value != null && entry.getSourceAttachmentPath() != null && value.equals(entry.getSourceAttachmentPath().toPortableString())) {
          props.remove(path + PROPERTY_SRC_PATH);
        }
        value = (String) props.get(path + PROPERTY_SRC_ROOT);
        if (value != null && entry.getSourceAttachmentRootPath() != null && value.equals(entry.getSourceAttachmentRootPath().toPortableString())) {
          props.remove(path + PROPERTY_SRC_ROOT);
        }
      }
    }

    File file = getSourceAttachmentPropertiesFile(project.getProject());

    try {
      OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
      try {
        props.store(os, null);
      } finally {
        os.close();
      }
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, "Can't save classpath container changes", e));
    }

    updateClasspath(project.getProject(), new NullProgressMonitor());
  }

  /** public for unit tests only */
  public String getJavadocLocation(IClasspathEntry entry) {
    IClasspathAttribute[] attributes = entry.getExtraAttributes();
    for (int j = 0; j < attributes.length; j++) {
      IClasspathAttribute attribute = attributes[j];
      if (IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME.equals(attribute.getName())) {
        return attribute.getValue();
      }
    }
    return null;
  }

  /** public for unit tests only */
  public File getSourceAttachmentPropertiesFile(IProject project) {
    File stateLocationDir = MavenPlugin.getDefault().getStateLocation().toFile();
    return new File(stateLocationDir, project.getName() + ".sources");
  }

  public void resourceChanged(IResourceChangeEvent event) {
    int type = event.getType();
    if(IResourceChangeEvent.PRE_DELETE == type) {
      getSourceAttachmentPropertiesFile((IProject) event.getResource()).delete();
    }
  }

  /**
   * Flatten hierarchical projects
   *   
   * @param projects a collection of {@link MavenProjectInfo}
   * @param includeModules of true 
   * 
   * @return flattened collection of {@link MavenProjectInfo}
   */
  public Set collectProjects(Collection projects, boolean includeModules) {
    Set projectSet = collectProjects(projects);
    if(!includeModules) {
      return projectSet;
    }
    
    Set parentProjects = new HashSet();
    for(Iterator it = projectSet.iterator(); it.hasNext();) {
      MavenProjectInfo projectInfo = (MavenProjectInfo) it.next();
      MavenProjectInfo parent = projectInfo.getParent();
      if(parent==null || !projectSet.contains(parent)) {
        parentProjects.add(projectInfo);
      }
    }
    return parentProjects;
  }

  private Set collectProjects(Collection projects) {
    return new LinkedHashSet() {
      private static final long serialVersionUID = 1L;
      public Set collectProjects(Collection projects) {
        for(Iterator it = projects.iterator(); it.hasNext();) {
          MavenProjectInfo projectInfo = (MavenProjectInfo) it.next();
          add(projectInfo);
          collectProjects(projectInfo.getProjects());
        }
        return this;
      }
    }.collectProjects(projects);
  }
  
}
