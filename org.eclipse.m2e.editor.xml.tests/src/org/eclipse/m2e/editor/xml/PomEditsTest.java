/*******************************************************************************
 * Copyright (c) 2012 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml;

import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.editor.xml.internal.XmlUtils;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Element;


@SuppressWarnings("restriction")
public class PomEditsTest extends AbstractMavenProjectTestCase {
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
}
