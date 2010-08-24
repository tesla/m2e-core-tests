
package org.maven.ide.eclipse.internal.project;

import org.maven.ide.eclipse.tests.common.AbstractMavenProjectTestCase;


public class MavenProjectChangedListenerExtensionTest extends AbstractMavenProjectTestCase {
  public void testExtension() throws Exception {
    TestMavenProjectChangedListener.events.clear();

    importProject("projects/projectimport/p001/pom.xml");
    waitForJobsToComplete();

    assertFalse(TestMavenProjectChangedListener.events.isEmpty());
  }
}
