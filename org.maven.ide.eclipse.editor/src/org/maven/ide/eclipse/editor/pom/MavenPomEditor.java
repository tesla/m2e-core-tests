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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.emf.edit.domain.IEditingDomainProvider;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.ReflectiveItemProviderAdapterFactory;
import org.eclipse.emf.edit.provider.resource.ResourceItemProviderAdapterFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.search.ui.text.ISearchEditorAccess;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IShowEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.common.internal.emf.resource.EMF2DOMRenderer;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.xml.core.internal.document.ElementImpl;
import org.eclipse.wst.xml.core.internal.emf2xml.EMF2DOMSSEAdapter;
import org.eclipse.wst.xml.core.internal.emf2xml.EMF2DOMSSERenderer;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.util.PomResourceFactoryImpl;
import org.maven.ide.components.pom.util.PomResourceImpl;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
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
 * @author Anton Kraev
 */
@SuppressWarnings("restriction")
public class MavenPomEditor extends FormEditor implements IResourceChangeListener, IShowEditorInput, IGotoMarker, ISearchEditorAccess, IEditingDomainProvider {

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

  IStructuredModel structuredModel;

  private EMF2DOMSSERenderer renderer;

  private MavenProject mavenProject;

  private AdapterFactory adapterFactory;

  private AdapterFactoryEditingDomain editingDomain;
  
  private int sourcePageIndex;

  NotificationCommandStack commandStack;

  IModelManager modelManager;

  IFile pomFile;

  public MavenPomEditor() {
    modelManager = StructuredModelManager.getModelManager();
  }

  // IResourceChangeListener

  /**
   * Closes all project files on project close.
   */
  public void resourceChanged(final IResourceChangeEvent event) {
    
    class ResourceDeltaVisitor implements IResourceDeltaVisitor {
      public boolean changed = false;
      
      public boolean visit(IResourceDelta delta) {
        if(delta.getFlags() != IResourceDelta.MARKERS && delta.getResource().getType() == IResource.FILE) {
          if((delta.getKind() & (IResourceDelta.CHANGED | IResourceDelta.REMOVED)) != 0) {
            URI id = URI.createURI(delta.getFullPath().toString());
            if(modelManager.getExistingModelForEdit(id.toString()) != null) {
              if((delta.getKind() & IResourceDelta.REMOVED) != 0) {
                // XXX remove is not implemented
                // modelManager.removeModel();
              } else {
                changed = true;
              }
            }
          }
        }
        return true;
      }
    }

    try {
      ResourceDeltaVisitor visitor = new ResourceDeltaVisitor();
      event.getDelta().accept(visitor);
      
      if(visitor.changed) {
        getContainer().getDisplay().asyncExec(new Runnable() {
          public void run() {
            if(MessageDialog.openQuestion(getSite().getShell(), "File Changed",
                "File has been changes externally. Do you want to replace editor content with these changes?")) {
              new Job("") {
                protected IStatus run(IProgressMonitor monitor) {
                  try {
                    structuredModel.reload(pomFile.getContents());
                    reload();
                  } catch(CoreException e) {
                    MavenLogger.log(e);
                  } catch(Exception e) {
                    MavenLogger.log("Can't load model", e);
                  }
                  return Status.OK_STATUS;
                }
              }.schedule();
            }
          }
        });
      }
      
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }

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
      MavenLogger.log(e);
    }
    for(MavenPomEditorPage page : pages) {
      page.reload();
    }
    commandStack.flush();
    editorDirtyStateChanged();
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
      sourcePageIndex = addPage(sourcePage, getEditorInput());
      setPageText(sourcePageIndex, "pom.xml");
      sourcePage.update();
      try {
        readProjectDocument();
      } catch(CoreException e) {
        MavenLogger.log(e);
      }
      
      // TODO activate xml source page is model is empty or have errors
      IDocument doc = sourcePage.getDocumentProvider().getDocument(getEditorInput());
      if (doc instanceof IStructuredDocument) {
        List<AdapterFactoryImpl> factories = new ArrayList<AdapterFactoryImpl>();
        factories.add(new ResourceItemProviderAdapterFactory());
        factories.add(new ReflectiveItemProviderAdapterFactory());
  
        commandStack = new NotificationCommandStack(this);
        adapterFactory = new ComposedAdapterFactory(factories);
        editingDomain = new AdapterFactoryEditingDomain(adapterFactory, //
            commandStack, new HashMap<Resource, Boolean>());

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
      MavenLogger.log(ex);
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
      MavenLogger.log(ex);
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
      MavenLogger.log("Can't load model " + path, ex);
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
    
    return rootNode;
  }
  
  public MavenProject readMavenProject(boolean force) throws MavenEmbedderException, CoreException {
    if(force || mavenProject==null) {
      MavenPlugin plugin = MavenPlugin.getDefault();
      MavenProjectManager projectManager = plugin.getMavenProjectManager();
      
      IEditorInput input = getEditorInput();
      if(input instanceof IFileEditorInput) {
        IFileEditorInput fileInput = (IFileEditorInput) input;
        pomFile = fileInput.getFile();
        pomFile.refreshLocal(1, null);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);

        MavenProjectFacade projectFacade = projectManager.create(pomFile, true, new NullProgressMonitor());
        mavenProject = projectFacade.getMavenProject();

      } else if(input instanceof IStorageEditorInput) {
        IStorageEditorInput storageInput = (IStorageEditorInput) input;
        IStorage storage = storageInput.getStorage();
        IPath path = storage.getFullPath();
        if(path == null || !new File(path.toOSString()).exists()) {
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
            MavenLogger.log("Can't load POM", ex);
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
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

    } catch (MetadataResolutionException ex) {
      String msg = "Metadata resolution error";
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

    } catch (ComponentLookupException ex) {
      String msg = "Metadata resolver error";
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

    } catch (ProjectBuildingException ex) {
      String msg = "Metadata resolver error";
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

    } catch (ExtensionScanningException ex) {
      String msg = "Metadata resolver error";
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

    } catch (MavenExecutionException ex) {
      String msg = "Metadata resolver error";
      MavenLogger.log(msg, ex);
      throw new CoreException(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, -1, msg, ex));

    } finally {
      if (embedder != null) {
        try {
          embedder.stop();
        } catch (MavenEmbedderException ex) {
          MavenLogger.log("Can't stop Maven Embedder", ex);
        }
      }
    }
    
  }
  
  public void dispose() {
    try {
      structuredModel.reload(pomFile.getContents());
    } catch(Exception e) {
      MavenLogger.log("Can't reload model", e);
    }
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
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(MavenPomEditor.this);
        sourcePage.doSave(monitor);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(MavenPomEditor.this);
        commandStack.saveIsDone();
        editorDirtyStateChanged();
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
        break;
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
    return new BasicEList<PropertyPair>();
  }
  
  public void setElement(EObject o, IDOMElement element) {
    for(Adapter adapter : o.eAdapters()) {
      if(adapter instanceof EMF2DOMSSEAdapter) {
        EMF2DOMSSEAdapter a = (EMF2DOMSSEAdapter) adapter;
        a.setNode(element);
        break;
      }
    }
  }

  public void setProperties(EObject o, EList<PropertyPair> properties) {
    ElementImpl node = (ElementImpl) getElement(o);
    if(node!=null) {
      EList<Adapter> adapters = new BasicEList<Adapter>(o.eAdapters());
      o.eAdapters().clear();
      NodeList old = node.getElementsByTagName("properties");
      if (old != null && old.getLength() > 0) {
        node.removeChild(old.item(0));
      }
      Node elements = node.getOwnerDocument().createElement("properties");
      node.appendChild(elements);
      o.eAdapters().addAll(adapters);
      for (PropertyPair p: properties) {
        Node prop = node.getOwnerDocument().createElement(p.getKey());
        elements.appendChild(prop);
        prop.setTextContent(p.getValue());
      }
    }
    setElement(o, node);
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
    return commandStack.isSaveNeeded();
  }

  public List<MavenPomEditorPage> getPages() {
    return pages;
  }
  
  /*public void menuAboutToShow(IMenuManager menuManager) {
    ((IMenuListener)getEditorSite().getActionBarContributor()).menuAboutToShow(menuManager);
  }

  public EditingDomainActionBarContributor getActionBarContributor() {
    return (EditingDomainActionBarContributor)getEditorSite().getActionBarContributor();
  }

  public IActionBars getActionBars() {
    return getActionBarContributor().getActionBars();
  }*/
}

