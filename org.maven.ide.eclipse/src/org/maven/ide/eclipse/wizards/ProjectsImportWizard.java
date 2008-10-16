/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wizards;

import org.eclipse.jface.wizard.Wizard;

import org.maven.ide.eclipse.MavenImages;

/**
 * WizardExtension
 *
 * @author Eugene Kuleshov
 */
public class ProjectsImportWizard extends Wizard {
  private final String location;

  private ProjectsImportPage mainPage;

  public ProjectsImportWizard(String location) {
    this.location = location;
    setWindowTitle("Import");
    setDefaultPageImageDescriptor(MavenImages.WIZ_IMPORT_WIZ);
  }

  public void addPages() {
    mainPage = new ProjectsImportPage(this.location);
    addPage(mainPage);
  }

  public boolean performCancel() {
    mainPage.performCancel();
    return true;
  }

  public boolean performFinish() {
    return mainPage.createProjects();
  }
}