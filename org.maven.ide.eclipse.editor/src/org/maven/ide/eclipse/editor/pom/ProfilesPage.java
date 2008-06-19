/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import static org.maven.ide.eclipse.editor.pom.FormUtils.nvl;

import java.util.List;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.maven.ide.components.pom.Activation;
import org.maven.ide.components.pom.ActivationFile;
import org.maven.ide.components.pom.ActivationOS;
import org.maven.ide.components.pom.ActivationProperty;
import org.maven.ide.components.pom.BuildBase;
import org.maven.ide.components.pom.Dependencies;
import org.maven.ide.components.pom.Profile;
import org.maven.ide.components.pom.ProfilesType;
import org.maven.ide.components.pom.Reporting;
import org.maven.ide.components.pom.Repositories;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.editor.composites.BuildComposite;
import org.maven.ide.eclipse.editor.composites.DependenciesComposite;
import org.maven.ide.eclipse.editor.composites.ListEditorComposite;
import org.maven.ide.eclipse.editor.composites.ListEditorContentProvider;
import org.maven.ide.eclipse.editor.composites.PluginsComposite;
import org.maven.ide.eclipse.editor.composites.ReportingComposite;
import org.maven.ide.eclipse.editor.composites.RepositoriesComposite;
import org.maven.ide.eclipse.editor.composites.StringLabelProvider;
import org.maven.ide.eclipse.wizards.WidthGroup;

/**
 * @author Eugene Kuleshov
 */
public class ProfilesPage extends MavenPomEditorPage {

  // controls
  private Text activationFileMissingText;
  private Text activationFileExistText;
  private Text activationPropertyValueText;
  private Text activationPropertyNameText;
  private Text activationOsVersionText;
  private Text activationOsArchitectureText;
  private Text activationOsFamilyText;
  private Text activationOsNameText;
  private Text activationJdkText;
  private ListEditorComposite<Profile> profilesEditor;
  private ListEditorComposite<PropertyPair> propertiesEditor;
  private ListEditorComposite<String> modulesEditor;
  private Section modulesSection;
  private Section propertiesSection;
  
  private BuildComposite buildTabComposite;
  private PluginsComposite pluginsTabComposite;
  private DependenciesComposite dependenciesTabComposite;
  private RepositoriesComposite repositoriesTabComposite;
  private ReportingComposite reportingTabComposite;
  private Button activeByDefaultbutton;
  
  
  public ProfilesPage(MavenPomEditor pomEditor) {
    super(pomEditor, MavenPlugin.PLUGIN_ID + ".pom.profiles", "Profiles");
  }

  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();
    ScrolledForm form = managedForm.getForm();
    form.setText("Profiles (work in progress)");
    // form.setExpandHorizontal(true);
    
    Composite body = form.getBody();
    GridLayout gridLayout = new GridLayout(2, true);
    gridLayout.horizontalSpacing = 7;
    body.setLayout(gridLayout);
    toolkit.paintBordersFor(body);
  
    createProfilesSection(toolkit, body);

    createProfileDetailsSection(toolkit, body);
    createModulesSection(toolkit, body);
    createPropertiesSection(toolkit, body);

    CTabFolder tabFolder = new CTabFolder(body, SWT.FLAT | SWT.MULTI);
    GridData tabFolderData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
    tabFolderData.verticalIndent = 5;
    tabFolder.setLayoutData(tabFolderData);
    toolkit.adapt(tabFolder, true, true);
    
    toolkit.getColors().initializeSectionToolBarColors();
    Color selectedColor = toolkit.getColors().getColor("org.eclipse.ui.forms.TB_BG");
    tabFolder.setSelectionBackground(new Color[] {selectedColor, toolkit.getColors().getBackground()}, //
        new int[] {100}, true);

    tabFolder.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        // updateTabSelection();
      }
    });

    createActivationTab(tabFolder, toolkit);
    createDependenciesTab(tabFolder, toolkit);
    createRepositoriesTab(toolkit, tabFolder);
    createBuildTab(toolkit, tabFolder);
    createPluginsTab(toolkit, tabFolder);
    createReportsTab(toolkit, tabFolder);
    
    tabFolder.setSelection(0);

//    form.pack();

    super.createFormContent(managedForm);
  }

  private void createProfilesSection(FormToolkit toolkit, Composite body) {
    Section profilesSection = toolkit.createSection(body, Section.TITLE_BAR);
    profilesSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 3));
    profilesSection.setText("Profiles");
    profilesEditor = new ListEditorComposite<Profile>(profilesSection, SWT.NONE);
    profilesSection.setClient(profilesEditor);
    toolkit.adapt(profilesEditor);
    toolkit.paintBordersFor(profilesEditor);
    
    profilesEditor.setContentProvider(new ListEditorContentProvider<Profile>());
    profilesEditor.setLabelProvider(new LabelProvider() {
      public String getText(Object element) {
        if(element instanceof Profile) {
          String profileId = ((Profile) element).getId();
          return profileId==null || profileId.length()==0 ? "[unknown]" : profileId;
        }
        return super.getText(element);
      }
      
      public Image getImage(Object element) {
        return MavenEditorImages.IMG_PROFILE;
      }
    });
    
    profilesEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Profile> selection = profilesEditor.getSelection();
        updateProfileDetails(selection.size()==1 ? selection.get(0) : null);
      }
    });
  
    // XXX implement actions

    profilesEditor.setReadOnly(pomEditor.isReadOnly());
  }

  private void createProfileDetailsSection(FormToolkit toolkit, Composite body) {
  }

  private void createPropertiesSection(FormToolkit toolkit, Composite body) {
    // XXX implement actions

    propertiesEditor.setReadOnly(pomEditor.isReadOnly());
  }

  private void createModulesSection(FormToolkit toolkit, Composite body) {
    propertiesSection = toolkit.createSection(body, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
    propertiesSection.setText("Properties");
    GridData propertiesSectionData = new GridData(SWT.FILL, SWT.FILL, true, false);
    propertiesSection.setLayoutData(propertiesSectionData);
    
    propertiesEditor = new ListEditorComposite<PropertyPair>(propertiesSection, SWT.NONE);
    propertiesEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    propertiesSection.setClient(propertiesEditor);
    toolkit.paintBordersFor(propertiesEditor);
    toolkit.adapt(propertiesEditor);
    
    propertiesEditor.setContentProvider(new ListEditorContentProvider<PropertyPair>());
    propertiesEditor.setLabelProvider(new StringLabelProvider(MavenEditorImages.IMG_PROPERTY));
    
    FormUtils.setEnabled(propertiesSection, false);
    modulesSection = toolkit.createSection(body, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
    GridData modulesSectionData = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 2);
    modulesSection.setLayoutData(modulesSectionData);
    modulesSection.setText("Modules");

    modulesEditor = new ListEditorComposite<String>(modulesSection, SWT.NONE);
    modulesEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    modulesSection.setClient(modulesEditor);
    toolkit.adapt(modulesEditor);
    toolkit.paintBordersFor(modulesEditor);
    
    modulesEditor.setContentProvider(new ListEditorContentProvider<String>());
    modulesEditor.setLabelProvider(new ModulesLabelProvider(this));
    
    // XXX implement actions

    modulesEditor.setReadOnly(pomEditor.isReadOnly());
  }

  protected void updateProfileDetails(Profile profile) {
    if(profile==null) {
      FormUtils.setEnabled(propertiesSection, false);
      FormUtils.setEnabled(modulesSection, false);
      
      propertiesEditor.setInput(null);
      modulesEditor.setInput(null);
      updateProfileTabs(profile);
      
      return;
    }

    FormUtils.setEnabled(propertiesSection, true);
    FormUtils.setEnabled(modulesSection, true);
    
    FormUtils.setReadonly(propertiesSection, isReadOnly());
    FormUtils.setReadonly(modulesSection, isReadOnly());
    
    modulesEditor.setInput(profile.getModules()==null ? null : profile.getModules().getModule());
    propertiesEditor.setInput(profile.getProperties());

    updateProfileTabs(profile);
  }

  private void updateProfileTabs(Profile profile) {
    updateActivationTab(profile==null || profile.getActivation()==null ? null : profile.getActivation());
    updateDependenciesTab(profile==null || profile.getDependencies()==null ? null : profile.getDependencies());
    updateRepositoriesTab(profile==null || profile.getRepositories()==null ? null : profile.getRepositories());
    updateBuildTab(profile==null || profile.getBuild()==null ? null : profile.getBuild());
    updatePluginsTab(profile==null || profile.getBuild()==null ? null : profile.getBuild());
    updateReportsTab(profile==null || profile.getReporting()==null ? null : profile.getReporting());
  }

  private void updateActivationTab(Activation activation) {
    if(activation==null) {
      return;
    }
    
    activeByDefaultbutton.setSelection("true".equals(activation.getActiveByDefault()));
    activationJdkText.setText(nvl(activation.getJdk()));
    
    ActivationProperty property = activation.getProperty();
    if(property==null) {
      activationPropertyNameText.setText("");
      activationPropertyValueText.setText("");
    } else {
      activationPropertyNameText.setText(nvl(property.getName()));
      activationPropertyValueText.setText(nvl(property.getValue()));
    }
    
    ActivationFile file = activation.getFile();
    if(file==null) {
      activationFileExistText.setText("");
      activationFileMissingText.setText("");
    } else {
      activationFileExistText.setText(nvl(file.getExists()));
      activationFileMissingText.setText(nvl(file.getMissing()));
    }
    
    ActivationOS os = activation.getOs();
    if(os==null) {
      activationOsArchitectureText.setText("");
      activationOsFamilyText.setText("");
      activationOsNameText.setText("");
      activationOsVersionText.setText("");
    } else {
      activationOsArchitectureText.setText(nvl(os.getArch()));
      activationOsFamilyText.setText(nvl(os.getFamily()));
      activationOsNameText.setText(nvl(os.getName()));
      activationOsVersionText.setText(nvl(os.getVersion()));
    }
  }
  
  private void updateDependenciesTab(Dependencies dependencies) {
    // XXX need to pass Dependencies
    // dependenciesTabComposite.loadData(this);
  }

  private void updateRepositoriesTab(Repositories repositories) {
    // TODO Auto-generated method stub
    
  }

  private void updateBuildTab(BuildBase buildBase) {
    // TODO Auto-generated method stub
    
  }

  private void updatePluginsTab(BuildBase buildBase) {
    // TODO Auto-generated method stub
    
  }

  private void updateReportsTab(Reporting reporting) {
    // TODO Auto-generated method stub
    
  }

  private void createBuildTab(FormToolkit toolkit, CTabFolder tabFolder) {
    CTabItem buildTabItem = new CTabItem(tabFolder, SWT.NONE);
    buildTabItem.setText("Build");
  
    buildTabComposite = new BuildComposite(tabFolder, SWT.NONE);
    buildTabItem.setControl(buildTabComposite);
    toolkit.adapt(buildTabComposite);
  }

  private void createPluginsTab(FormToolkit toolkit, CTabFolder tabFolder) {
    CTabItem pluginsTabItem = new CTabItem(tabFolder, SWT.NONE);
    pluginsTabItem.setText("Plugins");

    pluginsTabComposite = new PluginsComposite(tabFolder, SWT.NONE);
    pluginsTabItem.setControl(pluginsTabComposite);
    toolkit.adapt(pluginsTabComposite);
  }

  private void createDependenciesTab(CTabFolder tabFolder, FormToolkit toolkit) {
    CTabItem dependenciesTabItem = new CTabItem(tabFolder, SWT.NONE);
    dependenciesTabItem.setText("Dependencies");
  
    dependenciesTabComposite = new DependenciesComposite(tabFolder, SWT.NONE);
    dependenciesTabItem.setControl(dependenciesTabComposite);
    toolkit.adapt(dependenciesTabComposite);
  }

  private void createRepositoriesTab(FormToolkit toolkit, CTabFolder tabFolder) {
    CTabItem repositoriesTabItem = new CTabItem(tabFolder, SWT.NONE);
    repositoriesTabItem.setText("Repositories");

    repositoriesTabComposite = new RepositoriesComposite(tabFolder, SWT.NONE);
    repositoriesTabItem.setControl(repositoriesTabComposite);
    toolkit.adapt(repositoriesTabComposite);
  }

  private void createReportsTab(FormToolkit toolkit, CTabFolder tabFolder) {
    CTabItem reportingTabItem = new CTabItem(tabFolder, SWT.NONE);
    reportingTabItem.setText("Reporting");

    reportingTabComposite = new ReportingComposite(tabFolder, SWT.NONE);
    toolkit.adapt(reportingTabComposite);
    reportingTabItem.setControl(reportingTabComposite);
  }

  private void createActivationTab(CTabFolder tabFolder, FormToolkit toolkit) {
    CTabItem activationTabItem = new CTabItem(tabFolder, SWT.NONE);
    activationTabItem.setText("Activation");

    Composite activationComposite = new Composite(tabFolder, SWT.NONE);
    toolkit.paintBordersFor(activationComposite);
    GridLayout activationLayout = new GridLayout(2, false);
    activationLayout.marginWidth = 0;
    activationComposite.setLayout(activationLayout);
    activationTabItem.setControl(activationComposite);
    toolkit.adapt(activationComposite);
    
    activeByDefaultbutton = toolkit.createButton(activationComposite, "Active by default", SWT.CHECK);
    activeByDefaultbutton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

    Section propertySection = toolkit.createSection(activationComposite, Section.TITLE_BAR);
    propertySection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    propertySection.setText("Property");

    Composite propertyComposite = toolkit.createComposite(propertySection, SWT.NONE);
    GridLayout propertyLayout = new GridLayout(2, false);
    propertyLayout.marginHeight = 2;
    propertyLayout.marginWidth = 1;
    propertyComposite.setLayout(propertyLayout);
    propertySection.setClient(propertyComposite);
    toolkit.paintBordersFor(propertyComposite);

    Label propertyNameLabel = toolkit.createLabel(propertyComposite, "Name:", SWT.NONE);

    activationPropertyNameText = toolkit.createText(propertyComposite, null, SWT.NONE);
    activationPropertyNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label propertyValueLabel = toolkit.createLabel(propertyComposite, "Value:", SWT.NONE);

    activationPropertyValueText = toolkit.createText(propertyComposite, null, SWT.NONE);
    activationPropertyValueText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Section osSection = toolkit.createSection(activationComposite, Section.TITLE_BAR);
    osSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 3));
    osSection.setText("OS");

    Composite osComposite = new Composite(osSection, SWT.NONE);
    GridLayout osLayout = new GridLayout(2, false);
    osLayout.marginWidth = 1;
    osLayout.marginHeight = 2;
    osComposite.setLayout(osLayout);
    toolkit.paintBordersFor(osComposite);
    toolkit.adapt(osComposite);
    osSection.setClient(osComposite);

    toolkit.createLabel(osComposite, "Name:", SWT.NONE);

    activationOsNameText = toolkit.createText(osComposite, null, SWT.NONE);
    activationOsNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    toolkit.createLabel(osComposite, "Family:", SWT.NONE);

    activationOsFamilyText = toolkit.createText(osComposite, null, SWT.NONE);
    activationOsFamilyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    toolkit.createLabel(osComposite, "Architecture:", SWT.NONE);

    activationOsArchitectureText = toolkit.createText(osComposite, null, SWT.NONE);
    activationOsArchitectureText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    toolkit.createLabel(osComposite, "Version:", SWT.NONE);

    activationOsVersionText = toolkit.createText(osComposite, null, SWT.NONE);
    activationOsVersionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Section fileSection = toolkit.createSection(activationComposite, Section.TITLE_BAR);
    GridData fileGridData = new GridData(SWT.FILL, SWT.TOP, false, false);
    fileGridData.verticalIndent = 5;
    fileSection.setLayoutData(fileGridData);
    fileSection.setText("File");

    Composite fileComposite = toolkit.createComposite(fileSection, SWT.NONE);
    GridLayout fileCompositeLayout = new GridLayout();
    fileCompositeLayout.marginWidth = 1;
    fileCompositeLayout.marginHeight = 2;
    fileCompositeLayout.numColumns = 2;
    fileComposite.setLayout(fileCompositeLayout);
    toolkit.paintBordersFor(fileComposite);
    fileSection.setClient(fileComposite);

    Label fileExistLabel = toolkit.createLabel(fileComposite, "Exist:", SWT.NONE);

    activationFileExistText = toolkit.createText(fileComposite, null, SWT.NONE);
    activationFileExistText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label fileMissingLabel = toolkit.createLabel(fileComposite, "Missing:", SWT.NONE);

    activationFileMissingText = toolkit.createText(fileComposite, null, SWT.NONE);
    activationFileMissingText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    
    Section jdkSection = toolkit.createSection(activationComposite, Section.TITLE_BAR);
    jdkSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    jdkSection.setText("JDK");

    Composite composite = toolkit.createComposite(jdkSection, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginHeight = 2;
    gridLayout.marginWidth = 1;
    gridLayout.numColumns = 2;
    composite.setLayout(gridLayout);
    toolkit.paintBordersFor(composite);
    jdkSection.setClient(composite);

    Label jdkLabel = toolkit.createLabel(composite, "JDK:", SWT.NONE);

    activationJdkText = toolkit.createText(composite, null, SWT.NONE);
    activationJdkText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    WidthGroup widthGroup = new WidthGroup();
    activationComposite.addControlListener(widthGroup);
    widthGroup.addControl(propertyNameLabel);
    widthGroup.addControl(propertyValueLabel);
    widthGroup.addControl(fileExistLabel);
    widthGroup.addControl(fileMissingLabel);
    widthGroup.addControl(jdkLabel);
  }

  public void loadData() {
    ProfilesType profiles = model.getProfiles();
    profilesEditor.setInput(profiles==null ? null : profiles.getProfile());
  }

  public void updateView(Notification notification) {
    // TODO Auto-generated method stub
    
  }

}
