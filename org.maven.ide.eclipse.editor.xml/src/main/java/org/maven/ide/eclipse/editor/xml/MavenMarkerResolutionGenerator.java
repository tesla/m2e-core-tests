/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;


/**
 * MavenMarkerResolutionGenerator
 * 
 * @author dyocum
 */
public class MavenMarkerResolutionGenerator implements IMarkerResolutionGenerator {

  /* (non-Javadoc)
   * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
   */
  public IMarkerResolution[] getResolutions(IMarker marker) {
    String type;
    try {
      type = marker.getType();
    } catch(CoreException e) {
      MavenLogger.log(e);
      type = "";
    }
    if(IMavenConstants.MARKER_ID.equals(type)) {
      Integer offset = (Integer) marker.getAttribute("offset", -1);
      //only provide a quickfix for the schema marker
      if(offset != -1) {
        return new IMarkerResolution[] {new MavenMarkerResolution()};
      }
    }
    return new IMarkerResolution[0];
  }

}
