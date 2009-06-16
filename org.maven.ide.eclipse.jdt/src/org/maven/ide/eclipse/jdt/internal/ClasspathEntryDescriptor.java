/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt.internal;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import org.apache.maven.artifact.Artifact;

import org.maven.ide.eclipse.jdt.IClasspathEntryDescriptor;


/**
 * ClasspathEntryDescriptor
 * 
 * @author igor
 */
public class ClasspathEntryDescriptor implements IClasspathEntryDescriptor {

  private IClasspathEntry entry;

  private final String scope;

  private final boolean optionalDependency;

  private final String groupId;

  private final String artifactId;

  public ClasspathEntryDescriptor(Artifact artifact, IClasspathEntry entry) {
    this.entry = entry;
    this.scope = artifact.getScope();
    this.optionalDependency = artifact.isOptional();
    this.groupId = artifact.getGroupId();
    this.artifactId = artifact.getArtifactId();
  }

  public ClasspathEntryDescriptor(IClasspathEntry entry) {
    this.entry = entry;

    this.scope = null;
    this.optionalDependency = false;
    this.groupId = null;
    this.artifactId = null;
  }

  public IClasspathEntry getClasspathEntry() {
    return entry;
  }

  public String getScope() {
    return scope;
  }

  /**
   * @return true if this entry corresponds to an optional maven dependency, false otherwise
   */
  public boolean isOptionalDependency() {
    return optionalDependency;
  }

  public void addClasspathAttribute(IClasspathAttribute attribute) {

    LinkedHashSet<IClasspathAttribute> attributes = new LinkedHashSet<IClasspathAttribute>();
    attributes.addAll(Arrays.asList(entry.getExtraAttributes()));
    attributes.add(attribute);
    IClasspathAttribute[] attributesArray = attributes.toArray(new IClasspathAttribute[attributes.size()]);

    switch(entry.getEntryKind()) {
      case IClasspathEntry.CPE_CONTAINER:
        entry = JavaCore.newContainerEntry(entry.getPath(), //
            entry.getAccessRules(), //
            attributesArray, //
            entry.isExported());
        break;
      case IClasspathEntry.CPE_LIBRARY:
        entry = JavaCore.newLibraryEntry(entry.getPath(), //
            entry.getSourceAttachmentPath(), //
            entry.getSourceAttachmentRootPath(), //
            entry.getAccessRules(), //
            attributesArray, //
            entry.isExported());
        break;
      case IClasspathEntry.CPE_SOURCE:
        entry = JavaCore.newSourceEntry(entry.getPath(), //
            entry.getInclusionPatterns(), //
            entry.getExclusionPatterns(), //
            entry.getOutputLocation(), //
            attributesArray);
        break;
      case IClasspathEntry.CPE_PROJECT:
        entry = JavaCore.newProjectEntry(entry.getPath(), //
            entry.getAccessRules(), //
            entry.combineAccessRules(), //
            attributesArray, //
            entry.isExported());
        break;
      default:
        throw new IllegalArgumentException("Unsupported IClasspathEntry kind=" + entry.getEntryKind());
    }
  }

  public String getGroupId() {
    return groupId;
  }

  public void setClasspathEntry(IClasspathEntry entry) {
    this.entry = entry;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public IPath getPath() {
    return entry.getPath();
  }

}
