
package org.eclipse.m2e.tests.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class CompilerSettings18Test extends AbstractMavenProjectTestCase {

  public void testCompilerSettings18() throws Exception {
    IProject project = importProject("projects/compilerSettings18/pom.xml");
    assertNoErrors(project);
    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    assertEquals("1.8", javaProject.getOption(JavaCore.COMPILER_SOURCE, true));
    assertEquals("1.8", javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true));
  }

}
