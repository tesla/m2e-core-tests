/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.tests;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.embedder.ArtifactRef;
import org.maven.ide.eclipse.internal.project.MavenProjectFacade;
import org.maven.ide.eclipse.internal.project.MavenProjectManagerImpl;
import org.maven.ide.eclipse.project.IMavenProjectChangedListener;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IMavenProjectVisitor;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.MavenUpdateRequest;
import org.maven.ide.eclipse.project.ResolverConfiguration;


public class MavenProjectManagerTest extends AsbtractMavenProjectTestCase {
  
  MavenPlugin plugin = MavenPlugin.getDefault();

  MavenProjectManagerImpl manager;
  
  ArrayList<MavenProjectChangedEvent> events;
  
  IMavenProjectChangedListener listener = new IMavenProjectChangedListener() {
    public void mavenProjectChanged(MavenProjectChangedEvent[] event, IProgressMonitor monitor) {
      events.addAll(Arrays.asList(event));
    }
  };
  
  protected void setUp() throws Exception {
    super.setUp();

    manager = MavenPlugin.lookup(MavenProjectManagerImpl.class);
    
    events = new ArrayList<MavenProjectChangedEvent>();
    manager.addMavenProjectChangedListener(listener);
  }

  protected void tearDown() throws Exception {
    manager.removeMavenProjectChangedListener(listener);
    listener = null;
    events = null;
    manager = null;

    super.tearDown();
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

  private List<Artifact> getTestArtifacts(IMavenProjectFacade f1) throws CoreException {
    MavenProject mavenProject = f1.getMavenProject(monitor);
    return mavenProject.getTestArtifacts();
  }

  public void test000_simple() throws Exception {
//    System.out.println("ENTER!"); System.in.read();
    
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

    p1.delete(false, true, monitor);
    waitForJobsToComplete();

    assertNull(manager.create(p1, monitor));
  }

  public void test000_deletePom() throws Exception {
    IProject p1 = createExisting("t000-p1");
    waitForJobsToComplete();

    assertNotNull(manager.create(p1, monitor));

    p1.getFile(IMavenConstants.POM_FILE_NAME).delete(true, monitor);
    waitForJobsToComplete();

    assertNull(manager.create(p1, monitor));
  }

  public void test000_noChangeReload() throws Exception {
    IProject p1 = createExisting("t000-p1");
    IFile pom = p1.getFile(IMavenConstants.POM_FILE_NAME);
    waitForJobsToComplete();

    IMavenProjectFacade oldFacade = manager.create(p1, monitor);

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
    assertNotSame(oldFacade.getMavenProject(monitor), newFacade.getMavenProject(monitor));

    assertEquals(1, events.size());
    event = events.get(0);
    assertEquals(pom, event.getSource());
    assertEquals(MavenProjectChangedEvent.KIND_CHANGED, event.getKind());
    assertEquals(MavenProjectChangedEvent.FLAG_NONE, event.getFlags());
    assertNotNull(event.getOldMavenProject());
    assertNotNull(event.getMavenProject());
  }

  public void test001_missingParent() throws Exception {
    IProject p2 = createExisting("t001-p2");
    waitForJobsToComplete();

    assertNull(manager.create(p2, monitor));

    IMarker[] markers = p2.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(toString(markers), 1, markers.length);

    IProject p3 = createExisting("t001-p3");
    waitForJobsToComplete();

    assertNotNull(manager.create(p2, monitor));

    p3.delete(true, monitor);
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

    p2.delete(false, true, monitor);
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

  public void test006_parentAvailableFromLocalRepoAndWorkspace() throws Exception {
    IProject p1 = createExisting("t006-p1");
    IProject p2 = createExisting("t006-p2");
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(p1, monitor);
    assertEquals(p2.getFile(IMavenConstants.POM_FILE_NAME).getLocation().toFile(), f1.getMavenProject(monitor).getParentArtifact().getFile());

    p2.delete(false, true, monitor);
    waitForJobsToComplete();

    f1 = manager.create(p1, monitor);
    // assertTrue(f1.getMavenProject().getParent().getFile().getAbsolutePath().startsWith(repo.getAbsolutePath()));
    assertStartWith(repo.getAbsolutePath(), f1.getMavenProject(monitor).getParentArtifact().getFile().getAbsolutePath());
  }

  public void test007_staleDependencies() throws Exception {
    // p1 depends on p2
    IProject p1 = createExisting("t007-p1");
    IProject p2 = createExisting("t007-p2");
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(p1, monitor);
    assertEquals(1, f1.getMavenProject(monitor).getArtifacts().size());

    // update p1 to remove dependency on p2
    InputStream contents = p1.getFile("pom_updated.xml").getContents();
    p1.getFile("pom.xml").setContents(contents, IResource.FORCE, monitor);
    contents.close();
    waitForJobsToComplete();

    f1 = manager.create(p1, monitor);
    assertEquals(0, f1.getMavenProject(monitor).getArtifacts().size());

    events.clear();

    // remove p2
    p2.delete(true, monitor);
    waitForJobsToComplete();

    assertEquals(1, events.size());
    MavenProjectChangedEvent event = events.get(0);
    assertEquals(p2.getFile(IMavenConstants.POM_FILE_NAME), event.getSource());
    assertEquals(MavenProjectChangedEvent.KIND_REMOVED, event.getKind());
  }

  public void test008_staleMissingParent() throws Exception {
    // p1 does not have parent
    IProject p1 = createExisting("t008-p1");
    IProject p3 = createExisting("t008-p3");
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(p1, monitor);
    assertNull(f1); // XXX should I return non-null facade that does not have MavenProject?

    // update p1 to have p3 parent
    InputStream contents = p1.getFile("pom_updated.xml").getContents();
    p1.getFile("pom.xml").setContents(contents, IResource.FORCE, monitor);
    contents.close();
    waitForJobsToComplete();

    f1 = manager.create(p1, monitor);
    assertEquals("t008-p3", f1.getMavenProject(monitor).getParent().getArtifactId());

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
    
    p3.delete(true, monitor);
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

  public void test010_projectWithNestedModules() throws Exception {
    IProject p1 = createExisting("t010-p1");
    IProject p2 = createExisting("t010-p2");
    IProject p3 = createExisting("t010-p3");
    createExisting("t010-p4");
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(p1, monitor);
//    MavenProject f2 = manager.create(p2).getMavenProject();

    Artifact[] a1 = getMavenProjectArtifacts(f1).toArray(new Artifact[0]);
    assertEquals(2, a1.length);
    assertEquals(p2.getFile(new Path("t010-p2-m1/pom.xml")).getLocation().toFile(), a1[0].getFile());
    assertEquals(p3.getFile("pom.xml").getLocation().toFile(), a1[1].getFile());

//    Artifact[] a2 = (Artifact[]) f2.getArtifacts().toArray(new Artifact[f2.getArtifacts().size()]);
//    assertEquals(1, a2.length);
//    assertEquals(p3.getFile("t010-p3/pom.xml").getLocation().toFile(), a2[0].getFile());
  }

  public void test011_interModuleDependencies() throws Exception {
    IProject p1 = createExisting("t011-p1");
    waitForJobsToComplete();

    File m1 = p1.getFile("t011-p1-m1/pom.xml").getLocation().toFile().getAbsoluteFile();
    File m2 = p1.getFile("t011-p1-m2/pom.xml").getLocation().toFile().getAbsoluteFile();

    IMavenProjectFacade f1 = manager.create(p1, monitor);
    IMavenProjectFacade[] mavenProjects = getAllMavenProjects(f1);
    assertEquals(3, mavenProjects.length);

    assertEquals(m1, mavenProjects[1].getMavenProject(monitor).getFile().getAbsoluteFile());
//    XXX look why mavenProjects[1].getArtifact().getFile() == null
//    assertEquals(m1, mavenProjects[1].getArtifact().getFile().getAbsoluteFile());

    Artifact[] a1 = getMavenProjectArtifacts(mavenProjects[1]).toArray(new Artifact[0]);
    assertEquals(1, a1.length);
    assertEquals(m2, a1[0].getFile().getAbsoluteFile());

    assertEquals(m2, mavenProjects[2].getMavenProject(monitor).getFile().getAbsoluteFile());
//    assertEquals(m2, mavenProjects[2].getArtifact().getFile().getAbsoluteFile());

    assertEquals(0, getTestArtifacts(f1).size());

    IMarker[] markers = p1.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(toString(markers), 0, markers.length);
  }

  private IMavenProjectFacade[] getAllMavenProjects(IMavenProjectFacade facade) throws CoreException {
    final ArrayList<IMavenProjectFacade> projects = new ArrayList<IMavenProjectFacade>();
    facade.accept(new IMavenProjectVisitor() {
      public boolean visit(IMavenProjectFacade mavenProject) {
        projects.add(mavenProject);
        return true;
      }
    }, IMavenProjectVisitor.NESTED_MODULES);
    return projects.toArray(new MavenProjectFacade[projects.size()]);
  }

  public void test012_downloadExternalDependency() throws Exception {
    IProject p1 = createExisting("t012-p1");
    waitForJobsToComplete();

    File jar = new File(repo + "/log4j/log4j/1.2.13", "log4j-1.2.13.jar").getAbsoluteFile();
//    assertTrue(jar.exists());

    jar.delete();

    MavenUpdateRequest request = new MavenUpdateRequest(p1, false /*offline*/, false /*updateSnapshots*/ );
    plugin.getMavenProjectManager().refresh(request);
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(p1, monitor);
    IMavenProjectFacade[] mavenProjects = getAllMavenProjects(f1);
    assertEquals(1, mavenProjects.length);

    Artifact[] a1 = getMavenProjectArtifacts(mavenProjects[0]).toArray(new Artifact[0]);
    assertEquals(1, a1.length);
    assertEquals(jar, a1[0].getFile());
    assertTrue(jar.exists());
  }

  public void test013_cantParsePomMarker() throws Exception {
    IProject p1 = createExisting("t013-p1");
    waitForJobsToComplete();

    assertNull(manager.create(p1, monitor));
    IMarker[] markers = p1.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(toString(markers), 1, markers.length);

    copyContent(p1, "pom_good.xml", "pom.xml");
    markers = p1.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(toString(markers), 0, markers.length);
    assertNotNull(manager.create(p1, monitor));
  }

  public void test013_missingDependencyMarker() throws Exception {
    IProject p2 = createExisting("t013-p2");
    waitForJobsToComplete();

    workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    
    IMarker[] markers = p2.findMarkers(null, true, IResource.DEPTH_INFINITE);
    // (jdt) The container 'Maven Dependencies' references non existing library ...missing/missing/0.0.0/missing-0.0.0.jar'
    // (jdt) The project cannot be built until build path errors are resolved
    // (maven) Missing artifact missing:missing:jar:0.0.0:compile
    assertEquals(toString(markers), 3, markers.length); 
  }

  public void __test014_resolveDependencies() throws Exception {
    IProject p1 = createExisting("t014-p1");
    waitForJobsToComplete();

    copyContent(p1, "pom.xml", "pom_original.xml");

    IMavenProjectFacade f1 = manager.create(p1, monitor);
    IMavenProjectFacade[] mavenProjects = getAllMavenProjects(f1);
    assertEquals(1, mavenProjects.length);

    Artifact[] a1 = mavenProjects[0].getMavenProjectArtifacts().toArray(new Artifact[mavenProjects[0].getMavenProjectArtifacts().size()]);
    assertEquals("junit-4.0.jar", a1[0].getFile().getName());
    assertEquals(true, a1[0].isResolved());

    copyContent(p1, "pom_changed.xml", "pom.xml");
    f1 = manager.create(p1, monitor);
    mavenProjects = getAllMavenProjects(f1);
    a1 = mavenProjects[0].getMavenProjectArtifacts().toArray(new Artifact[mavenProjects[0].getMavenProjectArtifacts().size()]);
    assertEquals(1, a1.length);
    assertEquals("junit-4.0.jar", a1[0].getFile().getName());

    copyContent(p1, "pom_original.xml", "pom.xml");
    f1 = manager.create(p1, monitor);
    mavenProjects = getAllMavenProjects(f1);
    a1 = mavenProjects[0].getMavenProjectArtifacts().toArray(new Artifact[mavenProjects[0].getMavenProjectArtifacts().size()]);
    assertEquals("junit-4.0.jar", a1[0].getFile().getName());
  }

  public void test015_refreshOffline() throws Exception {
    // XXX fix this test on Windows and remove this condition 
    if(System.getProperty("os.name", "").toLowerCase().indexOf("windows")>-1) {
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

  public void test016_removeModule() throws Exception {
    IProject p1 = createExisting("t016-p1");
    waitForJobsToComplete();

    IFile pom = p1.getFile("t016-p1-m1/pom.xml");

    assertNotNull(manager.create(pom, false, null));

    copyContent(p1, "pom_changed.xml", "pom.xml");

    assertNull(manager.create(pom, false, null));
  }

  public void test017_nestedModuleRefresh() throws Exception {
    IProject p1 = createExisting("t017-p1");
    IProject p4 = createExisting("t017-p4");
    waitForJobsToComplete();

    IFile pom = p1.getFile("t017-p1-m1/pom.xml");

    MavenProject mavenProject = manager.create(pom, false, null).getMavenProject(monitor);
    Artifact a = mavenProject.getArtifacts().iterator().next();
    assertTrue(a.isResolved());
    assertEquals(p4.getFile(IMavenConstants.POM_FILE_NAME).getLocation().toFile().getAbsoluteFile(), 
        a.getFile().getAbsoluteFile());

    copyContent(p1, "pom_changed.xml", "pom.xml");

    mavenProject = manager.create(pom, false, null).getMavenProject(monitor);
    a = mavenProject.getArtifacts().iterator().next();
    assertFalse(a.isResolved());
  }

  public void test017_moduleRefresh() throws Exception {
    IProject p2 = createExisting("t017-p2");
    IProject p3 = createExisting("t017-p3");
    IProject p4 = createExisting("t017-p4");
    waitForJobsToComplete();

    IFile pom = p3.getFile("pom.xml");

    MavenProject mavenProject = manager.create(pom, false, null).getMavenProject(monitor);
    Artifact a = mavenProject.getArtifacts().iterator().next();
    assertTrue(a.isResolved());
    assertEquals(p4.getFile(IMavenConstants.POM_FILE_NAME).getLocation().toFile().getAbsoluteFile(), 
        a.getFile().getAbsoluteFile());

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

    p1.delete(true, monitor);
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

    InputStream contents = p1.getFile("pom_updated.xml").getContents();
    p1.getFile("pom.xml").setContents(contents, IResource.FORCE, monitor);
    contents.close();
    waitForJobsToComplete();

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
        new String[] {"p001/pom.xml", "p002/pom.xml"}, 
        new ResolverConfiguration());
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(projects[0], monitor);
    ArrayList<ArtifactRef> a1 = new ArrayList<ArtifactRef>(f1.getMavenProjectArtifacts());
    assertEquals(2, a1.size());
    assertEquals("junit", a1.get(1).getArtifactId());
  }

  public void testWorkspaceDependencyVersionRange() throws Exception {
    IProject[] projects = importProjects("projects/versionrange", 
        new String[] {"p001/pom.xml", "p002/pom.xml"}, 
        new ResolverConfiguration());
    waitForJobsToComplete();

    IMavenProjectFacade f1 = manager.create(projects[0], monitor);
    MavenProject p1 = f1.getMavenProject(monitor);
    ArrayList<Artifact> a1 = new ArrayList<Artifact>(p1.getArtifacts());
    assertEquals(1, a1.size());

    assertEquals(projects[1].getLocation().append("target/classes").toFile(), a1.get(0).getFile());
  }

}
