/*******************************************************************************
 * Copyright (c) 2008-2014 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import org.codehaus.plexus.util.FileUtils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactRef;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.embedder.MavenExecutionContext;
import org.eclipse.m2e.core.internal.preferences.MavenConfigurationImpl;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryRefreshJob;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.FilexWagon;
import org.eclipse.m2e.tests.common.RequireMavenExecutionContext;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


public class ProjectRegistryManagerTest extends AbstractMavenProjectTestCase {

  ProjectRegistryManager manager;

  ArrayList<MavenProjectChangedEvent> events;

  IMavenProjectChangedListener listener = new IMavenProjectChangedListener() {
    public void mavenProjectChanged(MavenProjectChangedEvent[] event, IProgressMonitor monitor) {
      events.addAll(Arrays.asList(event));
    }
  };

  protected void setUp() throws Exception {
    super.setUp();

    manager = MavenPluginActivator.getDefault().getMavenProjectManagerImpl();

    events = new ArrayList<MavenProjectChangedEvent>();
    manager.addMavenProjectChangedListener(listener);
  }

  protected void tearDown() throws Exception {
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

  public void test000_simple() throws Exception {
    IProject p1 = createExisting("t000-p1");
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(p1, monitor);
    assertEquals(p1.getFullPath(), f1.getFullPath());

    assertEquals("t000", f1.getMavenProject(monitor).getGroupId());
    assertEquals("t000-p1", f1.getMavenProject(monitor).getArtifactId());
    assertEquals("0.0.1-SNAPSHOT", f1.getMavenProject(monitor).getVersion());
  }

  public void test000_eventMerge() throws Exception {
    IProject p1 = createExisting("t000-p1");
    waitForJobsToComplete();

    events.clear();

    // this emulates project refresh 
    IFile pom = p1.getFile("pom.xml");
    pom.setLocalTimeStamp(pom.getLocalTimeStamp() + 1000L);
    pom.touch(monitor);
    waitForJobsToComplete();

    assertEquals(1, events.size());
    assertEquals(MavenProjectChangedEvent.KIND_CHANGED, events.get(0).getKind());
  }

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

  public void test000_removeDeleted() throws Exception {
    IProject p1 = createExisting("t000-p1");
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(p1, monitor);
    assertEquals(p1.getFullPath(), f1.getFullPath());

    deleteProject(p1);
    waitForJobsToComplete();

    assertNull(manager.create(p1, monitor));
  }

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
            "Project build error: Non-resolvable parent POM: Could not find artifact t001:t001-p3:pom:0.0.1-SNAPSHOT in central (file:repositories/remoterepo) and 'parent.relativePath' points at wrong local POM",
            6 /*lineNumber*/, p2);

    IProject p3 = createExisting("t001-p3");
    waitForJobsToComplete();

    assertNotNull(manager.create(p2, monitor));

    deleteProject(p3);
    waitForJobsToComplete();

    assertNull(manager.create(p2, monitor));
  }

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

  public void _test003_projectsWithDuplicateGroupArtifactVersion() throws Exception {
    fail("Implement me");
  }

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
      assertEquals(p2.getFile(IMavenConstants.POM_FILE_NAME).getLocation().toFile(), a.getFile());
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

  public void test006_parentAvailableFromLocalRepoAndWorkspace01() throws Exception {
    boolean oldSuspended = Job.getJobManager().isSuspended();

    Job.getJobManager().suspend();
    try {
      IProject p1 = createExisting("t006-p1");
      IProject p2 = createExisting("t006-p2");

      // sanity check
      assertNull(manager.getProject(p1));
      assertNull(manager.getProject(p2));

      MavenUpdateRequest request = new MavenUpdateRequest(new IProject[] {p1, p2}, false, true);
      manager.refresh(request, monitor);

      IMavenProjectFacade f1 = manager.create(p1, monitor);
      assertEquals("workspace", f1.getMavenProject(monitor).getProperties().get("property"));

      p2.delete(true, monitor);
      manager.refresh(request, monitor);

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
    MavenExecutionContext context = manager.createExecutionContext(f.getPom(), f.getResolverConfiguration());
    return context.execute(f.getMavenProject(monitor), new ICallable<MavenProject>() {
      public MavenProject call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
        return MavenPlugin.getMaven().resolveParentProject(f.getMavenProject(monitor), monitor);
      }
    }, monitor);
  }

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
    List<Artifact> a1 = new ArrayList<Artifact>(f1.getMavenProject(monitor).getArtifacts());
    assertEquals(2, a1.size());
    assertEquals("t007-p2", a1.get(0).getArtifactId());
    assertEquals("junit", a1.get(1).getArtifactId());

    assertEquals(2, events.size());
    assertEquals(p1.getFile(IMavenConstants.POM_FILE_NAME), events.get(0).getSource());
    assertEquals(p2.getFile(IMavenConstants.POM_FILE_NAME), events.get(1).getSource());
  }

  public void test007_changedVersion() throws Exception {
    // p1 depends on p2
    IProject p1 = createExisting("t007-p1");
    IProject p2 = createExisting("t007-p2");
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(p1, monitor);
    List<Artifact> a1 = new ArrayList<Artifact>(f1.getMavenProject(monitor).getArtifacts());
    assertEquals(1, f1.getMavenProject(monitor).getArtifacts().size());
    assertEquals(p2.getFile(IMavenConstants.POM_FILE_NAME).getLocation().toFile(), a1.get(0).getFile());

    // update p2 to have new version
    copyContent(p2, "pom_newVersion.xml", "pom.xml");
    f1 = manager.create(p1, monitor);
    a1 = new ArrayList<Artifact>(f1.getMavenProject(monitor).getArtifacts());
    assertEquals(1, f1.getMavenProject(monitor).getArtifacts().size());
    assertStartWith(repo.getAbsolutePath(), a1.get(0).getFile().getAbsolutePath());

    // update p2 back to the original version
    copyContent(p2, "pom_original.xml", "pom.xml");
    f1 = manager.create(p1, monitor);
    a1 = new ArrayList<Artifact>(f1.getMavenProject(monitor).getArtifacts());
    assertEquals(1, f1.getMavenProject(monitor).getArtifacts().size());
    assertEquals(p2.getFile(IMavenConstants.POM_FILE_NAME).getLocation().toFile(), a1.get(0).getFile());
  }

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
      List<Artifact> a1 = new ArrayList<Artifact>(f1.getMavenProject(monitor).getArtifacts());
      assertEquals(1, a1.size());
      assertEquals("t007-p2", a1.get(0).getArtifactId());

      // simulate workspace restart
      deserializeFromWorkspaceState(manager.create(p1, monitor));
      deserializeFromWorkspaceState(manager.create(p2, monitor));

      // add new dependency to p2, which should trigger update of p1
      copyContent(p2, "pom_newDependency.xml", "pom.xml", false /*don't wait for jobs to complete*/);
      manager.refresh(new MavenUpdateRequest(p2, false, false), monitor);

      // assert p1 got refreshed
      f1 = manager.create(p1, monitor);
      a1 = new ArrayList<Artifact>(f1.getMavenProject(monitor).getArtifacts());
      assertEquals(2, a1.size());
      assertEquals("t007-p2", a1.get(0).getArtifactId());
      assertEquals("junit", a1.get(1).getArtifactId());
    } finally {
      if(!origSuspended) {
        Job.getJobManager().resume();
      }
    }
  }

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
    InputStream contents = p1.getFile("pom_updated.xml").getContents();
    p1.getFile("pom.xml").setContents(contents, IResource.FORCE, monitor);
    contents.close();
    waitForJobsToComplete();

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

  public void test008_staleMissingParent2() throws Exception {
    // p1 does not have parent
    IProject p1 = createExisting("t008-p1");
    waitForJobsToComplete();

    // update p1 to have p3 parent
    InputStream contents = p1.getFile("pom_updated.xml").getContents();
    p1.getFile("pom.xml").setContents(contents, IResource.FORCE, monitor);
    contents.close();
    waitForJobsToComplete();

    events.clear();
    IProject p2 = createExisting("t008-p2");
    waitForJobsToComplete();

    assertEquals(1, events.size());
    MavenProjectChangedEvent event = events.get(0);
    assertEquals(p2.getFile(IMavenConstants.POM_FILE_NAME), event.getSource());
    assertEquals(MavenProjectChangedEvent.KIND_ADDED, event.getKind());
  }

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

  public void test012_downloadExternalDependency() throws Exception {
    IProject p1 = createExisting("t012-p1");
    waitForJobsToComplete();

    File jar = new File(repo + "/log4j/log4j/1.2.13", "log4j-1.2.13.jar").getAbsoluteFile();
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
  public void test015_refreshOffline() throws Exception {
    // XXX fix this test on Windows and remove this condition 
    if(System.getProperty("os.name", "").toLowerCase().indexOf("windows") > -1) {
      return;
    }

    IProject p1 = createExisting("t015-p1");
    waitForJobsToComplete();

    File file = new File(repo, "junit/junit/3.8.1/junit-3.8.1.jar");
    assertTrue("Can't delete file " + file.getAbsolutePath(), !file.exists() || file.delete());

    MavenUpdateRequest updateRequest = new MavenUpdateRequest(true /*offline*/, false /* updateSources */);
    updateRequest.addPomFile(p1);
    manager.refresh(updateRequest, monitor);
    assertEquals(false, file.exists());

    {
      IMavenProjectFacade f1 = manager.create(p1, monitor);
      List<Artifact> a1 = new ArrayList<Artifact>(getMavenProjectArtifacts(f1));
      assertEquals(false, a1.get(0).isResolved());
    }

    updateRequest = new MavenUpdateRequest(false /*offline*/, false /* updateSources */);
    updateRequest.addPomFile(p1);
    manager.refresh(updateRequest, monitor);
    assertEquals(true, file.exists());

    {
      IMavenProjectFacade f1 = manager.create(p1, monitor);
      List<Artifact> a1 = new ArrayList<Artifact>(getMavenProjectArtifacts(f1));
      assertEquals(true, a1.get(0).isResolved());
    }
  }

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
      assertEquals("junit-3.8.1.jar", a[1].getFile().getName());
    }

    copyContent(p1, "pom-changed.xml", "pom.xml");

    {
      IMavenProjectFacade f = manager.create(pom2, false, null);
      Artifact[] a = getMavenProjectArtifacts(f).toArray(new Artifact[0]);
      assertEquals(1, a.length);
      assertEquals(pom1.getLocation().toFile().getCanonicalFile(), a[0].getFile().getCanonicalFile());
    }
  }

  /**
   * This test disabled until https://issues.sonatype.org/browse/MNGECLIPSE-1448 is resolved
   */
  public void _testExtensionPluginResolution() throws Exception {
    IProject p1 = createExisting("MNGECLIPSE380-plugin", "resources/MNGECLIPSE380/plugin");
    IProject p2 = createExisting("MNGECLIPSE380-project", "resources/MNGECLIPSE380/project");
    waitForJobsToComplete();

    p1.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    IFile pom2 = p2.getFile("pom.xml");
    assertNotNull(manager.create(pom2, false, null));

    deleteProject(p1);
    waitForJobsToComplete();
    assertNull(manager.create(pom2, false, null));

    assertNotNull(manager.create(pom2, false, null));
  }

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

  public void test020_moduleWithPomErrors() throws Exception {
    IProject p1 = createExisting("t020-p1");
    IProject p1m1 = createExisting("t020-p1-m1");
    waitForJobsToComplete();

    copyContent(p1, "pom_updated.xml", "pom.xml");

    IFile pom11 = p1m1.getFile("pom.xml");
    IMavenProjectFacade f11 = manager.create(pom11, false, null);

    Artifact a = f11.getMavenProject(monitor).getArtifacts().iterator().next();
    assertEquals("3.8.1", a.getVersion());
  }

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
      assertEquals("junit-3.8.1.jar", a[1].getFile().getName());
    }

    copyContent(p1, "pom-scope-changed.xml", "pom.xml");

    {
      IMavenProjectFacade f = manager.create(pom2, false, null);
      Artifact[] a = getMavenProjectArtifacts(f).toArray(new Artifact[0]);
      assertEquals("provided scope dependency should disappear", 1, a.length);
      assertEquals(pom1.getLocation().toFile().getCanonicalFile(), a[0].getFile().getCanonicalFile());
    }
  }

  public void testJdkProfileActivation() throws Exception {
    IProject[] projects = importProjects("projects/jdkprofileactivation",
        new String[] {"p001/pom.xml", "p002/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(projects[0], monitor);
    ArrayList<ArtifactRef> a1 = new ArrayList<ArtifactRef>(f1.getMavenProjectArtifacts());
    assertEquals(2, a1.size());
    assertEquals("p002", a1.get(0).getArtifactId());
  }

  public void testWorkspaceDependencyVersionRange() throws Exception {
    IProject[] projects = importProjects("projects/versionrange", new String[] {"p001/pom.xml", "p002/pom.xml"},
        new ResolverConfiguration());
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(projects[0], monitor);
    MavenProject p1 = f1.getMavenProject(monitor);
    ArrayList<Artifact> a1 = new ArrayList<Artifact>(p1.getArtifacts());
    assertEquals(1, a1.size());

    assertEquals(projects[1].getLocation().append("target/classes").toFile(), a1.get(0).getFile());
  }

  public void testRepositoryMetadataCacheUsed() throws Exception {
    FileUtils.deleteDirectory(new File(repo, "mngeclipse1996"));
    String oldSettings = mavenConfiguration.getUserSettingsFile();
    try {
      injectFilexWagon();
      IJobChangeListener jobChangeListener = new JobChangeAdapter() {
        public void scheduled(IJobChangeEvent event) {
          if(event.getJob() instanceof ProjectRegistryRefreshJob) {
            // cancel all those concurrent refresh jobs, we want to monitor the main thread only
            event.getJob().cancel();
            System.out.println(getName() + ": ProjectRegistryRefreshJob was cancelled");
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

  public void testEnvironmentVariablesConsidered() throws Exception {
    String tmpDir = System.getenv("TEMP");
    assertTrue("This test requires the environment variable TEMP to be set", tmpDir != null);

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

  public void testPluginMainArtifactNotResolvedFromWorkspace() throws Exception {
    IProject[] projects = importProjects("projects/MNGECLIPSE-2116",
        new String[] {"plugin/pom.xml", "project/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
    assertEquals(2, projects.length);
    assertNotNull(projects[0]);
    assertNotNull(projects[1]);
  }

  @RequireMavenExecutionContext
  public void test021_dependencyVersionRange() throws Exception {
    IProject p1 = importProject("resources/t021_dependencyVersionRange/t021-p1/pom.xml");
    waitForJobsToComplete();

    MavenProjectFacade f1 = manager.create(p1, monitor);
    List<Artifact> a1 = new ArrayList<Artifact>(f1.getMavenProject(monitor).getArtifacts());
    assertEquals(1, a1.size());
    assertStartWith(repo.getAbsolutePath(), a1.get(0).getFile().getAbsolutePath());

    IProject p2 = importProject("resources/t021_dependencyVersionRange/t021-p2/pom.xml");
    waitForJobsToComplete();

    f1 = manager.create(p1, monitor);
    a1 = new ArrayList<Artifact>(f1.getMavenProject(monitor).getArtifacts());
    assertEquals(1, a1.size());
    assertEquals(p2.getFile(IMavenConstants.POM_FILE_NAME).getLocation().toFile(), a1.get(0).getFile());
  }

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
      List<Artifact> a1 = new ArrayList<Artifact>(f1.getMavenProject(monitor).getArtifacts());
      assertEquals(1, a1.size());
      assertEquals("1.0-20110411.112213-72", a1.get(0).getVersion());

      FileUtils.copyDirectoryStructure(new File("repositories/updateRepo2"), updatepolicyrepoDir);

      manager.refresh(new MavenUpdateRequest(p1, false, false), monitor);

      // assert dependency version did not change
      f1 = manager.create(p1, monitor);
      a1 = new ArrayList<Artifact>(f1.getMavenProject(monitor).getArtifacts());
      assertEquals(1, a1.size());
      assertEquals("1.0-20110411.112213-72", a1.get(0).getVersion());

      ((MavenConfigurationImpl) mavenConfiguration).setGlobalUpdatePolicy(null);

      manager.refresh(new MavenUpdateRequest(p1, false, false), monitor);
      f1 = manager.create(p1, monitor);
      a1 = new ArrayList<Artifact>(f1.getMavenProject(monitor).getArtifacts());
      assertEquals(1, a1.size());
      assertEquals("1.0-20110411.112327-73", a1.get(0).getVersion());
    } finally {
      ((MavenConfigurationImpl) mavenConfiguration).setGlobalUpdatePolicy(origPolicy);
    }
  }

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
    List<Artifact> a1 = new ArrayList<Artifact>(f1.getMavenProject(monitor).getArtifacts());
    assertEquals(1, a1.size());
    assertFalse(a1.get(0).isResolved());

    MavenProjectFacade f2 = manager.create(projects[1], monitor);
    List<Artifact> a2 = new ArrayList<Artifact>(f2.getMavenProject(monitor).getArtifacts());
    assertEquals(1, a2.size());
    assertFalse(a2.get(0).isResolved());

    // make the missing dependency available

    FileUtils.copyDirectoryStructure(new File("repositories/updateRepo1"), updatepolicyrepoDir);

    // refresh one of the two projects
    manager.refresh(new MavenUpdateRequest(projects[0], false, true), monitor);

    // both projects should now have resolved the missing dependency

    f1 = manager.create(projects[0], monitor);
    a1 = new ArrayList<Artifact>(f1.getMavenProject(monitor).getArtifacts());
    assertEquals(1, a1.size());
    assertTrue(a1.get(0).isResolved());

    f2 = manager.create(projects[1], monitor);
    a2 = new ArrayList<Artifact>(f2.getMavenProject(monitor).getArtifacts());
    assertEquals(1, a2.size());
    assertTrue(a2.get(0).isResolved());
  }

  public void test022_noChangeReloadWithUnrelatedRemoveProject() throws Exception {
    IProject[] p = importProjects("resources/t022/", new String[] {"t022-p1/pom.xml", "t022-p2/pom.xml"},
        new ResolverConfiguration());
    waitForJobsToComplete();

    boolean origSuspended = Job.getJobManager().isSuspended();

    Job.getJobManager().suspend();
    try {
      this.events.clear();

      p[0].close(monitor);

      manager.refresh(new MavenUpdateRequest(p, false, true), monitor);

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
    List<Artifact> a = new ArrayList<Artifact>(f.getMavenProject(monitor).getArtifacts());
    assertEquals(1, a.size());
    assertTrue(a.get(0).isResolved());
    assertEquals("junit", a.get(0).getArtifactId());

    String expectedErrorMessage = "Project build error: 'dependencies.dependency.version' for missing:missing:jar is missing.";
    List<IMarker> markers = WorkspaceHelpers.findErrorMarkers(p);
    assertEquals(WorkspaceHelpers.toString(markers), 1, markers.size());
    WorkspaceHelpers.assertErrorMarker(IMavenConstants.MARKER_POM_LOADING_ID, expectedErrorMessage, 21 /*lineNumber*/,
        p);
  }

  public void test356645_redundantSnapshotResolution() throws Exception {
    FileUtils.deleteDirectory(new File("target/356645localrepo"));

    mavenConfiguration.setUserSettingsFile("projects/356645_redundantSnapshotResolution/settings.xml");
    waitForJobsToComplete();
    injectFilexWagon();

    IProject[] projects = importProjects("projects/356645_redundantSnapshotResolution", new String[] {
        "projectA/pom.xml", "projectB/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();

    FilexWagon.setRequestFilterPattern("missing/missing/.*", true);

    MavenUpdateRequest request = new MavenUpdateRequest(projects, false, true);
    manager.refresh(request, monitor);

    assertEquals(3, FilexWagon.getRequests().size());
  }

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

    MavenUpdateRequest request = new MavenUpdateRequest(project, false, true);
    manager.refresh(request, monitor);

    assertNoErrors(project);
  }

  public void test418674_ChecksumPolicyFail() throws Exception {
    // clean local repo
    FileUtils.deleteDirectory(new File(repo, "org/eclipse/m2e/test/bad-checksum"));

    // import two projects with the same missing dependency

    String originalPolicy = mavenConfiguration.getGlobalChecksumPolicy();
    try {
      setChecksumPolicy(ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL);
      IProject project = importProject("projects/418674_ChecksumPolicy/checksum-test/pom.xml");
      List<IMarker> errors = findErrorMarkers(project);
      assertEquals(toString(errors), 1, errors.size());
      assertTrue(errors.get(0).getAttribute(IMarker.MESSAGE, null).contains("Checksum validation failed"));

      setChecksumPolicy(ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);

      MavenUpdateRequest request = new MavenUpdateRequest(new IProject[] {project}, false, true);
      manager.refresh(request, monitor);

      waitForJobsToComplete();
      assertNoErrors(project);

    } finally {
      ((MavenConfigurationImpl) mavenConfiguration).setGlobalChecksumPolicy(originalPolicy);
    }
  }

  public void test418674_ChecksumPolicyWarn() throws Exception {
    // clean local repo
    FileUtils.deleteDirectory(new File(repo, "org/eclipse/m2e/test/bad-checksum"));

    // import two projects with the same missing dependency

    String originalPolicy = mavenConfiguration.getGlobalChecksumPolicy();
    try {
      //repo is configured to fail if checksums don't match
      IProject project = importProject("projects/418674_ChecksumPolicy/checksum-test2/pom.xml");
      List<IMarker> errors = findErrorMarkers(project);
      assertEquals(toString(errors), 1, errors.size());
      assertTrue(errors.get(0).getAttribute(IMarker.MESSAGE, null).contains("Checksum validation failed"));

      //Override specific repo config
      setChecksumPolicy(ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);

      MavenUpdateRequest request = new MavenUpdateRequest(new IProject[] {project}, false, true);
      manager.refresh(request, monitor);

      waitForJobsToComplete();
      assertNoErrors(project);

    } finally {
      ((MavenConfigurationImpl) mavenConfiguration).setGlobalChecksumPolicy(originalPolicy);
    }
  }

  private void setChecksumPolicy(String value) {
    ((MavenConfigurationImpl) mavenConfiguration).setGlobalChecksumPolicy(value);
  }

  public void test435692_affected_changes() throws Exception {
    // parent 1.0
    IProject parent = importProject("projects/435692_affected_changes/parent/pom.xml");
    // parent 2.0 
    IProject parent2 = importProject("projects/435692_affected_changes/parent2/pom.xml");
    // workaround to allow the same artifact be imported with different versions
    copyContent(parent2, "pom_replace.xml", "pom.xml");

    // p1:1.0
    IProject p1 = importProject("projects/435692_affected_changes/p1/pom.xml");

    // p1:1.1
    IProject p1_1 = importProject("projects/435692_affected_changes/p1-1/pom.xml");
    // workaround to allow the same artifact be imported with different versions
    copyContent(p1_1, "pom_replace.xml", "pom.xml");

    // depends on p1:1.0
    IProject p2 = importProject("projects/435692_affected_changes/p2/pom.xml");

    // depends on p1:1.1
    IProject p3 = importProject("projects/435692_affected_changes/p3/pom.xml");

    // depends on p1:2.0
    IProject p4 = importProject("projects/435692_affected_changes/p4/pom.xml");

    // depends on p1:3.0
    IProject p5 = importProject("projects/435692_affected_changes/p5/pom.xml");

    // does not depend on anything
    importProject("projects/435692_affected_changes/p6/pom.xml");

    waitForJobsToComplete();

    // update p1 to have new dependency on junit
    events.clear();
    copyContent(p1, "pom_newDependency.xml", "pom.xml");

    assertEventsFromProjects(events, p1, p2 /* 1.0 */, p4 /* 2.0 (unresolved) */, p5 /* 3.0 (unresolved) */);

    // update p1's parent to a new version (should behave same as with dependency change)
    events.clear();
    copyContent(p1, "pom_changedParent.xml", "pom.xml"); // parent 1.0->2.0

    assertEventsFromProjects(events, p1, p2, p4, p5);

    // update p1 to a new version (should rebuild all versionless dependents)
    events.clear();
    copyContent(p1, "pom_changedVersion.xml", "pom.xml"); // 1.0->2.0

    assertEventsFromProjects(events, p1, p2, p3, p4, p5);

    // update parent to new version
    events.clear();
    copyContent(parent, "pom_changedVersion.xml", "pom.xml"); // 1.0 -> 3.0

    assertEventsFromProjects(events, parent, p1, p1_1, p2, p3, p4, p5);

  }

  private static void assertEventsFromProjects(List<MavenProjectChangedEvent> events, IProject... projects) {

    Set<IFile> actualPoms = new HashSet<IFile>();
    Set<IFile> expectedPoms = new HashSet<IFile>();

    for(MavenProjectChangedEvent event : events) {
      actualPoms.add(event.getSource());
    }

    for(IProject project : projects) {
      expectedPoms.add(project.getFile(IMavenConstants.POM_FILE_NAME));
    }

    assertEquals(expectedPoms, actualPoms);
  }
}
