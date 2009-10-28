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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.Messages;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.project.IMavenMarkerManager;

public class MavenMarkerManager implements IMavenMarkerManager {
  /**
   * 
   */
  private static final String XSI_SCHEMA_LOCATION = "xsi:schemaLocation";

  /**
   * 
   */
  private static final String PROJECT_NODE = "project";
  public static final String OFFSET = "offset";
  public static final String NO_SCHEMA_ERR = "There is no schema defined for this pom.xml. Code completion will not work without a schema defined.";
  
  private final MavenConsole console;
  private final IMavenConfiguration mavenConfiguration; 

  public MavenMarkerManager(MavenRuntimeManager runtimeManager, MavenConsole console) {
    this.console = console;
    this.mavenConfiguration = MavenPlugin.lookup(IMavenConfiguration.class);
  }
  
  public void addMarkers(IResource pomFile, MavenExecutionResult result) {
    List<Throwable> exceptions = result.getExceptions();
    
    for(Throwable ex : exceptions) {
      if(ex instanceof ProjectBuildingException) {
        handleProjectBuildingException(pomFile, (ProjectBuildingException) ex);
  
      } else if(ex instanceof AbstractArtifactResolutionException) {
        AbstractArtifactResolutionException rex = (AbstractArtifactResolutionException) ex;
        String errorMessage = getArtifactId(rex) + " " + getErrorMessage(ex);
        addMarker(pomFile, errorMessage, 1, IMarker.SEVERITY_ERROR);
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
      addErrorMarkers(pomFile, "Circular dependency error", resolutionResult.getCircularDependencyExceptions());
    }

    MavenProject mavenProject = result.getProject();
    if (mavenProject != null) {
      addMissingArtifactMarkers(pomFile, mavenProject);
    }
    
    checkForSchema(pomFile);

  }

  /**
   * The xsi:schema info is not part of the model, it is stored in the xml only. Need to open the DOM
   * and look for the project node to see if it has this schema defined
   * @param pomFile
   */
  protected void checkForSchema(IResource pomFile){
    try{
      if(!(pomFile instanceof IFile)){
        return;
      }
      IDOMModel domModel = (IDOMModel)StructuredModelManager.getModelManager().getModelForRead((IFile)pomFile);
      boolean needsSchema = false;
      IStructuredDocumentRegion[] structuredDocumentRegions = domModel.getStructuredDocument().getStructuredDocumentRegions();
      int lineNumber = 0;
      int offset = 0;
      for(int i=0;i<structuredDocumentRegions.length;i++){
        int hasProject = structuredDocumentRegions[i].getText().lastIndexOf("<project");
        int schemaLoc = structuredDocumentRegions[i].getText().lastIndexOf(XSI_SCHEMA_LOCATION);
        if(hasProject >=0 && schemaLoc < 0 ){
          needsSchema=true;
          lineNumber = domModel.getStructuredDocument().getLineOfOffset( structuredDocumentRegions[i].getStartOffset())+1;
          offset = structuredDocumentRegions[i].getStartOffset();
        }
      }
      if(needsSchema){
        IMarker marker = addMarker(pomFile, NO_SCHEMA_ERR, lineNumber, IMarker.SEVERITY_WARNING, false);
        //the quick fix in the marker view needs to know the offset, since it doesn't have access to the
        //editor/source viewer
        if(marker != null){
          marker.setAttribute(OFFSET, offset);
        }
      } 
    } catch(Throwable ex) {
      //something went wrong looking for the schema. don't add the warning
    }
  }
  
  public IMarker addMarker(IResource resource, String message, int lineNumber, int severity) {
     return addMarker(resource, message, lineNumber, severity, true); 
  }


  private IMarker addMarker(IResource resource, String message, int lineNumber, int severity, boolean isTransient) {
    IMarker marker = null;
    try {
      if(resource.isAccessible()) {
        marker= resource.createMarker(IMavenConstants.MARKER_ID);
        marker.setAttribute(IMarker.MESSAGE, message);
        marker.setAttribute(IMarker.SEVERITY, severity);
        marker.setAttribute(IMarker.TRANSIENT, isTransient);
        
        if(lineNumber == -1) {
          lineNumber = 1;
        }
        marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
      }
    } catch(CoreException ex) {
      console.logError("Unable to add marker; " + ex.toString());
    }
    return marker;
  }

  private void handleProjectBuildingException(IResource pomFile, ProjectBuildingException ex) {
    Throwable cause = ex.getCause();
    if(cause instanceof ModelBuildingException) {
      ModelBuildingException mbe = (ModelBuildingException) cause;
      for (ModelProblem problem : mbe.getProblems()) {
        String msg = Messages.getString("plugin.markerBuildError", problem.getMessage()); //$NON-NLS-1$
//      console.logError(msg);
        int severity = (Severity.WARNING == problem.getSeverity())? IMarker.SEVERITY_WARNING: IMarker.SEVERITY_ERROR;
        addMarker(pomFile, msg, 1, severity);
      }
    } else {
      handleBuildException(pomFile, ex);
    }
  }

  private void handleBuildException(IResource pomFile, Throwable ex) {
    Throwable cause = getRootCause(ex);
    // String msg = Messages.getString("plugin.markerBuildError", cause.getMessage());  //$NON-NLS-1$
    String msg = cause.getMessage();
    addMarker(pomFile, msg, 1, IMarker.SEVERITY_ERROR);
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

  private String getErrorMessage(Throwable ex) {
    return getRootCause(ex).getMessage();
  }

  private Throwable getRootCause(Throwable ex) {
    Throwable lastCause = ex;
    Throwable cause = lastCause.getCause();
    while(cause != null && cause != lastCause) {
      if(cause instanceof ArtifactNotFoundException) {
        cause = null;
      } else {
        lastCause = cause;
        cause = cause.getCause();
      }
    }
    return cause == null ? lastCause : cause;
  }

  
  private void addErrorMarkers(IResource pomFile, String msg, List<? extends Exception> exceptions) {
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

  public void deleteMarkers(IResource resource) throws CoreException {
    if (resource != null && resource.exists()) {
      resource.deleteMarkers(IMavenConstants.MARKER_ID, true, IResource.DEPTH_INFINITE);
    }
  }

  private void addMissingArtifactMarkers(IResource pomFile, MavenProject mavenProject) {
//    @SuppressWarnings("unchecked")
//    Set<Artifact> directDependencies = mavenProject.getDependencyArtifacts();
//    @SuppressWarnings("unchecked")
    Set<Artifact> artifacts = mavenProject.getArtifacts();
    for(Artifact artifact : artifacts) {
      if (!artifact.isResolved()) {
        String errorMessage;
//        if (directDependencies.contains(artifact)) {
          errorMessage = "Missing artifact " + artifact.toString();
//        } else {
//          errorMessage = "Missing indirectly referenced artifact " + artifact.toString();
//        }
        
        if(mavenConfiguration.isOffline()) {
          errorMessage = "Offline / " + errorMessage; 
        }
        
        addMarker(pomFile, errorMessage, 1, IMarker.SEVERITY_ERROR);
        console.logError(errorMessage);
      }
    }
  }
  
 public  void addErrorMarkers(IResource resource, Exception ex) {
   Throwable cause = getRootCause(ex);
   if (cause instanceof CoreException) {
     CoreException cex = (CoreException)cause;
     IStatus status = cex.getStatus();
     if(status != null) {
       addMarker(resource, status.getMessage(), 1, IMarker.SEVERITY_ERROR, false); //$NON-NLS-1$
       IStatus[] children = status.getChildren();
       if(children != null) {
         for(IStatus childStatus : children) {
           addMarker(resource, childStatus.getMessage(), 1, IMarker.SEVERITY_ERROR, false); //$NON-NLS-1$
         }
       } 
     }
   } else {
     addMarker(resource, cause.getMessage(), 1, IMarker.SEVERITY_ERROR, false); //$NON-NLS-1$
   }
 }

}
