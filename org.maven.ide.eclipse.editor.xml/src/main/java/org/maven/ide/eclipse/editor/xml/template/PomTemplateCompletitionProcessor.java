/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml.template;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.swt.graphics.Image;

import org.maven.ide.eclipse.editor.xml.MvnIndexPlugin;
import org.maven.ide.eclipse.editor.xml.PomContentAssistProcessor;
import org.maven.ide.eclipse.editor.xml.PomTemplateContext;
import org.maven.ide.eclipse.editor.xml.search.ArtifactInfo;
import org.maven.ide.eclipse.editor.xml.search.Packaging;
import org.maven.ide.eclipse.editor.xml.search.SearchEngine;


/**
 * Does code competition. It is hooked to the XML editor by {@link PomContentAssistProcessor}.
 * 
 * @author Lukas Krecan
 */
public class PomTemplateCompletitionProcessor extends TemplateCompletionProcessor {
  private String contextTypeId = null;

  private Node currentNode;

  private String prefix;

  public String getContextTypeId() {
    return contextTypeId;
  }

  public void setContextTypeId(String contextTypeId) {
    this.contextTypeId = contextTypeId;
  }

  protected SearchEngine getSearchEngine() {
    return MvnIndexPlugin.getDefault().getSearchEngine();
  }

  @Override
  protected TemplateContextType getContextType(ITextViewer viewer, IRegion region) {
    ContextTypeRegistry registry = getTemplateContextRegistry();
    if(registry != null) {
      return registry.getContextType(contextTypeId);
    }
    return null;
  }

  @Override
  protected Image getImage(Template template) {
    return null;
  }

  @Override
  protected Template[] getTemplates(String contextTypeId) {
    List<Template> templates = new ArrayList<Template>();
    
      
    PomTemplateContext templateContext = PomTemplateContext.fromId(contextTypeId);
    if (templateContext.isTemplate())
    {
      TemplateStore store = getTemplateStore();
      if(store != null) {
        return store.getTemplates(contextTypeId);
      }
    }
    else
    {
      switch(templateContext) {
        case GROUP_ID:
          for(String groupId : getSearchEngine().findGroupIds(prefix, getPackaging(), getContainingArtifact())) {
            templates.add(new Template(groupId, groupId, contextTypeId, groupId, false));
          }
          break;
          
        case ARTIFACT_ID:
          addArtifactIdTemplates(templates, contextTypeId);
          break;
  
        case VERSION:
          addVersionTemplates(templates, contextTypeId);
          break;
          
        case CLASSIFIER:
          addClassifierTemplates(templates, contextTypeId);
          break;
  
        case TYPE:
          addTypeTemplates(templates, contextTypeId);
          break;
      }
    }

    return templates.toArray(new Template[templates.size()]);
  }

  private void addTypeTemplates(List<Template> templates, String contextTypeId) {
    String groupId = getGroupId();
    String artifactId = getArtifactId();
    String version = getVersion();
    if(groupId != null && artifactId != null && version != null) {
      for(String type : getSearchEngine().findTypes(groupId, artifactId, version, prefix, getPackaging())) {
        templates.add(new Template(type, groupId + ":" + artifactId + ":" + version + ":" + type, //
            contextTypeId, type, false));
      }
    }
  }

  private void addClassifierTemplates(List<Template> templates, String contextTypeId) {
    String groupId = getGroupId();
    String artifactId = getArtifactId();
    String version = getVersion();
    if(groupId != null && artifactId != null && version != null) {
      for(String classifier : getSearchEngine().findClassifiers(groupId, artifactId, version, prefix, getPackaging())) {
        templates.add(new Template(classifier, groupId + ":" + artifactId + ":" + version + ":" + classifier,
            contextTypeId, classifier, false));
      }
    }
  }

  private void addVersionTemplates(List<Template> templates, String contextTypeId) {
    String groupId = getGroupId();
    String artifactId = getArtifactId();
    if(groupId != null && artifactId != null) {
      for(String version : getSearchEngine().findVersions(groupId, artifactId, prefix, getPackaging())) {
        templates.add(new Template(version, groupId + ":" + artifactId + ":" + version, //
            contextTypeId, version, false));
      }
    }
  }

  private void addArtifactIdTemplates(List<Template> templates, String contextTypeId) {
    String groupId = getGroupId();
    if(groupId != null) {
      for(String artifactId : getSearchEngine().findArtifactIds(groupId, prefix, getPackaging(),
          getContainingArtifact())) {
        templates.add(new Template(artifactId, groupId + ":" + artifactId, contextTypeId, artifactId, false));
      }
    }
  }

  /**
   * Returns containing artifactInfo for exclusions. Otherwise returns null.
   */
  private ArtifactInfo getContainingArtifact() {
    if(isExclusion()) {
      Node node = currentNode.getParentNode().getParentNode();
      return getArtifactInfo(node);
    }
    return null;
  }

  /**
   * Returns artifact info from siblings of given node.
   */
  private ArtifactInfo getArtifactInfo(Node node) {
    return new ArtifactInfo(getGroupId(node), getArtifactId(node), getVersion(node), //
        getSiblingTextValue(node, "classifier"), getSiblingTextValue(node, "type"));
  }

  private Packaging getPackaging() {
    if(isPlugin()) {
      return Packaging.PLUGIN;
    } else if(isParent()) {
      return Packaging.POM;
    }
    return Packaging.ALL;
  }

  public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset, Node currentNode) {
    this.currentNode = currentNode;
    return super.computeCompletionProposals(viewer, offset);
  }

  private String getGroupId() {
    return getGroupId(currentNode);
  }

  private String getGroupId(Node sibling) {
    return getSiblingTextValue(sibling, "groupId");
  }

  private String getArtifactId() {
    return getArtifactId(currentNode);
  }

  private String getArtifactId(Node sibling) {
    return getSiblingTextValue(sibling, "artifactId");
  }

  private String getVersion() {
    return getVersion(currentNode);
  }

  private String getVersion(Node sibling) {
    return getSiblingTextValue(sibling, "version");
  }

  /**
   * Returns true if user is editing plugin dependency.
   */
  private boolean isPlugin() {
    return "plugin".equals(currentNode.getParentNode().getNodeName());
  }

  /**
   * Returns true if user is editing plugin dependency exclusion.
   */
  private boolean isExclusion() {
    return "exclusion".equals(currentNode.getParentNode().getNodeName());
  }

  /**
   * Returns true if user is editing parent dependency.
   */
  protected boolean isParent() {
    return "parent".equals(currentNode.getParentNode().getNodeName());
  }

  private String getSiblingTextValue(Node sibling, String name) {
    Node node = getSiblingWithName(sibling, name);
    return getNodeTextValue(node);
  }

  /**
   * Returns sibling with given name.
   */
  private Node getSiblingWithName(Node node, String name) {
    NodeList nodeList = node.getParentNode().getChildNodes();
    for(int i = 0; i < nodeList.getLength(); i++ ) {
      if(name.equals(nodeList.item(i).getNodeName())) {
        return nodeList.item(i);
      }
    }
    return null;
  }

  /**
   * Returns text value of the node.
   */
  private String getNodeTextValue(Node node) {
    if(node != null && hasOneNode(node.getChildNodes())) {
      return ((Text) node.getChildNodes().item(0)).getData().trim();
    }
    return null;
  }

  /**
   * Returns true if there is only one node in the nodeList.
   */
  private boolean hasOneNode(NodeList nodeList) {
    return nodeList != null && nodeList.getLength() == 1;
  }

  /**
   * Copy from the {@link TemplateCompletionProcessor}. We need to store prefix and do not want to ignore dots.
   */
  protected String extractPrefix(ITextViewer viewer, int offset) {
    int i = offset;
    IDocument document = viewer.getDocument();
    if(i > document.getLength()) {
      return ""; //$NON-NLS-1$
    }

    try {
      while(i > 0) {
        char ch = document.getChar(i - 1);
        if(!Character.isJavaIdentifierPart(ch) && ch != '.' && ch != '-') {
          break;
        }
        i-- ;
      }
      prefix = document.get(i, offset - i);
      return prefix;
    } catch(BadLocationException e) {
      return ""; //$NON-NLS-1$
    }
  }

  private TemplateStore getTemplateStore() {
    return MvnIndexPlugin.getDefault().getTemplateStore();
  }

  private ContextTypeRegistry getTemplateContextRegistry() {
    return MvnIndexPlugin.getDefault().getTemplateContextRegistry();
  }

}
