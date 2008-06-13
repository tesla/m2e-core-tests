/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import static org.maven.ide.eclipse.editor.pom.FormUtils.isEmpty;
import static org.maven.ide.eclipse.editor.pom.FormUtils.nvl;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.FileEditorInput;
import org.maven.ide.components.pom.CiManagement;
import org.maven.ide.components.pom.Dependency;
import org.maven.ide.components.pom.IssueManagement;
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.Modules;
import org.maven.ide.components.pom.Organization;
import org.maven.ide.components.pom.Parent;
import org.maven.ide.components.pom.PomFactory;
import org.maven.ide.components.pom.Properties;
import org.maven.ide.components.pom.Scm;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.actions.MavenRepositorySearchDialog;
import org.maven.ide.eclipse.actions.OpenPomAction;
import org.maven.ide.eclipse.editor.composites.ListEditorComposite;
import org.maven.ide.eclipse.editor.composites.ListEditorContentProvider;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.wizards.MavenModuleWizard;
import org.maven.ide.eclipse.wizards.WidthGroup;


/**
 * @author Eugene Kuleshov
 */
public class OverviewPage extends MavenPomEditorPage {

  //controls
  private Text parentRealtivePathText;
  private Text artifactIdText;
  private Text artifactVersionText;
  private Text artifactGroupIdText;
  private CCombo artifactPackagingCombo;
  private Text parentVersionText;
  private Text parentArtifactIdText;
  private Text parentGroupIdText;
  private Text projectUrlText;
  private Text projectNameText;
  private Text projectDescriptionText;
  private Text inceptionYearText;
  private Text organizationUrlText;
  private Text organizationNameText;
  private Text scmUrlText;
  private Text scmDevConnectionText;
  private Text scmConnectionText;
  private Text scmTagText;
  private Text issueManagementSystemText;
  private Text issueManagementUrlText;
  private Text ciManagementUrlText;
  private Text ciManagementSystemText;

  private ListEditorComposite<PropertyPair> propertiesEditor;
  private ListEditorComposite<String> modulesEditor;
  
  //model
  private CiManagement ciManagement;
  private Scm scm;
  private Parent parent;
  private Organization organization;
  private IssueManagement issueManagement;
  private Modules modules;
  private Properties properties;
  private Section propertiesSection;
  private Section modulesSection;
  private Section parentSection;
  private Section projectSection;
  private Section organizationSection;
  private Section scmSection;
  private Section issueManagementSection;
  private Section ciManagementSection;

  public OverviewPage(MavenPomEditor pomEditor) {
    super(pomEditor, MavenPlugin.PLUGIN_ID + ".pom.overview", "Overview");
  }

  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();
    ScrolledForm form = managedForm.getForm();
    form.setText("Overview");
    
    Composite body = form.getBody();
    GridLayout gridLayout = new GridLayout(2, true);
    gridLayout.horizontalSpacing = 7;
    body.setLayout(gridLayout);
    toolkit.paintBordersFor(body);

    Composite leftComposite = toolkit.createComposite(body, SWT.NONE);
    leftComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    GridLayout gridLayout_1 = new GridLayout();
    gridLayout_1.marginWidth = 0;
    gridLayout_1.marginHeight = 0;
    leftComposite.setLayout(gridLayout_1);

    WidthGroup leftWidthGroup = new WidthGroup();
    leftComposite.addControlListener(leftWidthGroup);
    
    createArtifactSection(toolkit, leftComposite, leftWidthGroup);
    createParentsection(toolkit, leftComposite, leftWidthGroup);
    createPropertiesSection(toolkit, leftComposite, leftWidthGroup);
    createModulesSection(toolkit, leftComposite, leftWidthGroup);

    Composite rightComposite = toolkit.createComposite(body, SWT.NONE);
    rightComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    GridLayout gridLayout_2 = new GridLayout();
    gridLayout_2.marginWidth = 0;
    gridLayout_2.marginHeight = 0;
    rightComposite.setLayout(gridLayout_2);

    WidthGroup rightWidthGroup = new WidthGroup();
    rightComposite.addControlListener(rightWidthGroup);
    
    createProjectSection(toolkit, rightComposite, rightWidthGroup);
    createOrganizationSection(toolkit, rightComposite, rightWidthGroup);
    createScmSection(toolkit, rightComposite, rightWidthGroup);
    createIssueManagementSection(toolkit, rightComposite, rightWidthGroup);
    createCiManagementSection(toolkit, rightComposite, rightWidthGroup);
    
    toolkit.paintBordersFor(leftComposite);
    toolkit.paintBordersFor(rightComposite);
    
    toolkit.decorateFormHeading(form.getForm());
  }
  
  private void createArtifactSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    Section artifactSection = toolkit.createSection(composite, Section.TITLE_BAR);
    artifactSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    artifactSection.setText("Artifact");
  
    Composite artifactComposite = toolkit.createComposite(artifactSection, SWT.NONE);
    toolkit.adapt(artifactComposite);
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginBottom = 5;
    gridLayout.marginHeight = 2;
    gridLayout.marginWidth = 1;
    artifactComposite.setLayout(gridLayout);
    artifactSection.setClient(artifactComposite);
  
    Label groupIdLabel = toolkit.createLabel(artifactComposite, "Group Id:", SWT.NONE);
  
    artifactGroupIdText = toolkit.createText(artifactComposite, null, SWT.NONE);
    artifactGroupIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    Label artifactIdLabel = toolkit.createLabel(artifactComposite, "Artifact Id:", SWT.NONE);
  
    artifactIdText = toolkit.createText(artifactComposite, null, SWT.NONE);
    artifactIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    Label versionLabel = toolkit.createLabel(artifactComposite, "Version:", SWT.NONE);
  
    artifactVersionText = toolkit.createText(artifactComposite, null, SWT.NONE);
    GridData gd_versionText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_versionText.widthHint = 200;
    artifactVersionText.setLayoutData(gd_versionText);
  
    Label packagingLabel = toolkit.createLabel(artifactComposite, "Packaging:", SWT.NONE);
  
    artifactPackagingCombo = new CCombo(artifactComposite, SWT.FLAT);
    
    artifactPackagingCombo.add("jar");
    artifactPackagingCombo.add("war");
    artifactPackagingCombo.add("ear");
    artifactPackagingCombo.add("pom");
    artifactPackagingCombo.add("maven-plugin");
    artifactPackagingCombo.add("osgi-bundle");
    artifactPackagingCombo.add("eclipse-feature");
    
    toolkit.adapt(artifactPackagingCombo, true, true);
    GridData gd_packagingText = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    gd_packagingText.widthHint = 120;
    artifactPackagingCombo.setLayoutData(gd_packagingText);
    artifactPackagingCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    toolkit.paintBordersFor(artifactPackagingCombo);
    
    widthGroup.addControl(groupIdLabel);
    widthGroup.addControl(artifactIdLabel);
    widthGroup.addControl(versionLabel);
    widthGroup.addControl(packagingLabel);
    
    toolkit.paintBordersFor(artifactComposite);
  }

  private void createParentsection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    parentSection = toolkit.createSection(composite, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
    parentSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    parentSection.setText("Parent");
  
    Composite parentComposite = toolkit.createComposite(parentSection, SWT.NONE);
    GridLayout gridLayout = new GridLayout(3, false);
    gridLayout.marginBottom = 5;
    gridLayout.marginWidth = 1;
    gridLayout.marginHeight = 2;
    parentComposite.setLayout(gridLayout);
    parentSection.setClient(parentComposite);

    Label parentGroupIdLabel = toolkit.createLabel(parentComposite, "Group Id:", SWT.NONE);
  
    parentGroupIdText = toolkit.createText(parentComposite, null, SWT.NONE);
    parentGroupIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
  
    Hyperlink parentArtifactIdLabel = toolkit.createHyperlink(parentComposite, "Artifact Id:", SWT.NONE);
    parentArtifactIdLabel.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        final String parentGroupId = parentGroupIdText.getText();
        final String parentArtifactId = parentArtifactIdText.getText();
        final String parentVersion = parentVersionText.getText();
        new Job("Opening POM") {
          protected IStatus run(IProgressMonitor arg0) {
            OpenPomAction.openEditor(parentGroupId, parentArtifactId, parentVersion);
            return Status.OK_STATUS;
          }
        }.schedule();
      }
    });
  
    parentArtifactIdText = toolkit.createText(parentComposite, null, SWT.NONE);
    parentArtifactIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
  
    Label parentVersionLabel = toolkit.createLabel(parentComposite, "Version:", SWT.NONE);
    parentVersionLabel.setLayoutData(new GridData());
  
    parentVersionText = toolkit.createText(parentComposite, null, SWT.NONE);
    GridData parentVersionTextData = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    parentVersionTextData.widthHint = 200;
    parentVersionText.setLayoutData(parentVersionTextData);
  
    Button parentSelectButton = toolkit.createButton(parentComposite, "Select...", SWT.NONE);
    parentSelectButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        // TODO calculate current list of artifacts for the project
        Set<Dependency> artifacts = Collections.emptySet();
        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getEditorSite().getShell(),
            "Add Dependency", IndexManager.SEARCH_ARTIFACT, artifacts);
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            parentGroupIdText.setText(nvl(af.group));
            parentArtifactIdText.setText(nvl(af.artifact));
            parentVersionText.setText(nvl(af.artifact));
          }
        }
      }
    });

    Label parentRealtivePathLabel = toolkit.createLabel(parentComposite, "Relative Path:", SWT.NONE);

    parentRealtivePathText = toolkit.createText(parentComposite, null, SWT.NONE);
    parentRealtivePathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    
    widthGroup.addControl(parentGroupIdLabel);
    widthGroup.addControl(parentArtifactIdLabel);
    widthGroup.addControl(parentVersionLabel);
    widthGroup.addControl(parentRealtivePathLabel);
    
    toolkit.paintBordersFor(parentComposite);
  }

  private void createPropertiesSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    propertiesSection = toolkit.createSection(composite, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
    propertiesSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    propertiesSection.setText("Properties");
    toolkit.paintBordersFor(propertiesSection);

    propertiesEditor = new ListEditorComposite<PropertyPair>(propertiesSection, SWT.NONE);
    propertiesSection.setClient(propertiesEditor);
    
    propertiesEditor.setContentProvider(new ListEditorContentProvider<PropertyPair>());
    propertiesEditor.setLabelProvider(new PropertyPairLabelProvider());

    // XXX implement properties support
    propertiesEditor.setEnabled(false);
  }

  private void createModulesSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    modulesSection = toolkit.createSection(composite, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
    modulesSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    modulesSection.setText("Modules");

    modulesEditor = new ListEditorComposite<String>(modulesSection, SWT.NONE);
    toolkit.paintBordersFor(modulesEditor);
    toolkit.adapt(modulesEditor);
    modulesSection.setClient(modulesEditor);

    modulesEditor.setContentProvider(new ListEditorContentProvider<String>());
    modulesEditor.setLabelProvider(new ModulesLabelProvider(this));
    
    modulesEditor.setOpenListener(new IOpenListener() {
      public void open(OpenEvent openevent) {
        List<String> selection = modulesEditor.getSelection();
        for(String module : selection) {
          MavenProjectFacade projectFacade = findModuleProject(module);
          if(projectFacade!=null) {
            MavenProject mavenProject = projectFacade.getMavenProject();
            OpenPomAction.openEditor(mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getVersion());
          }
        }
      }
    });
    
    modulesEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        IEditorInput editorInput = OverviewPage.this.pomEditor.getEditorInput();
        if(editorInput instanceof FileEditorInput) {
          MavenModuleWizard wizard = new MavenModuleWizard(true);
          wizard.init(PlatformUI.getWorkbench(), new StructuredSelection(((FileEditorInput) editorInput).getFile()));
          WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
          int res = dialog.open();
          if(res == Window.OK) {
            CompoundCommand compoundCommand = new CompoundCommand();
            EditingDomain editingDomain = getEditingDomain();

            Modules modules = model.getModules();
            if(modules == null) {
              modules = PomFactory.eINSTANCE.createModules();
              Command addModules = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Modules(), modules);
              compoundCommand.append(addModules);
            }
            
            Command addModule = AddCommand.create(editingDomain, modules, POM_PACKAGE.getModules_Module(), wizard.getModuleName());
            compoundCommand.append(addModule);
            
            // modules.getModule().add(wizard.getModuleName());

            editingDomain.getCommandStack().execute(compoundCommand);
            modulesEditor.setInput(modules == null ? null : modules.getModule());
          }
        }
      }
    });
    
    modulesEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = getEditingDomain();

        int n = 0;
        Modules modules = model.getModules();
        for(String module : modulesEditor.getSelection()) {
          Command removeCommand = RemoveCommand.create(editingDomain, modules, POM_PACKAGE.getModules_Module(), module);
          compoundCommand.append(removeCommand);
          n++ ;
        }
        if(modules.getModule().size() - n == 0) {
          Command removeModules = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Modules(), null);
          compoundCommand.append(removeModules);
        }

        editingDomain.getCommandStack().execute(compoundCommand);
        modulesEditor.setInput(modules == null ? null : modules.getModule());
      }
    });
  }
  
  // right side

  private void createProjectSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    projectSection = toolkit.createSection(composite, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
    projectSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    projectSection.setText("Project");
  
    Composite projectComposite = toolkit.createComposite(projectSection, SWT.NONE);
    projectComposite.setLayout(new GridLayout(2, false));
    projectSection.setClient(projectComposite);
  
    Label nameLabel = toolkit.createLabel(projectComposite, "Name:", SWT.NONE);
  
    projectNameText = toolkit.createText(projectComposite, null, SWT.NONE);
    GridData gd_projectNameText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_projectNameText.widthHint = 150;
    projectNameText.setLayoutData(gd_projectNameText);
  
    Hyperlink urlLabel = toolkit.createHyperlink(projectComposite, "URL:", SWT.NONE);
    urlLabel.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(projectUrlText.getText());
      }
    });
  
    projectUrlText = toolkit.createText(projectComposite, null, SWT.NONE);
    GridData gd_projectUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_projectUrlText.widthHint = 150;
    projectUrlText.setLayoutData(gd_projectUrlText);
  
    Label descriptionLabel = toolkit.createLabel(projectComposite, "Description:", SWT.NONE);
    descriptionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
  
    projectDescriptionText = toolkit.createText(projectComposite, null, SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
    GridData gd_descriptionText = new GridData(SWT.FILL, SWT.FILL, true, false);
    gd_descriptionText.widthHint = 150;
    gd_descriptionText.heightHint = 55;
    projectDescriptionText.setLayoutData(gd_descriptionText);
  
    Label inceptionYearLabel = toolkit.createLabel(projectComposite, "Inception:", SWT.NONE);
  
    inceptionYearText = toolkit.createText(projectComposite, null, SWT.NONE);
    GridData gd_inceptionYearText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_inceptionYearText.widthHint = 150;
    inceptionYearText.setLayoutData(gd_inceptionYearText);
    
    widthGroup.addControl(nameLabel);
    widthGroup.addControl(urlLabel);
    widthGroup.addControl(descriptionLabel);
    widthGroup.addControl(inceptionYearLabel);
    
    toolkit.paintBordersFor(projectComposite);
  }

  private void createOrganizationSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    organizationSection = toolkit.createSection(composite, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
    organizationSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    organizationSection.setText("Organization");
  
    Composite organizationComposite = toolkit.createComposite(organizationSection, SWT.NONE);
    organizationComposite.setLayout(new GridLayout(2, false));
    organizationSection.setClient(organizationComposite);
  
    Label organizationNameLabel = toolkit.createLabel(organizationComposite, "Name:", SWT.NONE);
  
    organizationNameText = toolkit.createText(organizationComposite, null, SWT.NONE);
    GridData gd_organizationNameText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_organizationNameText.widthHint = 150;
    organizationNameText.setLayoutData(gd_organizationNameText);
  
    Hyperlink organizationUrlLabel = toolkit.createHyperlink(organizationComposite, "URL:", SWT.NONE);
    organizationUrlLabel.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(organizationUrlText.getText());
      }
    });
  
    organizationUrlText = toolkit.createText(organizationComposite, null, SWT.NONE);
    GridData gd_organizationUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_organizationUrlText.widthHint = 150;
    organizationUrlText.setLayoutData(gd_organizationUrlText);
    
    widthGroup.addControl(organizationNameLabel);
    widthGroup.addControl(organizationUrlLabel);
    
    toolkit.paintBordersFor(organizationComposite);
  }

  private void createScmSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    scmSection = toolkit.createSection(composite, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
    GridData gd_scmSection = new GridData(SWT.FILL, SWT.TOP, false, false);
    scmSection.setLayoutData(gd_scmSection);
    scmSection.setText("SCM");
  
    Composite scmComposite = toolkit.createComposite(scmSection, SWT.NONE);
    scmComposite.setLayout(new GridLayout(2, false));
    scmSection.setClient(scmComposite);
  
    Hyperlink scmUrlLabel = toolkit.createHyperlink(scmComposite, "URL:", SWT.NONE);
    scmUrlLabel.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(scmUrlText.getText());
      }
    });
  
    scmUrlText = toolkit.createText(scmComposite, null, SWT.NONE);
    GridData gd_scmUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_scmUrlText.widthHint = 150;
    scmUrlText.setLayoutData(gd_scmUrlText);
  
    Label scmConnectionLabel = toolkit.createLabel(scmComposite, "Connection:", SWT.NONE);
  
    scmConnectionText = toolkit.createText(scmComposite, null, SWT.NONE);
    GridData gd_scmConnectionText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_scmConnectionText.widthHint = 150;
    scmConnectionText.setLayoutData(gd_scmConnectionText);
  
    Label scmDevConnectionLabel = toolkit.createLabel(scmComposite, "Developer:", SWT.NONE);
  
    scmDevConnectionText = toolkit.createText(scmComposite, null, SWT.NONE);
    GridData gd_scmDevConnectionText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_scmDevConnectionText.widthHint = 150;
    scmDevConnectionText.setLayoutData(gd_scmDevConnectionText);
  
    Label scmTagLabel = toolkit.createLabel(scmComposite, "Tag:", SWT.NONE);
  
    scmTagText = toolkit.createText(scmComposite, null, SWT.NONE);
    GridData gd_scmTagText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_scmTagText.widthHint = 150;
    scmTagText.setLayoutData(gd_scmTagText);
    
    widthGroup.addControl(scmUrlLabel);
    widthGroup.addControl(scmConnectionLabel);
    widthGroup.addControl(scmDevConnectionLabel);
    widthGroup.addControl(scmTagLabel);
    
    toolkit.paintBordersFor(scmComposite);
  }

  private void createIssueManagementSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    issueManagementSection = toolkit.createSection(composite, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
    issueManagementSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    issueManagementSection.setText("Issue Management");
  
    Composite issueManagementComposite = toolkit.createComposite(issueManagementSection, SWT.NONE);
    issueManagementComposite.setLayout(new GridLayout(2, false));
    issueManagementSection.setClient(issueManagementComposite);
  
    Label issueManagementSystemLabel = toolkit.createLabel(issueManagementComposite, "System:", SWT.NONE);
  
    issueManagementSystemText = toolkit.createText(issueManagementComposite, null, SWT.NONE);
    GridData gd_issueManagementSystemText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_issueManagementSystemText.widthHint = 150;
    issueManagementSystemText.setLayoutData(gd_issueManagementSystemText);
  
    Hyperlink issueManagementUrlLabel = toolkit.createHyperlink(issueManagementComposite, "URL:", SWT.NONE);
    issueManagementUrlLabel.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(issueManagementUrlText.getText());
      }
    });
  
    issueManagementUrlText = toolkit.createText(issueManagementComposite, null, SWT.NONE);
    GridData gd_issueManagementUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_issueManagementUrlText.widthHint = 150;
    issueManagementUrlText.setLayoutData(gd_issueManagementUrlText);
    
    widthGroup.addControl(issueManagementSystemLabel);
    widthGroup.addControl(issueManagementUrlLabel);
    
    toolkit.paintBordersFor(issueManagementComposite);
  }

  private void createCiManagementSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    ciManagementSection = toolkit.createSection(composite, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
    ciManagementSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    ciManagementSection.setText("Continuous Integration");
  
    Composite ciManagementComposite = toolkit.createComposite(ciManagementSection, SWT.NONE);
    ciManagementComposite.setLayout(new GridLayout(2, false));
    ciManagementSection.setClient(ciManagementComposite);
  
    Label ciManagementSystemLabel = toolkit.createLabel(ciManagementComposite, "System:", SWT.NONE);
  
    ciManagementSystemText = toolkit.createText(ciManagementComposite, null, SWT.NONE);
    GridData gd_ciManagementSystemText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_ciManagementSystemText.widthHint = 150;
    ciManagementSystemText.setLayoutData(gd_ciManagementSystemText);
  
    Hyperlink ciManagementUrlLabel = toolkit.createHyperlink(ciManagementComposite, "URL:", SWT.NONE);
    ciManagementUrlLabel.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(ciManagementUrlText.getText());
      }
    });
  
    ciManagementUrlText = toolkit.createText(ciManagementComposite, null, SWT.NONE);
    GridData gd_ciManagementUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_ciManagementUrlText.widthHint = 150;
    ciManagementUrlText.setLayoutData(gd_ciManagementUrlText);
    
    widthGroup.addControl(ciManagementSystemLabel);
    widthGroup.addControl(ciManagementUrlLabel);
    
    toolkit.paintBordersFor(ciManagementComposite);
  }

  public void registerFormListeners() {
    // form to model
    setModifyListener(artifactGroupIdText, model, POM_PACKAGE.getModel_GroupId());
    setModifyListener(artifactIdText, model, POM_PACKAGE.getModel_ArtifactId());
    setModifyListener(artifactVersionText, model, POM_PACKAGE.getModel_ModelVersion());
    setModifyListener(artifactPackagingCombo, model, POM_PACKAGE.getModel_Packaging(), "jar");
    
    setModifyListener(parentGroupIdText, parent, POM_PACKAGE.getParent_GroupId());
    setModifyListener(parentArtifactIdText, parent, POM_PACKAGE.getParent_ArtifactId());
    setModifyListener(parentVersionText, parent, POM_PACKAGE.getParent_Version());
    setModifyListener(parentRealtivePathText, parent, POM_PACKAGE.getParent_RelativePath());

    setModifyListener(projectNameText, model, POM_PACKAGE.getModel_Name());
    setModifyListener(projectDescriptionText, model, POM_PACKAGE.getModel_Description());
    setModifyListener(projectUrlText, model, POM_PACKAGE.getModel_Url());
    setModifyListener(inceptionYearText, model, POM_PACKAGE.getModel_InceptionYear());

    setModifyListener(organizationNameText, organization, POM_PACKAGE.getOrganization_Name());
    setModifyListener(organizationUrlText, organization, POM_PACKAGE.getOrganization_Url());

    setModifyListener(scmUrlText, scm, POM_PACKAGE.getScm_Url());
    setModifyListener(scmConnectionText, scm, POM_PACKAGE.getScm_Connection());
    setModifyListener(scmDevConnectionText, scm, POM_PACKAGE.getScm_DeveloperConnection());
    setModifyListener(scmTagText, scm, POM_PACKAGE.getScm_Tag());

    setModifyListener(issueManagementUrlText, issueManagement, POM_PACKAGE.getIssueManagement_Url());
    setModifyListener(issueManagementSystemText, issueManagement, POM_PACKAGE.getIssueManagement_Url());
    
    setModifyListener(ciManagementUrlText, ciManagement, POM_PACKAGE.getCiManagement_Url());
    setModifyListener(ciManagementSystemText, ciManagement, POM_PACKAGE.getCiManagement_System());
  }
    
  public void updateView(Notification notification) {
    EObject object = (EObject) notification.getNotifier();
    if (object instanceof Model) {
      loadThis();
    }

    if (object instanceof Parent) {
      loadParent();
    }

    if (object instanceof Organization) {
      loadOrganization();
    }

    if (object instanceof Scm) {
      loadScm();
    }

    if (object instanceof CiManagement) {
      loadCiManagement();
    }

    if (object instanceof IssueManagement) {
      loadIssueManagement();
    }
    
    if(object instanceof Modules) {
      loadModules();
    }
    
    if(object instanceof Properties) {
      loadProperties();
    }
  }

  public void loadData() {
    loadThis();
    loadParent();
    loadOrganization();
    loadScm();
    loadIssueManagement();
    loadCiManagement();
    loadModules();
    loadProperties();
    registerFormListeners();
  }

  private void loadThis() {
    artifactGroupIdText.setText(nvl(model.getGroupId()));
    artifactIdText.setText(nvl(model.getArtifactId()));
    artifactVersionText.setText(nvl(model.getVersion()));
    artifactPackagingCombo.setText(nvl(model.getPackaging()));
    
    projectNameText.setText(nvl(model.getName()));
    projectDescriptionText.setText(nvl(model.getDescription()));
    projectUrlText.setText(nvl(model.getUrl()));
    inceptionYearText.setText(nvl(model.getInceptionYear()));
    
    projectSection.setExpanded(!isEmpty(model.getName()) || !isEmpty(model.getDescription())
        || !isEmpty(model.getUrl()) || !isEmpty(model.getInceptionYear()));
  }

  private void loadProperties() {
    properties = model.getProperties();
    propertiesEditor.setInput(properties);
    propertiesSection.setExpanded(false);
  }
  
  private void loadModules() {
    modules = model.getModules();
    modulesEditor.setInput(modules==null ? null : modules.getModule());
    modulesSection.setExpanded(modules!=null && modules.getModule().size()>0);
  }
  
  private void loadParent() {
    parent = model.getParent();
    if(parent!=null) {
      parentGroupIdText.setText(nvl(parent.getGroupId()));
      parentArtifactIdText.setText(nvl(parent.getArtifactId()));
      parentVersionText.setText(nvl(parent.getVersion()));
      parentRealtivePathText.setText(nvl(parent.getRelativePath()));
      parentSection.setExpanded(!isEmpty(parent.getGroupId()));
    } else {
      parentSection.setExpanded(false);
    }
  }

  private void loadOrganization() {
    organization = model.getOrganization();
    if(organization!=null) {
      organizationNameText.setText(nvl(organization.getName()));
      organizationUrlText.setText(nvl(organization.getUrl()));
      organizationSection.setExpanded(!isEmpty(organization.getName()) || !isEmpty(organization.getUrl()));
    } else {
      organizationSection.setExpanded(false);
    }
  }

  private void loadScm() {
    scm = model.getScm();
    if(scm!=null) {
      scmUrlText.setText(nvl(scm.getUrl()));
      scmConnectionText.setText(nvl(scm.getConnection()));
      scmDevConnectionText.setText(nvl(scm.getDeveloperConnection()));
      scmTagText.setText(nvl(scm.getTag()));
      scmSection.setExpanded(!isEmpty(scm.getUrl()) || !isEmpty(scm.getConnection()) || !isEmpty(scm.getDeveloperConnection()));
    } else {
      scmSection.setExpanded(false);
    }
  }

  private void loadCiManagement() {
    ciManagement = model.getCiManagement();
    if(ciManagement!=null) {
      ciManagementSystemText.setText(nvl(ciManagement.getSystem()));
      ciManagementUrlText.setText(nvl(ciManagement.getUrl()));
      ciManagementSection.setExpanded(!isEmpty(ciManagement.getSystem()) || !isEmpty(ciManagement.getUrl()));
    } else {
      ciManagementSection.setExpanded(false);
    }
  }

  private void loadIssueManagement() {
    issueManagement = model.getIssueManagement();
    if(issueManagement!=null) {
      issueManagementSystemText.setText(nvl(issueManagement.getSystem()));
      issueManagementUrlText.setText(nvl(issueManagement.getUrl()));
      issueManagementSection.setExpanded(!isEmpty(issueManagement.getSystem()) || !isEmpty(issueManagement.getUrl()));
    } else {
      issueManagementSection.setExpanded(false);
    }
  }

}
