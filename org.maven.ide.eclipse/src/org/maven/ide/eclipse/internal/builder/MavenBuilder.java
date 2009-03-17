/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.builder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;


public class MavenBuilder extends IncrementalProjectBuilder {

  public static boolean DEBUG = MavenPlugin.getDefault().isDebugging()
      & Boolean.parseBoolean(Platform.getDebugOption(IMavenConstants.PLUGIN_ID + "/debug/builder"));

  static interface GetDeltaCallback {
    public IResourceDelta getDelta(IProject project);
  }

  private GetDeltaCallback getDeltaCallback = new GetDeltaCallback() {
    public IResourceDelta getDelta(IProject project) {
      return MavenBuilder.this.getDelta(project);
    }
  };

  /*
   * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
   *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
   */
  @SuppressWarnings("unchecked")
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
    MavenPlugin plugin = MavenPlugin.getDefault();
    MavenConsole console = plugin.getConsole();
    MavenProjectManager projectManager = plugin.getMavenProjectManager();
    IProjectConfigurationManager configurationManager = plugin.getProjectConfigurationManager();

    IProject project = getProject();
    if(project.hasNature(IMavenConstants.NATURE_ID)) {
      IFile pomResource = project.getFile(IMavenConstants.POM_FILE_NAME);
      if(pomResource == null) {
        console.logError("Project " + project.getName() + " does not have pom.xml");
        return null;
      }

      IMavenProjectFacade projectFacade = projectManager.create(getProject(), monitor);
      if(projectFacade == null) {
        // XXX is this really possible? should we warn the user?
        return null;
      }

      IResourceDelta delta = getDelta(project);

      ILifecycleMapping lifecycleMapping = configurationManager.getLifecycleMapping(projectFacade);

      Set<IProject> dependencies = new HashSet<IProject>();

      for(InternalBuildParticipant participant : lifecycleMapping.getBuildParticipants()) {
        participant.setMavenProjectFacade(projectFacade);
        participant.setGetDeltaCallback(getDeltaCallback);
        try {
          if(FULL_BUILD == kind || delta != null || participant.callOnEmptyDelta()) {
            Set<IProject> sub = participant.build(kind, monitor);
            if(sub != null) {
              dependencies.addAll(sub);
            }
          }
        } catch(Exception e) {
          MavenLogger.log("Exception in build participant", e);
        } finally {
          participant.setMavenProjectFacade(null);
          participant.setGetDeltaCallback(null);
        }
      }

      return !dependencies.isEmpty()? dependencies.toArray(new IProject[dependencies.size()]): null;
    }
    return null;
  }

}
