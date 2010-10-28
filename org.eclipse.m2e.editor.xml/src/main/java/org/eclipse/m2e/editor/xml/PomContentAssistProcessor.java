/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.Text;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.eclipse.wst.xml.ui.internal.contentassist.XMLContentAssistProcessor;

/**
 * @author Lukas Krecan
 * @author Eugene Kuleshov
 */
@SuppressWarnings("restriction")
public class PomContentAssistProcessor extends XMLContentAssistProcessor {

  private static final ProposalComparator PROPOSAL_COMPARATOR = new ProposalComparator();
  
  private ISourceViewer sourceViewer;

  public PomContentAssistProcessor(ISourceViewer sourceViewer) {
    this.sourceViewer = sourceViewer;
  }

    //broken
  
  protected void addTagNameProposals(ContentAssistRequest contentAssistRequest, int childPosition) {
    String currentNodeName = getCurrentNode(contentAssistRequest).getNodeName();
    PomTemplateContext context = PomTemplateContext.fromNodeName(currentNodeName);
    if(PomTemplateContext.CONFIGURATION == context) 
    {
      contentAssistRequest.setReplacementBeginPosition(contentAssistRequest.getReplacementBeginPosition() - 1);
      contentAssistRequest.setReplacementLength(contentAssistRequest.getReplacementLength() + 1);
      addProposals(contentAssistRequest, context, getCurrentNode(contentAssistRequest), contentAssistRequest.getMatchString());
    }
    if(PomTemplateContext.UNKNOWN == context) 
    {
      context = PomTemplateContext.fromNodeName(getCurrentNode(contentAssistRequest).getParentNode().getNodeName());
      if(PomTemplateContext.CONFIGURATION == context) 
      {
        addProposals(contentAssistRequest, context, getCurrentNode(contentAssistRequest).getParentNode(), contentAssistRequest.getMatchString());
      }
    }
    super.addTagNameProposals(contentAssistRequest, childPosition);
  }
  
  
  @Override
  protected void addTagInsertionProposals(ContentAssistRequest contentAssistRequest, int childPosition) {
    String currentNodeName = getCurrentNode(contentAssistRequest).getNodeName();
    // TODO don't offer "parent" section if it is already present
    addProposals(contentAssistRequest, PomTemplateContext.fromNodeName(currentNodeName));
    super.addTagInsertionProposals(contentAssistRequest, childPosition);
  }

  private Node getCurrentNode(ContentAssistRequest contentAssistRequest) {
    Node currentNode = contentAssistRequest.getNode();
    if(currentNode instanceof Text) {
      currentNode = currentNode.getParentNode();
    }
    return currentNode;
  }

  private void addProposals(ContentAssistRequest request, PomTemplateContext context) {
    ITextSelection selection = (ITextSelection) sourceViewer.getSelectionProvider().getSelection();
    int offset = request.getReplacementBeginPosition();
    // adjust offset to end of normalized selection
    if(selection.getOffset() == offset) {
      offset = selection.getOffset() + selection.getLength();
    }

    String prefix = extractPrefix(sourceViewer, offset);
    
    addProposals(request, context, getCurrentNode(request), prefix);
  }
  
  private void addProposals(ContentAssistRequest request, PomTemplateContext context, Node currentNode, String prefix) {
    if(request != null) {
      ITextFileBuffer buf = FileBuffers.getTextFileBufferManager().getTextFileBuffer(sourceViewer.getDocument());
      IFileStore folder = buf.getFileStore();
      File file = new File(folder.toURI());
      IPath path = Path.fromOSString(file.getAbsolutePath());
      IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(path);
      IProject prj = ifile != null ? ifile.getProject() : null;

      ICompletionProposal[] templateProposals = getTemplateProposals(prj, sourceViewer,
          request.getReplacementBeginPosition(), context.getContextTypeId(), currentNode, prefix);
      for(ICompletionProposal proposal : templateProposals) {
        if(request.shouldSeparate()) {
          request.addMacro(proposal);
        } else {
          request.addProposal(proposal);
        }
      }
    }
  }
  
  private ICompletionProposal[] getTemplateProposals(IProject project, ITextViewer viewer, int offset, String contextTypeId, Node currentNode, String prefix) {
    ITextSelection selection = (ITextSelection) viewer.getSelectionProvider().getSelection();

    // adjust offset to end of normalized selection
    if(selection.getOffset() == offset) {
      offset = selection.getOffset() + selection.getLength();
    }

//    String prefix = extractPrefix(viewer, offset);
    Region region = new Region(offset - prefix.length(), prefix.length()); 
    TemplateContext context = createContext(viewer, region, contextTypeId);
    if(context == null) {
      return new ICompletionProposal[0];
    }

    // name of the selection variables {line, word}_selection 
    context.setVariable("selection", selection.getText()); //$NON-NLS-1$

    PomTemplateContext templateContext = PomTemplateContext.fromId(contextTypeId);
    Image image = null;
    switch(templateContext) {
      case CONFIGURATION:
        image = MvnImages.IMG_PARAMETER;
        break;
      case PLUGINS:
        image = MvnImages.IMG_PLUGIN;
        break;
      case DEPENDENCIES:
        image = MvnImages.IMG_JAR;
        break;
      case EXECUTIONS:
        image = MvnImages.IMG_EXECUTION;
        break;
      case PROFILES:
        image = MvnImages.IMG_PROFILE;
        break;
      case PROPERTIES:
        image = MvnImages.IMG_PROPERTY;
        break;
      case REPOSITORIES:
        image = MvnImages.IMG_REPOSITORY;
        break;
    }
    
    List<TemplateProposal> matches = new ArrayList<TemplateProposal>();
    Template[] templates = templateContext.getTemplates(project, currentNode, prefix);
    for(final Template template : templates) {
      try {
        context.getContextType().validate(template.getPattern());
        if(template.matches(prefix, context.getContextType().getId())) {
          TemplateProposal proposal = new TemplateProposal(template, context, region, image, getRelevance(template, prefix)) {
            public String getAdditionalProposalInfo() {
              return getTemplate().getDescription();
            }

            public String getDisplayString() {
              return template.getName();
            }
          };
          matches.add(proposal);
        }
      } catch(TemplateException e) {
        // ignore
      }
    }
    if (templateContext!=PomTemplateContext.VERSION) {
      // versions are already sorted with o.a.m.artifact.versioning.ComparableVersion
      Collections.sort(matches, PROPOSAL_COMPARATOR);
    }

    return (ICompletionProposal[]) matches.toArray(new ICompletionProposal[matches.size()]);

  }

  protected TemplateContext createContext(ITextViewer viewer, IRegion region, String contextTypeId) {
    TemplateContextType contextType= getContextType(viewer, region, contextTypeId);
    if (contextType != null) {
      IDocument document= viewer.getDocument();
      return new DocumentTemplateContext(contextType, document, region.getOffset(), region.getLength());
    }
    return null;
  }

  protected int getRelevance(Template template, String prefix) {
    if (template.getName().startsWith(prefix))
      return 90;
    return 0;
  }
  
  protected TemplateContextType getContextType(ITextViewer viewer, IRegion region, String contextTypeId) {
    ContextTypeRegistry registry = MvnIndexPlugin.getDefault().getTemplateContextRegistry();
    if(registry != null) {
      return registry.getContextType(contextTypeId);
    }
    return null;
  }
  
  public static final String extractPrefix(ITextViewer viewer, int offset) {
    int i = offset;
    IDocument document = viewer.getDocument();
    if(i > document.getLength()) {
      return ""; //$NON-NLS-1$
    }

    try {
      while(i > 0) {
        char ch = document.getChar(i - 1);
        if(ch == '>' || ch == '<' || ch == ' ' || ch == '\n') {
          break;
        }
        i-- ;
      }
      return document.get(i, offset - i);
    } catch(BadLocationException e) {
      return ""; //$NON-NLS-1$
    }
  }

  
  static final class ProposalComparator implements Comparator<TemplateProposal> {
    public int compare(TemplateProposal o1, TemplateProposal o2) {
      int res = o2.getRelevance() - o1.getRelevance();
      if(res == 0) {
        res = o1.getDisplayString().compareTo(o2.getDisplayString());
      }
      return res;
    }
  }
  
}
