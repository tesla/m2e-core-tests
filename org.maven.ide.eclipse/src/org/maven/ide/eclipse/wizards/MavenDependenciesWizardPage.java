/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wizards;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;

import org.maven.ide.eclipse.core.Messages;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.project.ProjectImportConfiguration;


/**
 * Wizard page for gathering information about Maven artifacts. Allows to select 
 * artifacts from the repository index.
 */
public class MavenDependenciesWizardPage extends AbstractMavenWizardPage {

  boolean showLocation = false;
  
  /** 
   * Viewer containing dependencies
   */
  TableViewer dependencyViewer;
  
  Combo locationCombo;
  Label locationLabel;
  Button useDefaultWorkspaceLocationButton;

  private Dependency[] dependencies;
  
  /**
   * Listeners notified about all changes
   */
  private List<ISelectionChangedListener> listeners = new ArrayList<ISelectionChangedListener>();

  private Button checkOutAllButton;

  private Button useDeveloperConnectionButton;

  public MavenDependenciesWizardPage() {
    this(null, Messages.getString("wizard.project.page.dependencies.title"), //
        Messages.getString("wizard.project.page.dependencies.description"));
  }
  
  public MavenDependenciesWizardPage(ProjectImportConfiguration projectImportConfiguration, String title, String description) {
    super("MavenDependenciesWizardPage", projectImportConfiguration);
    setTitle(title);
    setDescription(description);
    setPageComplete(true);
  }

  public void showLocation(boolean showLocation) {
    this.showLocation = showLocation;
  }
  
  public void setDependencies(Dependency[] dependencies) {
    this.dependencies = dependencies;
  }
  
  /**
   * {@inheritDoc} This wizard page contains a <code>TableViewer</code> to display the currently included Maven2
   * directories and a button area with buttons to add further dependencies or remove existing ones.
   */
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout(3, false);
    composite.setLayout(layout);

    if(dependencies!=null) {
      createArtifacts(composite);
    }
    
    if(showLocation) {
      createLocationControls(composite);
    }
    
    GridData advancedSettingsData = new GridData(SWT.FILL, SWT.TOP, false, false, 2, 1);
    advancedSettingsData.verticalIndent = 10;
    createAdvancedSettings(composite, advancedSettingsData);

    setControl(composite);

    updatePage();
  }

  private void createLocationControls(Composite composite) {
    checkOutAllButton = new Button(composite, SWT.CHECK);
    checkOutAllButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    checkOutAllButton.setText("Check out &All projects");

    useDeveloperConnectionButton = new Button(composite, SWT.CHECK);
    useDeveloperConnectionButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    useDeveloperConnectionButton.setText("Use &developer connection");

    Label separatorLabel = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
    GridData labelData = new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1);
    labelData.verticalIndent = 7;
    separatorLabel.setLayoutData(labelData);

    useDefaultWorkspaceLocationButton = new Button(composite, SWT.CHECK);
    useDefaultWorkspaceLocationButton.setText("Use default &Workspace location");
    GridData useDefaultWorkspaceLocationButtonData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
    useDefaultWorkspaceLocationButton.setLayoutData(useDefaultWorkspaceLocationButtonData);
    useDefaultWorkspaceLocationButton.setSelection(true);
    useDefaultWorkspaceLocationButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updatePage();
      }
    });

    locationLabel = new Label(composite, SWT.NONE);
    GridData locationLabelData = new GridData();
    locationLabelData.horizontalIndent = 10;
    locationLabel.setLayoutData(locationLabelData);
    locationLabel.setText("&Location:");

    locationCombo = new Combo(composite, SWT.NONE);
    locationCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    addFieldWithHistory("location", locationCombo);

    Button locationBrowseButton = new Button(composite, SWT.NONE);
    locationBrowseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    locationBrowseButton.setText("&Browse...");
    locationBrowseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        DirectoryDialog dialog = new DirectoryDialog(getShell());
        dialog.setText("Select Location");
        
        String path = locationCombo.getText();
        if(path.length()==0) {
          path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toPortableString();
        }
        dialog.setFilterPath(path);

        String selectedDir = dialog.open();
        if(selectedDir != null) {
          locationCombo.setText(selectedDir);
          useDefaultWorkspaceLocationButton.setSelection(false);
          updatePage();
        }
      }
    });
  }

  private void createArtifacts(Composite composite) {
    Label mavenArtifactsLabel = new Label(composite, SWT.NONE);
    mavenArtifactsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    mavenArtifactsLabel.setText("Maven Artifacts:");
    
    dependencyViewer = new TableViewer(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
    dependencyViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 2));
    dependencyViewer.setUseHashlookup(true);
    dependencyViewer.setLabelProvider(new ArtifactLabelProvider());
    dependencyViewer.setSorter(new DependencySorter());
    dependencyViewer.add(dependencies);

    Button addDependencyButton = new Button(composite, SWT.PUSH);
    GridData gd_addDependencyButton = new GridData(SWT.FILL, SWT.TOP, false, false);
    addDependencyButton.setLayoutData(gd_addDependencyButton);
    addDependencyButton.setText(Messages.getString("wizard.project.page.dependencies.add"));

    addDependencyButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
            "Add Dependency", IndexManager.SEARCH_ARTIFACT, Collections.<Artifact>emptySet(), !showLocation);
        if(dialog.open() == Window.OK) {
          Object result = dialog.getFirstResult();
          if(result instanceof IndexedArtifactFile) {
            Dependency dependency = ((IndexedArtifactFile) result).getDependency();
            dependency.setScope(dialog.getSelectedScope());
            dependencyViewer.add(dependency);
            notifyListeners();
          } else if(result instanceof IndexedArtifact) {
            // If we have an ArtifactInfo, we add the first FileInfo it contains
            // which corresponds to the latest version of the artifact.
            Set<IndexedArtifactFile> files = ((IndexedArtifact) result).files;
            if(files != null && !files.isEmpty()) {
              dependencyViewer.add(files.iterator().next().getDependency());
              notifyListeners();
            }
          }
        }
      }
    });

    final Button removeDependencyButton = new Button(composite, SWT.PUSH);
    removeDependencyButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));
    removeDependencyButton.setText(Messages.getString("wizard.project.page.dependencies.remove"));
    removeDependencyButton.setEnabled(false);
    
    removeDependencyButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        IStructuredSelection selection = (IStructuredSelection) dependencyViewer.getSelection();
        if(selection != null) {
          dependencyViewer.remove(selection.toArray());
          notifyListeners();
        }
      }
    });

    dependencyViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        removeDependencyButton.setEnabled(selection.size() > 0);
      }
    });
  }
  
  public IWizardContainer getContainer() {
    return super.getContainer();
  }

  void updatePage() {
    if(showLocation) {
      boolean defaultWorkspaceLocation = isDefaultWorkspaceLocation();
      locationLabel.setEnabled(!defaultWorkspaceLocation);
      locationCombo.setEnabled(!defaultWorkspaceLocation);
    }

    setPageComplete(isPageValid());
  }

  private boolean isPageValid() {
    setErrorMessage(null);
    if(showLocation && !isDefaultWorkspaceLocation()) {
      if(locationCombo.getText().trim().length()==0) {
        setErrorMessage("Location fied is required");
        return false;
      }
    }
    return true;
  }
  
  public boolean isCheckoutAllProjects() {
    return checkOutAllButton.getSelection();
  }
  
  public boolean isDeveloperConnection() {
    return this.useDeveloperConnectionButton.getSelection();
  }
  
  public boolean isDefaultWorkspaceLocation() {
    return useDefaultWorkspaceLocationButton.getSelection();
  }

  public File getLocation() {
    if(isDefaultWorkspaceLocation()) {
      return ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
    }
    return new File(locationCombo.getText());
  }
  
  /**
   * Notify listeners about changes
   */
  protected void notifyListeners() {
    SelectionChangedEvent event = new SelectionChangedEvent(dependencyViewer, dependencyViewer.getSelection());
    for(ISelectionChangedListener listener : listeners) {
      listener.selectionChanged(event);
    }
  }

  public void addListener(ISelectionChangedListener listener) {
    listeners.add(listener);
  }
  
  /**
   * Returns dependencies currently chosen by the user.
   * 
   * @return dependencies currently chosen by the user. Neither the array nor any of its elements is
   *         <code>null</code>.
   */
  public Dependency[] getDependencies() {
    List<Dependency> dependencies = new ArrayList<Dependency>();
    for(int i = 0; i < dependencyViewer.getTable().getItemCount(); i++ ) {
      Object element = dependencyViewer.getElementAt(i);
      if(element instanceof Dependency) {
        dependencies.add((Dependency) element);
      }
    }
    return dependencies.toArray(new Dependency[dependencies.size()]);
  }
  
//  public IndexedArtifactFile[] getIndexedArtifactFiles() {
//    List artifactFiles = new ArrayList();
//    for(int i = 0; i < artifactViewer.getTable().getItemCount(); i++ ) {
//      Object element = artifactViewer.getElementAt(i);
//      if(element instanceof IndexedArtifactFile) {
//        artifactFiles.add(element);
//      }
//    }
//    return (IndexedArtifactFile[]) artifactFiles.toArray(new IndexedArtifactFile[artifactFiles.size()]);
//  }


  /**
   * Simple <code>LabelProvider</code> attached to the dependency viewer.
   * <p>
   * The information displayed for objects of type <code>Dependency</code> inside the dependency viewer is the
   * following:
   * </p>
   * <p>
   * {groupId} - {artifactId} - {version} - {type}
   * </p>
   */
  public static class ArtifactLabelProvider extends LabelProvider {

    /** The image to show for all objects of type <code>Dependency</code>. */
    private static final Image DEPENDENCY_IMAGE = JavaUI.getSharedImages().getImage(
        ISharedImages.IMG_OBJS_EXTERNAL_ARCHIVE);

    /**
     * {@inheritDoc}
     * <p>
     * The text returned for objects of type <code>Dependency</code> contains the following information about the
     * dependency:
     * </p>
     * <p>
     * {groupId} - {artifactId} - {version} - {type}
     * </p>
     */
    public String getText(Object element) {
      if(element instanceof Dependency) {
        Dependency d = (Dependency) element;
        return d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getVersion() + (d.getClassifier() == null ? "" : ":" + d.getClassifier());
      }
      return super.getText(element);
    }

    public Image getImage(Object element) {
      if(element instanceof Dependency) {
        return DEPENDENCY_IMAGE;
      }
      return super.getImage(element);
    }
  }

  /**
   * Simple <code>ViewerSorter</code> attached to the dependency viewer. Objects of type <code>Dependency</code> are
   * sorted by (1) their groupId and (2) their artifactId.
   */
  public static class DependencySorter extends ViewerSorter {

    /**
     * Two objects of type <code>Dependency</code> are sorted by (1) their groupId and (2) their artifactId.
     */
    public int compare(Viewer viewer, Object e1, Object e2) {
      if(!(e1 instanceof Dependency) || !(e2 instanceof Dependency)) {
        return super.compare(viewer, e1, e2);
      }

      // First of all, compare the group IDs of the two dependencies.
      String group1 = ((Dependency) e1).getGroupId();
      String group2 = ((Dependency) e2).getGroupId();

      int result = (group1 == null) ? -1 : group1.compareToIgnoreCase(group2);

      // If the group IDs match, we sort by the artifact IDs.
      if(result == 0) {
        String artifact1 = ((Dependency) e1).getArtifactId();
        String artifact2 = ((Dependency) e2).getArtifactId();
        result = artifact1 == null ? -1 : artifact1.compareToIgnoreCase(artifact2);
      }

      return result;
    }
  }

}
