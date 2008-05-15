/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml;

/**
 * Context types
 * 
 * @author Lukas Krecan
 */
public enum PomTemplateContext {
  UNKNOWN("unknown"),
  PROJECT("project"),
  PARENT("parent"),
  PROPERTIES("properties"),
  DEPENDENCIES("dependencies"),
  EXCLUSIONS("exclusions"),
  PLUGINS("plugins"),
  REPOSITORIES("repositories"),
  GROUP_ID("groupId"),
  ARTIFACT_ID("artifactId"),
  VERSION("version"),
  CLASSIFIER("classifier"),
  TYPE("type");
  
  private static final String PREFIX = MvnIndexPlugin.PLUGIN_ID + ".templates.contextType.";
  
  private String contextTypeId;
  
  private PomTemplateContext(String contextTypeId) {
    this.contextTypeId = PREFIX + contextTypeId;
  }

  public String getContextTypeId() {
    return contextTypeId;
  }

  public void setContextTypeId(String contextTypeId) {
    this.contextTypeId = contextTypeId;
  }

  public static PomTemplateContext fromId(String contextTypeId) {
    for(PomTemplateContext context : values() ) {
      if(context.getContextTypeId().equals(contextTypeId)) {
        return context;
      }
    }
    return UNKNOWN;
  }
  
}
