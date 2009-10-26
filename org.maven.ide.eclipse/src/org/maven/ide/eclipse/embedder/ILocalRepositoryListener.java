/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.io.File;

/**
 * ILocalRepositoryListener
 *
 * @author igor
 * 
 * @provisional This interface is provisional and can be changed or removed without notice
 */
public interface ILocalRepositoryListener {

  /**
   * New artifact has been downloaded or installed to maven local repository
   */
  public void artifactInstalled(File repositoryBasedir, ArtifactKey artifact, File artifactFile);
}
