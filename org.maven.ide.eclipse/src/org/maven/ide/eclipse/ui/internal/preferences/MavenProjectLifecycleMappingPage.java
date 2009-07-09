/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.preferences;

import java.util.List;

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
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
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

  public static final String[] CONFIG_TABLE_COLUMN_PROPERTIES = new String[]{ "configurator"};
  public static final String DESC_STRING = "Maven lifecycle mapping strategy: ";
  public static final String GENERIC_STRATEGY = "Generic";
  private static final int TABLE_WIDTH = 500;
  private static final int TABLE_HEIGHT = 320;
  private Text goalsCleanText;
  private Text goalsChangedText;
  private TableViewer configuratorsTable;
  private ConfiguratorsTableContentProvider configuratorsContentProvider;
  private ConfiguratorsTableLabelProvider configuratorsLabelProvider;

  public MavenProjectLifecycleMappingPage() {
    setTitle("");
    setDescription("Lifecycle mapping details:");
  }

  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));
    composite.setLayoutData(new GridData(GridData.FILL));

    
    Label goalsCleanLabel = new Label(composite, SWT.NONE);
    goalsCleanLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    goalsCleanLabel.setText("Goals to invoke after project clea&n:");

    goalsCleanText = new Text(composite, SWT.BORDER);
    goalsCleanText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Button selectGoalsCleanButton = new Button(composite, SWT.NONE);
    selectGoalsCleanButton.setLayoutData(new GridData());
    selectGoalsCleanButton.setText("&Select...");
    selectGoalsCleanButton.addSelectionListener(new MavenGoalSelectionAdapter(goalsCleanText, getShell()));
    
    final Label goalsChangedLabel = new Label(composite, SWT.NONE);
    goalsChangedLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    goalsChangedLabel.setText("&Goals to invoke on resource changes:");
    
    goalsChangedText = new Text(composite, SWT.SINGLE | SWT.BORDER);
    goalsChangedText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    
    final Button selectGoalsChangedButton = new Button(composite, SWT.NONE);
    selectGoalsChangedButton.setText("S&elect...");
    selectGoalsChangedButton.addSelectionListener(new MavenGoalSelectionAdapter(goalsChangedText, getShell()));

    Label configuratorsLabel = new Label(composite, SWT.NONE);
    configuratorsLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    configuratorsLabel.setText("Project Configurators:");
    
    configuratorsTable = new TableViewer(composite);
    TableViewerColumn configColumn = new TableViewerColumn(configuratorsTable, SWT.LEFT);
    configColumn.getColumn().setText("");
    configColumn.getColumn().setWidth(TABLE_WIDTH);
    
    configuratorsContentProvider = new ConfiguratorsTableContentProvider();
    configuratorsLabelProvider = new ConfiguratorsTableLabelProvider();
    configuratorsTable.setContentProvider(configuratorsContentProvider);
    configuratorsTable.setLabelProvider(configuratorsLabelProvider);
    configuratorsTable.setColumnProperties(CONFIG_TABLE_COLUMN_PROPERTIES);
    
    GridData gd = new GridData(SWT.LEFT, SWT.TOP, true, true, 2, 1);
    gd.widthHint = TABLE_WIDTH;
    gd.heightHint = TABLE_HEIGHT;
    //set up the column to resize
    final TableColumn col = configColumn.getColumn();
    final Table tab = configuratorsTable.getTable();
    configuratorsTable.getControl().setLayoutData(gd);
    configuratorsTable.getTable().addControlListener(new ControlAdapter() {
      public void controlResized(ControlEvent e) {
        col.setWidth(tab.getClientArea().width);
      }
    });
    
    init(getResolverConfiguration());
    
    return composite;
  }

  protected void performDefaults() {
    init(new ResolverConfiguration());
  }
  
  private void init(ResolverConfiguration configuration) {
    goalsCleanText.setText(configuration.getFullBuildGoals());
    goalsChangedText.setText(configuration.getResourceFilteringGoals());
    configuratorsTable.setInput(getLifecycleMapping(getProjectFacade()));
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
        return ((AbstractProjectConfigurator)element).getName();
      } 
      return element.toString();
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

