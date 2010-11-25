/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.composites;

import static org.eclipse.m2e.editor.pom.FormUtils.nvl;
import static org.eclipse.m2e.editor.pom.FormUtils.setText;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionRequest;
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
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.actions.OpenUrlAction;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.ui.dialogs.AddDependencyDialog;
import org.eclipse.m2e.core.ui.dialogs.MavenRepositorySearchDialog;
import org.eclipse.m2e.core.util.M2EUtils;
import org.eclipse.m2e.core.util.ProposalUtil;
import org.eclipse.m2e.core.util.search.Packaging;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.MavenEditorPlugin;
import org.eclipse.m2e.editor.dialogs.ManageDependenciesDialog;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.editor.pom.FormUtils;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.editor.pom.MavenPomEditor.Callback;
import org.eclipse.m2e.editor.pom.MavenPomEditorPage;
import org.eclipse.m2e.editor.pom.SearchControl;
import org.eclipse.m2e.editor.pom.SearchMatcher;
import org.eclipse.m2e.editor.pom.ValueProvider;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.DependencyManagement;
import org.eclipse.m2e.model.edit.pom.Exclusion;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.sonatype.aether.graph.DependencyNode;


/**
 * @author Eugene Kuleshov
 */
public class DependenciesComposite extends Composite {

  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;
  
  protected MavenPomEditorPage editorPage; 
  MavenPomEditor pomEditor;
  
  private FormToolkit toolkit = new FormToolkit(Display.getCurrent());

  // controls
  
  ListEditorComposite<Dependency> dependencyManagementEditor;
  ListEditorComposite<Dependency> dependenciesEditor;

  Composite rightSideComposite;
  
  protected DependencyDetailComposite dependencyDetailComposite;

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
  MavenProject mavenProject;
  ValueProvider<DependencyManagement> dependencyManagementProvider;

  DependencyLabelProvider dependencyLabelProvider = new DependencyLabelProvider();
  DependencyLabelProvider dependencyManagementLabelProvider = new DependencyLabelProvider();
  DependencyLabelProvider exclusionLabelProvider = new DependencyLabelProvider();



  public DependenciesComposite(Composite composite, MavenPomEditorPage editorPage, int flags, MavenPomEditor pomEditor) {
    super(composite, flags);
    this.editorPage = editorPage;
    this.pomEditor = pomEditor;
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

    rightSideComposite = new Composite(horizontalSash, SWT.NONE);
    GridLayout dependencyDetailsLayout = new GridLayout(1, false);
    dependencyDetailsLayout.marginWidth = 0;
    dependencyDetailsLayout.marginHeight = 0;
    rightSideComposite.setLayout(dependencyDetailsLayout);
    toolkit.adapt(rightSideComposite, true, true);
    
    createDependencyDetails(toolkit, rightSideComposite);
    createExclusionsSection(toolkit, rightSideComposite);
    createExclusionDetailsSection(toolkit, rightSideComposite);
    
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
        dependencyDetailComposite.setFocus();
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
        openManageDependenciesDialog();
        
        final AddDependencyDialog addDepDialog = new AddDependencyDialog(getShell(), false, editorPage.getProject());
        
        /*
         * Load the dependency tree for the dialog so it can show already
         * added transitive dependencies.
         */
        Runnable runnable = new Runnable() {
          
          public void run() {
            pomEditor.loadDependencies(new Callback() {
              
              public void onFinish(DependencyNode node) {
                addDepDialog.setDepdencyNode(node);
              }
              
              public void onException(CoreException ex) {
                MavenLogger.log(ex);
              }
            }, Artifact.SCOPE_TEST);
          }
        };
        
        addDepDialog.onLoad(runnable);
        
        if (addDepDialog.open() == Window.OK) {
          List<Dependency> deps = addDepDialog.getDependencies();
          for (Dependency dep : deps) {
            setupDependency(new ValueProvider<Model>() {
              @Override
              public Model getValue() {
                return model;
              }
            }, POM_PACKAGE.getModel_Dependencies(), dep);
          }
          dependenciesEditor.setInput(model.getDependencies());
          dependenciesEditor.setSelection(Collections.singletonList(deps.get(0)));
          updateDependencyDetails(deps.get(0));
          dependencyDetailComposite.setFocus();
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
      {
        setChecked(true);
      }
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
        dependencyDetailComposite.setFocus();
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
            dependencyDetailComposite.setFocus();
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
      {
        setChecked(true);
      }
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

  private void createDependencyDetails(FormToolkit toolkit, Composite parent) {
    Section dependencyDetailsSection = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
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
            dependencyDetailComposite.setGroupId(af.group);
            dependencyDetailComposite.setArtifactId(af.artifact);
            dependencyDetailComposite.setVersion(af.version);
            //MNGECLIPSE-2363
            String type = nvl(af.type);
            if ("jar".equals(type)) {//$NON-NLS-1$
              type = ""; //$NON-NLS-1$
            }
            dependencyDetailComposite.setType(type);
            String scope = nvl(dialog.getSelectedScope());
            if ("compile".equals(scope)) {//$NON-NLS-1$
              scope = "";//$NON-NLS-1$
            }
            dependencyDetailComposite.setScope(scope);
          }
        }
      }
    };
    dependencySelectAction.setEnabled(false);

    openWebPageAction = new Action(Messages.DependenciesComposite_action_open_project_page, MavenEditorImages.WEB_PAGE) {
      public void run() {
        final String groupId = dependencyDetailComposite.getGroupId();
        final String artifactId = dependencyDetailComposite.getArtifactId();
        final String version = dependencyDetailComposite.getVersion();
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
    
    dependencyDetailComposite = new DependencyDetailComposite(dependencyDetailsSection, editorPage);
    dependencyDetailsSection.setClient(dependencyDetailComposite);
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
//    exclusionDetailsComposite.addControlListener(detailsWidthGroup);
    
    Label exclusionGroupIdLabel = toolkit.createLabel(exclusionDetailsComposite, Messages.DependenciesComposite_lblExclusionGroupId, SWT.NONE);
    exclusionGroupIdLabel.setLayoutData(new GridData());
//    detailsWidthGroup.addControl(exclusionGroupIdLabel);

    exclusionGroupIdText = toolkit.createText(exclusionDetailsComposite, null, SWT.NONE);
    GridData gd_exclusionGroupIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_exclusionGroupIdText.horizontalIndent = 4;
    exclusionGroupIdText.setLayoutData(gd_exclusionGroupIdText);
    // TODO handle ArtifactInfo
    ProposalUtil.addGroupIdProposal(editorPage.getProject(), exclusionGroupIdText, Packaging.ALL);
    M2EUtils.addRequiredDecoration(exclusionGroupIdText);

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
//    detailsWidthGroup.addControl(exclusionArtifactIdLabel);

    exclusionArtifactIdText = toolkit.createText(exclusionDetailsComposite, null, SWT.NONE);
    GridData gd_exclusionArtifactIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_exclusionArtifactIdText.horizontalIndent = 4;
    exclusionArtifactIdText.setLayoutData(gd_exclusionArtifactIdText);
    // TODO handle ArtifactInfo
    ProposalUtil.addArtifactIdProposal(editorPage.getProject(), exclusionGroupIdText, exclusionArtifactIdText, Packaging.ALL);
    M2EUtils.addRequiredDecoration(exclusionArtifactIdText);
    
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
    
    this.currentDependency = dependency;
    
    dependencyDetailComposite.update(dependency);
    
    openWebPageAction.setEnabled(dependency!=null);
    dependencySelectAction.setEnabled(dependency!=null && !editorPage.isReadOnly());

    if(editorPage == null || dependency == null) {
      FormUtils.setEnabled(rightSideComposite, false);

      exclusionsEditor.setSelection(Collections.<Exclusion>emptyList());
      exclusionsEditor.setInput(null);
      
      updateExclusionDetails(null);
      
      return;
    }

    FormUtils.setEnabled(rightSideComposite, true);
    FormUtils.setReadonly(rightSideComposite, editorPage.isReadOnly());

    exclusionsEditor.setInput(dependency.getExclusions());
    exclusionsEditor.setSelection(Collections.<Exclusion>emptyList());
    
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
          Model model2 = (Model) object;
          
          if (model2.getDependencyManagement() != null && dependencyManagementEditor.getInput() == null) {
            dependencyManagementEditor.setInput(model2.getDependencyManagement().getDependencies());
          } else if (model2.getDependencyManagement() == null) {
            dependencyManagementEditor.setInput(null);
          }
          
          if (model2.getDependencies() != null && dependenciesEditor.getInput() == null) {
            dependenciesEditor.setInput(model2.getDependencies());
          } else if (model2.getDependencies() == null) {
            dependenciesEditor.setInput(null);
          }
          
          dependenciesEditor.refresh();
          dependencyManagementEditor.refresh();
        }
        
        if(object instanceof DependencyManagement) {
          if (dependenciesEditor.getInput() == null) {
            dependenciesEditor.setInput(((DependencyManagement) object).getDependencies());
          }
          dependencyManagementEditor.refresh();
        }
        
        Object notificationObject = MavenPomEditorPage.getFromNotification(notification);
        if(object instanceof Dependency) {
          Dependency dependency = (Dependency) object;

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
  
  void setupDependency(ValueProvider<? extends EObject> parentProvider, 
      EReference feature, Dependency dependency) {
    CompoundCommand compoundCommand = new CompoundCommand();
    EditingDomain editingDomain = editorPage.getEditingDomain();
    
    EObject parent = parentProvider.getValue();
    if(parent == null) {
      parent = parentProvider.create(editingDomain, compoundCommand);
    }
    
    Command addDependencyCommand = AddCommand.create(editingDomain, parent, 
        feature, dependency);
    compoundCommand.append(addDependencyCommand);
    
    editingDomain.getCommandStack().execute(compoundCommand);
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
    //we add filter here as the default behaviour is to filter..
    TableViewer viewer = dependenciesEditor.getViewer();
    viewer.addFilter(searchFilter);
    viewer = dependencyManagementEditor.getViewer();
    viewer.addFilter(searchFilter);
    
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

  private void openManageDependenciesDialog() {
    /*
     * A linked list representing the path from child to root parent pom.
     * The head is the child, the tail is the root pom
     */
    final LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    
    IRunnableWithProgress projectLoader = new IRunnableWithProgress() {
      
      public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        try {
          IMaven maven = MavenPlugin.getDefault().getMaven();
          MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
          mavenProject = pomEditor.readMavenProject(false, monitor);
          maven.detachFromSession(mavenProject);
          
          hierarchy.addFirst(mavenProject);
          
          MavenProject project = mavenProject;
          while (project.getModel().getParent() != null) {
            IMavenProjectFacade projectFacade = projectManager.create(pomEditor.getPomFile(), true, monitor);
            MavenExecutionRequest request = projectManager.createExecutionRequest(projectFacade, monitor);
            project = maven.resolveParentProject(request, project, monitor);
            hierarchy.add(project);
          }
        } catch(CoreException e) {
          MavenEditorPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, "Unable to resolve POMs: " + e));
        }
      }
    };
    
    try {
      PlatformUI.getWorkbench().getProgressService().run(true, true, projectLoader);
    } catch(InvocationTargetException e1) {
      MavenEditorPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, "Unable to resolve POMs: " + e1));
      return;
    } catch(InterruptedException e1) {
      MavenEditorPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, "Unable to resolve POMs: " + e1));
      return;
    }
    
    
    final ManageDependenciesDialog manageDepDialog = new ManageDependenciesDialog(getShell(), model, hierarchy, pomEditor.getEditingDomain());
    manageDepDialog.open();
  }
}

