/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.osgi.service.prefs.BackingStoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.DelegatingLocalArtifactRepository;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.project.IMavenMarkerManager;
import org.maven.ide.eclipse.project.IMavenProjectChangedListener;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.MavenUpdateRequest;
import org.maven.ide.eclipse.project.ResolverConfiguration;

/**
 * This class keeps track of all maven projects present in the workspace and
 * provides mapping between Maven and the workspace.
 */
public class MavenProjectManagerImpl {

  public static boolean DEBUG = MavenPlugin.getDefault().isDebugging()
      & Boolean.parseBoolean(Platform.getDebugOption(IMavenConstants.PLUGIN_ID + "/debug/projectManager"));

  static final String ARTIFACT_TYPE_POM = "pom";
  static final String ARTIFACT_TYPE_JAR = "jar";
  public static final String ARTIFACT_TYPE_JAVA_SOURCE = "java-source";
  public static final String ARTIFACT_TYPE_JAVADOC = "javadoc";

  private static final String P_VERSION = "version";
  private static final String P_INCLUDE_MODULES = "includeModules";
  private static final String P_RESOLVE_WORKSPACE_PROJECTS = "resolveWorkspaceProjects";
  private static final String P_RESOURCE_FILTER_GOALS = "resourceFilterGoals";
  private static final String P_FULL_BUILD_GOALS = "fullBuildGoals";
  private static final String P_SKIP_COMPILER_PLUGIN = "skipCompilerPlugin";
  private static final String P_ACTIVE_PROFILES = "activeProfiles";

  private static final String VERSION = "1";

  /**
   * Path of project metadata files, relative to the project. These
   * files are used to determine if project dependencies need to be
   * updated.
   * 
   * Note that path of pom.xml varies for nested projects and pom.xml
   * are treated separately.
   */
  public static final List<? extends IPath> METADATA_PATH = Arrays.asList( //
      new Path(".project"), //
      new Path(".classpath"), //
      new Path(".settings/org.maven.ide.eclipse.prefs")); // dirty hack!

  private final ProjectRegistry projectRegistry;

  private final MavenConsole console;
  private final IMaven maven;

  private final IMavenMarkerManager markerManager;

  private final ProjectRegistryReader stateReader;

  private final Set<IMavenProjectChangedListener> projectChangeListeners = new LinkedHashSet<IMavenProjectChangedListener>();

  private final transient List<IManagedCache> caches = new ArrayList<IManagedCache>();

  private volatile Thread syncRefreshThread;

  public MavenProjectManagerImpl(MavenConsole console,
      File stateLocationDir, boolean readState, MavenRuntimeManager runtimeManager, IMavenMarkerManager mavenMarkerManager) {
    this.console = console;
    this.markerManager = mavenMarkerManager;
    this.maven = MavenPlugin.lookup(IMaven.class);

    this.stateReader = new ProjectRegistryReader(stateLocationDir);

    ProjectRegistry state = readState && stateReader != null ? stateReader.readWorkspaceState(this) : null;
    this.projectRegistry = (state != null && state.isValid())? state: new ProjectRegistry();
  }
  
  /**
   * Creates or returns cached MavenProjectFacade for the given project.
   * 
   * This method will not block if called from IMavenProjectChangedListener#mavenProjectChanged
   */
  public MavenProjectFacade create(IProject project, IProgressMonitor monitor) {
    return create(getPom(project), false, monitor);
  }

  /**
   * Returns MavenProjectFacade corresponding to the pom.
   * 
   * This method first looks in the project cache, then attempts to load
   * the pom if the pom is not found in the cache. In the latter case,
   * workspace resolution is assumed to be enabled for the pom but the pom
   * will not be added to the cache.
   */
  public MavenProjectFacade create(IFile pom, boolean load, IProgressMonitor monitor) {
    if(pom == null) {
      return null;
    }

    // MavenProjectFacade projectFacade = (MavenProjectFacade) workspacePoms.get(pom.getFullPath());
    MavenProjectFacade projectFacade = projectRegistry.getProjectFacade(pom);
    if(projectFacade == null && load) {
      ResolverConfiguration configuration = readResolverConfiguration(pom.getProject());

      MavenExecutionResult executionResult = readProjectWithDependencies(projectRegistry, pom, configuration, //
          new MavenUpdateRequest(true /* offline */, false /* updateSnapshots */),
          monitor);
      MavenProject mavenProject = executionResult.getProject();
      if(mavenProject != null) {
        projectFacade = new MavenProjectFacade(this, pom, mavenProject, configuration);
      } else {
        List<Exception> exceptions = executionResult.getExceptions();
        if (exceptions != null) {
          for(Exception ex : exceptions) {
            String msg = "Failed to read Maven project";
            console.logError(msg);
            console.logError(ex.toString());
            MavenLogger.log(msg, ex);
          }
        }
      }
    }
    return projectFacade;
  }

  public boolean saveResolverConfiguration(IProject project, ResolverConfiguration configuration) {
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(IMavenConstants.PLUGIN_ID);
    if(projectNode != null) {
      projectNode.put(P_VERSION, VERSION);
      
      projectNode.putBoolean(P_SKIP_COMPILER_PLUGIN, configuration.isSkipCompiler());
      projectNode.putBoolean(P_RESOLVE_WORKSPACE_PROJECTS, configuration.shouldResolveWorkspaceProjects());
      projectNode.putBoolean(P_INCLUDE_MODULES, configuration.shouldIncludeModules());
      
      projectNode.put(P_RESOURCE_FILTER_GOALS, configuration.getResourceFilteringGoals());
      projectNode.put(P_FULL_BUILD_GOALS, configuration.getFullBuildGoals());
      projectNode.put(P_ACTIVE_PROFILES, configuration.getActiveProfiles());
      
      try {
        projectNode.flush();
        return true;
      } catch(BackingStoreException ex) {
        MavenLogger.log("Failed to save resolver configuration", ex);
      }
    }
    
    return false;
  }

  public ResolverConfiguration readResolverConfiguration(IProject project) {
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(IMavenConstants.PLUGIN_ID);
    if(projectNode==null) {
      return new ResolverConfiguration();
    }
    
    String version = projectNode.get(P_VERSION, null);
    if(version == null) {  // migrate from old config
      // return LegacyBuildPathManager.getResolverConfiguration(project);
      return new ResolverConfiguration();
    }
  
    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setSkipCompiler(projectNode.getBoolean(P_SKIP_COMPILER_PLUGIN, true));
    configuration.setResolveWorkspaceProjects(projectNode.getBoolean(P_RESOLVE_WORKSPACE_PROJECTS, false));
    configuration.setIncludeModules(projectNode.getBoolean(P_INCLUDE_MODULES, false));
    
    configuration.setResourceFilteringGoals(projectNode.get(P_RESOURCE_FILTER_GOALS, ResolverConfiguration.DEFAULT_FILTERING_GOALS));
    configuration.setFullBuildGoals(projectNode.get(P_FULL_BUILD_GOALS, ResolverConfiguration.DEFAULT_FULL_BUILD_GOALS));
    configuration.setActiveProfiles(projectNode.get(P_ACTIVE_PROFILES, ""));
    return configuration;
  }

  IFile getPom(IProject project) {
    if (project == null || !project.isAccessible()) {
      // XXX sensible handling
      return null;
    }
    return project.getFile(IMavenConstants.POM_FILE_NAME);
  }

  /**
   * Removes specified poms from the cache.
   * Adds dependent poms to pomSet but does not directly refresh dependent poms.
   * Recursively removes all nested modules if appropriate.
   * 
   * @return a {@link Set} of {@link IFile} affected poms
   */
  public Set<IFile> remove(MutableProjectRegistry state, Set<IFile> poms, boolean force) {
    Set<IFile> pomSet = new LinkedHashSet<IFile>();
    for (Iterator<IFile> it = poms.iterator(); it.hasNext(); ) {
      IFile pom = it.next();
      MavenProjectFacade facade = state.getProjectFacade(pom);
      if (force || facade == null || facade.isStale()) {
        pomSet.addAll(remove(state, pom));
      }
    }
    return pomSet;
  }
  
  /**
   * Removes the pom from the cache. 
   * Adds dependent poms to pomSet but does not directly refresh dependent poms.
   * Recursively removes all nested modules if appropriate.
   * 
   * @return a {@link Set} of {@link IFile} affected poms
   */
  public Set<IFile> remove(MutableProjectRegistry state, IFile pom) {
    MavenProjectFacade facade = state.getProjectFacade(pom);
    ArtifactKey mavenProject = facade != null ? facade.getArtifactKey() : null;

    flushCaches(pom, facade);

    if (mavenProject == null) {
      state.removeProject(pom, null);
      return Collections.emptySet();
    }

    Set<IFile> pomSet = new LinkedHashSet<IFile>();

    pomSet.addAll(state.getDependents(pom, mavenProject, false));
    state.removeProject(pom, mavenProject);

    // XXX this will likely NOT work for closed/removed projects, need to move to IResourceChangeEventListener
    if(facade!=null) {
      ResolverConfiguration resolverConfiguration = facade.getResolverConfiguration();
      if (resolverConfiguration != null && resolverConfiguration.shouldIncludeModules()) {
        pomSet.addAll(removeNestedModules(state, pom, facade.getMavenProjectModules()));
      }
    }

    pomSet.addAll(refreshWorkspaceModules(state, pom, mavenProject));

    pomSet.remove(pom);
    
    return pomSet;
  }

  private void flushCaches(IFile pom, MavenProjectFacade facade) {
    ArtifactKey key = null;
    MavenProject project = null;
    
    if (facade != null) {
      key = facade.getArtifactKey();
      project = facade.getMavenProject();
    }
    for (IManagedCache cache : caches) {
      cache.removeProject(pom, key);
    }
    if (project != null) {
      maven.xxxRemoveExtensionsRealm(project);
    }
  }

  /**
   * This method acquires workspace root's lock and sends project change events.
   * It is meant for synchronous registry updates.
   */
  public void refresh(MavenUpdateRequest request, IProgressMonitor monitor) throws CoreException {
    ISchedulingRule rule = ResourcesPlugin.getWorkspace().getRoot();
    Job.getJobManager().beginRule(rule, monitor);
    try {
      syncRefreshThread = Thread.currentThread();

      MutableProjectRegistry newState = newMutableProjectRegistry();
      try {
        refresh(newState, request, monitor);
  
        applyMutableProjectRegistry(newState, monitor);
      } finally {
        newState.close();
      }
    } finally {
      syncRefreshThread = null;
      Job.getJobManager().endRule(rule);
    }
  }

  void refresh(MutableProjectRegistry newState, MavenUpdateRequest updateRequest, IProgressMonitor monitor) throws CoreException {

    DependencyResolutionContext context = new DependencyResolutionContext(updateRequest);

    while(!context.isEmpty()) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      if (newState.isStale() || (syncRefreshThread != null && syncRefreshThread != Thread.currentThread())) {
        throw new StaleMutableProjectRegistryException();
      }

      IFile pom = context.pop();
      monitor.subTask(pom.getFullPath().toString());

      if (!pom.isAccessible() || !pom.getProject().hasNature(IMavenConstants.NATURE_ID)) {
        context.forcePomFiles(remove(newState, pom));
        continue;
      }

      refresh(newState, pom, context, monitor);
      monitor.worked(1);
    }

  }

  MavenExecutionPlan calculateExecutionPlan(MavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
    return calculateExecutionPlan(projectRegistry, facade, monitor);
  }
  private MavenExecutionPlan calculateExecutionPlan(IProjectRegistry state, MavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
    MavenExecutionRequest request = createExecutionRequest(state, facade.getPom(), facade.getResolverConfiguration(), monitor);
    request.setGoals(Arrays.asList("package"));
    return maven.calculateExecutionPlan(request, facade.getMavenProject(monitor), monitor);
  }

  private void refresh(MutableProjectRegistry state, IFile pom, DependencyResolutionContext context, IProgressMonitor monitor) throws CoreException {
    MavenProjectFacade oldFacade = state.getProjectFacade(pom);

    if(!context.isForce(pom) && oldFacade != null && !oldFacade.isStale()) {
      // skip refresh if not forced and up-to-date facade
      return;
    }

    flushCaches(pom, oldFacade);

    markerManager.deleteMarkers(pom);

    ResolverConfiguration resolverConfiguration = readResolverConfiguration(pom.getProject());

    MavenProject mavenProject = null;
    MavenExecutionResult result = null;
    if (pom.isAccessible()) {
      result = readProjectWithDependencies(state, pom, resolverConfiguration, context.getRequest(), monitor);
      mavenProject = result.getProject();
      markerManager.addMarkers(pom, result);
    }

    if (mavenProject == null) {
      context.forcePomFiles(remove(state, pom));
      if (result != null && resolverConfiguration.shouldResolveWorkspaceProjects()) {
        addMissingProjectDependencies(state, pom, result);
      }
      try {
        // MNGECLIPSE-605 embedder is not able to resolve the project due to missing configuration in the parent
        Model model = maven.readModel(pom.getLocation().toFile());
        if (model != null && model.getParent() != null) {
          Parent parent = model.getParent();
          if (parent.getGroupId() != null && parent.getArtifactId() != null && parent.getVersion() != null) {
            ArtifactKey parentKey = new ArtifactKey(parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), null);
            state.addWorkspaceModule(pom, parentKey);
          }
        }
      } catch(Exception ex) {
        // we've tried our best, there is nothing else we can do
      }
      return;
    }

    ArtifactKey projectKey = new ArtifactKey(mavenProject.getArtifact());

    MavenProject oldMavenProject = null;
    ArtifactKey oldProjectKey = null;
    List<String> oldModules = new ArrayList<String>();
    if (oldFacade != null) {
      oldProjectKey = oldFacade.getArtifactKey();
      oldMavenProject = oldFacade.getMavenProject();
      oldModules = oldFacade.getMavenProjectModules();
    }

    boolean dependencyChanged = hasDependencyChange(oldMavenProject, mavenProject);

    // refresh modules
    if (resolverConfiguration.shouldIncludeModules()) {
      context.forcePomFiles(remove(state, getRemovedNestedModules(pom, oldModules, getMavenProjectModules(mavenProject)), true));
      context.forcePomFiles(refreshNestedModules(pom, mavenProject));
    } else {
      context.forcePomFiles(refreshWorkspaceModules(state, pom, oldProjectKey));
      context.forcePomFiles(refreshWorkspaceModules(state, pom, projectKey));
    }

    // refresh dependents
    if (dependencyChanged) {
      context.forcePomFiles(state.getDependents(pom, oldProjectKey, resolverConfiguration.shouldIncludeModules()));
      context.forcePomFiles(state.getDependents(pom, projectKey, resolverConfiguration.shouldIncludeModules()));
    }

    // cleanup old project and old dependencies
    state.removeProject(pom, oldProjectKey);

    // create new project and new dependencies
    MavenProjectFacade facade = new MavenProjectFacade(this, pom, mavenProject, resolverConfiguration);
    state.addProject(pom, facade);
    if (resolverConfiguration.shouldResolveWorkspaceProjects()) {
      addProjectDependencies(state, pom, mavenProject, true);
      addMissingProjectDependencies(state, pom, result);
    }
    if (resolverConfiguration.shouldIncludeModules()) {
      addProjectDependencies(state, pom, mavenProject, false);
    }
    MavenProject parentProject = mavenProject.getParent();
    if (parentProject != null) {
      state.addWorkspaceModule(pom, new ArtifactKey(mavenProject.getParentArtifact()));
      if (resolverConfiguration.shouldResolveWorkspaceProjects()) {
        state.addProjectDependency(pom, new ArtifactKey(mavenProject.getParentArtifact()), true);
      }
    }
  }

  private Set<IFile> refreshNestedModules(IFile pom, MavenProject mavenProject) {
    if (mavenProject == null) {
      return Collections.emptySet();
    }
    
    Set<IFile> pomSet = new LinkedHashSet<IFile>();
    for(String module : getMavenProjectModules(mavenProject)) {
      IFile modulePom = getModulePom(pom, module);
      if (modulePom != null) {
        pomSet.add(modulePom);
      }
    }
    return pomSet;
  }

  private Set<IFile> removeNestedModules(MutableProjectRegistry state, IFile pom, List<String> modules) {
    if (modules == null || modules.isEmpty()) {
      return Collections.emptySet();
    }

    Set<IFile> pomSet = new LinkedHashSet<IFile>();
    for(String module : modules) {
      IFile modulePom = getModulePom(pom, module);
      if (modulePom != null) {
        pomSet.addAll(remove(state, modulePom));
      }
    }
    return pomSet;
  }

  private List<String> getMavenProjectModules(MavenProject mavenProject) {
    return mavenProject == null ? new ArrayList<String>() : mavenProject.getModules();
  }

  /**
   * Returns Set<IFile> of nested module POMs that are present in oldMavenProject
   * but not in mavenProjec.
   */
  private Set<IFile> getRemovedNestedModules(IFile pom, List<String> oldModules, List<String> modules) {
    Set<IFile> result = new LinkedHashSet<IFile>();

    for(String oldModule : oldModules) {
      if (!modules.contains(oldModule)) {
        IFile modulePOM = getModulePom(pom, oldModule);
        if (modulePOM != null) {
          result.add(modulePOM);
        }
      }
    }

    return result;
  }

  public IFile getModulePom(IFile pom, String moduleName) {
    return pom.getParent().getFile(new Path(moduleName).append(IMavenConstants.POM_FILE_NAME));
  }

  private Set<IFile> refreshWorkspaceModules(MutableProjectRegistry state, IFile pom, ArtifactKey mavenProject) {
    if (mavenProject == null) {
      return Collections.emptySet();
    }

    Set<IFile> pomSet = new LinkedHashSet<IFile>();
    Set<IPath> modules = state.removeWorkspaceModules(pom, mavenProject);
    if (modules != null) {
      IWorkspaceRoot root = pom.getWorkspace().getRoot();
      for(IPath modulePath : modules) {
        IFile module = root.getFile(modulePath);
        if (module != null) {
          pomSet.add(module);
        }
      }
    }
    return pomSet;
  }

  private void addProjectDependencies(MutableProjectRegistry state, IFile pom, MavenProject mavenProject, boolean workspace) {
    for(Artifact artifact : mavenProject.getArtifacts()) {
      state.addProjectDependency(pom, new ArtifactKey(artifact), workspace);
    }
    for (Plugin plugin : mavenProject.getBuildPlugins()) {
      if (plugin.isExtensions()) {
        ArtifactKey artifactKey = new ArtifactKey(plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion(), null);
        state.addProjectDependency(pom, artifactKey, workspace);
      }
    }
  }

  private void addMissingProjectDependencies(MutableProjectRegistry state, IFile pom, MavenExecutionResult executionResult) {
    ArtifactResolutionResult resolutionResult = executionResult.getArtifactResolutionResult();
    if (resolutionResult != null && resolutionResult.getMissingArtifacts() != null) {
      for (Artifact artifact : resolutionResult.getMissingArtifacts()) {
        ArtifactKey dependencyKey = new ArtifactKey(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), null);
        state.addProjectDependency(pom, dependencyKey, true);
      }
    }
  }

  static boolean hasDependencyChange(MavenProject before, MavenProject after) {
    if(before == after) {
      // either same instance or both null
      return false;
    }

    if(before == null || after == null) {
      // one but not both null
      return true;
    }

    if(!ArtifactKey.equals(before.getArtifact(), after.getArtifact())) {
      // groupId/artifactId/version changed
      return true;
    }

    if(!ArtifactKey.equals(before.getParentArtifact(), after.getParentArtifact()) //
        || !(before.getParent() == null ? after.getParent() == null //
            : equals(before.getParent().getFile(), after.getParent().getFile()))) {
      return true;
    }

    if (before.getArtifacts().size() != after.getArtifacts().size()) {
      return true;
    }

    Iterator<Artifact> beforeIt = before.getArtifacts().iterator();
    Iterator<Artifact> afterIt = after.getArtifacts().iterator();
    while(beforeIt.hasNext()) {
      Artifact beforeDependency = beforeIt.next();
      Artifact afterDependency = afterIt.next();
      if(!ArtifactKey.equals(beforeDependency, afterDependency)
          || !equals(beforeDependency.getFile(), afterDependency.getFile())
          // scope change should trigger update event
          || !equals(beforeDependency.getScope(), afterDependency.getScope())
          || beforeDependency.isOptional() != afterDependency.isOptional()) {
        return true;
      }
    }

    return false;
  }

  private static boolean equals(Object o1, Object o2) {
    return o1 == null ? o2 == null : o1.equals(o2);
  }

  public void addMavenProjectChangedListener(IMavenProjectChangedListener listener) {
    synchronized (projectChangeListeners) {
      projectChangeListeners.add(listener);
    }
  }

  public void removeMavenProjectChangedListener(IMavenProjectChangedListener listener) {
    synchronized (projectChangeListeners) {
      projectChangeListeners.remove(listener);
    }
  }

  public void notifyProjectChangeListeners(List<MavenProjectChangedEvent> events, IProgressMonitor monitor) {
    if (events.size() > 0) {
      MavenProjectChangedEvent[] eventsArray = events.toArray(new MavenProjectChangedEvent[events.size()]);
      IMavenProjectChangedListener[] listeners;
      synchronized (this.projectChangeListeners) {
        listeners = this.projectChangeListeners.toArray(new IMavenProjectChangedListener[this.projectChangeListeners.size()]);
      }
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].mavenProjectChanged(eventsArray, monitor);
      }
    }
  }

  public MavenProjectFacade getMavenProject(String groupId, String artifactId, String version) {
    return projectRegistry.getProjectFacade(groupId, artifactId, version);
  }

  public void addManagedCache(IManagedCache cache) {
    caches.add(cache);
  }

  public void removeManagedCache(IManagedCache cache) {
    caches.remove(cache);
  }

  MavenExecutionResult readProjectWithDependencies(IFile pomFile, ResolverConfiguration resolverConfiguration,
      MavenUpdateRequest updateRequest, IProgressMonitor monitor) {
    return readProjectWithDependencies(projectRegistry, pomFile, resolverConfiguration, updateRequest, monitor);
  }
  private MavenExecutionResult readProjectWithDependencies(IProjectRegistry state, IFile pomFile, ResolverConfiguration resolverConfiguration,
      MavenUpdateRequest updateRequest, IProgressMonitor monitor) {

    try {
      MavenExecutionRequest request = createExecutionRequest(state, pomFile, resolverConfiguration, monitor);
      maven.populateDefaults(request);
      request.setOffline(updateRequest.isOffline());
      request.getProjectBuildingRequest().setResolveDependencies(true);
      return maven.readProject(request, monitor);
    } catch(CoreException ex) {
      DefaultMavenExecutionResult result = new DefaultMavenExecutionResult();
      result.addException(ex);
      return result;
    }

  }

  public IMavenProjectFacade[] getProjects() {
    return projectRegistry.getProjects();
  }

  public IMavenProjectFacade getProject(IProject project) {
    return projectRegistry.getProjectFacade(getPom(project));
  }

  public boolean setResolverConfiguration(IProject project, ResolverConfiguration configuration) {
    MavenProjectFacade projectFacade = create(project, new NullProgressMonitor());
    if(projectFacade!=null) {
      projectFacade.setResolverConfiguration(configuration);
    }
    return saveResolverConfiguration(project, configuration);
  }

  /**
   * Context
   */
  static class Context {
    final IProjectRegistry state;

    final ResolverConfiguration resolverConfiguration;

    final IFile pom;

    Context(IProjectRegistry state, ResolverConfiguration resolverConfiguration, IFile pom) {
      this.state = state;
      this.resolverConfiguration = resolverConfiguration;
      this.pom = pom;
    }
  }

  public MavenExecutionRequest createExecutionRequest(IFile pom, ResolverConfiguration resolverConfiguration, IProgressMonitor monitor) throws CoreException {
    return createExecutionRequest(projectRegistry, pom, resolverConfiguration, monitor);
  }

  private MavenExecutionRequest createExecutionRequest(IProjectRegistry state, IFile pom, ResolverConfiguration resolverConfiguration, IProgressMonitor monitor) throws CoreException {
    MavenExecutionRequest request = maven.createExecutionRequest(monitor);

    request.setPom(pom.getLocation().toFile());

    request.addActiveProfiles(resolverConfiguration.getActiveProfileList());

    // temporary solution for https://issues.sonatype.org/browse/MNGECLIPSE-1607
    Properties systemProperties = new Properties();
    systemProperties.putAll(System.getProperties());
    request.setSystemProperties(systemProperties);

    // eclipse workspace repository implements both workspace dependency resolution
    // and inter-module dependency resolution for multi-module projects.

    Context context = new Context(state, resolverConfiguration, pom);
    ArtifactRepository localRepository = maven.getLocalRepository();
    EclipseWorkspaceArtifactRepository workspaceRepsotory = new EclipseWorkspaceArtifactRepository(context);
    DelegatingLocalArtifactRepository repository = new DelegatingLocalArtifactRepository(localRepository);
    repository.setIdeWorkspace(workspaceRepsotory);
    request.setLocalRepository(repository);

    return request;
  }

  MutableProjectRegistry newMutableProjectRegistry() {
    return new MutableProjectRegistry(projectRegistry);
  }

  /**
   * Applies mutable project registry to the primary project registry and
   * and corresponding MavenProjectChangedEvent's to all registered 
   * IMavenProjectChangedListener's.
   * 
   * This method must be called from a thread holding workspace root's lock.
   * 
   * @throws StaleMutableProjectRegistryException if primary project registry
   *    was modified after mutable registry has been created
   */
  void applyMutableProjectRegistry(MutableProjectRegistry newState, IProgressMonitor monitor) {
    List<MavenProjectChangedEvent> events = projectRegistry.apply(newState);
    stateReader.writeWorkspaceState(projectRegistry);
    notifyProjectChangeListeners(events, monitor);
  }

}
