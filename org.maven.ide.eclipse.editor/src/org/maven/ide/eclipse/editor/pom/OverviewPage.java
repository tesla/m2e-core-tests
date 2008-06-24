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
import static org.maven.ide.eclipse.editor.pom.FormUtils.setText;

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
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ICellModifier;
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
import org.maven.ide.eclipse.editor.MavenEditorImages;
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
  Text parentRealtivePathText;
  Text artifactIdText;
  Text artifactVersionText;
  Text artifactGroupIdText;
  CCombo artifactPackagingCombo;
  Text parentVersionText;
  Text parentArtifactIdText;
  Text parentGroupIdText;
  Text projectUrlText;
  Text projectNameText;
  Text projectDescriptionText;
  Text inceptionYearText;
  Text organizationUrlText;
  Text organizationNameText;
  Text scmUrlText;
  Text scmDevConnectionText;
  Text scmConnectionText;
  Text scmTagText;
  Text issueManagementSystemText;
  Text issueManagementUrlText;
  Text ciManagementUrlText;
  Text ciManagementSystemText;

  ListEditorComposite<PropertyPair> propertiesEditor;
  ListEditorComposite<String> modulesEditor;
  
  Section propertiesSection;
  Section modulesSection;
  Section parentSection;
  Section projectSection;
  Section organizationSection;
  Section scmSection;
  Section issueManagementSection;
  Section ciManagementSection;
  private Action newModuleProjectAction;

  //model
//  Properties properties;
//  Modules modules;
//  Parent parent;
//  Organization organization;
//  Scm scm;
//  IssueManagement issueManagement;
//  CiManagement ciManagement;

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
    GridLayout leftCompositeLayout = new GridLayout();
    leftCompositeLayout.marginWidth = 0;
    leftCompositeLayout.marginHeight = 0;
    leftComposite.setLayout(leftCompositeLayout);

    WidthGroup leftWidthGroup = new WidthGroup();
    leftComposite.addControlListener(leftWidthGroup);
    
    createArtifactSection(toolkit, leftComposite, leftWidthGroup);
    createParentsection(toolkit, leftComposite, leftWidthGroup);
    createPropertiesSection(toolkit, leftComposite, leftWidthGroup);
    createModulesSection(toolkit, leftComposite, leftWidthGroup);

    Composite rightComposite = toolkit.createComposite(body, SWT.NONE);
    rightComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    GridLayout rightCompositeLayout = new GridLayout();
    rightCompositeLayout.marginWidth = 0;
    rightCompositeLayout.marginHeight = 0;
    rightComposite.setLayout(rightCompositeLayout);

    WidthGroup rightWidthGroup = new WidthGroup();
    rightComposite.addControlListener(rightWidthGroup);
    
    createProjectSection(toolkit, rightComposite, rightWidthGroup);
    createOrganizationSection(toolkit, rightComposite, rightWidthGroup);
    createScmSection(toolkit, rightComposite, rightWidthGroup);
    createIssueManagementSection(toolkit, rightComposite, rightWidthGroup);
    createCiManagementSection(toolkit, rightComposite, rightWidthGroup);
    
    toolkit.paintBordersFor(leftComposite);
    toolkit.paintBordersFor(rightComposite);
    
    super.createFormContent(managedForm);
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
  
    Label artifactIdLabel = toolkit.createLabel(artifactComposite, "Artifact Id:*", SWT.NONE);
  
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

    Label parentGroupIdLabel = toolkit.createLabel(parentComposite, "Group Id:*", SWT.NONE);
  
    parentGroupIdText = toolkit.createText(parentComposite, null, SWT.NONE);
    parentGroupIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
  
    Hyperlink parentArtifactIdLabel = toolkit.createHyperlink(parentComposite, "Artifact Id:*", SWT.NONE);
    parentArtifactIdLabel.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        final String groupId = parentGroupIdText.getText();
        final String artifactId = parentArtifactIdText.getText();
        final String version = parentVersionText.getText();
        new Job("Opening " + groupId + ":" + artifactId + ":" + version) {
          protected IStatus run(IProgressMonitor arg0) {
            OpenPomAction.openEditor(groupId, artifactId, version);
            return Status.OK_STATUS;
          }
        }.schedule();
      }
    });
  
    parentArtifactIdText = toolkit.createText(parentComposite, null, SWT.NONE);
    parentArtifactIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
  
    Label parentVersionLabel = toolkit.createLabel(parentComposite, "Version:*", SWT.NONE);
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
            parentVersionText.setText(nvl(af.version));
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
    propertiesSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    propertiesSection.setText("Properties");
    toolkit.paintBordersFor(propertiesSection);

    propertiesEditor = new ListEditorComposite<PropertyPair>(propertiesSection, SWT.NONE);
    propertiesSection.setClient(propertiesEditor);
    
    propertiesEditor.setContentProvider(new ListEditorContentProvider<PropertyPair>());
    propertiesEditor.setLabelProvider(new PropertyPairLabelProvider());

    // XXX implement properties support
    // propertiesEditor.setReadOnly(pomEditor.isReadOnly());
    propertiesEditor.setReadOnly(true);
  }

  private void createModulesSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    // XXX should disable Modules actions based on artifact packaging and only add modules when packaging is "pom"

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
        createNewModule("?");
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
        // modulesEditor.refresh();
      }
    });
  
    modulesEditor.setCellModifier(new ICellModifier() {
      public boolean canModify(Object element, String property) {
        return true;
      }
 
      public Object getValue(Object element, String property) {
        return element;
      }
 
      public void modify(Object element, String property, Object value) {
        EditingDomain editingDomain = getEditingDomain();
        Command command = SetCommand.create(editingDomain, model.getModules(), //
            POM_PACKAGE.getModules_Module(), value, //
            modulesEditor.getViewer().getTable().getSelectionIndex());
        editingDomain.getCommandStack().execute(command);
        // modulesEditor.refresh();
      }
    });
    
    newModuleProjectAction = new Action("Create new module project", MavenEditorImages.ADD_MODULE) {
      public void run() {
        IEditorInput editorInput = OverviewPage.this.pomEditor.getEditorInput();
        if(editorInput instanceof FileEditorInput) {
          MavenModuleWizard wizard = new MavenModuleWizard(true);
          wizard.init(PlatformUI.getWorkbench(), new StructuredSelection(((FileEditorInput) editorInput).getFile()));
          WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
          int res = dialog.open();
          if(res == Window.OK) {
            createNewModule(wizard.getModuleName());
          }
        }
      }
    };

    ToolBarManager modulesToolBarManager = new ToolBarManager(SWT.FLAT);
    modulesToolBarManager.add(newModuleProjectAction);
    
    Composite toolbarComposite = toolkit.createComposite(modulesSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);
 
    modulesToolBarManager.createControl(toolbarComposite);
    modulesSection.setTextClient(toolbarComposite);
    
    modulesEditor.setReadOnly(pomEditor.isReadOnly());
    newModuleProjectAction.setEnabled(!pomEditor.isReadOnly());
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

  public void updateView(Notification notification) {
    EObject object = (EObject) notification.getNotifier();
    if (object instanceof Model) {
      loadThis();
      modulesEditor.refresh();
    }

    // XXX event is not received when <parent> is deleted in XML
    if (object instanceof Parent) {
      loadParent((Parent) object);
    }

    // XXX event is not received when <organization> is deleted in XML
    if (object instanceof Organization) {
      loadOrganization((Organization) object);
    }

    // XXX event is not received when <scm> is deleted in XML
    if (object instanceof Scm) {
      loadScm((Scm) object);
    }

    // XXX event is not received when <ciManagement> is deleted in XML
    if (object instanceof CiManagement) {
      loadCiManagement((CiManagement) object);
    }

    // XXX event is not received when <issueManagement> is deleted in XML
    if (object instanceof IssueManagement) {
      loadIssueManagement((IssueManagement) object);
    }
    
    // XXX event is not received when <modules> is deleted in XML
    if(object instanceof Modules) {
      modulesEditor.refresh();
      // loadModules((Modules) object);
    }
    
    // XXX event is not received when <properties> is deleted in XML
    if(object instanceof Properties) {
      loadProperties(pomEditor.getProperties(model));
    }
  }

  public void loadData() {
    Parent parent = model.getParent();
    Organization organization = model.getOrganization();
    Scm scm = model.getScm();
    IssueManagement issueManagement = model.getIssueManagement();
    CiManagement ciManagement = model.getCiManagement();
    
    loadThis();
    loadParent(parent);
    loadOrganization(organization);
    loadScm(scm);
    loadIssueManagement(issueManagement);
    loadCiManagement(ciManagement);
    loadModules(model.getModules());
    
    EList<PropertyPair> properties = pomEditor.getProperties(model);
    loadProperties(properties);
    
    registerFormListeners();
    
    projectSection.setExpanded(!isEmpty(model.getName()) || !isEmpty(model.getDescription())
        || !isEmpty(model.getUrl()) || !isEmpty(model.getInceptionYear()));
    
    parentSection.setExpanded(parent != null && !isEmpty(parent.getGroupId()));

    organizationSection.setExpanded(organization != null
        && (!isEmpty(organization.getName()) || !isEmpty(organization.getUrl())));
    
    scmSection.setExpanded(scm != null
        && (!isEmpty(scm.getUrl()) || !isEmpty(scm.getConnection()) || !isEmpty(scm.getDeveloperConnection())));
    
    ciManagementSection.setExpanded(ciManagement != null
        && (!isEmpty(ciManagement.getSystem()) || !isEmpty(ciManagement.getUrl())));
    
    issueManagementSection.setExpanded(issueManagement != null
        && (!isEmpty(issueManagement.getSystem()) || !isEmpty(issueManagement.getUrl())));

    propertiesSection.setExpanded(properties != null && !properties.isEmpty());
    
    // Modules modules = model.getModules();
    // modulesSection.setExpanded(modules !=null && modules.getModule().size()>0);
  }

  private void loadThis() {
    setText(artifactGroupIdText, model.getGroupId());
    setText(artifactIdText, model.getArtifactId());
    setText(artifactVersionText, model.getVersion());
    setText(artifactPackagingCombo, model.getPackaging());
    
    setText(projectNameText, model.getName());
    setText(projectDescriptionText, model.getDescription());
    setText(projectUrlText, model.getUrl());
    setText(inceptionYearText, model.getInceptionYear());
  }

  private void loadParent(Parent parent) {
    if(parent!=null) {
      setText(parentGroupIdText, parent.getGroupId());
      setText(parentArtifactIdText, parent.getArtifactId());
      setText(parentVersionText, parent.getVersion());
      setText(parentRealtivePathText, parent.getRelativePath());
    } else {
      setText(parentGroupIdText, "");
      setText(parentArtifactIdText, "");
      setText(parentVersionText, "");
      setText(parentRealtivePathText, "");
    }
  }
  
  private void loadProperties(EList<PropertyPair> properties) {
    propertiesEditor.setInput(properties);
  }
  
  private void loadModules(Modules modules) {
    modulesEditor.setInput(modules==null ? null : modules.getModule());
    modulesEditor.setReadOnly(isReadOnly());
    newModuleProjectAction.setEnabled(!isReadOnly());
  }
  
  private void loadOrganization(Organization organization) {
    if(organization!=null) {
      setText(organizationNameText, organization.getName());
      setText(organizationUrlText, organization.getUrl());
    }
  }

  private void loadScm(Scm scm) {
    if(scm!=null) {
      setText(scmUrlText, scm.getUrl());
      setText(scmConnectionText, scm.getConnection());
      setText(scmDevConnectionText, scm.getDeveloperConnection());
      setText(scmTagText, scm.getTag());
    }
  }

  private void loadCiManagement(CiManagement ciManagement) {
    if(ciManagement!=null) {
      setText(ciManagementSystemText, ciManagement.getSystem());
      setText(ciManagementUrlText, ciManagement.getUrl());
    }
  }

  private void loadIssueManagement(IssueManagement issueManagement) {
    if(issueManagement!=null) {
      setText(issueManagementSystemText, issueManagement.getSystem());
      setText(issueManagementUrlText, issueManagement.getUrl());
    }
  }

  private void registerFormListeners() {
    ValueProvider<Model> modelProvider = new ValueProvider.DefaultValueProvider<Model>(model);
    setModifyListener(artifactGroupIdText, modelProvider, POM_PACKAGE.getModel_GroupId(), "");
    setModifyListener(artifactIdText, modelProvider, POM_PACKAGE.getModel_ArtifactId(), "");
    setModifyListener(artifactVersionText, modelProvider, POM_PACKAGE.getModel_ModelVersion(), "");
    setModifyListener(artifactPackagingCombo, modelProvider, POM_PACKAGE.getModel_Packaging(), "jar");
    
    setModifyListener(projectNameText, modelProvider, POM_PACKAGE.getModel_Name(), "");
    setModifyListener(projectDescriptionText, modelProvider, POM_PACKAGE.getModel_Description(), "");
    setModifyListener(projectUrlText, modelProvider, POM_PACKAGE.getModel_Url(), "");
    setModifyListener(inceptionYearText, modelProvider, POM_PACKAGE.getModel_InceptionYear(), "");

    registerParentListener();
    registerOrganizationListnener();
    registerScmListnener();
    registerIssueManagementListener();
    registerCiManagementListnener();    
  }

  private void registerParentListener() {
    ValueProvider<Parent> parentProvider = new ValueProvider.ParentValueProvider<Parent>(parentGroupIdText,
        parentArtifactIdText, parentVersionText, parentRealtivePathText) {
      public Parent getValue() {
        return model.getParent();
      }
      public Parent create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        Parent parent = PomFactory.eINSTANCE.createParent();
        compoundCommand.append(SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Parent(), parent));
        return parent;
      }
    };
    setModifyListener(parentGroupIdText, parentProvider, POM_PACKAGE.getParent_GroupId(), "");
    setModifyListener(parentArtifactIdText, parentProvider, POM_PACKAGE.getParent_ArtifactId(), "");
    setModifyListener(parentVersionText, parentProvider, POM_PACKAGE.getParent_Version(), "");
    setModifyListener(parentRealtivePathText, parentProvider, POM_PACKAGE.getParent_RelativePath(), "");
  }

  private void registerOrganizationListnener() {
    ValueProvider<Organization> organizationProvider = new ValueProvider.ParentValueProvider<Organization>(
        organizationNameText, organizationUrlText) {
      public Organization getValue() {
        return model.getOrganization();
      }
      public Organization create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        Organization organization = PomFactory.eINSTANCE.createOrganization();
        compoundCommand.append(SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Organization(), //
            organization));
        return organization;
      }
    };
    setModifyListener(organizationNameText, organizationProvider, POM_PACKAGE.getOrganization_Name(), "");
    setModifyListener(organizationUrlText, organizationProvider, POM_PACKAGE.getOrganization_Url(), "");
  }

  private void registerScmListnener() {
    ValueProvider<Scm> scmProvider = new ValueProvider.ParentValueProvider<Scm>(scmUrlText, scmConnectionText,
        scmDevConnectionText, scmTagText) {
      public Scm getValue() {
        return null;
      }
      public Scm create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        Scm scm = PomFactory.eINSTANCE.createScm();
        compoundCommand.append(SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Scm(), scm));
        return scm;
      }
    };
    setModifyListener(scmUrlText, scmProvider, POM_PACKAGE.getScm_Url(), "");
    setModifyListener(scmConnectionText, scmProvider, POM_PACKAGE.getScm_Connection(), "");
    setModifyListener(scmDevConnectionText, scmProvider, POM_PACKAGE.getScm_DeveloperConnection(), "");
    setModifyListener(scmTagText, scmProvider, POM_PACKAGE.getScm_Tag(), "");

//    ModifyListener scmListener = new ModifyListener() {
//      public void modifyText(ModifyEvent e) {
//        EditingDomain editingDomain = getEditingDomain();
//        CompoundCommand compoundCommand = new CompoundCommand();
//
//        Scm scm = model.getScm();
//        if(isEmpty(scmUrlText) || isEmpty(scmConnectionText) || isEmpty(scmDevConnectionText) || isEmpty(scmTagText)) {
//          // XXX this cause severe issues when last element is deleted in xml
//          //            if(scm!=null) {
//          //              Command command = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Scm(), null);
//          //              compoundCommand.append(command);
//          //            }
//        } else {
//          if(scm == null) {
//            scm = PomFactory.eINSTANCE.createScm();
//            Command command = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Scm(), scm);
//            compoundCommand.append(command);
//          }
//        }
//
//        Text source = (Text) e.getSource();
//        if(scm != null) {
//          Object feature = null;
//          if(scmUrlText == source) {
//            feature = POM_PACKAGE.getScm_Url();
//          } else if(scmConnectionText == source) {
//            feature = POM_PACKAGE.getScm_Connection();
//          } else if(scmDevConnectionText == source) {
//            feature = POM_PACKAGE.getScm_DeveloperConnection();
//          } else if(scmTagText == source) {
//            feature = POM_PACKAGE.getScm_Tag();
//          }
//
//          String text = source.getText().length() == 0 ? null : source.getText();
//          compoundCommand.append(SetCommand.create(editingDomain, scm, feature, text));
//        }
//
//        editingDomain.getCommandStack().execute(compoundCommand);
//      }
//    };
//    scmUrlText.addModifyListener(scmListener);
//    scmConnectionText.addModifyListener(scmListener);
//    scmDevConnectionText.addModifyListener(scmListener);
//    scmTagText.addModifyListener(scmListener);
  }

  private void registerIssueManagementListener() {
    ValueProvider<IssueManagement> issueManagementProvider = new ValueProvider.ParentValueProvider<IssueManagement>(
        issueManagementUrlText, issueManagementSystemText) {
      public IssueManagement getValue() {
        return model.getIssueManagement();
      }
      public IssueManagement create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        IssueManagement issueManagement = PomFactory.eINSTANCE.createIssueManagement();
        compoundCommand.append(SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_IssueManagement(), //
            issueManagement));
        return issueManagement;
      }
    };
    setModifyListener(issueManagementUrlText, issueManagementProvider, POM_PACKAGE.getIssueManagement_Url(), "");
    setModifyListener(issueManagementSystemText, issueManagementProvider, POM_PACKAGE.getIssueManagement_System(), "");
  }
  

  private void registerCiManagementListnener() {
    ValueProvider<CiManagement> ciManagementProvider = new ValueProvider.ParentValueProvider<CiManagement>(
        ciManagementUrlText, ciManagementSystemText) {
      public CiManagement getValue() {
        return model.getCiManagement();
      }
      public CiManagement create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        CiManagement ciManagement = PomFactory.eINSTANCE.createCiManagement();
        compoundCommand.append(SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_CiManagement(), //
            ciManagement));
        return ciManagement;
      }
    };
    setModifyListener(ciManagementUrlText, ciManagementProvider, POM_PACKAGE.getCiManagement_Url(), "");
    setModifyListener(ciManagementSystemText, ciManagementProvider, POM_PACKAGE.getCiManagement_System(), "");
  }

  protected void createNewModule(String moduleName) {
    CompoundCommand compoundCommand = new CompoundCommand();
    EditingDomain editingDomain = getEditingDomain();

    Modules modules = model.getModules();
    if(modules == null) {
      modules = PomFactory.eINSTANCE.createModules();
      Command addModules = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Modules(), modules);
      compoundCommand.append(addModules);
    }
    
    Command addModule = AddCommand.create(editingDomain, modules, POM_PACKAGE.getModules_Module(), moduleName);
    compoundCommand.append(addModule);
    
    editingDomain.getCommandStack().execute(compoundCommand);
    modulesEditor.setInput(model.getModules()==null ? null : model.getModules().getModule());
  }
  
}

