/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.maven.model.Dependency;
import org.apache.maven.pom.x400.ProjectDocument;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlCursor.TokenType;
import org.maven.ide.eclipse.MavenPlugin;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


/**
 * @author Eugene Kuleshov
 */
public class MavenModelManagerTest extends TestCase {

  private static final String URI = ProjectDocument.type.getDocumentElementName().getNamespaceURI();

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
    
    ProjectDocument document = getProjectDocument(pom);
    new MavenModelManager.ModuleAdder("test").update(document);
    
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
    		"</project>", toString(document));
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
    
    ProjectDocument document = getProjectDocument(pom);
    new MavenModelManager.ModuleAdder("test").update(document);
    
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
        "</project>", toString(document));
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

    ProjectDocument document = getProjectDocument(pom);
    new MavenModelManager.ModuleAdder("test").update(document);

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
        "</project>", toString(document));
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
    
    ProjectDocument document = getProjectDocument(pom);
    new MavenModelManager.DependencyAdder(dependency).update(document);
    
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
        "</project>", toString(document));
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
    
    ProjectDocument document = getProjectDocument(pom);
    new MavenModelManager.DependencyAdder(dependency).update(document);
    
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
        "</project>", toString(document));
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
    
    ProjectDocument document = getProjectDocument(pom);
    new MavenModelManager.PluginAdder("org.apache.maven.plugins", "maven-help-plugin", "2.0.2").update(document);
    
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
        "        <version>2.0.2</version>\n" + // 
        "      </plugin>\n" + // 
        "    </plugins>\n" + // 
        "  </build>\n" + //
        "</project>", toString(document));
  }
  
  public void testCreateMavenModel() throws Exception {

    testCreateMavenModel("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + // 
        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" " + //
        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + // 
        "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" + // 
        "  <modelVersion>4.0.0</modelVersion>\n" + //
        "  <groupId>org.sonatype.projects</groupId>\n" + // 
        "  <artifactId>foo</artifactId>\n" + //
        "  <version>0.0.1-SNAPSHOT</version>\n" + // 
        "</project>");
    
    testCreateMavenModel("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + // 
        "<project>\n" + //
        "  <modelVersion>4.0.0</modelVersion>\n" + // 
        "  <groupId>org.sonatype.projects</groupId>\n" + // 
        "  <artifactId>foo</artifactId>\n" + //
        "  <version>0.0.1-SNAPSHOT</version>\n" + // 
        "</project>");
    
  }

  private void testCreateMavenModel(String pom) throws XmlException, IOException {
    XmlOptions options = new XmlOptions();
    
    Map ns = Collections.singletonMap("", URI);
    options.setLoadSubstituteNamespaces(ns);
    options.setSaveNamespacesFirst();

    ProjectDocument document = ProjectDocument.Factory.parse(pom, options);

    new MavenModelManager.NamespaceAdder().update(document);
    
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    document.save(os, options);
    
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
    		"<project xmlns=\"http://maven.apache.org/POM/4.0.0\" " +
    		    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
    		    "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" + 
    		"  <modelVersion>4.0.0</modelVersion>\n" + 
    		"  <groupId>org.sonatype.projects</groupId>\n" + 
    		"  <artifactId>foo</artifactId>\n" + 
    		"  <version>0.0.1-SNAPSHOT</version>\n" + 
    		"</project>", new String(os.toByteArray()).replaceAll("\r\n", "\n"));
  }
  
  private ProjectDocument getProjectDocument(String pom) throws XmlException {
    XmlOptions options = new XmlOptions();
    
    Map ns = Collections.singletonMap("", URI);
    options.setLoadSubstituteNamespaces(ns);
    options.setSaveImplicitNamespaces(ns);
    
    return ProjectDocument.Factory.parse(pom, options);
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

