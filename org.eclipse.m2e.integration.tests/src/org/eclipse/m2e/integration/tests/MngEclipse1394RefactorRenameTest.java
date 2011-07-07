
package org.eclipse.m2e.integration.tests;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.integration.tests.common.ContextMenuHelper;
import org.eclipse.m2e.integration.tests.common.SwtbotUtil;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.Assert;
import org.junit.Test;


public class MngEclipse1394RefactorRenameTest extends M2EUIIntegrationTestCase {

  /**
   * Main test method.
   */
  @Test
  public void testMngEclipse1394RefactorRename() throws Exception {
    String originalName = "someTestProject";
    String newName = "testProject2";

    createArchetypeProject("maven-archetype-quickstart", "someTestProject");
    IMavenProjectFacade mavenProject = MavenPlugin.getMavenProjectRegistry().getMavenProject(
        "org.sonatype.test", originalName, "0.0.1-SNAPSHOT");
    Assert.assertNotNull(mavenProject);

    ContextMenuHelper.clickContextMenu(selectProject(originalName), "Refactor", "Rename Maven Artifact...");

    SWTBotShell shell = bot.shell("Rename Maven Artifact");
    try {
      shell.activate();

      bot.textWithLabel("Group Id:").setText("x.y.z");
      bot.textWithLabel("Artifact Id:").setText(newName);
      bot.textWithLabel("Version:").setText("1.1.1");

      bot.checkBox("Rename Eclipse project in Workspace").select();

      bot.button("OK").click();

    } finally {
      SwtbotUtil.waitForClose(shell);
    }
    waitForAllBuildsToComplete();

    IProject project2 = ResourcesPlugin.getWorkspace().getRoot().getProject(newName);
    Assert.assertTrue(project2.exists());
    mavenProject = MavenPlugin.getMavenProjectRegistry().getMavenProject("org.sonatype.test", originalName,
        "0.0.1-SNAPSHOT");
    Assert.assertNull(mavenProject);
    IMavenProjectFacade mavenProject2 = MavenPlugin.getMavenProjectRegistry().getMavenProject("x.y.z",
        newName, "1.1.1");
    Assert.assertNotNull(mavenProject2);
  }

}
