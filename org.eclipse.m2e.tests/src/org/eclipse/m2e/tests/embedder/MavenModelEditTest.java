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

package org.eclipse.m2e.tests.embedder;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.Parent;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.util.PomResourceImpl;


public class MavenModelEditTest extends TestCase {

  private static final String TEST_PROJECT_NAME = "editor-tests";
  
  private IProject project;

  protected void setUp() throws Exception {
    super.setUp();
    
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();
    
    project = root.getProject(TEST_PROJECT_NAME);
    if(!project.exists()) {
      project.create(new NullProgressMonitor());
    }
    if(!project.isOpen()) {
      project.open(new NullProgressMonitor());
    }
  }
  
  public void testAttributeRoundtrip() throws Exception {
    PomResourceImpl resource = loadModel("attr.xml");
    assertEquals(loadFile("attr.xml"), MavenModelUtil.toString(resource));
    resource.unload();
  }

  public void testAttributeRemove() throws Exception {
    PomResourceImpl resource = loadModel("attr.xml");
    
    Model model = resource.getModel();
    // model.eUnset(model.eClass().getEStructuralFeature(PomPackage.MODEL__VERSION));
    model.setVersion(null);
    
    assertEquals(loadFile("attr_remove.xml"), MavenModelUtil.toString(resource));
    resource.unload();
  }

  public void testAttributeAdd() throws Exception {
    PomResourceImpl resource = loadModel("attr.xml");
    Model model = resource.getModel();
    model.setDescription("description");
    
    assertEquals(loadFile("attr_add.xml"), MavenModelUtil.toString(resource));
    resource.unload();
  }

  public void testAttributeChange() throws Exception {
    PomResourceImpl resource = loadModel("attr.xml");
    Model model = resource.getModel();
    model.setArtifactId("changed-artifactId");
    assertEquals(loadFile("attr_change.xml"), MavenModelUtil.toString(resource));
    resource.unload();
  }

  public void testOneRoundtrip() throws Exception {
    PomResourceImpl resource = loadModel("one.xml");
    assertEquals(loadFile("one.xml"), MavenModelUtil.toString(resource));
    resource.unload();
  }

  public void testOneRemove() throws Exception {
    PomResourceImpl resource = loadModel("one.xml");
    Model model = resource.getModel();
    
    model.setParent(null);
    
    assertEquals(loadFile("one_remove.xml"), MavenModelUtil.toString(resource));
    resource.unload();
  }

  public void testOneReplace() throws Exception {
    PomResourceImpl resource = loadModel("one.xml");
    Model model = resource.getModel();

    Parent parent = model.getParent();
    parent.setArtifactId("tttt");
    
    assertEquals(loadFile("one_replace.xml"), MavenModelUtil.toString(resource));
    resource.unload();
  }

  public void testManyRoundtrip() throws Exception {
    PomResourceImpl resource = loadModel("many.xml");
    assertEquals(loadFile("many.xml"), MavenModelUtil.toString(resource));
    resource.unload();
  }

  public void testManyRemove() throws Exception {
    PomResourceImpl resource = loadModel("many.xml");
    Model model = resource.getModel();

    // model.getProject().getDependencies().removeDependency(1);
    model.getDependencies().remove(1);

    assertEquals(loadFile("many_remove.xml"), MavenModelUtil.toString(resource));
    resource.unload();
  }

  public void testManyChange() throws Exception {
    PomResourceImpl resource = loadModel("many.xml");
    Model model = resource.getModel();

    // Dependency dependency = model.getDependencies().getDependencyArray(0);
    Dependency dependency = model.getDependencies().get(0);
    dependency.setArtifactId("changed-maven-lifecycle");
    
    assertEquals(loadFile("many_change.xml"), MavenModelUtil.toString(resource));
    resource.unload();
  }

  public void testManyAdd() throws Exception {
    PomResourceImpl resource = loadModel("many.xml");
    Model model = resource.getModel();

    Dependency dependency = PomFactory.eINSTANCE.createDependency();
    dependency.setGroupId("added-groupId");
    dependency.setArtifactId("added-artifactId");
    
    EList<Dependency> dependencies = model.getDependencies();
    dependencies.add(dependency);

    assertEquals(loadFile("many_add.xml"), MavenModelUtil.toString(resource));
    resource.unload();
  }

  public void testManybuildinRoundtrip() throws Exception {
    PomResourceImpl resource = loadModel("manybuiltin.xml");
    assertEquals(loadFile("manybuiltin.xml"), MavenModelUtil.toString(resource));
    resource.unload();
  }

  public void testManybuildinRemove() throws Exception {
    PomResourceImpl resource = loadModel("manybuiltin.xml");
    Model model = resource.getModel();
    model.getModules().remove(0);
    assertEquals(loadFile("manybuiltin_remove.xml"), MavenModelUtil.toString(resource));
    resource.unload();
  }

  public void testManybuildincommentRemove() throws Exception {
    PomResourceImpl resource = loadModel("manybuiltincomment.xml");
    Model model = resource.getModel();
    model.getModules().remove(1);
    assertEquals(loadFile("manybuiltincomment_remove.xml"), MavenModelUtil.toString(resource));
    resource.unload();
  }

// XXX restore properties tests  
//  public void testPropertiesRemove() throws Exception {
//    PomResourceImpl resource = loadModel("properties.xml");
//    Model model = resource.getModel();
//    
//    Properties properties = model.getProperties();
//    
//    XmlCursor cursor = properties.newCursor();
//    // model.getProperties().remove("wagonVersion");
//    boolean found = cursor.toChild(URI, "wagonVersion");
//    assertTrue(found);
//    
//    cursor.removeXml();
//    
//    assertEquals(loadFile("properties_remove.xml"), MavenModelUtil.toString(resource));
//  }
//
//  public void testPropertiesNew() throws Exception {
//    PomResourceImpl resource = loadModel("attr.xml");
//    Model model = resource.getModel();
//    
//    Properties properties = model.getProperties();
//    
//    if(properties==null) {
//      properties = model.addNewProperties();
//
//      XmlCursor newCursor = properties.newCursor();
//      newCursor.insertChars("  ");
//      newCursor.toEndToken();
//      newCursor.insertChars("\n  ");
//      newCursor.toNextToken();
//      newCursor.insertChars("\n");
//      newCursor.dispose();
//    }
//    
//    XmlCursor cursor = properties.newCursor();
//    cursor.toFirstContentToken();
//    cursor.insertChars("\n    ");
//    cursor.insertElementWithText("plexusVersion", URI, "1.0-alpha-30");
//    cursor.insertChars("\n    ");
//    cursor.insertElementWithText("wagonVersion", URI, "1.0-beta-2");
//    cursor.dispose();
//    
//    assertEquals(loadFile("properties_new.xml"), MavenModelUtil.toString(resource));
//  }

  public void testXpp3domRoundtrip() throws Exception {
    PomResourceImpl resource = loadModel("xpp3dom.xml");
    assertEquals(loadFile("xpp3dom.xml"), MavenModelUtil.toString(resource));
    resource.unload();
  }

  public void testEncoding() throws Exception {
    PomResourceImpl resource = loadModel("encoding.xml");
    Model model = resource.getModel();

    String description = model.getDescription();
    String expected = "\u043f\u043e\u002d\u0440\u0443\u0441\u0441\u043a\u0438";
    assertEquals(expected, description);
    assertEquals(loadFile("encoding.xml"), MavenModelUtil.toString(resource));
    resource.unload();
  }

  private PomResourceImpl loadModel(String name) throws Exception {
    return MavenModelUtil.createResource(project, name, loadFile(name));
  }

  private String loadFile(String name) throws IOException {
    InputStream is = null;
    try {
      is = MavenModelEditTest.class.getResourceAsStream(name);
      return new String(IOUtil.toByteArray(is), "UTF-8").replaceAll("\r\n", "\n");
    } finally {
      is.close();
    }
  }

//  private String toString(ProjectDocument document) throws IOException {
//    XmlOptions options = new XmlOptions();
//    options.setSaveImplicitNamespaces(Collections.singletonMap("", URI));
//    options.remove(XmlOptions.SAVE_NO_XML_DECL);
//    
//    ByteArrayOutputStream os = new ByteArrayOutputStream();
//    document.save(os, options);
//  
//    return new String(os.toByteArray(), "UTF-8").replaceAll("\r\n", "\n");
//  }
  
}
