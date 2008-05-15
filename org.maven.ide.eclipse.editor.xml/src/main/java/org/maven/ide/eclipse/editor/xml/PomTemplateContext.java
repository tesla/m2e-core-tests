/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import org.maven.ide.eclipse.editor.xml.search.NodeInfo;
import org.maven.ide.eclipse.editor.xml.search.SearchEngine;

/**
 * Context types. 
 * 
 * @author Lukas Krecan
 */
public enum PomTemplateContext {
  UNKNOWN("unknown",false),
  PROJECT("project", true),
  PARENT("parent", true),
  PROPERTIES("properties", true),
  DEPENDENCIES("dependencies", true),
  EXCLUSIONS("exclusions", true),
  PLUGINS("plugins", true),
  REPOSITORIES("repositories", true),
  GROUP_ID("groupId", false){
    @Override
    protected void findCompletitionTemplates(NodeInfo nodeInfo, Collection<Template> templates) {
      for(String groupId : getSearchEngine().findGroupIds(nodeInfo.getPrefix(), nodeInfo.getPackaging(), nodeInfo.getContainingArtifact())) {
        templates.add(new Template(groupId, groupId, getContextTypeId(), groupId, false));
      }
    }
  },
  ARTIFACT_ID("artifactId", false)
  {
    @Override
    protected void findCompletitionTemplates(NodeInfo nodeInfo, Collection<Template> templates) {
      String groupId = nodeInfo.getGroupId();
      if(groupId != null) {
        for(String artifactId : getSearchEngine().findArtifactIds(groupId, nodeInfo.getPrefix(), nodeInfo.getPackaging(), nodeInfo.getContainingArtifact())) {
          templates.add(new Template(artifactId, groupId + ":" + artifactId, getContextTypeId(), artifactId, false));
        }
      }
    }
  },
  VERSION("version", false)
  {
    @Override
    protected void findCompletitionTemplates(NodeInfo nodeInfo, Collection<Template> templates) {
      String groupId = nodeInfo.getGroupId();
      String artifactId = nodeInfo.getArtifactId();
      if(groupId != null && artifactId != null) {
        for(String version : getSearchEngine().findVersions(groupId, artifactId, nodeInfo.getPrefix(), nodeInfo.getPackaging())) {
          templates.add(new Template(version, groupId + ":" + artifactId + ":" + version, //
              getContextTypeId(), version, false));
        }
      }
    }
  },
  CLASSIFIER("classifier",false)
  {
    @Override
    protected void findCompletitionTemplates(NodeInfo nodeInfo, Collection<Template> templates) {
      String groupId = nodeInfo.getGroupId();
      String artifactId = nodeInfo.getArtifactId();
      String version = nodeInfo.getVersion();
      if(groupId != null && artifactId != null && version != null) {
        for(String classifier : getSearchEngine().findClassifiers(groupId, artifactId, version, nodeInfo.getPrefix(), nodeInfo.getPackaging())) {
          templates.add(new Template(classifier, groupId + ":" + artifactId + ":" + version + ":" + classifier,
              getContextTypeId(), classifier, false));
        }
      }
    }
  },
  TYPE("type",false)
  {
    @Override
    protected void findCompletitionTemplates(NodeInfo nodeInfo, Collection<Template> templates) {
      String groupId = nodeInfo.getGroupId();
      String artifactId = nodeInfo.getArtifactId();
      String version = nodeInfo.getVersion();
      if(groupId != null && artifactId != null && version != null) {
        for(String type : getSearchEngine().findTypes(groupId, artifactId, version, nodeInfo.getPrefix(), nodeInfo.getPackaging())) {
          templates.add(new Template(type, groupId + ":" + artifactId + ":" + version + ":" + type, //
              getContextTypeId(), type, false));
        }
      }
    }
  };
  
  private static final String PREFIX = MvnIndexPlugin.PLUGIN_ID + ".templates.contextType.";
  
  private final String idSuffix;
  private final boolean template;
  
  private PomTemplateContext(String idSuffix, boolean template) {
    this.idSuffix =  idSuffix;
    this.template = template;
  }
  
  /**
   * Return templates depending on the context type.
   * @param templates
   * @param contextTypeId
   */
  public Template[] getTemplates(NodeInfo nodeInfo)
  {
    if (isTemplate())
    {
      TemplateStore store = getTemplateStore();
      if(store != null) {
        return store.getTemplates(getContextTypeId());
      }
      else
      {
        return new Template[0];
      }
    }
    else
    {
      Collection<Template> templates = new ArrayList<Template>();
      findCompletitionTemplates(nodeInfo, templates);
      return templates.toArray(new Template[templates.size()]);
    }
  }
  
  /**
   * Looks for completition using search engine. To be overriden if needed.
   * @return
   */
  protected void findCompletitionTemplates(NodeInfo nodeInfo, Collection<Template> templates) {
   
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

  private static TemplateStore getTemplateStore() {
    return MvnIndexPlugin.getDefault().getTemplateStore();
  }
  
  private static SearchEngine getSearchEngine() {
    return MvnIndexPlugin.getDefault().getSearchEngine();
  }
}
