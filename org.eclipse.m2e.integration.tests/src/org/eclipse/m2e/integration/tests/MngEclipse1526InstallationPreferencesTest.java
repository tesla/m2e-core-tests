
package org.eclipse.m2e.integration.tests;
 
 import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.internal.resources.XMLWriter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenRuntime;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.integration.tests.common.SwtbotUtil;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.hamcrest.text.StringStartsWith;
import org.junit.Assert;
import org.junit.Test;


@SuppressWarnings("restriction")
public class MngEclipse1526InstallationPreferencesTest extends M2EUIIntegrationTestCase {

  protected String externalRuntime;

  public MngEclipse1526InstallationPreferencesTest() {
    super();
    externalRuntime = new File("target/maven/apache-maven-2.0.10").getAbsolutePath();
  }

  /**
   * Main test method.
   */
  @Test
  public void testInstallationPrefs() throws Exception {
    importMavenProjects("projects/someproject.zip");
    MavenPlugin.getDefault().getMavenRuntimeManager().getDefaultRuntime();
    IMavenProjectFacade mavenProject = MavenPlugin.getDefault().getMavenProjectRegistry().getMavenProject(
        "org.sonatype.test", "someproject", "0.0.1-SNAPSHOT");
    Assert.assertNotNull(mavenProject);

    //add external maven runtime

    MavenRuntime newRuntime = MavenRuntimeManager.createExternalRuntime(externalRuntime);
    List<MavenRuntime> currRuntimes = MavenPlugin.getDefault().getMavenRuntimeManager().getMavenRuntimes();
    ArrayList<MavenRuntime> list = new ArrayList<MavenRuntime>(currRuntimes);
    list.add(newRuntime);
    MavenPlugin.getDefault().getMavenRuntimeManager().setRuntimes(list);
    MavenPlugin.getDefault().getMavenRuntimeManager().setDefaultRuntime(newRuntime);

    //create the global_settings.xml file to use
    FileOutputStream out = null;
    File repoDir = createTempDir("testing_repo");
    File globalSettingsFile = File.createTempFile("global_settings", ".xml");
    try {
      out = new FileOutputStream(globalSettingsFile);
      XMLWriter writer = new XMLWriter(out);
      writer.println("<settings>");
      writer.println("<localRepository>" + repoDir.getAbsolutePath() + "</localRepository>");
      writer.println("</settings>");
      writer.flush();
    } finally {
      if(out != null) {
        out.close();
      }
    }

    IProject project = mavenProject.getProject();
    IPath externalLocation = new Path(globalSettingsFile.getAbsolutePath());
    IFile linkedFile = project.getFile(externalLocation.lastSegment());
    linkedFile.createLink(externalLocation, IResource.NONE, null);

    SWTBotEditor editor = bot.editorByTitle(openFile(project, externalLocation.lastSegment()).getTitle());
    editor.bot().cTabItem("Source");

    bot.menu("Window").menu("Preferences").click();
    SWTBotShell shell = bot.shell("Preferences");
    try {
      shell.activate();

      bot.tree().expandNode("Maven").select("Installations");
      //assertFalse(new ButtonLocator("Browse...").isEnabled(getUI()));

      Assert.assertFalse(bot.button("Browse...").isEnabled());
      Assert.assertEquals(new File(externalRuntime, "conf/settings.xml"), new File(bot.textWithName(
          "globalSettingsText").getText()));
//      "Global settings from installation directory (open file):"
      //now check the embedded and make sure its blank
      bot.table().getTableItem(0).check();

      Assert.assertEquals("", bot.textWithName("globalSettingsText").getText());

      Assert.assertTrue(bot.button("Browse...").isEnabled());
      bot.textWithName("globalSettingsText").setText(globalSettingsFile.getAbsolutePath());

      bot.button("OK");
    } finally {
      SwtbotUtil.waitForClose(shell);
    }

    //now open the index view and make sure 'local' has the right value, 
    checkLocalRepo();

    //now for the user settings test
    String newRepoDir;

    shell = showUserSettingsPrefs();
    try {
      verifyRepoSettingsValue(repoDir.getAbsolutePath());
      newRepoDir = setUserSettingsXML(mavenProject.getProject());
    } finally {
      closeUserSettingPrefs(shell);
    }

    waitForAllBuildsToComplete();

    shell = showUserSettingsPrefs();
    try {
      verifyRepoSettingsValue(newRepoDir);
    } finally {
      closeUserSettingPrefs(shell);
    }

    //now open the index view and make sure 'local' has the right new value, 
    checkLocalRepo();
  }

  private void checkLocalRepo() throws Exception {
    showView("org.eclipse.m2e.core.views.MavenRepositoryView");
    SWTBotView view = openView("org.eclipse.m2e.core.views.MavenRepositoryView");
    SWTBotTree tree = view.bot().tree();
    Assert.assertNotNull(findItem(tree.expandNode("Local Repositories"),
        StringStartsWith.startsWith("Local Repository")).select());
  }

  protected String setUserSettingsXML(IProject project) throws Exception {
    //create the global_settings.xml file to use
    FileOutputStream userOut = null;
    File userRepoDir = createTempDir("user_testing_repo");
    File userSettings = File.createTempFile("user_settings", ".xml");
    try {
      userOut = new FileOutputStream(userSettings);
      XMLWriter writer = new XMLWriter(userOut);
      writer.println("<settings>");
      writer.println("<localRepository>" + userRepoDir.getAbsolutePath() + "</localRepository>");
      writer.println("</settings>");
      writer.flush();
    } finally {
      if(userOut != null) {
        userOut.close();
      }
    }
    IPath userExternalLocation = new Path(userSettings.getAbsolutePath());
    IFile linkedUserFile = project.getFile(userExternalLocation.lastSegment());
    linkedUserFile.createLink(userExternalLocation, IResource.NONE, null);
    updateUserSettingsValue(userSettings.getAbsolutePath());
    return userRepoDir.getAbsolutePath();
  }

  protected void updateUserSettingsValue(final String value) throws Exception {
    final SWTWorkbenchBot swtbot = bot;
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        try {
          assert (swtbot.textWithLabel("User Settings (open file):").getText().endsWith(value));
        } catch(Exception e) {
          Assert.fail("Value for settings file was wrong");
        }
      }
    });
  }

  protected void verifyRepoSettingsValue(final String value) throws Exception {
    final SWTWorkbenchBot swtbot = bot;
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        try {
          assert (swtbot.textWithLabel("Local Repository (From merged user and global settings):").getText()
              .endsWith(value));
        } catch(Exception e) {
          Assert.fail("Value for settings file was wrong");
        }
      }
    });
  }

  protected void selectEditorTab(final String id) throws Exception {
    final MavenPomEditor editor = (MavenPomEditor) getActivePage().getActiveEditor();
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        editor.setActivePage(id);
      }
    });
    waitForAllBuildsToComplete();
  }

  private void closeUserSettingPrefs(SWTBotShell shell) throws Exception {
    try {
      bot.button("OK").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }

  }

  private SWTBotShell showUserSettingsPrefs() {
    bot.menu("Window").menu("Preferences").click();
    SWTBotShell shell = bot.shell("Preferences");
    shell.activate();

    bot.tree().expandNode("Maven").select("User Settings");
    return shell;
  }

}
