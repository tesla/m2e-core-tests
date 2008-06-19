/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.composites;

import static org.maven.ide.eclipse.editor.pom.FormUtils.isEmpty;
import static org.maven.ide.eclipse.editor.pom.FormUtils.setText;
import static org.maven.ide.eclipse.editor.pom.FormUtils.setButton;

import java.util.Collections;
import java.util.List;

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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.maven.ide.components.pom.DeploymentRepository;
import org.maven.ide.components.pom.DistributionManagement;
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.PluginRepositories;
import org.maven.ide.components.pom.PomFactory;
import org.maven.ide.components.pom.PomPackage;
import org.maven.ide.components.pom.Relocation;
import org.maven.ide.components.pom.Repositories;
import org.maven.ide.components.pom.Repository;
import org.maven.ide.components.pom.RepositoryPolicy;
import org.maven.ide.components.pom.Site;
import org.maven.ide.eclipse.actions.OpenPomAction;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.editor.pom.FormUtils;
import org.maven.ide.eclipse.editor.pom.MavenPomEditorPage;
import org.maven.ide.eclipse.editor.pom.ValueProvider;
import org.maven.ide.eclipse.wizards.WidthGroup;

/**
 * @author Eugene Kuleshov
 */
public class RepositoriesComposite extends Composite {

  static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;

  MavenPomEditorPage parent;

  FormToolkit toolkit = new FormToolkit(Display.getCurrent());
  
  // controls
  
  ListEditorComposite<Repository> repositoriesEditor;
  ListEditorComposite<Repository> pluginRepositoriesEditor;

  Section repositoryDetailsSection;

  Section releaseRepositorySection;
  Section snapshotRepositorySection;
  Section projectSiteSection;
  Section relocationSection;
  
  Text repositoryIdText;
  Text repositoryNameText;
  Text repositoryUrlText;
  CCombo repositoryLayoutCombo;

  Button releasesEnabledButton;
  CCombo releasesUpdatePolicyCombo;
  CCombo releasesChecksumPolicyCombo;
  Label releasesChecksumPolicyLabel;
  Label releasesUpdatePolicyLabel;

  Button snapshotsEnabledButton;
  CCombo snapshotsUpdatePolicyCombo;
  CCombo snapshotsChecksumPolicyCombo;
  Label snapshotsChecksumPolicyLabel;
  Label snapshotsUpdatePolicyLabel;

  Text projectSiteIdText;
  Text projectSiteNameText;
  Text projectSiteUrlText;
  Text projectDownloadUrlText;
  
  Text relocationGroupIdText;
  Text relocationArtifactIdText;
  Text relocationVersionText;
  Text relocationMessageText;
  
  Text snapshotRepositoryIdText;
  Text snapshotRepositoryNameText;
  Text snapshotRepositoryUrlText;
  CCombo snapshotRepositoryLayoutCombo;
  Button snapshotRepositoryUniqueVersionButton;
  
  Text releaseRepositoryIdText;
  Text releaseRepositoryNameText;
  Text releaseRepositoryUrlText;
  CCombo releaseRepositoryLayoutCombo;
  Button releaseRepositoryUniqueVersionButton;
  
  WidthGroup leftWidthGroup = new WidthGroup();
  WidthGroup rightWidthGroup = new WidthGroup();
  
  Composite projectSiteComposite;
  Composite releaseDistributionRepositoryComposite;
  Composite relocationComposite;
  Composite snapshotRepositoryComposite;
  
  boolean changingSelection = false;
  
  // model
  
  Model model;
  Repository currentRepository;

  
  public RepositoriesComposite(Composite parent, int flags) {
    super(parent, flags);
    
    toolkit.adapt(this);
  
    GridLayout gridLayout = new GridLayout(1, true);
    gridLayout.marginWidth = 0;
    setLayout(gridLayout);

    SashForm horizontalSash = new SashForm(this, SWT.NONE);
    horizontalSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    SashForm verticalSash = new SashForm(horizontalSash, SWT.VERTICAL);
    toolkit.adapt(verticalSash, true, true);

    createRepositoriesSection(verticalSash);
    createPluginRepositoriesSection(verticalSash);

    verticalSash.setWeights(new int[] {1, 1});
    
    createRepositoryDetailsSection(horizontalSash);
    
    toolkit.adapt(horizontalSash, true, true);
    horizontalSash.setWeights(new int[] {1, 1});
    
    SashForm repositoriesSash = new SashForm(this, SWT.NONE);
    repositoriesSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    toolkit.adapt(repositoriesSash, true, true);

    createReleaseRepositorySection(repositoriesSash);
    createSnapshotRepositorySection(repositoriesSash);
    
    repositoriesSash.setWeights(new int[] {1, 1});
    
    SashForm projectSiteSash = new SashForm(this, SWT.NONE);
    projectSiteSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    toolkit.adapt(projectSiteSash, true, true);

    createProjectSiteSection(projectSiteSash);
    createRelocationSection(projectSiteSash);
    
    projectSiteSash.setWeights(new int[] {1, 1});
  }

  public void dispose() {
    // projectSiteComposite.removeControlListener(leftWidthGroup);
    // releaseDistributionRepositoryComposite.removeControlListener(leftWidthGroup);
    
    // snapshotRepositoryComposite.removeControlListener(rightWidthGroup);
    // relocationComposite.removeControlListener(rightWidthGroup);
    
    super.dispose();
  }
  
  private void createRepositoriesSection(SashForm verticalSash) {
    Section repositoriesSection = toolkit.createSection(verticalSash, Section.TITLE_BAR | Section.COMPACT);
    repositoriesSection.setText("Repositories");

    repositoriesEditor = new ListEditorComposite<Repository>(repositoriesSection, SWT.NONE);

    repositoriesEditor.setLabelProvider(new RepositoryLabelProvider());
    repositoriesEditor.setContentProvider(new ListEditorContentProvider<Repository>());

    repositoriesEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Repository> selection = repositoriesEditor.getSelection();
        updateRepositoryDetailsSection(selection.size() == 1 ? selection.get(0) : null);
        
        if(!selection.isEmpty()) {
          changingSelection = true;
          pluginRepositoriesEditor.setSelection(Collections.<Repository>emptyList());
          changingSelection = false;
        }
      }
    });
    
    repositoriesEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
        
        Repositories repositories = model.getRepositories();
        if(repositories == null) {
          repositories = PomFactory.eINSTANCE.createRepositories();
          Command createCommand = SetCommand.create(editingDomain, model, 
              POM_PACKAGE.getModel_Repositories(), repositories);
          compoundCommand.append(createCommand);
        }
        
        Repository repository = PomFactory.eINSTANCE.createRepository();
        Command addCommand = AddCommand.create(editingDomain, repositories, 
            POM_PACKAGE.getRepositories_Repository(), repository);
        compoundCommand.append(addCommand);
        
        editingDomain.getCommandStack().execute(compoundCommand);
        
        repositoriesEditor.setSelection(Collections.singletonList(repository));
        updateRepositoryDetailsSection(repository);
        repositoryIdText.setFocus();
      }
    });
 
    repositoriesEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
 
        List<Repository> list = repositoriesEditor.getSelection();
        for(Repository repository : list) {
          Command removeCommand = RemoveCommand.create(editingDomain, model.getRepositories(), 
              POM_PACKAGE.getRepositories_Repository(), repository);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
        updateRepositoryDetailsSection(null);
      }
    });

    toolkit.paintBordersFor(repositoriesEditor);
    toolkit.adapt(repositoriesEditor);
    repositoriesSection.setClient(repositoriesEditor);
  }

  private void createPluginRepositoriesSection(SashForm verticalSash) {
    Section pluginRepositoriesSection = toolkit.createSection(verticalSash, Section.TITLE_BAR | Section.COMPACT);
    pluginRepositoriesSection.setText("Plugin Repositories");
  
    pluginRepositoriesEditor = new ListEditorComposite<Repository>(pluginRepositoriesSection, SWT.NONE);
  
    pluginRepositoriesEditor.setLabelProvider(new RepositoryLabelProvider());
    pluginRepositoriesEditor.setContentProvider(new ListEditorContentProvider<Repository>());
    
    pluginRepositoriesEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Repository> selection = pluginRepositoriesEditor.getSelection();
        updateRepositoryDetailsSection(selection.size()==1 ? selection.get(0) : null);
        
        if(!selection.isEmpty()) {
          changingSelection = true;
          repositoriesEditor.setSelection(Collections.<Repository>emptyList());
          changingSelection = false;
        }
      }
    });
    
    pluginRepositoriesEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
        
        PluginRepositories pluginRepositories = model.getPluginRepositories();
        if(pluginRepositories == null) {
          pluginRepositories = PomFactory.eINSTANCE.createPluginRepositories();
          Command createCommand = SetCommand.create(editingDomain, model, 
              POM_PACKAGE.getModel_PluginRepositories(), pluginRepositories);
          compoundCommand.append(createCommand);
        }
        
        Repository pluginRepository = PomFactory.eINSTANCE.createRepository();
        Command addCommand = AddCommand.create(editingDomain, pluginRepositories, 
            POM_PACKAGE.getPluginRepositories_PluginRepository(), pluginRepository);
        compoundCommand.append(addCommand);
        
        editingDomain.getCommandStack().execute(compoundCommand);
        
        pluginRepositoriesEditor.setSelection(Collections.singletonList(pluginRepository));
        updateRepositoryDetailsSection(pluginRepository);
        repositoryIdText.setFocus();
      }
    });
    
    pluginRepositoriesEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
 
        List<Repository> list = pluginRepositoriesEditor.getSelection();
        for(Repository repository : list) {
          Command removeCommand = RemoveCommand.create(editingDomain, model.getPluginRepositories(), 
              POM_PACKAGE.getPluginRepositories_PluginRepository(), repository);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
        updateRepositoryDetailsSection(null);
      }
    });
    
    toolkit.paintBordersFor(pluginRepositoriesEditor);
    toolkit.adapt(pluginRepositoriesEditor);
    pluginRepositoriesSection.setClient(pluginRepositoriesEditor);
  }

  private void createRepositoryDetailsSection(Composite parent) {
    repositoryDetailsSection = toolkit.createSection(parent, Section.TITLE_BAR);
    repositoryDetailsSection.setText("Repository Details");
  
    Composite repositoryDetailsComposite = toolkit.createComposite(repositoryDetailsSection);
    repositoryDetailsComposite.setLayout(new GridLayout(2, false));
    repositoryDetailsSection.setClient(repositoryDetailsComposite);
    toolkit.paintBordersFor(repositoryDetailsComposite);
    
    Label idLabel = new Label(repositoryDetailsComposite, SWT.NONE);
    idLabel.setText("Id:*");
  
    repositoryIdText = toolkit.createText(repositoryDetailsComposite, "");
    GridData gd_repositoryIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_repositoryIdText.widthHint = 100;
    repositoryIdText.setLayoutData(gd_repositoryIdText);
  
    Label nameLabel = new Label(repositoryDetailsComposite, SWT.NONE);
    nameLabel.setText("Name:");
  
    repositoryNameText = toolkit.createText(repositoryDetailsComposite, "");
    repositoryNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    Hyperlink repositoryUrlHyperlink = toolkit.createHyperlink(repositoryDetailsComposite, "URL:*", SWT.NONE);
    repositoryUrlHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(repositoryUrlText.getText());
      }
    });
  
    repositoryUrlText = toolkit.createText(repositoryDetailsComposite, "");
    GridData gd_repositoryUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_repositoryUrlText.widthHint = 100;
    repositoryUrlText.setLayoutData(gd_repositoryUrlText);
  
    Label layoutLabel = new Label(repositoryDetailsComposite, SWT.NONE);
    layoutLabel.setText("Layout:");
  
    repositoryLayoutCombo = new CCombo(repositoryDetailsComposite, SWT.FLAT);
    repositoryLayoutCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    repositoryLayoutCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    repositoryLayoutCombo.setItems(new String[] {"default", "legacy"});
  
    Composite composite = new Composite(repositoryDetailsComposite, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
    toolkit.adapt(composite, true, true);
    toolkit.paintBordersFor(composite);
    GridLayout compositeLayout = new GridLayout();
    compositeLayout.marginBottom = 2;
    compositeLayout.marginWidth = 2;
    compositeLayout.marginHeight = 0;
    compositeLayout.numColumns = 2;
    composite.setLayout(compositeLayout);
  
    releasesEnabledButton = toolkit.createButton(composite, "Enable Releases", SWT.CHECK | SWT.FLAT);
    GridData releasesEnabledButtonData = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    releasesEnabledButtonData.verticalIndent = 5;
    releasesEnabledButton.setLayoutData(releasesEnabledButtonData);
    releasesEnabledButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        boolean isEnabled = releasesEnabledButton.getSelection();
        releasesUpdatePolicyLabel.setEnabled(isEnabled);
        releasesUpdatePolicyCombo.setEnabled(isEnabled);
        releasesChecksumPolicyLabel.setEnabled(isEnabled);
        releasesChecksumPolicyCombo.setEnabled(isEnabled);
      }
    });
  
    releasesUpdatePolicyLabel = new Label(composite, SWT.NONE);
    releasesUpdatePolicyLabel.setText("Update Policy:");
    GridData releasesUpdatePolicyLabelData = new GridData();
    releasesUpdatePolicyLabelData.horizontalIndent = 15;
    releasesUpdatePolicyLabel.setLayoutData(releasesUpdatePolicyLabelData);
  
    releasesUpdatePolicyCombo = new CCombo(composite, SWT.FLAT);
    releasesUpdatePolicyCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    releasesUpdatePolicyCombo.setItems(new String[] {"daily", "always", "interval:30", "never"});
    releasesUpdatePolicyCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    toolkit.adapt(releasesUpdatePolicyCombo, true, true);
  
    releasesChecksumPolicyLabel = new Label(composite, SWT.NONE);
    releasesChecksumPolicyLabel.setText("Checksum Policy:");
    GridData releasesChecksumPolicyLabelData = new GridData();
    releasesChecksumPolicyLabelData.horizontalIndent = 15;
    releasesChecksumPolicyLabel.setLayoutData(releasesChecksumPolicyLabelData);
  
    releasesChecksumPolicyCombo = new CCombo(composite, SWT.READ_ONLY | SWT.FLAT);
    toolkit.adapt(releasesChecksumPolicyCombo, true, true);
    releasesChecksumPolicyCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    releasesChecksumPolicyCombo.setItems(new String[] {"ignore", "fail", "warn"});
    releasesChecksumPolicyCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
  
    snapshotsEnabledButton = toolkit.createButton(composite, "Enable Snapshots", SWT.CHECK | SWT.FLAT);
    GridData snapshotsEnabledButtonData = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    snapshotsEnabledButtonData.verticalIndent = 5;
    snapshotsEnabledButton.setLayoutData(snapshotsEnabledButtonData);
    snapshotsEnabledButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        boolean isEnabled = releasesEnabledButton.getSelection();
        snapshotsUpdatePolicyLabel.setEnabled(isEnabled);
        snapshotsUpdatePolicyCombo.setEnabled(isEnabled);
        snapshotsChecksumPolicyLabel.setEnabled(isEnabled);
        snapshotsChecksumPolicyCombo.setEnabled(isEnabled);
      }
    });
  
    snapshotsUpdatePolicyLabel = new Label(composite, SWT.NONE);
    snapshotsUpdatePolicyLabel.setText("Update Policy:");
    GridData snapshotsUpdatePolicyLabelData = new GridData();
    snapshotsUpdatePolicyLabelData.horizontalIndent = 15;
    snapshotsUpdatePolicyLabel.setLayoutData(snapshotsUpdatePolicyLabelData);
  
    snapshotsUpdatePolicyCombo = new CCombo(composite, SWT.FLAT);
    snapshotsUpdatePolicyCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    snapshotsUpdatePolicyCombo.setItems(new String[] {"daily", "always", "interval:30", "never"});
    snapshotsUpdatePolicyCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    toolkit.adapt(snapshotsUpdatePolicyCombo, true, true);
    toolkit.paintBordersFor(snapshotsUpdatePolicyCombo);
  
    snapshotsChecksumPolicyLabel = new Label(composite, SWT.NONE);
    snapshotsChecksumPolicyLabel.setText("Checksum Policy:");
    GridData checksumPolicyLabelData = new GridData();
    checksumPolicyLabelData.horizontalIndent = 15;
    snapshotsChecksumPolicyLabel.setLayoutData(checksumPolicyLabelData);
    toolkit.adapt(snapshotsChecksumPolicyLabel, true, true);
  
    snapshotsChecksumPolicyCombo = new CCombo(composite, SWT.READ_ONLY | SWT.FLAT);
    snapshotsChecksumPolicyCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    snapshotsChecksumPolicyCombo.setItems(new String[] {"ignore", "fail", "warn"});
    snapshotsChecksumPolicyCombo.setLayoutData(new GridData());
    toolkit.adapt(snapshotsChecksumPolicyCombo, true, true);
    toolkit.paintBordersFor(snapshotsChecksumPolicyCombo);
    
    updateRepositoryDetailsSection(null);
  }

  private void createRelocationSection(SashForm sashForm) {
    relocationSection = toolkit.createSection(sashForm, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
    relocationSection.setText("Relocation");

    relocationComposite = toolkit.createComposite(relocationSection, SWT.NONE);
    relocationComposite.setLayout(new GridLayout(2, false));
    toolkit.paintBordersFor(relocationComposite);
    relocationSection.setClient(relocationComposite);
    relocationComposite.addControlListener(rightWidthGroup);

    Label relocationGroupIdLabel = toolkit.createLabel(relocationComposite, "Group Id:", SWT.NONE);
    rightWidthGroup.addControl(relocationGroupIdLabel);

    relocationGroupIdText = toolkit.createText(relocationComposite, null, SWT.NONE);
    GridData gd_relocationGroupIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_relocationGroupIdText.widthHint = 100;
    relocationGroupIdText.setLayoutData(gd_relocationGroupIdText);

    Hyperlink relocationArtifactIdHyperlink = toolkit.createHyperlink(relocationComposite, "Artifact Id:", SWT.NONE);
    relocationArtifactIdHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        final String groupId = relocationGroupIdText.getText();
        final String artifactId = relocationArtifactIdText.getText();
        final String version = relocationVersionText.getText();
        new Job("Opening " + groupId + ":" + artifactId + ":" + version) {
          protected IStatus run(IProgressMonitor arg0) {
            OpenPomAction.openEditor(groupId, artifactId, version);
            return Status.OK_STATUS;
          }
        }.schedule();
      }
    });

    rightWidthGroup.addControl(relocationArtifactIdHyperlink);

    relocationArtifactIdText = toolkit.createText(relocationComposite, null, SWT.NONE);
    GridData gd_relocationArtifactIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_relocationArtifactIdText.widthHint = 100;
    relocationArtifactIdText.setLayoutData(gd_relocationArtifactIdText);

    Label relocationVersionLabel = toolkit.createLabel(relocationComposite, "Version:", SWT.NONE);
    rightWidthGroup.addControl(relocationVersionLabel);

    relocationVersionText = toolkit.createText(relocationComposite, null, SWT.NONE);
    GridData gd_relocationVersionText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_relocationVersionText.widthHint = 100;
    relocationVersionText.setLayoutData(gd_relocationVersionText);

    Label relocationMessageLabel = toolkit.createLabel(relocationComposite, "Message:", SWT.NONE);
    rightWidthGroup.addControl(relocationMessageLabel);

    relocationMessageText = toolkit.createText(relocationComposite, null, SWT.NONE);
    GridData gd_relocationMessageText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_relocationMessageText.widthHint = 100;
    relocationMessageText.setLayoutData(gd_relocationMessageText);
  }

  private void createProjectSiteSection(SashForm sashForm) {
    projectSiteSection = toolkit.createSection(sashForm, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
    projectSiteSection.setText("Project Site");

    projectSiteComposite = toolkit.createComposite(projectSiteSection, SWT.NONE);
    projectSiteComposite.setLayout(new GridLayout(2, false));
    toolkit.paintBordersFor(projectSiteComposite);
    projectSiteSection.setClient(projectSiteComposite);
    projectSiteComposite.addControlListener(leftWidthGroup);

    Label siteIdLabel = toolkit.createLabel(projectSiteComposite, "Id:", SWT.NONE);
    leftWidthGroup.addControl(siteIdLabel);

    projectSiteIdText = toolkit.createText(projectSiteComposite, null, SWT.NONE);
    GridData gd_projectSiteIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_projectSiteIdText.widthHint = 100;
    projectSiteIdText.setLayoutData(gd_projectSiteIdText);

    Label siteNameLabel = toolkit.createLabel(projectSiteComposite, "Name:", SWT.NONE);
    leftWidthGroup.addControl(siteNameLabel);

    projectSiteNameText = toolkit.createText(projectSiteComposite, null, SWT.NONE);
    GridData gd_projectSiteNameText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_projectSiteNameText.widthHint = 100;
    projectSiteNameText.setLayoutData(gd_projectSiteNameText);

    Hyperlink projectSiteUrlHyperlink = toolkit.createHyperlink(projectSiteComposite, "URL:", SWT.NONE);
    projectSiteUrlHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(projectSiteUrlText.getText());
      }
    });
    leftWidthGroup.addControl(projectSiteUrlHyperlink);

    projectSiteUrlText = toolkit.createText(projectSiteComposite, null, SWT.NONE);
    GridData gd_projectSiteUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_projectSiteUrlText.widthHint = 100;
    projectSiteUrlText.setLayoutData(gd_projectSiteUrlText);
    sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    toolkit.adapt(sashForm, true, true);

    Hyperlink projectDownloadUrlHyperlink = toolkit.createHyperlink(projectSiteComposite, "Download:", SWT.NONE);
    projectDownloadUrlHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(projectDownloadUrlText.getText());
      }
    });
    leftWidthGroup.addControl(projectDownloadUrlHyperlink);

    projectDownloadUrlText = toolkit.createText(projectSiteComposite, null, SWT.NONE);
    GridData gd_projectDownloadUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_projectDownloadUrlText.widthHint = 100;
    projectDownloadUrlText.setLayoutData(gd_projectDownloadUrlText);
  }

  private void createSnapshotRepositorySection(SashForm distributionManagementSash) {
    snapshotRepositorySection = toolkit.createSection(distributionManagementSash, //
        Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
    snapshotRepositorySection.setText("Snapshots Distribution Repository");

    snapshotRepositoryComposite = toolkit.createComposite(snapshotRepositorySection, SWT.NONE);
    snapshotRepositoryComposite.setLayout(new GridLayout(2, false));
    toolkit.paintBordersFor(snapshotRepositoryComposite);
    snapshotRepositorySection.setClient(snapshotRepositoryComposite);
    snapshotRepositoryComposite.addControlListener(rightWidthGroup);

    Label snapshotRepositoryIdLabel = toolkit.createLabel(snapshotRepositoryComposite, "Id:", SWT.NONE);
    rightWidthGroup.addControl(snapshotRepositoryIdLabel);
    
    snapshotRepositoryIdText = toolkit.createText(snapshotRepositoryComposite, null, SWT.NONE);
    GridData gd_snapshotRepositoryIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_snapshotRepositoryIdText.widthHint = 100;
    snapshotRepositoryIdText.setLayoutData(gd_snapshotRepositoryIdText);

    Label snapshotRepositoryNameLabel = toolkit.createLabel(snapshotRepositoryComposite, "Name:", SWT.NONE);
    rightWidthGroup.addControl(snapshotRepositoryNameLabel);
    
    snapshotRepositoryNameText = toolkit.createText(snapshotRepositoryComposite, null, SWT.NONE);
    GridData gd_snapshotRepositoryNameText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_snapshotRepositoryNameText.widthHint = 100;
    snapshotRepositoryNameText.setLayoutData(gd_snapshotRepositoryNameText);

    Hyperlink snapshotRepositoryUrlHyperlink = toolkit.createHyperlink(snapshotRepositoryComposite, "URL:", SWT.NONE);
    snapshotRepositoryUrlHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(snapshotRepositoryUrlText.getText());
      }
    });
    rightWidthGroup.addControl(snapshotRepositoryUrlHyperlink);
    
    snapshotRepositoryUrlText = toolkit.createText(snapshotRepositoryComposite, null, SWT.NONE);
    GridData gd_snapshotRepositoryUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_snapshotRepositoryUrlText.widthHint = 100;
    snapshotRepositoryUrlText.setLayoutData(gd_snapshotRepositoryUrlText);

    Label snapshotRepositoryLayoutLabel = toolkit.createLabel(snapshotRepositoryComposite, "Layout:", SWT.NONE);
    snapshotRepositoryLayoutLabel.setLayoutData(new GridData());
    rightWidthGroup.addControl(snapshotRepositoryLayoutLabel);

    snapshotRepositoryLayoutCombo = new CCombo(snapshotRepositoryComposite, SWT.FLAT);
    snapshotRepositoryLayoutCombo.setItems(new String[] {"default", "legacy"});
    snapshotRepositoryLayoutCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    snapshotRepositoryLayoutCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    toolkit.adapt(snapshotRepositoryLayoutCombo, true, true);
    new Label(snapshotRepositoryComposite, SWT.NONE);

    snapshotRepositoryUniqueVersionButton = toolkit.createButton(snapshotRepositoryComposite, //
        "Unique Version", SWT.CHECK);
    snapshotRepositoryUniqueVersionButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
  }

  private void createReleaseRepositorySection(SashForm distributionManagementSash) {
    releaseRepositorySection = toolkit.createSection(distributionManagementSash, //
        Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
    releaseRepositorySection.setText("Release Distribution Repository");

    releaseDistributionRepositoryComposite = toolkit.createComposite(releaseRepositorySection, SWT.NONE);
    releaseDistributionRepositoryComposite.setLayout(new GridLayout(2, false));
    toolkit.paintBordersFor(releaseDistributionRepositoryComposite);
    releaseRepositorySection.setClient(releaseDistributionRepositoryComposite);
    releaseDistributionRepositoryComposite.addControlListener(leftWidthGroup);

    Label releaseRepositoryIdLabel = toolkit.createLabel(releaseDistributionRepositoryComposite, "Id:", SWT.NONE);
    leftWidthGroup.addControl(releaseRepositoryIdLabel);

    releaseRepositoryIdText = toolkit.createText(releaseDistributionRepositoryComposite, null, SWT.NONE);
    GridData gd_releaseRepositoryIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_releaseRepositoryIdText.widthHint = 100;
    releaseRepositoryIdText.setLayoutData(gd_releaseRepositoryIdText);

    Label releaseRepositoryNameLabel = toolkit.createLabel(releaseDistributionRepositoryComposite, "Name:", SWT.NONE);
    leftWidthGroup.addControl(releaseRepositoryNameLabel);

    releaseRepositoryNameText = toolkit.createText(releaseDistributionRepositoryComposite, null, SWT.NONE);
    GridData gd_releaseRepositoryNameText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_releaseRepositoryNameText.widthHint = 100;
    releaseRepositoryNameText.setLayoutData(gd_releaseRepositoryNameText);

    Hyperlink releaseRepositoryUrlHyperlink = toolkit.createHyperlink(releaseDistributionRepositoryComposite, "URL:", SWT.NONE);
    releaseRepositoryUrlHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(releaseRepositoryUrlText.getText());
      }
    });
    leftWidthGroup.addControl(releaseRepositoryUrlHyperlink);

    releaseRepositoryUrlText = toolkit.createText(releaseDistributionRepositoryComposite, null, SWT.NONE);
    GridData gd_releaseRepositoryUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_releaseRepositoryUrlText.widthHint = 100;
    releaseRepositoryUrlText.setLayoutData(gd_releaseRepositoryUrlText);

    Label releaseRepositoryLayoutLabel = toolkit.createLabel(releaseDistributionRepositoryComposite, "Layout:", SWT.NONE);
    releaseRepositoryLayoutLabel.setLayoutData(new GridData());
    leftWidthGroup.addControl(releaseRepositoryLayoutLabel);

    releaseRepositoryLayoutCombo = new CCombo(releaseDistributionRepositoryComposite, SWT.FLAT);
    releaseRepositoryLayoutCombo.setItems(new String[] {"default", "legacy"});
    releaseRepositoryLayoutCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    releaseRepositoryLayoutCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    toolkit.adapt(releaseRepositoryLayoutCombo, true, true);
    new Label(releaseDistributionRepositoryComposite, SWT.NONE);

    releaseRepositoryUniqueVersionButton = toolkit.createButton(releaseDistributionRepositoryComposite,
        "Unique Version", SWT.CHECK);
    releaseRepositoryUniqueVersionButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
  }

  public void loadData(MavenPomEditorPage editorPage) {
    parent = editorPage;
    model = editorPage.getModel();
    
    loadRepositories(model.getRepositories());
    loadPluginRepositories(model.getPluginRepositories());
    
    DistributionManagement dm = model.getDistributionManagement();
    loadReleaseDistributionRepository(dm);
    loadSnapshotDistributionRepository(dm);
    loadProjectSite(dm);
    loadRelocation(dm);
    
    registerReleaseRepositoryListeners();
    registerSnapshotRepositoryListeners();
    registerProjectListeners();
    registerRelocationListeners();
    
    repositoriesEditor.setReadOnly(parent.isReadOnly());
    pluginRepositoriesEditor.setReadOnly(parent.isReadOnly());
    
    expandSections(dm);
  }

  private void expandSections(DistributionManagement dm) {
    if(dm!=null) {
      boolean isRepositoriesExpanded = false;
      
      if(dm.getRepository()!=null){
        DeploymentRepository r = dm.getRepository();
        isRepositoriesExpanded |= !isEmpty(r.getId()) || !isEmpty(r.getName()) || !isEmpty(r.getUrl())
            || !isEmpty(r.getLayout()) || !isEmpty(r.getUniqueVersion());
      }
      
      if(dm.getSnapshotRepository()!=null){
        DeploymentRepository r = dm.getSnapshotRepository();
        isRepositoriesExpanded |= !isEmpty(r.getId()) || !isEmpty(r.getName()) || !isEmpty(r.getUrl())
            || !isEmpty(r.getLayout()) || !isEmpty(r.getUniqueVersion());
      }

      releaseRepositorySection.setExpanded(isRepositoriesExpanded);
      snapshotRepositorySection.setExpanded(isRepositoriesExpanded);
      
      boolean isSiteExpanded = false;
      
      Site s = dm.getSite();
      if(s!=null) {
        isSiteExpanded |= !isEmpty(s.getId()) || !isEmpty(s.getName()) || !isEmpty(s.getUrl())
        || !isEmpty(dm.getDownloadUrl());
      } else {
        isSiteExpanded |= !isEmpty(dm.getDownloadUrl());
      }
      
      if(dm.getRelocation()!=null) {
        Relocation r = dm.getRelocation();
        isSiteExpanded |= !isEmpty(r.getGroupId()) || !isEmpty(r.getArtifactId()) || !isEmpty(r.getVersion())
        || !isEmpty(r.getMessage());
      }
      
      projectSiteSection.setExpanded(isSiteExpanded);
      relocationSection.setExpanded(isSiteExpanded);
      
    } else {
      releaseRepositorySection.setExpanded(false);
      snapshotRepositorySection.setExpanded(false);
      projectSiteSection.setExpanded(false);
      relocationSection.setExpanded(false);
    }
    
    relocationSection.addExpansionListener(new ExpansionAdapter() {
      boolean isExpanding = false;
      public void expansionStateChanged(ExpansionEvent e) {
        if(!isExpanding) {
          isExpanding = true;
          projectSiteSection.setExpanded(relocationSection.isExpanded());
          isExpanding = false;
        }
      }
    });
    projectSiteSection.addExpansionListener(new ExpansionAdapter() {
      boolean isExpanding = false;
      public void expansionStateChanged(ExpansionEvent e) {
        if(!isExpanding) {
          isExpanding = true;
          relocationSection.setExpanded(projectSiteSection.isExpanded());
          isExpanding = false;
        }
      }
    });
    
    releaseRepositorySection.addExpansionListener(new ExpansionAdapter() {
      boolean isExpanding = false;
      public void expansionStateChanged(ExpansionEvent e) {
        if(!isExpanding) {
          isExpanding = true;
          snapshotRepositorySection.setExpanded(releaseRepositorySection.isExpanded());
          isExpanding = false;
        }
      }
    });
    snapshotRepositorySection.addExpansionListener(new ExpansionAdapter() {
      boolean isExpanding = false;
      public void expansionStateChanged(ExpansionEvent e) {
        if(!isExpanding) {
          isExpanding = true;
          releaseRepositorySection.setExpanded(snapshotRepositorySection.isExpanded());
          isExpanding = false;
        }
      }
    });
  }

  private void registerReleaseRepositoryListeners() {
    ValueProvider<DeploymentRepository> repositoryProvider = new ValueProvider.ParentValueProvider<DeploymentRepository>(
        releaseRepositoryIdText, releaseRepositoryNameText, releaseRepositoryUrlText, releaseRepositoryLayoutCombo,
        releaseRepositoryUniqueVersionButton) {
      public DeploymentRepository getValue() {
        DistributionManagement dm = model.getDistributionManagement();
        return dm==null ? null : dm.getRepository();
      }
      public void create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        DistributionManagement dm = model.getDistributionManagement();
        if(dm==null) {
          dm = PomFactory.eINSTANCE.createDistributionManagement();
          Command command = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_DistributionManagement(), dm);
          compoundCommand.append(command);
        }
        DeploymentRepository r = dm.getRepository();
        if(r==null) {
          r = PomFactory.eINSTANCE.createDeploymentRepository();
          Command command = SetCommand.create(editingDomain, dm, POM_PACKAGE.getDistributionManagement_Repository(), r);
          compoundCommand.append(command);
        }
      }
    };
    parent.setModifyListener(releaseRepositoryIdText, repositoryProvider, POM_PACKAGE.getDeploymentRepository_Id(), "");
    parent.setModifyListener(releaseRepositoryNameText, repositoryProvider, POM_PACKAGE.getDeploymentRepository_Name(), "");
    parent.setModifyListener(releaseRepositoryUrlText, repositoryProvider, POM_PACKAGE.getDeploymentRepository_Url(), "");
    parent.setModifyListener(releaseRepositoryLayoutCombo, repositoryProvider, POM_PACKAGE.getDeploymentRepository_Layout(), "default");
    parent.setModifyListener(releaseRepositoryUniqueVersionButton, repositoryProvider, POM_PACKAGE.getDeploymentRepository_UniqueVersion(), "true");
  }

  private void registerSnapshotRepositoryListeners() {
    ValueProvider<DeploymentRepository> repositoryProvider = new ValueProvider.ParentValueProvider<DeploymentRepository>(
        snapshotRepositoryIdText, snapshotRepositoryNameText, snapshotRepositoryUrlText, snapshotRepositoryLayoutCombo,
        snapshotRepositoryUniqueVersionButton) {
      public DeploymentRepository getValue() {
        DistributionManagement dm = model.getDistributionManagement();
        return dm==null ? null : dm.getSnapshotRepository();
      }
      public void create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        DistributionManagement dm = model.getDistributionManagement();
        if(dm==null) {
          dm = PomFactory.eINSTANCE.createDistributionManagement();
          Command command = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_DistributionManagement(), dm);
          compoundCommand.append(command);
        }
        DeploymentRepository r = dm.getSnapshotRepository();
        if(r==null) {
          r = PomFactory.eINSTANCE.createDeploymentRepository();
          Command command = SetCommand.create(editingDomain, dm, POM_PACKAGE.getDistributionManagement_SnapshotRepository(), r);
          compoundCommand.append(command);
        }
      }
    };
    parent.setModifyListener(snapshotRepositoryIdText, repositoryProvider, POM_PACKAGE.getDeploymentRepository_Id(), "");
    parent.setModifyListener(snapshotRepositoryNameText, repositoryProvider, POM_PACKAGE.getDeploymentRepository_Name(), "");
    parent.setModifyListener(snapshotRepositoryUrlText, repositoryProvider, POM_PACKAGE.getDeploymentRepository_Url(), "");
    parent.setModifyListener(snapshotRepositoryLayoutCombo, repositoryProvider, POM_PACKAGE.getDeploymentRepository_Layout(), "default");
    parent.setModifyListener(snapshotRepositoryUniqueVersionButton, repositoryProvider, POM_PACKAGE.getDeploymentRepository_UniqueVersion(), "true");
  }

  private void registerProjectListeners() {
    ValueProvider<DistributionManagement> dmProvider = new ValueProvider.ParentValueProvider<DistributionManagement>(
        projectDownloadUrlText) {
      public DistributionManagement getValue() {
        return model.getDistributionManagement();
      }
      public void create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        DistributionManagement dm = model.getDistributionManagement();
        if(dm==null) {
          dm = PomFactory.eINSTANCE.createDistributionManagement();
          Command command = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_DistributionManagement(), dm);
          compoundCommand.append(command);
        }
      }
    };
    parent.setModifyListener(projectDownloadUrlText, dmProvider, POM_PACKAGE.getDistributionManagement_DownloadUrl(), "");
    
    ValueProvider<Site> siteProvider = new ValueProvider.ParentValueProvider<Site>(
        projectSiteIdText, projectSiteNameText, projectSiteUrlText) {
      public Site getValue() {
        DistributionManagement dm = model.getDistributionManagement();
        return dm==null ? null : dm.getSite();
      }
      public void create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        DistributionManagement dm = model.getDistributionManagement();
        if(dm==null) {
          dm = PomFactory.eINSTANCE.createDistributionManagement();
          Command command = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_DistributionManagement(), dm);
          compoundCommand.append(command);
        }
        Site s = dm.getSite();
        if(s==null) {
          s = PomFactory.eINSTANCE.createSite();
          Command command = SetCommand.create(editingDomain, dm, POM_PACKAGE.getDistributionManagement_Site(), s);
          compoundCommand.append(command);
        }
      }
    };
    parent.setModifyListener(projectSiteIdText, siteProvider, POM_PACKAGE.getSite_Id(), "");
    parent.setModifyListener(projectSiteNameText, siteProvider, POM_PACKAGE.getSite_Name(), "");
    parent.setModifyListener(projectSiteUrlText, siteProvider, POM_PACKAGE.getSite_Url(), "");
  }

  private void registerRelocationListeners() {
    ValueProvider<Relocation> relocationProvider = new ValueProvider.ParentValueProvider<Relocation>(
        relocationGroupIdText, relocationArtifactIdText, relocationVersionText, relocationMessageText) {
      public Relocation getValue() {
        DistributionManagement dm = model.getDistributionManagement();
        return dm==null ? null : dm.getRelocation();
      }
      public void create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        DistributionManagement dm = model.getDistributionManagement();
        if(dm==null) {
          dm = PomFactory.eINSTANCE.createDistributionManagement();
          Command command = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_DistributionManagement(), dm);
          compoundCommand.append(command);
        }
        Relocation r = dm.getRelocation();
        if(r==null) {
          r = PomFactory.eINSTANCE.createRelocation();
          Command command = SetCommand.create(editingDomain, dm, POM_PACKAGE.getDistributionManagement_Relocation(), r);
          compoundCommand.append(command);
        }
        super.create(editingDomain, compoundCommand);
      }
    };
    parent.setModifyListener(relocationGroupIdText, relocationProvider, POM_PACKAGE.getRelocation_GroupId(), "");
    parent.setModifyListener(relocationArtifactIdText, relocationProvider, POM_PACKAGE.getRelocation_ArtifactId(), "");
    parent.setModifyListener(relocationVersionText, relocationProvider, POM_PACKAGE.getRelocation_Version(), "");
    parent.setModifyListener(relocationMessageText, relocationProvider, POM_PACKAGE.getRelocation_Message(), "");
  }

  private void loadReleaseDistributionRepository(DistributionManagement distributionManagement) {
    DeploymentRepository repository = distributionManagement==null ? null : distributionManagement.getRepository();
    if(repository!=null) {
      setText(releaseRepositoryIdText, repository.getId());
      setText(releaseRepositoryNameText, repository.getName());
      setText(releaseRepositoryUrlText, repository.getUrl());
      setText(releaseRepositoryLayoutCombo, repository.getLayout());
      setButton(releaseRepositoryUniqueVersionButton, "true".equals(repository.getUniqueVersion()));
    } else {
      setText(releaseRepositoryIdText, "");
      setText(releaseRepositoryNameText, "");
      setText(releaseRepositoryUrlText, "");
      setText(releaseRepositoryLayoutCombo, "");
      setButton(releaseRepositoryUniqueVersionButton, true); // default
    }
  }

  private void loadSnapshotDistributionRepository(DistributionManagement distributionManagement) {
    DeploymentRepository repository = distributionManagement==null ? null : distributionManagement.getSnapshotRepository();
    if(repository!=null) {
      setText(snapshotRepositoryIdText, repository.getId());
      setText(snapshotRepositoryNameText, repository.getName());
      setText(snapshotRepositoryUrlText, repository.getUrl());
      setText(snapshotRepositoryLayoutCombo, repository.getLayout());
      setButton(snapshotRepositoryUniqueVersionButton, "true".equals(repository.getUniqueVersion()));
    } else {
      setText(snapshotRepositoryIdText, "");
      setText(snapshotRepositoryNameText, "");
      setText(snapshotRepositoryUrlText, "");
      setText(snapshotRepositoryLayoutCombo, "");
      setButton(snapshotRepositoryUniqueVersionButton, true); // default
    }
  }

  private void loadProjectSite(DistributionManagement distributionManagement) {
    Site site = distributionManagement==null ? null : distributionManagement.getSite();
    if(site!=null) {
      setText(projectSiteIdText, site.getId());
      setText(projectSiteNameText, site.getName());
      setText(projectSiteUrlText, site.getUrl());
    } else {
      setText(projectSiteIdText, "");
      setText(projectSiteNameText, "");
      setText(projectSiteUrlText, "");
    }
    
    setText(projectDownloadUrlText, distributionManagement==null ? null : distributionManagement.getDownloadUrl());
  }

  private void loadRelocation(DistributionManagement distributionManagement) {
    Relocation relocation = distributionManagement==null ? null : distributionManagement.getRelocation();
    if(relocation!=null) {
      setText(relocationGroupIdText, relocation.getGroupId());
      setText(relocationArtifactIdText, relocation.getArtifactId());
      setText(relocationVersionText, relocation.getVersion());
      setText(relocationMessageText, relocation.getMessage());
    } else {
      setText(relocationGroupIdText, "");
      setText(relocationArtifactIdText, "");
      setText(relocationVersionText, "");
      setText(relocationMessageText, "");
    }
  }

  private void loadRepositories(Repositories repositories) {
    repositoriesEditor.setInput(repositories==null ? null : repositories.getRepository());
    repositoriesEditor.setReadOnly(parent.isReadOnly());
    changingSelection = true;
    updateRepositoryDetailsSection(null);
    changingSelection = false;
  }
  
  private void loadPluginRepositories(PluginRepositories pluginRepositories) {
    pluginRepositoriesEditor.setInput(pluginRepositories==null ? null : pluginRepositories.getPluginRepository());
    pluginRepositoriesEditor.setReadOnly(parent.isReadOnly());
    changingSelection = true;
    updateRepositoryDetailsSection(null);
    changingSelection = false;
  }

  public void updateView(MavenPomEditorPage editorPage, Notification notification) {
    EObject object = (EObject) notification.getNotifier();
    if(object instanceof Repositories) {
      repositoriesEditor.refresh();
    }
  
    if(object instanceof PluginRepositories) {
      pluginRepositoriesEditor.refresh();
    }
    
    if(object instanceof Repository) {
      repositoriesEditor.refresh();
      pluginRepositoriesEditor.refresh();
      if(currentRepository==object) {
        updateRepositoryDetailsSection((Repository) object);
      }
    }
    
    if(object instanceof DistributionManagement) {
      loadProjectSite((DistributionManagement) object);
      loadRelocation((DistributionManagement) object);
      loadReleaseDistributionRepository((DistributionManagement) object);
      loadSnapshotDistributionRepository((DistributionManagement) object);
    }
    
    if(object instanceof Site) {
      loadProjectSite(model.getDistributionManagement());
    }

    if(object instanceof Relocation) {
      loadRelocation(model.getDistributionManagement());
    }    
    
    if(object instanceof DeploymentRepository) {
      loadReleaseDistributionRepository(model.getDistributionManagement());
      loadSnapshotDistributionRepository(model.getDistributionManagement());
    }    
    
    // XXX
  }

  protected void updateRepositoryDetailsSection(final Repository repository) {
    if(changingSelection) {
      return;
    }
//    if(repository != null && currentRepository == repository) {
//      return;
//    }
    currentRepository = repository;
    
    if(parent != null) {
      parent.removeNotifyListener(repositoryIdText);
      parent.removeNotifyListener(repositoryNameText);
      parent.removeNotifyListener(repositoryUrlText);
      parent.removeNotifyListener(repositoryLayoutCombo);
      
      parent.removeNotifyListener(releasesEnabledButton);
      parent.removeNotifyListener(releasesChecksumPolicyCombo);
      parent.removeNotifyListener(releasesUpdatePolicyCombo);
      
      parent.removeNotifyListener(snapshotsEnabledButton);
      parent.removeNotifyListener(snapshotsChecksumPolicyCombo);
      parent.removeNotifyListener(snapshotsUpdatePolicyCombo);
    }
    
    if(repository==null) {
      FormUtils.setEnabled(repositoryDetailsSection, false);
  
      setText(repositoryIdText, "");
      setText(repositoryNameText, "");
      setText(repositoryLayoutCombo, "");
      setText(repositoryUrlText, "");
      
      setButton(releasesEnabledButton, false);
      setText(releasesChecksumPolicyCombo, "");
      setText(releasesUpdatePolicyCombo, "");
      
      setButton(snapshotsEnabledButton, false); 
      setText(snapshotsChecksumPolicyCombo, "");  // move into listener
      setText(snapshotsUpdatePolicyCombo, "");
      
      // XXX swap repository details panel
      
      return;
    }
  
//    repositoryIdText.setEnabled(true);
//    repositoryNameText.setEnabled(true);
//    repositoryLayoutCombo.setEnabled(true);
//    repositoryUrlText.setEnabled(true);
//    releasesEnabledButton.setEnabled(true);
//    snapshotsEnabledButton.setEnabled(true); 

    setText(repositoryIdText, repository.getId());
    setText(repositoryNameText, repository.getName());
    setText(repositoryLayoutCombo, repository.getLayout());
    setText(repositoryUrlText, repository.getUrl());
    
    {
      RepositoryPolicy releases = repository.getReleases();
      if(releases!=null) {
        setButton(releasesEnabledButton, "true".equals(releases.getEnabled()));
        setText(releasesChecksumPolicyCombo, releases.getChecksumPolicy());
        setText(releasesUpdatePolicyCombo, releases.getUpdatePolicy());
      } else {
        setButton(releasesEnabledButton, false);
      }
      boolean isReleasesEnabled = releasesEnabledButton.getSelection();
      releasesChecksumPolicyCombo.setEnabled(isReleasesEnabled);
      releasesUpdatePolicyCombo.setEnabled(isReleasesEnabled);
      releasesChecksumPolicyLabel.setEnabled(isReleasesEnabled);
      releasesUpdatePolicyLabel.setEnabled(isReleasesEnabled);
    }
    
    {
      RepositoryPolicy snapshots = repository.getSnapshots();
      if(snapshots!=null) {
        setButton(snapshotsEnabledButton, "true".equals(snapshots.getEnabled()));
        setText(snapshotsChecksumPolicyCombo, snapshots.getChecksumPolicy());
        setText(snapshotsUpdatePolicyCombo, snapshots.getUpdatePolicy());
      } else {
        snapshotsEnabledButton.setSelection(false);
      }
      boolean isSnapshotsEnabled = snapshotsEnabledButton.getSelection();
      snapshotsChecksumPolicyCombo.setEnabled(isSnapshotsEnabled);
      snapshotsUpdatePolicyCombo.setEnabled(isSnapshotsEnabled);
      snapshotsChecksumPolicyLabel.setEnabled(isSnapshotsEnabled);
      snapshotsUpdatePolicyLabel.setEnabled(isSnapshotsEnabled);
    }

    FormUtils.setEnabled(repositoryDetailsSection, true);
    FormUtils.setReadonly(repositoryDetailsSection, parent.isReadOnly());
    
    ValueProvider<Repository> repositoryProvider = new ValueProvider.DefaultValueProvider<Repository>(repository); 
    parent.setModifyListener(repositoryIdText, repositoryProvider, POM_PACKAGE.getRepository_Id(), "");
    parent.setModifyListener(repositoryNameText, repositoryProvider, POM_PACKAGE.getRepository_Name(), "");
    parent.setModifyListener(repositoryUrlText, repositoryProvider, POM_PACKAGE.getRepository_Url(), "");
    parent.setModifyListener(repositoryLayoutCombo, repositoryProvider, POM_PACKAGE.getRepository_Layout(), "default");

    ValueProvider<RepositoryPolicy> releasesProvider = new ValueProvider.ParentValueProvider<RepositoryPolicy>(
        releasesEnabledButton, releasesChecksumPolicyCombo, releasesUpdatePolicyCombo) {
      public RepositoryPolicy getValue() {
        return repository.getReleases();
      }
      public void create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        RepositoryPolicy policy = getValue();
        if(policy == null) {
          policy = PomFactory.eINSTANCE.createRepositoryPolicy();
          Command command = SetCommand.create(editingDomain, repository, POM_PACKAGE.getRepository_Releases(), policy);
          compoundCommand.append(command);
        }
      }
    };
    parent.setModifyListener(releasesEnabledButton, releasesProvider, POM_PACKAGE.getRepositoryPolicy_Enabled(), "true");
    parent.setModifyListener(releasesChecksumPolicyCombo, releasesProvider, POM_PACKAGE.getRepositoryPolicy_ChecksumPolicy(), "");
    parent.setModifyListener(releasesUpdatePolicyCombo, releasesProvider, POM_PACKAGE.getRepositoryPolicy_UpdatePolicy(), "");
    
    ValueProvider<RepositoryPolicy> snapshotsProvider = new ValueProvider.ParentValueProvider<RepositoryPolicy>(
        snapshotsEnabledButton, snapshotsChecksumPolicyCombo, snapshotsUpdatePolicyCombo) {
      public RepositoryPolicy getValue() {
        return repository.getSnapshots();
      }
      public void create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        RepositoryPolicy policy = getValue();
        if(policy == null) {
          policy = PomFactory.eINSTANCE.createRepositoryPolicy();
          Command command = SetCommand.create(editingDomain, repository, POM_PACKAGE.getRepository_Snapshots(), policy);
          compoundCommand.append(command);
        }
      }
    };
    parent.setModifyListener(snapshotsEnabledButton, snapshotsProvider, POM_PACKAGE.getRepositoryPolicy_Enabled(), "true");
    parent.setModifyListener(snapshotsChecksumPolicyCombo, snapshotsProvider, POM_PACKAGE.getRepositoryPolicy_ChecksumPolicy(), "");
    parent.setModifyListener(snapshotsUpdatePolicyCombo, snapshotsProvider, POM_PACKAGE.getRepositoryPolicy_UpdatePolicy(), "");
  }

  /**
   * Repository label provider
   */
  public class RepositoryLabelProvider extends LabelProvider {

    public String getText(Object element) {
      if(element instanceof Repository) {
        Repository r = (Repository) element;
        return (isEmpty(r.getId()) ? "?" : r.getId()) + " : " + (isEmpty(r.getUrl()) ? "?" : r.getUrl());
      }
      return super.getText(element);
    }
    
    public Image getImage(Object element) {
      return MavenEditorImages.IMG_REPOSITORY;
    }
    
  }
  
}
