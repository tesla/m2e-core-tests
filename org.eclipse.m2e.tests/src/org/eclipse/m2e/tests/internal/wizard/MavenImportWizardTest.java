/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.internal.wizard;

import java.io.File;
import java.util.Collections;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.wizards.MavenImportWizard;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class MavenImportWizardTest extends AbstractMavenProjectTestCase {

  /**
   * When importing a project using MavenImportWizard we should never not the directory we're importing from by default.
   * 
   * @throws Exception
   */
  public void testNoRenameOnMavenImport() throws Exception {
    IWorkspaceRoot root = workspace.getRoot();

    File src = new File("projects/projectimport/p001");
    File dst = root.getLocation().append("sub-dir").append("not-artifact-id").toFile();
    copyDir(src, dst);

    MavenImportWizard wizard = new MavenImportWizard(new ProjectImportConfiguration(), Collections.singletonList(dst
        .getAbsolutePath()));
    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    WizardDialog dialog = new WizardDialog(shell, wizard);
    dialog.setBlockOnOpen(false);
    dialog.open();

    wizard.performFinish();
    dialog.close();
    waitForJobsToComplete();

    IProject project = workspace.getRoot().getProjects()[0];
    assertEquals("not-artifact-id", project.getLocation().lastSegment());
    assertEquals("p001", project.getName());
  }

  /**
   * When importing a project located in the workspace root using MavenImportWizard the directory shouldn't be renamed
   * by default. Instead the project should be renamed to match the directory.
   * 
   * @throws Exception
   */
  public void testNoRenameOnMavenImportInWorkspaceRoot() throws Exception {
    IWorkspaceRoot root = workspace.getRoot();

    File src = new File("projects/projectimport/p001");
    File dst = root.getLocation().append("not-artifact-id").toFile();
    copyDir(src, dst);

    MavenImportWizard wizard = new MavenImportWizard(new ProjectImportConfiguration(), Collections.singletonList(dst
        .getAbsolutePath()));
    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    WizardDialog dialog = new WizardDialog(shell, wizard);
    dialog.setBlockOnOpen(false);
    dialog.open();

    wizard.performFinish();
    dialog.close();
    waitForJobsToComplete();

    IProject project = workspace.getRoot().getProjects()[0];
    assertEquals("not-artifact-id", project.getLocation().lastSegment());
    assertEquals("not-artifact-id", project.getName());
  }

  /**
   * When importing a maven project with rename required set to 'true'. Ensure the folder is renamed to match the
   * artifact-id.
   * 
   * @throws Exception
   */
  public void testRenameOnMavenImport() throws Exception {
    IWorkspaceRoot root = workspace.getRoot();

    File src = new File("projects/projectimport/p001");
    File dst = root.getLocation().append("not-artifact-id").toFile();
    copyDir(src, dst);

    MavenImportWizard wizard = new MavenImportWizard(new ProjectImportConfiguration(), Collections.singletonList(dst
        .getAbsolutePath()));
    wizard.setBasedirRemameRequired(true);

    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    WizardDialog dialog = new WizardDialog(shell, wizard);
    dialog.setBlockOnOpen(false);
    dialog.open();

    wizard.performFinish();
    dialog.close();
    waitForJobsToComplete();

    IProject project = workspace.getRoot().getProjects()[0];
    assertEquals("p001", project.getLocation().lastSegment());
    assertEquals("p001", project.getName());
  }

}
