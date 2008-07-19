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
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;

import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;

public class MavenLaunchConfigurationListener implements ILaunchConfigurationListener {

  public void launchConfigurationAdded(ILaunchConfiguration configuration) {
    updateLaunchConfiguration(configuration);
  }

  public void launchConfigurationChanged(ILaunchConfiguration configuration) {
    updateLaunchConfiguration(configuration);
  }

  public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
    // do nothing
  }

  private void updateLaunchConfiguration(ILaunchConfiguration configuration) {
    try {
      if (!MavenRuntimeClasspathProvider.isSupportedType(configuration.getType().getIdentifier())) {
        return;
      }
      if (configuration.getAttributes().containsKey(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER)) {
        return;
      }
      IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
      if (javaProject != null && javaProject.getProject().hasNature(IMavenConstants.NATURE_ID)) {
        MavenRuntimeClasspathProvider.enable(configuration);
      }
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }
  }

}
