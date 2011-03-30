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
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.xml.ui.internal.contentassist.XMLContentAssistProcessor;


@SuppressWarnings("restriction")
public abstract class AbstractCompletionTest extends AbstractPOMEditorTestCase {
  protected XMLContentAssistProcessor xmlContentAssistProcessor = null;
  
  protected IFile loadProjectsAndFiles() throws Exception {
    return null;
  }

  protected void setUp() throws Exception {
    super.setUp();
    xmlContentAssistProcessor = new PomContentAssistProcessor(sourceViewer);
  }
  

  protected void tearDown() throws Exception {
    try {
      xmlContentAssistProcessor.release();
    } finally {
      super.tearDown();
    }
  }

  protected ICompletionProposal[] getProposals(int offset) throws Exception {
    return xmlContentAssistProcessor.computeCompletionProposals(sourceViewer, offset);
  }
}
