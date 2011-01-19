/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.tests.ui.dialogs;

import java.util.List;

import junit.framework.Assert;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.window.Window;
import org.eclipse.m2e.core.ui.dialogs.AddDependencyDialog;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class AddDependencyDialogTest extends AbstractMavenProjectTestCase {

  int shortDelayInMs = 1000 * 1;
  int longDelayInMs = 1000 * 10;
  
  protected void setUp() throws Exception {
    super.setUp();
  }
  
  public void testManualEntry() throws Exception {
    IProject project = this.createProject("dependencies", "projects/dependencies/pom.xml");
    final TestAddDependencyDialog dialog = new TestAddDependencyDialog(Display.getCurrent().getActiveShell(), false, project);
    Display.getDefault().asyncExec(new Runnable() {
      
      public void run() {
        try {
          //Wait for dialog to open
          Thread.sleep(shortDelayInMs);
          dialog.testManualEntry();
        } catch(InterruptedException ex) {
          throw new RuntimeException(ex);
        }
      }
    });
    int retval = dialog.open();
    assertEquals(retval, Window.OK);
    
    List<Dependency> deps = dialog.getDependencies();
    assertNotNull(deps);
    assertTrue(deps.size() == 1);
    Dependency dep = deps.get(0);
    assertNotNull(dep);
    assertEquals(dep.getArtifactId(), "feature");
    assertEquals(dep.getGroupId(), "fooGroup");
    assertEquals(dep.getVersion(), "1.0");
  }

// mkleint: i'm about to remove the info display about transitive dependencies..
  
//  public void testShowTransitiveDependency() throws Exception {
//    IProject project = this.createProject("dependencies", "projects/dependencies/pom.xml");
//    final TestAddDependencyDialog dialog = new TestAddDependencyDialog(Display.getCurrent().getActiveShell(), false, project);
//    dialog.setDepdencyNode(createDependencyTree());
//    Display.getDefault().asyncExec(new Runnable() {
//      
//      public void run() {
//        try {
//          //Wait for dialog to open
//          Thread.sleep(shortDelayInMs);
//          dialog.testShowTransitiveDependency();
//        } catch(InterruptedException ex) {
//          throw new RuntimeException(ex);
//        }
//      }
//    });
//    int retval = dialog.open();
//    assertEquals(retval, Window.OK);
//    
//  }
//  
  private class TestAddDependencyDialog extends AddDependencyDialog {

    public TestAddDependencyDialog(Shell parent, boolean isForDependencyManagement, IProject project) {
      super(parent, isForDependencyManagement, project, new MavenProject());
    }
//    
//    public void testShowTransitiveDependency() {
//      Display.getDefault().asyncExec(new Runnable() {
//        
//        public void run() {
//          try {
//            Assert.assertNotNull(dependencyNode);
//            
//            final Map<String, IndexedArtifact> results = new TreeMap<String, IndexedArtifact>();
//            dependencyNode.accept(new DependencyVisitor() {
//              
//              public boolean visitLeave(DependencyNode arg0) {
//                if (arg0.getDependency() != null && arg0.getDependency().getArtifact() != null) {
//                  Artifact source = arg0.getDependency().getArtifact();
//                  IndexedArtifact artifact = new IndexedArtifact(source.getGroupId(), source.getArtifactId(), null, null, null);
//                  Date date = new Date();
//                  IndexedArtifactFile file = new IndexedArtifactFile("", source.getGroupId(), source.getArtifactId(), source.getVersion(), "", "", "", 0, date, 0, 0, "", null);
//                  artifact.addFile(file);
//                  results.put(arg0.getDependency().getArtifact().getArtifactId(), artifact);
//                }
//                return true;
//              }
//              
//              public boolean visitEnter(DependencyNode arg0) {
//                return true;
//              }
//            });
//            
//            resultsViewer.setInput(results);
//            StructuredSelection selection = new StructuredSelection(results.get("main"));
//            resultsViewer.setSelection(selection);
//            resultsListener.selectionChanged(new SelectionChangedEvent(resultsViewer, selection));
//            assertFalse(infoTextarea.getText().equals(""));
//            assertTrue(infoTextarea.getText().contains("main"));
//            assertTrue(infoTextarea.getText().contains("foo"));
//            assertTrue(infoTextarea.getText().contains("1.1"));
//            assertTrue(infoTextarea.getText().contains("transitive"));
//          } catch (Throwable e) {
//            cancelPressed();
//            throw new RuntimeException(e);
//          } 
//          okPressed();
//          
//        }
//      });
//    }
//
    public void testManualEntry() {
      Display.getDefault().asyncExec(new Runnable() {
        
        @SuppressWarnings("synthetic-access")
        public void run() {
          artifactIDtext.setText("feature");
          groupIDtext.setText("fooGroup");
          versionText.setText("1.0");
          Assert.assertTrue(infoTextarea.getText().equals(""));
          okPressed();
        }
      });
    }
    
  }
//  
//  public static DependencyNode createDependencyTree() {
//    DefaultDependencyNode root = new DefaultDependencyNode();
//    Artifact fooFeatureArtifact = new DefaultArtifact("foo:feature:0.1");
//    org.sonatype.aether.graph.Dependency dep = new org.sonatype.aether.graph.Dependency(fooFeatureArtifact, "compile");
//    
//    DefaultDependencyNode fooFeature = new DefaultDependencyNode(dep);
//    
//    root.getChildren().add(fooFeature);
//    
//    Artifact fooMainArtifact = new DefaultArtifact("foo:main:1.1");
//    Artifact fooDataArtifact = new DefaultArtifact("foo:data:0.5");
//    
//    dep = new org.sonatype.aether.graph.Dependency(fooMainArtifact, "test");
//    DefaultDependencyNode fooMain = new DefaultDependencyNode(dep);
//    
//    dep = new org.sonatype.aether.graph.Dependency(fooDataArtifact, "test");
//    DefaultDependencyNode fooData = new DefaultDependencyNode(dep);
//    
//    fooFeature.getChildren().add(fooMain);
//    fooFeature.getChildren().add(fooData);
//    return root;
//  }
}
