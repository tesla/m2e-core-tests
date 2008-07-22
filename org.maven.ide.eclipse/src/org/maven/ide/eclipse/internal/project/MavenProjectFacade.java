/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.util.StringUtils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.ArtifactRef;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.IMavenProjectVisitor2;
import org.maven.ide.eclipse.project.MavenProjectUtils;
import org.maven.ide.eclipse.project.MavenUpdateRequest;
import org.maven.ide.eclipse.project.ResolverConfiguration;


/**
 * MavenProject facade
 */
public class MavenProjectFacade implements IMavenProjectFacade, Serializable {
  private static final long serialVersionUID = 707484407691175077L;

  private final MavenProjectManagerImpl manager;

  private final IFile pom;
  
  private final File pomFile;

  private transient MavenProject mavenProject;

  private ResolverConfiguration resolverConfiguration;

  private final long[] timestamp = new long[MavenProjectManagerImpl.METADATA_PATH.size() + 1];

  // cached values from mavenProject
  private ArtifactKey artifactKey;
  private List<String> modules;
  private String packaging;
  private IPath[] resourceLocations;
  private IPath[] testResourceLocations;
  private IPath[] compileSourceLocations;
  private IPath[] testCompileSourceLocations;
  private IPath outputLocation;
  private IPath testOutputLocation;
  private Set<ArtifactRef> artifacts;

  public MavenProjectFacade(MavenProjectManagerImpl manager, IFile pom, MavenProject mavenProject,
      ResolverConfiguration resolverConfiguration) {
    this.manager = manager;
    this.pom = pom;
    this.pomFile = pom.getLocation().toFile();
    setMavenProject(mavenProject);
    this.resolverConfiguration = resolverConfiguration;
    updateTimestamp();
  }

  @SuppressWarnings("unchecked")
  private void setMavenProject(MavenProject mavenProject) {
    this.mavenProject = mavenProject;
    
    this.artifactKey = new ArtifactKey(mavenProject.getArtifact());
    this.packaging = mavenProject.getPackaging();
    this.modules = mavenProject.getModules();

    this.resourceLocations = MavenProjectUtils.getResourceLocations(getProject(), mavenProject.getResources());
    this.testResourceLocations = MavenProjectUtils.getResourceLocations(getProject(), mavenProject.getTestResources());
    this.compileSourceLocations = MavenProjectUtils.getSourceLocations(getProject(), mavenProject.getCompileSourceRoots());
    this.testCompileSourceLocations = MavenProjectUtils.getSourceLocations(getProject(),mavenProject.getTestCompileSourceRoots());

    IPath fullPath = getProject().getFullPath();

    IPath path = getProjectRelativePath(mavenProject.getBuild().getOutputDirectory());
    this.outputLocation = (path != null) ? fullPath.append(path) : null;

    path = getProjectRelativePath(mavenProject.getBuild().getTestOutputDirectory());
    this.testOutputLocation = path != null ? fullPath.append(path) : null;

    this.artifacts = ArtifactRef.fromArtifact(mavenProject.getArtifacts());
  }

  /**
   * Returns project relative paths of resource directories
   */
  public IPath[] getResourceLocations() {
    return resourceLocations;
  }

  /**
   * Returns project relative paths of test resource directories
   */
  public IPath[] getTestResourceLocations() {
    return testResourceLocations;
  }

  public IPath[] getCompileSourceLocations() {
    return compileSourceLocations;
  }

  public IPath[] getTestCompileSourceLocations() {
    return testCompileSourceLocations;
  }

  /**
   * Returns project resource for given filesystem location or null the location is outside of project.
   * 
   * @param resourceLocation absolute filesystem location
   * @return IPath the full, absolute workspace path resourceLocation
   */
  public IPath getProjectRelativePath(String resourceLocation) {
    return MavenProjectUtils.getProjectRelativePath(getProject(), resourceLocation);
  }

  /**
   * Filters resources of this project. Does not recurse into nested modules.
   */
  public void filterResources(IProgressMonitor monitor) throws CoreException {
    try {
      String goalsStr = resolverConfiguration.getResourceFilteringGoals();
      List<String> goals = Arrays.asList(StringUtils.split(goalsStr));
      manager.execute(this, goals, false, monitor);
      refreshBuildDirectory(monitor); // XXX only need to refresh classes and test-classes
    } catch(MavenEmbedderException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, ex.getMessage(), ex));
    }
  }

  /**
   * Returns the full, absolute path of this project maven build output directory relative to the workspace or null if
   * maven build output directory cannot be determined or outside of the workspace.
   */
  public IPath getOutputLocation() {
    return outputLocation;
  }

  /**
   * Returns the full, absolute path of this project maven build test output directory relative to the workspace or null
   * if maven build output directory cannot be determined or outside of the workspace.
   */
  public IPath getTestOutputLocation() {
    return testOutputLocation;
  }

  public IPath getFullPath() {
    return getProject().getFullPath();
  }

  /**
   * Lazy load and cache MavenProject instance
   */
  public MavenProject getMavenProject(IProgressMonitor monitor) throws CoreException {
    if (mavenProject == null) {
      try {
        MavenExecutionResult result = manager.readProjectWithDependencies(pom, resolverConfiguration, //
            new MavenUpdateRequest(true /* offline */, false /* updateSnapshots */),
            monitor);
        mavenProject = result.getProject();
        if (mavenProject == null) {
          MultiStatus status = new MultiStatus(IMavenConstants.PLUGIN_ID, 0, "Could not read maven project", null);
          @SuppressWarnings("unchecked")
          List<Exception> exceptions = result.getExceptions();
          for (Exception e : exceptions) {
            status.add(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, 0, e.getMessage(), e));
          }
          throw new CoreException(status);
        }
      } catch (MavenEmbedderException e) {
        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, 0, "Could not read maven project", e));
      }
    }
    return mavenProject;
  }

  public String getPackaging() {
    return packaging;
  }

  public IProject getProject() {
    return pom.getProject();
  }

  public IFile getPom() {
    return pom;
  }

  public File getPomFile() {
    return pomFile;
  }

  /**
   * Returns the full, absolute path of the given file relative to the workspace. Returns null if the file does not
   * exist or is not a member of this project.
   */
  public IPath getFullPath(File file) {
    return MavenProjectUtils.getFullPath(getProject(), file);
  }

  /**
   * Visits trough Maven project artifacts and modules
   * 
   * @param visitor a project visitor used to visit Maven project
   * @param flags flags to specify visiting behavior. See {@link IMavenProjectVisitor#LOAD},
   *          {@link IMavenProjectVisitor#NESTED_MODULES}.
   */
  public void accept(IMavenProjectVisitor visitor, int flags) throws CoreException {
    acceptImpl(visitor, flags, null);
  }

  public void accept(IMavenProjectVisitor2 visitor, int flags, IProgressMonitor monitor) throws CoreException {
    acceptImpl(visitor, flags, monitor);
  }

  @SuppressWarnings("unchecked")
  private void acceptImpl(IMavenProjectVisitor visitor, int flags, IProgressMonitor monitor) throws CoreException {
    if(visitor.visit(this)) {
      
      if (visitor instanceof IMavenProjectVisitor2 && monitor != null) {
        MavenProject mavenProject = getMavenProject(monitor);
        for(Artifact artifact : (Set<Artifact>) mavenProject.getArtifacts()) {
          ((IMavenProjectVisitor2) visitor).visit(this, artifact);
        }
      }

      if((flags & IMavenProjectVisitor.FORCE_MODULES) > 0 //
          || ((flags & IMavenProjectVisitor.NESTED_MODULES) > 0 //
              && resolverConfiguration.shouldIncludeModules())) {
        IFile pom = getPom();
        for(String moduleName : getMavenProjectModules()) {
          IFile modulePom = pom.getParent().getFile(new Path(moduleName).append(IMavenConstants.POM_FILE_NAME));
          IMavenProjectFacade moduleFacade = manager.create(modulePom, false, null);
          if(moduleFacade == null && ((flags & IMavenProjectVisitor.LOAD) > 0)) {
            moduleFacade = manager.create(modulePom, true, new NullProgressMonitor());
          }
          if(moduleFacade != null) {
            moduleFacade.accept(visitor, flags);
          }
        }
      }
    }
  }

  public List<String> getMavenProjectModules() {
    return modules;
  }
  
  public Set<ArtifactRef> getMavenProjectArtifacts() {
    return artifacts;
  }

  public ResolverConfiguration getResolverConfiguration() {
    return resolverConfiguration;
  }

  public void setResolverConfiguration(ResolverConfiguration configuration) {
    resolverConfiguration = configuration;
  }

  /**
   * @return true if maven project needs to be re-read from disk  
   */
  public boolean isStale() {
    IProject project = getProject();
    int i = 0;
    for(IPath path : MavenProjectManagerImpl.METADATA_PATH) {
      IFile file = project.getFile(path);
      if (timestamp[i] != file.getLocalTimeStamp()) {
        return true;
      }
      i++;
    }
    return timestamp[timestamp.length - 1] != pom.getLocalTimeStamp();
  }

  private void updateTimestamp() {
    IProject project = getProject();
    int i = 0;
    for(IPath path : MavenProjectManagerImpl.METADATA_PATH) {
      timestamp[i] = project.getFile(path).getLocalTimeStamp(); 
      i++;
    }
    timestamp[timestamp.length - 1] = pom.getLocalTimeStamp();
  }

  /**
   * Executes specified maven goals. 
   * 
   * Recurses into nested modules dependending on resolver configuration.
   */
  public void execute(List<String> goals, IProgressMonitor monitor) throws CoreException {
    try {
      manager.execute(this, goals, true, monitor);
      refreshBuildDirectory(monitor);
    } catch(MavenEmbedderException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, ex.getMessage(), ex));
    }
  }

  private void refreshBuildDirectory(final IProgressMonitor monitor) throws CoreException {
    accept(new IMavenProjectVisitor() {
      public boolean visit(IMavenProjectFacade projectFacade) throws CoreException {
        Build build = projectFacade.getMavenProject(monitor).getBuild();
        refreshFolder(projectFacade.getProjectRelativePath(build.getDirectory()), monitor);
        return true; // keep visiting
      }
    }, IMavenProjectVisitor.NESTED_MODULES);
  }

  void refreshFolder(IPath path, IProgressMonitor monitor) throws CoreException {
    IFolder folder = getProject().getFolder(path);
    if (folder != null) {
      folder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    }
  }

  public ArtifactKey getArtifactKey() {
    return artifactKey;
  }

  public MavenProject getMavenProject() {
    return mavenProject;
  }
}
