
package org.eclipse.m2e.tests.project;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class CompilerSynonymTest extends AbstractMavenProjectTestCase {

  public void testSynonym() throws Exception {
    IProject project = importProject("projects/compilersynonym/pom.xml");
    assertNoErrors(project);
    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    assertEquals("1.6", javaProject.getOption(JavaCore.COMPILER_SOURCE, true));
    assertEquals("1.5", javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true));
  }

}
