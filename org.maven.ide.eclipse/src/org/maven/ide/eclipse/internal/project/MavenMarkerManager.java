/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.extension.ExtensionScanningException;
import org.apache.maven.project.InvalidProjectModelException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.validation.ModelValidationResult;

import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.Messages;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;

public class MavenMarkerManager {
  
  private final MavenRuntimeManager runtimeManager;
  private final MavenConsole console;

  public MavenMarkerManager(MavenRuntimeManager runtimeManager, MavenConsole console) {
    this.runtimeManager = runtimeManager;
    this.console = console;
  }
  
  @SuppressWarnings("unchecked")
  void addMarkers(IFile pomFile, MavenExecutionResult result) {
    List<Exception> exceptions = result.getExceptions();
    for(Exception ex : exceptions) {
      if(ex instanceof ExtensionScanningException) {
        if(ex.getCause() instanceof ProjectBuildingException) {
          handleProjectBuildingException(pomFile, (ProjectBuildingException) ex.getCause());
        } else {
          handleBuildException(pomFile, ex);
        }
  
      } else if(ex instanceof ProjectBuildingException) {
        handleProjectBuildingException(pomFile, (ProjectBuildingException) ex);
  
      } else if(ex instanceof AbstractArtifactResolutionException) {
        // String msg = ex.getMessage().replaceAll("----------", "").replaceAll("\r\n\r\n", "\n").replaceAll("\n\n", "\n");
        // addMarker(pomFile, msg, 1, IMarker.SEVERITY_ERROR);
        // console.logError(msg);
  
        AbstractArtifactResolutionException rex = (AbstractArtifactResolutionException) ex;
        String errorMessage = getArtifactId(rex) + " " + getErrorMessage(ex);
        addMarker(pomFile, errorMessage, 1, IMarker.SEVERITY_ERROR);
//        console.logError(errorMessage);
  
      } else {
        handleBuildException(pomFile, ex);
      }
    }

    ArtifactResolutionResult resolutionResult = result.getArtifactResolutionResult();
    if(resolutionResult != null) {
      // @see also addMissingArtifactMarkers
      addErrorMarkers(pomFile, "Metadata resolution error", resolutionResult.getMetadataResolutionExceptions());
      addErrorMarkers(pomFile, "Artifact error", resolutionResult.getErrorArtifactExceptions());
      addErrorMarkers(pomFile, "Version range violation", resolutionResult.getVersionRangeViolations());
      addErrorMarkers(pomFile, "Curcular dependency error", resolutionResult.getCircularDependencyExceptions());
    }

    MavenProject mavenProject = result.getProject();
    if (mavenProject != null) {
      addMissingArtifactMarkers(pomFile, mavenProject);
    }
  }

  void addMarker(IResource resource, String message, int lineNumber, int severity) {
    try {
      if(resource.isAccessible()) {
        IMarker marker = resource.createMarker(IMavenConstants.MARKER_ID);
        marker.setAttribute(IMarker.MESSAGE, message);
        marker.setAttribute(IMarker.SEVERITY, severity);
        if(lineNumber == -1) {
          lineNumber = 1;
        }
        marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
      }
    } catch(CoreException ex) {
      console.logError("Unable to add marker; " + ex.toString());
    }
  }

  private void handleProjectBuildingException(IFile pomFile, ProjectBuildingException ex) {
    Throwable cause = ex.getCause();
    if(cause instanceof XmlPullParserException) {
      XmlPullParserException pex = (XmlPullParserException) cause;
//      console.logError(Messages.getString("plugin.markerParsingError") + getPomName(pomFile) + "; " + pex.getMessage());
      addMarker(pomFile, pex.getMessage(), pex.getLineNumber(), IMarker.SEVERITY_ERROR); //$NON-NLS-1$

    } else if(ex instanceof InvalidProjectModelException) {
      InvalidProjectModelException mex = (InvalidProjectModelException) ex;
      ModelValidationResult validationResult = mex.getValidationResult();
      String msg = Messages.getString("plugin.markerBuildError") + mex.getMessage();
//      console.logError(msg);
      if(validationResult == null) {
        addMarker(pomFile, msg, 1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
      } else {
        @SuppressWarnings("unchecked")
        List<String> messages = validationResult.getMessages();
        for(String message : messages) {
          addMarker(pomFile, message, 1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
//          console.logError("  " + message);
        }
      }

    } else {
      handleBuildException(pomFile, ex);
    }
  }

  private void handleBuildException(IFile pomFile, Exception ex) {
    String msg = Messages.getString("plugin.markerBuildError") + ex.getMessage();
    addMarker(pomFile, msg, 1, IMarker.SEVERITY_ERROR); //$NON-NLS-1$
//    console.logError(msg);
  }

  private String getArtifactId(AbstractArtifactResolutionException rex) {
    String id = rex.getGroupId() + ":" + rex.getArtifactId() + ":" + rex.getVersion();
    if(rex.getClassifier() != null) {
      id += ":" + rex.getClassifier();
    }
    if(rex.getType() != null) {
      id += ":" + rex.getType();
    }
    return id;
  }

  private String getErrorMessage(Exception ex) {
    Throwable lastCause = ex;
    Throwable cause = lastCause.getCause();

    String msg = lastCause.getMessage();
    while(cause != null && cause != lastCause) {
      msg = cause.getMessage();
//      if(lastCause instanceof ResourceDoesNotExistException) {
//        msg = ((ResourceDoesNotExistException) lastCause).getLocalizedMessage();
//      } else {
//      }
      lastCause = cause;
      cause = cause.getCause();
    }

    return msg;
  }

  private void addErrorMarkers(IFile pomFile, String msg, List<? extends Exception> exceptions) {
    if(exceptions != null) {
      for(Exception ex : exceptions) {
        if(ex instanceof AbstractArtifactResolutionException) {
          AbstractArtifactResolutionException rex = (AbstractArtifactResolutionException) ex;
          String errorMessage = getArtifactId(rex) + " " + getErrorMessage(ex);
          addMarker(pomFile, errorMessage, 1, IMarker.SEVERITY_ERROR);
//          console.logError(errorMessage);

        } else {
          addMarker(pomFile, ex.getMessage(), 1, IMarker.SEVERITY_ERROR);
//          console.logError(msg + "; " + ex.toString());
        }
      }
    }
  }

  void deleteMarkers(IFile pom) throws CoreException {
    if (pom != null && pom.exists()) {
      pom.deleteMarkers(IMavenConstants.MARKER_ID, true, IResource.DEPTH_INFINITE);
    }
  }

  private void addMissingArtifactMarkers(IFile pomFile, MavenProject mavenProject) {
    @SuppressWarnings("unchecked")
    Set<Artifact> directDependencies = mavenProject.getDependencyArtifacts();
    @SuppressWarnings("unchecked")
    Set<Artifact> artifacts = mavenProject.getArtifacts();
    for(Artifact artifact : artifacts) {
      if (!artifact.isResolved()) {
        String errorMessage;
        if (directDependencies.contains(artifact)) {
          errorMessage = "Missing artifact " + artifact.toString();
        } else {
          errorMessage = "Missing indirectly referenced artifact " + artifact.toString();
        }
        
        if(runtimeManager.isOffline()) {
          errorMessage = "Offline / " + errorMessage; 
        }
        
        addMarker(pomFile, errorMessage, 1, IMarker.SEVERITY_ERROR);
        console.logError(errorMessage);
      }
    }
  }

}
