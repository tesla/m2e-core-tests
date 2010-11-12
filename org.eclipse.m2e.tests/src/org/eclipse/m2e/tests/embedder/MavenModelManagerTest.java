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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.model.edit.pom.util.PomResourceImpl;

/**
 * @author Eugene Kuleshov
 */
public class MavenModelManagerTest extends TestCase {

  private static final String TEST_PROJECT_NAME = "editor-tests";
  
  private IProject project;

  private PomResourceImpl resource = null;

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
  
  protected void tearDown() throws Exception {
    if (resource != null) {
      resource.unload();
    }
    super.tearDown();
  }
  
  public void testAddingNewModule() throws Exception {
    String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + // 
    		"<project>\n" + //
    		"  <modelVersion>4.0.0</modelVersion>\n" + // 
    		"  <groupId>test-groupId</groupId>\n" + //
    		"  <artifactId>test-artifactId</artifactId>\n" + // 
    		"  <version>1.0-SNAPSHOT</version>\n" + //
    		"  <packaging>jar</packaging>\n" + //
    		"  <modules>\n" + //
    		"    <module>maven-core</module>\n" + // 
    		"  </modules>\n" + //
    		"</project>";
    
    resource = MavenModelUtil.createResource(project, "testAddingNewModule.xml", pom);
    new MavenModelManager.ModuleAdder("test").update(resource.getModel());
    
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + // 
    		"<project>\n" + //
    		"  <modelVersion>4.0.0</modelVersion>\n" + // 
    		"  <groupId>test-groupId</groupId>\n" + //
    		"  <artifactId>test-artifactId</artifactId>\n" + // 
    		"  <version>1.0-SNAPSHOT</version>\n" + //
    		"  <packaging>jar</packaging>\n" + //
    		"  <modules>\n" + //
    		"    <module>maven-core</module>\n" + // 
    		"    <module>test</module>\n" + //
    		"  </modules>\n" + //
    		"</project>", MavenModelUtil.toString(resource));
  }

  public void testAddingNewModule2() throws Exception {
    String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + // 
    "<project>\n" + //
    "  <modelVersion>4.0.0</modelVersion>\n" + // 
    "  <groupId>test-groupId</groupId>\n" + //
    "  <artifactId>test-artifactId</artifactId>\n" + // 
    "  <version>1.0-SNAPSHOT</version>\n" + //
    "  <packaging>jar</packaging>\n" + //
    "  <modules>\n" + //
    "  </modules>\n" + //
    "</project>";
    
    resource = MavenModelUtil.createResource(project, "testAddingNewModule2.xml", pom);
    new MavenModelManager.ModuleAdder("test").update(resource.getModel());
    
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + // 
        "<project>\n" + //
        "  <modelVersion>4.0.0</modelVersion>\n" + // 
        "  <groupId>test-groupId</groupId>\n" + //
        "  <artifactId>test-artifactId</artifactId>\n" + // 
        "  <version>1.0-SNAPSHOT</version>\n" + //
        "  <packaging>jar</packaging>\n" + //
        "  <modules>\n" + //
        "    <module>test</module>\n" + //
        "  </modules>\n" + //
        "</project>", MavenModelUtil.toString(resource));
  }
  
  public void testAddingModules() throws Exception {
    String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + // 
        "<project>\n" + //
        "  <modelVersion>4.0.0</modelVersion>\n" + // 
        "  <groupId>test-groupId</groupId>\n" + //
        "  <artifactId>test-artifactId</artifactId>\n" + // 
        "  <version>1.0-SNAPSHOT</version>\n" + //
        "  <packaging>jar</packaging>\n" + //
        "</project>";

    resource = MavenModelUtil.createResource(project, "testAddingModules.xml", pom);
    new MavenModelManager.ModuleAdder("test").update(resource.getModel());

    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + // 
        "<project>\n" + //
        "  <modelVersion>4.0.0</modelVersion>\n" + // 
        "  <groupId>test-groupId</groupId>\n" + //
        "  <artifactId>test-artifactId</artifactId>\n" + // 
        "  <version>1.0-SNAPSHOT</version>\n" + //
        "  <packaging>jar</packaging>\n" + //
        "  <modules>\n" + //
        "    <module>test</module>\n" + //
        "  </modules>\n" + //
        "</project>", MavenModelUtil.toString(resource));
  }
  
  public void testAddingNewDependency() throws Exception {
    String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + // 
    "<project>\n" + //
    "  <modelVersion>4.0.0</modelVersion>\n" + // 
    "  <groupId>test-groupId</groupId>\n" + //
    "  <artifactId>test-artifactId</artifactId>\n" + // 
    "  <version>1.0-SNAPSHOT</version>\n" + //
    "  <packaging>jar</packaging>\n" + //
    "  <dependencies>\n" + //
    "    <dependency>\n" + // 
    "      <groupId>org.junit</groupId>\n" + // 
    "      <artifactId>junit</artifactId>\n" + // 
    "      <version>4.4</version>\n" + // 
    "    </dependency>\n" + // 
    "  </dependencies>\n" + //
    "</project>";
    
    Dependency dependency = new Dependency();
    dependency.setGroupId("org.springframework");
    dependency.setArtifactId("spring");
    dependency.setVersion("2.5");
    
    resource = MavenModelUtil.createResource(project, "testAddingNewDependency.xml", pom);
    new MavenModelManager.DependencyAdder(dependency).update(resource.getModel());
    
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + // 
        "<project>\n" + //
        "  <modelVersion>4.0.0</modelVersion>\n" + // 
        "  <groupId>test-groupId</groupId>\n" + //
        "  <artifactId>test-artifactId</artifactId>\n" + // 
        "  <version>1.0-SNAPSHOT</version>\n" + //
        "  <packaging>jar</packaging>\n" + //
        "  <dependencies>\n" + //
        "    <dependency>\n" + // 
        "      <groupId>org.junit</groupId>\n" + // 
        "      <artifactId>junit</artifactId>\n" + // 
        "      <version>4.4</version>\n" + // 
        "    </dependency>\n" + // 
        "    <dependency>\n" + // 
        "      <groupId>org.springframework</groupId>\n" + // 
        "      <artifactId>spring</artifactId>\n" + // 
        "      <version>2.5</version>\n" + // 
        "    </dependency>\n" + // 
        "  </dependencies>\n" + //
        "</project>", MavenModelUtil.toString(resource));
  }
  
  public void testAddingDependencies() throws Exception {
    String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + // 
    "<project>\n" + //
    "  <modelVersion>4.0.0</modelVersion>\n" + // 
    "  <groupId>test-groupId</groupId>\n" + //
    "  <artifactId>test-artifactId</artifactId>\n" + // 
    "  <version>1.0-SNAPSHOT</version>\n" + //
    "  <packaging>jar</packaging>\n" + //
    "</project>";
    
    Dependency dependency = new Dependency();
    dependency.setGroupId("org.springframework");
    dependency.setArtifactId("spring");
    dependency.setVersion("2.5");
    
    resource = MavenModelUtil.createResource(project, "testAddingDependencies.xml", pom);
    new MavenModelManager.DependencyAdder(dependency).update(resource.getModel());
    
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + // 
        "<project>\n" + //
        "  <modelVersion>4.0.0</modelVersion>\n" + // 
        "  <groupId>test-groupId</groupId>\n" + //
        "  <artifactId>test-artifactId</artifactId>\n" + // 
        "  <version>1.0-SNAPSHOT</version>\n" + //
        "  <packaging>jar</packaging>\n" + //
        "  <dependencies>\n" + //
        "    <dependency>\n" + // 
        "      <groupId>org.springframework</groupId>\n" + // 
        "      <artifactId>spring</artifactId>\n" + // 
        "      <version>2.5</version>\n" + // 
        "    </dependency>\n" + // 
        "  </dependencies>\n" + //
        "</project>", MavenModelUtil.toString(resource));
    
  }
  
  public void testAddingNewPlugin() throws Exception {
    String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + // 
    "<project>\n" + //
    "  <modelVersion>4.0.0</modelVersion>\n" + // 
    "  <groupId>test-groupId</groupId>\n" + //
    "  <artifactId>test-artifactId</artifactId>\n" + // 
    "  <version>1.0-SNAPSHOT</version>\n" + //
    "  <packaging>jar</packaging>\n" + //
    "</project>";
    
    Dependency dependency = new Dependency();
    dependency.setGroupId("org.springframework");
    dependency.setArtifactId("spring");
    dependency.setVersion("2.5");
    
    resource = MavenModelUtil.createResource(project, "testAddingPlugin.xml", pom);
    new MavenModelManager.PluginAdder("org.apache.maven.plugins", "maven-help-plugin", "2.0.2").update(resource.getModel());
    
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + // 
        "<project>\n" + //
        "  <modelVersion>4.0.0</modelVersion>\n" + // 
        "  <groupId>test-groupId</groupId>\n" + //
        "  <artifactId>test-artifactId</artifactId>\n" + // 
        "  <version>1.0-SNAPSHOT</version>\n" + //
        "  <packaging>jar</packaging>\n" + //
        "  <build>\n" + //
        "    <plugins>\n" + // 
        "      <plugin>\n" + // 
        "        <artifactId>maven-help-plugin</artifactId>\n" + // 
        "        <version>2.0.2</version>\n" +  //
        "        <configuration></configuration>\n" + // 
        "      </plugin>\n" + // 
        "    </plugins>\n" + // 
        "  </build>\n" + //
        "</project>", MavenModelUtil.toString(resource));
  }
  

  public void testCreateMavenModel() throws Exception {

    testCreateMavenModel("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" " + //
        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + // 
        "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" + // 
        "  <modelVersion>4.0.0</modelVersion>\n" + //
        "  <groupId>org.sonatype.projects</groupId>\n" + // 
        "  <artifactId>foo</artifactId>\n" + //
        "  <version>0.0.1-SNAPSHOT</version>\n" + // 
        "</project>", "createMavenModel1.xml");

    testCreateMavenModel("<project>\n" + //
        "  <modelVersion>4.0.0</modelVersion>\n" + // 
        "  <groupId>org.sonatype.projects</groupId>\n" + // 
        "  <artifactId>foo</artifactId>\n" + //
        "  <version>0.0.1-SNAPSHOT</version>\n" + // 
        "</project>", "createMavenModel2.xml");

  }

  private void testCreateMavenModel(String pom, String pomFileName) throws Exception {
    MavenModelManager modelManager = MavenPlugin.getDefault().getMavenModelManager();
    
    Model model = modelManager.readMavenModel(new ByteArrayInputStream(pom.getBytes("UTF-8")));
    
    IFile pomFile = project.getFile(pomFileName);
    modelManager.createMavenModel(pomFile, model);

    StringWriter sw = new StringWriter();

    InputStream is = pomFile.getContents();
    try {
      IOUtil.copy(is, sw, "UTF-8");
    } finally {
      is.close();
    }

    assertEquals("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" " //
        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
        + "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
        + "  <modelVersion>4.0.0</modelVersion>\n" //
        + "  <groupId>org.sonatype.projects</groupId>\n" //
        + "  <artifactId>foo</artifactId>\n" //
        + "  <version>0.0.1-SNAPSHOT</version>\n" //
        + "</project>", //
        sw.toString().replaceAll("\r\n", "\n"));
  }

  
//private XMLResource getResource(String pom) throws IOException {
//  // register PomPackage in standalone environment 
//  PomPackage.eINSTANCE.getEFactoryInstance();
//
//  ReadableInputStream is = new URIConverter.ReadableInputStream(new StringReader(pom), "UTF-8");
//  
////  ResourceSet resourceSet = new ResourceSetImpl();
////  XMLResource resource = (XMLResource) resourceSet.MavenModelUtil.createResource(project, URI.createURI("*.xml"));
//
//  // mainResource.setURI(uri);
////  resource.load(is, resourceSet.getLoadOptions());    
//  
//  
////  // String path = pomFile.getFullPath().toOSString();
//  URI uri = URI.createPlatformResourceURI("/", true);
//
//  Map<Object, Object> loadOptions = new HashMap<Object, Object>();
//  loadOptions.put(XMLResource.XML_NS, "http://maven.apache.org/POM/4.0.0");
//  loadOptions.put(XMLResource.XML_SCHEMA_URI, "http://maven.apache.org/xsd/maven-4.0.0.xsd");
//  
//  ExtendedMetaData extendedMetaData = new BasicExtendedMetaData();
//  extendedMetaData.setDocumentRoot(PomPackage.eINSTANCE.getDocumentRoot());
//  extendedMetaData.setNamespace(PomPackage.eINSTANCE, null);
//  
//  EStructuralFeature f;
//  
//  loadOptions.put(XMLResource.OPTION_EXTENDED_META_DATA, extendedMetaData);
//  
//  XMLOptions xmlOptions = new XMLOptionsImpl();
//  // xmlOptions.setProcessAnyXML(true);
//  xmlOptions.setProcessSchemaLocations(true);
//  
//  xmlOptions.setExternalSchemaLocations(Collections.singletonMap("http://maven.apache.org/POM/4.0.0", URI.createURI("http://maven.apache.org/xsd/maven-4.0.0.xsd")));
//  
//  loadOptions.put(XMLResource.OPTION_XML_OPTIONS, xmlOptions);    
//  
//  PomResourceFactoryImpl factory = new PomResourceFactoryImpl();
//  PomResourceImpl resource = (PomResourceImpl) factory.MavenModelUtil.createResource(project, uri);
//  resource.load(is, loadOptions);
//  
//  return resource;
//}

  
}

