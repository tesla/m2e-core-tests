/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.editor.pom;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.editor.MavenEditorPlugin;
import org.eclipse.m2e.editor.pom.MavenPomEditor.Callback;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Test;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.DefaultDependencyNode;

public class MavenPomEditorTest extends AbstractMavenProjectTestCase {
  
  Object dependencyNode;
  Object exception;
  boolean secondCalled = false;
  
  protected void setUp() throws Exception {
    super.setUp();
    dependencyNode = null;
    exception = null;
    secondCalled = false;
  }

  @Test
  public void testLoadDependencies() throws Exception {
    MavenPomEditor editor = new TestingMavenPomEditor(false, 1000);
    
    editor.loadDependencies(new Callback() {
      
      public void onFinish(DependencyNode node) {
        assertNotNull(node); 
        dependencyNode = node;
      }
      
      public void onException(CoreException ex) {
        assertTrue(false);
      }
    }, org.apache.maven.artifact.Artifact.SCOPE_TEST);
    
    Thread.sleep(2000);
    
    assertNotNull(dependencyNode);
  }
  
  @Test
  public void testMultipleLoadDependencies() throws Exception {
    int delayInMS = 1000 * 10;
    MavenPomEditor editor = new TestingMavenPomEditor(false, delayInMS);
    
    dependencyNode = null;
    long previous = System.currentTimeMillis();
    editor.loadDependencies(new Callback() {
      
      public void onFinish(DependencyNode node) {
        if (dependencyNode == null) {
          dependencyNode = node; 
        } else {
          assertEquals(node, dependencyNode);
        }
      }
      
      public void onException(CoreException ex) {
        assertTrue(false);
      }
    }, "");
    long post = System.currentTimeMillis();
    long diff = post-previous;
    
    assertTrue(diff < (delayInMS - 1000));
    
    editor.loadDependencies(new Callback() {
      
      public void onFinish(DependencyNode node) {
        if (dependencyNode == null) {
          dependencyNode = node; 
        } else {
          assertEquals(node, dependencyNode);
        }
        secondCalled = true;
      }
      
      public void onException(CoreException ex) {
        assertTrue(false);
      }
    }, "");
    
    Thread.sleep(delayInMS + 1000);
    
    assertNotNull(dependencyNode);
    assertTrue(secondCalled);
  }
  
  @Test
  public void testLoadingDependenciesError() throws Exception {
    MavenPomEditor editor = new TestingMavenPomEditor(true, 0);
    exception = null;
    editor.loadDependencies(new Callback() {
      
      public void onFinish(DependencyNode node) {
        assertTrue(false);
      }
      
      public void onException(CoreException ex) {
        assertNotNull(ex);
        exception = ex;
      }
    }, "");
    Thread.sleep(1000);
    assertNotNull(exception);
  }
  
  private static class TestingMavenPomEditor extends MavenPomEditor {
    
    private boolean throwException = false;
    private int delayInMS;

    public TestingMavenPomEditor(boolean throwException, int delayInMS) {
      this.throwException  = throwException;
      this.delayInMS = delayInMS;
    }

    /* (non-Javadoc)
     * @see org.eclipse.m2e.editor.pom.MavenPomEditor#readDependencyTree(boolean, java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
     */
    public synchronized DependencyNode readDependencyTree(boolean force, String classpath, IProgressMonitor monitor)
        throws CoreException {
      /*
       * We don't care to load the real dependency tree. That's for a separate test. We
       * want to test that the loading job works, so this task can just sleep
       * for a bit and then return something hard coded.
       */
      
      if (this.throwException) {
        throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, "CoreException thrown as requested"));
      }
      
      DefaultDependencyNode root = new DefaultDependencyNode();
      Artifact fooFeatureArtifact = new DefaultArtifact("foo:feature:0.1");
      Dependency dep = new Dependency(fooFeatureArtifact, "compile");
      
      DefaultDependencyNode fooFeature = new DefaultDependencyNode(dep);
      
      root.getChildren().add(fooFeature);
      
      Artifact fooMainArtifact = new DefaultArtifact("foo:main:1.1");
      Artifact fooDataArtifact = new DefaultArtifact("foo:data:0.5");
      
      dep = new Dependency(fooMainArtifact, "test");
      DefaultDependencyNode fooMain = new DefaultDependencyNode(dep);
      
      dep = new Dependency(fooDataArtifact, "test");
      DefaultDependencyNode fooData = new DefaultDependencyNode(dep);
      
      fooFeature.getChildren().add(fooMain);
      fooFeature.getChildren().add(fooData);
      
      try {
        Thread.sleep(delayInMS);
      } catch(InterruptedException ex) {
        throw new RuntimeException(ex);
      }
      
      return root;
    }
    
  }

}
