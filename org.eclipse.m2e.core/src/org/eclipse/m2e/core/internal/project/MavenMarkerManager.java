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

import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.xml.core.internal.parser.regions.TagNameRegion;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import org.sonatype.aether.graph.Dependency;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.core.Messages;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.project.IMavenMarkerManager;

public class MavenMarkerManager implements IMavenMarkerManager {
  /**
   * 
   */
  private static final String XSI_SCHEMA_LOCATION = "xsi:schemaLocation"; //$NON-NLS-1$

  /**
   * 
   */
  private static final String PROJECT_NODE = "project"; //$NON-NLS-1$
  public static final String OFFSET = "offset"; //$NON-NLS-1$
  
  private final MavenConsole console;
  private final IMavenConfiguration mavenConfiguration; 

  public MavenMarkerManager(MavenConsole console, IMavenConfiguration mavenConfiguration) {
    this.console = console;
    this.mavenConfiguration = mavenConfiguration;
  }
  
  public void addMarkers(IResource pomFile, MavenExecutionResult result) {
    List<Throwable> exceptions = result.getExceptions();
    
    for(Throwable ex : exceptions) {
      if(ex instanceof ProjectBuildingException) {
        handleProjectBuildingException(pomFile, (ProjectBuildingException) ex);
  
      } else if(ex instanceof AbstractArtifactResolutionException) {
        AbstractArtifactResolutionException rex = (AbstractArtifactResolutionException) ex;
        String errorMessage = getArtifactId(rex) + " " + getErrorMessage(ex); //$NON-NLS-1$
        addMarker(pomFile, errorMessage, 1, IMarker.SEVERITY_ERROR);
      } else {
        handleBuildException(pomFile, ex);
      }
    }

    DependencyResolutionResult resolutionResult = result.getDependencyResolutionResult();
    if(resolutionResult != null) {
      // @see also addMissingArtifactMarkers
      addErrorMarkers(pomFile, org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_metadata_resolution, resolutionResult.getCollectionErrors());
      for(Dependency dependency : resolutionResult.getUnresolvedDependencies()) {
        addErrorMarkers(pomFile, org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_error_artifact, resolutionResult.getResolutionErrors(dependency));
      }
    }

    MavenProject mavenProject = result.getProject();
    if (mavenProject != null) {
      addMissingArtifactMarkers(pomFile, mavenProject);
    }
    
    checkForSchema(pomFile);
    //mkleint: adding here but I'm sort of not entirely clear what the usage patter of this class is.
    checkVarious(pomFile);
  }

  /**
   * @param pomFile
   */
  private void checkVarious(IResource pomFile) {
    IDOMModel domModel = null;
    try{
      if(!(pomFile instanceof IFile)){
        return;
      }
      domModel = (IDOMModel)StructuredModelManager.getModelManager().getModelForRead((IFile)pomFile);
      IStructuredDocument document = domModel.getStructuredDocument();
      Element root = domModel.getDocument().getDocumentElement();

      //now check parent version and groupid against the current project's ones..
      if (root.getNodeName().equals("project")) { //$NON-NLS-1$
        Element parent = findChildElement(root, "parent"); //$NON-NLS-1$
        Element groupId = findChildElement(root, "groupId"); //$NON-NLS-1$
        if (parent != null && groupId != null) {
          Element parentGroupId = findChildElement(parent, "groupId"); //$NON-NLS-1$
          if (parentGroupId != null) {
            //now compare the values of parent and project groupid..
            String parentString = getElementTextValue(parentGroupId);
            String childString = getElementTextValue(groupId);
            if (parentString != null && parentString.equals(childString)) {
              //now figure out the offset
              if (groupId instanceof IndexedRegion) {
                IndexedRegion off = (IndexedRegion)groupId;
                IMarker mark = addMarker(pomFile, org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_duplicate_groupid, document.getLineOfOffset(off.getStartOffset()) + 1, IMarker.SEVERITY_WARNING);
                mark.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, "parent_groupid"); //$NON-NLS-1$ //$NON-NLS-2$
                mark.setAttribute(IMarker.CHAR_START, off.getStartOffset());
                mark.setAttribute(IMarker.CHAR_END, off.getEndOffset());
              }
            }
          }
        }
        Element version = findChildElement(root, "version"); //$NON-NLS-1$
        if (parent != null && version != null) {
          Element parentVersion = findChildElement(parent, "version"); //$NON-NLS-1$
          if (parentVersion != null) {
            //now compare the values of parent and project version..
            String parentString = getElementTextValue(parentVersion);
            String childString = getElementTextValue(version);
            if (parentString != null && parentString.equals(childString)) {
              //now figure out the offset
              if (version instanceof IndexedRegion) {
                IndexedRegion off = (IndexedRegion)version;
                IMarker mark = addMarker(pomFile, org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_duplicate_version, document.getLineOfOffset(off.getStartOffset()) + 1, IMarker.SEVERITY_WARNING);
                mark.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, "parent_version"); //$NON-NLS-1$ //$NON-NLS-2$
                mark.setAttribute(IMarker.CHAR_START, off.getStartOffset());
                mark.setAttribute(IMarker.CHAR_END, off.getEndOffset());
              }
            }
          }
        }
      }
    }
    catch (Throwable t) {
      MavenLogger.log("Error checking for warnings", t);
    }
    finally {
      if ( domModel != null ) {
        domModel.releaseFromRead();
      }
    }
    
  }
  
  public static Element findChildElement(Element parent, String name) {
    NodeList rootList = parent.getChildNodes(); 
    for (int i = 0; i < rootList.getLength(); i++) {
        Node nd = rootList.item(i);
        if (nd instanceof Element) {
          Element el = (Element)nd;
          if (name.equals(el.getNodeName())) {
            return el;
          }
        }
    }
    return null;
  }
  public static String getElementTextValue(Node element) {
    if (element == null) return null;
    StringBuffer buff = new StringBuffer();
    NodeList list = element.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      Node child = list.item(i);
      if (child instanceof Text) {
        Text text = (Text)child;
        buff.append(text.getData());
      }
    }
    return buff.toString();
  }  

  /**
   * The xsi:schema info is not part of the model, it is stored in the xml only. Need to open the DOM
   * and look for the project node to see if it has this schema defined
   * @param pomFile
   */
  protected void checkForSchema(IResource pomFile){
    IDOMModel domModel = null;
    try{
      if(!(pomFile instanceof IFile)){
        return;
      }
      domModel = (IDOMModel)StructuredModelManager.getModelManager().getModelForRead((IFile)pomFile);
      IStructuredDocument document = domModel.getStructuredDocument();
      
      // iterate through document regions
      documentLoop:for(IStructuredDocumentRegion documentRegion : document.getStructuredDocumentRegions()) {
        // only check tag regions
        if (DOMRegionContext.XML_TAG_NAME.equals(documentRegion.getType())){
          for(ITextRegion textRegion: documentRegion.getRegions().toArray()){
            // find a project tag
            if(textRegion instanceof TagNameRegion && PROJECT_NODE.equals(documentRegion.getText(textRegion))){
              // check if schema is missing
              if (documentRegion.getText().lastIndexOf(XSI_SCHEMA_LOCATION) == -1) {
                int offset = documentRegion.getStartOffset();
                int lineNumber = document.getLineOfOffset(offset) + 1;
                IMarker marker = addMarker(pomFile, org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_error_noschema, lineNumber, IMarker.SEVERITY_WARNING, false);
                //the quick fix in the marker view needs to know the offset, since it doesn't have access to the
                //editor/source viewer
                if(marker != null){
                  marker.setAttribute(OFFSET, offset);
                  marker.setAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, "schema"); //$NON-NLS-1$ //$NON-NLS-2$
                  marker.setAttribute(IMarker.CHAR_START, documentRegion.getStartOffset());
                  marker.setAttribute(IMarker.CHAR_END, documentRegion.getEndOffset());
                }
              }
              // there could only be one project tag
              break documentLoop;
            }
          }
        }
      }
    } catch(Throwable ex) {
      MavenLogger.log("Error checking for schema", ex);
    }
    finally {
      if ( domModel != null ) {
        domModel.releaseFromRead();
      }
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
    String id = rex.getGroupId() + ":" + rex.getArtifactId() + ":" + rex.getVersion(); //$NON-NLS-1$ //$NON-NLS-2$
    if(rex.getClassifier() != null) {
      id += ":" + rex.getClassifier(); //$NON-NLS-1$
    }
    if(rex.getType() != null) {
      id += ":" + rex.getType(); //$NON-NLS-1$
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
        if(ex instanceof org.sonatype.aether.transfer.ArtifactNotFoundException) {
          // ignored here, handled by addMissingArtifactMarkers
        } else if(ex instanceof AbstractArtifactResolutionException) {
          AbstractArtifactResolutionException rex = (AbstractArtifactResolutionException) ex;
          String errorMessage = getArtifactId(rex) + " " + getErrorMessage(ex); //$NON-NLS-1$
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
          errorMessage = NLS.bind(org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_error_missing, artifact.toString());
//        } else {
//          errorMessage = "Missing indirectly referenced artifact " + artifact.toString();
//        }
        
        if(mavenConfiguration.isOffline()) {
          errorMessage = NLS.bind(org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_error_offline, errorMessage); 
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
