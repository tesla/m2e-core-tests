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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.maven.ide.components.pom.Build;
import org.maven.ide.components.pom.BuildBase;
import org.maven.ide.components.pom.PluginManagement;
import org.maven.ide.components.pom.PomFactory;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.editor.composites.PluginsComposite;

/**
 * @author Eugene Kuleshov
 */
public class PluginsPage extends MavenPomEditorPage {

  private PluginsComposite pluginsComposite;
  private SearchControl searchControl;
  
  public PluginsPage(MavenPomEditor pomEditor) {
    super(pomEditor, IMavenConstants.PLUGIN_ID + ".pom.plugins", "Plugins");
  }
  
  public void dispose() {
    if(pluginsComposite != null) {
      pluginsComposite.dispose();
    }
    super.dispose();
  }

  public void setActive(boolean active) {
    super.setActive(active);
    if(active) {
      pluginsComposite.setSearchControl(searchControl);
      searchControl.getSearchText().setEditable(true);
    }
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
    
    searchControl = new SearchControl("Find", managedForm);
    
    IToolBarManager pageToolBarManager = form.getForm().getToolBarManager();
    pageToolBarManager.add(searchControl);
    pageToolBarManager.add(new Separator());
    
    form.updateToolBar();
    
//    form.pack();

    super.createFormContent(managedForm);
  }

  public void loadData() {
    ValueProvider<BuildBase> buildProvider = new ValueProvider<BuildBase>() {
      public BuildBase getValue() {
        BuildBase build = model.getBuild();
        return build;
      }
      
      public BuildBase create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        Build build = model.getBuild();
        if(build==null) {
          build = PomFactory.eINSTANCE.createBuild();
          Command command = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Build(), build);
          compoundCommand.append(command);
        }
        
        return build;
      }
    };
    
    ValueProvider<PluginManagement> pluginManagementProvider = new ValueProvider<PluginManagement>() {
      public PluginManagement getValue() {
        Build build = model.getBuild();
        PluginManagement management = build == null ? null : build.getPluginManagement();
        return management;
      }
      
      public PluginManagement create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        Build build = model.getBuild();
        if(build == null) {
          build = PomFactory.eINSTANCE.createBuild();
          Command command = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Build(), build);
          compoundCommand.append(command);
        }

        PluginManagement management = build.getPluginManagement();
        if(management == null) {
          management = PomFactory.eINSTANCE.createPluginManagement();
          Command command = SetCommand.create(editingDomain, build, //
              POM_PACKAGE.getBuildBase_PluginManagement(), management);
          compoundCommand.append(command);
        }
        
        return management;
      }
    };
    
    pluginsComposite.loadData(this, buildProvider, pluginManagementProvider);
  }
  
  public void updateView(Notification notification) {
    pluginsComposite.updateView(this, notification);
  }
  
}
