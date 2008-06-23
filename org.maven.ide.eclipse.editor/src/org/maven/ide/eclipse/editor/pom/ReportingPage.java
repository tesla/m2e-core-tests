/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.editor.composites.ReportingComposite;

/**
 * @author Eugene Kuleshov
 */
public class ReportingPage extends MavenPomEditorPage {

  private ReportingComposite reportingComposite;

  public ReportingPage(MavenPomEditor pomEditor) {
    super(pomEditor, MavenPlugin.PLUGIN_ID + ".pom.reporting", "Reporting");
  }

  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();
    ScrolledForm form = managedForm.getForm();
    form.setText("Reporting (work in progress)");

    Composite body = form.getBody();
    GridLayout gridLayout = new GridLayout(1, true);
    gridLayout.marginHeight = 0;
    body.setLayout(gridLayout);
    toolkit.paintBordersFor(body);

    reportingComposite = new ReportingComposite(body, SWT.NONE);
    reportingComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    toolkit.adapt(reportingComposite);

//    form.pack();

    super.createFormContent(managedForm);
  }

  public void loadData() {
    reportingComposite.loadData(this);
  }

  public void updateView(Notification notification) {
    if(isActive()) {
      reportingComposite.updateView(this, notification);
    }
  }

}
