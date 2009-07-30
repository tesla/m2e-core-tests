package org.maven.ide.eclipse.ui.internal.preferences;

import java.io.File;
import java.lang.reflect.Constructor;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.validation.SettingsValidationResult;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.core.Messages;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.embedder.MavenRuntime;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.index.IndexManager;


/**
 * Maven installations preference page
 * 
 * @author Eugene Kuleshov
 */
public class MavenSettingsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  final MavenPlugin mavenPlugin;

  final MavenRuntimeManager runtimeManager;
  
  final IMavenConfiguration mavenConfiguration;
  
  final IMaven maven;

  MavenRuntime defaultRuntime;


  Text userSettingsText;

  Text localRepositoryText;

  boolean dirty = false;

  private Link userSettingsLink;

  public MavenSettingsPreferencePage() {
    setTitle("Maven User Settings");

    this.mavenPlugin = MavenPlugin.getDefault();
    this.runtimeManager = mavenPlugin.getMavenRuntimeManager();
    this.mavenConfiguration = MavenPlugin.lookup(IMavenConfiguration.class);
    this.maven = MavenPlugin.lookup(IMaven.class);
  }

  public void init(IWorkbench workbench) {
  }

  
  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
   */
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if(visible){
      updateLocalRepository();
    }
  }

  protected void performDefaults() {
    userSettingsText.setText(MavenEmbedder.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath());
    setDirty(true);
    updateLocalRepository();
    super.performDefaults();
  }

  protected void updateSettings(){
    final String userSettings = getUserSettings();
    
    new Job("Updating Maven settings") {
      protected IStatus run(IProgressMonitor monitor) {
        try {
          final File localRepositoryDir = new File(maven.getLocalRepository().getBasedir());
          if(userSettings.length() > 0) {
            mavenConfiguration.setUserSettingsFile(userSettings);
          } else {
            mavenConfiguration.setUserSettingsFile(null);
          }

          File newRepositoryDir = new File(maven.getLocalRepository().getBasedir());
          if(!newRepositoryDir.equals(localRepositoryDir)) {
            mavenPlugin.getIndexManager().scheduleIndexUpdate(IndexManager.LOCAL_INDEX, true, 0L);
          }
          return Status.OK_STATUS;
        } catch (CoreException e) {
          return e.getStatus();
        }
      }
    }.schedule();
  }
  
  protected void performApply() {
    if(dirty){
      updateSettings();
    }
  }
  
  public boolean performOk() {
    if (dirty) {
      updateSettings();
    }
    return true;
  }
  
  public void setDirty(boolean dirty){
    this.dirty = dirty;
  }
  
  public boolean isDirty(){
    return this.dirty;
  }

  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout(4, false);
    gridLayout.marginBottom = 5;
    gridLayout.marginRight = 5;
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    composite.setLayout(gridLayout);

    createUserSettings(composite);
    Label localRepositoryLabel = new Label(composite, SWT.NONE);
    GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
    gd.verticalIndent=25;
    localRepositoryLabel.setLayoutData(gd);
    localRepositoryLabel.setText("Local Repository (From merged user and global settings):");
    
    localRepositoryText = new Text(composite, SWT.READ_ONLY|SWT.BORDER);
    localRepositoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
    localRepositoryText.setData("name", "localRepositoryText");
    localRepositoryText.setEditable(false);
    Button reindexButton = new Button(composite, SWT.NONE);
    reindexButton.setLayoutData(new GridData(SWT.FILL, SWT.RIGHT, false, false, 1, 1));
    reindexButton.setText(Messages.getString("preferences.reindexButton"));    
    reindexButton.addSelectionListener(new SelectionAdapter(){

      /* (non-Javadoc)
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      public void widgetSelected(SelectionEvent e) {
        new WorkspaceJob("Indexing Local Repository...") {
            public IStatus runInWorkspace(IProgressMonitor monitor) {
              IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
              indexManager.scheduleIndexUpdate(IndexManager.LOCAL_INDEX, true, 0L);
              return Status.OK_STATUS;
            }
         }.schedule();
      }
    });
    defaultRuntime = runtimeManager.getDefaultRuntime();

    String userSettings = mavenConfiguration.getUserSettingsFile();
    if(userSettings == null || userSettings.length() == 0) {
      userSettingsText.setText(MavenEmbedder.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath());
    } else {
      userSettingsText.setText(userSettings);
    }

    checkSettings();
    updateLocalRepository();

    userSettingsText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent modifyevent) {
        updateLocalRepository();
        checkSettings();
        setDirty(true);
      }
    });

    return composite;
  }
  
  public void updateSettingsLink(boolean active){
    String text = "User &Settings:";
    if(active){
      text = "User &Settings (<a href=\"#\">open file</a>):";
    }
    userSettingsLink.setText(text);
  }
  /**
   * @param composite
   */
  private void createUserSettings(Composite composite) {

    userSettingsLink = new Link(composite, SWT.NONE);
    userSettingsLink.setData("name", "userSettingsLink");
    userSettingsLink.setText("User &Settings (<a href=\"#\">open file</a>):");
    userSettingsLink.setToolTipText("Open editor for user settings");
    GridData gd_userSettingsLabel = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
    
    gd_userSettingsLabel.verticalIndent = 15;
    userSettingsLink.setLayoutData(gd_userSettingsLabel);
    userSettingsLink.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        String userSettings = getUserSettings();
        if(userSettings.length() == 0) {
          userSettings = MavenEmbedder.DEFAULT_USER_SETTINGS_FILE.getAbsolutePath();
        }
        openEditor(userSettings);
      }
    });
    userSettingsText = new Text(composite, SWT.BORDER);
    userSettingsText.setData("name", "userSettingsText");
    GridData gd_userSettingsText = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
    gd_userSettingsText.verticalIndent = 5;
    gd_userSettingsText.widthHint = 100;
    userSettingsText.setLayoutData(gd_userSettingsText);

    Button userSettingsBrowseButton = new Button(composite, SWT.NONE);
    GridData gd_userSettingsBrowseButton = new GridData(SWT.FILL, SWT.RIGHT, false, false, 1, 1);
   
    userSettingsBrowseButton.setLayoutData(gd_userSettingsBrowseButton);
    userSettingsBrowseButton.setText("&Browse...");
    userSettingsBrowseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
        if(getUserSettings().length() > 0) {
          dialog.setFileName(getUserSettings());
        }
        String file = dialog.open();
        if(file != null) {
          file = file.trim();
          if(file.length() > 0) {
            userSettingsText.setText(file);
            updateLocalRepository();
            checkSettings();
          }
        }
      }
    });
  }

  protected void updateLocalRepository() {
    final String userSettings = getUserSettings();
    String globalSettings = runtimeManager.getGlobalSettingsFile();
    try {
      Settings settings = maven.buildSettings(globalSettings, userSettings);
      String localRepository = settings.getLocalRepository();
      if(localRepository == null){
        localRepository = RepositorySystem.defaultUserLocalRepository.getAbsolutePath();
      }
      if(!localRepositoryText.isDisposed()) {
        localRepositoryText.setText(localRepository == null ? "" : localRepository);
      }
    } catch (CoreException e) {
      setMessage(e.getMessage(), IMessageProvider.ERROR);
    }
  }

  protected void checkSettings() {
    setErrorMessage(null);
    setMessage(null);
    boolean fileExists = false;
    String userSettings = getUserSettings();
    if(userSettings != null && userSettings.length() > 0) {
      File userSettingsFile = new File(userSettings);
      if(!userSettingsFile.exists()) {
        setMessage("User settings file doesn't exist", IMessageProvider.WARNING);
        userSettings = null;
        
      } else {
        fileExists = true;
      }
      
    } else {
      userSettings = null;
    }
    updateSettingsLink(fileExists);
    SettingsValidationResult result = maven.validateSettings(userSettings);
    if(result.getMessageCount() > 0) {
      setMessage("Unable to parse user settings file; " + result.getMessage(0), IMessageProvider.WARNING);
    }
  }


  
  @SuppressWarnings("unchecked")
  void openEditor(final String fileName) {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    IWorkbenchPage page = window.getActivePage();

    IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor("settings.xml");

    File file = new File(fileName);
    IEditorInput input = null;
    try {
      //class implementing editor input for external file has been renamed in eclipse 3.3, hence reflection
      Class javaInput = null;
      try {
        javaInput = Class.forName("org.eclipse.ui.internal.editors.text.JavaFileEditorInput");
        Constructor cons = javaInput.getConstructor(new Class[] {File.class});
        input = (IEditorInput) cons.newInstance(new Object[] {file});
      } catch(Exception e) {
        try {
          IFileStore fileStore = EFS.getLocalFileSystem().fromLocalFile(file);
          Class storeInput = Class.forName("org.eclipse.ui.ide.FileStoreEditorInput");
          Constructor cons = storeInput.getConstructor(new Class[] {IFileStore.class});
          input = (IEditorInput) cons.newInstance(new Object[] {fileStore});
        } catch(Exception ex) {
          //ignore...
        }
      }
      final IEditorPart editor = IDE.openEditor(page, input, desc.getId());
      editor.addPropertyListener(new IPropertyListener() {
        public void propertyChanged(Object source, int propId) {
          if(!editor.isDirty()) {
            mavenPlugin.getConsole().logMessage("Refreshing settings " + fileName);
          }
        }
      });

    } catch(PartInitException ex) {
      MavenLogger.log(ex);
    }
  }

  String getUserSettings() {
    return userSettingsText.getText().trim();
  }


}
