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
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.Resource;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.editor.pom.MavenPomEditorPage;
import org.maven.ide.eclipse.editor.pom.FormUtils;

/**
 * @author Eugene Kuleshov
 */
public class BuildComposite extends Composite {

  private FormToolkit toolkit = new FormToolkit(Display.getCurrent());
  
  // controls
  private Text defaultGoalText;
  private Text directoryText;
  private Text finalNameText;

  private ListEditorComposite<String> filtersEditor;

  private ListEditorComposite<Resource> resourcesEditor;
  private ListEditorComposite<Resource> testResourcesEditor;

  private Text resourceDirectoryText;
  private Text resourceTargetPathText;
  private ListEditorComposite<String> resourceIncludesEditor;
  private ListEditorComposite<String> resourceExcludesEditor;

  // model
  private Model model;


  private Build build;

  private Button resourceFilteringButton;

  private Section resourceDetailsSection;

  
  public BuildComposite(Composite parent, int flags) {
    super(parent, flags);
    
    toolkit.adapt(this);
  
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.makeColumnsEqualWidth = true;
    setLayout(layout);
  
    createBuildSection();
  }

  private void createBuildSection() {
    SashForm horizontalSash = new SashForm(this, SWT.NONE);
    toolkit.adapt(horizontalSash);
    horizontalSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    
    Section buildSection = toolkit.createSection(horizontalSash, Section.TITLE_BAR);
    buildSection.setText("Build");
  
    Composite composite = toolkit.createComposite(buildSection, SWT.NONE);
    GridLayout compositeLayout = new GridLayout(2, false);
    compositeLayout.marginWidth = 1;
    compositeLayout.marginHeight = 2;
    composite.setLayout(compositeLayout);
    toolkit.paintBordersFor(composite);
    buildSection.setClient(composite);
  
    toolkit.createLabel(composite, "Default Goal:", SWT.NONE);
  
    defaultGoalText = toolkit.createText(composite, null, SWT.NONE);
    defaultGoalText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    toolkit.createLabel(composite, "Directory:", SWT.NONE);
  
    directoryText = toolkit.createText(composite, null, SWT.NONE);
    directoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    toolkit.createLabel(composite, "Final Name:", SWT.NONE);
  
    finalNameText = toolkit.createText(composite, null, SWT.NONE);
    finalNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    Label filtersLabel = toolkit.createLabel(composite, "Filters:", SWT.NONE);
    filtersLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
  
    filtersEditor = new ListEditorComposite<String>(composite, SWT.NONE);
    GridData filtersEditorData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
    filtersEditorData.heightHint = 47;
    filtersEditor.setLayoutData(filtersEditorData);
    toolkit.adapt(filtersEditor);
    toolkit.paintBordersFor(filtersEditor);

    filtersEditor.setContentProvider(new ListEditorContentProvider<String>());
    filtersEditor.setLabelProvider(new StringLabelProvider(MavenEditorImages.IMG_FILTER));

    SashForm verticalSash = new SashForm(horizontalSash, SWT.VERTICAL);
    
    createResourceSection(verticalSash);
    createTestResourcesSection(verticalSash);
    
    verticalSash.setWeights(new int[] {1, 1 });
    
    resourceDetailsSection = toolkit.createSection(horizontalSash, Section.TITLE_BAR);
    resourceDetailsSection.setText("Resource Details");
  
    Composite resourceDetailsComposite = toolkit.createComposite(resourceDetailsSection, SWT.NONE);
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginWidth = 1;
    gridLayout.marginHeight = 2;
    resourceDetailsComposite.setLayout(gridLayout);
    toolkit.paintBordersFor(resourceDetailsComposite);
    resourceDetailsSection.setClient(resourceDetailsComposite);
  
    Label resourceDirectoryLabel = toolkit.createLabel(resourceDetailsComposite, "Directory:", SWT.NONE);
    resourceDirectoryLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
  
    resourceDirectoryText = toolkit.createText(resourceDetailsComposite, null, SWT.NONE);
    resourceDirectoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    Label resourceTargetPathLabel = toolkit.createLabel(resourceDetailsComposite, "Target Path:", SWT.NONE);
    resourceTargetPathLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
  
    resourceTargetPathText = toolkit.createText(resourceDetailsComposite, null, SWT.NONE);
    resourceTargetPathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    resourceFilteringButton = toolkit.createButton(resourceDetailsComposite, "filtering", SWT.CHECK);
    resourceFilteringButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
  
    Label includesLabel = toolkit.createLabel(resourceDetailsComposite, "Includes:", SWT.NONE);
    includesLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
  
    resourceIncludesEditor = new ListEditorComposite<String>(resourceDetailsComposite, SWT.NONE);
    GridData includesEditorData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
    includesEditorData.heightHint = 60;
    resourceIncludesEditor.setLayoutData(includesEditorData);
    toolkit.adapt(resourceIncludesEditor);
    toolkit.paintBordersFor(resourceIncludesEditor);
  
    resourceIncludesEditor.setContentProvider(new ListEditorContentProvider<String>());
    resourceIncludesEditor.setLabelProvider(new StringLabelProvider(MavenEditorImages.IMG_INCLUDE));
    
    Label excludesLabel = toolkit.createLabel(resourceDetailsComposite, "Excludes:", SWT.NONE);
    excludesLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
  
    resourceExcludesEditor = new ListEditorComposite<String>(resourceDetailsComposite, SWT.NONE);
    GridData excludesEditorData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
    excludesEditorData.heightHint = 60;
    resourceExcludesEditor.setLayoutData(excludesEditorData);
    toolkit.adapt(resourceExcludesEditor);
    toolkit.paintBordersFor(resourceExcludesEditor);
    
    resourceExcludesEditor.setContentProvider(new ListEditorContentProvider<String>());
    resourceExcludesEditor.setLabelProvider(new StringLabelProvider(MavenEditorImages.IMG_EXCLUDE));

    horizontalSash.setWeights(new int[] {1, 1, 1});
  }

  private void createResourceSection(SashForm verticalSash) {
    Section resourcesSection = toolkit.createSection(verticalSash, Section.TITLE_BAR);
    resourcesSection.setText("Resources");
  
    resourcesEditor = new ListEditorComposite<Resource>(resourcesSection, SWT.NONE);
    resourcesSection.setClient(resourcesEditor);
    toolkit.adapt(resourcesEditor);
    toolkit.paintBordersFor(resourcesEditor);
    
    resourcesEditor.setContentProvider(new ListEditorContentProvider<Resource>());
    resourcesEditor.setLabelProvider(new ResourceLabelProvider());
    
    resourcesEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Resource> selection = resourcesEditor.getSelection();
        loadResourceDetails(selection.size()==1 ? selection.get(0) : null);
      }
    });
    
    // XXX add actions
  }

  private void createTestResourcesSection(SashForm verticalSash) {
    Section testResourcesSection = toolkit.createSection(verticalSash, Section.TITLE_BAR);
    testResourcesSection.setText("Test Resources");
    toolkit.adapt(verticalSash, true, true);
    
    testResourcesEditor = new ListEditorComposite<Resource>(testResourcesSection, SWT.NONE);
    testResourcesSection.setClient(testResourcesEditor);
    toolkit.adapt(testResourcesEditor);
    toolkit.paintBordersFor(testResourcesEditor);

    testResourcesEditor.setContentProvider(new ListEditorContentProvider<Resource>());
    testResourcesEditor.setLabelProvider(new ResourceLabelProvider());

    testResourcesEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Resource> selection = testResourcesEditor.getSelection();
        loadResourceDetails(selection.size()==1 ? selection.get(0) : null);
      }
    });
    
    // XXX add actions
  }
  
  public void loadData(MavenPomEditorPage editorPage) {
    model = editorPage.getModel();
    build = model.getBuild();
    loadBuild(build);
    loadResources(build);
    loadTestResources(build);
    loadResourceDetails(null);
  }

  public void updateView(MavenPomEditorPage editorPage, Notification notification) {
    // XXX
  }
  
  private void loadBuild(Build build) {
    if(build!=null) {
      defaultGoalText.setText(nvl(build.getDefaultGoal()));
      directoryText.setText(nvl(build.getDirectory()));
      finalNameText.setText(nvl(build.getFinalName()));
    } else {
      defaultGoalText.setText("");
      directoryText.setText("");
      finalNameText.setText("");
    }
    
    filtersEditor.setInput(build == null //
        || build.getFilters() == null ? null : build.getFilters().getFilter());
  }
  
  private void loadResources(Build build) {
    resourcesEditor.setInput(build == null //
        || build.getResources() == null ? null : build.getResources().getResource());
  }
  
  private void loadTestResources(Build build) {
    testResourcesEditor.setInput(build == null //
        || build.getTestResources() == null ? null : build.getTestResources().getTestResource());
  }

  private void loadResourceDetails(Resource resource) {
    if(resource == null) {
      FormUtils.setEnabled(resourceDetailsSection, false);
      
      resourceDirectoryText.setText("");
      resourceTargetPathText.setText("");
      
      resourceFilteringButton.setSelection(false);
      
      resourceIncludesEditor.setInput(null);
      resourceExcludesEditor.setInput(null);
      
      return;
    }

    FormUtils.setEnabled(resourceDetailsSection, true);
    
    resourceDirectoryText.setText(nvl(resource.getDirectory()));
    resourceTargetPathText.setText(nvl(resource.getTargetPath()));
    
    resourceFilteringButton.setSelection("true".equals(resource.getFiltering()));
    
    resourceIncludesEditor.setInput(resource.getIncludes()==null ? null : resource.getIncludes().getInclude());
    resourceExcludesEditor.setInput(resource.getExcludes()==null ? null : resource.getExcludes().getExclude());
  }

  /**
   * Label provider for {@link Resource}
   */
  public class ResourceLabelProvider extends LabelProvider {

    public String getText(Object element) {
      if(element instanceof Resource) {
        return ((Resource) element).getDirectory();
      }
      return super.getText(element);
    }
    
    public Image getImage(Object element) {
      return MavenEditorImages.IMG_RESOURCE;
    }
    
  }
  
}
