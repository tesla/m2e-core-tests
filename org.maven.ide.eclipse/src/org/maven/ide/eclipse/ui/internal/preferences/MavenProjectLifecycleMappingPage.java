/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.preferences;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.ide.IDE;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.internal.project.GenericLifecycleMapping;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;

/**
 * Maven project preference page
 *
 * @author Dan Yocum
 */
public class MavenProjectLifecycleMappingPage extends PropertyPage {

  public static final String[] CONFIG_TABLE_COLUMN_PROPERTIES = new String[]{ "name", "id"};
  public static final String[] CONFIG_TABLE_COLUMN_NAMES = new String[]{ "Name", "Id"};
  public static final String DESC_STRING = "Maven lifecycle mapping strategy: ";
  public static final String GENERIC_STRATEGY = "Generic";
  private static final int TABLE_WIDTH = 500;
  private static final int TABLE_HEIGHT = 320;
  private Text goalsCleanText;
  private Text goalsChangedText;
  private TableViewer configuratorsTable;
  private Hyperlink pomEditorHyperlink;
  private ConfiguratorsTableContentProvider configuratorsContentProvider;
  private ConfiguratorsTableLabelProvider configuratorsLabelProvider;

  public MavenProjectLifecycleMappingPage() {
    setTitle("");
    
  }

  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));
    composite.setLayoutData(new GridData(GridData.FILL));
    if(!canSpecifyGoals() && !canShowConfigurators()){
      Label noInfoLabel = new Label(composite, SWT.NONE);
      noInfoLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
      noInfoLabel.setAlignment(SWT.CENTER);
      noInfoLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
      
      noInfoLabel.setText(getNoLifecycleInfoMsg());
    }
    if(canSpecifyGoals()){
      Label goalsCleanLabel = new Label(composite, SWT.NONE);
      GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
      gd.horizontalIndent=4;
      goalsCleanLabel.setLayoutData(gd);
      goalsCleanLabel.setText("Goals to invoke after project clea&n (may affect incremental build performance):");
  
      goalsCleanText = new Text(composite, SWT.BORDER);
      gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
      gd.horizontalIndent = 6;
      goalsCleanText.setLayoutData(gd);
  
      Button selectGoalsCleanButton = new Button(composite, SWT.NONE);
      selectGoalsCleanButton.setLayoutData(new GridData());
      selectGoalsCleanButton.setText("&Select...");
      selectGoalsCleanButton.addSelectionListener(new MavenGoalSelectionAdapter(goalsCleanText, getShell()));
      
      final Label goalsChangedLabel = new Label(composite, SWT.NONE);
      gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
      gd.horizontalIndent = 4;
      goalsChangedLabel.setLayoutData(gd);
      goalsChangedLabel.setText("&Goals to invoke on resource changes (may affect incremental build performance):");
      
      goalsChangedText = new Text(composite, SWT.SINGLE | SWT.BORDER);
      gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
      gd.horizontalIndent = 6;
      goalsChangedText.setLayoutData(gd);
      
      final Button selectGoalsChangedButton = new Button(composite, SWT.NONE);
      selectGoalsChangedButton.setText("S&elect...");
      selectGoalsChangedButton.addSelectionListener(new MavenGoalSelectionAdapter(goalsChangedText, getShell()));
    }
    if(canShowConfigurators()){
      
      Composite labelComp = new Composite(composite, SWT.NONE);
      GridLayout layout = new GridLayout(2, false);
      labelComp.setLayout(layout);
      GridData gd = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
      
      if(canSpecifyGoals()){
        gd.verticalIndent = 15;
      }
      gd.horizontalAlignment = SWT.LEFT;
      labelComp.setLayoutData(gd);
      
      Label configuratorsLabel = new Label(labelComp, SWT.NONE);
      gd = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
      configuratorsLabel.setLayoutData(gd);
      configuratorsLabel.setText("Project Configurators:");
      pomEditorHyperlink = new Hyperlink(labelComp, SWT.NONE);
      pomEditorHyperlink.setUnderlined(true);
      pomEditorHyperlink.addHyperlinkListener(new IHyperlinkListener() {
        
        public void linkExited(HyperlinkEvent e) {
        }
        
        public void linkEntered(HyperlinkEvent e) {
        }
        
        public void linkActivated(HyperlinkEvent e) {
          IFile pomFile = getProjectFacade().getPom();
          if(pomFile != null){
            if(performOk()){
              getShell().close();
              try{
                IEditorPart part = IDE.openEditor(MavenPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage(), getProjectFacade().getPom());
                if(part instanceof FormEditor){
                  ((FormEditor)part).setActivePage(IMavenConstants.PLUGIN_ID + ".pom.lifecycleMappings");
                }
              } catch(PartInitException pie){
                MavenLogger.log("Unable to open the POM file", pie);
              }
            }
          }
        }
      });
      pomEditorHyperlink.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
      pomEditorHyperlink.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
      pomEditorHyperlink.setText("View/Edit in POM Editor");
      
      configuratorsTable = new TableViewer(composite, SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL);
      TableViewerColumn nameColumn = new TableViewerColumn(configuratorsTable, SWT.LEFT);
      nameColumn.getColumn().setText(CONFIG_TABLE_COLUMN_NAMES[0]);
      nameColumn.getColumn().setWidth((int)(TABLE_WIDTH*.50));
      
      TableViewerColumn idColumn = new TableViewerColumn(configuratorsTable, SWT.LEFT);
      idColumn.getColumn().setText(CONFIG_TABLE_COLUMN_NAMES[1]);
      idColumn.getColumn().setWidth((int)(TABLE_WIDTH*.50));
      
      configuratorsTable.getTable().setHeaderVisible(true);
      configuratorsTable.getTable().setLinesVisible(true);
      configuratorsContentProvider = new ConfiguratorsTableContentProvider();
      configuratorsLabelProvider = new ConfiguratorsTableLabelProvider();
      configuratorsTable.setContentProvider(configuratorsContentProvider);
      configuratorsTable.setLabelProvider(configuratorsLabelProvider);
      configuratorsTable.setColumnProperties(CONFIG_TABLE_COLUMN_PROPERTIES);
      
      gd = new GridData(SWT.LEFT, SWT.TOP, true, true, 2, 1);
      gd.widthHint = TABLE_WIDTH;
      gd.heightHint = TABLE_HEIGHT;
      gd.horizontalIndent=6;
      configuratorsTable.getControl().setLayoutData(gd);
      //TODO: get the table/columns to resize
//      composite.addControlListener(new ControlAdapter(){
//        public void controlResized(ControlEvent e){
//          
//        }
//      });
//      final TableColumn nCol = nameColumn.getColumn();
//      final TableColumn iCol = idColumn.getColumn();
//      final Table tab = configuratorsTable.getTable();
//      configuratorsTable.getTable().addControlListener(new ControlAdapter() {
//        public void controlResized(ControlEvent e) {
//          nCol.setWidth((int)(tab.getClientArea().width*0.50));
//          iCol.setWidth((int)(tab.getClientArea().width*0.50));
//        }
//      });
    }
    init(getResolverConfiguration());
    return composite;
  }

  /**
   * @return
   */
  private String getNoLifecycleInfoMsg() {
    ILifecycleMapping mapping = getLifecycleMapping(getProjectFacade());
    if(mapping == null){
      return "No lifecycle mapping info to display";
    }
    return "No lifecycle mapping info to display for "+mapping.getName();
  }

  /**
   * @return
   */
  private boolean canShowConfigurators() {
    ILifecycleMapping mapping = getLifecycleMapping(getProjectFacade());
    if(mapping == null || !mapping.showConfigurators()){
      return false;
    }
    return true;
  }

  /**
   * @return
   */
  private boolean canSpecifyGoals() {
    ILifecycleMapping mapping = getLifecycleMapping(getProjectFacade());
    if(mapping == null || !(mapping instanceof GenericLifecycleMapping)){
      return false;
    }
    return true;
  }

  protected void performDefaults() {
    init(new ResolverConfiguration());
  }
  
  private void init(ResolverConfiguration configuration) {
    if(goalsCleanText != null){
      goalsCleanText.setText(configuration.getFullBuildGoals());
    }
    if(goalsChangedText != null){
      goalsChangedText.setText(configuration.getResourceFilteringGoals());
    }
    if(configuratorsTable != null){
      configuratorsTable.setInput(getLifecycleMapping(getProjectFacade()));
    }
    updateLifecycleTitle();
  }

  /**
   * @param lifecycleMapping
   */
  private void updateLifecycleTitle() {
    ILifecycleMapping lifecycleMapping = getLifecycleMapping(getProjectFacade());
    if(lifecycleMapping != null){
      setMessage(lifecycleMapping.getName());
    } else {
      setMessage("Generic Lifecycle Mapping");
    }
  }

  public boolean performOk() {
    final IProject project = getProject();
    try {
      if(!project.isAccessible() || !project.hasNature(IMavenConstants.NATURE_ID)) {
        return true;
      }
    } catch(CoreException ex) {
      MavenLogger.log(ex);
      return false;
    }

    if(canSpecifyGoals()){
      final ResolverConfiguration configuration = getResolverConfiguration();
      if(configuration.getFullBuildGoals().equals(goalsCleanText.getText()) &&
          configuration.getResourceFilteringGoals().equals(goalsChangedText.getText())) {
        return true;
      }
      
      configuration.setFullBuildGoals(goalsCleanText.getText());
      configuration.setResourceFilteringGoals(goalsChangedText.getText());
      
      MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
      boolean isSet = projectManager.setResolverConfiguration(getProject(), configuration);
      if(isSet) {
        
          boolean res = MessageDialog.openQuestion(getShell(), "Maven Settings", //
              "Maven settings have changed. Do you want to update project configuration?");
          if(res) {
            final MavenPlugin plugin = MavenPlugin.getDefault();
            WorkspaceJob job = new WorkspaceJob("Updating " + project.getName() + " Sources") {
              public IStatus runInWorkspace(IProgressMonitor monitor) {
                try {
                  final IMavenConfiguration mavenConfiguration = MavenPlugin.lookup(IMavenConfiguration.class);
                  plugin.getProjectConfigurationManager().updateProjectConfiguration(project, configuration,
                      mavenConfiguration.getGoalOnUpdate(), monitor);
                } catch(CoreException ex) {
                  return ex.getStatus();
                }
                return Status.OK_STATUS;
              }
            };
            job.setRule(plugin.getProjectConfigurationManager().getRule());
            job.schedule();
          }
      }
      
      return isSet;
    }
    return true;
  }

  
  protected ILifecycleMapping getLifecycleMapping(IMavenProjectFacade projectFacade){
    ILifecycleMapping lifecycleMapping = null;
    try{
      IProjectConfigurationManager configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
      lifecycleMapping = configurationManager.getLifecycleMapping(projectFacade, new NullProgressMonitor());
    } catch(CoreException ce){
      MavenLogger.log(ce);
      setErrorMessage("Unable to load lifecycle mapping for project.");
    }
    return lifecycleMapping;
  }
  
  private ResolverConfiguration getResolverConfiguration() {
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    return projectManager.getResolverConfiguration(getProject());
  }

  protected IProject getProject() {
    return (IProject) getElement();
  }
  
  protected IMavenProjectFacade getProjectFacade(){
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    return  projectManager.create(getProject(), new NullProgressMonitor());
  }
  /**
   * ConfiguratorsTableContentProvider
   *
   * @author dyocum
   */
  public class ConfiguratorsTableContentProvider implements IStructuredContentProvider {

    protected String[] getNoConfigMsg(){
      ILifecycleMapping mapping = getLifecycleMapping(getProjectFacade());
      if(mapping == null){
        return new String[]{"No Configurators"};
      } 
      return new String[]{"No Configurators for "+mapping.getName()};
    }
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    public Object[] getElements(Object inputElement) {
      
      ILifecycleMapping mapping = (ILifecycleMapping)inputElement;
      if(mapping == null){
        return getNoConfigMsg();
      }
      try{
        List<AbstractProjectConfigurator> projectConfigurators = mapping.getProjectConfigurators(getProjectFacade(), new NullProgressMonitor());
        if(projectConfigurators == null || projectConfigurators.size() == 0){
          return getNoConfigMsg();
        }
        return projectConfigurators.toArray();
      } catch(CoreException ce){
        MavenLogger.log("Unable to read project configurators", ce);
        return getNoConfigMsg();
      }
    
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {
      // TODO Auto-generated method dispose
      
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      // TODO Auto-generated method inputChanged
      
    }
  }
  
  class ConfiguratorsTableLabelProvider implements ITableLabelProvider, IColorProvider{

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
     */
    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
     */
    public String getColumnText(Object element, int columnIndex) {
      if(element == null){
        return "";
      } else if(element instanceof AbstractProjectConfigurator){
        return columnIndex == 0 ? ((AbstractProjectConfigurator)element).getName() : ((AbstractProjectConfigurator)element).getId();
      } 
      return columnIndex == 0 ? element.toString() : "";
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
     */
    public void addListener(ILabelProviderListener listener) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
     */
    public void dispose() {
 
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
     */
    public boolean isLabelProperty(Object element, String property) {
      // TODO Auto-generated method isLabelProperty
      return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
     */
    public void removeListener(ILabelProviderListener listener) {

    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
     */
    public Color getBackground(Object element) {
      return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
     */
    public Color getForeground(Object element) {
      if(element instanceof AbstractProjectConfigurator){
        return Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
      }
      return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
      
    }
    
  }
}

