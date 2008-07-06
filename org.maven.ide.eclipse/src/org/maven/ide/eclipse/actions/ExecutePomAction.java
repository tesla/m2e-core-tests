/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.actions;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.internal.launch.MavenLaunchConstants;
import org.maven.ide.eclipse.ui.internal.launch.MavenLaunchMainTab;
import org.maven.ide.eclipse.util.ITraceable;
import org.maven.ide.eclipse.util.Tracer;
import org.maven.ide.eclipse.util.Util;


/**
 * Maven launch shortcut
 * 
 * @author Dmitri Maximovich
 * @author Eugene Kuleshov
 */
public class ExecutePomAction implements ILaunchShortcut, IExecutableExtension, ITraceable {
  private static final boolean TRACE_ENABLED = Boolean
      .valueOf(Platform.getDebugOption("org.maven.ide.eclipse/actions")).booleanValue();

  private boolean showDialog = false;

  private String goalName = null;

  public boolean isTraceEnabled() {
    return TRACE_ENABLED;
  }

  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    if("WITH_DIALOG".equals(data)) {
      this.showDialog = true;
    } else {
      this.goalName = (String) data;
    }
  }

  public void launch(IEditorPart editor, String mode) {
    IEditorInput editorInput = editor.getEditorInput();
    if(editorInput instanceof IFileEditorInput) {
      launch(((IFileEditorInput) editorInput).getFile().getParent(), mode);
    }
  }

  public void launch(ISelection selection, String mode) {
    if(selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      Object object = structuredSelection.getFirstElement();

      IContainer basedir = null;
      if(object instanceof IProject || object instanceof IFolder) {
        basedir = (IContainer) object;
      } else if(object instanceof IFile) {
        basedir = ((IFile) object).getParent();
      } else if(object instanceof IAdaptable) {
        IAdaptable adaptable = (IAdaptable) object;
        Object adapter = adaptable.getAdapter(IProject.class);
        if(adapter != null) {
          basedir = (IContainer) adapter;
        } else {
          adapter = adaptable.getAdapter(IFolder.class);
          if(adapter != null) {
            basedir = (IContainer) adapter;
          } else {
            adapter = adaptable.getAdapter(IFile.class);
            if(adapter != null) {
              basedir = ((IFile) object).getParent();
            }
          }
        }
      }

      launch(basedir, mode);
    }
  }

  @SuppressWarnings("deprecation")
  private void launch(IContainer basecon, String mode) {
    if(basecon == null) {
      return;
    }
    
    IContainer basedir = findPomXmlBasedir(basecon);

    Tracer.trace(this, "Launching", basedir);

    ILaunchConfiguration launchConfiguration = getLaunchConfiguration(basedir);
    if(launchConfiguration == null) {
      return;
    }

    boolean openDialog = showDialog;
    if(!openDialog) {
      try {
        // if no goals specified
        String goals = launchConfiguration.getAttribute(MavenLaunchConstants.ATTR_GOALS, (String) null);
        openDialog = goals == null || goals.trim().length() == 0;
      } catch(CoreException ex) {
        MavenLogger.log(ex);
      }
    }

    if(openDialog) {
      Tracer.trace(this, "Opening dialog for launch configuration", launchConfiguration.getName());
      DebugUITools.saveBeforeLaunch();
      Shell shell = MavenPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();
      // ILaunchGroup group = DebugUITools.getLaunchGroup(launchConfiguration, mode);
      DebugUITools.openLaunchConfigurationDialog(shell, launchConfiguration,
          MavenLaunchMainTab.ID_EXTERNAL_TOOLS_LAUNCH_GROUP, null);
    } else {
      Tracer.trace(this, "Launch configuration", launchConfiguration.getName());
      DebugUITools.launch(launchConfiguration, mode);
    }
  }

  private IContainer findPomXmlBasedir(IContainer origDir) {
    if(origDir == null) {
      return null;
    }

    try {
      // loop upwards through the parents as long as we do not cross the project boundary
      while(origDir.exists() && origDir.getProject() != null && origDir.getProject() != origDir) {
        // see if pom.xml exists
        if(origDir.getType() == IResource.FOLDER) {
          IFolder fold = (IFolder) origDir;
          if(fold.findMember(IMavenConstants.POM_FILE_NAME) != null) {
            return fold;
          }
        } else if(origDir.getType() == IResource.FILE) {
          if(((IFile) origDir).getName().equals(IMavenConstants.POM_FILE_NAME)) {
            return origDir.getParent();
          }
        }
        origDir = origDir.getParent();
      }
    } catch(Exception e) {
      return origDir;
    }
    return origDir;
  }

  private ILaunchConfiguration createLaunchConfiguration(IContainer basedir, String goal) {
    try {
      ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
      ILaunchConfigurationType launchConfigurationType = launchManager
          .getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);

      ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, //
          "Executing " + goal + " in " + basedir.getLocation());
      workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, basedir.getLocation().toOSString());
      workingCopy.setAttribute(MavenLaunchConstants.ATTR_GOALS, goal);
      workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_SCOPE, "${project}");
      workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, true);

      IPath path = getJREContainerPath(basedir);
      if(path != null) {
        workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, path.toPortableString());
      }

      return workingCopy;
    } catch(CoreException ex) {
      Tracer.trace(this, "Error creating new launch configuration", "", ex);
    }
    return null;
  }

  // TODO ideally it should use MavenProject, but it is faster to scan IJavaProjects 
  private IPath getJREContainerPath(IContainer basedir) throws JavaModelException {
    IProject project = basedir.getProject();
    if(project != null) {
      IJavaProject javaProject = JavaCore.create(project);
      IClasspathEntry[] entries = javaProject.getRawClasspath();
      for(int i = 0; i < entries.length; i++ ) {
        IClasspathEntry entry = entries[i];
        if(JavaRuntime.JRE_CONTAINER.equals(entry.getPath().segment(0))) {
          return entry.getPath();
        }
      }
    }
    return null;
  }

  private ILaunchConfiguration getLaunchConfiguration(IContainer basedir) {
    if(goalName != null) {
      return createLaunchConfiguration(basedir, goalName);
    }

    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType launchConfigurationType = launchManager
        .getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);

    // scan existing launch configurations
    IPath basedirLocation = basedir.getLocation();
    if(!showDialog) {
      try {
        ILaunch[] launches = launchManager.getLaunches();
        ILaunchConfiguration[] launchConfigurations = null;

        if(launches.length > 0) {
          for(int i = 0; i < launches.length; i++ ) {
            ILaunchConfiguration config = launches[i].getLaunchConfiguration();
            if(config != null && launchConfigurationType.equals(config.getType())) {
              launchConfigurations = new ILaunchConfiguration[] {config};
            }
          }
        }
        if(launchConfigurations == null) {
          launchConfigurations = launchManager.getLaunchConfigurations(launchConfigurationType);
        }
        for(int i = 0; i < launchConfigurations.length; i++ ) {
          ILaunchConfiguration cfg = launchConfigurations[i];
          Tracer.trace(this, "Processing existing launch configuration", cfg.getName());
          // don't forget to substitute variables
          String workDir = Util.substituteVar(cfg.getAttribute(MavenLaunchConstants.ATTR_POM_DIR, (String) null));
          if(workDir == null) {
            Tracer.trace(this, "Launch configuration doesn't have workdir!");
            continue;
          }
          Tracer.trace(this, "Workdir", workDir);
          IPath workPath = new Path(workDir);
          if(basedirLocation.equals(workPath)) {
            Tracer.trace(this, "Found matching existing configuration", cfg.getName());
            return cfg;
          }
        }
      } catch(CoreException e) {
        Tracer.trace(this, "Error scanning existing launch configurations", "", e);
      }

      Tracer.trace(this, "No existing configurations found, creating new");
    }

    Tracer.trace(this, "No existing configurations found, creating new");

    String newName = launchManager.generateUniqueLaunchConfigurationNameFrom(basedirLocation.lastSegment());
    try {
      Tracer.trace(this, "New configuration name", newName);
      ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, newName);
      workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, basedirLocation.toString());

      // set other defaults if needed
      // MavenLaunchMainTab maintab = new MavenLaunchMainTab();
      // maintab.setDefaults(workingCopy);
      // maintab.dispose();

      ILaunchConfiguration newConfiguration = workingCopy.doSave();
      return newConfiguration;
    } catch(Exception e) {
      String msg = "Error creating new launch configuration";
      Tracer.trace(this, msg, newName, e);
    }
    return null;
  }

}
