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

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.ARTIFACT_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCIES;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCY;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCY_MANAGEMENT;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.GROUP_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.VERSION;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.childEquals;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChilds;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getTextValue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.w3c.dom.Element;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.CompoundOperation;
import org.eclipse.m2e.editor.AbstractMavenProjectTestJunit4;


@SuppressWarnings("restriction")
public class ManageDependenciesDialogTest extends AbstractMavenProjectTestJunit4 {
  private static final String TEST_VERSION = "1.0.0";

  public static String TEST_GROUP_ID = "org.eclipse.m2e.tests";

  private Color foreground;

  /*
   * Cases to test: - target and starting POM are the same - target and starting POM are different - target POM is
   * broken - starting POM is broken - dependency already exists in target POM's dependencyManagement - dependency
   * already exists in target POM's dependencyManagement but has different version - moving multiple dependencies -
   * moving multiple dependencies while at least one exists in target already - DepLabelProvider provides a different
   * colour for poms not in the workspace - test moving a dependency from A to C, where C -> B -> A is the hierarchy
   */

  //mkleint: this test is more or less useless. every change done to the label provider is to be manually (visually) tested, no test can be better..
  @Test
  public void testDepLabelProvider() throws Exception {
    importProjects("projects/colourprovider", new String[] {"child/pom.xml"}, new ResolverConfiguration());
    //    assertEquals(model.getArtifactId(), "child");

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getMavenProject(TEST_GROUP_ID, "child",
        TEST_VERSION);
    final MavenProject project = facade.getMavenProject(monitor);
    assertEquals(project.getArtifactId(), "child");

    final ManageDependenciesDialog.DepLabelProvider provider = new ManageDependenciesDialog.DepLabelProvider();

    Display.getDefault().syncExec(() -> foreground = provider.getForeground(project));
    assertNull(foreground);

    IMaven maven = MavenPlugin.getMaven();
    maven.detachFromSession(project);

    final MavenProject project2 = maven.resolveParentProject(project, new NullProgressMonitor());
    assertNotNull(project2);
    assertEquals(project2.getArtifactId(), "forge-parent");
    assertEquals(project2.getGroupId(), "org.sonatype.forge");
    assertEquals(project2.getVersion(), "6");
    Display.getDefault().syncExec(() -> foreground = provider.getForeground(project2));
    assertNotNull(foreground);
  }

  @Test
  public void testSamePOM() throws Exception {

    IStructuredModel model = loadModels("projects/same", new String[] {"parent/pom.xml", "child/pom.xml"}).get("child");

    assertNotNull(model);
    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getMavenProject(TEST_GROUP_ID + ".same", "child",
        TEST_VERSION);
    MavenProject project = facade.getMavenProject(monitor);
    assertEquals(project.getArtifactId(), "child");

    LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    hierarchy.add(project);

    LinkedList<Dependency> dependencies = new LinkedList<Dependency>();
    assertNotNull(project.getOriginalModel().getDependencies().get(0));
    dependencies.add(project.getOriginalModel().getDependencies().get(0));

    IDOMModel tempModel = createTempModel(model);

    tempModel.aboutToChangeModel();
    new CompoundOperation(ManageDependenciesDialog.createManageOperation(dependencies),
        ManageDependenciesDialog.createRemoveVersionOperation(dependencies)).process(tempModel.getDocument());
    tempModel.changedModel();

    // now assert..
    List<Element> deps = findChilds(findChild(tempModel.getDocument().getDocumentElement(), DEPENDENCIES), DEPENDENCY);
    assertEquals(1, deps.size());
    Element dep = deps.get(0);
    assertNotNull(dep);
    assertEquals("test", getTextValue(findChild(dep, PomEdits.GROUP_ID)));
    assertEquals("a", getTextValue(findChild(dep, ARTIFACT_ID)));
    assertNull(findChild(dep, PomEdits.VERSION));
    Element dm = findChild(tempModel.getDocument().getDocumentElement(), DEPENDENCY_MANAGEMENT);
    assertNotNull(dm);
    deps = findChilds(findChild(dm, DEPENDENCIES), DEPENDENCY);
    assertEquals(1, deps.size());
    dep = deps.get(0);
    Element g = findChild(dep, PomEdits.GROUP_ID);
    assertNotNull(g);
    assertEquals("test", getTextValue(g));
    assertEquals("a", getTextValue(findChild(dep, ARTIFACT_ID)));
    assertEquals("1.0", getTextValue(findChild(dep, PomEdits.VERSION)));

  }

  private IDOMModel createTempModel(IStructuredModel model) {
    IDOMModel tempModel = (IDOMModel) StructuredModelManager.getModelManager()
        .createUnManagedStructuredModelFor("org.eclipse.m2e.core.pomFile");
    assertNotNull(tempModel);
    tempModel.getStructuredDocument().setText(StructuredModelManager.getModelManager(),
        model.getStructuredDocument().getText());
    assertNotNull(tempModel.getStructuredDocument());
    return tempModel;
  }

  /*
   * Move dep from child to parent
   */
  @Test
  public void testDiffPOMs() throws Exception {
    Map<String, IStructuredModel> models = loadModels("projects/diff",
        new String[] {"child/pom.xml", "parent/pom.xml"});
    IStructuredModel child = models.get("child-diff");
    IStructuredModel parent = models.get("parent-diff");

    assertNotNull(child);
    assertNotNull(parent);

    MavenProject childProject = getMavenProject(TEST_GROUP_ID + ".diff", "child-diff", TEST_VERSION);
    MavenProject parentProject = getMavenProject(TEST_GROUP_ID + ".diff", "parent-diff", TEST_VERSION);

    LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    hierarchy.addFirst(childProject);
    hierarchy.addLast(parentProject);

    LinkedList<Dependency> selectedDeps = new LinkedList<Dependency>();
    List<Dependency> dependencies = childProject.getOriginalModel().getDependencies();
    assertNotNull(dependencies);
    assertEquals(dependencies.size(), 1);
    selectedDeps.add(dependencies.get(0));

    IDOMModel tempChild = createTempModel(child);
    IDOMModel tempParent = createTempModel(parent);

    tempChild.aboutToChangeModel();
    tempParent.aboutToChangeModel();
    ManageDependenciesDialog.createManageOperation(dependencies).process(tempParent.getDocument());
    ManageDependenciesDialog.createRemoveVersionOperation(dependencies).process(tempChild.getDocument());
    tempChild.changedModel();
    tempParent.changedModel();

    //now asserts
    List<Element> deps = findChilds(
        findChild(findChild(tempParent.getDocument().getDocumentElement(), DEPENDENCY_MANAGEMENT), DEPENDENCIES),
        DEPENDENCY);
    assertEquals(1, deps.size());

    Element depManDep = deps.get(0);
    assertEquals("test", getTextValue(findChild(depManDep, PomEdits.GROUP_ID)));
    assertEquals("a", getTextValue(findChild(depManDep, ARTIFACT_ID)));
    assertEquals("1.0", getTextValue(findChild(depManDep, PomEdits.VERSION)));

    deps = findChilds(findChild(tempChild.getDocument().getDocumentElement(), DEPENDENCIES), DEPENDENCY);
    assertEquals(1, deps.size());
    Element oldDep = deps.get(0);
    assertEquals("test", getTextValue(findChild(depManDep, PomEdits.GROUP_ID)));
    assertEquals("a", getTextValue(findChild(depManDep, ARTIFACT_ID)));
    assertNull(findChild(oldDep, PomEdits.VERSION));
  }

  @Test
  public void testDepExists() throws Exception {

    IStructuredModel model = loadModels("projects/dep_exists", new String[] {"project/pom.xml"}).get("dep_exists");

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getMavenProject(TEST_GROUP_ID, "dep_exists",
        TEST_VERSION);
    MavenProject project = facade.getMavenProject(monitor);

    LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    hierarchy.add(project);

    LinkedList<Dependency> dependencies = new LinkedList<Dependency>();
    dependencies.add(project.getOriginalModel().getDependencies().get(0));

    IDOMModel temp = createTempModel(model);

    temp.aboutToChangeModel();
    new CompoundOperation(ManageDependenciesDialog.createManageOperation(dependencies),
        ManageDependenciesDialog.createRemoveVersionOperation(dependencies)).process(temp.getDocument());
    temp.changedModel();

    // now assert..
    List<Element> deps = findChilds(findChild(temp.getDocument().getDocumentElement(), DEPENDENCIES), DEPENDENCY);
    assertEquals(1, deps.size());
    Element dep = deps.get(0);
    assertNotNull(dep);
    assertEquals("test", getTextValue(findChild(dep, PomEdits.GROUP_ID)));
    assertEquals("b", getTextValue(findChild(dep, ARTIFACT_ID)));
    assertNull(findChild(dep, PomEdits.VERSION));
    Element dm = findChild(temp.getDocument().getDocumentElement(), DEPENDENCY_MANAGEMENT);
    assertNotNull(dm);
    deps = findChilds(findChild(dm, DEPENDENCIES), DEPENDENCY);
    assertEquals(1, deps.size());
    dep = deps.get(0);
    Element g = findChild(dep, PomEdits.GROUP_ID);
    assertNotNull(g);
    assertEquals("test", getTextValue(g));
    assertEquals("b", getTextValue(findChild(dep, ARTIFACT_ID)));
    assertEquals("0.1", getTextValue(findChild(dep, PomEdits.VERSION)));
  }

  @Test
  public void testDepExistsDiffVersion() throws Exception {

    IStructuredModel model = loadModels("projects/dep_exists_diff_version", new String[] {"project/pom.xml"})
        .get("dep_exists_diff_version");

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getMavenProject(TEST_GROUP_ID,
        "dep_exists_diff_version", TEST_VERSION);
    MavenProject project = facade.getMavenProject(monitor);

    LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    hierarchy.add(project);

    LinkedList<Dependency> dependencies = new LinkedList<Dependency>();
    assertNotNull(project.getOriginalModel().getDependencies().get(0));
    assertEquals(project.getOriginalModel().getDependencies().get(0).getVersion(), "0.1");
    dependencies.add(project.getOriginalModel().getDependencies().get(0));

    IDOMModel temp = createTempModel(model);

    temp.aboutToChangeModel();
    new CompoundOperation(ManageDependenciesDialog.createManageOperation(dependencies),
        ManageDependenciesDialog.createRemoveVersionOperation(dependencies)).process(temp.getDocument());
    temp.changedModel();

    //
    List<Element> deps = findChilds(findChild(temp.getDocument().getDocumentElement(), DEPENDENCIES), DEPENDENCY);
    assertEquals(1, deps.size());
    Element dep = deps.get(0);
    assertNotNull(dep);
    assertEquals("test", getTextValue(findChild(dep, PomEdits.GROUP_ID)));
    assertEquals("b", getTextValue(findChild(dep, ARTIFACT_ID)));
    assertNull(findChild(dep, PomEdits.VERSION));
    Element dm = findChild(temp.getDocument().getDocumentElement(), DEPENDENCY_MANAGEMENT);
    assertNotNull(dm);
    deps = findChilds(findChild(dm, DEPENDENCIES), DEPENDENCY);
    assertEquals(1, deps.size());
    dep = deps.get(0);
    Element g = findChild(dep, PomEdits.GROUP_ID);
    assertNotNull(g);
    assertEquals("test", getTextValue(g));
    assertEquals("b", getTextValue(findChild(dep, ARTIFACT_ID)));
    assertEquals("0.2", getTextValue(findChild(dep, PomEdits.VERSION)));

  }

  @Test
  public void testDepExistsDiffVersionDiffPOMs() throws Exception {
    String ARTIFACT_ID_CHILD = "dep_exists_diff_version_diff_poms_child";
    String ARTIFACT_ID_PARENT = "dep_exists_diff_version_diff_poms_parent";
    Map<String, IStructuredModel> models = loadModels("projects/dep_exists_diff_version_diff_poms",
        new String[] {"child/pom.xml", "parent/pom.xml"});
    IStructuredModel child = models.get(ARTIFACT_ID_CHILD);
    IStructuredModel parent = models.get(ARTIFACT_ID_PARENT);

    assertNotNull(child);
    assertNotNull(parent);

    MavenProject childProject = getMavenProject(TEST_GROUP_ID, ARTIFACT_ID_CHILD, TEST_VERSION);
    MavenProject parentProject = getMavenProject(TEST_GROUP_ID, ARTIFACT_ID_PARENT, TEST_VERSION);

    LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    hierarchy.addFirst(childProject);
    hierarchy.addLast(parentProject);

    LinkedList<Dependency> selectedDeps = new LinkedList<Dependency>();
    List<Dependency> dependencies = childProject.getOriginalModel().getDependencies();
    assertNotNull(dependencies);
    assertEquals(dependencies.size(), 1);
    assertEquals(dependencies.get(0).getVersion(), "1.0");
    selectedDeps.add(dependencies.get(0));

    assertNotNull(parentProject.getOriginalModel().getDependencyManagement());
    assertEquals(parentProject.getOriginalModel().getDependencyManagement().getDependencies().get(0).getVersion(),
        "1.1");

    IDOMModel tempChild = createTempModel(child);
    IDOMModel tempParent = createTempModel(parent);

    tempChild.aboutToChangeModel();
    tempParent.aboutToChangeModel();
    ManageDependenciesDialog.createManageOperation(dependencies).process(tempParent.getDocument());
    ManageDependenciesDialog.createRemoveVersionOperation(dependencies).process(tempChild.getDocument());
    tempChild.changedModel();
    tempParent.changedModel();

    //
    assertNull(findChild(tempChild.getDocument().getDocumentElement(), DEPENDENCY_MANAGEMENT));

    List<Element> deps = findChilds(
        findChild(findChild(tempParent.getDocument().getDocumentElement(), DEPENDENCY_MANAGEMENT), DEPENDENCIES),
        DEPENDENCY);
    assertEquals(1, deps.size());

    Element depManDep = deps.get(0);
    assertEquals("test", getTextValue(findChild(depManDep, PomEdits.GROUP_ID)));
    assertEquals("a", getTextValue(findChild(depManDep, ARTIFACT_ID)));
    assertEquals("1.1", getTextValue(findChild(depManDep, PomEdits.VERSION)));

    deps = findChilds(findChild(tempChild.getDocument().getDocumentElement(), DEPENDENCIES), DEPENDENCY);
    assertEquals(1, deps.size());
    assertNotNull(deps.get(0));

    Element oldDep = deps.get(0);
    assertEquals("test", getTextValue(findChild(oldDep, PomEdits.GROUP_ID)));
    assertEquals("a", getTextValue(findChild(oldDep, ARTIFACT_ID)));
    assertNull(getTextValue(findChild(oldDep, VERSION)));

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
  public void testMultipleDependencies() throws Exception {
    String ARTIFACT_CHILD = "multi-child";
    String ARTIFACT_PARENT = "multi-parent";
    Map<String, IStructuredModel> models = loadModels("projects/multi",
        new String[] {"child/pom.xml", "parent/pom.xml"});
    final IStructuredModel child = models.get(ARTIFACT_CHILD);
    IStructuredModel parent = models.get(ARTIFACT_PARENT);

    assertNotNull(child);
    assertNotNull(parent);

    MavenProject childProject = getMavenProject(TEST_GROUP_ID, ARTIFACT_CHILD, TEST_VERSION);
    MavenProject parentProject = getMavenProject(TEST_GROUP_ID, ARTIFACT_PARENT, TEST_VERSION);

    final LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    hierarchy.addFirst(childProject);
    hierarchy.addLast(parentProject);

    LinkedList<Dependency> selectedDeps = new LinkedList<Dependency>();
    List<Dependency> dependencies = childProject.getOriginalModel().getDependencies();

    assertNotNull(dependencies);
    assertEquals(dependencies.size(), 4);

    for(Dependency dep : dependencies) {
      if(dep.getArtifactId().equals("to-move") || dep.getArtifactId().equals("move-but-exists")) {
        selectedDeps.add(dep);
      }
    }

    assertEquals(selectedDeps.size(), 3);

    IDOMModel tempChild = createTempModel(child);
    IDOMModel tempParent = createTempModel(parent);

    tempChild.aboutToChangeModel();
    tempParent.aboutToChangeModel();
    ManageDependenciesDialog.createManageOperation(selectedDeps).process(tempParent.getDocument());
    ManageDependenciesDialog.createRemoveVersionOperation(selectedDeps).process(tempChild.getDocument());
    tempChild.changedModel();
    tempParent.changedModel();

    Element deps = findChild(findChild(tempParent.getDocument().getDocumentElement(), DEPENDENCY_MANAGEMENT),
        DEPENDENCIES);
    assertNotNull(deps);
//    assertEquals(3, findChilds(deps, DEPENDENCY).size());

    checkContainsDependency(deps, "test", "to-move", "1.0");
    checkContainsDependency(deps, "test2", "to-move", "0.242");
    checkContainsDependency(deps, "test", "move-but-exists", "0.9");

    assertNull(findChild(deps, DEPENDENCY, childEquals(GROUP_ID, "test"), childEquals(ARTIFACT_ID, "dont-move"),
        childEquals(VERSION, "1.0")));

    deps = findChild(tempChild.getDocument().getDocumentElement(), DEPENDENCIES);
    assertNotNull(deps);
    assertEquals(4, findChilds(deps, DEPENDENCY).size());

    Element dep = findChild(deps, DEPENDENCY, childEquals(GROUP_ID, "test"), childEquals(ARTIFACT_ID, "to-move"));
    assertNotNull(dep);
    assertNull(findChild(dep, VERSION));
    dep = findChild(deps, DEPENDENCY, childEquals(GROUP_ID, "test2"), childEquals(ARTIFACT_ID, "to-move"));
    assertNotNull(dep);
    assertNull(findChild(dep, VERSION));
    dep = findChild(deps, DEPENDENCY, childEquals(GROUP_ID, "test"), childEquals(ARTIFACT_ID, "move-but-exists"));
    assertNotNull(dep);
    assertNull(findChild(dep, VERSION));

    checkContainsDependency(deps, "test", "dont-move", "1.0");
  }

  protected void checkContainsDependency(Element deps, String group, String artifact, String version) {
    Element dep = findChild(deps, DEPENDENCY, childEquals(GROUP_ID, group), childEquals(ARTIFACT_ID, artifact),
        childEquals(VERSION, version));
    assertNotNull("Dependency '" + group + "-" + artifact + "-" + version + "' not found.", dep);
  }

  protected MavenProject getMavenProject(String groupID, String artifactID, String version) throws CoreException {
    IMavenProjectRegistry mavenProjectManager = MavenPlugin.getMavenProjectRegistry();
    assertNotNull(mavenProjectManager);

    IMavenProjectFacade facade = mavenProjectManager.getMavenProject(groupID, artifactID, version);
    assertNotNull(facade);

    MavenProject project = facade.getMavenProject(monitor);
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
  protected Map<String, IStructuredModel> loadModels(String baseDir, String[] poms) throws Exception {
    IProject[] projects = importProjects(baseDir, poms, new ResolverConfiguration());
    HashMap<String, IStructuredModel> models = new HashMap<String, IStructuredModel>(poms.length);

    for(int i = 0; i < poms.length; i++ ) {
      IProject project = projects[i];
      IFile pomFile = project.getFile("pom.xml");
      IStructuredModel model = StructuredModelManager.getModelManager().getModelForRead(pomFile);
      models.put(pomFile.getParent().getName(), model);
    }

    return models;
  }

}
