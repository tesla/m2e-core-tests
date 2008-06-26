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
import static org.maven.ide.eclipse.editor.pom.FormUtils.setButton;
import static org.maven.ide.eclipse.editor.pom.FormUtils.setText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
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
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.maven.ide.components.pom.Dependencies;
import org.maven.ide.components.pom.Dependency;
import org.maven.ide.components.pom.ExecutionsType;
import org.maven.ide.components.pom.Plugin;
import org.maven.ide.components.pom.PluginExecution;
import org.maven.ide.components.pom.PluginManagement;
import org.maven.ide.components.pom.Plugins;
import org.maven.ide.components.pom.PomFactory;
import org.maven.ide.components.pom.PomPackage;
import org.maven.ide.components.pom.StringGoals;
import org.maven.ide.eclipse.actions.MavenRepositorySearchDialog;
import org.maven.ide.eclipse.actions.OpenPomAction;
import org.maven.ide.eclipse.actions.OpenUrlAction;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.editor.pom.FormUtils;
import org.maven.ide.eclipse.editor.pom.MavenPomEditorPage;
import org.maven.ide.eclipse.editor.pom.SearchControl;
import org.maven.ide.eclipse.editor.pom.SearchMatcher;
import org.maven.ide.eclipse.editor.pom.ValueProvider;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifactFile;

/**
 * @author Eugene Kuleshov
 */
public class PluginsComposite extends Composite {

  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;
  
  private MavenPomEditorPage parent;
  
  // controls
  CCombo executionPhaseCombo;
  Text executionIdText;
  Hyperlink pluginExecutionConfigurationHyperlink;
  Button executionInheritedButton;
  FormToolkit toolkit = new FormToolkit(Display.getCurrent());
  
  ListEditorComposite<Plugin> pluginsEditor;
  ListEditorComposite<Plugin> pluginManagementEditor;
  
  Text groupIdText;
  Text artifactIdText;
  Text versionText;
  Section pluginDetailsSection;
  Button pluginExtensionsButton;
  Button pluginInheritedButton;

  Hyperlink pluginConfigurationHyperlink;
  ListEditorComposite<Dependency> pluginDependenciesEditor;
  ListEditorComposite<String> goalsEditor;
  ListEditorComposite<PluginExecution> pluginExecutionsEditor;

  Section pluginExecutionSection;
  Section pluginExecutionsSection;
  Section pluginDependenciesSection;

  Button pluginSelectButton;

  Action pluginSelectAction;
  
  Action pluginAddAction;
  
  Action pluginManagementAddAction;
  
  Action openWebPageAction;

  ViewerFilter searchFilter;
  
  SearchControl searchControl;
  
  SearchMatcher searchMatcher;
  
  // model
  
  Plugin currentPlugin;
  PluginExecution currentPluginExecution;

  ValueProvider<Plugins> pluginsProvider;

  ValueProvider<Plugins> pluginManagementProvider;

  boolean changingSelection = false;

  
  public PluginsComposite(Composite composite, int style) {
    super(composite, style);
    
    GridLayout gridLayout = new GridLayout(1, false);
    gridLayout.marginWidth = 0;
    setLayout(gridLayout);
    toolkit.adapt(this);
  
    SashForm horizontalSashForm = new SashForm(this, SWT.NONE);
    GridData gd_horizontalSashForm = new GridData(SWT.FILL, SWT.FILL, true, true);
    horizontalSashForm.setLayoutData(gd_horizontalSashForm);
    toolkit.adapt(horizontalSashForm, true, true);

    SashForm verticalSashForm = new SashForm(horizontalSashForm, SWT.VERTICAL);
    toolkit.adapt(verticalSashForm, true, true);
  
    createPluginsSection(verticalSashForm);
    createPluginManagementSection(verticalSashForm);
    
    verticalSashForm.setWeights(new int[] {1, 1});

    createPluginDetailsSection(horizontalSashForm);
    horizontalSashForm.setWeights(new int[] {1, 1 });

    updatePluginDetails(null);
  }

  private void createPluginsSection(SashForm verticalSashForm) {
    Section pluginsSection = toolkit.createSection(verticalSashForm, Section.TITLE_BAR | Section.COMPACT);
    pluginsSection.setText("Plugins");
  
    pluginsEditor = new ListEditorComposite<Plugin>(pluginsSection, SWT.NONE);
    pluginsSection.setClient(pluginsEditor);
    toolkit.adapt(pluginsEditor);
    toolkit.paintBordersFor(pluginsEditor);
    
    final PluginLabelProvider labelProvider = new PluginLabelProvider();
    pluginsEditor.setLabelProvider(labelProvider);
    pluginsEditor.setContentProvider(new ListEditorContentProvider<Plugin>());
  
    pluginsEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Plugin> selection = pluginsEditor.getSelection();
        updatePluginDetails(selection.size()==1 ? selection.get(0) : null);
        
        if(!selection.isEmpty()) {
          changingSelection = true;
          pluginManagementEditor.setSelection(Collections.<Plugin>emptyList());
          changingSelection = false;
        }
      }
    });
    
    pluginsEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        createPlugin(pluginsEditor, pluginsProvider, null, null, null);
      }
    });
    
    pluginsEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
  
        List<Plugin> list = pluginsEditor.getSelection();
        for(Plugin plugin : list) {
          Command removeCommand = RemoveCommand.create(editingDomain, pluginsProvider.getValue(), //
              POM_PACKAGE.getPlugins_Plugin(), plugin);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
        updatePluginDetails(null);
      }
    });
    
    pluginAddAction = new Action("Add Plugin", MavenEditorImages.ADD_PLUGIN) {
      public void run() {
        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
            "Add Plugin", IndexManager.SEARCH_PLUGIN, Collections.<Artifact>emptySet());
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            createPlugin(pluginsEditor, pluginsProvider, af.group, af.artifact, af.version);
          }
        }
      }
    };
    pluginAddAction.setEnabled(false);

    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
    toolBarManager.add(pluginAddAction);
    toolBarManager.add(new Separator());
    
    toolBarManager.add(new Action("Show GroupId", MavenEditorImages.SHOW_GROUP) {
      {
        setChecked(true);
      }
      public int getStyle() {
        return AS_CHECK_BOX;
      }
      public void run() {
        labelProvider.setShowGroupId(isChecked());
        pluginsEditor.getViewer().refresh();
      }
    });
    
    toolBarManager.add(new Action("Filter", MavenEditorImages.FILTER) {
      public int getStyle() {
        return AS_CHECK_BOX;
      }
      public void run() {
        TableViewer viewer = pluginsEditor.getViewer();
        viewer.setFilters(isChecked() ? new ViewerFilter[] {searchFilter} : new ViewerFilter[0]);
        viewer.refresh();
      }
    });
    
    Composite toolbarComposite = toolkit.createComposite(pluginsSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);
 
    toolBarManager.createControl(toolbarComposite);
    pluginsSection.setTextClient(toolbarComposite);
  }

  private void createPluginManagementSection(SashForm verticalSashForm) {
    Section pluginManagementSection = toolkit.createSection(verticalSashForm, Section.TITLE_BAR);
    pluginManagementSection.setText("Plugin Management");
  
    pluginManagementEditor = new ListEditorComposite<Plugin>(pluginManagementSection, SWT.NONE);
    pluginManagementSection.setClient(pluginManagementEditor);
    toolkit.adapt(pluginManagementEditor);
    toolkit.paintBordersFor(pluginManagementEditor);

    final PluginLabelProvider labelProvider = new PluginLabelProvider();
    pluginManagementEditor.setLabelProvider(labelProvider);
    pluginManagementEditor.setContentProvider(new ListEditorContentProvider<Plugin>());
  
    pluginManagementEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Plugin> selection = pluginManagementEditor.getSelection();
        updatePluginDetails(selection.size()==1 ? selection.get(0) : null);
        
        if(!selection.isEmpty()) {
          changingSelection = true;
          pluginsEditor.setSelection(Collections.<Plugin>emptyList());
          changingSelection = false;
        }
      }
    });
    
    pluginManagementEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        createPlugin(pluginManagementEditor, pluginManagementProvider, null, null, null);
      }
    });
    
    pluginManagementEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
  
        List<Plugin> list = pluginManagementEditor.getSelection();
        for(Plugin plugin : list) {
          Command removeCommand = RemoveCommand.create(editingDomain, //
              pluginManagementProvider.getValue(), POM_PACKAGE.getPlugins_Plugin(), plugin);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
        updatePluginDetails(null);
      }
    });
    
    pluginManagementAddAction = new Action("Add Plugin", MavenEditorImages.ADD_PLUGIN) {
      public void run() {
        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
            "Add Plugin", IndexManager.SEARCH_PLUGIN, Collections.<Artifact>emptySet());
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            createPlugin(pluginManagementEditor, pluginManagementProvider, af.group, af.artifact, af.version);
          }
        }
      }
    };
    pluginManagementAddAction.setEnabled(false);

    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
    toolBarManager.add(pluginManagementAddAction);
    toolBarManager.add(new Separator());
    
    toolBarManager.add(new Action("Show GroupId", MavenEditorImages.SHOW_GROUP) {
      {
        setChecked(true);
      }
      public int getStyle() {
        return AS_CHECK_BOX;
      }
      public void run() {
        labelProvider.setShowGroupId(isChecked());
        pluginManagementEditor.getViewer().refresh();
      }
    });
    
    toolBarManager.add(new Action("Filter", MavenEditorImages.FILTER) {
      public int getStyle() {
        return AS_CHECK_BOX;
      }
      public void run() {
        TableViewer viewer = pluginManagementEditor.getViewer();
        viewer.setFilters(isChecked() ? new ViewerFilter[] {searchFilter} : new ViewerFilter[0]);
        viewer.refresh();
      }
    });
    
    Composite toolbarComposite = toolkit.createComposite(pluginManagementSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);
 
    toolBarManager.createControl(toolbarComposite);
    pluginManagementSection.setTextClient(toolbarComposite);
  }

  private void createPluginDetailsSection(Composite horizontalSashForm) {
      Composite detailsComposite = toolkit.createComposite(horizontalSashForm, SWT.NONE);
      GridLayout detailsCompositeLayout = new GridLayout();
      detailsCompositeLayout.marginWidth = 0;
      detailsCompositeLayout.marginHeight = 0;
      detailsComposite.setLayout(detailsCompositeLayout);
      toolkit.paintBordersFor(detailsComposite);
      
      pluginDetailsSection = toolkit.createSection(detailsComposite, Section.TITLE_BAR);
      pluginDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
      pluginDetailsSection.setText("Plugin Details");
    
      Composite pluginDetailsComposite = toolkit.createComposite(pluginDetailsSection, SWT.NONE);
      GridLayout pluginDetailsLayout = new GridLayout(3, false);
      pluginDetailsLayout.marginWidth = 1;
      pluginDetailsLayout.marginHeight = 2;
      pluginDetailsComposite.setLayout(pluginDetailsLayout);
      toolkit.paintBordersFor(pluginDetailsComposite);
      pluginDetailsSection.setClient(pluginDetailsComposite);
    
      toolkit.createLabel(pluginDetailsComposite, "Group Id:*", SWT.NONE);
    
      groupIdText = toolkit.createText(pluginDetailsComposite, null, SWT.NONE);
      groupIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    
      Hyperlink artifactIdHyperlink = toolkit.createHyperlink(pluginDetailsComposite, "Artifact Id:*", SWT.NONE);
      artifactIdHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
        public void linkActivated(HyperlinkEvent e) {
          final String groupId = groupIdText.getText();
          final String artifactId = artifactIdText.getText();
          final String version = versionText.getText();
          new Job("Opening " + groupId + ":" + artifactId + ":" + version) {
            protected IStatus run(IProgressMonitor arg0) {
              OpenPomAction.openEditor(groupId, artifactId, version);
              return Status.OK_STATUS;
            }
          }.schedule();
        }
      });
    
      artifactIdText = toolkit.createText(pluginDetailsComposite, null, SWT.NONE);
      artifactIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    
      Label label = toolkit.createLabel(pluginDetailsComposite, "Version:", SWT.NONE);
      label.setLayoutData(new GridData());
    
      versionText = toolkit.createText(pluginDetailsComposite, null, SWT.NONE);
      GridData versionTextData = new GridData(SWT.LEFT, SWT.CENTER, true, false);
      versionTextData.widthHint = 200;
      versionText.setLayoutData(versionTextData);
  
  //    pluginSelectButton = toolkit.createButton(pluginDetailsComposite, "Select...", SWT.NONE);
  //    pluginSelectButton.addSelectionListener(new SelectionAdapter() {
  //      public void widgetSelected(SelectionEvent e) {
  //        Set<Dependency> artifacts = Collections.emptySet();
  //        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(parent.getEditorSite().getShell(),
  //            "Add Plugin", IndexManager.SEARCH_PLUGIN, artifacts);
  //        if(dialog.open() == Window.OK) {
  //          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
  //          if(af != null) {
  //            groupIdText.setText(nvl(af.group));
  //            artifactIdText.setText(nvl(af.artifact));
  //            versionText.setText(nvl(af.version));
  //          }
  //        }
  //      }
  //    });
      
      pluginSelectAction = new Action("Select Plugin", MavenEditorImages.SELECT_PLUGIN) {
        public void run() {
          MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
              "Select Plugin", IndexManager.SEARCH_PLUGIN, Collections.<Artifact>emptySet());
          if(dialog.open() == Window.OK) {
            IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
            if(af != null) {
              groupIdText.setText(nvl(af.group));
              artifactIdText.setText(nvl(af.artifact));
              versionText.setText(nvl(af.version));
            }
          }
        }
      };
      pluginSelectAction.setEnabled(false);
  
      openWebPageAction = new Action("Open Web Page", MavenEditorImages.WEB_PAGE) {
        public void run() {
          final String groupId = groupIdText.getText();
          final String artifactId = artifactIdText.getText();
          final String version = versionText.getText();
          new Job("Opening " + groupId + ":" + artifactId + ":" + version) {
            protected IStatus run(IProgressMonitor arg0) {
              OpenUrlAction.openBrowser(OpenUrlAction.ID_PROJECT, groupId, artifactId, version);
              return Status.OK_STATUS;
            }
          }.schedule();
          
        }      
      };
      openWebPageAction.setEnabled(false);
      
      ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
      toolBarManager.add(pluginSelectAction);
      toolBarManager.add(new Separator());
      toolBarManager.add(openWebPageAction);
      
      Composite toolbarComposite = toolkit.createComposite(pluginDetailsSection);
      GridLayout toolbarLayout = new GridLayout(1, true);
      toolbarLayout.marginHeight = 0;
      toolbarLayout.marginWidth = 0;
      toolbarComposite.setLayout(toolbarLayout);
      toolbarComposite.setBackground(null);
   
      toolBarManager.createControl(toolbarComposite);
      pluginDetailsSection.setTextClient(toolbarComposite);
    
      Composite composite = new Composite(pluginDetailsComposite, SWT.NONE);
      GridLayout compositeLayout = new GridLayout();
      compositeLayout.marginWidth = 0;
      compositeLayout.marginHeight = 0;
      compositeLayout.numColumns = 3;
      composite.setLayout(compositeLayout);
      composite.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 3, 1));
      toolkit.adapt(composite);
    
      pluginExtensionsButton = toolkit.createButton(composite, "Extensions", SWT.CHECK);
      pluginExtensionsButton.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
    
      pluginInheritedButton = toolkit.createButton(composite, "Inherited", SWT.CHECK);
      pluginInheritedButton.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
    
      pluginConfigurationHyperlink = toolkit.createHyperlink(composite, "Configuration", SWT.NONE);
      pluginConfigurationHyperlink.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
      pluginConfigurationHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
        public void linkActivated(HyperlinkEvent e) {
          EObject element = currentPlugin.getConfiguration();
          parent.getPomEditor().showInSourceEditor(element==null ? currentPlugin : element);
        }
      });
  
      pluginExecutionsSection = toolkit.createSection(detailsComposite, Section.TITLE_BAR);
      GridData gd_pluginExecutionsSection = new GridData(SWT.FILL, SWT.FILL, true, true);
      gd_pluginExecutionsSection.minimumHeight = 50;
      pluginExecutionsSection.setLayoutData(gd_pluginExecutionsSection);
      pluginExecutionsSection.setText("Executions");
  
      pluginExecutionsEditor = new ListEditorComposite<PluginExecution>(pluginExecutionsSection, SWT.NONE);
      pluginExecutionsSection.setClient(pluginExecutionsEditor);
      toolkit.adapt(pluginExecutionsEditor);
      toolkit.paintBordersFor(pluginExecutionsEditor);
      pluginExecutionsEditor.setContentProvider(new ListEditorContentProvider<PluginExecution>());
      pluginExecutionsEditor.setLabelProvider(new LabelProvider() {
        public String getText(Object element) {
          if(element instanceof PluginExecution) {
            PluginExecution pluginExecution = (PluginExecution) element;
            String label = isEmpty(pluginExecution.getId()) ? "?" : pluginExecution.getId();
            if(pluginExecution.getPhase()!=null) {
              label +=  " : " + pluginExecution.getPhase();
            }
            return label;
          }
          return "";
        }
        public Image getImage(Object element) {
          return MavenEditorImages.IMG_EXECUTION;
        }
      });
      
      pluginExecutionsEditor.addSelectionListener(new ISelectionChangedListener() {
        public void selectionChanged(SelectionChangedEvent event) {
          List<PluginExecution> selection = pluginExecutionsEditor.getSelection();
          updatePluginExecution(selection.size()==1 ? selection.get(0) : null);
        }
      });
      
      pluginExecutionsEditor.setAddListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          CompoundCommand compoundCommand = new CompoundCommand();
          EditingDomain editingDomain = parent.getEditingDomain();
          
          ExecutionsType executions = currentPlugin.getExecutions();
          if(executions == null) {
            executions = PomFactory.eINSTANCE.createExecutionsType();
            Command command = SetCommand.create(editingDomain, currentPlugin, POM_PACKAGE.getPlugin_Executions(), executions);
            compoundCommand.append(command);
          }
          
          PluginExecution pluginExecution = PomFactory.eINSTANCE.createPluginExecution();
          Command command = AddCommand.create(editingDomain, executions, POM_PACKAGE.getExecutionsType_Execution(), pluginExecution);
          compoundCommand.append(command);
          
          editingDomain.getCommandStack().execute(compoundCommand);
          
          pluginExecutionsEditor.setSelection(Collections.singletonList(pluginExecution));
          updatePluginExecution(pluginExecution);
          executionIdText.setFocus();
        }
      });
      
      pluginExecutionsEditor.setRemoveListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          CompoundCommand compoundCommand = new CompoundCommand();
          EditingDomain editingDomain = parent.getEditingDomain();
   
          List<PluginExecution> list = pluginExecutionsEditor.getSelection();
          for(PluginExecution pluginExecution : list) {
            Command removeCommand = RemoveCommand.create(editingDomain, //
                currentPlugin.getExecutions(), POM_PACKAGE.getExecutionsType_Execution(), pluginExecution);
            compoundCommand.append(removeCommand);
          }
          
          editingDomain.getCommandStack().execute(compoundCommand);
          updatePluginExecution(null);
        }
      });
      
      pluginExecutionSection = toolkit.createSection(detailsComposite, Section.TITLE_BAR);
      GridData gd_pluginExecutionSection = new GridData(SWT.FILL, SWT.CENTER, true, false);
      gd_pluginExecutionSection.minimumHeight = 50;
      pluginExecutionSection.setLayoutData(gd_pluginExecutionSection);
      pluginExecutionSection.setText("Execution Details");
  
      Composite executionComposite = toolkit.createComposite(pluginExecutionSection, SWT.NONE);
      GridLayout executionCompositeLayout = new GridLayout(2, false);
      executionCompositeLayout.marginWidth = 2;
      executionCompositeLayout.marginHeight = 2;
      executionComposite.setLayout(executionCompositeLayout);
      pluginExecutionSection.setClient(executionComposite);
      toolkit.paintBordersFor(executionComposite);
  
      toolkit.createLabel(executionComposite, "Id:", SWT.NONE);
  
      executionIdText = toolkit.createText(executionComposite, null, SWT.NONE);
      executionIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
      toolkit.createLabel(executionComposite, "Phase:", SWT.NONE);
  
      executionPhaseCombo = new CCombo(executionComposite, SWT.FLAT);
      executionPhaseCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
      executionPhaseCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      executionPhaseCombo.setItems(new String[] { //
          "pre-clean", // Clean Lifecycle
          "clean", //
          "post-clean", //
          "validate", // Default Lifecycle
          "generate-sources", //
          "process-sources", //
          "generate-resources", //
          "process-resources", //
          "compile", //
          "process-classes", //
          "generate-test-sources", //
          "process-test-sources", //
          "generate-test-resources", //
          "process-test-resources", //
          "test-compile", //
          "process-test-classes", //
          "test", //
          "prepare-package", //
          "package", //
          "pre-integration-test", //
          "integration-test", //
          "post-integration-test", //
          "verify", //
          "install", //
          "deploy", //
          "pre-site", //
          "site", // Site Lifecycle
          "post-site", //
          "site-deploy"});
      toolkit.adapt(executionPhaseCombo, true, true);
  
      Label goalsLabel = toolkit.createLabel(executionComposite, "Goals:", SWT.NONE);
      goalsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
  
      goalsEditor = new ListEditorComposite<String>(executionComposite, SWT.NONE);
      goalsEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      toolkit.paintBordersFor(goalsEditor);
      goalsEditor.setContentProvider(new ListEditorContentProvider<String>());
      goalsEditor.setLabelProvider(new StringLabelProvider(MavenEditorImages.IMG_GOAL));
      
      goalsEditor.setAddListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          CompoundCommand compoundCommand = new CompoundCommand();
          EditingDomain editingDomain = parent.getEditingDomain();
   
          StringGoals goals = currentPluginExecution.getGoals();
          if(goals==null) {
            goals = PomFactory.eINSTANCE.createStringGoals();
            Command command = SetCommand.create(editingDomain, currentPluginExecution, //
                POM_PACKAGE.getPluginExecution_Goals(), goals);
            compoundCommand.append(command);
          }
          
          String goal = "?";
          Command command = AddCommand.create(editingDomain, goals, POM_PACKAGE.getStringGoals_Goal(), goal);
          compoundCommand.append(command);
          
          editingDomain.getCommandStack().execute(compoundCommand);
          
          goalsEditor.setSelection(Collections.singletonList(goal));
        }
      });
      
      goalsEditor.setRemoveListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          CompoundCommand compoundCommand = new CompoundCommand();
          EditingDomain editingDomain = parent.getEditingDomain();
   
          List<String> list = goalsEditor.getSelection();
          for(String goal : list) {
            Command removeCommand = RemoveCommand.create(editingDomain, //
                currentPluginExecution.getGoals(), POM_PACKAGE.getStringGoals_Goal(), goal);
            compoundCommand.append(removeCommand);
          }
          
          editingDomain.getCommandStack().execute(compoundCommand);
        }
      });
      
      goalsEditor.setCellModifier(new ICellModifier() {
        public boolean canModify(Object element, String property) {
          return true;
        }
   
        public Object getValue(Object element, String property) {
          return element;
        }
   
        public void modify(Object element, String property, Object value) {
          int n = goalsEditor.getViewer().getTable().getSelectionIndex();
          
          EditingDomain editingDomain = parent.getEditingDomain();
          Command command = SetCommand.create(editingDomain, //
              currentPluginExecution.getGoals(), POM_PACKAGE.getStringGoals_Goal(), value, n);
          editingDomain.getCommandStack().execute(command);
  
          // currentPluginExecution.getGoals().getGoal().set(n, (String) value);
          goalsEditor.update();
        }
      });
  
      Composite executionConfigureComposite = new Composite(executionComposite, SWT.NONE);
      executionConfigureComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));
      GridLayout gridLayout = new GridLayout(2, false);
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      executionConfigureComposite.setLayout(gridLayout);
      toolkit.adapt(executionConfigureComposite);
  
      executionInheritedButton = toolkit.createButton(executionConfigureComposite, "Inherited", SWT.CHECK);
      executionInheritedButton.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
  
      pluginExecutionConfigurationHyperlink = toolkit.createHyperlink(executionConfigureComposite, "Configuration", SWT.NONE);
      pluginExecutionConfigurationHyperlink.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
      pluginExecutionConfigurationHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
        public void linkActivated(HyperlinkEvent e) {
          EObject element = currentPluginExecution.getConfiguration();
          parent.getPomEditor().showInSourceEditor(element==null ? currentPluginExecution : element);
        }
      });
  
      pluginDependenciesSection = toolkit.createSection(detailsComposite, Section.TITLE_BAR);
      GridData pluginDependenciesSectionData = new GridData(SWT.FILL, SWT.FILL, true, true);
      pluginDependenciesSectionData.minimumHeight = 50;
      pluginDependenciesSection.setLayoutData(pluginDependenciesSectionData);
      pluginDependenciesSection.setText("Plugin Dependencies");
  
      pluginDependenciesEditor = new ListEditorComposite<Dependency>(pluginDependenciesSection, SWT.NONE);
      pluginDependenciesSection.setClient(pluginDependenciesEditor);
      toolkit.adapt(pluginDependenciesEditor);
      toolkit.paintBordersFor(pluginDependenciesEditor);
      pluginDependenciesEditor.setContentProvider(new ListEditorContentProvider<Dependency>());
      pluginDependenciesEditor.setLabelProvider(new DependencyLabelProvider());
      
      pluginDependenciesEditor.setReadOnly(true);
      
      // XXX implement plugin dependency editor actions and UI
      
      FormUtils.setEnabled(pluginDependenciesEditor, false);
    }

  private void updatePluginDetails(Plugin plugin) {
    if(changingSelection) {
      return;
    }
//    if(plugin!=null && currentPlugin==plugin) {
//      return;
//    }
    this.currentPlugin = plugin;
    
    if(parent!=null) {
      parent.removeNotifyListener(groupIdText);
      parent.removeNotifyListener(artifactIdText);
      parent.removeNotifyListener(versionText);
      parent.removeNotifyListener(pluginExtensionsButton);
      parent.removeNotifyListener(pluginInheritedButton);
    }
    
    if(plugin==null) {
      FormUtils.setEnabled(pluginDetailsSection, false);
      FormUtils.setEnabled(pluginExecutionsSection, false);
      FormUtils.setEnabled(pluginDependenciesSection, false);
      pluginSelectAction.setEnabled(false);
      openWebPageAction.setEnabled(false);
    
      setText(groupIdText, "");
      setText(artifactIdText, "");
      setText(versionText, "");
      setButton(pluginExtensionsButton, false);
      setButton(pluginInheritedButton, false);
      
      pluginExecutionsEditor.setInput(null);
      pluginDependenciesEditor.setInput(null);
      updatePluginExecution(null);
      return;
    }
    
    FormUtils.setEnabled(pluginDetailsSection, true);
    FormUtils.setEnabled(pluginExecutionsSection, true);
    FormUtils.setEnabled(pluginDependenciesSection, true);

    FormUtils.setReadonly(pluginDetailsSection, parent.isReadOnly());
    FormUtils.setReadonly(pluginExecutionsSection, parent.isReadOnly());
    pluginSelectAction.setEnabled(!parent.isReadOnly());
    openWebPageAction.setEnabled(true);

    // XXX implement dependency editing
    FormUtils.setReadonly(pluginDependenciesSection, true);

    setText(groupIdText, plugin.getGroupId());
    setText(artifactIdText, plugin.getArtifactId());
    setText(versionText, plugin.getVersion());
    
    setButton(pluginInheritedButton, plugin.getInherited()==null || "true".equals(plugin.getInherited()));
    setButton(pluginExtensionsButton, "true".equals(plugin.getExtensions()));
    
    ExecutionsType plugineExecutions = plugin.getExecutions();
    pluginExecutionsEditor.setInput(plugineExecutions==null ? null : plugineExecutions.getExecution());
    
    Dependencies pluginDependencies = plugin.getDependencies();
    pluginDependenciesEditor.setInput(pluginDependencies==null ? null : pluginDependencies.getDependency());
    
    updatePluginExecution(null);
    
    // register listeners
    
    ValueProvider<Plugin> provider = new ValueProvider.DefaultValueProvider<Plugin>(currentPlugin);
    parent.setModifyListener(groupIdText, provider, POM_PACKAGE.getPlugin_GroupId(), "");
    parent.setModifyListener(artifactIdText, provider, POM_PACKAGE.getPlugin_ArtifactId(), "");
    parent.setModifyListener(versionText, provider, POM_PACKAGE.getPlugin_Version(), "");
    parent.setModifyListener(pluginInheritedButton, provider, POM_PACKAGE.getPlugin_Inherited(), "true");
    parent.setModifyListener(pluginExtensionsButton, provider, POM_PACKAGE.getPlugin_Extensions(), "false");
  }

  private void updatePluginExecution(PluginExecution pluginExecution) {
//    if(pluginExecution!=null && currentPluginExecution==pluginExecution) {
//      return;
//    }
    currentPluginExecution = pluginExecution;
    
    if(parent!=null) {
      parent.removeNotifyListener(executionIdText);
      parent.removeNotifyListener(executionPhaseCombo);
      parent.removeNotifyListener(executionInheritedButton);
    }
    
    if(pluginExecution==null) {
      FormUtils.setEnabled(pluginExecutionSection, false);

      setText(executionIdText, "");
      setText(executionPhaseCombo, "");
      setButton(executionInheritedButton, false);
      goalsEditor.setInput(null);
      
      return;
    }
    
    FormUtils.setEnabled(pluginExecutionSection, true);
    FormUtils.setReadonly(pluginExecutionSection, parent.isReadOnly());
    
    setText(executionIdText, pluginExecution.getId());
    setText(executionPhaseCombo, pluginExecution.getPhase());
    setButton(executionInheritedButton, pluginExecution.getInherited()==null || "true".equals(pluginExecution.getInherited()));

    StringGoals goals = pluginExecution.getGoals();
    goalsEditor.setInput(goals==null ? null : goals.getGoal());
    // goalsEditor.setSelection(Collections.<String>emptyList());
    
    // register listeners
    ValueProvider<PluginExecution> provider = new ValueProvider.DefaultValueProvider<PluginExecution>(pluginExecution);
    parent.setModifyListener(executionIdText, provider, POM_PACKAGE.getPluginExecution_Id(), "");
    parent.setModifyListener(executionPhaseCombo, provider, POM_PACKAGE.getPluginExecution_Phase(), "");
    parent.setModifyListener(executionInheritedButton, provider, POM_PACKAGE.getPluginExecution_Inherited(), "true");
  }

  public void loadData(MavenPomEditorPage editorPage, ValueProvider<Plugins> pluginsProvider,
      ValueProvider<Plugins> pluginManagementProvider) {
    this.parent = editorPage;
    this.pluginsProvider = pluginsProvider;
    this.pluginManagementProvider = pluginManagementProvider;
    
    changingSelection = true;
    loadPlugins();
    loadPluginManagement();
    changingSelection = false;
    
    pluginsEditor.setReadOnly(parent.isReadOnly());
    pluginManagementEditor.setReadOnly(parent.isReadOnly());
    
    pluginAddAction.setEnabled(!parent.isReadOnly());
    pluginManagementAddAction.setEnabled(!parent.isReadOnly());
    
    updatePluginDetails(null);
    
//    pluginExecutionsEditor.setReadOnly(parent.isReadOnly());
//    goalsEditor.setReadOnly(parent.isReadOnly());
//    pluginDependenciesEditor.setReadOnly(parent.isReadOnly());
  }

  private void loadPlugins() {
    Plugins plugins = pluginsProvider.getValue();
    pluginsEditor.setInput(plugins==null ? null : plugins.getPlugin());
  }

  private void loadPluginManagement() {
    Plugins plugins = pluginManagementProvider.getValue();
    pluginManagementEditor.setInput(plugins == null ? null : plugins.getPlugin());
  }
  
  public void updateView(MavenPomEditorPage editorPage, Notification notification) {
    EObject object = (EObject) notification.getNotifier();
    if(object instanceof Plugins) {
      pluginsEditor.refresh();
      pluginManagementEditor.refresh();
    }
    
    if(object instanceof PluginManagement) {
      pluginManagementEditor.refresh();
    }
    
    if(object instanceof Plugin) {
      pluginsEditor.refresh();
      pluginManagementEditor.refresh();
      if(object==currentPlugin) {
        updatePluginDetails(currentPlugin);
      }
    }
    
    if(object instanceof ExecutionsType) {
      pluginExecutionsEditor.refresh();
      goalsEditor.refresh();
    }
    
    if(object instanceof PluginExecution) {
      pluginExecutionsEditor.refresh();
      if(currentPluginExecution==object) {
        updatePluginExecution(currentPluginExecution);
      }
    }
    
    if(object instanceof StringGoals) {
      goalsEditor.refresh();
    }
  }

  void createPlugin(ListEditorComposite<Plugin> editor, ValueProvider<Plugins> provider, String groupId, String artifactId, String version) {
    CompoundCommand compoundCommand = new CompoundCommand();
    EditingDomain editingDomain = parent.getEditingDomain();
    
    Plugins plugins = provider.getValue();
    boolean pluginsCreated = false;
    if(plugins==null) {
      plugins = pluginsProvider.create(editingDomain, compoundCommand);
      pluginsCreated = true;
    }
    
    Plugin plugin = PomFactory.eINSTANCE.createPlugin();
    plugin.setGroupId(groupId);
    plugin.setArtifactId(artifactId);
    plugin.setVersion(version);
    
    Command command = AddCommand.create(editingDomain, plugins, POM_PACKAGE.getPlugins_Plugin(), plugin);
    compoundCommand.append(command);
    
    editingDomain.getCommandStack().execute(compoundCommand);
    
    if(pluginsCreated){
//      editor.setInput(plugins.getPlugin());
    }
    editor.setSelection(Collections.singletonList(plugin));
    updatePluginDetails(plugin);
    groupIdText.setFocus();
  }

  /**
   * Plugin label provider
   */
  private static final class PluginLabelProvider extends LabelProvider {

    private boolean showGroupId = true;

    public void setShowGroupId(boolean showGroupId) {
      this.showGroupId = showGroupId;
    }
    
    public String getText(Object element) {
      if(element instanceof Plugin) {
        Plugin plugin = (Plugin) element;
        String label = "";
        
        if(showGroupId) {
          if(!isEmpty(plugin.getGroupId())) {
            label += plugin.getGroupId() + " : ";
          }
        }
        
        label += isEmpty(plugin.getArtifactId()) ? "?" : plugin.getArtifactId();
        
        if(!isEmpty(plugin.getVersion())) {
          label += " : " + plugin.getVersion();
        }
        
        return label;
      }
      return super.getText(element);
    }
    
    public Image getImage(Object element) {
      return MavenEditorImages.IMG_PLUGIN;
    }
    
  }

  public void setSearchControl(SearchControl searchControl) {
    if(this.searchControl!=null) {
      return;
    }
    
    this.searchMatcher = new SearchMatcher(searchControl);
    this.searchFilter = new PluginFilter(searchMatcher);
    this.searchControl = searchControl;
    this.searchControl.getSearchText().addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        changingSelection = true;
        selectPlugins(pluginsEditor, pluginsProvider);
        selectPlugins(pluginManagementEditor, pluginManagementProvider);
        changingSelection = false;
        
        updatePluginDetails(null);
      }

      private void selectPlugins(ListEditorComposite<Plugin> editor, ValueProvider<Plugins> provider) {
        List<Plugin> plugins = new ArrayList<Plugin>();
        Plugins value = provider.getValue();
        if(value!=null) {
          for(Plugin p : value.getPlugin()) {
            if(searchMatcher.isMatchingArtifact(p.getGroupId(), p.getArtifactId())) {
              plugins.add(p);
            }
          }
        }
        editor.setSelection(plugins);
        editor.refresh();
      }
    });
  }

  
  public static class PluginFilter extends ViewerFilter {

    private final SearchMatcher searchMatcher;

    public PluginFilter(SearchMatcher searchMatcher) {
      this.searchMatcher = searchMatcher;
    }

    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if(element instanceof Plugin) {
        Plugin p = (Plugin) element;
        return searchMatcher.isMatchingArtifact(p.getGroupId(), p.getArtifactId());
      }
      return false;
    }

  }

  
}
