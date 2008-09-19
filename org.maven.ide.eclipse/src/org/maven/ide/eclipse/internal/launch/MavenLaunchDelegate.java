/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.launch;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;
import org.eclipse.ui.externaltools.internal.model.ExternalToolBuilder;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.embedder.MavenEmbedder;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.embedder.MavenRuntime;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.util.Util;


@SuppressWarnings("restriction")
public class MavenLaunchDelegate extends JavaLaunchDelegate implements MavenLaunchConstants {

  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
      throws CoreException {
    MavenConsole console = MavenPlugin.getDefault().getConsole();
    console.logMessage("" + getWorkingDirectory(configuration));
    console.logMessage(" mvn" + getProgramArguments(configuration));
    
    ISourceLookupDirector sourceLocator = createSourceLocator(configuration);
    launch.setSourceLocator(sourceLocator);
    
    super.launch(configuration, mode, launch, monitor);
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
  private ISourceLookupDirector createSourceLocator(ILaunchConfiguration configuration) throws CoreException {
    ISourceLookupDirector sourceLocator = new AbstractSourceLookupDirector() {
      public void initializeParticipants() {
        addParticipants(new ISourceLookupParticipant[] {new JavaSourceLookupParticipant()});
      }
    };

    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IProject[] projects = root.getProjects();
    List<IRuntimeClasspathEntry> entries = new ArrayList<IRuntimeClasspathEntry>();
    IRuntimeClasspathEntry jreEntry = JavaRuntime.computeJREEntry(configuration);
    if (jreEntry != null) {
      entries.add(jreEntry);
    }
    for(IProject project : projects) {
      IJavaProject javaProject = JavaCore.create(project);
      if (javaProject != null) {
        entries.add(JavaRuntime.newDefaultProjectClasspathEntry(javaProject));
      }
    }
    IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveSourceLookupPath(entries.toArray(new IRuntimeClasspathEntry[entries.size()]), configuration);
    sourceLocator.initializeDefaults(configuration);
    sourceLocator.setSourceContainers(JavaRuntime.getSourceContainers(resolved));
    return sourceLocator;
  }

  public String getMainTypeName(ILaunchConfiguration configuration) throws CoreException {
    // return MAVEN_EXECUTOR_CLASS;
    return getMavenRuntime(configuration).getMainTypeName();
  }

  public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
    String[] forcedCompoStrings = getForcedComponents(configuration);
    return getMavenRuntime(configuration).getClasspath(forcedCompoStrings);
  }
  
  private MavenRuntime getMavenRuntime(ILaunchConfiguration configuration) throws CoreException {
    MavenRuntimeManager runtimeManager = MavenPlugin.getDefault().getMavenRuntimeManager();
    String location = getMavenRuntimeLocation(configuration);
    MavenRuntime runtime = runtimeManager.getRuntime(location);
    if(runtime==null) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, //
          "Can't find Maven installation " + location, null));
    }
    return runtime;
  }

  private String getMavenRuntimeLocation(ILaunchConfiguration configuration) throws CoreException {
    return configuration.getAttribute(MavenLaunchConstants.ATTR_RUNTIME, "");
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
    StringBuffer sb = new StringBuffer();
    if (shouldResolveWorkspaceArtifacts(configuration)) {
      File state = MavenPlugin.getDefault().getMavenProjectManager().getWorkspaceStateFile();
      sb.append("-Dm2eclipse.workspace.state=\"").append(state.getAbsolutePath()).append("\"");
    }
    sb.append(" ").append(super.getVMArguments(configuration));
    File state = MavenPlugin.getDefault().getStateLocation().toFile();
    String[] forcedComponents = getForcedComponents(configuration);
    sb.append(getMavenRuntime(configuration).getOptions(new File(state, configuration.getName()), forcedComponents));
    return sb.toString();
  }

  private boolean shouldResolveWorkspaceArtifacts(ILaunchConfiguration configuration) throws CoreException {
    return configuration.getAttribute(ATTR_WORKSPACE_RESOLUTION, false);
  }

  private String getCliResolver() throws CoreException {
    URL url = MavenPlugin.getDefault().getBundle().getEntry("org.maven.ide.eclipse.cliresolver.jar");
    try {
      URL fileURL = FileLocator.toFileURL(url);
      // MNGECLIPSE-804 workaround for spaces in the original path
      URI fileURI = new URI(fileURL.getProtocol(), fileURL.getHost(), fileURL.getPath(), fileURL.getQuery());
      return new File(fileURI).getCanonicalPath();
    } catch(Exception ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, ex.getMessage(), ex));
    }
  }

  private String getGoals(ILaunchConfiguration configuration) throws CoreException {
    String buildType = ExternalToolBuilder.getBuildType();
    String key = MavenLaunchConstants.ATTR_GOALS;
    if(IExternalToolConstants.BUILD_TYPE_AUTO.equals(buildType)) {
      key = MavenLaunchConstants.ATTR_GOALS_AUTO_BUILD;
    } else if(IExternalToolConstants.BUILD_TYPE_CLEAN.equals(buildType)) {
      key = MavenLaunchConstants.ATTR_GOALS_CLEAN;
    } else if(IExternalToolConstants.BUILD_TYPE_FULL.equals(buildType)) {
      key = MavenLaunchConstants.ATTR_GOALS_AFTER_CLEAN;
    } else if(IExternalToolConstants.BUILD_TYPE_INCREMENTAL.equals(buildType)) {
      key = MavenLaunchConstants.ATTR_GOALS_MANUAL_BUILD;
    }
    String goals = configuration.getAttribute(key, "");
    if(goals==null || goals.length()==0) {
      // use default goals when "full build" returns nothing
      goals = configuration.getAttribute(MavenLaunchConstants.ATTR_GOALS, "");
    }
    
    MavenPlugin.getDefault().getConsole().logMessage("Build type " + buildType + " : " + goals);
    return goals;
  }

  public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) {
    return false;
  }
  
  /**
   * Construct string with properties to pass to JVM as system properties
   */
  private String getProperties(ILaunchConfiguration configuration) {
    StringBuffer sb = new StringBuffer();

    try {
      @SuppressWarnings("unchecked")
      List<String> properties = configuration.getAttribute(ATTR_PROPERTIES, Collections.EMPTY_LIST);
      for(String property : properties) {
        String[] s = property.split("=");
        String n = s[0];
        String v = Util.substituteVar(s[1]);
        if(v.indexOf(' ') >= 0) {
          v = '"' + v + '"';
        }
        sb.append(" -D").append(n).append("=").append(v);
      }
    } catch(CoreException e) {
      String msg = "Exception while getting configuration attribute " + ATTR_PROPERTIES;
      MavenLogger.log(msg, e);
    }

    try {
      String profiles = configuration.getAttribute(ATTR_PROFILES, (String) null);
      if(profiles != null && profiles.trim().length() > 0) {
        sb.append(" -P").append(profiles.replaceAll("\\s+", ","));
      }
    } catch(CoreException ex) {
      String msg = "Exception while getting configuration attribute " + ATTR_PROFILES;
      MavenLogger.log(msg, ex);
    }

    return sb.toString();
  }

  /**
   * Construct string with preferences to pass to JVM as system properties
   */
  private String getPreferences(ILaunchConfiguration configuration) throws CoreException {
    MavenRuntimeManager runtimeManager = MavenPlugin.getDefault().getMavenRuntimeManager();
    
    StringBuffer sb = new StringBuffer();

    sb.append(" -B");

    if(configuration.getAttribute(MavenLaunchConstants.ATTR_DEBUG_OUTPUT, runtimeManager.isDebugOutput())) {
      sb.append(" -X").append(" -e");
    }
    // sb.append(" -D").append(MavenPreferenceConstants.P_DEBUG_OUTPUT).append("=").append(debugOutput);

    if(configuration.getAttribute(MavenLaunchConstants.ATTR_OFFLINE, runtimeManager.isOffline())) {
      sb.append(" -o");
    }
    // sb.append(" -D").append(MavenPreferenceConstants.P_OFFLINE).append("=").append(offline);

    if(configuration.getAttribute(MavenLaunchConstants.ATTR_UPDATE_SNAPSHOTS, false)) {
      sb.append(" -U");
    }
    
    if(configuration.getAttribute(MavenLaunchConstants.ATTR_NON_RECURSIVE, false)) {
      sb.append(" -N");
    }
    
    if(configuration.getAttribute(MavenLaunchConstants.ATTR_SKIP_TESTS, false)) {
      sb.append(" -Dmaven.test.skip=true");
    }
    
    String settings = runtimeManager.getUserSettingsFile();
//    if(settings==null) {
//      settings = getMavenRuntime(configuration).getSettings();
//    }
    if(settings != null && settings.trim().length() > 0) {
      sb.append(" -s ");
      if(settings.indexOf(' ') > -1) {
        sb.append('\"').append(settings).append('\"');
      } else {
        sb.append(settings);
      }
    }

    // boolean b = preferenceStore.getBoolean(MavenPreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION);
    // sb.append(" -D").append(MavenPreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION).append("=").append(b);

    // b = preferenceStore.getBoolean(MavenPreferenceConstants.P_UPDATE_SNAPSHOTS);
    // sb.append(" -D").append(MavenPreferenceConstants.P_UPDATE_SNAPSHOTS).append("=").append(b);

    // String s = preferenceStore.getString(MavenPreferenceConstants.P_GLOBAL_CHECKSUM_POLICY);
    // if(s != null && s.trim().length() > 0) {
    //   sb.append(" -D").append(MavenPreferenceConstants.P_GLOBAL_CHECKSUM_POLICY).append("=").append(s);
    // }
    
    return sb.toString();
  }

  /**
   * Refreshes resources as specified by a launch configuration, when 
   * an associated process terminates.
   * 
   * Adapted from org.eclipse.ui.externaltools.internal.program.launchConfigurations.BackgroundResourceRefresher
   */
  public static class BackgroundResourceRefresher implements IDebugEventSetListener  {
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
            MavenLogger.log(e);
            return e.getStatus();
          } 
        }
      };
      job.schedule();
    }
  }

  private String[] getForcedComponents(ILaunchConfiguration configuration) throws CoreException {
    List<String> components = new ArrayList<String>();
    if (shouldResolveWorkspaceArtifacts(configuration)) {
      components.add(getCliResolver());
    }
    addUserComponents(configuration, components);
    return components.toArray(new String[components.size()]);
  }

  private void addUserComponents(ILaunchConfiguration configuration, List<String> components) throws CoreException {
    @SuppressWarnings("unchecked")
    List<String> list = configuration.getAttribute(ATTR_FORCED_COMPONENTS_LIST, new ArrayList());
    if(list == null) {
      return;
    }
    
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    MavenEmbedderManager embedderManager = MavenPlugin.getDefault().getMavenEmbedderManager();
    for(String gav : list) {
      // groupId:artifactId:version
      StringTokenizer st = new StringTokenizer(gav, ":");
      String groupId = st.nextToken();
      String artifactId = st.nextToken();
      String version = st.nextToken();

      IMavenProjectFacade facade = projectManager.getMavenProject(groupId, artifactId, version);

      File file = null;
      if (facade != null) {
        IFolder output = root.getFolder(facade.getOutputLocation());
        if (output.isAccessible()) {
          file = output.getLocation().toFile();
        }
      } else {
        String name = groupId + ":" + artifactId + ":" + version;
        try {
          MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();
          Artifact artifact = embedder.createArtifact(groupId, artifactId, version, null, "jar");
          embedder.resolve(artifact, Collections.EMPTY_LIST, embedder.getLocalRepository());
          file = artifact.getFile();
        } catch(ArtifactResolutionException ex) {
          MavenLogger.log("Artifact resolution error " + name, ex);
        } catch(ArtifactNotFoundException ex) {
          MavenLogger.log("Artifact not found " + name, ex);
        }
      }
      
      if (file != null) {
        components.add(file.getAbsolutePath());
      }
    }
  }
}
