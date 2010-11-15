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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.util.ImageSupport;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.editor.xml.internal.Messages;

public class PomQuickAssistProcessor implements IQuickAssistProcessor {

  public static final String PROJECT_NODE = "project"; //$NON-NLS-1$
  public static final String XSI_VALUE = " xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"+ //$NON-NLS-1$
  "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\""; //$NON-NLS-1$
  
  public boolean canAssist(IQuickAssistInvocationContext arg0) {
    return true;
  }

  public boolean canFix(Annotation an) {
    
    if (an instanceof MarkerAnnotation) {
      MarkerAnnotation mark = (MarkerAnnotation) an;
      try {
        if (IMavenConstants.MARKER_ID.equals(mark.getMarker().getType())) {
          String hint = mark.getMarker().getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, "");
          if (!hint.equals("")) {
            return true;
          }
        }
      } catch(CoreException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return false;
  }
  
  public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext context) {
   List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
   Iterator<Annotation> annotationIterator = context.getSourceViewer().getAnnotationModel().getAnnotationIterator();
   while(annotationIterator.hasNext()){
     Annotation annotation = annotationIterator.next();
     if (annotation instanceof MarkerAnnotation) {
       MarkerAnnotation mark = (MarkerAnnotation) annotation;
       try {
         Position position = context.getSourceViewer().getAnnotationModel().getPosition(annotation);
         int lineNum = context.getSourceViewer().getDocument().getLineOfOffset(position.getOffset()) + 1;
         int currentLineNum = context.getSourceViewer().getDocument().getLineOfOffset(context.getOffset()) + 1;
         if (currentLineNum == lineNum) {
           if (IMavenConstants.MARKER_ID.equals(mark.getMarker().getType())) {
             String hint = mark.getMarker().getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, "");
             if (hint.equals("parent_groupid")) {
               
             }
             if (hint.equals("parent_version")) {
               
             }
             if (hint.equals("schema")) {
               proposals.add(new SchemaCompletionProposal(context));
             }
           }
         }
       } catch(Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
       }
     }
     
   }
   
   if (proposals.size() > 0) {
     return proposals.toArray(new ICompletionProposal[0]);
   }
   return null;
  }

  public String getErrorMessage() {
    return null;
  }
}

class SchemaCompletionProposal implements ICompletionProposal, ICompletionProposalExtension5 {

  IQuickAssistInvocationContext context;
  public SchemaCompletionProposal(IQuickAssistInvocationContext context){
    this.context = context;
  }
  
  public void apply(IDocument doc) {

    IDOMModel domModel = (IDOMModel) StructuredModelManager.getModelManager().getExistingModelForRead(doc);
    IStructuredDocument document = domModel.getStructuredDocument();
    Element root = domModel.getDocument().getDocumentElement();

    //now check parent version and groupid against the current project's ones..
    if (root.getNodeName().equals(PomQuickAssistProcessor.PROJECT_NODE)) { //$NON-NLS-1$
      if (root instanceof IndexedRegion) {
        IndexedRegion off = (IndexedRegion) root;

        int offset = off.getStartOffset() + PomQuickAssistProcessor.PROJECT_NODE.length() + 1;
        if (offset <= 0) {
          return;
        }
        InsertEdit edit = new InsertEdit(offset, PomQuickAssistProcessor.XSI_VALUE);
        try {
          edit.apply(doc);
          Display.getDefault().asyncExec(new Runnable() {
            public void run() {
              IEditorPart activeEditor = MvnIndexPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow()
                  .getActivePage().getActiveEditor();
              MvnIndexPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage()
                  .saveEditor(activeEditor, false);
            }
          });
        } catch(Exception e) {
          MavenLogger.log("Unable to insert schema info", e); //$NON-NLS-1$
        }
      }
    }
  }

  public String getAdditionalProposalInfo() {
    return null;
  }

  public IContextInformation getContextInformation() {
    return null;
  }

  public String getDisplayString() {
    return Messages.PomQuickAssistProcessor_name;
  }

  public Image getImage() {
    return WorkbenchPlugin.getDefault().getImageRegistry().get(org.eclipse.ui.internal.SharedImages.IMG_OBJ_ADD);
  }

  public Point getSelection(IDocument arg0) {
    return null;
  }

  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
    // TODO Auto-generated method stub
    return "<html>...<br>&lt;project <b>" + PomQuickAssistProcessor.XSI_VALUE + "</b>&gt;<br>...</html>";
  }
  
}
