/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import static org.maven.ide.eclipse.editor.pom.FormUtils.nvl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.maven.ide.components.pom.Build;
import org.maven.ide.components.pom.Extension;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.editor.composites.BuildComposite;
import org.maven.ide.eclipse.editor.composites.DependencyLabelProvider;
import org.maven.ide.eclipse.editor.composites.ListEditorComposite;
import org.maven.ide.eclipse.editor.composites.ListEditorContentProvider;

/**
 * @author Eugene Kuleshov
 */
public class BuildPage extends MavenPomEditorPage {
  
  // controls
  private Text sourceText;
  private Text outputText;
  private Text testSourceText;
  private Text testOutputText;
  private Text scriptsSourceText;
  
  private ListEditorComposite<Extension> extensionsEditor;
  private BuildComposite buildComposite;
  
  public BuildPage(MavenPomEditor pomEditor) {
    super(pomEditor, MavenPlugin.PLUGIN_ID + ".pom.build", "Build (work in progress)");
  }

  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();
    ScrolledForm form = managedForm.getForm();
    form.setText("Overview");
    // form.setExpandHorizontal(true);
    
    Composite body = form.getBody();
    GridLayout gridLayout = new GridLayout(2, true);
    gridLayout.horizontalSpacing = 7;
    body.setLayout(gridLayout);
    toolkit.paintBordersFor(body);

    createFoldersSection(body, toolkit);
    createExtensionsSection(body, toolkit);

    buildComposite = new BuildComposite(body, SWT.NONE);
    GridData buildCompositeData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
    buildCompositeData.heightHint = 270;
    buildComposite.setLayoutData(buildCompositeData);
    toolkit.adapt(buildComposite);

//    form.pack();

    super.createFormContent(managedForm);
  }

  private void createFoldersSection(Composite body, FormToolkit toolkit) {
    Section foldersSection = toolkit.createSection(body, Section.TITLE_BAR);
    foldersSection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    foldersSection.setText("Folders");
  
    Composite composite = toolkit.createComposite(foldersSection, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));
    toolkit.paintBordersFor(composite);
    foldersSection.setClient(composite);
  
    toolkit.createLabel(composite, "Sources:", SWT.NONE);
  
    sourceText = toolkit.createText(composite, null, SWT.NONE);
    sourceText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    toolkit.createLabel(composite, "Output:", SWT.NONE);
  
    outputText = toolkit.createText(composite, null, SWT.NONE);
    outputText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    toolkit.createLabel(composite, "Test Sources:", SWT.NONE);
  
    testSourceText = toolkit.createText(composite, null, SWT.NONE);
    testSourceText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    toolkit.createLabel(composite, "Test Output:", SWT.NONE);
  
    testOutputText = toolkit.createText(composite, null, SWT.NONE);
    testOutputText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    toolkit.createLabel(composite, "Scripts:", SWT.NONE);
  
    scriptsSourceText = toolkit.createText(composite, null, SWT.NONE);
    scriptsSourceText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  }

  private void createExtensionsSection(Composite body, FormToolkit toolkit) {
    Section extensionsSection = toolkit.createSection(body, Section.TITLE_BAR);
    extensionsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    extensionsSection.setText("Extensions");

    extensionsEditor = new ListEditorComposite<Extension>(extensionsSection, SWT.NONE);
    toolkit.paintBordersFor(extensionsEditor);
    toolkit.adapt(extensionsEditor);
    extensionsSection.setClient(extensionsEditor);
    
    extensionsEditor.setContentProvider(new ListEditorContentProvider<Extension>());
    extensionsEditor.setLabelProvider(new DependencyLabelProvider());
  }

  public void loadData() {
    loadBuild(model.getBuild());
    buildComposite.loadData(this);
  }
  
  public void updateView(Notification notification) {
    // TODO Auto-generated method stub
    
  }
  
  private void loadBuild(Build build) {
    if(build==null) {
      sourceText.setText("");
      outputText.setText("");
      testSourceText.setText("");
      testOutputText.setText("");
      scriptsSourceText.setText("");
    } else {
      sourceText.setText(nvl(build.getSourceDirectory()));
      outputText.setText(nvl(build.getOutputDirectory()));
      testSourceText.setText(nvl(build.getTestSourceDirectory()));
      testOutputText.setText(nvl(build.getTestOutputDirectory()));
      scriptsSourceText.setText(nvl(build.getScriptSourceDirectory()));
    }
    
    extensionsEditor.setInput(build == null //
        || build.getExtensions() == null ? null : build.getExtensions().getExtension());
  }
  
}
