package org.maven.ide.eclipse.embedder;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.MavenPlugin;

public class MavenImplTest extends TestCase {
  
  private IProgressMonitor monitor = new NullProgressMonitor();
  private IMaven maven = MavenPlugin.lookup(IMaven.class);

  public void testGetMojoParameterValue() throws Exception {
    MavenExecutionRequest request = maven.createExecutionRequest(monitor);
    request.setPom(new File("projects/mojoparametervalue/pom.xml"));
    request.setGoals(Arrays.asList("compile"));
    
    MavenExecutionResult result = maven.readProjectWithDependencies(request, monitor);
    assertFalse(result.hasExceptions());
    MavenProject project = result.getProject();

    MavenSession session = maven.createSession(request, project);

    MavenExecutionPlan executionPlan = maven.calculateExecutionPlan(request, project, monitor);

    MojoExecution execution = getExecution( executionPlan, "maven-compiler-plugin", "compile" );

    assertEquals("1.7", maven.getMojoParameterValue(session, execution, "source", String.class));

    assertEquals(Arrays.asList("a", "b", "c"), maven.getMojoParameterValue(session, execution, "excludes", List.class));
  }

  private MojoExecution getExecution(MavenExecutionPlan executionPlan, String artifactId, String goal) {
    for (MojoExecution execution : executionPlan.getExecutions()) {
      if (artifactId.equals(execution.getArtifactId()) && goal.equals(execution.getGoal())) {
        return execution;
      }
    }
    fail("Execution plan does not contain " + artifactId + ":" + goal);
    return null;
  }
}
