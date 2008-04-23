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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.prefs.BackingStoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.embedder.ContainerCustomizer;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.path.PathTranslator;

import org.maven.ide.eclipse.MavenConsole;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.container.MavenClasspathContainer;
import org.maven.ide.eclipse.embedder.EmbedderFactory;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.internal.embedder.TransferListenerAdapter;
import org.maven.ide.eclipse.project.BuildPathManager;
import org.maven.ide.eclipse.project.DownloadSourceEvent;
import org.maven.ide.eclipse.project.IDownloadSourceListener;
import org.maven.ide.eclipse.project.IMavenProjectChangedListener;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.ResolverConfiguration;

/**
 * This class keeps track of all maven projects present in the workspace and
 * provides mapping between Maven and the workspace.
 * 
 * XXX materializeArtifactPath does not belong here
 * XXX downloadSources does not belong here
 */
public class MavenProjectManagerImpl {

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
  private static final String P_USE_MAVEN_FOLDERS = "useMavenFolders";
  private static final String P_FILTER_RESOURCES = "filterResources";
  private static final String P_RESOURCE_FILTER_GOALS = "resourceFilterGoals";
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
  public static final IPath[] METADATA_PATH = {
    new Path(".project"),
    new Path(".classpath"),
    new Path(".settings/org.maven.ide.eclipse.prefs") // dirty hack!
  };

  private final WorkspaceState state;

  private final MavenConsole console;
  private final IndexManager indexManager;
  private final MavenEmbedderManager embedderManager;
  private final MavenRuntimeManager runtimeManager;

  private final MavenMarkerManager markerManager;
  
  private final Set/*<IMavenProjectChangedListener>*/ projectChangeListeners = new LinkedHashSet/*<IMavenProjectChangedListener>*/();
  private final Map/*<IFile, MavenProjectChangedEvent>*/ projectChangeEvents = new LinkedHashMap/*<IFile, MavenProjectChangedEvent>*/();

  private final Set/*<IDownloadSourceListener>*/ downloadSourceListeners = new LinkedHashSet/*<IDownloadSourceListener>*/();
  private final Map/*<IProject, DownloadSourceEvent>*/ downloadSourceEvents = new LinkedHashMap/*<IProject, DownloadSourceEvent>*/();

  private static final ThreadLocal/*<Context>*/ context = new ThreadLocal/*<Context>*/();
  
  static Context getContext() {
    return (Context) context.get();
  }

  public MavenProjectManagerImpl(MavenConsole console, IndexManager indexManager, MavenEmbedderManager embedderManager,
      MavenRuntimeManager runtimeManager) {
    this.console = console;
    this.indexManager = indexManager;
    this.embedderManager = embedderManager;
    this.markerManager = new MavenMarkerManager();
    this.runtimeManager = runtimeManager;
    this.state = new WorkspaceState(null);
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
//  private MavenProjectFacade create(IFile pom) {
//    return create(pom, false, new NullProgressMonitor());
//  }

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
        }
      } catch(MavenEmbedderException ex) {
        String msg = "Failed to create Maven embedder";
        console.logError(msg + "; " + ex.toString());
        MavenPlugin.log(msg, ex);
      }
    }
    return projectFacade;
  }

  public boolean saveResolverConfiguration(IProject project, ResolverConfiguration configuration) {
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(MavenPlugin.PLUGIN_ID);
    if(projectNode != null) {
      projectNode.put(P_VERSION, VERSION);
      
      projectNode.putBoolean(P_USE_MAVEN_FOLDERS, configuration.shouldUseMavenOutputFolders());
      projectNode.putBoolean(P_RESOLVE_WORKSPACE_PROJECTS, configuration.shouldResolveWorkspaceProjects());
      projectNode.putBoolean(P_INCLUDE_MODULES, configuration.shouldIncludeModules());
      projectNode.putBoolean(P_FILTER_RESOURCES, configuration.shouldFilterResources());
      
      projectNode.put(P_RESOURCE_FILTER_GOALS, configuration.getResourceFilteringGoals());
      projectNode.put(P_ACTIVE_PROFILES, configuration.getActiveProfiles());
      
      try {
        projectNode.flush();
        return true;
      } catch(BackingStoreException ex) {
        MavenPlugin.log("Failed to save resolver configuration", ex);
      }
    }
    
    return false;
  }

  public ResolverConfiguration readResolverConfiguration(IProject project) {
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(MavenPlugin.PLUGIN_ID);
    if(projectNode==null) {
      return new ResolverConfiguration();
    }
    
    String version = projectNode.get(P_VERSION, null);
    if(version == null) {  // migrate from old config
      return getResolverConfiguration(BuildPathManager.getMavenContainerEntry(JavaCore.create(project)));
    }
  
    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setUseMavenOutputFolders(projectNode.getBoolean(P_USE_MAVEN_FOLDERS, false));
    configuration.setResolveWorkspaceProjects(projectNode.getBoolean(P_RESOLVE_WORKSPACE_PROJECTS, false));
    configuration.setIncludeModules(projectNode.getBoolean(P_INCLUDE_MODULES, false));
    configuration.setFilterResources(projectNode.getBoolean(P_FILTER_RESOURCES, false));
    
    configuration.setResourceFilteringGoals(projectNode.get(P_RESOURCE_FILTER_GOALS, ResolverConfiguration.DEFAULT_FILTERING_GOALS));
    configuration.setActiveProfiles(projectNode.get(P_ACTIVE_PROFILES, ""));
    return configuration;
  }

  private ResolverConfiguration getResolverConfiguration(IClasspathEntry entry) {
    if(entry == null) {
      return new ResolverConfiguration();
    }
  
    String containerPath = entry.getPath().toString();
  
    boolean includeModules = containerPath.indexOf("/" + MavenPlugin.INCLUDE_MODULES) > -1;
  
    boolean resolveWorkspaceProjects = containerPath.indexOf("/" + MavenPlugin.NO_WORKSPACE_PROJECTS) == -1;
  
    boolean filterResources = containerPath.indexOf("/" + MavenPlugin.FILTER_RESOURCES) != -1;
  
    boolean useMavenOutputFolders = containerPath.indexOf("/" + MavenPlugin.USE_MAVEN_OUPUT_FOLDERS) != -1;
  
    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(includeModules);
    configuration.setResolveWorkspaceProjects(resolveWorkspaceProjects);
    configuration.setFilterResources(filterResources);
    configuration.setUseMavenOutputFolders(useMavenOutputFolders);
    configuration.setActiveProfiles(getActiveProfiles(entry));
    return configuration;
  }

  private String getActiveProfiles(IClasspathEntry entry) {
    String path = entry.getPath().toString();
    String prefix = "/" + MavenPlugin.ACTIVE_PROFILES + "[";
    int n = path.indexOf(prefix);
    if(n == -1) {
      return "";
    }
  
    return path.substring(n + prefix.length(), path.indexOf("]", n));
  }

  IFile getPom(IProject project) {
    if (project == null || !project.isAccessible()) {
      // XXX sensible handling
      return null;
    }
    return project.getFile(MavenPlugin.POM_FILE_NAME);
  }

  /**
   * Removes specified poms from the cache.
   * Adds dependent poms to pomSet but does not directly refresh dependent poms.
   * Recursively removes all nested modules if appropriate.
   * 
   * @return a {@link Set} of {@link IFile} affected poms
   */
  public Set/*<IFile>*/ remove(Set/*<IFile>*/ poms, boolean force) {
    Set pomSet = new LinkedHashSet();
    for (Iterator it = poms.iterator(); it.hasNext(); ) {
      IFile pom = (IFile) it.next();
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
  public Set/*<IFile>*/ remove(IFile pom) {
    MavenProjectFacade facade = state.getProjectFacade(pom);
    MavenProject mavenProject = facade != null ? facade.getMavenProject() : null;

    if (mavenProject == null) {
      return Collections.EMPTY_SET;
    }

    Set pomSet = new LinkedHashSet();

    pomSet.addAll(state.getDependents(pom, mavenProject, false));
    state.removeProject(pom, mavenProject);
    removeFromIndex(mavenProject);
    addProjectChangeEvent(pom, MavenProjectChangedEvent.KIND_REMOVED, MavenProjectChangedEvent.FLAG_NONE, facade, null);

    // XXX this will likely NOT work for closed/removed projects, need to move to IResourceChangeEventListener
    if(facade!=null) {
      ResolverConfiguration resolverConfiguration = facade.getResolverConfiguration();
      if (resolverConfiguration != null && resolverConfiguration.shouldIncludeModules()) {
        pomSet.addAll(removeNestedModules(pom, mavenProject));
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
  public void refresh(Set/*<MavenUpdateRequest>*/ updateRequests, IProgressMonitor monitor) throws CoreException, MavenEmbedderException, InterruptedException {
    MavenEmbedder embedder = embedderManager.createEmbedder(EmbedderFactory.createWorkspaceCustomizer());
    try {
      for(Iterator it = updateRequests.iterator(); it.hasNext();) {
        if(monitor.isCanceled()) {
          throw new InterruptedException();
        }

        MavenUpdateRequest updateRequest = (MavenUpdateRequest) it.next();
        
        while(!updateRequest.isEmpty()) {
          IFile pom = updateRequest.pop();
          monitor.subTask(pom.getFullPath().toString());
          
          if (!pom.isAccessible() || !pom.getProject().hasNature(MavenPlugin.NATURE_ID)) {
            updateRequest.addPomFiles(remove(pom));
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

  public void refresh(MavenEmbedder embedder, IFile pom, MavenUpdateRequest updateRequest, IProgressMonitor monitor) throws CoreException {
    MavenProjectFacade oldFacade = state.getProjectFacade(pom);

    if(!updateRequest.isForce(pom) && oldFacade != null && !oldFacade.isStale()) {
      // skip refresh if not forced and up-to-date facade
      return;
    }

    markerManager.deleteMarkers(pom);

    ResolverConfiguration resolverConfiguration = readResolverConfiguration(pom.getProject());

    MavenProject mavenProject = null;
    MavenExecutionResult result = null;
    if (pom.isAccessible() && resolverConfiguration != null) {
      result = execute(embedder, pom, resolverConfiguration, new MavenProjectReader(updateRequest), monitor);
      mavenProject = result.getProject();
      markerManager.addMarkers(pom, result);
    }

    if (resolverConfiguration == null || mavenProject == null) {
      updateRequest.addPomFiles(remove(pom));
      if (result != null && resolverConfiguration != null && resolverConfiguration.shouldResolveWorkspaceProjects()) {
        // this only really add missing parent
        addMissingProjectDependencies(pom, result);
      }
      return;
    }

    MavenProject oldMavenProject = oldFacade != null? oldFacade.getMavenProject() : null;

    boolean dependencyChanged = hasDependencyChange(oldMavenProject, mavenProject);

    // refresh modules
    if (resolverConfiguration.shouldIncludeModules()) {
      updateRequest.addPomFiles(remove(getRemovedNestedModules(pom, oldMavenProject, mavenProject), true));
      updateRequest.addPomFiles(refreshNestedModules(pom, mavenProject));
    } else {
      updateRequest.addPomFiles(refreshWorkspaceModules(pom, oldMavenProject));
      updateRequest.addPomFiles(refreshWorkspaceModules(pom, mavenProject));
    }

    // refresh dependents
    if (dependencyChanged) {
      updateRequest.addPomFiles(state.getDependents(pom, oldMavenProject, resolverConfiguration.shouldIncludeModules()));
      updateRequest.addPomFiles(state.getDependents(pom, mavenProject, resolverConfiguration.shouldIncludeModules()));
    }

    // cleanup old project and old dependencies
    state.removeProject(pom, oldMavenProject);

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
      state.addWorkspaceModules(pom, mavenProject);
      if (resolverConfiguration.shouldResolveWorkspaceProjects()) {
        state.addProjectDependency(pom, new ArtifactKey(mavenProject.getParentArtifact()), true);
      }
    }

    // send appropriate event
    int kind;
    int flags = MavenProjectChangedEvent.FLAG_NONE;
    if (oldMavenProject == null) {
      kind = MavenProjectChangedEvent.KIND_ADDED;
    } else {
      kind = MavenProjectChangedEvent.KIND_CHANGED;
    }
    if (dependencyChanged) {
      flags |= MavenProjectChangedEvent.FLAG_DEPENDENCIES;
    }
    addProjectChangeEvent(pom, kind, flags, oldFacade, facade);

    // update index
    removeFromIndex(oldMavenProject);
    addToIndex(mavenProject);
  }

  private Set/*<IFile>*/ refreshNestedModules(IFile pom, MavenProject mavenProject) {
    if (mavenProject == null) {
      return Collections.EMPTY_SET;
    }
    
    Set pomSet = new LinkedHashSet();
    for (Iterator i = mavenProject.getModules().iterator(); i.hasNext(); ) {
      IFile modulePom = getModulePom(pom, (String) i.next());
      if (modulePom != null) {
        pomSet.add(modulePom);
      }
    }
    return pomSet;
  }

  private Set/*<IFile>*/ removeNestedModules(IFile pom, MavenProject mavenProject) {
    if (mavenProject == null) {
      return Collections.EMPTY_SET;
    }
    
    Set pomSet = new LinkedHashSet();
    for (Iterator i = mavenProject.getModules().iterator(); i.hasNext(); ) {
      IFile modulePom = getModulePom(pom, (String) i.next());
      if (modulePom != null) {
        pomSet.addAll(remove(modulePom));
      }
    }
    return pomSet;
  }

  /**
   * Returns Set<IFile> of nested module POMs that are present in oldMavenProject
   * but not in mavenProjec.
   */
  private Set/*<IFile>*/ getRemovedNestedModules(IFile pom, MavenProject oldMavenProject, MavenProject mavenProject) {
    List modules = mavenProject != null? mavenProject.getModules(): null;

    Set result = new LinkedHashSet();

    if (oldMavenProject != null) {
      for (Iterator i = oldMavenProject.getModules().iterator(); i.hasNext(); ) {
        String moduleName = (String) i.next();
        if (modules != null && !modules.contains(moduleName)) {
          IFile modulePOM = getModulePom(pom, moduleName);
          if (modulePOM != null) {
            result.add(modulePOM);
          }
        }
      }
    }

    return result;
  }

  public IFile getModulePom(IFile pom, String moduleName) {
    return pom.getParent().getFile(new Path(moduleName).append(MavenPlugin.POM_FILE_NAME));
  }

  private Set/*<IFile>*/ refreshWorkspaceModules(IFile pom, MavenProject mavenProject) {
    if (mavenProject == null) {
      return Collections.EMPTY_SET;
    }

    Set modules = state.removeWorkspaceModules(pom, mavenProject);

    Set pomSet = new LinkedHashSet();
    if (modules != null) {
      IWorkspaceRoot root = pom.getWorkspace().getRoot();
      for (Iterator it = modules.iterator(); it.hasNext(); ) {
        IPath modulePath = (IPath) it.next();
        IFile module = root.getFile(modulePath);
        if (module != null) {
          pomSet.add(module);
        }
      }
    }
    return pomSet;
  }

  private void addProjectDependencies(IFile pom, MavenProject mavenProject, boolean workspace) {
    for (Iterator it = mavenProject.getArtifacts().iterator(); it.hasNext(); ) {
      Artifact artifact = (Artifact) it.next();
      state.addProjectDependency(pom, new ArtifactKey(artifact), workspace);
    }
  }

  private void addToIndex(MavenProject mavenProject) {
    if (mavenProject != null) {
      indexManager.addDocument(IndexManager.WORKSPACE_INDEX, mavenProject.getFile().getAbsoluteFile(), //
          indexManager.getDocumentKey(mavenProject.getArtifact()), -1, -1, null, IndexManager.NOT_PRESENT, IndexManager.NOT_PRESENT);
    }
  }
  
  private void removeFromIndex(MavenProject mavenProject) {
    if (mavenProject != null) {
      indexManager.removeDocument(IndexManager.WORKSPACE_INDEX, //
          mavenProject.getFile().getAbsoluteFile(), indexManager.getDocumentKey(mavenProject.getArtifact()));
    }
  }

  private void addProjectChangeEvent(IFile pom, int kind, int flags, MavenProjectFacade oldMavenProject, MavenProjectFacade mavenProject) {
    MavenProjectChangedEvent event = (MavenProjectChangedEvent) projectChangeEvents.get(pom);
    if (event == null) {
      event = new MavenProjectChangedEvent(pom, kind, flags, oldMavenProject, mavenProject);
      projectChangeEvents.put(pom, event);
      return;
    }

    // merge events
    MavenProjectFacade old = event.getOldMavenProject() != null? event.getOldMavenProject(): oldMavenProject; 
    event = new MavenProjectChangedEvent(pom, event.getKind(), event.getFlags() | flags, old, mavenProject);
    projectChangeEvents.put(pom, event);
  }

  private void addMissingProjectDependencies(IFile pom, MavenExecutionResult result) {
    // kinda hack, but this is the only way I can get info about missing parent artifacts
    List exceptions = result.getExceptions();
    if (exceptions != null) {
      for (Iterator it = exceptions.iterator(); it.hasNext(); ) {
        Throwable t = (Throwable) it.next();
        while (t != null && !(t instanceof AbstractArtifactResolutionException)) {
          t = t.getCause();
        }
        if (t instanceof AbstractArtifactResolutionException) {
          AbstractArtifactResolutionException re = (AbstractArtifactResolutionException) t;
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

    Iterator beforeIt = before.getArtifacts().iterator();
    Iterator afterIt = after.getArtifacts().iterator();
    while (beforeIt.hasNext()) {
      Artifact beforeDependeny = (Artifact) beforeIt.next();
      Artifact afterDependeny = (Artifact) afterIt.next();
      if (!ArtifactKey.equals(beforeDependeny, afterDependeny)
            || !equals(beforeDependeny.getFile(), afterDependeny.getFile())) 
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
      Collection eventCollection = this.projectChangeEvents.values();
      MavenProjectChangedEvent[] events = (MavenProjectChangedEvent[]) eventCollection.toArray(new MavenProjectChangedEvent[eventCollection.size()]);
      IMavenProjectChangedListener[] listeners;
      synchronized (this.projectChangeListeners) {
        listeners = (IMavenProjectChangedListener[]) this.projectChangeListeners.toArray(new IMavenProjectChangedListener[this.projectChangeListeners.size()]);
      }
      this.projectChangeEvents.clear();
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].mavenProjectChanged(events, monitor);
      }
    }
  }

  public MavenProjectFacade getMavenProject(Artifact artifact) {
    return state.getMavenProject(artifact);
  }

  public void downloadSources(List/*<DownloadSourceRequest>*/ downloadRequests, IProgressMonitor monitor) throws MavenEmbedderException, InterruptedException, CoreException {
    MavenEmbedder embedder = embedderManager.createEmbedder(EmbedderFactory.createWorkspaceCustomizer());
    try {
      for (Iterator requestIterator = downloadRequests.iterator(); requestIterator.hasNext(); ) {
        if(monitor.isCanceled()) {
          throw new InterruptedException();
        }

        DownloadSourceRequest request = (DownloadSourceRequest) requestIterator.next();

        final ArtifactKey key = request.getArtifactKey();
        MavenProjectFacade projectFacade = create(request.getProject(), monitor);

        Artifact artifact = null;
        List remoteRepositories = null;

        if(projectFacade != null) {
          // for maven projects find actual artifact and MavenProject corresponding to the artifactKey

          // XXX ugly, need to find a better way
          class MavenProjectVisitor implements IMavenProjectVisitor {
            MavenProjectFacade mavenProject = null;
            Artifact artifact = null;

            public boolean visit(MavenProjectFacade mavenProject) {
              return this.mavenProject == null;
            }

            public void visit(MavenProjectFacade mavenProject, Artifact artifact) {
              ArtifactKey otherKey = new ArtifactKey(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getClassifier());
              if(key.equals(otherKey)) {
                this.mavenProject = mavenProject;
                this.artifact = artifact;
              }
            }
          };

          MavenProjectVisitor pv = new MavenProjectVisitor(); 
          projectFacade.accept(pv, IMavenProjectVisitor.NESTED_MODULES);

          artifact = pv.artifact;

          if (pv.mavenProject != null) {
            remoteRepositories = pv.mavenProject.getMavenProject().getRemoteArtifactRepositories();
          }

        } else {
          // not a maven project
          artifact = embedder.createArtifact(key.getGroupId(), key.getArtifactId(), key.getVersion(), null, ARTIFACT_TYPE_JAR);
          remoteRepositories = indexManager.getArtifactRepositories(null, null);
          try {
            embedder.resolve(artifact, remoteRepositories, embedder.getLocalRepository());
          } catch(Exception ex) {
            MavenPlugin.log("Can not resolve artifact for classpath entry of non-maven project", ex);
            continue;
          }
        }

        if (artifact != null && remoteRepositories != null) {
          monitor.subTask(artifact.getId());
          IPath srcPath = materializeArtifactPath(embedder, remoteRepositories, artifact, 
                ARTIFACT_TYPE_JAVA_SOURCE, monitor);
          String javadocUrl = null;
          if(srcPath == null) {
            IPath javadocPath = materializeArtifactPath(embedder, remoteRepositories, artifact,
                ARTIFACT_TYPE_JAVADOC, monitor);
            if (javadocPath != null) {
              javadocUrl = MavenClasspathContainer.getJavaDocUrl(javadocPath.toString());
            } else {
              // guess the javadoc url from the project url in the artifact's pom.xml
              String artifactLocation = artifact.getFile().getAbsolutePath();
              File file = new File(artifactLocation.substring(0, artifactLocation.length() - 4) + ".pom");
              if(file.exists()) {
                Model model;
                try {
                  model = embedder.readModel(file);
                } catch(XmlPullParserException ex) {
                  throw new MavenEmbedderException(ex);
                } catch(IOException ex) {
                  throw new MavenEmbedderException(ex);
                }
                String url = model.getUrl();
                if(url != null) {
                  url = url.trim();
                  if(url.length() > 0) {
                    if(!url.endsWith("/"))
                      url += "/";
                    javadocUrl =  url + "apidocs/"; // assuming project is using maven-generated site
                  }
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
        listeners = (IDownloadSourceListener[]) this.downloadSourceListeners.toArray(new IDownloadSourceListener[this.downloadSourceListeners.size()]);
      }
      for (int i = 0; i < listeners.length; i++) {
        for (Iterator eventsIter = downloadSourceEvents.values().iterator(); eventsIter.hasNext(); ) {
          DownloadSourceEvent event = (DownloadSourceEvent) eventsIter.next();
          try {
            listeners[i].sourcesDownloaded(event, monitor);
          } catch(CoreException ex) {
            MavenPlugin.log(ex);
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

  private IPath materializeArtifactPath(MavenEmbedder embedder, List remoteRepositories, Artifact base, String type, IProgressMonitor monitor) {
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
        af = indexManager.getIndexedArtifactFile(IndexManager.LOCAL_INDEX, indexManager.getDocumentKey(base));
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
        MavenPlugin.log(msg, ex);
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
      String name = ex.getGroupId() + ":" + ex.getArtifactId() + "-" + ex.getVersion() + "." + ex.getType();
      console.logError("Can't download " + type + " for artifact " + name);
      if(!isJavaSource && !isJavaDoc) {
        String msg = ex.getOriginalMessage()==null ? ex.toString() : ex.getOriginalMessage();
        console.logError("Error: " + msg);
        MavenPlugin.log(msg, ex);
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
      indexManager.addDocument(IndexManager.LOCAL_INDEX, null, indexManager.getDocumentKey(artifact), //
          artifactFile.length(), artifactFile.lastModified(), artifactFile, sourcesExists, javadocExists);
    }
  }

  public MavenExecutionResult readProjectWithDependencies(IFile pomFile, ResolverConfiguration resolverConfiguration, MavenUpdateRequest updateRequest, IProgressMonitor monitor) throws MavenEmbedderException {
    MavenEmbedder embedder = embedderManager.createEmbedder(EmbedderFactory.createWorkspaceCustomizer());
    try {
      return execute(embedder, pomFile, resolverConfiguration, new MavenProjectReader(updateRequest), monitor);
    } finally {
      try {
        embedder.stop();
      } catch(MavenEmbedderException ex) {
        MavenPlugin.log("Failed to stop Maven embedder", ex);
      }
    }
  }
  
  MavenExecutionResult execute(MavenEmbedder embedder, IFile pomFile, ResolverConfiguration resolverConfiguration, MavenRunnable runnable, IProgressMonitor monitor) {
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

  private static final ContainerCustomizer filterResourcesCustomizer =  new ContainerCustomizer() {
    public void customize(PlexusContainer container) {
      ComponentDescriptor resolverDescriptor = container.getComponentDescriptor(ArtifactResolver.ROLE);
      resolverDescriptor.setImplementation(EclipseArtifactResolver.class.getName());

      ComponentDescriptor translatorDescriptor = container.getComponentDescriptor(PathTranslator.ROLE);
      translatorDescriptor.setImplementation(FilterResourcesPathTranslator.class.getName());
    }
  };

  public void filterResources(MavenProjectFacade facade, IProgressMonitor monitor) throws MavenEmbedderException, CoreException {
    MavenEmbedder embedder = embedderManager.createEmbedder(filterResourcesCustomizer);

    try {
      IFile pom = facade.getPom();
      ResolverConfiguration resolverConfiguration = facade.getResolverConfiguration();
      String goals = resolverConfiguration.getResourceFilteringGoals();
      
      execute(embedder, pom, resolverConfiguration, new MavenExecutor(Arrays.asList(StringUtils.split(goals))), monitor);

      IProject project = pom.getProject();

      IFolder output;
      IFolder testOutput;

      if (resolverConfiguration.shouldUseMavenOutputFolders()) {
        IWorkspaceRoot root = pom.getWorkspace().getRoot();
        output = root.getFolder(facade.getOutputLocation());
        testOutput = root.getFolder(facade.getTestOutputLocation());
      } else {
        IFolder outputFolder = project.getFolder(runtimeManager.getDefaultOutputFolder());
        output = outputFolder.getFolder(FilterResourcesPathTranslator.RESOURCES_FOLDERNAME);
        testOutput = outputFolder.getFolder(FilterResourcesPathTranslator.TEST_RESOURCES_FOLDERNAME);
      }
      
      output.refreshLocal(IResource.DEPTH_INFINITE, monitor);
      testOutput.refreshLocal(IResource.DEPTH_INFINITE, monitor);

    } finally {
      embedder.stop();
    }
  }
  
  public MavenProjectFacade[] getProjects() {
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

  interface MavenRunnable {
    public MavenExecutionResult execute(MavenEmbedder embedder, MavenExecutionRequest request);
  }

  private static final class MavenExecutor implements MavenRunnable {
    private final List goals;

    MavenExecutor(List goals) {
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

}
