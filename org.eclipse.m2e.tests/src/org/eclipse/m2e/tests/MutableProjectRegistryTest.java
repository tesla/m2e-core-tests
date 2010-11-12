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

package org.eclipse.m2e.tests;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.project.registry.Capability;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.internal.project.registry.MutableProjectRegistry;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistry;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryReader;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

public class MutableProjectRegistryTest extends AbstractMavenProjectTestCase {

  private IMaven maven = MavenPlugin.getDefault().getMaven();

  public void testAddProject() throws Exception {
    IProject project = createExisting("dummy", "resources/dummy");

    ProjectRegistry state = new ProjectRegistry();

    MavenProjectFacade f1 = newProjectFacade(project.getFile("p1.xml"));
    MavenProjectFacade f2 = newProjectFacade(project.getFile("p2.xml"));

    MutableProjectRegistry delta1 = new MutableProjectRegistry(state);

    delta1.setProject(f1.getPom(), f1);

    List<MavenProjectChangedEvent> events = state.apply(delta1);
    assertEquals(1, events.size());
    assertEquals(MavenProjectChangedEvent.KIND_ADDED, events.get(0).getKind());
    assertSame(f1, events.get(0).getMavenProject());

    MutableProjectRegistry delta2 = new MutableProjectRegistry(state);
    delta2.setProject(f2.getPom(), f2);

    MavenProjectFacade[] facades = delta2.getProjects();
    assertEquals(2, facades.length);

    assertSame(f1, delta2.getProjectFacade(f1.getPom()));
    assertSame(f2, delta2.getProjectFacade(f2.getPom()));

    events = state.apply(delta2);
    assertEquals(1, events.size());
    assertEquals(MavenProjectChangedEvent.KIND_ADDED, events.get(0).getKind());
    assertSame(f2, events.get(0).getMavenProject());
  }

  public void testReplaceProject() throws Exception {
    IProject project = createExisting("dummy", "resources/dummy");

    ProjectRegistry state = new ProjectRegistry();

    MavenProjectFacade f1 = newProjectFacade(project.getFile("p1.xml"));
    MavenProjectFacade f2 = newProjectFacade(project.getFile("p1.xml"));

    MutableProjectRegistry delta = new MutableProjectRegistry(state);
    delta.setProject(f1.getPom(), f1);
    state.apply(delta);

    delta = new MutableProjectRegistry(state);
    delta.setProject(f2.getPom(), f2);

    MavenProjectFacade[] facades = delta.getProjects();
    assertEquals(1, facades.length);
    assertSame(f2, facades[0]);

    assertSame(f2, delta.getProjectFacade(f1.getPom()));
    assertSame(f1, state.getProjects()[0]);

    List<MavenProjectChangedEvent> events = state.apply(delta);
    assertEquals(1, events.size());
    assertEquals(MavenProjectChangedEvent.KIND_CHANGED, events.get(0).getKind());
    assertSame(f1, events.get(0).getOldMavenProject());
    assertSame(f2, events.get(0).getMavenProject());
  }

  public void testRemoveProject() throws Exception {
    IProject project = createExisting("dummy", "resources/dummy");

    ProjectRegistry state = new ProjectRegistry();

    MavenProjectFacade f1 = newProjectFacade(project.getFile("p1.xml"));

    MutableProjectRegistry delta = new MutableProjectRegistry(state);
    delta.setProject(f1.getPom(), f1);
    state.apply(delta);

    delta = new MutableProjectRegistry(state);
    delta.removeProject(f1.getPom(), f1.getArtifactKey());

    assertNull(delta.getProjectFacade(f1.getPom()));
    assertNull(delta.getWorkspaceArtifact(f1.getArtifactKey()));

    assertEquals(0, delta.getProjects().length);
    assertSame(f1, state.getProjects()[0]);

    List<MavenProjectChangedEvent> events = state.apply(delta);

    assertNull(state.getProjectFacade(f1.getPom()));
    assertNull(state.getWorkspaceArtifact(f1.getArtifactKey()));
    assertEquals(0, state.getProjects().length);

    assertEquals(1, events.size());
    assertEquals(MavenProjectChangedEvent.KIND_REMOVED, events.get(0).getKind());
    assertSame(f1, events.get(0).getOldMavenProject());
    assertNull(events.get(0).getMavenProject());
  }

  public void testRemoveUnknownProject() throws Exception {
    IProject project = createExisting("dummy", "resources/dummy");

    ProjectRegistry state = new ProjectRegistry();
    MutableProjectRegistry delta = new MutableProjectRegistry(state);

    MavenProjectFacade f1 = newProjectFacade(project.getFile("p1.xml"));

    delta.removeProject(f1.getPom(), f1.getArtifactKey());
    
    assertNull(delta.getProjectFacade(f1.getPom()));
    assertEquals(0, delta.getProjects().length);

    List<MavenProjectChangedEvent> events = state.apply(delta);

    assertNull(state.getProjectFacade(f1.getPom()));
    assertEquals(0, state.getProjects().length);
    assertTrue(events.isEmpty());
  }

  public void testIllageStateMerge() throws Exception {
    ProjectRegistry state = new ProjectRegistry();
    MutableProjectRegistry delta = new MutableProjectRegistry(state);

    state.apply(delta);

    try {
      state.apply(delta);
      fail("IllegalStateException is expected");
    } catch (IllegalStateException expected) {
      //
    }

  }

  public void testDetectNoLongerExistingProjectsInWorkspaceState() throws Exception {
    ProjectRegistry state = new ProjectRegistry();
    MutableProjectRegistry delta = new MutableProjectRegistry(state);

    IProject project = createExisting("dummy", "resources/dummy");
    IFile pom = project.getFile("p1.xml");
    delta.setProject(pom, newProjectFacade(pom));
    state.apply(delta);

    File tmpDir = File.createTempFile("m2e-" + getName(), "dir");
    tmpDir.delete();
    tmpDir.mkdir();
    ProjectRegistryReader reader = new ProjectRegistryReader(tmpDir);
    reader.writeWorkspaceState(state);

    project.delete(true, true, monitor);

    state = reader.readWorkspaceState(null);
    assertFalse(state.isValid());

    new File(tmpDir, "workspaceState.ser").delete();
    tmpDir.delete();
  }

  public void testForeignClassesInSerializedProjectRegistry() throws Exception {
    ProjectRegistry state = new ProjectRegistry();
    MutableProjectRegistry delta = new MutableProjectRegistry(state);

    IProject project = createExisting("dummy", "resources/dummy");
    IFile pom = project.getFile("p1.xml");
    delta.setProject(pom, newProjectFacade(pom));
    Set<Capability> capabilities = new HashSet<Capability>();
    capabilities.add(new TestCapability("test", "test", "1"));
    delta.setCapabilities(pom, capabilities);
    state.apply(delta);

    File tmpDir = File.createTempFile("m2e-" + getName(), "dir");
    tmpDir.delete();
    tmpDir.mkdir();
    ProjectRegistryReader reader = new ProjectRegistryReader(tmpDir);
    reader.writeWorkspaceState(state);

    state = reader.readWorkspaceState(null);
    assertTrue(state.isValid());

    new File(tmpDir, "workspaceState.ser").delete();
    tmpDir.delete();
  }

  private MavenProjectFacade newProjectFacade(IFile pom) throws Exception {
    MavenProject mavenProject = maven.readProject(pom.getLocation().toFile(), monitor);
    return new MavenProjectFacade(null, pom, mavenProject, null, null);
  }
}
