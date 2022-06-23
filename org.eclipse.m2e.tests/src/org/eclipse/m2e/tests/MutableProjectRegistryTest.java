/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.project.registry.Capability;
import org.eclipse.m2e.core.internal.project.registry.IProjectRegistry;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.internal.project.registry.MutableProjectRegistry;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistry;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryReader;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class MutableProjectRegistryTest extends AbstractMavenProjectTestCase {

  private static final String WORKSPACE_STATE_SER = "workspaceState.ser";
  private IMaven maven = MavenPlugin.getMaven();

  @Rule
  public TestName name = new TestName();

  @Test
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

  @Test
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

  @Test
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
    assertTrue(getWorkspaceArtifacts(delta, f1.getArtifactKey()).isEmpty());

    assertEquals(0, delta.getProjects().length);
    assertSame(f1, state.getProjects()[0]);

    List<MavenProjectChangedEvent> events = state.apply(delta);

    assertNull(state.getProjectFacade(f1.getPom()));
    assertTrue(getWorkspaceArtifacts(state, f1.getArtifactKey()).isEmpty());
    assertEquals(0, state.getProjects().length);

    assertEquals(1, events.size());
    assertEquals(MavenProjectChangedEvent.KIND_REMOVED, events.get(0).getKind());
    assertSame(f1, events.get(0).getOldMavenProject());
    assertNull(events.get(0).getMavenProject());
  }

  private Map<ArtifactKey, Collection<IFile>> getWorkspaceArtifacts(IProjectRegistry state, ArtifactKey artifact) {
    return state.getWorkspaceArtifacts(artifact.groupId(), artifact.artifactId());
  }

  @Test
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

  @Test
  public void testIllegalStateMerge() throws Exception {
    ProjectRegistry state = new ProjectRegistry();
    MutableProjectRegistry delta = new MutableProjectRegistry(state);

    state.apply(delta);

    try {
      state.apply(delta);
      fail("IllegalStateException is expected");
    } catch(IllegalStateException expected) {
      //
    }
  }

  @Test
  public void testDetectNoLongerExistingProjectsInWorkspaceState() throws Exception {
    ProjectRegistry state = new ProjectRegistry();
    MutableProjectRegistry delta = new MutableProjectRegistry(state);

    IProject project = createExisting("dummy", "resources/dummy");
    IFile pom = project.getFile("p1.xml");
    delta.setProject(pom, newProjectFacade(pom));
    state.apply(delta);

    File tmpDir = File.createTempFile("m2e-" + name.getMethodName(), "dir");
    tmpDir.delete();
    tmpDir.mkdir();
    ProjectRegistryReader reader = new ProjectRegistryReader();
    reader.setStateLocation(tmpDir);
    reader.writeWorkspaceState(state);

    project.delete(true, true, monitor);

    state = reader.readWorkspaceState(null);
    assertFalse(state.isValid());

    new File(tmpDir, WORKSPACE_STATE_SER).delete();
    tmpDir.delete();
  }

  @Test
  public void testSaveParticipant() throws Exception {
    File stateLocationDir = MavenPluginActivator.getDefault().getStateLocation().toFile();
    File workspaceFile = new File(stateLocationDir, WORKSPACE_STATE_SER);
    workspaceFile.delete();
    ResourcesPlugin.getWorkspace().save(true, null);
    assertTrue(workspaceFile.exists());
    workspaceFile.delete();
  }

  @Test
  public void testForeignClassesInSerializedProjectRegistry() throws Exception {
    ProjectRegistry state = new ProjectRegistry();
    MutableProjectRegistry delta = new MutableProjectRegistry(state);

    IProject project = createExisting("dummy", "resources/dummy");
    IFile pom = project.getFile("p1.xml");
    delta.setProject(pom, newProjectFacade(pom));
    Set<Capability> capabilities = new HashSet<>();
    capabilities.add(new TestCapability("test", "test", "1"));
    delta.setCapabilities(pom, capabilities);
    state.apply(delta);

    File tmpDir = File.createTempFile("m2e-" + name.getMethodName(), "dir");
    tmpDir.delete();
    tmpDir.mkdir();
    ProjectRegistryReader reader = new ProjectRegistryReader();
    reader.setStateLocation(tmpDir);
    reader.writeWorkspaceState(state);

    state = reader.readWorkspaceState(null);
    assertTrue(state.isValid());

    new File(tmpDir, WORKSPACE_STATE_SER).delete();
    tmpDir.delete();
  }

  private MavenProjectFacade newProjectFacade(IFile pom) throws Exception {
    MavenProject mavenProject = maven.readProject(pom.getLocation().toFile(), monitor);
    return new MavenProjectFacade(null, pom, mavenProject, null);
  }
}
