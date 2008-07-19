package org.maven.ide.eclipse.ui.internal.preferences;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.ide.IDE;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.ContainerCustomizer;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.core.Messages;
import org.maven.ide.eclipse.embedder.EmbedderFactory;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.embedder.MavenRuntime;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.ui.internal.editors.MavenFileEditorInput;


/**
 * Maven installations preference page
 * 
 * @author Eugene Kuleshov
 */
public class MavenInstallationsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  final MavenPlugin mavenPlugin;
  
  final MavenRuntimeManager runtimeManager;
  
  final MavenEmbedderManager embedderManager;
  
  final File localRepositoryDir;
  
  MavenRuntime defaultRuntime;
  List<MavenRuntime> runtimes;
  
  CheckboxTableViewer runtimesViewer;
  Text userSettingsText;
  Text globalSettingsText;
  Text localRepositoryText;
  Button globalSettingsOpenButton;


  public MavenInstallationsPreferencePage() {
    setTitle("Maven Installations");

    mavenPlugin = MavenPlugin.getDefault();
    runtimeManager = mavenPlugin.getMavenRuntimeManager();
    embedderManager = mavenPlugin.getMavenEmbedderManager();
    localRepositoryDir = embedderManager.getLocalRepositoryDir();
  }

  public void init(IWorkbench workbench) {
  }

  protected void performDefaults() {
    runtimeManager.reset();
    defaultRuntime = runtimeManager.getDefaultRuntime();
    runtimes = runtimeManager.getMavenRuntimes();
    
    runtimesViewer.setInput(runtimes);
    runtimesViewer.setChecked(defaultRuntime, true);
    runtimesViewer.refresh();  // should listen on property changes instead?
    
    super.performDefaults();
  }

  public boolean performOk() {
    boolean isOk = checkSettings() && super.performOk();
    if(isOk) {
      runtimeManager.setRuntimes(runtimes);
      runtimeManager.setDefaultRuntime(defaultRuntime);
      
      runtimeManager.setUserSettingsFile(userSettingsText.getText());
      // runtimeManager.setGlobalSettingsFile(globalSettingsText.getText());

      embedderManager.invalidateMavenSettings();
      
      File newRepositoryDir = embedderManager.getLocalRepositoryDir();
      if(!newRepositoryDir.equals(localRepositoryDir)) {
        mavenPlugin.getIndexManager().scheduleIndexUpdate(IndexManager.LOCAL_INDEX, true, 0L);
      }
    }
    return isOk;
  }
  
  protected Control createContents(Composite parent) {
    noDefaultAndApplyButton();
    
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout(4, false);
    gridLayout.marginBottom = 5;
    gridLayout.marginRight = 5;
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    composite.setLayout(gridLayout);

    Link link = new Link(composite, SWT.NONE);
    link.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 4, 1));
    link.setText("Configure Maven <a href=\"http://maven.apache.org/\">installations</a> " +
    		"and <a href=\"http://maven.apache.org/settings.html\">settings</a>:");
    link.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        try {
          URL url = new URL(e.text);
          IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
          browser.openURL(url);
        } catch(MalformedURLException ex) {
          MavenLogger.log("Malformed URL", ex);
        } catch(PartInitException ex) {
          MavenLogger.log(ex);
        }
      }
    });

    runtimesViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.FULL_SELECTION);

    runtimesViewer.setLabelProvider(new RuntimesLabelProvider());

    runtimesViewer.setContentProvider(new IStructuredContentProvider() {

      @SuppressWarnings("unchecked")
      public Object[] getElements(Object input) {
        if(input instanceof List) {
          List list = (List) input;
          if(list.size()>0) {
            return list.toArray(new MavenRuntime[list.size()]);
          }
        }
        return new Object[0];
      }
      
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      public void dispose() {
      }
      
    });

    Table table = runtimesViewer.getTable();
    table.setLinesVisible(false);
    table.setHeaderVisible(false);
    GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 3);
    gd_table.heightHint = 151;
    gd_table.widthHint = 333;
    table.setLayoutData(gd_table);

    TableColumn typeColumn = new TableColumn(table, SWT.NONE);
    typeColumn.setWidth(250);
    typeColumn.setText("");

    Button addButton = new Button(composite, SWT.NONE);
    addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    addButton.setText("&Add...");
    addButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        DirectoryDialog dlg = new DirectoryDialog(getShell());
        dlg.setText("Maven Installation");
        dlg.setMessage("Select Maven installation directory");
        String dir = dlg.open();
        if (dir != null) {
          MavenRuntime runtime = MavenRuntime.createExternalRuntime(dir);
          if(runtimes.contains(runtime)) {
            MessageDialog.openError(getShell(), "Maven Install", "Selected Maven install is already registered");
          } else {
            runtimes.add(runtime);
            runtimesViewer.refresh();
            runtimesViewer.setSelection(new StructuredSelection(runtime));
          }
        }
      }
    });

    final Button editButton = new Button(composite, SWT.NONE);
    editButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    editButton.setEnabled(false);
    editButton.setText("&Edit...");
    editButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        MavenRuntime runtime = getSelectedRuntime();
        DirectoryDialog dlg = new DirectoryDialog(Display.getCurrent().getActiveShell());
        dlg.setText("Maven Installation");
        dlg.setMessage("Select Maven installation directory");
        dlg.setFilterPath(runtime.getLocation());
        String dir = dlg.open();
        if (dir != null && !dir.equals(runtime.getLocation())) {
          MavenRuntime newRuntime = MavenRuntime.createExternalRuntime(dir);
          if(runtimes.contains(newRuntime)) {
            MessageDialog.openError(getShell(), "Maven Install", "Selected Maven install is already registered");
          } else {
            runtimes.set(runtimes.indexOf(runtime), newRuntime);
            runtimesViewer.refresh();
          }
        }
      }
    });

    final Button removeButton = new Button(composite, SWT.NONE);
    removeButton.setEnabled(false);
    removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    removeButton.setText("&Remove");
    removeButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        MavenRuntime runtime = getSelectedRuntime();
        runtimes.remove(runtime);
        runtimesViewer.refresh();
        
        if(runtimesViewer.getSelection().isEmpty()) {
          defaultRuntime = runtimeManager.getRuntime(MavenRuntimeManager.EMBEDDED);
          runtimesViewer.setChecked(defaultRuntime, true);
        }
      }
    });
    
    runtimesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        if(runtimesViewer.getSelection() instanceof IStructuredSelection) {
          MavenRuntime runtime = getSelectedRuntime();
          boolean isEnabled = runtime != null && runtime.isEditable();
          removeButton.setEnabled(isEnabled);
          editButton.setEnabled(isEnabled);
        }
      }
    });
    
    runtimesViewer.addCheckStateListener(new ICheckStateListener() {
      public void checkStateChanged(CheckStateChangedEvent event) {
        runtimesViewer.setAllChecked(false);
        runtimesViewer.setChecked(event.getElement(), true);
        defaultRuntime = (MavenRuntime) event.getElement();
        
        if(!defaultRuntime.isEditable()) {
          globalSettingsText.setText("");
          globalSettingsOpenButton.setEnabled(false);
        } else {
          String globalSettings = defaultRuntime.getSettings();
          globalSettingsText.setText(globalSettings==null ? "" : globalSettings);
          globalSettingsOpenButton.setEnabled(true);
        }
        initLocalRepository();
      }
    });

    Text noteLabel = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
    GridData noteLabelData = new GridData(SWT.FILL, SWT.TOP, false, false, 4, 1);
    noteLabelData.horizontalIndent = 12;
    noteLabelData.widthHint = 100;
    noteLabel.setLayoutData(noteLabelData);
    noteLabel.setText("The checked installation will be used to launch Maven by default. " + //
        "It also points to the location of the Global Settings. " + //
        "Note that Embedded runtime is always used for dependency resolution, but " +
        "can't use Global Settings when it is used to launch Maven.");
    noteLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));

    Label userSettingsLabel = new Label(composite, SWT.NONE);
    GridData gd_userSettingsLabel = new GridData();
    gd_userSettingsLabel.verticalIndent = 5;
    userSettingsLabel.setLayoutData(gd_userSettingsLabel);
    userSettingsLabel.setText("User &Settings:");

    userSettingsText = new Text(composite, SWT.BORDER);
    GridData gd_userSettingsText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_userSettingsText.verticalIndent = 5;
    gd_userSettingsText.widthHint = 100;
    userSettingsText.setLayoutData(gd_userSettingsText);
    userSettingsText.setText(MavenEmbedder.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath());

    Button userSettingsBrowseButton = new Button(composite, SWT.NONE);
    GridData gd_userSettingsBrowseButton = new GridData(SWT.FILL, SWT.CENTER, false, false);
    gd_userSettingsBrowseButton.verticalIndent = 5;
    userSettingsBrowseButton.setLayoutData(gd_userSettingsBrowseButton);
    userSettingsBrowseButton.setText("&Browse...");
    userSettingsBrowseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
        if(userSettingsText.getText().length()>0) {
          dialog.setFileName(userSettingsText.getText());
        }
        String file = dialog.open();
        if(file != null) {
          file = file.trim();
          if(file.length() > 0) {
            userSettingsText.setText(file);
            initLocalRepository();
            checkSettings();
          }
        }
      }
    });

    Button userSettingsOpenButton = new Button(composite, SWT.NONE);
    GridData gd_userSettingsOpenButton = new GridData(SWT.FILL, SWT.CENTER, false, false);
    gd_userSettingsOpenButton.verticalIndent = 5;
    userSettingsOpenButton.setLayoutData(gd_userSettingsOpenButton);
    userSettingsOpenButton.setText("&Open...");
    userSettingsOpenButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        openEditor(userSettingsText.getText());
      }
    });

    Label globalSettingsLabel = new Label(composite, SWT.NONE);
    globalSettingsLabel.setLayoutData(new GridData());
    globalSettingsLabel.setText("Global Settings:");

    globalSettingsText = new Text(composite, SWT.READ_ONLY | SWT.BORDER);
    globalSettingsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    globalSettingsText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent modifyevent) {
        initLocalRepository();
        checkSettings();
      }
    });

//    globalSettingsBrowseButton = new Button(composite, SWT.NONE);
//    globalSettingsBrowseButton.setText("&Browse...");
//    GridData gd_localSettingsBrowseButton = new GridData();
//    gd_localSettingsBrowseButton.verticalIndent = 5;
//    globalSettingsBrowseButton.setLayoutData(gd_localSettingsBrowseButton);
//    globalSettingsBrowseButton.addSelectionListener(new SelectionAdapter() {
//      public void widgetSelected(SelectionEvent e) {
//        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
//        if(globalSettingsText.getText().length()>0) {
//          dialog.setFileName(globalSettingsText.getText());
//        }
//        String file = dialog.open();
//        if(file != null) {
//          file = file.trim();
//          if(file.length() > 0) {
//            globalSettingsText.setText(file);
//            initLocalRepository();
//            checkSettings();
//          }
//        }
//      }
//    });

    globalSettingsOpenButton = new Button(composite, SWT.NONE);
    globalSettingsOpenButton.setText("Ope&n...");
    globalSettingsOpenButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    globalSettingsOpenButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        String globalSettings = globalSettingsText.getText().trim();
        if(globalSettings.length()==0) {
          globalSettings = defaultRuntime.getSettings();
        }
        if(globalSettings!=null && globalSettings.length()>0) {
          openEditor(globalSettings);
        }
      }
    });

    Label localRepositoryLabel = new Label(composite, SWT.NONE);
    localRepositoryLabel.setText("Local Repository:");

    localRepositoryText = new Text(composite, SWT.READ_ONLY | SWT.BORDER);
    localRepositoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
    
    GridData buttonsCompositeGridData = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 4, 1);

    Composite buttonsComposite = new Composite(composite, SWT.NONE);
    RowLayout rowLayout = new RowLayout();
    rowLayout.spacing = 5;
    rowLayout.marginTop = 0;
    rowLayout.marginRight = 0;
    rowLayout.marginLeft = 0;
    rowLayout.marginBottom = 0;
    buttonsComposite.setLayout(rowLayout);
    buttonsComposite.setLayoutData(buttonsCompositeGridData);

    Button refreshButton = new Button(buttonsComposite, SWT.NONE);
    refreshButton.setText(Messages.getString("preferences.refreshButton"));
    refreshButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        initLocalRepository();
        checkSettings();
        mavenPlugin.getMavenEmbedderManager().invalidateMavenSettings();
      }
    });

    Button reindexButton = new Button(buttonsComposite, SWT.NONE);
    reindexButton.setText(Messages.getString("preferences.reindexButton"));
    reindexButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        embedderManager.invalidateMavenSettings();
        mavenPlugin.getIndexManager().scheduleIndexUpdate(IndexManager.LOCAL_INDEX, true, 0L);
      }
    });
    
    defaultRuntime = runtimeManager.getDefaultRuntime();
    runtimes = runtimeManager.getMavenRuntimes();
    
    runtimesViewer.setInput(runtimes);
    runtimesViewer.setChecked(defaultRuntime, true);
    runtimesViewer.refresh();  // should listen on property changes instead?
    
    String globalSettings = runtimeManager.getGlobalSettingsFile();
    globalSettingsText.setText(globalSettings==null ? "" : globalSettings);
    
    String userSettings = runtimeManager.getUserSettingsFile();
    userSettingsText.setText(userSettings==null ? "" : userSettings);
    
    checkSettings();
    // initLocalRepository();
    
    return composite;
  }
  
  void initLocalRepository() {
    // XXX this is quite slow. is there a faster way?
    final String globalSettings = globalSettingsText.getText().trim();
    new Job("Reading local repository") {
      protected IStatus run(IProgressMonitor monitor) {
        String settings = globalSettings.length()==0 ? defaultRuntime.getSettings() : globalSettings;
        final File localRepository = getLocalRepository(settings);
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            if(!localRepositoryText.isDisposed()) {
              localRepositoryText.setText(localRepository==null ? "" : localRepository.getAbsolutePath());
            }
          }
        });
        return Status.OK_STATUS;
      }
      
      private File getLocalRepository(String globalSettings) {
        MavenEmbedder embedder = null;
        try {
          ContainerCustomizer customizer = EmbedderFactory.createExecutionCustomizer();
          Configuration configuration = embedderManager.createDefaultConfiguration(customizer);
          if(globalSettings!=null) {
            configuration.setGlobalSettingsFile(new File(globalSettings));
          }
          configuration.setUserSettingsFile(null);
          
          embedder = EmbedderFactory.createMavenEmbedder(configuration, null);
          
          ArtifactRepository localRepository = embedder.getLocalRepository();
          if(localRepository != null) {
            return new File(localRepository.getBasedir());
          }
          
        } catch(MavenEmbedderException ex) {
          MavenLogger.log("Can't create Maven embedder", ex);
        } finally {
          try {
            if(embedder!=null) {
              embedder.stop();
            }
          } catch(MavenEmbedderException ex) {
            MavenLogger.log("Can't stop Maven embedder", ex);
          }
        }
        return null;
      }
    }.schedule();
  }

  boolean checkSettings() {
    setErrorMessage(null);
    setMessage(null);
  
    Configuration configuration = embedderManager.createDefaultConfiguration(null);
    configuration.setClassLoader(Thread.currentThread().getContextClassLoader());
    
    String globalSettings = globalSettingsText.getText();
    if(globalSettings != null && globalSettings.length() > 0) {
      File globalSettingsFile = new File(globalSettings);
      if(globalSettingsFile.exists()) {
        configuration.setGlobalSettingsFile(globalSettingsFile);
      } else {
        setMessage("Global settings file don't exists", IMessageProvider.WARNING);
      }
    }
    
    String userSettings = userSettingsText.getText();
    if(userSettings != null && userSettings.length() > 0) { 
      File userSettingsFile = new File(userSettings);
      if(userSettingsFile.exists()) {
        configuration.setUserSettingsFile(userSettingsFile);
      } else {
        setMessage("User settings file don't exists", IMessageProvider.WARNING);
      }
    }
  
    ConfigurationValidationResult result = MavenEmbedder.validateConfiguration(configuration);
    if(!result.isValid()) {
      Exception uex = result.getUserSettingsException();
      Exception gex = result.getGlobalSettingsException();
      if(uex!=null) {
        setMessage("Unable to parse user settings file; " + uex.toString(), IMessageProvider.WARNING);
        return false;
      } else if(gex!=null) {
        setMessage("Unable to parse global settings file; " + gex.toString(), IMessageProvider.WARNING);
        return false;
      } else {
        setMessage("User configuration is invalid", IMessageProvider.WARNING);
        return false;
      }
    }
  
    return true;
  }

  void openEditor(final String fileName) {
    // XXX create new settings.xml if does not exist
    
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    IWorkbenchPage page = window.getActivePage();
    
    IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor("settings.xml");
    
    try {
      final IEditorPart editor = IDE.openEditor(page, new MavenFileEditorInput(fileName), desc.getId());
      editor.addPropertyListener(new IPropertyListener() {
        public void propertyChanged(Object source, int propId) {
          if(!editor.isDirty()) {
            mavenPlugin.getConsole().logMessage("Refreshing settings " + fileName);
            embedderManager.invalidateMavenSettings();
          }
        }
      });
      
    } catch(PartInitException ex) {
      MavenLogger.log(ex);
    }
  }

  MavenRuntime getSelectedRuntime() {
    IStructuredSelection selection = (IStructuredSelection) runtimesViewer.getSelection();
    return (MavenRuntime) selection.getFirstElement();
  }


  static class RuntimesLabelProvider implements ITableLabelProvider, IColorProvider {
    
    public String getColumnText(Object element, int columnIndex) {
      MavenRuntime runtime = (MavenRuntime) element;
      return runtime.toString();
    }
    
    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }

    public Color getBackground(Object element) {
      return null;
    }

    public Color getForeground(Object element) {
      MavenRuntime runtime = (MavenRuntime) element;
      if(!runtime.isEditable()) {
        return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
      }
      return null;
    }
    
    public void dispose() {
    }

    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    public void addListener(ILabelProviderListener listener) {
    }
    
    public void removeListener(ILabelProviderListener listener) {
    }

  }
  
}
