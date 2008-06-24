/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.xml.ui.views.contentoutline.XMLContentOutlineConfiguration;

/**
 * @author Eugene Kuleshov
 */
public class PomContentOutlineConfiguration extends XMLContentOutlineConfiguration {

  public ILabelProvider getLabelProvider(TreeViewer viewer) {
    return new PomLabelProvider(super.getLabelProvider(viewer));
  }

  /**
   * POM label provider
   */
  private final class PomLabelProvider implements ILabelProvider {
    
    private static final int MAX_LABEL_LENGTH = 120;
    
    private final ILabelProvider labelProvider;
  
    private PomLabelProvider(ILabelProvider labelProvider) {
      this.labelProvider = labelProvider;
    }
  
    public Image getImage(Object element) {
      return labelProvider.getImage(element);
    }
  
    public String getText(Object element) {
      String text = labelProvider.getText(element);
  
      Node node = (Node) element;
      String namespace = node.getNamespaceURI();
      String nodeName = node.getNodeName();
      
      if(node.getNodeType()==Node.COMMENT_NODE) {
        return cleanText(node);
      }
      
      if("http://maven.apache.org/POM/4.0.0".equals(namespace)) {
        if("parent".equals(nodeName)) {
          return getLabel(text, node, "groupId", "artifactId", "version");
        
        } else if("dependency".equals(nodeName)) {
          return getLabel(text, node, "groupId", "artifactId", "version", "classifier", "type", "scope");
        
        } else if("exclusion".equals(nodeName)) {
          return getLabel(text, node, "groupId", "artifactId");
        
        } else if("repository".equals(nodeName) || "pluginRepository".equals(nodeName)
            || "snapshotRepository".equals(nodeName) || "site".equals(nodeName) || "profile".equals(nodeName)
            || "execution".equals(nodeName)) {
          return getLabel(text, node, "id");
          
        } else if("mailingList".equals(nodeName)) {
          return getLabel(text, node, "name");
          
        } else if("developer".equals(nodeName)) {
          return getLabel(text, node, "id", "name", "email");
          
        } else if("contributor".equals(nodeName)) {
          return getLabel(text, node, "name", "email");
          
        } else if("plugin".equals(nodeName)) {
          return getLabel(text, node, "groupId", "artifactId", "version");
        
        }
        
        NodeList childNodes = node.getChildNodes();
        if(childNodes.getLength()==1) {
          Node item = childNodes.item(0);
          short nodeType = item.getNodeType();
          if(nodeType==Node.TEXT_NODE || nodeType==Node.COMMENT_NODE) {
            String nodeText = item.getNodeValue();
            if(nodeText.length()>0) {
              return text + "  " + cleanText(item);
            }
          }
        }
      }
      
      return text;
    }
  
    public boolean isLabelProperty(Object element, String name) {
      return labelProvider.isLabelProperty(element, name);
    }
  
    public void addListener(ILabelProviderListener listener) {
      labelProvider.addListener(listener);
    }
  
    public void removeListener(ILabelProviderListener listener) {
      labelProvider.removeListener(listener);
    }
  
    public void dispose() {
      labelProvider.dispose();
    }
  
    private String getLabel(String text, Node node, String... names) {
      StringBuilder sb = new StringBuilder(text).append("  ");
      String sep = "";
      for(String name : names) {
        String value = getValue(node, name);
        if(value!=null) {
          sb.append(sep).append(value);
          sep = " : ";
        }
      }
      
      return sb.toString();
    }
  
    private String getValue(Node node, String name) {
      NodeList childNodes = node.getChildNodes();
      for(int i = 0; i < childNodes.getLength(); i++ ) {
        Node item = childNodes.item(i);
        if(item.getNodeType()==Node.ELEMENT_NODE && name.equals(item.getNodeName())) {
          NodeList nodes = item.getChildNodes();
          if(nodes.getLength()==1) {
            String value = nodes.item(0).getNodeValue().trim();
            if(value.length()>0) {
              return value;
            }
          }
          return null;
        }
      }
      return null;
    }
  
    private String cleanText(Node node) {
      String value = node.getNodeValue();
      if(value==null) {
        return "";
      }
      
      value = value.replaceAll("\\s", " ").replaceAll("(\\s){2,}", " ").trim();
      if(value.length()>MAX_LABEL_LENGTH) {
        value = value.substring(0, 120) + "...";
      }
      
      return value;
    }
  }

}

