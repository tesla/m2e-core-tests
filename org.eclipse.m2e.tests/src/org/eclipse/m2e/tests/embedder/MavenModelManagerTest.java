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
import java.util.List;

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.codehaus.plexus.util.IOUtil;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.model.edit.pom.util.PomResourceImpl;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.RequireMavenExecutionContext;


/**
 * @author Eugene Kuleshov
 */
public class MavenModelManagerTest extends AbstractMavenProjectTestCase {

  private static final String TEST_PROJECT_NAME = "editor-tests";

  private MavenModelManager modelManager = MavenPlugin.getMavenModelManager();

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
    try {
      if(resource != null) {
        resource.unload();
      }
    } finally {
      super.tearDown();
    }
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

  @RequireMavenExecutionContext
  public void test416882_dependencyTree() throws Exception {
    IProject[] projects = importProjects("projects/416882_dependencyTree", new String[] {"pom.xml",
        "direct-depA/pom.xml", "direct-depB/pom.xml"}, new ResolverConfiguration());

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(projects[0], monitor);
    MavenProject mavenProject = facade.getMavenProject(monitor);

    DependencyNode root = modelManager.readDependencyTree(null /*context*/, mavenProject, JavaScopes.TEST, monitor);

    List<DependencyNode> children = root.getChildren();
    assertEquals(2, children.size());

    DependencyNode depA = children.get(0);
    assertEquals("direct-depA", depA.getArtifact().getArtifactId());
    assertEquals(2, depA.getChildren().size());

    DependencyNode plexusUtilsA = depA.getChildren().get(0);
    assertEquals("plexus-utils", plexusUtilsA.getArtifact().getArtifactId());
    assertEquals("2.0.5", plexusUtilsA.getArtifact().getVersion());
    assertEquals(JavaScopes.COMPILE, plexusUtilsA.getDependency().getScope());

    DependencyNode junitA = depA.getChildren().get(1);
    assertEquals("junit", junitA.getArtifact().getArtifactId());
    assertEquals("3.8.2", junitA.getArtifact().getVersion());
    assertEquals(JavaScopes.TEST, junitA.getDependency().getScope());
    assertEquals(DependencyNode.MANAGED_SCOPE, junitA.getManagedBits() & DependencyNode.MANAGED_SCOPE);
    assertEquals(JavaScopes.COMPILE, DependencyManagerUtils.getPremanagedScope(junitA));
    assertEquals(DependencyNode.MANAGED_VERSION, junitA.getManagedBits() & DependencyNode.MANAGED_VERSION);
    assertEquals("3.8.1", DependencyManagerUtils.getPremanagedVersion(junitA));

    DependencyNode depB = children.get(1);
    assertEquals("direct-depB", depB.getArtifact().getArtifactId());
    assertEquals(1, depB.getChildren().size());

    DependencyNode plexusUtilsB = depB.getChildren().get(0);
    assertEquals("plexus-utils", plexusUtilsB.getArtifact().getArtifactId());
    assertEquals("3.0", plexusUtilsB.getArtifact().getVersion());
    assertEquals(plexusUtilsA.getArtifact(),
        ((DependencyNode) plexusUtilsB.getData().get(ConflictResolver.NODE_DATA_WINNER)).getArtifact());
  }
}
