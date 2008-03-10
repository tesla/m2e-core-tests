/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.io.File;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.project.path.DefaultPathTranslator;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.project.BuildPathManager;

/**
 * Special implementation or PathTranslator that redirects filtered resources
 * into target-eclipse/resources and target-eclipse/resources.
 */
public class FilterResourcesPathTranslator extends DefaultPathTranslator {
  public static final String TEST_RESOURCES_FOLDERNAME = BuildPathManager.TEST_CLASSES_FOLDERNAME;
  public static final String RESOURCES_FOLDERNAME = BuildPathManager.CLASSES_FOLDERNAME;

  public void alignToBaseDirectory(Model model, File basedir) {
    super.alignToBaseDirectory(model, basedir);

    MavenProjectManagerImpl.Context context = MavenProjectManagerImpl.getContext();
    
    if (context != null && !context.resolverConfiguration.shouldUseMavenOutputFolders()) {

      IProject project = context.pom.getProject();
  
      MavenRuntimeManager runtimeManager = MavenPlugin.getDefault().getMavenRuntimeManager();
      IFolder outputFolder = project.getFolder(runtimeManager.getDefaultOutputFolder());
      IFolder resourcesFolder = outputFolder.getFolder(RESOURCES_FOLDERNAME);
      IFolder testResourcesFolder = outputFolder.getFolder(TEST_RESOURCES_FOLDERNAME);
  
      Build build = model.getBuild();
      build.setOutputDirectory(resourcesFolder.getLocation().toOSString());
      build.setTestOutputDirectory(testResourcesFolder.getLocation().toOSString());
    }
  }
}
