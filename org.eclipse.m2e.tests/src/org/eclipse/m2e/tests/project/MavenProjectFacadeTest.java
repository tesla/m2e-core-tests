
package org.eclipse.m2e.tests.project;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class MavenProjectFacadeTest extends AbstractMavenProjectTestCase {

  public void testGetMojoExecution() throws Exception {
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = plugin.getMavenProjectManager().create(project, monitor);

    MojoExecutionKey compileKey = null;
    for(MojoExecutionKey executionKey : facade.getMojoExecutionMapping().keySet()) {
      if("maven-compiler-plugin".equals(executionKey.getArtifactId()) && "compile".equals(executionKey.getGoal())) {
        compileKey = executionKey;
        break;
      }
    }

    assertNotNull(compileKey);
    MojoExecution compileMojo = facade.getMojoExecution(compileKey, monitor);

    MavenExecutionRequest request = plugin.getMaven().createExecutionRequest(monitor);
    MavenSession session = plugin.getMaven().createSession(request, facade.getMavenProject());

    assertEquals("1.5", plugin.getMaven().getMojoParameterValue(session, compileMojo, "source", String.class));
    assertEquals("1.6", plugin.getMaven().getMojoParameterValue(session, compileMojo, "target", String.class));

  }

  public void testGetMojoExecutionAfterWorkspaceRestart() throws Exception {
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = plugin.getMavenProjectManager().create(project, monitor);
    deserialize(facade);

    MojoExecutionKey compileKey = null;
    for(MojoExecutionKey executionKey : facade.getMojoExecutionMapping().keySet()) {
      if("maven-compiler-plugin".equals(executionKey.getArtifactId()) && "compile".equals(executionKey.getGoal())) {
        compileKey = executionKey;
        break;
      }
    }

    assertNotNull(compileKey);
    MojoExecution compileMojo = facade.getMojoExecution(compileKey, monitor);

    MavenExecutionRequest request = plugin.getMaven().createExecutionRequest(monitor);
    MavenSession session = plugin.getMaven().createSession(request, facade.getMavenProject());

    assertEquals("1.5", plugin.getMaven().getMojoParameterValue(session, compileMojo, "source", String.class));
    assertEquals("1.6", plugin.getMaven().getMojoParameterValue(session, compileMojo, "target", String.class));

  }

  public void testGetMojoExecutions() throws Exception {
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = plugin.getMavenProjectManager().create(project, monitor);

    List<MojoExecution> executions = facade.getMojoExecutions("org.apache.maven.plugins", "maven-compiler-plugin",
        monitor, "compile");
    assertEquals(executions.toString(), 1, executions.size());

    MavenExecutionRequest request = plugin.getMaven().createExecutionRequest(monitor);
    MavenSession session = plugin.getMaven().createSession(request, facade.getMavenProject());

    assertEquals("1.5", plugin.getMaven().getMojoParameterValue(session, executions.get(0), "source", String.class));
    assertEquals("1.6", plugin.getMaven().getMojoParameterValue(session, executions.get(0), "target", String.class));
  }

  public void testGetMojoExecutionsAfterWorkspaceRestart() throws Exception {
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = plugin.getMavenProjectManager().create(project, monitor);
    deserialize(facade);

    List<MojoExecution> executions = facade.getMojoExecutions("org.apache.maven.plugins", "maven-compiler-plugin",
        monitor, "compile");
    assertEquals(executions.toString(), 1, executions.size());

    MavenExecutionRequest request = plugin.getMaven().createExecutionRequest(monitor);
    MavenSession session = plugin.getMaven().createSession(request, facade.getMavenProject());

    assertEquals("1.5", plugin.getMaven().getMojoParameterValue(session, executions.get(0), "source", String.class));
    assertEquals("1.6", plugin.getMaven().getMojoParameterValue(session, executions.get(0), "target", String.class));
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
