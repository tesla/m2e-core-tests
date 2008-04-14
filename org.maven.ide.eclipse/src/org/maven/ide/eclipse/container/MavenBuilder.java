/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.container;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.apache.maven.artifact.Artifact;

import org.maven.ide.eclipse.MavenConsole;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;


public class MavenBuilder extends IncrementalProjectBuilder {

  private final MavenConsole console;
  private MavenProjectManager projectManager;

  public MavenBuilder() {
    MavenPlugin plugin = MavenPlugin.getDefault();
    console = plugin.getConsole();
    this.projectManager = plugin.getMavenProjectManager();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
   *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
   */
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
    IProject project = getProject();
    if(project.hasNature(MavenPlugin.NATURE_ID)) {
      IFile pomResource = project.getFile(MavenPlugin.POM_FILE_NAME);
      if(pomResource == null) {
        console.logError("Project " + project.getName() + " don't have pom.xml");
        return null;
      }

      // if( kind == AUTO_BUILD || kind == INCREMENTAL_BUILD ) {
      if (CLEAN_BUILD == kind || FULL_BUILD == kind) {
//        buildpathManager.updateClasspathContainer(project, monitor);
      }

      ResolverConfiguration configuration = projectManager.getResolverConfiguration(project);
      if (configuration.shouldFilterResources()) {
        filterResources(project, kind, monitor);
      }
    }
    return null;
  }

  private void filterResources(IProject project, int kind, final IProgressMonitor monitor) throws CoreException {
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    MavenProjectFacade mavenProject = projectManager.create(getProject(), monitor);
    if (mavenProject == null) {
      return;
    }
    
    Set allResourceFolders = new LinkedHashSet();
    allResourceFolders.addAll(Arrays.asList(mavenProject.getResourceLocations()));
    allResourceFolders.addAll(Arrays.asList(mavenProject.getTestCompileSourceLocations()));

    if (INCREMENTAL_BUILD == kind || AUTO_BUILD == kind) {
      IResourceDelta delta = getDelta(project);
      if (delta != null) {
        boolean found = false;
        found = delta.findMember(new Path(MavenPlugin.POM_FILE_NAME)) != null;
        if (!found) {
          for (Iterator i = allResourceFolders.iterator(); i.hasNext(); ) {
            IPath folderPath = (IPath) i.next();
            IResourceDelta member = delta.findMember(folderPath);
            // XXX deal with delta kind/flags
            if (member != null) {
              found = true;
              break;
            }
          }
        }
        if (!found) {
          return;
        }
      }
    }

    mavenProject.accept(new IMavenProjectVisitor() {
      public boolean visit(MavenProjectFacade projectFacade) throws CoreException {
        projectFacade.filterResources(monitor);
        return true;
      }
      public void visit(MavenProjectFacade projectFacade, Artifact artifact) {
      }
    }, IMavenProjectVisitor.NESTED_MODULES);
  }

}
