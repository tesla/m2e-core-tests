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
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.ReportPlugin;
import org.maven.ide.components.pom.ReportSet;
import org.maven.ide.components.pom.ReportSetsType;
import org.maven.ide.components.pom.Reporting;
import org.maven.ide.components.pom.StringReports;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.editor.pom.MavenPomEditorPage;
import org.maven.ide.eclipse.editor.pom.FormUtils;


/**
 * @author Eugene Kuleshov
 */
public class ReportingComposite extends Composite {

  private FormToolkit toolkit = new FormToolkit(Display.getCurrent());

  private Text outputFolderText;

  private Text reportSetIdText;

  private Text groupIdText;

  private Text artifactIdText;

  private Text versionText;

  private Button pluginInheritedButton;

  private Button pluginConfigureButton;

  private ListEditorComposite<ReportSet> reportSetsEditor;

  private ListEditorComposite<String> reportsEditor;

  private ListEditorComposite<ReportPlugin> reportPluginsEditor;

  private Section pluginDetailsSection;

  private Button reportSetInheritedButton;

  private Button reportSetConfigureButton;

  private Section reportSetDetailsSection;

  private Button excludeDefaultsButton;

  public ReportingComposite(Composite parent, int style) {
    super(parent, style);

    GridLayout gridLayout = new GridLayout(1, false);
    gridLayout.marginWidth = 0;
    setLayout(gridLayout);
    toolkit.adapt(this);

    SashForm horizontalSash = new SashForm(this, SWT.NONE);
    horizontalSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    toolkit.adapt(horizontalSash, true, true);

    createContentSection(horizontalSash);

    SashForm verticalSash = new SashForm(horizontalSash, SWT.VERTICAL);
    toolkit.adapt(verticalSash, true, true);

    createPluginDetailsSection(verticalSash);
    createReportSetDetails(verticalSash);
    
    verticalSash.setWeights(new int[] {215, 170});

    horizontalSash.setWeights(new int[] {1, 1});
  }

  private void createContentSection(SashForm horizontalSash) {
    Section contentSection = toolkit.createSection(horizontalSash, Section.TITLE_BAR);
    contentSection.setText("Content");
  
    Composite composite = toolkit.createComposite(contentSection, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));
    contentSection.setClient(composite);
    toolkit.paintBordersFor(composite);
  
    toolkit.createLabel(composite, "Output Folder:", SWT.NONE);
  
    outputFolderText = toolkit.createText(composite, null, SWT.NONE);
    outputFolderText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    excludeDefaultsButton = toolkit.createButton(composite, "Exclude Defaults", SWT.CHECK);
    excludeDefaultsButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
  
    Label label = toolkit.createLabel(composite, "Plugins:", SWT.NONE);
    label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
  
    reportPluginsEditor = new ListEditorComposite<ReportPlugin>(composite, SWT.NONE);
    reportPluginsEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 2, 1));
    toolkit.paintBordersFor(reportPluginsEditor);
    toolkit.adapt(reportPluginsEditor);
    
    reportPluginsEditor.setContentProvider(new ListEditorContentProvider<ReportPlugin>());
    reportPluginsEditor.setLabelProvider(new LabelProvider() {
      public String getText(Object element) {
        if(element instanceof ReportPlugin) {
          ReportPlugin reportPlugin = (ReportPlugin) element;

          String groupId = reportPlugin.getGroupId();
          String artifactId = reportPlugin.getArtifactId();
          String version = reportPlugin.getVersion();

          String label = groupId==null ? "[unknown]" : groupId;
          label += " : " + (artifactId==null ? "[unknown]" : artifactId);
          if(version!=null) {
            label += " : " + version;
          }
          return label;
        }
        return super.getText(element);
      }
      public Image getImage(Object element) {
        return MavenEditorImages.IMG_PLUGIN;
      }
    });
    
    reportPluginsEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<ReportPlugin> selection = reportPluginsEditor.getSelection();
        updateReportPluginDetails(selection.size()==1 ? selection.get(0) : null);
      }
    });
    
    // XXX implement actions
  }

  private void createPluginDetailsSection(SashForm verticalSash) {
    pluginDetailsSection = toolkit.createSection(verticalSash, Section.TITLE_BAR);
    pluginDetailsSection.setText("Plugin Details");
  
    Composite pluginDetailsComposite = toolkit.createComposite(pluginDetailsSection, SWT.NONE);
    pluginDetailsComposite.setLayout(new GridLayout(2, false));
    pluginDetailsSection.setClient(pluginDetailsComposite);
    toolkit.paintBordersFor(pluginDetailsComposite);
  
    toolkit.createLabel(pluginDetailsComposite, "Group Id:", SWT.NONE);
  
    groupIdText = toolkit.createText(pluginDetailsComposite, null, SWT.NONE);
    groupIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    toolkit.createLabel(pluginDetailsComposite, "Artifact Id:", SWT.NONE);
  
    artifactIdText = toolkit.createText(pluginDetailsComposite, null, SWT.NONE);
    artifactIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    toolkit.createLabel(pluginDetailsComposite, "Version:", SWT.NONE);
  
    versionText = toolkit.createText(pluginDetailsComposite, null, SWT.NONE);
    versionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    Composite pluginConfigureComposite = toolkit.createComposite(pluginDetailsComposite, SWT.NONE);
    GridData gd_pluginConfigureComposite = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    pluginConfigureComposite.setLayoutData(gd_pluginConfigureComposite);
    GridLayout gridLayout_2 = new GridLayout(2, false);
    gridLayout_2.marginWidth = 0;
    gridLayout_2.marginHeight = 0;
    pluginConfigureComposite.setLayout(gridLayout_2);
    toolkit.paintBordersFor(pluginConfigureComposite);
  
    pluginInheritedButton = toolkit.createButton(pluginConfigureComposite, "Inherited", SWT.CHECK);
    pluginInheritedButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
  
    pluginConfigureButton = toolkit.createButton(pluginConfigureComposite, "Configure...", SWT.NONE);
    pluginConfigureButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
  
    Label reportSetsLabel = toolkit.createLabel(pluginDetailsComposite, "Report Sets:", SWT.NONE);
    reportSetsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
  
    reportSetsEditor = new ListEditorComposite<ReportSet>(pluginDetailsComposite, SWT.NONE);
    reportSetsEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 2, 1));
    toolkit.paintBordersFor(reportSetsEditor);
    toolkit.adapt(reportSetsEditor);
  
    reportSetsEditor.setContentProvider(new ListEditorContentProvider<ReportSet>());
    reportSetsEditor.setLabelProvider(new LabelProvider() {
      public String getText(Object element) {
        if(element instanceof ReportSet) {
          ReportSet reportSet = (ReportSet) element;
          String id = reportSet.getId();
          return id==null || id.length()==0 ? "[unknown]" : id;
        }
        return "";
      }
      public Image getImage(Object element) {
        // TODO add icon for report set
        return null;
      }
    });
    
    reportSetsEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<ReportSet> selection = reportSetsEditor.getSelection();
        updateReportSetDetails(selection.size()==1 ? selection.get(0) : null);
      }
    });
  }

  private void createReportSetDetails(SashForm verticalSash) {
    reportSetDetailsSection = toolkit.createSection(verticalSash, Section.TITLE_BAR);
    reportSetDetailsSection.setText("Report Set Details");

    Composite reportSetDetailsComposite = toolkit.createComposite(reportSetDetailsSection, SWT.NONE);
    reportSetDetailsComposite.setLayout(new GridLayout(2, false));
    reportSetDetailsSection.setClient(reportSetDetailsComposite);
    toolkit.paintBordersFor(reportSetDetailsComposite);

    toolkit.createLabel(reportSetDetailsComposite, "Id:", SWT.NONE);

    reportSetIdText = toolkit.createText(reportSetDetailsComposite, null, SWT.NONE);
    reportSetIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Composite reportSetConfigureComposite = toolkit.createComposite(reportSetDetailsComposite, SWT.NONE);
    reportSetConfigureComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    GridLayout reportSetConfigureCompositeLayout = new GridLayout();
    reportSetConfigureCompositeLayout.numColumns = 2;
    reportSetConfigureCompositeLayout.marginWidth = 0;
    reportSetConfigureCompositeLayout.marginHeight = 0;
    reportSetConfigureComposite.setLayout(reportSetConfigureCompositeLayout);
    toolkit.paintBordersFor(reportSetConfigureComposite);

    reportSetInheritedButton = toolkit.createButton(reportSetConfigureComposite, "Inherited", SWT.CHECK);
    reportSetInheritedButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

    reportSetConfigureButton = toolkit.createButton(reportSetConfigureComposite, "Configure...", SWT.NONE);
    reportSetConfigureButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

    Label reportsLabel = toolkit.createLabel(reportSetDetailsComposite, "Reports:", SWT.NONE);
    reportsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

    reportsEditor = new ListEditorComposite<String>(reportSetDetailsComposite, SWT.NONE);
    reportsEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 2, 1));
    toolkit.paintBordersFor(reportsEditor);
    toolkit.adapt(reportsEditor);
    
    reportsEditor.setContentProvider(new ListEditorContentProvider<String>());
    reportsEditor.setLabelProvider(new StringLabelProvider(MavenEditorImages.IMG_REPORT));

    // XXX implement actions
  }

  protected void updateReportPluginDetails(ReportPlugin reportPlugin) {
    if(reportPlugin==null) {
      FormUtils.setEnabled(pluginDetailsSection, false);
      
      groupIdText.setText("");
      artifactIdText.setText("");
      versionText.setText("");

      pluginInheritedButton.setSelection(false);

      reportSetsEditor.setInput(null);
      
      updateReportSetDetails(null);
      
      return;
    }
    
    FormUtils.setEnabled(pluginDetailsSection, true);
    
    groupIdText.setText(nvl(reportPlugin.getGroupId()));
    artifactIdText.setText(nvl(reportPlugin.getArtifactId()));
    versionText.setText(nvl(reportPlugin.getVersion()));

    pluginInheritedButton.setSelection("true".equals(reportPlugin.getInherited()));

    ReportSetsType reportSets = reportPlugin.getReportSets();
    reportSetsEditor.setInput(reportSets == null ? null : reportSets.getReportSet());
    
    updateReportSetDetails(null);
  }

  protected void updateReportSetDetails(ReportSet reportSet) {
    if(reportSet==null) {
      FormUtils.setEnabled(reportSetDetailsSection, false);
      reportSetIdText.setText("");
      reportSetInheritedButton.setSelection(false);
      reportsEditor.setInput(null);
      return;
    }

    FormUtils.setEnabled(reportSetDetailsSection, true);
    
    reportSetIdText.setText(nvl(reportSet.getId()));
    reportSetInheritedButton.setSelection("true".equals(reportSet.getInherited()));
    
    StringReports reports = reportSet.getReports();
    reportsEditor.setInput(reports==null ? null : reports.getReport());
  }

  public void loadData(MavenPomEditorPage editorPage) {
    Model model = editorPage.getModel();
    updateContent(model.getReporting());
  }

  private void updateContent(Reporting reporting) {
    if(reporting==null) {
      outputFolderText.setText("");
      excludeDefaultsButton.setSelection(false);
      reportPluginsEditor.setInput(null);
    } else {
      outputFolderText.setText(nvl(reporting.getOutputDirectory()));
      excludeDefaultsButton.setSelection("true".equals(reporting.getExcludeDefaults()));
      reportPluginsEditor.setInput(reporting.getPlugins()==null ? null : reporting.getPlugins().getPlugin());
    }
    updateReportPluginDetails(null);
  }
  
}
