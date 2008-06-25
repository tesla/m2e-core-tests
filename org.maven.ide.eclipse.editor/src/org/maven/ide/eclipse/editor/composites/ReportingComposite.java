/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.composites;

import static org.maven.ide.eclipse.editor.pom.FormUtils.setText;

import java.util.Collections;
import java.util.List;

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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.maven.ide.components.pom.PomFactory;
import org.maven.ide.components.pom.PomPackage;
import org.maven.ide.components.pom.ReportPlugin;
import org.maven.ide.components.pom.ReportPlugins;
import org.maven.ide.components.pom.ReportSet;
import org.maven.ide.components.pom.ReportSetsType;
import org.maven.ide.components.pom.Reporting;
import org.maven.ide.components.pom.StringReports;
import org.maven.ide.eclipse.actions.OpenPomAction;
import org.maven.ide.eclipse.editor.MavenEditorImages;
import org.maven.ide.eclipse.editor.pom.FormUtils;
import org.maven.ide.eclipse.editor.pom.MavenPomEditorPage;
import org.maven.ide.eclipse.editor.pom.ValueProvider;


/**
 * @author Eugene Kuleshov
 * @author Dmitry Platonoff
 */
public class ReportingComposite extends Composite {

  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;

  private MavenPomEditorPage parent;
  
  private ValueProvider<Reporting> reportingProvider;

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

  private ReportPlugin currentReportPlugin = null;

  private ReportSet currentReportSet = null;

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

          String label = groupId == null ? "[unknown]" : groupId;
          label += " : " + (artifactId == null ? "[unknown]" : artifactId);
          if(version != null) {
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
        updateReportPluginDetails(selection.size() == 1 ? selection.get(0) : null);
      }
    });

    reportPluginsEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        boolean reportsCreated = false;

        Reporting reporting = reportingProvider.getValue();
        if(reporting == null) {
          reporting = reportingProvider.create(editingDomain, compoundCommand);
          reportsCreated = true;
        }

        ReportPlugins reportPlugins = reporting.getPlugins();
        if(reportPlugins == null) {
          reportPlugins = PomFactory.eINSTANCE.createReportPlugins();
          Command addReportPlugins = SetCommand.create(editingDomain, reporting, POM_PACKAGE.getReporting_Plugins(),
              reportPlugins);
          compoundCommand.append(addReportPlugins);
          reportsCreated = true;
        }

        ReportPlugin reportPlugin = PomFactory.eINSTANCE.createReportPlugin();
        Command addReportPlugin = AddCommand.create(editingDomain, reportPlugins,
            POM_PACKAGE.getReportPlugins_Plugin(), reportPlugin);
        compoundCommand.append(addReportPlugin);
        editingDomain.getCommandStack().execute(compoundCommand);

        if(reportsCreated) {
          updateContent(reporting);
        } else {
          updateReportPluginDetails(reportPlugin);
        }
        reportPluginsEditor.setSelection(Collections.singletonList(reportPlugin));
        groupIdText.setFocus();
      }
    });

    reportPluginsEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        Reporting reporting = reportingProvider.getValue();
        ReportPlugins reportPlugins = reporting == null ? null : reporting.getPlugins();
        if(reportPlugins != null) {
          List<ReportPlugin> pluginList = reportPluginsEditor.getSelection();
          for(ReportPlugin reportPlugin : pluginList) {
            Command removeCommand = RemoveCommand.create(editingDomain, reportPlugins, POM_PACKAGE
                .getReportPlugins_Plugin(), reportPlugin);
            compoundCommand.append(removeCommand);
          }

          editingDomain.getCommandStack().execute(compoundCommand);
          updateContent(reporting);
        }
      }
    });
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
    pluginConfigureButton.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        if(currentReportPlugin != null) {
          EObject element = currentReportPlugin.getConfiguration();
          parent.getPomEditor().showInSourceEditor(element == null ? currentReportPlugin : element);
        }
      }
    });

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
          return id == null || id.length() == 0 ? "[unknown]" : id;
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
        updateReportSetDetails(selection.size() == 1 ? selection.get(0) : null);
      }
    });

    reportSetsEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if(currentReportPlugin == null) {
          return;
        }

        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        boolean reportSetsCreated = false;

        ReportSetsType reportSets = currentReportPlugin.getReportSets();
        if(reportSets == null) {
          reportSets = PomFactory.eINSTANCE.createReportSetsType();
          Command addReportSets = SetCommand.create(editingDomain, currentReportPlugin, POM_PACKAGE
              .getReportPlugin_ReportSets(), reportSets);
          compoundCommand.append(addReportSets);
          reportSetsCreated = true;
        }

        ReportSet reportSet = PomFactory.eINSTANCE.createReportSet();
        Command addReportSet = AddCommand.create(editingDomain, reportSets, POM_PACKAGE.getReportSetsType_ReportSet(),
            reportSet);
        compoundCommand.append(addReportSet);
        editingDomain.getCommandStack().execute(compoundCommand);

        if(reportSetsCreated) {
          updateReportPluginDetails(currentReportPlugin);
        }
        reportSetsEditor.setSelection(Collections.singletonList(reportSet));
      }
    });

    reportSetsEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if(currentReportPlugin == null) {
          return;
        }

        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        ReportSetsType reportSets = currentReportPlugin.getReportSets();
        if(reportSets != null) {
          List<ReportSet> reportSetList = reportSetsEditor.getSelection();
          for(ReportSet reportSet : reportSetList) {
            Command removeCommand = RemoveCommand.create(editingDomain, reportSets, POM_PACKAGE
                .getReportSetsType_ReportSet(), reportSet);
            compoundCommand.append(removeCommand);
          }

          editingDomain.getCommandStack().execute(compoundCommand);
          updateReportPluginDetails(currentReportPlugin);
        }
      }
    });

    reportSetsEditor.setCellModifier(new ICellModifier() {
      public boolean canModify(Object element, String property) {
        return true;
      }

      public Object getValue(Object element, String property) {
        if(element instanceof ReportSet) {
          String id = ((ReportSet) element).getId();
          return id == null ? "" : id;
        }
        return element;
      }

      public void modify(Object element, String property, Object value) {
        EditingDomain editingDomain = parent.getEditingDomain();
        Command command = SetCommand.create(editingDomain, currentReportSet, POM_PACKAGE.getReportSet_Id(), value);
        editingDomain.getCommandStack().execute(command);
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

    reportsEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if(currentReportSet == null) {
          return;
        }

        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        boolean reportsCreated = false;

        StringReports reports = currentReportSet.getReports();
        if(reports == null) {
          reports = PomFactory.eINSTANCE.createStringReports();
          Command addReports = SetCommand.create(editingDomain, currentReportSet, POM_PACKAGE.getReportSet_Reports(),
              reports);
          compoundCommand.append(addReports);
          reportsCreated = true;
        }

        Command addReport = AddCommand.create(editingDomain, reports, POM_PACKAGE.getStringReports_Report(), "?");
        compoundCommand.append(addReport);
        editingDomain.getCommandStack().execute(compoundCommand);

        if(reportsCreated) {
          reportsEditor.setInput(reports == null ? null : reports.getReport());
        } else {
          reportsEditor.refresh();
        }
      }
    });

    reportsEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if(currentReportSet == null) {
          return;
        }

        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        StringReports reports = currentReportSet.getReports();
        if(reports != null) {
          List<String> reportList = reportsEditor.getSelection();
          for(String report : reportList) {
            Command removeCommand = RemoveCommand.create(editingDomain, reports, POM_PACKAGE.getStringReports_Report(),
                report);
            compoundCommand.append(removeCommand);
          }

          editingDomain.getCommandStack().execute(compoundCommand);
        }
      }
    });

    reportsEditor.setCellModifier(new ICellModifier() {
      public boolean canModify(Object element, String property) {
        return true;
      }

      public Object getValue(Object element, String property) {
        return element;
      }

      public void modify(Object element, String property, Object value) {
        EditingDomain editingDomain = parent.getEditingDomain();
        Command command = SetCommand.create(editingDomain, currentReportSet.getReports(), POM_PACKAGE
            .getStringReports_Report(), value, reportsEditor.getViewer().getTable().getSelectionIndex());
        editingDomain.getCommandStack().execute(command);
      }
    });

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
    reportSetConfigureButton.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        if(currentReportSet != null) {
          EObject element = currentReportSet.getConfiguration();
          parent.getPomEditor().showInSourceEditor(element == null ? currentReportSet : element);
        }
      }
    });

    // XXX implement editor actions
  }

  protected void updateReportPluginDetails(ReportPlugin reportPlugin) {
    currentReportPlugin = reportPlugin;

    if(parent != null) {
      parent.removeNotifyListener(groupIdText);
      parent.removeNotifyListener(artifactIdText);
      parent.removeNotifyListener(versionText);
      parent.removeNotifyListener(pluginInheritedButton);
    }

    if(parent == null || reportPlugin == null) {
      FormUtils.setEnabled(pluginDetailsSection, false);

      setText(groupIdText, "");
      setText(artifactIdText, "");
      setText(versionText, "");

      pluginInheritedButton.setSelection(false);

      reportSetsEditor.setInput(null);

      updateReportSetDetails(null);

      return;
    }

    FormUtils.setEnabled(pluginDetailsSection, true);
    FormUtils.setReadonly(pluginDetailsSection, parent.isReadOnly());

    setText(groupIdText, reportPlugin.getGroupId());
    setText(artifactIdText, reportPlugin.getArtifactId());
    setText(versionText, reportPlugin.getVersion());

    pluginInheritedButton.setSelection(Boolean.parseBoolean(reportPlugin.getInherited()));

    ValueProvider<ReportPlugin> provider = new ValueProvider.DefaultValueProvider<ReportPlugin>(reportPlugin);
    parent.setModifyListener(groupIdText, provider, POM_PACKAGE.getReportPlugin_GroupId(), "");
    parent.setModifyListener(artifactIdText, provider, POM_PACKAGE.getReportPlugin_ArtifactId(), "");
    parent.setModifyListener(versionText, provider, POM_PACKAGE.getReportPlugin_Version(), "");
    parent.setModifyListener(pluginInheritedButton, provider, POM_PACKAGE.getReportPlugin_Inherited(), "false");
    parent.registerListeners();

    ReportSetsType reportSets = reportPlugin.getReportSets();
    reportSetsEditor.setInput(reportSets == null ? null : reportSets.getReportSet());

    updateReportSetDetails(null);
  }

  protected void updateReportSetDetails(ReportSet reportSet) {
    if(parent != null) {
      parent.removeNotifyListener(reportSetInheritedButton);
    }

    currentReportSet = reportSet;

    if(reportSet == null || parent == null) {
      FormUtils.setEnabled(reportSetDetailsSection, false);
      reportSetInheritedButton.setSelection(false);
      reportsEditor.setInput(null);
      return;
    }

    FormUtils.setEnabled(reportSetDetailsSection, true);
    FormUtils.setReadonly(reportSetDetailsSection, parent.isReadOnly());

    reportSetInheritedButton.setSelection(Boolean.parseBoolean(reportSet.getInherited()));
    ValueProvider<ReportSet> provider = new ValueProvider.DefaultValueProvider<ReportSet>(reportSet);
    parent.setModifyListener(reportSetInheritedButton, provider, POM_PACKAGE.getReportSet_Inherited(), "false");
    parent.registerListeners();

    StringReports reports = reportSet.getReports();
    reportsEditor.setInput(reports == null ? null : reports.getReport());
  }

  public void loadData(MavenPomEditorPage editorPage,ValueProvider<Reporting> reportingProvider) {
    parent = editorPage;
    this.reportingProvider = reportingProvider;
    updateContent(reportingProvider.getValue());
  }

  private void updateContent(Reporting reporting) {
    if(reporting == null) {
      setText(outputFolderText,"");
      excludeDefaultsButton.setSelection(false);
      reportPluginsEditor.setInput(null);
    } else {
      setText(outputFolderText,reporting.getOutputDirectory());
      excludeDefaultsButton.setSelection(Boolean.parseBoolean(reporting.getExcludeDefaults()));
      reportPluginsEditor.setInput(reporting.getPlugins() == null ? null : reporting.getPlugins().getPlugin());
      
      parent.setModifyListener(outputFolderText, reportingProvider, POM_PACKAGE.getReporting_OutputDirectory(), "");
      parent.setModifyListener(excludeDefaultsButton, reportingProvider, POM_PACKAGE.getReporting_ExcludeDefaults(), "");
      parent.registerListeners();
    }
    updateReportPluginDetails(null);
  }

  public void updateView(MavenPomEditorPage editorPage, Notification notification) {
    EObject object = (EObject) notification.getNotifier();

    if(object instanceof Reporting || object instanceof ReportPlugins) {
      reportPluginsEditor.refresh();
    } else if(object instanceof ReportPlugin) {
      reportPluginsEditor.refresh();
      if(object == currentReportPlugin) {
        updateReportPluginDetails((ReportPlugin) object);
      }
    } else if(object instanceof ReportSetsType || object instanceof ReportSet) {
      reportSetsEditor.refresh();
    } else if(object instanceof StringReports) {
      reportsEditor.refresh();
    }
  }

}
