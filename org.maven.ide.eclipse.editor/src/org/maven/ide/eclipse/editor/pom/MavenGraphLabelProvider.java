/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.ArtifactScopeEnum;
import org.apache.maven.artifact.resolver.metadata.ArtifactMetadata;
import org.apache.maven.artifact.resolver.metadata.MetadataGraphEdge;
import org.apache.maven.artifact.resolver.metadata.MetadataGraphVertex;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.zest.core.viewers.EntityConnectionData;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.IConnectionStyleProvider;
import org.eclipse.zest.core.viewers.IEntityStyleProvider;
import org.eclipse.zest.core.widgets.ZestStyles;

public class MavenGraphLabelProvider implements ILabelProvider,
      IEntityStyleProvider, IConnectionStyleProvider, ISelectionChangedListener {
    private final GraphViewer viewer;
    private final MavenGraphContentProvider contentProvider;

    private Set selectedConnections;
    
    // private final Color colorTestBackground =
    // Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
    // private final Color colorTestBackground = new Color(null, 213, 243, 255);

    private final Color colorTestBackground = new Color(null, 255, 255, 255);
    private final Color colorTestRel = new Color(null, 216, 228, 248);
    private final Color colorRel = new Color(null, 150, 150, 255);

    // private final Color colorTestHighlight =
    // Display.getCurrent().getSystemColor(SWT.COLOR_DARK_YELLOW);
    // private final Color colorTestHighlight = new Color(null, 255, 255, 206);
    private final Color colorTestHighlight = new Color(null, 255, 255, 180);
    private final Color colorBorder = new Color(null, 0, 0, 0);

    private final Color colorRelated = new Color(Display.getDefault(), 127, 0, 0);
    
    private boolean showGroup = false;
    private boolean showVersion = true;
    private boolean showScope = true;
    private boolean wrapLabel = true;

    public MavenGraphLabelProvider(GraphViewer viewer, MavenGraphContentProvider contentProvider) {
      this.viewer = viewer;
      this.contentProvider = contentProvider;
    }

    public void setShowGroup(boolean showGroup) {
      this.showGroup = showGroup;
      viewer.refresh(true);
      viewer.getGraphControl().applyLayout();
    }

    public void setShowVersion(boolean showVersion) {
      this.showVersion = showVersion;
      viewer.refresh(true);
      viewer.getGraphControl().applyLayout();
    }

    public void setShowScope(boolean showScope) {
      this.showScope = showScope;
      viewer.refresh(true);
      viewer.getGraphControl().applyLayout();
    }

    public void setWarpLabel(boolean wrapLabel) {
      this.wrapLabel = wrapLabel;
      viewer.refresh(true);
      viewer.getGraphControl().applyLayout();
    }

//    public void setCurrentSelection(Object currentSelection) {
//      this.currentSelection = currentSelection;
//
//      interestingRelationships = new HashSet();
//      interestingDependencies = new HashSet();
//      if (this.currentSelection != null) {
//        calculateInterestingDependencies(interestingRelationships,
//            interestingDependencies);
//      }
//
//      Object[] connections = viewer.getConnectionElements();
//      for (Iterator iter = interestingRelationships.iterator(); iter.hasNext();) {
//        Object entityConnectionData = iter.next();
//        viewer.reveal(entityConnectionData);
//      }
//
//      for (int i = 0; i < connections.length; i++) {
//        viewer.update(connections[i], null);
//      }
//    }

//    private void calculateInterestingDependencies(Set interestingRelationships,
//        Set interestingDependencies) {
//
//    }
    
    // ISelectionChangedListener
    
    public void selectionChanged(SelectionChangedEvent event) {
      if(selectedConnections!=null) {
        for(Iterator it = selectedConnections.iterator(); it.hasNext();) {
          EntityConnectionData data = (EntityConnectionData) it.next();
          viewer.unReveal(data);
        }
        selectedConnections = null;
      }
      
      ISelection selection = event.getSelection();
      if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
        selectedConnections = new HashSet();
        List list = ((IStructuredSelection) selection).toList();
        for(Iterator it = list.iterator(); it.hasNext();) {
          Object o = it.next();
          if(o instanceof MetadataGraphVertex) {
            MetadataGraphVertex v = (MetadataGraphVertex) o;
            Object[] s = contentProvider.getConnectedTo(v);
            if(s!=null) {
              for(int i = 0; i < s.length; i++ ) {
                MetadataGraphVertex vertex = (MetadataGraphVertex) s[i];
                EntityConnectionData data1 = new EntityConnectionData(v, vertex);
                viewer.reveal(data1);
                selectedConnections.add(data1);
              }
            }
            
            List incidentEdges = contentProvider.getGraph().getIncidentEdges(v);
            if(incidentEdges!=null) {
              for(Iterator iterator = incidentEdges.iterator(); iterator.hasNext();) {
                MetadataGraphEdge edge = (MetadataGraphEdge) iterator.next();
                EntityConnectionData data = new EntityConnectionData(edge.getSource(), v);
                viewer.reveal(data);
                selectedConnections.add(data);
              }
            }
          }
        }
      }
      
      Object[] connections = viewer.getConnectionElements();
      for (int i = 0; i < connections.length; i++) {
        viewer.update(connections[i], null);
      }
    }

    
    // ILabelProvider

    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    public void addListener(ILabelProviderListener listener) {
    }

    public void removeListener(ILabelProviderListener listener) {
    }

    public void dispose() {
      colorTestBackground.dispose();
      colorTestHighlight.dispose();
      colorTestRel.dispose();
      colorRel.dispose();
      colorBorder.dispose();
      colorRelated.dispose();
    }

    public Image getImage(Object element) {
      return null;
    }

    public String getText(Object element) {
      if (element instanceof MetadataGraphVertex) {
        MetadataGraphVertex v = (MetadataGraphVertex) element;
        ArtifactMetadata md = v.getMd();

        String label = "";

        if (showGroup) {
          label += md.getGroupId() + (wrapLabel ? "\n" : " : ");
        }

        label += md.getArtifactId();
        if (md.getClassifier() != null) {
          label += " : " + md.getClassifier();
        }

        if (showVersion || showScope) {
          label += wrapLabel ? "\n" : " : ";

          if (showVersion) {
            label += md.getVersion();
          }

          if (showScope) {
            label += (showVersion ? " [" : "[") + md.getScope() + "]";
          }
        }

        return label;
      } else if (element instanceof MetadataGraphEdge) {
        return null;
      } else if (element instanceof EntityConnectionData) {
        return null;
        // } else if(element instanceof MetadataGraph) {
        // MetadataGraph graph = (MetadataGraph) element;
        // MetadataGraphVertex entry = graph.getEntry();
        // Map<String, List<MetadataGraphEdge>> edges = graph.getEdges();
        // Map<String, MetadataGraphVertex> vertices = graph.getVertices();
        // //
        // }
      }
      return element.toString();
    }

    // IEntityStyleProvider

    public IFigure getTooltip(Object entity) {
      if (entity instanceof MetadataGraphVertex) {
        MetadataGraphVertex v = (MetadataGraphVertex) entity;
        ArtifactMetadata md = v.getMd();
        // return md.getGroupId() + ":" + md.getArtifactId() + "-" +
        // md.getVersion() + "-" + md.getClassifier() + " [" + md.getScope() +
        // "]";
        String label = " " + md.getGroupId() + " \n" // 
            + " " + md.getArtifactId() + " \n" //
            + " " + md.getVersion() + " [" + md.getScope() + "] ";
        if (md.getClassifier() != null) {
          label += "\n " + md.getClassifier() + " ";
        }
        return new Label(label);
      }
      return null;
    }

    public boolean fisheyeNode(Object entity) {
      return false;
    }

    public int getBorderWidth(Object entity) {
      return 4;
    }

    public Color getBorderColor(Object entity) {
      return colorBorder;
    }

    public Color getBorderHighlightColor(Object entity) {
      return colorBorder;
    }

    public Color getForegroundColour(Object entity) {
      return null;
    }

    public Color getBackgroundColour(Object entity) {
      if (entity instanceof MetadataGraphVertex) {
        ArtifactMetadata md = ((MetadataGraphVertex) entity).getMd();
        String scope = md.getScope();
        // if ("test".equals(scope) || "runtime".equals(scope) ||
        // "provided".equals(scope) || "system".equals(scope)) {
        if ("compile".equals(scope)) {
          return null;
        } else {
          return colorTestBackground;
        }
      }
      return null;
    }

    public Color getNodeHighlightColor(Object entity) {
      if (entity instanceof MetadataGraphVertex) {
        ArtifactMetadata md = ((MetadataGraphVertex) entity).getMd();
        String scope = md.getScope();
        if ("compile".equals(scope)) {
          return null;
        } else {
          return colorTestHighlight;
        }
      }
      return null;
    }

    // IConnectionStyleProvider

    public int getLineWidth(Object rel) {
      if (rel instanceof EntityConnectionData) {
        if (selectedConnections!=null && selectedConnections.contains(rel)) {
          return 1;
        }
      }
      return 0;
    }

    public Color getHighlightColor(Object rel) {
      return colorRelated;
    }

    public Color getColor(Object rel) {
      if (rel instanceof MetadataGraphEdge) {
        ArtifactScopeEnum scope = ((MetadataGraphEdge) rel).getScope();
        if (scope == ArtifactScopeEnum.test) {
          return null;
        }
//      } else if (rel instanceof EntityConnectionData) {
//        EntityConnectionData data = (EntityConnectionData) rel;
//        Object dest = data.dest;
//        Object source = data.source;
//        if (selectedElements.contains(source)
//            || selectedElements.contains(dest)) {
//        // if(dest instanceof MetadataGraphVertex) {
//        //   ArtifactMetadata md = ((MetadataGraphVertex) dest).getMd();
//        //   if(md.getArtifactScope()==ArtifactScopeEnum.test) {
//        //     return colorTestRel;
//        //   }
//        // }
//          return viewer.getGraphControl().DARK_BLUE;
//        }
//        if (dest instanceof MetadataGraphVertex) {
//          ArtifactMetadata md = ((MetadataGraphVertex) dest).getMd();
//          if (md.getArtifactScope() == ArtifactScopeEnum.test) {
//            return null;
//          }
//        }
      }
      return colorRel;
    }

    public int getConnectionStyle(Object rel) {
      if (rel instanceof MetadataGraphEdge) {
        ArtifactScopeEnum scope = ((MetadataGraphEdge) rel).getScope();
        if (scope == ArtifactScopeEnum.test) {
          return ZestStyles.CONNECTIONS_DOT;
        }
      } else if (rel instanceof EntityConnectionData) {
        EntityConnectionData data = (EntityConnectionData) rel;

        Object dest = data.dest;
        if (dest instanceof MetadataGraphVertex) {
          ArtifactMetadata md = ((MetadataGraphVertex) dest).getMd();
          if (md.getArtifactScope() == ArtifactScopeEnum.test) {
            return ZestStyles.CONNECTIONS_DOT | ZestStyles.CONNECTIONS_DIRECTED;
          }
        }
        
        Object source = data.source;
        if (source instanceof MetadataGraphVertex) {
          ArtifactMetadata md = ((MetadataGraphVertex) source).getMd();
          if (md.getArtifactScope() == ArtifactScopeEnum.test) {
            return ZestStyles.CONNECTIONS_DOT | ZestStyles.CONNECTIONS_DIRECTED;
          }
        }
        
      }
      return ZestStyles.CONNECTIONS_SOLID | ZestStyles.CONNECTIONS_DIRECTED;
    }

  }