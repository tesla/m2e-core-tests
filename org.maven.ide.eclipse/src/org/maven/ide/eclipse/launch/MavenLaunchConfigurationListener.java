/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.launch;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;

import org.maven.ide.eclipse.MavenPlugin;

public class MavenLaunchConfigurationListener implements ILaunchConfigurationListener {

  private static final Set supportedTypes = new HashSet();
  static {
    // not exactly nice, but works with eclipse 3.2, 3.3 and 3.4M3
    supportedTypes.add(MavenRuntimeClasspathProvider.JDT_JAVA_APPLICATION);
    supportedTypes.add(MavenRuntimeClasspathProvider.JDT_JUNIT_TEST);
  }

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
      if (!supportedTypes.contains(configuration.getType().getAttribute("id"))) {
        return;
      }
      if (configuration.getAttributes().containsKey(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER)) {
        return;
      }
      IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
      if (javaProject != null && javaProject.getProject().hasNature(MavenPlugin.NATURE_ID)) {
        if (configuration instanceof ILaunchConfigurationWorkingCopy) {
          enable((ILaunchConfigurationWorkingCopy) configuration);
        } else {
          ILaunchConfigurationWorkingCopy wc = configuration.getWorkingCopy();
          enable(wc);
          wc.doSave();
        }
      }
    } catch(CoreException ex) {
      MavenPlugin.log(ex);
    }
  }

  private void enable(ILaunchConfigurationWorkingCopy wc) {
    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, "org.maven.ide.eclipse.launchconfig.classpathProvider");
    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, "org.maven.ide.eclipse.launchconfig.sourcepathProvider");
  }

}
