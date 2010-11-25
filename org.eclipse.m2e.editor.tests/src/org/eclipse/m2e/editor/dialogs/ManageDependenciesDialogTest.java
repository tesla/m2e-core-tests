
package org.eclipse.m2e.editor.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.ReflectiveItemProviderAdapterFactory;
import org.eclipse.emf.edit.provider.resource.ResourceItemProviderAdapterFactory;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.util.PomResourceImpl;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;


public class ManageDependenciesDialogTest extends AbstractMavenProjectTestCase {
  
  private static final String VERSION = "1.0.0";
  public static String GROUP_ID = "org.eclipse.m2e.tests";

  /*
   * Cases to test:
   * - target and starting POM are the same
   * - target and starting POM are different
   * - target POM is broken
   * - starting POM is broken
   * - dependency already exists in target POM's dependencyManagement
   * - dependency already exists in target POM's dependencyManagement but has different version
   * - moving multiple dependencies
   * - moving multiple dependencies while at least one exists in target already
   * - DepLabelProvider provides a different colour for poms not in the workspace
   * - test moving a dependency from A to C, where C -> B -> A is the hierarchy
   */

  public void testSamePOM() throws Exception {

    Model model = loadModels("projects/same", new String[] { "child/pom.xml" }).get("child");
    assertEquals(model.getArtifactId(), "child");
    
    IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager().getMavenProject(GROUP_ID+".same", "child", VERSION);
    MavenProject project = facade.getMavenProject();
    assertEquals(project.getArtifactId(), "child");
    
    LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    hierarchy.add(project);
    
    TestDialog dialog = new TestDialog(Display.getDefault().getActiveShell(), 
        model, hierarchy);

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
  public void testDiffPOMs() throws Exception {
    Map<String, Model> models = loadModels("projects/diff", new String[] { "child/pom.xml", "parent/pom.xml" });
    Model child = models.get("child-diff");
    Model parent = models.get("parent-diff");
    
    assertNotNull(child);
    assertNotNull(parent);
    assertEquals(child.getArtifactId(), "child-diff");
    assertEquals(parent.getArtifactId(), "parent-diff");
    
    MavenProject childProject = getMavenProject(GROUP_ID+".diff", "child-diff", VERSION);
    MavenProject parentProject = getMavenProject(GROUP_ID+".diff", "parent-diff", VERSION);
    
    LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    hierarchy.addFirst(childProject);
    hierarchy.addLast(parentProject);
    
    TestDialog dialog = new TestDialog(Display.getDefault().getActiveShell(), 
        child, hierarchy);
    
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
  
  public void testDepExists() throws Exception {
    final String ARTIFACT_ID = "dep_exists";
    
    Model model = loadModels("projects/dep_exists", new String[] { "project/pom.xml" }).get(ARTIFACT_ID);
    assertEquals(model.getArtifactId(), ARTIFACT_ID);
    
    IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager().getMavenProject(GROUP_ID, ARTIFACT_ID, VERSION);
    MavenProject project = facade.getMavenProject();
    assertEquals(project.getArtifactId(), ARTIFACT_ID);
    
    LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    hierarchy.add(project);
    
    TestDialog dialog = new TestDialog(Display.getDefault().getActiveShell(), 
        model, hierarchy);

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
  
  public void testBiggerHierarchy() throws Exception {
    Map<String, Model> models = loadModels("projects/grandparent", 
        new String[] { "child/pom.xml", "parent/pom.xml", "grandparent/pom.xml" });
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
    
    TestDialog dialog = new TestDialog(Display.getDefault().getActiveShell(), 
        child, hierarchy);
    
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
  
  public void testMultipleDependencies() throws Exception {
    String ARTIFACT_CHILD = "multi-child";
    String ARTIFACT_PARENT = "multi-parent";
    Map<String, Model> models = loadModels("projects/multi", new String[] { "child/pom.xml", "parent/pom.xml" });
    Model child = models.get(ARTIFACT_CHILD);
    Model parent = models.get(ARTIFACT_PARENT);
    
    assertNotNull(child);
    assertNotNull(parent);
    assertEquals(child.getArtifactId(), ARTIFACT_CHILD);
    assertEquals(parent.getArtifactId(), ARTIFACT_PARENT);
    
    MavenProject childProject = getMavenProject(GROUP_ID, ARTIFACT_CHILD, VERSION);
    MavenProject parentProject = getMavenProject(GROUP_ID, ARTIFACT_PARENT, VERSION);
    
    LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    hierarchy.addFirst(childProject);
    hierarchy.addLast(parentProject);
    
    TestDialog dialog = new TestDialog(Display.getDefault().getActiveShell(), 
        child, hierarchy);
    
    LinkedList<Dependency> selectedDeps = new LinkedList<Dependency>();
    List<Dependency> dependencies = child.getDependencies();
    
    assertNotNull(dependencies);
    assertEquals(dependencies.size(), 4);
    
    for (Dependency dep : dependencies) {
      if (dep.getArtifactId().equals("to-move") || dep.getArtifactId().equals("move-but-exists")) {
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
    
    Dependency shouldBeNull = findDependency(parent.getDependencyManagement().getDependencies(), "test", "dont-move", "1.0");
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
  
  protected void checkContainsDependency(List<Dependency> deps, String group, String artifact, String version) {
    Dependency dep = findDependency(deps, group, artifact, version);
    assertNotNull("Dependency '"+group+"-"+artifact+"-"+version+"' not found.", dep);
  }
  
  protected Dependency findDependency(List<Dependency> deps, String group, String artifact, String version) {
    for (Dependency dep : deps ) {
      if (dep.getGroupId().equals(group) 
          && dep.getArtifactId().equals(artifact)) {
        if (version == null || version.equals("") || dep.getVersion().equals(version)) {
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

    for (int i = 0; i < poms.length; i++) {
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
      super(parent, model, hierarchy, createEditingDomain());
    }

    protected static EditingDomain createEditingDomain() {
      List<AdapterFactoryImpl> factories = new ArrayList<AdapterFactoryImpl>();
      factories.add(new ResourceItemProviderAdapterFactory());
      factories.add(new ReflectiveItemProviderAdapterFactory());
      
      ComposedAdapterFactory adapterFactory = new ComposedAdapterFactory(factories);
      BasicCommandStack commandStack = new BasicCommandStack();
      EditingDomain editingDomain = new AdapterFactoryEditingDomain(adapterFactory,
          commandStack, new HashMap<Resource, Boolean>());
      return editingDomain;
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
    protected Model loadTargetModel(IMavenProjectFacade facade) {
      return targetModel;
    }
  }
}
