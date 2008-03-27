/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.container;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;

import org.maven.ide.eclipse.internal.preferences.MavenProjectPreferencePage;
import org.maven.ide.eclipse.project.BuildPathManager;


/**
 * MavenClasspathContainerPage
 * 
 * @author Eugene Kuleshov
 */
public class MavenClasspathContainerPage extends WizardPage implements IClasspathContainerPage,
    IClasspathContainerPageExtension {

  IJavaProject javaProject;

  private IClasspathEntry containerEntry;
  
//  private Text includeModulesText;
//
//  private Button enableResourceFilteringButton;
//
//  private Button useMavenOutputFoldersButton;
//
//  private Button resolveWorspaceProjectsButton;
//
//  private Button includeModulesButton;
//
//  private Text activeProfilesText;
//
//  private ResolverConfiguration resolverConfiguration;


  public MavenClasspathContainerPage() {
    super("Maven Dependencies");
  }

  // IClasspathContainerPageExtension

  public void initialize(IJavaProject javaProject, IClasspathEntry[] currentEntries) {
    this.javaProject = javaProject;
    // this.currentEntries = currentEntries;
  }

  // IClasspathContainerPage

  public IClasspathEntry getSelection() {
    return this.containerEntry;
  }

  public void setSelection(IClasspathEntry containerEntry) {
    this.containerEntry = containerEntry == null ? BuildPathManager.getDefaultContainerEntry() : containerEntry;
  }

  public void createControl(Composite parent) {
    setTitle("Maven Managed Dependencies");
    setDescription("Set the dependency resolver configuration");


    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout());
    setControl(composite);

    Link link = new Link(composite, SWT.NONE);
    link.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    link.setText("Use <a href=\"#maven\">Maven Project settings</a> to configure Maven dependency resolution");
    link.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        // IWorkbenchPreferenceContainer container= (IWorkbenchPreferenceContainer) getContainer();
        // container.openPage(MavenProjectPreferencePage.ID, javaProject.getProject());
        
        PreferencesUtil.createPropertyDialogOn(getShell(), javaProject.getProject(), //
            MavenProjectPreferencePage.ID, new String[] {MavenProjectPreferencePage.ID}, null).open();
      }
    });

//    resolverConfiguration = BuildPathManager.getResolverConfiguration(containerEntry);
//    
//    final CTabFolder tabFolder = new CTabFolder(parent, SWT.FLAT);
//    tabFolder.setSelectionForeground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_FOREGROUND));
//    tabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_BACKGROUND));
//    tabFolder.setBorderVisible(true);
//    tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//    tabFolder.setLayout(new FillLayout());
//    setControl(tabFolder);
//
//    final CTabItem configurationTabItem = new CTabItem(tabFolder, SWT.NONE);
//    configurationTabItem.setText("Resolver Configuration");
//
//    final Composite tabComposite = new Composite(tabFolder, SWT.NONE);
//    final GridLayout gridLayout = new GridLayout();
//    gridLayout.marginWidth = 10;
//    gridLayout.marginHeight = 10;
//    tabComposite.setLayout(gridLayout);
//    configurationTabItem.setControl(tabComposite);
//
//    Text projectText = new Text(tabComposite, SWT.NONE);
//    projectText.setText("Project: " + javaProject.getElementName());
//    projectText.setEditable(false);
//
//    enableResourceFilteringButton = new Button(tabComposite, SWT.CHECK);
//    GridData gd_enableResourceFilteringButton = new GridData();
//    gd_enableResourceFilteringButton.verticalIndent = 5;
//    enableResourceFilteringButton.setLayoutData(gd_enableResourceFilteringButton);
//    enableResourceFilteringButton.setText("Enable resource &filtering");
//    enableResourceFilteringButton.setSelection(resolverConfiguration.shouldFilterResources());
//
//    useMavenOutputFoldersButton = new Button(tabComposite, SWT.CHECK);
//    GridData gd_useMavenOutputFoldersButton = new GridData();
//    gd_enableResourceFilteringButton.verticalIndent = 5;
//    useMavenOutputFoldersButton.setLayoutData(gd_useMavenOutputFoldersButton);
//    useMavenOutputFoldersButton.setText("Use Maven &output folders");
//    useMavenOutputFoldersButton.setSelection(resolverConfiguration.shouldUseMavenOutputFolders());
//    
//    resolveWorspaceProjectsButton = new Button(tabComposite, SWT.CHECK);
//    GridData gd_resolveWorspaceProjectsButton = new GridData(SWT.LEFT, SWT.CENTER, true, false);
//    gd_resolveWorspaceProjectsButton.verticalIndent = 5;
//    resolveWorspaceProjectsButton.setLayoutData(gd_resolveWorspaceProjectsButton);
//    resolveWorspaceProjectsButton.setText("Resolve dependencies from &Workspace projects");
//    resolveWorspaceProjectsButton.setSelection(resolverConfiguration.shouldResolveWorkspaceProjects());
//    
//    includeModulesButton = new Button(tabComposite, SWT.CHECK);
//    GridData gd_includeModulesButton = new GridData(SWT.LEFT, SWT.CENTER, true, false);
//    gd_includeModulesButton.verticalIndent = 5;
//    includeModulesButton.setLayoutData(gd_includeModulesButton);
//    includeModulesButton.setText("Include &Modules");
//    includeModulesButton.setSelection(resolverConfiguration.shouldIncludeModules());
//
//    includeModulesText = new Text(tabComposite, SWT.WRAP | SWT.READ_ONLY | SWT.MULTI);
//    includeModulesText.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
//    includeModulesText.setBackground(tabComposite.getBackground());
//    GridData includeModulesData = new GridData(SWT.FILL, SWT.FILL, true, false);
//    includeModulesData.widthHint = 300;
//    includeModulesData.verticalIndent = -3;
//    includeModulesData.horizontalIndent = 8;
//    includeModulesText.setLayoutData(includeModulesData);
//    includeModulesText.setText("When enabled, dependencies from all nested modules are " +
//    		"added to the \"Maven Dependencies\" container and source folders from nested " +
//    		"modules are added to the current project build path (use \"Update Sources\" action)");
//
//    Label profilesLabel = new Label(tabComposite, SWT.NONE);
//    final GridData gd_profilesLabel = new GridData(SWT.LEFT, SWT.CENTER, true, false);
//    gd_profilesLabel.verticalIndent = 5;
//    profilesLabel.setLayoutData(gd_profilesLabel);
//    profilesLabel.setBounds(0, 0, 191, 16);
//    profilesLabel.setText("Active Maven &Profiles (comma separated):");
//
//    activeProfilesText = new Text(tabComposite, SWT.BORDER);
//    activeProfilesText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//    activeProfilesText.setBounds(0, 0, 472, 22);
//    activeProfilesText.setText(resolverConfiguration.getActiveProfiles());
//
//    // TODO show/manage container entries
////    final CTabItem dependenciesTabItem = new CTabItem(tabFolder, SWT.NONE);
////    dependenciesTabItem.setText("Dependencies");
//    
//    tabFolder.setSelection(configurationTabItem);
//    tabFolder.showSelection();
  }

  public boolean finish() {
//    boolean newIncludeModules = includeModulesButton.getSelection();
//    boolean newResolveWorspaceProjects = resolveWorspaceProjectsButton.getSelection();
//    boolean newEnableResourceFiltering = enableResourceFilteringButton.getSelection();
//    boolean newUseMavenOutputFolders = useMavenOutputFoldersButton.getSelection();
//    String newProfiles = activeProfilesText.getText();
//    if(newIncludeModules != resolverConfiguration.shouldIncludeModules()
//        || newResolveWorspaceProjects != resolverConfiguration.shouldResolveWorkspaceProjects()
//        || newEnableResourceFiltering != resolverConfiguration.shouldFilterResources()
//        || newUseMavenOutputFolders != resolverConfiguration.shouldUseMavenOutputFolders()
//        || !newProfiles.equals(resolverConfiguration.getActiveProfiles())) {
//
//      final ResolverConfiguration configuration = new ResolverConfiguration();
//      configuration.setIncludeModules(newIncludeModules);
//      configuration.setResolveWorkspaceProjects(newResolveWorspaceProjects);
//      configuration.setFilterResources(newEnableResourceFiltering);
//      configuration.setUseMavenOutputFolders(newUseMavenOutputFolders);
//      configuration.setActiveProfiles(newProfiles);
//      
//      containerEntry = BuildPathManager.createContainerEntry(configuration);
//      
//      // XXX this hack need to be replaced with a listener listening on java model or resource changes
//      Display.getCurrent().asyncExec(new Runnable() {
//        public void run() {
//          boolean res = MessageDialog.openQuestion(getShell(), "Maven Configuration", //
//              "Maven project configuration has changed. Do you want to update source folders?");
//          if(res) {
//            new Job("Updating " + javaProject.getProject().getName() + " Sources") {
//              protected IStatus run(IProgressMonitor monitor) {
//                MavenPlugin plugin = MavenPlugin.getDefault();
//                plugin.getBuildpathManager().updateSourceFolders(javaProject.getProject(),
//                    configuration, plugin.getMavenRuntimeManager().getGoalOnUpdate(), monitor);
//                return Status.OK_STATUS;
//              }
//            }.schedule();
//          }
//        }
//      });
//    }
    return true;
  }

}
