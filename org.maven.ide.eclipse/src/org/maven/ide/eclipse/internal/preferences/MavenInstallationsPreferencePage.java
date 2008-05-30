package org.maven.ide.eclipse.internal.preferences;

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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
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
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.EmbedderFactory;
import org.maven.ide.eclipse.embedder.MavenRuntime;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.internal.embedder.PluginConsoleMavenEmbeddedLogger;


/**
 * Maven installations preference page
 * 
 * @author Eugene Kuleshov
 */
public class MavenInstallationsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  final MavenPlugin mavenPlugin;
  
  final MavenRuntimeManager runtimeManager;
  
  final File localRepositoryDir;
  
  MavenRuntime defaultRuntime;
  List<MavenRuntime> runtimes;
  
  CheckboxTableViewer runtimesViewer;
  Text userSettingsText;
  Text globalSettingsText;
  Text localRepositoryText;

  public MavenInstallationsPreferencePage() {
    setTitle("Maven Installations");

    mavenPlugin = MavenPlugin.getDefault();
    runtimeManager = mavenPlugin.getMavenRuntimeManager();
    
    localRepositoryDir = mavenPlugin.getMavenEmbedderManager().getLocalRepositoryDir();
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
      
      runtimeManager.setGlobalSettingsFile(globalSettingsText.getText());

      mavenPlugin.getMavenEmbedderManager().invalidateMavenSettings();
      
      File newRepositoryDir = mavenPlugin.getMavenEmbedderManager().getLocalRepositoryDir();
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
          MavenPlugin.log("Malformed URL", ex);
        } catch(PartInitException ex) {
          MavenPlugin.log(ex);
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
    gd_table.heightHint = 50;
    gd_table.widthHint = 100;
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
    removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));
    removeButton.setText("&Remove");
    removeButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        MavenRuntime runtime = getSelectedRuntime();
        runtimes.remove(runtime);
        runtimesViewer.refresh();
        
        if(runtimesViewer.getSelection().isEmpty()) {
          defaultRuntime = MavenRuntime.EMBEDDED;
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
        
        if(defaultRuntime.getSettings()!=null) {
          globalSettingsText.setText(defaultRuntime.getSettings());
        }
        initLocalRepository();
      }
    });

    Text label = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
    GridData gd_label = new GridData(SWT.FILL, SWT.TOP, false, false, 4, 1);
    gd_label.widthHint = 100;
    label.setLayoutData(gd_label);
    label.setText("The checked installation will be used to launch Maven by default. " +
        "It also points to the location of the Global Settings.");

    Label globalSettingsLabel = new Label(composite, SWT.NONE);
    GridData gd_globalSettingsLabel = new GridData();
    gd_globalSettingsLabel.verticalIndent = 5;
    globalSettingsLabel.setLayoutData(gd_globalSettingsLabel);
    globalSettingsLabel.setText("Global &Settings:");

    globalSettingsText = new Text(composite, SWT.BORDER);
    GridData gd_globalSettingsText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_globalSettingsText.verticalIndent = 5;
    globalSettingsText.setLayoutData(gd_globalSettingsText);
    globalSettingsText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent modifyevent) {
        initLocalRepository();
        checkSettings();
      }
    });

    Button globalSettingsBrowseButton = new Button(composite, SWT.NONE);
    globalSettingsBrowseButton.setText("&Browse...");
    GridData gd_localSettingsBrowseButton = new GridData();
    gd_localSettingsBrowseButton.verticalIndent = 5;
    globalSettingsBrowseButton.setLayoutData(gd_localSettingsBrowseButton);
    globalSettingsBrowseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
        if(globalSettingsText.getText().length()>0) {
          dialog.setFileName(globalSettingsText.getText());
        }
        String file = dialog.open();
        if(file != null) {
          file = file.trim();
          if(file.length() > 0) {
            globalSettingsText.setText(file);
            initLocalRepository();
            checkSettings();
          }
        }
      }
    });

    Button globalSettingsOpenButton = new Button(composite, SWT.NONE);
    globalSettingsOpenButton.setText("Ope&n...");
    GridData gd_globalSettingsEditButton = new GridData(SWT.FILL, SWT.CENTER, false, false);
    gd_globalSettingsEditButton.verticalIndent = 5;
    globalSettingsOpenButton.setLayoutData(gd_globalSettingsEditButton);
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

    Label userSettingsLabel = new Label(composite, SWT.NONE);
    userSettingsLabel.setLayoutData(new GridData());
    userSettingsLabel.setText("User Settings:");

    userSettingsText = new Text(composite, SWT.READ_ONLY | SWT.BORDER);
    GridData gd_userSettingsText = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    gd_userSettingsText.widthHint = 100;
    userSettingsText.setLayoutData(gd_userSettingsText);
    userSettingsText.setText(MavenEmbedder.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath());

    Button userSettingsOpenButton = new Button(composite, SWT.NONE);
    userSettingsOpenButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    userSettingsOpenButton.setText("&Open...");
    userSettingsOpenButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        openEditor(userSettingsText.getText());
      }
    });

    Label localRepositoryLabel = new Label(composite, SWT.NONE);
    localRepositoryLabel.setText("Local Repository:");

    localRepositoryText = new Text(composite, SWT.READ_ONLY | SWT.BORDER);
    localRepositoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
    
    defaultRuntime = runtimeManager.getDefaultRuntime();
    runtimes = runtimeManager.getMavenRuntimes();
    
    runtimesViewer.setInput(runtimes);
    runtimesViewer.setChecked(defaultRuntime, true);
    runtimesViewer.refresh();  // should listen on property changes instead?
    
    initLocalRepository();
    checkSettings();
    
    return composite;
  }
  
  void initLocalRepository() {
    // XXX this is quite slow. is there a faster way?
    final String globalSettings = globalSettingsText.getText().trim();
    final Shell shell = getShell();
    new Job("Reading local repository") {
      protected IStatus run(IProgressMonitor monitor) {
        String settings = globalSettings.length()==0 ? defaultRuntime.getSettings() : globalSettings;
        final File localRepository = getLocalRepository(settings);
        shell.getDisplay().asyncExec(new Runnable() {
          public void run() {
            localRepositoryText.setText(localRepository==null ? "" : localRepository.getAbsolutePath());
          }
        });
        return Status.OK_STATUS;
      }
      
      private File getLocalRepository(String globalSettings) {
        MavenEmbedder embedder = null;
        try {
          ContainerCustomizer customizer = EmbedderFactory.createExecutionCustomizer();
          embedder = EmbedderFactory.createMavenEmbedder(customizer,
              new PluginConsoleMavenEmbeddedLogger(mavenPlugin.getConsole(), false), 
              globalSettings, null);
          
          ArtifactRepository localRepository = embedder.getLocalRepository();
          if(localRepository != null) {
            return new File(localRepository.getBasedir());
          }
          
        } catch(MavenEmbedderException ex) {
          MavenPlugin.log("Can't create Maven embedder", ex);
        } finally {
          try {
            if(embedder!=null) {
              embedder.stop();
            }
          } catch(MavenEmbedderException ex) {
            MavenPlugin.log("Can't stop Maven embedder", ex);
          }
        }
        return null;
      }
    }.schedule();
  }

  boolean checkSettings() {
    setErrorMessage(null);
    setMessage(null);
  
    Configuration configuration = new DefaultConfiguration() //
        .setClassLoader(Thread.currentThread().getContextClassLoader()) //
        .setMavenEmbedderLogger(new PluginConsoleMavenEmbeddedLogger(mavenPlugin.getConsole(), false));
  
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
            MavenPlugin plugin = MavenPlugin.getDefault();
            plugin.getConsole().logMessage("Refreshing settings " + fileName);
            plugin.getMavenEmbedderManager().invalidateMavenSettings();
          }
        }
      });
      
    } catch(PartInitException ex) {
      MavenPlugin.log(ex);
    }
  }

  MavenRuntime getSelectedRuntime() {
    IStructuredSelection selection = (IStructuredSelection) runtimesViewer.getSelection();
    return (MavenRuntime) selection.getFirstElement();
  }


  static class RuntimesLabelProvider implements ITableLabelProvider, IColorProvider {
    
    private Color disabledColor = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);

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
      return !runtime.isEditable() ? disabledColor : null;
    }
    
    public void dispose() {
      disabledColor.dispose();
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
