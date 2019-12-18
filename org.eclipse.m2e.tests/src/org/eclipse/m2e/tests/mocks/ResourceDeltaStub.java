/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.mocks;

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


public class ResourceDeltaStub extends PlatformObject implements IResourceDelta {

  private IResource resource;

  private int kind;

  private int flags;

  private List<IResourceDelta> children = new ArrayList<>();

  public ResourceDeltaStub(IResource resource) {
    this.resource = resource;
    kind = IResourceDelta.CHANGED;
    flags = IResourceDelta.CONTENT;
  }

  public <T extends IResourceDelta> T addChild(T child) {
    children.add(child);
    return child;
  }

  @Override
  public void accept(IResourceDeltaVisitor visitor) throws CoreException {
    accept(visitor, 0);
  }

  @Override
  public void accept(IResourceDeltaVisitor visitor, boolean includePhantoms) throws CoreException {
    accept(visitor, includePhantoms ? IContainer.INCLUDE_PHANTOMS : 0);
  }

  @Override
  public void accept(IResourceDeltaVisitor visitor, int memberFlags) throws CoreException {
    if(!visitor.visit(this)) {
      return;
    }
    for(IResourceDelta child : children) {
      child.accept(visitor, memberFlags);
    }
  }

  @Override
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

  @Override
  public IResourceDelta[] getAffectedChildren() {
    return getAffectedChildren(ADDED | REMOVED | CHANGED, IResource.NONE);
  }

  @Override
  public IResourceDelta[] getAffectedChildren(int kindMask) {
    return getAffectedChildren(kindMask, IResource.NONE);
  }

  @Override
  public IResourceDelta[] getAffectedChildren(int kindMask, int memberFlags) {
    List<IResourceDelta> result = new ArrayList<>();
    for(IResourceDelta child : children) {
      if((child.getKind() & kindMask) != 0) {
        result.add(child);
      }
    }
    return result.toArray(new IResourceDelta[result.size()]);
  }

  @Override
  public int getKind() {
    return kind;
  }

  @Override
  public int getFlags() {
    return flags;
  }

  @Override
  public IPath getFullPath() {
    return resource.getFullPath();
  }

  @Override
  public IPath getProjectRelativePath() {
    return resource.getProjectRelativePath();
  }

  @Override
  public IMarkerDelta[] getMarkerDeltas() {
    return new IMarkerDelta[0];
  }

  @Override
  public IPath getMovedFromPath() {
    return null;
  }

  @Override
  public IPath getMovedToPath() {
    return null;
  }

  @Override
  public IResource getResource() {
    return resource;
  }

}
