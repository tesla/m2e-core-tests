/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr.internal.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.cli.MavenCli;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.pr.internal.sources.ConfigurationDetailsSource;
import org.maven.ide.eclipse.pr.internal.sources.EffectivePomSource;
import org.maven.ide.eclipse.pr.internal.sources.ExternalFileSource;
import org.maven.ide.eclipse.pr.internal.sources.TextConsoleSource;
import org.maven.ide.eclipse.pr.internal.sources.WorkspaceFileSource;


/**
 * @author Eugene Kuleshov
 */
public enum Data {

  MAVEN_USER_SETTINGS("Maven user settings.xml") {
    public void gather(DataGatherer gatherer, IDataTarget target, IProgressMonitor monitor) {
      String settings = gatherer.getMavenConfiguration().getUserSettingsFile();
      if(settings==null || settings.trim().length() == 0) {
        settings = MavenCli.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath();
      }
      gatherer.gather("config", target, new ExternalFileSource(settings, "user-settings.xml"));
      
      // TODO user profiles and toolchain
    }
  },

  MAVEN_GLOBAL_SETTINGS("Maven global settings.xml") {
    public void gather(DataGatherer gatherer, IDataTarget target, IProgressMonitor monitor) {
      String settings = gatherer.getMavenConfiguration().getGlobalSettingsFile();
      gatherer.gather("config", target, new ExternalFileSource(settings, "global-settings.xml"));
    }
  },

  MAVEN_CONSOLE("Maven console") {
    public void gather(DataGatherer gatherer, IDataTarget target, IProgressMonitor monitor) {
      gatherer.gather("config", target, new TextConsoleSource(gatherer.getConsole(), "mavenConsole.log"));
    }
  },

  MAVEN_POM_FILES("Maven project files") {
    public void gather(DataGatherer gatherer, IDataTarget target, IProgressMonitor monitor) {
      for(IProject project : gatherer.getProjects()) {
        String folderName = "projects/" + project.getName();
        if(project.isAccessible()) {
          gatherer.gather(folderName, target, new WorkspaceFileSource(project.getFile("pom.xml")));
          gatherer.gather(folderName, target, new WorkspaceFileSource(project.getFile("profiles.xml")));
          gatherer.gather(folderName, target, new WorkspaceFileSource(project.getFile(".project")));
          gatherer.gather(folderName, target, new WorkspaceFileSource(project.getFile(".classpath")));
  
          gatherer.gather(folderName, target, //
              new EffectivePomSource(gatherer.getProjectManager(), project.getFile("pom.xml"), monitor));
        }
      }
    }
  },

  MAVEN_SOURCES("Maven project sources") {
    public void gather(final DataGatherer gatherer, final IDataTarget target, final IProgressMonitor monitor) {
      Set<IProject> projectSet = gatherer.getProjects();
      List<IProject> projects = new ArrayList<IProject>(projectSet);
      for(IProject project : projectSet) {
        IPath projectLocation = project.getLocation();
        for(Iterator<IProject> it = projects.iterator(); it.hasNext();) {
          IProject project2 = it.next();
          if(project != project2 && projectLocation.isPrefixOf(project2.getLocation())) {
            it.remove();
          }
        }
      }
      
      for(final IProject project : projects) {
        if(project.isAccessible()) {
          try {
            project.accept(new IResourceVisitor() {
              long t = System.currentTimeMillis();
              public boolean visit(IResource resource) throws CoreException {
                if((t - System.currentTimeMillis()) % 1000L == 0) {
                  monitor.subTask(resource.getFullPath().toOSString());
                  t = System.currentTimeMillis();
                }
                if(resource instanceof IFile) {
                  if(!resource.getName().endsWith(".class")) {
                    gatherer.gather("projects" + resource.getParent().getFullPath(), target, //
                        new WorkspaceFileSource((IFile) resource));
                  }
                }
                return true;
              }
            }, IResource.DEPTH_INFINITE, 0);
          } catch(CoreException ex) {
            gatherer.addStatus(ex.getStatus());
          }
        }
      }
    }
  },

  ECLIPSE_CONFIG("Eclipse configuration") {
    public void gather(DataGatherer gatherer, IDataTarget target, IProgressMonitor monitor) {
      gatherer.gather("config", target, new ConfigurationDetailsSource());
    }
  },

  ECLIPSE_LOG("Eclipse log") {
    public void gather(DataGatherer gatherer, IDataTarget target, IProgressMonitor monitor) {
      String file = gatherer.getWorkspace().getRoot().getLocation().append(".metadata/.log").toOSString();
      gatherer.gather("config", target, new ExternalFileSource(file, "error.log"));
    }
  };
  
  
  private final String name;

  private Data(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public abstract void gather(DataGatherer gatherer, IDataTarget target, IProgressMonitor monitor);

}
