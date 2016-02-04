/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.embedder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.codehaus.plexus.util.FileUtils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.wagon.proxy.ProxyInfo;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.ILocalRepositoryListener;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.embedder.MavenExecutionContext;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.internal.repository.RepositoryRegistry;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.FileHelpers;
import org.eclipse.m2e.tests.common.HttpServer;


@SuppressWarnings("restriction")
public class MavenImplTest extends AbstractMavenProjectTestCase {

  private IProgressMonitor monitor = new NullProgressMonitor();

  MavenImpl maven = (MavenImpl) MavenPlugin.getMaven();

  private IMavenConfiguration configuration = MavenPlugin.getMavenConfiguration();

  public void testGetMojoParameterValue() throws Exception {
    MavenExecutionResult result = readMavenProject(new File("projects/mojoparametervalue/pom.xml"), false);
    assertFalse(result.hasExceptions());
    MavenProject project = result.getProject();

    MavenExecutionPlan executionPlan = maven.calculateExecutionPlan(project, Arrays.asList("compile"), true, monitor);

    MojoExecution execution = getExecution(executionPlan, "maven-compiler-plugin", "compile");

    assertEquals("1.7", maven.getMojoParameterValue(project, execution, "source", String.class, monitor));

    assertEquals(Arrays.asList("a", "b", "c"),
        maven.getMojoParameterValue(project, execution, "excludes", List.class, monitor));
  }

  private MojoExecution getExecution(MavenExecutionPlan executionPlan, String artifactId, String goal) {
    for(MojoExecution execution : executionPlan.getMojoExecutions()) {
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
      assertEquals("file:repositories/remoterepo", repositories.get(0).getUrl());
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
      assertEquals("https://repo.maven.apache.org/maven2", maven.getArtifactRepositories().get(0).getUrl());

      configuration.setGlobalSettingsFile(new File("settingsWithCustomRepo.xml").getCanonicalPath());
      assertEquals("file:repositories/remoterepo", maven.getArtifactRepositories().get(0).getUrl());
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
    assertTrue(maven.isUnavailable(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(),
        a.getClassifier(), null));

    ArrayList<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
    repositories.add(repositorySystem.createDefaultRemoteRepository());

    // don't know yet if the artifact is available from defaultRepository
    assertFalse(maven.isUnavailable(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(),
        a.getClassifier(), repositories));

    // attempt resolve from default repository and verify unresolved
    try {
      maven.resolve(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(), a.getClassifier(),
          repositories, monitor);
      fail();
    } catch(CoreException e) {
      // expected
    }

    // the artifact is known to be UNavailable from default remote repository
    assertTrue(maven.isUnavailable(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(),
        a.getClassifier(), repositories));

    // now lets try with custom repository
    repositories.add(repositorySystem.createArtifactRepository("foo", "bar", null, null, null));
    assertFalse(maven.isUnavailable(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(),
        a.getClassifier(), repositories));
    try {
      maven.resolve(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(), a.getClassifier(),
          repositories, monitor);
      fail();
    } catch(CoreException e) {
      // expected
    }
    assertTrue(maven.isUnavailable(a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getType(),
        a.getClassifier(), repositories));

  }

  public void testLocalRepositoryListener() throws Exception {
    List<ArtifactRepository> repositories = maven.getArtifactRepositories(true);
    final ArtifactRepository localRepository = maven.getLocalRepository();

    FileUtils.deleteDirectory(new File(localRepository.getBasedir(), "junit/junit/3.8.2"));

    ILocalRepositoryListener listener = new ILocalRepositoryListener() {
      public void artifactInstalled(File repositoryBasedir, ArtifactKey baseArtifact, ArtifactKey artifact,
          File artifactFile) {
        assertEquals(localRepository.getBasedir(), repositoryBasedir.getAbsolutePath());

        assertEquals("junit:junit:3.8.2::", artifact.toPortableString());

        assertEquals(new File(localRepository.getBasedir(), "junit/junit/3.8.2/junit-3.8.2.jar"),
            artifactFile.getAbsoluteFile());
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
    } catch(CoreException e) {
      assertFalse(e.getStatus().isOK());
    }
  }

  public void testGetProxy() throws Exception {
    String origSettings = configuration.getUserSettingsFile();
    try {
      configuration.setUserSettingsFile(new File("src/org/eclipse/m2e/tests/embedder/settings-with-proxy.xml")
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
      configuration.setUserSettingsFile(new File("src/org/eclipse/m2e/tests/embedder/settings-unreadable.xml")
          .getCanonicalPath());
      assertNotNull(maven.getSettings());
      assertNotNull(maven.getLocalRepository());
      ((RepositoryRegistry) MavenPlugin.getRepositoryRegistry()).updateRegistry(null);
    } finally {
      configuration.setUserSettingsFile(origSettings);
    }
  }

  public void testReadProjectWithDependenciesFromMirror() throws Exception {
    String origSettings = configuration.getUserSettingsFile();
    try {
      configuration.setUserSettingsFile(new File("src/org/eclipse/m2e/tests/embedder/settings-mirror.xml")
          .getCanonicalPath());
      assertFalse(maven.getSettings().getMirrors().isEmpty());
      MavenExecutionResult result = readMavenProject(new File("projects/dependencies/pom.xml"), true);
      assertFalse(result.getExceptions().toString(), result.hasExceptions());
      assertTrue(result.getDependencyResolutionResult().getUnresolvedDependencies().toString(), result
          .getDependencyResolutionResult().getUnresolvedDependencies().isEmpty());
      assertTrue(result.getDependencyResolutionResult().getCollectionErrors().isEmpty());
      assertNotNull(result.getProject());
      assertNotNull(result.getProject().getArtifacts());
      assertEquals(result.getProject().getArtifacts().toString(), 2, result.getProject().getArtifacts().size());
    } finally {
      configuration.setUserSettingsFile(origSettings);
    }
  }

  public void testProxySupport() throws Exception {
    FileHelpers.deleteDirectory(new File("target/localrepo/org/eclipse/m2e/test/mngeclipse-2126"));

    HttpServer httpServer = new HttpServer();
    httpServer.addResources("/", "");
    httpServer.addSecuredRealm("/repositories/*", "auth");
    httpServer.addUser("testuser", "testpass", "auth");
    httpServer.setProxyAuth("proxyuser", "proxypass");
    httpServer.enableRecording(".*mngeclipse-2126.*");
    httpServer.start();

    String origSettings = configuration.getUserSettingsFile();
    try {
      File settingsFile = new File("target/settings-mngeclipse-2126.xml");
      FileHelpers.filterXmlFile(new File("projects/MNGECLIPSE-2126/settings-template.xml"), settingsFile,
          Collections.singletonMap("@port.http@", Integer.toString(httpServer.getHttpPort())));
      configuration.setUserSettingsFile(settingsFile.getCanonicalPath());

      MavenExecutionResult result = readMavenProject(new File("projects/MNGECLIPSE-2126/pom.xml"), true);

      assertFalse(httpServer.getRecordedRequests().isEmpty());
      assertFalse(result.getExceptions().toString(), result.hasExceptions());
      assertTrue(result.getDependencyResolutionResult().getUnresolvedDependencies().toString(), result
          .getDependencyResolutionResult().getUnresolvedDependencies().isEmpty());
      assertTrue(result.getDependencyResolutionResult().getCollectionErrors().isEmpty());
      assertNotNull(result.getProject());
      assertNotNull(result.getProject().getArtifacts());
      assertEquals(result.getProject().getArtifacts().toString(), 1, result.getProject().getArtifacts().size());

      MavenExecutionPlan plan = maven.calculateExecutionPlan(result.getProject(), Arrays.asList("verify"), true,
          monitor);
      assertEquals(plan.getMojoExecutions().toString(), 2, plan.getMojoExecutions().size());
    } finally {
      configuration.setUserSettingsFile(origSettings);
      httpServer.stop();
    }
  }

  // Standard JDK SSL implementation reads javax.net.ssl.keyStore/trustStore system properties once and
  // ignores all subsequent properties value changes. Because of this limitation, this test cannot be run 
  // together with any other code that uses SSL, which is impractical. DISABLED the tests to avoid false
  // automated test failures, but we can still run the test manually if there are any concerns about
  // javax.net.ssl.keyStore/trustStore system properties support.
  public void DISABLED_testSslWithMutualHandshake() throws Exception {
    FileHelpers.deleteDirectory(new File("target/localrepo/org/eclipse/m2e/its/mngeclipse-2149"));

    HttpServer httpServer = new HttpServer();
    httpServer.setHttpPort(-1);
    httpServer.setHttpsPort(0);
    httpServer.addResources("/", "");
    httpServer.setKeyStore("resources/ssl/server-store", "server-pwd");
    httpServer.setTrustStore("resources/ssl/client-store", "client-pwd");
    httpServer.setNeedClientAuth(true);
    httpServer.enableRecording(".*mngeclipse-2149.*");
    httpServer.start();

    String origSettings = configuration.getUserSettingsFile();
    Properties props = System.getProperties();
    try {
      System.setProperty("javax.net.ssl.keyStore", new File("resources/ssl/client-store").getAbsolutePath());
      System.setProperty("javax.net.ssl.keyStorePassword", "client-pwd");
      System.setProperty("javax.net.ssl.keyStoreType", "jks");
      System.setProperty("javax.net.ssl.trustStore", new File("resources/ssl/server-store").getAbsolutePath());
      System.setProperty("javax.net.ssl.trustStorePassword", "server-pwd");
      System.setProperty("javax.net.ssl.trustStoreType", "jks");

      File settingsFile = new File("target/settings-mngeclipse-2149.xml");
      FileHelpers.filterXmlFile(new File("projects/MNGECLIPSE-2149/settings-template.xml"), settingsFile,
          Collections.singletonMap("@port.https@", Integer.toString(httpServer.getHttpsPort())));
      configuration.setUserSettingsFile(settingsFile.getCanonicalPath());

      MavenExecutionResult result = readMavenProject(new File("projects/MNGECLIPSE-2149/pom.xml"), true);

      assertFalse(httpServer.getRecordedRequests().isEmpty());
      assertFalse(result.getExceptions().toString(), result.hasExceptions());
      assertTrue(result.getDependencyResolutionResult().getUnresolvedDependencies().toString(), result
          .getDependencyResolutionResult().getUnresolvedDependencies().isEmpty());
      assertTrue(result.getDependencyResolutionResult().getCollectionErrors().isEmpty());
      assertNotNull(result.getProject());
      assertNotNull(result.getProject().getArtifacts());
      assertEquals(result.getProject().getArtifacts().toString(), 1, result.getProject().getArtifacts().size());
    } finally {
      System.setProperties(props);
      configuration.setUserSettingsFile(origSettings);
      httpServer.stop();
    }
  }

  public void testReadLocalParent() throws Exception {
    MavenExecutionResult result = readMavenProject(new File("projects/readparent/local/module01/pom.xml"), false);
    assertFalse(result.hasExceptions());
    MavenProject project = result.getProject();

    MavenProject parent = maven.resolveParentProject(project, monitor);
    assertEquals("local-parent", parent.getArtifactId());
  }

  public void testReadRemoteParent() throws Exception {
    MavenExecutionResult result = readMavenProject(new File("projects/readparent/remote/module02/pom.xml"), false);
    assertFalse(result.hasExceptions());
    MavenProject project = result.getProject();

    MavenProject parent = maven.resolveParentProject(project, monitor);
    assertEquals("remote-parent", parent.getArtifactId());
    assertNotNull("remote-parent.file", parent.getFile());
    assertNotNull("remote-parent.artifact.file", parent.getArtifact().getFile());
  }

  public void testReadNoParent() throws Exception {
    MavenExecutionResult result = readMavenProject(new File("projects/readparent/noparent/pom.xml"), false);
    assertFalse(result.hasExceptions());
    MavenProject project = result.getProject();

    MavenProject parent = maven.resolveParentProject(project, monitor);
    assertNull(parent);
  }

  public void testMNGECLIPSE2556_settingsSystemPropertiesSubstiution() throws Exception {
    String origSettings = configuration.getUserSettingsFile();
    try {
      configuration.setUserSettingsFile(new File("resources/settings/settingsWithSystemPropertySubstitution.xml")
          .getCanonicalPath());

      ArtifactRepository repository = maven.getLocalRepository();

      File location = new File(System.getProperty("user.home"), ".m2/repository-my-project").getCanonicalFile();

      assertEquals(location, new File(repository.getBasedir()).getCanonicalFile());
    } finally {
      configuration.setUserSettingsFile(origSettings);
    }
  }

  public void test358620_reparse_changed_user_settings() throws Exception {
    String origSettings = configuration.getUserSettingsFile();
    try {
      File settings = new File("target/358620_settings.xml").getCanonicalFile();

      FileUtils.copyFile(new File("resources/358620_reparse_changed_user_settings/settings.xml"), settings);
      configuration.setUserSettingsFile(settings.getCanonicalPath());

      assertEquals(0, maven.getSettings().getActiveProfiles().size());

      FileUtils.copyFile(new File("resources/358620_reparse_changed_user_settings/settings.xml-changed"), settings);

      assertEquals(1, maven.getSettings().getActiveProfiles().size());
    } finally {
      configuration.setUserSettingsFile(origSettings);
    }
  }

  public void test366839_userAgent() throws Exception {
    HttpServer httpServer = new HttpServer();
    httpServer.addResources("/", "");
    httpServer.enableRecording(".*");
    httpServer.start();

    String origSettings = configuration.getUserSettingsFile();
    try {
      File settingsFile = new File("target/settings-366839.xml");
      FileHelpers.filterXmlFile(new File("projects/366839_user_agent/settings-template.xml"), settingsFile,
          Collections.singletonMap("@port.http@", Integer.toString(httpServer.getHttpPort())));
      configuration.setUserSettingsFile(settingsFile.getCanonicalPath());

      try {
        FileHelpers.deleteDirectory(new File("target/localrepo/missing"));
        maven.resolve("missing", "missing", "1", "jar", null, null, monitor);
      } catch(CoreException ignored) {
        // we only check http request headers
      }

      assertFalse(httpServer.getRecordedRequests().isEmpty());
      for(String httpRequest : httpServer.getRecordedRequests()) {
        String uri = httpRequest.split(" ")[1];
        assertEquals(MavenPluginActivator.getUserAgent(), httpServer.getRecordedHeaders(uri).get("User-Agent"));
      }

      httpServer.resetRecording();

      FileHelpers.deleteDirectory(new File("target/localrepo/missing"));
      readMavenProject(new File("projects/366839_user_agent/pom.xml"), false);

      assertFalse(httpServer.getRecordedRequests().isEmpty());
      for(String httpRequest : httpServer.getRecordedRequests()) {
        String uri = httpRequest.split(" ")[1];
        assertEquals(MavenPluginActivator.getUserAgent(), httpServer.getRecordedHeaders(uri).get("User-Agent"));
      }

    } finally {
      configuration.setUserSettingsFile(origSettings);
      httpServer.stop();
    }
  }

  private MavenExecutionResult readMavenProject(final File pomFile, final boolean resolveDependencies)
      throws CoreException {
    return readMavenProject(pomFile, resolveDependencies, monitor);
  }

  private MavenExecutionResult readMavenProject(final File pomFile, final boolean resolveDependencies,
      IProgressMonitor monitor) throws CoreException {
    return maven.execute(new ICallable<MavenExecutionResult>() {
      public MavenExecutionResult call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
        ProjectBuildingRequest configuration = context.newProjectBuildingRequest();
        configuration.setResolveDependencies(resolveDependencies);
        return maven.readMavenProject(pomFile, configuration);
      }
    }, monitor);
  }

  public void test386196_ParallelDownloads() throws Exception {
    IProgressMonitor monitor = new NullProgressMonitor() {

      private Pattern pattern = Pattern.compile("(\\d*)%");

      public void subTask(String name) {
        Matcher m = pattern.matcher(name);
        if(m.find()) {
          String progress = m.group(1);
          assertFalse("Unexpected progress value :" + name, Integer.parseInt(progress) > 100);
        }
      }
    };

    String origSettings = configuration.getUserSettingsFile();
    try {
      configuration.setUserSettingsFile(new File("src/org/eclipse/m2e/tests/embedder/settings-emptylocal.xml")
          .getCanonicalPath());

      FileHelpers.deleteDirectory(new File("target/emptylocalrepo/"));

      readMavenProject(new File("projects/386196-parallel-downloads/pom.xml"), true, monitor);

    } finally {
      configuration.setUserSettingsFile(origSettings);
    }
  }

  public void test438454_guiceScopedComponentInjection() throws Exception {
    // the point of this test is to verify that @MojoExecutionScoped component can be instantiated and injected

    MavenExecutionResult result = readMavenProject(new File("projects/438454_guiceScopes/pom.xml"), false);
    assertFalse(result.hasExceptions());
    MavenProject project = result.getProject();
    MavenExecutionPlan executionPlan = maven.calculateExecutionPlan(project, Arrays.asList("compile"), true, monitor);
    final MojoExecution execution = getExecution(executionPlan, "438454_guicescopes-plugin", "guicescopes");

    MavenExecutionContext context = maven.createExecutionContext();
    result = context.execute(project, new ICallable<MavenExecutionResult>() {
      public MavenExecutionResult call(IMavenExecutionContext context, IProgressMonitor monitor) {
        MavenSession session = context.getSession();
        maven.execute(session, execution, monitor);
        return session.getResult();
      }
    }, monitor);

    assertFalse(result.getExceptions().toString(), result.hasExceptions());
  }

  public void test486737_readProject() throws Exception {
    // make sure MavenImpl.readProject() works with an active Extension
    MavenProject mavenProject = MavenPlugin.getMaven().readProject(new File("projects/486737_lifecycleParticipant/pom.xml"), monitor);
    
    // the Extension changes the source folder
    assertTrue(mavenProject.getModel().getBuild().getSourceDirectory().endsWith("dummySrc"));
  }
  
  public void test486737_lifecycleParticipant() throws Exception {
    MavenExecutionResult result = readMavenProject(new File("projects/486737_lifecycleParticipant/pom.xml"), false);
    assertFalse(result.hasExceptions());

    MavenProject project = result.getProject();

    // the Extension changes the source folder
    assertTrue(project.getModel().getBuild().getSourceDirectory().endsWith("dummySrc"));
  }
}
