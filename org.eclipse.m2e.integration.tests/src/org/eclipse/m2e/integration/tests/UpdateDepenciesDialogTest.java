/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.integration.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.WidgetResult;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.PlatformUI;

import org.apache.maven.model.Model;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.ui.internal.dialogs.UpdateDepenciesDialog;
import org.eclipse.m2e.integration.tests.common.SonatypeSWTBot;
import org.eclipse.m2e.integration.tests.common.SwtbotUtil;
import org.eclipse.m2e.tests.common.FileHelpers;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;


/**
 * UpdateDepenciesDialogTest
 */
public class UpdateDepenciesDialogTest extends M2EUIIntegrationTestCase {

  UpdateDepenciesDialog dialog;

  @BeforeClass
  public static void setUp() throws Exception {
    importProjects("projects/updateDepenciesDialogTest/other", new String[] {"pom.xml"});
    importProjects("projects/updateDepenciesDialogTest/withChildren", new String[] {"pom.xml", "projectA/pom.xml",
        "projectB/pom.xml"});
    bot = new SonatypeSWTBot();
    waitForAllBuildsToComplete();
  }

  @After
  public void tearDown() throws Exception {
    if(dialog != null) {
      dialog.close();
    }
  }

  @Test
  public void test_Hierarchy() throws Exception {
    openDialog(new IProject[0]);
    SWTBotShell shell = bot.shell("Update Maven Dependencies");
    try {
      shell.activate();
      SWTBotTree tree = bot.tree();
      Assert.assertEquals(0, tree.getTreeItem("other").getItems().length);
      Assert.assertEquals(2, tree.getTreeItem("withChildren").getItems().length);
    } finally {
      try {
        if(shell.isOpen()) {
          bot.button("Cancel").click();
        }
      } catch(Exception e) {
        // do nothing
      }
      SwtbotUtil.waitForClose(shell);
    }
  }

  @Test
  public void test_ContextMenuPreselection() throws Exception {
    bot.viewById(PACKAGE_EXPLORER_VIEW_ID).bot().tree().setFocus();
    getSubMenuItem(getProjectTreeItem("projectA").contextMenu("Maven"), "Update Dependencies...").click();

    SWTBotShell shell = bot.shell("Update Maven Dependencies");
    try {
      shell.activate();
      Assert.assertFalse(bot.tree().getTreeItem("other").isChecked());

      SWTBotTreeItem withChildrenTree = bot.tree().getTreeItem("withChildren");
      Assert.assertFalse(withChildrenTree.isChecked());
      Assert.assertTrue(getChild(withChildrenTree, "projectA").isChecked());
      Assert.assertFalse(getChild(withChildrenTree, "projectB").isChecked());
    } finally {
      try {
        if(shell.isOpen()) {
          bot.button("Cancel").click();
        }
      } catch(Exception e) {
        // do nothing
      }
      SwtbotUtil.waitForClose(shell);
    }
  }

  @Test
  public void test_SelectAll() throws Exception {
    openDialog(new IProject[0]);
    SWTBotShell shell = bot.shell("Update Maven Dependencies");
    try {
      shell.activate();
      bot.button("Select All").click();
      SWTBotTree tree = bot.tree();
      checkTreeState(tree.getAllItems(), true);

      // Close dialog
      bot.button("OK").click();
      SwtbotUtil.waitForClose(shell);

      // Check output
      Assert.assertEquals(4, dialog.getSelectedProjects().length);
      Assert.assertFalse(dialog.isOffline());
    } finally {
      try {
        if(shell.isOpen()) {
          bot.button("Cancel").click();
        }
      } catch(Exception e) {
        // do nothing
      }
      SwtbotUtil.waitForClose(shell);
    }
  }

  @Test
  public void test_DeselectAll() throws Exception {
    openDialog(new IProject[] {getProject("projectA"), getProject("other")});
    Assert.assertTrue(bot.tree().getTreeItem("other").isChecked());
    Assert.assertTrue(bot.tree().getTreeItem("withChildren").getNode("projectA").isChecked());

    SWTBotShell shell = bot.shell("Update Maven Dependencies");
    try {
      shell.activate();
      bot.button("Deselect All").click();
      SWTBotTree tree = bot.tree();
      checkTreeState(tree.getAllItems(), false);

      // Close dialog
      bot.button("OK").click();
      SwtbotUtil.waitForClose(shell);

      // Check output
      Assert.assertEquals(0, dialog.getSelectedProjects().length);
      Assert.assertFalse(dialog.isOffline());
    } finally {
      try {
        if(shell.isOpen()) {
          bot.button("Cancel").click();
        }
      } catch(Exception e) {
        // do nothing
      }
      SwtbotUtil.waitForClose(shell);
    }
  }

  @Test
  public void test_PreselectedProjects() throws Exception {
    openDialog(new IProject[] {getProject("projectA"), getProject("other")});
    Assert.assertTrue(bot.tree().getTreeItem("other").isChecked());
    Assert.assertTrue(bot.tree().getTreeItem("withChildren").getNode("projectA").isChecked());

    SWTBotShell shell = bot.shell("Update Maven Dependencies");
    try {
      shell.activate();
      // Close dialog
      bot.button("OK").click();
      SwtbotUtil.waitForClose(shell);

      // Check output
      Assert.assertEquals(2, dialog.getSelectedProjects().length);
      Assert.assertTrue(contains(dialog.getSelectedProjects(), "projectA"));
      Assert.assertTrue(contains(dialog.getSelectedProjects(), "other"));
      Assert.assertFalse(dialog.isOffline());
    } finally {
      try {
        if(shell.isOpen()) {
          bot.button("Cancel").click();
        }
      } catch(Exception e) {
        // do nothing
      }
      SwtbotUtil.waitForClose(shell);
    }
  }

  @Test
  public void test_OfflineMode() throws Exception {
    openDialog(new IProject[0]);

    SWTBotShell shell = bot.shell("Update Maven Dependencies");
    try {
      shell.activate();
      // Close dialog
      bot.checkBox("Offline").click();
      bot.button("OK").click();
      SwtbotUtil.waitForClose(shell);

      // Check output
      Assert.assertTrue(dialog.isOffline());
    } finally {
      try {
        if(shell.isOpen()) {
          bot.button("Cancel").click();
        }
      } catch(Exception e) {
        // do nothing
      }
      SwtbotUtil.waitForClose(shell);
    }
  }

  private boolean contains(IProject[] projects, String text) {
    for(IProject project : projects) {
      if(project.getName().equals(text))
        return true;
    }
    return false;
  }

  private SWTBotTreeItem getChild(SWTBotTreeItem parent, String itemText) {
    for(SWTBotTreeItem item : parent.getItems()) {
      if(itemText.equals(item.getText())) {
        return item;
      }
    }
    throw new WidgetNotFoundException("Unable to find " + itemText);
  }

  private IProject getProject(String name) {
    return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
  }

  private void openDialog(final IProject[] projects) {
    PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

      public void run() {
        dialog = new UpdateDepenciesDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), projects);
        dialog.setBlockOnOpen(false);
        dialog.open();
      }
    });
  }

  private SWTBotTreeItem getProjectTreeItem(String projectName) throws Exception {
    return bot.viewById(PACKAGE_EXPLORER_VIEW_ID).bot().tree().getTreeItem(projectName);
  }

  private static IProject[] importProjects(String basedir, String[] pomNames) throws IOException, CoreException {
    MavenModelManager mavenModelManager = MavenPlugin.getMavenModelManager();
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

    File src = new File(basedir);
    File dst = new File(root.getLocation().toFile(), src.getName());
    FileHelpers.copyDir(src, dst);

    final ArrayList<MavenProjectInfo> projectInfos = new ArrayList<MavenProjectInfo>();
    for(String pomName : pomNames) {
      File pomFile = new File(dst, pomName);
      Model model = mavenModelManager.readMavenModel(pomFile);
      MavenProjectInfo projectInfo = new MavenProjectInfo(pomName, pomFile, model, null);
      setBasedirRename(projectInfo);
      projectInfos.add(projectInfo);
    }

    final ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration(new ResolverConfiguration());

    final ArrayList<IMavenProjectImportResult> importResults = new ArrayList<IMavenProjectImportResult>();

    ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        importResults.addAll(MavenPlugin.getProjectConfigurationManager().importProjects(projectInfos,
            importConfiguration, monitor));
      }
    }, MavenPlugin.getProjectConfigurationManager().getRule(), IWorkspace.AVOID_UPDATE, monitor);

    IProject[] projects = new IProject[projectInfos.size()];
    for(int i = 0; i < projectInfos.size(); i++ ) {
      IMavenProjectImportResult importResult = importResults.get(i);
      Assert.assertSame(projectInfos.get(i), importResult.getMavenProjectInfo());
      projects[i] = importResult.getProject();
      Assert.assertNotNull("Failed to import project " + projectInfos, projects[i]);

      /*
       * Sanity check: make sure they were all imported
       */
        Model model = projectInfos.get(0).getModel();
        IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(projects[i], monitor);
        if(facade == null) {
          Assert.fail("Project " + model.getGroupId() + "-" + model.getArtifactId() + "-" + model.getVersion()
              + " was not imported. Errors: "
              + WorkspaceHelpers.toString(WorkspaceHelpers.findErrorMarkers(projects[i])));
        }
    }

    return projects;
  }

  private static void setBasedirRename(MavenProjectInfo projectInfo) throws IOException {
    File workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
    File basedir = projectInfo.getPomFile().getParentFile().getCanonicalFile();

    projectInfo.setBasedirRename(basedir.getParentFile().equals(workspaceRoot) ? MavenProjectInfo.RENAME_REQUIRED
        : MavenProjectInfo.RENAME_NO);
  }

  private static SWTBotMenu getSubMenuItem(final SWTBotMenu parent, final String menuText) {
    MenuItem menuItem = UIThreadRunnable.syncExec(new WidgetResult<MenuItem>() {

      public MenuItem run() {
        Menu bar = parent.widget.getMenu();
        if(bar != null) {
          for(MenuItem item : bar.getItems()) {
            if(item.getText().equals(menuText)) {
              return item;
            }
          }
        }
        return null;
      }
    });
    if(menuItem == null) {
      throw new WidgetNotFoundException("Did not find menu: " + menuText);
    }
    return new SWTBotMenu(menuItem);
  }

  private static void checkTreeState(SWTBotTreeItem[] items, boolean state) {
    for(SWTBotTreeItem item : items) {
      if(state != item.isChecked()) {
        throw new RuntimeException(item.getText() + " checkstate is " + item.isChecked() + " expected " + state );
      }
      checkTreeState(item.getItems(), state);
    }
  }
}
