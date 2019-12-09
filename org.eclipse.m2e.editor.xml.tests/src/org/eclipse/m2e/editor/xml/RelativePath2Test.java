/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.project.ResolverConfiguration;


/**
 * Hello fellow tester: everytime this test finds a regression add an 'x' here: x everytime you do mindless test update
 * add an 'y' here:
 * 
 * @author mkleint
 */

public class RelativePath2Test extends AbstractCompletionTest {

  protected IFile loadProjectsAndFiles() throws Exception {
    // Create the projects
    IProject[] projects = importProjects("projects/MNGECLIPSE-2601", new String[] {"parent2601/pom.xml",
        "parent2601/child2/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
    return (IFile) projects[1].findMember("pom.xml");
  }

  public void testRelativePath() throws Exception {
    assertEquals("../pom.xml",
        PomContentAssistProcessor.findRelativePath(sourceViewer, "org.eclipse.m2e", "parent2601", "0.0.1-SNAPSHOT"));
  }

}
