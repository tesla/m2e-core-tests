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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;

/**
 * Maven project preference page
 *
 * @author Eugene Kuleshov
 */
public class MavenProjectPreferencePage extends PropertyPage {
  private Button skipMavenCompilerButton;
  private Button resolveWorspaceProjectsButton;
  private Button includeModulesButton;
  
  private Text goalsCleanText;
  private Text goalsChangedText;
  private Text activeProfilesText;

  public MavenProjectPreferencePage() {
    setTitle("Maven");
    setDescription("Maven project configuration:");
  }

  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));
    composite.setLayoutData(new GridData(GridData.FILL));

    Label profilesLabel = new Label(composite, SWT.NONE);
    profilesLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
    profilesLabel.setText("Active Maven &Profiles (comma separated):");

    activeProfilesText = new Text(composite, SWT.BORDER);
    activeProfilesText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

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

    final Label warningLabel = new Label(composite, SWT.NONE);
    warningLabel.setText("Note that these goals can affect incremental build performance");
    warningLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
    GridData warningLabelData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    warningLabelData.horizontalIndent = 12;
    warningLabel.setLayoutData(warningLabelData);

    skipMavenCompilerButton = new Button(composite, SWT.CHECK);
    skipMavenCompilerButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    skipMavenCompilerButton.setData("name", "skipMavenCompilerButton");
    skipMavenCompilerButton.setText("Skip Maven compiler plugin when processing resources (recommended)");

    resolveWorspaceProjectsButton = new Button(composite, SWT.CHECK);
    GridData resolveWorspaceProjectsButtonData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    resolveWorspaceProjectsButtonData.verticalIndent = 7;
    resolveWorspaceProjectsButton.setLayoutData(resolveWorspaceProjectsButtonData);
    resolveWorspaceProjectsButton.setText("Resolve dependencies from &Workspace projects");

    includeModulesButton = new Button(composite, SWT.CHECK);
    includeModulesButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
    includeModulesButton.setText("Include &Modules");

    Text includeModulesText = new Text(composite, SWT.WRAP | SWT.READ_ONLY | SWT.MULTI);
    includeModulesText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
    GridData gd_includeModulesText = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
    gd_includeModulesText.horizontalIndent = 12;
    gd_includeModulesText.widthHint = 300;
    includeModulesText.setLayoutData(gd_includeModulesText);
    includeModulesText.setText("When enabled, dependencies from all nested modules "
        + "are added to the \"Maven Dependencies\" container and "
        + "source folders from nested modules are added to the current "
        + "project build path (use \"Update Sources\" action)");

    init(getResolverConfiguration());
    
    return composite;
  }

  protected void performDefaults() {
    init(new ResolverConfiguration());
  }
  
  private void init(ResolverConfiguration configuration) {
    skipMavenCompilerButton.setSelection(configuration.isSkipCompiler());
    resolveWorspaceProjectsButton.setSelection(configuration.shouldResolveWorkspaceProjects());
    includeModulesButton.setSelection(configuration.shouldIncludeModules());

    goalsCleanText.setText(configuration.getFullBuildGoals());
    goalsChangedText.setText(configuration.getResourceFilteringGoals());
    activeProfilesText.setText(configuration.getActiveProfiles());
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
    if(configuration.getActiveProfiles().equals(activeProfilesText.getText()) &&
        configuration.getFullBuildGoals().equals(goalsCleanText.getText()) &&
        configuration.getResourceFilteringGoals().equals(goalsChangedText.getText()) &&
        configuration.shouldIncludeModules()==includeModulesButton.getSelection() &&
        configuration.shouldResolveWorkspaceProjects()==resolveWorspaceProjectsButton.getSelection() &&
        configuration.isSkipCompiler()==skipMavenCompilerButton.getSelection()) {
      return true;
    }
    
    configuration.setSkipCompiler(skipMavenCompilerButton.getSelection());
    configuration.setResolveWorkspaceProjects(resolveWorspaceProjectsButton.getSelection());
    configuration.setIncludeModules(includeModulesButton.getSelection());
    
    configuration.setFullBuildGoals(goalsCleanText.getText());
    configuration.setResourceFilteringGoals(goalsChangedText.getText());
    configuration.setActiveProfiles(activeProfilesText.getText());
    
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    boolean isSet = projectManager.setResolverConfiguration(getProject(), configuration);
    if(isSet) {
      
      // XXX this hack need to be replaced with a listener listening on java model or resource changes
//      Display.getCurrent().asyncExec(new Runnable() {
//        public void run() {
          boolean res = MessageDialog.openQuestion(getShell(), "Maven Settings", //
              "Maven settings has changed. Do you want to update project configuration?");
          if(res) {
            final MavenPlugin plugin = MavenPlugin.getDefault();
            WorkspaceJob job = new WorkspaceJob("Updating " + project.getName() + " Sources") {
              public IStatus runInWorkspace(IProgressMonitor monitor) {
                try {
                  plugin.getProjectConfigurationManager().updateProjectConfiguration(project, configuration,
                      plugin.getMavenRuntimeManager().getGoalOnUpdate(), monitor);
                } catch(CoreException ex) {
                  return ex.getStatus();
                }
                return Status.OK_STATUS;
              }
            };
            job.setRule(plugin.getProjectConfigurationManager().getRule());
            job.schedule();
          }
//        }
//      });
    }
    
    return isSet;
  }

  private ResolverConfiguration getResolverConfiguration() {
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    return projectManager.getResolverConfiguration(getProject());
  }

  private IProject getProject() {
    return (IProject) getElement();
  }

}

