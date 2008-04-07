/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.maven.ide.eclipse.internal.launch.ui.MavenGoalSelectionAdapter;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;

/**
 * Maven project preference page
 *
 * @author Eugene Kuleshov
 */
public class MavenProjectPreferencePage extends PropertyPage {

  public static final String ID = "org.maven.ide.eclipse.MavenProjectPreferencePage";
  
  private Button useMavenOutputFoldersButton;
  private Button resolveWorspaceProjectsButton;
	private Button includeModulesButton;
	Button enableResourceFilteringButton;
	
	Text resourceFilteringGoalsText;
	private Text activeProfilesText;

	public MavenProjectPreferencePage() {
	  setTitle("Maven");
    setDescription("Maven project configuration:");
	}

  protected Control createContents(Composite parent) {
  	Composite composite = new Composite(parent, SWT.NONE);
  	composite.setLayout(new GridLayout(2, false));
  	composite.setLayoutData(new GridData(GridData.FILL));

  	resolveWorspaceProjectsButton = new Button(composite, SWT.CHECK);
  	GridData gd_resolveWorspaceProjectsButton = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
  	resolveWorspaceProjectsButton.setLayoutData(gd_resolveWorspaceProjectsButton);
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

  	Label profilesLabel = new Label(composite, SWT.NONE);
  	GridData gd_profilesLabel = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
  	gd_profilesLabel.verticalIndent = 3;
  	profilesLabel.setLayoutData(gd_profilesLabel);
  	profilesLabel.setText("Active Maven &Profiles (comma separated):");

  	activeProfilesText = new Text(composite, SWT.BORDER);
  	activeProfilesText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
  	
  	useMavenOutputFoldersButton = new Button(composite, SWT.CHECK);
  	GridData gd_useMavenOutputFoldersButton = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
  	gd_useMavenOutputFoldersButton.verticalIndent = 15;
  	useMavenOutputFoldersButton.setLayoutData(gd_useMavenOutputFoldersButton);
  	useMavenOutputFoldersButton.setText("Use Maven &output folders");

  	enableResourceFilteringButton = new Button(composite, SWT.CHECK);
  	enableResourceFilteringButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
  	enableResourceFilteringButton.setText("Enable resource &filtering");
  
  	final Label goalsLabel = new Label(composite, SWT.NONE);
  	GridData gd_goalsLabel = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
  	gd_goalsLabel.horizontalIndent = 12;
  	goalsLabel.setLayoutData(gd_goalsLabel);
  	goalsLabel.setText("&Goals to invoke for resource filtering:");
    
  	resourceFilteringGoalsText = new Text(composite, SWT.SINGLE | SWT.BORDER);
  	GridData gd_resourceFilteringGoalsText = new GridData(SWT.FILL, SWT.CENTER, true, false);
  	gd_resourceFilteringGoalsText.horizontalIndent = 12;
  	resourceFilteringGoalsText.setLayoutData(gd_resourceFilteringGoalsText);
    
  	final Button selectGoalsButton = new Button(composite, SWT.NONE);
  	selectGoalsButton.setText("&Select...");
    selectGoalsButton.addSelectionListener(new MavenGoalSelectionAdapter(resourceFilteringGoalsText, getShell()));

  	final Label warningLabel = new Label(composite, SWT.NONE);
  	warningLabel.setText("Note that these goals can affect build performance");
  	warningLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
  	GridData warningLabelData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
  	warningLabelData.horizontalIndent = 12;
  	warningLabel.setLayoutData(warningLabelData);

  	enableResourceFilteringButton.addSelectionListener(new SelectionAdapter() {
  	  public void widgetSelected(SelectionEvent e) {
  	    boolean selected = enableResourceFilteringButton.getSelection();
  	    goalsLabel.setEnabled(selected);
  	    resourceFilteringGoalsText.setEnabled(selected);
  	    selectGoalsButton.setEnabled(selected);
  	    warningLabel.setEnabled(selected);
  	  }
  	});
  	
    init(getProjectFacade().getResolverConfiguration());
    
  	return composite;
  }

  protected void performDefaults() {
    init(new ResolverConfiguration());
  }
  
  private void init(ResolverConfiguration configuration) {
    useMavenOutputFoldersButton.setSelection(configuration.shouldUseMavenOutputFolders());
    resolveWorspaceProjectsButton.setSelection(configuration.shouldResolveWorkspaceProjects());
    includeModulesButton.setSelection(configuration.shouldIncludeModules());
    enableResourceFilteringButton.setSelection(configuration.shouldFilterResources());

    resourceFilteringGoalsText.setText(configuration.getResourceFilteringGoals());
    activeProfilesText.setText(configuration.getActiveProfiles());
  }

	public boolean performOk() {
	  MavenProjectFacade projectFacade = getProjectFacade();
	  
	  final ResolverConfiguration configuration = projectFacade.getResolverConfiguration();

	  configuration.setUseMavenOutputFolders(useMavenOutputFoldersButton.getSelection());
	  configuration.setResolveWorkspaceProjects(resolveWorspaceProjectsButton.getSelection());
	  configuration.setIncludeModules(includeModulesButton.getSelection());
	  configuration.setFilterResources(enableResourceFilteringButton.getSelection());
	  
	  configuration.setResourceFilteringGoals(resourceFilteringGoalsText.getText());
	  configuration.setActiveProfiles(activeProfilesText.getText());
	  
    boolean isSet = projectFacade.setResolverConfiguration(configuration);
    
    if(isSet) {
      final IProject project = getProject();
      
      // XXX this hack need to be replaced with a listener listening on java model or resource changes
//      Display.getCurrent().asyncExec(new Runnable() {
//        public void run() {
          boolean res = MessageDialog.openQuestion(getShell(), "Maven Project Configuration", //
              "Maven project configuration has changed. Do you want to update source folders?");
          if(res) {
            new Job("Updating " + project.getName() + " Sources") {
              protected IStatus run(IProgressMonitor monitor) {
                MavenPlugin plugin = MavenPlugin.getDefault();
                plugin.getBuildpathManager().updateSourceFolders(project, configuration,
                    plugin.getMavenRuntimeManager().getGoalOnUpdate(), monitor);
                return Status.OK_STATUS;
              }
            }.schedule();
          }
//        }
//      });
    }
    
    return isSet;
	}

  private MavenProjectFacade getProjectFacade() {
    IProject project = getProject();
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    return projectManager.create(project, new NullProgressMonitor());
  }

  private IProject getProject() {
    return (IProject) getElement();
  }

}

