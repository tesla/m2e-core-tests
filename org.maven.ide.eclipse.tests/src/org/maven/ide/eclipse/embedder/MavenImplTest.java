
package org.maven.ide.eclipse.embedder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.internal.embedder.MavenImpl;
import org.maven.ide.eclipse.internal.repository.RepositoryRegistry;
import org.maven.ide.eclipse.tests.common.AbstractMavenProjectTestCase;


public class MavenImplTest extends AbstractMavenProjectTestCase {

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
      repositories = maven.getArtifactRepositories(false);
      assertEquals(3, repositories.size());
      assertEquals("http://central", repositories.get(0).getUrl());
      assertEquals("http:customremote", repositories.get(1).getUrl());
      assertEquals("http:customrepo", repositories.get(2).getUrl());

      // plugin repositories
      repositories = maven.getPluginArtifactRepositories(false);
      assertEquals(2, repositories.size());
      assertEquals("http://central", repositories.get(0).getUrl());
      assertEquals("http:customrepo", repositories.get(1).getUrl());      
    } finally {
      configuration.setUserSettingsFile(origSettings);
    }
  }

  public void testGetRepositoriesWithSettingsInjection() throws Exception {
    String origSettings = configuration.getUserSettingsFile();
    try {
      configuration.setUserSettingsFile(new File("settingsWithCustomRepo.xml").getCanonicalPath());
      List<ArtifactRepository> repositories;
      
      // artifact repositories
      repositories = maven.getArtifactRepositories(true);
      assertEquals(2, repositories.size());
      assertEquals("nexus", repositories.get(0).getId());
      assertEquals("file:remoterepo", repositories.get(0).getUrl());
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
      repositories = maven.getArtifactRepositories(false);
      assertEquals(2, repositories.size());
      assertEquals("central", repositories.get(0).getId());
      assertEquals("custom", repositories.get(1).getId());

      // plugin repositories
      repositories = maven.getPluginArtifactRepositories(false);
      assertEquals(2, repositories.size());
      assertEquals("central", repositories.get(0).getId());
      assertEquals("custom", repositories.get(1).getId());
    } finally {
      configuration.setUserSettingsFile(origSettings);
    }
  }

  public void testGlobalSettings() throws Exception {
    String userSettings = configuration.getUserSettingsFile();
    String globalSettings = configuration.getGlobalSettingsFile();
    
    try {
      configuration.setUserSettingsFile(new File("settings_empty.xml").getCanonicalPath());
      configuration.setGlobalSettingsFile(new File("settings_empty.xml").getCanonicalPath());

      // sanity check
      assertEquals("http://repo1.maven.org/maven2", maven.getArtifactRepositories().get(0).getUrl());
      
      configuration.setGlobalSettingsFile(new File("settingsWithCustomRepo.xml").getCanonicalPath());
      assertEquals("file:remoterepo", maven.getArtifactRepositories().get(0).getUrl());
    } finally {
      configuration.setUserSettingsFile(userSettings);
      configuration.setGlobalSettingsFile(globalSettings);
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
    try {
      maven.resolve(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(), a.getClassifier(), repositories, monitor);
      fail();
    } catch (CoreException e) {
      // expected
    }

    // the artifact is known to be UNavailable from default remote repository
    assertTrue(maven.isUnavailable(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(), a.getClassifier(), repositories));

    // now lets try with custom repository
    repositories.add(repositorySystem.createArtifactRepository("foo", "bar", null, null, null));
    assertFalse(maven.isUnavailable(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(), a.getClassifier(), repositories));
    try {
      maven.resolve(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(), a.getClassifier(), repositories, monitor);
      fail();
    } catch (CoreException e) {
      // expected
    }
    assertTrue(maven.isUnavailable(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(), a.getClassifier(), repositories));

  }

  public void testLocalRepositoryListener() throws Exception {
    List<ArtifactRepository> repositories = maven.getArtifactRepositories(true);
    final ArtifactRepository localRepository = maven.getLocalRepository();

    FileUtils.deleteDirectory(new File(localRepository.getBasedir(), "junit/junit/3.8.2"));

    ILocalRepositoryListener listener = new ILocalRepositoryListener() {
      public void artifactInstalled(File repositoryBasedir, ArtifactKey artifact, File artifactFile) {
        assertEquals(localRepository.getBasedir(), repositoryBasedir.getAbsolutePath());

        assertEquals("junit:junit:3.8.2::", artifact.toPortableString());
        
        assertEquals(new File(localRepository.getBasedir(), "junit/junit/3.8.2/junit-3.8.2.jar"), artifactFile.getAbsoluteFile());
      }
    };

    maven.addLocalRepositoryListener(listener);
    try {
      maven.resolve("junit", "junit", "3.8.2", "jar", null, repositories, monitor);
    } finally {
      maven.removeLocalRepositoryListener(listener);
    }
  
  }

  public void testMissingArtifact() throws Exception {
    try {
      maven.resolve("missing", "missing", "0", "unknown", null, null, monitor);
      fail();
    } catch (CoreException e) {
      assertFalse(e.getStatus().isOK());
    }
  }

  public void testGetProxy() throws Exception {
    String origSettings = configuration.getUserSettingsFile();
    try {
      configuration.setUserSettingsFile(new File("src/org/maven/ide/eclipse/embedder/settings-with-proxy.xml")
          .getCanonicalPath());
      ProxyInfo proxy = maven.getProxyInfo("http");
      assertEquals("rso", proxy.getHost());
      assertEquals(80, proxy.getPort());
      assertEquals("http", proxy.getType());
      assertEquals("user", proxy.getUserName());
      assertEquals("pass", proxy.getPassword());
      assertEquals("*", proxy.getNonProxyHosts());
    } finally {
      configuration.setUserSettingsFile(origSettings);
    }
  }

  public void testUnreadableSettings() throws Exception {
    String origSettings = configuration.getUserSettingsFile();
    try {
      configuration.setUserSettingsFile(new File("src/org/maven/ide/eclipse/embedder/settings-unreadable.xml")
          .getCanonicalPath());
      assertNotNull(maven.getSettings());
      assertNotNull(maven.getLocalRepository());
      ((RepositoryRegistry) MavenPlugin.getDefault().getRepositoryRegistry()).updateRegistry(null);
    } finally {
      configuration.setUserSettingsFile(origSettings);
    }
  }

  public void testReadProjectWithDependenciesFromMirror() throws Exception {
    String origSettings = configuration.getUserSettingsFile();
    try {
      configuration.setUserSettingsFile(new File("src/org/maven/ide/eclipse/embedder/settings-mirror.xml")
          .getCanonicalPath());
      assertFalse(maven.getSettings().getMirrors().isEmpty());
      MavenExecutionRequest request = maven.createExecutionRequest(monitor);
      request.getProjectBuildingRequest().setResolveDependencies(true);
      request.setPom(new File("projects/dependencies/pom.xml"));
      MavenExecutionResult result = maven.readProject(request, monitor);
      assertFalse(result.hasExceptions());
      assertFalse(result.getArtifactResolutionResult().getMissingArtifacts().toString(), result
          .getArtifactResolutionResult().hasMissingArtifacts());
      assertFalse(result.getArtifactResolutionResult().hasExceptions());
      assertNotNull(result.getProject());
      assertNotNull(result.getProject().getArtifacts());
      assertEquals(result.getProject().getArtifacts().toString(), 2, result.getProject().getArtifacts().size());
    } finally {
      configuration.setUserSettingsFile(origSettings);
    }
  }

}
