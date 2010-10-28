/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.editor.pom;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.zest.core.viewers.EntityConnectionData;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.IConnectionStyleProvider;
import org.eclipse.zest.core.viewers.IEntityStyleProvider;
import org.eclipse.zest.core.widgets.ZestStyles;


/**
 * Dependency graph label provider
 * 
 * @author Eugene Kuleshov
 */
public class DependencyGraphLabelProvider implements ILabelProvider, IEntityStyleProvider, IConnectionStyleProvider,
    ISelectionChangedListener {
  private static final String SCOPE_TEST = "test";

  private static final String SCOPE_COMPILE = "compile";

  private final GraphViewer viewer;

  private final DependencyGraphContentProvider contentProvider;

  private Set<DependencyNode> selectedConnections;

  // private final Color colorTestBackground = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
  // private final Color colorTestBackground = new Color(null, 213, 243, 255);
  private final Color colorTestBackground = new Color(null, 255, 255, 255);
  private final Color colorTestRel = new Color(null, 216, 228, 248);
  private final Color colorRel = new Color(null, 150, 150, 255);
  private final Color colorRelResolved = new Color(null, 255, 100, 100);
  // private final Color colorTestHighlight = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_YELLOW);
  // private final Color colorTestHighlight = new Color(null, 255, 255, 206);
  private final Color colorTestHighlight = new Color(null, 255, 255, 180);
  private final Color colorRelated = new Color(Display.getDefault(), 127, 0, 0);
  
  private boolean showGroup = false;
  private boolean showVersion = true;
  private boolean showScope = true;
  private boolean wrapLabel = true;

  private boolean showIcon = true;

  
  public DependencyGraphLabelProvider(GraphViewer viewer, DependencyGraphContentProvider contentProvider) {
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
  
  public void setShowIcon(boolean showIcon) {
    this.showIcon = showIcon;
    viewer.refresh(true);
    viewer.getGraphControl().applyLayout();
  }

  public void setWarpLabel(boolean wrapLabel) {
    this.wrapLabel = wrapLabel;
    viewer.refresh(true);
    viewer.getGraphControl().applyLayout();
  }

  // ISelectionChangedListener
  public void selectionChanged(SelectionChangedEvent event) {
    if(selectedConnections != null) {
      for(DependencyNode node : selectedConnections) {
        viewer.unReveal(node);
      }
      selectedConnections = null;
    }

    ISelection selection = event.getSelection();
    if(!selection.isEmpty() && selection instanceof IStructuredSelection) {
      selectedConnections = new HashSet<DependencyNode>();
      for(Object o : ((IStructuredSelection) selection).toList()) {
        if(o instanceof Artifact) {
          Artifact a = (Artifact) o;
          for(DependencyNode node : contentProvider.getConnections(a)) {
            // EntityConnectionData data = new EntityConnectionData(a, node.getArtifact());
            viewer.reveal(node);
            selectedConnections.add(node);
          }
        }
      }
    }

    Object[] connections = viewer.getConnectionElements();
    for(int i = 0; i < connections.length; i++ ) {
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
    colorRelResolved.dispose();
    colorRelated.dispose();
  }

  public Image getImage(Object element) {
    if(showIcon && element instanceof Artifact) {
      Artifact artifact = (Artifact) element;
      MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
      IMavenProjectFacade projectFacade = projectManager.getMavenProject(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
      return projectFacade == null ? MavenEditorImages.IMG_JAR : MavenEditorImages.IMG_PROJECT;
    }
    return null;
  }

  public String getText(Object element) {
    if(element instanceof Artifact) {
      Artifact a = (Artifact) element;

      String label = "";

      if(showGroup) {
        label += a.getGroupId() + (wrapLabel ? "\n" : " : ");
      }

      label += a.getArtifactId();
      if(a.getClassifier() != null) {
        label += " : " + a.getClassifier();
      }

      if(showVersion || showScope) {
        label += wrapLabel ? "\n" : " : ";

        if(showVersion) {
          label += a.getVersion();
        }

        if(showScope) {
          label += (showVersion ? " [" : "[") + (a.getScope()==null ? SCOPE_COMPILE : a.getScope()) + "]";
        }
      }

      return label;
    } else if(element instanceof DependencyNode) {
      return null;
    } else if(element instanceof EntityConnectionData) {
      return null;
    }
    return element.toString();
  }

  // IEntityStyleProvider

  public IFigure getTooltip(Object entity) {
    if(entity instanceof Artifact) {
      Artifact a = (Artifact) entity;
      String label = " " + a.getGroupId() + " \n" // 
          + " " + a.getArtifactId() + " \n" //
          + " " + a.getVersion() + " [" + a.getScope() + "] ";
      if(a.getClassifier() != null) {
        label += "\n " + a.getClassifier() + " ";
      }
      return new Label(label);
    } else if(entity instanceof DependencyNode) {
      DependencyNode node = (DependencyNode) entity;
      
      Artifact a = node.getState() == DependencyNode.OMITTED_FOR_CONFLICT ? node.getRelatedArtifact() //
          : node.getArtifact();

      String label = " " + a.getGroupId() + " \n" //
          + " " + a.getArtifactId() + " \n";

      if(a.getVersion() != null || a.getBaseVersion() != null) {
        label += " " + (a.getBaseVersion() == null ? a.getVersion() : a.getBaseVersion());
      } else {
        label += " " + a.getVersionRange().toString();
      }
      
      switch(node.getState()) {
        case DependencyNode.INCLUDED:
        case DependencyNode.OMITTED_FOR_CYCLE:
        case DependencyNode.OMITTED_FOR_DUPLICATE:
          if(node.getPremanagedVersion() != null) {
            label += " (from " + node.getPremanagedVersion() + ")";
          }
          if(node.getVersionSelectedFromRange() != null) {
            label += " (from " + node.getVersionSelectedFromRange().toString() //
            + " available " + node.getAvailableVersions().toString() + ")";
          }
          break;

        case DependencyNode.OMITTED_FOR_CONFLICT:
          label += " (conflicted " + node.getArtifact().getVersion() + ")";
          break;
      }
      
      label += " \n";

      if(a.getClassifier() != null) {
        label += " " + a.getClassifier() + " \n";
      }

      label += " [" + (a.getScope() == null ? SCOPE_COMPILE : a.getScope()) + "]";

      if(node.getPremanagedScope() != null) {
        label += " (from " + node.getPremanagedScope() + ")";
      }

      if(node.getOriginalScope() != null) {
        label += " (from " + node.getOriginalScope() + ")";
      }

      if(node.getFailedUpdateScope() != null) {
        label += " (not updated from " + node.getFailedUpdateScope() + ")";
      }
      
      label += " \n";
      
      return new Label(label); 
    }
    
    return null;
  }

  public boolean fisheyeNode(Object entity) {
    return false;
  }

  public int getBorderWidth(Object entity) {
    return 1;
  }

  public Color getBorderColor(Object entity) {
    return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
  }

  public Color getBorderHighlightColor(Object entity) {
    return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
  }

  public Color getForegroundColour(Object entity) {
    return null;
  }

  public Color getBackgroundColour(Object entity) {
    if(entity instanceof DependencyNode) {
      DependencyNode node = (DependencyNode) entity;
      Artifact a = node.getArtifact();
      String scope = a.getScope();
      // if ("test".equals(scope) || "runtime".equals(scope) ||
      // "provided".equals(scope) || "system".equals(scope)) {
      if(scope==null || SCOPE_COMPILE.equals(scope)) {
        return null;
      } else {
        return colorTestBackground;
      }
    } else if(entity instanceof Artifact) {
      Artifact a = (Artifact) entity;
      String scope = a.getScope();
      // if ("test".equals(scope) || "runtime".equals(scope) ||
      // "provided".equals(scope) || "system".equals(scope)) {
      if(scope==null || SCOPE_COMPILE.equals(scope)) {
        return null;
      } else {
        return colorTestBackground;
      }
    }
    return null;
  }

  public Color getNodeHighlightColor(Object entity) {
    if(entity instanceof DependencyNode) {
      DependencyNode node = (DependencyNode) entity;
      Artifact a = node.getArtifact();
      String scope = a.getScope();
      if(scope==null || SCOPE_COMPILE.equals(scope)) {
        return null;
      } else {
        return colorTestHighlight;
      }
    } else if(entity instanceof Artifact) {
      Artifact a = (Artifact) entity;
      String scope = a.getScope();
      if(scope==null || SCOPE_COMPILE.equals(scope)) {
        return null;
      } else {
        return colorTestHighlight;
      }
    }
    return null;
  }

  // IConnectionStyleProvider

  public int getLineWidth(Object rel) {
    if(rel instanceof DependencyNode) {
      if(selectedConnections != null && selectedConnections.contains(rel)) {
        return 1;
      }

    } else if(rel instanceof EntityConnectionData) {
      if(selectedConnections != null && selectedConnections.contains(rel)) {
        return 1;
      }
    }
    return 0;
  }

  public Color getHighlightColor(Object rel) {
    return colorRelated;
  }

  public Color getColor(Object rel) {
    if(rel instanceof DependencyNode) {
      DependencyNode node = (DependencyNode) rel;

      if(node.getState() == DependencyNode.OMITTED_FOR_CONFLICT || node.getState() == DependencyNode.OMITTED_FOR_CYCLE) {
        return colorRelResolved;
      }
      
      Artifact md = node.getArtifact();
      String scope = md.getScope();
      if(SCOPE_TEST.equals(scope)) {
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
    if(rel instanceof DependencyNode) {
      DependencyNode node = (DependencyNode) rel;
      if(SCOPE_TEST.equals(node.getArtifact().getScope())) {
        return ZestStyles.CONNECTIONS_DOT;
      }
    } else if(rel instanceof EntityConnectionData) {
      EntityConnectionData data = (EntityConnectionData) rel;

      Object dest = data.dest;
      if(dest instanceof DependencyNode) {
        DependencyNode node = (DependencyNode) dest;
        if(SCOPE_TEST.equals(node.getArtifact().getScope())) {
          return ZestStyles.CONNECTIONS_DOT | ZestStyles.CONNECTIONS_DIRECTED;
        }
      }

      Object source = data.source;
      if(source instanceof DependencyNode) {
        DependencyNode node = (DependencyNode) source;
        if(SCOPE_TEST.equals(node.getArtifact().getScope())) {
          return ZestStyles.CONNECTIONS_DOT | ZestStyles.CONNECTIONS_DIRECTED;
        }
      }

    }
    return ZestStyles.CONNECTIONS_SOLID | ZestStyles.CONNECTIONS_DIRECTED;
  }

}
