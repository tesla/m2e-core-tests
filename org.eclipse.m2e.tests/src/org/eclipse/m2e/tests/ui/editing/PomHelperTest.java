/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
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

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import junit.framework.TestCase;

import org.apache.maven.model.Dependency;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.editing.PomHelper;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Element;


@SuppressWarnings("restriction")
public class PomHelperTest extends TestCase {

  private IDOMModel tempModel;

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  @Override
  protected void setUp() throws Exception {
    tempModel = (IDOMModel) StructuredModelManager.getModelManager().createUnManagedStructuredModelFor(
        "org.eclipse.m2e.core.pomFile");
  }

  public void testFindDependencies() {
    tempModel.getStructuredDocument().setText(StructuredModelManager.getModelManager(), //
        "<project><dependencies>" + //
            "<dependency><groupId>AAA</groupId><artifactId>BBB</artifactId><version>1.0</version></dependency>" + //
            "<dependency><groupId>AAAB</groupId><artifactId>BBB</artifactId><version>1.0</version></dependency>" + //
            "<dependency><groupId>AAA</groupId><artifactId>BBBB</artifactId><version>1.0</version></dependency>" + //
            "</dependencies></project>");
    assertNotNull(PomHelper.findDependencies(tempModel.getDocument().getDocumentElement()));
  }

  public void testFindDependenciesMissing() {
    tempModel.getStructuredDocument().setText(StructuredModelManager.getModelManager(), //
        "<project><adependencies>" + //
            "<dependency><groupId>AAA</groupId><artifactId>BBB</artifactId><version>1.0</version></dependency>" + //
            "<dependency><groupId>AAAB</groupId><artifactId>BBB</artifactId><version>1.0</version></dependency>" + //
            "<dependency><groupId>AAA</groupId><artifactId>BBBB</artifactId><version>1.0</version></dependency>" + //
            "</adependencies></project>");
    assertTrue(PomHelper.findDependencies(tempModel.getDocument().getDocumentElement()).isEmpty());
  }

  public void testFindDependency() {
    tempModel.getStructuredDocument().setText(StructuredModelManager.getModelManager(), //
        "<project><dependencies>" + //
            "<dependency><groupId>AAA</groupId><artifactId>BBB</artifactId><version>1.0</version></dependency>" + //
            "<dependency><groupId>AAAB</groupId><artifactId>BBB</artifactId><version>1.0</version></dependency>" + //
            "<dependency><groupId>AAA</groupId><artifactId>BBBB</artifactId><version>1.0</version></dependency>" + //
            "</dependencies></project>");

    Dependency dependency = new Dependency();
    dependency.setArtifactId("BBB");
    dependency.setGroupId("AAA");
    dependency.setVersion("1.0");

    Element depElement = PomHelper.findDependency(tempModel.getDocument(), dependency);
    assertDependencyChild("Dependency", "AAA", "BBB", "1.0", depElement);
  }

  public void testCreateDependency() {
    tempModel.getStructuredDocument().setText(StructuredModelManager.getModelManager(), //
        "<project><dependencies>" + //
            "<dependency><groupId>AAA</groupId><artifactId>BBB</artifactId><version>1.0</version></dependency>" + //
            "<dependency><groupId>AAAB</groupId><artifactId>BBB</artifactId><version>1.0</version></dependency>" + //
            "<dependency><groupId>AAA</groupId><artifactId>BBBB</artifactId><version>1.0</version></dependency>" + //
            "</dependencies></project>");

    Dependency dep = new Dependency();
    dep.setArtifactId("BB2B");
    dep.setGroupId("AAA");
    dep.setVersion("2.0");

    PomHelper.createDependency(findChild(tempModel.getDocument().getDocumentElement(), "dependencies"),
        dep.getGroupId(), dep.getArtifactId(), dep.getVersion());

    assertNotNull(PomHelper.findDependency(tempModel.getDocument(), dep));
  }

  public void testCreateDependencyInEmptyList() {
    tempModel.getStructuredDocument().setText(StructuredModelManager.getModelManager(),
        "<project><dependencies/></project>");

    Dependency dep = new Dependency();
    dep.setArtifactId("BB2B");
    dep.setGroupId("AAA");
    dep.setVersion("2.0");

    PomHelper.createDependency(findChild(tempModel.getDocument().getDocumentElement(), "dependencies"),
        dep.getGroupId(), dep.getArtifactId(), dep.getVersion());

    assertNotNull(PomHelper.findDependency(tempModel.getDocument(), dep));
  }

  public void testCreatePlugin() {
    tempModel.getStructuredDocument().setText(StructuredModelManager.getModelManager(), //
        "<project><plugins>" + //
            "<plugin><groupId>AAA</groupId><artifactId>BBB</artifactId><version>1.0</version></plugin>" + //
            "<plugin><groupId>AAAB</groupId><artifactId>BBB</artifactId><version>1.0</version></plugin>" + //
            "<plugin><groupId>AAA</groupId><artifactId>BBBB</artifactId><version>1.0</version></plugin>" + //
            "</plugins></project>");

    ArtifactKey dep = new ArtifactKey("AAA", "BB2B", "2.0", null);

    PomHelper.createPlugin(findChild(tempModel.getDocument().getDocumentElement(), "plugins"), dep.getGroupId(),
        dep.getArtifactId(), dep.getVersion());

    assertNotNull(findPlugin(findChild(tempModel.getDocument().getDocumentElement(), "plugins"), dep));
  }

  public void testCreatePluginInEmptyList() {
    tempModel.getStructuredDocument()
        .setText(StructuredModelManager.getModelManager(), "<project><plugins/></project>");

    ArtifactKey dep = new ArtifactKey("AAA", "BB2B", "2.0", null);

    PomHelper.createPlugin(findChild(tempModel.getDocument().getDocumentElement(), "plugins"), dep.getGroupId(),
        dep.getArtifactId(), dep.getVersion());

    assertNotNull(findPlugin(findChild(tempModel.getDocument().getDocumentElement(), "plugins"), dep));
  }

  static void assertDependencyChild(String msg, String groupId, String artifactId, String version, Element dep) {
    assertEquals(msg + ": element name", "dependency", dep.getLocalName());
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
    if(version != null) {
      assertEquals(msg + ":version", version, PomEdits.getTextValue(findChild(dep, "version")));
    } else {
      assertNull(msg + ":version", findChild(dep, "version"));
    }
  }

  private static Element findPlugin(Element parent, ArtifactKey key) {
    return findChild(parent, "plugin", PomEdits.childEquals("groupId", key.getGroupId()),
        PomEdits.childEquals("artifactId", key.getArtifactId()), PomEdits.childEquals("version", key.getVersion()));
  }
}
