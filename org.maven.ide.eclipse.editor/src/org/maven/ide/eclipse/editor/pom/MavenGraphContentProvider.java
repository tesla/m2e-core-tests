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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.repository.metadata.MetadataGraph;
import org.apache.maven.repository.metadata.MetadataGraphEdge;
import org.apache.maven.repository.metadata.MetadataGraphVertex;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.zest.core.viewers.IGraphEntityContentProvider;


/**
 * Graph Content provider
 */
public class MavenGraphContentProvider implements IGraphEntityContentProvider {
  // IGraphContentProvider {
  // IGraphEntityRelationshipContentProvider {

  private MetadataGraph graph;

  private Map<MetadataGraphVertex, Set<MetadataGraphVertex>> connections;

  public MetadataGraph getGraph() {
    return graph;
  }

  // IContentProvider

  public void dispose() {
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    this.graph = (MetadataGraph) newInput;
    this.connections = new HashMap<MetadataGraphVertex, Set<MetadataGraphVertex>>();
  }

  // IStructuredContentProvider

  // should return all edges
  public Object[] getElements(Object inputElement) {
    if(inputElement instanceof MetadataGraph) {
      MetadataGraph graph = (MetadataGraph) inputElement;

      // MetadataGraphVertex entry = graph.getEntry();
      // init(graph, entry);

      TreeSet<MetadataGraphVertex> vertices = graph.getVertices();
      for(MetadataGraphVertex v1 : vertices) {
//          init(graph, v1);

        for(MetadataGraphVertex v2 : vertices) {
          if(v1 != v2) {
            List<MetadataGraphEdge> edges = graph.getEdgesBetween(v1, v2);
            if(edges != null && !edges.isEmpty()) {
//                if (edges.size() > 1) {
//                  // why do we have more then one edge?
//                  System.err.println(v1 + " : " + v2 + " : " + edges);
//                }

              Set<MetadataGraphVertex> dests = connections.get(v1);
              if(dests == null) {
                dests = new HashSet<MetadataGraphVertex>();
                connections.put(v1, dests);
              }
              dests.add(v2);
            }
          }
        }
      }

      return vertices.toArray(new MetadataGraphVertex[vertices.size()]);
    }

    return DependencyGraphPage.EMPTY;
  }

//    private void init(MetadataGraph graph, MetadataGraphVertex v) {
//      List<MetadataGraphEdge> incidentEdges = graph.getIncidentEdges(v);
//      if(incidentEdges!=null && !incidentEdges.isEmpty()) {
//        Set<MetadataGraphVertex> sourceNodes = sources.get(v);
//        if(sourceNodes==null) {
//          sourceNodes = new HashSet<MetadataGraphVertex>();
//          sources.put(v, sourceNodes);
//        }
//        for(Iterator<MetadataGraphEdge> iterator = incidentEdges.iterator(); iterator.hasNext();) {
//          MetadataGraphEdge edge = iterator.next();
//          sourceNodes.add(edge.getSource());
//        }
//      }
//
//      List<MetadataGraphEdge> excidentEdges = graph.getExcidentEdges(v);
//      if(excidentEdges!=null && !excidentEdges.isEmpty()) {
//        Set<MetadataGraphVertex> targetNodes = targets.get(v);
//        if(targetNodes==null) {
//          targetNodes = new HashSet<MetadataGraphVertex>();
//          targets.put(v, targetNodes);
//        }
//        for(Iterator<MetadataGraphEdge> iterator = excidentEdges.iterator(); iterator.hasNext();) {
//          MetadataGraphEdge edge = iterator.next();
//          targetNodes.add(edge.getTarget());
//        }
//      }
//    }

  // IGraphEntityContentProvider

  public Object[] getConnectedTo(Object element) {
    if(element instanceof MetadataGraphVertex) {
//        HashSet<MetadataGraphVertex> v = new HashSet<MetadataGraphVertex>();
//        Set<MetadataGraphVertex> t = targets.get(element);
//        if (t != null) {
//          v.addAll(t);
//        }
//        return v.toArray(new MetadataGraphVertex[v.size()]);
      Set<MetadataGraphVertex> nodes = connections.get(element);
      if(nodes != null) {
        return nodes.toArray(new MetadataGraphVertex[nodes.size()]);
      }
    }
    return DependencyGraphPage.EMPTY;
  }

  // IGraphContentProvider

  // public Object getDestination(Object rel) {
  //   if(rel instanceof MetadataGraphEdge) {
  //     return destinations.get(rel);
  //   }
  //   return null;
  // }
  //  
  // public Object getSource(Object rel) {
  //   if(rel instanceof MetadataGraphVertex) {
  //     return sources.get(rel);
  //   }
  //   return null;
  // }

  // IGraphEntityRelationshipContentProvider

  // public Object[] getRelationships(Object source, Object dest) {
  //   return null;
  // }

  // ISelectionChangedListener

}
