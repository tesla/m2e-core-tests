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
  
  private String idSuffix;
  
  private PomTemplateContext(String idSuffix) {
    this.idSuffix =  idSuffix;
  }
  
  protected String getIdSuffix() {
    return idSuffix;
  }

  public String getContextTypeId() {
    return PREFIX + idSuffix;
  }

  public static PomTemplateContext fromId(String contextTypeId) {
    for(PomTemplateContext context : values() ) {
      if(context.getContextTypeId().equals(contextTypeId)) {
        return context;
      }
    }
    return UNKNOWN;
  }
  
  public static PomTemplateContext fromNodeName(String idSuffix) {
    for(PomTemplateContext context : values() ) {
      if(context.getIdSuffix().equals(idSuffix)) {
        return context;
      }
    }
    return UNKNOWN;
  }

 
  
}
