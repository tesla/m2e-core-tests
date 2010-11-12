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

import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.wst.sse.core.text.IStructuredPartitions;
import org.eclipse.wst.xml.core.text.IXMLPartitions;
import org.eclipse.wst.xml.ui.StructuredTextViewerConfigurationXML;

/**
 * @author Lukas Krecan
 */
public class PomStructuredTextViewConfiguration extends StructuredTextViewerConfigurationXML {

  @Override
  public IContentAssistProcessor[] getContentAssistProcessors(ISourceViewer sourceViewer, String partitionType) {
    if(partitionType == IStructuredPartitions.DEFAULT_PARTITION || partitionType == IXMLPartitions.XML_DEFAULT) {
      return new IContentAssistProcessor[] {new PomContentAssistProcessor(sourceViewer)};
    }
    return super.getContentAssistProcessors(sourceViewer, partitionType);
  }
  
  @Override
  public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
    IHyperlinkDetector[] detectors = super.getHyperlinkDetectors(sourceViewer);
    if(detectors==null) {
      detectors = new IHyperlinkDetector[0];
    }

    IHyperlinkDetector[] pomDetectors = new IHyperlinkDetector[detectors.length + 1];
    pomDetectors[0] = new PomHyperlinkDetector();
    System.arraycopy(detectors, 0, pomDetectors, 1, detectors.length);
    
    return pomDetectors;
  }

  public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
    // TODO Auto-generated method stub
    IQuickAssistAssistant quickAssistAssistant = super.getQuickAssistAssistant(sourceViewer);
    quickAssistAssistant.setQuickAssistProcessor(new PomQuickAssistProcessor());
    return quickAssistAssistant;
  }

}

