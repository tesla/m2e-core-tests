/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.io.Serializable;


public class ArtifactRepositoryRef implements Serializable {

  private static final long serialVersionUID = -8681574362933142640L;

  private final String id;

  private final String url;

  public ArtifactRepositoryRef(String id, String url) {
    this.id = id;
    this.url = url;
  }

  public String getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }

  public int hashCode() {
    int hash = 17;
    hash = hash * 31 + (id != null ? id.hashCode() : 0);
    hash = hash * 31 + (url != null ? url.hashCode() : 0);
    return hash;
  }

  public boolean equals(Object o) {
    if(o == this) {
      return true;
    }
    if(!(o instanceof ArtifactRepositoryRef)) {
      return false;
    }
    ArtifactRepositoryRef other = (ArtifactRepositoryRef) o;
    return id.equals(other.id) && url.equals(other.url);
  }
}
