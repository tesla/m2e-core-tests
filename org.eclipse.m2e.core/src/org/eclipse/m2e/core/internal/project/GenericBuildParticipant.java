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

package org.eclipse.m2e.core.internal.project;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;


/**
 * BuildParticipant that executes maven goals on full project build
 * and on resource changes.
 * 
 * @author igor
 */
public class GenericBuildParticipant extends AbstractBuildParticipant {

  public static boolean DEBUG = MavenPlugin.getDefault().isDebugging()
      & Boolean.parseBoolean(Platform.getDebugOption(IMavenConstants.PLUGIN_ID + "/debug/builder")); //$NON-NLS-1$

  public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
    MavenPlugin plugin = MavenPlugin.getDefault();
    MavenConsole console = plugin.getConsole();
    IMavenConfiguration configuration = MavenPlugin.getDefault().getMavenConfiguration();

    IMavenProjectFacade projectFacade = getMavenProjectFacade();

    boolean requireFullBuild = getRequireFullBuild(projectFacade);

    if(DEBUG) {
      System.out.println("\nStarting Maven build for " + projectFacade.getProject().getName() //$NON-NLS-1$
          + " kind:" + kind + " requestedFullBuild:" + requireFullBuild //$NON-NLS-1$ //$NON-NLS-2$
          + " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$
    }
    
    console.logMessage("Maven Builder: " + getKind(kind) + " " + (requireFullBuild ? "requireFullBuild" : "")); //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    if(IncrementalProjectBuilder.FULL_BUILD == kind || IncrementalProjectBuilder.CLEAN_BUILD == kind
        || requireFullBuild) {
      try {
        executePostBuild(projectFacade, configuration.isOffline(), monitor);
      } finally {
        resetRequireFullBuild(projectFacade.getProject());
      }
    } else {
      // kind == AUTO_BUILD || kind == INCREMENTAL_BUILD
      processResources(projectFacade, monitor);
    }

    return null;
  }
  
  public List<String> getPossibleGoalsForBuildKind(IMavenProjectFacade projectFacade, int kind) {
    ResolverConfiguration configuration = projectFacade.getResolverConfiguration();
    if(IncrementalProjectBuilder.FULL_BUILD == kind || IncrementalProjectBuilder.CLEAN_BUILD == kind) {
      return Arrays.asList(configuration.getFullBuildGoals().split("[,\\s]+")); //$NON-NLS-1$
    }
    return Arrays.asList(configuration.getResourceFilteringGoals().split("[,\\s]+")); //$NON-NLS-1$
  }

  private boolean getRequireFullBuild(IMavenProjectFacade projectFacade) throws CoreException {
    if(projectFacade.getResolverConfiguration().isSkipCompiler()) {
      // see MNGECLIPSE-823
      return projectFacade.getProject().getSessionProperty(IMavenConstants.FULL_MAVEN_BUILD) != null;
    }
    return false;
  }

  private String getKind(int kind) {
    switch(kind) {
      case IncrementalProjectBuilder.FULL_BUILD:
        return "FULL_BUILD"; //$NON-NLS-1$
      case IncrementalProjectBuilder.AUTO_BUILD:
        return "AUTO_BUILD"; //$NON-NLS-1$
      case IncrementalProjectBuilder.INCREMENTAL_BUILD:
        return "INCREMENTAL_BUILD"; //$NON-NLS-1$
      case IncrementalProjectBuilder.CLEAN_BUILD:
        return "CLEAN_BUILD"; //$NON-NLS-1$
    }
    return "unknown"; //$NON-NLS-1$
  }

  private void executePostBuild(final IMavenProjectFacade projectFacade, //
      final boolean offline, final IProgressMonitor monitor) throws CoreException {
    
    MavenProjectManager manager = MavenPlugin.getDefault().getMavenProjectManager();
    IMaven maven = MavenPlugin.getDefault().getMaven();

    ResolverConfiguration configuration = projectFacade.getResolverConfiguration();

    MavenExecutionRequest request = manager.createExecutionRequest(projectFacade.getPom(), configuration, monitor);
    List<String> goals = Arrays.asList(configuration.getFullBuildGoals().split("[,\\s]+")); //$NON-NLS-1$
    List<String> filteredGoals = getFilteredGoals(maven, goals, projectFacade, monitor);
    request.setGoals(filteredGoals.isEmpty() ? goals : filteredGoals);

    request.setRecursive(false);

    if(offline) {
      request.setOffline(true);
    }

    MavenExecutionResult result = maven.execute(request, monitor);

    refreshBuildFolders(projectFacade, monitor);

    logErrors(result, projectFacade.getProject().getName());
  }

  private void refreshBuildFolders(final IMavenProjectFacade projectFacade, final IProgressMonitor monitor)
      throws CoreException {
    Build build = projectFacade.getMavenProject(monitor).getBuild();
    IFolder folder = projectFacade.getProject().getFolder(projectFacade.getProjectRelativePath(build.getDirectory()));
    if(folder != null) {
      folder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    }
  }

  private void processResources(IMavenProjectFacade projectFacade, final IProgressMonitor monitor) throws CoreException {
    final IResourceDelta delta = getDelta(projectFacade.getProject());
    MavenExecutionResult result = null;
    if(hasChangedResources(projectFacade, delta, true, monitor)) {
      result = filterResources(projectFacade, monitor);
    } else if(hasChangedResources(projectFacade, delta, false, monitor)) {
      // XXX optimize! no filtering, just copy the changed resources
      result = filterResources(projectFacade, monitor);
    }
    if(result != null) {
      logErrors(result, projectFacade.getProject().getName());
    }
  }

  protected MavenExecutionResult filterResources(final IMavenProjectFacade projectFacade, //
      final IProgressMonitor monitor) throws CoreException {

    MavenProjectManager manager = MavenPlugin.getDefault().getMavenProjectManager();
    IMaven maven = MavenPlugin.getDefault().getMaven();

    ResolverConfiguration configuration = projectFacade.getResolverConfiguration();
    
    MavenExecutionRequest request = manager.createExecutionRequest(projectFacade.getPom(), configuration, monitor);
    List<String> goals = Arrays.asList(configuration.getResourceFilteringGoals().split("[,\\s]+")); //$NON-NLS-1$
    List<String> filteredGoals = getFilteredGoals(maven, goals, projectFacade, monitor);
    request.setGoals(filteredGoals.isEmpty() ? goals : filteredGoals);

    request.setRecursive(false);
    request.setOffline(true); // always execute resource filtering offline

    MavenExecutionResult result = maven.execute(request, monitor);
    
    refreshBuildFolders(projectFacade, monitor);
    
    return result;
  }

  boolean hasChangedResources(IMavenProjectFacade facade, IResourceDelta delta, boolean filteredOnly,
      IProgressMonitor monitor) throws CoreException {
    MavenProject mavenProject = facade.getMavenProject(monitor);
    List<Resource> resources = mavenProject.getBuild().getResources();
    List<Resource> testResources = mavenProject.getBuild().getTestResources();

    Set<IPath> folders = new HashSet<IPath>();
    folders.addAll(getResourceFolders(facade, resources, filteredOnly));
    folders.addAll(getResourceFolders(facade, testResources, filteredOnly));

    if(delta == null) {
      return !folders.isEmpty();
    }

    for(IPath folderPath : folders) {
      IResourceDelta member = delta.findMember(folderPath);
      // XXX deal with member kind/flags
      if(member != null) {
        if(DEBUG) {
          System.out.println("  found changed folder " + folderPath); //$NON-NLS-1$
        }
        return true;
      }
    }

    return false;
  }

  private Set<IPath> getResourceFolders(IMavenProjectFacade facade, List<Resource> resources, boolean filteredOnly) {
    Set<IPath> folders = new LinkedHashSet<IPath>();
    for(Resource resource : resources) {
      if(!filteredOnly || resource.isFiltering()) {
        if(DEBUG) {
          System.out.println("  included folder " + resource.getDirectory()); //$NON-NLS-1$
        }
        IPath folder = facade.getProjectRelativePath(resource.getDirectory());
        if(folder != null) {
          folders.add(folder);
        }
      }
    }
    return folders;
  }

  void logErrors(MavenExecutionResult result, String projectNname) {
    if(result.hasExceptions()) {
      String msg = "Build errors for " + projectNname;
      List<Throwable> exceptions = result.getExceptions();
      for(Throwable ex : exceptions) {
        MavenPlugin.getDefault().getConsole().logError(msg + "; " + ex.toString()); //$NON-NLS-1$
        MavenLogger.log(msg, ex);
      }

      // XXX add error markers
    }

    // TODO log artifact resolution errors 
    // ArtifactResolutionResult resolutionResult = result.getArtifactResolutionResult();
    // resolutionResult.getCircularDependencyExceptions();
    // resolutionResult.getErrorArtifactExceptions();
    // resolutionResult.getMetadataResolutionExceptions();
    // resolutionResult.getVersionRangeViolations();
  }

  private void resetRequireFullBuild(IProject project) throws CoreException {
    project.setSessionProperty(IMavenConstants.FULL_MAVEN_BUILD, null);
  }

  protected List<String> getFilteredGoals(IMaven embedder, List<String> goals,
      IMavenProjectFacade projectFacade, IProgressMonitor monitor) {
//    if(projectFacade.getResolverConfiguration().isSkipCompiler()) {
//      try {
//        MavenProject mavenProject = projectFacade.getMavenProject(monitor);
//        BuildPlan buildPlan = embedder.getBuildPlan(goals, mavenProject);
//
//        List<String> result = new ArrayList<String>();
//        for(MojoBinding m : (List<MojoBinding>) buildPlan.renderExecutionPlan(new Stack())) {
//          if(!("org.apache.maven.plugins".equals(m.getGroupId()) && "maven-compiler-plugin".equals(m.getArtifactId()))) {
//            result.add(m.getGroupId() + ":" + m.getArtifactId() + ":" + m.getVersion() + ":" + m.getGoal());
//          }
//        }
//        return result;
//
//      } catch(MavenEmbedderException ex) {
//        MavenLogger.log("Can't get build plan", ex);
//      } catch(NoSuchPhaseException ex) {
//        MavenLogger.log("Can't get build plan", ex);
//      } catch(CoreException ex) {
//        MavenLogger.log(ex);
//      }
//    }
    return goals;
  }

}
