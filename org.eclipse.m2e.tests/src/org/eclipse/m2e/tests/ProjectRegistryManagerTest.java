/*******************************************************************************
 * Copyright (c) 2008-2014 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.util.FileUtils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactRef;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.preferences.MavenConfigurationImpl;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryRefreshJob;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.jdt.internal.BuildPathManager;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.ClasspathHelpers;
import org.eclipse.m2e.tests.common.FilexWagon;
import org.eclipse.m2e.tests.common.MavenRunner;
import org.eclipse.m2e.tests.common.RequireMavenExecutionContext;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;

@RunWith(MavenRunner.class)
public class ProjectRegistryManagerTest extends AbstractMavenProjectTestCase {

  ProjectRegistryManager manager;

  ArrayList<MavenProjectChangedEvent> events;

  IMavenProjectChangedListener listener = (event, monitor) -> events.addAll(event);

  @Rule
  public TestName name = new TestName();

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    manager = MavenPluginActivator.getDefault().getMavenProjectManagerImpl();

    events = new ArrayList<>();
    manager.addMavenProjectChangedListener(listener);
  }

  @Override
  @After
  public void tearDown() throws Exception {
    try {
      manager.removeMavenProjectChangedListener(listener);
      listener = null;
      events = null;
      manager = null;
    } finally {
      super.tearDown();
    }
  }

  private IProject createExisting(String name) throws Exception {
    String test = name.substring(0, name.indexOf('-'));
    File dir = new File("resources/" + test, name);

    return createExisting(name, dir.getAbsolutePath());
  }

  private MavenProjectChangedEvent[] getEvents() {
    return events.toArray(new MavenProjectChangedEvent[events.size()]);
  }

  private Set<Artifact> getMavenProjectArtifacts(IMavenProjectFacade f1) throws CoreException {
    MavenProject mavenProject = f1.getMavenProject(monitor);
    return mavenProject.getArtifacts();
  }

  @Test
  public void test000_simple() throws Exception {
    IProject p1 = createExisting("t000-p1");
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(p1, monitor);
    assertEquals(p1.getFullPath(), f1.getFullPath());

    assertEquals("t000", f1.getMavenProject(monitor).getGroupId());
    assertEquals("t000-p1", f1.getMavenProject(monitor).getArtifactId());
    assertEquals("0.0.1-SNAPSHOT", f1.getMavenProject(monitor).getVersion());
  }

  @Test
  public void test000_eventMerge() throws Exception {
    IProject p1 = createExisting("t000-p1");
    waitForJobsToComplete();

    events.clear();

    // this emulates project refresh 
    IFile pom = p1.getFile("pom.xml");
    pom.setLocalTimeStamp(pom.getLocalTimeStamp() + 1000L);
    pom.touch(monitor);
    refreshMavenProject(p1);
    waitForJobsToComplete();

    assertEquals(1, events.size());
    assertEquals(MavenProjectChangedEvent.KIND_CHANGED, events.get(0).getKind());
  }

  @Test
  public void test000_pom_simple() throws Exception {
    IProject p1 = createExisting("t000-p1");
    waitForJobsToComplete();

    IFile pom = p1.getFile(IMavenConstants.POM_FILE_NAME);

    IMavenProjectFacade f1 = manager.create(pom, false, null);
    assertEquals(p1.getFullPath(), f1.getFullPath());

    assertEquals("t000", f1.getMavenProject(monitor).getGroupId());
    assertEquals("t000-p1", f1.getMavenProject(monitor).getArtifactId());
    assertEquals("0.0.1-SNAPSHOT", f1.getMavenProject(monitor).getVersion());
  }

  @Test
  public void test000_removeClosed() throws Exception {
    IProject p1 = createExisting("t000-p1");
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(p1, monitor);

    MavenProjectChangedEvent event;
//    assertEquals(1, events.size());
//    event = events.get(0);
//    assertEquals(MavenProjectChangedEvent.KIND_ADDED, event.getKind());
//    assertNull(event.getOldMavenProject());
//    assertSame(f1, event.getMavenProject());

    assertEquals(p1.getFullPath(), f1.getFullPath());

    events.clear();

    p1.close(monitor);
    waitForJobsToComplete();

    assertNull(manager.create(p1, monitor));

    assertEquals(1, events.size());
    event = events.get(0);
    assertEquals(MavenProjectChangedEvent.KIND_REMOVED, event.getKind());
    assertSame(f1, event.getOldMavenProject());
    assertNull(event.getMavenProject());
  }

  @Test
  public void test000_removeDeleted() throws Exception {
    IProject p1 = createExisting("t000-p1");
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(p1, monitor);
    assertEquals(p1.getFullPath(), f1.getFullPath());

    deleteProject(p1);
    waitForJobsToComplete();

    assertNull(manager.create(p1, monitor));
  }

  @Test
  public void test000_deletePom() throws Exception {
    IProject p1 = createExisting("t000-p1");
    waitForJobsToComplete();

    assertNotNull(manager.create(p1, monitor));

    deleteProject(p1);
    waitForJobsToComplete();

    assertNull(manager.create(p1, monitor));
  }

  public void test000_noChangeReload() throws Exception {
    IProject p1 = createExisting("t000-p1");
    IFile pom = p1.getFile(IMavenConstants.POM_FILE_NAME);
    waitForJobsToComplete();

    IMavenProjectFacade oldFacade = manager.create(p1, monitor);
    MavenProject oldMavenProject = oldFacade.getMavenProject(monitor);

    MavenProjectChangedEvent event;
//    assertEquals(1, events.size());
//    event = events.get(0);
//    assertEquals(pom, event.getSource());
//    assertEquals(MavenProjectChangedEvent.KIND_ADDED, event.getKind());

    events.clear();

    pom.setLocalTimeStamp(pom.getLocalTimeStamp() + 1000L);
    pom.touch(monitor);
    refreshMavenProject(p1);
    waitForJobsToComplete();

    IMavenProjectFacade newFacade = manager.create(p1, monitor);
    assertNotSame(oldMavenProject, newFacade.getMavenProject(monitor));

    assertEquals(1, events.size());
    event = events.get(0);
    assertEquals(pom, event.getSource());
    assertEquals(MavenProjectChangedEvent.KIND_CHANGED, event.getKind());
    assertEquals(MavenProjectChangedEvent.FLAG_NONE, event.getFlags());
    assertNotNull(event.getOldMavenProject());
    assertNotNull(event.getMavenProject());
  }

  @Test
  public void test001_missingParent() throws Exception {
    FileUtils.deleteDirectory(new File(repo, "t001"));

    IProject p2 = createExisting("t001-p2");
    waitForJobsToComplete();

    assertNull(manager.create(p2, monitor));

    IMarker[] markers = p2.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(WorkspaceHelpers.toString(markers), 1, markers.length);
    WorkspaceHelpers
        .assertErrorMarker(
            IMavenConstants.MARKER_POM_LOADING_ID,
            "Project build error: Non-resolvable parent POM for t001:t001-p2:0.0.1-SNAPSHOT: Could not find artifact t001:t001-p3:pom:0.0.1-SNAPSHOT in central (file:repositories/remoterepo) and 'parent.relativePath' points at wrong local POM",
            6 /*lineNumber*/, p2);

    IProject p3 = createExisting("t001-p3");
    waitForJobsToComplete();

    assertNotNull(manager.create(p2, monitor));

    deleteProject(p3);
    waitForJobsToComplete();

    assertNull(manager.create(p2, monitor));
  }

  @Test
  public void test001_dependencyOnModuleWithInheritedGroupAndVersion() throws Exception {
    IProject p1 = createExisting("t001-p1");
    IProject p2 = createExisting("t001-p2");
    IProject p3 = createExisting("t001-p3");
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(p1, monitor);
    Set<Artifact> artifacts = getMavenProjectArtifacts(f1);
    assertEquals(1, artifacts.size());
    Artifact a1 = artifacts.iterator().next();
    assertEquals(true, a1.isResolved());
    assertEquals(p2.getFile(IMavenConstants.POM_FILE_NAME).getLocation().toFile(), a1.getFile());

    IMavenProjectFacade f2 = manager.create(p2, monitor);
    assertNotNull(f2);

    IMavenProjectFacade f3 = manager.create(p3, monitor);
    assertNotNull(f3);
  }

  @Test
  public void test005_dependencyAvailableFromLocalRepoAndWorkspace() throws Exception {
    IProject p1 = createExisting("t005-p1");
    IProject p2 = createExisting("t005-p2");
    waitForJobsToComplete();

    {
      IMavenProjectFacade f = manager.create(p1, monitor);
      Set<Artifact> artifacts = getMavenProjectArtifacts(f);
      assertEquals(1, artifacts.size());
      Artifact a = artifacts.iterator().next();
      assertEquals(true, a.isResolved());
      assertEquals(p2.getFolder("target/classes").getLocation().toFile(), a.getFile());
    }

    deleteProject(p2);
    waitForJobsToComplete();

    {
      IMavenProjectFacade f = manager.create(p1, monitor);
      Set<Artifact> artifacts1 = getMavenProjectArtifacts(f);
      assertEquals(1, artifacts1.size());
      Artifact a = artifacts1.iterator().next();
      assertEquals(true, a.isResolved());
      // assertTrue(a.getFile().getAbsolutePath().startsWith(repo.getAbsolutePath()));
      assertStartWith(repo.getAbsolutePath(), a.getFile().getAbsolutePath());
    }
  }

  @Test
  public void test005_snapshotAvailableFromLocalRepoAndWorkspace() throws Exception {
    IProject p1 = createExisting("t005-p3");
    waitForJobsToComplete();

    {
      IMavenProjectFacade f = manager.create(p1, monitor);
      Set<Artifact> artifacts = getMavenProjectArtifacts(f);
      assertEquals(1, artifacts.size());
      Artifact a = artifacts.iterator().next();
      assertEquals(true, a.isResolved());
      // assertTrue(a.getFile().getAbsolutePath().startsWith(repo.getAbsolutePath()));
      assertStartWith(repo.getAbsolutePath(), a.getFile().getAbsolutePath());
    }

    IProject p2 = createExisting("t005-p4");
    waitForJobsToComplete();

    {
      IMavenProjectFacade f = manager.create(p1, monitor);
      Set<Artifact> artifacts = getMavenProjectArtifacts(f);
      assertEquals(1, artifacts.size());
      Artifact a = artifacts.iterator().next();
      assertEquals(true, a.isResolved());
      assertEquals(p2.getFile(IMavenConstants.POM_FILE_NAME).getLocation().toFile(), a.getFile());
    }
  }

  @RequireMavenExecutionContext
  @Test
  public void test006_parentAvailableFromLocalRepoAndWorkspace() throws Exception {
    IProject p1 = createExisting("t006-p1");
    IProject p2 = createExisting("t006-p2");
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(p1, monitor);
    getParentProject(f1);
    assertEquals(p2.getFile(IMavenConstants.POM_FILE_NAME).getLocation().toFile(), f1.getMavenProject(monitor)
        .getParentArtifact().getFile());
    assertEquals("workspace", f1.getMavenProject(monitor).getProperties().get("property"));

    deleteProject(p2);
    waitForJobsToComplete();

    f1 = manager.create(p1, monitor);
    getParentProject(f1);
    // assertTrue(f1.getMavenProject().getParent().getFile().getAbsolutePath().startsWith(repo.getAbsolutePath()));
    assertStartWith(repo.getAbsolutePath(), f1.getMavenProject(monitor).getParentArtifact().getFile().getAbsolutePath());
    assertEquals("repository", f1.getMavenProject(monitor).getProperties().get("property"));
  }

  @Test
  public void test006_parentAvailableFromLocalRepoAndWorkspace01() throws Exception {
    boolean oldSuspended = Job.getJobManager().isSuspended();

    Job.getJobManager().suspend();
    try {
      IProject p1 = createExisting("t006-p1");
      IProject p2 = createExisting("t006-p2");

      // sanity check
      assertNull(manager.getProject(p1));
      assertNull(manager.getProject(p2));

      manager.refresh(getPomFiles(p1, p2), monitor);

      IMavenProjectFacade f1 = manager.create(p1, monitor);
      assertEquals("workspace", f1.getMavenProject(monitor).getProperties().get("property"));

      p2.delete(true, monitor);
      manager.refresh(getPomFiles(p1, p2), monitor);

      f1 = manager.create(p1, monitor);
      assertEquals("repository", f1.getMavenProject(monitor).getProperties().get("property"));
    } finally {
      if(!oldSuspended) {
        Job.getJobManager().resume();
      }
    }
  }

  protected MavenProject getParentProject(final IMavenProjectFacade f) throws CoreException {
    // create execution context with proper resolver configuration
    IMavenExecutionContext context = manager.createExecutionContext(f.getPom(), f.getResolverConfiguration());
    return context.execute(f.getMavenProject(monitor), (context1, monitor) -> f.getMavenProject(monitor).getParent(),
        monitor);
  }

  @Test
  public void test007_staleDependencies() throws Exception {
    // p1 depends on p2
    IProject p1 = createExisting("t007-p1");
    IProject p2 = createExisting("t007-p2");
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(p1, monitor);
    assertEquals(1, f1.getMavenProject(monitor).getArtifacts().size());

    // update p1 to remove dependency on p2
    copyContent(p1, "pom_updated.xml", "pom.xml");

    f1 = manager.create(p1, monitor);
    assertEquals(0, f1.getMavenProject(monitor).getArtifacts().size());

    events.clear();

    // remove p2
    deleteProject(p2);
    waitForJobsToComplete();

    assertEquals(1, events.size());
    MavenProjectChangedEvent event = events.get(0);
    assertEquals(p2.getFile(IMavenConstants.POM_FILE_NAME), event.getSource());
    assertEquals(MavenProjectChangedEvent.KIND_REMOVED, event.getKind());
  }

  @Test
  public void test007_newTransitiveDependency() throws Exception {
    // p1 depends on p2
    IProject p1 = createExisting("t007-p1");
    IProject p2 = createExisting("t007-p2");
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(p1, monitor);
    assertEquals(1, f1.getMavenProject(monitor).getArtifacts().size());

    events.clear();

    // update p2 to have new dependency on junit
    copyContent(p2, "pom_newDependency.xml", "pom.xml");

    f1 = manager.create(p1, monitor);
    List<Artifact> a1 = new ArrayList<>(f1.getMavenProject(monitor).getArtifacts());
    assertEquals(2, a1.size());
    assertEquals("t007-p2", a1.get(0).getArtifactId());
    assertEquals("junit", a1.get(1).getArtifactId());

    assertContainsOnly(getProjectsFromEvents(events), p1, p2);
  }

  @Test
  public void test007_changedVersion() throws Exception {
    // p1 depends on p2
    IProject p1 = createExisting("t007-p1");
    IProject p2 = createExisting("t007-p2");
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(p1, monitor);
    List<Artifact> a1 = new ArrayList<>(f1.getMavenProject(monitor).getArtifacts());
    assertEquals(1, f1.getMavenProject(monitor).getArtifacts().size());
    assertEquals(p2.getFile(IMavenConstants.POM_FILE_NAME).getLocation().toFile(), a1.get(0).getFile());

    // update p2 to have new version
    copyContent(p2, "pom_newVersion.xml", "pom.xml");
    f1 = manager.create(p1, monitor);
    a1 = new ArrayList<>(f1.getMavenProject(monitor).getArtifacts());
    assertEquals(1, f1.getMavenProject(monitor).getArtifacts().size());
    assertStartWith(repo.getAbsolutePath(), a1.get(0).getFile().getAbsolutePath());

    // update p2 back to the original version
    copyContent(p2, "pom_original.xml", "pom.xml");
    f1 = manager.create(p1, monitor);
    a1 = new ArrayList<>(f1.getMavenProject(monitor).getArtifacts());
    assertEquals(1, f1.getMavenProject(monitor).getArtifacts().size());
    assertEquals(p2.getFile(IMavenConstants.POM_FILE_NAME).getLocation().toFile(), a1.get(0).getFile());
  }

  @Test
  public void test007_dependentRefreshAfterWorkspaceRestart() throws Exception {
    // p1 depends on p2
    IProject p1 = createExisting("t007-p1");
    IProject p2 = createExisting("t007-p2");
    waitForJobsToComplete();

    boolean origSuspended = Job.getJobManager().isSuspended();

    Job.getJobManager().suspend();
    try {
      // sanity check
      MavenProjectFacade f1 = manager.create(p1, monitor);
      List<Artifact> a1 = new ArrayList<>(f1.getMavenProject(monitor).getArtifacts());
      assertEquals(1, a1.size());
      assertEquals("t007-p2", a1.get(0).getArtifactId());

      // simulate workspace restart
      deserializeFromWorkspaceState(manager.create(p1, monitor));
      deserializeFromWorkspaceState(manager.create(p2, monitor));

      // add new dependency to p2, which should trigger update of p1
      copyContent(p2, "pom_newDependency.xml", "pom.xml", false /*don't wait for jobs to complete*/);
      manager.refresh(getPomFiles(p2), monitor);

      // assert p1 got refreshed
      f1 = manager.create(p1, monitor);
      a1 = new ArrayList<>(f1.getMavenProject(monitor).getArtifacts());
      assertEquals(2, a1.size());
      assertEquals("t007-p2", a1.get(0).getArtifactId());
      assertEquals("junit", a1.get(1).getArtifactId());
    } finally {
      if(!origSuspended) {
        Job.getJobManager().resume();
      }
    }
  }

  @Test
  public void test008_staleMissingParent() throws Exception {
    // p1 does not have parent
    IProject p1 = createExisting("t008-p1");
    assertNotNull(p1);
    IProject p3 = createExisting("t008-p3");
    assertNotNull(p3);
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(p1, monitor);
    assertNull(f1); // XXX should I return non-null facade that does not have MavenProject?

    // update p1 to have p3 parent
    copyContent(p1, "pom_updated.xml", "pom.xml");

    f1 = manager.create(p1, monitor);
    assertEquals("t008-p3", getParentProject(f1).getArtifactId());

    events.clear();
    IProject p2 = createExisting("t008-p2");
    waitForJobsToComplete();

    assertEquals(1, events.size());
    MavenProjectChangedEvent event = events.get(0);
    assertEquals(p2.getFile(IMavenConstants.POM_FILE_NAME), event.getSource());
    assertEquals(MavenProjectChangedEvent.KIND_ADDED, event.getKind());
  }

  @Test
  public void test008_staleMissingParent2() throws Exception {
    // p1 does not have parent
    IProject p1 = createExisting("t008-p1");
    waitForJobsToComplete();

    // update p1 to have p3 parent
    try (InputStream contents = p1.getFile("pom_updated.xml").getContents()) {
      p1.getFile("pom.xml").setContents(contents, IResource.FORCE, monitor);
    }
    waitForJobsToComplete();

    events.clear();
    IProject p2 = createExisting("t008-p2");
    waitForJobsToComplete();

    assertEquals(1, events.size());
    MavenProjectChangedEvent event = events.get(0);
    assertEquals(p2.getFile(IMavenConstants.POM_FILE_NAME), event.getSource());
    assertEquals(MavenProjectChangedEvent.KIND_ADDED, event.getKind());
  }

  @Test
  public void test009_noworkspaceResolution() throws Exception {
    IProject p1 = createExisting("t009-p1");
    IProject p2 = createExisting("t009-p2");
    IProject p3 = createExisting("t009-p3");
    waitForJobsToComplete();

    {
      IMavenProjectFacade f1 = manager.create(p1, monitor);
      IMavenProjectFacade f2 = manager.create(p2, monitor);

      assertEquals(2, f1.getMavenProjectArtifacts().size());
      Artifact[] a1 = getMavenProjectArtifacts(f1).toArray(new Artifact[0]);
      // assertTrue(a1[0].getFile().getAbsolutePath().startsWith(workspace.getRoot().getLocation().toFile().getAbsolutePath()));
      assertStartWith(workspace.getRoot().getLocation().toFile().getAbsolutePath(), a1[0].getFile().getAbsolutePath());
      assertEquals(p2.getFile("pom.xml").getLocation().toFile(), a1[1].getFile());

      assertEquals(1, f2.getMavenProjectArtifacts().size());
      Artifact a2 = getMavenProjectArtifacts(f2).iterator().next();
      // assertTrue(a2.getFile().getAbsolutePath().startsWith(repo.getAbsolutePath()));
      assertStartWith(repo.getAbsolutePath(), a2.getFile().getAbsolutePath());
    }

    this.events.clear();

    deleteProject(p3);
    waitForJobsToComplete();

    {
      IMavenProjectFacade f1 = manager.create(p1, monitor);
      IMavenProjectFacade f2 = manager.create(p2, monitor);
      assertNull(manager.create(p3, monitor));

      assertEquals(2, f1.getMavenProjectArtifacts().size());
      Artifact[] a1 = getMavenProjectArtifacts(f1).toArray(new Artifact[0]);
      // assertTrue(a1[0].getFile().getAbsolutePath().startsWith(repo.getAbsolutePath()));
      assertStartWith(repo.getAbsolutePath(), a1[0].getFile().getAbsolutePath());
      assertEquals(p2.getFile("pom.xml").getLocation().toFile(), a1[1].getFile());

      assertEquals(1, f2.getMavenProjectArtifacts().size());
      Artifact a2 = getMavenProjectArtifacts(f1).iterator().next();
      // assertTrue(a2.getFile().getAbsolutePath().startsWith(repo.getAbsolutePath()));
      assertStartWith(repo.getAbsolutePath(), a2.getFile().getAbsolutePath());
    }

    MavenProjectChangedEvent[] events = getEvents();
    assertEquals(2, events.length);
    assertEquals(p3.getFile(IMavenConstants.POM_FILE_NAME), events[0].getSource());
    assertEquals(p1.getFile(IMavenConstants.POM_FILE_NAME), events[1].getSource());

    this.events.clear();
    p2.delete(true, monitor);
    waitForJobsToComplete();

    {
      IMavenProjectFacade f1 = manager.create(p1, monitor);
      assertNull(manager.create(p2, monitor));
      assertNull(manager.create(p3, monitor));

      Artifact[] a1 = getMavenProjectArtifacts(f1).toArray(new Artifact[0]);
      // assertTrue(a1[0].getFile().getAbsolutePath().startsWith(repo.getAbsolutePath()));
      assertStartWith(repo.getAbsolutePath(), a1[0].getFile().getAbsolutePath());
      assertEquals(false, a1[1].isResolved());
    }

    events = getEvents();
    assertEquals(2, events.length);
    assertEquals(p2.getFile(IMavenConstants.POM_FILE_NAME), events[0].getSource());
    assertEquals(p1.getFile(IMavenConstants.POM_FILE_NAME), events[1].getSource());
  }

  @Test
  public void test012_downloadExternalDependency() throws Exception {
    IProject p1 = createExisting("t012-p1");
    waitForJobsToComplete();

    File jar = new File(repo + "/org/apache/logging/log4j/log4j-core/2.17.1", "log4-core-2.17-1.jar").getAbsoluteFile();
//    assertTrue(jar.exists());

    jar.delete();

    MavenUpdateRequest request = new MavenUpdateRequest(p1, false /*offline*/, false /*updateSnapshots*/);
    MavenPlugin.getMavenProjectRegistry().refresh(request);
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(p1, monitor);

    Artifact[] a1 = getMavenProjectArtifacts(f1).toArray(new Artifact[0]);
    assertEquals(1, a1.length);
    assertEquals(jar, a1[0].getFile());
    assertTrue(jar.exists());
  }

  @Test
  public void test013_cantParsePomMarker() throws Exception {
    IProject project = createExisting("t013-p1");
    waitForJobsToComplete();

    assertNull(manager.create(project, monitor));
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_POM_LOADING_ID, "Project build error: Non-readable POM ",
        1 /*lineNumber*/, project);

    copyContent(project, "pom_good.xml", "pom.xml");
    MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);
    WorkspaceHelpers.assertNoErrors(project);
    assertNotNull(manager.create(project, monitor));
  }

  @Test
  public void test013_missingDependencyMarker() throws Exception {
    IProject project = createExisting("t013-p2");
    waitForJobsToComplete();

    String expectedErrorMessage = "Missing artifact missing:missing:jar:0.0.0";
    List<IMarker> markers = WorkspaceHelpers.findErrorMarkers(project);
    // (jdt) The container 'Maven Dependencies' references non existing library ...missing/missing/0.0.0/missing-0.0.0.jar'
    // (maven) Missing artifact missing:missing:jar:0.0.0
    assertEquals(WorkspaceHelpers.toString(markers), 2, markers.size());
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_DEPENDENCY_ID, expectedErrorMessage, 9 /*lineNumber*/,
        project);

    workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();

    markers = WorkspaceHelpers.findErrorMarkers(project);
    // (jdt) The container 'Maven Dependencies' references non existing library ...missing/missing/0.0.0/missing-0.0.0.jar'
    // (jdt) The project cannot be built until build path errors are resolved
    // (maven) Missing artifact missing:missing:jar:0.0.0
    assertEquals(WorkspaceHelpers.toString(markers), 3, markers.size());
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_DEPENDENCY_ID, expectedErrorMessage, 9 /*lineNumber*/,
        project);
  }

  @RequireMavenExecutionContext
  @Test
  public void test015_refreshOffline() throws Exception {
    // XXX fix this test on Windows and remove this condition 
    if(System.getProperty("os.name", "").toLowerCase().indexOf("windows") > -1) {
      return;
    }

    IProject p1 = createExisting("t015-p1");
    waitForJobsToComplete();

    File file = new File(repo, "junit/junit/4.13.1/junit-4.13.1.jar");
    assertTrue("Can't delete file " + file.getAbsolutePath(), !file.exists() || file.delete());

    manager.refresh(getPomFiles(p1), monitor);
    assertEquals(false, file.exists());

    {
      IMavenProjectFacade f1 = manager.create(p1, monitor);
      List<Artifact> a1 = new ArrayList<>(getMavenProjectArtifacts(f1));
      assertEquals(false, a1.get(0).isResolved());
    }

    manager.refresh(getPomFiles(p1), monitor);
    assertEquals(true, file.exists());

    {
      IMavenProjectFacade f1 = manager.create(p1, monitor);
      List<Artifact> a1 = new ArrayList<>(getMavenProjectArtifacts(f1));
      assertEquals(true, a1.get(0).isResolved());
    }
  }

  @Test
  public void test017_moduleRefresh() throws Exception {
    IProject p2 = createExisting("t017-p2");
    IProject p3 = createExisting("t017-p3");
    IProject p4 = createExisting("t017-p4");
    waitForJobsToComplete();

    IFile pom = p3.getFile("pom.xml");

    MavenProject mavenProject = manager.create(pom, false, monitor).getMavenProject(monitor);
    Artifact a = mavenProject.getArtifacts().iterator().next();
    assertTrue(a.isResolved());
    assertEquals(p4.getFile(IMavenConstants.POM_FILE_NAME).getLocation().toFile().getAbsoluteFile(), a.getFile()
        .getAbsoluteFile());

    copyContent(p2, "pom_changed.xml", "pom.xml");

    mavenProject = manager.create(pom, false, null).getMavenProject(monitor);
    a = mavenProject.getArtifacts().iterator().next();
    assertFalse(a.isResolved());
  }

  @Test
  public void testOptionalDependencies() throws Exception {
    IProject p1 = createExisting("optionaldependency-p01");
    IProject p2 = createExisting("optionaldependency-p02");
    waitForJobsToComplete();

    IFile pom1 = p1.getFile("pom.xml");
    IFile pom2 = p2.getFile("pom.xml");

    {
      IMavenProjectFacade f = manager.create(pom2, false, null);
      Artifact[] a = getMavenProjectArtifacts(f).toArray(new Artifact[0]);
      assertEquals(2, a.length);
      assertEquals(pom1.getLocation().toFile().getCanonicalFile(), a[0].getFile().getCanonicalFile());
      assertEquals("junit-4.13.1.jar", a[1].getFile().getName());
    }

    copyContent(p1, "pom-changed.xml", "pom.xml");

    {
      IMavenProjectFacade f = manager.create(pom2, false, null);
      Artifact[] a = getMavenProjectArtifacts(f).toArray(new Artifact[0]);
      assertEquals(1, a.length);
      assertEquals(pom1.getLocation().toFile().getCanonicalFile(), a[0].getFile().getCanonicalFile());
    }
  }

  @Test
  public void testPropertiesSubstitution() throws Exception {
    IProject p1 = createExisting("t019-p1");
    waitForJobsToComplete();

    p1.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    IMavenProjectFacade f1 = manager.create(p1, monitor);
    MavenProject m1 = f1.getMavenProject(monitor);

    assertEquals("t019-p1", m1.getArtifactId());
    assertEquals("1.0.0-SNAPSHOT", m1.getVersion());
    assertEquals("plain description", m1.getDescription());
  }

  private void assertStartWith(String expected, String actual) {
    assertTrue("Expected to start with " + expected + " but got " + actual, actual.startsWith(expected));
  }

  @Test
  public void test020_moduleWithPomErrors() throws Exception {
    IProject p1 = createExisting("t020-p1");
    IProject p1m1 = createExisting("t020-p1-m1");
    waitForJobsToComplete();

    copyContent(p1, "pom_updated.xml", "pom.xml");

    IFile pom11 = p1m1.getFile("pom.xml");
    IMavenProjectFacade f11 = manager.create(pom11, false, null);

    Artifact a = f11.getMavenProject(monitor).getArtifacts().iterator().next();
    assertEquals("4.13.1", a.getVersion());
  }

  @Test
  public void testDependencyScopeChanged() throws Exception {
    //Changing the scope of a dependency should trigger a project update
    IProject p1 = createExisting("changedscope-p01");
    IProject p2 = createExisting("changedscope-p02");
    waitForJobsToComplete();

    IFile pom1 = p1.getFile("pom.xml");
    IFile pom2 = p2.getFile("pom.xml");

    {
      IMavenProjectFacade f = manager.create(pom2, false, null);
      Artifact[] a = getMavenProjectArtifacts(f).toArray(new Artifact[0]);
      assertEquals(2, a.length);
      assertEquals(pom1.getLocation().toFile().getCanonicalFile(), a[0].getFile().getCanonicalFile());
      assertEquals("junit-4.13.1.jar", a[1].getFile().getName());
    }

    copyContent(p1, "pom-scope-changed.xml", "pom.xml");

    {
      IMavenProjectFacade f = manager.create(pom2, false, null);
      Artifact[] a = getMavenProjectArtifacts(f).toArray(new Artifact[0]);
      assertEquals("provided scope dependency should disappear", 1, a.length);
      assertEquals(pom1.getLocation().toFile().getCanonicalFile(), a[0].getFile().getCanonicalFile());
    }
  }

  @Test
  public void testJdkProfileActivation() throws Exception {
    IProject[] projects = importProjects("projects/jdkprofileactivation",
        new String[] {"p001/pom.xml", "p002/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(projects[0], monitor);
    ArrayList<ArtifactRef> a1 = new ArrayList<>(f1.getMavenProjectArtifacts());
    assertEquals(2, a1.size());
    assertEquals("p002", a1.get(0).artifactKey().artifactId());
  }

  @Test
  public void testWorkspaceDependencyVersionRange() throws Exception {
    IProject[] projects = importProjects("projects/versionrange", new String[] {"p001/pom.xml", "p002/pom.xml"},
        new ResolverConfiguration());
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(projects[0], monitor);
    MavenProject p1 = f1.getMavenProject(monitor);
    ArrayList<Artifact> a1 = new ArrayList<>(p1.getArtifacts());
    assertEquals(1, a1.size());

    assertEquals(projects[1].getLocation().append("target/classes").toFile(), a1.get(0).getFile());
  }

  @Test
  public void testRepositoryMetadataCacheUsed() throws Exception {
    FileUtils.deleteDirectory(new File(repo, "mngeclipse1996"));
    String oldSettings = mavenConfiguration.getUserSettingsFile();
    try {
      injectFilexWagon();
      IJobChangeListener jobChangeListener = new JobChangeAdapter() {
        @Override
        public void scheduled(IJobChangeEvent event) {
          if(event.getJob() instanceof ProjectRegistryRefreshJob) {
            // cancel all those concurrent refresh jobs, we want to monitor the main thread only
            event.getJob().cancel();
            System.out.println(name.getMethodName() + ": ProjectRegistryRefreshJob was cancelled");
          }
        }
      };
      Job.getJobManager().addJobChangeListener(jobChangeListener);
      mavenConfiguration.setUserSettingsFile(new File("projects/MNGECLIPSE-1996/settings.xml").getAbsolutePath());
      waitForJobsToComplete();
      FilexWagon.setRequestFilterPattern("mngeclipse1996/.*xml", true);
      List<String> requests;
      try {
        importProjects("projects/MNGECLIPSE-1996", new String[] {"pom.xml", "mod-a/pom.xml", "mod-b/pom.xml",
            "mod-c/pom.xml", "mod-d/pom.xml", "mod-e/pom.xml"}, new ResolverConfiguration());
        requests = FilexWagon.getRequests();
      } finally {
        Job.getJobManager().removeJobChangeListener(jobChangeListener);
      }
      // up to 2 requests (for POM and JAR) are allowed, more would indicate an issue with the cache
      assertTrue("Accessed metadata " + requests.size() + " times: " + requests,
          requests.size() == 1 || requests.size() == 2 || requests.size() == 3);
    } finally {
      mavenConfiguration.setUserSettingsFile(oldSettings);
    }
  }

  @Test
  public void testEnvironmentVariablesConsidered() throws Exception {
    String tmpDir = System.getenv("TEMP");
    assertNotNull("This test requires the environment variable TEMP to be set", tmpDir);

    File systemJar = new File("projects/MNGECLIPSE-581/mngeclipse-581.jar");
    File tempJar = new File(tmpDir, "mngeclipse-581.jar");
    FileUtils.copyFile(systemJar, tempJar);
    tempJar.deleteOnExit();

    IProject[] projects = importProjects("projects/MNGECLIPSE-581", new String[] {"pom.xml"},
        new ResolverConfiguration());
    assertEquals(1, projects.length);
    assertNotNull(projects[0]);
    IMavenProjectFacade facade = manager.getProject(projects[0]);
    assertNotNull(facade);
    MavenProject project = facade.getMavenProject(monitor);
    assertNotNull(project);
    File file = project.getArtifacts().iterator().next().getFile();
    assertTrue(file.toString(), file.isFile());
  }

  @Test
  public void testWorkspaceResolutionApi() throws Exception {
    IProject[] projects = importProjects("projects/simple-pom", new String[] {"pom.xml"}, new ResolverConfiguration());
    assertEquals(1, projects.length);
    assertNotNull(projects[0]);
    Artifact artifact = new DefaultArtifact("org.eclipse.m2e.projects", "simple-pom", "1.0.0", null, "pom", "", null);
    artifact = manager.getWorkspaceLocalRepository().find(artifact);
    assertTrue(artifact.isResolved());
    assertNotNull(artifact.getFile());
    assertEquals(projects[0].getFile("pom.xml").getLocation().toFile(), artifact.getFile());
  }

  @Test
  public void testPluginMainArtifactNotResolvedFromWorkspace() throws Exception {
    IProject[] projects = importProjects("projects/MNGECLIPSE-2116",
        new String[] {"plugin/pom.xml", "project/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
    assertEquals(2, projects.length);
    assertNotNull(projects[0]);
    assertNotNull(projects[1]);
  }

  @RequireMavenExecutionContext
  @Test
  public void test021_dependencyVersionRange() throws Exception {
    IProject p1 = importProject("resources/t021_dependencyVersionRange/t021-p1/pom.xml");
    waitForJobsToComplete();

    MavenProjectFacade f1 = manager.create(p1, monitor);
    List<Artifact> a1 = new ArrayList<>(f1.getMavenProject(monitor).getArtifacts());
    assertEquals(1, a1.size());
    assertStartWith(repo.getAbsolutePath(), a1.get(0).getFile().getAbsolutePath());

    IProject p2 = importProject("resources/t021_dependencyVersionRange/t021-p2/pom.xml");
    waitForJobsToComplete();

    f1 = manager.create(p1, monitor);
    a1 = new ArrayList<>(f1.getMavenProject(monitor).getArtifacts());
    assertEquals(1, a1.size());
    assertEquals(p2.getFile(IMavenConstants.POM_FILE_NAME).getLocation().toFile(), a1.get(0).getFile());
  }

  @Test
  public void testGlobalUpdatePolicyNever() throws Exception {
    // clean local repo
    FileUtils.deleteDirectory(new File(repo, "updateTest/b"));

    // reset/setup "remote" repo
    File updatepolicyrepoDir = new File("target/updatepolicynever-repo");
    FileUtils.deleteDirectory(updatepolicyrepoDir);
    FileUtils.copyDirectoryStructure(new File("repositories/updateRepo1"), updatepolicyrepoDir);

    String origPolicy = mavenConfiguration.getGlobalUpdatePolicy();

    ((MavenConfigurationImpl) mavenConfiguration).setGlobalUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_NEVER);
    try {
      IProject p1 = importProject("projects/updatepolicynever/pom.xml");
      waitForJobsToComplete();

      MavenProjectFacade f1 = manager.create(p1, monitor);
      List<Artifact> a1 = new ArrayList<>(f1.getMavenProject(monitor).getArtifacts());
      assertEquals(1, a1.size());
      assertEquals("1.0-20110411.112213-72", a1.get(0).getVersion());

      FileUtils.copyDirectoryStructure(new File("repositories/updateRepo2"), updatepolicyrepoDir);

      manager.refresh(getPomFiles(p1), monitor);

      // assert dependency version did not change
      f1 = manager.create(p1, monitor);
      a1 = new ArrayList<>(f1.getMavenProject(monitor).getArtifacts());
      assertEquals(1, a1.size());
      assertEquals("1.0-20110411.112213-72", a1.get(0).getVersion());

      ((MavenConfigurationImpl) mavenConfiguration).setGlobalUpdatePolicy(null);

      manager.refresh(getPomFiles(p1), monitor);
      f1 = manager.create(p1, monitor);
      a1 = new ArrayList<>(f1.getMavenProject(monitor).getArtifacts());
      assertEquals(1, a1.size());
      assertEquals("1.0-20110411.112327-73", a1.get(0).getVersion());
    } finally {
      ((MavenConfigurationImpl) mavenConfiguration).setGlobalUpdatePolicy(origPolicy);
    }
  }

  @Test
  public void testCorrelatedMissingDependency() throws Exception {
    // clean local repo
    FileUtils.deleteDirectory(new File(repo, "updateTest/b"));

    // reset/setup "remote" repo
    File updatepolicyrepoDir = new File("target/correlatedMissingDependency-repo");
    FileUtils.deleteDirectory(updatepolicyrepoDir);

    // import two projects with the same missing dependency

    IProject[] projects = importProjects("projects/correlatedMissingDependency", new String[] {"p01/pom.xml",
        "p02/pom.xml"}, new ResolverConfiguration());

    MavenProjectFacade f1 = manager.create(projects[0], monitor);
    List<Artifact> a1 = new ArrayList<>(f1.getMavenProject(monitor).getArtifacts());
    assertEquals(1, a1.size());
    assertFalse(a1.get(0).isResolved());

    MavenProjectFacade f2 = manager.create(projects[1], monitor);
    List<Artifact> a2 = new ArrayList<>(f2.getMavenProject(monitor).getArtifacts());
    assertEquals(1, a2.size());
    assertFalse(a2.get(0).isResolved());

    // make the missing dependency available

    FileUtils.copyDirectoryStructure(new File("repositories/updateRepo1"), updatepolicyrepoDir);

    // refresh one of the two projects
    manager.refresh(getPomFiles(projects[0]), monitor);

    // both projects should now have resolved the missing dependency

    f1 = manager.create(projects[0], monitor);
    a1 = new ArrayList<>(f1.getMavenProject(monitor).getArtifacts());
    assertEquals(1, a1.size());
    assertTrue(a1.get(0).isResolved());

    f2 = manager.create(projects[1], monitor);
    a2 = new ArrayList<>(f2.getMavenProject(monitor).getArtifacts());
    assertEquals(1, a2.size());
    assertTrue(a2.get(0).isResolved());
  }

  @Test
  public void test022_noChangeReloadWithUnrelatedRemoveProject() throws Exception {
    IProject[] p = importProjects("resources/t022/", new String[] {"t022-p1/pom.xml", "t022-p2/pom.xml"},
        new ResolverConfiguration());
    waitForJobsToComplete();

    boolean origSuspended = Job.getJobManager().isSuspended();

    Job.getJobManager().suspend();
    try {
      this.events.clear();

      p[0].close(monitor);

      manager.refresh(getPomFiles(p), monitor);

      assertEquals(2, events.size());

      MavenProjectChangedEvent e0 = events.get(0); // close events always appear before add/change events
      assertEquals(p[0], e0.getOldMavenProject().getProject());
      assertNull(e0.getMavenProject());

      MavenProjectChangedEvent e1 = events.get(1);
      assertEquals(p[1], e1.getMavenProject().getProject());
      assertEquals(MavenProjectChangedEvent.KIND_CHANGED, e1.getKind());
      assertEquals(MavenProjectChangedEvent.FLAG_NONE, e1.getFlags());
    } finally {
      if(!origSuspended) {
        Job.getJobManager().resume();
      }
    }
  }

  public void bug343568_malformedDependencyElement() throws Exception {
    IProject p = importProject("projects/343568_missingAndAvailableDependencies/pom.xml");
    waitForJobsToComplete();
    MavenProjectFacade f = manager.create(p, monitor);
    List<Artifact> a = new ArrayList<>(f.getMavenProject(monitor).getArtifacts());
    assertEquals(1, a.size());
    assertTrue(a.get(0).isResolved());
    assertEquals("junit", a.get(0).getArtifactId());

    String expectedErrorMessage = "Project build error: 'dependencies.dependency.version' for missing:missing:jar is missing.";
    List<IMarker> markers = WorkspaceHelpers.findErrorMarkers(p);
    assertEquals(WorkspaceHelpers.toString(markers), 1, markers.size());
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_POM_LOADING_ID, expectedErrorMessage, 21 /*lineNumber*/,
        p);
  }

  @Test
  public void test356645_redundantSnapshotResolution() throws Exception {
    FileUtils.deleteDirectory(new File("target/356645localrepo"));

    mavenConfiguration.setUserSettingsFile("projects/356645_redundantSnapshotResolution/settings.xml");
    waitForJobsToComplete();
    injectFilexWagon();

    IProject[] projects = importProjects("projects/356645_redundantSnapshotResolution", new String[] {
        "projectA/pom.xml", "projectB/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();

    FilexWagon.setRequestFilterPattern("missing/missing/.*", true);

    Set<IFile> pomFiles = getPomFiles(projects);
    manager.refresh(pomFiles, monitor);

    assertEquals(3, FilexWagon.getRequests().size());
  }

  @Test
  public void test405090_staleBuildExtensionsResolutionError() throws Exception {
    FileUtils.deleteDirectory(new File("target/405090localrepo"));

    mavenConfiguration.setUserSettingsFile("projects/405090_staleBuildExtensionsResolutionError/settings.xml");
    waitForJobsToComplete();
    injectFilexWagon();

    FilexWagon.setRequestFailPattern(".*/test-lifecyclemapping-plugin/.*");

    IProject project = importProjects("projects/405090_staleBuildExtensionsResolutionError", new String[] {"/pom.xml"},
        new ResolverConfiguration(), true)[0];
    waitForJobsToComplete();

    String expectedErrorMessage = "Project build error: Unresolveable build extension: Plugin org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_POM_LOADING_ID, expectedErrorMessage, 1 /*lineNumber*/,
        project);

    FilexWagon.setRequestFailPattern(null);

    Set<IFile> pomFiles = getPomFiles(project);
    manager.refresh(pomFiles, monitor);

    assertNoErrors(project);
  }

  @Test
  public void test418674_ChecksumPolicyFail() throws Exception {
    // clean local repo
    FileUtils.deleteDirectory(new File(repo, "org/eclipse/m2e/test/bad-checksum"));

    // import two projects with the same missing dependency

    String originalPolicy = mavenConfiguration.getGlobalChecksumPolicy();
    try {
      setChecksumPolicy(ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL);
      IProject project = importProject("projects/418674_ChecksumPolicy/checksum-test/pom.xml");
      IJavaProject javaProject = JavaCore.create(project);
      assertHasMarker(project, IMavenConstants.MARKER_DEPENDENCY_ID, "Checksum validation failed");

      IClasspathEntry[] cp;
      cp = BuildPathManager.getMaven2ClasspathContainer(javaProject).getClasspathEntries();
      ClasspathHelpers.assertClasspath(new String[] {".*bad-checksum-0.0.1-SNAPSHOT.jar"}, cp);

      setChecksumPolicy(ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);

      Set<IFile> pomFiles = getPomFiles(project);
      manager.refresh(pomFiles, monitor);

      waitForJobsToComplete();
      assertNoErrors(project);

      cp = BuildPathManager.getMaven2ClasspathContainer(javaProject).getClasspathEntries();
      ClasspathHelpers.assertClasspath(new String[] {".*bad-checksum-0.0.1-SNAPSHOT.jar"}, cp);
    } finally {
      ((MavenConfigurationImpl) mavenConfiguration).setGlobalChecksumPolicy(originalPolicy);
    }
  }

  private void assertHasMarker(IProject project, String type, String messageSubstring) throws CoreException {
    IMarker[] markers = project.findMarkers(type, true /*subtypes*/, IResource.DEPTH_INFINITE);
    for(IMarker marker : markers) {
      String message = marker.getAttribute(IMarker.MESSAGE, "");
      if(message.contains(messageSubstring)) {
        return;
      }
    }
    fail("Expected markers not found, found messages " + toString(markers));
  }

  @Test
  public void test418674_ChecksumPolicyWarn() throws Exception {
    // clean local repo
    FileUtils.deleteDirectory(new File(repo, "org/eclipse/m2e/test/bad-checksum"));

    // import two projects with the same missing dependency

    String originalPolicy = mavenConfiguration.getGlobalChecksumPolicy();
    try {
      //repo is configured to fail if checksums don't match
      IProject project = importProject("projects/418674_ChecksumPolicy/checksum-test2/pom.xml");
      IJavaProject javaProject = JavaCore.create(project);
      assertHasMarker(project, IMavenConstants.MARKER_DEPENDENCY_ID, "Checksum validation failed");

      IClasspathEntry[] cp;
      cp = BuildPathManager.getMaven2ClasspathContainer(javaProject).getClasspathEntries();
      ClasspathHelpers.assertClasspath(new String[] {".*bad-checksum-0.0.1-SNAPSHOT.jar"}, cp);

      //Override specific repo config
      setChecksumPolicy(ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);

      Set<IFile> pomFiles = getPomFiles(project);
      manager.refresh(pomFiles, monitor);

      waitForJobsToComplete();
      assertNoErrors(project);

      cp = BuildPathManager.getMaven2ClasspathContainer(javaProject).getClasspathEntries();
      ClasspathHelpers.assertClasspath(new String[] {".*bad-checksum-0.0.1-SNAPSHOT.jar"}, cp);
    } finally {
      ((MavenConfigurationImpl) mavenConfiguration).setGlobalChecksumPolicy(originalPolicy);
    }
  }

  private void setChecksumPolicy(String value) {
    ((MavenConfigurationImpl) mavenConfiguration).setGlobalChecksumPolicy(value);
  }

  @Test
  public void test435692_affected_changes() throws Exception {

    // parent 1.0
    IProject parent = importProject("projects/435692_affected_changes/parent/pom.xml");

    // p1:1.0
    IProject p1 = importProject("projects/435692_affected_changes/p1/pom.xml");

    // depends on p1:1.0 (from workspace), parent 1.0 (workspace)
    IProject p2 = importProject("projects/435692_affected_changes/p2/pom.xml");

    // depends on p1:1.1 (from repo), parent, parent 1.0 (workspace)
    IProject p3 = importProject("projects/435692_affected_changes/p3/pom.xml");

    // depends on p1:2.0 (missing), parent 2.0 (repo)
    IProject p4 = importProject("projects/435692_affected_changes/p4/pom.xml");

    waitForJobsToComplete();

    // 1. update p1 to have new dependency on junit
    events.clear();
    copyContent(p1, "pom_newDependency.xml", "pom.xml");
    assertContainsOnly(getProjectsFromEvents(events), p1 /* self */, p2 /* 1.0 */, p4 /* 2.0 (unresolved) */);

    // 2. update p1's parent to a new version (should behave same as with dependency change)
    events.clear();
    copyContent(p1, "pom_changedParent.xml", "pom.xml"); // parent 1.0->2.0
    assertContainsOnly(getProjectsFromEvents(events), p1 /* self */, p2, p4);

    // 3. update p1 to a new version (should rebuild all versionless dependents)
    events.clear();
    copyContent(p1, "pom_changedVersion.xml", "pom.xml"); // 1.0->2.0
    assertContainsOnly(getProjectsFromEvents(events), p1 /* self */, p2, p3, p4);

    // 4. add new dependency to parent (similar to [1])
    events.clear();
    copyContent(parent, "pom_newDependency.xml", "pom.xml");
    // p1 at this point references parent 2.0 (from repo), same with p4
    assertContainsOnly(getProjectsFromEvents(events), parent /* self */, p2, p3);

    // 5. update parent to new version (similar to [3])
    events.clear();
    copyContent(parent, "pom_changedVersion.xml", "pom.xml"); // 1.0 -> 3.0
    assertContainsOnly(getProjectsFromEvents(events), parent /* self */, p1, p2, p3, p4);

  }

  @Test
  public void test436929_import_refresh() throws Exception {

    IProject[] projects = importProjects("projects/436929_import_refresh", new String[] {"p1/pom.xml", "deps/pom.xml",
        "deps2/pom.xml", "p2/pom.xml"}, new ResolverConfiguration());

    waitForJobsToComplete();

    // imports deps3 (from repo)
    IProject p1 = projects[0];

    // imports deps (from workspace) and deps3 (from repo), depends on p1
    IProject p2 = projects[3];

    // imported by p2 and specifies p1's version
    IProject managedDeps = projects[1];

    // imported by p1 and p2's parent
    IProject managedDeps2 = projects[2];

    // (jdt) The container 'Maven Dependencies' references non existing library ...test/436929-p1/2.0/436929-p1-2.0.jar
    // (maven) Missing artifact test:436929-p1:jar:2.0
    List<IMarker> markers = WorkspaceHelpers.findErrorMarkers(p2);
    assertEquals(WorkspaceHelpers.toString(markers), 2, markers.size());
    String expectedErrorMessage = "Missing artifact test:436929-p1:jar:2.0";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_DEPENDENCY_ID, expectedErrorMessage, 34 /*lineNumber*/,
        p2);

    // 1. imported directly
    events.clear();
    copyContent(managedDeps, "pom_modified.xml", "pom.xml");
    assertContainsOnly(getProjectsFromEvents(events), managedDeps /* self */, p2);
    assertNoErrors(p2);

    IMavenProjectFacade f2 = manager.create(p2, monitor);
    List<Artifact> a2 = new ArrayList<>(f2.getMavenProject(monitor).getArtifacts());
    assertEquals(1, a2.size());
    assertEquals("436929-p1", a2.get(0).getArtifactId());
    assertEquals("1.0", a2.get(0).getVersion());

    // 2. imported by parent which is not in the workspace 
    events.clear();
    IFile d2pom = managedDeps2.getFile("pom.xml");
    d2pom.setLocalTimeStamp(d2pom.getLocalTimeStamp() + 1000L);
    d2pom.touch(monitor);
    refreshMavenProject(managedDeps2);
    waitForJobsToComplete();

    /*
     * assertion would fail with p1 missing if deps2 does not exist in repository
     * since p1's parent (which imports deps2) will be set to null due to build failure
     */
    assertContainsOnly(getProjectsFromEvents(events), managedDeps2 /* self */, p1, p2);

    // 3. refresh on imported artifact download
    events.clear();

    // force download of deps3 from repo
    FileUtils.deleteDirectory(new File(repo, "test/436929-deps3"));
    Set<IFile> pomFiles = getPomFiles(p1);
    manager.refresh(pomFiles, monitor);
    // both p1 and p2 reference deps2
    assertContainsOnly(getProjectsFromEvents(events), p1 /* self */, p2);
  }

  @Test
  public void test441257_stalePluginRealms() throws Exception {
    IProject project = importProject("projects/441257_stalePluginRealms/basic/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);

    MavenProject mavenProject = manager.create(project, monitor).getMavenProject();

    ClassRealm projectRealm = mavenProject.getClassRealm();
    assertNotNull(projectRealm);
    ClassWorld world = projectRealm.getWorld();
    ClassRealm extensionRealm = world
        .getRealm("extension>org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0");
    assertNotNull(extensionRealm);

    Set<IFile> pomFiles = getPomFiles(project);
    manager.refresh(pomFiles, monitor);
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);

    mavenProject = manager.create(project, monitor).getMavenProject();
    assertNotSame(projectRealm, mavenProject.getClassRealm());
    assertNotSame(extensionRealm,
        world.getRealm("extension>org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0"));
  }

  @Test
  public void test441257_stalePluginRealms_withParent() throws Exception {
    IProject project = importProject("projects/441257_stalePluginRealms/with-parent/pom.xml");
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);

    MavenProject mavenProject = manager.create(project, monitor).getMavenProject();

    ClassRealm projectRealm = mavenProject.getClassRealm();
    assertNotNull(projectRealm);
    ClassWorld world = projectRealm.getWorld();
    ClassRealm extensionRealm = world
        .getRealm("extension>org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0");
    assertNotNull(extensionRealm);

    Set<IFile> pomFiles = getPomFiles(project);
    manager.refresh(pomFiles, monitor);
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertNoErrors(project);

    mavenProject = manager.create(project, monitor).getMavenProject();
    assertNotSame(projectRealm, mavenProject.getClassRealm());
    assertNotSame(extensionRealm,
        world.getRealm("extension>org.eclipse.m2e.test.lifecyclemapping:test-lifecyclemapping-plugin:1.0.0"));
  }

  @Test
  public void test453995_dependencyManagementVersionless() throws Exception {
    IProject project = importProject("projects/453995_dependencyManagementVersionless/pom.xml");
    waitForJobsToComplete();

    Set<IFile> pomFiles = getPomFiles(project);
    manager.refresh(pomFiles, monitor); // shouldn't throw any exceptions

    List<IMarker> markers = WorkspaceHelpers.findErrorMarkers(project);
    assertEquals(WorkspaceHelpers.toString(markers), 1, markers.size());
    String expectedErrorMessage = "Project build error: 'dependencyManagement.dependencies.dependency.version' for test:nonexisting:pom is missing.";
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_POM_LOADING_ID, expectedErrorMessage, 15 /*lineNumber*/,
        project);

  }

  @Test
  public void test460983_parentVersionRange() throws Exception {
    mavenConfiguration.setUserSettingsFile(new File("settings-filex.xml").getCanonicalPath());
    injectFilexWagon();

    FilexWagon.setRequestFilterPattern("460983_parentVersionRange/.*pom", true);

    IProject[] projects = importProjects("projects/460983_parentVersionRange", new String[] {"pom.xml",
        "module/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
    assertNoErrors(projects[0]);
    assertNoErrors(projects[1]);

    assertTrue(FilexWagon.getRequests().isEmpty());

    Set<IFile> pomFiles = getPomFiles(projects[1]);
    manager.refresh(pomFiles, monitor); // shouldn't throw any exceptions
    assertNoErrors(projects[1]);

    assertTrue(FilexWagon.getRequests().isEmpty());
  }

  @Test
  public void test463075_extensionAndPluginRealm() throws Exception {
    IProject[] projects = importProjects("projects/463075_extensionAndPluginRealm", new String[] {
        "extension-and-plugin/pom.xml", "extension-only/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
    assertNoErrors(projects[0]);
    assertNoErrors(projects[1]);

    Set<IFile> pomFiles = getPomFiles(projects[0]);
    manager.refresh(pomFiles, monitor);
    assertNoErrors(projects[0]);
  }
}
