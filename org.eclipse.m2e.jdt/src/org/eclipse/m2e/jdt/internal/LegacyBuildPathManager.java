/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.jdt.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.jdt.BuildPathManager;


/**
 * LegacyBuildPathManager provides data compatibility for projects created using older versions of m2e. It will be
 * removed during codebase migration to eclipse.org and corresponding package name change.
 * 
 * @author Igor Fedorenko
 */
public class LegacyBuildPathManager {

//  private static boolean isMaven2ClasspathContainer(IPath containerPath) {
//    return containerPath != null && containerPath.segmentCount() > 0
//        && BuildPathManager.CONTAINER_ID.equals(containerPath.segment(0));
//  }

  private static ResolverConfiguration getResolverConfiguration(IClasspathEntry entry) {
    if(entry == null) {
      return new ResolverConfiguration();
    }

    String containerPath = entry.getPath().toString();

    boolean resolveWorkspaceProjects = containerPath.indexOf("/" + IMavenConstants.NO_WORKSPACE_PROJECTS) == -1; //$NON-NLS-1$

    // boolean filterResources = containerPath.indexOf("/" + MavenPlugin.FILTER_RESOURCES) != -1;

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setResolveWorkspaceProjects(resolveWorkspaceProjects);
    configuration.setActiveProfiles(getActiveProfiles(entry));
    return configuration;
  }

  private static String getActiveProfiles(IClasspathEntry entry) {
    String path = entry.getPath().toString();
    String prefix = "/" + IMavenConstants.ACTIVE_PROFILES + "["; //$NON-NLS-1$ //$NON-NLS-2$
    int n = path.indexOf(prefix);
    if(n == -1) {
      return ""; //$NON-NLS-1$
    }

    return path.substring(n + prefix.length(), path.indexOf("]", n)); //$NON-NLS-1$
  }

  public static ResolverConfiguration getResolverConfiguration(IProject project) {
    try {
      IJavaProject javaProject = JavaCore.create(project);
      IClasspathEntry[] entries = javaProject.getRawClasspath();
      for(int i = 0; i < entries.length; i++ ) {
        IClasspathEntry entry = entries[i];
        if(entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER
            && BuildPathManager.isMaven2ClasspathContainer(entry.getPath())) {
          return getResolverConfiguration(entry);
        }
      }
    } catch(CoreException e) {
      MavenLogger.log("Can't load legacy resolver configuration", e);
    }
    return new ResolverConfiguration();
  }

}
