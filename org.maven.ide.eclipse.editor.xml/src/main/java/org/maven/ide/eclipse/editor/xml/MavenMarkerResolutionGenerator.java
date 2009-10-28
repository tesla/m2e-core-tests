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
    if(IMavenConstants.MARKER_ID.equals(marker.getId())) {
      Integer offset = (Integer) marker.getAttribute("offset", -1);
      //only provide a quickfix for the schema marker
      if(offset != -1) {
        return new IMarkerResolution[] {new MavenMarkerResolution()};
      }

    }
    return null;
  }

}
