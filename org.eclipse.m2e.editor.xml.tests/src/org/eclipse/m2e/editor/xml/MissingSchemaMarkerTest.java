package org.eclipse.m2e.editor.xml;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;

public class MissingSchemaMarkerTest extends AbstractMavenProjectTestCase {
  public void testMissingSchemaMarker() throws Exception {
    ResolverConfiguration config = new ResolverConfiguration();
    IProject[] projects = importProjects("projects/MissingSchemaMarker", new String[] {"pom.xml"}, config);
    waitForJobsToComplete();

    IProject project = projects[0];
    IMarker[] markers = XmlEditorHelpers.findEditorHintWarningMarkers(project).toArray(new IMarker[0]);
    assertEquals(WorkspaceHelpers.toString(markers), 1, markers.length);
    XmlEditorHelpers.assertEditorHintWarningMarker(IMavenConstants.MARKER_POM_LOADING_ID,
        IMavenConstants.EDITOR_HINT_MISSING_SCHEMA, null /*message*/, 2 /*lineNumber*/, 1 /*resolutions*/,
        markers[0]);

    // Fix the problem - the marker should be removed
    copyContent(project, "pom_good.xml", "pom.xml");
    waitForJobsToComplete();
    XmlEditorHelpers.assertNoEditorHintWarningMarkers(project);
  }
}
