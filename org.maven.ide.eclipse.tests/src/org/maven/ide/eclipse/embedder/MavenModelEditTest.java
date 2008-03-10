/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import junit.framework.TestCase;

import org.apache.maven.pom.x400.Dependency;
import org.apache.maven.pom.x400.Parent;
import org.apache.maven.pom.x400.ProjectDocument;
import org.apache.maven.pom.x400.Model.Dependencies;
import org.apache.maven.pom.x400.Model.Properties;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlOptions;
import org.codehaus.plexus.util.IOUtil;


public class MavenModelEditTest extends TestCase {

  private static final String URI = ProjectDocument.type.getDocumentElementName().getNamespaceURI();

  public void testA() throws Exception {
    assertEquals("\n", "\n\r".replaceAll("\n\r", "\n"));
  }
  
  public void testAttributeRoundtrip() throws Exception {
    ProjectDocument model = loadModel("attr.xml");
    assertEquals(loadFile("attr.xml"), toString(model));
  }

  public void testAttributeRemove() throws Exception {
    ProjectDocument model = loadModel("attr.xml");
    
    model.getProject().unsetVersion();
    
    assertEquals(loadFile("attr_remove.xml"), toString(model));
  }

  public void testAttributeAdd() throws Exception {
    ProjectDocument model = loadModel("attr.xml");
    model.getProject().setDescription("description");
    XmlCursor cursor = model.getProject().newCursor();
    
    cursor.toChild(URI, "description");
    cursor.insertChars("  ");
    cursor.toEndToken();
    cursor.toNextToken();
    cursor.insertChars("\n");
    cursor.dispose();
    
    assertEquals(loadFile("attr_add.xml"), toString(model));
  }

  public void testAttributeChange() throws Exception {
    ProjectDocument model = loadModel("attr.xml");
    model.getProject().setArtifactId("changed-artifactId");
    assertEquals(loadFile("attr_change.xml"), toString(model));
  }

  public void testOneRoundtrip() throws Exception {
    ProjectDocument model = loadModel("one.xml");
    assertEquals(loadFile("one.xml"), toString(model));
  }

  public void testOneRemove() throws Exception {
    ProjectDocument model = loadModel("one.xml");
    model.getProject().unsetParent();
    assertEquals(loadFile("one_remove.xml"), toString(model));
  }

  public void testOneReplace() throws Exception {
    ProjectDocument model = loadModel("one.xml");

    Parent parent = model.getProject().getParent();
    parent.setArtifactId("tttt");
    
    assertEquals(loadFile("one_replace.xml"), toString(model));
  }

  public void testManyRoundtrip() throws Exception {
    ProjectDocument model = loadModel("many.xml");
    assertEquals(loadFile("many.xml"), toString(model));
  }

  public void testManyRemove() throws Exception {
    ProjectDocument model = loadModel("many.xml");

    model.getProject().getDependencies().removeDependency(1);

    assertEquals(loadFile("many_remove.xml"), toString(model));
  }

  public void testManyChange() throws Exception {
    ProjectDocument model = loadModel("many.xml");
    Dependency dependency = model.getProject().getDependencies().getDependencyArray(0);
    dependency.setArtifactId("changed-maven-lifecycle");
    assertEquals(loadFile("many_change.xml"), toString(model));
  }

  public void testManyAdd() throws Exception {
    ProjectDocument model = loadModel("many.xml");

    Dependencies dependencies = model.getProject().getDependencies();

    Dependency dependency = dependencies.addNewDependency();
    
//    dependency.setGroupId("added-groupId");
//    dependency.setArtifactId("added-artifactId");
    
    XmlCursor cursor = dependency.newCursor();
    cursor.insertChars("  ");
    cursor.toFirstContentToken();
    
    cursor.insertChars("\n      ");
    cursor.insertElementWithText("groupId", URI, "added-groupId");
    cursor.insertChars("\n");

    cursor.insertChars("      ");
    cursor.insertElementWithText("artifactId", URI, "added-artifactId");
    cursor.insertChars("\n    ");
    cursor.toNextToken();
    cursor.insertChars("\n  ");

    cursor.dispose();
    
    assertEquals(loadFile("many_add.xml"), toString(model));
  }

  public void testManybuildinRoundtrip() throws Exception {
    ProjectDocument model = loadModel("manybuiltin.xml");
    assertEquals(loadFile("manybuiltin.xml"), toString(model));
  }

  public void testManybuildinRemove() throws Exception {
    ProjectDocument model = loadModel("manybuiltin.xml");
    model.getProject().getModules().removeModule(0);
    assertEquals(loadFile("manybuiltin_remove.xml"), toString(model));
  }

  public void testManybuildincommentRemove() throws Exception {
    ProjectDocument model = loadModel("manybuiltincomment.xml");
    model.getProject().getModules().removeModule(1);
    assertEquals(loadFile("manybuiltincomment_remove.xml"), toString(model));
  }

  public void testPropertiesRemove() throws Exception {
    ProjectDocument model = loadModel("properties.xml");
    
    
    Properties properties = model.getProject().getProperties();
    
    XmlCursor cursor = properties.newCursor();
    // model.getProperties().remove("wagonVersion");
    boolean found = cursor.toChild(URI, "wagonVersion");
    assertTrue(found);
    
    cursor.removeXml();
    
    assertEquals(loadFile("properties_remove.xml"), toString(model));
  }

  public void testPropertiesNew() throws Exception {
    ProjectDocument model = loadModel("attr.xml");
    
    Properties properties = model.getProject().getProperties();
    
    if(properties==null) {
      properties = model.getProject().addNewProperties();

      XmlCursor newCursor = properties.newCursor();
      newCursor.insertChars("  ");
      newCursor.toEndToken();
      newCursor.insertChars("\n  ");
      newCursor.toNextToken();
      newCursor.insertChars("\n");
      newCursor.dispose();
    }
    
    XmlCursor cursor = properties.newCursor();
    cursor.toFirstContentToken();
    cursor.insertChars("\n    ");
    cursor.insertElementWithText("plexusVersion", URI, "1.0-alpha-30");
    cursor.insertChars("\n    ");
    cursor.insertElementWithText("wagonVersion", URI, "1.0-beta-2");
    cursor.dispose();
    
    assertEquals(loadFile("properties_new.xml"), toString(model));
  }

  public void testXpp3domRoundtrip() throws Exception {
    ProjectDocument model = loadModel("xpp3dom.xml");
    assertEquals(loadFile("xpp3dom.xml"), toString(model));
  }

  public void testEncoding() throws Exception {
    ProjectDocument model = loadModel("encoding.xml");
    String description = model.getProject().getDescription();
    String expected = "\u043f\u043e\u002d\u0440\u0443\u0441\u0441\u043a\u0438";
    assertEquals(expected, description);
    assertEquals(loadFile("encoding.xml"), toString(model));
  }

  private ProjectDocument loadModel(String name) throws Exception {
    XmlOptions options = new XmlOptions();
    options.setLoadSubstituteNamespaces(Collections.singletonMap("", URI));
    options.remove(XmlOptions.LOAD_STRIP_PROCINSTS);
    options.remove(XmlOptions.LOAD_STRIP_WHITESPACE);
    options.remove(XmlOptions.LOAD_STRIP_COMMENTS);
    
    InputStream is = MavenModelEditTest.class.getResourceAsStream(name);
    try {
      return ProjectDocument.Factory.parse(is, options);
    } finally {
      is.close();
    }
  }

  private String loadFile(String name) throws IOException {
    InputStream is = MavenModelEditTest.class.getResourceAsStream(name);
  
    return new String(IOUtil.toByteArray(is), "UTF-8").replaceAll("\r\n", "\n");
  }

  private String toString(ProjectDocument document) throws IOException {
    XmlOptions options = new XmlOptions();
    options.setSaveImplicitNamespaces(Collections.singletonMap("", URI));
    options.remove(XmlOptions.SAVE_NO_XML_DECL);
    
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    document.save(os, options);
  
    return new String(os.toByteArray(), "UTF-8").replaceAll("\r\n", "\n");
  }
  
}
