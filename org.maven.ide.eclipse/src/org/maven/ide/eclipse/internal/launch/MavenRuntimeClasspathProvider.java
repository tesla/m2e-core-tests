/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.launch;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.StandardClasspathProvider;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.project.BuildPathManager;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;


public class MavenRuntimeClasspathProvider extends StandardClasspathProvider {
  
  private static final String TESTS_PROJECT_CLASSIFIER = "tests";

  private static final String THIS_PROJECT_CLASSIFIER = "";

  private static final Path MAVEN2_CONTAINER_PATH = new Path(IMavenConstants.CONTAINER_ID);
  
  public static final String JDT_JUNIT_TEST = "org.eclipse.jdt.junit.launchconfig";

  public static final String JDT_JAVA_APPLICATION = "org.eclipse.jdt.launching.localJavaApplication";

  private static final Set<String> supportedTypes = new HashSet<String>();
  static {
    // not exactly nice, but works with eclipse 3.2, 3.3 and 3.4M3
    supportedTypes.add(MavenRuntimeClasspathProvider.JDT_JAVA_APPLICATION);
    supportedTypes.add(MavenRuntimeClasspathProvider.JDT_JUNIT_TEST);
  }

  MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();

  public IRuntimeClasspathEntry[] computeUnresolvedClasspath(final ILaunchConfiguration configuration) throws CoreException {
    boolean useDefault = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, true);
    if (useDefault) {
      IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
      IRuntimeClasspathEntry jreEntry = JavaRuntime.computeJREEntry(configuration);
      IRuntimeClasspathEntry projectEntry = JavaRuntime.newProjectRuntimeClasspathEntry(javaProject);
      IRuntimeClasspathEntry mavenEntry = JavaRuntime.newRuntimeContainerClasspathEntry(MAVEN2_CONTAINER_PATH, IRuntimeClasspathEntry.USER_CLASSES);

      if(jreEntry == null) {
        return new IRuntimeClasspathEntry[] {projectEntry, mavenEntry};
      }

      return new IRuntimeClasspathEntry[] {jreEntry, projectEntry, mavenEntry};
    }
    
    return recoverRuntimePath(configuration, IJavaLaunchConfigurationConstants.ATTR_CLASSPATH);
  }

  public IRuntimeClasspathEntry[] resolveClasspath(IRuntimeClasspathEntry[] entries, ILaunchConfiguration configuration)
      throws CoreException 
  {
    int scope = getArtifactScope(configuration);

    Set<IRuntimeClasspathEntry> all = new LinkedHashSet<IRuntimeClasspathEntry>(entries.length);
    for (int i = 0; i < entries.length; i++) {
      IRuntimeClasspathEntry entry = entries[i];
      if (MAVEN2_CONTAINER_PATH.equals(entry.getPath()) && entry.getType() == IRuntimeClasspathEntry.CONTAINER) {
        addMavenClasspathEntries(all, entry, configuration, scope);
      } else if (entry.getType() == IRuntimeClasspathEntry.PROJECT) {
        IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
        if (javaProject.getPath().equals(entry.getPath())) {
          addProjectEntries(all, entry.getPath(), scope, THIS_PROJECT_CLASSIFIER, configuration);
        } else {
          addStandardClasspathEntries(all, entry, configuration);
        }
      } else {
        addStandardClasspathEntries(all, entry, configuration);
      }
    }
    return all.toArray(new IRuntimeClasspathEntry[all.size()]);
  }

  private void addStandardClasspathEntries(Set<IRuntimeClasspathEntry> all, IRuntimeClasspathEntry entry, ILaunchConfiguration configuration)
      throws CoreException 
  {
    IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveRuntimeClasspathEntry(entry, configuration);
    for (int j = 0; j < resolved.length; j++) {
      all.add(resolved[j]);
    }
  }

  private void addMavenClasspathEntries(Set<IRuntimeClasspathEntry> resolved, IRuntimeClasspathEntry runtimeClasspathEntry,
      ILaunchConfiguration configuration, int scope) throws CoreException 
  {
    IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
    MavenPlugin plugin = MavenPlugin.getDefault();
    BuildPathManager buildpathManager = plugin.getBuildpathManager();
    IClasspathEntry[] cp = buildpathManager.getClasspath(javaProject.getProject(), scope, false, new NullProgressMonitor());
    for (int i = 0; i < cp.length; i++) {
      switch (cp[i].getEntryKind()) {
        case IClasspathEntry.CPE_PROJECT:
          addProjectEntries(resolved, cp[i].getPath(), scope, getArtifactClassifier(cp[i]), configuration);
          break;
        case IClasspathEntry.CPE_LIBRARY:
          resolved.add(JavaRuntime.newArchiveRuntimeClasspathEntry(cp[i].getPath()));
          break;
//        case IClasspathEntry.CPE_SOURCE:
//          resolved.add(newSourceClasspathEntry(javaProject, cp[i]));
//          break;
      }
    }
  }

  private int getArtifactScope(ILaunchConfiguration configuration) throws CoreException {
    String typeid = configuration.getType().getAttribute("id");
    if (JDT_JAVA_APPLICATION.equals(typeid)) {
      IResource[] resources = configuration.getMappedResources();

      // MNGECLIPSE-530: NPE starting openarchitecture workflow 
      if (resources == null || resources.length == 0) {
        return BuildPathManager.CLASSPATH_RUNTIME;
      }

      // ECLIPSE-33: applications from test sources should use test scope 
      final Set<IPath> testSources = new HashSet<IPath>();
      IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
      MavenProjectFacade facade = projectManager.create(javaProject.getProject(), new NullProgressMonitor());
      if (facade == null) {
        return BuildPathManager.CLASSPATH_RUNTIME;
      }
      
      facade.accept(new IMavenProjectVisitor() {
        public boolean visit(MavenProjectFacade projectFacade) {
          testSources.addAll(Arrays.asList(projectFacade.getTestCompileSourceLocations()));
          return true; // keep visiting
        }
        public void visit(MavenProjectFacade projectFacade, Artifact artifact) {
        }
      }, IMavenProjectVisitor.NESTED_MODULES);

      for (int i = 0; i < resources.length; i++) {
        for (IPath testPath : testSources) {
          if (testPath.isPrefixOf(resources[i].getProjectRelativePath())) {
            return BuildPathManager.CLASSPATH_TEST;
          }
        }
      }

      return BuildPathManager.CLASSPATH_RUNTIME;
    } else if (JDT_JUNIT_TEST.equals(typeid)) {
      return BuildPathManager.CLASSPATH_TEST;
    } else {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, 0, "Unsupported launch configuratio type " + typeid, null));
    }
  }

  protected void addProjectEntries(Set<IRuntimeClasspathEntry> resolved, IPath path, int scope, String classifier, ILaunchConfiguration launchConfiguration) throws CoreException {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IProject project = root.getProject(path.segment(0));

    MavenProjectFacade projectFacade = projectManager.create(project, new NullProgressMonitor());
    if(projectFacade == null) {
      return;
    }

    ResolverConfiguration configuration = projectFacade.getResolverConfiguration();
    if (configuration == null) {
      return;
    }

    final Set<IPath> allClasses = new LinkedHashSet<IPath>();
    final Set<IPath> allTestClasses = new LinkedHashSet<IPath>();

    projectFacade.accept(new IMavenProjectVisitor() {
      public boolean visit(MavenProjectFacade projectFacade) {
        // add real resources output folders
        Build build = projectFacade.getMavenProject().getBuild();
        allClasses.add(projectFacade.getProjectRelativePath(build.getOutputDirectory()));
        allTestClasses.add(projectFacade.getProjectRelativePath(build.getTestOutputDirectory()));

        // continue traversal
        return true; 
      }

      public void visit(MavenProjectFacade projectFacade, Artifact artifact) {
      }
    }, IMavenProjectVisitor.NESTED_MODULES);

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] cp = javaProject.getRawClasspath();

    boolean projectResolved = false;
    for (int i = 0; i < cp.length; i++) {
      IRuntimeClasspathEntry rce = null;
      switch (cp[i].getEntryKind()) {
        case IClasspathEntry.CPE_SOURCE:
          if (!projectResolved) {
            if (BuildPathManager.CLASSPATH_TEST == scope && isTestClassifier(classifier)) {
              // ECLIPSE-19: test classes come infront on the rest
              addFolders(resolved, project, allTestClasses);
            }
            if (isMainClassifier(classifier)) {
              addFolders(resolved, project, allClasses);
            }
            projectResolved = true;
          }
          break;
        case IClasspathEntry.CPE_CONTAINER:
          IClasspathContainer container = JavaCore.getClasspathContainer(cp[i].getPath(), javaProject);
          if (container != null && !MAVEN2_CONTAINER_PATH.isPrefixOf(cp[i].getPath())) {
            switch (container.getKind()) {
              case IClasspathContainer.K_APPLICATION:
                rce = JavaRuntime.newRuntimeContainerClasspathEntry(container.getPath(), IRuntimeClasspathEntry.USER_CLASSES, javaProject);
                break;
//                case IClasspathContainer.K_DEFAULT_SYSTEM:
//                  unresolved.add(JavaRuntime.newRuntimeContainerClasspathEntry(container.getPath(), IRuntimeClasspathEntry.STANDARD_CLASSES, javaProject));
//                  break;
//                case IClasspathContainer.K_SYSTEM:
//                  unresolved.add(JavaRuntime.newRuntimeContainerClasspathEntry(container.getPath(), IRuntimeClasspathEntry.BOOTSTRAP_CLASSES, javaProject));
//                  break;
            }
          }
          break;
        case IClasspathEntry.CPE_LIBRARY:
          rce = JavaRuntime.newArchiveRuntimeClasspathEntry(cp[i].getPath());
          break;
        case IClasspathEntry.CPE_VARIABLE:
          if (!JavaRuntime.JRELIB_VARIABLE.equals(cp[i].getPath().segment(0))) {
            rce = JavaRuntime.newVariableRuntimeClasspathEntry(cp[i].getPath());
          }
          break;
        case IClasspathEntry.CPE_PROJECT:
          IProject res = root.getProject(cp[i].getPath().segment(0));
          if (res != null) {
            IJavaProject otherProject = JavaCore.create(res);
            if (otherProject != null) {
              rce = JavaRuntime.newDefaultProjectClasspathEntry(otherProject);
            }
          }
          break;
        default:
          break;
      }
      if (rce != null) {
        addStandardClasspathEntries(resolved, rce, launchConfiguration);
      }
    }
  }

  private boolean isMainClassifier(String classifier) {
    return THIS_PROJECT_CLASSIFIER.equals(classifier) // main project
        || classifier == null; // default classifier
  }

  private boolean isTestClassifier(String classifier) {
    return THIS_PROJECT_CLASSIFIER.equals(classifier) // main project
        || TESTS_PROJECT_CLASSIFIER.equals(classifier) // tests classifier
        || classifier != null; // unknown classifier
  }

  private void addFolders(Set<IRuntimeClasspathEntry> resolved, IProject project, Set<IPath> folders) {
    for(IPath folder : folders) {
      IResource member = project.findMember(folder); // only returns existing members
      if(member instanceof IFolder) { // must exist and be a folder
        resolved.add(JavaRuntime.newArchiveRuntimeClasspathEntry(member.getFullPath()));
      }
    }
  }

  public static boolean isSupportedType(String id) {
    return supportedTypes.contains(id);
  }

  public static void enable(ILaunchConfiguration config) throws CoreException {
    if (config instanceof ILaunchConfigurationWorkingCopy) {
      enable((ILaunchConfigurationWorkingCopy) config);
    } else {
      ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
      enable(wc);
      wc.doSave();
    }
  }

  private static void enable(ILaunchConfigurationWorkingCopy wc) {
    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, "org.maven.ide.eclipse.launchconfig.classpathProvider");
    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, "org.maven.ide.eclipse.launchconfig.sourcepathProvider");
  }

  public static void disable(ILaunchConfiguration config) throws CoreException {
    ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, (String) null);
    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, (String) null);
    wc.doSave();
  }
  
  private static String getArtifactClassifier(IClasspathEntry entry) {
    IClasspathAttribute[] attributes = entry.getExtraAttributes();
    for(int j = 0; j < attributes.length; j++ ) {
      if(IMavenConstants.CLASSIFIER_ATTRIBUTE.equals(attributes[j].getName())) {
        return attributes[j].getValue();
      }
    }
    return null;
  }
}
