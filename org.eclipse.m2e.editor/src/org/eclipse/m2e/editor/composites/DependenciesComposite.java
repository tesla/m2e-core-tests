/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.editor.composites;

import static org.eclipse.m2e.editor.pom.FormUtils.nvl;
import static org.eclipse.m2e.editor.pom.FormUtils.setButton;
import static org.eclipse.m2e.editor.pom.FormUtils.setText;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.m2e.core.actions.OpenPomAction;
import org.eclipse.m2e.core.actions.OpenUrlAction;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.ui.dialogs.MavenRepositorySearchDialog;
import org.eclipse.m2e.core.wizards.WidthGroup;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.editor.pom.FormUtils;
import org.eclipse.m2e.editor.pom.MavenPomEditorPage;
import org.eclipse.m2e.editor.pom.SearchControl;
import org.eclipse.m2e.editor.pom.SearchMatcher;
import org.eclipse.m2e.editor.pom.ValueProvider;
import org.eclipse.m2e.editor.xml.search.Packaging;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.DependencyManagement;
import org.eclipse.m2e.model.edit.pom.Exclusion;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
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


/**
 * @author Eugene Kuleshov
 */
public class DependenciesComposite extends Composite {

  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;
  
  protected MavenPomEditorPage editorPage; 
  
  private FormToolkit toolkit = new FormToolkit(Display.getCurrent());

  // controls
  
  ListEditorComposite<Dependency> dependencyManagementEditor;
  ListEditorComposite<Dependency> dependenciesEditor;

  Composite dependencyDetailsComposite;

  WidthGroup detailsWidthGroup = new WidthGroup();

  Text groupIdText;
  Text artifactIdText;
  Text versionText;
  Text classifierText;
  CCombo scopeCombo;
  CCombo typeCombo;
  Text systemPathText;
  Button selectSystemPathButton;

  Button optionalButton;

  ListEditorComposite<Exclusion> exclusionsEditor;

  Section exclusionDetailsSection;
  Text exclusionArtifactIdText;
  Text exclusionGroupIdText;

  Button dependencySelectButton;
  Button exclusionSelectButton;
  
  //Action dependencyAddAction;
  //Action dependencyManagementAddAction;
  Action dependencySelectAction;

  SearchControl searchControl;
  SearchMatcher searchMatcher;
  DependencyFilter searchFilter;
  
  Action exclusionSelectAction;
//  Action exclusionAddAction;
  Action openWebPageAction;
  
  // model
  
  Dependency currentDependency;

  Exclusion currentExclusion;

  boolean changingSelection = false;

  Model model;
  ValueProvider<DependencyManagement> dependencyManagementProvider;

  DependencyLabelProvider dependencyLabelProvider = new DependencyLabelProvider();
  DependencyLabelProvider dependencyManagementLabelProvider = new DependencyLabelProvider();
  DependencyLabelProvider exclusionLabelProvider = new DependencyLabelProvider();


  public DependenciesComposite(Composite composite, MavenPomEditorPage editorPage, int flags) {
    super(composite, flags);
    this.editorPage = editorPage;
    createComposite();
    editorPage.initPopupMenu(dependenciesEditor.getViewer(), ".dependencies");
    editorPage.initPopupMenu(dependencyManagementEditor.getViewer(), ".dependencyManagement");
  }

  private void createComposite() {
    GridLayout gridLayout = new GridLayout();
    gridLayout.makeColumnsEqualWidth = true;
    gridLayout.marginWidth = 0;
    setLayout(gridLayout);
    toolkit.adapt(this);

    SashForm horizontalSash = new SashForm(this, SWT.NONE);
    GridData horizontalCompositeGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    horizontalCompositeGridData.heightHint = 200;
    horizontalSash.setLayoutData(horizontalCompositeGridData);
    toolkit.adapt(horizontalSash, true, true);

    SashForm verticalSash = new SashForm(horizontalSash, SWT.VERTICAL);
    toolkit.adapt(verticalSash, true, true);

    createDependenciesSection(verticalSash);
    createDependencyManagementSection(verticalSash);

    verticalSash.setWeights(new int[] {1, 1});

    dependencyDetailsComposite = new Composite(horizontalSash, SWT.NONE);
    GridLayout dependencyDetailsLayout = new GridLayout(1, false);
    dependencyDetailsLayout.marginWidth = 0;
    dependencyDetailsLayout.marginHeight = 0;
    dependencyDetailsComposite.setLayout(dependencyDetailsLayout);
    toolkit.adapt(dependencyDetailsComposite, true, true);
    
    createDependencyDetails(toolkit, dependencyDetailsComposite);
    createExclusionsSection(toolkit, dependencyDetailsComposite);
    createExclusionDetailsSection(toolkit, dependencyDetailsComposite);
    
    horizontalSash.setWeights(new int[] {1, 1});
    
    updateDependencyDetails(null);
  }

  private void createDependenciesSection(SashForm verticalSash) {
    Section dependenciesSection = toolkit.createSection(verticalSash, ExpandableComposite.TITLE_BAR);
    dependenciesSection.marginWidth = 3;
    dependenciesSection.setText(Messages.DependenciesComposite_sectionDependencies);

    dependenciesEditor = new ListEditorComposite<Dependency>(dependenciesSection, SWT.NONE, true);
    dependenciesEditor.setLabelProvider(dependencyLabelProvider);
    dependenciesEditor.setContentProvider(new ListEditorContentProvider<Dependency>());

    dependenciesEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        Dependency dependency = createDependency(new ValueProvider<Model>() {
          @Override
          public Model getValue() {
            return model;
          }
        }, POM_PACKAGE.getModel_Dependencies(), null, null, null, null, null, null);
        dependenciesEditor.setInput(model.getDependencies());
        dependenciesEditor.setSelection(Collections.singletonList(dependency));
        updateDependencyDetails(dependency);
        groupIdText.setFocus();
      }
    });

    dependenciesEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = editorPage.getEditingDomain();

        List<Dependency> dependencyList = dependenciesEditor.getSelection();
        for(Dependency dependency : dependencyList) {
          Command removeCommand = RemoveCommand.create(editingDomain, model, 
        		  POM_PACKAGE.getModel_Dependencies(), dependency);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
        updateDependencyDetails(null);
      }
    });

    dependenciesEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Dependency> selection = dependenciesEditor.getSelection();
        updateDependencyDetails(selection.size()==1 ? selection.get(0) : null);
        
        if(!selection.isEmpty()) {
          changingSelection = true;
          try {
            dependencyManagementEditor.setSelection(Collections.<Dependency>emptyList());
          } finally {
            changingSelection = false;
          }
        }
      }
    });

    dependenciesSection.setClient(dependenciesEditor);
    toolkit.adapt(dependenciesEditor);
    toolkit.paintBordersFor(dependenciesEditor);
    
    dependenciesEditor.setSelectListener(new SelectionAdapter(){
      public void widgetSelected(SelectionEvent e) {
        // TODO calculate current list of artifacts for the project
        Set<ArtifactKey> artifacts = Collections.emptySet();
        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
            Messages.DependenciesComposite_searchDialog_title, IIndex.SEARCH_ARTIFACT, artifacts, true);
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            Dependency dependency = createDependency(new ValueProvider<Model>() {
              @Override
              public Model getValue() {
                return model;
              }
            }, POM_PACKAGE.getModel_Dependencies(), //
                af.group, af.artifact, af.version, af.classifier, 
                "jar".equals(nvl(af.type)) ? "" : nvl(af.type), //$NON-NLS-1$ //$NON-NLS-2$
                "compile".equals(nvl(dialog.getSelectedScope())) ? "" : nvl(dialog.getSelectedScope()));//$NON-NLS-0$ //$NON-NLS-1$
            dependenciesEditor.setInput(model.getDependencies());
            dependenciesEditor.setSelection(Collections.singletonList(dependency));
            updateDependencyDetails(dependency);
            groupIdText.setFocus();
          }
        }
      }
    });
//    dependencyAddAction = new Action("Add Dependency", MavenEditorImages.ADD_ARTIFACT) {
//      public void run() {
//        // TODO calculate current list of artifacts for the project
//        Set<ArtifactKey> artifacts = Collections.emptySet();
//        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
//            "Add Dependency", IndexManager.SEARCH_ARTIFACT, artifacts, true);
//        if(dialog.open() == Window.OK) {
//          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
//          if(af != null) {
//            Dependency dependency = createDependency(dependenciesProvider, //
//                af.group, af.artifact, af.version, af.classifier, af.type, dialog.getSelectedScope());
//            dependenciesEditor.setInput(dependenciesProvider.getValue().getDependency());
//            dependenciesEditor.setSelection(Collections.singletonList(dependency));
//            updateDependencyDetails(dependency);
//            groupIdText.setFocus();
//          }
//        }
//      }
//    };
//    dependencyAddAction.setEnabled(false);

    
//    modulesToolBarManager.add(dependencyAddAction);
//    modulesToolBarManager.add(new Separator());
    ToolBarManager modulesToolBarManager = new ToolBarManager(SWT.FLAT);
    modulesToolBarManager.add(new Action(Messages.DependenciesComposite_action_showgroupid, MavenEditorImages.SHOW_GROUP) {
      {
        setChecked(false);
      }
      public int getStyle() {
        return AS_CHECK_BOX;
      }
      public void run() {
        dependencyLabelProvider.setShowGroupId(isChecked());
        dependenciesEditor.getViewer().refresh();
      }
    });
    
    modulesToolBarManager.add(new Action(Messages.DependenciesComposite_action_filter, MavenEditorImages.FILTER) {
      public int getStyle() {
        return AS_CHECK_BOX;
      }
      public void run() {
        TableViewer viewer = dependenciesEditor.getViewer();
        if(isChecked()) {
          viewer.addFilter(searchFilter);
        } else {
          viewer.removeFilter(searchFilter);
        }
        viewer.refresh();
        if(isChecked()) {
          searchControl.getSearchText().setFocus();
        }
      }
    });
    
    Composite toolbarComposite = toolkit.createComposite(dependenciesSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);
 
    modulesToolBarManager.createControl(toolbarComposite);
    dependenciesSection.setTextClient(toolbarComposite);
  }

  private void createDependencyManagementSection(SashForm verticalSash) {
    Section dependencyManagementSection = toolkit.createSection(verticalSash, ExpandableComposite.TITLE_BAR);
    dependencyManagementSection.marginWidth = 3;
    dependencyManagementSection.setText(Messages.DependenciesComposite_sectionDependencyManagement);

    dependencyManagementEditor = new ListEditorComposite<Dependency>(dependencyManagementSection, SWT.NONE, true);
    dependencyManagementSection.setClient(dependencyManagementEditor);

    dependencyManagementEditor.setLabelProvider(dependencyManagementLabelProvider);
    dependencyManagementEditor.setContentProvider(new ListEditorContentProvider<Dependency>());

    dependencyManagementEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        Dependency dependency = createDependency(dependencyManagementProvider, POM_PACKAGE.getDependencyManagement_Dependencies(), null, null, null, null, null, null);
        dependencyManagementEditor.setInput(dependencyManagementProvider.getValue().getDependencies());
        dependencyManagementEditor.setSelection(Collections.singletonList(dependency));
        updateDependencyDetails(dependency);
        groupIdText.setFocus();
      }
    });
 
    dependencyManagementEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = editorPage.getEditingDomain();
 
        List<Dependency> dependencyList = dependencyManagementEditor.getSelection();
        for(Dependency dependency : dependencyList) {
          Command removeCommand = RemoveCommand.create(editingDomain, //
              dependencyManagementProvider.getValue(), POM_PACKAGE.getDependencyManagement_Dependencies(), dependency);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
        updateDependencyDetails(null);
      }
    });

    dependencyManagementEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Dependency> selection = dependencyManagementEditor.getSelection();
        updateDependencyDetails(selection.size()==1 ? selection.get(0) : null);
        
        if(!selection.isEmpty()) {
          changingSelection = true;
          try {
            dependenciesEditor.setSelection(Collections.<Dependency>emptyList());
          } finally {
            changingSelection = false;
          }
        }
      }
    });

    toolkit.adapt(dependencyManagementEditor);
    toolkit.paintBordersFor(dependencyManagementEditor);
    
    dependencyManagementEditor.setSelectListener(new SelectionAdapter(){
      public void widgetSelected(SelectionEvent e){
        // TODO calculate current list of artifacts for the project
        Set<ArtifactKey> artifacts = Collections.emptySet();
        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
            Messages.DependenciesComposite_searchDialog_title, IIndex.SEARCH_ARTIFACT, artifacts, true);
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            Dependency dependency = createDependency(dependencyManagementProvider, POM_PACKAGE.getDependencyManagement_Dependencies(), //
                af.group, af.artifact, af.version, af.classifier, 
                "jar".equals(nvl(af.type)) ? "" : nvl(af.type), //$NON-NLS-1$ //$NON-NLS-2$
                "compile".equals(nvl(dialog.getSelectedScope())) ? "" : nvl(dialog.getSelectedScope()));//$NON-NLS-1$ //$NON-NLS-2$
            dependencyManagementEditor.setInput(dependencyManagementProvider.getValue().getDependencies());
            dependencyManagementEditor.setSelection(Collections.singletonList(dependency));
            updateDependencyDetails(dependency);
            groupIdText.setFocus();
          }
        }
      }
    });
//    dependencyManagementAddAction = new Action("Add Dependency", MavenEditorImages.ADD_ARTIFACT) {
//      public void run() {
//        // TODO calculate current list of artifacts for the project
//        Set<ArtifactKey> artifacts = Collections.emptySet();
//        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
//            "Add Dependency", IndexManager.SEARCH_ARTIFACT, artifacts, true);
//        if(dialog.open() == Window.OK) {
//          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
//          if(af != null) {
//            Dependency dependency = createDependency(dependencyManagementProvider, //
//                af.group, af.artifact, af.version, af.classifier, af.type, dialog.getSelectedScope());
//            dependencyManagementEditor.setInput(dependencyManagementProvider.getValue().getDependency());
//            dependencyManagementEditor.setSelection(Collections.singletonList(dependency));
//            updateDependencyDetails(dependency);
//            groupIdText.setFocus();
//          }
//        }
//      }
//    };
//    dependencyManagementAddAction.setEnabled(false);

    ToolBarManager modulesToolBarManager = new ToolBarManager(SWT.FLAT);
//    modulesToolBarManager.add(dependencyManagementAddAction);
//    modulesToolBarManager.add(new Separator());

    modulesToolBarManager.add(new Action(Messages.DependenciesComposite_action_showgroupid, MavenEditorImages.SHOW_GROUP) {
      {
        setChecked(false);
      }
      public int getStyle() {
        return AS_CHECK_BOX;
      }
      public void run() {
        dependencyManagementLabelProvider.setShowGroupId(isChecked());
        dependencyManagementEditor.getViewer().refresh();
      }
    });
    
    modulesToolBarManager.add(new Action(Messages.DependenciesComposite_action_filter, MavenEditorImages.FILTER) {
      public int getStyle() {
        return AS_CHECK_BOX;
      }
      public void run() {
        TableViewer viewer = dependencyManagementEditor.getViewer();
        if(isChecked()) {
          viewer.addFilter(searchFilter);
        } else {
          viewer.removeFilter(searchFilter);
        }
        viewer.refresh();
        if(isChecked()) {
          searchControl.getSearchText().setFocus();
        }
      }
    });
    
    Composite toolbarComposite = toolkit.createComposite(dependencyManagementSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);
 
    modulesToolBarManager.createControl(toolbarComposite);
    dependencyManagementSection.setTextClient(toolbarComposite);
  }

  private void createDependencyDetails(FormToolkit toolkit, Composite dependencyDetailsComposite) {
    Section dependencyDetailsSection = toolkit.createSection(dependencyDetailsComposite, ExpandableComposite.TITLE_BAR);
    dependencyDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    dependencyDetailsSection.setText(Messages.DependenciesComposite_section_dependency_details);
    dependencyDetailsSection.marginWidth = 3;
    
    dependencySelectAction = new Action(Messages.DependenciesComposite_action_selectDependency, MavenEditorImages.SELECT_ARTIFACT) {
      public void run() {
        // TODO calculate current list of artifacts for the project
        Set<ArtifactKey> artifacts = Collections.emptySet();
        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
            Messages.DependenciesComposite_searchDialog_title, IIndex.SEARCH_ARTIFACT, artifacts, true);
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            groupIdText.setText(nvl(af.group));
            artifactIdText.setText(nvl(af.artifact));
            versionText.setText(nvl(af.version));
            //MNGECLIPSE-2363
            String type = nvl(af.type);
            if ("jar".equals(type)) {//$NON-NLS-1$
              type = ""; //$NON-NLS-1$
            }
            typeCombo.setText(type);
            String scope = nvl(dialog.getSelectedScope());
            if ("compile".equals(scope)) {//$NON-NLS-1$
              scope = "";//$NON-NLS-1$
            }
            scopeCombo.setText(scope);
          }
        }
      }
    };
    dependencySelectAction.setEnabled(false);

    openWebPageAction = new Action(Messages.DependenciesComposite_action_open_project_page, MavenEditorImages.WEB_PAGE) {
      public void run() {
        final String groupId = groupIdText.getText();
        final String artifactId = artifactIdText.getText();
        final String version = versionText.getText();
        new Job("Opening " + groupId + ":" + artifactId + ":" + version) {
          protected IStatus run(IProgressMonitor monitor) {
            OpenUrlAction.openBrowser(OpenUrlAction.ID_PROJECT, groupId, artifactId, //
                version != null ? version : getVersion(groupId, artifactId, monitor), monitor);
            return Status.OK_STATUS;
          }
        }.schedule();
        
      }      
    };
    openWebPageAction.setEnabled(false);
    
    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
    toolBarManager.add(dependencySelectAction);
    toolBarManager.add(new Separator());
    toolBarManager.add(openWebPageAction);
    
    Composite toolbarComposite = toolkit.createComposite(dependencyDetailsSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);
 
    toolBarManager.createControl(toolbarComposite);
    dependencyDetailsSection.setTextClient(toolbarComposite);
    
    Composite dependencyComposite = toolkit.createComposite(dependencyDetailsSection, SWT.NONE);
    GridLayout dependencyCompositeLayout = new GridLayout(3, false);
    dependencyCompositeLayout.marginWidth = 2;
    dependencyCompositeLayout.marginHeight = 2;
    dependencyComposite.setLayout(dependencyCompositeLayout);
    toolkit.paintBordersFor(dependencyComposite);
    dependencyDetailsSection.setClient(dependencyComposite);
    dependencyComposite.addControlListener(detailsWidthGroup);

    Label groupIdLabel = toolkit.createLabel(dependencyComposite, Messages.DependenciesComposite_lblGroupId, SWT.NONE);
    groupIdLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(groupIdLabel);

    groupIdText = toolkit.createText(dependencyComposite, null, SWT.NONE);
    GridData gd_groupIdText = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    gd_groupIdText.horizontalIndent = 4;
    groupIdText.setLayoutData(gd_groupIdText);
    FormUtils.addGroupIdProposal(editorPage.getProject(), groupIdText, Packaging.ALL);

    Hyperlink artifactIdHyperlink = toolkit.createHyperlink(dependencyComposite, Messages.DependenciesComposite_lblArtifactId, SWT.NONE);
    artifactIdHyperlink.setLayoutData(new GridData());
    artifactIdHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        final String groupId = groupIdText.getText();
        final String artifactId = artifactIdText.getText();
        final String version = versionText.getText();
        new Job("Opening " + groupId + ":" + artifactId + ":" + version) {
          protected IStatus run(IProgressMonitor monitor) {
            OpenPomAction.openEditor(groupId, artifactId, //
                version != null ? version : getVersion(groupId, artifactId, monitor), //
                    monitor);
            return Status.OK_STATUS;
          }
        }.schedule();
      }
    });
    
    detailsWidthGroup.addControl(artifactIdHyperlink);

    artifactIdText = toolkit.createText(dependencyComposite, null, SWT.NONE);
    GridData gd_artifactIdText = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    gd_artifactIdText.horizontalIndent = 4;
    artifactIdText.setLayoutData(gd_artifactIdText);
    FormUtils.addArtifactIdProposal(editorPage.getProject(), groupIdText, artifactIdText, Packaging.ALL);

    Label versionLabel = toolkit.createLabel(dependencyComposite, Messages.DependenciesComposite_lblVersion, SWT.NONE);
    versionLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(versionLabel);

    versionText = toolkit.createText(dependencyComposite, null, SWT.NONE);
    GridData versionTextData = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    versionTextData.horizontalIndent = 4;
    versionTextData.widthHint = 200;
    versionText.setLayoutData(versionTextData);
    FormUtils.addVersionProposal(editorPage.getProject(), groupIdText, artifactIdText, versionText, Packaging.ALL);

//    dependencySelectButton = toolkit.createButton(dependencyComposite, "Select...", SWT.NONE);
//    dependencySelectButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 2));
//    dependencySelectButton.addSelectionListener(new SelectionAdapter() {
//      public void widgetSelected(SelectionEvent e) {
//        // TODO calculate current list of artifacts for the project
//        Set<Dependency> artifacts = Collections.emptySet();
//        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
//            "Add Dependency", IndexManager.SEARCH_ARTIFACT, artifacts);
//        if(dialog.open() == Window.OK) {
//          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
//          if(af != null) {
//            groupIdText.setText(nvl(af.group));
//            artifactIdText.setText(nvl(af.artifact));
//            versionText.setText(nvl(af.version));
//            typeCombo.setText(nvl(af.type));
//          }
//        }
//      }
//    });

    Label classifierLabel = toolkit.createLabel(dependencyComposite, Messages.DependenciesComposite_lblClassifier, SWT.NONE);
    classifierLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(classifierLabel);

    classifierText = toolkit.createText(dependencyComposite, null, SWT.NONE);
    GridData gd_classifierText = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    gd_classifierText.horizontalIndent = 4;
    gd_classifierText.widthHint = 200;
    classifierText.setLayoutData(gd_classifierText);
    FormUtils.addClassifierProposal(editorPage.getProject(), groupIdText, artifactIdText, versionText, classifierText, Packaging.ALL);
    
    Label typeLabel = toolkit.createLabel(dependencyComposite, Messages.DependenciesComposite_lblType, SWT.NONE);
    typeLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(typeLabel);

    typeCombo = new CCombo(dependencyComposite, SWT.FLAT);
    // FormUtils.addTypeProposal(groupIdText, artifactIdText, versionText, typeCombo, Packaging.ALL);
    
    // TODO retrieve artifact type from selected dependency 
    typeCombo.add("jar"); //$NON-NLS-1$
    typeCombo.add("war"); //$NON-NLS-1$
    typeCombo.add("rar"); //$NON-NLS-1$
    typeCombo.add("ear"); //$NON-NLS-1$
    typeCombo.add("par"); //$NON-NLS-1$
    typeCombo.add("ejb"); //$NON-NLS-1$
    typeCombo.add("ejb-client"); //$NON-NLS-1$
    typeCombo.add("test-jar"); //$NON-NLS-1$
    typeCombo.add("java-source"); //$NON-NLS-1$
    typeCombo.add("javadoc"); //$NON-NLS-1$
    typeCombo.add("maven-plugin"); //$NON-NLS-1$
    typeCombo.add("pom"); //$NON-NLS-1$
    
    GridData gd_typeText = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    gd_typeText.horizontalIndent = 4;
    gd_typeText.widthHint = 120;
    typeCombo.setLayoutData(gd_typeText);
    typeCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    toolkit.adapt(typeCombo, true, true);

    Label scopeLabel = toolkit.createLabel(dependencyComposite, Messages.DependenciesComposite_lblScope, SWT.NONE);
    scopeLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(scopeLabel);

    scopeCombo = new CCombo(dependencyComposite, SWT.READ_ONLY | SWT.FLAT);
    scopeCombo.add("compile"); //$NON-NLS-1$
    scopeCombo.add("test"); //$NON-NLS-1$
    scopeCombo.add("provided"); //$NON-NLS-1$
    scopeCombo.add("runtime"); //$NON-NLS-1$
    scopeCombo.add("system"); //$NON-NLS-1$
    // TODO should be only used on a dependency of type pom in the <dependencyManagement> section
    scopeCombo.add("import");  //$NON-NLS-1$
    
    GridData gd_scopeText = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    gd_scopeText.horizontalIndent = 4;
    gd_scopeText.widthHint = 120;
    scopeCombo.setLayoutData(gd_scopeText);
    scopeCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    toolkit.adapt(scopeCombo, true, true);

    Label systemPathLabel = toolkit.createLabel(dependencyComposite, Messages.DependenciesComposite_lblSystemPath, SWT.NONE);
    systemPathLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(systemPathLabel);

    systemPathText = toolkit.createText(dependencyComposite, null, SWT.NONE);
    GridData gd_systemPathText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_systemPathText.horizontalIndent = 4;
    gd_systemPathText.widthHint = 200;
    systemPathText.setLayoutData(gd_systemPathText);

    selectSystemPathButton = toolkit.createButton(dependencyComposite, Messages.DependenciesComposite_btnSelect, SWT.NONE);
    new Label(dependencyComposite, SWT.NONE);

    optionalButton = toolkit.createButton(dependencyComposite, Messages.DependenciesComposite_btnOptional, SWT.CHECK);
    GridData gd_optionalButton = new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1);
    gd_optionalButton.horizontalIndent = 4;
    optionalButton.setLayoutData(gd_optionalButton);
    dependencyComposite.setTabList(new Control[] {groupIdText, artifactIdText, versionText, classifierText, typeCombo,
        scopeCombo, systemPathText, selectSystemPathButton, optionalButton});
  }

  private void createExclusionsSection(FormToolkit toolkit, Composite composite) {
    Section exclusionsSection = toolkit.createSection(composite, ExpandableComposite.TITLE_BAR);
    exclusionsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    exclusionsSection.setText(Messages.DependenciesComposite_sectionExclusions);
    exclusionsSection.marginWidth = 3;

    exclusionsEditor = new ListEditorComposite<Exclusion>(exclusionsSection, SWT.NONE, true);
    exclusionsSection.setClient(exclusionsEditor);
    toolkit.adapt(exclusionsEditor);

    exclusionsEditor.setContentProvider(new ListEditorContentProvider<Exclusion>());
    exclusionsEditor.setLabelProvider(exclusionLabelProvider);
    
    exclusionsEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Exclusion> selection = exclusionsEditor.getSelection();
        updateExclusionDetails(selection.size()==1 ? selection.get(0) : null);
      }
    });

    exclusionsEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        createExclusion(null, null);        
      }
    });
 
    exclusionsEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = editorPage.getEditingDomain();
 
        EList<Exclusion> exclusions = currentDependency.getExclusions();
        int n = 0;
        for(Exclusion exclusion : exclusionsEditor.getSelection()) {
          Command removeCommand = RemoveCommand.create(editingDomain, currentDependency, 
              POM_PACKAGE.getExclusion(), exclusion);
          compoundCommand.append(removeCommand);
          n++;
        }
        if(exclusions.size()-n==0) {
          //if there are no exclusions left, just create a command to yank all the exclusions at once (by
          //removing the root <exclusions> tag
            Command removeExclusions = SetCommand.create(editingDomain, currentDependency, 
                POM_PACKAGE.getDependency_Exclusions(), SetCommand.UNSET_VALUE);
            compoundCommand = new CompoundCommand();
            compoundCommand.append(removeExclusions);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
        updateExclusionDetails(null);
      }
    });
    exclusionsEditor.setSelectListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        Set<ArtifactKey> artifacts = Collections.emptySet();
        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
            Messages.DependenciesComposite_searchTitle_addExclusion, IIndex.SEARCH_ARTIFACT, artifacts);
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            createExclusion(af.group, af.artifact);        
          }
        }
      }
    });
//    exclusionAddAction = new Action("Add Exclusion", MavenEditorImages.ADD_ARTIFACT) {
//      public void run() {
//        // XXX calculate list available for exclusion
//        Set<ArtifactKey> artifacts = Collections.emptySet();
//        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
//            "Add Exclusion", IndexManager.SEARCH_ARTIFACT, artifacts);
//        if(dialog.open() == Window.OK) {
//          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
//          if(af != null) {
//            createExclusion(af.group, af.artifact);        
//          }
//        }
//      }
//    };
//    exclusionAddAction.setEnabled(false);

//    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
//    //toolBarManager.add(exclusionAddAction);
//    
//    Composite toolbarComposite = toolkit.createComposite(exclusionsSection);
//    GridLayout toolbarLayout = new GridLayout(1, true);
//    toolbarLayout.marginHeight = 0;
//    toolbarLayout.marginWidth = 0;
//    toolbarComposite.setLayout(toolbarLayout);
//    toolbarComposite.setBackground(null);
// 
//    toolBarManager.createControl(toolbarComposite);
//    exclusionsSection.setTextClient(toolbarComposite);
  }

  private void createExclusionDetailsSection(FormToolkit toolkit, Composite dependencyDetailsComposite) {
    exclusionDetailsSection = toolkit.createSection(dependencyDetailsComposite, ExpandableComposite.TITLE_BAR);
    exclusionDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    exclusionDetailsSection.setText(Messages.DependenciesComposite_section_ExclusionDetails);

    Composite exclusionDetailsComposite = toolkit.createComposite(exclusionDetailsSection, SWT.NONE);
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginHeight = 2;
    gridLayout.marginWidth = 2;
    exclusionDetailsComposite.setLayout(gridLayout);
    toolkit.paintBordersFor(exclusionDetailsComposite);
    exclusionDetailsSection.setClient(exclusionDetailsComposite);
    exclusionDetailsComposite.addControlListener(detailsWidthGroup);
    
    Label exclusionGroupIdLabel = toolkit.createLabel(exclusionDetailsComposite, Messages.DependenciesComposite_lblExclusionGroupId, SWT.NONE);
    exclusionGroupIdLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(exclusionGroupIdLabel);

    exclusionGroupIdText = toolkit.createText(exclusionDetailsComposite, null, SWT.NONE);
    GridData gd_exclusionGroupIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_exclusionGroupIdText.horizontalIndent = 4;
    exclusionGroupIdText.setLayoutData(gd_exclusionGroupIdText);
    // TODO handle ArtifactInfo
    FormUtils.addGroupIdProposal(editorPage.getProject(), exclusionGroupIdText, Packaging.ALL);

//    exclusionSelectButton = toolkit.createButton(exclusionDetailsComposite, "Select...", SWT.NONE);
//    exclusionSelectButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 2));
//    exclusionSelectButton.addSelectionListener(new SelectionAdapter() {
//      public void widgetSelected(SelectionEvent e) {
//        // TODO calculate current list of artifacts for the project
//        Set<Dependency> artifacts = Collections.emptySet();
//        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
//            "Add Dependency", IndexManager.SEARCH_ARTIFACT, artifacts);
//        if(dialog.open() == Window.OK) {
//          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
//          if(af != null) {
//            exclusionGroupIdText.setText(nvl(af.group));
//            exclusionArtifactIdText.setText(nvl(af.artifact));
//          }
//        }
//      }
//    });

    Label exclusionArtifactIdLabel = toolkit.createLabel(exclusionDetailsComposite, Messages.DependenciesComposite_lblExclusionArtifactId, SWT.NONE);
    exclusionArtifactIdLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(exclusionArtifactIdLabel);

    exclusionArtifactIdText = toolkit.createText(exclusionDetailsComposite, null, SWT.NONE);
    GridData gd_exclusionArtifactIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_exclusionArtifactIdText.horizontalIndent = 4;
    exclusionArtifactIdText.setLayoutData(gd_exclusionArtifactIdText);
    // TODO handle ArtifactInfo
    FormUtils.addArtifactIdProposal(editorPage.getProject(), exclusionGroupIdText, exclusionArtifactIdText, Packaging.ALL);
    
    exclusionSelectAction = new Action(Messages.DependenciesComposite_action_selectExclusion, MavenEditorImages.SELECT_ARTIFACT) {
      public void run() {
        // XXX calculate list available for exclusion
        Set<ArtifactKey> artifacts = Collections.emptySet();
        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
            Messages.DependenciesComposite_searchDialog_selectExclusion, IIndex.SEARCH_ARTIFACT, artifacts);
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            exclusionGroupIdText.setText(nvl(af.group));
            exclusionArtifactIdText.setText(nvl(af.artifact));
          }
        }
      }
    };
    exclusionSelectAction.setEnabled(false);

    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
    toolBarManager.add(exclusionSelectAction);
    
    Composite toolbarComposite = toolkit.createComposite(exclusionDetailsSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);
 
    toolBarManager.createControl(toolbarComposite);
    exclusionDetailsSection.setTextClient(toolbarComposite);
  }

  protected void updateDependencyDetails(Dependency dependency) {
    if(changingSelection) {
      return;
    }
//    if(dependency != null && dependency == currentDependency) {
//      return;
//    }
    
    this.currentDependency = dependency;
    
    openWebPageAction.setEnabled(dependency!=null);
    dependencySelectAction.setEnabled(dependency!=null && !editorPage.isReadOnly());
//    exclusionAddAction.setEnabled(dependency!=null && !editorPage.isReadOnly());
    
    if(editorPage != null) {
      editorPage.removeNotifyListener(groupIdText);
      editorPage.removeNotifyListener(artifactIdText);
      editorPage.removeNotifyListener(versionText);
      editorPage.removeNotifyListener(classifierText);
      editorPage.removeNotifyListener(typeCombo);
      editorPage.removeNotifyListener(scopeCombo);
      editorPage.removeNotifyListener(systemPathText);
      editorPage.removeNotifyListener(optionalButton);
    }

    if(editorPage == null || dependency == null) {
      FormUtils.setEnabled(dependencyDetailsComposite, false);

      setText(groupIdText, ""); //$NON-NLS-1$
      setText(artifactIdText, ""); //$NON-NLS-1$
      setText(versionText, ""); //$NON-NLS-1$
      setText(classifierText, ""); //$NON-NLS-1$
      setText(typeCombo, ""); //$NON-NLS-1$
      setText(scopeCombo, ""); //$NON-NLS-1$
      setText(systemPathText, ""); //$NON-NLS-1$

      setButton(optionalButton, false);

      exclusionsEditor.setSelection(Collections.<Exclusion>emptyList());
      exclusionsEditor.setInput(null);
      
      updateExclusionDetails(null);
      
      return;
    }

    FormUtils.setEnabled(dependencyDetailsComposite, true);
    FormUtils.setReadonly(dependencyDetailsComposite, editorPage.isReadOnly());

    setText(groupIdText, dependency.getGroupId());
    setText(artifactIdText, dependency.getArtifactId());
    setText(versionText, dependency.getVersion());
    setText(classifierText, dependency.getClassifier());
    setText(typeCombo, "".equals(nvl(dependency.getType())) ? "jar" : dependency.getType());
    setText(scopeCombo, "".equals(nvl(dependency.getScope())) ? "compile" : dependency.getScope());
    setText(systemPathText, dependency.getSystemPath());

    if(optionalButton.getSelection()!="true".equals(dependency.getOptional())) {
      optionalButton.setSelection("true".equals(dependency.getOptional()));
    }

    exclusionsEditor.setInput(dependency.getExclusions());
    exclusionsEditor.setSelection(Collections.<Exclusion>emptyList());
    
    // set new listeners
    ValueProvider<Dependency> dependencyProvider = new ValueProvider.DefaultValueProvider<Dependency>(dependency); 
    editorPage.setModifyListener(groupIdText, dependencyProvider, POM_PACKAGE.getDependency_GroupId(), ""); //$NON-NLS-1$
    editorPage.setModifyListener(artifactIdText, dependencyProvider, POM_PACKAGE.getDependency_ArtifactId(), ""); //$NON-NLS-1$
    editorPage.setModifyListener(versionText, dependencyProvider, POM_PACKAGE.getDependency_Version(), ""); //$NON-NLS-1$
    editorPage.setModifyListener(classifierText, dependencyProvider, POM_PACKAGE.getDependency_Classifier(), ""); //$NON-NLS-1$
    editorPage.setModifyListener(typeCombo, dependencyProvider, POM_PACKAGE.getDependency_Type(), "jar"); //$NON-NLS-1$
    editorPage.setModifyListener(scopeCombo, dependencyProvider, POM_PACKAGE.getDependency_Scope(), "compile"); //$NON-NLS-1$
    editorPage.setModifyListener(systemPathText, dependencyProvider, POM_PACKAGE.getDependency_SystemPath(), ""); //$NON-NLS-1$
    editorPage.setModifyListener(optionalButton, dependencyProvider, POM_PACKAGE.getDependency_Optional(), "false");
    
    editorPage.registerListeners();
    
    updateExclusionDetails(null);
  }

  void updateExclusionDetails(Exclusion exclusion) {
//    if(exclusion!=null && exclusion==currentExclusion) {
//      return;
//    }
    
    currentExclusion = exclusion;
    
    if(editorPage != null) {
      editorPage.removeNotifyListener(exclusionGroupIdText);
      editorPage.removeNotifyListener(exclusionArtifactIdText);
    }
    
    if(editorPage == null || exclusion == null) {
      FormUtils.setEnabled(exclusionDetailsSection, false);
      exclusionSelectAction.setEnabled(false);

      exclusionGroupIdText.setText(""); //$NON-NLS-1$
      exclusionArtifactIdText.setText(""); //$NON-NLS-1$
      return;
    }
    
    FormUtils.setEnabled(exclusionDetailsSection, true);
    FormUtils.setReadonly(exclusionDetailsSection, editorPage.isReadOnly());
    exclusionSelectAction.setEnabled(!editorPage.isReadOnly());
    
    setText(exclusionGroupIdText, exclusion.getGroupId());
    setText(exclusionArtifactIdText, exclusion.getArtifactId());
    
    ValueProvider<Exclusion> exclusionProvider = new ValueProvider.DefaultValueProvider<Exclusion>(exclusion);
    editorPage.setModifyListener(exclusionGroupIdText, exclusionProvider, POM_PACKAGE.getExclusion_GroupId(), ""); //$NON-NLS-1$
    editorPage.setModifyListener(exclusionArtifactIdText, exclusionProvider, POM_PACKAGE.getExclusion_ArtifactId(), ""); //$NON-NLS-1$
    
    editorPage.registerListeners();
  }

  public void loadData(Model model, ValueProvider<DependencyManagement> dependencyManagementProvider) {
    this.model = model;
    this.dependencyManagementProvider = dependencyManagementProvider;
    this.dependencyLabelProvider.setPomEditor(editorPage.getPomEditor());
    this.dependencyManagementLabelProvider.setPomEditor(editorPage.getPomEditor());
    this.exclusionLabelProvider.setPomEditor(editorPage.getPomEditor());
    
    changingSelection = true;
    dependenciesEditor.setInput(model.getDependencies());
    
    DependencyManagement dependencyManagement = dependencyManagementProvider.getValue();
    dependencyManagementEditor.setInput(dependencyManagement == null ? null : dependencyManagement.getDependencies());
    
    changingSelection = false;
    
    dependenciesEditor.setReadOnly(editorPage.isReadOnly());
    dependencyManagementEditor.setReadOnly(editorPage.isReadOnly());
    
    //dependencyAddAction.setEnabled(!editorPage.isReadOnly());
    //dependencyManagementAddAction.setEnabled(!editorPage.isReadOnly());

    if(searchControl!=null) {
      searchControl.getSearchText().setEditable(true);
    }
    
    updateDependencyDetails(null);
    
    // exclusionsEditor.setReadOnly(parent.isReadOnly());
  }

  public void updateView(final MavenPomEditorPage editorPage, final Notification notification) {
    Display.getDefault().asyncExec(new Runnable(){
      public void run(){
        EObject object = (EObject) notification.getNotifier();

        // XXX event is not received when <dependencies> is deleted in XML
        if(object instanceof Model) {
          dependenciesEditor.refresh();
          dependencyManagementEditor.refresh();
        }
        
        if(object instanceof DependencyManagement) {
          dependencyManagementEditor.refresh();
        }
        
        Object notificationObject = MavenPomEditorPage.getFromNotification(notification);
        if(object instanceof Dependency) {
          dependenciesEditor.refresh();
          dependencyManagementEditor.refresh();
          
          if(object == currentDependency && (notificationObject == null || notificationObject instanceof Dependency)) {
            updateDependencyDetails((Dependency) notificationObject);
          }
        }
        
        EList<Exclusion> exclusions = currentDependency==null ? null : currentDependency.getExclusions();
        if(object instanceof Dependency) {
          exclusionsEditor.refresh();
          if(exclusions == object) {
            updateDependencyDetails(currentDependency);
          }
        }
        
        if(object instanceof Exclusion) {
          exclusionsEditor.refresh();
          if(currentExclusion == object && (notificationObject == null || notificationObject instanceof Exclusion)) {
            updateExclusionDetails((Exclusion) notificationObject);
          }
        }        
      }
    });
  }

  Dependency createDependency(ValueProvider<? extends EObject> parentProvider, EReference feature,
      String groupId, String artifactId, String version, String classifier, String type, String scope) {
    CompoundCommand compoundCommand = new CompoundCommand();
    EditingDomain editingDomain = editorPage.getEditingDomain();
    
    EObject parent = parentProvider.getValue();
    if(parent == null) {
      parent = parentProvider.create(editingDomain, compoundCommand);
    }
    
    Dependency dependency = PomFactory.eINSTANCE.createDependency();
    dependency.setGroupId(groupId);
    dependency.setArtifactId(artifactId);
    dependency.setVersion(version);
    dependency.setClassifier(classifier);
    dependency.setType(type);
    dependency.setScope(scope);
    
    Command addDependencyCommand = AddCommand.create(editingDomain, parent, 
        feature, dependency);
    compoundCommand.append(addDependencyCommand);
    
    editingDomain.getCommandStack().execute(compoundCommand);
    
    return dependency;
  }

  public void setSearchControl(SearchControl searchControl) {
    if(this.searchControl!=null) {
      return;
    }
    
    this.searchMatcher = new SearchMatcher(searchControl);
    this.searchFilter = new DependencyFilter(searchMatcher);
    this.searchControl = searchControl;
    this.searchControl.getSearchText().addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        changingSelection = true;
        selectDepenendencies(dependenciesEditor, model, POM_PACKAGE.getModel_Dependencies());
        selectDepenendencies(dependencyManagementEditor, dependencyManagementProvider.getValue(), POM_PACKAGE.getDependencyManagement_Dependencies());
        changingSelection = false;
        
        updateDependencyDetails(null);
      }

      private void selectDepenendencies(ListEditorComposite<Dependency> editor,
          EObject parent, EStructuralFeature feature) {
        if(parent != null) {
          editor.setSelection((EList<Dependency>)parent.eGet(feature));
          editor.refresh();
        }
      }
    });
  }
  
  void createExclusion(String groupId, String artifactId) {
    CompoundCommand compoundCommand = new CompoundCommand();
    EditingDomain editingDomain = editorPage.getEditingDomain();
    
    Exclusion exclusion = PomFactory.eINSTANCE.createExclusion();
    exclusion.setGroupId(groupId);
    exclusion.setArtifactId(artifactId);
    
    Command addCommand = AddCommand.create(editingDomain, currentDependency, 
        POM_PACKAGE.getDependency_Exclusions(), exclusion);
    compoundCommand.append(addCommand);
    
    editingDomain.getCommandStack().execute(compoundCommand);

    exclusionsEditor.setSelection(Collections.singletonList(exclusion));
    updateExclusionDetails(exclusion);
    exclusionGroupIdText.setFocus();
  }

  String getVersion(String groupId, String artifactId, IProgressMonitor monitor) {
    try {
      MavenProject mavenProject = editorPage.getPomEditor().readMavenProject(false, monitor);
      Artifact a = mavenProject.getArtifactMap().get(groupId + ":" + artifactId); //$NON-NLS-1$
      if(a!=null) {
        return a.getBaseVersion();
      }
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }
    return null;
  }

  public static class DependencyFilter extends ViewerFilter {
    private SearchMatcher searchMatcher;

    public DependencyFilter(SearchMatcher searchMatcher) {
      this.searchMatcher = searchMatcher;
    }

    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if(element instanceof Dependency) {
        Dependency d = (Dependency) element;
        return searchMatcher.isMatchingArtifact(d.getGroupId(), d.getArtifactId());
      }
      return false;
    }
  }

  
  

//  private List<Dependency> getUpdatedSelection(Dependencies dependencies, List<Dependency> selection) {
//    List<Dependency> newSelection = new ArrayList<Dependency>();
//    for(Dependency dependency : selection) {
//      if (dependencies.getDependency().contains(dependency)) {
//        newSelection.add(dependency);
//      }
//    }
//    return newSelection;
//  }

//  private List<Exclusion> getUpdatedSelection(ExclusionsType exclusions, List<Exclusion> selection) {
//    List<Exclusion> newSelection = new ArrayList<Exclusion>();
//    for(Exclusion exclusion : selection) {
//      if (exclusions.getExclusion().contains(exclusion)) {
//        newSelection.add(exclusion);
//      }
//    }
//    return newSelection;
//  }

}

