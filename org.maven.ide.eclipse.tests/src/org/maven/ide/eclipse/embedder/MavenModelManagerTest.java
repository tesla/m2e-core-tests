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
import java.util.Collections;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.maven.model.Dependency;
import org.apache.maven.pom.x400.ProjectDocument;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;


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
  
  private ProjectDocument getProjectDocument(String pom) throws XmlException {
    XmlOptions options = new XmlOptions();
    
    Map ns = Collections.singletonMap("", URI);
    options.setLoadSubstituteNamespaces(ns);
    options.setSaveImplicitNamespaces(ns);
    
    ProjectDocument document = ProjectDocument.Factory.parse(pom, options);
    return document;
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

