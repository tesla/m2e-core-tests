/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.composites;

import java.util.Collection;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.maven.ide.components.pom.Dependency;
import org.maven.ide.components.pom.Exclusion;
import org.maven.ide.components.pom.Extension;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.editor.pom.MavenPomEditor;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;


/**
 * Label provider for Dependency, Exclusion and Extension elements
 * 
 * @author Eugene Kuleshov
 */
public class DependencyLabelProvider extends LabelProvider implements IColorProvider {

  private MavenPomEditor pomEditor;

  private boolean showGroupId = true;

  public void setPomEditor(MavenPomEditor pomEditor) {
    this.pomEditor = pomEditor;
  }
  
  public void setShowGroupId(boolean showGroupId) {
    this.showGroupId = showGroupId;
  }

  // IColorProvider
  
  public Color getForeground(Object element) {
    if(element instanceof Dependency) {
      Dependency dependency = (Dependency) element;
      String scope = dependency.getScope();
      if(scope != null && !"compile".equals(scope)) {
        return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
      }
    }
    return null;
  }
  
  public Color getBackground(Object element) {
    return null;
  }

  // LabelProvider
  
  @Override
  public String getText(Object element) {
    if(element instanceof Dependency) {
      Dependency dependency = (Dependency) element;
      return getText(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), //
          dependency.getClassifier(), dependency.getType(), dependency.getScope());
    } else if(element instanceof Exclusion) {
      Exclusion exclusion = (Exclusion) element;
      return getText(exclusion.getGroupId(), exclusion.getArtifactId(), null, null, null, null);
    } else if(element instanceof Extension) {
      Extension extension = (Extension) element;
      return getText(extension.getGroupId(), extension.getArtifactId(), extension.getVersion(), null, null, null);
    }
    return super.getText(element);
  }

  @Override
  public Image getImage(Object element) {
    if(element instanceof Dependency) {
      Dependency dependency = (Dependency) element;
      return getImage(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    } else if(element instanceof Exclusion) {
      Exclusion exclusion = (Exclusion) element;
      return getImage(exclusion.getGroupId(), exclusion.getArtifactId(), null);
    } else if(element instanceof Extension) {
      Extension extension = (Extension) element;
      return getImage(extension.getGroupId(), extension.getArtifactId(), extension.getVersion());
    }

    return null;
  }

  private Image getImage(String groupId, String artifactId, String version) {
    // XXX need to resolve actual dependencies (i.e. inheritance, dependency management or properties)
    // XXX need to handle version ranges
    
    if((version == null || version.indexOf("${") > -1) && pomEditor != null) {
      try {
        MavenProject mavenProject = pomEditor.readMavenProject(false, null);
        if(mavenProject != null) {
          Artifact artifact = (Artifact) mavenProject.getArtifactMap().get(groupId + ":" + artifactId);
          if(artifact!=null) {
            version = artifact.getVersion();
          }
          if(version==null || version.indexOf("${") > -1) {
            @SuppressWarnings("unchecked")
            Collection<Artifact> artifacts = mavenProject.getManagedVersionMap().values();
            for(Artifact a : artifacts) {
              if(a.getGroupId().equals(groupId) && a.getArtifactId().equals(artifactId)) {
                version = a.getVersion();
                break;
              }
            }
          }
        }
      } catch(MavenEmbedderException ex) {
        MavenLogger.log("Error reading Maven project", ex);
      } catch(CoreException ex) {
        MavenLogger.log(ex);
      }
    }
    
    if(groupId != null && artifactId != null && version != null) {
      MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
      IMavenProjectFacade projectFacade = projectManager.getMavenProject(groupId, artifactId, version);
      if(projectFacade != null) {
        return MavenEditorImages.IMG_PROJECT;
      }
    } 
    return MavenEditorImages.IMG_JAR;
  }

  private String getText(String groupId, String artifactId, String version, String classifier, String type, String scope) {
    StringBuilder sb = new StringBuilder();

    if(showGroupId) {
      sb.append(isEmpty(groupId) ? "?" : groupId).append(" : ");
    }
    sb.append(isEmpty(artifactId) ? "?" : artifactId);

    if(!isEmpty(version)) {
      sb.append(" : ").append(version);
    }

    if(!isEmpty(classifier)) {
      sb.append(" : ").append(classifier);
    }

    if(!isEmpty(type)) {
      sb.append(" : ").append(type);
    }

    if(!isEmpty(scope)) {
      sb.append(" [").append(scope).append(']');
    }

    return sb.toString();
  }

  private boolean isEmpty(String s) {
    return s == null || s.trim().length() == 0;
  }

}
