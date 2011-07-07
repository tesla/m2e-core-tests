
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


public class MavenProjectFacadeTest extends AbstractMavenProjectTestCase {

  public void testGetMojoExecution() throws Exception {
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);

    MojoExecutionKey compileKey = null;
    for(MojoExecutionKey executionKey : facade.getMojoExecutionMapping().keySet()) {
      if("maven-compiler-plugin".equals(executionKey.getArtifactId()) && "compile".equals(executionKey.getGoal())) {
        compileKey = executionKey;
        break;
      }
    }

    assertNotNull(compileKey);
    MojoExecution compileMojo = facade.getMojoExecution(compileKey, monitor);

    MavenExecutionRequest request = MavenPlugin.getMaven().createExecutionRequest(monitor);
    MavenSession session = MavenPlugin.getMaven().createSession(request, facade.getMavenProject());

    assertEquals("1.5", MavenPlugin.getMaven().getMojoParameterValue(session, compileMojo, "source", String.class));
    assertEquals("1.6", MavenPlugin.getMaven().getMojoParameterValue(session, compileMojo, "target", String.class));

  }

  public void testGetMojoExecutionAfterWorkspaceRestart() throws Exception {
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
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

    MavenExecutionRequest request = MavenPlugin.getMaven().createExecutionRequest(monitor);
    MavenSession session = MavenPlugin.getMaven().createSession(request, facade.getMavenProject());

    assertEquals("1.5", MavenPlugin.getMaven().getMojoParameterValue(session, compileMojo, "source", String.class));
    assertEquals("1.6", MavenPlugin.getMaven().getMojoParameterValue(session, compileMojo, "target", String.class));

  }

  public void testGetMojoExecutions() throws Exception {
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);

    List<MojoExecution> executions = facade.getMojoExecutions("org.apache.maven.plugins", "maven-compiler-plugin",
        monitor, "compile");
    assertEquals(executions.toString(), 1, executions.size());

    MavenExecutionRequest request = MavenPlugin.getMaven().createExecutionRequest(monitor);
    MavenSession session = MavenPlugin.getMaven().createSession(request, facade.getMavenProject());

    assertEquals("1.5", MavenPlugin.getMaven()
        .getMojoParameterValue(session, executions.get(0), "source", String.class));
    assertEquals("1.6", MavenPlugin.getMaven()
        .getMojoParameterValue(session, executions.get(0), "target", String.class));
  }

  public void testGetMojoExecutionsAfterWorkspaceRestart() throws Exception {
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
    deserialize(facade);

    List<MojoExecution> executions = facade.getMojoExecutions("org.apache.maven.plugins", "maven-compiler-plugin",
        monitor, "compile");
    assertEquals(executions.toString(), 1, executions.size());

    MavenExecutionRequest request = MavenPlugin.getMaven().createExecutionRequest(monitor);
    MavenSession session = MavenPlugin.getMaven().createSession(request, facade.getMavenProject());

    assertEquals("1.5", MavenPlugin.getMaven()
        .getMojoParameterValue(session, executions.get(0), "source", String.class));
    assertEquals("1.6", MavenPlugin.getMaven()
        .getMojoParameterValue(session, executions.get(0), "target", String.class));
  }

  public void testGetProjectConfigurators() throws Exception {
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
    assertNotNull("Expected not null MavenProjectFacade", facade);
    ILifecycleMapping lifecycleMapping = MavenPlugin.getProjectConfigurationManager().getLifecycleMapping(facade);
    assertNotNull("Expected not null LifecycleMapping", lifecycleMapping);

    List<AbstractProjectConfigurator> projectConfigurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertNotNull("Expected not null projectConfigurators", projectConfigurators);
    assertEquals(1, projectConfigurators.size());
  }

  public void testGetProjectConfiguratorsAfterWorkspaceRestart() throws Exception {
    IProject project = importProject("projects/getmojoexecution/pom.xml");
    assertNoErrors(project);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
    assertNotNull("Expected not null MavenProjectFacade", facade);
    deserialize(facade);

    ILifecycleMapping lifecycleMapping = MavenPlugin.getProjectConfigurationManager().getLifecycleMapping(facade);
    assertNotNull("Expected not null LifecycleMapping", lifecycleMapping);

    List<AbstractProjectConfigurator> projectConfigurators = lifecycleMapping.getProjectConfigurators(facade, monitor);
    assertNotNull("Expected not null projectConfigurators", projectConfigurators);
    assertEquals(1, projectConfigurators.size());
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

  public void testIsStale() throws Exception {
    IProject project = importProject("projects/testIsStale/pom.xml");
    waitForJobsToComplete();
    assertNoErrors(project);

    testIsStale(project, "pom.xml");
    for(IPath filename : ProjectRegistryManager.METADATA_PATH) {
      MavenUpdateRequest updateRequest = new MavenUpdateRequest(project, true /*offline*/, false /*updateSnapshots*/);
      MavenPluginActivator.getDefault().getMavenProjectManagerImpl().refresh(updateRequest, monitor);

      testIsStale(project, filename.toString());
    }
  }

  private void testIsStale(IProject project, String filename) throws Exception {
    IMavenProjectFacade projectFacade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
    assertFalse("Expected not stale MavenProjectFacade before changing the " + filename + " file",
        projectFacade.isStale());

    project.getFile(filename).touch(monitor);
    assertTrue("Expected stale MavenProjectFacade after changing the " + filename + " file", projectFacade.isStale());
  }
}
