/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;


/**
 * Adapts IProject to ArtifactKey
 * 
 * @author Igor Fedorenko
 */
@SuppressWarnings("unchecked")
public class ProjectAdaptor implements IAdapterFactory {

  private static final Class[] ADAPTER_LIST = new Class[] {ArtifactKey.class,};

  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if(ArtifactKey.class.equals(adapterType)) {
      IProject project = (IProject) adaptableObject;
      MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
      IMavenProjectFacade projectFacade = projectManager.create(project,
          new NullProgressMonitor());
      if(projectFacade != null) {
        return projectFacade.getArtifactKey();
      }
    }
    return null;
  }

  public Class[] getAdapterList() {
    // target type
    return ADAPTER_LIST;
  }

}
