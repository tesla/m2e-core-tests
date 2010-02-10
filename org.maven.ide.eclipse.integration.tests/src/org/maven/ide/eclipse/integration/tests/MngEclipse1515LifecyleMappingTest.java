
package org.maven.ide.eclipse.integration.tests;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withId;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.Assert;
import org.junit.Test;
import org.maven.ide.eclipse.editor.pom.MavenPomEditor;
import org.maven.ide.eclipse.integration.tests.common.ContextMenuHelper;
import org.maven.ide.eclipse.integration.tests.common.SwtbotUtil;


public class MngEclipse1515LifecyleMappingTest extends M2EUIIntegrationTestCase {

  public MngEclipse1515LifecyleMappingTest() {
    super();
  }

  /**
   * Main test method.
   */
  @Test
  public void testMgnEclipse1515() throws Exception {
    setXmlPrefs();
    String projectName = "lifecycleTest";
    IProject project = createQuickstartProject(projectName);
    Assert.assertNotNull(project);

    //open project prefs, navigate to maven->lifecycle mapping, make sure that the 'generic' lifecycle mapping is showing
    showGenericLifecycle(projectName);

    SWTBotEclipseEditor editor = bot.editorByTitle(openPomFile(projectName + "/pom.xml").getTitle()).toTextEditor();
    editor.bot().cTabItem("pom.xml").activate();

    //then set to customizable and make sure that one is showing
    findText("</project");
    editor.pressShortcut(KeyStroke.getInstance(SWT.ARROW_LEFT));

    editor.insertText("<build>" + //
        "<plugins>" + //
        "<plugin>" + //
        "<groupId>org.maven.ide.eclipse</groupId>" + //
        "<artifactId>lifecycle-mapping</artifactId>" + //
        "<version>0.9.9-SNAPSHOT</version>" + //
        "<configuration>" + //
        "<mappingId>customizable</mappingId>" + //
        "<configurators></configurators>" + //
        "<mojoExecutions></mojoExecutions>" + //
        "</configuration>" + //
        "</plugin>" + //
        "</plugins>" + //
        "</build>");
    editor.save();
    waitForAllBuildsToComplete();
    showCustomizableLifecycle(projectName);

    editor.setFocus();
    //then, back to generic
    replaceTextWithWrap("customizable", "generic", true);
    editor.save();

    waitForAllBuildsToComplete();
    showGenericLifecycle(projectName);

    editor.setFocus();
    //then switch to empty lifecycle mapping
    replaceTextWithWrap("generic", "NULL", true);
    editor.saveAndClose();
    waitForAllBuildsToComplete();
    showEmptyLifecycle(projectName);
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

  private void showEmptyLifecycle(String projectName) {
    SWTBotShell tree = showLifecyclePropsPage(projectName);
    try {
      Widget widgetLocator = bot.widget(withId("name", "noInfoLabel"));
      Assert.assertNotNull(widgetLocator);
    } finally {
      hideLifecyclePropsPage(tree);
    }
  }

  private void showCustomizableLifecycle(String projectName) {
    SWTBotShell tree = showLifecyclePropsPage(projectName);
    try {
      Widget widgetLocator = bot.widget(withId("name", "projectConfiguratorsTable"));
      Assert.assertNotNull(widgetLocator);
    } finally {
      hideLifecyclePropsPage(tree);
    }
  }

  private void hideLifecyclePropsPage(SWTBotShell shell) {
    try {
      bot.button("Cancel").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }
  }

  private SWTBotShell showLifecyclePropsPage(String projectName) {
    ContextMenuHelper.clickContextMenu(selectProject(projectName), "Properties");
    SWTBotShell shell = bot.shell("Properties for " + projectName);
    shell.activate();

    bot.tree().expandNode("Maven").select("Lifecycle Mapping");
    return shell;
  }

  private void showGenericLifecycle(String projectName) {
    SWTBotShell shell = showLifecyclePropsPage(projectName);
    try {
      Widget widgetLocator = bot.widget(withId("name", "goalsText"));
      Assert.assertNotNull(widgetLocator);
    } finally {
      hideLifecyclePropsPage(shell);
    }
  }

}
