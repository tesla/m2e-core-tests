/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import org.maven.ide.eclipse.core.MavenLogger;

/**
 * MavenMarkerResolution
 *
 * @author dyocum
 */
public class MavenMarkerResolution implements IMarkerResolution {

  /* (non-Javadoc)
   * @see org.eclipse.ui.IMarkerResolution#getLabel()
   */
  public String getLabel() {
    return "Add Schema information to the specified pom.xml";
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
   */
  public void run(final IMarker marker) {
    if(marker.getResource().getType() == IResource.FILE){
      try {
        IDOMModel domModel = (IDOMModel)StructuredModelManager.getModelManager().getModelForEdit((IFile)marker.getResource());
        int offset = ((Integer)marker.getAttribute("offset"));
        IStructuredDocumentRegion regionAtCharacterOffset = domModel.getStructuredDocument().getRegionAtCharacterOffset(offset);
        if(regionAtCharacterOffset != null && regionAtCharacterOffset.getText() != null &&
            regionAtCharacterOffset.getText().lastIndexOf("<project") >=0){
          //in case there are unsaved changes, find the current offset of the <project> node before inserting
          offset = regionAtCharacterOffset.getStartOffset();
          IDE.openEditor(MvnIndexPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage(), (IFile)marker.getResource());
          InsertEdit edit = new InsertEdit(offset+8, PomQuickAssistProcessor.XSI_VALUE);
          try {
            edit.apply(domModel.getStructuredDocument());
            IEditorPart activeEditor = MvnIndexPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            MvnIndexPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().saveEditor(activeEditor, false);
          } catch(Exception e){
            MavenLogger.log("Unable to insert schema info", e);
          }
        } else {
          String msg = "Unable to apply the quick fix. The file may have unsaved changes that invalidate the current quick fix.";
          MessageDialog.openError(MvnIndexPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(), "Error", msg);
        }
      } catch(Exception e) {
        MavenLogger.log("Unable to run quick fix for maven marker", e);
      }
    }
  }

}
