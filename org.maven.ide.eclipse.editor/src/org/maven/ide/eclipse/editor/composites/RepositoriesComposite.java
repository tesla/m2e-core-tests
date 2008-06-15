/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.composites;

import static org.maven.ide.eclipse.editor.pom.FormUtils.isEmpty;
import static org.maven.ide.eclipse.editor.pom.FormUtils.nvl;
import static org.maven.ide.eclipse.editor.pom.FormUtils.setText;

import java.util.List;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
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
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.editor.pom.FormUtils;
import org.maven.ide.eclipse.editor.pom.MavenPomEditorPage;
import org.maven.ide.eclipse.editor.pom.ValueProvider;

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
  
  Text idText;
  Text nameText;
  Text repositoryUrlText;
  CCombo layoutCombo;

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
  
  // model
  
  private Model model;
  
//  private Repositories repositories;
//  private PluginRepositories pluginRepositories;
  
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
    createRepositoryManagementSection(verticalSash);

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

  private void createRepositoriesSection(SashForm verticalSash) {
  Section repositoriesSection = toolkit.createSection(verticalSash, Section.TITLE_BAR | Section.COMPACT);
  repositoriesSection.setText("Repositories");

  repositoriesEditor = new ListEditorComposite<Repository>(repositoriesSection, SWT.NONE);
  
  repositoriesEditor.setLabelProvider(new RepositoryLabelProvider());
  repositoriesEditor.setContentProvider(new ListEditorContentProvider<Repository>());
  
  repositoriesEditor.setAddListener(new SelectionAdapter() {
    public void widgetSelected(SelectionEvent e) {
      Repositories repositories = model.getRepositories();
      if(repositories==null) {
        repositories = PomFactory.eINSTANCE.createRepositories();
        model.setRepositories(repositories);
      }
      repositories.getRepository().add(PomFactory.eINSTANCE.createRepository());
      // XXX update details panel
    }
  });
  
  repositoriesEditor.setRemoveListener(new SelectionAdapter() {
    public void widgetSelected(SelectionEvent e) {
      // TODO Auto-generated method stub
    }
  });
  
  repositoriesEditor.addSelectionListener(new ISelectionChangedListener() {
    public void selectionChanged(SelectionChangedEvent event) {
      List<Repository> selection = repositoriesEditor.getSelection();
      updateRepositoryDetailsSection(selection.size()==1 ? selection.get(0) : null);
    }
  });
  
  toolkit.paintBordersFor(repositoriesEditor);
  toolkit.adapt(repositoriesEditor);
  repositoriesSection.setClient(repositoriesEditor);
}

  private void createRepositoryManagementSection(SashForm verticalSash) {
    Section pluginRepositoriesSection = toolkit.createSection(verticalSash, Section.TITLE_BAR | Section.COMPACT);
    pluginRepositoriesSection.setText("Plugin Repositories");
  
    pluginRepositoriesEditor = new ListEditorComposite<Repository>(pluginRepositoriesSection, SWT.NONE);
  
    pluginRepositoriesEditor.setLabelProvider(new RepositoryLabelProvider());
    pluginRepositoriesEditor.setContentProvider(new ListEditorContentProvider<Repository>());
    
    pluginRepositoriesEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        PluginRepositories pluginRepositories = model.getPluginRepositories();
        if(pluginRepositories==null) {
          pluginRepositories = PomFactory.eINSTANCE.createPluginRepositories();
          model.setPluginRepositories(pluginRepositories);
        }
        pluginRepositories.getPluginRepository().add(PomFactory.eINSTANCE.createRepository());
        // XXX update details panel
      }
    });
    
    pluginRepositoriesEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        // TODO Auto-generated method stub
      }
    });
    
    pluginRepositoriesEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Repository> selection = pluginRepositoriesEditor.getSelection();
        updateRepositoryDetailsSection(selection.size()==1 ? selection.get(0) : null);
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
    idLabel.setText("Id:");
  
    idText = toolkit.createText(repositoryDetailsComposite, "");
    idText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    Label nameLabel = new Label(repositoryDetailsComposite, SWT.NONE);
    nameLabel.setText("Name:");
  
    nameText = toolkit.createText(repositoryDetailsComposite, "");
    nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    Hyperlink repositoryUrlHyperlink = toolkit.createHyperlink(repositoryDetailsComposite, "URL:", SWT.NONE);
    repositoryUrlHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(repositoryUrlText.getText());
      }
    });
  
    repositoryUrlText = toolkit.createText(repositoryDetailsComposite, "");
    repositoryUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    Label layoutLabel = new Label(repositoryDetailsComposite, SWT.NONE);
    layoutLabel.setText("Layout:");
  
    layoutCombo = new CCombo(repositoryDetailsComposite, SWT.FLAT);
    layoutCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    layoutCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    layoutCombo.setItems(new String[] {"default", "legacy"});
  
    Composite composite = new Composite(repositoryDetailsComposite, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
    toolkit.adapt(composite, true, true);
    toolkit.paintBordersFor(composite);
    GridLayout gridLayout_22 = new GridLayout();
    gridLayout_22.marginBottom = 2;
    gridLayout_22.marginWidth = 2;
    gridLayout_22.marginHeight = 0;
    gridLayout_22.numColumns = 2;
    composite.setLayout(gridLayout_22);
  
    releasesEnabledButton = toolkit.createButton(composite, "Enable Releases", SWT.CHECK | SWT.FLAT);
    GridData gd_releasesEnabledButton = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    gd_releasesEnabledButton.verticalIndent = 5;
    releasesEnabledButton.setLayoutData(gd_releasesEnabledButton);
  
    releasesUpdatePolicyLabel = new Label(composite, SWT.NONE);
    releasesUpdatePolicyLabel.setText("Update Policy:");
    GridData gd_releasesUpdatePolicyLabel = new GridData();
    gd_releasesUpdatePolicyLabel.horizontalIndent = 15;
    releasesUpdatePolicyLabel.setLayoutData(gd_releasesUpdatePolicyLabel);
  
    releasesUpdatePolicyCombo = new CCombo(composite, SWT.FLAT);
    releasesUpdatePolicyCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    releasesUpdatePolicyCombo.setItems(new String[] {"daily", "always", "interval:30", "never"});
    releasesUpdatePolicyCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    toolkit.adapt(releasesUpdatePolicyCombo, true, true);
  
    releasesChecksumPolicyLabel = new Label(composite, SWT.NONE);
    releasesChecksumPolicyLabel.setText("Checksum Policy:");
    GridData gd_releasesChecksumPolicyLabel = new GridData();
    gd_releasesChecksumPolicyLabel.horizontalIndent = 15;
    releasesChecksumPolicyLabel.setLayoutData(gd_releasesChecksumPolicyLabel);
  
    releasesChecksumPolicyCombo = new CCombo(composite, SWT.READ_ONLY | SWT.FLAT);
    toolkit.adapt(releasesChecksumPolicyCombo, true, true);
    releasesChecksumPolicyCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    releasesChecksumPolicyCombo.setItems(new String[] {"ignore", "fail", "warn"});
    releasesChecksumPolicyCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
  
    snapshotsEnabledButton = toolkit.createButton(composite, "Enable Snapshots", SWT.CHECK | SWT.FLAT);
    GridData gd_snapshotsEnabledButton = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    gd_snapshotsEnabledButton.verticalIndent = 5;
    snapshotsEnabledButton.setLayoutData(gd_snapshotsEnabledButton);
  
    snapshotsUpdatePolicyLabel = new Label(composite, SWT.NONE);
    snapshotsUpdatePolicyLabel.setText("Update Policy:");
    GridData gd_snapshotsUpdatePolicyLabel = new GridData();
    gd_snapshotsUpdatePolicyLabel.horizontalIndent = 15;
    snapshotsUpdatePolicyLabel.setLayoutData(gd_snapshotsUpdatePolicyLabel);
  
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

    Composite relocationComposite = toolkit.createComposite(relocationSection, SWT.NONE);
    relocationComposite.setLayout(new GridLayout(2, false));
    toolkit.paintBordersFor(relocationComposite);
    relocationSection.setClient(relocationComposite);

    toolkit.createLabel(relocationComposite, "Group Id:", SWT.NONE);

    relocationGroupIdText = toolkit.createText(relocationComposite, null, SWT.NONE);
    relocationGroupIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    toolkit.createLabel(relocationComposite, "Artifact Id:", SWT.NONE);

    relocationArtifactIdText = toolkit.createText(relocationComposite, null, SWT.NONE);
    relocationArtifactIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    toolkit.createLabel(relocationComposite, "Version:", SWT.NONE);

    relocationVersionText = toolkit.createText(relocationComposite, null, SWT.NONE);
    relocationVersionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    toolkit.createLabel(relocationComposite, "Message:", SWT.NONE);

    relocationMessageText = toolkit.createText(relocationComposite, null, SWT.NONE);
    relocationMessageText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  }

  private void createProjectSiteSection(SashForm sashForm) {
    projectSiteSection = toolkit.createSection(sashForm, //
        Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
    projectSiteSection.setText("Project Site");

    Composite projectSiteComposite = toolkit.createComposite(projectSiteSection, SWT.NONE);
    projectSiteComposite.setLayout(new GridLayout(2, false));
    toolkit.paintBordersFor(projectSiteComposite);
    projectSiteSection.setClient(projectSiteComposite);

    toolkit.createLabel(projectSiteComposite, "Id:", SWT.NONE);

    projectSiteIdText = toolkit.createText(projectSiteComposite, null, SWT.NONE);
    projectSiteIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    toolkit.createLabel(projectSiteComposite, "Name:", SWT.NONE);

    projectSiteNameText = toolkit.createText(projectSiteComposite, null, SWT.NONE);
    projectSiteNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Hyperlink projectSiteUrlHyperlink = toolkit.createHyperlink(projectSiteComposite, "URL:", SWT.NONE);
    projectSiteUrlHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(projectSiteUrlText.getText());
      }
    });

    projectSiteUrlText = toolkit.createText(projectSiteComposite, null, SWT.NONE);
    projectSiteUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    toolkit.adapt(sashForm, true, true);

    Hyperlink projectDownloadUrlHyperlink = toolkit.createHyperlink(projectSiteComposite, "Download:", SWT.NONE);
    projectDownloadUrlHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(projectDownloadUrlText.getText());
      }
    });

    projectDownloadUrlText = toolkit.createText(projectSiteComposite, null, SWT.NONE);
    projectDownloadUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  }

  private void createSnapshotRepositorySection(SashForm distributionManagementSash) {
    snapshotRepositorySection = toolkit.createSection(distributionManagementSash, //
        Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
    snapshotRepositorySection.setText("Snapshots Distribution Repository");

    Composite snapshotRepositoryComposite = toolkit.createComposite(snapshotRepositorySection, SWT.NONE);
    snapshotRepositoryComposite.setLayout(new GridLayout(2, false));
    toolkit.paintBordersFor(snapshotRepositoryComposite);
    snapshotRepositorySection.setClient(snapshotRepositoryComposite);

    toolkit.createLabel(snapshotRepositoryComposite, "Id:", SWT.NONE);

    snapshotRepositoryIdText = toolkit.createText(snapshotRepositoryComposite, null, SWT.NONE);
    snapshotRepositoryIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    toolkit.createLabel(snapshotRepositoryComposite, "Name:", SWT.NONE);

    snapshotRepositoryNameText = toolkit.createText(snapshotRepositoryComposite, null, SWT.NONE);
    snapshotRepositoryNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Hyperlink snapshotRepositoryUrlHyperlink = toolkit.createHyperlink(snapshotRepositoryComposite, "URL:", SWT.NONE);
    snapshotRepositoryUrlHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(snapshotRepositoryUrlText.getText());
      }
    });
    
    snapshotRepositoryUrlText = toolkit.createText(snapshotRepositoryComposite, null, SWT.NONE);
    snapshotRepositoryUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label layoutLabel = toolkit.createLabel(snapshotRepositoryComposite, "Layout:", SWT.NONE);
    layoutLabel.setLayoutData(new GridData());

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

    Composite releaseDistributionRepositoryComposite = toolkit.createComposite(releaseRepositorySection, SWT.NONE);
    releaseDistributionRepositoryComposite.setLayout(new GridLayout(2, false));
    toolkit.paintBordersFor(releaseDistributionRepositoryComposite);
    releaseRepositorySection.setClient(releaseDistributionRepositoryComposite);

    toolkit.createLabel(releaseDistributionRepositoryComposite, "Id:", SWT.NONE);

    releaseRepositoryIdText = toolkit.createText(releaseDistributionRepositoryComposite, null, SWT.NONE);
    releaseRepositoryIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    toolkit.createLabel(releaseDistributionRepositoryComposite, "Name:", SWT.NONE);

    releaseRepositoryNameText = toolkit.createText(releaseDistributionRepositoryComposite, null, SWT.NONE);
    releaseRepositoryNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Hyperlink repositoryUrlHyperlink = toolkit
        .createHyperlink(releaseDistributionRepositoryComposite, "URL:", SWT.NONE);
    repositoryUrlHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(releaseRepositoryUrlText.getText());
      }
    });

    releaseRepositoryUrlText = toolkit.createText(releaseDistributionRepositoryComposite, null, SWT.NONE);
    releaseRepositoryUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label label = toolkit.createLabel(releaseDistributionRepositoryComposite, "Layout:", SWT.NONE);
    label.setLayoutData(new GridData());

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
    
    if(dm!=null && dm.getRepository()!=null) {
      DeploymentRepository r = dm.getRepository();
      releaseRepositorySection.setExpanded(!isEmpty(r.getId()) || !isEmpty(r.getName()) || !isEmpty(r.getUrl())
          || !isEmpty(r.getLayout()) || !isEmpty(r.getUniqueVersion()));
      
    } else {
      releaseRepositorySection.setExpanded(false);
    }
    
    if(dm!=null && dm.getSnapshotRepository()!=null) {
      DeploymentRepository r = dm.getSnapshotRepository();
      snapshotRepositorySection.setExpanded(!isEmpty(r.getId()) || !isEmpty(r.getName()) || !isEmpty(r.getUrl())
          || !isEmpty(r.getLayout()) || !isEmpty(r.getUniqueVersion()));
    } else {
      snapshotRepositorySection.setExpanded(false);
    }
    
    if(dm!=null) {
      Site s = dm.getSite();
      if(s!=null) {
        projectSiteSection.setExpanded(!isEmpty(s.getId()) || !isEmpty(s.getName()) || !isEmpty(s.getUrl())
            || !isEmpty(dm.getDownloadUrl()));
      } else {
        projectSiteSection.setExpanded(!isEmpty(dm.getDownloadUrl()));
      }
    } else {
      projectSiteSection.setExpanded(false);
    }

    if(dm!=null && dm.getRelocation()!=null) {
      Relocation r = dm.getRelocation();
      releaseRepositorySection.setExpanded(!isEmpty(r.getGroupId()) || !isEmpty(r.getArtifactId())
          || !isEmpty(r.getVersion()) || !isEmpty(r.getMessage()));
    } else {
      releaseRepositorySection.setExpanded(false);
    }
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
        DeploymentRepository r = dm.getRepository();
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
    DeploymentRepository repository = distributionManagement.getRepository();
    if(repository!=null) {
      setText(releaseRepositoryIdText, repository.getId());
      setText(releaseRepositoryNameText, repository.getName());
      setText(releaseRepositoryUrlText, repository.getUrl());
      setText(releaseRepositoryLayoutCombo, repository.getLayout());
      releaseRepositoryUniqueVersionButton.setSelection("true".equals(repository.getUniqueVersion()));
    } else {
      setText(releaseRepositoryIdText, "");
      setText(releaseRepositoryNameText, "");
      setText(releaseRepositoryUrlText, "");
      setText(releaseRepositoryLayoutCombo, "");
      releaseRepositoryUniqueVersionButton.setSelection(true); // default
    }
  }

  private void loadSnapshotDistributionRepository(DistributionManagement distributionManagement) {
    DeploymentRepository repository = distributionManagement.getSnapshotRepository();
    if(repository!=null) {
      setText(snapshotRepositoryIdText, repository.getId());
      setText(snapshotRepositoryNameText, repository.getName());
      setText(snapshotRepositoryUrlText, repository.getUrl());
      setText(snapshotRepositoryLayoutCombo, repository.getLayout());
      snapshotRepositoryUniqueVersionButton.setSelection("true".equals(repository.getUniqueVersion()));
    } else {
      setText(snapshotRepositoryIdText, "");
      setText(snapshotRepositoryNameText, "");
      setText(snapshotRepositoryUrlText, "");
      setText(snapshotRepositoryLayoutCombo, "");
      snapshotRepositoryUniqueVersionButton.setSelection(true); // default
    }
  }

  private void loadProjectSite(DistributionManagement distributionManagement) {
    Site site = distributionManagement.getSite();
    if(site!=null) {
      setText(projectSiteIdText, site.getId());
      setText(projectSiteNameText, site.getName());
      setText(projectSiteUrlText, site.getUrl());
    } else {
      setText(projectSiteIdText, "");
      setText(projectSiteNameText, "");
      setText(projectSiteUrlText, "");
    }
    
    setText(projectDownloadUrlText, distributionManagement.getDownloadUrl());
  }

  private void loadRelocation(DistributionManagement distributionManagement) {
    Relocation relocation = distributionManagement.getRelocation();
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
  }
  
  private void loadPluginRepositories(PluginRepositories pluginRepositories) {
    pluginRepositoriesEditor.setInput(pluginRepositories==null ? null : pluginRepositories.getPluginRepository());
  }

  public void updateView(MavenPomEditorPage editorPage, Notification notification) {
    EObject object = (EObject) notification.getNotifier();
    if(object instanceof Repositories || object instanceof PluginRepositories || object instanceof Repository) {
      loadRepositories(model.getRepositories());
      loadPluginRepositories(model.getPluginRepositories());
    }
    
    if(object instanceof Repository) {
      updateRepositoryDetailsSection((Repository) object);
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

  protected void updateRepositoryDetailsSection(Repository repository) {
    // XXX unregister listeners
    
    if(repository==null) {
      FormUtils.setEnabled(repositoryDetailsSection, false);
  
      idText.setText("");
      nameText.setText("");
      layoutCombo.setText("");
      repositoryUrlText.setText("");
      
      releasesEnabledButton.setSelection(false);
      releasesChecksumPolicyCombo.setText("");
      releasesUpdatePolicyCombo.setText("");
      
      snapshotsEnabledButton.setSelection(false); 
      snapshotsChecksumPolicyCombo.setText("");  // move into listener
      snapshotsUpdatePolicyCombo.setText("");
      
      // XXX swap repository details panel
      
      return;
    }
  
    FormUtils.setEnabled(repositoryDetailsSection, true);
  
    idText.setEnabled(true);
    nameText.setEnabled(true);
    layoutCombo.setEnabled(true);
    repositoryUrlText.setEnabled(true);
    releasesEnabledButton.setEnabled(true);
    snapshotsEnabledButton.setEnabled(true); 
    
    idText.setText(nvl(repository.getId()));
    nameText.setText(nvl(repository.getName()));
    layoutCombo.setText(nvl(repository.getLayout()));
    repositoryUrlText.setText(nvl(repository.getUrl()));
    
    {
      RepositoryPolicy releases = repository.getReleases();
      if(releases!=null) {
        releasesEnabledButton.setSelection("true".equals(releases.getEnabled()));
        releasesChecksumPolicyCombo.setText(nvl(releases.getChecksumPolicy()));
        releasesUpdatePolicyCombo.setText(nvl(releases.getUpdatePolicy()));
      } else {
        releasesEnabledButton.setSelection(false);
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
        snapshotsEnabledButton.setSelection("true".equals(snapshots.getEnabled()));
        snapshotsChecksumPolicyCombo.setText(nvl(snapshots.getChecksumPolicy()));
        snapshotsUpdatePolicyCombo.setText(nvl(snapshots.getUpdatePolicy()));
      } else {
        snapshotsEnabledButton.setSelection(false);
      }
      boolean isSnapshotsEnabled = snapshotsEnabledButton.getSelection();
      snapshotsChecksumPolicyCombo.setEnabled(isSnapshotsEnabled);
      snapshotsUpdatePolicyCombo.setEnabled(isSnapshotsEnabled);
      snapshotsChecksumPolicyLabel.setEnabled(isSnapshotsEnabled);
      snapshotsUpdatePolicyLabel.setEnabled(isSnapshotsEnabled);
    }
  
    // XXX register new listeners
  }

  /**
   * Repository label provider
   */
  public class RepositoryLabelProvider extends LabelProvider {

    public String getText(Object element) {
      if(element instanceof Repository) {
        Repository r = (Repository) element;
        return r.getId() + " : " + r.getUrl();
      }
      return super.getText(element);
    }
    
    public Image getImage(Object element) {
      return MavenEditorImages.IMG_REPOSITORY;
    }
    
  }
  
}
