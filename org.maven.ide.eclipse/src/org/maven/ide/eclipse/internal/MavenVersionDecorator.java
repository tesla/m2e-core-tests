/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;

import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.IMavenProjectChangedListener;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;

/**
 * @author Eugene Kuleshov
 */
public class MavenVersionDecorator implements ILabelDecorator {

  private Map listeners = new HashMap();

  public Image decorateImage(Image image, Object element) {
    return null;
  }

  public String decorateText(String text, Object element) {
    if(element instanceof IResource) {
      IProject project = ((IResource) element).getProject();
      if(project!=null) {
        MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
        MavenProjectFacade facade = projectManager.create(project, new NullProgressMonitor());
        if(facade!=null) {
          MavenProject mavenProject = facade.getMavenProject();
          if(mavenProject!=null) {
            int n = text.indexOf(' ');
            if(n==-1) {
              return text + "  " + mavenProject.getVersion();
            }
            return text.substring(0, n) + "  " + mavenProject.getVersion() + text.substring(n);
          }
        }
      }
    }
    return null;
  }

  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  public void addListener(final ILabelProviderListener listener) {
    IMavenProjectChangedListener projectChangeListener = new IMavenProjectChangedListener() {
      public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
        ArrayList pomList = new ArrayList();
        for(int i = 0; i < events.length; i++ ) {
          // pomList.add(events[i].getSource());
          if(events[i]!=null && events[i].getMavenProject()!=null) {
            IFile pom = events[i].getMavenProject().getPom();
            pomList.add(pom);
            if(pom.getParent().getType()==IResource.PROJECT) {
              pomList.add(pom.getParent());
            }
          }
        }
        listener.labelProviderChanged(new LabelProviderChangedEvent(MavenVersionDecorator.this, pomList.toArray()));
      }
    };
    
    listeners .put(listener, projectChangeListener);
    
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    projectManager.addMavenProjectChangedListener(projectChangeListener);
  }
  
  public void removeListener(ILabelProviderListener listener) {
    IMavenProjectChangedListener projectChangeListener = (IMavenProjectChangedListener) listeners.get(listener);
    if(projectChangeListener!=null) {
      MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
      projectManager.removeMavenProjectChangedListener(projectChangeListener);
    }
  }

  public void dispose() {
    // TODO remove all listeners
  }
  
}
