/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.metadata.ArtifactMetadata;
import org.apache.maven.artifact.resolver.metadata.MetadataResolutionException;
import org.apache.maven.artifact.resolver.metadata.MetadataResolutionRequest;
import org.apache.maven.artifact.resolver.metadata.MetadataResolutionResult;
import org.apache.maven.artifact.resolver.metadata.MetadataResolver;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.extension.ExtensionScanningException;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.filter.ArtifactDependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.traversal.BuildingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.FilteringDependencyNodeVisitor;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.command.CommandStackListener;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.domain.IEditingDomainProvider;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.ReflectiveItemProviderAdapterFactory;
import org.eclipse.emf.edit.provider.resource.ResourceItemProviderAdapterFactory;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.search.ui.text.ISearchEditorAccess;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IShowEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.common.internal.emf.resource.EMF2DOMRenderer;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.undo.IStructuredTextUndoManager;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.xml.core.internal.emf2xml.EMF2DOMSSEAdapter;
import org.eclipse.wst.xml.core.internal.emf2xml.EMF2DOMSSERenderer;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.util.PomResourceFactoryImpl;
import org.maven.ide.components.pom.util.PomResourceImpl;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.actions.OpenPomAction;
import org.maven.ide.eclipse.actions.SelectionUtil;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.editor.MavenEditorPlugin;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.util.Util;
import org.maven.ide.eclipse.util.Util.FileStoreEditorInputStub;


/**
 * Maven POM editor
 * 
 * @author Eugene Kuleshov
 * @author Anton Kraev
 */
@SuppressWarnings("restriction")
public class MavenPomEditor extends FormEditor implements IResourceChangeListener, IShowEditorInput, IGotoMarker,
    ISearchEditorAccess, IEditingDomainProvider {

  public static final String EDITOR_ID = "org.maven.ide.eclipse.editor.MavenPomEditor";

  private static final String EXTENSION_FACTORIES = MavenEditorPlugin.PLUGIN_ID + ".pageFactories";

  private static final String ELEMENT_PAGE = "factory";
  
  private static final String EFFECTIVE_POM = "Effective POM";

  OverviewPage overviewPage;

  DependenciesPage dependenciesPage;

  RepositoriesPage repositoriesPage;

  BuildPage buildPage;

  PluginsPage pluginsPage;

  ReportingPage reportingPage;

  ProfilesPage profilesPage;

  TeamPage teamPage;

  DependencyTreePage dependencyTreePage;

  DependencyGraphPage graphPage;

  StructuredTextEditor sourcePage;
  
  StructuredTextEditor effectivePomSourcePage;
  
  List<MavenPomEditorPage> pages = new ArrayList<MavenPomEditorPage>();

  private Model projectDocument;

  private Map<String,DependencyNode> rootNode = new HashMap<String, DependencyNode>();

  private PomResourceImpl resource;

  IStructuredModel structuredModel;

  EMF2DOMSSERenderer renderer;

  private MavenProject mavenProject;

  AdapterFactory adapterFactory;

  AdapterFactoryEditingDomain editingDomain;

  private int sourcePageIndex;

  NotificationCommandStack commandStack;

  IModelManager modelManager;

  IFile pomFile;

  MavenPomActivationListener activationListener;

  boolean dirty;

  CommandStackListener commandStackListener;

  BasicCommandStack sseCommandStack;

  List<IPomFileChangedListener> fileChangeListeners = new ArrayList<IPomFileChangedListener>();

  public MavenPomEditor() {
    modelManager = StructuredModelManager.getModelManager();
  }

  // IResourceChangeListener

  /**
   * Closes all project files on project close.
   */
  public void resourceChanged(final IResourceChangeEvent event) {
    if(pomFile == null) {
      return;
    }

    //handle project delete
    if(event.getType() == IResourceChangeEvent.PRE_CLOSE || event.getType() == IResourceChangeEvent.PRE_DELETE) {
      if(pomFile.getProject().equals(event.getResource())) {
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            close(true);
          }
        });
      }
      return;
    }

    //handle pom delete
    class RemovedResourceDeltaVisitor implements IResourceDeltaVisitor {
      boolean removed = false;

      public boolean visit(IResourceDelta delta) throws CoreException {
        if(delta.getResource() == pomFile //
            && (delta.getKind() & (IResourceDelta.REMOVED)) != 0) {
          removed = true;
          return false;
        }
        return true;
      }
    };
    
    try {
      RemovedResourceDeltaVisitor visitor = new RemovedResourceDeltaVisitor();
      event.getDelta().accept(visitor);
      if(visitor.removed) {
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            close(true);
          }
        });
      }
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }

    // Reload model if pom file was changed externally.
    // TODO implement generic merge scenario (when file is externally changed and is dirty)
    class ChangedResourceDeltaVisitor implements IResourceDeltaVisitor {

      public boolean visit(IResourceDelta delta) throws CoreException {
        if(delta.getResource().equals(pomFile)
            && (delta.getKind() & IResourceDelta.CHANGED) != 0 && delta.getResource().exists()) {
          int flags = delta.getFlags();
          if ((flags & (IResourceDelta.CONTENT | flags & IResourceDelta.REPLACED)) != 0) {
            handleContentChanged();
            return false;
          }
          if ((flags & IResourceDelta.MARKERS) != 0) {
            handleMarkersChanged();
            return false;
          }
        }
        return true;
      }
      
      private void handleContentChanged() {
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            try {
              structuredModel.reload(pomFile.getContents());
              reload();
            } catch(CoreException e) {
              MavenLogger.log(e);
            } catch(Exception e) {
              MavenLogger.log("Error loading pom editor model.", e);
            }
          }
        });
      }
      private void handleMarkersChanged() {
        try {
        IMarker[] markers = pomFile.findMarkers(IMavenConstants.MARKER_ID, true, IResource.DEPTH_ZERO);
        final String msg = markers != null && markers.length > 0 //
            ? markers[0].getAttribute(IMarker.MESSAGE, "Unknown error") : null;
        
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            for(MavenPomEditorPage page : pages) {
              page.setErrorMessage(msg, msg == null ? IMessageProvider.NONE : IMessageProvider.ERROR);
            }
          }
        });
        } catch (CoreException ex ) {
          MavenLogger.log("Error updating pom file markers.", ex);
        }
      }
    };
    
    try {
      ChangedResourceDeltaVisitor visitor = new ChangedResourceDeltaVisitor();
      event.getDelta().accept(visitor);
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }

  }

  public void reload() {
    projectDocument = null;
    try {
      readProjectDocument();
    } catch(CoreException e) {
      MavenLogger.log(e);
    }
    for(MavenPomEditorPage page : pages) {
      page.reload();
    }
    if(isEffectiveActive()){
      loadEffectivePOM();
    }
    flushCommandStack();
  }

  private boolean isEffectiveActive(){
    int active = getActivePage();
    String name = getPageText(active);
    return EFFECTIVE_POM.equals(name);
  }
  
  void flushCommandStack() {
    dirty = false;
    if (sseCommandStack != null)
      sseCommandStack.saveIsDone();
    if (getContainer() != null && !getContainer().isDisposed())
      getContainer().getDisplay().asyncExec(new Runnable() {
        public void run() {
          editorDirtyStateChanged();
        }
      });
  }

  /**
   * Show or hide the advanced pages within the editor
   */
  protected void showAdvancedPages(){
    boolean showAdvancedTabs = MavenPlugin.getDefault().getPreferenceStore().getBoolean(PomEditorPreferencePage.P_SHOW_ADVANCED_TABS);
    if(showAdvancedTabs && repositoriesPage == null){
      repositoriesPage = new RepositoriesPage(this);
      addPomPage(repositoriesPage);
      buildPage = new BuildPage(this);
      addPomPage(buildPage);
      profilesPage = new ProfilesPage(this);
      addPomPage(profilesPage);
      teamPage = new TeamPage(this);
      addPomPage(teamPage);
    } else {
      if(repositoriesPage == null){
        return;
      }
      removePomPage(repositoriesPage);
      repositoriesPage = null;
      removePomPage(buildPage);
      buildPage = null;
      removePomPage(profilesPage);
      profilesPage = null;
      removePomPage(teamPage);
      teamPage = null;
    }
  }
  
  protected void addPages() {
    
    overviewPage = new OverviewPage(this);
    addPomPage(overviewPage);

    dependenciesPage = new DependenciesPage(this);
    addPomPage(dependenciesPage);

    pluginsPage = new PluginsPage(this);
    addPomPage(pluginsPage);

    reportingPage = new ReportingPage(this);
    addPomPage(reportingPage);
    
    dependencyTreePage = new DependencyTreePage(this);
    addPomPage(dependencyTreePage);

    graphPage = new DependencyGraphPage(this);
    addPomPage(graphPage);

    addSourcePage();
    
    showAdvancedPages();
    
    addEditorPageExtensions();
    selectActivePage();
  }

  protected void selectActivePage(){
    boolean showXML = MavenPlugin.getDefault().getPreferenceStore().getBoolean(PomEditorPreferencePage.P_DEFAULT_POM_EDITOR_PAGE);
    if(showXML){
      setActivePage(null);
    }    
  }
  
  protected void pageChange(int newPageIndex) {
    String name = getPageText(newPageIndex);
    if(EFFECTIVE_POM.equals(name)){
      loadEffectivePOM();
    }
    super.pageChange(newPageIndex);
    
    // a workaround for editor pages not returned 
    IEditorActionBarContributor contributor = getEditorSite().getActionBarContributor();
    if(contributor != null && contributor instanceof MultiPageEditorActionBarContributor) {
      IEditorPart activeEditor = getActivePageInstance();
      ((MultiPageEditorActionBarContributor) contributor).setActivePage(activeEditor);
    }
  }

  private void addEditorPageExtensions() {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint indexesExtensionPoint = registry.getExtensionPoint(EXTENSION_FACTORIES);
    if(indexesExtensionPoint != null) {
      IExtension[] indexesExtensions = indexesExtensionPoint.getExtensions();
      for(IExtension extension : indexesExtensions) {
        for(IConfigurationElement element : extension.getConfigurationElements()) {
          if(element.getName().equals(ELEMENT_PAGE)) {
            try {
              MavenPomEditorPageFactory factory;
              factory = (MavenPomEditorPageFactory) element.createExecutableExtension("class");
              factory.addPages(this);
            } catch(CoreException ex) {
              MavenLogger.log(ex);
            }
          }
        }
      }
    }
  }

  /**
   * Load the effective POM in a job and then update the effective pom page when its done
   * @author dyocum
   *
   */
  class LoadEffectivePomJob extends Job{

    public LoadEffectivePomJob(String name) {
      super(name);
    }
    
    private void showEffectivePomError(final String name){
      Display.getDefault().asyncExec(new Runnable(){
        public void run(){
          String error = "Unable to load Effective POM. See console for errors.";
          IEditorInput editorInput = new OpenPomAction.MavenEditorStorageInput(name, name, null, error.getBytes());
          effectivePomSourcePage.setInput(editorInput);
        }
      });
    }
    @Override
    protected IStatus run(IProgressMonitor monitor) {
      try{
        StringWriter sw = new StringWriter();
        final String name = getPartName() + " [effective]";
        MavenProject mavenProject = SelectionUtil.getMavenProject(getEditorInput());
        if(mavenProject == null){
          showEffectivePomError(name);
          return Status.CANCEL_STATUS;
        }
        new MavenXpp3Writer().write(sw, mavenProject.getModel());
        final String content = sw.toString();

        Display.getDefault().asyncExec(new Runnable(){
          public void run() {
            try{
              IEditorInput editorInput = new OpenPomAction.MavenEditorStorageInput(name, name, null, content.getBytes("UTF-8"));
              effectivePomSourcePage.setInput(editorInput);
              effectivePomSourcePage.update();
            }catch(IOException ie){
              MavenLogger.log(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, "Failed to load Effective POM", ie));
            }
          }
        });
        return Status.OK_STATUS;
      } catch(CoreException ce){
        return new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, "Failed to load Effective POM", ce);
      } catch(IOException ie){
        return new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, "Failed to load Effective POM", ie);
      } 
    }
  }
  
  /**
   * Load the effective POM. Should only happen when tab is brought to front or tab
   * is in front when a reload happens.
   */
  private void loadEffectivePOM(){
    //put a msg in the editor saying that the effective pom is loading, in case this is a long running job
    String content = "Loading Effective POM...";
    String name = getPartName() + " [effective]";
    IEditorInput editorInput = new OpenPomAction.MavenEditorStorageInput(name, name, null, content.getBytes());
    effectivePomSourcePage.setInput(editorInput);
    
    //then start the load
    LoadEffectivePomJob job = new LoadEffectivePomJob("Loading effective POM...");
    job.schedule();
  }
  
  private void addSourcePage() {
    sourcePage = new StructuredTextEditor() {
      public void doSave(IProgressMonitor monitor) {
        // always save text editor
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(MavenPomEditor.this);
        try {
          super.doSave(monitor);
          flushCommandStack();
        } finally {
          ResourcesPlugin.getWorkspace().addResourceChangeListener(MavenPomEditor.this);
        }
      }

      private boolean oldDirty;
      public boolean isDirty() {
        if (oldDirty != dirty) {
          oldDirty = dirty; 
          updatePropertyDependentActions();
        }
        return dirty;
      }
    };
    sourcePage.setEditorPart(this);
    //the page for showing the effective POM
    effectivePomSourcePage = new StructuredTextEditor();
    effectivePomSourcePage.setEditorPart(this);
    try {
      int dex = addPage(effectivePomSourcePage, getEditorInput());
      setPageText(dex, EFFECTIVE_POM);
      
      sourcePageIndex = addPage(sourcePage, getEditorInput());
      setPageText(sourcePageIndex, "pom.xml");
      sourcePage.update();
      
      IDocument doc = sourcePage.getDocumentProvider().getDocument(getEditorInput());
      structuredModel = modelManager.getExistingModelForEdit(doc);
      if(structuredModel == null) {
        structuredModel = modelManager.getModelForEdit((IStructuredDocument) doc);
      }

      commandStackListener = new CommandStackListener() {
        public void commandStackChanged(EventObject event) {
          boolean oldDirty = dirty;          
          dirty = sseCommandStack.isSaveNeeded();
          if (dirty != oldDirty)
            MavenPomEditor.this.editorDirtyStateChanged();
        }
      };
      
      IStructuredTextUndoManager undoManager = structuredModel.getUndoManager();
      if(undoManager != null) {
        sseCommandStack = (BasicCommandStack) undoManager.getCommandStack();
        if(sseCommandStack != null) {
          sseCommandStack.addCommandStackListener(commandStackListener);
        }
      }
      
      flushCommandStack();
      try {
        readProjectDocument();
      } catch(CoreException e) {
        MavenLogger.log(e);
      }

      // TODO activate xml source page if model is empty or have errors
      
      if(doc instanceof IStructuredDocument) {
        List<AdapterFactoryImpl> factories = new ArrayList<AdapterFactoryImpl>();
        factories.add(new ResourceItemProviderAdapterFactory());
        factories.add(new ReflectiveItemProviderAdapterFactory());

        adapterFactory = new ComposedAdapterFactory(factories);
        commandStack = new NotificationCommandStack(this);
        editingDomain = new AdapterFactoryEditingDomain(adapterFactory, //
            commandStack, new HashMap<Resource, Boolean>());

        if(resource != null && resource.getRenderer() instanceof EMF2DOMSSERenderer) {
          renderer = (EMF2DOMSSERenderer) resource.getRenderer();
          structuredModel.addModelStateListener(renderer);
          structuredModel.addModelLifecycleListener(renderer);
        }
      }
    } catch(PartInitException ex) {
      MavenLogger.log(ex);
    }
  }

  public boolean isReadOnly() {
    return !(getEditorInput() instanceof IFileEditorInput);
  }

  private void removePomPage(IFormPage page){
    if(page == null){
      return;
    }
    if(page instanceof IPomFileChangedListener){
      fileChangeListeners.remove(page);
    }
    super.removePage(page.getIndex());
  }
  
  private int addPomPage(IFormPage page) {
    try {
      if(page instanceof MavenPomEditorPage) {
        pages.add((MavenPomEditorPage) page);
      }
      if (page instanceof IPomFileChangedListener) {
        fileChangeListeners.add((IPomFileChangedListener) page);
      }
      return addPage(page);
    } catch(PartInitException ex) {
      MavenLogger.log(ex);
      return -1;
    }
  }

  public EditingDomain getEditingDomain() {
    return editingDomain;
  }

  // XXX move to MavenModelManager (CommandStack and EditorDomain too)
  public synchronized Model readProjectDocument() throws CoreException {
    if(projectDocument == null) {
      IEditorInput input = getEditorInput();
      if(input instanceof IFileEditorInput) {
        pomFile = ((IFileEditorInput) input).getFile();
        pomFile.refreshLocal(1, null);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);

        MavenModelManager modelManager = MavenPlugin.getDefault().getMavenModelManager();
        PomResourceImpl resource = modelManager.loadResource(pomFile);
        projectDocument = resource.getModel();

      } else if(input instanceof IStorageEditorInput) {
        IStorageEditorInput storageInput = (IStorageEditorInput) input;
        IStorage storage = storageInput.getStorage();
        IPath path = storage.getFullPath();
        if(path == null || !new File(path.toOSString()).exists()) {
          File tempPomFile = null;
          InputStream is = null;
          OutputStream os = null;
          try {
            tempPomFile = File.createTempFile("maven-pom", ".pom");
            os = new FileOutputStream(tempPomFile);
            is = storage.getContents();
            IOUtil.copy(is, os);
            projectDocument = loadModel(tempPomFile.getAbsolutePath());
          } catch(IOException ex) {
            MavenLogger.log("Can't close stream", ex);
          } finally {
            IOUtil.close(is);
            IOUtil.close(os);
            if(tempPomFile != null) {
              tempPomFile.delete();
            }
          }
        } else {
          projectDocument = loadModel(path.toOSString());
        }

      } else if(input.getClass().getName().endsWith("FileStoreEditorInput")) {
        projectDocument = loadModel(Util.proxy(input, FileStoreEditorInputStub.class).getURI().getPath());
      }
    }

    return projectDocument;
  }

  private Model loadModel(String path) {
    URI uri = URI.createFileURI(path);
    PomResourceFactoryImpl factory = new PomResourceFactoryImpl();
    resource = (PomResourceImpl) factory.createResource(uri);

    // disable SSE support for read-only external documents
    EMF2DOMRenderer renderer = new EMF2DOMRenderer();
    renderer.setValidating(false);
    resource.setRenderer(renderer);

    try {
      resource.load(Collections.EMPTY_MAP);
      return resource.getModel();

    } catch(Exception ex) {
      MavenLogger.log("Can't load model " + path, ex);
      return null;

    }
  }

  /**
   * @param force
   * @param monitor
   * @param scope one of 
   *   {@link Artifact#SCOPE_COMPILE}, 
   *   {@link Artifact#SCOPE_TEST}, 
   *   {@link Artifact#SCOPE_SYSTEM}, 
   *   {@link Artifact#SCOPE_PROVIDED}, 
   *   {@link Artifact#SCOPE_RUNTIME}
   *   
   * @return dependency node
   */
  public synchronized DependencyNode readDependencies(boolean force, IProgressMonitor monitor, String scope) throws CoreException {
    if(force || !rootNode.containsKey(scope)) {
      MavenPlugin plugin = MavenPlugin.getDefault();

      try {
        monitor.setTaskName("Reading project");
        MavenProject mavenProject = readMavenProject(force, monitor);
        if(mavenProject == null){
          MavenLogger.log("Unable to read maven project. Dependencies not updated.", null);
          return null;
        }
        MavenProjectManager mavenProjectManager = plugin.getMavenProjectManager();
        MavenEmbedder embedder = mavenProjectManager.createWorkspaceEmbedder();
        try {
          monitor.setTaskName("Building dependency tree");
          PlexusContainer plexus = embedder.getPlexusContainer();

          ArtifactFactory artifactFactory = (ArtifactFactory) plexus.lookup(ArtifactFactory.class);
          ArtifactMetadataSource artifactMetadataSource = //
          (ArtifactMetadataSource) plexus.lookup(ArtifactMetadataSource.class);

          ArtifactCollector artifactCollector = (ArtifactCollector) plexus.lookup(ArtifactCollector.class);

          ArtifactRepository localRepository = embedder.getLocalRepository();

          DependencyTreeBuilder builder = (DependencyTreeBuilder) plexus.lookup(DependencyTreeBuilder.ROLE);
          DependencyNode node = builder.buildDependencyTree(mavenProject, localRepository, artifactFactory,
              artifactMetadataSource, null, artifactCollector);
          
          BuildingDependencyNodeVisitor visitor = new BuildingDependencyNodeVisitor(); 
          node.accept(new FilteringDependencyNodeVisitor(visitor,
              new ArtifactDependencyNodeFilter(new ScopeArtifactFilter(scope))));
//          node.accept(visitor);
          
          rootNode.put(scope, visitor.getDependencyTree());
        } finally {
          embedder.stop();
        }

      } catch(MavenEmbedderException ex) {
        String msg = "Can't create Maven embedder";
        MavenLogger.log(msg, ex);
        throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

      } catch(ComponentLookupException ex) {
        String msg = "Component lookup error";
        MavenLogger.log(msg, ex);
        throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

      } catch(DependencyTreeBuilderException ex) {
        String msg = "Project read error";
        MavenLogger.log(msg, ex);
        throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

      }
    }

    return rootNode.get(scope);
  }

  public MavenProject readMavenProject(boolean force, IProgressMonitor monitor) throws MavenEmbedderException,
      CoreException {
    if(force || mavenProject == null) {
      IEditorInput input = getEditorInput();
      
      if(input instanceof IFileEditorInput) {
        IFileEditorInput fileInput = (IFileEditorInput) input;
        pomFile = fileInput.getFile();
        pomFile.refreshLocal(1, null);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
      }
      
      mavenProject = SelectionUtil.getMavenProject(input);
    }
    return mavenProject;
  }

  public MetadataResolutionResult readDependencyMetadata(IFile pomFile, IProgressMonitor monitor) throws CoreException {
    File pom = pomFile.getLocation().toFile();

    MavenPlugin plugin = MavenPlugin.getDefault();

    MavenEmbedder embedder = null;
    try {
      MavenProjectManager mavenProjectManager = plugin.getMavenProjectManager();
      embedder = mavenProjectManager.createWorkspaceEmbedder();

      monitor.setTaskName("Reading project");
      monitor.subTask("Reading project");

      // ResolverConfiguration configuration = new ResolverConfiguration();
      // MavenExecutionRequest request = embedderManager.createRequest(embedder);
      // request.setPomFile(pom.getAbsolutePath());
      // request.setBaseDirectory(pom.getParentFile());
      // request.setTransferListener(new TransferListenerAdapter(monitor, console, indexManager));
      // request.setProfiles(configuration.getActiveProfileList());
      // request.addActiveProfiles(configuration.getActiveProfileList());
      // request.setRecursive(false);
      // request.setUseReactor(false);

      // MavenExecutionResult result = projectManager.readProjectWithDependencies(embedder, pomFile, //
      // configuration, offline, monitor);

      // MavenExecutionResult result = embedder.readProjectWithDependencies(request);
      // MavenProject project = result.getProject();

      MavenProject project = embedder.readProject(pom);
      if(project == null) {
        return null;
      }

      ArtifactRepository localRepository = embedder.getLocalRepository();

      @SuppressWarnings("unchecked")
      List<ArtifactRepository> remoteRepositories = project.getRemoteArtifactRepositories();

      PlexusContainer plexus = embedder.getPlexusContainer();

      MetadataResolver resolver = (MetadataResolver) plexus.lookup(MetadataResolver.ROLE, "default");

      ArtifactMetadata query = new ArtifactMetadata(project.getGroupId(), project.getArtifactId(), project.getVersion());

      monitor.subTask("Building dependency graph...");
      MetadataResolutionResult result = resolver.resolveMetadata( //
          new MetadataResolutionRequest(query, localRepository, remoteRepositories));

      if(result == null) {
        return null;
      }

      monitor.subTask("Building dependency tree");
      result.initTreeProcessing(plexus);
      return result;

    } catch(MetadataResolutionException ex) {
      String msg = "Metadata resolution error";
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

    } catch(ComponentLookupException ex) {
      String msg = "Metadata resolver error";
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

    } catch(ProjectBuildingException ex) {
      String msg = "Metadata resolver error";
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

    } catch(ExtensionScanningException ex) {
      String msg = "Metadata resolver error";
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

    } catch(MavenExecutionException ex) {
      String msg = "Metadata resolver error";
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

    } finally {
      if(embedder != null) {
        try {
          embedder.stop();
        } catch(MavenEmbedderException ex) {
          MavenLogger.log("Can't stop Maven Embedder", ex);
        }
      }
    }

  }

  public void dispose() {
    new UIJob("Disposing") {
      @SuppressWarnings("synthetic-access")
      public IStatus runInUIThread(IProgressMonitor monitor) {
        structuredModel.releaseFromEdit();
        if (sseCommandStack != null)
          sseCommandStack.removeCommandStackListener(commandStackListener);

        if(activationListener != null) {
          activationListener.dispose();
          activationListener = null;
        }

        ResourcesPlugin.getWorkspace().removeResourceChangeListener(MavenPomEditor.this);
        
        structuredModel.removeModelStateListener(renderer);
        structuredModel.removeModelLifecycleListener(renderer);
        
        MavenPomEditor.super.dispose();
        return Status.OK_STATUS;
      }
    }.schedule();
  }

  /**
   * Saves structured editor XXX form model need to be synchronized
   */
  public void doSave(IProgressMonitor monitor) {
    new UIJob("Saving") {
      public IStatus runInUIThread(IProgressMonitor monitor) {
        sourcePage.doSave(monitor);
        return Status.OK_STATUS;
      }
    }.schedule();
  }

  public void doSaveAs() {
    // IEditorPart editor = getEditor(0);
    // editor.doSaveAs();
    // setPageText(0, editor.getTitle());
    // setInput(editor.getEditorInput());
  }

  /*
   * (non-Javadoc) Method declared on IEditorPart.
   */
  public boolean isSaveAsAllowed() {
    return false;
  }

  public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
//    if(!(editorInput instanceof IStorageEditorInput)) {
//      throw new PartInitException("Unsupported editor input " + editorInput);
//    }

    setPartName(editorInput.getToolTipText());
    // setContentDescription(name);

    super.init(site, editorInput);

    activationListener = new MavenPomActivationListener(site.getWorkbenchWindow().getPartService());
  }

  public void showInSourceEditor(EObject o) {
    IDOMElement element = getElement(o);
    if(element != null) {
      int start = element.getStartOffset();
      int lenght = element.getLength();
      setActivePage(sourcePageIndex);
      sourcePage.selectAndReveal(start, lenght);
    }
  }

  public IDOMElement getElement(EObject o) {
    for(Adapter adapter : o.eAdapters()) {
      if(adapter instanceof EMF2DOMSSEAdapter) {
        EMF2DOMSSEAdapter a = (EMF2DOMSSEAdapter) adapter;
        if(a.getNode() instanceof IDOMElement) {
          return (IDOMElement) a.getNode();
        }
        break;
      }
    }
    return null;
  }

  // IShowEditorInput

  public void showEditorInput(IEditorInput editorInput) {
    // could activate different tabs based on the editor input
  }

  // IGotoMarker

  public void gotoMarker(IMarker marker) {
    // TODO use selection to activate corresponding form page elements
    setActivePage(sourcePageIndex);
    IGotoMarker adapter = (IGotoMarker) sourcePage.getAdapter(IGotoMarker.class);
    adapter.gotoMarker(marker);
  }

  // ISearchEditorAccess

  public IDocument getDocument(Match match) {
    return sourcePage.getDocumentProvider().getDocument(getEditorInput());
  }

  public IAnnotationModel getAnnotationModel(Match match) {
    return sourcePage.getDocumentProvider().getAnnotationModel(getEditorInput());
  }

  public boolean isDirty() {
    return sourcePage.isDirty();
  }

  public List<MavenPomEditorPage> getPages() {
    return pages;
  }

  public void showDependencyHierarchy(ArtifactKey artifactKey) {
    setActivePage(dependencyTreePage.getId());
    dependencyTreePage.selectDepedency(artifactKey);
  }

  
  /**
   * Adapted from <code>org.eclipse.ui.texteditor.AbstractTextEditor.ActivationListener</code>
   */
  class MavenPomActivationListener implements IPartListener, IWindowListener, IPropertyChangeListener {

    private IWorkbenchPart activePart;

    private boolean isHandlingActivation = false;

    private IPartService partService;

    public MavenPomActivationListener(IPartService partService) {
      this.partService = partService;
      this.partService.addPartListener(this);
      PlatformUI.getWorkbench().addWindowListener(this);
      MavenPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
    }

    public void dispose() {
      partService.removePartListener(this);
      PlatformUI.getWorkbench().removeWindowListener(this);
      MavenEditorPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
      partService = null;
    }

    // IPartListener

    public void partActivated(IWorkbenchPart part) {
      activePart = part;
      handleActivation();
    }

    public void partBroughtToTop(IWorkbenchPart part) {
    }

    public void partClosed(IWorkbenchPart part) {
    }

    public void partDeactivated(IWorkbenchPart part) {
      activePart = null;
    }

    public void partOpened(IWorkbenchPart part) {
    }

    // IWindowListener

    public void windowActivated(IWorkbenchWindow window) {
      if(window == getEditorSite().getWorkbenchWindow()) {
        /*
         * Workaround for problem described in
         * http://dev.eclipse.org/bugs/show_bug.cgi?id=11731
         * Will be removed when SWT has solved the problem.
         */
        window.getShell().getDisplay().asyncExec(new Runnable() {
          public void run() {
            handleActivation();
          }
        });
      }
    }

    public void windowDeactivated(IWorkbenchWindow window) {
    }

    public void windowClosed(IWorkbenchWindow window) {
    }

    public void windowOpened(IWorkbenchWindow window) {
    }

    /**
     * Handles the activation triggering a element state check in the editor.
     */
    void handleActivation() {
      if(isHandlingActivation) {
        return;
      }

      if(activePart == MavenPomEditor.this) {
        isHandlingActivation = true;
        try {
          final boolean[] changed = new boolean[] {false};
          ITextListener listener = new ITextListener() {
            public void textChanged(TextEvent event) {
              changed[0] = true;
            }
          };

          if (sourcePage != null) {
            sourcePage.getTextViewer().addTextListener(listener);
            try {
              sourcePage.safelySanityCheckState(getEditorInput());
            } finally {
              sourcePage.getTextViewer().removeTextListener(listener);
            }
          }
          
          if(changed[0]) {
            
            try {
              pomFile.refreshLocal(IResource.DEPTH_ZERO, null);
            } catch(CoreException e) {
              MavenLogger.log(e);
            } 
          }
          
        } finally {
          isHandlingActivation = false;
        }
      }
    }

    public void propertyChange(PropertyChangeEvent event) {
      if(event.getProperty().equals(PomEditorPreferencePage.P_SHOW_ADVANCED_TABS)){
        showAdvancedPages();
      }
    }
  }

  public StructuredTextEditor getSourcePage() {
    return sourcePage;
  }

  @Override
  public IFormPage setActivePage(String pageId) {
    if(pageId == null) {
      setActivePage(sourcePageIndex);
    }
    return super.setActivePage(pageId);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object getAdapter(Class adapter) {
    Object result = super.getAdapter(adapter);
    if(result != null && Display.getCurrent() == null) {
      return result; 
    }
    return sourcePage.getAdapter(adapter);
  }

  public IFile getPomFile() {
    return pomFile;
  }

  
}
