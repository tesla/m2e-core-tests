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
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;


/**
 * Hello fellow tester: everytime this test finds a regression add an 'x' here: everytime you do mindless test update
 * add an 'y' here:
 * 
 * @author mkleint
 */

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
    return child;
  }

  public void testGetTemplatesPhase() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element phase = doc.createElement("phase");

    PomTemplateContext context = PomTemplateContext.fromNodeName("phase");

    assertNotNull(context);
    assertSame(PomTemplateContext.PHASE, context);

    Template[] templates = context.getTemplates(null, null, phase, "");
    assertNotNull(templates);
    assertEquals(29, templates.length);
    assertContextTypeId(PREFIX + "phase", templates);
  }
  
  public void testGetTemplatesDependencyScope() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element depElement = addNode(doc.createElement("dependencies"), "dependency");
    Element scope = addNode(depElement, "scope");

    PomTemplateContext context = PomTemplateContext.fromNodeName("scope");

    assertNotNull(context);
    assertSame(PomTemplateContext.SCOPE, context);

    Template[] templates = context.getTemplates(null, null, scope, "");
    assertNotNull(templates);
    assertEquals(5, templates.length);
    assertContextTypeId(PREFIX + "scope", templates);
  }
  
  public void testGetTemplatesDependencyManagementScope() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element depElement = addNode(addNode(doc.createElement("dependencyManagement"), "dependencies"), "dependency");
    Element scope = addNode(depElement, "scope");

    PomTemplateContext context = PomTemplateContext.fromNodeName("scope");

    assertNotNull(context);
    assertSame(PomTemplateContext.SCOPE, context);

    Template[] templates = context.getTemplates(null, null, scope, "");
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

    Template[] templates = context.getTemplates(null, null, packaging, "");
    assertNotNull(templates);
    assertEquals(7, templates.length);
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

    Template[] templates = context.getTemplates(null, null, pluginConfiguration, "");
    assertNotNull(templates);
    assertEquals(10, templates.length);
    assertContextTypeId(PREFIX + "configuration", templates);

    templates = context.getTemplates(null, null, execConfiguration, "");
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

    Template[] templates = context.getTemplates(null, null, goal, "");
    assertNotNull(templates);
    assertEquals(2, templates.length);
    assertContextTypeId(PREFIX + "goal", templates);
  }

  public void testGetTemplatesArtifactId_WithGroupId() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    String groupId = "testGetTemplatesArtifactId_WithGroupId";
    String artifactId = "testGetTemplatesArtifactId_WithGroupId_artifact";
    Element plugin = doc.createElement("plugin");
    addNode(plugin, "groupId", groupId);
    Element artifactIdElement = addNode(plugin, "artifactId");

    SearchEngineMock searchEngineMock = new SearchEngineMock();
    searchEngineMock.addArtifact(groupId, artifactId, null, null, null);
    searchEngineMock.addArtifact("foo", "bar", null, null, null);
    PomTemplateContext.setSearchEngineForTests(searchEngineMock);
    try {
      PomTemplateContext context = PomTemplateContext.fromNodeName("artifactId");

      assertNotNull(context);
      assertSame(PomTemplateContext.ARTIFACT_ID, context);

      Template[] templates = context.getTemplates(null, null, artifactIdElement, "");
      assertNotNull(templates);
      assertEquals(1, templates.length);
      assertContextTypeId(PREFIX + "artifactId", templates);
      assertEquals(artifactId, templates[0].getName());
    } finally {
      PomTemplateContext.setSearchEngineForTests(null);
    }
  }

  // Missing groupId should default to org.apache.maven.plugins
  public void testGetTemplatesArtifactId_WithoutGroupId() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    // String groupId = "testGetTemplatesArtifactId_WithGroupId";
    String artifactId = "testGetTemplatesArtifactId_WithoutGroupId_artifact";
    Element plugin = doc.createElement("plugin");
    addNode(plugin, "groupId");
    Element artifactIdElement = addNode(plugin, "artifactId");

    SearchEngineMock searchEngineMock = new SearchEngineMock();
    searchEngineMock.addArtifact("org.apache.maven.plugins", artifactId, null, null, null);
    searchEngineMock.addArtifact("foo", "bar", null, null, null);
    PomTemplateContext.setSearchEngineForTests(searchEngineMock);
    try {
      PomTemplateContext context = PomTemplateContext.fromNodeName("artifactId");

      assertNotNull(context);
      assertSame(PomTemplateContext.ARTIFACT_ID, context);

      Template[] templates = context.getTemplates(null, null, artifactIdElement, "");
      assertNotNull(templates);
      assertEquals(1, templates.length);
      assertContextTypeId(PREFIX + "artifactId", templates);
      assertEquals(artifactId, templates[0].getName());
    } finally {
      PomTemplateContext.setSearchEngineForTests(null);
    }
  }

  public void testGetTemplatesVersion_WithGroupId() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    String groupId = "testGetTemplatesVersion_WithGroupId";
    String artifactId = "testGetTemplatesVersion_WithGroupId_artifact";
    String version = "1.2.3";
    Element plugin = doc.createElement("plugin");
    addNode(plugin, "groupId", groupId);
    addNode(plugin, "artifactId", artifactId);
    Element versionElement = addNode(plugin, "version");

    SearchEngineMock searchEngineMock = new SearchEngineMock();
    searchEngineMock.addArtifact(groupId, artifactId, version, null, null);
    searchEngineMock.addArtifact("foo", "bar", "1.1.1", null, null);
    PomTemplateContext.setSearchEngineForTests(searchEngineMock);
    try {
      PomTemplateContext context = PomTemplateContext.fromNodeName("version");

      assertNotNull(context);
      assertSame(PomTemplateContext.VERSION, context);

      Template[] templates = context.getTemplates(null, null, versionElement, "");
      assertNotNull(templates);
      assertEquals(1, templates.length);
      assertContextTypeId(PREFIX + "version", templates);
      assertEquals(version, templates[0].getName());
    } finally {
      PomTemplateContext.setSearchEngineForTests(null);
    }
  }

  // Missing groupId should default to org.apache.maven.plugins
  public void testGetTemplatesVersion_WithoutGroupId() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    String artifactId = "testGetTemplatesVersion_WithoutGroupId_artifact";
    String version = "1.2.3";
    Element plugin = doc.createElement("plugin");
    addNode(plugin, "groupId");
    addNode(plugin, "artifactId", artifactId);
    Element versionElement = addNode(plugin, "version");

    SearchEngineMock searchEngineMock = new SearchEngineMock();
    searchEngineMock.addArtifact("org.apache.maven.plugins", artifactId, version, null, null);
    searchEngineMock.addArtifact("foo", "bar", "1.1.1", null, null);
    PomTemplateContext.setSearchEngineForTests(searchEngineMock);
    try {
      PomTemplateContext context = PomTemplateContext.fromNodeName("version");

      assertNotNull(context);
      assertSame(PomTemplateContext.VERSION, context);

      Template[] templates = context.getTemplates(null, null, versionElement, "");
      assertNotNull(templates);
      assertEquals(1, templates.length);
      assertContextTypeId(PREFIX + "version", templates);
      assertEquals(version, templates[0].getName());
    } finally {
      PomTemplateContext.setSearchEngineForTests(null);
    }
  }
  
  public void test439251_GetTemplatesConfigurationFromMojoWithEmptyParameters() throws Exception {
	    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
	    Element plugin = doc.createElement("plugin");
	    addNode(plugin, "groupId", "org.eclipse.dummy");
	    addNode(plugin, "artifactId", "emptymojo");
	    addNode(plugin, "version", "1.0.0");
	    Element pluginConfiguration = addNode(plugin, "configuration");

	    PomTemplateContext context = PomTemplateContext.fromNodeName("configuration");

	    assertNotNull(context);
	    assertSame(PomTemplateContext.CONFIGURATION, context);

	    Template[] templates = context.getTemplates(null, null, pluginConfiguration, "");
	    assertNotNull(templates);
	    assertEquals(0, templates.length);
	  }
}
