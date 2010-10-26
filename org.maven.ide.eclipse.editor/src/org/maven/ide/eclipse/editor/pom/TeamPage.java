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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.editor.composites.TeamComposite;
import org.maven.ide.eclipse.editor.internal.Messages;


/**
 * @author Eugene Kuleshov
 */
public class TeamPage extends MavenPomEditorPage {

  private TeamComposite teamComposite;
  
  public TeamPage(MavenPomEditor pomEditor) {
    super(pomEditor, IMavenConstants.PLUGIN_ID + ".pom.team", Messages.TeamPage_title); //$NON-NLS-1$
  }

  public void dispose() {
    if(teamComposite!=null) {
      teamComposite.dispose();
    }
    super.dispose();
  }
  
  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();
    
    ScrolledForm form = managedForm.getForm();
    form.setText(Messages.TeamPage_form);
    
    form.getBody().setLayout(new GridLayout(1, true));

    teamComposite = new TeamComposite(this, form.getBody(), SWT.NONE);
    teamComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    toolkit.adapt(teamComposite);
    
//    form.pack();

    super.createFormContent(managedForm);
  }

  public void loadData() {
    teamComposite.loadData(model );
  }
  
  public void updateView(final Notification notification) {
    Display.getDefault().asyncExec(new Runnable(){
      public void run(){
        if(teamComposite!=null) {
          teamComposite.updateView(notification);
        }        
      }
    });

  }
}
