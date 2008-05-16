/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml;

import org.w3c.dom.Node;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.graphics.Image;



/**
 * Does code competition. It is hooked to the XML editor by {@link PomContentAssistProcessor}.
 * 
 * @author Lukas Krecan
 */
public class PomTemplateCompletitionProcessor extends TemplateCompletionProcessor {
  private String contextTypeId = null;

  private Node currentNode;

  private String prefix;

  public String getContextTypeId() {
    return contextTypeId;
  }

  public void setContextTypeId(String contextTypeId) {
    this.contextTypeId = contextTypeId;
  }

  public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset, Node currentNode) {
    this.currentNode = currentNode;
    return super.computeCompletionProposals(viewer, offset);
  }

  @Override
  protected TemplateContextType getContextType(ITextViewer viewer, IRegion region) {
    ContextTypeRegistry registry = getTemplateContextRegistry();
    if(registry != null) {
      return registry.getContextType(contextTypeId);
    }
    return null;
  }

  @Override
  protected Image getImage(Template template) {
    return null;
  }

  @Override
  protected Template[] getTemplates(String contextTypeId) {
    return PomTemplateContext.fromId(contextTypeId).getTemplates(currentNode, prefix);
  }

  /**
   * Copy from the {@link TemplateCompletionProcessor}. We need to store prefix and do not want to ignore dots.
   */
  @Override
  protected String extractPrefix(ITextViewer viewer, int offset) {
    int i = offset;
    IDocument document = viewer.getDocument();
    if(i > document.getLength()) {
      return ""; //$NON-NLS-1$
    }

    try {
      while(i > 0) {
        char ch = document.getChar(i - 1);
        if(!Character.isJavaIdentifierPart(ch) && ch != '.' && ch != '-') {
          break;
        }
        i-- ;
      }
      prefix = document.get(i, offset - i);
      return prefix;
    } catch(BadLocationException e) {
      return ""; //$NON-NLS-1$
    }
  }

  private ContextTypeRegistry getTemplateContextRegistry() {
    return MvnIndexPlugin.getDefault().getTemplateContextRegistry();
  }

}
