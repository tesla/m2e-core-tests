/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.artifact.MavenMetadataCache;

/**
 * EclipseMavenMetadataCache
 *
 * @author igor
 */
public class EclipseMavenMetadataCache implements MavenMetadataCache {

  public ResolutionGroup get(Artifact artifact, ArtifactRepository localRepository,
      List<ArtifactRepository> remoteRepositories) {
    // TODO Auto-generated method get
    return null;
  }

  public void put(Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
      ResolutionGroup result) {
    // TODO Auto-generated method put
    
  }

}
