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
  
  private class TestAddDependencyDialog extends AddDependencyDialog {

    public TestAddDependencyDialog(Shell parent, boolean isForDependencyManagement, IProject project) {
      super(parent, isForDependencyManagement, project);
    }
    
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
}
