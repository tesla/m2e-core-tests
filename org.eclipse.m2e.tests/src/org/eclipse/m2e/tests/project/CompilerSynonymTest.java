
package org.eclipse.m2e.tests.project;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;


public class CompilerSynonymTest extends AbstractMavenProjectTestCase {

  public void testSynonym() throws Exception {
    IProject project = importProject("projects/compilersynonym/pom.xml");
    assertNoErrors(project);
    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    assertEquals("1.6", javaProject.getOption(JavaCore.COMPILER_SOURCE, true));
    assertEquals("1.5", javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true));


  }

  private void deserialize(IMavenProjectFacade facade) throws IllegalArgumentException, IllegalAccessException {
    // pretend it was deserialized from workspace state
    for(Field field : facade.getClass().getDeclaredFields()) {
      if(Modifier.isTransient(field.getModifiers())) {
        field.setAccessible(true);
        field.set(facade, null);
      }
    }
  }
}
