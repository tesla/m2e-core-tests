/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.tests.ui.editing;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.ARTIFACT_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.BUILD;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCY;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.GROUP_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.PLUGINS;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.PLUGIN_MANAGEMENT;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.childEquals;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.childMissingOrEqual;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChilds;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.removeIfNoChildElement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import org.eclipse.m2e.core.ui.internal.editing.PomEdits;


@SuppressWarnings("restriction")
public class PomEditsTest {
  private IDOMModel tempModel;

  @Before
  public void setUp() throws Exception {
    tempModel = (IDOMModel) StructuredModelManager.getModelManager().createUnManagedStructuredModelFor(
        "org.eclipse.m2e.core.pomFile");
  }

  @Test
  public void testRemoveChild() {
    assertEquals("<project></project>", removeChild("<project><build></build></project>"));
    assertEquals("<project>a</project>", removeChild("<project>a\nb<build></build></project>"));
    assertEquals("<project>a\nc</project>", removeChild("<project>a\nc\nb<build></build></project>"));
    assertEquals("<project>a</project>", removeChild("<project>a\r\nb<build></build></project>"));
    assertEquals("<project>a\r\nc</project>", removeChild("<project>a\r\nc\r\nb<build></build></project>"));
    assertEquals("<project>a</project>", removeChild("<project>a\rb<build></build></project>"));
    assertEquals("<project>a\rc</project>", removeChild("<project>a\rc\rb<build></build></project>"));
    assertEquals("<project>a\n</project>", removeChild("<project>a\n\rb<build></build></project>"));
    assertEquals("<project>a\rc</project>", removeChild("<project>a\rc\nb<build></build></project>"));
    assertEquals("<project>a\nc</project>", removeChild("<project>a\nc\rb<build></build></project>"));
  }

  private String removeChild(String xml) {
    tempModel.getStructuredDocument().setText(StructuredModelManager.getModelManager(), xml);
    Document doc = tempModel.getDocument();
    Element parent = doc.getDocumentElement();
    Element child = PomEdits.findChild(parent, PomEdits.BUILD);
    PomEdits.removeChild(parent, child);
    return tempModel.getStructuredDocument().getText();
  }

  @Test
  public void testRemoveIfNoChildElement() {
    tempModel.getStructuredDocument().setText(
        StructuredModelManager.getModelManager(),
        "<project>" + "<build>" + "<pluginManagement>" + "<plugins></plugins" + "</pluginManagement>" + "</build>"
            + "</project>");
    Document doc = tempModel.getDocument();
    Element plugins = findChild(findChild(findChild(doc.getDocumentElement(), BUILD), PLUGIN_MANAGEMENT), PLUGINS);
    assertNotNull(plugins);
    removeIfNoChildElement(plugins);
    assertNull(findChild(doc.getDocumentElement(), BUILD));

    tempModel.getStructuredDocument().setText(
        StructuredModelManager.getModelManager(),
        "<project>" + "<build>" + "<pluginManagement>" + "<plugins></plugins" + "</pluginManagement>"
            + "<STOP_ELEMENT/>" + "</build>" + "</project>");
    doc = tempModel.getDocument();
    plugins = findChild(findChild(findChild(doc.getDocumentElement(), BUILD), PLUGIN_MANAGEMENT), PLUGINS);
    assertNotNull(plugins);
    removeIfNoChildElement(plugins);
    Element build = findChild(doc.getDocumentElement(), BUILD);
    assertNotNull(build);
    assertNull(findChild(build, PLUGIN_MANAGEMENT));

  }

  @Test
  public void testMatchers() {
    tempModel.getStructuredDocument().setText(
        StructuredModelManager.getModelManager(),
        "<dependencies>" + "<dependency><groupId>AAA</groupId><artifactId>BBB</artifactId><tag1/></dependency>"
            + "<dependency><groupId>AAAB</groupId><artifactId>BBB</artifactId><tag2/></dependency>"
            + "<dependency><groupId>AAA</groupId><artifactId>BBBB</artifactId><tag3/></dependency>"
            + "<dependency><artifactId>BBB</artifactId><tag4/></dependency>" + "</dependencies>");
    Document doc = tempModel.getDocument();
    Element el = findChild(doc.getDocumentElement(), DEPENDENCY, childEquals(ARTIFACT_ID, "BBBB"));
    assertNotNull(findChild(el, "tag3"));

    el = findChild(doc.getDocumentElement(), DEPENDENCY, childEquals(ARTIFACT_ID, "BBB"));
    assertNotNull(findChild(el, "tag1"));

    el = findChild(doc.getDocumentElement(), DEPENDENCY, childEquals(GROUP_ID, "AAAB"), childEquals(ARTIFACT_ID, "BBB"));
    assertNotNull(findChild(el, "tag2"));

    el = findChild(doc.getDocumentElement(), DEPENDENCY, childMissingOrEqual(GROUP_ID, "CCC"),
        childEquals(ARTIFACT_ID, "BBB"));
    assertNotNull(findChild(el, "tag4"));

    el = findChild(doc.getDocumentElement(), DEPENDENCY, childEquals(GROUP_ID, "AAA"),
        childMissingOrEqual(ARTIFACT_ID, "BBBB"));
    assertNotNull(findChild(el, "tag3"));
  }

  @Test
  public void testFindChild() {
    tempModel.getStructuredDocument().setText(StructuredModelManager.getModelManager(), //
        "<dependencies>" + //
            "<dependency><groupId>AAA</groupId><artifactId>BBB</artifactId><tag1/></dependency>" + //
            "<dependency><groupId>AAAB</groupId><artifactId>BBB</artifactId><tag2/></dependency>" + //
            "<dependency><groupId>AAA</groupId><artifactId>BBBB</artifactId><tag3/></dependency>" + //
            "<dependency><artifactId>BBB</artifactId><tag4/></dependency>" + "</dependencies>");
    Element docElement = tempModel.getDocument().getDocumentElement();

    assertNull("null parent should return null", findChild(null, "dependency"));
    assertNull("missing child should return null", findChild(docElement, "missingElement"));

    Element dep = findChild(docElement, "dependency");
    assertNotNull("Expected node found", dep);
    assertEquals("Node type", "dependency", dep.getLocalName());
    // Should return first match
    assertDependencyChild(null, "AAA", "BBB", "tag1", dep);
  }

  @Test
  public void testFindChildren() {
    tempModel.getStructuredDocument().setText(StructuredModelManager.getModelManager(), //
        "<dependencies>" + //
            "<dependency><groupId>AAA</groupId><artifactId>BBB</artifactId><tag1/></dependency>" + //
            "<dependency><groupId>AAAB</groupId><artifactId>BBB</artifactId><tag2/></dependency>" + //
            "<dependency><groupId>AAA</groupId><artifactId>BBBB</artifactId><tag3/></dependency>" + //
            "<dependency><artifactId>BBB</artifactId><tag4/></dependency>" + "</dependencies>");
    Element docElement = tempModel.getDocument().getDocumentElement();

    assertTrue("null parent should return empty list", findChilds(null, "dependency").isEmpty());
    assertTrue("missing child should return empty list", findChilds(docElement, "missingElement").isEmpty());

    List<Element> dep = findChilds(docElement, "dependency");
    assertEquals("Children found", 4, dep.size());
    for(Element d : dep) {
      assertEquals("Node type", "dependency", d.getLocalName());
    }
    // Should return first match
    assertDependencyChild("Unexpected child", "AAA", "BBB", "tag1", dep.get(0));
    assertDependencyChild("Unexpected child", "AAAB", "BBB", "tag2", dep.get(1));
    assertDependencyChild("Unexpected child", "AAA", "BBBB", "tag3", dep.get(2));
    assertDependencyChild("Unexpected child", null, "BBB", "tag4", dep.get(3));
  }

  private static void assertDependencyChild(String msg, String groupId, String artifactId, String tag, Element dep) {
    if(groupId != null) {
      assertEquals(msg + ":groupId", groupId, PomEdits.getTextValue(findChild(dep, "groupId")));
    } else {
      assertNull(msg + ":groupId", findChild(dep, "groupId"));
    }
    if(artifactId != null) {
      assertEquals(msg + ":artifactId", artifactId, PomEdits.getTextValue(findChild(dep, "artifactId")));
    } else {
      assertNull(msg + ":artifactId", findChild(dep, "artifactId"));
    }
    assertNotNull(msg + ":tag", findChild(dep, tag));
  }
}
