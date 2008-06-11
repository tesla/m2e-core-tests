/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.composites;

import static org.maven.ide.eclipse.editor.pom.FormUtils.nvl;

import java.util.ArrayList;
import java.util.List;

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
import org.maven.ide.eclipse.editor.pom.EMFEditorPage;
import org.maven.ide.eclipse.editor.pom.FormUtils;


/**
 * @author Eugene Kuleshov
 */
public class DependenciesComposite extends Composite {

  private Text exclusionArtifactIdText;
  private Text exclusionGroupIdText;
  private FormToolkit toolkit = new FormToolkit(Display.getCurrent());

  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;

  private ListEditorComposite<Dependency> dependencyManagementListEditor;

  private ListEditorComposite<Dependency> dependenciesListEditor;

  private Composite dependencyDetailsComposite;

  private Text groupIdText;

  private Text artifactIdText;

  private Text versionText;

  private Text classifierText;

  private CCombo scopeText;

  private CCombo typeText;

  private Text systemPathText;

  private Button optionalButton;

  private ListEditorComposite<Exclusion> exclusionsListEditor;

  protected Model model;

  protected EMFEditorPage parent; 

  private DependencyManagement dependencyManagement;

  private Dependency currentDependency;


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

    createDependencyDetails(toolkit, horizontalSash);

    horizontalSash.setWeights(new int[] {1, 1});
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
        
        List<Dependency> selection = new ArrayList<Dependency>();
        selection.add(dependency);
        dependenciesListEditor.setSelection(selection);
        updateDependencyDetails(dependency);
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
        List<Dependency> selection = new ArrayList<Dependency>();
        selection.add(dependency);
        dependencyManagementListEditor.setSelection(selection);
        updateDependencyDetails(dependency);
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

  private void createDependencyDetails(FormToolkit toolkit, Composite composite) {
    dependencyDetailsComposite = new Composite(composite, SWT.NONE);
    GridLayout gridLayout = new GridLayout(1, false);
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    dependencyDetailsComposite.setLayout(gridLayout);
    toolkit.adapt(dependencyDetailsComposite, true, true);

    Section dependencyDetailsSection = toolkit.createSection(dependencyDetailsComposite, Section.TITLE_BAR);
    dependencyDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    dependencyDetailsSection.setText("Dependency Details");
    dependencyDetailsSection.marginWidth = 3;

    Composite dependencyComposite = toolkit.createComposite(dependencyDetailsSection, SWT.NONE);
    GridLayout dependencyCompositeLayout = new GridLayout(2, false);
    dependencyCompositeLayout.marginWidth = 2;
    dependencyCompositeLayout.marginHeight = 2;
    dependencyComposite.setLayout(dependencyCompositeLayout);
    toolkit.paintBordersFor(dependencyComposite);
    dependencyDetailsSection.setClient(dependencyComposite);

    Label groupIdLabel = toolkit.createLabel(dependencyComposite, "Group Id:", SWT.NONE);
    groupIdLabel.setLayoutData(new GridData());

    groupIdText = toolkit.createText(dependencyComposite, null, SWT.NONE);
    groupIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label artifactIdLabel = toolkit.createLabel(dependencyComposite, "Artifact Id:", SWT.NONE);
    artifactIdLabel.setLayoutData(new GridData());

    artifactIdText = toolkit.createText(dependencyComposite, null, SWT.NONE);
    artifactIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label versionLabel = toolkit.createLabel(dependencyComposite, "Version:", SWT.NONE);
    versionLabel.setLayoutData(new GridData());

    versionText = toolkit.createText(dependencyComposite, null, SWT.NONE);
    GridData gd_versionText = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    gd_versionText.widthHint = 200;
    versionText.setLayoutData(gd_versionText);

    Label classifierLabel = toolkit.createLabel(dependencyComposite, "Classifier:", SWT.NONE);
    classifierLabel.setLayoutData(new GridData());

    classifierText = toolkit.createText(dependencyComposite, null, SWT.NONE);
    GridData gd_classifierText = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    gd_classifierText.widthHint = 200;
    classifierText.setLayoutData(gd_classifierText);

    Label typeLabel = toolkit.createLabel(dependencyComposite, "Type:", SWT.NONE);
    typeLabel.setLayoutData(new GridData());

    typeText = new CCombo(dependencyComposite, SWT.FLAT);
    typeText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    GridData gd_typeText = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    gd_typeText.widthHint = 120;
    typeText.setLayoutData(gd_typeText);
    toolkit.adapt(typeText, true, true);

    Label scopeLabel = toolkit.createLabel(dependencyComposite, "Scope:", SWT.NONE);
    scopeLabel.setLayoutData(new GridData());

    scopeText = new CCombo(dependencyComposite, SWT.READ_ONLY | SWT.FLAT);
    scopeText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    GridData gd_scopeText = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    gd_scopeText.widthHint = 120;
    scopeText.setLayoutData(gd_scopeText);
    toolkit.adapt(scopeText, true, true);

    Label systemPathLabel = toolkit.createLabel(dependencyComposite, "System Path:", SWT.NONE);
    systemPathLabel.setLayoutData(new GridData());

    systemPathText = toolkit.createText(dependencyComposite, null, SWT.NONE);
    GridData gd_systemPathText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_systemPathText.widthHint = 200;
    systemPathText.setLayoutData(gd_systemPathText);
    new Label(dependencyComposite, SWT.NONE);

    optionalButton = toolkit.createButton(dependencyComposite, "Optional", SWT.CHECK);
    optionalButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

    Section exclusionsSection = toolkit.createSection(dependencyDetailsComposite, Section.TITLE_BAR);
    exclusionsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    exclusionsSection.setText("Exclusions");
    exclusionsSection.marginWidth = 3;

    exclusionsListEditor = new ListEditorComposite<Exclusion>(exclusionsSection, SWT.NONE);
    exclusionsSection.setClient(exclusionsListEditor);
    toolkit.adapt(exclusionsListEditor);

    exclusionsListEditor.setContentProvider(new ListEditorContentProvider<Exclusion>());
    exclusionsListEditor.setLabelProvider(new DependencyLabelProvider());

    exclusionsListEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        // TODO Auto-generated method stub

      }
    });

    exclusionsListEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        // TODO Auto-generated method stub

      }
    });

    Section exclusionDetailsSection = toolkit.createSection(dependencyDetailsComposite, Section.TITLE_BAR);
    exclusionDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    exclusionDetailsSection.setText("Exclusion Details");

    Composite exclusionDetailsComposite = toolkit.createComposite(exclusionDetailsSection, SWT.NONE);
    GridLayout gridLayout_1 = new GridLayout();
    gridLayout_1.numColumns = 2;
    exclusionDetailsComposite.setLayout(gridLayout_1);
    toolkit.paintBordersFor(exclusionDetailsComposite);
    exclusionDetailsSection.setClient(exclusionDetailsComposite);

    Label exclusionGroupIdLabel = toolkit.createLabel(exclusionDetailsComposite, "Group Id:", SWT.NONE);
    GridData gd_exclusionGroupIdLabel = new GridData();
    exclusionGroupIdLabel.setLayoutData(gd_exclusionGroupIdLabel);

    exclusionGroupIdText = toolkit.createText(exclusionDetailsComposite, null, SWT.NONE);
    GridData gd_exclusionGroupIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    exclusionGroupIdText.setLayoutData(gd_exclusionGroupIdText);

    Label exclusionArtifactIdLabel = toolkit.createLabel(exclusionDetailsComposite, "Artifact Id:", SWT.NONE);
    GridData gd_exclusionArtifactIdLabel = new GridData();
    exclusionArtifactIdLabel.setLayoutData(gd_exclusionArtifactIdLabel);

    exclusionArtifactIdText = toolkit.createText(exclusionDetailsComposite, null, SWT.NONE);
    GridData gd_exclusionArtifactIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    exclusionArtifactIdText.setLayoutData(gd_exclusionArtifactIdText);

    updateDependencyDetails(null);
  }

  protected void updateDependencyDetails(Dependency dependency) {
    if (parent == null)
      return;
    
    //remove listeners first
    parent.removeNotifyListener(groupIdText);
    parent.removeNotifyListener(artifactIdText);
    parent.removeNotifyListener(versionText);
    parent.removeNotifyListener(classifierText);
    parent.removeNotifyListener(typeText);
    parent.removeNotifyListener(scopeText);
    parent.removeNotifyListener(systemPathText);
    parent.removeNotifyListener(optionalButton);
    //TODO: exclusions listener

    //set data
    if(dependency == null) {
      FormUtils.setEnabled(dependencyDetailsComposite, false);

      groupIdText.setText("");
      artifactIdText.setText("");
      versionText.setText("");
      classifierText.setText("");
      typeText.setText("");
      scopeText.setText("");
      systemPathText.setText("");

      optionalButton.setSelection(false);

      exclusionsListEditor.setInput(null);
      return;
    }

    FormUtils.setEnabled(dependencyDetailsComposite, true);

    groupIdText.setText(nvl(dependency.getGroupId()));
    artifactIdText.setText(nvl(dependency.getArtifactId()));
    versionText.setText(nvl(dependency.getVersion()));
    classifierText.setText(nvl(dependency.getClassifier()));
    typeText.setText(nvl(dependency.getType()));
    scopeText.setText(nvl(dependency.getScope()));
    systemPathText.setText(nvl(dependency.getSystemPath()));

    optionalButton.setSelection("true".equals(dependency.getOptional()));

    ExclusionsType exclusions = dependency.getExclusions();
    exclusionsListEditor.setInput(exclusions == null ? null : exclusions.getExclusion());
    this.currentDependency = dependency;

    //put new listeners in
    parent.setModifyListener(groupIdText, dependency, POM_PACKAGE.getDependency_GroupId());
    parent.setModifyListener(artifactIdText, dependency, POM_PACKAGE.getDependency_ArtifactId());
    parent.setModifyListener(versionText, dependency, POM_PACKAGE.getDependency_Version());
    parent.setModifyListener(classifierText, dependency, POM_PACKAGE.getDependency_Classifier());
    parent.setModifyListener(typeText, dependency, POM_PACKAGE.getDependency_Type());
    parent.setModifyListener(scopeText, dependency, POM_PACKAGE.getDependency_Scope());
    parent.setModifyListener(systemPathText, dependency, POM_PACKAGE.getDependency_SystemPath());
    parent.addModifyListener(optionalButton, dependency, POM_PACKAGE.getDependency_Optional(), true);
    //TODO: exclusions listener
  }

  public void loadData(EMFEditorPage editorPage) {
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

  public void updateView(EMFEditorPage editorPage, Notification notification) {
    EObject object = (EObject) notification.getNotifier();
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
    
    if(object instanceof Dependency) {
      // handle modification
    	if (object == currentDependency) {
        updateDependencyDetails((Dependency) object);
      }
    	
      loadDependencies(model);
      loadDependencyManagement(model);
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

}

