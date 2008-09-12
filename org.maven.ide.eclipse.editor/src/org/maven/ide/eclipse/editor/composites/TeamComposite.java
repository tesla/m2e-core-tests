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
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.viewers.ICellModifier;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.maven.ide.components.pom.Contributor;
import org.maven.ide.components.pom.ContributorsType;
import org.maven.ide.components.pom.Developer;
import org.maven.ide.components.pom.DevelopersType;
import org.maven.ide.components.pom.PomFactory;
import org.maven.ide.components.pom.PomPackage;
import org.maven.ide.components.pom.Properties;
import org.maven.ide.components.pom.Roles;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.editor.pom.FormUtils;
import org.maven.ide.eclipse.editor.pom.MavenPomEditorPage;
import org.maven.ide.eclipse.editor.pom.PropertiesSection;
import org.maven.ide.eclipse.editor.pom.ValueProvider;
import org.maven.ide.eclipse.wizards.WidthGroup;


/**
 * @author Dmitry Platonoff
 */
public class TeamComposite extends Composite {

  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;

  private FormToolkit toolkit = new FormToolkit(Display.getCurrent());

  MavenPomEditorPage parent;

  // controls

  ValueProvider<DevelopersType> developersProvider;
  
  ValueProvider<ContributorsType> contributorsProvider;
  
  ListEditorComposite<Developer> developersEditor;

  ListEditorComposite<Contributor> contributorsEditor;

  Composite detailsComposite;

  Text userIdText;

  Text userNameText;

  Text userEmailText;

  Text userUrlText;

  CCombo userTimezoneText;

  Text organizationNameText;

  Text organizationUrlText;

  ListEditorComposite<String> rolesEditor;

  Label userIdLabel;

  // model
  EObject currentSelection;
  
  boolean changingSelection = false;

  private PropertiesSection propertiesSection;

  public TeamComposite(MavenPomEditorPage editorPage, Composite composite, int flags) {
    super(composite, flags);
    this.parent = editorPage;
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
    Section developersSection = toolkit.createSection(verticalSash, ExpandableComposite.TITLE_BAR);
    developersSection.setText("Developers");

    developersEditor = new ListEditorComposite<Developer>(developersSection, SWT.NONE);

    developersEditor.setContentProvider(new ListEditorContentProvider<Developer>());
    developersEditor.setLabelProvider(new TeamLabelProvider());

    developersEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        DevelopersType developers = developersProvider.getValue();
        boolean developersCreated = false;
        if(developers == null) {
          developers = developersProvider.create(editingDomain, compoundCommand);
          developersCreated = true;
        }

        Developer developer = PomFactory.eINSTANCE.createDeveloper();
        Command addDependencyCommand = AddCommand.create(editingDomain, developers, POM_PACKAGE
            .getDevelopersType_Developer(), developer);
        compoundCommand.append(addDependencyCommand);

        editingDomain.getCommandStack().execute(compoundCommand);

        if(developersCreated) {
          loadDevelopers();
        }
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
    Section contributorsSection = toolkit.createSection(verticalSash, ExpandableComposite.TITLE_BAR);
    contributorsSection.setText("Contributors");

    contributorsEditor = new ListEditorComposite<Contributor>(contributorsSection, SWT.NONE);
    contributorsEditor.setContentProvider(new ListEditorContentProvider<Contributor>());
    contributorsEditor.setLabelProvider(new TeamLabelProvider());

    contributorsEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        ContributorsType contributors = contributorsProvider.getValue();
        boolean contributorsCreated = false;
        if(contributors == null) {
          contributors = contributorsProvider.create(editingDomain, compoundCommand);
          contributorsCreated = true;
        }

        Contributor contributor = PomFactory.eINSTANCE.createContributor();
        Command addDependencyCommand = AddCommand.create(editingDomain, contributors, POM_PACKAGE
            .getContributorsType_Contributor(), contributor);
        compoundCommand.append(addDependencyCommand);

        editingDomain.getCommandStack().execute(compoundCommand);

        if(contributorsCreated) {
          loadContributors();
        }
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

    Section userDetailsSection = toolkit.createSection(detailsComposite, ExpandableComposite.TITLE_BAR);
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

    Section organizationSection = toolkit.createSection(detailsComposite, ExpandableComposite.TITLE_BAR);
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
    userDetailsComposite.setTabList(new Control[] {userIdText, userNameText, userEmailText, userUrlText, userTimezoneText});
    organizationComposite.addControlListener(widthGroup);
    organizationComposite.setTabList(new Control[] {organizationNameText, organizationUrlText});

    createRolesSection(toolkit, detailsComposite);
    createPropertiesSection(toolkit, detailsComposite);
  }

  private void createRolesSection(FormToolkit toolkit, Composite detailsComposite) {
    Section rolesSection = toolkit.createSection(detailsComposite, ExpandableComposite.TITLE_BAR);
    rolesSection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    rolesSection.setText("Roles");

    rolesEditor = new ListEditorComposite<String>(rolesSection, SWT.NONE);
    toolkit.paintBordersFor(rolesEditor);
    toolkit.adapt(rolesEditor);
    rolesSection.setClient(rolesEditor);

    rolesEditor.setContentProvider(new ListEditorContentProvider<String>());
    rolesEditor.setLabelProvider(new StringLabelProvider(MavenEditorImages.IMG_ROLE));

    rolesEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        Roles roles = getRoles();
        if(roles == null) {
          roles = PomFactory.eINSTANCE.createRoles();
          Command createRolesCommand = SetCommand.create(editingDomain, currentSelection,
              currentSelection instanceof Contributor ? POM_PACKAGE.getContributor_Roles() : POM_PACKAGE
                  .getDeveloper_Roles(), roles);
          compoundCommand.append(createRolesCommand);
        }

        Command addRoleCommand = AddCommand.create(editingDomain, roles, POM_PACKAGE.getRoles_Role(), "?");
        compoundCommand.append(addRoleCommand);

        editingDomain.getCommandStack().execute(compoundCommand);

        updateRoles(roles);
      }
    });

    rolesEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        Roles roles = getRoles();
        List<String> roleList = rolesEditor.getSelection();
        for(String role : roleList) {
          Command removeCommand = RemoveCommand.create(editingDomain, roles, POM_PACKAGE.getRoles_Role(), role);
          compoundCommand.append(removeCommand);
        }

        editingDomain.getCommandStack().execute(compoundCommand);
      }
    });

    rolesEditor.setCellModifier(new ICellModifier() {
      public boolean canModify(Object element, String property) {
        return true;
      }
 
      public Object getValue(Object element, String property) {
        return element;
      }
 
      public void modify(Object element, String property, Object value) {
        int n = rolesEditor.getViewer().getTable().getSelectionIndex();
        if(!value.equals(getRoles().getRole().get(n))) {
          EditingDomain editingDomain = parent.getEditingDomain();
          Command command = SetCommand.create(editingDomain, getRoles(),
              POM_PACKAGE.getRoles_Role(), value, n);
          editingDomain.getCommandStack().execute(command);
        }
      }
    });

  }

  private void createPropertiesSection(FormToolkit toolkit, Composite composite) {
    propertiesSection = new PropertiesSection(toolkit, composite, parent.getEditingDomain());
  }

  public void loadContributors() {
    ContributorsType contributors = contributorsProvider.getValue();
    changingSelection = true;
    contributorsEditor.setInput(contributors == null ? null : contributors.getContributor());
    changingSelection = false;
  }

  void loadDevelopers() {
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

    Roles roles = null;
    if(eo instanceof Contributor) {
      Contributor contributor = (Contributor) eo;
      updateContributorDetails(contributor);
      roles = contributor.getRoles();
      propertiesSection.setModel(contributor, POM_PACKAGE.getContributor_Properties());
    } else if(eo instanceof Developer) {
      Developer developer = (Developer) eo;
      updateDeveloperDetails(developer);
      roles = developer.getRoles();
      propertiesSection.setModel(developer, POM_PACKAGE.getDeveloper_Properties());
    }

    parent.registerListeners();

    updateRoles(roles);
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

    ValueProvider<Developer> developerProvider = new ValueProvider.DefaultValueProvider<Developer>(developer);
    parent.setModifyListener(userIdText, developerProvider, POM_PACKAGE.getDeveloper_Id(), "");
    parent.setModifyListener(userNameText, developerProvider, POM_PACKAGE.getDeveloper_Name(), "");
    parent.setModifyListener(userEmailText, developerProvider, POM_PACKAGE.getDeveloper_Email(), "");
    parent.setModifyListener(userUrlText, developerProvider, POM_PACKAGE.getDeveloper_Url(), "");
    parent.setModifyListener(userTimezoneText, developerProvider, POM_PACKAGE.getDeveloper_Timezone(), "");
    parent.setModifyListener(organizationNameText, developerProvider, POM_PACKAGE.getDeveloper_Organization(), "");
    parent.setModifyListener(organizationUrlText, developerProvider, POM_PACKAGE.getDeveloper_OrganizationUrl(), "");
  }

  public void updateView(Notification notification) {
    EObject object = (EObject) notification.getNotifier();

    if(object instanceof DevelopersType) {
      developersEditor.refresh();
    } else if(object instanceof ContributorsType) {
      contributorsEditor.refresh();
    } else {
      Object notificationObject = MavenPomEditorPage.getFromNotification(notification);
      if(object instanceof Contributor) {
        contributorsEditor.refresh();

        if(object == currentSelection && (notificationObject == null || notificationObject instanceof EObject)) {
          updateDetails((EObject) notificationObject);
        }
      } else if(object instanceof Developer) {
        developersEditor.refresh();

        if(object == currentSelection && (notificationObject == null || notificationObject instanceof EObject)) {
          updateDetails((EObject) notificationObject);
        }
      } else if(object instanceof Roles) {
        if(object == getRoles() && (notificationObject == null || notificationObject instanceof Roles)) {
          updateRoles((Roles) notificationObject);
        }
      }
    }
  }

  public void loadData(ValueProvider<DevelopersType> developersProvider,
      ValueProvider<ContributorsType> contributorsProvider) {
    this.developersProvider = developersProvider;
    this.contributorsProvider = contributorsProvider;
    loadDevelopers();
    loadContributors();

    developersEditor.setReadOnly(parent.isReadOnly());
    contributorsEditor.setReadOnly(parent.isReadOnly());

    updateDetails(null);
  }

  protected Roles getRoles() {
    if(currentSelection != null) {
      if(currentSelection instanceof Contributor) {
        return ((Contributor) currentSelection).getRoles();
      } else if(currentSelection instanceof Developer) {
        return ((Developer) currentSelection).getRoles();
      }
    }
    return null;
  }
  
  protected void updateRoles(Roles roles) {
    rolesEditor.setInput(roles == null ? null : roles.getRole());
  }

  protected Properties getProperties() {
    if(currentSelection != null) {
      if(currentSelection instanceof Contributor) {
        return ((Contributor) currentSelection).getProperties();
      } else if(currentSelection instanceof Developer) {
        return ((Developer) currentSelection).getProperties();
      }
    }
    return null;
  }
}
