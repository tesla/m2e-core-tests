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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.notify.Notification;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.FileEditorInput;
import org.maven.ide.components.pom.CiManagement;
import org.maven.ide.components.pom.IssueManagement;
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.Modules;
import org.maven.ide.components.pom.Organization;
import org.maven.ide.components.pom.Parent;
import org.maven.ide.components.pom.PomFactory;
import org.maven.ide.components.pom.Properties;
import org.maven.ide.components.pom.Scm;
import org.maven.ide.eclipse.actions.OpenPomAction;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.editor.composites.ListEditorComposite;
import org.maven.ide.eclipse.editor.composites.ListEditorContentProvider;
import org.maven.ide.eclipse.editor.xml.search.Packaging;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.wizards.MavenModuleWizard;
import org.maven.ide.eclipse.wizards.MavenRepositorySearchDialog;
import org.maven.ide.eclipse.wizards.WidthGroup;


/**
 * @author Eugene Kuleshov
 */
public class OverviewPage extends MavenPomEditorPage {

  //controls
  Text artifactIdText;
  Text artifactVersionText;
  Text artifactGroupIdText;
  CCombo artifactPackagingCombo;
  
  Text parentVersionText;
  Text parentArtifactIdText;
  Text parentGroupIdText;
  Text parentRelativePathText;

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

  ListEditorComposite<String> modulesEditor;
  
  PropertiesSection propertiesSection;
  Section modulesSection;
  Section parentSection;
  Section projectSection;
  Section organizationSection;
  Section scmSection;
  Section issueManagementSection;
  Section ciManagementSection;

  private Action newModuleProjectAction;
  private Action parentSelectAction;

  public OverviewPage(MavenPomEditor pomEditor) {
    super(pomEditor, IMavenConstants.PLUGIN_ID + ".pom.overview", "Overview");
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
    Section artifactSection = toolkit.createSection(composite, ExpandableComposite.TITLE_BAR);
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
    artifactGroupIdText.setData("name", "groupId");
    GridData gd_artifactGroupIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_artifactGroupIdText.horizontalIndent = 4;
    artifactGroupIdText.setLayoutData(gd_artifactGroupIdText);
    FormUtils.addGroupIdProposal(artifactGroupIdText, Packaging.ALL);
  
    Label artifactIdLabel = toolkit.createLabel(artifactComposite, "Artifact Id:*", SWT.NONE);
  
    artifactIdText = toolkit.createText(artifactComposite, null, SWT.NONE);
    artifactIdText.setData("name", "artifactId");
    GridData gd_artifactIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_artifactIdText.horizontalIndent = 4;
    artifactIdText.setLayoutData(gd_artifactIdText);
  
    Label versionLabel = toolkit.createLabel(artifactComposite, "Version:", SWT.NONE);
  
    artifactVersionText = toolkit.createText(artifactComposite, null, SWT.NONE);
    GridData gd_versionText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_versionText.horizontalIndent = 4;
    gd_versionText.widthHint = 200;
    artifactVersionText.setLayoutData(gd_versionText);
    artifactVersionText.setData("name", "version");
  
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
    gd_packagingText.horizontalIndent = 4;
    gd_packagingText.widthHint = 120;
    artifactPackagingCombo.setLayoutData(gd_packagingText);
    artifactPackagingCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    artifactPackagingCombo.setData("name", "packaging");
    toolkit.paintBordersFor(artifactPackagingCombo);
    
    widthGroup.addControl(groupIdLabel);
    widthGroup.addControl(artifactIdLabel);
    widthGroup.addControl(versionLabel);
    widthGroup.addControl(packagingLabel);
    
    toolkit.paintBordersFor(artifactComposite);
  }

  private void createParentsection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    parentSection = toolkit.createSection(composite, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    parentSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    parentSection.setText("Parent");
    parentSection.setData("name", "parentSection");
  
    parentSelectAction = new Action("Select Parent", MavenEditorImages.SELECT_ARTIFACT) {
      public void run() {
        // TODO calculate current list of artifacts for the project
        Set<ArtifactKey> artifacts = Collections.emptySet();
        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getEditorSite().getShell(), //
            "Add Dependency", IndexManager.SEARCH_ARTIFACT, artifacts, false);
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            parentGroupIdText.setText(nvl(af.group));
            parentArtifactIdText.setText(nvl(af.artifact));
            parentVersionText.setText(nvl(af.version));
          }
        }
      }
    };
    parentSelectAction.setEnabled(false);

    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
    toolBarManager.add(parentSelectAction);
    
    Composite toolbarComposite = toolkit.createComposite(parentSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);
 
    toolBarManager.createControl(toolbarComposite);
    parentSection.setTextClient(toolbarComposite);    
    
    Composite parentComposite = toolkit.createComposite(parentSection, SWT.NONE);
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginBottom = 5;
    gridLayout.marginWidth = 1;
    gridLayout.marginHeight = 2;
    parentComposite.setLayout(gridLayout);
    parentSection.setClient(parentComposite);

    Label parentGroupIdLabel = toolkit.createLabel(parentComposite, "Group Id:*", SWT.NONE);
  
    parentGroupIdText = toolkit.createText(parentComposite, null, SWT.NONE);
    GridData gd_parentGroupIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_parentGroupIdText.horizontalIndent = 4;
    parentGroupIdText.setLayoutData(gd_parentGroupIdText);
    parentGroupIdText.setData("name", "parentGroupId");
    FormUtils.addGroupIdProposal(parentGroupIdText, Packaging.POM);
    
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
    GridData gd_parentArtifactIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_parentArtifactIdText.horizontalIndent = 4;
    parentArtifactIdText.setLayoutData(gd_parentArtifactIdText);
    parentArtifactIdText.setData("name", "parentArtifactId");
    FormUtils.addArtifactIdProposal(parentGroupIdText, parentArtifactIdText, Packaging.POM);
  
    Label parentVersionLabel = toolkit.createLabel(parentComposite, "Version:*", SWT.NONE);
    parentVersionLabel.setLayoutData(new GridData());
  
    parentVersionText = toolkit.createText(parentComposite, null, SWT.NONE);
    GridData parentVersionTextData = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    parentVersionTextData.horizontalIndent = 4;
    parentVersionTextData.widthHint = 200;
    parentVersionText.setLayoutData(parentVersionTextData);
    parentVersionText.setData("name", "parentVersion");
    FormUtils.addVersionProposal(parentGroupIdText, parentArtifactIdText, parentVersionText, Packaging.POM);
  
//    Button parentSelectButton = toolkit.createButton(parentComposite, "Select...", SWT.NONE);
//    parentSelectButton.addSelectionListener(new SelectionAdapter() {
//      public void widgetSelected(SelectionEvent e) {
//        // TODO calculate current list of artifacts for the project
//        Set<Artifact> artifacts = Collections.emptySet();
//        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getEditorSite().getShell(),
//            "Add Dependency", IndexManager.SEARCH_ARTIFACT, artifacts);
//        if(dialog.open() == Window.OK) {
//          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
//          if(af != null) {
//            parentGroupIdText.setText(nvl(af.group));
//            parentArtifactIdText.setText(nvl(af.artifact));
//            parentVersionText.setText(nvl(af.version));
//          }
//        }
//      }
//    });

    Label parentRealtivePathLabel = toolkit.createLabel(parentComposite, "Relative Path:", SWT.NONE);

    parentRelativePathText = toolkit.createText(parentComposite, null, SWT.NONE);
    GridData gd_parentRelativePathText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_parentRelativePathText.horizontalIndent = 4;
    parentRelativePathText.setLayoutData(gd_parentRelativePathText);
    parentRelativePathText.setData("name", "parentRelativePath");
    
    widthGroup.addControl(parentGroupIdLabel);
    widthGroup.addControl(parentArtifactIdLabel);
    widthGroup.addControl(parentVersionLabel);
    widthGroup.addControl(parentRealtivePathLabel);
    
    toolkit.paintBordersFor(parentComposite);
    parentComposite.setTabList(new Control[] {parentGroupIdText, parentArtifactIdText, parentVersionText, parentRelativePathText});
  }

  private void createPropertiesSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    propertiesSection = new PropertiesSection(toolkit, composite, getEditingDomain());
  }

  private void createModulesSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    // XXX should disable Modules actions based on artifact packaging and only add modules when packaging is "pom"

    modulesSection = toolkit.createSection(composite, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    modulesSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    modulesSection.setText("Modules");
    modulesSection.setData("name", "modulesSection");

    modulesEditor = new ListEditorComposite<String>(modulesSection, SWT.NONE);
    modulesEditor.getViewer().getTable().setData("name", "modulesEditor");
    toolkit.paintBordersFor(modulesEditor);
    toolkit.adapt(modulesEditor);
    modulesSection.setClient(modulesEditor);

    modulesEditor.setContentProvider(new ListEditorContentProvider<String>());
    modulesEditor.setLabelProvider(new ModulesLabelProvider(this));
    
    modulesEditor.setOpenListener(new IOpenListener() {
      public void open(OpenEvent openevent) {
        List<String> selection = modulesEditor.getSelection();
        for(String module : selection) {
          IMavenProjectFacade projectFacade = findModuleProject(module);
          if(projectFacade!=null) {
            ArtifactKey mavenProject = projectFacade.getArtifactKey();
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
        int n = modulesEditor.getViewer().getTable().getSelectionIndex();
        Modules modules = model.getModules();
        if(!value.equals(modules.getModule().get(n))) {
          EditingDomain editingDomain = getEditingDomain();
          Command command = SetCommand.create(editingDomain, modules, //
              POM_PACKAGE.getModules_Module(), value, n);
          editingDomain.getCommandStack().execute(command);
          // modulesEditor.refresh();
        }
      }
    });
    
    newModuleProjectAction = new Action("New module project", MavenEditorImages.ADD_MODULE) {
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
    projectSection = toolkit.createSection(composite, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    projectSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    projectSection.setText("Project");
    projectSection.setData("name", "projectSection");
  
    Composite projectComposite = toolkit.createComposite(projectSection, SWT.NONE);
    projectComposite.setLayout(new GridLayout(2, false));
    projectSection.setClient(projectComposite);
  
    Label nameLabel = toolkit.createLabel(projectComposite, "Name:", SWT.NONE);
  
    projectNameText = toolkit.createText(projectComposite, null, SWT.NONE);
    GridData gd_projectNameText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_projectNameText.widthHint = 150;
    projectNameText.setLayoutData(gd_projectNameText);
    projectNameText.setData("name", "projectName");
  
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
    projectUrlText.setData("name", "projectUrl");
  
    Label descriptionLabel = toolkit.createLabel(projectComposite, "Description:", SWT.NONE);
    descriptionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
  
    projectDescriptionText = toolkit.createText(projectComposite, null, SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
    GridData gd_descriptionText = new GridData(SWT.FILL, SWT.FILL, true, false);
    gd_descriptionText.widthHint = 150;
    gd_descriptionText.heightHint = 55;
    projectDescriptionText.setLayoutData(gd_descriptionText);
    projectDescriptionText.setData("name", "projectDescription");
  
    Label inceptionYearLabel = toolkit.createLabel(projectComposite, "Inception:", SWT.NONE);
  
    inceptionYearText = toolkit.createText(projectComposite, null, SWT.NONE);
    GridData gd_inceptionYearText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_inceptionYearText.widthHint = 150;
    inceptionYearText.setLayoutData(gd_inceptionYearText);
    inceptionYearText.setData("name", "projectInceptionYear");
    
    widthGroup.addControl(nameLabel);
    widthGroup.addControl(urlLabel);
    widthGroup.addControl(descriptionLabel);
    widthGroup.addControl(inceptionYearLabel);
    
    toolkit.paintBordersFor(projectComposite);
    projectComposite.setTabList(new Control[] {projectNameText, projectUrlText, projectDescriptionText, inceptionYearText});
  }

  private void createOrganizationSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    organizationSection = toolkit.createSection(composite, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    organizationSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    organizationSection.setText("Organization");
    organizationSection.setData("name", "organizationSection");
  
    Composite organizationComposite = toolkit.createComposite(organizationSection, SWT.NONE);
    organizationComposite.setLayout(new GridLayout(2, false));
    organizationSection.setClient(organizationComposite);
  
    Label organizationNameLabel = toolkit.createLabel(organizationComposite, "Name:", SWT.NONE);
  
    organizationNameText = toolkit.createText(organizationComposite, null, SWT.NONE);
    GridData gd_organizationNameText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_organizationNameText.widthHint = 150;
    organizationNameText.setLayoutData(gd_organizationNameText);
    organizationNameText.setData("name", "organizationName");

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
    organizationUrlText.setData("name", "organizationUrl");
    
    widthGroup.addControl(organizationNameLabel);
    widthGroup.addControl(organizationUrlLabel);
    
    toolkit.paintBordersFor(organizationComposite);
    organizationComposite.setTabList(new Control[] {organizationNameText, organizationUrlText});
  }

  private void createScmSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    scmSection = toolkit.createSection(composite, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    GridData gd_scmSection = new GridData(SWT.FILL, SWT.TOP, false, false);
    scmSection.setLayoutData(gd_scmSection);
    scmSection.setText("SCM");
    scmSection.setData("name", "scmSection");
  
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
    scmUrlText.setData("name", "scmUrl");
  
    Label scmConnectionLabel = toolkit.createLabel(scmComposite, "Connection:", SWT.NONE);
  
    scmConnectionText = toolkit.createText(scmComposite, null, SWT.NONE);
    GridData gd_scmConnectionText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_scmConnectionText.widthHint = 150;
    scmConnectionText.setLayoutData(gd_scmConnectionText);
    scmConnectionText.setData("name", "scmConnection");
  
    Label scmDevConnectionLabel = toolkit.createLabel(scmComposite, "Developer:", SWT.NONE);
  
    scmDevConnectionText = toolkit.createText(scmComposite, null, SWT.NONE);
    GridData gd_scmDevConnectionText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_scmDevConnectionText.widthHint = 150;
    scmDevConnectionText.setLayoutData(gd_scmDevConnectionText);
    scmDevConnectionText.setData("name", "scmDevConnection");
  
    Label scmTagLabel = toolkit.createLabel(scmComposite, "Tag:", SWT.NONE);
  
    scmTagText = toolkit.createText(scmComposite, null, SWT.NONE);
    GridData gd_scmTagText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_scmTagText.widthHint = 150;
    scmTagText.setLayoutData(gd_scmTagText);
    scmTagText.setData("name", "scmTag");
    
    widthGroup.addControl(scmUrlLabel);
    widthGroup.addControl(scmConnectionLabel);
    widthGroup.addControl(scmDevConnectionLabel);
    widthGroup.addControl(scmTagLabel);
    
    toolkit.paintBordersFor(scmComposite);
  }

  private void createIssueManagementSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    issueManagementSection = toolkit.createSection(composite, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    issueManagementSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    issueManagementSection.setText("Issue Management");
    issueManagementSection.setData("name", "issueManagementSection");
  
    Composite issueManagementComposite = toolkit.createComposite(issueManagementSection, SWT.NONE);
    issueManagementComposite.setLayout(new GridLayout(2, false));
    issueManagementSection.setClient(issueManagementComposite);
  
    Label issueManagementSystemLabel = toolkit.createLabel(issueManagementComposite, "System:", SWT.NONE);
  
    issueManagementSystemText = toolkit.createText(issueManagementComposite, null, SWT.NONE);
    GridData gd_issueManagementSystemText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_issueManagementSystemText.widthHint = 150;
    issueManagementSystemText.setLayoutData(gd_issueManagementSystemText);
    issueManagementSystemText.setData("name", "issueManagementSystem");
  
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
    issueManagementUrlText.setData("name", "issueManagementUrl");
    
    widthGroup.addControl(issueManagementSystemLabel);
    widthGroup.addControl(issueManagementUrlLabel);
    
    toolkit.paintBordersFor(issueManagementComposite);
    issueManagementComposite.setTabList(new Control[] {issueManagementSystemText, issueManagementUrlText});
  }

  private void createCiManagementSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    ciManagementSection = toolkit.createSection(composite, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    ciManagementSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    ciManagementSection.setText("Continuous Integration");
    ciManagementSection.setData("name", "continuousIntegrationSection");
  
    Composite ciManagementComposite = toolkit.createComposite(ciManagementSection, SWT.NONE);
    ciManagementComposite.setLayout(new GridLayout(2, false));
    ciManagementSection.setClient(ciManagementComposite);
  
    Label ciManagementSystemLabel = toolkit.createLabel(ciManagementComposite, "System:", SWT.NONE);
  
    ciManagementSystemText = toolkit.createText(ciManagementComposite, null, SWT.NONE);
    GridData gd_ciManagementSystemText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_ciManagementSystemText.widthHint = 150;
    ciManagementSystemText.setLayoutData(gd_ciManagementSystemText);
    ciManagementSystemText.setData("name", "ciManagementSystem");
  
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
    ciManagementUrlText.setData("name", "ciManagementUrl");
    
    widthGroup.addControl(ciManagementSystemLabel);
    widthGroup.addControl(ciManagementUrlLabel);
    
    toolkit.paintBordersFor(ciManagementComposite);
    ciManagementComposite.setTabList(new Control[] {ciManagementSystemText, ciManagementUrlText});
  }

  public void updateView(Notification notification) {
    EObject object = (EObject) notification.getNotifier();
    if (object instanceof Model) {
      loadThis();
      modulesEditor.refresh();
      propertiesSection.refresh();
    }

    Object notificationObject = getFromNotification(notification);
    
    if(object instanceof Parent && (notificationObject == null || notificationObject instanceof Parent)) {
      loadParent((Parent) notificationObject);
    }

    if(object instanceof Organization && (notificationObject == null || notificationObject instanceof Organization)) {
      loadOrganization((Organization) notificationObject);
    }

    if(object instanceof Scm && (notificationObject == null || notificationObject instanceof Scm)) {
      loadScm((Scm) notificationObject);
    }

    if(object instanceof CiManagement && (notificationObject == null || notificationObject instanceof CiManagement)) {
      loadCiManagement((CiManagement) notificationObject);
    }

    if(object instanceof IssueManagement
        && (notificationObject == null || notificationObject instanceof IssueManagement)) {
      loadIssueManagement((IssueManagement) notificationObject);
    }
    
    if(object instanceof Modules) {
      modulesEditor.refresh();
    }
    
    if(notificationObject instanceof Properties) {
      propertiesSection.setModel(model, POM_PACKAGE.getModel_Properties());
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
    propertiesSection.setModel(model, POM_PACKAGE.getModel_Properties());
    
    projectSection.setExpanded(!isEmpty(model.getName()) || !isEmpty(model.getDescription())
        || !isEmpty(model.getUrl()) || !isEmpty(model.getInceptionYear()));
    
    parentSection.setExpanded(parent != null //
        && (!isEmpty(parent.getGroupId()) || !isEmpty(parent.getArtifactId()) //
            || !isEmpty(parent.getVersion()) || !isEmpty(parent.getRelativePath())));

    organizationSection.setExpanded(organization != null
        && (!isEmpty(organization.getName()) || !isEmpty(organization.getUrl())));
    
    scmSection.setExpanded(scm != null
        && (!isEmpty(scm.getUrl()) || !isEmpty(scm.getConnection()) || !isEmpty(scm.getDeveloperConnection())));
    
    ciManagementSection.setExpanded(ciManagement != null
        && (!isEmpty(ciManagement.getSystem()) || !isEmpty(ciManagement.getUrl())));
    
    issueManagementSection.setExpanded(issueManagement != null
        && (!isEmpty(issueManagement.getSystem()) || !isEmpty(issueManagement.getUrl())));

    propertiesSection.getSection().setExpanded(model.getProperties() != null && !model.getProperties().getProperty().isEmpty());
    
    // Modules modules = model.getModules();
    // modulesSection.setExpanded(modules !=null && modules.getModule().size()>0);
  }

  private void loadThis() {
    removeNotifyListener(artifactGroupIdText);
    removeNotifyListener(artifactIdText);
    removeNotifyListener(artifactVersionText);
    removeNotifyListener(artifactPackagingCombo);

    removeNotifyListener(projectNameText);
    removeNotifyListener(projectDescriptionText);
    removeNotifyListener(projectUrlText);
    removeNotifyListener(inceptionYearText);
    
    setText(artifactGroupIdText, model.getGroupId());
    setText(artifactIdText, model.getArtifactId());
    setText(artifactVersionText, model.getVersion());
    setText(artifactPackagingCombo, model.getPackaging());
    
    setText(projectNameText, model.getName());
    setText(projectDescriptionText, model.getDescription());
    setText(projectUrlText, model.getUrl());
    setText(inceptionYearText, model.getInceptionYear());

    ValueProvider<Model> modelProvider = new ValueProvider.DefaultValueProvider<Model>(model);
    setModifyListener(artifactGroupIdText, modelProvider, POM_PACKAGE.getModel_GroupId(), "");
    setModifyListener(artifactIdText, modelProvider, POM_PACKAGE.getModel_ArtifactId(), "");
    setModifyListener(artifactVersionText, modelProvider, POM_PACKAGE.getModel_Version(), "");
    setModifyListener(artifactPackagingCombo, modelProvider, POM_PACKAGE.getModel_Packaging(), "jar");
    
    setModifyListener(projectNameText, modelProvider, POM_PACKAGE.getModel_Name(), "");
    setModifyListener(projectDescriptionText, modelProvider, POM_PACKAGE.getModel_Description(), "");
    setModifyListener(projectUrlText, modelProvider, POM_PACKAGE.getModel_Url(), "");
    setModifyListener(inceptionYearText, modelProvider, POM_PACKAGE.getModel_InceptionYear(), "");
  }

  private void loadParent(Parent parent) {
    removeNotifyListener(parentGroupIdText);
    removeNotifyListener(parentArtifactIdText);
    removeNotifyListener(parentVersionText);
    removeNotifyListener(parentRelativePathText);

    if(parent!=null) {
      setText(parentGroupIdText, parent.getGroupId());
      setText(parentArtifactIdText, parent.getArtifactId());
      setText(parentVersionText, parent.getVersion());
      setText(parentRelativePathText, parent.getRelativePath());
    } else {
      setText(parentGroupIdText, "");
      setText(parentArtifactIdText, "");
      setText(parentVersionText, "");
      setText(parentRelativePathText, "");
    }
    
//    parentGroupIdText.setEditable(!isReadOnly());
//    parentArtifactIdText.setEditable(!isReadOnly());
//    parentVersionText.setEditable(!isReadOnly());
//    parentRelativePathText.setEditable(!isReadOnly());
    parentSelectAction.setEnabled(!isReadOnly());
    
    ValueProvider<Parent> parentProvider = new ValueProvider.ParentValueProvider<Parent>(parentGroupIdText,
        parentArtifactIdText, parentVersionText, parentRelativePathText) {
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
    setModifyListener(parentRelativePathText, parentProvider, POM_PACKAGE.getParent_RelativePath(), "");
  }
  
  private void loadModules(Modules modules) {
    modulesEditor.setInput(modules==null ? null : modules.getModule());
    modulesEditor.setReadOnly(isReadOnly());
    newModuleProjectAction.setEnabled(!isReadOnly());
  }
  
  private void loadOrganization(Organization organization) {
    removeNotifyListener(organizationNameText);
    removeNotifyListener(organizationUrlText);

    if(organization==null) {
      setText(organizationNameText, "");
      setText(organizationUrlText, "");
    } else {
      setText(organizationNameText, organization.getName());
      setText(organizationUrlText, organization.getUrl());
    }
    
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

  private void loadScm(Scm scm) {
    if(scm==null) {
      setText(scmUrlText, "");
      setText(scmConnectionText, "");
      setText(scmDevConnectionText, "");
      setText(scmTagText, "");
    } else {
      setText(scmUrlText, scm.getUrl());
      setText(scmConnectionText, scm.getConnection());
      setText(scmDevConnectionText, scm.getDeveloperConnection());
      setText(scmTagText, scm.getTag());
    }
    
    ValueProvider<Scm> scmProvider = new ValueProvider.ParentValueProvider<Scm>(scmUrlText, scmConnectionText,
        scmDevConnectionText, scmTagText) {
      public Scm getValue() {
        return model.getScm();
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
  }

  private void loadCiManagement(CiManagement ciManagement) {
    removeNotifyListener(ciManagementUrlText);
    removeNotifyListener(ciManagementSystemText);

    if(ciManagement==null) {
      setText(ciManagementSystemText, "");
      setText(ciManagementUrlText, "");
    } else {
      setText(ciManagementSystemText, ciManagement.getSystem());
      setText(ciManagementUrlText, ciManagement.getUrl());
    }
    
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

  private void loadIssueManagement(IssueManagement issueManagement) {
    removeNotifyListener(issueManagementUrlText);
    removeNotifyListener(issueManagementSystemText);

    if(issueManagement==null) {
      setText(issueManagementSystemText, "");
      setText(issueManagementUrlText, "");
    } else {
      setText(issueManagementSystemText, issueManagement.getSystem());
      setText(issueManagementUrlText, issueManagement.getUrl());
    }
    
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

