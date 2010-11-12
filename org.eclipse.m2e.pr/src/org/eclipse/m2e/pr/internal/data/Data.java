/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.pr.internal.data;

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
import org.eclipse.m2e.pr.IDataTarget;
import org.eclipse.m2e.pr.internal.Messages;
import org.eclipse.m2e.pr.internal.sources.ConfigurationDetailsSource;
import org.eclipse.m2e.pr.internal.sources.EffectivePomSource;
import org.eclipse.m2e.pr.internal.sources.ObfuscatedSettingsSource;
import org.eclipse.m2e.pr.internal.sources.TextConsoleSource;
import org.eclipse.m2e.pr.internal.sources.WorkspaceFileSource;
import org.eclipse.m2e.pr.sources.ExternalFileSource;


/**
 * @author Eugene Kuleshov
 */
public enum Data {

  MAVEN_USER_SETTINGS(Messages.Data_user_settings) {
    public void gather(DataGatherer gatherer, IDataTarget target, IProgressMonitor monitor) {
      String settings = gatherer.getMavenConfiguration().getUserSettingsFile();
      if(settings==null || settings.trim().length() == 0) {
        settings = MavenCli.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath();
      }
      gatherer.gather("config", target, new ObfuscatedSettingsSource(settings, "user-settings.xml")); //$NON-NLS-1$ //$NON-NLS-2$
      
      // TODO user profiles and toolchain
    }
  },

  MAVEN_GLOBAL_SETTINGS(Messages.Data_global_settings) {
    public void gather(DataGatherer gatherer, IDataTarget target, IProgressMonitor monitor) {
      String settings = gatherer.getMavenConfiguration().getGlobalSettingsFile();
      gatherer.gather("config", target, new ObfuscatedSettingsSource(settings, "global-settings.xml")); //$NON-NLS-1$ //$NON-NLS-2$
    }
  },

  MAVEN_CONSOLE(Messages.Data_console) {
    public void gather(DataGatherer gatherer, IDataTarget target, IProgressMonitor monitor) {
      gatherer.gather("config", target, new TextConsoleSource(gatherer.getConsole(), "mavenConsole.log")); //$NON-NLS-1$ //$NON-NLS-2$
    }
  },

  MAVEN_POM_FILES(Messages.Data_project_files) {
    public void gather(DataGatherer gatherer, IDataTarget target, IProgressMonitor monitor) {
      for(IProject project : gatherer.getProjects()) {
        String folderName = "projects/" + project.getName(); //$NON-NLS-1$
        if(project.isAccessible()) {
          gatherer.gather(folderName, target, new WorkspaceFileSource(project.getFile("pom.xml"))); //$NON-NLS-1$
          gatherer.gather(folderName, target, new WorkspaceFileSource(project.getFile("profiles.xml"))); //$NON-NLS-1$
          gatherer.gather(folderName, target, new WorkspaceFileSource(project.getFile(".project"))); //$NON-NLS-1$
          gatherer.gather(folderName, target, new WorkspaceFileSource(project.getFile(".classpath"))); //$NON-NLS-1$
  
          gatherer.gather(folderName, target, //
              new EffectivePomSource(gatherer.getProjectManager(), project.getFile("pom.xml"), monitor)); //$NON-NLS-1$
        }
      }
    }
  },

  MAVEN_SOURCES(Messages.Data_project_sources) {
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
                  if(!resource.getName().endsWith(".class")) { //$NON-NLS-1$
                    gatherer.gather("projects" + resource.getParent().getFullPath(), target, // //$NON-NLS-1$
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

  ECLIPSE_CONFIG(Messages.Data_eclipse_config) {
    public void gather(DataGatherer gatherer, IDataTarget target, IProgressMonitor monitor) {
      gatherer.gather("config", target, new ConfigurationDetailsSource()); //$NON-NLS-1$
    }
  },

  ECLIPSE_LOG("Eclipse log") { //$NON-NLS-1$
    public void gather(DataGatherer gatherer, IDataTarget target, IProgressMonitor monitor) {
      String file = gatherer.getWorkspace().getRoot().getLocation().append(".metadata/.log").toOSString(); //$NON-NLS-1$
      gatherer.gather("config", target, new ExternalFileSource(file, "error.log")); //$NON-NLS-1$ //$NON-NLS-2$
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
