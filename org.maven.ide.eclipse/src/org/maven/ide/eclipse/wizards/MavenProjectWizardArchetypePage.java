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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardContainer;
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
import org.eclipse.ui.dialogs.PreferencesUtil;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import org.maven.ide.eclipse.MavenImages;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.archetype.ArchetypeCatalogFactory;
import org.maven.ide.eclipse.archetype.ArchetypeManager;
import org.maven.ide.eclipse.archetype.ArchetypeCatalogFactory.NexusIndexerCatalogFactory;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.core.Messages;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.index.IIndex;
import org.maven.ide.eclipse.index.IMutableIndex;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.project.ProjectImportConfiguration;


/**
 * Maven Archetype selection wizard page presents the user with a list of available Maven
 * Archetypes available for creating new project.
 */
public class MavenProjectWizardArchetypePage extends AbstractMavenWizardPage implements IPropertyChangeListener {

  private static final String KEY_CATALOG = "catalog";

  public static final Comparator<Archetype> ARCHETYPE_COMPARATOR = new Comparator<Archetype>() {

    public int compare(Archetype a1, Archetype a2) {
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

  ComboViewer catalogsComboViewer;
  
  Text filterText;

  /** the archetype table viewer */
  TableViewer viewer;
  
  /** the description value label */
  Text descriptionText;

  Button showLastVersionButton;

  Button addArchetypeButton;
  
  /** the list of available archetypes */
  Collection<Archetype> archetypes;

  Collection<Archetype> lastVersionArchetypes;

  /** a flag indicating if the archetype selection is actually used in the wizard */
  private boolean isUsed = true;

  ArchetypeCatalogFactory catalogFactory = null;

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
    composite.setLayout(new GridLayout(3, false));

    createViewer(composite);

    createAdvancedSettings(composite, new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));


    setControl(composite);
  }

  /** Creates the archetype table viewer. */
  private void createViewer(Composite parent) {
    Label catalogsLabel = new Label(parent, SWT.NONE);
    catalogsLabel.setText("C&atalog:");

    Composite catalogsComposite = new Composite(parent, SWT.NONE);
    GridLayout catalogsCompositeLayout = new GridLayout();
    catalogsCompositeLayout.marginWidth = 0;
    catalogsCompositeLayout.marginHeight = 0;
    catalogsCompositeLayout.numColumns = 2;
    catalogsComposite.setLayout(catalogsCompositeLayout);
    catalogsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));

    catalogsComboViewer = new ComboViewer(catalogsComposite);
    catalogsComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    catalogsComboViewer.setContentProvider(new IStructuredContentProvider() {
      public Object[] getElements(Object input) {
        if(input instanceof Collection) {
          return ((Collection<?>) input).toArray();
        }
        return new Object[0];
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      public void dispose() {
      }
    });
    
    catalogsComboViewer.setLabelProvider(new LabelProvider() {
      public String getText(Object element) {
        if(element instanceof ArchetypeCatalogFactory) {
          return ((ArchetypeCatalogFactory) element).getDescription();
        }
        return super.getText(element);
      }
    });
    
    catalogsComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        ISelection selection = event.getSelection();
        if(selection instanceof IStructuredSelection) {
          catalogFactory = (ArchetypeCatalogFactory) ((IStructuredSelection) selection).getFirstElement();
          boolean canModifyCatalog = NexusIndexerCatalogFactory.ID.equals(catalogFactory.getId());
          addArchetypeButton.setEnabled(canModifyCatalog);
          loadArchetypes("org.apache.maven.archetypes", "maven-archetype-quickstart", "1.0");
        } else {
          catalogFactory = null;
          addArchetypeButton.setEnabled(false);
          loadArchetypes(null, null, null);
        }
      }
    });
    
    final ArchetypeManager archetypeManager = MavenPlugin.getDefault().getArchetypeManager();
    catalogsComboViewer.setInput(archetypeManager.getArchetypeCatalogs());
    
    Button configureButton = new Button(catalogsComposite, SWT.NONE);
    configureButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    configureButton.setText("Con&figure...");
    configureButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        PreferencesUtil.createPreferenceDialogOn(getShell(),
            "org.maven.ide.eclipse.preferences.MavenArchetypesPreferencePage", null, null).open(); //$NON-NLS-1$

        if(catalogFactory == null || archetypeManager.getArchetypeCatalogFactory(catalogFactory.getId()) == null) {
          catalogFactory = archetypeManager.getArchetypeCatalogFactory(NexusIndexerCatalogFactory.ID);
        }
        
        catalogsComboViewer.setInput(archetypeManager.getArchetypeCatalogs());
        catalogsComboViewer.setSelection(new StructuredSelection(catalogFactory));
      }
    });
    
    
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
    clearToolItem.setImage(MavenImages.IMG_CLEAR);
    clearToolItem.setDisabledImage(MavenImages.IMG_CLEAR_DISABLED);
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
    gd_sashForm.heightHint = 200;
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
    table.setData("name", "archetypesTable");
    table.setHeaderVisible(true);

    TableColumn column1 = new TableColumn(table, SWT.LEFT);
    column1.setWidth(150);
    column1.setText(Messages.getString("wizard.project.page.archetype.column.groupId"));

    TableColumn column0 = new TableColumn(table, SWT.LEFT);
    column0.setWidth(150);
    column0.setText(Messages.getString("wizard.project.page.archetype.column.artifactId"));

    TableColumn column2 = new TableColumn(table, SWT.LEFT);
    column2.setWidth(100);
    column2.setText(Messages.getString("wizard.project.page.archetype.column.version"));

    GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
    tableData.widthHint = 400;
    tableData.heightHint = 200;
    table.setLayoutData(tableData);

    viewer.setLabelProvider(new ArchetypeLabelProvider());

    viewer.setSorter(new ViewerSorter() {
      public int compare(Viewer viewer, Object e1, Object e2) {
        return ARCHETYPE_COMPARATOR.compare((Archetype) e1, (Archetype) e2);
      }
    });

    viewer.addFilter(quickViewerFilter);
    viewer.addFilter(versionFilter);

    viewer.setContentProvider(new IStructuredContentProvider() {
      public Object[] getElements(Object inputElement) {
        if(inputElement instanceof Collection) {
          return ((Collection<?>) inputElement).toArray();
        }
        return new Object[0];
      }

      public void dispose() {
      }

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
    
    viewer.addOpenListener(new IOpenListener() {
      public void open(OpenEvent openevent) {
        if (canFlipToNextPage()) {
          getContainer().showPage(getNextPage());
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

    sashForm.setWeights(new int[] {80, 20});

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

    addArchetypeButton = new Button(buttonComposite, SWT.NONE);
    addArchetypeButton.setText("&Add Archetype...");
    addArchetypeButton.setData("name", "addArchetypeButton");
    addArchetypeButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CustomArchetypeDialog dialog = new CustomArchetypeDialog(getShell(), "Add Archetype");
        if(dialog.open()==Window.OK) {
          String archetypeGroupId = dialog.getArchetypeGroupId();
          String archetypeArtifactId = dialog.getArchetypeArtifactId();
          String archetypeVersion = dialog.getArchetypeVersion();
          String repositoryUrl = dialog.getRepositoryUrl();
          downloadArchetype(archetypeGroupId, archetypeArtifactId, archetypeVersion, repositoryUrl);
        }
      }
    });
    MavenPlugin.getDefault().addPropertyChangeListener(this);
  }
  
  protected IWizardContainer getContainer() {
    return super.getContainer();
  }

  public void addArchetypeSelectionListener(ISelectionChangedListener listener) {
    viewer.addSelectionChangedListener(listener);
  }

  public void dispose() {
    if(dialogSettings != null && catalogFactory!=null) {
      dialogSettings.put(KEY_CATALOG, catalogFactory.getId());
    }
    MavenPlugin.getDefault().removePropertyChangeListener(this);
    super.dispose();
  }
  
  /** Loads the available archetypes. */
  void loadArchetypes(final String groupId, final String artifactId, final String version) {
    Job job = new Job(Messages.getString("wizard.project.page.archetype.retrievingArchetypes")) {
      protected IStatus run(IProgressMonitor monitor) {
        try {
          @SuppressWarnings("unchecked")
          List<Archetype> catalogArchetypes = getArchetypeCatalog() == null ? null : getArchetypeCatalog().getArchetypes();

          if(catalogArchetypes == null || catalogArchetypes.size() == 0){
            Display.getDefault().asyncExec(new Runnable(){
              public void run(){
                if(catalogFactory != null && "Nexus Indexer".equals(catalogFactory.getDescription())){
                  setErrorMessage("No archetypes currently available. The archetype list will refresh when the indexes finish updating.");
                }
              }
            });
          } else {
            Display.getDefault().asyncExec(new Runnable(){
              public void run(){
                setErrorMessage(null);
              }
            });
          }
          if(catalogArchetypes == null){
            return Status.CANCEL_STATUS;
          }
          archetypes = new TreeSet<Archetype>(ARCHETYPE_COMPARATOR);
          archetypes.addAll(catalogArchetypes);
          
          Display.getDefault().asyncExec(new Runnable() {
            public void run() {
              updateViewer(groupId, artifactId, version);
            }
          });

        } catch(CoreException e) {
          // ignore
        }

        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  public Set<Archetype> filterVersions(Collection<Archetype> archetypes) {
    HashMap<String, Archetype> filteredArchetypes = new HashMap<String, Archetype>();

    for(Archetype currentArchetype : archetypes) {
      String key = getArchetypeKey(currentArchetype);
      Archetype archetype = filteredArchetypes.get(key);
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

    TreeSet<Archetype> result = new TreeSet<Archetype>(new Comparator<Archetype>() {
      public int compare(Archetype a1, Archetype a2) {
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
    return catalogFactory==null ? null: catalogFactory.getArchetypeCatalog();
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
      ArchetypeManager archetypeManager = MavenPlugin.getDefault().getArchetypeManager();
      String catalogId = dialogSettings.get(KEY_CATALOG);
      if(catalogId != null) {
        catalogFactory = archetypeManager.getArchetypeCatalogFactory(catalogId);
      }
      if(catalogFactory == null) {
        catalogFactory = archetypeManager.getArchetypeCatalogFactory(NexusIndexerCatalogFactory.ID);
      }
      catalogsComboViewer.setSelection(new StructuredSelection(catalogFactory));
      
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
    for(Archetype archetype : archetypes) {
      if(archetype.getGroupId().equals(groupId) && archetype.getArtifactId().equals(artifactId)) {
        if(version == null || version.equals(archetype.getVersion())) {
          return archetype;
        }
      }
    }

    return version == null ? null : findArchetype(groupId, artifactId, null);
  }

  protected void downloadArchetype(final String archetypeGroupId, final String archetypeArtifactId,
      final String archetypeVersion, final String repositoryUrl) {
    final String archetypeName = archetypeGroupId + ":" + archetypeArtifactId + ":" + archetypeVersion;

    try {
      getContainer().run(true, true, new IRunnableWithProgress() {
        public void run(IProgressMonitor monitor) throws InterruptedException {
          monitor.beginTask("Downloading Archetype " + archetypeName, IProgressMonitor.UNKNOWN);

          try {
            final IMaven maven = MavenPlugin.lookup(IMaven.class);

            final MavenPlugin plugin = MavenPlugin.getDefault();
            
            final List<ArtifactRepository> remoteRepositories;
            if(repositoryUrl.length()==0) {
              remoteRepositories = maven.getArtifactRepositories(monitor); // XXX should use ArchetypeManager.getArhetypeRepositories()
            } else {
              ArtifactRepository repository = new DefaultArtifactRepository( //
                  "archetype", repositoryUrl, new DefaultRepositoryLayout(), null, null);
              remoteRepositories = Collections.singletonList(repository);
            }
            
            monitor.subTask("resolving POM...");
            Artifact pomArtifact = maven.resolve(archetypeGroupId, archetypeArtifactId, archetypeVersion, "pom", null, remoteRepositories, monitor);
            monitor.worked(1);
            if(monitor.isCanceled()) {
              throw new InterruptedException();
            }

            File pomFile = pomArtifact.getFile();
            if(pomFile.exists()) {
              monitor.subTask("resolving JAR...");
              Artifact jarArtifact = maven.resolve(archetypeGroupId, archetypeArtifactId, archetypeVersion, null, "jar", remoteRepositories, monitor);
              monitor.worked(1);
              if(monitor.isCanceled()) {
                throw new InterruptedException();
              }
              
              File jarFile = jarArtifact.getFile();

              monitor.subTask("reading project...");
              maven.readProject(pomFile, monitor); // TODO what's the point of this?
              monitor.worked(1);
              if(monitor.isCanceled()) {
                throw new InterruptedException();
              }

              monitor.subTask("indexing...");
              IndexManager indexManager = plugin.getIndexManager();
              IMutableIndex localIndex = indexManager.getLocalIndex();
              localIndex.addArtifact(jarFile, //
                  new ArtifactKey(pomArtifact), jarFile.length(), jarFile.lastModified(), jarFile, //
                  IIndex.NOT_PRESENT, IIndex.NOT_PRESENT);
              
              loadArchetypes(archetypeGroupId, archetypeArtifactId, archetypeVersion);
            }
            
          } catch(InterruptedException ex) {
            throw ex;
            
          } catch(final Exception ex) {
            final String msg = "Can't resolve Archetype " + archetypeName;
            MavenLogger.log(msg, ex);
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
      MavenLogger.log(msg, ex);
      setErrorMessage(msg + "\n" + ex.toString());
      
    }
  }
  
  /**
   * ArchetypeLabelProvider
   */
  protected static class ArchetypeLabelProvider extends LabelProvider implements ITableLabelProvider {
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

  /* (non-Javadoc)
   * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent event) {
    if(IMavenConstants.INDEX_UPDATE_PROP.equals(event.getProperty())){
      Display.getDefault().asyncExec(new Runnable(){
        public void run(){
          loadArchetypes("org.apache.maven.archetypes", "maven-archetype-quickstart", "1.0");
        }
      });
    }
  }

}
