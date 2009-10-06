
package org.maven.ide.eclipse.integration.tests;

import java.io.File;
import java.net.URL;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IViewPart;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.internal.index.NexusIndexManager;
import org.maven.ide.eclipse.jdt.BuildPathManager;

import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.locator.IWidgetLocator;
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

  public MEclipse193IndexerTest(){
  }

  
  protected void oneTimeSetup()throws Exception{
    File projectDir = unzipProject("projects/m2.zip");
    
    File settingsXML = new File(projectDir.getAbsolutePath() + "/m2/settings.xml");
    System.out.println("path to settings.xml: "+settingsXML.getAbsolutePath());
    IMavenConfiguration mavenConfiguration = MavenPlugin.lookup(IMavenConfiguration.class);
    mavenConfiguration.setUserSettingsFile(settingsXML.getAbsolutePath());
    IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
    indexManager.getWorkspaceIndex().updateIndex(true, new NullProgressMonitor());
    super.oneTimeSetup();
  }
  
  public void setUp() throws Exception {
    super.setUp();
    FileUtils
    .deleteDirectory(System.getProperty("user.home") + "/.m2/repository/commons-logging/commons-logging/1.1.1");
    FileUtils
    .deleteDirectory(System.getProperty("user.home") + "/.m2/repository/args4j");
    FileUtils
    .deleteDirectory(System.getProperty("user.home") + "/.m2/repository/org/sonatype/test/depencency");
    
  }
  
  //needs to be 2 'mirrored' repos in an active profile in the settings.xml. one id 'central', one id 'foo'
  public void testMirroredRepos() throws Exception {
    IUIContext ui = getUI();
    IViewPart indexView = showView("org.maven.ide.eclipse.views.MavenRepositoryView");
    ui.click(new TreeItemLocator("Global Repositories",
        new ViewLocator("org.maven.ide.eclipse.views.MavenRepositoryView")));
    ui.click(new TreeItemLocator("Global Repositories/nexus .*",
        new ViewLocator("org.maven.ide.eclipse.views.MavenRepositoryView")));
    IWidgetLocator[] findAll = ui.findAll(new TreeItemLocator("Global Repositories/.*mirrored by nexus.*"));
    assertTrue(findAll.length == 2);
  }
  
  public void testUpdateRemote() throws Exception{
    IUIContext ui = getUI();
    IViewPart indexView = showView("org.maven.ide.eclipse.views.MavenRepositoryView");

    ui.click(new TreeItemLocator("Global Repositories",
        new ViewLocator("org.maven.ide.eclipse.views.MavenRepositoryView")));
    ui.contextClick(new TreeItemLocator("Global Repositories/nexus .*",
        new ViewLocator("org.maven.ide.eclipse.views.MavenRepositoryView")), "Update Index");
    
    waitForAllBuildsToComplete();
    //now make sure the index update worked
    ui.contextClick(new TreeItemLocator("Global Repositories/nexus.*/abbot/abbot - jar/abbot : 0.13.0",
        new ViewLocator("org.maven.ide.eclipse.views.MavenRepositoryView")), "Update Index");

  }
  
  public void testLocalResolution() throws Exception {
    IUIContext ui = getUI();

    // set up two projects.
    IProject project = createArchetypeProject("maven-archetype-quickstart", "project");
    createArchetypeProject("maven-archetype-quickstart", "dependency");
    
    // mvn install "dependency" project
    ui.click(new TreeItemLocator("dependency", new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")));
    ui.click(new MenuItemLocator("Run/Run As/Maven install"));
    ui.wait(new JobsCompleteCondition(), 240000);
    
    // bump version # of "dependency" project
    openPomFile("dependency/pom.xml");
    ui.click(new CTabItemLocator("dependency/pom.xml"));
    replaceText(new NamedWidgetLocator("version"), "0.0.1-SNAPSHOT");
    ui.keyClick(SWT.MOD1, 's');
    Thread.sleep(5000);
    ui.wait(new JobsCompleteCondition(), 240000);
    
    assertProjectsHaveNoErrors();
    
    updateLocalIndex(ui);
    
    // Make sure local dependency from above can be added to 2nd proejct
    ui.click(new TreeItemLocator("project", new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")));
    ui.wait(new SWTIdleCondition());
    ui.contextClick(new TreeItemLocator("project", new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")), "Maven/Add Dependency");
    ui.wait(new ShellShowingCondition("Add Dependency"));
    ui.enterText("dependency");
    getUI().click(
        new TreeItemLocator("org.sonatype.test   dependency", new LabeledLocator(Tree.class, "&Search Results:")));
    ui.click(new TreeItemLocator(
        "org.sonatype.test   dependency/0.0.1-SNAPSHOT - dependency-0.0.1-SNAPSHOT.jar.*",
        new LabeledLocator(Tree.class, "&Search Results:")));
    ui.click(new ButtonLocator("OK"));
    ui.wait(new ShellDisposedCondition("Add Dependency"));
    
    waitForAllBuildsToComplete();
    
    assertProjectsHaveNoErrors();
    
    IJavaProject jp = (IJavaProject)project.getNature(JavaCore.NATURE_ID);
    Thread.sleep(10000);
    IClasspathContainer maven2Container = BuildPathManager.getMaven2ClasspathContainer(jp);
    
    for(IClasspathEntry entry : maven2Container.getClasspathEntries()) {
        if (entry.getPath().toString().endsWith("dependency")) {
          return;
      }
    }
    fail("Failed to find dependency-0.0.1-SNAPSHOT.jar in project");
  }
  
  private void updateLocalIndex(IUIContext ui) throws Exception {
    IViewPart indexView = showView("org.maven.ide.eclipse.views.MavenRepositoryView");

    ui.click(new TreeItemLocator("Local Repositories/Local repository.*",
            new ViewLocator("org.maven.ide.eclipse.views.MavenRepositoryView")));

    ui.contextClick(new TreeItemLocator("Local Repositories/Local repository.*", new ViewLocator(
        "org.maven.ide.eclipse.views.MavenRepositoryView")), "Rebuild Index");
    ui.wait(new ShellShowingCondition("Rebuild Index"));
    ui.click(new ButtonLocator("OK"));
    ui.wait(new ShellDisposedCondition("Rebuild Index"));
    waitForAllBuildsToComplete();
    
    //now make sure that the local index updated correctly
    ui.click(new TreeItemLocator("Local Repositories/Local repository.*/*",
        new ViewLocator("org.maven.ide.eclipse.views.MavenRepositoryView")));
  }

}
