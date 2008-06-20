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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.PomPackage;
import org.maven.ide.components.pom.ReportPlugin;
import org.maven.ide.components.pom.ReportSet;
import org.maven.ide.components.pom.ReportSetsType;
import org.maven.ide.components.pom.Reporting;
import org.maven.ide.components.pom.StringReports;
import org.maven.ide.eclipse.actions.OpenPomAction;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.editor.pom.FormUtils;
import org.maven.ide.eclipse.editor.pom.MavenPomEditorPage;


/**
 * @author Eugene Kuleshov
 */
public class ReportingComposite extends Composite {

  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;
  
  private MavenPomEditorPage parent;
  
  private FormToolkit toolkit = new FormToolkit(Display.getCurrent());

  private Text outputFolderText;


  private Text groupIdText;

  private Text artifactIdText;

  private Text versionText;

  private Button pluginInheritedButton;

  private Hyperlink pluginConfigureButton;

  private ListEditorComposite<ReportSet> reportSetsEditor;

  private ListEditorComposite<String> reportsEditor;

  private ListEditorComposite<ReportPlugin> reportPluginsEditor;

  private Section pluginDetailsSection;

  private Button reportSetInheritedButton;

  private Hyperlink reportSetConfigureButton;

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

    Composite verticalSash = toolkit.createComposite(horizontalSash);
    GridLayout reportingPluginDetailsLayout = new GridLayout();
    reportingPluginDetailsLayout.marginWidth = 0;
    reportingPluginDetailsLayout.marginHeight = 0;
    verticalSash.setLayout(reportingPluginDetailsLayout);
    
    createPluginDetailsSection(verticalSash);
    createReportSetDetails(verticalSash);

    horizontalSash.setWeights(new int[] {1, 1});
  }

  private void createContentSection(SashForm horizontalSash) {

    Composite composite_1 = toolkit.createComposite(horizontalSash, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    composite_1.setLayout(gridLayout);
    toolkit.paintBordersFor(composite_1);
    Section contentSection = toolkit.createSection(composite_1, Section.TITLE_BAR);
    contentSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    contentSection.setText("Content");
  
    Composite composite = toolkit.createComposite(contentSection, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));
    contentSection.setClient(composite);
    toolkit.paintBordersFor(composite);
  
    toolkit.createLabel(composite, "Output Folder:", SWT.NONE);
  
    outputFolderText = toolkit.createText(composite, null, SWT.NONE);
    outputFolderText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    excludeDefaultsButton = toolkit.createButton(composite, "Exclude Defaults", SWT.CHECK);
    excludeDefaultsButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));

    Section reportingPluginsSection = toolkit.createSection(composite_1, Section.TITLE_BAR);
    reportingPluginsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
    reportingPluginsSection.setText("Reporting Plugins");
  
    reportPluginsEditor = new ListEditorComposite<ReportPlugin>(reportingPluginsSection, SWT.NONE);
    reportingPluginsSection.setClient(reportPluginsEditor);
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

  private void createPluginDetailsSection(Composite verticalSash) {
    
    // XXX implement editor actions
  }

  private void createReportSetDetails(Composite verticalSash) {
    pluginDetailsSection = toolkit.createSection(verticalSash, Section.TITLE_BAR);
    pluginDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    pluginDetailsSection.setText("Reporting Plugin Details");
  
    Composite pluginDetailsComposite = toolkit.createComposite(pluginDetailsSection, SWT.NONE);
    GridLayout gridLayout_1 = new GridLayout(2, false);
    gridLayout_1.marginWidth = 2;
    gridLayout_1.marginHeight = 2;
    pluginDetailsComposite.setLayout(gridLayout_1);
    pluginDetailsSection.setClient(pluginDetailsComposite);
    toolkit.paintBordersFor(pluginDetailsComposite);
  
    toolkit.createLabel(pluginDetailsComposite, "Group Id:*", SWT.NONE);
  
    groupIdText = toolkit.createText(pluginDetailsComposite, null, SWT.NONE);
    groupIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
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
    artifactIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    toolkit.createLabel(pluginDetailsComposite, "Version:", SWT.NONE);
  
    versionText = toolkit.createText(pluginDetailsComposite, null, SWT.NONE);
    versionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    Composite pluginConfigureComposite = toolkit.createComposite(pluginDetailsComposite, SWT.NONE);
    GridData gd_pluginConfigureComposite = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1);
    pluginConfigureComposite.setLayoutData(gd_pluginConfigureComposite);
    GridLayout gridLayout_2 = new GridLayout(2, false);
    gridLayout_2.marginWidth = 0;
    gridLayout_2.marginHeight = 0;
    pluginConfigureComposite.setLayout(gridLayout_2);
    toolkit.paintBordersFor(pluginConfigureComposite);
  
    pluginInheritedButton = toolkit.createButton(pluginConfigureComposite, "Inherited", SWT.CHECK);
    pluginInheritedButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
  
    pluginConfigureButton = toolkit.createHyperlink(pluginConfigureComposite, "Configuration", SWT.NONE);
    pluginConfigureButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    Section reportSetsSection = toolkit.createSection(verticalSash, Section.TITLE_BAR);
    reportSetsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    reportSetsSection.setText("Report Sets");
  
    reportSetsEditor = new ListEditorComposite<ReportSet>(reportSetsSection, SWT.NONE);
    reportSetsSection.setClient(reportSetsEditor);
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
    reportSetDetailsSection = toolkit.createSection(verticalSash, Section.TITLE_BAR);
    reportSetDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    reportSetDetailsSection.setText("Report Set Reports");

    Composite reportSetDetailsComposite = toolkit.createComposite(reportSetDetailsSection, SWT.NONE);
    GridLayout gridLayout = new GridLayout(1, false);
    gridLayout.marginWidth = 1;
    gridLayout.marginHeight = 1;
    reportSetDetailsComposite.setLayout(gridLayout);
    reportSetDetailsSection.setClient(reportSetDetailsComposite);
    toolkit.paintBordersFor(reportSetDetailsComposite);

    reportsEditor = new ListEditorComposite<String>(reportSetDetailsComposite, SWT.NONE);
    reportsEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    toolkit.paintBordersFor(reportsEditor);
    toolkit.adapt(reportsEditor);
    
    reportsEditor.setContentProvider(new ListEditorContentProvider<String>());
    reportsEditor.setLabelProvider(new StringLabelProvider(MavenEditorImages.IMG_REPORT));

    Composite reportSetConfigureComposite = toolkit.createComposite(reportSetDetailsComposite, SWT.NONE);
    reportSetConfigureComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
    GridLayout reportSetConfigureCompositeLayout = new GridLayout();
    reportSetConfigureCompositeLayout.numColumns = 2;
    reportSetConfigureCompositeLayout.marginWidth = 0;
    reportSetConfigureCompositeLayout.marginHeight = 0;
    reportSetConfigureComposite.setLayout(reportSetConfigureCompositeLayout);
    toolkit.paintBordersFor(reportSetConfigureComposite);

    reportSetInheritedButton = toolkit.createButton(reportSetConfigureComposite, "Inherited", SWT.CHECK);
    reportSetInheritedButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

    reportSetConfigureButton = toolkit.createHyperlink(reportSetConfigureComposite, "Configuration", SWT.NONE);
    reportSetConfigureButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

    // XXX implement editor actions
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
    FormUtils.setReadonly(pluginDetailsSection, parent.isReadOnly());
    
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
      reportSetInheritedButton.setSelection(false);
      reportsEditor.setInput(null);
      return;
    }

    FormUtils.setEnabled(reportSetDetailsSection, true);
    FormUtils.setReadonly(reportSetDetailsSection, parent.isReadOnly());
    
    reportSetInheritedButton.setSelection("true".equals(reportSet.getInherited()));
    
    StringReports reports = reportSet.getReports();
    reportsEditor.setInput(reports==null ? null : reports.getReport());
    
    reportPluginsEditor.setReadOnly(parent.isReadOnly());
    reportSetsEditor.setReadOnly(parent.isReadOnly());
    reportsEditor.setReadOnly(parent.isReadOnly());
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

  public void updateView(MavenPomEditorPage editorPage, Notification notification) {
    // XXX implement notification andling
  
  }
  
}
