/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;

/**
 * Hello fellow tester:
 * everytime this test finds a regression add an 'x' here:
 * everytime you do mindless test update add an 'y' here: 
 * @author mkleint
 *
 */

public class RelativePath3Test extends AbstractCompletionTest {
  
  
  protected IFile loadProjectsAndFiles() throws Exception {
    //Create the projects
    IProject[] projects = importProjects("projects/MNGECLIPSE-2601", 
        new String[] {
        "parent2601/pom.xml",
        "child1/child3/pom.xml"
        }, new ResolverConfiguration());
    waitForJobsToComplete();
    return (IFile) projects[1].findMember("pom.xml");
  }

  public void testRelativePath() throws Exception {
    assertEquals("../../parent2601/pom.xml", PomContentAssistProcessor.findRelativePath(sourceViewer, "org.eclipse.m2e", "parent2601", "0.0.1-SNAPSHOT").toOSString());
  }

}
