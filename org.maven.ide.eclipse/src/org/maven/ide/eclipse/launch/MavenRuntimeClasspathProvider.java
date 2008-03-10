/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.launch;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
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

  public static final String JDT_JUNIT_TEST = "org.eclipse.jdt.junit.launchconfig";

  public static final String JDT_JAVA_APPLICATION = "org.eclipse.jdt.launching.localJavaApplication";

  private static final Path MAVEN2_CONTAINER_PATH = new Path(MavenPlugin.CONTAINER_ID);

  public IRuntimeClasspathEntry[] computeUnresolvedClasspath(final ILaunchConfiguration configuration) throws CoreException {
    IRuntimeClasspathEntry jreEntry = JavaRuntime.computeJREEntry(configuration);

    IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
    IRuntimeClasspathEntry projectEntry = JavaRuntime.newProjectRuntimeClasspathEntry(javaProject);

    IRuntimeClasspathEntry mavenEntry = JavaRuntime.newRuntimeContainerClasspathEntry(MAVEN2_CONTAINER_PATH, IRuntimeClasspathEntry.USER_CLASSES);

    return new IRuntimeClasspathEntry[] {jreEntry, projectEntry, mavenEntry};
  }

  public IRuntimeClasspathEntry[] resolveClasspath(IRuntimeClasspathEntry[] entries, ILaunchConfiguration configuration)
      throws CoreException 
  {
    int scope = getArtifactScope(configuration);

    Set all = new LinkedHashSet(entries.length);
    for (int i = 0; i < entries.length; i++) {
      if (MAVEN2_CONTAINER_PATH.equals(entries[i].getPath()) && entries[i].getType() == IRuntimeClasspathEntry.CONTAINER) {
        addMavenClasspathEntries(all, entries[i], configuration, scope);
      } else if (entries[i].getType() == IRuntimeClasspathEntry.PROJECT) {
        addProjectEntries(all, entries[i].getPath(), scope);
      } else {
        IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveRuntimeClasspathEntry(entries[i], configuration);
        for (int j = 0; j < resolved.length; j++) {
          all.add(resolved[j]);
        }
      }
    }
    return (IRuntimeClasspathEntry[])all.toArray(new IRuntimeClasspathEntry[all.size()]);
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
          addProjectEntries(resolved, cp[i].getPath(), scope);
          break;
        case IClasspathEntry.CPE_LIBRARY:
          resolved.add(JavaRuntime.newArchiveRuntimeClasspathEntry(cp[i].getPath()));
          break;
        case IClasspathEntry.CPE_SOURCE:
          resolved.add(newSourceClasspathEntry(javaProject, cp[i]));
          break;
      }
    }
  }

  private int getArtifactScope(ILaunchConfiguration configuration) throws CoreException {
    String typeid = configuration.getType().getAttribute("id");
    if (JDT_JAVA_APPLICATION.equals(typeid)) {
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

      IResource[] resources = configuration.getMappedResources();
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

  protected void addProjectEntries(Set resolved, IPath path, int scope) throws CoreException {
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));
    IJavaProject javaProject = JavaCore.create(project);

    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    MavenProjectFacade facade = projectManager.create(project, new NullProgressMonitor());
    ResolverConfiguration resolverConfiguration = BuildPathManager.getResolverConfiguration(javaProject);

    if (facade != null && resolverConfiguration != null) {

      final Set allClasses = new LinkedHashSet();
      final Set allResources = new LinkedHashSet();
      final Set allTestClasses = new LinkedHashSet();
      final Set allTestResources = new LinkedHashSet();
      
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

      facade.accept(new IMavenProjectVisitor() {
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
  
      // putting all together
      if (BuildPathManager.CLASSPATH_TEST == scope) {
        // ECLIPSE-19: test classes resources come infront on the rest
        addFolders(resolved, project, allTestClasses);
        if (!resolverConfiguration.shouldFilterResources()) {
          addFolders(resolved, project, allTestResources);
        }
      }
      addFolders(resolved, project, allClasses);
      if (!resolverConfiguration.shouldFilterResources()) {
        addFolders(resolved, project, allResources);
      }
    }

//
//    resolved.add(JavaRuntime.newArchiveRuntimeClasspathEntry(javaProject.getOutputLocation()));

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

  private IRuntimeClasspathEntry newSourceClasspathEntry(IJavaProject javaProject, IClasspathEntry cpe) throws JavaModelException {
    IPath path = cpe.getOutputLocation();
    if (path != null) {
      return JavaRuntime.newArchiveRuntimeClasspathEntry(path);
    }
    return JavaRuntime.newArchiveRuntimeClasspathEntry(javaProject.getOutputLocation());
  }
}
