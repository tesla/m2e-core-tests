/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;


public class PomQuickAssistProcessor implements IQuickAssistProcessor {

  public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean canFix(Annotation annotation) {
    // TODO Auto-generated method stub
    return false;
  }

  public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext invocationContext) {
    // TODO Auto-generated method stub
    return null;
  }

  public String getErrorMessage() {
    // TODO Auto-generated method stub
    return null;
  }

}
