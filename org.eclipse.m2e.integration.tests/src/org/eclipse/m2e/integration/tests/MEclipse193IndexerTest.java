
package org.eclipse.m2e.integration.tests;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withMnemonic;

import java.io.File;
import java.util.List;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.index.IndexManager;
import org.eclipse.m2e.integration.tests.common.ContextMenuHelper;
import org.eclipse.m2e.integration.tests.common.matchers.ContainsMnemonic;
import org.eclipse.m2e.jdt.BuildPathManager;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.hamcrest.text.StringContains;
import org.hamcrest.text.StringStartsWith;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class MEclipse193IndexerTest extends M2EUIIntegrationTestCase {

  private static String originalUserSettingsFile;

  public MEclipse193IndexerTest() {
  }

  @BeforeClass
  public static void init() throws Exception {
//    File projectDir = unzipProject("projects/m2.zip");

//    File settingsXML = new File(projectDir.getAbsolutePath() + "/m2/settings.xml");
//    System.out.println("path to settings.xml: " + settingsXML.getAbsolutePath());
    IMavenConfiguration mavenConfiguration = MavenPlugin.getDefault().getMavenConfiguration();
    originalUserSettingsFile = mavenConfiguration.getUserSettingsFile();
//    mavenConfiguration.setUserSettingsFile(settingsXML.getAbsolutePath());
    mavenConfiguration.setUserSettingsFile(new File( "resources/settings_mirror.xml" ).getAbsolutePath());
    IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
    indexManager.getWorkspaceIndex().updateIndex(true, new NullProgressMonitor());
    waitForAllBuildsToComplete();
  }

  @AfterClass
  public static void restoreUserSettings() {
    IMavenConfiguration mavenConfiguration = MavenPlugin.getDefault().getMavenConfiguration();
    mavenConfiguration.setUserSettingsFile(originalUserSettingsFile);
  }

  @Before
  public void setUp() throws Exception {
    FileUtils
        .deleteDirectory(System.getProperty("user.home") + "/.m2/repository/commons-logging/commons-logging/1.1.1");
    FileUtils.deleteDirectory(System.getProperty("user.home") + "/.m2/repository/args4j");
    FileUtils.deleteDirectory(System.getProperty("user.home") + "/.m2/repository/org/sonatype/test/depencency");
  }

  @Test
  //needs to be 2 'mirrored' repos in an active profile in the settings.xml. one id 'central', one id 'foo'
  public void testMirroredRepos() throws Exception {
    SWTBotView indexView = openView("org.eclipse.m2e.views.MavenRepositoryView");
    SWTBotTreeItem tree = indexView.bot().tree().expandNode("Global Repositories");
    Assert.assertEquals(1, findItems(tree, StringStartsWith.startsWith("nexus")).size());
    Assert.assertEquals(2, findItems(tree, StringContains.containsString("mirrored by nexus")).size());
  }

  @Test
  public void testUpdateRemote() throws Exception {
    SWTBotView indexView = openView("org.eclipse.m2e.views.MavenRepositoryView");

    SWTBotTree tree = indexView.bot().tree();
    SWTBotTreeItem globalRepos = tree.expandNode("Global Repositories");
    List<SWTBotTreeItem> items = findItems(globalRepos, StringStartsWith.startsWith("nexus"));
    Assert.assertEquals(1, items.size());
    items.get(0).select();

    ContextMenuHelper.clickContextMenu(tree, "Update Index");

    waitForAllBuildsToComplete();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testLocalResolution() throws Exception {
    String projectName = "localResolutionProject";
    String dependencyName = "dependentProject";

    // set up two projects.
    IProject project = createSimpleMavenProject(projectName);
    createSimpleMavenProject(dependencyName);

    waitForAllBuildsToComplete();

    // mvn install "dependency" project
    ContextMenuHelper.clickContextMenu(selectProject(dependencyName), withMnemonic("Run As"), ContainsMnemonic
        .containsMnemonic("Maven install"));
    waitForAllLaunchesToComplete(30 * 1000);

    // bump version # of "dependency" project
    SWTBotEditor editor = bot.editorByTitle(openPomFile(dependencyName + "/pom.xml").getTitle());
    editor.bot().textWithLabel("Version:").setText("0.0.1-SNAPSHOT");
    editor.saveAndClose();

    waitForAllBuildsToComplete();

    assertProjectsHaveNoErrors();

    updateLocalIndex();

    // Make sure local dependency from above can be added to 2nd proejct
    addDependency(projectName, "org.sonatype.test", dependencyName, "0.0.1-SNAPSHOT");
    takeScreenShot();

    waitForAllBuildsToComplete();

    assertProjectsHaveNoErrors();

    IJavaProject jp = (IJavaProject) project.getNature(JavaCore.NATURE_ID);

    IClasspathContainer maven2Container = BuildPathManager.getMaven2ClasspathContainer(jp);

    for(IClasspathEntry entry : maven2Container.getClasspathEntries()) {
      if(entry.getPath().toString().endsWith(dependencyName)) {
        return;
      }
    }
    Assert.fail("Failed to find " + dependencyName + "-0.0.1-SNAPSHOT.jar in project");
  }

}
