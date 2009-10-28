/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml;

import java.util.Iterator;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;

import org.maven.ide.eclipse.core.MavenLogger;

public class PomQuickAssistProcessor implements IQuickAssistProcessor {

  public static final String PROJECT_NODE = "project";
  public static final String XSI_VALUE = " xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"+
  "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\"";
  public static final String NO_SCHEMA_ERR = "There is no schema defined for this pom.xml. Code completion will not work without a schema defined.";
  
  public boolean canAssist(IQuickAssistInvocationContext arg0) {
    return true;
  }

  public boolean canFix(Annotation arg0) {
    return false;
  }
  
  public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext context) {
    IAnnotationModel annotationModel = context.getSourceViewer().getAnnotationModel();
    // TODO Auto-generated method stub
   Iterator annotationIterator = context.getSourceViewer().getAnnotationModel().getAnnotationIterator();
   while(annotationIterator.hasNext()){
     Annotation annotation = (Annotation)annotationIterator.next();
     if(NO_SCHEMA_ERR.equals(annotation.getText())){
       IDOMNode node = (IDOMNode) ContentAssistUtils.getNodeAt(context.getSourceViewer(), context.getOffset());
       if(node != null && PROJECT_NODE.equals(node.getNodeName())){
         return new ICompletionProposal[]{new SchemaCompletionProposal(context)};
       }
     }
   }
   return null;
  }

  public String getErrorMessage() {
    return null;
  }
}

class SchemaCompletionProposal implements ICompletionProposal{

  IQuickAssistInvocationContext context;
  public SchemaCompletionProposal(IQuickAssistInvocationContext context){
    this.context = context;
  }
  
  public void apply(IDocument doc) {

    IStructuredDocumentRegion structuredDocumentRegion = ContentAssistUtils.getStructuredDocumentRegion(context.getSourceViewer(), context.getOffset());
    int offset = structuredDocumentRegion.getStartOffset()+PomQuickAssistProcessor.PROJECT_NODE.length()+1;
    if(offset <= 0){
      return;
    }
    InsertEdit edit = new InsertEdit(offset, PomQuickAssistProcessor.XSI_VALUE);
    try {
      edit.apply(doc);
      Display.getDefault().asyncExec(new Runnable(){
        public void run(){
          IEditorPart activeEditor = MvnIndexPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
          MvnIndexPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().saveEditor(activeEditor, false);
        }
      });
    } catch(Exception e){
      MavenLogger.log("Unable to insert schema info", e);
    }
  }

  public String getAdditionalProposalInfo() {
    return null;
  }

  public IContextInformation getContextInformation() {
    return null;
  }

  public String getDisplayString() {
    return "Add maven schema definiton to pom.xml";
  }

  public Image getImage() {
    return null;
  }

  public Point getSelection(IDocument arg0) {
    return null;
  }
  
}
