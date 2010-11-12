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

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.jface.text.templates.Template;
import org.eclipse.m2e.editor.xml.PomTemplateContext;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;


public class PomTemplateContextTest extends AbstractMavenProjectTestCase {

  private static final String PREFIX = "org.eclipse.m2e.editor.xml.templates.contextType.";

  private void assertContextTypeId(String contextTypeId, Template[] templates) {
    for(Template template : templates) {
      assertEquals(contextTypeId, template.getContextTypeId());
    }
  }

  private Element addNode(Element parent, String childName) {
    Element child = parent.getOwnerDocument().createElement(childName);
    parent.appendChild(child);
    return child;
  }

  private Element addNode(Element parent, String childName, String childValue) {
    Element child = addNode(parent, childName);
    Text text = parent.getOwnerDocument().createTextNode(childValue);
    child.appendChild(text);
    return parent;
  }

  public void testGetTemplatesPhase() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element phase = doc.createElement("phase");

    PomTemplateContext context = PomTemplateContext.fromNodeName("phase");

    assertNotNull(context);
    assertSame(PomTemplateContext.PHASE, context);

    Template[] templates = context.getTemplates(null, phase, "");
    assertNotNull(templates);
    assertEquals(29, templates.length);
    assertContextTypeId(PREFIX + "phase", templates);
  }

  public void testGetTemplatesScope() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element scope = doc.createElement("scope");

    PomTemplateContext context = PomTemplateContext.fromNodeName("scope");

    assertNotNull(context);
    assertSame(PomTemplateContext.SCOPE, context);

    Template[] templates = context.getTemplates(null, scope, "");
    assertNotNull(templates);
    assertEquals(6, templates.length);
    assertContextTypeId(PREFIX + "scope", templates);
  }

  public void testGetTemplatesPackaging() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element packaging = doc.createElement("packaging");

    PomTemplateContext context = PomTemplateContext.fromNodeName("packaging");

    assertNotNull(context);
    assertSame(PomTemplateContext.PACKAGING, context);

    Template[] templates = context.getTemplates(null, packaging, "");
    assertNotNull(templates);
    assertEquals(10, templates.length);
    assertContextTypeId(PREFIX + "packaging", templates);
  }

  public void testGetTemplatesConfiguration() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element plugin = doc.createElement("plugin");
    addNode(plugin, "artifactId", "maven-clean-plugin");
    addNode(plugin, "version", "2.4");
    Element pluginConfiguration = addNode(plugin, "configuration");
    Element execution = addNode(addNode(plugin, "executions"), "execution");
    Element execConfiguration = addNode(execution, "configuration");

    PomTemplateContext context = PomTemplateContext.fromNodeName("configuration");

    assertNotNull(context);
    assertSame(PomTemplateContext.CONFIGURATION, context);

    Template[] templates = context.getTemplates(null, pluginConfiguration, "");
    assertNotNull(templates);
    assertEquals(10, templates.length);
    assertContextTypeId(PREFIX + "configuration", templates);

    templates = context.getTemplates(null, execConfiguration, "");
    assertNotNull(templates);
    assertEquals(10, templates.length);
    assertContextTypeId(PREFIX + "configuration", templates);
  }

  public void testGetTemplatesGoal() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element plugin = doc.createElement("plugin");
    addNode(plugin, "artifactId", "maven-clean-plugin");
    addNode(plugin, "version", "2.4");
    Element goal = addNode(addNode(addNode(addNode(plugin, "executions"), "execution"), "goals"), "goal");

    PomTemplateContext context = PomTemplateContext.fromNodeName("goal");

    assertNotNull(context);
    assertSame(PomTemplateContext.GOAL, context);

    Template[] templates = context.getTemplates(null, goal, "");
    assertNotNull(templates);
    assertEquals(2, templates.length);
    assertContextTypeId(PREFIX + "goal", templates);
  }

}
