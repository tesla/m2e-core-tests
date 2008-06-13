/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.composites;

import static org.maven.ide.eclipse.editor.pom.FormUtils.nvl;

import java.util.List;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
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
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.PluginRepositories;
import org.maven.ide.components.pom.PomFactory;
import org.maven.ide.components.pom.Repositories;
import org.maven.ide.components.pom.Repository;
import org.maven.ide.components.pom.RepositoryPolicy;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.editor.pom.MavenPomEditorPage;
import org.maven.ide.eclipse.editor.pom.FormUtils;

/**
 * @author Eugene Kuleshov
 */
public class RepositoriesComposite extends Composite {

  private Text relocationMessageText;
  private Text relocationVersionText;
  private Text relocationArtifactIdText;
  private Text relocationGroupIdText;
  private Text projectSiteUrlText;
  private Text projectSiteNameText;
  private Text projectSiteIdText;
  private CCombo layoutCombo_2;
  private Text snapshotRepositoryUrlText;
  private Text snapshotRepositoryNameText;
  private Text snapshotRepositoryIdText;
  private Text text_2;
  private CCombo layoutCombo_1;
  private Text text_1;
  private Text text;
  private FormToolkit toolkit = new FormToolkit(Display.getCurrent());
  
  private ListEditorComposite<Repository> repositoriesEditor;
  private ListEditorComposite<Repository> pluginRepositoriesEditor;

  private Section repositoryDetailsSection;
  
  private Text idText;
  private Text nameText;
  private Text repositoryUrlText;
  private CCombo layoutCombo;

  private Button releasesEnabledButton;
  private CCombo releasesUpdatePolicyCombo;
  private CCombo releasesChecksumPolicyCombo;

  private Button snapshotsEnabledButton;
  private CCombo snapshotsUpdatePolicyCombo;
  private CCombo snapshotsChecksumPolicyCombo;

  private Label snapshotsChecksumPolicyLabel;
  private Label snapshotsUpdatePolicyLabel;
  private Label releasesChecksumPolicyLabel;
  private Label releasesUpdatePolicyLabel;

  private Model model;
  
  private Repositories repositories;
  private PluginRepositories pluginRepositories;
  private Text projectDownloadUrlText;
  
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
    
    SashForm distributionManagementSash = new SashForm(this, SWT.NONE);

    Section releaseDistributionRepositorySection = toolkit.createSection(distributionManagementSash, Section.TITLE_BAR | Section.TWISTIE);
    releaseDistributionRepositorySection.setText("Release Distribution Repository");

    Composite composite = toolkit.createComposite(releaseDistributionRepositorySection, SWT.NONE);
    GridLayout gridLayout_1 = new GridLayout();
    gridLayout_1.numColumns = 2;
    composite.setLayout(gridLayout_1);
    toolkit.paintBordersFor(composite);
    releaseDistributionRepositorySection.setClient(composite);

    toolkit.createLabel(composite, "Id:", SWT.NONE);

    text = toolkit.createText(composite, null, SWT.NONE);
    text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    toolkit.createLabel(composite, "Name:", SWT.NONE);

    text_1 = toolkit.createText(composite, null, SWT.NONE);
    text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Hyperlink repositoryUrlHyperlink = toolkit.createHyperlink(composite, "URL:", SWT.NONE);
    repositoryUrlHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(text_2.getText());
      }
    });

    text_2 = toolkit.createText(composite, null, SWT.NONE);
    text_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    
    Label label = toolkit.createLabel(composite, "Layout:", SWT.NONE);
    label.setLayoutData(new GridData());

    layoutCombo_1 = new CCombo(composite, SWT.READ_ONLY | SWT.FLAT);
    layoutCombo_1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    layoutCombo_1.setData("FormWidgetFactory.drawBorder", "textBorder");
    toolkit.adapt(layoutCombo_1, true, true);
    new Label(composite, SWT.NONE);

    Button button = toolkit.createButton(composite, "Unique Version", SWT.CHECK);
    button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    distributionManagementSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    toolkit.adapt(distributionManagementSash, true, true);

    Section distributionSnapshotsRepositorySection = toolkit.createSection(distributionManagementSash, Section.TITLE_BAR | Section.TWISTIE);
    distributionSnapshotsRepositorySection.setText("Snapshots Distribution Repository");

    Composite composite_1 = toolkit.createComposite(distributionSnapshotsRepositorySection, SWT.NONE);
    composite_1.setLayout(new GridLayout(2, false));
    toolkit.paintBordersFor(composite_1);
    distributionSnapshotsRepositorySection.setClient(composite_1);

    toolkit.createLabel(composite_1, "Id:", SWT.NONE);

    snapshotRepositoryIdText = toolkit.createText(composite_1, null, SWT.NONE);
    snapshotRepositoryIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    toolkit.createLabel(composite_1, "Name:", SWT.NONE);

    snapshotRepositoryNameText = toolkit.createText(composite_1, null, SWT.NONE);
    snapshotRepositoryNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Hyperlink snapshotRepositoryUrlHyperlink = toolkit.createHyperlink(composite_1, "URL:", SWT.NONE);
    snapshotRepositoryUrlHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(snapshotRepositoryUrlText.getText());
      }
    });
    
    snapshotRepositoryUrlText = toolkit.createText(composite_1, null, SWT.NONE);
    snapshotRepositoryUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label layoutLabel = toolkit.createLabel(composite_1, "Layout:", SWT.NONE);
    layoutLabel.setLayoutData(new GridData());

    layoutCombo_2 = new CCombo(composite_1, SWT.READ_ONLY | SWT.FLAT);
    layoutCombo_2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    layoutCombo_2.setData("FormWidgetFactory.drawBorder", "textBorder");
    toolkit.adapt(layoutCombo_2, true, true);
    new Label(composite_1, SWT.NONE);

    Button uniqueVersionButton = toolkit.createButton(composite_1, "Unique Version", SWT.CHECK);
    uniqueVersionButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    
    distributionManagementSash.setWeights(new int[] {1, 1 });
    
    SashForm sashForm = new SashForm(this, SWT.NONE);

    Section projectSiteSection = toolkit.createSection(sashForm, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
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
    sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    toolkit.adapt(sashForm, true, true);
    
    Section relocationSection = toolkit.createSection(sashForm, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
    relocationSection.setText("Relocation");

    Composite relocationComposite = toolkit.createComposite(relocationSection, SWT.NONE);
    GridLayout gridLayout_2 = new GridLayout();
    gridLayout_2.numColumns = 2;
    relocationComposite.setLayout(gridLayout_2);
    toolkit.paintBordersFor(relocationComposite);
    relocationSection.setClient(relocationComposite);

    toolkit.createLabel(relocationComposite, "Group Id:", SWT.NONE);

    relocationGroupIdText = toolkit.createText(relocationComposite, null, SWT.NONE);
    GridData gd_relocationGroupIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    relocationGroupIdText.setLayoutData(gd_relocationGroupIdText);

    toolkit.createLabel(relocationComposite, "Artifact Id:", SWT.NONE);

    relocationArtifactIdText = toolkit.createText(relocationComposite, null, SWT.NONE);
    GridData gd_relocationArtifactIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    relocationArtifactIdText.setLayoutData(gd_relocationArtifactIdText);

    toolkit.createLabel(relocationComposite, "Version:", SWT.NONE);

    relocationVersionText = toolkit.createText(relocationComposite, null, SWT.NONE);
    GridData gd_relocationVersionText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    relocationVersionText.setLayoutData(gd_relocationVersionText);

    toolkit.createLabel(relocationComposite, "Message:", SWT.NONE);

    relocationMessageText = toolkit.createText(relocationComposite, null, SWT.NONE);
    GridData gd_relocationMessageText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    relocationMessageText.setLayoutData(gd_relocationMessageText);

    
    
    sashForm.setWeights(new int[] {1, 1 });
    
  }

  private void createRepositoriesSection(SashForm verticalSash) {
    Section repositoriesSection = toolkit.createSection(verticalSash, Section.TITLE_BAR | Section.COMPACT);
    repositoriesSection.setText("Repositories");

    repositoriesEditor = new ListEditorComposite<Repository>(repositoriesSection, SWT.NONE);
    
    repositoriesEditor.setLabelProvider(new RepositoryLabelProvider());
    repositoriesEditor.setContentProvider(new ListEditorContentProvider<Repository>());
    
    repositoriesEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
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

    layoutCombo = new CCombo(repositoryDetailsComposite, SWT.READ_ONLY | SWT.FLAT);
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
    toolkit.adapt(releasesUpdatePolicyCombo, true, true);
    releasesUpdatePolicyCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    releasesUpdatePolicyCombo.setItems(new String[] {"daily", "always", "interval:30", "never"});
    releasesUpdatePolicyCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

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

  public void loadData(MavenPomEditorPage editorPage) {
    model = editorPage.getModel();
    loadRepositories(model);
    loadPluginRepositories(model);
  }

  private void loadRepositories(Model model) {
    repositories = model.getRepositories();
    repositoriesEditor.setInput(repositories==null ? null : repositories.getRepository());
  }
  
  private void loadPluginRepositories(Model model) {
    pluginRepositories = model.getPluginRepositories();
    pluginRepositoriesEditor.setInput(pluginRepositories==null ? null : pluginRepositories.getPluginRepository());
  }

  public void updateView(MavenPomEditorPage editorPage, Notification notification) {
    EObject object = (EObject) notification.getNotifier();
    if(object instanceof Repositories || object instanceof Repository || object instanceof PluginRepositories) {
      loadRepositories(model);
    }
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
