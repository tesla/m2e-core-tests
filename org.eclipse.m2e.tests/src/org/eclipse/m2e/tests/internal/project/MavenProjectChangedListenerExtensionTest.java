
package org.eclipse.m2e.tests.internal.project;

import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class MavenProjectChangedListenerExtensionTest extends AbstractMavenProjectTestCase {
  public void testExtension() throws Exception {
    TestMavenProjectChangedListener.events.clear();

    importProject("projects/projectimport/p001/pom.xml");
    waitForJobsToComplete();

    assertFalse(TestMavenProjectChangedListener.events.isEmpty());
  }
}
