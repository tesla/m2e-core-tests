/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.editor.pom.PomHyperlinkDetector;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.junit.Test;


/**
 * Hello fellow tester: everytime this test finds a regression add an 'x' here: everytime you do mindless test update
 * add an 'y' here: yy
 * 
 * @author mkleint
 */
public class HyperlinkTest extends AbstractPOMEditorTestCase {
  private IFile parentPom;

  IProject[] projects;

  @Override
public IFile loadProjectsAndFiles() throws Exception {
    projects = importProjects("projects/Hyperlink", new String[] {"hyperlinkParent/pom.xml", "hyperlinkChild/pom.xml"},
        new ResolverConfiguration());
    waitForJobsToComplete();

    parentPom = (IFile) projects[0].findMember("pom.xml");
    System.out.println(parentPom.exists());
    return (IFile) projects[1].findMember("pom.xml");

  }
  @Test
  public void testHasLink() throws Exception {
    // Locate the area where we want to detect the link
    IRegion region = new Region(sourceViewer.getDocument().getLineOffset(12) + 17, 10);
    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(projects[1]);
    assertNotNull(facade);
    assertNotNull(facade.getMavenProject(monitor));
    sourceViewer.setMavenProject(facade.getMavenProject());

    IHyperlink[] links = new PomHyperlinkDetector().detectHyperlinks(sourceViewer, region, true);
    assertNotNull(links);
    assertEquals(1, links.length);
    assertNotNull(links[0].getHyperlinkText());
    assertTrue(links[0].getHyperlinkText().contains("aProperty"));

    // test opening the link
    links[0].open();
    IWorkbench wbch = PlatformUI.getWorkbench();
    IEditorPart editor = wbch.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    assertTrue(editor instanceof MavenPomEditor);
    assertEquals(parentPom, ((MavenPomEditor) editor).getPomFile());
    ((MavenPomEditor) editor).close(false);
  }
}
