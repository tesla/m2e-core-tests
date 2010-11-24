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
import org.eclipse.wst.xml.ui.internal.contentassist.XMLContentAssistProcessor;


public class PropertyCompletionTest extends AbstractPOMEditorTestCase {
  protected XMLContentAssistProcessor xmlContentAssistProcessor = null;
  
  protected IFile loadProjectsAndFiles() throws Exception {
    //Create the projects
    IProject[] projects = importProjects("projects/MNGECLIPSE-2576", new String[] {"child2576/pom.xml",
    "parent2576/pom.xml"}, new ResolverConfiguration());
    waitForJobsToComplete();
    return (IFile) projects[0].findMember("pom.xml");
  }

  protected void setUp() throws Exception {
    super.setUp();
    xmlContentAssistProcessor = new PomContentAssistProcessor(sourceViewer);
  }
  
  public void testCompletion() throws Exception {
    //Get the location of the place where we want to start the completion
    int offset = sourceViewer.getDocument().getLineOffset(11) + 24;
    IDOMNode node = (IDOMNode) ContentAssistUtils.getNodeAt(sourceViewer, offset);
    assertEquals("anotherProperty", node.getLocalName());

    ICompletionProposal[] proposals = getProposals(offset);
    assertTrue("Length less than 1", proposals.length > 1);
    assertEquals(InsertExpressionProposal.class, proposals[0].getClass());
    assertEquals("${aProperty}", ((InsertExpressionProposal) proposals[0]).getDisplayString());
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    xmlContentAssistProcessor.release();
  }

  private ICompletionProposal[] getProposals(int offset) throws Exception {
    return xmlContentAssistProcessor.computeCompletionProposals(sourceViewer, offset);
  }

}
