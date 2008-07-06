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
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.maven.ide.components.pom.ContributorsType;
import org.maven.ide.components.pom.DevelopersType;
import org.maven.ide.components.pom.PomFactory;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.editor.composites.TeamComposite;


/**
 * @author Eugene Kuleshov
 */
public class TeamPage extends MavenPomEditorPage {

  private TeamComposite teamComposite;
  
  public TeamPage(MavenPomEditor pomEditor) {
    super(pomEditor, IMavenConstants.PLUGIN_ID + ".pom.team", "Team");
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
    form.setText("Team");
    
    form.getBody().setLayout(new GridLayout(1, true));

    teamComposite = new TeamComposite(form.getBody(), SWT.NONE);
    teamComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    toolkit.adapt(teamComposite);
    
//    form.pack();

    super.createFormContent(managedForm);
  }

  public void loadData() {
    ValueProvider<DevelopersType> developersProvider = new ValueProvider<DevelopersType>() {
      public DevelopersType getValue() {
        return model.getDevelopers();
      }

      public DevelopersType create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        DevelopersType developers = PomFactory.eINSTANCE.createDevelopersType();
        Command createDevelopersCommand = SetCommand.create(editingDomain, model,
            POM_PACKAGE.getModel_Developers(), developers);
        compoundCommand.append(createDevelopersCommand);
        return developers;
      }
    };

    ValueProvider<ContributorsType> contributorsProvider = new ValueProvider<ContributorsType>() {
      public ContributorsType getValue() {
        return model.getContributors();
      }

      public ContributorsType create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        ContributorsType contributors = PomFactory.eINSTANCE.createContributorsType();
        Command createContributorsCommand = SetCommand.create(editingDomain, model,
            POM_PACKAGE.getModel_Contributors(), contributors);
        compoundCommand.append(createContributorsCommand);
        return contributors;
      }
    };

    teamComposite.loadData(this, developersProvider, contributorsProvider);
  }
  
  public void updateView(Notification notification) {
    if(!isActive()) {
      return;
    }

    if(teamComposite!=null) {
      teamComposite.updateView(this, notification);
    }
  }
}
