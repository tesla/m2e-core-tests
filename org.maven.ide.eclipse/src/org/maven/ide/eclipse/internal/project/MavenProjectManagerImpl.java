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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

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
import org.maven.ide.eclipse.project.DownloadSourceEvent;
import org.maven.ide.eclipse.project.IDownloadSourceListener;
import org.maven.ide.eclipse.project.IMavenProjectChangedListener;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.MavenProjectFacade;
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

  static final String ARTIFACT_TYPE_JAR = "jar";
  public static final String ARTIFACT_TYPE_JAVA_SOURCE = "java-source";
  public static final String ARTIFACT_TYPE_JAVADOC = "javadoc";

  static final String CLASSIFIER_SOURCES = "sources";
  static final String CLASSIFIER_JAVADOC = "javadoc";
  static final String CLASSIFIER_TESTS = "tests";
  static final String CLASSIFIER_TESTSOURCES = "test-sources";
  
  /** 
   * Maps ArtifactKey to full workspace IPath of the POM file that defines this artifact. 
   */
  final Map workspaceArtifacts = new HashMap();

  /**
   * Map<ArtifactKey, Set<IPath>> 
   * Maps ArtifactKey to Set of IPath of poms that depend on the artifact.
   * This map only includes dependencies between different (eclipse) projects.
   */
  private final Map workspaceDependencies = new HashMap();

  /**
   * Map<ArtifactKey, Set<IPath>> 
   * Maps ArtifactKey to Set of IPath of poms that depend on the artifact.
   * This map only includes dependencies within the same (eclipse) projects.
   */
  private final Map inprojectDependencies = new HashMap();

  /**
   * Maps parent ArtifactKey to Set of module poms IPath. This map only includes
   * module defined in eclipse projects other than project that defines parent pom. 
   */
  private final Map workspaceModules = new HashMap();

  /**
   * Maps full pom IPath to MavenProjectFacade
   */
  private final Map workspacePoms = new HashMap();

  private final MavenConsole console;
  private final IndexManager indexManager;
  private final MavenEmbedderManager embedderManager;
  private final MavenRuntimeManager runtimeManager;

  private final MavenMarkerManager markerManager;
  
  private final Set projectChangeListeners = new LinkedHashSet();
  private final Map projectChangeEvents = new LinkedHashMap();

  private final Set downloadSourceListeners = new LinkedHashSet();
  private final Map downloadSourceEvents = new LinkedHashMap();

  private static final ThreadLocal context = new ThreadLocal();
  
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
    MavenProjectFacade projectFacade = getProjectFacade(pom);
    if(projectFacade == null && load) {
      ResolverConfiguration configuration = MavenProjectFacade.readResolverConfiguration(pom.getProject());

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

  IFile getPom(IProject project) {
    if (project == null || !project.isAccessible()) {
      // XXX sensible handling
      return null;
    }
    return project.getFile(MavenPlugin.POM_FILE_NAME);
  }

  private MavenProjectFacade getProjectFacade(IFile pom) {
    return (MavenProjectFacade) workspacePoms.get(pom.getFullPath());
  }

  /**
   * Removes specified poms from the cache.
   * Adds dependent poms to pomSet but does not directly refresh dependent poms.
   * Recursively removes all nested modules if appropriate.
   * 
   * @return a {@link Set} of {@link IFile} affected poms
   */
  public Set remove(Set poms) {
    Set pomSet = new LinkedHashSet();
    for (Iterator it = poms.iterator(); it.hasNext(); ) {
      pomSet.addAll(remove((IFile) it.next()));
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
  public Set remove(IFile pom) {
    MavenProjectFacade facade = getProjectFacade(pom);
    MavenProject mavenProject = facade != null ? facade.getMavenProject() : null;

    if (mavenProject == null) {
      return Collections.EMPTY_SET;
    }
    
    Set pomSet = new LinkedHashSet();
    
    pomSet.addAll(refreshDependents(pom, mavenProject, workspaceDependencies));
    removeProject(pom, mavenProject);
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
  public void refresh(Set updateRequests, IProgressMonitor monitor) throws CoreException, MavenEmbedderException, InterruptedException {
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

  private void refresh(MavenEmbedder embedder, IFile pom, MavenUpdateRequest updateRequest, IProgressMonitor monitor) throws CoreException {
    markerManager.deleteMarkers(pom);

    ResolverConfiguration resolverConfiguration = MavenProjectFacade.readResolverConfiguration(pom.getProject());

    MavenProject mavenProject = null;
    MavenExecutionResult result = null;
    if (pom.isAccessible() && resolverConfiguration != null) {
      result = readProjectWithDependencies(embedder, pom, resolverConfiguration, updateRequest, monitor);
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

    MavenProjectFacade oldFacade = getProjectFacade(pom);
    MavenProject oldMavenProject = oldFacade != null? oldFacade.getMavenProject() : null;

    boolean dependencyChanged = hasDependencyChange(oldMavenProject, mavenProject);

    // refresh modules
    if (resolverConfiguration.shouldIncludeModules()) {
      updateRequest.addPomFiles(remove(getRemovedNestedModules(pom, oldMavenProject, mavenProject)));
      updateRequest.addPomFiles(refreshNestedModules(pom, mavenProject));
    } else {
      updateRequest.addPomFiles(refreshWorkspaceModules(pom, oldMavenProject));
      updateRequest.addPomFiles(refreshWorkspaceModules(pom, mavenProject));
    }

    // refresh dependents
    if (dependencyChanged) {
      updateRequest.addPomFiles(refreshDependents(pom, oldMavenProject, workspaceDependencies));
      updateRequest.addPomFiles(refreshDependents(pom, mavenProject, workspaceDependencies));
      if (resolverConfiguration.shouldIncludeModules()) {
        updateRequest.addPomFiles(refreshDependents(pom, oldMavenProject, inprojectDependencies));
        updateRequest.addPomFiles(refreshDependents(pom, mavenProject, inprojectDependencies));
      }
    }

    // cleanup old project and old dependencies
    removeProject(pom, oldMavenProject);

    // create new project and new dependencies
    MavenProjectFacade facade = new MavenProjectFacade(this, pom, mavenProject, resolverConfiguration);
    addProject(pom, facade);
    if (resolverConfiguration.shouldResolveWorkspaceProjects()) {
      addProjectDependencies(pom, mavenProject, true);
    }
    if (resolverConfiguration.shouldIncludeModules()) {
      addProjectDependencies(pom, mavenProject, false);
    }
    MavenProject parentProject = mavenProject.getParent();
    if (parentProject != null) {
      addWorkspaceModules(pom, mavenProject);
      if (resolverConfiguration.shouldResolveWorkspaceProjects()) {
        addProjectDependency(pom, new ArtifactKey(mavenProject.getParentArtifact()), true);
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

  private void addWorkspaceModules(IFile pom, MavenProject mavenProject) {
    ArtifactKey parentArtifactKey = new ArtifactKey(mavenProject.getParentArtifact());
    Set children = (Set) workspaceModules.get(parentArtifactKey);
    if (children == null) {
      children = new HashSet();
      workspaceModules.put(parentArtifactKey, children);
    }
    children.add(pom.getFullPath());
  }

  private Set refreshNestedModules(IFile pom, MavenProject mavenProject) {
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

  private Set removeNestedModules(IFile pom, MavenProject mavenProject) {
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
  private Set getRemovedNestedModules(IFile pom, MavenProject oldMavenProject, MavenProject mavenProject) {
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

  private Set refreshWorkspaceModules(IFile pom, MavenProject mavenProject) {
    if (mavenProject == null) {
      return Collections.EMPTY_SET;
    }

    Set pomSet = new LinkedHashSet();
    ArtifactKey key = new ArtifactKey(mavenProject.getArtifact());
    Set modules = (Set) workspaceModules.remove(key);
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
      addProjectDependency(pom, new ArtifactKey(artifact), workspace);
    }
  }

  /**
   * Configures dependency management for maven projects after the projects have beed added to
   * the workspace (created, opened, renamed, groupId/artifactId/version changed).
   *
   * More specifically, this method will
   * * Add the project to workspaceProjects map
   * * Add the project to workspaceArtifacts map
   * * Force refresh of all projects that depend on artifact of the project
   * 
   * Note that this method does not add the project to artifactDependents map
   * 
   * XXX I do not like the name of the method
   */
  private void addProject(IFile pom, MavenProjectFacade facade) {
    // Add the project to workspaceProjects map
    workspacePoms.put(pom.getFullPath(), facade);

    // Add the project to workspaceArtifacts map
    ArtifactKey artifactKey = new ArtifactKey(facade.getMavenProject().getArtifact());
    workspaceArtifacts.put(artifactKey, pom.getFullPath());
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

  /**
   * Performs necessary cleanup/refresh after maven project has been removed
   * from the workspace (closed, deleted, renamed, groupId/artifactId/version changed or
   * POM cannot be parsed any more). 
   *
   * More specifically, this method will
   * * Remove the project from workspaceProjects map
   * * Remove the project from workspaceArtifacts map
   * * Remove the project from artifactDependents map
   * * Force refresh of all projects that depend on the project
   * 
   * XXX I do not like the name of the method
   */
  private void removeProject(IFile pom, MavenProject mavenProject) {
    // Remove the project from workspaceDependents and inprojectDependenys maps
    removeDependencies(pom, true);
    removeDependencies(pom, false);

    // Remove the project from workspaceProjects map
    workspacePoms.remove(pom.getFullPath());

    // Remove the project from workspaceArtifacts map
    if (mavenProject != null) {
      ArtifactKey artifactKey = new ArtifactKey(mavenProject.getArtifact());
      workspaceArtifacts.remove(artifactKey);
    }
  }

  private void removeDependencies(IFile pom, boolean workspace) {
    // XXX may not be fast enough
    Map dependencies = workspace? workspaceDependencies: inprojectDependencies;
    for (Iterator it = dependencies.values().iterator(); it.hasNext(); ) {
      Set dependents = (Set) it.next();
      dependents.remove(pom.getFullPath());
    }
  }

  private Set refreshDependents(IFile pom, MavenProject mavenProject, Map dependencies) {
    if (mavenProject == null) {
      return Collections.EMPTY_SET;
    }
    
    IWorkspaceRoot root = pom.getWorkspace().getRoot();
    // Map dependencies = workspace ? workspaceDependencies : inprojectDependencies;
    Set dependents = (Set) dependencies.get(new ArtifactKey(mavenProject.getArtifact()));
    if (dependents == null) {
      return Collections.EMPTY_SET;
    }
      
    Set pomSet = new LinkedHashSet();
    for(Iterator it = dependents.iterator(); it.hasNext();) {
      IFile dependentPom = root.getFile((IPath) it.next());
      if(dependentPom == null || dependentPom.equals(pom)) {
        continue;
      }
      if(dependencies == workspaceDependencies || isSameProject(pom, dependentPom)) {
        pomSet.add(dependentPom);
      }
    }
    return pomSet;
  }

  static boolean isSameProject(IResource r1, IResource r2) {
    if (r1 == null || r2 == null) {
      return false;
    }
    return r1.getProject().equals(r2.getProject());
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

  private void addProjectDependency(IFile pom, ArtifactKey dependencyKey, boolean workspace) {
    Map dependencies = workspace? workspaceDependencies: inprojectDependencies;
    Set dependentProjects = (Set) dependencies.get(dependencyKey);
    if (dependentProjects == null) {
      dependentProjects = new HashSet();
      dependencies.put(dependencyKey, dependentProjects);
    }
    dependentProjects.add(pom.getFullPath());
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
          addProjectDependency(pom, dependencyKey, true);
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

  private void notifyProjectChangeListeners(IProgressMonitor monitor) {
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
    IPath path = (IPath) workspaceArtifacts.get(new ArtifactKey(artifact));
    if (path == null) {
      return null;
    }
    return (MavenProjectFacade) workspacePoms.get(path);
  }

  public void downloadSources(List downloadRequests, IProgressMonitor monitor) throws MavenEmbedderException, InterruptedException, CoreException {
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
      return readProjectWithDependencies(embedder, pomFile, resolverConfiguration, updateRequest, monitor);
    } finally {
      try {
        embedder.stop();
      } catch(MavenEmbedderException ex) {
        MavenPlugin.log("Failed to stop Maven embedder", ex);
      }
    }
  }
  
  public MavenExecutionResult execute(MavenEmbedder embedder, IFile pomFile, ResolverConfiguration resolverConfiguration, final List goals, IProgressMonitor monitor) {
    return execute(embedder, pomFile, resolverConfiguration, new MavenRunnable() {
      public MavenExecutionResult execute(MavenEmbedder embedder, MavenExecutionRequest request) {
        request.setGoals(goals);
        return embedder.execute(request);
      }
    }, monitor);
  }

  public MavenExecutionResult readProjectWithDependencies(MavenEmbedder embedder, IFile pomFile, ResolverConfiguration resolverConfiguration, final MavenUpdateRequest updateRequest, IProgressMonitor monitor) {
      return execute(embedder, pomFile, resolverConfiguration, new MavenRunnable() {
        public MavenExecutionResult execute(MavenEmbedder embedder, MavenExecutionRequest request) {
          request.setOffline(updateRequest.isOffline());
          request.setUpdateSnapshots(updateRequest.isUpdateSnapshots());
          return embedder.readProjectWithDependencies(request);
        }
      }, monitor);
  }

  private interface MavenRunnable {
    public MavenExecutionResult execute(MavenEmbedder embedder, MavenExecutionRequest request);
  }

  private MavenExecutionResult execute(MavenEmbedder embedder, IFile pomFile, ResolverConfiguration resolverConfiguration, MavenRunnable runnable, IProgressMonitor monitor) {
    File pom = pomFile.getLocation().toFile();

    MavenExecutionRequest request = embedderManager.createRequest(embedder);
    request.setPomFile(pom.getAbsolutePath());
    request.setBaseDirectory(pom.getParentFile());
    request.setTransferListener(new TransferListenerAdapter(monitor, console, indexManager));
    request.setProfiles(resolverConfiguration.getActiveProfileList());
    request.addActiveProfiles(resolverConfiguration.getActiveProfileList());
    request.setRecursive(false);
    request.setUseReactor(false);

    context.set(new Context(this, resolverConfiguration, pomFile));
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
      
      execute(embedder, pom, resolverConfiguration, Arrays.asList(StringUtils.split(goals)), monitor);

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
  
  /**
   * Context
   */
  static class Context {
    final MavenProjectManagerImpl manager;

    final ResolverConfiguration resolverConfiguration;

    final IFile pom;

    Context(MavenProjectManagerImpl manager, ResolverConfiguration resolverConfiguration, IFile pom) {
      this.manager = manager;
      this.resolverConfiguration = resolverConfiguration;
      this.pom = pom;
    }
  }

  
}
