/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.internal.wizard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    wizard = new Wizard() {
      @Override
      public boolean performFinish() {
        return false;
      }
    };
    dialog = new WizardDialog(shell, wizard);

    page = new MavenImportWizardPage(new ProjectImportConfiguration());

    wizard.addPage(page);
    dialog.setBlockOnOpen(false);
  }

  @Override
  @After
  public void tearDown() throws Exception {
    dialog.close();
    super.tearDown();
  }

  private void scanProjects(String pathname) throws IOException {
    page.setLocations(Arrays.asList(new File(pathname).getCanonicalPath()));
    dialog.open();
    page.scanProjects();
  }

  @Test
  public void test408042_simpleProject() throws Exception {
    scanProjects("projects/408042_importWorkingSet/simple");
    waitForJobsToComplete();
    assertFalse(page.shouldCreateWorkingSet());
    assertEquals("simple", page.getWorkingSetName());
  }

  @Test
  public void test408042_multimoduleProject() throws Exception {
    scanProjects("projects/408042_importWorkingSet/multimodule");
    waitForJobsToComplete();
    assertTrue(page.shouldCreateWorkingSet());
    assertEquals("multimodule-parent", page.getWorkingSetName());
  }

  @Test
  public void test408042_noRootProject() throws Exception {
    scanProjects("projects/408042_importWorkingSet");
    assertFalse(page.shouldCreateWorkingSet());
    assertEquals("", page.getWorkingSetName());
  }

  @Test
  public void test408042_nestedproject() throws Exception {
    IProject outer = importProject("projects/408042_importWorkingSet/nestedproject/pom.xml");
    WorkingSets.addToWorkingSet(List.of(outer), "testworkingset");
    scanProjects(outer.getLocation().append("inner").toOSString());
    assertTrue(page.shouldCreateWorkingSet());
    assertEquals("testworkingset", page.getWorkingSetName());
  }

  @Test
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

  @Test
  public void test533463_noArtifactId() throws Exception {
    scanProjects("projects/533463_noArtifactId");
    assertFalse(page.shouldCreateWorkingSet());
    assertEquals("", page.getWorkingSetName());
  }
}
