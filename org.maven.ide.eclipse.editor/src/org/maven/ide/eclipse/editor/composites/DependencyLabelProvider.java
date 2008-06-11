/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.composites;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.maven.ide.components.pom.Dependency;
import org.maven.ide.components.pom.Exclusion;
import org.maven.ide.components.pom.Extension;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;


/**
 * Label provider for Dependency, Exclusion and Extension elements
 * 
 * @author Eugene Kuleshov
 */
public class DependencyLabelProvider extends LabelProvider {

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
    if(groupId != null && artifactId != null && version != null) {
      MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
      MavenProjectFacade projectFacade = projectManager.getMavenProject(groupId, artifactId, version);
      if(projectFacade != null) {
        return MavenEditorImages.IMG_PROJECT;
      }
    }
    return MavenEditorImages.IMG_JAR;
  }

  private String getText(String groupId, String artifactId, String version, String classifier, String type, String scope) {
    StringBuilder sb = new StringBuilder();

    sb.append(isEmpty(groupId) ? "[unknown]" : groupId);
    sb.append(" : ").append(isEmpty(artifactId) ? "[unknown]" : artifactId);

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
