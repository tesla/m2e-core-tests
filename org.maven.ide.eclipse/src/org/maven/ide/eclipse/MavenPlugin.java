/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.MutablePlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.discovery.ComponentDiscoverer;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryEvent;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryListener;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.util.IOUtil;

import org.apache.maven.ArtifactFilterManagerDelegate;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.artifact.MavenMetadataCache;
import org.apache.maven.repository.RepositorySystem;

import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.ThreadBuildContext;

import org.maven.ide.eclipse.archetype.ArchetypeCatalogFactory;
import org.maven.ide.eclipse.archetype.ArchetypeManager;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.index.IndexInfo;
import org.maven.ide.eclipse.index.IndexListener;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexManager.IndexUpdaterRule;
import org.maven.ide.eclipse.internal.ExtensionReader;
import org.maven.ide.eclipse.internal.console.MavenConsoleImpl;
import org.maven.ide.eclipse.internal.embedder.EclipseArtifactFilterManagerDelegate;
import org.maven.ide.eclipse.internal.embedder.EclipseLoggerManager;
import org.maven.ide.eclipse.internal.embedder.MavenConfigurationImpl;
import org.maven.ide.eclipse.internal.embedder.MavenEmbeddedRuntime;
import org.maven.ide.eclipse.internal.embedder.MavenImpl;
import org.maven.ide.eclipse.internal.embedder.MavenWorkspaceRuntime;
import org.maven.ide.eclipse.internal.index.IndexInfoWriter;
import org.maven.ide.eclipse.internal.index.NexusIndexManager;
import org.maven.ide.eclipse.internal.preferences.MavenPreferenceConstants;
import org.maven.ide.eclipse.internal.project.EclipseMavenMetadataCache;
import org.maven.ide.eclipse.internal.project.EclipsePluginManager;
import org.maven.ide.eclipse.internal.project.IManagedCache;
import org.maven.ide.eclipse.internal.project.MavenMarkerManager;
import org.maven.ide.eclipse.internal.project.MavenProjectManagerImpl;
import org.maven.ide.eclipse.internal.project.MavenProjectManagerRefreshJob;
import org.maven.ide.eclipse.internal.project.ProjectConfigurationManager;
import org.maven.ide.eclipse.internal.project.WorkspaceStateWriter;
import org.maven.ide.eclipse.project.IMavenMarkerManager;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.MavenUpdateRequest;


/**
 * MavenPlugin main plug-in class.
 */
public class MavenPlugin extends AbstractUIPlugin implements IStartup {

  // preferences
  private static final String PREFS_INDEXES = "indexInfo.xml";
  
  private static final String PREFS_ARCHETYPES = "archetypesInfo.xml";
  
  private static final String PREFS_INDEXES_READ = "indexesHaveBeenRead";
  
  private static final String PREFS_NO_REBUILD_ON_START = "forceRebuildOnUpgrade";

  private static final String CENTRAL_URL = "http://repo1.maven.org/maven2/";

  // The shared instance
  private static MavenPlugin plugin;

  /** 
   * In-process maven runtime. MavenProject and mojo caches must be
   * populated using components looked up from this container.
   */
  private MutablePlexusContainer mavenCore;

  /**
   * General purpose plexus container. Containers components from maven embedder
   * and all other bundles visible from this classloader.
   */
  private MutablePlexusContainer plexus;

  /**
   * Poor man's component registry
   */
  @SuppressWarnings("unchecked")
  private Map<Class, Object> components = new HashMap<Class, Object>();

  private MavenConsole console;

  private MavenModelManager modelManager;

  private IndexManager indexManager;

  private BundleContext bundleContext;

  private MavenProjectManager projectManager;

  private MavenRuntimeManager runtimeManager;
  
  private ProjectConfigurationManager configurationManager;

  private MavenProjectManagerRefreshJob mavenBackgroundJob;

  private ArchetypeManager archetypeManager;

  private MavenProjectManagerImpl managerImpl;

  private IMavenMarkerManager mavenMarkerManager;

  private ArrayList<IPropertyChangeListener> listeners;

  private String version = "0.0.0";

  public MavenPlugin() {
    plugin = this;

    if(Boolean.parseBoolean(Platform.getDebugOption(IMavenConstants.PLUGIN_ID + "/debug/initialization"))) {
      System.err.println("### executing constructor " + IMavenConstants.PLUGIN_ID); //$NON-NLS-1$
      new Throwable().printStackTrace();
    }
  }
  
  public IMaven getMaven(){
    return lookup(IMaven.class);
  }

  public RepositorySystem getRepositorySystem() throws ComponentLookupException{
    if(getPlexusContainer() != null){
      return getPlexusContainer().lookup(RepositorySystem.class);
    } 
    return null;
  }


  /**
   * This method is called upon plug-in activation
   */
  public void start(final BundleContext context) throws Exception {
    super.start(context);
    
    if(Boolean.parseBoolean(Platform.getDebugOption(IMavenConstants.PLUGIN_ID + "/debug/initialization"))) {
      System.err.println("### executing start() " + IMavenConstants.PLUGIN_ID); //$NON-NLS-1$
      new Throwable().printStackTrace();
    }
    
    this.bundleContext = context;

    try {
      Version bundleVersion = Version.parseVersion((String) getBundle().getHeaders().get(Constants.BUNDLE_VERSION));
      this.version = bundleVersion.getMajor() + "." + bundleVersion.getMinor() + "." + bundleVersion.getMicro();
    } catch (IllegalArgumentException e) {
      // ignored
    }

    MavenLogger.setLog(getLog());
    
    try {
      this.console = new MavenConsoleImpl(MavenImages.M2); //$NON-NLS-1$
    } catch(RuntimeException ex) {
      MavenLogger.log(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Unable to start console: " + ex.toString(), ex));
    }

    final String mavenCoreRealmId = "plexus.core";
    ContainerConfiguration mavenCoreCC = new DefaultContainerConfiguration()
      .setClassWorld( new ClassWorld(mavenCoreRealmId, ClassWorld.class.getClassLoader()) )
      .setName("mavenCore");
    
    mavenCoreCC.addComponentDiscoverer(new ComponentDiscoverer() {

      public List<ComponentSetDescriptor> findComponents(Context context, ClassRealm classRealm)
          throws PlexusConfigurationException {

        List<ComponentSetDescriptor> componentSetDescriptors = new ArrayList<ComponentSetDescriptor>();

        if (mavenCoreRealmId.equals(classRealm.getId())) {
          ComponentSetDescriptor componentSetDescriptor = new ComponentSetDescriptor();

          ComponentDescriptor componentDescriptor = new ComponentDescriptor();
          componentDescriptor.setRealm(classRealm);
          componentDescriptor.setRole(ArtifactFilterManagerDelegate.class.getName());
          componentDescriptor.setImplementationClass(EclipseArtifactFilterManagerDelegate.class);
          componentSetDescriptor.addComponentDescriptor(componentDescriptor);

          componentSetDescriptors.add(componentSetDescriptor);
        }

        return componentSetDescriptors;
      }

    });
    
    mavenCoreCC.addComponentDiscoveryListener(new ComponentDiscoveryListener() {
      @SuppressWarnings("unchecked")
      public void componentDiscovered(ComponentDiscoveryEvent event) {
        ComponentSetDescriptor set = event.getComponentSetDescriptor();
        for (ComponentDescriptor desc : set.getComponents()) {
          if (MavenMetadataCache.class.getName().equals(desc.getRole())) {
            desc.setImplementationClass(EclipseMavenMetadataCache.class);
          } else if (BuildContext.class.getName().equals(desc.getRole())) {
            desc.setImplementationClass(ThreadBuildContext.class);
          } else if (PluginManager.class.getName().equals(desc.getRole())) {
            desc.setImplementationClass(EclipsePluginManager.class);
          }
        }
      }
    });

    IMavenConfiguration mavenConfiguration = new MavenConfigurationImpl(getPreferenceStore());

    this.mavenCore = new DefaultPlexusContainer( mavenCoreCC );
    this.mavenCore.setLoggerManager(new EclipseLoggerManager(console, mavenConfiguration));

    IMaven maven = new MavenImpl(mavenCore, mavenConfiguration);

    components.put(IMavenConfiguration.class, mavenConfiguration);
    components.put(IMaven.class, maven);

    ClassLoader cl = MavenPlugin.class.getClassLoader();

//    Enumeration<URL> resources = cl.getResources("META-INF/plexus/components.xml");
//    while (resources.hasMoreElements()) {
//      System.out.println(FileLocator.resolve(resources.nextElement()));
//    }
    
    ContainerConfiguration cc = new DefaultContainerConfiguration()
      .setClassWorld(new ClassWorld(mavenCoreRealmId, cl))
      .setName("plexus");
    this.plexus = new DefaultPlexusContainer( cc);

    this.runtimeManager = new MavenRuntimeManager(getPreferenceStore());
    
    this.runtimeManager.setEmbeddedRuntime(new MavenEmbeddedRuntime(getBundleContext()));
    
    File stateLocationDir = getStateLocation().toFile();
    
    this.archetypeManager = new ArchetypeManager(new File(stateLocationDir, PREFS_ARCHETYPES));
    this.archetypeManager.addArchetypeCatalogFactory(new ArchetypeCatalogFactory.NexusIndexerCatalogFactory());
    this.archetypeManager.addArchetypeCatalogFactory(new ArchetypeCatalogFactory.InternalCatalogFactory());
    this.archetypeManager.addArchetypeCatalogFactory(new ArchetypeCatalogFactory.DefaultLocalCatalogFactory());
    for(ArchetypeCatalogFactory archetypeCatalogFactory : ExtensionReader.readArchetypeExtensions()) {
      archetypeManager.addArchetypeCatalogFactory(archetypeCatalogFactory);
    }
    try {
      this.archetypeManager.readCatalogs();
    } catch (Exception ex) {
      String msg = "Can't read archetype catalog configuration";
      this.console.logError(msg + "; " + ex.getMessage());
      MavenLogger.log(msg, ex);
    }
    
    // this.indexManager = new LegacyIndexManager(getStateLocation().toFile(), console);
    this.indexManager = new NexusIndexManager(console, stateLocationDir);

    this.indexManager.addIndex(new IndexInfo(IndexManager.WORKSPACE_INDEX, //
        null, null, IndexInfo.Type.WORKSPACE, false), false);

    try {
      IndexInfo local = new IndexInfo(IndexManager.LOCAL_INDEX, //
          new File(maven.getLocalRepository().getBasedir()), null, IndexInfo.Type.LOCAL, false);
      this.indexManager.addIndex(local, false);
      boolean forceUpdate = !this.getPreferenceStore().getBoolean(PREFS_NO_REBUILD_ON_START);
      if(forceUpdate && !local.isNew()){
        indexManager.scheduleIndexUpdate(local.getIndexName(), true, 4000L);
      }
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }

    Set<IndexInfo> indexes = loadIndexConfiguration(new File(stateLocationDir, PREFS_INDEXES));

    boolean updateProjectsOnStartup = mavenConfiguration.isUpdateProjectsOnStartup();

    mavenMarkerManager = new MavenMarkerManager(runtimeManager, console);

    this.managerImpl = new MavenProjectManagerImpl(console, indexManager,
        stateLocationDir, !updateProjectsOnStartup /* readState */, runtimeManager, mavenMarkerManager);
    this.components.put(MavenProjectManagerImpl.class, managerImpl);
    this.managerImpl.addManagedCache((IManagedCache) mavenCore.lookup(MavenMetadataCache.class));

    this.mavenBackgroundJob = new MavenProjectManagerRefreshJob(managerImpl, runtimeManager, console);

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.addResourceChangeListener(mavenBackgroundJob, IResourceChangeEvent.POST_CHANGE
        | IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE);

    this.projectManager = new MavenProjectManager(managerImpl, mavenBackgroundJob, stateLocationDir);
    this.components.put(MavenProjectManager.class, projectManager);
    this.projectManager.addMavenProjectChangedListener(new WorkspaceStateWriter(projectManager));
    if(updateProjectsOnStartup || managerImpl.getProjects().length == 0) {
      this.projectManager.refresh(new MavenUpdateRequest(workspace.getRoot().getProjects(), //
          true /*offline*/, false /* updateSnapshots */));
    }

    this.modelManager = new MavenModelManager(maven, projectManager, console);
    
    this.runtimeManager.setWorkspaceRuntime(new MavenWorkspaceRuntime(projectManager));
    
    this.configurationManager = new ProjectConfigurationManager(modelManager, console, 
        runtimeManager, projectManager, 
        indexManager, modelManager, mavenMarkerManager);
    projectManager.addMavenProjectChangedListener(this.configurationManager);

    initializeIndexes(indexes, mavenConfiguration.isUpdateIndexesOnStartup());

    checkJdk();
  }

  public void fireIndexUpdate(String indexName){
    if(listeners != null){
      for(IPropertyChangeListener listener : listeners){
        listener.propertyChange(new PropertyChangeEvent(this, IMavenConstants.INDEX_UPDATE_PROP, null, indexName));
      }
    }
  }
  
  public void addPropertyChangeListener(IPropertyChangeListener listener){
    if(listeners == null){
      listeners = new ArrayList<IPropertyChangeListener>();
    }
    listeners.add(listener);
  }
  
  public void removePropertyChangeListener(IPropertyChangeListener listener){
    if(listeners != null){
      listeners.remove(listener);
    }
  }
  
  private void initializeIndexes(Set<IndexInfo> indexes, boolean updateIndexesOnStartup) {
    boolean forceUpdate = !this.getPreferenceStore().getBoolean(PREFS_NO_REBUILD_ON_START);
    if(forceUpdate){
      this.getPreferenceStore().setValue(PREFS_NO_REBUILD_ON_START, true);
    }
    for(IndexInfo indexInfo : indexes) {
      if(IndexInfo.Type.LOCAL.equals(indexInfo.getType())) {
        if(indexInfo.isNew()) {
            indexManager.scheduleIndexUpdate(indexInfo.getIndexName(), false, 4000L);
        }
      } else if(IndexInfo.Type.REMOTE.equals(indexInfo.getType())) {
        if(indexInfo.isNew() || updateIndexesOnStartup || forceUpdate) {
          indexManager.scheduleIndexUpdate(indexInfo.getIndexName(), forceUpdate, 4000L);
        }
      }
    }
  }
  
  void unzipFile(URL url, File dest) throws IOException {

    InputStream is = new BufferedInputStream(url.openStream());
    ZipInputStream zis = new ZipInputStream(is);
    try {
      ZipEntry entry = zis.getNextEntry();
      while(entry != null) {
        File f = new File(dest, entry.getName());
        if(entry.isDirectory()) {
          f.mkdirs();
        } else {
          if(!f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
          }
          OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
          try {
            IOUtil.copy(zis, os);
          } finally {
            os.close();
          }
        }
        zis.closeEntry();
        entry = zis.getNextEntry();
      }
    } finally {
      zis.close();
    }
  }
  
  private IndexInfo createCentralIndex() throws IllegalStateException {
    String indexId = "central";
    String repositoryUrl = CENTRAL_URL;
    IndexInfo indexInfo = new IndexInfo(indexId, null, repositoryUrl, IndexInfo.Type.REMOTE, false);
    indexInfo.setIndexUpdateUrl("");
    // Hook for integration tests. If plug-in contains a pre-processed central index then install it. 
    // This speeds up test execution significantly. 
    URL url = FileLocator.find(getBundle(), new Path("/indexes/maven-central.zip"), null);
    if (url != null) {
      installCachedMavenCentralIndex(url);
    }
    return indexInfo;
  }
  
  private void installCachedMavenCentralIndex(final URL indexURL) {
    Job job = new Job("Installing maven central index.") {

      public IStatus run(IProgressMonitor monitor) {
        try {
          monitor.beginTask("Installing index...", IProgressMonitor.UNKNOWN);
          unzipFile(indexURL, getStateLocation().toFile());
        } catch(Exception ex) {
          MavenLogger.log("Error unzipping maven central index", ex);
        } finally {
          monitor.done();
        }
        return Status.OK_STATUS;
      }
      
    };
    job.setRule(new ISchedulingRule() {
      public boolean contains(ISchedulingRule rule) {
        return rule == this;
      }

      public boolean isConflicting(ISchedulingRule rule) {
        return rule == this || rule instanceof IndexUpdaterRule;
      }
      
    });
    job.schedule();
  }
  
  private Set<IndexInfo> loadIndexConfiguration(File configFile) throws IllegalStateException {
    LinkedHashSet<IndexInfo> indexes = new LinkedHashSet<IndexInfo>();
    indexes.addAll(ExtensionReader.readIndexInfoConfig(configFile));
    Map<String, IndexInfo> extensionIndexes = ExtensionReader.readIndexInfoExtensions();
    
    //check if this is the first time in this workspace
    boolean firstTime = !this.getPreferenceStore().getBoolean(PREFS_INDEXES_READ);
    if(firstTime){
      this.getPreferenceStore().setValue(PREFS_INDEXES_READ, true);
    }
    //add central if this is first time through and there are no user defined
    //extension points
    if(firstTime && extensionIndexes.size() == 0){
      //this should go away when we get the real central-setup from the settings
      IndexInfo info = createCentralIndex();
      indexes.add(info);
    }  
    //now add all the extension point indexes 
    if(extensionIndexes.size() > 0){
      for(IndexInfo xInfo : extensionIndexes.values()){
        indexes.add(xInfo);
      }
    }
    for(IndexInfo indexInfo : indexes) {
      this.indexManager.addIndex(indexInfo, false);
    }
    this.indexManager.addIndexListener(new IndexManagerWriterListener(indexManager, configFile));
    return indexes;
  }

  public void earlyStartup() {
    // nothing to do here, all startup work is done in #start(BundleContext)
  }

  public PlexusContainer getPlexusContainer(){
    return plexus;
  }
  
  /**
   * This method is called when the plug-in is stopped
   */
  public void stop(BundleContext context) throws Exception {
    super.stop(context);

    this.mavenBackgroundJob.cancel();
    try {
      this.mavenBackgroundJob.join();
    } catch(InterruptedException ex) {
      // ignored
    }
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.removeResourceChangeListener(this.mavenBackgroundJob);
    this.mavenBackgroundJob = null;

    this.projectManager.removeMavenProjectChangedListener(this.configurationManager);
    this.projectManager = null;

    managerImpl.removeManagedCache((IManagedCache) mavenCore.lookup(MavenMetadataCache.class));

    this.plexus.dispose();
    this.mavenCore.dispose();

    this.configurationManager = null;

    if(this.console != null) {
      this.console.shutdown();
    }
    if(listeners != null){
      listeners.clear();
    }
    components.clear();
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
                    MavenLogger.log("Malformed URL", ex);
                  } catch(PartInitException ex) {
                    MavenLogger.log(ex);
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

  public MavenRuntimeManager getMavenRuntimeManager() {
    return this.runtimeManager;
  }

  public ArchetypeManager getArchetypeManager() {
    return this.archetypeManager;
  }
  
  public IMavenMarkerManager getMavenMarkerManager() {
    return this.mavenMarkerManager;
  }

  /**
   * Returns an Image for the file at the given relative path.
   */
  public static Image getImage(String path) {
    ImageRegistry registry = getDefault().getImageRegistry();
    Image image = registry.get(path);
    if(image == null) {
      registry.put(path, imageDescriptorFromPlugin(IMavenConstants.PLUGIN_ID, path));
      image = registry.get(path);
    }
    return image;
  }

  public static ImageDescriptor getImageDescriptor(String path) {
    return imageDescriptorFromPlugin(IMavenConstants.PLUGIN_ID, path);
  }

  public BundleContext getBundleContext() {
    return this.bundleContext;
  }

  public IProjectConfigurationManager getProjectConfigurationManager() {
    return configurationManager;
  }

  /** for use by unit tests */
  public MavenProjectManagerRefreshJob getProjectManagerRefreshJob() {
    return mavenBackgroundJob;
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
        MavenLogger.log("Unable to write index info", ex);
      } finally {
        if(os != null) {
          try {
            os.close();
          } catch(IOException ex) {
            MavenLogger.log("Unable to close stream for index configuration", ex);
          }
        }
      }
    }
  }

  /**
   * @deprecated will likely be removed before 1.0
   */
  public static <C> C lookup(Class<C> role) {
    C c = role.cast(plugin.components.get(role));
    if (c != null) {
      return c;
    }

    try {
      return plugin.plexus.lookup(role);
    } catch(ComponentLookupException ex) {
      throw new NoSuchComponentException(ex);
    }
  }

  /**
   * @deprecated will likely be removed before 1.0
   */
  public static <T> T lookup(Class<T> role, String roleHint) {
    try {
      return plugin.plexus.lookup(role, roleHint);
    } catch(ComponentLookupException ex) {
      throw new NoSuchComponentException(ex);
    }
  }

  public static String getVersion() {
    return plugin.version;
  }
}
