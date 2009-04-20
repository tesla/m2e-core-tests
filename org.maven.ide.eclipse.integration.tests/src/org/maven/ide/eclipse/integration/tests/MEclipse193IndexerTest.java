
package org.maven.ide.eclipse.integration.tests;

import java.io.IOException;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IViewPart;

import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.locator.IWidgetReference;
import com.windowtester.runtime.swt.condition.SWTIdleCondition;
import com.windowtester.runtime.swt.condition.eclipse.JobsCompleteCondition;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.LabeledLocator;
import com.windowtester.runtime.swt.locator.MenuItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;


public class MEclipse193IndexerTest extends UIIntegrationTestCase {

  public void setUp() throws IOException {
    FileUtils
    .deleteDirectory(System.getProperty("user.home") + "/.m2/repository/commons-logging/commons-logging/1.1.1");
    FileUtils
    .deleteDirectory(System.getProperty("user.home") + "/.m2/repository/args4j");
    FileUtils
    .deleteDirectory(System.getProperty("user.home") + "/.m2/repository/org/sonatype/test/depencency");
  }
  
  public void testLocalResolution() throws Exception {
    IUIContext ui = getUI();
    
    createArchetypeProjct("maven-archetype-quickstart", "project");
    createArchetypeProjct("maven-archetype-quickstart", "dependency");
    
    //Install version 1.0-SNAPSHOT of project2
    ui.click(new TreeItemLocator("dependency", new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")));
    ui.click(new MenuItemLocator("Run/Run As/Maven install"));
    ui.wait(new JobsCompleteCondition(), 240000);
    
    openPomFile("dependency/pom.xml");
    ui.click(new CTabItemLocator("dependency/pom.xml"));
    replaceText(new NamedWidgetLocator("version"), "0.0.2-SNAPSHOT");
    ui.keyClick(SWT.MOD1, 's');
    Thread.sleep(5000);
    ui.wait(new JobsCompleteCondition(), 240000);
    
    assertProjectsHaveNoErrors();
    
    updateLocalIndex(ui);
    
    ui.click(new TreeItemLocator("project", new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")));
    ui.click(new TreeItemLocator("project", new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")));
    ui.contextClick(new TreeItemLocator("project", new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")), "Maven/Add Dependency");
    ui.wait(new ShellShowingCondition("Add Dependency"));
    ui.enterText("dependency");
    getUI().click(
        new TreeItemLocator("org.sonatype.test   dependency", new LabeledLocator(Tree.class, "&Search Results:")));
    ui.click(new TreeItemLocator(
        "org.sonatype.test   dependency/0.0.1-SNAPSHOT - dependency-0.0.1-SNAPSHOT.jar.*",
        new LabeledLocator(Tree.class, "&Search Results:")));
   // ui.click(new LabeledLocator(Button.class, "Scope:"));
    ui.click(new ButtonLocator("OK"));
    ui.wait(new ShellDisposedCondition("Add Dependency"));
    
    waitForAllBuildsToComplete();
    
    assertProjectsHaveNoErrors();
  }
  
  public void testUpdateLocalIndex() throws Exception {
    IUIContext ui = getUI();

    showView("org.maven.ide.eclipse.views.MavenIndexesView");

    ui
        .click(new TreeItemLocator("local .*repository",
            new ViewLocator("org.maven.ide.eclipse.views.MavenIndexesView")));
    ui.contextClick(new TreeItemLocator("local : .*repository", new ViewLocator(
        "org.maven.ide.eclipse.views.MavenIndexesView")), "Update Index");
    ui.wait(new JobsCompleteCondition(), 300000);

    showView("org.maven.ide.eclipse.views.MavenIndexesView");

    // Assert local index does not have commons-logging.
    TreeItemLocator locator = new TreeItemLocator(
        "local : .*repository/commons-logging/commons-logging - jar/commons-logging : 1.1.1", new ViewLocator(
            "org.maven.ide.eclipse.views.MavenIndexesView"));
    ui.assertThat(locator.isVisible(false));

    // Create a test project and add common-logging 1.1.1 as dependency.
    IProject project = createArchetypeProjct("maven-archetype-quickstart", "project");
    addDependency(project, "commons-logging", "commons-logging", "1.1.1");

    updateLocalIndex(ui);
    //TODO: There is a bug, you need to re-index twice to get new items to show up. Remove this when new indexer put into m2e.
    updateLocalIndex(ui);

    showView("org.maven.ide.eclipse.views.MavenIndexesView");

    // Find commons-logging 1.1.1 and materialize it.
    ui.click(new TreeItemLocator("local : .*repository/commons-logging/commons-logging - jar", new ViewLocator(
        "org.maven.ide.eclipse.views.MavenIndexesView")));

    ui.click(new TreeItemLocator("local : .*repository/commons-logging/commons-logging - jar/commons-logging : 1.1.1",
        new ViewLocator("org.maven.ide.eclipse.views.MavenIndexesView")));
    ui.contextClick(new TreeItemLocator("local .*commons-logging/commons-logging - jar/commons-logging : 1.1.1",
        new ViewLocator("org.maven.ide.eclipse.views.MavenIndexesView")), "Materialize Projects");
    waitForAllBuildsToComplete();
    ui.wait(new ShellShowingCondition("Import Maven Projects"));
    ui.click(new ButtonLocator("&Next >"));
    ui.click(new ButtonLocator("&Finish"));
    ui.wait(new ShellDisposedCondition("Import Maven Projects"));

    waitForAllBuildsToComplete();

    Thread.sleep(5000);
    ui.click(new ButtonLocator("&Finish"));
    waitForAllBuildsToComplete();

    IProject loggingProject = ResourcesPlugin.getWorkspace().getRoot().getProject("commons-logging");
    assertTrue(loggingProject.exists());
    assertTrue(loggingProject.hasNature(JavaCore.NATURE_ID));
    assertProjectsHaveNoErrors();
  }

  private void updateLocalIndex(IUIContext ui) throws Exception {
    showView("org.maven.ide.eclipse.views.MavenIndexesView");

    ui
        .click(new TreeItemLocator("local .*repository",
            new ViewLocator("org.maven.ide.eclipse.views.MavenIndexesView")));

    ui.contextClick(new TreeItemLocator("local : .*repository", new ViewLocator(
        "org.maven.ide.eclipse.views.MavenIndexesView")), "Update Index");
    ui.wait(new JobsCompleteCondition(), 240000);

  }

  public void testAddNewIndex() throws Exception {
    importZippedProject("projects/add_index_test.zip");
    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("project");
    assertTrue(project.exists());

    openFile(project, "src/main/java/org/sonatype/test/project/App.java");

    // there should be compile errors
    int severity = project.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
    assertEquals(IMarker.SEVERITY_ERROR, severity);

    launchQuickFix(project);

    UIThreadTask.executeOnEventQueue(new UIThreadTask() {

      public Object runEx() throws Exception {
        Tree tree = (Tree) ((IWidgetReference) getUI().find(new NamedWidgetLocator("searchResultTree"))).getWidget();
        assertEquals(0, tree.getItemCount()); // Missing class shouldn't be found, since it isn't in maven central.
        return null;
      }
    });

    getUI().keyClick(SWT.ESC);
    getUI().wait(new ShellDisposedCondition("Search in Maven repositories"));
    
    
    // Now add in the index:  http://download.java.net/maven/2/
    IViewPart indexView = showView("org.maven.ide.eclipse.views.MavenIndexesView");

    getUI().click(new CTabItemLocator("Maven Indexes"));

    getUI().wait(new SWTIdleCondition());

    getUI().contextClick(
        new TreeItemLocator("workspace", new ViewLocator("org.maven.ide.eclipse.views.MavenIndexesView")),
        "Add Index");

    getUI().wait(new ShellShowingCondition("Add Repository Index"));
    getUI().click(new NamedWidgetLocator("repositoryUrlCombo"));
    getUI().enterText("http://download.java.net/maven/2/");
    getUI().click(new NamedWidgetLocator("retrieveButton"));
    getUI().wait(new JobsCompleteCondition());
    getUI().wait(new SWTIdleCondition());
    getUI().click(new ButtonLocator("OK"));
    getUI().wait(new ShellDisposedCondition("Add Repository Index"));
    getUI().contextClick(
        new TreeItemLocator("central.*", new ViewLocator("org.maven.ide.eclipse.views.MavenIndexesView")),
        "Update Index");
    hideView(indexView);
    
    getUI().wait(new JobsCompleteCondition(), 240000);
    
    // Attempt to quick fix again...
    launchQuickFix(project);
    
    getUI().click(new TreeItemLocator("ShortOptionHandler   org.kohsuke.args4j.spi   args4j   args4j", new NamedWidgetLocator("searchResultTree")));
    getUI().click(new TreeItemLocator(
        "ShortOptionHandler   org.kohsuke.args4j.spi   args4j   args4j/2.0.12 - args4j-2.0.12.jar .*",
        new NamedWidgetLocator("searchResultTree")));
    
    getUI().wait(new SWTIdleCondition());
    getUI().keyClick(SWT.CR);

    getUI().wait(new ShellDisposedCondition("Search in Maven repositories"));

    waitForAllBuildsToComplete();

    assertProjectsHaveNoErrors();

  }

  private void launchQuickFix(IProject project) throws Exception {
    //Workaround for Window tester bug, close & reopen tab to prevent editor from being in invalid state.
    getUI().close(new CTabItemLocator("App.java"));
    openFile(project, "src/main/java/org/sonatype/test/project/App.java");

    //launch quick fix for SessionFactory dependency
    getUI().click(new TreeItemLocator("project.*", new ViewLocator(PACKAGE_EXPLORER_VIEW_ID)));
    getUI().keyClick(SWT.MOD1 | SWT.SHIFT, 't');
    getUI().wait(new ShellShowingCondition("Open Type"));
    getUI().enterText("app");
    getUI().wait(new SWTIdleCondition());
    getUI().click(new ButtonLocator("OK"));
    getUI().wait(new ShellDisposedCondition("Open Type"));
    getUI().wait(new JobsCompleteCondition(), 60000);

    getUI().keyClick(SWT.MOD1, '.'); // next annotation

    getUI().keyClick(SWT.MOD1, '1');
    getUI().wait(new ShellShowingCondition(""));
    getUI().keyClick(SWT.END);
    getUI().keyClick(SWT.ARROW_UP);

    getUI().keyClick(SWT.CR);
    getUI().wait(new ShellShowingCondition("Search in Maven repositories"));
    getUI().wait(new SWTIdleCondition());
  }
}
