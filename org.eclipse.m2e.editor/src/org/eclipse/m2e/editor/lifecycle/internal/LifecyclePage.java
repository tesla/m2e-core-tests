/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.lifecycle.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.internal.ExtensionReader;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.editor.lifecycle.ILifecycleMappingEditorContribution;
import org.eclipse.m2e.editor.lifecycle.MojoExecutionData;
import org.eclipse.m2e.editor.lifecycle.generic.GenericLifecycleMappingEditorContribution;
import org.eclipse.m2e.editor.pom.FormUtils;
import org.eclipse.m2e.editor.pom.IPomFileChangedListener;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.editor.pom.MavenPomEditorPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.progress.UIJob;


public class LifecyclePage extends MavenPomEditorPage implements IMavenProjectChangedListener, IPomFileChangedListener {
  public static final String[] EXEC_TABLE_COLUMN_PROPERTIES = new String[]{ "name", "incremental" }; //$NON-NLS-1$ //$NON-NLS-2$
  public static final String EXTENSION_LIFECYLE_EDITOR = "org.eclipse.m2e.editor.lifecycleMappingEditorContribution"; //$NON-NLS-1$
  public static final String ELEMENT_LIFECYCLE_EDITOR = "editContributor"; //$NON-NLS-1$
  private static final String FORM_NAME = Messages.LifecyclePage_form_name;

  private final MavenPomEditor pomEditor;

  private CCombo cmbLifecycleType;
  private TableViewer tblConfiguratorsTable;
  private TableViewer tblExecutionsTable;
  
  private Button btnAddConfigurator;
  private Button btnEditConfigurator;
  private Button btnRemoveConfigurator;
  
  private Button btnEnableMojo;
  private Button btnDisableMojo;
  
  private String[] lifecycleNames;
  private String[] lifecycleIds;
  
  private IMavenProjectFacade projectFacade;
  private Map<String, ILifecycleMappingEditorContribution> contributions = new HashMap<String, ILifecycleMappingEditorContribution>();
  private Map<String, ILifecycleMapping> mappings;
  private ExecutionsTableContentProvider executionsContentProvider;
  
  private ILifecycleMappingEditorContribution currentContribution;
  
  public LifecyclePage(MavenPomEditor pomEditor) {
    super(pomEditor, IMavenConstants.PLUGIN_ID + ".pom.lifecycleMappings", FORM_NAME); //$NON-NLS-1$
    this.pomEditor = pomEditor;
    
    //Read in our contributors
    mappings = new HashMap<String, ILifecycleMapping>(ExtensionReader.readLifecycleMappingExtensions());
    lifecycleNames = new String[mappings.size()];
    lifecycleIds = new String[mappings.size()];
    int i = 0;
    for(Map.Entry<String, ILifecycleMapping> mapping : mappings.entrySet()) {
      lifecycleIds[i] = mapping.getKey();
      lifecycleNames[i] = mapping.getValue().getName();
      i++;
    }
    

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_LIFECYLE_EDITOR);
    if(configuratorsExtensionPoint != null) {
      IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
      for(IExtension extension : configuratorExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_LIFECYCLE_EDITOR)) {
            try {
              Object o = element.createExecutableExtension(AbstractProjectConfigurator.ATTR_CLASS);
              
              ILifecycleMappingEditorContribution contributor = (ILifecycleMappingEditorContribution) o;
              contributions.put(element.getAttribute("editorFor"), contributor); //$NON-NLS-1$

            } catch(CoreException ex) {
              MavenLogger.log(ex);
            }
          }
        }
      }
    }

  }

  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();
    ScrolledForm form = managedForm.getForm();
    form.setText(Messages.LifecyclePage_scrolledForm);

    Composite body = form.getBody();
    GridLayout gridLayout = new GridLayout(1, true);
    gridLayout.verticalSpacing = 7;
    body.setLayout(gridLayout);
    toolkit.paintBordersFor(body);
    
    Composite topComposite = toolkit.createComposite(body, SWT.NONE);
    topComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    GridLayout topCompositeLayout = new GridLayout();
    topCompositeLayout.marginWidth = 0;
    topCompositeLayout.marginHeight = 0;
    topComposite.setLayout(topCompositeLayout);
    
    buildTopSection(topComposite, toolkit);
    
    Composite bottomComposite = toolkit.createComposite(body, SWT.NONE);
    bottomComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    GridLayout bottomCompositeLayout = new GridLayout();
    bottomCompositeLayout.marginWidth = 0;
    bottomCompositeLayout.marginHeight = 0;
    bottomComposite.setLayout(bottomCompositeLayout);
    
    buildBottomSection(bottomComposite, toolkit);
    
    toolkit.paintBordersFor(topComposite);
    toolkit.paintBordersFor(bottomComposite);
    
    MavenPlugin.getDefault().getMavenProjectManager().addMavenProjectChangedListener(this);
    super.createFormContent(managedForm);
  }
  
private void buildTopSection(Composite parent, FormToolkit toolkit) {
    
    Section topSection = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
    topSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    topSection.setText(Messages.LifecyclePage_section_mapping);
  
    Composite topComposite = toolkit.createComposite(topSection, SWT.NONE);
    toolkit.adapt(topComposite);
    GridLayout gridLayout = new GridLayout(1, false);
    gridLayout.marginBottom = 5;
    gridLayout.marginHeight = 2;
    gridLayout.marginWidth = 1;
    topComposite.setLayout(gridLayout);
    topSection.setClient(topComposite);
  
    cmbLifecycleType = new CCombo(topComposite, SWT.FLAT | SWT.READ_ONLY);
    cmbLifecycleType.setItems(lifecycleNames);
    toolkit.adapt(cmbLifecycleType, true, true);
    
    GridData cmbLayout = new GridData(SWT.FILL, SWT.TOP, true, false);
    cmbLayout.widthHint = 250;
    cmbLifecycleType.setLayoutData(cmbLayout);
    
    cmbLifecycleType.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    cmbLifecycleType.setData("name", "lifecycle"); //$NON-NLS-1$ //$NON-NLS-2$
    toolkit.paintBordersFor(cmbLifecycleType);
    
    cmbLifecycleType.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent arg0) {
        handleLifecycleSelect(cmbLifecycleType.getSelectionIndex(), false);
      }
      public void widgetSelected(SelectionEvent arg0) {
        handleLifecycleSelect(cmbLifecycleType.getSelectionIndex(), true);
      }
    });
    
    toolkit.paintBordersFor(topComposite);
  }

  private void buildBottomSection(Composite parent, FormToolkit toolkit) {
    Section bottomSection = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
    bottomSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    bottomSection.setText(Messages.LifecyclePage_section_details);
  
    Composite bottomComposite = toolkit.createComposite(bottomSection, SWT.NONE);
    toolkit.adapt(bottomComposite);
    GridLayout gridLayout = new GridLayout(4, false);
    gridLayout.marginWidth = 1;
    gridLayout.marginHeight = 1;
    gridLayout.verticalSpacing = 1;
    bottomComposite.setLayout(gridLayout);
    bottomSection.setClient(bottomComposite);
    
    final Table confTable = toolkit.createTable(bottomComposite, SWT.FLAT | SWT.MULTI);
    final TableColumn column = new TableColumn(confTable, SWT.NONE);
    confTable.addControlListener(new ControlAdapter() {
      public void controlResized(ControlEvent e) {
        column.setWidth(confTable.getClientArea().width);
      }
    });
    column.setText(Messages.LifecyclePage_column_name);
    GridData viewerData = new GridData( SWT.FILL, SWT.FILL, true, true, 1, 1);
    viewerData.widthHint = 100;
    viewerData.heightHint = 125;
    viewerData.minimumHeight = 125;
    confTable.setLayoutData(viewerData);
    confTable.setHeaderVisible(true);
    
    tblConfiguratorsTable = new TableViewer(confTable);
    ConfiguratorsTableContentProvider cp = new ConfiguratorsTableContentProvider();
    tblConfiguratorsTable.setContentProvider(cp);
    tblConfiguratorsTable.setLabelProvider(cp);
    tblConfiguratorsTable.setColumnProperties(new String[] { "Name" });
    tblConfiguratorsTable.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.TRUE);
    
    Composite configuratorActions = toolkit.createComposite(bottomComposite, SWT.NONE);
    toolkit.adapt(configuratorActions);
    configuratorActions.setLayout(new GridLayout(1, true));
    configuratorActions.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
    btnAddConfigurator = toolkit.createButton(configuratorActions, Messages.LifecyclePage_btnAdd, SWT.FLAT);
    btnAddConfigurator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    btnAddConfigurator.setEnabled(false);
    btnAddConfigurator.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent arg0) { }
      public void widgetSelected(SelectionEvent arg0) {
        addConfigurator();
      }
    });
    btnEditConfigurator = toolkit.createButton(configuratorActions, Messages.LifecyclePage_btnEdit, SWT.FLAT);
    btnEditConfigurator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    btnEditConfigurator.setEnabled(false);
    btnEditConfigurator.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent arg0) { }
      public void widgetSelected(SelectionEvent arg0) {
        editConfigurator();
      }
    });
    btnRemoveConfigurator = toolkit.createButton(configuratorActions, Messages.LifecyclePage_btnRemove, SWT.FLAT);
    btnRemoveConfigurator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    btnRemoveConfigurator.setEnabled(false);
    btnRemoveConfigurator.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent arg0) { }
      public void widgetSelected(SelectionEvent arg0) {
        removeConfigurator();
      }
    });
    tblConfiguratorsTable.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        updateConfiguratorButtons();
      }
    });
    
    final Table execTable = toolkit.createTable(bottomComposite, SWT.SINGLE | SWT.FLAT | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
    TableLayout tableLayout = new TableLayout();
    tableLayout.addColumnData(new ColumnWeightData(100, true));
    tableLayout.addColumnData(new ColumnPixelData(25, false));
    execTable.setLayout(tableLayout);
    viewerData = new GridData( SWT.FILL, SWT.FILL, true, true, 1, 1);
    viewerData.widthHint = 300;
    viewerData.heightHint = 125;
    viewerData.minimumHeight = 125;
    execTable.setLayoutData(viewerData);
    execTable.setHeaderVisible(true);
    execTable.setLinesVisible(true);
    
    tblExecutionsTable = new TableViewer(execTable);
    tblExecutionsTable.setUseHashlookup(true);
    TableViewerColumn nameColumn = new TableViewerColumn(tblExecutionsTable, SWT.LEFT);
    nameColumn.getColumn().setText(Messages.LifecyclePage_column_name);
    final TableViewerColumn incrColumn = new TableViewerColumn(tblExecutionsTable, SWT.LEFT);
    incrColumn.getColumn().setText(Messages.LifecyclePage_column_incremental);
    
    executionsContentProvider = new ExecutionsTableContentProvider();
    tblExecutionsTable.setContentProvider(executionsContentProvider);
    tblExecutionsTable.setLabelProvider(executionsContentProvider);
    tblExecutionsTable.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.TRUE);
    tblExecutionsTable.setColumnProperties(EXEC_TABLE_COLUMN_PROPERTIES);
    
    execTable.addMouseListener(new MouseAdapter(){
      public void mouseUp(MouseEvent event) {
        TableItem item = execTable.getItem(new Point(event.x, event.y));
        Rectangle imgRect = item.getImageBounds(1);
        if(imgRect.contains(event.x, event.y)) {
          if(executionsContentProvider.canModify(item.getData(), EXEC_TABLE_COLUMN_PROPERTIES[1])) {
            executionsContentProvider.modify(item, EXEC_TABLE_COLUMN_PROPERTIES[1], !((MojoExecutionData)item.getData()).isRunOnIncrementalBuild());
          }
        }
      }
    });
    
    Composite executionActions = toolkit.createComposite(bottomComposite, SWT.NONE);
    toolkit.adapt(executionActions);
    executionActions.setLayout(new GridLayout(1, true));
    executionActions.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
    btnEnableMojo = toolkit.createButton(executionActions, Messages.LifecyclePage_btnEnable, SWT.BORDER);
    btnEnableMojo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    btnEnableMojo.setEnabled(false);
    btnEnableMojo.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent arg0) { }
      public void widgetSelected(SelectionEvent arg0) {
        enableMojo();
      }
    });
    btnDisableMojo = toolkit.createButton(executionActions, Messages.LifecyclePage_btnDisable, SWT.BORDER);
    btnDisableMojo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    btnDisableMojo.setEnabled(false);
    btnDisableMojo.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent arg0) { }
      public void widgetSelected(SelectionEvent arg0) {
        disableMojo();
      }
    });
    
    tblExecutionsTable.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        updateExecutionButtons();
      }
    });
    
    toolkit.paintBordersFor(executionActions);
    toolkit.paintBordersFor(configuratorActions);
    toolkit.paintBordersFor(bottomComposite);
   
  }

  void loadData(final boolean force) {
    try {
      IProjectConfigurationManager configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
      projectFacade = MavenPlugin.getDefault().getMavenProjectManager().create(pomEditor.getPomFile(), true, new NullProgressMonitor());
      ILifecycleMapping selectedLifecycleMapping = configurationManager.getLifecycleMapping(projectFacade, new NullProgressMonitor());
      int i = Arrays.asList(lifecycleNames).indexOf(selectedLifecycleMapping.getName());
      cmbLifecycleType.select(i);
      handleLifecycleSelect(i, false);
    } catch(CoreException e) {
      MavenLogger.log(e);
    }
  }
  
  private void updateConfiguratorButtons() {
    try {
      btnAddConfigurator.setEnabled(false);
      btnEditConfigurator.setEnabled(false);
      btnRemoveConfigurator.setEnabled(false);
      if(currentContribution != null) {
        if(currentContribution.canAddProjectConfigurator()) {
          btnAddConfigurator.setEnabled(true);
        }
        
        ISelection selection = tblConfiguratorsTable.getSelection();
        if(selection instanceof IStructuredSelection && !((IStructuredSelection)selection).isEmpty()) {
          AbstractProjectConfigurator selected = (AbstractProjectConfigurator)((IStructuredSelection)selection).getFirstElement();
          if(currentContribution.canEditProjectConfigurator(selected)) {
            btnEditConfigurator.setEnabled(true);
          }
          
          if(currentContribution.canRemoveProjectConfigurator(selected)) {
            btnRemoveConfigurator.setEnabled(true);
          }
        }
      }
    } catch(CoreException e) {
      MavenLogger.log(e);
    }
  }
  
  private void updateExecutionButtons() {
    try {
      btnEnableMojo.setEnabled(false);
      btnDisableMojo.setEnabled(false);
      if(currentContribution != null) {
        ISelection selection = tblExecutionsTable.getSelection();
        if(selection instanceof IStructuredSelection && !((IStructuredSelection)selection).isEmpty()) {
          MojoExecutionData selected = (MojoExecutionData)((IStructuredSelection)selection).getFirstElement();
          if(!selected.isEnabled() && currentContribution.canEnableMojoExecution(selected)) {
            btnEnableMojo.setEnabled(true);
          }
          
          if(selected.isEnabled() && currentContribution.canDisableMojoExecution(selected)) {
            btnDisableMojo.setEnabled(true);
          }
        } 
      }
    } catch(CoreException e) {
      MavenLogger.log(e);
    }
  }
  
  private void addConfigurator() {
    try {
      currentContribution.addProjectConfigurator();
      tblConfiguratorsTable.refresh();
    } catch(CoreException e) {
      MavenLogger.log(e);
    }
  }
  
  private void editConfigurator() {
    try {
      AbstractProjectConfigurator selected = (AbstractProjectConfigurator)((IStructuredSelection)tblConfiguratorsTable.getSelection()).getFirstElement();
      currentContribution.editProjectConfigurator(selected);
      tblConfiguratorsTable.refresh();
    } catch(CoreException e) {
      MavenLogger.log(e);
    }
  }
  
  private void removeConfigurator() {
    try {
      AbstractProjectConfigurator selected = (AbstractProjectConfigurator)((IStructuredSelection)tblConfiguratorsTable.getSelection()).getFirstElement();
      currentContribution.removeProjectConfigurator(selected);
      tblConfiguratorsTable.refresh();
    } catch(CoreException e) {
      MavenLogger.log(e);
    }
  }
  
  private void enableMojo() {
    try {
      MojoExecutionData selected = (MojoExecutionData)((IStructuredSelection)tblExecutionsTable.getSelection()).getFirstElement();
      currentContribution.enableMojoExecution(selected);
      tblExecutionsTable.refresh();
    } catch(CoreException e) {
      MavenLogger.log(e);
    }
  }
  
  private void disableMojo() {
    try {
      MojoExecutionData selected = (MojoExecutionData)((IStructuredSelection)tblExecutionsTable.getSelection()).getFirstElement();
      currentContribution.disableMojoExecution(selected);
      tblExecutionsTable.refresh();
    } catch(CoreException e) {
      MavenLogger.log(e);
    }
  }
  
  /**
   * Handle changing the XML when the user selects a mapping type.
   * @param i
   */
  private void handleLifecycleSelect(int i, boolean initialize) {
    if(i > -1) {
      String id = lifecycleIds[i];
      ILifecycleMappingEditorContribution contribution = null;
      if(contributions.containsKey(id)) {
        contribution = contributions.get(id);
      } else {
        ILifecycleMapping mapping = mappings.get(id);
        contribution = new GenericLifecycleMappingEditorContribution(mapping, id);
      }
      
      contribution.setSiteData(pomEditor, projectFacade, getModel());
      if(initialize) {
        try {
          contribution.initializeConfiguration();
        } catch(CoreException e) {
          MavenLogger.log(e);
        }
      }
      currentContribution = contribution;
      executionsContentProvider.setMavenProject(projectFacade);
      tblConfiguratorsTable.setInput(contribution);
      tblExecutionsTable.setInput(contribution);
      updateConfiguratorButtons();
      updateExecutionButtons();
    }
  }

  

  @Override
  public void dispose() {
    MavenPlugin.getDefault().getMavenProjectManager().removeMavenProjectChangedListener(this);

    super.dispose();
  }

  public void loadData() {
    loadData(true);
  }
  
  @Override
  public void updateView(Notification notification) {
  } 

  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    if(getManagedForm() == null || getManagedForm().getForm() == null)
      return;

    for(int i = 0; i < events.length; i++ ) {
      if(events[i].getSource().equals(((MavenPomEditor) getEditor()).getPomFile())) {
        // file has been changed. need to update graph  
        new UIJob(Messages.LifecyclePage_job_reloading) {
          public IStatus runInUIThread(IProgressMonitor monitor) {
            loadData();
            FormUtils.setMessage(getManagedForm().getForm(), null, IMessageProvider.WARNING);
            return Status.OK_STATUS;
          }
        }.schedule();
      }
    }
  }

  public void fileChanged() {
    if(getManagedForm() == null || getManagedForm().getForm() == null)
      return;

    new UIJob(Messages.LifecyclePage_job_reloading) {
      public IStatus runInUIThread(IProgressMonitor monitor) {
        FormUtils.setMessage(getManagedForm().getForm(), Messages.LifecyclePage_message_updating, IMessageProvider.WARNING);
        return Status.OK_STATUS;
      }
    }.schedule();
  }
}
