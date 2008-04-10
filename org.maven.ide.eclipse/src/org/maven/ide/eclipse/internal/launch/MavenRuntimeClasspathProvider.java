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
import java.util.Iterator;
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

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.BuildPathManager;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;


public class MavenRuntimeClasspathProvider extends StandardClasspathProvider {

  private static final Path MAVEN2_CONTAINER_PATH = new Path(MavenPlugin.CONTAINER_ID);
  
  public static final String JDT_JUNIT_TEST = "org.eclipse.jdt.junit.launchconfig";

  public static final String JDT_JAVA_APPLICATION = "org.eclipse.jdt.launching.localJavaApplication";

  private static final Set supportedTypes = new HashSet();
  static {
    // not exactly nice, but works with eclipse 3.2, 3.3 and 3.4M3
    supportedTypes.add(MavenRuntimeClasspathProvider.JDT_JAVA_APPLICATION);
    supportedTypes.add(MavenRuntimeClasspathProvider.JDT_JUNIT_TEST);
  }

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

    Set all = new LinkedHashSet(entries.length);
    for (int i = 0; i < entries.length; i++) {
      IRuntimeClasspathEntry entry = entries[i];
      if (MAVEN2_CONTAINER_PATH.equals(entry.getPath()) && entry.getType() == IRuntimeClasspathEntry.CONTAINER) {
        addMavenClasspathEntries(all, entry, configuration, scope);
      } else if (entry.getType() == IRuntimeClasspathEntry.PROJECT) {
        IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
        if (javaProject.getPath().equals(entry.getPath())) {
          addProjectEntries(all, entry.getPath(), scope, configuration);
        } else {
          addStandardClasspathEntries(all, entry, configuration);
        }
      } else {
        addStandardClasspathEntries(all, entry, configuration);
      }
    }
    return (IRuntimeClasspathEntry[])all.toArray(new IRuntimeClasspathEntry[all.size()]);
  }

  private void addStandardClasspathEntries(Set all, IRuntimeClasspathEntry entry, ILaunchConfiguration configuration)
      throws CoreException 
  {
    IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveRuntimeClasspathEntry(entry, configuration);
    for (int j = 0; j < resolved.length; j++) {
      all.add(resolved[j]);
    }
  }

  private void addMavenClasspathEntries(Set resolved, IRuntimeClasspathEntry runtimeClasspathEntry,
      ILaunchConfiguration configuration, int scope) throws CoreException 
  {
    IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
    MavenPlugin plugin = MavenPlugin.getDefault();
    BuildPathManager buildpathManager = plugin.getBuildpathManager();
    IClasspathEntry[] cp = buildpathManager.getClasspath(javaProject.getProject(), scope, new NullProgressMonitor());
    for (int i = 0; i < cp.length; i++) {
      switch (cp[i].getEntryKind()) {
        case IClasspathEntry.CPE_PROJECT:
          addProjectEntries(resolved, cp[i].getPath(), scope, configuration);
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
      Set testSources = new HashSet();
      IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
      IClasspathEntry[] cp = javaProject.getRawClasspath();
      for(int i = 0; i < cp.length; i++ ) {
        if(IClasspathEntry.CPE_SOURCE == cp[i].getEntryKind()) {
          if (BuildPathManager.TEST_TYPE.equals(getType(cp[i]))) {
            testSources.add(cp[i].getPath());
          }
        }
      }

      for (int i = 0; i < resources.length; i++) {
        for (Iterator j = testSources.iterator(); j.hasNext(); ) {
          IPath testPath = (IPath) j.next();
          if (testPath.isPrefixOf(resources[i].getFullPath())) {
            return BuildPathManager.CLASSPATH_TEST;
          }
        }
      }

      return BuildPathManager.CLASSPATH_RUNTIME;
    } else if (JDT_JUNIT_TEST.equals(typeid)) {
      return BuildPathManager.CLASSPATH_TEST;
    } else {
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, 0, "Unsupported launch configuratio type " + typeid, null));
    }
  }

  protected void addProjectEntries(Set resolved, IPath path, int scope, ILaunchConfiguration launchConfiguration) throws CoreException {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IProject project = root.getProject(path.segment(0));

    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    MavenProjectFacade projectFacade = projectManager.create(project, new NullProgressMonitor());
    if(projectFacade == null) {
      return;
    }
    
    ResolverConfiguration configuration = projectFacade.getResolverConfiguration();
    if (configuration == null) {
      return;
    }

    final Set allClasses = new LinkedHashSet();
    final Set allResources = new LinkedHashSet();
    final Set allTestClasses = new LinkedHashSet();
    final Set allTestResources = new LinkedHashSet();
    
    IJavaProject javaProject = JavaCore.create(project);
    // JDT source->output mapping
    IClasspathEntry[] cp = javaProject.getRawClasspath();
    for(int i = 0; i < cp.length; i++ ) {
      if(IClasspathEntry.CPE_SOURCE == cp[i].getEntryKind()) {
        IPath outputLocation;
        if (cp[i].getOutputLocation() != null) {
          outputLocation = cp[i].getOutputLocation();
        } else {
          outputLocation = javaProject.getOutputLocation();
        }
        outputLocation = outputLocation.removeFirstSegments(1).makeRelative();
        if (BuildPathManager.TEST_TYPE.equals(getType(cp[i]))) {
          allTestClasses.add(outputLocation);
        } else {
          allClasses.add(outputLocation);
        }
      }
    }

    projectFacade.accept(new IMavenProjectVisitor() {
      public boolean visit(MavenProjectFacade projectFacade) {
        allResources.addAll(Arrays.asList(projectFacade.getResourceLocations()));
        allTestResources.addAll(Arrays.asList(projectFacade.getTestResourceLocations()));
        return true; // continue traversal
      }

      public void visit(MavenProjectFacade projectFacade, Artifact artifact) {
      }
    }, IMavenProjectVisitor.NESTED_MODULES);

    // compensate for resources source entries 
    allClasses.removeAll(allResources);
    allClasses.removeAll(allTestResources);

    boolean projectResolved = false;
    for (int i = 0; i < cp.length; i++) {
      IRuntimeClasspathEntry rce = null;
      switch (cp[i].getEntryKind()) {
        case IClasspathEntry.CPE_SOURCE:
          if (!projectResolved) {
            if (BuildPathManager.CLASSPATH_TEST == scope) {
              // ECLIPSE-19: test classes resources come infront on the rest
              addFolders(resolved, project, allTestClasses);
              if (!configuration.shouldFilterResources()) {
                addFolders(resolved, project, allTestResources);
              }
            }
            addFolders(resolved, project, allClasses);
            if (!configuration.shouldFilterResources()) {
              addFolders(resolved, project, allResources);
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

  private String getType(IClasspathEntry entry) {
    IClasspathAttribute[] attrs = entry.getExtraAttributes();
    for (int i = 0; i < attrs.length; i++) {
      if (MavenPlugin.TYPE_ATTRIBUTE.equals(attrs[i].getName())) {
        return attrs[i].getValue();
      }
    }
    return null;
  }

  private void addFolders(Set resolved, IProject project, Set folders) {
    for(Iterator i = folders.iterator(); i.hasNext(); ) {
      IPath folder = (IPath) i.next();
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
}
