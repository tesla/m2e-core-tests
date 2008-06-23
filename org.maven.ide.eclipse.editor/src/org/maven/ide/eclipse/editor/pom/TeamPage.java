/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import java.util.List;

import static org.maven.ide.eclipse.editor.pom.FormUtils.nvl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.maven.ide.components.pom.Contributor;
import org.maven.ide.components.pom.ContributorsType;
import org.maven.ide.components.pom.Developer;
import org.maven.ide.components.pom.DevelopersType;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.editor.composites.ListEditorComposite;
import org.maven.ide.eclipse.editor.composites.ListEditorContentProvider;
import org.maven.ide.eclipse.editor.composites.StringLabelProvider;
import org.maven.ide.eclipse.wizards.WidthGroup;


/**
 * @author Eugene Kuleshov
 */
public class TeamPage extends MavenPomEditorPage {

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
  

  public TeamPage(MavenPomEditor pomEditor) {
    super(pomEditor, MavenPlugin.PLUGIN_ID + ".pom.team", "Team");
  }

  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();
    ScrolledForm form = managedForm.getForm();
    form.setText("Team (work in progress)");

    Composite body = form.getBody();
    toolkit.paintBordersFor(body);
    GridLayout gridLayout = new GridLayout(1, true);
    body.setLayout(gridLayout);

    SashForm horizontalSash = new SashForm(body, SWT.NONE);
    horizontalSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    toolkit.adapt(horizontalSash, true, true);

    SashForm verticalSash = new SashForm(horizontalSash, SWT.VERTICAL);
    toolkit.adapt(verticalSash, true, true);

    createDevelopersSection(toolkit, verticalSash);
    createContributorsSection(toolkit, verticalSash);

    verticalSash.setWeights(new int[] {1, 1});

    createDetailsPanel(toolkit, horizontalSash);

    horizontalSash.setWeights(new int[] {1, 1});

//    form.pack();

    super.createFormContent(managedForm);
  }

  private void createDevelopersSection(FormToolkit toolkit, SashForm verticalSash) {
    Section developersSection = toolkit.createSection(verticalSash, Section.TITLE_BAR);
    developersSection.setText("Developers");

    developersEditor = new ListEditorComposite<Developer>(developersSection, SWT.NONE);
    toolkit.paintBordersFor(developersEditor);
    toolkit.adapt(developersEditor);
    developersSection.setClient(developersEditor);

    developersEditor.setContentProvider(new ListEditorContentProvider<Developer>());
    developersEditor.setLabelProvider(new LabelProvider() {
      public String getText(Object element) {
        if(element instanceof Developer) {
          Developer developer = (Developer) element;
          String label = developer.getId();
          if(developer.getName() != null) {
            label += " : " + developer.getName();
          }
          if(developer.getEmail() != null) {
            label += " : " + developer.getEmail();
          }
          return label;
        }
        return "";
      }

      public Image getImage(Object element) {
        return MavenEditorImages.IMG_PERSON;
      }
    });

    developersEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Developer> selection = developersEditor.getSelection();
        updateDeveloperDetails(selection.size() == 1 ? selection.get(0) : null);
      }
    });
    
    // XXX implement actions
  }

  private void createContributorsSection(FormToolkit toolkit, SashForm verticalSash) {
    Section contributorsSection = toolkit.createSection(verticalSash, Section.TITLE_BAR);
    contributorsSection.setText("Contributors");

    contributorsEditor = new ListEditorComposite<Contributor>(contributorsSection, SWT.NONE);
    toolkit.paintBordersFor(contributorsEditor);
    toolkit.adapt(contributorsEditor);
    contributorsSection.setClient(contributorsEditor);

    contributorsEditor.setContentProvider(new ListEditorContentProvider<Contributor>());
    contributorsEditor.setLabelProvider(new LabelProvider() {
      public String getText(Object element) {
        if(element instanceof Contributor) {
          Contributor contributor = (Contributor) element;
          String label = contributor.getName();
          if(contributor.getEmail() != null) {
            label += " : " + contributor.getEmail();
          }
          return label;
        }
        return "";
      }

      public Image getImage(Object element) {
        return MavenEditorImages.IMG_PERSON;
      }
    });

    contributorsEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Contributor> selection = contributorsEditor.getSelection();
        updateContributorDetails(selection.size()==1 ? selection.get(0) : null);
      }
    });
    
    // XXX implement actions
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

  public void loadData() {
    DevelopersType developers = model.getDevelopers();
    loadeDevelopers(developers);

    ContributorsType contributors = model.getContributors();
    loadContributors(contributors);

    updateDeveloperDetails(null);
    updateContributorDetails(null);
  }

  private void loadContributors(ContributorsType contributors) {
    contributorsEditor.setInput(contributors == null ? null : contributors.getContributor());
  }

  private void loadeDevelopers(DevelopersType developers) {
    developersEditor.setInput(developers == null ? null : developers.getDeveloper());
  }

  public void updateView(Notification notification) {
    if(!isActive()) {
      return;
    }
    
    // TODO Auto-generated method stub

  }

  protected void updateDeveloperDetails(Developer developer) {
    if(developer == null) {
      FormUtils.setEnabled(detailsComposite, false);

      userIdText.setText("");
      userNameText.setText("");
      userEmailText.setText("");
      userUrlText.setText("");
      userTimezoneText.setText("");

      organizationNameText.setText("");
      organizationUrlText.setText("");

      rolesEditor.setInput(null);

      return;
    }

    FormUtils.setEnabled(detailsComposite, true);
    FormUtils.setReadonly(detailsComposite, isReadOnly());

    userIdText.setText(nvl(developer.getId()));
    userNameText.setText(nvl(developer.getName()));
    userEmailText.setText(nvl(developer.getEmail()));
    userUrlText.setText(nvl(developer.getUrl()));
    userTimezoneText.setText(nvl(developer.getTimezone()));

    organizationNameText.setText(nvl(developer.getOrganization()));
    organizationUrlText.setText(nvl(developer.getOrganizationUrl()));

    rolesEditor.setInput(developer.getRoles() == null ? null : developer.getRoles().getRole());
    // propertiesEditor.setInput(...);
  }

  private void updateContributorDetails(Contributor contributor) {
    if(contributor == null) {
      FormUtils.setEnabled(detailsComposite, false);

      userIdText.setText("");
      userNameText.setText("");
      userEmailText.setText("");
      userUrlText.setText("");
      userTimezoneText.setText("");

      organizationNameText.setText("");
      organizationUrlText.setText("");

      rolesEditor.setInput(null);

      return;
    }

    FormUtils.setEnabled(detailsComposite, true);
    FormUtils.setReadonly(detailsComposite, isReadOnly());

    userIdLabel.setEnabled(false);
    userIdText.setEnabled(false);
    userIdText.setText("");
    
    userNameText.setText(nvl(contributor.getName()));
    userEmailText.setText(nvl(contributor.getEmail()));
    userUrlText.setText(nvl(contributor.getUrl()));
    userTimezoneText.setText(nvl(contributor.getTimezone()));

    organizationNameText.setText(nvl(contributor.getOrganization()));
    organizationUrlText.setText(nvl(contributor.getOrganizationUrl()));

    rolesEditor.setInput(contributor.getRoles() == null ? null : contributor.getRoles().getRole());
    // propertiesEditor.setInput(...);
  }
  
}
