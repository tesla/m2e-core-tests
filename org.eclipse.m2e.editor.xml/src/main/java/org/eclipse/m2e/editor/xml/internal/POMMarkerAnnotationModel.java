package org.eclipse.m2e.editor.xml.internal;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.wst.sse.ui.internal.StructuredResourceMarkerAnnotationModel;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.internal.project.MavenMarkerManager;

/**
 * created this file to get the proper lightbulb icon for the warnings with hint
 * @author mkleint
 */
public class POMMarkerAnnotationModel extends StructuredResourceMarkerAnnotationModel {

  public POMMarkerAnnotationModel(IResource resource) {
    super(resource);
  }

  public POMMarkerAnnotationModel(IResource resource, String secondaryID) {
    super(resource, secondaryID);
  }

  @Override
  protected MarkerAnnotation createMarkerAnnotation(IMarker marker) {
    try {
      if (marker.isSubtypeOf(IMavenConstants.MARKER_HINT_ID)) {
        MarkerAnnotation ann = new MarkerAnnotation(marker);
        ann.setQuickFixable(true);
        return ann;
      }
    } catch (CoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return super.createMarkerAnnotation(marker);
  }

}
