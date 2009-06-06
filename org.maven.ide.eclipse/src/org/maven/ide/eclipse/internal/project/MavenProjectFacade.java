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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import org.apache.maven.artifact.Artifact;
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
import org.maven.ide.eclipse.project.configurator.AbstractBuildParticipant;


/**
 * MavenProject facade
 */
public class MavenProjectFacade implements IMavenProjectFacade, Serializable {

  private static final long serialVersionUID = 707484407691175077L;

  private static final String VERSION = "[version]";
  private static final String ARTIFACT_ID = "[artifactId]";
  private static final String GROUP_ID = "[groupId]";
  
  private final MavenProjectManagerImpl manager;

  private final IFile pom;

  private final File pomFile;

  private transient MavenProject mavenProject;
  private transient List<AbstractBuildParticipant> buildParticipants;

  // XXX make final, there should be no need to change it
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
    IPath location = pom.getLocation();
    this.pomFile = location == null ? null : location.toFile(); // save pom file
    this.resolverConfiguration = resolverConfiguration;
    setMavenProject(mavenProject);
    updateTimestamp();
  }

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
   * Returns project resource for given file system location or null the location is outside of project.
   * 
   * @param resourceLocation absolute file system location
   * @return IPath the full, absolute workspace path resourceLocation
   */
  public IPath getProjectRelativePath(String resourceLocation) {
    return MavenProjectUtils.getProjectRelativePath(getProject(), resourceLocation);
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
  public synchronized MavenProject getMavenProject(IProgressMonitor monitor) throws CoreException {
    if (mavenProject == null) {
      MavenExecutionResult result = manager.readProjectWithDependencies(pom, resolverConfiguration, //
          new MavenUpdateRequest(true /* offline */, false /* updateSnapshots */), monitor);
      mavenProject = result.getProject();
      if (mavenProject == null) {
        MultiStatus status = new MultiStatus(IMavenConstants.PLUGIN_ID, 0, "Could not read maven project", null);
        List<Exception> exceptions = result.getExceptions();
        for (Exception e : exceptions) {
          status.add(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, 0, e.getMessage(), e));
        }
        throw new CoreException(status);
      }
    }
    return mavenProject;
  }
  
  public synchronized List<AbstractBuildParticipant> getBuildParticipants(IProgressMonitor monitor) throws CoreException {
    if (buildParticipants == null) {
      buildParticipants = manager.getBuildParticipants(this, monitor);
    }
    return buildParticipants;
  }

  public synchronized  void setBuildParticipants(List<AbstractBuildParticipant> buildParticipants) {
    this.buildParticipants = buildParticipants;
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

  private void acceptImpl(IMavenProjectVisitor visitor, int flags, IProgressMonitor monitor) throws CoreException {
    if(visitor.visit(this)) {
      if (visitor instanceof IMavenProjectVisitor2 && monitor != null) {
        for(Artifact artifact : getMavenProject(monitor).getArtifacts()) {
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
      if (timestamp[i] != getModificationStamp(project.getFile(path))) {
        return true;
      }
      i++;
    }
    return timestamp[timestamp.length - 1] != getModificationStamp(pom);
  }

  private void updateTimestamp() {
    IProject project = getProject();
    int i = 0;
    for(IPath path : MavenProjectManagerImpl.METADATA_PATH) {
      timestamp[i] = getModificationStamp(project.getFile(path)); 
      i++;
    }
    timestamp[timestamp.length - 1] = getModificationStamp(pom);
  }

  private static long getModificationStamp(IFile file) {
    /*
     * this implementation misses update in the following scenario
     * 
     * 1. two files, A and B, with different content but same length were created
     *    with same localTimeStamp
     * 2. original A was deleted and B moved to A
     * 
     * See also https://bugs.eclipse.org/bugs/show_bug.cgi?id=160728
     */
    return file.getLocation().toFile().length() + file.getLocalTimeStamp() + file.getModificationStamp();
  }

  /**
   * Executes specified maven goals. 
   * 
   * Recurses into nested modules depending on resolver configuration.
   * 
   * @return execution result 
   */
//  public MavenExecutionResult execute(List<String> goals, IProgressMonitor monitor) throws CoreException {
//    MavenExecutionResult result = execute(goals, resolverConfiguration.shouldIncludeModules(), monitor);
//    refreshBuildDirectory(monitor);
//    return result;
//  }

//  public MavenExecutionResult execute(MavenRunnable runnable, IProgressMonitor monitor) throws CoreException {
//    MavenExecutionResult result = manager.execute(pom, resolverConfiguration, runnable, monitor, monitor);
//
//    // XXX only need to refresh target or target/classes and target/test-classes
//    refreshBuildFolders(monitor);
//    
//    return result; 
//  }
  
  private void refreshBuildFolders(final IProgressMonitor monitor) throws CoreException {
    accept(new IMavenProjectVisitor() {
      public boolean visit(IMavenProjectFacade projectFacade) throws CoreException {
        Build build = projectFacade.getMavenProject(monitor).getBuild();
        IFolder folder = getProject().getFolder(projectFacade.getProjectRelativePath(build.getDirectory()));
        if(folder != null) {
          folder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
        return true; // keep visiting
      }
    }, IMavenProjectVisitor.NESTED_MODULES);
  }

  public ArtifactKey getArtifactKey() {
    return artifactKey;
  }

  public MavenProject getMavenProject() {
    return mavenProject;
  }

  public String getNameTemplate() {
    return getNamePattern(getProject().getName(), getArtifactKey().getGroupId(), 
        getArtifactKey().getArtifactId(), getArtifactKey().getVersion());
  }
  
  public static String getNamePattern(String projectName, String groupId, String artifactId, String version) {
    GAVPatternIterator iterator = new GAVPatternIterator(groupId, artifactId, version);
    while (iterator.hasNext()) {
      Pattern p = iterator.next();
      Matcher m = p.matcher(projectName); 
      if (m.matches()) {
        // replace longest first. if same length, priority is A > G > V. calculate weighted length
        // assumption is weighted lengths are never equal
        int l1 = groupId.length() * groupId.length() + 1;
        int l2 = artifactId.length() * artifactId.length() + 2;
        int l3 = version.length() * version.length();
        
        if (l1 > l2 && l1 > l3) {
          projectName = replace(projectName, groupId, GROUP_ID);
          if (l2 > l3) {
            projectName = replace(projectName, artifactId, ARTIFACT_ID);
            projectName = replace(projectName, version, VERSION);
          } else {
            projectName = replace(projectName, version, VERSION);
            projectName = replace(projectName, artifactId, ARTIFACT_ID);
          }
        } else if (l2 > l1 && l2 > l3) {
          projectName = replace(projectName, artifactId, ARTIFACT_ID);
          if (l1 > l3) {
            projectName = replace(projectName, groupId, GROUP_ID);
            projectName = replace(projectName, version, VERSION);
          } else {
            projectName = replace(projectName, version, VERSION);
            projectName = replace(projectName, groupId, GROUP_ID);
          }
        } else {
          projectName = replace(projectName, version, VERSION);
          if (l1 > l2) {
            projectName = replace(projectName, groupId, GROUP_ID);
            projectName = replace(projectName, artifactId, ARTIFACT_ID);
          } else {
            projectName = replace(projectName, artifactId, ARTIFACT_ID);
            projectName = replace(projectName, groupId, GROUP_ID);
          }
        }

        return projectName;
      }
    }

    // cannot guess...
    return ARTIFACT_ID;
  }
  
  private static String replace(String str, String value, String template) {
    int pos = str.indexOf(value);
    if (pos >= 0) {
      str = str.substring(0, pos) + template + str.substring(pos + value.length());
    }
    return str;
  }

  
  public static class GAVPatternIterator implements Iterator<Pattern> {
    private static final int num1 = 3;
    private static final int num2 = 6;
    private static final int num3 = 6;
    private static final int totalNum = num1 + num2 + num3;
    private static final String ANY = ".*";

    private int currentNum;
    private String groupId;
    private String artifactId;
    private String version;    
    
    public GAVPatternIterator(String groupId, String artifactId, String version) {
      this.currentNum = totalNum;
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.version = version;
    }

    public boolean hasNext() {
      return currentNum > 0;
    }

    public Pattern next() {
      return Pattern.compile(getPattern());
    }

    private String getPattern() {
      if(currentNum < num1) {
        // one
        return ANY + get(currentNum-- ) + ANY;
      } else if(currentNum < num1 + num2) {
        // two
        int offset = (currentNum - num1) / 3;
        return ANY + get(currentNum + offset) + ANY + get(currentNum-- + offset * 2 + 1) + ANY;
      } else {
        // three
        int offset = (currentNum - num1 - num2) / 3;
        return ANY + get(currentNum + offset) + ANY + get(currentNum + offset * 2 + 1) + ANY
            + get(currentNum-- + offset + 2);
      }
    }

    private String get(int pos) {
      pos %= 3;
      if (pos == 0) {
        return groupId;
      } else if (pos == 1) {
        return artifactId;
      } else {
        return version;
      }
    }
    
    public void remove() {
      //not supported
    }
    
  }
}
