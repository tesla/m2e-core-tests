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
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.m2e.core.project.ResolverConfiguration;

/**
 * Hello fellow tester:
 * everytime this test finds a regression add an 'x' here:
 * everytime you do mindless test update add an 'y' here: y
 * @author mkleint
 *
 */

public class HoverTest extends AbstractPOMEditorTestCase {

  public IFile loadProjectsAndFiles() throws Exception {
    IProject[] projects = importProjects("projects/Hyperlink", new String[] {
        "hyperlinkParent/pom.xml",
        "hyperlinkChild/pom.xml"
        }, new ResolverConfiguration());
    waitForJobsToComplete();
    
    return (IFile) projects[1].findMember("pom.xml");
    
  }
  
  public void testHasHover() {
    //Locate the area where we want to detect the hover
    String docString = sourceViewer.getDocument().get();
    int offset = docString.indexOf("${aProperty}");
    
    PomTextHover hover = new PomTextHover(null, null, 0);
    IRegion region = hover.getHoverRegion(sourceViewer, offset + 5); //+5 as a way to point to the middle..
    String s = hover.getHoverInfo(sourceViewer, region);
    assertTrue(s.contains("theValue"));
    assertTrue(s.contains("org.eclipse.m2e:hyperlinkParent"));
  }
}
