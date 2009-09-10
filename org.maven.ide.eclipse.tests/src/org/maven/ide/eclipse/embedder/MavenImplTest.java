
package org.maven.ide.eclipse.embedder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.internal.embedder.MavenImpl;


public class MavenImplTest extends TestCase {

  private IProgressMonitor monitor = new NullProgressMonitor();

  private MavenImpl maven = (MavenImpl) MavenPlugin.lookup(IMaven.class);

  private IMavenConfiguration configuration = MavenPlugin.lookup(IMavenConfiguration.class);

  public void testGetMojoParameterValue() throws Exception {
    MavenExecutionRequest request = maven.createExecutionRequest(monitor);
    request.setPom(new File("projects/mojoparametervalue/pom.xml"));
    request.setGoals(Arrays.asList("compile"));

    MavenExecutionResult result = maven.readProject(request, monitor);
    assertFalse(result.hasExceptions());
    MavenProject project = result.getProject();

    MavenSession session = maven.createSession(request, project);

    MavenExecutionPlan executionPlan = maven.calculateExecutionPlan(request, project, monitor);

    MojoExecution execution = getExecution(executionPlan, "maven-compiler-plugin", "compile");

    assertEquals("1.7", maven.getMojoParameterValue(session, execution, "source", String.class));

    assertEquals(Arrays.asList("a", "b", "c"), maven.getMojoParameterValue(session, execution, "excludes", List.class));
  }

  private MojoExecution getExecution(MavenExecutionPlan executionPlan, String artifactId, String goal) {
    for(MojoExecution execution : executionPlan.getExecutions()) {
      if(artifactId.equals(execution.getArtifactId()) && goal.equals(execution.getGoal())) {
        return execution;
      }
    }
    fail("Execution plan does not contain " + artifactId + ":" + goal);
    return null;
  }

  public void testGetRepositories() throws Exception {
    String origSettings = configuration.getUserSettingsFile();
    try {
      configuration.setUserSettingsFile(new File("settingsWithCustomRepo.xml").getCanonicalPath());
      List<ArtifactRepository> repositories;
      
      // artifact repositories
      repositories = maven.getArtifactRepositories();
      assertEquals(3, repositories.size());
      assertEquals("http://central", repositories.get(0).getUrl());
      assertEquals("http:customremote", repositories.get(1).getUrl());
      assertEquals("http:customrepo", repositories.get(2).getUrl());

      // plugin repositories
      repositories = maven.getPluginArtifactRepositories();
      assertEquals(2, repositories.size());
      assertEquals("http://central", repositories.get(0).getUrl());
      assertEquals("http:customrepo", repositories.get(1).getUrl());      
    } finally {
      configuration.setUserSettingsFile(origSettings);
    }
  }

  public void testGetDefaultRepositories() throws Exception {
    String origSettings = configuration.getUserSettingsFile();
    try {
      configuration.setUserSettingsFile(new File("settingsWithDefaultRepos.xml").getCanonicalPath());
      List<ArtifactRepository> repositories;

      // artifact repositories
      repositories = maven.getArtifactRepositories();
      assertEquals(2, repositories.size());
      assertEquals("central", repositories.get(0).getId());
      assertEquals("custom", repositories.get(1).getId());

      // plugin repositories
      repositories = maven.getPluginArtifactRepositories();
      assertEquals(2, repositories.size());
      assertEquals("central", repositories.get(0).getId());
      assertEquals("custom", repositories.get(1).getId());
    } finally {
      configuration.setUserSettingsFile(origSettings);
    }
  }

  public void testIsUnavailable() throws Exception {
    RepositorySystem repositorySystem = maven.getPlexusContainer().lookup(RepositorySystem.class);

    Artifact a = repositorySystem.createArtifactWithClassifier("missing", "missing", "1.2.3", "jar", "sources");

    ArtifactRepository localRepository = maven.getLocalRepository();
    File localPath = new File(localRepository.getBasedir(), localRepository.pathOf(a)).getParentFile();
    FileUtils.deleteDirectory(localPath);

    // no remote repositories
    assertTrue(maven.isUnavailable(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(), a.getClassifier(), null));

    ArrayList<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
    repositories.add(repositorySystem.createDefaultRemoteRepository());

    // don't know yet if the artifact is available from defaultRepository
    assertFalse(maven.isUnavailable(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(), a.getClassifier(), repositories));

    // attempt resolve from default repository and verify unresolved
    assertFalse(maven.resolve(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(), a.getClassifier(), repositories, monitor).isResolved());

    // the artifact is known to be UNavailable from default remote repository
    assertTrue(maven.isUnavailable(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(), a.getClassifier(), repositories));

    // now lets try with custom repository
    repositories.add(repositorySystem.createArtifactRepository("foo", "bar", null, null, null));
    assertFalse(maven.isUnavailable(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(), a.getClassifier(), repositories));
    assertFalse(maven.resolve(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(), a.getClassifier(), repositories, monitor).isResolved());
    assertTrue(maven.isUnavailable(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(), a.getClassifier(), repositories));

  }
}
