/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.internal.wizard;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.WorkingSets;
import org.eclipse.m2e.core.ui.internal.wizards.MavenImportWizardPage;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class MavenImportWizardPageTest extends AbstractMavenProjectTestCase {

  private WizardDialog dialog;

  private Wizard wizard;

  private MavenImportWizardPage page;

  protected void setUp() throws Exception {
    super.setUp();

    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    wizard = new Wizard() {
      public boolean performFinish() {
        return false;
      }
    };
    dialog = new WizardDialog(shell, wizard);

    page = new MavenImportWizardPage(new ProjectImportConfiguration());

    wizard.addPage(page);
    dialog.setBlockOnOpen(false);
  }

  protected void tearDown() throws Exception {
    dialog.close();
    super.tearDown();
  }

  private void scanProjects(String pathname) throws IOException {
    page.setLocations(Arrays.asList(new File(pathname).getCanonicalPath()));
    dialog.open();
    page.scanProjects();
  }

  public void test408042_simpleProject() throws Exception {
    scanProjects("projects/408042_importWorkingSet/simple");
    assertFalse(page.shouldCreateWorkingSet());
    assertEquals("simple", page.getWorkingSetName());
  }

  public void test408042_multimoduleProject() throws Exception {
    scanProjects("projects/408042_importWorkingSet/multimodule");
    assertTrue(page.shouldCreateWorkingSet());
    assertEquals("multimodule-parent", page.getWorkingSetName());
  }

  public void test408042_noRootProject() throws Exception {
    scanProjects("projects/408042_importWorkingSet");
    assertFalse(page.shouldCreateWorkingSet());
    assertEquals("", page.getWorkingSetName());
  }

  public void test408042_nestedproject() throws Exception {
    IProject outer = importProject("projects/408042_importWorkingSet/nestedproject/pom.xml");
    WorkingSets.addToWorkingSet(new IProject[] {outer}, "testworkingset");
    scanProjects(outer.getLocation().append("inner").toOSString());
    assertTrue(page.shouldCreateWorkingSet());
    assertEquals("testworkingset", page.getWorkingSetName());
  }

  public void test422008_preselectedWorkingSet() throws Exception {
    String workingSetName = "preselected";
    WorkingSets.getOrCreateWorkingSet(workingSetName);
    IProject project = importProject("projects/408042_importWorkingSet/simple/pom.xml");
    page.setWorkingSetName(workingSetName);
    page.setLocations(Arrays.asList(project.getLocation().toOSString()));
    dialog.open();
    page.scanProjects();
    assertTrue(page.shouldCreateWorkingSet());
    assertEquals(workingSetName, page.getWorkingSetName());
  }

  public void test533463_noArtifactId() throws Exception {
    scanProjects("projects/533463_noArtifactId");
    assertFalse(page.shouldCreateWorkingSet());
    assertEquals("", page.getWorkingSetName());
  }
}
