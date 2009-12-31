/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.ui.externaltools.internal.model.ExternalToolBuilder;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.actions.MavenLaunchConstants;


public class MavenBuilderLaunchDelegate extends MavenLaunchDelegate {

  @Override
  protected String getGoals(ILaunchConfiguration configuration) throws CoreException {
    String buildType = ExternalToolBuilder.getBuildType();
    String key = MavenLaunchConstants.ATTR_GOALS;
    if(IExternalToolConstants.BUILD_TYPE_AUTO.equals(buildType)) {
      key = MavenLaunchConstants.ATTR_GOALS_AUTO_BUILD;
    } else if(IExternalToolConstants.BUILD_TYPE_CLEAN.equals(buildType)) {
      key = MavenLaunchConstants.ATTR_GOALS_CLEAN;
    } else if(IExternalToolConstants.BUILD_TYPE_FULL.equals(buildType)) {
      key = MavenLaunchConstants.ATTR_GOALS_AFTER_CLEAN;
    } else if(IExternalToolConstants.BUILD_TYPE_INCREMENTAL.equals(buildType)) {
      key = MavenLaunchConstants.ATTR_GOALS_MANUAL_BUILD;
    }
    String goals = configuration.getAttribute(key, "");
    if(goals == null || goals.length() == 0) {
      // use default goals when "full build" returns nothing
      goals = configuration.getAttribute(MavenLaunchConstants.ATTR_GOALS, "");
    }

    MavenPlugin.getDefault().getConsole().logMessage("Build type " + buildType + " : " + goals);
    return goals;
  }

}
