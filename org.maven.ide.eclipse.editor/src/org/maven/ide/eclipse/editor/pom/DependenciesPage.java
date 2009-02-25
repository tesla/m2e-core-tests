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
import org.maven.ide.components.pom.Dependencies;
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

    dependenciesComposite = new DependenciesComposite(form.getBody(), SWT.NONE);
    dependenciesComposite.setEditorPage(this);
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
    ValueProvider<Dependencies> dependenciesProvider = new ValueProvider<Dependencies>() {
      public Dependencies getValue() {
        return model.getDependencies();
      }

      public Dependencies create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        Dependencies dependencies = PomFactory.eINSTANCE.createDependencies();
        Command createDependenciesCommand = SetCommand.create(editingDomain, model,
            POM_PACKAGE.getModel_Dependencies(), dependencies);
        compoundCommand.append(createDependenciesCommand);
        return dependencies;
      }
    };

    ValueProvider<Dependencies> dependencyManagementProvider = new ValueProvider<Dependencies>() {
      public Dependencies getValue() {
        DependencyManagement management = model.getDependencyManagement();
        return management==null ? null : management.getDependencies();
      }

      public Dependencies create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        DependencyManagement management = model.getDependencyManagement();
        if(management==null) {
          management = PomFactory.eINSTANCE.createDependencyManagement();
          Command command = SetCommand.create(editingDomain, model, //
              POM_PACKAGE.getModel_DependencyManagement(), management);
          compoundCommand.append(command);
        }
        
        Dependencies dependencies = management.getDependencies();
        if(dependencies == null) {
          dependencies = PomFactory.eINSTANCE.createDependencies();
          Command createDependency = SetCommand.create(editingDomain, management, 
              POM_PACKAGE.getDependencyManagement_Dependencies(), dependencies);
          compoundCommand.append(createDependency);
        }
 
        return dependencies;
      }
    };

    dependenciesComposite.loadData(dependenciesProvider, dependencyManagementProvider);
  }
  
  public void updateView(Notification notification) {
    dependenciesComposite.updateView(this, notification);
  }
  
}
