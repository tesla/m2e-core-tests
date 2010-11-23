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

import java.io.IOException;

import junit.framework.Assert;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.exceptions.ResourceAlreadyExists;
import org.eclipse.wst.sse.core.internal.provisional.exceptions.ResourceInUse;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;
import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;
import org.eclipse.wst.xml.core.internal.encoding.XMLDocumentLoader;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.ui.StructuredTextViewerConfigurationXML;
import org.eclipse.wst.xml.ui.internal.contentassist.XMLContentAssistProcessor;


public class PropertyCompletionTest extends AbstractMavenProjectTestCase {

  protected String projectName = null;

  protected String fileName = null;

  protected IFile file = null;

  protected IEditorPart textEditorPart = null;

  protected ITextEditor editor = null;

  protected IStructuredDocument document = null;

  protected StructuredTextViewer sourceViewer = null;

  private IStructuredModel model;

  protected XMLContentAssistProcessor xmlContentAssistProcessor = null;

  protected void setUp() throws Exception {
    super.setUp();

    //Create the projects
    IProject[] projects = importProjects("projects/MNGECLIPSE-2576", new String[] {"child2576/pom.xml",
        "parent2576/pom.xml"}, new ResolverConfiguration());
    file = (IFile) projects[0].findMember("pom.xml");
    waitForJobsToComplete();

    //Initialize random objects from the XML editor
    loadXMLFile();
    initializeSourceViewer();
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

  protected void initializeSourceViewer() {
    // some test environments might not have a "real" display
    if(Display.getCurrent() != null) {

      Shell shell = null;
      Composite parent = null;

      if(PlatformUI.isWorkbenchRunning()) {
        shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
      } else {
        shell = new Shell(Display.getCurrent());
      }
      parent = new Composite(shell, SWT.NONE);

      // dummy viewer
      sourceViewer = new StructuredTextViewer(parent, null, null, false, SWT.NONE);
    } else {
      Assert.fail("Unable to run the test as a display must be available.");
    }

    configureSourceViewer();
  }

  protected void configureSourceViewer() {
    sourceViewer.configure(new StructuredTextViewerConfigurationXML());

    sourceViewer.setDocument(document);
  }

  protected void loadXMLFile() throws ResourceAlreadyExists, ResourceInUse, IOException, CoreException {
    IModelManager modelManager = StructuredModelManager.getModelManager();
    model = modelManager.getModelForEdit(file);
    document = model.getStructuredDocument();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    model.releaseFromEdit();
    xmlContentAssistProcessor.release();
  }

  private ICompletionProposal[] getProposals(int offset) throws Exception {
    return xmlContentAssistProcessor.computeCompletionProposals(sourceViewer, offset);
  }

}
