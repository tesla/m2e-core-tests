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
import org.maven.ide.eclipse.editor.composites.RepositoriesComposite;

/**
 * @author Eugene Kuleshov
 */
public class RepositoriesPage extends EMFEditorPage {

  private RepositoriesComposite repositoriesComposite;
  
  public RepositoriesPage(MavenPomEditor pomEditor) {
    super(pomEditor, MavenPlugin.PLUGIN_ID + ".pom.repositories", "Repositories");
  }
  
  public void dispose() {
    if(repositoriesComposite!=null) {
      repositoriesComposite.dispose();
    }
    super.dispose();
  }

  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();

    ScrolledForm form = managedForm.getForm();
    form.setText("Repositories");
    
    Composite body = form.getBody();
    body.setLayout(new GridLayout(1, true));

    repositoriesComposite = new RepositoriesComposite(body, SWT.NONE);
    repositoriesComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    toolkit.adapt(repositoriesComposite);
    
    toolkit.decorateFormHeading(form.getForm());
    
    // form.pack();
  }

  public void loadData() {
    repositoriesComposite.loadData(this);
  }
  
  public void updateView(Notification notification) {
    if(repositoriesComposite!=null) {
      repositoriesComposite.updateView(this, notification);
    }
  }
  

//  public static class PairNode {
//    final String label;
//    final Object value;
//  
//    public PairNode(String label, Object value) {
//      this.label = label;
//      this.value = value;
//    }
//  }
//
//
//  public static class ExclusionsNode {
//
//    final String label;
//    final Exclusion exclusions;
//
//    public ExclusionsNode(String label, Exclusion exclusions) {
//      this.label = label;
//      this.exclusions = exclusions;
//    }
//  
//  }

}
