/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

public interface IBuildPathManager {

  /**
   * Download and attach sources and/or javadoc to the specified package 
   * fragment root using backgound job.
   * 
   * This method works both for Maven dependencies and plain jar/zip project
   * classpath entries. In the latter case, implementation will use jar/zip
   * SHA1 checksum of the file to identify Maven artifact using Nexus index.
   */
  public void scheduleDownload(IPackageFragmentRoot fragment, boolean downloadSources, boolean downloadJavadoc);

  /**
   * Download and attach sources and/or javadoc to all Maven dependencies of
   * specified Maven project using background job.
   */
  void scheduleDownload(IProject project, boolean downloadSources, boolean downloadJavadoc);

}
