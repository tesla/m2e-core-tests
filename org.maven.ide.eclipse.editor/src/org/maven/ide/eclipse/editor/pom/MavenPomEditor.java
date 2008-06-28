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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.metadata.ArtifactMetadata;
import org.apache.maven.artifact.resolver.metadata.MetadataResolutionException;
import org.apache.maven.artifact.resolver.metadata.MetadataResolutionRequest;
import org.apache.maven.artifact.resolver.metadata.MetadataResolutionResult;
import org.apache.maven.artifact.resolver.metadata.MetadataResolver;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.extension.ExtensionScanningException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.ReflectiveItemProviderAdapterFactory;
import org.eclipse.emf.edit.provider.resource.ResourceItemProviderAdapterFactory;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.common.internal.emf.resource.EMF2DOMRenderer;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.xml.core.internal.emf2xml.EMF2DOMSSEAdapter;
import org.eclipse.wst.xml.core.internal.emf2xml.EMF2DOMSSERenderer;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.util.PomResourceFactoryImpl;
import org.maven.ide.components.pom.util.PomResourceImpl;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.editor.MavenEditorPlugin;
import org.maven.ide.eclipse.embedder.EmbedderFactory;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.embedder.MavenModelManager;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.MavenRunnable;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Maven POM editor
 * 
 * @author Eugene Kuleshov
 */
@SuppressWarnings("restriction")
public class MavenPomEditor extends FormEditor implements IResourceChangeListener {

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
  
  List<MavenPomEditorPage> pages = new ArrayList<MavenPomEditorPage>();
  
  private Model projectDocument;

  private DependencyNode rootNode;

  private PomResourceImpl resource;

  private IStructuredModel structuredModel;

  private EMF2DOMSSERenderer renderer;

  private MavenProject mavenProject;

  private AdapterFactory adapterFactory;

  private AdapterFactoryEditingDomain editingDomain;
  
  public MavenPomEditor() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
  }

  // IResourceChangeListener

  /**
   * Closes all project files on project close.
   */
  public void resourceChanged(final IResourceChangeEvent event) {
    if(event.getType() == IResourceChangeEvent.PRE_CLOSE) {
      Display.getDefault().asyncExec(new Runnable() {
        public void run() {
          IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
          for(int i = 0; i < pages.length; i++ ) {
            if(((FileEditorInput) getEditorInput()).getFile().getProject().equals(event.getResource())) {
              IEditorPart editorPart = pages[i].findEditor(getEditorInput());
              pages[i].closeEditor(editorPart, true);
            }
          }
        }
      });
    }
  }

  public void reload() {
    projectDocument = null;
    try {
      readProjectDocument();
    } catch(CoreException e) {
      MavenPlugin.log(e);
    }
    for(MavenPomEditorPage page : pages) {
      page.reload();
    }
  }
  
  protected void addPages() {
    overviewPage = new OverviewPage(this);
    addPomPage(overviewPage);
    
    dependenciesPage = new DependenciesPage(this);
    addPomPage(dependenciesPage);
    
    repositoriesPage = new RepositoriesPage(this);
    addPomPage(repositoriesPage);

    buildPage = new BuildPage(this);
    addPomPage(buildPage);
    
    pluginsPage = new PluginsPage(this);
    addPomPage(pluginsPage);
    
    reportingPage = new ReportingPage(this);
    addPomPage(reportingPage);
    
    profilesPage = new ProfilesPage(this);
    addPomPage(profilesPage);

    teamPage = new TeamPage(this);
    addPomPage(teamPage);
    
    dependencyTreePage = new DependencyTreePage(this);
    addPomPage(dependencyTreePage);
    
    graphPage = new DependencyGraphPage(this);
    addPomPage(graphPage);
    
    sourcePage = new StructuredTextEditor();
    sourcePage.setEditorPart(this);

    try {
      int sourcePageIndex = addPage(sourcePage, getEditorInput());
      setPageText(sourcePageIndex, "pom.xml");
      sourcePage.update();
      try {
        readProjectDocument();
      } catch(CoreException e) {
        MavenPlugin.log(e);
      }
      
      IDocument doc = sourcePage.getDocumentProvider().getDocument(getEditorInput());
      if (doc instanceof IStructuredDocument) {
        List<AdapterFactoryImpl> factories = new ArrayList<AdapterFactoryImpl>();
        factories.add(new ResourceItemProviderAdapterFactory());
        factories.add(new ReflectiveItemProviderAdapterFactory());
  
        adapterFactory = new ComposedAdapterFactory(factories);
        editingDomain = new AdapterFactoryEditingDomain(adapterFactory, //
            new NotificationCommandStack(pages), new HashMap<Resource, Boolean>());
        
        IModelManager modelManager = StructuredModelManager.getModelManager();
        structuredModel = modelManager.getExistingModelForEdit(doc);
        if(structuredModel == null) {
          structuredModel = modelManager.getModelForEdit((IStructuredDocument) doc);
        }
        if(resource!=null && resource.getRenderer() instanceof EMF2DOMSSERenderer) {
          renderer = (EMF2DOMSSERenderer) resource.getRenderer();
          structuredModel.addModelStateListener(renderer);
          structuredModel.addModelLifecycleListener(renderer);
        }
      }
    } catch(PartInitException ex) {
      MavenPlugin.log(ex);
    }
  }

  public boolean isReadOnly() {
    return !(getEditorInput() instanceof IFileEditorInput);
  }
  
  private int addPomPage(IFormPage page) {
    try {
      if (page instanceof MavenPomEditorPage) {
        pages.add((MavenPomEditorPage) page);
      }
      return addPage(page);
    } catch (PartInitException ex) {
      MavenPlugin.log(ex);
      return -1;
    }
  }
  
  public EditingDomain getEditingDomain() {
    return editingDomain;
  }
  
  // XXX move to MavenModelManager (CommandStack and EditorDomain too)
  public synchronized Model readProjectDocument() throws CoreException {
    if(projectDocument==null) {
      IEditorInput input = getEditorInput();
      if(input instanceof IFileEditorInput) {
        IFile pomFile = ((IFileEditorInput) input).getFile();

        MavenModelManager modelManager = MavenPlugin.getDefault().getMavenModelManager();
        PomResourceImpl resource = modelManager.loadResource(pomFile);
        projectDocument = resource.getModel();
      
      } else if(input instanceof IStorageEditorInput) {
        IStorageEditorInput fileInput = (IStorageEditorInput) input;
        IStorage storage = fileInput.getStorage();
        IPath path = storage.getFullPath();
        if(path == null || path.toString().startsWith("http") || path.toString().startsWith("svn")) {
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
            MavenPlugin.log("Can't close stream", ex);
          } finally {
            IOUtil.close(is);
            IOUtil.close(os);
            if(tempPomFile!=null) {
              tempPomFile.delete();
            }
          }
        } else {
          projectDocument = loadModel(path.toOSString());
        }
      
      } else if(input.getClass().getName().endsWith("FileStoreEditorInput")) {
        // since Eclipse 3.3
        java.net.URI uri = FormUtils.proxy(input, C.class).getURI();
        projectDocument = loadModel(uri.getPath());
      }
    }

    return projectDocument;
  }

  /**
   * Stub interface for FileStoreEditorInput
   * 
   * @see FormUtils#proxy(Object, Class)
   */
  public static interface C {
    public java.net.URI getURI();
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
    
    } catch(IOException ex) {
      MavenPlugin.log("Can't load model " + path, ex);
      return null;

    }
  }

  public synchronized DependencyNode readDependencies(boolean force, IProgressMonitor monitor) throws CoreException {
    if(force || rootNode==null) {
      MavenPlugin plugin = MavenPlugin.getDefault();
  
      try {
        monitor.setTaskName("Reading project");
        MavenProject mavenProject = readMavenProject(force);
    
        MavenEmbedderManager embedderManager = plugin.getMavenEmbedderManager();
        MavenEmbedder embedder = embedderManager.createEmbedder(EmbedderFactory.createWorkspaceCustomizer());
        
        monitor.setTaskName("Building dependency tree");
        PlexusContainer plexus = embedder.getPlexusContainer();
    
        ArtifactFactory artifactFactory = (ArtifactFactory) plexus.lookup(ArtifactFactory.class);
        ArtifactMetadataSource artifactMetadataSource = (ArtifactMetadataSource) plexus.lookup(ArtifactMetadataSource.class);
    
        ArtifactCollector artifactCollector = (ArtifactCollector) plexus.lookup(ArtifactCollector.class);
    
        // ArtifactFilter artifactFilter = new ScopeArtifactFilter( scope );
        ArtifactFilter artifactFilter = null;
    
        ArtifactRepository localRepository = embedder.getLocalRepository();
        
        DependencyTreeBuilder builder = (DependencyTreeBuilder) plexus.lookup(DependencyTreeBuilder.ROLE);
        
        rootNode = builder.buildDependencyTree(mavenProject,
            localRepository, artifactFactory, artifactMetadataSource,
            artifactFilter, artifactCollector);
    
  //       DependencyNodeVisitor visitor = new DependencyNodeVisitor() {
  //         public boolean visit(DependencyNode dependencynode) {
  //           return true;
  //         }
  //         public boolean endVisit(DependencyNode dependencynode) {
  //           return true;
  //         }
  //       };
  //  
  //      // TODO remove the need for this when the serializer can calculate last nodes from visitor calls only
  //      visitor = new BuildingDependencyNodeVisitor(visitor);
  //      rootNode.accept(visitor);
        
      } catch(MavenEmbedderException ex) {
        String msg = "Can't create Maven embedder";
        MavenPlugin.log(msg, ex);
        throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));
        
      } catch(ComponentLookupException ex) {
        String msg = "Component lookup error";
        MavenPlugin.log(msg, ex);
        throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));
        
      } catch(DependencyTreeBuilderException ex) {
        String msg = "Project read error";
        MavenPlugin.log(msg, ex);
        throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));
        
      }
    }
    
    return rootNode;
  }
  
  public MavenProject readMavenProject(boolean force) throws MavenEmbedderException, CoreException {
    if(force || mavenProject==null) {
      MavenPlugin plugin = MavenPlugin.getDefault();
      MavenProjectManager projectManager = plugin.getMavenProjectManager();
      
      IEditorInput input = getEditorInput();
      if(input instanceof IFileEditorInput) {
        IFileEditorInput fileInput = (IFileEditorInput) input;
        IFile pomfile = fileInput.getFile();
        
        MavenProjectFacade projectFacade = projectManager.create(pomfile, true, new NullProgressMonitor());
        mavenProject = projectFacade.getMavenProject(); 
  
      } else if(input instanceof IStorageEditorInput) {
        IStorageEditorInput storageInput = (IStorageEditorInput) input;
        IStorage storage = storageInput.getStorage();
        IPath path = storage.getFullPath();
        if(path == null || path.toString().startsWith("http") || path.toString().startsWith("svn")) {
          InputStream is = null;
          FileOutputStream fos = null;
          File tempPomFile = null;
          try {
            tempPomFile = File.createTempFile("maven-pom", "pom");
            is = storage.getContents();
            fos = new FileOutputStream(tempPomFile);
            IOUtil.copy(is, fos);
            mavenProject = readMavenProject(tempPomFile);
          } catch(Exception ex) {
            MavenPlugin.log("Can't load POM", ex);
          } finally {
            IOUtil.close(is);
            IOUtil.close(fos);
            if(tempPomFile != null) {
              tempPomFile.delete();
            }
          }
        } else {
          mavenProject = readMavenProject(path.toFile());
        }
      } else if(input instanceof IURIEditorInput) {
        IURIEditorInput uriInput = (IURIEditorInput) input;
        mavenProject = readMavenProject(new File(uriInput.getURI().getPath()));
      }
    }
    return mavenProject;
  }

  private MavenProject readMavenProject(File pomFile) throws MavenEmbedderException {
    MavenPlugin plugin = MavenPlugin.getDefault();
    MavenEmbedderManager embedderManager = plugin.getMavenEmbedderManager();
    MavenEmbedder embedder = embedderManager.createEmbedder(EmbedderFactory.createWorkspaceCustomizer());
    ResolverConfiguration resolverConfiguration = new ResolverConfiguration();
    IProgressMonitor monitor = new NullProgressMonitor();
    
    MavenProjectManager projectManager = plugin.getMavenProjectManager();
    MavenExecutionResult result = projectManager.execute(embedder, pomFile, resolverConfiguration, new MavenRunnable() {
      public MavenExecutionResult execute(MavenEmbedder embedder, MavenExecutionRequest request) {
        request.setOffline(false);
        request.setUpdateSnapshots(false);
        return embedder.readProjectWithDependencies(request);
      }
    }, monitor);
    
    // XXX handle project read errors
    // result.getExceptions();
    // result.getArtifactResolutionResult();
    
    return result.getProject();
  }

  public MetadataResolutionResult readDependencyMetadata(IFile pomFile, IProgressMonitor monitor) throws CoreException {
    File pom = pomFile.getLocation().toFile();
    
    MavenPlugin plugin = MavenPlugin.getDefault();

    MavenEmbedder embedder = null;
    try {
      MavenEmbedderManager embedderManager = plugin.getMavenEmbedderManager();
      embedder = embedderManager.createEmbedder(EmbedderFactory.createWorkspaceCustomizer());

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
      if (project == null) {
        return null;
      }
      
      ArtifactRepository localRepository = embedder.getLocalRepository();

      @SuppressWarnings("unchecked")
      List<ArtifactRepository> remoteRepositories = project.getRemoteArtifactRepositories();

      PlexusContainer plexus = embedder.getPlexusContainer();

      MetadataResolver resolver = (MetadataResolver) plexus.lookup(MetadataResolver.ROLE, "default");

      ArtifactMetadata query = new ArtifactMetadata(project.getGroupId(),
          project.getArtifactId(), project.getVersion());

      monitor.subTask("Building dependency graph...");
      MetadataResolutionResult result = resolver.resolveMetadata( //
          new MetadataResolutionRequest(query, localRepository, remoteRepositories));

      if (result == null) {
        return null;
      }
      
      monitor.subTask("Building dependency tree");
      result.initTreeProcessing(plexus);
      return result;

    } catch (MavenEmbedderException ex) {
      String msg = "Can't create Maven embedder";
      MavenPlugin.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

    } catch (MetadataResolutionException ex) {
      String msg = "Metadata resolution error";
      MavenPlugin.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

    } catch (ComponentLookupException ex) {
      String msg = "Metadata resolver error";
      MavenPlugin.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

    } catch (ProjectBuildingException ex) {
      String msg = "Metadata resolver error";
      MavenPlugin.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

    } catch (ExtensionScanningException ex) {
      String msg = "Metadata resolver error";
      MavenPlugin.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

    } catch (MavenExecutionException ex) {
      String msg = "Metadata resolver error";
      MavenPlugin.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

    } finally {
      if (embedder != null) {
        try {
          embedder.stop();
        } catch (MavenEmbedderException ex) {
          MavenPlugin.log("Can't stop Maven Embedder", ex);
        }
      }
    }
    
  }
  
  public void dispose() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
    structuredModel.removeModelStateListener(renderer);
    structuredModel.removeModelLifecycleListener(renderer);
    super.dispose();
  }

  /**
   * Saves structured editor
   * 
   * XXX form model need to be synchronized
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

  public void gotoMarker(IMarker marker) {
    // setActivePage(0);
    // IDE.gotoMarker(getEditor(0), marker);
  }

  public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
//    if(!(editorInput instanceof IStorageEditorInput)) {
//      throw new PartInitException("Unsupported editor input " + editorInput);
//    }

    setPartName(editorInput.getToolTipText());
    // setContentDescription(name);
    
    super.init(site, editorInput);
  }

  public void showInSourceEditor(EObject o) {
    IDOMElement element = getElement(o);
    if(element!=null) {
      int start = element.getStartOffset();
      int lenght = element.getLength();
      setActiveEditor(sourcePage);
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
      }
    }
    return null;
  }

  // XXX move to model and translators
  public EList<PropertyPair> getProperties(EObject o) {
    IDOMElement node = getElement(o);
    if(node!=null) {
      NodeList elements = node.getElementsByTagName("properties");
      if(elements!=null && elements.getLength()>0) {
        Node propertiesNode = elements.item(0);
        NodeList propertiesElements = propertiesNode.getChildNodes();
        
        EList<PropertyPair> properties = new BasicEList<PropertyPair>();
        for(int i = 0; i < propertiesElements.getLength(); i++ ) {
          Node item = propertiesElements.item(i);
          if(item instanceof Element) {
            String nodetext = getNodeText(item);
            
            properties.add(new PropertyPair(item.getNodeName(), nodetext));
          }
        }
        return properties;
      }
    }
    return null;
  }
  
  private String getNodeText(Node node) {
    NodeList childNodes = node.getChildNodes();
    for(int i = 0; i < childNodes.getLength(); i++ ) {
      Node childNode = childNodes.item(i);
      if(childNode.getNodeType()==Node.TEXT_NODE) {
        return childNode.getNodeValue();
      }
    }
    return null;
  }
  
}
