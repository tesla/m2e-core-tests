/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.zest.core.viewers.IGraphContentProvider;

/**
 * Dependency graph content provider
 * 
 * @author Eugene Kuleshov
 */
public class DependencyGraphContentProvider implements IGraphContentProvider {
  // IGraphEntityContentProvider {
  // IGraphEntityRelationshipContentProvider {

  private Set<DependencyNode> nodes;
  private HashMap<Artifact, Set<DependencyNode>> artifacts;
  
  private boolean showResolved = true;

  // IContentProvider

  public void dispose() {
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
  }
  
  public Set<DependencyNode> getConnections(Artifact artifact) {
    return artifacts.get(artifact);
  }
  
  public Set<DependencyNode> getNodes() {
    return nodes;
  }

  public void setShowResolved(boolean showResolved) {
    this.showResolved = showResolved;
  }
  
  // IStructuredContentProvider

  // should return all edges
  public Object[] getElements(Object inputElement) {
    if(inputElement instanceof DependencyNode) {
      nodes = new HashSet<DependencyNode>();
      artifacts = new HashMap<Artifact, Set<DependencyNode>>();

      DependencyNode node = (DependencyNode) inputElement;
      // nodes.add(node);
      node.accept(new DependencyNodeVisitor() {
        public boolean visit(DependencyNode node) {
          nodes.add(node);

          boolean isOmitted = showResolved && node.getState()==DependencyNode.OMITTED_FOR_CONFLICT;
          // boolean isOmitted = false;
          
          Artifact artifact;
          if(isOmitted) {
            artifact = node.getRelatedArtifact();
          } else {
            artifact = node.getArtifact();
          }
          
          Set<DependencyNode> connections = (Set<DependencyNode>) artifacts.get(artifact);
          if(connections==null) {
            connections = new HashSet<DependencyNode>();
            artifacts.put(artifact, connections);
          }
          
          // if(!isOmitted) {
            connections.add(node);
          // }
          
          connections.addAll(getChildren(node));
          
          return true;
        }

        public boolean endVisit(DependencyNode node) {
          return true;
        }
      });
      
      
      return nodes.toArray(new DependencyNode[nodes.size()]);
    }
    return DependencyGraphPage.EMPTY;
  }

  // IGraphEntityContentProvider
  
  public Object[] getConnectedTo(Object entity) {
    if(entity instanceof DependencyNode) {
      DependencyNode node = (DependencyNode) entity;
      List<DependencyNode> children = getChildren(node);
      return children.toArray(new DependencyNode[children.size()]);
    }
    return DependencyGraphPage.EMPTY;
  }
  
  
  // IGraphContentProvider
  
  public Object getSource(Object rel) {
    if(rel instanceof DependencyNode) {
      DependencyNode parent = ((DependencyNode) rel).getParent();
      if(parent==null) {
        return null;
      } else {
        if(showResolved && parent.getState()==DependencyNode.OMITTED_FOR_CONFLICT) {
          return parent.getRelatedArtifact();
        } else {
          return parent.getArtifact();
        }
      }
    }
    return null;
  }
  
  public Object getDestination(Object rel) {
    if(rel instanceof DependencyNode) {
      DependencyNode node = (DependencyNode) rel;
      if(showResolved && node.getState()==DependencyNode.OMITTED_FOR_CONFLICT) {
        return node.getRelatedArtifact();
      } else {
        return node.getArtifact();
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private List<DependencyNode> getChildren(DependencyNode node) {
    return node.getChildren();
  }
  
}