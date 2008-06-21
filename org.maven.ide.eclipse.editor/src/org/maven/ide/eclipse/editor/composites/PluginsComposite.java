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
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
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
import org.maven.ide.components.pom.Build;
import org.maven.ide.components.pom.Dependencies;
import org.maven.ide.components.pom.Dependency;
import org.maven.ide.components.pom.ExecutionsType;
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.Plugin;
import org.maven.ide.components.pom.PluginExecution;
import org.maven.ide.components.pom.PluginManagement;
import org.maven.ide.components.pom.Plugins;
import org.maven.ide.components.pom.PomFactory;
import org.maven.ide.components.pom.PomPackage;
import org.maven.ide.components.pom.StringGoals;
import org.maven.ide.eclipse.actions.MavenRepositorySearchDialog;
import org.maven.ide.eclipse.actions.OpenPomAction;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.editor.pom.FormUtils;
import org.maven.ide.eclipse.editor.pom.MavenPomEditorPage;
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
  Plugins plugins;
  PluginManagement pluginManagement;
  ListEditorComposite<String> goalsEditor;
  ListEditorComposite<PluginExecution> pluginExecutionsEditor;

  Section pluginExecutionSection;
  Section pluginExecutionsSection;
  Section pluginDependenciesSection;

  Button pluginSelectButton;
  
  // model
  Model model;
  Plugin currentPlugin;
  PluginExecution currentPluginExecution;

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
  
    Section pluginsSection = toolkit.createSection(verticalSashForm, Section.TITLE_BAR | Section.COMPACT);
    pluginsSection.setText("Plugins");
  
    pluginsEditor = new ListEditorComposite<Plugin>(pluginsSection, SWT.NONE);
    pluginsSection.setClient(pluginsEditor);
    toolkit.adapt(pluginsEditor);
    toolkit.paintBordersFor(pluginsEditor);
    pluginsEditor.setContentProvider(new ListEditorContentProvider<Plugin>());
    pluginsEditor.setLabelProvider(new PluginLabelProvider());

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
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
        
        Build build = model.getBuild();
        if(build==null) {
          build = PomFactory.eINSTANCE.createBuild();
          Command command = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Build(), build);
          compoundCommand.append(command);
        }
        
        Plugins plugins = build.getPlugins();
        if(plugins==null) {
          plugins = PomFactory.eINSTANCE.createPlugins();
          Command command = SetCommand.create(editingDomain, build, POM_PACKAGE.getBuildBase_Plugins(), plugins);
          compoundCommand.append(command);
        }
        
        Plugin plugin = PomFactory.eINSTANCE.createPlugin();
        Command command = AddCommand.create(editingDomain, plugins, POM_PACKAGE.getPlugins_Plugin(), plugin);
        compoundCommand.append(command);
        
        editingDomain.getCommandStack().execute(compoundCommand);
        
        pluginsEditor.setSelection(Collections.singletonList(plugin));
        updatePluginDetails(plugin);
        groupIdText.setFocus();
      }
    });
    
    pluginsEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
 
        List<Plugin> list = pluginsEditor.getSelection();
        for(Plugin plugin : list) {
          Command removeCommand = RemoveCommand.create(editingDomain, model.getBuild().getPlugins(), 
              POM_PACKAGE.getPlugins_Plugin(), plugin);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
        updatePluginDetails(null);
      }
    });
  
    Section pluginManagementSection = toolkit.createSection(verticalSashForm, Section.TITLE_BAR);
    pluginManagementSection.setText("Plugin Management");
  
    pluginManagementEditor = new ListEditorComposite<Plugin>(pluginManagementSection, SWT.NONE);
    pluginManagementSection.setClient(pluginManagementEditor);
    toolkit.adapt(pluginManagementEditor);
    toolkit.paintBordersFor(pluginManagementEditor);
    pluginManagementEditor.setContentProvider(new ListEditorContentProvider<Plugin>());
    pluginManagementEditor.setLabelProvider(new PluginLabelProvider());

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
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
        
        Build build = model.getBuild();
        if(build == null) {
          build = PomFactory.eINSTANCE.createBuild();
          Command command = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Build(), build);
          compoundCommand.append(command);
        }
        
        PluginManagement pluginManagement = build.getPluginManagement();
        if(pluginManagement == null) {
          pluginManagement = PomFactory.eINSTANCE.createPluginManagement();
          Command command = SetCommand.create(editingDomain, build, POM_PACKAGE.getBuildBase_PluginManagement(),
              pluginManagement);
          compoundCommand.append(command);
        }
        
        Plugins plugins = pluginManagement.getPlugins();
        if(plugins==null) {
          plugins = PomFactory.eINSTANCE.createPlugins();
          Command command = SetCommand.create(editingDomain, pluginManagement, //
              POM_PACKAGE.getPluginManagement_Plugins(), plugins);
          compoundCommand.append(command);
        }
        
        Plugin plugin = PomFactory.eINSTANCE.createPlugin();
        Command command = AddCommand.create(editingDomain, plugins, POM_PACKAGE.getPlugins_Plugin(), plugin);
        compoundCommand.append(command);
        
        editingDomain.getCommandStack().execute(compoundCommand);
        
        pluginManagementEditor.setSelection(Collections.singletonList(plugin));
        updatePluginDetails(plugin);
        groupIdText.setFocus();
      }
    });
    
    pluginManagementEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
 
        List<Plugin> list = pluginManagementEditor.getSelection();
        for(Plugin plugin : list) {
          Command removeCommand = RemoveCommand.create(editingDomain, //
              model.getBuild().getPluginManagement().getPlugins(), POM_PACKAGE.getPlugins_Plugin(), plugin);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
        updatePluginDetails(null);
      }
    });
    
    verticalSashForm.setWeights(new int[] {1, 1});

    createPluginDetailsSection(horizontalSashForm);
    horizontalSashForm.setWeights(new int[] {1, 1 });

    updatePluginDetails(null);
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

    pluginSelectButton = toolkit.createButton(pluginDetailsComposite, "Select...", SWT.NONE);
    pluginSelectButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        Set<Dependency> artifacts = Collections.emptySet();
        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(parent.getEditorSite().getShell(),
            "Add Plugin", IndexManager.SEARCH_PLUGIN, artifacts);
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            groupIdText.setText(nvl(af.group));
            artifactIdText.setText(nvl(af.artifact));
            versionText.setText(nvl(af.version));
          }
        }
      }
    });
  
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
    pluginDependenciesSection.setText("Dependencies");

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

  public void loadData(MavenPomEditorPage editorPage) {
    parent = editorPage;
    model = editorPage.getModel();
    loadPlugins(model);
    loadPluginManagement(model);
    
    updatePluginDetails(null);
    
    pluginsEditor.setReadOnly(parent.isReadOnly());
    pluginManagementEditor.setReadOnly(parent.isReadOnly());
    
//    pluginExecutionsEditor.setReadOnly(parent.isReadOnly());
//    goalsEditor.setReadOnly(parent.isReadOnly());
//    pluginDependenciesEditor.setReadOnly(parent.isReadOnly());
  }

  private void loadPlugins(Model model) {
    Build build = model.getBuild();
    if(build!=null) {
      plugins = build.getPlugins();
      pluginsEditor.setInput(plugins==null ? null : plugins.getPlugin());
      return;
    }
    plugins = null;

    changingSelection = true;
    pluginsEditor.setInput(null);
    changingSelection = false;
  }

  private void loadPluginManagement(Model model2) {
    Build build = model.getBuild();
    if(build!=null) {
      pluginManagement = build.getPluginManagement();
      if(pluginManagement!=null) {
        Plugins plugins = pluginManagement.getPlugins();
        pluginManagementEditor.setInput(plugins==null ? null : plugins.getPlugin());
        return;
      }
    }
    pluginManagement = null;

    changingSelection = true;
    pluginManagementEditor.setInput(null);
    changingSelection = false;
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

  /**
   * Plugin label provider
   */
  private static final class PluginLabelProvider extends LabelProvider {
    public String getText(Object element) {
      if(element instanceof Plugin) {
        Plugin plugin = (Plugin) element;
        String label = ""; 
        if(!isEmpty(plugin.getGroupId())) {
          label += plugin.getGroupId() + " : ";
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

}
