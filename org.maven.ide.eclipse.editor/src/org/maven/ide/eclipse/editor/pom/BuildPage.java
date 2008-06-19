/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import static org.maven.ide.eclipse.editor.pom.FormUtils.nvl;

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.maven.ide.components.pom.Build;
import org.maven.ide.components.pom.Dependency;
import org.maven.ide.components.pom.Extension;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.actions.MavenRepositorySearchDialog;
import org.maven.ide.eclipse.actions.OpenPomAction;
import org.maven.ide.eclipse.editor.composites.BuildComposite;
import org.maven.ide.eclipse.editor.composites.DependencyLabelProvider;
import org.maven.ide.eclipse.editor.composites.ListEditorComposite;
import org.maven.ide.eclipse.editor.composites.ListEditorContentProvider;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifactFile;

/**
 * @author Eugene Kuleshov
 */
public class BuildPage extends MavenPomEditorPage {
  
  // controls
  private Text extensionGroupIdText;
  private Text sourceText;
  private Text outputText;
  private Text testSourceText;
  private Text testOutputText;
  private Text scriptsSourceText;
  
  private ListEditorComposite<Extension> extensionsEditor;
  private BuildComposite buildComposite;
  private Text extensionArtifactIdText;
  private Text extensionVersionText;
  private Button extensionSelectButton;
  
  public BuildPage(MavenPomEditor pomEditor) {
    super(pomEditor, MavenPlugin.PLUGIN_ID + ".pom.build", "Build");
  }

  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();
    ScrolledForm form = managedForm.getForm();
    form.setText("Build (work in progress)");
    // form.setExpandHorizontal(true);
    
    Composite body = form.getBody();
    GridLayout gridLayout = new GridLayout(3, true);
    gridLayout.horizontalSpacing = 3;
    body.setLayout(gridLayout);
    toolkit.paintBordersFor(body);

    SashForm buildSash = new SashForm(body, SWT.NONE);
    buildSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
    GridLayout buildSashLayout = new GridLayout();
    buildSashLayout.horizontalSpacing = 3;
    buildSashLayout.marginWidth = 0;
    buildSashLayout.marginHeight = 0;
    buildSashLayout.numColumns = 3;
    buildSash.setLayout(buildSashLayout);
    toolkit.adapt(buildSash);
    
    createFoldersSection(buildSash, toolkit);
    createExtensionsSection(buildSash, toolkit);
    createExtensionDetailsSection(buildSash, toolkit);

    buildComposite = new BuildComposite(body, SWT.NONE);
    GridData buildCompositeData = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
    buildCompositeData.heightHint = 270;
    buildComposite.setLayoutData(buildCompositeData);
    toolkit.adapt(buildComposite);

//    form.pack();

    super.createFormContent(managedForm);
  }

  private void createFoldersSection(Composite buildSash, FormToolkit toolkit) {
    Section foldersSection = toolkit.createSection(buildSash, Section.TITLE_BAR);
    foldersSection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    foldersSection.setText("Folders");
  
    Composite composite = toolkit.createComposite(foldersSection, SWT.NONE);
    GridLayout compositeLayout = new GridLayout(2, false);
    compositeLayout.marginWidth = 2;
    compositeLayout.marginHeight = 2;
    composite.setLayout(compositeLayout);
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

  private void createExtensionsSection(Composite buildSash, FormToolkit toolkit) {
    Section extensionsSection = toolkit.createSection(buildSash, Section.TITLE_BAR);
    extensionsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    extensionsSection.setText("Extensions");

    extensionsEditor = new ListEditorComposite<Extension>(extensionsSection, SWT.NONE);
    toolkit.paintBordersFor(extensionsEditor);
    toolkit.adapt(extensionsEditor);
    extensionsSection.setClient(extensionsEditor);
    
    extensionsEditor.setContentProvider(new ListEditorContentProvider<Extension>());
    extensionsEditor.setLabelProvider(new DependencyLabelProvider());
  }

  private void createExtensionDetailsSection(Composite buildSash, FormToolkit toolkit) {
    Section extensionDetailsSection = toolkit.createSection(buildSash, Section.TITLE_BAR);
    extensionDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    extensionDetailsSection.setText("Extension Details");

    Composite extensionDetialsComposite = toolkit.createComposite(extensionDetailsSection, SWT.NONE);
    GridLayout gridLayout_2 = new GridLayout(2, false);
    gridLayout_2.marginWidth = 2;
    gridLayout_2.marginHeight = 2;
    extensionDetialsComposite.setLayout(gridLayout_2);
    toolkit.paintBordersFor(extensionDetialsComposite);
    extensionDetailsSection.setClient(extensionDetialsComposite);

    toolkit.createLabel(extensionDetialsComposite, "Group Id:*");
    
    extensionGroupIdText = toolkit.createText(extensionDetialsComposite, null, SWT.FLAT);
    extensionGroupIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    
    Hyperlink extensionArtifactIdHyperlink = toolkit.createHyperlink(extensionDetialsComposite, "Artifact Id:*", SWT.NONE);
    extensionArtifactIdHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        final String groupId = extensionGroupIdText.getText();
        final String artifactId = extensionArtifactIdText.getText();
        final String version = extensionVersionText.getText();
        new Job("Opening " + groupId + ":" + artifactId + ":" + version) {
          protected IStatus run(IProgressMonitor arg0) {
            OpenPomAction.openEditor(groupId, artifactId, version);
            return Status.OK_STATUS;
          }
        }.schedule();
      }
    });
    
    extensionArtifactIdText = toolkit.createText(extensionDetialsComposite, null, SWT.FLAT);
    extensionArtifactIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    
    toolkit.createLabel(extensionDetialsComposite, "Version:");
    
    extensionVersionText = toolkit.createText(extensionDetialsComposite, null, SWT.FLAT);
    extensionVersionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    extensionSelectButton = toolkit.createButton(extensionDetialsComposite, "Select...", SWT.FLAT);
    extensionSelectButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));
    extensionSelectButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        // TODO calculate current list of artifacts for the project
        Set<Dependency> artifacts = Collections.emptySet();
        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getEditorSite().getShell(), //
            "Add Dependency", IndexManager.SEARCH_ARTIFACT, artifacts);
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            extensionGroupIdText.setText(nvl(af.group));
            extensionArtifactIdText.setText(nvl(af.artifact));
            extensionVersionText.setText(nvl(af.version));
          }
        }
      }
    });
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
