/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project.registry;

import org.maven.ide.eclipse.embedder.ArtifactKey;


/**
 * MavenCapability
 * 
 * @author igor
 */
public class MavenCapability extends Capability {

  private static final long serialVersionUID = 8930981127331238566L;

  /**
   * Regular Maven dependency as defined in <dependency/> pom.xml element.
   */
  public static final String NS_MAVEN_ARTIFACT = "maven-artifact";

  /**
   * Maven parent dependency as defined in <parent/> pom.xml element.
   */
  public static final String NS_MAVEN_PARENT = "maven-parent";

  private final String version;

  private MavenCapability(String namespace, String id, String version) {
    super(namespace, id);
    this.version = version;
  }

  public String getVersion() {
    return version;
  }

  public String toString() {
    return getVersionlessKey().toString() + "/" + version;
  }

  public int hashCode() {
    int hash = getVersionlessKey().hashCode();
    hash = hash * 17 + version.hashCode();
    return hash;
  }

  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(!(obj instanceof MavenCapability)) {
      return false;
    }
    MavenCapability other = (MavenCapability) obj;
    return getVersionlessKey().equals(other.getVersionlessKey()) && version.equals(other.version);
  }

  public static MavenCapability createMaven(ArtifactKey key) {
    return new MavenCapability(NS_MAVEN_ARTIFACT, getId(key), key.getVersion());
  }

  public static MavenCapability createMavenParent(ArtifactKey key) {
    return new MavenCapability(NS_MAVEN_PARENT, getId(key), key.getVersion());
  }

  static String getId(ArtifactKey key) {
    StringBuilder sb = new StringBuilder();
    sb.append(key.getGroupId());
    sb.append(':').append(key.getArtifactId());
    if(key.getClassifier() != null) {
      sb.append(':').append(key.getClassifier());
    }
    return sb.toString();
  }

}
