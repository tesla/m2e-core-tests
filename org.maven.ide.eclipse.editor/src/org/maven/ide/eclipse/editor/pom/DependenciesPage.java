/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.maven.ide.components.pom.DependencyManagement;
import org.maven.ide.components.pom.PomFactory;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.editor.composites.DependenciesComposite;

/**
 * @author Eugene Kuleshov
 */
public class DependenciesPage extends MavenPomEditorPage {
  
  private DependenciesComposite dependenciesComposite;
  private SearchControl searchControl;
  
  public DependenciesPage(MavenPomEditor pomEditor) {
    super(pomEditor, IMavenConstants.PLUGIN_ID + ".pom.dependencies", "Dependencies");
  }

  public void dispose() {
    if(dependenciesComposite!=null) {
      dependenciesComposite.dispose();
    }
    super.dispose();
  }

  public void setActive(boolean active) {
    super.setActive(active);
    if(active) {
      dependenciesComposite.setSearchControl(searchControl);
      searchControl.getSearchText().setEditable(true);
    }
  }
  
  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();
    
    ScrolledForm form = managedForm.getForm();
    form.setText("Dependencies");
    
    form.getBody().setLayout(new GridLayout(1, true));

    dependenciesComposite = new DependenciesComposite(form.getBody(), this, SWT.NONE);
    dependenciesComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    toolkit.adapt(dependenciesComposite);

    searchControl = new SearchControl("Find", managedForm);
    
    IToolBarManager pageToolBarManager = form.getForm().getToolBarManager();
    pageToolBarManager.add(searchControl);
    pageToolBarManager.add(new Separator());
    
    form.updateToolBar();
    
//    form.pack();

    super.createFormContent(managedForm);
  }

  public void loadData() {
    

    ValueProvider<DependencyManagement> dependencyManagementProvider = new ValueProvider<DependencyManagement>() {
      public DependencyManagement getValue() {
        return model.getDependencyManagement();
      }

      public DependencyManagement create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        DependencyManagement management = model.getDependencyManagement();
        if(management==null) {
          management = PomFactory.eINSTANCE.createDependencyManagement();
          Command command = SetCommand.create(editingDomain, model, //
              POM_PACKAGE.getModel_DependencyManagement(), management);
          compoundCommand.append(command);
        }
        
        return management;
      }
    };

    dependenciesComposite.loadData(model, dependencyManagementProvider);
  }
  
  public void updateView(Notification notification) {
    dependenciesComposite.updateView(this, notification);
  }
  
}
