/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.integration.tests;

import static org.hamcrest.Matchers.startsWith;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Assert;
import org.junit.Test;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.integration.tests.common.SwtbotUtil;
import org.maven.ide.eclipse.internal.index.NexusIndex;
import org.maven.ide.eclipse.internal.index.NexusIndexManager;
import org.maven.ide.eclipse.repository.IRepository;
import org.maven.ide.eclipse.repository.IRepositoryRegistry;


/**
 * @author Rich Seddon
 */
@SuppressWarnings("restriction")
public class MEclipse163ResolveDependenciesTest extends M2EUIIntegrationTestCase {

  private String projectName = "ResolveDepProject";

  protected void updateRepo() {

  }

  @Test
  public void testResolveDependencies() throws Exception {
    importZippedProject("projects/resolve_deps_test.zip");
    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    Assert.assertTrue(project.exists());
    waitForAllBuildsToComplete();
    //rebuild the mirror
    IRepositoryRegistry registry = MavenPlugin.getDefault().getRepositoryRegistry();
    waitForAllBuildsToComplete();
    List<IRepository> repos = registry.getRepositories(registry.SCOPE_SETTINGS);

    for(IRepository repo : repos) {
      buildFullRepoDetails(repo);
    }

    // there should be compile errors
    int severity = project.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
    Assert.assertEquals(IMarker.SEVERITY_ERROR, severity);

    SWTBotEclipseEditor editor = bot.editorByTitle(
        openFile(project, "src/main/java/org/sonatype/test/ResolveDepProject/App.java").getTitle()).toTextEditor();

    editor.pressShortcut(SWT.MOD1, '.'); // next annotation

    editor.pressShortcut(SWT.MOD1, '1');
    editor.pressShortcut(KeyStroke.getInstance(SWT.END));
    editor.pressShortcut(KeyStroke.getInstance(SWT.ARROW_UP));
    editor.pressShortcut(KeyStroke.getInstance(SWT.LF));

    SWTBotShell shell = bot.shell("Search in Maven repositories");
    try {
      shell.activate();

      String groupId = "org.jfree.chart";
      String artifactId = "jfreechart";
      String version = "1.0.7";
      bot.text().setText(artifactId);
      SWTBotTreeItem node = bot.tree().expandNode(groupId + "   " + artifactId);
      node.select(findNodeName(node, startsWith(version)));
    } finally {
      SwtbotUtil.waitForClose(shell);
    }

    waitForAllBuildsToComplete();

    assertProjectsHaveNoErrors();

  }

  /**
   * For any class details, we need a FULL_DETAILS
   * 
   * @param repo
   */
  private void buildFullRepoDetails(IRepository repo) throws Exception {
    NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();
    NexusIndex index = indexManager.getIndex(repo);
    IRepositoryRegistry registry = MavenPlugin.getDefault().getRepositoryRegistry();

    //build full repo details for the enabled non local/workspace repo
    if(index.isEnabled() && !(repo.equals(registry.getLocalRepository()))
        && !(repo.equals(registry.getWorkspaceRepository()))) {
      indexManager.setIndexDetails(repo, "full", null);
      indexManager.updateIndex(repo, true, null);
      waitForAllBuildsToComplete();
    }
  }
}
