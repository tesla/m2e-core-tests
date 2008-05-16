/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml.search;

/**
 * Information about the artifact.
 *
 * @author Lukas Krecan
 */
public class ArtifactInfo {
  private final String groupId;
  private final String artifactId;
  private final String version;
  private final String classfier;
  private final String type;
  
  public ArtifactInfo(String groupId, String artifactId, String version, String classfier, String type) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.classfier = classfier;
    this.type = type;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public String getClassfier() {
    return classfier;
  }

  public String getType() {
    return type;
  }

  /**
   * Constructs a <code>String</code> with all attributes
   * in name = value format.
   *
   * @return a <code>String</code> representation 
   * of this object.
   */
  public String toString()
  {
      final String TAB = "    ";
      
      String retValue = "";
      
      retValue = "ArtifactInfo ( "
          + "groupId = " + this.groupId + TAB
          + "artifactId = " + this.artifactId + TAB
          + "version = " + this.version + TAB
          + "classfier = " + this.classfier + TAB
          + "type = " + this.type + TAB
          + " )";
  
      return retValue;
  }
}
