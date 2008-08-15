/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.osgi.service.prefs.BackingStoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.ComponentDescriptor;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.embedder.ContainerCustomizer;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.internal.embedder.TransferListenerAdapter;
import org.maven.ide.eclipse.project.DownloadSourceEvent;
import org.maven.ide.eclipse.project.IDownloadSourceListener;
import org.maven.ide.eclipse.project.IMavenProjectChangedListener;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.IMavenProjectVisitor2;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.MavenRunnable;
import org.maven.ide.eclipse.project.MavenUpdateRequest;
import org.maven.ide.eclipse.project.ResolverConfiguration;

/**
 * This class keeps track of all maven projects present in the workspace and
 * provides mapping between Maven and the workspace.
 * 
 * XXX materializeArtifactPath does not belong here
 * XXX downloadSources does not belong here
 */
public class MavenProjectManagerImpl {

  public static boolean DEBUG = MavenPlugin.getDefault().isDebugging()
      & Boolean.parseBoolean(Platform.getDebugOption(IMavenConstants.PLUGIN_ID + "/debug/projectManager"));

  static final String ARTIFACT_TYPE_POM = "pom";
  static final String ARTIFACT_TYPE_JAR = "jar";
  public static final String ARTIFACT_TYPE_JAVA_SOURCE = "java-source";
  public static final String ARTIFACT_TYPE_JAVADOC = "javadoc";

  static final String CLASSIFIER_SOURCES = "sources";
  static final String CLASSIFIER_JAVADOC = "javadoc";
  static final String CLASSIFIER_TESTS = "tests";
  static final String CLASSIFIER_TESTSOURCES = "test-sources";

  private static final String P_VERSION = "version";
  private static final String P_INCLUDE_MODULES = "includeModules";
  private static final String P_RESOLVE_WORKSPACE_PROJECTS = "resolveWorkspaceProjects";
  private static final String P_RESOURCE_FILTER_GOALS = "resourceFilterGoals";
  private static final String P_FULL_BUILD_GOALS = "fullBuildGoals";
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

  private static final ThreadLocal<Context> context = new ThreadLocal<Context>();
  
  static Context getContext() {
    return context.get();
  }

  private final WorkspaceState state;

  private final MavenConsole console;
  private final IndexManager indexManager;
  private final MavenEmbedderManager embedderManager;

  private final MavenMarkerManager markerManager;

  private final WorkspaceStateReader stateReader;

  private final Set<IMavenProjectChangedListener> projectChangeListeners = new LinkedHashSet<IMavenProjectChangedListener>();
  private final Map<IFile, MavenProjectChangedEvent> projectChangeEvents = new LinkedHashMap<IFile, MavenProjectChangedEvent>();

  private final Set<IDownloadSourceListener> downloadSourceListeners = new LinkedHashSet<IDownloadSourceListener>();
  private final Map<IProject, DownloadSourceEvent> downloadSourceEvents = new LinkedHashMap<IProject, DownloadSourceEvent>();

  public MavenProjectManagerImpl(MavenConsole console, IndexManager indexManager, MavenEmbedderManager embedderManager,
      File stateLocationDir, boolean readState) {
    this.console = console;
    this.indexManager = indexManager;
    this.embedderManager = embedderManager;
    this.markerManager = new MavenMarkerManager(console);

    this.stateReader = new WorkspaceStateReader(stateLocationDir);

    WorkspaceState state = readState && stateReader != null ? stateReader.readWorkspaceState(this) : null;
    this.state = state == null ? new WorkspaceState() : state;

    for (MavenProjectFacade facade : this.state.getProjects()) {
      addToIndex(facade);
    }
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
    MavenProjectFacade projectFacade = state.getProjectFacade(pom);
    if(projectFacade == null && load) {
      ResolverConfiguration configuration = readResolverConfiguration(pom.getProject());

      try {
        MavenExecutionResult executionResult = readProjectWithDependencies(pom, configuration, //
            new MavenUpdateRequest(true /* offline */, false /* updateSnapshots */),
            monitor);
        MavenProject mavenProject = executionResult.getProject();
        if(mavenProject != null) {
          projectFacade = new MavenProjectFacade(this, pom, mavenProject, configuration);
        } else {
          @SuppressWarnings("unchecked")
          List<Exception> exceptions = executionResult.getExceptions();
          if (exceptions != null) {
            for(Exception ex : exceptions) {
              String msg = "Failed to create Maven embedder";
              console.logError(msg + "; " + ex.toString());
              MavenLogger.log(msg, ex);
            }
          }
        }
      } catch(MavenEmbedderException ex) {
        String msg = "Failed to create Maven embedder";
        console.logError(msg + "; " + ex.toString());
        MavenLogger.log(msg, ex);
      }
    }
    return projectFacade;
  }

  public boolean saveResolverConfiguration(IProject project, ResolverConfiguration configuration) {
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(IMavenConstants.PLUGIN_ID);
    if(projectNode != null) {
      projectNode.put(P_VERSION, VERSION);
      
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
      return LegacyBuildPathManager.getResolverConfiguration(project);
    }
  
    ResolverConfiguration configuration = new ResolverConfiguration();
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
  public Set<IFile> remove(Set<IFile> poms, boolean force) {
    Set<IFile> pomSet = new LinkedHashSet<IFile>();
    for (Iterator<IFile> it = poms.iterator(); it.hasNext(); ) {
      IFile pom = it.next();
      MavenProjectFacade facade = state.getProjectFacade(pom);
      if (force || facade == null || facade.isStale()) {
        pomSet.addAll(remove(pom));
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
  public Set<IFile> remove(IFile pom) {
    MavenProjectFacade facade = state.getProjectFacade(pom);
    ArtifactKey mavenProject = facade != null ? facade.getArtifactKey() : null;

    if (mavenProject == null) {
      state.removePom(pom);
      return Collections.emptySet();
    }

    Set<IFile> pomSet = new LinkedHashSet<IFile>();

    pomSet.addAll(state.getDependents(pom, mavenProject, false));
    state.removeProject(pom, mavenProject);
    removeFromIndex(facade);
    addProjectChangeEvent(pom, MavenProjectChangedEvent.KIND_REMOVED, MavenProjectChangedEvent.FLAG_NONE, facade, null);

    // XXX this will likely NOT work for closed/removed projects, need to move to IResourceChangeEventListener
    if(facade!=null) {
      ResolverConfiguration resolverConfiguration = facade.getResolverConfiguration();
      if (resolverConfiguration != null && resolverConfiguration.shouldIncludeModules()) {
        pomSet.addAll(removeNestedModules(pom, facade.getMavenProjectModules()));
      }
    }

    pomSet.addAll(refreshWorkspaceModules(pom, mavenProject));

    pomSet.remove(pom);
    
    return pomSet;
  }

  /**
   * @param updateRequests a set of {@link MavenUpdateRequest}
   * @param monitor progress monitor
   */
  public void refresh(Set<DependencyResolutionContext> updateRequests, IProgressMonitor monitor) throws CoreException, MavenEmbedderException, InterruptedException {
    MavenEmbedder embedder = createWorkspaceEmbedder();
    try {
      for(DependencyResolutionContext updateRequest : updateRequests) {
        while(!updateRequest.isEmpty()) {
          if(monitor.isCanceled()) {
            throw new InterruptedException();
          }

          IFile pom = updateRequest.pop();
          monitor.subTask(pom.getFullPath().toString());
          
          if (!pom.isAccessible() || !pom.getProject().hasNature(IMavenConstants.NATURE_ID)) {
            updateRequest.forcePomFiles(remove(pom));
            continue;
          }

          refresh(embedder, pom, updateRequest, monitor);
          monitor.worked(1);
        }
      }
    } finally {
      embedder.stop();
    }
    
    notifyProjectChangeListeners(monitor);
  }

  public void refresh(MavenEmbedder embedder, IFile pom, DependencyResolutionContext updateRequest, IProgressMonitor monitor) throws CoreException {
    MavenProjectFacade oldFacade = state.getProjectFacade(pom);

    if(!updateRequest.isForce(pom) && oldFacade != null && !oldFacade.isStale()) {
      // skip refresh if not forced and up-to-date facade
      return;
    }

    markerManager.deleteMarkers(pom);

    ResolverConfiguration resolverConfiguration = readResolverConfiguration(pom.getProject());

    MavenProject mavenProject = null;
    MavenExecutionResult result = null;
    if (pom.isAccessible()) {
      result = execute(embedder, pom, resolverConfiguration, new MavenProjectReader(updateRequest.getRequest()), monitor);
      mavenProject = result.getProject();
      markerManager.addMarkers(pom, result);
    }

    if (mavenProject == null) {
      updateRequest.forcePomFiles(remove(pom));
      if (result != null && resolverConfiguration.shouldResolveWorkspaceProjects()) {
        // this only really add missing parent
        addMissingProjectDependencies(pom, result);
      }
      try {
        // MNGECLIPSE-605 embedder is not able to resolve the project due to missing configuration in the parent
        Model model = embedder.readModel(pom.getLocation().toFile());
        if (model != null && model.getParent() != null) {
          Parent parent = model.getParent();
          if (parent.getGroupId() != null && parent.getArtifactId() != null && parent.getVersion() != null) {
            ArtifactKey parentKey = new ArtifactKey(parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), null);
            state.addWorkspaceModule(pom, parentKey);
          }
        }
      } catch(Exception ex) {
        // we've tried out best, but there is nothing we can do
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
      updateRequest.forcePomFiles(remove(getRemovedNestedModules(pom, oldModules, getMavenProjectModules(mavenProject)), true));
      updateRequest.forcePomFiles(refreshNestedModules(pom, mavenProject));
    } else {
      updateRequest.forcePomFiles(refreshWorkspaceModules(pom, oldProjectKey));
      updateRequest.forcePomFiles(refreshWorkspaceModules(pom, projectKey));
    }

    // refresh dependents
    if (dependencyChanged) {
      updateRequest.forcePomFiles(state.getDependents(pom, oldProjectKey, resolverConfiguration.shouldIncludeModules()));
      updateRequest.forcePomFiles(state.getDependents(pom, projectKey, resolverConfiguration.shouldIncludeModules()));
    }

    // cleanup old project and old dependencies
    state.removeProject(pom, oldProjectKey);

    // create new project and new dependencies
    MavenProjectFacade facade = new MavenProjectFacade(this, pom, mavenProject, resolverConfiguration);
    state.addProject(pom, facade);
    if (resolverConfiguration.shouldResolveWorkspaceProjects()) {
      addProjectDependencies(pom, mavenProject, true);
    }
    if (resolverConfiguration.shouldIncludeModules()) {
      addProjectDependencies(pom, mavenProject, false);
    }
    MavenProject parentProject = mavenProject.getParent();
    if (parentProject != null) {
      state.addWorkspaceModule(pom, mavenProject);
      if (resolverConfiguration.shouldResolveWorkspaceProjects()) {
        state.addProjectDependency(pom, new ArtifactKey(mavenProject.getParentArtifact()), true);
      }
    }

    // send appropriate event
    int kind;
    int flags = MavenProjectChangedEvent.FLAG_NONE;
    if (oldFacade == null) {
      kind = MavenProjectChangedEvent.KIND_ADDED;
    } else {
      kind = MavenProjectChangedEvent.KIND_CHANGED;
    }
    if (dependencyChanged) {
      flags |= MavenProjectChangedEvent.FLAG_DEPENDENCIES;
    }
    addProjectChangeEvent(pom, kind, flags, oldFacade, facade);

    // update index
    removeFromIndex(oldFacade);
    addToIndex(facade);
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

  private Set<IFile> removeNestedModules(IFile pom, List<String> modules) {
    if (modules == null || modules.isEmpty()) {
      return Collections.emptySet();
    }

    Set<IFile> pomSet = new LinkedHashSet<IFile>();
    for(String module : modules) {
      IFile modulePom = getModulePom(pom, module);
      if (modulePom != null) {
        pomSet.addAll(remove(modulePom));
      }
    }
    return pomSet;
  }

  @SuppressWarnings("unchecked")
  private List<String> getMavenProjectModules(MavenProject mavenProject) {
    return mavenProject == null ? Collections.emptyList() : mavenProject.getModules();
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

  private Set<IFile> refreshWorkspaceModules(IFile pom, ArtifactKey mavenProject) {
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

  @SuppressWarnings("unchecked")
  private void addProjectDependencies(IFile pom, MavenProject mavenProject, boolean workspace) {
    for(Artifact artifact : (Set<Artifact>) mavenProject.getArtifacts()) {
      state.addProjectDependency(pom, new ArtifactKey(artifact), workspace);
    }
    for (Plugin plugin : (List<Plugin>) mavenProject.getBuildPlugins()) {
      if (plugin.isExtensions()) {
        ArtifactKey artifactKey = new ArtifactKey(plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion(), null);
        state.addProjectDependency(pom, artifactKey, workspace);
      }
    }
  }

  private void addToIndex(MavenProjectFacade mavenProject) {
    if (mavenProject != null) {
      indexManager.addDocument(IndexManager.WORKSPACE_INDEX, mavenProject.getPomFile(), //
          indexManager.getDocumentKey(mavenProject.getArtifactKey()), -1, -1, null, IndexManager.NOT_PRESENT, IndexManager.NOT_PRESENT);
    }
  }
  
  private void removeFromIndex(MavenProjectFacade mavenProject) {
    if (mavenProject != null) {
      indexManager.removeDocument(IndexManager.WORKSPACE_INDEX, //
          mavenProject.getPomFile(), indexManager.getDocumentKey(mavenProject.getArtifactKey()));
    }
  }

  private void addProjectChangeEvent(IFile pom, int kind, int flags, MavenProjectFacade oldMavenProject, MavenProjectFacade mavenProject) {
    MavenProjectChangedEvent event = projectChangeEvents.get(pom);
    if (event == null) {
      event = new MavenProjectChangedEvent(pom, kind, flags, oldMavenProject, mavenProject);
      projectChangeEvents.put(pom, event);
      return;
    }

    // merge events
    IMavenProjectFacade old = event.getOldMavenProject() != null? event.getOldMavenProject(): oldMavenProject;
    int mergedKind;
    if (MavenProjectChangedEvent.KIND_REMOVED == kind) {
      mergedKind = MavenProjectChangedEvent.KIND_REMOVED;
    } else if (event.getKind() == kind) {
      mergedKind = kind;
    } else {
      mergedKind = MavenProjectChangedEvent.KIND_CHANGED;
    }
    event = new MavenProjectChangedEvent(pom, mergedKind, event.getFlags() | flags, old, mavenProject);
    projectChangeEvents.put(pom, event);
  }

  private void addMissingProjectDependencies(IFile pom, MavenExecutionResult result) {
    // kinda hack, but this is the only way I can get info about missing parent artifacts
    @SuppressWarnings("unchecked")
    List<Throwable> exceptions = result.getExceptions();
    if (exceptions != null) {
      for(Throwable t : exceptions) {
        AbstractArtifactResolutionException re = null;
        while (t != null) {
          if(t instanceof AbstractArtifactResolutionException) {
            re = (AbstractArtifactResolutionException) t;
            break;
          }
          t = t.getCause();
        }
        if(re != null) {
          ArtifactKey dependencyKey = new ArtifactKey(re.getGroupId(), re.getArtifactId(), re.getVersion(), null);
          state.addProjectDependency(pom, dependencyKey, true);
        }
      }
    }
  }

  private static boolean hasDependencyChange(MavenProject before, MavenProject after) {
    if (before == after) {
      // either same instance or both null
      return false;
    }

    if (before == null || after == null) {
      // one but not both null
      return true;
    }

    if (!ArtifactKey.equals(before.getArtifact(), after.getArtifact())) {
      // groupId/artifactId/version changed
      return true;
    }

    if (!ArtifactKey.equals(before.getParentArtifact(), after.getParentArtifact())
          || !(before.getParent() == null? after.getParent() == null: equals(before.getParent().getFile(), after.getParent().getFile()))) 
    {
      return true;
    }

    if (before.getArtifacts().size() != after.getArtifacts().size()) {
      return true;
    }

    @SuppressWarnings("unchecked")
    Iterator<Artifact> beforeIt = before.getArtifacts().iterator();
    @SuppressWarnings("unchecked")
    Iterator<Artifact> afterIt = after.getArtifacts().iterator();
    while (beforeIt.hasNext()) {
      Artifact beforeDependeny = beforeIt.next();
      Artifact afterDependeny = afterIt.next();
      if (!ArtifactKey.equals(beforeDependeny, afterDependeny)
            || !equals(beforeDependeny.getFile(), afterDependeny.getFile())
            || beforeDependeny.isOptional() != afterDependeny.isOptional()) 
      {
        return true;
      }
    }

    return false;
  }

  private static boolean equals(Object o1, Object o2) {
    return o1 == null? o2 == null: o1.equals(o2);
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

  public void notifyProjectChangeListeners(IProgressMonitor monitor) {
    if (projectChangeEvents.size() > 0) {
      stateReader.writeWorkspaceState(state); // TODO create a listener

      Collection<MavenProjectChangedEvent> eventCollection = this.projectChangeEvents.values();
      MavenProjectChangedEvent[] events = eventCollection.toArray(new MavenProjectChangedEvent[eventCollection.size()]);
      IMavenProjectChangedListener[] listeners;
      synchronized (this.projectChangeListeners) {
        listeners = this.projectChangeListeners.toArray(new IMavenProjectChangedListener[this.projectChangeListeners.size()]);
      }
      this.projectChangeEvents.clear();
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].mavenProjectChanged(events, monitor);
      }
    }
  }

  public MavenProjectFacade getMavenProject(String groupId, String artifactId, String version) {
    return state.getMavenProject(groupId, artifactId, version);
  }

  public void downloadSources(List<DownloadRequest> downloadRequests, IProgressMonitor monitor) throws MavenEmbedderException, InterruptedException, CoreException {
    MavenEmbedder embedder = createWorkspaceEmbedder();
    try {
      for(DownloadRequest request : downloadRequests) {
        if(monitor.isCanceled()) {
          throw new InterruptedException();
        }

        final ArtifactKey key = request.getArtifactKey();
        MavenProjectFacade projectFacade = create(request.getProject(), monitor);

        Artifact artifact = null;
        List<ArtifactRepository> remoteRepositories = null;

        if(projectFacade != null) {
          // for maven projects find actual artifact and MavenProject corresponding to the artifactKey

          // XXX ugly, need to find a better way
          class MavenProjectVisitor implements IMavenProjectVisitor2 {
            IMavenProjectFacade mavenProject = null;
            Artifact artifact = null;

            public boolean visit(IMavenProjectFacade mavenProject) {
              return this.mavenProject == null;
            }

            public void visit(IMavenProjectFacade mavenProject, Artifact artifact) {
              ArtifactKey otherKey = new ArtifactKey(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getClassifier());
              if(key.equals(otherKey)) {
                this.mavenProject = mavenProject;
                this.artifact = artifact;
              }
            }
          };

          MavenProjectVisitor pv = new MavenProjectVisitor(); 
          projectFacade.accept(pv, IMavenProjectVisitor.NESTED_MODULES, monitor);

          artifact = pv.artifact;

          IMavenProjectFacade facade = pv.mavenProject;
          if (facade != null) {
            @SuppressWarnings("unchecked")
            List<ArtifactRepository> mavenProjectRepositoreis = facade.getMavenProject(monitor).getRemoteArtifactRepositories();
            remoteRepositories = mavenProjectRepositoreis;
          }
        }
        
        if(remoteRepositories == null) {
          remoteRepositories = indexManager.getArtifactRepositories(null, null);
        }

        if(artifact == null) {
          // not a Maven managed artifact
          artifact = embedder.createArtifact(key.getGroupId(), key.getArtifactId(), key.getVersion(), null,
              ARTIFACT_TYPE_POM);
          try {
            embedder.resolve(artifact, remoteRepositories, embedder.getLocalRepository());
          } catch(Exception ex) {
            MavenLogger.log("Can not resolve artifact for classpath entry of non-maven project", ex);
            continue;
          }
        }

        if (artifact != null && remoteRepositories != null) {
          monitor.subTask(artifact.getId());
          IPath srcPath = null;
          if(request.isDownloadSources()) {
            srcPath = materializeArtifactPath(embedder, remoteRepositories, artifact, //
                ARTIFACT_TYPE_JAVA_SOURCE, monitor);
          }
          
          String javadocUrl = null;
          if(request.isDownloadJavaDoc()) {
            IPath javadocPath = materializeArtifactPath(embedder, remoteRepositories, artifact, //
                ARTIFACT_TYPE_JAVADOC, monitor);
            if (javadocPath != null) {
              javadocUrl = getJavaDocUrl(javadocPath.toString());
            } else {
              // guess the javadoc url from the project url in the artifact's pom.xml
              String artifactLocation = artifact.getFile().getAbsolutePath();
              File file = new File(artifactLocation.substring(0, artifactLocation.length() - 4) + ".pom");
              if(file.exists()) {
                try {
                  MavenProject mavenProject = embedder.readProject(file);
                  String url = mavenProject.getUrl();
                  if(url != null) {
                    url = url.trim();
                    if(url.length() > 0) {
                      if(!url.endsWith("/"))
                        url += "/";
                      javadocUrl =  url + "apidocs/"; // assuming project is using maven-generated site
                    }
                  }
                } catch(Exception ex) {
                  MavenLogger.log("Can't read Maven project from " + file, ex);
                }
              }
            }
          }
          
          if(srcPath != null || javadocUrl != null) {
            addDownloadSourceEvent(request.getProject(), request.getPath(), srcPath, javadocUrl);
          }
        }
      }
    } finally {
      embedder.stop();
    }
    
    notifyDownloadSourceListeners(monitor);
  }

  private void notifyDownloadSourceListeners(IProgressMonitor monitor) {
    if (downloadSourceEvents.size() > 0) {
      IDownloadSourceListener[] listeners;
      synchronized (this.downloadSourceListeners) {
        listeners = this.downloadSourceListeners.toArray(new IDownloadSourceListener[this.downloadSourceListeners.size()]);
      }
      for (int i = 0; i < listeners.length; i++) {
        for (Iterator<DownloadSourceEvent> eventsIter = downloadSourceEvents.values().iterator(); eventsIter.hasNext(); ) {
          DownloadSourceEvent event = eventsIter.next();
          try {
            listeners[i].sourcesDownloaded(event, monitor);
          } catch(CoreException ex) {
            MavenLogger.log(ex);
          }
        }
      }
      this.downloadSourceEvents.clear();
    }
  }

  public void addDownloadSourceListener(IDownloadSourceListener listener) {
    synchronized (downloadSourceListeners) {
      downloadSourceListeners.add(listener);
    }
  }

  public void removeDownloadSourceListener(IDownloadSourceListener listener) {
    synchronized (downloadSourceListeners) {
      downloadSourceListeners.remove(listener);
    }
  }

  private void addDownloadSourceEvent(IProject project, IPath path, IPath srcPath, String javadocUrl) {
    downloadSourceEvents.put(project, new DownloadSourceEvent(project, path, srcPath, javadocUrl));
  }

  public String getClassifier(String type, String classifier) {
    if (ARTIFACT_TYPE_JAVADOC.equals(type)) {
      return CLASSIFIER_JAVADOC;
    } else if (ARTIFACT_TYPE_JAVA_SOURCE.equals(type)) {
      if (CLASSIFIER_TESTS.equals(classifier)) {
        return CLASSIFIER_TESTSOURCES;
      }
      return CLASSIFIER_SOURCES;
    } else {
      // can't really happen
      return null;
    }
  }

  private IPath materializeArtifactPath(MavenEmbedder embedder, List<ArtifactRepository> remoteRepositories,
      Artifact base, String type, IProgressMonitor monitor) {
    File baseFile = base.getFile();
    if(baseFile == null) {
      console.logError("Missing artifact file for " + base.getId());
      return null;
    }

    boolean isJavaSource;
    boolean isJavaDoc;

    String classifier = getClassifier(type, base.getClassifier());

    if (ARTIFACT_TYPE_JAVADOC.equals(type)) {
      isJavaDoc = true;
      isJavaSource = false;
    } else if (ARTIFACT_TYPE_JAVA_SOURCE.equals(type)) {
      isJavaDoc = false;
      isJavaSource = true;
    } else {
      // can't really happen
      return null;
    }

    Artifact artifact = embedder.createArtifactWithClassifier(base.getGroupId(), //
        base.getArtifactId(), base.getVersion(), type, classifier);

//    File artifactFile = new File(baseFile.getParentFile(), artifact.getArtifactId()+ "-" + artifact.getVersion() + "-" + classifier + ".jar");
//
//    IPath path = getArtifactPath(base, type, classifier);
//    if (path != null) {
//      return null;
//    }

    monitor.beginTask("Resolving " + type + " " + base.getId(), IProgressMonitor.UNKNOWN);

    IndexedArtifactFile af = null;
    if(isJavaSource || isJavaDoc) {
      try {
        af = indexManager.getIndexedArtifactFile(IndexManager.LOCAL_INDEX, indexManager.getDocumentKey(new ArtifactKey(base)));
        if(isJavaSource && af != null && af.sourcesExists == IndexManager.NOT_AVAILABLE) {
          return null; // sources are not available in any remote repository
        }
        if(isJavaDoc && af != null && af.javadocExists == IndexManager.NOT_AVAILABLE) {
          return null; // javadoc is not available in any remote repository
        }
      } catch(Exception ex) {
        // XXX lets hide all indexer exceptions for now
        String msg = ex.getMessage()==null ? ex.toString() : ex.getMessage();
        console.logError("Error: " + msg);
        MavenLogger.log(msg, ex);
      }
    }

    try {
      if(artifact != null) {
        // TODO can optimize remote repositories using index info
        embedder.resolve(artifact, remoteRepositories, embedder.getLocalRepository());

        updateIndex(IndexManager.PRESENT, isJavaSource, isJavaDoc, af, baseFile, base);

        return new Path(artifact.getFile().getAbsolutePath());
      }
    } catch(AbstractArtifactResolutionException ex) {
      String name = ex.getGroupId() + ':' + ex.getArtifactId() + ':' + ex.getVersion();
      console.logError("Can't download " + type + " for artifact " + name);
      if(!isJavaSource && !isJavaDoc) {
        String msg = ex.getOriginalMessage()==null ? ex.toString() : ex.getOriginalMessage();
        console.logError("Error: " + msg);
        MavenLogger.log(msg, ex);
      }

      updateIndex(IndexManager.NOT_AVAILABLE, isJavaSource, isJavaDoc, af, baseFile, base);

    } finally {
      monitor.done();
    }

    return null;
  }

  private void updateIndex(int exist, boolean isJavaSource, boolean isJavaDoc, //
      IndexedArtifactFile af, File artifactFile, Artifact artifact) {
    if(isJavaSource || isJavaDoc) {
      int sourcesExists = isJavaSource ? exist : (af != null ? af.sourcesExists : IndexManager.NOT_PRESENT);
      int javadocExists = isJavaDoc ? exist : (af != null ? af.javadocExists : IndexManager.NOT_PRESENT);
      
      // XXX add test to make sure update don't erase class names
      indexManager.addDocument(IndexManager.LOCAL_INDEX, null, indexManager.getDocumentKey(new ArtifactKey(artifact)), //
          artifactFile.length(), artifactFile.lastModified(), artifactFile, sourcesExists, javadocExists);
    }
  }

  public MavenExecutionResult readProjectWithDependencies(IFile pomFile, ResolverConfiguration resolverConfiguration, MavenUpdateRequest updateRequest, IProgressMonitor monitor) throws MavenEmbedderException {
    MavenEmbedder embedder = createWorkspaceEmbedder();
    try {
      return execute(embedder, pomFile, resolverConfiguration, new MavenProjectReader(updateRequest), monitor);
    } finally {
      try {
        embedder.stop();
      } catch(MavenEmbedderException ex) {
        MavenLogger.log("Failed to stop Maven embedder", ex);
      }
    }
  }
  
  public MavenExecutionResult execute(MavenEmbedder embedder, File pom, ResolverConfiguration resolverConfiguration, MavenRunnable runnable, IProgressMonitor monitor) {
    MavenExecutionRequest request = embedderManager.createRequest(embedder);
    request.setPomFile(pom.getAbsolutePath());
    request.setBaseDirectory(pom.getParentFile());
    request.setTransferListener(new TransferListenerAdapter(monitor, console, indexManager));
    request.setProfiles(resolverConfiguration.getActiveProfileList());
    request.addActiveProfiles(resolverConfiguration.getActiveProfileList());
    request.setRecursive(false);
    request.setUseReactor(false);

    return runnable.execute(embedder, request);
  }
  
  public MavenExecutionResult execute(MavenEmbedder embedder, IFile pomFile, ResolverConfiguration resolverConfiguration, MavenRunnable runnable, IProgressMonitor monitor) {
    File pom = pomFile.getLocation().toFile();

    MavenExecutionRequest request = embedderManager.createRequest(embedder);
    request.setPomFile(pom.getAbsolutePath());
    request.setBaseDirectory(pom.getParentFile());
    request.setTransferListener(new TransferListenerAdapter(monitor, console, indexManager));
    request.setProfiles(resolverConfiguration.getActiveProfileList());
    request.addActiveProfiles(resolverConfiguration.getActiveProfileList());
    request.setRecursive(false);
    request.setUseReactor(false);

    context.set(new Context(this.state, resolverConfiguration, pomFile));
    try {
      return runnable.execute(embedder, request);
    } finally {
      context.set(null);
    }
  }

  public IMavenProjectFacade[] getProjects() {
    return state.getProjects();
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
    final WorkspaceState state;

    final ResolverConfiguration resolverConfiguration;

    final IFile pom;

    Context(WorkspaceState state, ResolverConfiguration resolverConfiguration, IFile pom) {
      this.state = state;
      this.resolverConfiguration = resolverConfiguration;
      this.pom = pom;
    }
  }

  private static class MavenExecutor implements MavenRunnable {
    private final List<String> goals;

    MavenExecutor(List<String> goals) {
      this.goals = goals;
    }

    public MavenExecutionResult execute(MavenEmbedder embedder, MavenExecutionRequest request) {
      request.setGoals(goals);
      return embedder.execute(request);
    }
  }

  private static final class MavenProjectReader implements MavenRunnable {
    private final MavenUpdateRequest updateRequest;

    MavenProjectReader(MavenUpdateRequest updateRequest) {
      this.updateRequest = updateRequest;
    }

    public MavenExecutionResult execute(MavenEmbedder embedder, MavenExecutionRequest request) {
      request.setOffline(updateRequest.isOffline());
      request.setUpdateSnapshots(updateRequest.isUpdateSnapshots());
      return embedder.readProjectWithDependencies(request);
    }
  }

  public MavenExecutionResult execute(MavenProjectFacade facade, List<String> goals, final boolean recursive, IProgressMonitor monitor) throws MavenEmbedderException {
    MavenEmbedder embedder = createWorkspaceEmbedder();
    try {
      IFile pom = facade.getPom();
      final ResolverConfiguration resolverConfiguration = facade.getResolverConfiguration();
      
      if(DEBUG) {
        System.out.println("  executing Maven for " + facade.getProject()  //$NON-NLS-1$
            + " goals:" + goals.toString() 
            + " recursive:" + recursive);  //$NON-NLS-1$
      }
      
      return execute(embedder, pom, resolverConfiguration, new MavenExecutor(goals) {
        public MavenExecutionResult execute(MavenEmbedder embedder, MavenExecutionRequest request) {
          request.setRecursive(recursive);
          return super.execute(embedder, request);
        }
      }, monitor);

    } finally {
      embedder.stop();
    }
  }

  public String getJavaDocUrl(String fileName) {
    try {
      URL fileUrl = new File(fileName).toURL();
      return "jar:" + fileUrl.toExternalForm() + "!/" + getJavaDocPathInArchive(fileName);
    } catch(MalformedURLException ex) {
      return null;
    }
  }

  private String getJavaDocPathInArchive(String name) {
    long l1 = System.currentTimeMillis();
    ZipFile jarFile = null;
    try {
      jarFile = new ZipFile(name);
      String marker = "package-list";
      for(Enumeration<? extends ZipEntry> en = jarFile.entries(); en.hasMoreElements();) {
        ZipEntry entry = en.nextElement();
        String entryName = entry.getName();
        if(entryName.endsWith(marker)) {
          return entry.getName().substring(0, entryName.length()-marker.length());
        }
      }
    } catch(IOException ex) {
      // ignore
    } finally {
      long l2 = System.currentTimeMillis();
      console.logMessage("Scanned javadoc " + name + " " + (l2-l1)/1000f);
      try {
        if(jarFile!=null) jarFile.close();
      } catch(IOException ex) {
        //
      }
    }
    
    return "";
  }

  public MavenEmbedder createWorkspaceEmbedder() throws MavenEmbedderException {
    return embedderManager.createEmbedder(createWorkspaceCustomizer());
  }

  private ContainerCustomizer createWorkspaceCustomizer() {
    return new ContainerCustomizer() {
      public void customize(PlexusContainer container) {
        ComponentDescriptor resolverDescriptor = container.getComponentDescriptor(ArtifactResolver.ROLE);
        resolverDescriptor.setImplementation(EclipseArtifactResolver.class.getName());

        // desc = plexusContainer.getComponentDescriptor(ArtifactFactory.ROLE);
        // desc.setImplementation(org.maven.ide.eclipse.embedder.EclipseArtifactFactory.class.getName());

        // Used for building hierarchy of dependencies
        // desc = container.getComponentDescriptor(ResolutionListener.ROLE);
        // if(desc == null) {
        //   desc = new ComponentDescriptor();
        //   desc.setRole(ResolutionListener.ROLE);
        //   container.addComponentDescriptor(desc);
        // }
        // desc.setImplementation(EclipseResolutionListener.class.getName());

        // Custom artifact resolver for resolving artifacts from Eclipse Worspace
//        if(resolveWorkspaceProjects) {
//          ComponentDescriptor resolverDescriptor = container.getComponentDescriptor(ArtifactResolver.ROLE);
//          // ComponentRequirement requirement = new ComponentRequirement();
//          // requirement.setRole(ResolutionListener.ROLE);
//          // desc.addRequirement(requirement);
//          resolverDescriptor.setImplementation(EclipseArtifactResolver.class.getName());
//        }
        
//          desc = container.getComponentDescriptor(WagonManager.ROLE);
//          desc.setImplementation(EclipseWagonManager.class.getName());
      }
    };
  }
  
}
