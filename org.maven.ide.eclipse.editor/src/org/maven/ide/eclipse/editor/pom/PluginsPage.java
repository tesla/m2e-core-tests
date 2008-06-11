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
import org.maven.ide.eclipse.editor.composites.PluginsComposite;

/**
 * @author Eugene Kuleshov
 */
public class PluginsPage extends EMFEditorPage {

  private PluginsComposite pluginsComposite;
  
  public PluginsPage(MavenPomEditor pomEditor) {
    super(pomEditor, MavenPlugin.PLUGIN_ID + ".pom.plugins", "Plugins");
  }
  
  public void dispose() {
    if(pluginsComposite != null) {
      pluginsComposite.dispose();
    }
    super.dispose();
  }

  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();
    ScrolledForm form = managedForm.getForm();
    form.setText("Plugins");
    
    Composite body = form.getBody();
    toolkit.paintBordersFor(body);
    GridLayout gridLayout = new GridLayout(1, true);
    gridLayout.marginHeight = 0;
    body.setLayout(gridLayout);

    pluginsComposite = new PluginsComposite(body, SWT.NONE);
    pluginsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    toolkit.adapt(pluginsComposite);
    
    toolkit.decorateFormHeading(form.getForm());
    
//    form.pack();
  }

  public void loadData() {
    pluginsComposite.loadData(this);
  }
  
  public void updateView(Notification notification) {
    if(pluginsComposite!=null) {
      pluginsComposite.updateView(this, notification);
    }
  }
  
}
