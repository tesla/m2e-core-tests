/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.plugins;

import org.eclipse.core.runtime.IConfigurationElement;

public class PluginExtensionDescriptor {
  public static final String ARTIFACT_ID = "artifactId";
  public static final String GROUP_ID = "groupId";
  public static final String NAME = "name";
  
  private String artifactId;
  private String groupId;
  private String name;
  
  public PluginExtensionDescriptor(IConfigurationElement element) {
    artifactId = element.getAttribute(ARTIFACT_ID);
    groupId = element.getAttribute(GROUP_ID);
    name = element.getAttribute(NAME);
  }
  
  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }
  public String getArtifactId() {
    return artifactId;
  }
  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }
  public String getGroupId() {
    return groupId;
  }
  
  public String toString() {
    return groupId + ':' + artifactId; 
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
