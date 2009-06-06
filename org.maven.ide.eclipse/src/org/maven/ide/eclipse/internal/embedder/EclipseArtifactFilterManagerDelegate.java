/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import java.util.Set;

import org.apache.maven.ArtifactFilterManagerDelegate;


/**
 * EclipseArtifactFilterManager
 * 
 * @author igor
 */
public class EclipseArtifactFilterManagerDelegate implements ArtifactFilterManagerDelegate {

  public void addCoreExcludes(Set<String> excludes) {
    excludes.add("plexus-utils");
    excludes.add("plexus-build-api");
  }

  public void addExcludes(Set<String> excludes) {
  }

}
