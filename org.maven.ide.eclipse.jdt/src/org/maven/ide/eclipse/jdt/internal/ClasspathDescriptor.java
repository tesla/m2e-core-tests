/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.apache.maven.artifact.Artifact;

import org.maven.ide.eclipse.jdt.BuildPathManager;
import org.maven.ide.eclipse.jdt.IClasspathDescriptor;
import org.maven.ide.eclipse.jdt.IClasspathEntryDescriptor;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.util.Util;


/**
 * This class is an attempt to encapsulate list of IClasspathEntry's and operations on the list such as
 * "removeEntry", "addSourceEntry" and "addEntryAttribute". The idea is to provide JavaProjectConfigurator's classpath
 * whiteboard they can use to incrementally define classpath in a consistent manner.
 * 
 * @author igor
 */
public class ClasspathDescriptor implements IClasspathDescriptor {

  private static final IClasspathAttribute ATTR_OPTIONAL = JavaCore.newClasspathAttribute(IClasspathAttribute.OPTIONAL,
      "true");

  private final ArrayList<IClasspathEntryDescriptor> entries = new ArrayList<IClasspathEntryDescriptor>();

  private final IJavaProject project;
  
  public ClasspathDescriptor(IJavaProject project) {
    this.project = project;
  }

  /**
   * @return true if classpath contains entry with specified path, false otherwise.
   */
  public boolean containsPath(IPath path) {
    for(IClasspathEntryDescriptor descriptor : entries) {
      if(path.equals(descriptor.getClasspathEntry().getPath())) {
        return true;
      }
    }
    return false;
  }

  public void addSourceEntry(IPath sourcePath, IPath outputLocation, boolean optional) throws CoreException {
    addSourceEntry(sourcePath, //
        outputLocation, //
        new IPath[0] /* inclusion */, //
        new IPath[0] /* exclusion */, //
        optional );
  }

  public void removeEntry(final IPath path) {
    removeEntry(new EntryFilter() {
      public boolean accept(IClasspathEntryDescriptor descriptor) {
        return path.equals(descriptor.getClasspathEntry().getPath());
      }
    });
  }
  
  public void removeEntry(EntryFilter filter) {
    Iterator<IClasspathEntryDescriptor> iter = entries.iterator();
    while(iter.hasNext()) {
      IClasspathEntryDescriptor descriptor = iter.next();
      if(filter.accept(descriptor)) {
        iter.remove();
      }
    }
  }

  public void addSourceEntry(IPath sourcePath, IPath outputLocation, IPath[] inclusion, IPath[] exclusion,
      boolean optional) throws CoreException {
    IWorkspaceRoot workspaceRoot = project.getProject().getWorkspace().getRoot();

    Util.createFolder(workspaceRoot.getFolder(sourcePath), true);

    IClasspathAttribute[] attrs;
    if(optional) {
      attrs = new IClasspathAttribute[] {ATTR_OPTIONAL};
    } else {
      attrs = new IClasspathAttribute[0];
    }

    IClasspathEntry entry = JavaCore.newSourceEntry(sourcePath, //
        inclusion, //
        exclusion, //
        outputLocation, //
        attrs);

    ClasspathEntryDescriptor descriptor = new ClasspathEntryDescriptor(entry);

    entries.add(descriptor);
  }

  public IClasspathEntry[] getEntries() {
    IClasspathEntry[] result = new IClasspathEntry[entries.size()];

    for (int i = 0; i < entries.size(); i++) {
      result[i] = entries.get(i).getClasspathEntry();
    }

    return result;
  }

  public List<IClasspathEntryDescriptor> getEntryDescriptors() {
    return entries;
  }

  public void addEntry(IClasspathEntry entry) {
    entries.add(new ClasspathEntryDescriptor(entry));
  }

  public void addProjectEntry(Artifact artifact, IMavenProjectFacade projectFacade) {
    ArrayList<IClasspathAttribute> attributes = getMavenAttributes(artifact);
    IClasspathEntry entry = JavaCore.newProjectEntry(projectFacade.getFullPath(), // 
        new IAccessRule[0] /*accessRules*/, //
        true /*combineAccessRules*/, //
        attributes.toArray(new IClasspathAttribute[attributes.size()]), //
        false /*isExported*/);
    entries.add(new ClasspathEntryDescriptor(artifact, entry));
  }

  public void addLibraryEntry(Artifact artifact, IPath srcPath, IPath srcRoot, String javaDocUrl) {
    IPath entryPath = new Path(artifact.getFile().getAbsolutePath());
    
    ArrayList<IClasspathAttribute> attributes = getMavenAttributes(artifact);

    if(javaDocUrl != null) {
      attributes.add(JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
          javaDocUrl));
    }

    IClasspathEntry entry = JavaCore.newLibraryEntry(entryPath, //
        srcPath, //
        srcRoot, //
        new IAccessRule[0], //
        attributes.toArray(new IClasspathAttribute[attributes.size()]), // 
        false /*not exported*/);

    entries.add(new ClasspathEntryDescriptor(artifact, entry));
  }

  private ArrayList<IClasspathAttribute> getMavenAttributes(Artifact artifact) {
    ArrayList<IClasspathAttribute> attributes = new ArrayList<IClasspathAttribute>();
    attributes.add(JavaCore.newClasspathAttribute(BuildPathManager.GROUP_ID_ATTRIBUTE, artifact.getGroupId()));
    attributes.add(JavaCore.newClasspathAttribute(BuildPathManager.ARTIFACT_ID_ATTRIBUTE, artifact.getArtifactId()));
    attributes.add(JavaCore.newClasspathAttribute(BuildPathManager.VERSION_ATTRIBUTE, artifact.getVersion()));
    if (artifact.getClassifier() != null) {
      attributes.add(JavaCore.newClasspathAttribute(BuildPathManager.CLASSIFIER_ATTRIBUTE, artifact.getClassifier()));
    }
    if (artifact.getScope() != null) {
      attributes.add(JavaCore.newClasspathAttribute(BuildPathManager.SCOPE_ATTRIBUTE, artifact.getScope()));
    }
    return attributes;
  }

}
