
package org.eclipse.m2e.integration.tests;

import org.eclipse.m2e.integration.tests.common.SwtbotUtil;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.Ignore;
import org.junit.Test;


public class MEclipse178IssueTrackerTest extends M2EUIIntegrationTestCase {

  /**
   * 
   */
  private static final String TEST_PROJECT = "test-project";

  @Test @Ignore
  public void testIssueTracker() throws Exception {
    createQuickstartProject(TEST_PROJECT);

    openIssueTracking(TEST_PROJECT);

    SWTBotShell shell = bot.shell("Open Browser");
    try {
      shell.activate();

      bot.button("OK").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }

    SWTBotEditor editor = bot.editorByTitle(openPomFile("test-project/pom.xml").getTitle());
    editor.bot().cTabItem("pom.xml").activate();
//    ui.click(new CTabItemLocator("pom.xml"));
    replaceText("</dependencies>",
        "</dependencies><issueManagement><system>JIRA</system><url>http://jira.codehaus.org</url></issueManagement>");

    editor.saveAndClose();

    waitForAllBuildsToComplete();

    openIssueTracking(TEST_PROJECT);

    bot.sleep(10000);
    waitForAllBuildsToComplete();

    takeScreenShot("issue-tracking");
    bot.editorByTitle("http://jira.codehaus.org").close();
  }

}
