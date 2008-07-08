/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.common.ArchetypeArtifactManager;
import org.apache.maven.archetype.exception.UnknownArchetype;
import org.apache.maven.archetype.metadata.ArchetypeDescriptor;
import org.apache.maven.archetype.metadata.RequiredProperty;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.model.Model;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.core.Messages;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.project.ProjectImportConfiguration;
import org.maven.ide.eclipse.util.JavaUtil;


/**
 * Wizard page responsible for gathering information about the Maven2 artifact when an archetype is being used to create
 * a project (thus the class name pun).
 */
public class MavenProjectWizardArchetypeParametersPage extends AbstractMavenWizardPage {

  Table propertiesTable;

  public static final String DEFAULT_VERSION = "0.0.1-SNAPSHOT";

  /** group id text field */
  protected Combo groupIdCombo;

  /** artifact id text field */
  protected Combo artifactIdCombo;

  /** version text field */
  protected Combo versionCombo;

  /** package text field */
  protected Combo packageCombo;

  protected Button removeButton;

  private boolean isUsed;

  protected Set<String> requiredProperties;

  protected Archetype archetype;

  protected boolean archetypeChanged = false;

  /** shows if the package has been customized by the user */
  protected boolean packageCustomized = false;

  /** Creates a new page. */
  public MavenProjectWizardArchetypeParametersPage(ProjectImportConfiguration projectImportConfiguration) {
    super("Maven2ProjectWizardArchifactPage", projectImportConfiguration);

    setTitle(Messages.getString("wizard.project.page.maven2.title"));
    setDescription(Messages.getString("wizard.project.page.maven2.archetype.parameters.description"));
    setPageComplete(false);

    requiredProperties = new HashSet<String>();
  }

  /** Creates page controls. */
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    composite.setLayout(new GridLayout(3, false));

    createArtifactGroup(composite);
    createPropertiesGroup(composite);

    validate();

    createAdvancedSettings(composite, new GridData(SWT.FILL, SWT.TOP, false, false, 2, 1));
    resolverConfigurationComponent.setModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        validate();
      }
    });

    setControl(composite);

  }

  private void createArtifactGroup(Composite parent) {
//    Composite artifactGroup = new Composite(parent, SWT.NONE);
//    GridData gd_artifactGroup = new GridData( SWT.FILL, SWT.FILL, true, false );
//    artifactGroup.setLayoutData(gd_artifactGroup);
//    artifactGroup.setLayout(new GridLayout(2, false));

    Label groupIdlabel = new Label(parent, SWT.NONE);
    groupIdlabel.setText(Messages.getString("artifactComponent.groupId"));

    groupIdCombo = new Combo(parent, SWT.BORDER);
    groupIdCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    addFieldWithHistory("groupId", groupIdCombo);
    groupIdCombo.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateJavaPackage();
        validate();
      }
    });

    Label artifactIdLabel = new Label(parent, SWT.NONE);
    artifactIdLabel.setText(Messages.getString("artifactComponent.artifactId"));

    artifactIdCombo = new Combo(parent, SWT.BORDER);
    artifactIdCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
    addFieldWithHistory("artifactId", artifactIdCombo);
    artifactIdCombo.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateJavaPackage();
        validate();
      }
    });

    Label versionLabel = new Label(parent, SWT.NONE);
    versionLabel.setText(Messages.getString("artifactComponent.version"));

    versionCombo = new Combo(parent, SWT.BORDER);
    GridData gd_versionCombo = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    gd_versionCombo.widthHint = 150;
    versionCombo.setLayoutData(gd_versionCombo);
    versionCombo.setText(DEFAULT_VERSION);
    addFieldWithHistory("version", versionCombo);
    versionCombo.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        validate();
      }
    });

    Label packageLabel = new Label(parent, SWT.NONE);
    packageLabel.setText(Messages.getString("artifactComponent.package"));

    packageCombo = new Combo(parent, SWT.BORDER);
    packageCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
    addFieldWithHistory("package", packageCombo);
    packageCombo.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        if(!packageCustomized && !packageCombo.getText().equals(getDefaultJavaPackage())) {
          packageCustomized = true;
        }
        validate();
      }
    });
  }

  private void createPropertiesGroup(Composite composite) {
    Label propertiesLabel = new Label(composite, SWT.NONE);
    propertiesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    propertiesLabel.setText("Properties:");

    final TableViewer propertiesViewer = new TableViewer(composite, SWT.BORDER);
    propertiesTable = propertiesViewer.getTable();
    propertiesTable.setLinesVisible(true);
    propertiesTable.setHeaderVisible(true);
    propertiesTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 2));

    propertiesViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        TableItem[] selection = propertiesTable.getSelection();
        if(selection.length == 1) {
          MavenPropertyDialog dialog = new MavenPropertyDialog(getShell(), //
              Messages.getString("launch.propEditDialogTitle"), //$NON-NLS-1$
              new String[] {selection[0].getText(0), selection[0].getText(1)}, false);
          int res = dialog.open();
          if(res == IDialogConstants.OK_ID) {
            String[] result = dialog.getNameValuePair();
            TableItem[] item = propertiesTable.getSelection();
            // we expect only one row selected
            item[0].setText(0, result[0]);
            item[0].setText(1, result[1]);
            validate();
          }
        }
      }
    });

    TableColumn propertiesTableNameColumn = new TableColumn(propertiesTable, SWT.NONE);
    propertiesTableNameColumn.setWidth(130);
    propertiesTableNameColumn.setText("Name");

    TableColumn propertiesTableValueColumn = new TableColumn(propertiesTable, SWT.NONE);
    propertiesTableValueColumn.setWidth(230);
    propertiesTableValueColumn.setText("Value");

    Button addButton = new Button(composite, SWT.NONE);
    addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    addButton.setText("&Add...");
    addButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        MavenPropertyDialog dialog = new MavenPropertyDialog(getShell(), //
            Messages.getString("launch.propAddDialogTitle"), new String[] {}, false); //$NON-NLS-1$
        int res = dialog.open();
        if(res == IDialogConstants.OK_ID) {
          String[] result = dialog.getNameValuePair();
          TableItem item = new TableItem(propertiesTable, SWT.NONE);
          item.setText(0, result[0]);
          item.setText(1, result[1]);
          validate();
        }
      }
    });

    removeButton = new Button(composite, SWT.NONE);
    removeButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
    removeButton.setText("&Remove");
    removeButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if(propertiesTable.getSelectionCount() > 0) {
          propertiesTable.remove(propertiesTable.getSelectionIndices());
          validate();
        }
      }
    });

    propertiesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        TableItem[] selection = propertiesTable.getSelection();
        if(selection.length == 1) {
          removeButton.setEnabled(!requiredProperties.contains(selection[0].getText(0)));
        }
      }
    });
  }

  /**
   * Validates the contents of this wizard page.
   * <p>
   * Feedback about the validation is given to the user by displaying error messages or informative messages on the
   * wizard page. Depending on the provided user input, the wizard page is marked as being complete or not.
   * <p>
   * If some error or missing input is detected in the user input, an error message or informative message,
   * respectively, is displayed to the user. If the user input is complete and correct, the wizard page is marked as
   * begin complete to allow the wizard to proceed. To that end, the following conditions must be met:
   * <ul>
   * <li>The user must have provided a group ID.</li>
   * <li>The user must have provided an artifact ID.</li>
   * <li>The user must have provided a version for the artifact.</li>
   * </ul>
   * </p>
   * 
   * @see org.eclipse.jface.dialogs.DialogPage#setMessage(java.lang.String)
   * @see org.eclipse.jface.wizard.WizardPage#setErrorMessage(java.lang.String)
   * @see org.eclipse.jface.wizard.WizardPage#setPageComplete(boolean)
   */
  void validate() {
    if(groupIdCombo.getText().trim().length() == 0) {
      setErrorMessage(Messages.getString("wizard.project.page.maven2.validator.groupID"));
      setPageComplete(false);
      return;
    }

    if(artifactIdCombo.getText().trim().length() == 0) {
      setErrorMessage(Messages.getString("wizard.project.page.maven2.validator.artifactID"));
      setPageComplete(false);
      return;
    }

    if(versionCombo.getText().trim().length() == 0) {
      setErrorMessage(Messages.getString("wizard.project.page.maven2.validator.version"));
      setPageComplete(false);
      return;
    }

    String packageName = packageCombo.getText();
    if(packageName.trim().length() != 0) {
      @SuppressWarnings("deprecation")
      IStatus status = JavaConventions.validatePackageName(packageName);
      if(!status.isOK()) {
        setErrorMessage(status.getMessage());
        setPageComplete(false);
        return;
      }
    }

    // validate project name
    IStatus nameStatus = getImportConfiguration().validateProjectName(getModel());
    if(!nameStatus.isOK()) {
      setErrorMessage(nameStatus.getMessage());
      setPageComplete(false);
      return;
    }

    if(requiredProperties.size() > 0) {
      Properties properties = getProperties();
      for(String key : requiredProperties) {
        String value = properties.getProperty(key);
        if(value == null || value.length() == 0) {
          setErrorMessage(Messages.getString("wizard.project.page.maven2.validator.requiredProperty", key));
          setPageComplete(false);
          return;
        }
      }
    }

    setPageComplete(true);

    setErrorMessage(null);
    setMessage(null);
  }

  /** Ends the wizard flow chain. */
  public IWizardPage getNextPage() {
    return null;
  }

  public void setArchetype(Archetype archetype) {
    if(archetype == null) {
      propertiesTable.removeAll();
      archetypeChanged = false;
    } else if(!archetype.equals(this.archetype)) {
      this.archetype = archetype;
      propertiesTable.removeAll();
      requiredProperties.clear();
      archetypeChanged = true;

      Properties properties = archetype.getProperties();
      if(properties != null) {
        for(Iterator<Map.Entry<Object, Object>> it = properties.entrySet().iterator(); it.hasNext();) {
          Map.Entry<?, ?> e = it.next();
          addTableItem((String) e.getKey(), (String) e.getValue());
        }
      }
    }
  }

  void loadArchetypeDescriptor() {
    final String groupId = archetype.getGroupId();
    final String artifactId = archetype.getArtifactId();
    final String version = archetype.getVersion();
    final String archetypeName = groupId + ":" + artifactId + ":" + version;

    try {
      getContainer().run(false, true, new IRunnableWithProgress() {
        public void run(IProgressMonitor monitor) {
          monitor.beginTask("Downloading Archetype " + archetypeName, IProgressMonitor.UNKNOWN);
          try {
            MavenPlugin mavenPlugin = MavenPlugin.getDefault();
            MavenEmbedderManager embedderMgr = mavenPlugin.getMavenEmbedderManager();
            MavenEmbedder embedder = embedderMgr.getWorkspaceEmbedder();

            ArtifactRepository localRepository = embedder.getLocalRepository();

            List<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
            repositories.addAll(mavenPlugin.getIndexManager().getArtifactRepositories(null, null));

            PlexusContainer plexus = embedder.getPlexusContainer();
            ArchetypeArtifactManager aaMgr = (ArchetypeArtifactManager) plexus.lookup(ArchetypeArtifactManager.class);
            if(aaMgr.isFileSetArchetype(groupId, artifactId, version, null, localRepository, repositories)) {
              ArchetypeDescriptor descriptor = aaMgr.getFileSetArchetypeDescriptor(groupId, artifactId, version, null,
                  localRepository, repositories);
              List<?> properties = descriptor.getRequiredProperties();
              if(properties != null) {
                for(Object o : properties) {
                  if(o instanceof RequiredProperty) {
                    RequiredProperty rp = (RequiredProperty) o;
                    requiredProperties.add(rp.getKey());
                    addTableItem(rp.getKey(), rp.getDefaultValue());
                  }
                }
              }
            }
          } catch(ComponentLookupException e) {
            MavenLogger.log("Error downloading archetype " + archetypeName, e);
          } catch(UnknownArchetype e) {
            MavenLogger.log("Error downloading archetype " + archetypeName, e);
          } finally {
            monitor.done();
          }
        }
      });
    } catch(InterruptedException ex) {
      // ignore
    } catch(InvocationTargetException ex) {
      String msg = "Error downloading archetype " + archetypeName;
      MavenLogger.log(msg, ex);
      setErrorMessage(msg + "\n" + ex.toString());
    }
  }

  /**
   * @param key
   * @param value
   */
  void addTableItem(String key, String value) {
    TableItem item = new TableItem(propertiesTable, SWT.NONE);
    item.setText(0, key);
    item.setText(1, value == null ? "" : value);
  }

  /**
   * Updates the properties when a project name is set on the first page of the wizard.
   */
  public void setProjectName(String projectName) {
    if(artifactIdCombo.getText().equals(groupIdCombo.getText())) {
      groupIdCombo.setText(projectName);
    }
    artifactIdCombo.setText(projectName);
    packageCombo.setText("org." + projectName.replace('-', '.'));
    validate();
  }

  /**
   * Updates the properties when a project name is set on the first page of the wizard.
   */
  public void setParentProject(String groupId, String artifactId, String version) {
    groupIdCombo.setText(groupId);
    versionCombo.setText(version);
    validate();
  }

  /** Enables or disables the artifact id text field. */
  public void setArtifactIdEnabled(boolean b) {
    artifactIdCombo.setEnabled(b);
  }

  /** Returns the package name. */
  public String getJavaPackage() {
    if(packageCombo.getText().length() > 0) {
      return packageCombo.getText();
    }
    return getDefaultJavaPackage();
  }

  /** Updates the package name if the related fields changed. */
  protected void updateJavaPackage() {
    if(packageCustomized) {
      return;
    }

    String defaultPackageName = getDefaultJavaPackage();
    packageCombo.setText(defaultPackageName);
  }

  /** Returns the default package name. */
  protected String getDefaultJavaPackage() {
    return JavaUtil.getDefaultJavaPackage(groupIdCombo.getText().trim(), artifactIdCombo.getText().trim());
  }

  /** Creates the Model object. */
  public Model getModel() {
    Model model = new Model();

    model.setModelVersion("4.0.0");
    model.setGroupId(groupIdCombo.getText());
    model.setArtifactId(artifactIdCombo.getText());
    model.setVersion(versionCombo.getText());

    return model;
  }

  public void setUsed(boolean isUsed) {
    this.isUsed = isUsed;
  }

  public boolean isPageComplete() {
    return !isUsed || super.isPageComplete();
  }

  /** Loads the group value when the page is displayed. */
  public void setVisible(boolean visible) {
    super.setVisible(visible);

    if(visible) {
      if(groupIdCombo.getText().length() == 0 && groupIdCombo.getItemCount() > 0) {
        groupIdCombo.setText(groupIdCombo.getItem(0));
        packageCombo.setText(getDefaultJavaPackage());
        packageCustomized = false;
      }

      if(archetypeChanged && archetype != null) {
        archetypeChanged = false;
        loadArchetypeDescriptor();
        validate();
      }
    }
  }

  public Properties getProperties() {
    Properties properties = new Properties();
    for(int i = 0; i < propertiesTable.getItemCount(); i++ ) {
      TableItem item = propertiesTable.getItem(i);
      properties.put(item.getText(0), item.getText(1));
    }
    return properties;
  }
}
