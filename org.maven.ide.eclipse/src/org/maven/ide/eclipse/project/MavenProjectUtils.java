/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.apache.maven.model.Resource;

/**
 * Collection of helper methods to map between MavenProject and IResource.
 *
 * @author igor
 */
public class MavenProjectUtils {

  private MavenProjectUtils() {
    
  }

  /**
   * Returns project resource for given filesystem location or null the location is outside of project.
   * 
   * @param resourceLocation absolute filesystem location
   * @return IPath the full, absolute workspace path resourceLocation
   */
  public static IPath getProjectRelativePath(IProject project, String resourceLocation) {
    if(resourceLocation == null) {
      return null;
    }
    IPath projectLocation = project.getLocation();
    IPath directory = Path.fromOSString(resourceLocation); // this is an absolute path!
    if(!projectLocation.isPrefixOf(directory)) {
      return null;
    }

    return directory.removeFirstSegments(projectLocation.segmentCount()).makeRelative().setDevice(null);
  }

  public static IPath[] getResourceLocations(IProject project, List/*<Resource>*/ resources) {
    LinkedHashSet locations = new LinkedHashSet();
    for(Iterator it = resources.iterator(); it.hasNext();) {
      Resource resource = (Resource) it.next();
      locations.add(getProjectRelativePath(project, resource.getDirectory()));
    }
    return (IPath[]) locations.toArray(new IPath[locations.size()]);
  }

  public static IPath[] getSourceLocations(IProject project, List/*<String>*/ roots) {
    LinkedHashSet locations = new LinkedHashSet();
    for(Iterator i = roots.iterator(); i.hasNext();) {
      IPath path = getProjectRelativePath(project, (String) i.next());
      if(path != null) {
        locations.add(path);
      }
    }
    return (IPath[]) locations.toArray(new IPath[locations.size()]);
  }

  /**
   * Returns the full, absolute path of the given file relative to the workspace. Returns null if the file does not
   * exist or is not a member of this project.
   */
  public static IPath getFullPath(IProject project, File file) {
    if (project == null || file == null) {
      return null;
    }

    IPath projectPath = project.getLocation();
    if(projectPath == null) {
      return null;
    }
    
    IPath filePath = new Path(file.getAbsolutePath());
    if (!projectPath.isPrefixOf(filePath)) {
      return null;
    }
    IResource resource = project.findMember(filePath.removeFirstSegments(projectPath.segmentCount()));
    if (resource == null) {
      return null;
    }
    return resource.getFullPath();
  }

}
