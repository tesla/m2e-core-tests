/*******************************************************************************
 * Copyright (c) 2012 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.editor.pom.ElementValueProvider;
import org.eclipse.m2e.editor.pom.XmlUtils;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
import org.eclipse.wst.xml.core.internal.preferences.XMLCorePreferenceNames;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.junit.Test;
import org.w3c.dom.Element;


@SuppressWarnings("restriction")
public class PomEditsTest extends AbstractMavenProjectTestCase {
	 @Test
  public void test371438_insertAt_insideEmptyElement() throws Exception {
    IProject project = importProject("projects/insertat_insideemptyelement/pom.xml");

    IDOMModel model = (IDOMModel) StructuredModelManager.getModelManager().getModelForRead(project.getFile("pom.xml"));
    try {
      IDOMDocument doc = model.getDocument();

      Element parent = XmlUtils.findChild(doc.getDocumentElement(), "dependencies");
      Element element = doc.createElement("dependency");

      int offset = ((IndexedRegion) parent).getStartOffset() + parent.getNodeName().length() + 2;
      Element result = PomEdits.insertAt(element, offset);

      assertEquals(parent, result.getParentNode());
    } finally {
      model.releaseFromRead();
    }
  }
	 @Test
  public void test467590_emptyPom() throws Exception {
    
    // default config for xml formatter
    XMLCorePlugin.getDefault().getPluginPreferences().setValue(XMLCorePreferenceNames.INDENTATION_CHAR, XMLCorePreferenceNames.SPACE);
    XMLCorePlugin.getDefault().getPluginPreferences().setValue(XMLCorePreferenceNames.INDENTATION_SIZE, 4);
    
    IProject project = importProject("projects/467590_emptyPom/pom.xml");
    
    PomEdits.performOnDOMDocument(new PomEdits.OperationTuple(project.getFile("test1/pom.xml"), (Operation) document -> {
        ElementValueProvider provider = new ElementValueProvider(PomEdits.GROUP_ID);
        Element el = provider.get(document);
        PomEdits.setText(el, "test1");
      }));
    
    assertContentsEqual(project.getFile("test1/result_pom.xml"), project.getFile("test1/pom.xml"));
    
    PomEdits.performOnDOMDocument(new PomEdits.OperationTuple(project.getFile("test2/pom.xml"), (Operation) document -> {
        ElementValueProvider provider = new ElementValueProvider(PomEdits.GROUP_ID);
        Element el = provider.get(document);
        PomEdits.setText(el, "test2");
      }));
    
    assertContentsEqual(project.getFile("test2/result_pom.xml"), project.getFile("test2/pom.xml"));
  }
  
  private void assertContentsEqual(IFile expected, IFile actual) throws Exception {
    String expectedContent = getContent(expected);
    String actualContent = getContent(actual);
    assertEquals(expectedContent, actualContent);
  }

  private String getContent(IFile file) throws Exception{
    IStructuredModel model = StructuredModelManager.getModelManager().getModelForRead(file);
    try {
      return model.getStructuredDocument().get();
    } finally {
      model.releaseFromRead();
    }
  }

}
