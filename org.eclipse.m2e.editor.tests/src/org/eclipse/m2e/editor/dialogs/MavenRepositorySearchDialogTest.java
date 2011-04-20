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
package org.eclipse.m2e.editor.dialogs;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.w3c.dom.Element;

import junit.framework.Assert;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import org.apache.maven.model.Dependency;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.editing.PomHelper;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.integration.tests.common.SwtbotUtil;
import org.eclipse.m2e.integration.tests.common.UIIntegrationTestCase;

public class MavenRepositorySearchDialogTest extends UIIntegrationTestCase {

  private IProject project;

  MavenPomEditor editor;

  @Before
  public void setUp() throws Exception {
    setUserSettings("settings.xml");
    updateRepositoryRegistry();
    updateIndex("file:remoterepo");
    waitForAllBuildsToComplete();
  }

  @After
  public void tearDown() throws Exception {
    project.delete(true, true, new NullProgressMonitor());
  }

  /*
   * Ensure that a version is added to the pom
   */
  @Test
  public void testUnmanagedDependency() throws Exception {
    project = importProjects("projects/managedDependencyTest/noDep", new String[] {"pom.xml"})[0];
    waitForAllBuildsToComplete();
    editor = openPomFile(project.getName() + "/pom.xml");
    UIThreadRunnable.syncExec(new VoidResult() {
      public void run() {
        editor.setActivePage(IMavenConstants.PLUGIN_ID + ".pom.dependencies");
      }
    });
    bot.activeEditor().setFocus();

    bot.button("Add...").click();
    SWTBotShell shell = bot.shell("Select Dependency");
    try {
      shell.activate();
      shell.pressShortcut(Keystrokes.toKeys(SWT.ALT, 'E'));
      SWTBotText text = new SWTBotText((Text) bot.getFocusedWidget());
      text.setText("org.apache");
      SWTBotTreeItem item = bot.tree().getTreeItem("org.apache   apache");
      item.select();
      bot.button("OK").click();
      SwtbotUtil.waitForClose(shell);
      bot.activeEditor().saveAndClose();

      assertDependencyChild(project.getFile("pom.xml"), "", "org.apache", "apache", "7");
    } finally {
      if(shell.isOpen()) {
        bot.button("Cancel").click();
      }
      SwtbotUtil.waitForClose(shell);
    }
  }

  /*
   * Ensure that an unnecessary version is not added to the pom
   */
  @Test
  public void testManagedDependency() throws Exception {
    project = importProjects("projects/managedDependencyTest/managedDep", new String[] {"pom.xml"})[0];
    waitForAllBuildsToComplete();
    editor = openPomFile(project.getName() + "/pom.xml");
    UIThreadRunnable.syncExec(new VoidResult() {
      public void run() {
        editor.setActivePage(IMavenConstants.PLUGIN_ID + ".pom.dependencies");
      }
    });
    bot.activeEditor().setFocus();

    bot.button("Add...").click();
    SWTBotShell shell = bot.shell("Select Dependency");
    try {
      shell.activate();
      shell.pressShortcut(Keystrokes.toKeys(SWT.ALT, 'E'));
      SWTBotText text = new SWTBotText((Text) bot.getFocusedWidget());
      text.setText("org.apache");
      SWTBotTreeItem item = bot.tree().getTreeItem("org.apache   apache  (managed)");
      item.select();
      bot.button("OK").click();
      SwtbotUtil.waitForClose(shell);
      bot.activeEditor().saveAndClose();

      assertDependencyChild(project.getFile("pom.xml"), "", "org.apache", "apache", null);
    } finally {
      if(shell.isOpen()) {
        bot.button("Cancel").click();
      }
      SwtbotUtil.waitForClose(shell);
    }
  }

  /*
   * Ensure that the overriding version is added to the pom
   */
  @Test
  public void testOverridingManagedDependency() throws Exception {
    project = importProjects("projects/managedDependencyTest/managedDep", new String[] {"pom.xml"})[0];
    waitForAllBuildsToComplete();
    editor = openPomFile(project.getName() + "/pom.xml");
    UIThreadRunnable.syncExec(new VoidResult() {
      public void run() {
        editor.setActivePage(IMavenConstants.PLUGIN_ID + ".pom.dependencies");
      }
    });
    bot.activeEditor().setFocus();

    bot.button("Add...").click();
    SWTBotShell shell = bot.shell("Select Dependency");
    try {
      shell.activate();
      shell.pressShortcut(Keystrokes.toKeys(SWT.ALT, 'E'));
      SWTBotText text = new SWTBotText((Text) bot.getFocusedWidget());
      text.setText("org.apache");
      SWTBotTreeItem item = bot.tree().getTreeItem("org.apache   apache  (managed)");
      item.expand().getNode("4 [pom]").select();
      bot.button("OK").click();
      SwtbotUtil.waitForClose(shell);
      bot.activeEditor().saveAndClose();

      assertDependencyChild(project.getFile("pom.xml"), "", "org.apache", "apache", "4");
    } finally {
      if(shell.isOpen()) {
        bot.button("Cancel").click();
      }
      SwtbotUtil.waitForClose(shell);
    }
  }

  static void assertDependencyChild(IFile file, String msg, String groupId, String artifactId, String version)
      throws IOException, CoreException {
    IModelManager modelManager = StructuredModelManager.getModelManager();
    IDOMModel model = (IDOMModel) modelManager.getModelForEdit(file);

    Dependency d = new Dependency();
    d.setGroupId(groupId);
    d.setArtifactId(artifactId);
    Element dep = PomHelper.findDependency(model.getDocument(), d);
    Assert.assertNotNull("Dependency not found", dep);
    Assert.assertEquals(msg + ": element name", "dependency", dep.getLocalName());
    if(groupId != null) {
      Assert.assertEquals(msg + ":groupId", groupId, PomEdits.getTextValue(findChild(dep, "groupId")));
    } else {
      Assert.assertNull(msg + ":groupId", findChild(dep, "groupId"));
    }
    if(artifactId != null) {
      Assert.assertEquals(msg + ":artifactId", artifactId, PomEdits.getTextValue(findChild(dep, "artifactId")));
    } else {
      Assert.assertNull(msg + ":artifactId", findChild(dep, "artifactId"));
    }
    if(version != null) {
      Assert.assertEquals(msg + ":version", version, PomEdits.getTextValue(findChild(dep, "version")));
    } else {
      Assert.assertNull(msg + ":version", findChild(dep, "version"));
    }
  }
}
