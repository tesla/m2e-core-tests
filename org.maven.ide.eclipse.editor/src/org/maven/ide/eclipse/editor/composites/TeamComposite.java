/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.composites;

import static org.maven.ide.eclipse.editor.pom.FormUtils.setText;

import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.maven.ide.components.pom.Contributor;
import org.maven.ide.components.pom.ContributorsType;
import org.maven.ide.components.pom.Developer;
import org.maven.ide.components.pom.DevelopersType;
import org.maven.ide.components.pom.PomFactory;
import org.maven.ide.components.pom.PomPackage;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.editor.pom.FormUtils;
import org.maven.ide.eclipse.editor.pom.MavenPomEditorPage;
import org.maven.ide.eclipse.editor.pom.PropertyPair;
import org.maven.ide.eclipse.editor.pom.PropertyPairLabelProvider;
import org.maven.ide.eclipse.editor.pom.ValueProvider;
import org.maven.ide.eclipse.wizards.WidthGroup;


/**
 * @author Dmitry Platonoff
 */
public class TeamComposite extends Composite {

  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;

  private FormToolkit toolkit = new FormToolkit(Display.getCurrent());

  protected MavenPomEditorPage parent;

  ValueProvider<DevelopersType> developersProvider;

  ValueProvider<ContributorsType> contributorsProvider;

  EObject currentSelection;

  boolean changingSelection = false;

  // controls
  private ListEditorComposite<Developer> developersEditor;

  private ListEditorComposite<Contributor> contributorsEditor;

  private Composite detailsComposite;

  private Text userIdText;

  private Text userNameText;

  private Text userEmailText;

  private Text userUrlText;

  private CCombo userTimezoneText;

  private Text organizationNameText;

  private Text organizationUrlText;

  private ListEditorComposite<PropertyPair> propertiesEditor;

  private ListEditorComposite<String> rolesEditor;

  private Label userIdLabel;

  public TeamComposite(Composite composite, int flags) {
    super(composite, flags);

    createComposite();
  }

  private void createComposite() {
    GridLayout gridLayout = new GridLayout();
    gridLayout.makeColumnsEqualWidth = true;
    gridLayout.marginWidth = 0;
    setLayout(gridLayout);
    toolkit.adapt(this);

    SashForm horizontalSash = new SashForm(this, SWT.NONE);
    horizontalSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    toolkit.adapt(horizontalSash, true, true);

    SashForm verticalSash = new SashForm(horizontalSash, SWT.VERTICAL);
    toolkit.adapt(verticalSash, true, true);

    createDevelopersSection(toolkit, verticalSash);
    createContributorsSection(toolkit, verticalSash);

    verticalSash.setWeights(new int[] {1, 1});

    createDetailsPanel(toolkit, horizontalSash);

    horizontalSash.setWeights(new int[] {1, 1});
  }

  private void createDevelopersSection(FormToolkit toolkit, SashForm verticalSash) {
    Section developersSection = toolkit.createSection(verticalSash, Section.TITLE_BAR);
    developersSection.setText("Developers");

    developersEditor = new ListEditorComposite<Developer>(developersSection, SWT.NONE);

    developersEditor.setContentProvider(new ListEditorContentProvider<Developer>());
    developersEditor.setLabelProvider(new TeamLabelProvider());

    developersEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        DevelopersType developers = developersProvider.getValue();
        if(developers == null) {
          developers = developersProvider.create(editingDomain, compoundCommand);
        }

        Developer developer = PomFactory.eINSTANCE.createDeveloper();
        Command addDependencyCommand = AddCommand.create(editingDomain, developers, POM_PACKAGE
            .getDevelopersType_Developer(), developer);
        compoundCommand.append(addDependencyCommand);

        editingDomain.getCommandStack().execute(compoundCommand);

        developersEditor.setSelection(Collections.singletonList(developer));
        updateDetails(developer);
        userIdText.setFocus();
      }
    });

    developersEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        List<Developer> developerList = developersEditor.getSelection();
        for(Developer developer : developerList) {
          Command removeCommand = RemoveCommand.create(editingDomain, developersProvider.getValue(), POM_PACKAGE
              .getDevelopersType_Developer(), developer);
          compoundCommand.append(removeCommand);
        }

        editingDomain.getCommandStack().execute(compoundCommand);
        updateDetails(null);
      }
    });

    developersEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Developer> selection = developersEditor.getSelection();
        updateDetails(selection.size() == 1 ? selection.get(0) : null);

        if(!selection.isEmpty()) {
          changingSelection = true;
          try {
            contributorsEditor.setSelection(Collections.<Contributor> emptyList());
          } finally {
            changingSelection = false;
          }
        }
      }
    });

    developersSection.setClient(developersEditor);
    toolkit.paintBordersFor(developersEditor);
    toolkit.adapt(developersEditor);
  }

  private void createContributorsSection(FormToolkit toolkit, SashForm verticalSash) {
    Section contributorsSection = toolkit.createSection(verticalSash, Section.TITLE_BAR);
    contributorsSection.setText("Contributors");

    contributorsEditor = new ListEditorComposite<Contributor>(contributorsSection, SWT.NONE);
    contributorsEditor.setContentProvider(new ListEditorContentProvider<Contributor>());
    contributorsEditor.setLabelProvider(new TeamLabelProvider());

    contributorsEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        ContributorsType contributors = contributorsProvider.getValue();
        if(contributors == null) {
          contributors = contributorsProvider.create(editingDomain, compoundCommand);
        }

        Contributor contributor = PomFactory.eINSTANCE.createContributor();
        Command addDependencyCommand = AddCommand.create(editingDomain, contributors, POM_PACKAGE
            .getContributorsType_Contributor(), contributor);
        compoundCommand.append(addDependencyCommand);

        editingDomain.getCommandStack().execute(compoundCommand);

        contributorsEditor.setSelection(Collections.singletonList(contributor));
        updateDetails(contributor);
        userNameText.setFocus();
      }
    });

    contributorsEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        List<Contributor> contributorList = contributorsEditor.getSelection();
        for(Contributor contributor : contributorList) {
          Command removeCommand = RemoveCommand.create(editingDomain, contributorsProvider.getValue(), POM_PACKAGE
              .getContributorsType_Contributor(), contributor);
          compoundCommand.append(removeCommand);
        }

        editingDomain.getCommandStack().execute(compoundCommand);
        updateDetails(null);
      }
    });

    contributorsEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Contributor> selection = contributorsEditor.getSelection();
        updateDetails(selection.size() == 1 ? selection.get(0) : null);

        if(!selection.isEmpty()) {
          changingSelection = true;
          try {
            developersEditor.setSelection(Collections.<Developer> emptyList());
          } finally {
            changingSelection = false;
          }
        }
      }
    });

    contributorsSection.setClient(contributorsEditor);
    toolkit.paintBordersFor(contributorsEditor);
    toolkit.adapt(contributorsEditor);
  }

  private void createDetailsPanel(FormToolkit toolkit, SashForm horizontalSash) {
    detailsComposite = toolkit.createComposite(horizontalSash, SWT.NONE);
    GridLayout detailsCompositeGridLayout = new GridLayout();
    detailsCompositeGridLayout.marginLeft = 5;
    detailsCompositeGridLayout.marginWidth = 0;
    detailsCompositeGridLayout.marginHeight = 0;
    detailsComposite.setLayout(detailsCompositeGridLayout);
    toolkit.paintBordersFor(detailsComposite);

    Section userDetailsSection = toolkit.createSection(detailsComposite, Section.TITLE_BAR);
    GridData gd_userDetailsSection = new GridData(SWT.FILL, SWT.CENTER, true, false);
    userDetailsSection.setLayoutData(gd_userDetailsSection);
    userDetailsSection.setText("Details");

    Composite userDetailsComposite = toolkit.createComposite(userDetailsSection, SWT.NONE);
    userDetailsComposite.setLayout(new GridLayout(2, false));
    toolkit.paintBordersFor(userDetailsComposite);
    userDetailsSection.setClient(userDetailsComposite);

    userIdLabel = toolkit.createLabel(userDetailsComposite, "Id:", SWT.NONE);

    userIdText = toolkit.createText(userDetailsComposite, null, SWT.NONE);
    userIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label userNameLabel = toolkit.createLabel(userDetailsComposite, "Name:", SWT.NONE);

    userNameText = toolkit.createText(userDetailsComposite, null, SWT.NONE);
    userNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label userEmailLabel = toolkit.createLabel(userDetailsComposite, "Email:", SWT.NONE);

    userEmailText = toolkit.createText(userDetailsComposite, null, SWT.NONE);
    userEmailText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Hyperlink userUrlLabel = toolkit.createHyperlink(userDetailsComposite, "URL:", SWT.NONE);
    userUrlLabel.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(userUrlText.getText());
      }
    });

    userUrlText = toolkit.createText(userDetailsComposite, null, SWT.NONE);
    userUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label userTimezoneLabel = toolkit.createLabel(userDetailsComposite, "Timezone:", SWT.NONE);

    userTimezoneText = new CCombo(userDetailsComposite, SWT.FLAT);
    userTimezoneText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    userTimezoneText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);

    Section organizationSection = toolkit.createSection(detailsComposite, Section.TITLE_BAR);
    organizationSection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    organizationSection.setText("Organization");

    Composite organizationComposite = toolkit.createComposite(organizationSection, SWT.NONE);
    organizationComposite.setLayout(new GridLayout(2, false));
    toolkit.paintBordersFor(organizationComposite);
    organizationSection.setClient(organizationComposite);

    Label organizationNameLabel = toolkit.createLabel(organizationComposite, "Name:", SWT.NONE);

    organizationNameText = toolkit.createText(organizationComposite, null, SWT.NONE);
    organizationNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Hyperlink organizationUrlLabel = toolkit.createHyperlink(organizationComposite, "URL:", SWT.NONE);
    organizationUrlLabel.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(organizationUrlText.getText());
      }
    });

    organizationUrlText = toolkit.createText(organizationComposite, null, SWT.NONE);
    organizationUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    WidthGroup widthGroup = new WidthGroup();
    widthGroup.addControl(userIdLabel);
    widthGroup.addControl(userNameLabel);
    widthGroup.addControl(userEmailLabel);
    widthGroup.addControl(userUrlLabel);
    widthGroup.addControl(userTimezoneLabel);
    widthGroup.addControl(organizationNameLabel);
    widthGroup.addControl(organizationUrlLabel);
    userDetailsComposite.addControlListener(widthGroup);
    organizationComposite.addControlListener(widthGroup);

    createRolesSection(toolkit, detailsComposite);
    createPropertiesSection(toolkit, detailsComposite);
  }

  private void createRolesSection(FormToolkit toolkit, Composite detailsComposite) {
    Section rolesSection = toolkit.createSection(detailsComposite, Section.TITLE_BAR);
    rolesSection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    rolesSection.setText("Roles");

    rolesEditor = new ListEditorComposite<String>(rolesSection, SWT.NONE);
    toolkit.paintBordersFor(rolesEditor);
    toolkit.adapt(rolesEditor);
    rolesSection.setClient(rolesEditor);

    rolesEditor.setContentProvider(new ListEditorContentProvider<String>());
    rolesEditor.setLabelProvider(new StringLabelProvider(MavenEditorImages.IMG_ROLE));

    // XXX implement actions
  }

  private void createPropertiesSection(FormToolkit toolkit, Composite parent) {
    Section propertiesSection = toolkit.createSection(parent, Section.TITLE_BAR);
    propertiesSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
    propertiesSection.setText("Properties");

    propertiesEditor = new ListEditorComposite<PropertyPair>(propertiesSection, SWT.NONE);
    toolkit.paintBordersFor(propertiesEditor);
    toolkit.adapt(propertiesEditor);
    propertiesSection.setClient(propertiesEditor);

    propertiesEditor.setContentProvider(new ListEditorContentProvider<PropertyPair>());
    propertiesEditor.setLabelProvider(new PropertyPairLabelProvider());

    // XXX implement actions
  }

  public void loadContributors() {
    ContributorsType contributors = contributorsProvider.getValue();
    changingSelection = true;
    contributorsEditor.setInput(contributors == null ? null : contributors.getContributor());
    changingSelection = false;
  }

  private void loadDevelopers() {
    DevelopersType developers = developersProvider.getValue();
    changingSelection = true;
    developersEditor.setInput(developers == null ? null : developers.getDeveloper());
    changingSelection = false;
  }

  protected void updateDetails(EObject eo) {
    if(changingSelection) {
      return;
    }

    this.currentSelection = eo;

    if(parent != null) {
      parent.removeNotifyListener(userIdText);
      parent.removeNotifyListener(userNameText);
      parent.removeNotifyListener(userEmailText);
      parent.removeNotifyListener(userUrlText);
      parent.removeNotifyListener(userTimezoneText);
      parent.removeNotifyListener(organizationNameText);
      parent.removeNotifyListener(organizationUrlText);
    }

    if(parent == null || eo == null) {
      FormUtils.setEnabled(detailsComposite, false);

      setText(userIdText, "");
      setText(userNameText, "");
      setText(userEmailText, "");
      setText(userUrlText, "");
      setText(userTimezoneText, "");

      setText(organizationNameText, "");
      setText(organizationUrlText, "");

      rolesEditor.setInput(null);

      return;
    }

    FormUtils.setEnabled(detailsComposite, true);
    FormUtils.setReadonly(detailsComposite, parent.isReadOnly());

    if(eo instanceof Contributor) {
      updateContributorDetails((Contributor) eo);
    } else if(eo instanceof Developer) {
      updateDeveloperDetails((Developer) eo);
    }

    parent.registerListeners();

//    rolesEditor.setInput(contributor.getRoles() == null ? null : contributor.getRoles().getRole());
    // propertiesEditor.setInput(...);
  }

  protected void updateContributorDetails(Contributor contributor) {
    setText(userIdText, "");
    setText(userNameText, contributor.getName());
    setText(userEmailText, contributor.getEmail());
    setText(userUrlText, contributor.getUrl());
    setText(userTimezoneText, contributor.getTimezone());
    setText(organizationNameText, contributor.getOrganization());
    setText(organizationUrlText, contributor.getOrganizationUrl());

    userIdLabel.setEnabled(false);
    userIdText.setEnabled(false);

    ValueProvider<Contributor> contributorProvider = new ValueProvider.DefaultValueProvider<Contributor>(contributor);
    parent.setModifyListener(userNameText, contributorProvider, POM_PACKAGE.getContributor_Name(), "");
    parent.setModifyListener(userEmailText, contributorProvider, POM_PACKAGE.getContributor_Email(), "");
    parent.setModifyListener(userUrlText, contributorProvider, POM_PACKAGE.getContributor_Url(), "");
    parent.setModifyListener(userTimezoneText, contributorProvider, POM_PACKAGE.getContributor_Timezone(), "");
    parent.setModifyListener(organizationNameText, contributorProvider, POM_PACKAGE.getContributor_Organization(), "");
    parent
        .setModifyListener(organizationUrlText, contributorProvider, POM_PACKAGE.getContributor_OrganizationUrl(), "");
  }

  protected void updateDeveloperDetails(Developer developer) {
    setText(userIdText, developer.getId());
    setText(userNameText, developer.getName());
    setText(userEmailText, developer.getEmail());
    setText(userUrlText, developer.getUrl());
    setText(userTimezoneText, developer.getTimezone());
    setText(organizationNameText, developer.getOrganization());
    setText(organizationUrlText, developer.getOrganizationUrl());

    ValueProvider<Developer> developerProvider = new ValueProvider.DefaultValueProvider<Developer>(
        (Developer) developer);
    parent.setModifyListener(userIdText, developerProvider, POM_PACKAGE.getDeveloper_Id(), "");
    parent.setModifyListener(userNameText, developerProvider, POM_PACKAGE.getDeveloper_Name(), "");
    parent.setModifyListener(userEmailText, developerProvider, POM_PACKAGE.getDeveloper_Email(), "");
    parent.setModifyListener(userUrlText, developerProvider, POM_PACKAGE.getDeveloper_Url(), "");
    parent.setModifyListener(userTimezoneText, developerProvider, POM_PACKAGE.getDeveloper_Timezone(), "");
    parent.setModifyListener(organizationNameText, developerProvider, POM_PACKAGE.getDeveloper_Organization(), "");
    parent.setModifyListener(organizationUrlText, developerProvider, POM_PACKAGE.getDeveloper_OrganizationUrl(), "");
  }

  public void updateView(MavenPomEditorPage editorPage, Notification notification) {
    EObject object = (EObject) notification.getNotifier();

    // XXX event is not received when <dependencies> is deleted in XML
    if(object instanceof DevelopersType) {
//      // handle add/remove
//      Dependencies dependencies = (Dependencies) object;
//      if (model.getDependencies() == dependencies) {
//        // dependencies updated
//        List<Dependency> selection = getUpdatedSelection(dependencies, dependenciesEditor.getSelection());
//        loadDependencies(model);
//        dependenciesEditor.setSelection(selection);
//        updateDependencyDetails(selection.size()==1 ? selection.get(0) : null);
//      } else {
//        // dependencyManagement updated
//        List<Dependency> selection = dependencyManagementEditor.getSelection();
//        getUpdatedSelection(dependencies, selection);
//        loadDependencyManagement(model);
//        dependencyManagementEditor.setSelection(selection);
//        updateDependencyDetails(selection.size()==1 ? selection.get(0) : null);
//      }
      developersEditor.refresh();
//      contributorsEditor.refresh();
    } else if(object instanceof ContributorsType) {
      contributorsEditor.refresh();
    } else if(object instanceof Contributor) {
      contributorsEditor.refresh();

      if(object == currentSelection) {
        updateDetails(object);
      }
    } else if(object instanceof Developer) {
      developersEditor.refresh();

      if(object == currentSelection) {
        updateDetails(object);
      }
    }
/*    
    ExclusionsType exclusions = currentDependency==null ? null : currentDependency.getExclusions();
    if(object instanceof ExclusionsType) {
      exclusionsEditor.refresh();
      if(exclusions == object) {
        updateDependencyDetails(currentDependency);
      }
    }
    
    if(object instanceof Exclusion) {
      exclusionsEditor.refresh();
      if(currentExclusion == object) {
        updateExclusionDetails((Exclusion) object);
      }
    }
*/
  }

  public void loadData(MavenPomEditorPage editorPage, ValueProvider<DevelopersType> developersProvider,
      ValueProvider<ContributorsType> contributorsProvider) {
    parent = editorPage;

    this.developersProvider = developersProvider;
    this.contributorsProvider = contributorsProvider;
    loadDevelopers();
    loadContributors();

    developersEditor.setReadOnly(parent.isReadOnly());
    contributorsEditor.setReadOnly(parent.isReadOnly());

    updateDetails(null);
  }

}
