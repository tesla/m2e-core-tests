/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.builder;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IProgressMonitor;

import org.maven.ide.eclipse.project.IMavenProjectFacade;

public abstract class InternalBuildParticipant {
  
  private IMavenProjectFacade facade;
  private MavenBuilder.GetDeltaCallback getDeltaCallback;
//  private BuildContext buildContext;

  protected IMavenProjectFacade getMavenProjectFacade() {
    return facade;
  }

  void setMavenProjectFacade(IMavenProjectFacade facade) {
    this.facade = facade;
  }

  protected IResourceDelta getDelta(IProject project) {
    return getDeltaCallback.getDelta(project);
  }
  
  void setGetDeltaCallback(MavenBuilder.GetDeltaCallback getDeltaCallback) {
    this.getDeltaCallback = getDeltaCallback;
  }

  public abstract Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception;

  public abstract boolean callOnEmptyDelta();

}
