/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.editor.xml.internal.markers.MavenMarkerResolutionGenerator;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;
import org.junit.Assert;


public class XmlEditorHelpers extends WorkspaceHelpers {
  private static final MavenMarkerResolutionGenerator mavenMarkerResolutionGenerator = new MavenMarkerResolutionGenerator();

  public static List<IMarker> findEditorHintWarningMarkers(IProject project) throws CoreException {
    return WorkspaceHelpers.findMarkers(project, IMarker.SEVERITY_WARNING, IMavenConstants.MARKER_ATTR_EDITOR_HINT);
  }

  public static void assertNoEditorHintWarningMarkers(IProject project) throws Exception {
    List<IMarker> markers = XmlEditorHelpers.findEditorHintWarningMarkers(project);
    Assert.assertEquals(toString(markers), 0, markers.size());
  }

  public static void assertEditorHintWarningMarker(String type, String hintType, String message, Integer lineNumber,
      int resolutions, IMarker actual) throws Exception {
	  assertEditorMarker(IMarker.SEVERITY_WARNING, type, hintType, message, lineNumber, resolutions, actual);
  }
  
  public static void assertEditorHintErrorMarker(String type, String hintType, String message, Integer lineNumber,
      int resolutions, IMarker actual) throws Exception {
    assertEditorMarker(IMarker.SEVERITY_ERROR, type, hintType, message, lineNumber, resolutions, actual);
  }

  public static void assertEditorMarker(int severity, String type, String hintType, String message, Integer lineNumber,
      int resolutions, IMarker actual) throws Exception {
    Assert.assertNotNull("Expected not null marker", actual);
    String sMarker = toString(actual);
    Assert.assertEquals(sMarker, severity, actual.getAttribute(IMarker.SEVERITY));
    Assert.assertEquals(sMarker, type, actual.getType());
    Assert.assertEquals(sMarker, hintType, actual.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT));
    if(message != null) {
      String actualMessage = actual.getAttribute(IMarker.MESSAGE, "");
      Assert.assertTrue(sMarker, actualMessage.startsWith(message));
    }
    if(lineNumber != null) {
      Assert.assertEquals(sMarker, lineNumber, actual.getAttribute(IMarker.LINE_NUMBER));
    }
    if(type != null && type.startsWith(IMavenConstants.MARKER_ID)) {
      Assert.assertEquals(sMarker, false, actual.getAttribute(IMarker.TRANSIENT));
    }
    Assert.assertTrue(sMarker, mavenMarkerResolutionGenerator.hasResolutions(actual));
    Assert.assertEquals(sMarker, resolutions, mavenMarkerResolutionGenerator.getResolutions(actual).length);
  }
}
