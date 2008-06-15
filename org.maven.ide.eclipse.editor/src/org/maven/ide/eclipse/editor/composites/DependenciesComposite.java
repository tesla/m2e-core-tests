/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.composites;

import static org.maven.ide.eclipse.editor.pom.FormUtils.nvl;
import static org.maven.ide.eclipse.editor.pom.FormUtils.setText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.maven.ide.components.pom.Dependencies;
import org.maven.ide.components.pom.Dependency;
import org.maven.ide.components.pom.DependencyManagement;
import org.maven.ide.components.pom.Exclusion;
import org.maven.ide.components.pom.ExclusionsType;
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.PomFactory;
import org.maven.ide.components.pom.PomPackage;
import org.maven.ide.eclipse.actions.MavenRepositorySearchDialog;
import org.maven.ide.eclipse.editor.pom.MavenPomEditorPage;
import org.maven.ide.eclipse.editor.pom.FormUtils;
import org.maven.ide.eclipse.editor.pom.ValueProvider;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.wizards.WidthGroup;


/**
 * @author Eugene Kuleshov
 */
public class DependenciesComposite extends Composite {

  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;
  
  protected MavenPomEditorPage parent; 
  
  private FormToolkit toolkit = new FormToolkit(Display.getCurrent());

  // controls
  
  ListEditorComposite<Dependency> dependencyManagementListEditor;
  ListEditorComposite<Dependency> dependenciesListEditor;

  Composite dependencyDetailsComposite;

  WidthGroup detailsWidthGroup = new WidthGroup();

  Text groupIdText;
  Text artifactIdText;
  Text versionText;
  Text classifierText;
  CCombo scopeCombo;
  CCombo typeCombo;
  Text systemPathText;

  Button optionalButton;

  ListEditorComposite<Exclusion> exclusionsListEditor;

  Section exclusionDetailsSection;
  Text exclusionArtifactIdText;
  Text exclusionGroupIdText;

  // model
  
  Model model;
  
  DependencyManagement dependencyManagement;
  
  Dependency currentDependency;
  

  public DependenciesComposite(Composite composite, int flags) {
    super(composite, flags);

    createComposite();
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
    createExclusionDetailsSection(toolkit, dependencyDetailsComposite);
    
    horizontalSash.setWeights(new int[] {1, 1});
    
    updateDependencyDetails(null);
  }

  private void createDependenciesSection(SashForm verticalSash) {
    Section dependenciesSection = toolkit.createSection(verticalSash, Section.TITLE_BAR);
    dependenciesSection.marginWidth = 3;
    dependenciesSection.setText("Dependencies");

    dependenciesListEditor = new ListEditorComposite<Dependency>(dependenciesSection, SWT.NONE);
    dependenciesListEditor.setLabelProvider(new DependencyLabelProvider());
    dependenciesListEditor.setContentProvider(new ListEditorContentProvider<Dependency>());

    dependenciesListEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
        
        Dependencies dependencies = model.getDependencies();
        if(dependencies == null) {
          dependencies = PomFactory.eINSTANCE.createDependencies();
          Command createDependenciesCommand = SetCommand.create(editingDomain, model, 
              POM_PACKAGE.getModel_Dependencies(), dependencies);
          compoundCommand.append(createDependenciesCommand);
        }
        
        Dependency dependency = PomFactory.eINSTANCE.createDependency();
        Command addDependencyCommand = AddCommand.create(editingDomain, dependencies, 
            POM_PACKAGE.getDependencies_Dependency(), dependency);
        compoundCommand.append(addDependencyCommand);
        
        editingDomain.getCommandStack().execute(compoundCommand);
        
        dependenciesListEditor.setSelection(Collections.singletonList(dependency));
        updateDependencyDetails(dependency);
        groupIdText.setFocus();
      }
    });

    dependenciesListEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        List<Dependency> dependencyList = dependenciesListEditor.getSelection();
        for(Dependency dependency : dependencyList) {
          Command removeCommand = RemoveCommand.create(editingDomain, model.getDependencies(), 
        		  POM_PACKAGE.getDependencies_Dependency(), dependency);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
        updateDependencyDetails(null);
      }
    });

    dependenciesListEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Dependency> dependencyList = dependenciesListEditor.getSelection();
        updateDependencyDetails(dependencyList.size()==1 ? dependencyList.get(0) : null);
      }
    });

    dependenciesSection.setClient(dependenciesListEditor);
    toolkit.adapt(dependenciesListEditor);
    toolkit.paintBordersFor(dependenciesListEditor);
  }

  private void createDependencyManagementSection(SashForm verticalSash) {
    Section dependencyManagementSection = toolkit.createSection(verticalSash, Section.TITLE_BAR);
    dependencyManagementSection.marginWidth = 3;
    dependencyManagementSection.setText("Dependency Management");

    dependencyManagementListEditor = new ListEditorComposite<Dependency>(dependencyManagementSection, SWT.NONE);
    dependencyManagementSection.setClient(dependencyManagementListEditor);

    dependencyManagementListEditor.setLabelProvider(new DependencyLabelProvider());
    dependencyManagementListEditor.setContentProvider(new ListEditorContentProvider<Dependency>());

    dependencyManagementListEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
        
        if(dependencyManagement == null) {
          dependencyManagement = PomFactory.eINSTANCE.createDependencyManagement();
          Command createDependencyManagement = SetCommand.create(editingDomain, model, 
              POM_PACKAGE.getModel_DependencyManagement(), dependencyManagement);
          compoundCommand.append(createDependencyManagement);
        }
        
        Dependencies dependencies = dependencyManagement.getDependencies();
        if(dependencies == null) {
          dependencies = PomFactory.eINSTANCE.createDependencies();
          Command createDependency = SetCommand.create(editingDomain, dependencyManagement, 
              POM_PACKAGE.getDependencyManagement_Dependencies(), dependencies);
          compoundCommand.append(createDependency);
        }

        Dependency dependency = PomFactory.eINSTANCE.createDependency();
        Command addDependencyCommand = AddCommand.create(editingDomain, dependencies, 
            POM_PACKAGE.getDependencies_Dependency(), dependency);
        compoundCommand.append(addDependencyCommand);
        
        editingDomain.getCommandStack().execute(compoundCommand);
        
        dependencyManagementListEditor.setSelection(Collections.singletonList(dependency));
        updateDependencyDetails(dependency);
        groupIdText.setFocus();
      }
    });

    dependencyManagementListEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        List<Dependency> dependencyList = dependencyManagementListEditor.getSelection();
        for(Dependency dependency : dependencyList) {
          Command removeCommand = RemoveCommand.create(editingDomain, model.getDependencyManagement().getDependencies(), 
              POM_PACKAGE.getDependencies_Dependency(), dependency);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
        updateDependencyDetails(null);
      }
    });

    dependencyManagementListEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Dependency> dependencyList = dependencyManagementListEditor.getSelection();
        updateDependencyDetails(dependencyList.size()==1 ? dependencyList.get(0) : null);
      }
    });

    toolkit.adapt(dependencyManagementListEditor);
    toolkit.paintBordersFor(dependencyManagementListEditor);
  }

  private void createDependencyDetails(FormToolkit toolkit, Composite dependencyDetailsComposite) {
    Section dependencyDetailsSection = toolkit.createSection(dependencyDetailsComposite, Section.TITLE_BAR);
    dependencyDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    dependencyDetailsSection.setText("Dependency Details");
    dependencyDetailsSection.marginWidth = 3;

    Composite dependencyComposite = toolkit.createComposite(dependencyDetailsSection, SWT.NONE);
    GridLayout dependencyCompositeLayout = new GridLayout(3, false);
    dependencyCompositeLayout.marginWidth = 2;
    dependencyCompositeLayout.marginHeight = 2;
    dependencyComposite.setLayout(dependencyCompositeLayout);
    toolkit.paintBordersFor(dependencyComposite);
    dependencyDetailsSection.setClient(dependencyComposite);
    dependencyComposite.addControlListener(detailsWidthGroup);

    Label groupIdLabel = toolkit.createLabel(dependencyComposite, "Group Id:", SWT.NONE);
    groupIdLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(groupIdLabel);

    groupIdText = toolkit.createText(dependencyComposite, null, SWT.NONE);
    groupIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

    Label artifactIdLabel = toolkit.createLabel(dependencyComposite, "Artifact Id:", SWT.NONE);
    artifactIdLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(artifactIdLabel);

    artifactIdText = toolkit.createText(dependencyComposite, null, SWT.NONE);
    artifactIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

    Label versionLabel = toolkit.createLabel(dependencyComposite, "Version:", SWT.NONE);
    versionLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(versionLabel);

    versionText = toolkit.createText(dependencyComposite, null, SWT.NONE);
    GridData gd_versionText = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    gd_versionText.widthHint = 200;
    versionText.setLayoutData(gd_versionText);

    Button dependencySelectButton = toolkit.createButton(dependencyComposite, "Select...", SWT.NONE);
    dependencySelectButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 2));
    dependencySelectButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        // TODO calculate current list of artifacts for the project
        Set<Dependency> artifacts = Collections.emptySet();
        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
            "Add Dependency", IndexManager.SEARCH_ARTIFACT, artifacts);
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            groupIdText.setText(nvl(af.group));
            artifactIdText.setText(nvl(af.artifact));
            versionText.setText(nvl(af.version));
            typeCombo.setText(nvl(af.type));
          }
        }
      }
    });

    Label classifierLabel = toolkit.createLabel(dependencyComposite, "Classifier:", SWT.NONE);
    classifierLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(classifierLabel);

    classifierText = toolkit.createText(dependencyComposite, null, SWT.NONE);
    GridData gd_classifierText = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    gd_classifierText.widthHint = 200;
    classifierText.setLayoutData(gd_classifierText);

    Label typeLabel = toolkit.createLabel(dependencyComposite, "Type:", SWT.NONE);
    typeLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(typeLabel);

    typeCombo = new CCombo(dependencyComposite, SWT.FLAT);
    
    // TODO retrieve artifact type from selected dependency 
    typeCombo.add("jar");
    typeCombo.add("war");
    typeCombo.add("rar");
    typeCombo.add("ear");
    typeCombo.add("par");
    typeCombo.add("ejb3");
    typeCombo.add("ejb-client");
    typeCombo.add("test-jar");
    typeCombo.add("java-source");
    typeCombo.add("javadoc");
    typeCombo.add("maven-plugin");
    typeCombo.add("pom");
    
    GridData gd_typeText = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    gd_typeText.widthHint = 120;
    typeCombo.setLayoutData(gd_typeText);
    typeCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    toolkit.adapt(typeCombo, true, true);

    Label scopeLabel = toolkit.createLabel(dependencyComposite, "Scope:", SWT.NONE);
    scopeLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(scopeLabel);

    scopeCombo = new CCombo(dependencyComposite, SWT.READ_ONLY | SWT.FLAT);
    scopeCombo.add("compile");
    scopeCombo.add("test");
    scopeCombo.add("provided");
    scopeCombo.add("runtime");
    scopeCombo.add("system");
    // TODO should be only used on a dependency of type pom in the <dependencyManagement> section
    scopeCombo.add("import"); 
    
    GridData gd_scopeText = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    gd_scopeText.widthHint = 120;
    scopeCombo.setLayoutData(gd_scopeText);
    scopeCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    toolkit.adapt(scopeCombo, true, true);

    Label systemPathLabel = toolkit.createLabel(dependencyComposite, "System Path:", SWT.NONE);
    systemPathLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(systemPathLabel);

    systemPathText = toolkit.createText(dependencyComposite, null, SWT.NONE);
    GridData gd_systemPathText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_systemPathText.widthHint = 200;
    systemPathText.setLayoutData(gd_systemPathText);

    toolkit.createButton(dependencyComposite, "Select...", SWT.NONE);
    new Label(dependencyComposite, SWT.NONE);

    optionalButton = toolkit.createButton(dependencyComposite, "Optional", SWT.CHECK);
    optionalButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));

    Section exclusionsSection = toolkit.createSection(dependencyDetailsComposite, Section.TITLE_BAR);
    exclusionsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    exclusionsSection.setText("Exclusions");
    exclusionsSection.marginWidth = 3;

    exclusionsListEditor = new ListEditorComposite<Exclusion>(exclusionsSection, SWT.NONE);
    exclusionsSection.setClient(exclusionsListEditor);
    toolkit.adapt(exclusionsListEditor);

    exclusionsListEditor.setContentProvider(new ListEditorContentProvider<Exclusion>());
    exclusionsListEditor.setLabelProvider(new DependencyLabelProvider());
    
    exclusionsListEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Exclusion> selection = exclusionsListEditor.getSelection();
        updateExclusionDetails(selection.size()==1 ? selection.get(0) : null);
      }
    });

    exclusionsListEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        ExclusionsType exclusions = currentDependency.getExclusions();
        if(exclusions==null) {
          exclusions = PomFactory.eINSTANCE.createExclusionsType();
          Command setExclusionsCommand = SetCommand.create(editingDomain, currentDependency, 
              POM_PACKAGE.getDependency_Exclusions(), exclusions);
          compoundCommand.append(setExclusionsCommand);
//          currentDependency.setExclusions(exclusions);
        }
        
        Exclusion exclusion = PomFactory.eINSTANCE.createExclusion();
        Command addCommand = AddCommand.create(editingDomain, exclusions, 
            POM_PACKAGE.getExclusion(), exclusion);
        compoundCommand.append(addCommand);
//        exclusions.getExclusion().add(exclusion);
        
        editingDomain.getCommandStack().execute(compoundCommand);
//        updateExclusionDetails(exclusion);
        
        exclusionsListEditor.setSelection(Collections.singletonList(exclusion));
        updateExclusionDetails(exclusion);
        exclusionGroupIdText.setFocus();
      }
    });

    exclusionsListEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        ExclusionsType exclusions = currentDependency.getExclusions();
        int n = 0;
        for(Exclusion exclusion : exclusionsListEditor.getSelection()) {
          Command removeCommand = RemoveCommand.create(editingDomain, exclusions, 
              POM_PACKAGE.getExclusion(), exclusion);
          compoundCommand.append(removeCommand);
          n++;
        }
        if(exclusions.getExclusion().size()-n==0) {
          Command removeExclusions = SetCommand.create(editingDomain, currentDependency, 
              POM_PACKAGE.getDependency_Exclusions(), null);
          compoundCommand.append(removeExclusions);
          // currentDependency.setExclusions(exclusions);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
        updateExclusionDetails(null);
      }
    });
  }

  private void createExclusionDetailsSection(FormToolkit toolkit, Composite dependencyDetailsComposite) {
    exclusionDetailsSection = toolkit.createSection(dependencyDetailsComposite, Section.TITLE_BAR);
    exclusionDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    exclusionDetailsSection.setText("Exclusion Details");

    Composite exclusionDetailsComposite = toolkit.createComposite(exclusionDetailsSection, SWT.NONE);
    GridLayout gridLayout = new GridLayout(3, false);
    gridLayout.marginHeight = 2;
    gridLayout.marginWidth = 2;
    exclusionDetailsComposite.setLayout(gridLayout);
    toolkit.paintBordersFor(exclusionDetailsComposite);
    exclusionDetailsSection.setClient(exclusionDetailsComposite);
    exclusionDetailsComposite.addControlListener(detailsWidthGroup);
    
    Label exclusionGroupIdLabel = toolkit.createLabel(exclusionDetailsComposite, "Group Id:", SWT.NONE);
    exclusionGroupIdLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(exclusionGroupIdLabel);

    exclusionGroupIdText = toolkit.createText(exclusionDetailsComposite, null, SWT.NONE);
    exclusionGroupIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Button exclusionSelectButton = toolkit.createButton(exclusionDetailsComposite, "Select...", SWT.NONE);
    exclusionSelectButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 2));
    exclusionSelectButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        // TODO calculate current list of artifacts for the project
        Set<Dependency> artifacts = Collections.emptySet();
        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
            "Add Dependency", IndexManager.SEARCH_ARTIFACT, artifacts);
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            exclusionGroupIdText.setText(nvl(af.group));
            exclusionArtifactIdText.setText(nvl(af.artifact));
          }
        }
      }
    });

    Label exclusionArtifactIdLabel = toolkit.createLabel(exclusionDetailsComposite, "Artifact Id:", SWT.NONE);
    exclusionArtifactIdLabel.setLayoutData(new GridData());
    detailsWidthGroup.addControl(exclusionArtifactIdLabel);

    exclusionArtifactIdText = toolkit.createText(exclusionDetailsComposite, null, SWT.NONE);
    exclusionArtifactIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  }

  protected void updateDependencyDetails(Dependency dependency) {
    this.currentDependency = dependency;
    
    if(parent != null) {
      parent.removeNotifyListener(groupIdText);
      parent.removeNotifyListener(artifactIdText);
      parent.removeNotifyListener(versionText);
      parent.removeNotifyListener(classifierText);
      parent.removeNotifyListener(typeCombo);
      parent.removeNotifyListener(scopeCombo);
      parent.removeNotifyListener(systemPathText);
      parent.removeNotifyListener(optionalButton);
      // TODO exclusions listener
    }

    if(parent == null || dependency == null) {
      FormUtils.setEnabled(dependencyDetailsComposite, false);

      groupIdText.setText("");
      artifactIdText.setText("");
      versionText.setText("");
      classifierText.setText("");
      typeCombo.setText("");
      scopeCombo.setText("");
      systemPathText.setText("");

      optionalButton.setSelection(false);

      exclusionsListEditor.setSelection(Collections.<Exclusion>emptyList());
      exclusionsListEditor.setInput(null);
      
      updateExclusionDetails(null);
      
      return;
    }

    FormUtils.setEnabled(dependencyDetailsComposite, true);

    setText(groupIdText, dependency.getGroupId());
    setText(artifactIdText, dependency.getArtifactId());
    setText(versionText, dependency.getVersion());
    setText(classifierText, dependency.getClassifier());
    setText(typeCombo, dependency.getType());
    setText(scopeCombo, dependency.getScope());
    setText(systemPathText, dependency.getSystemPath());

    if(optionalButton.getSelection()!="true".equals(dependency.getOptional())) {
      optionalButton.setSelection("true".equals(dependency.getOptional()));
    }

    ExclusionsType exclusions = dependency.getExclusions();
    exclusionsListEditor.setInput(exclusions == null ? null : exclusions.getExclusion());
    exclusionsListEditor.setSelection(Collections.<Exclusion>emptyList());
    
    // set new listeners
    ValueProvider<Dependency> dependencyProvider = new ValueProvider.DefaultValueProvider<Dependency>(dependency); 
    parent.setModifyListener(groupIdText, dependencyProvider, POM_PACKAGE.getDependency_GroupId(), "");
    parent.setModifyListener(artifactIdText, dependencyProvider, POM_PACKAGE.getDependency_ArtifactId(), "");
    parent.setModifyListener(versionText, dependencyProvider, POM_PACKAGE.getDependency_Version(), "");
    parent.setModifyListener(classifierText, dependencyProvider, POM_PACKAGE.getDependency_Classifier(), "");
    parent.setModifyListener(typeCombo, dependencyProvider, POM_PACKAGE.getDependency_Type(), "jar");
    parent.setModifyListener(scopeCombo, dependencyProvider, POM_PACKAGE.getDependency_Scope(), "compile");
    parent.setModifyListener(systemPathText, dependencyProvider, POM_PACKAGE.getDependency_SystemPath(), "");
    parent.setModifyListener(optionalButton, dependencyProvider, POM_PACKAGE.getDependency_Optional(), "false");
    // TODO exclusions listener
    
    parent.registerListeners();
  }

  private void updateExclusionDetails(Exclusion exclusion) {
    if(parent != null) {
      parent.removeNotifyListener(exclusionGroupIdText);
      parent.removeNotifyListener(exclusionArtifactIdText);
    }
    
    if(parent == null || exclusion == null) {
      FormUtils.setEnabled(exclusionDetailsSection, false);

      exclusionGroupIdText.setText("");
      exclusionArtifactIdText.setText("");
      return;
    }
    
    FormUtils.setEnabled(exclusionDetailsSection, true);
    
    setText(exclusionGroupIdText, exclusion.getGroupId());
    setText(exclusionArtifactIdText, exclusion.getArtifactId());
    
    ValueProvider<Exclusion> exclusionProvider = new ValueProvider.DefaultValueProvider<Exclusion>(exclusion);
    parent.setModifyListener(exclusionGroupIdText, exclusionProvider, POM_PACKAGE.getExclusion_GroupId(), "");
    parent.setModifyListener(exclusionArtifactIdText, exclusionProvider, POM_PACKAGE.getExclusion_ArtifactId(), "");
    
    parent.registerListeners();
  }

  public void loadData(MavenPomEditorPage editorPage) {
    parent = editorPage;
    model = editorPage.getModel();
    loadDependencies(model);
    loadDependencyManagement(model);
  }

  private void loadDependencyManagement(Model model) {
    dependencyManagement = model.getDependencyManagement();
    if(dependencyManagement != null) {
      Dependencies dependencies = dependencyManagement.getDependencies();
      dependencyManagementListEditor.setInput(dependencies == null ? null : dependencies.getDependency());
    } else {
      dependencyManagementListEditor.setInput(null);
    }
  }

  private void loadDependencies(Model model) {
    Dependencies dependencies = model.getDependencies();
    dependenciesListEditor.setInput(dependencies == null ? null : dependencies.getDependency());
  }

  public void updateView(MavenPomEditorPage editorPage, Notification notification) {
    EObject object = (EObject) notification.getNotifier();
    
    // XXX event is not received when <dependencies> is deleted in XML
    if(object instanceof Dependencies) {
    	// handle add/remove
    	Dependencies dependencies = (Dependencies) object;
    	if (model.getDependencies() == dependencies) {
    	  // dependencies updated
 	      List<Dependency> selection = getUpdatedSelection(dependencies, dependenciesListEditor.getSelection());
 	      loadDependencies(model);
 	      dependenciesListEditor.setSelection(selection);
        updateDependencyDetails(selection.size()==1 ? selection.get(0) : null);
    	} else {
    		// dependencyManagement updated
        List<Dependency> selection = dependencyManagementListEditor.getSelection();
        getUpdatedSelection(dependencies, selection);
        loadDependencyManagement(model);
        dependencyManagementListEditor.setSelection(selection);
        updateDependencyDetails(selection.size()==1 ? selection.get(0) : null);
    	}
    }
    
    // XXX handle <dependencyManagement> changes
    
    if(object instanceof Dependency) {
      // handle modification
    	if (object == currentDependency) {
        updateDependencyDetails((Dependency) object);
      }
    	
      loadDependencies(model);
      loadDependencyManagement(model);
    }
    
    ExclusionsType exclusions = currentDependency==null ? null : currentDependency.getExclusions();
    if(object instanceof ExclusionsType) {
      if(exclusions == object) {
        exclusionsListEditor.setInput(exclusions==null ? null : exclusions.getExclusion());
      }
      updateDependencyDetails(currentDependency);
    }
    
    if(object instanceof Exclusion) {
      updateExclusionDetails((Exclusion) object);
      exclusionsListEditor.setInput(exclusions==null ? null : exclusions.getExclusion());
    }
  }

  private List<Dependency> getUpdatedSelection(Dependencies dependencies, List<Dependency> selection) {
    List<Dependency> newSelection = new ArrayList<Dependency>();
    for(Dependency dependency : selection) {
      if (dependencies.getDependency().contains(dependency)) {
        newSelection.add(dependency);
      }
    }
    return newSelection;
  }

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

