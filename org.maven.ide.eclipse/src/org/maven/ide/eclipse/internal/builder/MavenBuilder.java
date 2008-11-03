/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.lifecycle.NoSuchPhaseException;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.plan.BuildPlan;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.MavenRunnable;
import org.maven.ide.eclipse.project.ResolverConfiguration;


public class MavenBuilder extends IncrementalProjectBuilder {

  public static boolean DEBUG = MavenPlugin.getDefault().isDebugging()
      & Boolean.parseBoolean(Platform.getDebugOption(IMavenConstants.PLUGIN_ID + "/debug/builder"));
  
  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
   *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
   */
  @SuppressWarnings("unchecked")
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
    MavenPlugin plugin = MavenPlugin.getDefault();
    MavenConsole console = plugin.getConsole();
    MavenProjectManager projectManager = plugin.getMavenProjectManager();

    IProject project = getProject();
    if(project.hasNature(IMavenConstants.NATURE_ID)) {
      IFile pomResource = project.getFile(IMavenConstants.POM_FILE_NAME);
      if(pomResource == null) {
        console.logError("Project " + project.getName() + " does not have pom.xml");
        return null;
      }

      IMavenProjectFacade projectFacade = projectManager.create(getProject(), monitor);
      if (projectFacade == null) {
        // XXX is this really possible? should we warn the user?
        return null;
      }

      if(DEBUG) {
        System.out.println("\nStarting Maven build for " + project.getName()
            + " kind:" + kind + " requestedFullBuild:" + getRequireFullBuild(projectFacade)
            + " @ " + new Date(System.currentTimeMillis()));
      }
      
      boolean requireFullBuild = getRequireFullBuild(projectFacade);

      console.logMessage("Maven Builder: " + getKind(kind) + " " + (requireFullBuild ? "requireFullBuild" : ""));
      
      if (FULL_BUILD == kind || CLEAN_BUILD == kind || requireFullBuild) {
        try {
          boolean offline = FULL_BUILD != kind && CLEAN_BUILD != kind;
          executePostBuild(projectFacade, offline, monitor);
        } finally {
          resetRequireFullBuild(getProject());
        }
      } else {
        // kind == AUTO_BUILD || kind == INCREMENTAL_BUILD
        processResources(projectFacade, monitor);
      }
    }
    return null;
  }

  private void executePostBuild(final IMavenProjectFacade projectFacade, //
      final boolean offline, final IProgressMonitor monitor) throws CoreException {
    MavenExecutionResult result = projectFacade.execute(new MavenRunnable() {
      public MavenExecutionResult execute(MavenEmbedder embedder, MavenExecutionRequest request) {
        ResolverConfiguration configuration = projectFacade.getResolverConfiguration();

        List<String> goals = Arrays.asList(configuration.getFullBuildGoals().split("[,\\s]+"));
        List<String> filteredGoals = getFilteredGoals(embedder, goals, projectFacade, monitor);
        request.setGoals(filteredGoals.isEmpty() ? goals : filteredGoals);
        
        request.setRecursive(configuration.shouldIncludeModules());
        
        if(offline) {
          request.setOffline(true);
        }
        
        return embedder.execute(request);
      }
    }, monitor);
    logErrors(result, projectFacade.getProject().getName());
  }
  
  private void processResources(IMavenProjectFacade projectFacade, final IProgressMonitor monitor) throws CoreException {
    final IResourceDelta delta = getDelta(projectFacade.getProject());
    projectFacade.accept(new IMavenProjectVisitor() {
      public boolean visit(IMavenProjectFacade projectFacade) throws CoreException {
        MavenExecutionResult result = null;
        if (hasChangedResources(projectFacade, delta, true, monitor)) {
          result = filterResources(projectFacade, monitor);
        } else if (hasChangedResources(projectFacade, delta, false, monitor)) {
          // XXX optimize! no filtering, just copy the changed resources
          result = filterResources(projectFacade, monitor);
        }
        if(result!=null) {
          logErrors(result, projectFacade.getProject().getName());
        }
        return true;
      }
    }, IMavenProjectVisitor.NESTED_MODULES);
  }

  protected MavenExecutionResult filterResources(final IMavenProjectFacade projectFacade, //
      final IProgressMonitor monitor) throws CoreException {
    return projectFacade.execute(new MavenRunnable() {
      public MavenExecutionResult execute(MavenEmbedder embedder, MavenExecutionRequest request) {
        ResolverConfiguration configuration = projectFacade.getResolverConfiguration();
        
        List<String> goals = Arrays.asList(configuration.getResourceFilteringGoals().split("[,\\s]+"));
        List<String> filteredGoals = getFilteredGoals(embedder, goals, projectFacade, monitor);
        request.setGoals(filteredGoals.isEmpty() ? goals : filteredGoals);
        
        request.setRecursive(configuration.shouldIncludeModules());
        request.setOffline(true);  // always execute resource filtering offline
        
        return embedder.execute(request);
      } 
    }, monitor);
  }

  boolean hasChangedResources(IMavenProjectFacade facade, IResourceDelta delta, boolean filteredOnly, IProgressMonitor monitor) throws CoreException {
    MavenProject mavenProject = facade.getMavenProject(monitor);
    @SuppressWarnings("unchecked")
    List<Resource> resources = mavenProject.getBuild().getResources();
    @SuppressWarnings("unchecked")
    List<Resource> testResources = mavenProject.getBuild().getTestResources();
    
    Set<IPath> folders = new HashSet<IPath>();
    folders.addAll(getResourceFolders(facade, resources, filteredOnly));
    folders.addAll(getResourceFolders(facade, testResources, filteredOnly));

    if (delta == null) {
      return !folders.isEmpty();
    }

    for(IPath folderPath : folders) {
      IResourceDelta member = delta.findMember(folderPath);
      // XXX deal with member kind/flags
      if (member != null) {
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
      if (!filteredOnly || resource.isFiltering()) {
        if(DEBUG) {
          System.out.println("  included folder " + resource.getDirectory()); //$NON-NLS-1$
        }
        folders.add(facade.getProjectRelativePath(resource.getDirectory()));
      }
    }
    return folders;
  }

  void logErrors(MavenExecutionResult result, String projectNname) {
    if(result.hasExceptions()) {
      String msg = "Build errors for " + projectNname;
      @SuppressWarnings("unchecked")
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

  private boolean getRequireFullBuild(IMavenProjectFacade projectFacade) throws CoreException {
    if(projectFacade.getResolverConfiguration().isSkipCompiler()) {
      // see MNGECLIPSE-823
      return projectFacade.getProject().getSessionProperty(IMavenConstants.FULL_MAVEN_BUILD) != null;
    }
    return false;  
  }
  
  private void resetRequireFullBuild(IProject project) throws CoreException {
    project.setSessionProperty(IMavenConstants.FULL_MAVEN_BUILD, null);
  }

  private String getKind(int kind) {
    switch(kind) {
      case FULL_BUILD:
        return "FULL_BUILD";
      case AUTO_BUILD:
        return "AUTO_BUILD";
      case INCREMENTAL_BUILD:
        return "INCREMENTAL_BUILD";
      case CLEAN_BUILD:
        return "CLEAN_BUILD";
    }
    return "unknown";
  }
  
  @SuppressWarnings("unchecked")
  protected List<String> getFilteredGoals(MavenEmbedder embedder, List<String> goals,
      IMavenProjectFacade projectFacade, IProgressMonitor monitor) {
    if(projectFacade.getResolverConfiguration().isSkipCompiler()) {
      try {
        MavenProject mavenProject = projectFacade.getMavenProject(monitor);
        BuildPlan buildPlan = embedder.getBuildPlan(goals, mavenProject);
        
        List<String> result = new ArrayList<String>();
        for(MojoBinding m : (List<MojoBinding>) buildPlan.renderExecutionPlan(new Stack())) {
          if(!("org.apache.maven.plugins".equals(m.getGroupId()) && "maven-compiler-plugin".equals(m.getArtifactId()))) {
            result.add(m.getGroupId() + ":" + m.getArtifactId() + ":" + m.getVersion() + ":" + m.getGoal());
          }
        }
        return result;
        
      } catch(MavenEmbedderException ex) {
        MavenLogger.log("Can't get build plan", ex);
      } catch(NoSuchPhaseException ex) {
        MavenLogger.log("Can't get build plan", ex);
      } catch(CoreException ex) {
        MavenLogger.log(ex);
      }
    }
    return goals;
  }

  
  
}

