/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.embedder.MavenEmbedder;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.Messages;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.project.ProjectImportConfiguration;


/**
 * Wizard page responsible for archetype selection. This wizard page presents the user with a list of available Maven
 * archetypes allowing them to select a suitable archetype for their project.
 */
public class MavenProjectWizardArchetypePage extends AbstractMavenWizardPage {

  public static final Comparator ARCHETYPE_COMPARATOR = new Comparator() {

    public int compare(Object o1, Object o2) {
      Archetype a1 = (Archetype) o1;
      Archetype a2 = (Archetype) o2;

      String g1 = a1.getGroupId();
      String g2 = a2.getGroupId();
      int res = g1.compareTo(g2);
      if(res != 0) {
        return res;
      }

      String i1 = a1.getArtifactId();
      String i2 = a2.getArtifactId();
      res = i1.compareTo(i2);
      if(res != 0) {
        return res;
      }

      String v1 = a1.getVersion();
      String v2 = a2.getVersion();
      if(v1 == null) {
        return v2 == null ? 0 : -1;
      }
      return v1.compareTo(v2);
    }

  };

  /** the archetype table viewer */
  TableViewer viewer;

  Text filterText;

  /** the description value label */
  Text descriptionText;

  Button showLastVersionButton;

  /** the list of available archetypes */
  Collection archetypes;

  Collection lastVersionArchetypes;

  /** a flag indicating if the archetype selection is actually used in the wizard */
  private boolean isUsed = true;

  /**
   * Default constructor. Sets the title and description of this wizard page and marks it as not being complete as user
   * input is required for continuing.
   */
  public MavenProjectWizardArchetypePage(ProjectImportConfiguration projectImportConfiguration) {
    super("MavenProjectWizardArchetypePage", projectImportConfiguration);
    setTitle(Messages.getString("wizard.project.page.archetype.title"));
    setDescription(Messages.getString("wizard.project.page.archetype.description"));
    setPageComplete(false);
  }

  /** Creates the page controls. */
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    composite.setLayout(gridLayout);

    createViewer(composite);

    createAdvancedSettings(composite, new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));

    loadArchetypes("org.apache.maven.archetypes", "maven-archetype-quickstart", "1.0");

    setControl(composite);
  }

  /** Creates the archetype table viewer. */
  private void createViewer(Composite parent) {
    Label filterLabel = new Label(parent, SWT.NONE);
    filterLabel.setLayoutData(new GridData());
    filterLabel.setText("&Filter:");

    QuickViewerFilter quickViewerFilter = new QuickViewerFilter();
    LastVersionFilter versionFilter = new LastVersionFilter();

    filterText = new Text(parent, SWT.BORDER);
    filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    filterText.addModifyListener(quickViewerFilter);
    filterText.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if(e.keyCode == SWT.ARROW_DOWN) {
          viewer.getTable().setFocus();
          viewer.getTable().setSelection(0);

          viewer.setSelection(new StructuredSelection(viewer.getElementAt(0)), true);
        }
      }
    });

    ToolBar toolBar = new ToolBar(parent, SWT.FLAT);
    toolBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

    final ToolItem clearToolItem = new ToolItem(toolBar, SWT.PUSH);
    clearToolItem.setEnabled(false);
    clearToolItem.setImage(MavenPlugin.getImage("icons/clear.gif"));
    clearToolItem.setDisabledImage(MavenPlugin.getImage("icons/clear_disabled.gif"));
    clearToolItem.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        filterText.setText("");
      }
    });

    filterText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        clearToolItem.setEnabled(filterText.getText().length() > 0);
      }
    });

    SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
    GridData gd_sashForm = new GridData(SWT.FILL, SWT.FILL, false, true, 3, 1);
    // gd_sashForm.widthHint = 500;
    gd_sashForm.heightHint = 400;
    sashForm.setLayoutData(gd_sashForm);
    sashForm.setLayout(new GridLayout());

    Composite composite1 = new Composite(sashForm, SWT.NONE);
    GridLayout gridLayout1 = new GridLayout();
    gridLayout1.horizontalSpacing = 0;
    gridLayout1.marginWidth = 0;
    gridLayout1.marginHeight = 0;
    composite1.setLayout(gridLayout1);

    viewer = new TableViewer(composite1, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);

    Table table = viewer.getTable();
    table.setHeaderVisible(true);

    TableColumn column1 = new TableColumn(table, SWT.LEFT);
    column1.setText(Messages.getString("wizard.project.page.archetype.column.groupId"));

    TableColumn column0 = new TableColumn(table, SWT.LEFT);
    column0.setText(Messages.getString("wizard.project.page.archetype.column.artifactId"));

    TableColumn column2 = new TableColumn(table, SWT.LEFT);
    column2.setText(Messages.getString("wizard.project.page.archetype.column.version"));

    GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
    tableData.widthHint = 300;
    tableData.heightHint = 300;
    table.setLayoutData(tableData);

    viewer.setLabelProvider(new ArchetypeLabelProvider());

    viewer.setSorter(new ViewerSorter() {
      public int compare(Viewer viewer, Object e1, Object e2) {
        return ARCHETYPE_COMPARATOR.compare(e1, e2);
      }
    });

    viewer.addFilter(quickViewerFilter);
    viewer.addFilter(versionFilter);

    viewer.setContentProvider(new IStructuredContentProvider() {
      /** Converts the object set into an array. */
      public Object[] getElements(Object inputElement) {
        if(inputElement instanceof Collection) {
          return ((Collection) inputElement).toArray();
        }
        return null;
      }

      /** Disposes of the content provider. */
      public void dispose() {
      }

      /** Handles the input change event. */
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }
    });

    viewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        Archetype archetype = getArchetype();
        if(archetype != null) {
          String repositoryUrl = archetype.getRepository();
          String description = archetype.getDescription();

          String text = description == null ? "" : description;
          text = text.replaceAll("\n", "").replaceAll("\\s{2,}", " ");

          if(repositoryUrl != null) {
            text += text.length() > 0 ? "\n" + repositoryUrl : repositoryUrl;
          }

          descriptionText.setText(text);
          setPageComplete(true);
        } else {
          descriptionText.setText("");
          setPageComplete(false);
        }
      }
    });

    Composite composite2 = new Composite(sashForm, SWT.NONE);
    GridLayout gridLayout2 = new GridLayout();
    gridLayout2.marginHeight = 0;
    gridLayout2.marginWidth = 0;
    gridLayout2.horizontalSpacing = 0;
    composite2.setLayout(gridLayout2);

    descriptionText = new Text(composite2, SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY | SWT.MULTI | SWT.BORDER);
    GridData descriptionTextData = new GridData(SWT.FILL, SWT.FILL, true, true);
    descriptionTextData.heightHint = 40;
    descriptionText.setLayoutData(descriptionTextData);

    sashForm.setWeights(new int[] {90, 10});

    Composite buttonComposite = new Composite(parent, SWT.NONE);
    GridData gd_buttonComposite = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
    buttonComposite.setLayoutData(gd_buttonComposite);
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    gridLayout.numColumns = 2;
    buttonComposite.setLayout(gridLayout);

    showLastVersionButton = new Button(buttonComposite, SWT.CHECK);
    showLastVersionButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    showLastVersionButton.setText("&Show the last version of Archetype only");
    showLastVersionButton.setSelection(true);
    showLastVersionButton.addSelectionListener(versionFilter);

    Button addButton = new Button(buttonComposite, SWT.NONE);
    addButton.setText("&Add Archetype...");
    addButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CustomArchetypeDialog dialog = new CustomArchetypeDialog(getShell(), "Add Archetype", "icons/new_m2_project.gif");
        if(dialog.open()==Window.OK) {
          String archetypeGroupId = dialog.getArchetypeGroupId();
          String archetypeArtifactId = dialog.getArchetypeArtifactId();
          String archetypeVersion = dialog.getArchetypeVersion();
          String repositoryUrl = dialog.getRepositoryUrl();
          downloadArchetype(archetypeGroupId, archetypeArtifactId, archetypeVersion, repositoryUrl);
        }
      }
    });
  }

  /** Loads the available archetypes. */
  void loadArchetypes(final String groupId, final String artifactId, final String version) {
    Job job = new Job(Messages.getString("wizard.project.page.archetype.retrievingArchetypes")) {
      protected IStatus run(IProgressMonitor monitor) {
        try {
          archetypes = new TreeSet(ARCHETYPE_COMPARATOR);
          archetypes.addAll(getArchetypeCatalog().getArchetypes());

          if(archetypes != null) {
            Display.getDefault().asyncExec(new Runnable() {
              public void run() {
                updateViewer(groupId, artifactId, version);
              }
            });
          }

        } catch(CoreException e) {
          // ignore
        }

        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  public Set filterVersions(Collection archetypes) {
    HashMap filteredArchetypes = new HashMap();

    for(Iterator it = archetypes.iterator(); it.hasNext();) {
      Archetype currentArchetype = (Archetype) it.next();

      String key = getArchetypeKey(currentArchetype);
      Archetype archetype = (Archetype) filteredArchetypes.get(key);
      if(archetype == null) {
        filteredArchetypes.put(key, currentArchetype);
      } else {
        DefaultArtifactVersion currentVersion = new DefaultArtifactVersion(currentArchetype.getVersion());
        DefaultArtifactVersion version = new DefaultArtifactVersion(archetype.getVersion());
        if(currentVersion.compareTo(version) > 0) {
          filteredArchetypes.put(key, currentArchetype);
        }
      }
    }

    TreeSet result = new TreeSet(new Comparator() {
      public int compare(Object o1, Object o2) {
        Archetype a1 = (Archetype) o1;
        Archetype a2 = (Archetype) o2;

        String k1 = a1.getGroupId() + ":" + a1.getArtifactId() + ":" + a1.getVersion();
        String k2 = a2.getGroupId() + ":" + a2.getArtifactId() + ":" + a2.getVersion();
        return k1.compareTo(k2);
      }
    });
    result.addAll(filteredArchetypes.values());
    return result;
  }

  private String getArchetypeKey(Archetype archetype) {
    return archetype.getGroupId() + ":" + archetype.getArtifactId();
  }

  ArchetypeCatalog getArchetypeCatalog() throws CoreException {
    MavenEmbedderManager embedderManager = MavenPlugin.getDefault().getMavenEmbedderManager();
    ArchetypeCatalog archetypeCatalog = embedderManager.getArchetypeCatalog();

    if(archetypeCatalog != null && !archetypeCatalog.getArchetypes().isEmpty()) {
      return archetypeCatalog;
    }

    // TODO use better merging strategy
    return embedderManager.getArchetyper().getInternalCatalog();
  }

  /** Sets the flag that the archetype selection is used in the wizard. */
  public void setUsed(boolean isUsed) {
    this.isUsed = isUsed;
  }

  /** Overrides the default to return "true" if the page is not used. */
  public boolean isPageComplete() {
    return !isUsed || super.isPageComplete();
  }

  /** Sets the focus to the table component. */
  public void setVisible(boolean visible) {
    super.setVisible(visible);

    if(visible) {
      viewer.getTable().setFocus();
      Archetype selected = getArchetype();
      if(selected != null) {
        viewer.reveal(selected);
      }
    }
  }

  /** Returns the selected archetype. */
  public Archetype getArchetype() {
    return (Archetype) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
  }

  void updateViewer(String groupId, String artifactId, String version) {
    lastVersionArchetypes = filterVersions(archetypes);

    viewer.setInput(archetypes);

    selectArchetype(groupId, artifactId, version);

    Table table = viewer.getTable();
    int columnCount = table.getColumnCount();
    int width = 0;
    for(int i = 0; i < columnCount; i++ ) {
      TableColumn column = table.getColumn(i);
      column.pack();
      width += column.getWidth();
    }
    GridData tableData = (GridData) table.getLayoutData();
    int oldHint = tableData.widthHint;
    if(width > oldHint) {
      tableData.widthHint = width;
    }
    getShell().pack(true);
    tableData.widthHint = oldHint;
  }

  protected void selectArchetype(String groupId, String artifactId, String version) {
    Archetype archetype = findArchetype(groupId, artifactId, version);

    Table table = viewer.getTable();
    if(archetype != null) {
      viewer.setSelection(new StructuredSelection(archetype));
      viewer.reveal(archetype);

      int n = table.getSelectionIndex();
      table.setSelection(n);
    }
  }
  
  /** Locates an archetype with given ids. */
  protected Archetype findArchetype(String groupId, String artifactId, String version) {
    for(Iterator it = archetypes.iterator(); it.hasNext();) {
      Archetype archetype = (Archetype) it.next();
      if(groupId.equals(archetype.getGroupId()) && artifactId.equals(archetype.getArtifactId())) {
        if(version == null || version.equals(archetype.getVersion())) {
          return archetype;
        }
      }
    }

    return version == null ? null : findArchetype(groupId, artifactId, null);
  }

  protected void downloadArchetype(final String archetypeGroupId, final String archetypeArtifactId,
      final String archetypeVersion, String repositoryUrl) {
    final String archetypeName = archetypeGroupId + ":" + archetypeArtifactId + ":" + archetypeVersion;

    final MavenPlugin plugin = MavenPlugin.getDefault();
    
    final List remoteRepositories;
    if(repositoryUrl.length()==0) {
      remoteRepositories = plugin.getIndexManager().getArtifactRepositories(null, null);
    } else {
      remoteRepositories = Collections.singletonList(new DefaultArtifactRepository( //
          "archetype", repositoryUrl, new DefaultRepositoryLayout(), null, null));
    }

    try {
      getContainer().run(true, true, new IRunnableWithProgress() {
        public void run(IProgressMonitor monitor) throws InterruptedException {
          monitor.beginTask("Downloading Archetype " + archetypeName, IProgressMonitor.UNKNOWN);

          MavenEmbedderManager embedderManager = plugin.getMavenEmbedderManager();
          MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();
          Artifact pomArtifact = embedder.createArtifact(archetypeGroupId, archetypeArtifactId, archetypeVersion, null, "pom");
          Artifact jarArtifact = embedder.createArtifact(archetypeGroupId, archetypeArtifactId, archetypeVersion, null, "jar");
          
          try {
            monitor.subTask("resolving POM...");
            embedder.resolve(pomArtifact, remoteRepositories, embedder.getLocalRepository());
            monitor.worked(1);
            if(monitor.isCanceled()) {
              throw new InterruptedException();
            }
            
            File pomFile = pomArtifact.getFile();
            if(pomFile.exists()) {
              monitor.subTask("resolving JAR...");
              embedder.resolve(jarArtifact, remoteRepositories, embedder.getLocalRepository());
              monitor.worked(1);
              if(monitor.isCanceled()) {
                throw new InterruptedException();
              }
              
              File jarFile = jarArtifact.getFile();

              monitor.subTask("reading project...");
              embedder.readProject(pomFile);
              monitor.worked(1);
              if(monitor.isCanceled()) {
                throw new InterruptedException();
              }

              monitor.subTask("indexing...");
              IndexManager indexManager = plugin.getIndexManager();
              indexManager.addDocument(IndexManager.LOCAL_INDEX, jarFile, //
                  indexManager.getDocumentKey(pomArtifact), jarFile.length(), jarFile.lastModified(), jarFile, //
                  IndexManager.NOT_PRESENT, IndexManager.NOT_PRESENT);
              
              loadArchetypes(archetypeGroupId, archetypeArtifactId, archetypeVersion);
            }
            
          } catch(InterruptedException ex) {
            throw ex;
            
          } catch(final Exception ex) {
            final String msg = "Can't resolve Archetype " + archetypeName;
            MavenPlugin.log(msg, ex);
            getShell().getDisplay().asyncExec(new Runnable() {
              public void run() {
                setErrorMessage(msg + "\n" + ex.toString());
              }
            });

          } finally {
            monitor.done();
            
          }
        }
      });
    
    } catch(InterruptedException ex) {
      // ignore
      
    } catch(InvocationTargetException ex) {
      String msg = "Can't resolve Archetype " + archetypeName;
      MavenPlugin.log(msg, ex);
      setErrorMessage(msg + "\n" + ex.toString());
      
    }
  }
  
  /**
   * ArchetypeLabelProvider
   */
  protected class ArchetypeLabelProvider extends LabelProvider implements ITableLabelProvider {
    /** Returns the element text */
    public String getColumnText(Object element, int columnIndex) {
      if(element instanceof Archetype) {
        Archetype archetype = (Archetype) element;
        switch(columnIndex) {
          case 0:
            return archetype.getGroupId();
          case 1:
            return archetype.getArtifactId();
          case 2:
            return archetype.getVersion();
        }
      }
      return super.getText(element);
    }

    /** Returns the element text */
    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }
  }

  /**
   * QuickViewerFilter
   */
  protected class QuickViewerFilter extends ViewerFilter implements ModifyListener {

    private String currentFilter;

    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if(currentFilter == null || currentFilter.length() == 0) {
        return true;
      }
      Archetype archetype = (Archetype) element;
      return archetype.getGroupId().indexOf(currentFilter) > -1
          || archetype.getArtifactId().indexOf(currentFilter) > -1;
    }

    public void modifyText(ModifyEvent e) {
      this.currentFilter = filterText.getText().trim();
      viewer.refresh();
    }
  }

  protected class LastVersionFilter extends ViewerFilter implements SelectionListener {

    private boolean showLastVersion = true;

    public boolean select(Viewer viewer, Object parentElement, Object element) {
      return showLastVersion ? lastVersionArchetypes.contains(element) : true;
    }

    public void widgetSelected(SelectionEvent e) {
      this.showLastVersion = showLastVersionButton.getSelection();
      viewer.refresh();
      viewer.reveal(getArchetype());
      viewer.getTable().setSelection(viewer.getTable().getSelectionIndex());
      viewer.getTable().setFocus();
    }

    public void widgetDefaultSelected(SelectionEvent e) {
    }
  }

}
