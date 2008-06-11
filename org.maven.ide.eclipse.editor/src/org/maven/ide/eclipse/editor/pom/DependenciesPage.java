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
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.editor.composites.DependenciesComposite;

/**
 * @author Eugene Kuleshov
 */
public class DependenciesPage extends EMFEditorPage {
  
  private DependenciesComposite dependenciesComposite;
  
  public DependenciesPage(MavenPomEditor pomEditor) {
    super(pomEditor, MavenPlugin.PLUGIN_ID + ".pom.dependencies", "Dependencies");
  }

  public void dispose() {
    if(dependenciesComposite!=null) {
      dependenciesComposite.dispose();
    }
    super.dispose();
  }
  
  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();
    
    ScrolledForm form = managedForm.getForm();
    form.setText("Dependencies");
    
    form.getBody().setLayout(new GridLayout(1, true));

    dependenciesComposite = new DependenciesComposite(form.getBody(), SWT.NONE);
    dependenciesComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    toolkit.adapt(dependenciesComposite);
    
    toolkit.decorateFormHeading(form.getForm());
    
//    form.pack();
  }

  public void loadData() {
    dependenciesComposite.loadData(this);
  }
  
  public void updateView(Notification notification) {
    dependenciesComposite.updateView(this, notification);
  }
  
}
