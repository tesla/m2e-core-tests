/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.maven.ide.eclipse.embedder.ArchetypeManager;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.index.IndexInfo;
import org.maven.ide.eclipse.index.IndexListener;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.internal.ExtensionReader;
import org.maven.ide.eclipse.internal.console.MavenConsoleImpl;
import org.maven.ide.eclipse.internal.index.IndexInfoWriter;
import org.maven.ide.eclipse.internal.index.NexusIndexManager;
import org.maven.ide.eclipse.internal.launch.MavenLaunchConfigurationListener;
import org.maven.ide.eclipse.internal.launch.WorkspaceStateWriter;
import org.maven.ide.eclipse.internal.preferences.MavenPreferenceConstants;
import org.maven.ide.eclipse.internal.project.MavenProjectManagerImpl;
import org.maven.ide.eclipse.internal.project.MavenProjectManagerRefreshJob;
import org.maven.ide.eclipse.internal.project.ProjectConfigurationManager;
import org.maven.ide.eclipse.project.BuildPathManager;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.MavenUpdateRequest;


/**
 * MavenPlugin main plug-in class.
 */
public class MavenPlugin extends AbstractUIPlugin implements IStartup {

  public static final String PLUGIN_ID = MavenPlugin.class.getPackage().getName();

  public static final String NATURE_ID = PLUGIN_ID + ".maven2Nature"; //$NON-NLS-1$

  public static final String BUILDER_ID = PLUGIN_ID + ".maven2Builder"; //$NON-NLS-1$

  public static final String MARKER_ID = PLUGIN_ID + ".maven2Problem"; //$NON-NLS-1$

  public static final String POM_FILE_NAME = "pom.xml"; //$NON-NLS-1$

  // preferences
  private static final String PREFS_INDEXES = "indexInfo.xml";
  
  private static final String PREFS_ARCHETYPES = "archetypesInfo.xml";
  
  // container settings
  public static final String CONTAINER_ID = PLUGIN_ID + ".MAVEN2_CLASSPATH_CONTAINER"; //$NON-NLS-1$

  public static final String INCLUDE_MODULES = "modules"; //$NON-NLS-1$

  public static final String NO_WORKSPACE_PROJECTS = "noworkspace"; //$NON-NLS-1$

  public static final String ACTIVE_PROFILES = "profiles"; //$NON-NLS-1$

  public static final String FILTER_RESOURCES = "filterresources"; //$NON-NLS-1$

  public static final String USE_MAVEN_OUPUT_FOLDERS = "mavenoutputfolders"; //$NON-NLS-1$

  // entry attributes
  public static final String GROUP_ID_ATTRIBUTE = "maven.groupId"; //$NON-NLS-1$

  public static final String ARTIFACT_ID_ATTRIBUTE = "maven.artifactId"; //$NON-NLS-1$

  public static final String VERSION_ATTRIBUTE = "maven.version"; //$NON-NLS-1$

  public static final String CLASSIFIER_ATTRIBUTE = "maven.classifier"; //$NON-NLS-1$

  public static final String TYPE_ATTRIBUTE = "maven.type"; //$NON-NLS-1$

  public static final String JAVADOC_CLASSIFIER = "javadoc"; //$NON-NLS-1$

  public static final String SOURCES_CLASSIFIER = "sources"; //$NON-NLS-1$

  // The shared instance
  private static MavenPlugin plugin;

  private MavenConsole console;

  private MavenModelManager modelManager;

  private IndexManager indexManager;

  private MavenEmbedderManager embedderManager;

  private BuildPathManager buildpathManager;

  private MavenLaunchConfigurationListener launchConfigurationListener;

  private BundleContext bundleContext;

  private MavenProjectManager projectManager;

  private MavenRuntimeManager runtimeManager;
  
  private ProjectConfigurationManager configurationManager;

  private MavenProjectManagerRefreshJob mavenBackgroundJob;

  private ArchetypeManager archetypeManager;
  

  public MavenPlugin() {
    plugin = this;
  }

  /**
   * This method is called upon plug-in activation
   */
  public void start(final BundleContext context) throws Exception {
    super.start(context);
    this.bundleContext = context;

    try {
      this.console = new MavenConsoleImpl();
    } catch(RuntimeException ex) {
      log(new Status(IStatus.ERROR, PLUGIN_ID, -1, "Unable to start console: " + ex.toString(), ex));
    }

    this.runtimeManager = new MavenRuntimeManager(getPreferenceStore());

    this.embedderManager = new MavenEmbedderManager(console, runtimeManager);

    File stateLocationDir = getStateLocation().toFile();
    
    this.archetypeManager = new ArchetypeManager(new File(stateLocationDir, PREFS_ARCHETYPES));
    this.archetypeManager.addArchetypeCatalogFactory(new ArchetypeManager.NexusIndexerCatalogFactory());
    this.archetypeManager.addArchetypeCatalogFactory(new ArchetypeManager.InternalCatalogFactory());
    this.archetypeManager.addArchetypeCatalogFactory(new ArchetypeManager.DefaultLocalCatalogFactory());
    ExtensionReader.readArchetypeExtensions(archetypeManager);
    try {
      this.archetypeManager.readCatalogs();
    } catch (Exception ex) {
      String msg = "Can't read archetype catalog configuration";
      this.console.logError(msg + "; " + ex.getMessage());
      log(msg, ex);
    }
    
    // this.indexManager = new LegacyIndexManager(getStateLocation().toFile(), console);
    this.indexManager = new NexusIndexManager(embedderManager, console, stateLocationDir);

    this.indexManager.addIndex(new IndexInfo(IndexManager.WORKSPACE_INDEX, //
        null, null, IndexInfo.Type.WORKSPACE, false), false);

    this.indexManager.addIndex(new IndexInfo(IndexManager.LOCAL_INDEX, //
        embedderManager.getLocalRepositoryDir(), null, IndexInfo.Type.LOCAL, false), false);

    Set indexes = loadIndexConfiguration(new File(stateLocationDir, PREFS_INDEXES));

    this.modelManager = new MavenModelManager(embedderManager, console);

    MavenProjectManagerImpl managerImpl = new MavenProjectManagerImpl(console, indexManager, embedderManager,
        runtimeManager);

    this.mavenBackgroundJob = new MavenProjectManagerRefreshJob(managerImpl);

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.addResourceChangeListener(mavenBackgroundJob, IResourceChangeEvent.POST_CHANGE
        | IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE);

    this.projectManager = new MavenProjectManager(managerImpl, indexManager, mavenBackgroundJob);
    this.projectManager.addMavenProjectChangedListener(new WorkspaceStateWriter());
    this.projectManager.refresh(new MavenUpdateRequest(workspace.getRoot().getProjects(), //
        true /*offline*/, false /* updateSnapshots */));

    this.buildpathManager = new BuildPathManager(embedderManager, console, projectManager, indexManager, modelManager,
        runtimeManager);
    workspace.addResourceChangeListener(buildpathManager, IResourceChangeEvent.PRE_DELETE);

    projectManager.addMavenProjectChangedListener(this.buildpathManager);
    projectManager.addDownloadSourceListener(this.buildpathManager);

    this.configurationManager = new ProjectConfigurationManager(modelManager, console, 
        runtimeManager, managerImpl, 
        indexManager, embedderManager);
    projectManager.addMavenProjectChangedListener(this.configurationManager);

    this.launchConfigurationListener = new MavenLaunchConfigurationListener();
    DebugPlugin.getDefault().getLaunchManager().addLaunchConfigurationListener(launchConfigurationListener);

    initializeIndexes(indexes, runtimeManager.isUpdateIndexesOnStartup());

    checkJdk();
  }

  private void initializeIndexes(Set indexes, boolean updateIndexesOnStartup) {
    for(Iterator it = indexes.iterator(); it.hasNext();) {
      IndexInfo indexInfo = (IndexInfo) it.next();
      if(IndexInfo.Type.LOCAL.equals(indexInfo.getType())) {
        if(indexInfo.isNew()) {
          indexManager.scheduleIndexUpdate(indexInfo.getIndexName(), false, 4000L);
        }
      } else if(IndexInfo.Type.REMOTE.equals(indexInfo.getType())) {
        if(indexInfo.isNew() || updateIndexesOnStartup) {
          indexManager.scheduleIndexUpdate(indexInfo.getIndexName(), false, 4000L);
        }
      }
    }
  }

  private Set loadIndexConfiguration(File configFile) throws IOException {
    LinkedHashSet indexes = new LinkedHashSet();
    indexes.addAll(ExtensionReader.readIndexInfoConfig(configFile));

    Map extensionIndexes = ExtensionReader.readIndexInfoExtensions();
    indexes.addAll(extensionIndexes.values());

    for(Iterator it = indexes.iterator(); it.hasNext();) {
      IndexInfo indexInfo = (IndexInfo) it.next();

      IndexInfo extensionInfo = (IndexInfo) extensionIndexes.get(indexInfo.getIndexName());
      if(extensionInfo != null) {
        URL archiveUrl = extensionInfo.getArchiveUrl();
        indexInfo.setArchiveUrl(archiveUrl);

        if(archiveUrl != null) {
          InputStream is = archiveUrl.openStream();
          Date indexArchiveTime = indexManager.getIndexArchiveTime(is);
          Date updateTime = indexInfo.getUpdateTime();
          if(updateTime == null || indexArchiveTime.after(updateTime)) {
            indexInfo = extensionInfo;
          }
        }
      }

      this.indexManager.addIndex(indexInfo, false);
    }
    
    this.indexManager.addIndexListener(new IndexManagerWriterListener(indexManager, configFile));
    
    return indexes;
  }

  public void earlyStartup() {
    // nothing to do here, all startup work is done in #start(BundleContext)
  }

  /**
   * This method is called when the plug-in is stopped
   */
  public void stop(BundleContext context) throws Exception {
    super.stop(context);

    DebugPlugin.getDefault().getLaunchManager().removeLaunchConfigurationListener(launchConfigurationListener);

    this.mavenBackgroundJob.cancel();
    try {
      this.mavenBackgroundJob.join();
    } catch(InterruptedException ex) {
      // ignored
    }
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.removeResourceChangeListener(this.mavenBackgroundJob);
    this.mavenBackgroundJob = null;

    this.projectManager.removeMavenProjectChangedListener(buildpathManager);
    this.projectManager.removeDownloadSourceListener(this.buildpathManager);
    this.projectManager.removeMavenProjectChangedListener(this.configurationManager);
    this.projectManager = null;

    workspace.removeResourceChangeListener(this.buildpathManager);

    this.embedderManager.shutdown();

    this.configurationManager = null;

    if(this.console != null) {
      this.console.shutdown();
    }
    plugin = null;
  }

  private void checkJdk() {
    // There is no tools.jar on Mac OS X
    // http://developer.apple.com/documentation/Java/Conceptual/Java14Development/02-JavaDevTools/JavaDevTools.html
    String osName = System.getProperty("os.name", "");
    if(osName.toLowerCase().indexOf("mac os") == -1) {
      String javaHome = System.getProperty("java.home");
      File toolsJar = new File(javaHome, "../lib/tools.jar");
      if(!toolsJar.exists()) {
        getConsole().logError("Eclipse is running in a JRE, but a JDK is required\n" // 
            + "  Some Maven plugins may not work when importing projects or updating source folders.");
        if(!getPreferenceStore().getBoolean(MavenPreferenceConstants.P_DISABLE_JDK_WARNING)) {
          showJdkWarning();
        }
      }
    }
  }

  private void showJdkWarning() {
    PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
      public void run() {
        Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
        MessageDialogWithToggle dialog = new MessageDialogWithToggle(shell, //
            "Maven Integration for Eclipse JDK Warning", //
            null, "The Maven Integration requires that Eclipse be running in a JDK, "
                + "because a number of Maven core plugins are using jars from the JDK.\n\n"
                + "Please make sure the -vm option in <a>eclipse.ini</a> "
                + "is pointing to a JDK and verify that <a>Installed JREs</a> " + "are also using JDK installs.", //
            MessageDialog.WARNING, //
            new String[] {IDialogConstants.OK_LABEL}, //
            0, "Do not warn again", false) {
          protected Control createMessageArea(Composite composite) {
            Image image = getImage();
            if(image != null) {
              imageLabel = new Label(composite, SWT.NULL);
              image.setBackground(imageLabel.getBackground());
              imageLabel.setImage(image);
              GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BEGINNING).applyTo(imageLabel);
            }

            Link link = new Link(composite, getMessageLabelStyle());
            link.setText(message);
            link.addSelectionListener(new SelectionAdapter() {
              public void widgetSelected(SelectionEvent e) {
                if("eclipse.ini".equals(e.text)) {
//                    String href = "topic=/org.eclipse.platform.doc.user/tasks/running_eclipse.htm";
//                    BaseHelpSystem.getHelpDisplay().displayHelpResource(href, false);

                  try {
                    IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
                    // browser.openURL(new URL("http://www.eclipse.org/swt/launcher.html"));
                    browser
                        .openURL(new URL(
                            "http://help.eclipse.org/help33/index.jsp?topic=/org.eclipse.platform.doc.user/tasks/running_eclipse.htm"));
                  } catch(MalformedURLException ex) {
                    log("Malformed URL", ex);
                  } catch(PartInitException ex) {
                    log(ex);
                  }
                } else {
                  PreferencesUtil.createPreferenceDialogOn(getShell(),
                      "org.eclipse.jdt.debug.ui.preferences.VMPreferencePage", null, null).open(); //$NON-NLS-1$
                }
              }
            });

            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).hint(
                convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH), SWT.DEFAULT).applyTo(link);

            return composite;
          }
        };

        dialog.setPrefStore(getPreferenceStore());
        dialog.setPrefKey(MavenPreferenceConstants.P_DISABLE_JDK_WARNING);

        dialog.open();

        getPreferenceStore().setValue(MavenPreferenceConstants.P_DISABLE_JDK_WARNING, dialog.getToggleState());
      }
    });
  }

  /**
   * Returns the shared instance.
   */
  public static MavenPlugin getDefault() {
    return plugin;
  }

  public MavenModelManager getMavenModelManager() {
    return this.modelManager;
  }

  public MavenProjectManager getMavenProjectManager() {
    return this.projectManager;
  }

  public IndexManager getIndexManager() {
    return this.indexManager;
  }

  public MavenConsole getConsole() {
    return this.console;
  }

  public MavenEmbedderManager getMavenEmbedderManager() {
    return this.embedderManager;
  }

  public BuildPathManager getBuildpathManager() {
    return this.buildpathManager;
  }

  public MavenRuntimeManager getMavenRuntimeManager() {
    return this.runtimeManager;
  }

  public ArchetypeManager getArchetypeManager() {
    return this.archetypeManager;
  }
  
  /**
   * Returns an Image for the file at the given relative path.
   */
  public static Image getImage(String path) {
    ImageRegistry registry = getDefault().getImageRegistry();
    Image image = registry.get(path);
    if(image == null) {
      registry.put(path, imageDescriptorFromPlugin(PLUGIN_ID, path));
      image = registry.get(path);
    }
    return image;
  }

  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(PLUGIN_ID, path);
  }

  public static void log(IStatus status) {
    getDefault().getLog().log(status);
  }

  public static void log(CoreException ex) {
    IStatus s = ex.getStatus();
    if(s.getException()==null) {
      log(new Status(s.getSeverity(), s.getPlugin(), s.getCode(), s.getMessage(), ex));
    } else {
      log(s);
    }
  }

  public static void log(String msg, Throwable t) {
    log(new Status(IStatus.ERROR, PLUGIN_ID, 0, msg, t));
  }

  public BundleContext getBundleContext() {
    return this.bundleContext;
  }

  /**
   * IndexManagerWriterListener
   */
  static class IndexManagerWriterListener implements IndexListener {
    private final File configFile;

    private final IndexManager indexManager;

    private IndexInfoWriter writer = new IndexInfoWriter();

    public IndexManagerWriterListener(IndexManager indexManager, File configFile) {
      this.indexManager = indexManager;
      this.configFile = configFile;
    }

    public void indexAdded(IndexInfo info) {
      writeIndexInfo();
    }

    public void indexChanged(IndexInfo info) {
      writeIndexInfo();
    }

    public void indexRemoved(IndexInfo info) {
      writeIndexInfo();
    }

    private void writeIndexInfo() {
      OutputStream os = null;
      try {
        os = new FileOutputStream(configFile);
        writer.writeIndexInfo(indexManager.getIndexes().values(), os);
      } catch(IOException ex) {
        log("Unable to write index info", ex);
      } finally {
        if(os != null) {
          try {
            os.close();
          } catch(IOException ex) {
            log("Unable to close stream for index configuration", ex);
          }
        }
      }
    }
  }

  public IProjectConfigurationManager getProjectConfigurationManager() {
    return configurationManager;
  }

  /** for use by unit tests */
  public MavenProjectManagerRefreshJob getProjectManagerRefreshJob() {
    return mavenBackgroundJob;
  }
}
