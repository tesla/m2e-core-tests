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
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
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
import org.maven.ide.components.pom.StringGoals;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.editor.pom.MavenPomEditorPage;

/**
 * @author Eugene Kuleshov
 */
public class PluginsComposite extends Composite {

  private Text executionPhasetext;
  private Text executionIdText;
  private Button executionConfigurePluginButton;
  private Button executionInheritedButton;
  private FormToolkit toolkit = new FormToolkit(Display.getCurrent());
  
  private ListEditorComposite<Plugin> pluginsEditor;
  private ListEditorComposite<Plugin> pluginManagementEditor;
  
  private Text groupIdText;
  private Text artifactIdText;
  private Text versionText;
  private Section pluginDetailsSection;
  private Button extensions;
  private Button inheritedButton;

  private Button configurePluginButton;
  private ListEditorComposite<Dependency> pluginDependenciesList;
  private Model model;
  private Plugins plugins;
  private PluginManagement pluginManagement;
  private ListEditorComposite<String> goalsEditor;
  private ListEditorComposite<PluginExecution> pluginExecutionsEditor;

  private Section pluginExecutionSection;
  
  
  public PluginsComposite(Composite parent, int style) {
    super(parent, style);
    
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
      }
    });
    
    verticalSashForm.setWeights(new int[] {1, 1 });

    createPluginDetailsSection(horizontalSashForm);

    horizontalSashForm.setWeights(new int[] {1, 1});

    updatePluginDetails(null);
  }

  private void createPluginDetailsSection(Composite horizontalSashForm) {
    pluginDetailsSection = toolkit.createSection(horizontalSashForm, Section.TITLE_BAR);
    pluginDetailsSection.setText("Plugin Details");
  
    Composite pluginDetailsComposite = toolkit.createComposite(pluginDetailsSection, SWT.NONE);
    GridLayout pluginDetailsLayout = new GridLayout(2, false);
    pluginDetailsLayout.marginWidth = 1;
    pluginDetailsLayout.marginHeight = 2;
    pluginDetailsComposite.setLayout(pluginDetailsLayout);
    toolkit.paintBordersFor(pluginDetailsComposite);
    pluginDetailsSection.setClient(pluginDetailsComposite);
  
    toolkit.createLabel(pluginDetailsComposite, "Group Id:", SWT.NONE);
  
    groupIdText = toolkit.createText(pluginDetailsComposite, null, SWT.NONE);
    groupIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    toolkit.createLabel(pluginDetailsComposite, "Artifact Id:", SWT.NONE);
  
    artifactIdText = toolkit.createText(pluginDetailsComposite, null, SWT.NONE);
    artifactIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    toolkit.createLabel(pluginDetailsComposite, "Version:", SWT.NONE);
  
    versionText = toolkit.createText(pluginDetailsComposite, null, SWT.NONE);
    GridData gd_versionText = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    gd_versionText.widthHint = 200;
    versionText.setLayoutData(gd_versionText);
  
    Composite composite = new Composite(pluginDetailsComposite, SWT.NONE);
    GridLayout compositeLayout = new GridLayout();
    compositeLayout.marginWidth = 0;
    compositeLayout.marginHeight = 0;
    compositeLayout.numColumns = 3;
    composite.setLayout(compositeLayout);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
    toolkit.adapt(composite);
  
    extensions = toolkit.createButton(composite, "Extensions", SWT.CHECK);
    extensions.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
  
    inheritedButton = toolkit.createButton(composite, "Inherited", SWT.CHECK);
    inheritedButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
  
    configurePluginButton = toolkit.createButton(composite, "Configure...", SWT.NONE);
    configurePluginButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false));

    Section pluginExecutionsSection = toolkit.createSection(pluginDetailsComposite, Section.TITLE_BAR);
    pluginExecutionsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
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
          String label = pluginExecution.getId();
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

    pluginExecutionSection = toolkit.createSection(pluginDetailsComposite, Section.TITLE_BAR);
    pluginExecutionSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    pluginExecutionSection.setText("Execution");

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

    executionPhasetext = toolkit.createText(executionComposite, null, SWT.NONE);
    executionPhasetext.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label label = toolkit.createLabel(executionComposite, "Goals:", SWT.NONE);
    GridData gd_label = new GridData(SWT.LEFT, SWT.TOP, false, false);
    label.setLayoutData(gd_label);

    goalsEditor = new ListEditorComposite<String>(executionComposite, SWT.NONE);
    goalsEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    toolkit.paintBordersFor(goalsEditor);
    goalsEditor.setContentProvider(new ListEditorContentProvider<String>());
    goalsEditor.setLabelProvider(new StringLabelProvider(MavenEditorImages.IMG_GOAL));

    Composite executionConfigureComposite = new Composite(executionComposite, SWT.NONE);
    executionConfigureComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    executionConfigureComposite.setLayout(gridLayout);
    toolkit.adapt(executionConfigureComposite);

    executionInheritedButton = toolkit.createButton(executionConfigureComposite, "Inherited", SWT.CHECK);
    executionInheritedButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

    executionConfigurePluginButton = toolkit.createButton(executionConfigureComposite, "Configure...", SWT.NONE);
    executionConfigurePluginButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false));

    Section dependenciesSection = toolkit.createSection(pluginDetailsComposite, Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
    dependenciesSection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    dependenciesSection.setText("Dependencies");

    pluginDependenciesList = new ListEditorComposite<Dependency>(dependenciesSection, SWT.NONE);
    dependenciesSection.setClient(pluginDependenciesList);
    toolkit.adapt(pluginDependenciesList);
    toolkit.paintBordersFor(pluginDependenciesList);
    pluginDependenciesList.setContentProvider(new ListEditorContentProvider<Dependency>());
    pluginDependenciesList.setLabelProvider(new DependencyLabelProvider());
  }

  private void updatePluginDetails(Plugin plugin) {
    if(plugin==null) {
      pluginDetailsSection.setEnabled(false);
    
      groupIdText.setText("");
      artifactIdText.setText("");
      versionText.setText("");
      
      extensions.setSelection(false);
      inheritedButton.setSelection(false);
      
      pluginDependenciesList.setInput(null);
      pluginExecutionsEditor.setInput(null);

      updatePluginExecution(null);
      
    } else {
      pluginDetailsSection.setEnabled(true);
  
      groupIdText.setText(nvl(plugin.getGroupId()));
      artifactIdText.setText(nvl(plugin.getArtifactId()));
      versionText.setText(nvl(plugin.getVersion()));
      
      // extensions.setSelection();
      inheritedButton.setSelection("true".equals(plugin.getInherited()));
      extensions.setSelection("true".equals(plugin.getExtensions()));
      
      Dependencies pluginDependencies = plugin.getDependencies();
      pluginDependenciesList.setInput(pluginDependencies==null ? null : pluginDependencies.getDependency());
  
      ExecutionsType plugineExecutions = plugin.getExecutions();
      pluginExecutionsEditor.setInput(plugineExecutions==null ? null : plugineExecutions.getExecution());
      
      updatePluginExecution(null);
    }
  }

  private void updatePluginExecution(PluginExecution pluginExecution) {
    if(pluginExecution==null) {
      pluginExecutionSection.setEnabled(false);

      executionIdText.setText("");
      executionPhasetext.setText("");
      goalsEditor.setInput(null);
      executionInheritedButton.setSelection(false);
    } else {
      pluginExecutionSection.setEnabled(true);
      
      executionIdText.setText(nvl(pluginExecution.getId()));
      executionPhasetext.setText(nvl(pluginExecution.getPhase()));
  
      StringGoals goals = pluginExecution.getGoals();
      goalsEditor.setInput(goals==null ? null : goals.getGoal());
      
      // XXX fix schema type
      executionInheritedButton.setSelection("true".equals(pluginExecution.getInherited()));
    }
  }

  public void loadData(MavenPomEditorPage editorPage) {
    model = editorPage.getModel();
    loadPlugins(model);
    loadPluginManagement(model);
  }

  private void loadPlugins(Model model) {
    Build build = model.getBuild();
    if(build!=null) {
      plugins = build.getPlugins();
      pluginsEditor.setInput(plugins==null ? null : plugins.getPlugin());
      return;
    }
    plugins = null;
    pluginsEditor.setInput(null);
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
    pluginManagementEditor.setInput(null);
  }
  
  public void updateView(MavenPomEditorPage editorPage, Notification notification) {
    EObject object = (EObject) notification.getNotifier();
    if(object instanceof Plugins || object instanceof Plugin) {
      loadPlugins(model);
    }
    
    if(object instanceof PluginManagement) {
      loadPluginManagement(model);
    }
  }

  /**
   * Plugin label provider
   */
  private static final class PluginLabelProvider extends LabelProvider {
    public String getText(Object element) {
      if(element instanceof Plugin) {
        Plugin plugin = (Plugin) element;
        String label = plugin.getGroupId() + ": " + plugin.getArtifactId();
        String version = plugin.getVersion();
        if(version!=null && version.length()>0) {
          label += " : " + version;
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
