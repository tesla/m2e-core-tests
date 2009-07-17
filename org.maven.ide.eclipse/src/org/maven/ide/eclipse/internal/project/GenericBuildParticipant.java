/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

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

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.project.configurator.AbstractBuildParticipant;


/**
 * BuildParticipant that executes maven goals on full project build
 * and on resource changes.
 * 
 * @author igor
 */
public class GenericBuildParticipant extends AbstractBuildParticipant {

  public static boolean DEBUG = MavenPlugin.getDefault().isDebugging()
      & Boolean.parseBoolean(Platform.getDebugOption(IMavenConstants.PLUGIN_ID + "/debug/builder"));

  public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
    MavenPlugin plugin = MavenPlugin.getDefault();
    MavenConsole console = plugin.getConsole();

    IMavenProjectFacade projectFacade = getMavenProjectFacade();

    boolean requireFullBuild = getRequireFullBuild(projectFacade);

    if(DEBUG) {
      System.out.println("\nStarting Maven build for " + projectFacade.getProject().getName()
          + " kind:" + kind + " requestedFullBuild:" + requireFullBuild
          + " @ " + new Date(System.currentTimeMillis()));
    }
    
    console.logMessage("Maven Builder: " + getKind(kind) + " " + (requireFullBuild ? "requireFullBuild" : ""));

    if(IncrementalProjectBuilder.FULL_BUILD == kind || IncrementalProjectBuilder.CLEAN_BUILD == kind
        || requireFullBuild) {
      try {
        boolean offline = IncrementalProjectBuilder.FULL_BUILD != kind && IncrementalProjectBuilder.CLEAN_BUILD != kind;
        executePostBuild(projectFacade, offline, monitor);
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
      return Arrays.asList(configuration.getFullBuildGoals().split("[,\\s]+"));
    }
    return Arrays.asList(configuration.getResourceFilteringGoals().split("[,\\s]+"));
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
        return "FULL_BUILD";
      case IncrementalProjectBuilder.AUTO_BUILD:
        return "AUTO_BUILD";
      case IncrementalProjectBuilder.INCREMENTAL_BUILD:
        return "INCREMENTAL_BUILD";
      case IncrementalProjectBuilder.CLEAN_BUILD:
        return "CLEAN_BUILD";
    }
    return "unknown";
  }

  private void executePostBuild(final IMavenProjectFacade projectFacade, //
      final boolean offline, final IProgressMonitor monitor) throws CoreException {
    
    MavenProjectManager manager = MavenPlugin.lookup(MavenProjectManager.class);
    IMaven maven = MavenPlugin.lookup(IMaven.class);

    ResolverConfiguration configuration = projectFacade.getResolverConfiguration();

    MavenExecutionRequest request = manager.createExecutionRequest(projectFacade.getPom(), configuration, monitor);
    List<String> goals = Arrays.asList(configuration.getFullBuildGoals().split("[,\\s]+"));
    List<String> filteredGoals = getFilteredGoals(maven, goals, projectFacade, monitor);
    request.setGoals(filteredGoals.isEmpty() ? goals : filteredGoals);

    request.setRecursive(configuration.shouldIncludeModules());

    if(offline) {
      request.setOffline(true);
    }

    MavenExecutionResult result = maven.execute(request, monitor);

    refreshBuildFolders(projectFacade, monitor);

    logErrors(result, projectFacade.getProject().getName());
  }

  private void refreshBuildFolders(final IMavenProjectFacade projectFacade, final IProgressMonitor monitor)
      throws CoreException {
    projectFacade.accept(new IMavenProjectVisitor() {
      public boolean visit(IMavenProjectFacade projectFacade) throws CoreException {
        Build build = projectFacade.getMavenProject(monitor).getBuild();
        IFolder folder = projectFacade.getProject().getFolder(projectFacade.getProjectRelativePath(build.getDirectory()));
        if(folder != null) {
          folder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
        return true; // keep visiting
      }
    }, IMavenProjectVisitor.NESTED_MODULES);
  }

  private void processResources(IMavenProjectFacade projectFacade, final IProgressMonitor monitor) throws CoreException {
    final IResourceDelta delta = getDelta(projectFacade.getProject());
    projectFacade.accept(new IMavenProjectVisitor() {
      public boolean visit(IMavenProjectFacade projectFacade) throws CoreException {
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
        return true;
      }
    }, IMavenProjectVisitor.NESTED_MODULES);
  }

  protected MavenExecutionResult filterResources(final IMavenProjectFacade projectFacade, //
      final IProgressMonitor monitor) throws CoreException {

    MavenProjectManager manager = MavenPlugin.lookup(MavenProjectManager.class);
    IMaven maven = MavenPlugin.lookup(IMaven.class);

    ResolverConfiguration configuration = projectFacade.getResolverConfiguration();
    
    MavenExecutionRequest request = manager.createExecutionRequest(projectFacade.getPom(), configuration, monitor);
    List<String> goals = Arrays.asList(configuration.getResourceFilteringGoals().split("[,\\s]+"));
    List<String> filteredGoals = getFilteredGoals(maven, goals, projectFacade, monitor);
    request.setGoals(filteredGoals.isEmpty() ? goals : filteredGoals);

    request.setRecursive(configuration.shouldIncludeModules());
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
      List<Exception> exceptions = result.getExceptions();
      for(Exception ex : exceptions) {
        MavenPlugin.getDefault().getConsole().logError(msg + "; " + ex.toString());
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
