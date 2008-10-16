/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt.internal.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;

import org.maven.ide.eclipse.core.MavenLogger;


/**
 * Maven Java source lookup director
 * 
 * @author Eugene Kuleshov
 */
public class MavenJavaSourceLookupDirector extends AbstractSourceLookupDirector {

  public void initializeParticipants() {
    addParticipants(new ISourceLookupParticipant[] {new JavaSourceLookupParticipant()});
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector#initializeDefaults(org.eclipse.debug.core.ILaunchConfiguration)
   */
  public void initializeDefaults(ILaunchConfiguration configuration) throws CoreException {
    dispose();
    setLaunchConfiguration(configuration);

    // if(ILaunchManager.DEBUG_MODE.equals(mode)) {
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      IProject[] projects = root.getProjects();
      List<IRuntimeClasspathEntry> entries = new ArrayList<IRuntimeClasspathEntry>();
      IRuntimeClasspathEntry jreEntry = JavaRuntime.computeJREEntry(configuration);
      if(jreEntry != null) {
        entries.add(jreEntry);
      }
      for(IProject project : projects) {
        IJavaProject javaProject = JavaCore.create(project);
        if(javaProject != null) {
          entries.add(JavaRuntime.newDefaultProjectClasspathEntry(javaProject));
        }
      }
  
      try {
        IRuntimeClasspathEntry[] array = entries.toArray(new IRuntimeClasspathEntry[entries.size()]);
        IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveSourceLookupPath(array, configuration);
        setSourceContainers(JavaRuntime.getSourceContainers(resolved));
      } catch(CoreException ex) {
        MavenLogger.log(ex);
      }
    // }
    
    initializeParticipants();
  }

}
