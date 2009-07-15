/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.lifecycle.AbstractLifecyclePropertyPage;
import org.maven.ide.eclipse.lifecycle.LifecycleMappingPropertyPageFactory;
import org.maven.ide.eclipse.lifecycle.ProjectConfiguratorsTable;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;

/**
 * GenericLifecycleMappingPropertyPage
 *
 * @author dyocum
 */
public class GenericLifecycleMappingPropertyPage extends AbstractLifecyclePropertyPage{

  public static final String DESC_STRING = "Maven lifecycle mapping strategy: ";
  public static final String GENERIC_STRATEGY = "Generic";

  private Button skipMavenCompilerButton;
  private Text goalsCleanText;
  private Text goalsChangedText;

  /* (non-Javadoc)
   * Called each time the property page is created. 
   * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  public Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));
    composite.setLayoutData(new GridData(GridData.FILL));

      Label goalsCleanLabel = new Label(composite, SWT.NONE);
      GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
      gd.horizontalIndent=4;
      goalsCleanLabel.setLayoutData(gd);
      goalsCleanLabel.setText("Goals to invoke after project clea&n:");
  
      goalsCleanText = new Text(composite, SWT.BORDER);
      gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
      gd.horizontalIndent = 6;
      goalsCleanText.setLayoutData(gd);
  
      Button selectGoalsCleanButton = new Button(composite, SWT.NONE);
      selectGoalsCleanButton.setLayoutData(new GridData());
      selectGoalsCleanButton.setText("&Select...");
      selectGoalsCleanButton.addSelectionListener(new MavenGoalSelectionAdapter(goalsCleanText, parent.getShell()));
      
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
      selectGoalsChangedButton.addSelectionListener(new MavenGoalSelectionAdapter(goalsChangedText, parent.getShell()));

      skipMavenCompilerButton = new Button(composite, SWT.CHECK);
      gd = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
      gd.horizontalIndent = 4;
      skipMavenCompilerButton.setLayoutData(gd);
      skipMavenCompilerButton.setData("name", "skipMavenCompilerButton");
      skipMavenCompilerButton.setText("Skip Maven compiler plugin when processing resources (recommended)");

      Composite labelComp = new Composite(composite, SWT.NONE);
      GridLayout layout = new GridLayout(2, false);
      labelComp.setLayout(layout);
      gd = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
      
      gd.verticalIndent = 15;
      
      gd.horizontalAlignment = SWT.LEFT;
      labelComp.setLayoutData(gd);
      
      Label configuratorsLabel = new Label(labelComp, SWT.NONE);
      gd = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
      configuratorsLabel.setLayoutData(gd);
      configuratorsLabel.setText("Project Configurators:");
//      pomEditorHyperlink = new Hyperlink(labelComp, SWT.NONE);
//      pomEditorHyperlink.setUnderlined(true);
//      pomEditorHyperlink.addHyperlinkListener(new IHyperlinkListener() {
//        
//        public void linkExited(HyperlinkEvent e) {
//        }
//        
//        public void linkEntered(HyperlinkEvent e) {
//        }
//        
//        public void linkActivated(HyperlinkEvent e) {
//          IFile pomFile = getProjectFacade().getPom();
//          if(pomFile != null){
//            if(performOk()){
//              getShell().close();
//              try{
//                IEditorPart part = IDE.openEditor(MavenPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage(), getProjectFacade().getPom());
//                if(part instanceof FormEditor){
//                  ((FormEditor)part).setActivePage(IMavenConstants.PLUGIN_ID + ".pom.lifecycleMappings");
//                }
//              } catch(PartInitException pie){
//                MavenLogger.log("Unable to open the POM file", pie);
//              }
//            }
//          }
//        }
//      });
//      pomEditorHyperlink.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
//      pomEditorHyperlink.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
//      pomEditorHyperlink.setText("View/Edit in POM Editor");
//
    new ProjectConfiguratorsTable(composite, getProject());
    init(LifecycleMappingPropertyPageFactory.getResolverConfiguration(getProject()));
    return composite;
  }

  public void performDefaults(){
    init(new ResolverConfiguration());
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
    final ResolverConfiguration configuration = LifecycleMappingPropertyPageFactory.getResolverConfiguration(getProject());
    if((configuration.getFullBuildGoals() != null && configuration.getFullBuildGoals().equals(goalsCleanText.getText())) &&
        (configuration.getResourceFilteringGoals() != null &&  configuration.getResourceFilteringGoals().equals(goalsChangedText.getText()))  &&
        configuration.isSkipCompiler()==skipMavenCompilerButton.getSelection()) {
      return true;
    }
    configuration.setSkipCompiler(skipMavenCompilerButton.getSelection());
    configuration.setFullBuildGoals(goalsCleanText.getText());
    configuration.setResourceFilteringGoals(goalsChangedText.getText());
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    boolean isSet = projectManager.setResolverConfiguration(getProject(), configuration);
    
    if(isSet) {
        boolean res = MessageDialog.openQuestion(this.getShell(), "Maven Settings", //
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

  public String getMessage(){
    return "Generic Lifecycle Mapping";
  }
  
  private void init(ResolverConfiguration configuration) {
    if(skipMavenCompilerButton != null){
      skipMavenCompilerButton.setSelection(configuration.isSkipCompiler());
    }
    if(goalsCleanText != null){
      goalsCleanText.setText(configuration.getFullBuildGoals());
    }
    if(goalsChangedText != null){
      goalsChangedText.setText(configuration.getResourceFilteringGoals());
    }
  }


}
