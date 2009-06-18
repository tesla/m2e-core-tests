package org.maven.ide.eclipse.tests;

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.internal.project.MavenProjectFacade;
import org.maven.ide.eclipse.internal.project.ProjectRegistry;
import org.maven.ide.eclipse.internal.project.MutableProjectRegistry;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;

public class WorkspaceStateDeltaTest extends AsbtractMavenProjectTestCase {

  private IMaven maven = MavenPlugin.lookup(IMaven.class);

  public void testAddProject() throws Exception {
    IProject project = createExisting("dummy", "resources/dummy");

    ProjectRegistry state = new ProjectRegistry();

    MavenProjectFacade f1 = newProjectFacade(project.getFile("p1.xml"));
    MavenProjectFacade f2 = newProjectFacade(project.getFile("p2.xml"));

    MutableProjectRegistry delta1 = new MutableProjectRegistry(state);

    delta1.addProject(f1.getPom(), f1);

    List<MavenProjectChangedEvent> events = state.apply(delta1);
    assertEquals(1, events.size());
    assertEquals(MavenProjectChangedEvent.KIND_ADDED, events.get(0).getKind());
    assertSame(f1, events.get(0).getMavenProject());

    MutableProjectRegistry delta2 = new MutableProjectRegistry(state);
    delta2.addProject(f2.getPom(), f2);

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
    delta.addProject(f1.getPom(), f1);
    state.apply(delta);

    delta = new MutableProjectRegistry(state);
    delta.addProject(f2.getPom(), f2);

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
    delta.addProject(f1.getPom(), f1);
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

  public void _testIllageStateMerge() throws Exception {
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

  private MavenProjectFacade newProjectFacade(IFile pom) throws Exception {
    MavenProject mavenProject = maven.readProject(pom.getLocation().toFile(), monitor);
    return new MavenProjectFacade(null, pom, mavenProject, null);
  }
}
