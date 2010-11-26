/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.m2e.core.project.ResolverConfiguration;

/**
 * Hello fellow tester:
 * everytime this test finds a regression add an 'x' here:
 * everytime you do mindless test update add an 'y' here:
 * @author mkleint
 *
 */
public class HoverDependencyManagedTest extends AbstractPOMEditorTestCase {

  public IFile loadProjectsAndFiles() throws Exception {
    IProject[] projects = importProjects("projects/MNGECLIPSE-2541", new String[] {
        "child2541/pom.xml", 
        "parent2541/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
    
    return (IFile) projects[0].findMember("pom.xml");
    
  }
  
  public void testHasHover() throws BadLocationException {
    //Locate the area where we want to detect the hover
    IHyperlink[] links = new PomHyperlinkDetector().detectHyperlinks(sourceViewer, new Region(sourceViewer.getDocument().getLineOffset(14) + 4, 0), true);
    assertTrue(links.length >= 1);
    String s = new PomTextHover(null, null, 0).getHoverInfo(sourceViewer, links[0].getHyperlinkRegion());
    assertTrue(s.contains("3.8.1"));
    assertTrue(s.contains("org.eclipse.m2e:parent2541:"));
  }
}
