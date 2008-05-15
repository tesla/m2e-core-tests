/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml;

import org.w3c.dom.Node;
import org.w3c.dom.Text;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.eclipse.wst.xml.ui.internal.contentassist.XMLContentAssistProcessor;

import org.maven.ide.eclipse.editor.xml.template.PomTemplateCompletitionProcessor;


/**
 * @author Lukas Krecan
 */
@SuppressWarnings("restriction")
public class PomContentAssistProcessor extends XMLContentAssistProcessor {

  private PomTemplateCompletitionProcessor pomTemplateCompletitionProcessor = new PomTemplateCompletitionProcessor();

  private ISourceViewer sourceViewer;

  public PomContentAssistProcessor(ISourceViewer sourceViewer) {
    this.sourceViewer = sourceViewer;
  }

  @Override
  protected void addTagInsertionProposals(ContentAssistRequest contentAssistRequest, int childPosition) {
    String currentNodeName = getCurrentNode(contentAssistRequest).getNodeName();
    // TODO don't offer "parent" section if it is already present
    addTemplates(contentAssistRequest, PomTemplateContext.fromNodeName(currentNodeName));
    super.addTagInsertionProposals(contentAssistRequest, childPosition);
  }

  private Node getCurrentNode(ContentAssistRequest contentAssistRequest) {
    Node currentNode = contentAssistRequest.getNode();
    if(currentNode instanceof Text) {
      currentNode = currentNode.getParentNode();
    }
    return currentNode;
  }

  private void addTemplates(ContentAssistRequest contentAssistRequest, PomTemplateContext context) {
    if(contentAssistRequest != null) {
      boolean useProposalList = !contentAssistRequest.shouldSeparate();
      pomTemplateCompletitionProcessor.setContextTypeId(context.getContextTypeId());
      ICompletionProposal[] proposals = pomTemplateCompletitionProcessor.computeCompletionProposals(sourceViewer,
          contentAssistRequest.getReplacementBeginPosition(), getCurrentNode(contentAssistRequest));
      for(ICompletionProposal element : proposals) {
        if(useProposalList) {
          contentAssistRequest.addProposal(element);
        } else {
          contentAssistRequest.addMacro(element);
        }
      }
    }
  }
  
}
