/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project.configurator;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.maven.ide.eclipse.internal.builder.InternalBuildParticipant;
import org.maven.ide.eclipse.project.IMavenProjectFacade;


/**
 * AbstractMavenBuildParticipant
 * 
 * @author igor
 */
public abstract class AbstractBuildParticipant extends InternalBuildParticipant {

  public abstract Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception;

  public boolean callOnEmptyDelta() {
    return false;
  }

//  protected List<MojoBinding> getMojoBindings(IMavenProjectFacade projectFacade, String pluginId, String goals,
//      VersionRange pluginVersion, IProgressMonitor monitor) throws CoreException {
//    ArrayList<MojoBinding> result = new ArrayList<MojoBinding>();
//
//    List<MojoBinding> mojoBindings = projectFacade.getMojoBindings(monitor);
//    for(MojoBinding mojoBinding : mojoBindings) {
//      ArtifactVersion version = new DefaultArtifactVersion(mojoBinding.getVersion());
//      if(pluginId.equals(mojoBinding.getGroupId() + ":" + mojoBinding.getArtifactId())
//          && (goals == null || goals.contains(mojoBinding.getGoal()))
//          && (pluginVersion == null || pluginVersion.containsVersion(version))) {
//        result.add(mojoBinding);
//      }
//    }
//
//    return result;
//  }

  @SuppressWarnings("unused")
  public void clean(IProgressMonitor monitor) throws CoreException {
    // default implementation does nothing
  }

  protected IMavenProjectFacade getMavenProjectFacade() {
    return super.getMavenProjectFacade();
  }
  
  protected IResourceDelta getDelta(IProject project) {
    return super.getDelta(project);
  }
}
