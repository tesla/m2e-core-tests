/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt.internal.ui;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;

import org.maven.ide.eclipse.core.IMavenConstants;


/**
 * MavenClasspathContainerPage
 * 
 * @author Eugene Kuleshov
 */
public class MavenClasspathContainerPage extends WizardPage implements IClasspathContainerPage,
    IClasspathContainerPageExtension {

  IJavaProject javaProject;

  private IClasspathEntry containerEntry;
  
  public MavenClasspathContainerPage() {
    super("Maven Dependencies");
  }

  // IClasspathContainerPageExtension

  public void initialize(IJavaProject javaProject, IClasspathEntry[] currentEntries) {
    this.javaProject = javaProject;
    // this.currentEntries = currentEntries;
  }

  // IClasspathContainerPage

  public IClasspathEntry getSelection() {
    return this.containerEntry;
  }

  public void setSelection(IClasspathEntry containerEntry) {
    this.containerEntry = containerEntry;
  }

  public void createControl(Composite parent) {
    setTitle("Maven Managed Dependencies");
    setDescription("Set the dependency resolver configuration");


    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout());
    setControl(composite);

    Link link = new Link(composite, SWT.NONE);
    link.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    link.setText("Use <a href=\"#maven\">Maven Project settings</a> to configure Maven dependency resolution");
    link.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        // IWorkbenchPreferenceContainer container= (IWorkbenchPreferenceContainer) getContainer();
        // container.openPage(MavenProjectPreferencePage.ID, javaProject.getProject());
        
        PreferencesUtil.createPropertyDialogOn(getShell(), javaProject.getProject(), //
            IMavenConstants.PREFERENCE_PAGE_ID, new String[] {IMavenConstants.PREFERENCE_PAGE_ID}, null).open();
      }
    });
  }

  public boolean finish() {
    return true;
  }

}
