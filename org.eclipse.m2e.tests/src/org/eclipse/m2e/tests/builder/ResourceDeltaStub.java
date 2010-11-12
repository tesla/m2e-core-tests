/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.builder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;


class ResourceDeltaStub extends PlatformObject implements IResourceDelta {

  private IResource resource;

  private int kind;

  private int flags;

  private List<IResourceDelta> children = new ArrayList<IResourceDelta>();

  public ResourceDeltaStub(IResource resource) {
    this.resource = resource;
    kind = IResourceDelta.CHANGED;
    flags = IResourceDelta.CONTENT;
  }

  public <T extends IResourceDelta> T addChild(T child) {
    children.add(child);
    return child;
  }

  public void accept(IResourceDeltaVisitor visitor) throws CoreException {
    accept(visitor, 0);
  }

  public void accept(IResourceDeltaVisitor visitor, boolean includePhantoms) throws CoreException {
    accept(visitor, includePhantoms ? IContainer.INCLUDE_PHANTOMS : 0);
  }

  public void accept(IResourceDeltaVisitor visitor, int memberFlags) throws CoreException {
    if(!visitor.visit(this)) {
      return;
    }
    for(IResourceDelta child : children) {
      child.accept(visitor, memberFlags);
    }
  }

  public IResourceDelta findMember(IPath path) {
    int segmentCount = path.segmentCount();
    if(segmentCount == 0) {
      return this;
    }

    String segment = path.segment(0);
    for(IResourceDelta child : children) {
      if(child.getFullPath().lastSegment().equals(segment)) {
        return child.findMember(path.removeFirstSegments(1));
      }
    }

    return null;
  }

  public IResourceDelta[] getAffectedChildren() {
    return getAffectedChildren(ADDED | REMOVED | CHANGED, IResource.NONE);
  }

  public IResourceDelta[] getAffectedChildren(int kindMask) {
    return getAffectedChildren(kindMask, IResource.NONE);
  }

  public IResourceDelta[] getAffectedChildren(int kindMask, int memberFlags) {
    List<IResourceDelta> result = new ArrayList<IResourceDelta>();
    for(IResourceDelta child : children) {
      if((child.getKind() & kindMask) != 0) {
        result.add(child);
      }
    }
    return result.toArray(new IResourceDelta[result.size()]);
  }

  public int getKind() {
    return kind;
  }

  public int getFlags() {
    return flags;
  }

  public IPath getFullPath() {
    return resource.getFullPath();
  }

  public IPath getProjectRelativePath() {
    return resource.getProjectRelativePath();
  }

  public IMarkerDelta[] getMarkerDeltas() {
    return new IMarkerDelta[0];
  }

  public IPath getMovedFromPath() {
    return null;
  }

  public IPath getMovedToPath() {
    return null;
  }

  public IResource getResource() {
    return resource;
  }

}
