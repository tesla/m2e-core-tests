/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jdt.launching.sourcelookup.JavaProjectSourceLocation;
import org.eclipse.jdt.launching.sourcelookup.JavaSourceLocator;
import org.eclipse.ui.externaltools.internal.model.ExternalToolBuilder;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;

import org.maven.ide.eclipse.MavenConsole;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.MavenRuntime;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.util.ITraceable;
import org.maven.ide.eclipse.util.Util;


public class MavenLaunchDelegate extends JavaLaunchDelegate implements MavenLaunchConstants, ITraceable {

  private static final boolean TRACE_ENABLED = Boolean.valueOf(
      Platform.getDebugOption("org.maven.ide.eclipse/launcher")).booleanValue();

  public boolean isTraceEnabled() {
    return TRACE_ENABLED;
  }

  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
      throws CoreException {
    MavenConsole console = MavenPlugin.getDefault().getConsole();
    console.logMessage("" + getWorkingDirectory(configuration));
    console.logMessage(" mvn" + getProgramArguments(configuration));
    
    launch.setSourceLocator(createSourceLocator());
    
    super.launch(configuration, ILaunchManager.DEBUG_MODE, launch, monitor);
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getVMRunner(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
   */
  public IVMRunner getVMRunner(final ILaunchConfiguration configuration, String mode) throws CoreException {
    final IVMRunner runner = super.getVMRunner(configuration, mode);
    
    return new IVMRunner() {
      public void run(VMRunnerConfiguration runnerConfiguration, ILaunch launch, IProgressMonitor monitor)
          throws CoreException {
        runner.run(runnerConfiguration, launch, monitor);
        
        IProcess[] processes = launch.getProcesses();
        if(processes!=null && processes.length>0) {
          BackgroundResourceRefresher refresher = new BackgroundResourceRefresher(configuration, processes[0]);
          refresher.startBackgroundRefresh();
        }
      }
    };
  }

  // grab source locations from all open java projects
  // TODO: refactor to use 3.0 interfaces
  private ISourceLocator createSourceLocator() throws CoreException {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IProject allProjects[] = root.getProjects();

    List/*<JavaProjectSourceLocation>*/locations = new ArrayList();

    for(int i = 0; i < allProjects.length; i++ ) {
      IProject project = allProjects[i];
      if(project.isOpen() && project.hasNature("org.eclipse.jdt.core.javanature")) {
        IJavaProject javaProject = JavaCore.create(project);
        locations.add(new JavaProjectSourceLocation(javaProject));
      }
    }

    JavaProjectSourceLocation[] locationArray = (JavaProjectSourceLocation[]) locations
        .toArray(new JavaProjectSourceLocation[locations.size()]);
    return new JavaSourceLocator(locationArray);
  }

  public String getMainTypeName(ILaunchConfiguration configuration) {
    // return MAVEN_EXECUTOR_CLASS;
    return getMavenRuntime(configuration).getMainTypeName();
  }

  public String[] getClasspath(ILaunchConfiguration configuration) {
    return getMavenRuntime(configuration).getClasspath();
//    if(CLASSPATH == null) {
//      List cp = new ArrayList();
//
//      Bundle bundle = findMavenEmbedderBundle();
//
//      Enumeration entries = bundle.findEntries("/", "*", true);
//      while(entries.hasMoreElements()) {
//        URL url = (URL) entries.nextElement();
//        String path = url.getPath();
//        if(path.endsWith(".jar") || path.endsWith("bin/")) {
//          try {
//            cp.add(FileLocator.toFileURL(url).getFile());
//          } catch(IOException ex) {
//            // TODO Auto-generated catch block
//            Tracer.trace(this, "Error adding classpath entry", url.toString() + "; " + ex.getMessage());
//            MavenPlugin.log("Error adding classpath entry " + url.toString(), ex);
//          }
//        }
//      }
//
//      Tracer.trace(this, "classpath", cp);
//
//      CLASSPATH = (String[]) cp.toArray(new String[cp.size()]);
//    }
//    return CLASSPATH;
  }
  
  private MavenRuntime getMavenRuntime(ILaunchConfiguration configuration) {
    MavenRuntimeManager runtimeManager = MavenPlugin.getDefault().getMavenRuntimeManager();
    try {
      String location = configuration.getAttribute(MavenLaunchConstants.ATTR_RUNTIME, "");
      return runtimeManager.getRuntime(location);
    } catch(CoreException ex) {
      MavenPlugin.log(ex);
      return runtimeManager.getDefaultRuntime();
    }
  }

//  private Bundle findMavenEmbedderBundle() {
//    Bundle[] bundles = MavenPlugin.getDefault().getBundleContext().getBundles();
//    for(int i = 0; i < bundles.length; i++ ) {
//      Bundle bundle = bundles[i];
//      if("org.maven.ide.components.maven_embedder".equals(bundle.getSymbolicName())) {
//        return bundle;
//      }
//    }
//
//    return null;
//  }

  public File getWorkingDirectory(ILaunchConfiguration configuration) throws CoreException {
    return super.getWorkingDirectory(configuration);
  }

  public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
//    String pomDirName = configuration.getAttribute(ATTR_POM_DIR, (String) null);
//    pomDirName = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(pomDirName);
//    Tracer.trace(this, "pomDirName", pomDirName);
//
//    String sep = System.getProperty("file.separator"); //$NON-NLS-1$
//    if(!pomDirName.endsWith(sep)) {
//      pomDirName += sep;
//    }
//    String pomFileName = pomDirName + MavenPlugin.POM_FILE_NAME;
//    // wrap file path with quotes to handle spaces
//    if(pomFileName.indexOf(' ') >= 0) {
//      pomFileName = '"' + pomFileName + '"';
//    }
//    Tracer.trace(this, "pomFileName", pomFileName);

    return getProperties(configuration) + //
        getPreferences(configuration) + " " + //
        getGoals(configuration);
  }

  public String getVMArguments(ILaunchConfiguration configuration) throws CoreException {
    return super.getVMArguments(configuration) + getMavenRuntime(configuration).getOptions();
  }

  private String getGoals(ILaunchConfiguration configuration) throws CoreException {
    String buildType = ExternalToolBuilder.getBuildType();
    if(IExternalToolConstants.BUILD_TYPE_AUTO.equals(buildType)) {
      return configuration.getAttribute(MavenLaunchConstants.ATTR_GOALS_AUTO_BUILD, "");
    } else if(IExternalToolConstants.BUILD_TYPE_CLEAN.equals(buildType)) {
      return configuration.getAttribute(MavenLaunchConstants.ATTR_GOALS_CLEAN, "");
    } else if(IExternalToolConstants.BUILD_TYPE_FULL.equals(buildType)) {
      return configuration.getAttribute(MavenLaunchConstants.ATTR_GOALS_AFTER_CLEAN, "");
    } else if(IExternalToolConstants.BUILD_TYPE_INCREMENTAL.equals(buildType)) {
      return configuration.getAttribute(MavenLaunchConstants.ATTR_GOALS_MANUAL_BUILD, "");
    }
    return configuration.getAttribute(MavenLaunchConstants.ATTR_GOALS, "");
  }

  /**
   * Construct string with properties to pass to JVM as system properties
   */
  private String getProperties(ILaunchConfiguration configuration) {
    StringBuffer sb = new StringBuffer();

    try {
      List properties = configuration.getAttribute(ATTR_PROPERTIES, Collections.EMPTY_LIST);
      for(Iterator it = properties.iterator(); it.hasNext();) {
        String[] s = ((String) it.next()).split("=");
        String n = s[0];
        // substitute var if any
        String v = Util.substituteVar(s[1]);
        // enclose in quotes if spaces
        if(v.indexOf(' ') >= 0) {
          v = '"' + v + '"';
        }
        sb.append(" -D").append(n).append("=").append(v);
      }
    } catch(CoreException e) {
      String msg = "Exception while getting configuration attribute " + ATTR_PROPERTIES;
      MavenPlugin.log(msg, e);
    }

    try {
      String profiles = configuration.getAttribute(ATTR_PROFILES, (String) null);
      if(profiles != null && profiles.trim().length() > 0) {
        sb.append(" -P").append(profiles.replaceAll("\\s+", ","));
      }
    } catch(CoreException ex) {
      String msg = "Exception while getting configuration attribute " + ATTR_PROFILES;
      MavenPlugin.log(msg, ex);
    }

    return sb.toString();
  }

  /**
   * Construct string with preferences to pass to JVM as system properties
   */
  private String getPreferences(ILaunchConfiguration configuration) throws CoreException {
    StringBuffer sb = new StringBuffer();

    sb.append(" -B");

    MavenRuntimeManager runtimeManager = MavenPlugin.getDefault().getMavenRuntimeManager();

    boolean debugOutput = configuration.getAttribute(MavenLaunchConstants.ATTR_DEBUG_OUTPUT, //
        runtimeManager.isDebugOutput());
    if(debugOutput) {
      sb.append(" -X").append(" -e");
    }
    // sb.append(" -D").append(MavenPreferenceConstants.P_DEBUG_OUTPUT).append("=").append(debugOutput);

    boolean offline = configuration.getAttribute(MavenLaunchConstants.ATTR_OFFLINE, //
        runtimeManager.isOffline());
    if(offline) {
      sb.append(" -o");
    }
    // sb.append(" -D").append(MavenPreferenceConstants.P_OFFLINE).append("=").append(offline);

    boolean updateSnapshots = configuration.getAttribute(MavenLaunchConstants.ATTR_UPDATE_SNAPSHOTS, false);
    if(updateSnapshots) {
      sb.append(" -U");
    }
    
    boolean skipTests = configuration.getAttribute(MavenLaunchConstants.ATTR_SKIP_TESTS, false);
    if(skipTests) {
      sb.append(" -Dmaven.test.skip=true");
    }
    
    // addSettings(sb, MavenPreferenceConstants.P_GLOBAL_SETTINGS_FILE, preferenceStore);
    addSettings(sb, runtimeManager.getUserSettingsFile());

    // boolean b = preferenceStore.getBoolean(MavenPreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION);
    // sb.append(" -D").append(MavenPreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION).append("=").append(b);

    // b = preferenceStore.getBoolean(MavenPreferenceConstants.P_UPDATE_SNAPSHOTS);
    // sb.append(" -D").append(MavenPreferenceConstants.P_UPDATE_SNAPSHOTS).append("=").append(b);

    // String s = preferenceStore.getString(MavenPreferenceConstants.P_LOCAL_REPOSITORY_DIR);
    // if(s != null && s.trim().length() > 0) {
    //   sb.append(" -D").append(MavenPreferenceConstants.P_LOCAL_REPOSITORY_DIR).append("=\"").append(s).append('\"');
    // }
    
    // String s = preferenceStore.getString(MavenPreferenceConstants.P_GLOBAL_CHECKSUM_POLICY);
    // if(s != null && s.trim().length() > 0) {
    //   sb.append(" -D").append(MavenPreferenceConstants.P_GLOBAL_CHECKSUM_POLICY).append("=").append(s);
    // }
    
    return sb.toString();
  }

  private void addSettings(StringBuffer sb, String value) {
    if(value != null && value.trim().length() > 0) {
      sb.append(" -s ");
      if(value.indexOf(' ') > -1) {
        sb.append('\"').append(value).append('\"');
      } else {
        sb.append(value);
      }
    }
  }

  
  /**
   * Refreshes resources as specified by a launch configuration, when 
   * an associated process terminates.
   * 
   * Adapted from org.eclipse.ui.externaltools.internal.program.launchConfigurations.BackgroundResourceRefresher
   */
  public class BackgroundResourceRefresher implements IDebugEventSetListener  {
    final ILaunchConfiguration configuration;
    final IProcess process;
    
    public BackgroundResourceRefresher(ILaunchConfiguration configuration, IProcess process) {
      this.configuration = configuration;
      this.process = process;
    }
    
    /**
     * If the process has already terminated, resource refreshing is done
     * immediately in the current thread. Otherwise, refreshing is done when the
     * process terminates.
     */
    public void startBackgroundRefresh() {
      synchronized (process) {
        if (process.isTerminated()) {
          refresh();
        } else {
          DebugPlugin.getDefault().addDebugEventListener(this);
        }
      }
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.debug.core.IDebugEventSetListener#handleDebugEvents(org.eclipse.debug.core.DebugEvent[])
     */
    public void handleDebugEvents(DebugEvent[] events) {
      for (int i = 0; i < events.length; i++) {
        DebugEvent event = events[i];
        if (event.getSource() == process && event.getKind() == DebugEvent.TERMINATE) {
          DebugPlugin.getDefault().removeDebugEventListener(this);
          refresh();
          break;
        }
      }
    }
    
    /**
     * Submits a job to do the refresh
     */
    protected void refresh() {
      Job job= new Job("Refreshing resources...") {
        public IStatus run(IProgressMonitor monitor) {
          try {
            RefreshTab.refreshResources(configuration, monitor);
            return Status.OK_STATUS;
          } catch (CoreException e) {
            MavenPlugin.log(e);
            return e.getStatus();
          } 
        }
      };
      job.schedule();
    }
  }
  
}
