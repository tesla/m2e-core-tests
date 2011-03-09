/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.dialogs;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.editor.AbstractMavenProjectTestJunit4;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.util.PomResourceImpl;


public class ManageDependenciesDialogTest extends AbstractMavenProjectTestJunit4 {
  private static final String VERSION = "1.0.0";

  public static String GROUP_ID = "org.eclipse.m2e.tests";

  private TestDialog dialog;

  private Color foreground;

  /*
   * Cases to test: - target and starting POM are the same - target and starting POM are different - target POM is
   * broken - starting POM is broken - dependency already exists in target POM's dependencyManagement - dependency
   * already exists in target POM's dependencyManagement but has different version - moving multiple dependencies -
   * moving multiple dependencies while at least one exists in target already - DepLabelProvider provides a different
   * colour for poms not in the workspace - test moving a dependency from A to C, where C -> B -> A is the hierarchy
   */

  @Test
  public void testDepLabelProvider() throws Exception {
    Model model = loadModels("projects/colourprovider", new String[] {"child/pom.xml"}).get("child");
    assertEquals(model.getArtifactId(), "child");

    IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager()
        .getMavenProject(GROUP_ID, "child", VERSION);
    final MavenProject project = facade.getMavenProject();
    assertEquals(project.getArtifactId(), "child");

    final ManageDependenciesDialog.DepLabelProvider provider = new ManageDependenciesDialog.DepLabelProvider();

    Display.getDefault().syncExec(new Runnable() {

      @SuppressWarnings("synthetic-access")
      public void run() {
        foreground = provider.getForeground(project);

      }
    });
    assertNull(foreground);

    IMaven maven = MavenPlugin.getDefault().getMaven();
    maven.detachFromSession(project);
    MavenExecutionRequest request = MavenPlugin.getDefault().getMavenProjectManager()
        .createExecutionRequest(facade, new NullProgressMonitor());

    final MavenProject project2 = maven.resolveParentProject(request, project, new NullProgressMonitor());
    assertNotNull(project2);
    assertEquals(project2.getArtifactId(), "forge-parent");
    assertEquals(project2.getGroupId(), "org.sonatype.forge");
    assertEquals(project2.getVersion(), "6");
    Display.getDefault().syncExec(new Runnable() {

      public void run() {
        foreground = provider.getForeground(project2);
      }
    });
    assertNotNull(foreground);
  }

  @Test
  public void testSamePOM() throws Exception {

    Model model = loadModels("projects/same", new String[] {"parent/pom.xml", "child/pom.xml"}).get("child");
    assertEquals(model.getArtifactId(), "child");

    IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager()
        .getMavenProject(GROUP_ID + ".same", "child", VERSION);
    MavenProject project = facade.getMavenProject();
    assertEquals(project.getArtifactId(), "child");

    LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    hierarchy.add(project);

    initDialog(model, hierarchy);

    LinkedList<Dependency> dependencies = new LinkedList<Dependency>();
    assertNotNull(model.getDependencies().get(0));
    dependencies.add(model.getDependencies().get(0));

    dialog.setDependenciesList(dependencies);
    dialog.setTargetPOM(project);

    assertNull(model.getDependencyManagement());

    dialog.compute();

    assertNotNull(model.getDependencyManagement());
    assertEquals(1, model.getDependencyManagement().getDependencies().size());

    Dependency depManDep = model.getDependencyManagement().getDependencies().get(0);
    assertEquals(depManDep.getGroupId(), "test");
    assertEquals(depManDep.getArtifactId(), "a");
    assertEquals(depManDep.getVersion(), "1.0");

    assertNotNull(model.getDependencies());
    assertNotNull(model.getDependencies().get(0));
    Dependency oldDep = model.getDependencies().get(0);
    assertEquals(oldDep.getGroupId(), "test");
    assertEquals(oldDep.getArtifactId(), "a");
    assertTrue(oldDep.getVersion() == null || oldDep.getVersion().equals(""));
  }

  /*
   * Move dep from child to parent
   */
  @Test
  public void testDiffPOMs() throws Exception {
    Map<String, Model> models = loadModels("projects/diff", new String[] {"child/pom.xml", "parent/pom.xml"});
    Model child = models.get("child-diff");
    Model parent = models.get("parent-diff");

    assertNotNull(child);
    assertNotNull(parent);
    assertEquals(child.getArtifactId(), "child-diff");
    assertEquals(parent.getArtifactId(), "parent-diff");

    MavenProject childProject = getMavenProject(GROUP_ID + ".diff", "child-diff", VERSION);
    MavenProject parentProject = getMavenProject(GROUP_ID + ".diff", "parent-diff", VERSION);

    LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    hierarchy.addFirst(childProject);
    hierarchy.addLast(parentProject);

    initDialog(child, hierarchy);

    LinkedList<Dependency> selectedDeps = new LinkedList<Dependency>();
    List<Dependency> dependencies = child.getDependencies();
    assertNotNull(dependencies);
    assertEquals(dependencies.size(), 1);
    selectedDeps.add(dependencies.get(0));
    dialog.setDependenciesList(selectedDeps);

    dialog.setTargetPOM(parentProject);
    dialog.setTargetModel(parent);

    assertNull(parent.getDependencyManagement());

    dialog.compute();

    assertNotNull(parent.getDependencyManagement());
    assertEquals(1, parent.getDependencyManagement().getDependencies().size());

    Dependency depManDep = parent.getDependencyManagement().getDependencies().get(0);
    assertEquals(depManDep.getGroupId(), "test");
    assertEquals(depManDep.getArtifactId(), "a");
    assertEquals(depManDep.getVersion(), "1.0");

    assertNotNull(child.getDependencies());
    assertNotNull(child.getDependencies().get(0));
    Dependency oldDep = child.getDependencies().get(0);
    assertEquals(oldDep.getGroupId(), "test");
    assertEquals(oldDep.getArtifactId(), "a");
    assertTrue(oldDep.getVersion() == null || oldDep.getVersion().equals(""));
  }

  @Test
  public void testDepExists() throws Exception {
    final String ARTIFACT_ID = "dep_exists";

    Model model = loadModels("projects/dep_exists", new String[] {"project/pom.xml"}).get(ARTIFACT_ID);
    assertEquals(model.getArtifactId(), ARTIFACT_ID);

    IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager()
        .getMavenProject(GROUP_ID, ARTIFACT_ID, VERSION);
    MavenProject project = facade.getMavenProject();
    assertEquals(project.getArtifactId(), ARTIFACT_ID);

    LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    hierarchy.add(project);

    initDialog(model, hierarchy);

    LinkedList<Dependency> dependencies = new LinkedList<Dependency>();
    assertNotNull(model.getDependencies().get(0));
    dependencies.add(model.getDependencies().get(0));

    dialog.setDependenciesList(dependencies);
    dialog.setTargetPOM(project);

    assertNotNull(model.getDependencyManagement());
    assertNotNull(model.getDependencyManagement().getDependencies());
    assertEquals(model.getDependencyManagement().getDependencies().size(), 1);
    Dependency dep = model.getDependencyManagement().getDependencies().get(0);
    assertEquals(dep.getGroupId(), "test");
    assertEquals(dep.getArtifactId(), "b");
    assertEquals(dep.getVersion(), "0.1");

    dialog.compute();

    assertNotNull(model.getDependencyManagement());
    assertEquals(1, model.getDependencyManagement().getDependencies().size());

    Dependency depManDep = model.getDependencyManagement().getDependencies().get(0);
    assertEquals(depManDep.getGroupId(), "test");
    assertEquals(depManDep.getArtifactId(), "b");
    assertEquals(depManDep.getVersion(), "0.1");

    assertNotNull(model.getDependencies());
    assertNotNull(model.getDependencies().get(0));
    Dependency oldDep = model.getDependencies().get(0);
    assertEquals(oldDep.getGroupId(), "test");
    assertEquals(oldDep.getArtifactId(), "b");
    assertTrue(oldDep.getVersion() == null || oldDep.getVersion().equals(""));
  }

  @Test
  public void testDepExistsDiffVersion() throws Exception {
    final String ARTIFACT_ID = "dep_exists_diff_version";

    Model model = loadModels("projects/dep_exists_diff_version", new String[] {"project/pom.xml"}).get(ARTIFACT_ID);
    assertEquals(model.getArtifactId(), ARTIFACT_ID);

    IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager()
        .getMavenProject(GROUP_ID, ARTIFACT_ID, VERSION);
    MavenProject project = facade.getMavenProject();
    assertEquals(project.getArtifactId(), ARTIFACT_ID);

    LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    hierarchy.add(project);

    initDialog(model, hierarchy);

    LinkedList<Dependency> dependencies = new LinkedList<Dependency>();
    assertNotNull(model.getDependencies().get(0));
    assertEquals(model.getDependencies().get(0).getVersion(), "0.1");
    dependencies.add(model.getDependencies().get(0));

    dialog.setDependenciesList(dependencies);
    dialog.setTargetPOM(project);

    assertNotNull(model.getDependencyManagement());
    assertNotNull(model.getDependencyManagement().getDependencies());
    assertEquals(model.getDependencyManagement().getDependencies().size(), 1);
    Dependency dep = model.getDependencyManagement().getDependencies().get(0);
    assertEquals(dep.getGroupId(), "test");
    assertEquals(dep.getArtifactId(), "b");
    assertEquals(dep.getVersion(), "0.2");

    dialog.compute();

    assertNotNull(model.getDependencyManagement());
    assertEquals(1, model.getDependencyManagement().getDependencies().size());

    Dependency depManDep = model.getDependencyManagement().getDependencies().get(0);
    assertEquals(depManDep.getGroupId(), "test");
    assertEquals(depManDep.getArtifactId(), "b");
    assertEquals(depManDep.getVersion(), "0.2");

    assertNotNull(model.getDependencies());
    assertNotNull(model.getDependencies().get(0));
    Dependency oldDep = model.getDependencies().get(0);
    assertEquals(oldDep.getGroupId(), "test");
    assertEquals(oldDep.getArtifactId(), "b");
    assertTrue(oldDep.getVersion() == null || oldDep.getVersion().equals(""));
  }

  @Test
  public void testDepExistsDiffVersionDiffPOMs() throws Exception {
    String ARTIFACT_ID_CHILD = "dep_exists_diff_version_diff_poms_child";
    String ARTIFACT_ID_PARENT = "dep_exists_diff_version_diff_poms_parent";
    Map<String, Model> models = loadModels("projects/dep_exists_diff_version_diff_poms", new String[] {"child/pom.xml",
        "parent/pom.xml"});
    Model child = models.get(ARTIFACT_ID_CHILD);
    Model parent = models.get(ARTIFACT_ID_PARENT);

    assertNotNull(child);
    assertNotNull(parent);
    assertEquals(child.getArtifactId(), ARTIFACT_ID_CHILD);
    assertEquals(parent.getArtifactId(), ARTIFACT_ID_PARENT);

    MavenProject childProject = getMavenProject(GROUP_ID, ARTIFACT_ID_CHILD, VERSION);
    MavenProject parentProject = getMavenProject(GROUP_ID, ARTIFACT_ID_PARENT, VERSION);

    LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    hierarchy.addFirst(childProject);
    hierarchy.addLast(parentProject);

    initDialog(child, hierarchy);

    LinkedList<Dependency> selectedDeps = new LinkedList<Dependency>();
    List<Dependency> dependencies = child.getDependencies();
    assertNotNull(dependencies);
    assertEquals(dependencies.size(), 1);
    assertEquals(dependencies.get(0).getVersion(), "1.0");
    selectedDeps.add(dependencies.get(0));
    dialog.setDependenciesList(selectedDeps);

    dialog.setTargetPOM(parentProject);
    dialog.setTargetModel(parent);

    assertNotNull(parent.getDependencyManagement());
    assertEquals(parent.getDependencyManagement().getDependencies().get(0).getVersion(), "1.1");

    assertNull(child.getDependencyManagement());

    dialog.compute();

    assertNull(child.getDependencyManagement());

    assertNotNull(parent.getDependencyManagement());
    assertEquals(1, parent.getDependencyManagement().getDependencies().size());

    Dependency depManDep = parent.getDependencyManagement().getDependencies().get(0);
    assertEquals(depManDep.getGroupId(), "test");
    assertEquals(depManDep.getArtifactId(), "a");
    assertEquals(depManDep.getVersion(), "1.1");

    assertNotNull(child.getDependencies());
    assertNotNull(child.getDependencies().get(0));
    Dependency oldDep = child.getDependencies().get(0);
    assertEquals(oldDep.getGroupId(), "test");
    assertEquals(oldDep.getArtifactId(), "a");
    assertTrue(oldDep.getVersion() == null || oldDep.getVersion().equals(""));
  }

  @Test
  public void testBrokenSource() throws Exception {
    try {
      loadModels("projects/broken_child", new String[] {"child/pom.xml"});
      assertTrue("Expected Exception but didn't get one", false);
    } catch(Throwable t) {

    }
  }

  @Test
  public void testBrokenTarget() throws Exception {
    try {
      loadModels("projects/broken_target", new String[] {"child/pom.xml", "parent/pom.xml"});
      assertTrue("Expected Exception but didn't get one", false);
    } catch(Throwable t) {

    }
  }

  @Test
  public void testBiggerHierarchy() throws Exception {
    Map<String, Model> models = loadModels("projects/grandparent", new String[] {"child/pom.xml", "parent/pom.xml",
        "grandparent/pom.xml"});
    Model child = models.get("grandparent-child");
    Model parent = models.get("grandparent-parent");
    Model grandparent = models.get("grandparent-grandparent");

    assertNotNull(child);
    assertNotNull(parent);
    assertNotNull(grandparent);
    assertEquals(child.getArtifactId(), "grandparent-child");
    assertEquals(parent.getArtifactId(), "grandparent-parent");
    assertEquals(grandparent.getArtifactId(), "grandparent-grandparent");

    MavenProject childProject = getMavenProject(GROUP_ID, "grandparent-child", VERSION);
    MavenProject parentProject = getMavenProject(GROUP_ID, "grandparent-parent", VERSION);
    MavenProject grandparentProject = getMavenProject(GROUP_ID, "grandparent-grandparent", VERSION);

    LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    hierarchy.addFirst(childProject);
    hierarchy.add(parentProject);
    hierarchy.addLast(grandparentProject);

    initDialog(child, hierarchy);

    LinkedList<Dependency> selectedDeps = new LinkedList<Dependency>();
    List<Dependency> dependencies = child.getDependencies();
    assertNotNull(dependencies);
    assertEquals(dependencies.size(), 1);
    selectedDeps.add(dependencies.get(0));
    dialog.setDependenciesList(selectedDeps);

    dialog.setTargetPOM(grandparentProject);
    dialog.setTargetModel(grandparent);

    assertNull(parent.getDependencyManagement());
    assertNull(grandparent.getDependencyManagement());

    dialog.compute();

    assertNotNull(grandparent.getDependencyManagement());
    assertEquals(1, grandparent.getDependencyManagement().getDependencies().size());

    Dependency depManDep = grandparent.getDependencyManagement().getDependencies().get(0);
    assertEquals(depManDep.getGroupId(), "test");
    assertEquals(depManDep.getArtifactId(), "a");
    assertEquals(depManDep.getVersion(), "1.0");

    assertNotNull(child.getDependencies());
    assertNotNull(child.getDependencies().get(0));
    Dependency oldDep = child.getDependencies().get(0);
    assertEquals(oldDep.getGroupId(), "test");
    assertEquals(oldDep.getArtifactId(), "a");
    assertTrue(oldDep.getVersion() == null || oldDep.getVersion().equals(""));

    assertNull(parent.getDependencyManagement());
  }

  @Test
  public void testMultipleDependencies() throws Exception {
    String ARTIFACT_CHILD = "multi-child";
    String ARTIFACT_PARENT = "multi-parent";
    Map<String, Model> models = loadModels("projects/multi", new String[] {"child/pom.xml", "parent/pom.xml"});
    final Model child = models.get(ARTIFACT_CHILD);
    Model parent = models.get(ARTIFACT_PARENT);

    assertNotNull(child);
    assertNotNull(parent);
    assertEquals(child.getArtifactId(), ARTIFACT_CHILD);
    assertEquals(parent.getArtifactId(), ARTIFACT_PARENT);

    MavenProject childProject = getMavenProject(GROUP_ID, ARTIFACT_CHILD, VERSION);
    MavenProject parentProject = getMavenProject(GROUP_ID, ARTIFACT_PARENT, VERSION);

    final LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    hierarchy.addFirst(childProject);
    hierarchy.addLast(parentProject);

    initDialog(child, hierarchy);

    LinkedList<Dependency> selectedDeps = new LinkedList<Dependency>();
    List<Dependency> dependencies = child.getDependencies();

    assertNotNull(dependencies);
    assertEquals(dependencies.size(), 4);

    for(Dependency dep : dependencies) {
      if(dep.getArtifactId().equals("to-move") || dep.getArtifactId().equals("move-but-exists")) {
        selectedDeps.add(dep);
      }
    }

    assertEquals(selectedDeps.size(), 3);

    dialog.setDependenciesList(selectedDeps);

    dialog.setTargetPOM(parentProject);
    dialog.setTargetModel(parent);

    assertNotNull(parent.getDependencyManagement());

    dialog.compute();

    assertNotNull(parent.getDependencyManagement());
    assertEquals(3, parent.getDependencyManagement().getDependencies().size());

    checkContainsDependency(parent.getDependencyManagement().getDependencies(), "test", "to-move", "1.0");
    checkContainsDependency(parent.getDependencyManagement().getDependencies(), "test2", "to-move", "0.242");
    checkContainsDependency(parent.getDependencyManagement().getDependencies(), "test", "move-but-exists", "0.9");

    Dependency shouldBeNull = findDependency(parent.getDependencyManagement().getDependencies(), "test", "dont-move",
        "1.0");
    assertNull(shouldBeNull);

    assertNotNull(child.getDependencies());
    assertEquals(child.getDependencies().size(), 4);

    Dependency dep = findDependency(child.getDependencies(), "test", "to-move", null);
    assertNotNull(dep);
    assertTrue(dep.getVersion() == null || dep.getVersion().equals(""));
    dep = findDependency(child.getDependencies(), "test2", "to-move", null);
    assertNotNull(dep);
    assertTrue(dep.getVersion() == null || dep.getVersion().equals(""));
    dep = findDependency(child.getDependencies(), "test", "move-but-exists", null);
    assertNotNull(dep);
    assertTrue(dep.getVersion() == null || dep.getVersion().equals(""));

    checkContainsDependency(child.getDependencies(), "test", "dont-move", "1.0");
  }

  protected void initDialog(final Model model, final LinkedList<MavenProject> hierarchy) {
    Display.getDefault().syncExec(new Runnable() {

      @SuppressWarnings("synthetic-access")
      public void run() {
        dialog = new TestDialog(Display.getDefault().getActiveShell(), model, hierarchy);
      }
    });
  }

  protected void checkContainsDependency(List<Dependency> deps, String group, String artifact, String version) {
    Dependency dep = findDependency(deps, group, artifact, version);
    assertNotNull("Dependency '" + group + "-" + artifact + "-" + version + "' not found.", dep);
  }

  protected Dependency findDependency(List<Dependency> deps, String group, String artifact, String version) {
    for(Dependency dep : deps) {
      if(dep.getGroupId().equals(group) && dep.getArtifactId().equals(artifact)) {
        if(version == null || version.equals("") || dep.getVersion().equals(version)) {
          return dep;
        }
      }
    }
    return null;
  }

  protected MavenProject getMavenProject(String groupID, String artifactID, String version) {
    MavenProjectManager mavenProjectManager = MavenPlugin.getDefault().getMavenProjectManager();
    assertNotNull(mavenProjectManager);

    IMavenProjectFacade facade = mavenProjectManager.getMavenProject(groupID, artifactID, version);
    assertNotNull(facade);

    MavenProject project = facade.getMavenProject();
    assertNotNull(project);

    return project;
  }

  /**
   * Returns a HashMap with the pom artifactID as key
   * 
   * @param baseDir
   * @param poms
   * @return
   * @throws Exception
   */
  protected Map<String, Model> loadModels(String baseDir, String[] poms) throws Exception {
    IProject[] projects = importProjects(baseDir, poms, new ResolverConfiguration());
    HashMap<String, Model> models = new HashMap<String, Model>(poms.length);

    for(int i = 0; i < poms.length; i++ ) {
      IProject project = projects[i];
      IFile pomFile = project.getFile("pom.xml");

      PomResourceImpl resource = MavenPlugin.getDefault().getMavenModelManager().loadResource(pomFile);
      Model model = resource.getModel();
      models.put(model.getArtifactId(), model);
    }

    return models;
  }

  private static class TestDialog extends ManageDependenciesDialog {

    private MavenProject targetPOM;

    private LinkedList<Dependency> dependencies;

    private Model targetModel;

    public TestDialog(Shell parent, Model model, LinkedList<MavenProject> hierarchy) {
      super(parent, model, hierarchy);
      isTest = true;
    }

    public void compute() {
      assertNotNull(dependencies);
      assertNotNull(targetPOM);
      computeResult();
    }

    public void setDependenciesList(LinkedList<Dependency> dependencies) {
      this.dependencies = dependencies;
    }

    public void setTargetPOM(MavenProject targetPOM) {
      this.targetPOM = targetPOM;
    }

    protected LinkedList<Dependency> getDependenciesList() {
      return dependencies;
    }

    protected MavenProject getTargetPOM() {
      return targetPOM;
    }

    protected void setTargetModel(Model target) {
      this.targetModel = target;
    }
  }
}
