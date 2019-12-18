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

import static org.junit.Assert.assertNotNull;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.junit.Test;


/**
 * Hello fellow tester: everytime this test finds a regression add an 'x' here: everytime you do mindless test update
 * add an 'y' here: yyy
 * 
 * @author mkleint
 */
public class HoverDependencyManagedTest extends AbstractPOMEditorTestCase {
  private IProject[] projects;

  @Override
public IFile loadProjectsAndFiles() throws Exception {
    projects = importProjects("projects/MNGECLIPSE-2541", new String[] {"parent2541/pom.xml", "child2541/pom.xml"},
        new ResolverConfiguration());
    waitForJobsToComplete();

    return (IFile) projects[1].findMember("pom.xml");

  }
  @Test
  public void testHasHover() throws CoreException {
    // Locate the area where we want to detect the hover
    String docString = sourceViewer.getDocument().get();
    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(projects[1]);
    assertNotNull(facade);
    assertNotNull(facade.getMavenProject(monitor));
    sourceViewer.setMavenProject(facade.getMavenProject());

    int offset = docString.indexOf("<artifactId>org.junit</artifactId>");

    PomTextHover hover = new PomTextHover(null, null, 0);
    IRegion region = hover.getHoverRegion(sourceViewer, offset + 5); // +5 as a way to point to the middle..
    assertNotNull(region);

    // with compound region and custom hover component this makes no longer sense.
    // String s = hover.getHoverInfo(sourceViewer, region);
    // assertTrue(s.contains("3.8.1"));
    // assertTrue(s.contains("org.eclipse.m2e:parent2541:"));
  }
}
