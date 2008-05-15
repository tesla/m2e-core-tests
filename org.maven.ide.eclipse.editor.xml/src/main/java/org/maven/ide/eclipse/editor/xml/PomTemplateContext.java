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
  UNKNOWN("unknown",false),
  PROJECT("project", true),
  PARENT("parent", true),
  PROPERTIES("properties", true),
  DEPENDENCIES("dependencies", true),
  EXCLUSIONS("exclusions", false),
  PLUGINS("plugins", false),
  REPOSITORIES("repositories", false),
  GROUP_ID("groupId", false),
  ARTIFACT_ID("artifactId", false),
  VERSION("version", false),
  CLASSIFIER("classifier",false),
  TYPE("type",false);
  
  private static final String PREFIX = MvnIndexPlugin.PLUGIN_ID + ".templates.contextType.";
  
  private final String idSuffix;
  private final boolean template;
  
  private PomTemplateContext(String idSuffix, boolean template) {
    this.idSuffix =  idSuffix;
    this.template = template;
  }
  
  protected String getIdSuffix() {
    return idSuffix;
  }
  
  /**
   * Returns true it the template is used. It means, that proposal is not found using SearchEngine but is loaded
   * from the template store.
   * @return
   */
  public boolean isTemplate() {
    return template;
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
