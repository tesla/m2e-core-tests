/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.actions;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionDelegate;
import org.eclipse.ui.part.FileEditorInput;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.model.Dependency;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.wizards.MavenRepositorySearchDialog;


/**
 * Open POM Action
 * 
 * @author Eugene Kuleshov
 */
public class OpenPomAction extends ActionDelegate implements IWorkbenchWindowActionDelegate, IExecutableExtension {

  public static final String ID = "org.maven.ide.eclipse.openPomAction";
  
  String type = IndexManager.SEARCH_ARTIFACT;

  private IStructuredSelection selection;

  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
   */
  public void init(IWorkbenchWindow window) {
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
   */
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    if("class".equals(data)) {
      this.type = IndexManager.SEARCH_CLASS_NAME;
    } else if("plugins".equals(data)) {
      this.type = IndexManager.SEARCH_PACKAGING;
    } else {
      this.type = IndexManager.SEARCH_ARTIFACT;
    }
  }
  
  public void selectionChanged(IAction action, ISelection selection) {
    if(selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    } else {
      this.selection = null;
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.actions.ActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run(IAction action) {
    // TODO check if POM is in Eclipse workspace
    
    if(selection!=null) {
      Object element = this.selection.getFirstElement();
      if(IndexManager.SEARCH_ARTIFACT.equals(type) && element !=null) {
        final ArtifactKey a = getArtifact(element);
        if(a!=null) {
          new Job("Opening POM") {
            protected IStatus run(IProgressMonitor monitor) {
              openEditor(a.getGroupId(), a.getArtifactId(), a.getVersion());
              return Status.OK_STATUS;
            }
          }.schedule();
          return;
        }
      }
    }
    
    String title;
    if(IndexManager.SEARCH_CLASS_NAME.equals(type)) {
      title = "Search class in Maven repositories";
    } else {
      title = "Search Maven POM";
    }
    
    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(shell, title, type, Collections.<ArtifactKey>emptySet());
    if(dialog.open() == Window.OK) {
      final IndexedArtifactFile iaf = (IndexedArtifactFile) dialog.getFirstResult();
      final IndexedArtifact indexedArtifact = dialog.getSelectedIndexedArtifact();
      new Job("Opening POM") {
        protected IStatus run(IProgressMonitor monitor) {
          if(IndexManager.SEARCH_CLASS_NAME.equals(type)) {
            if(indexedArtifact != null) {
              openEditor(indexedArtifact, iaf);
            }
          } else if(iaf!=null) {
            openEditor(iaf.group, iaf.artifact, iaf.version);
          }
          return Status.OK_STATUS;
        }
      }.schedule();
    }
  }

  private ArtifactKey getArtifact(Object element) {
    return SelectionUtil.getType(element, ArtifactKey.class);
  }

  public static void openEditor(IndexedArtifact ia, IndexedArtifactFile f) {
    if(f == null || ia.className == null || ia.packageName == null) {
      return;
    }

    Dependency dependency = f.getDependency();

    String groupId = dependency.getGroupId();
    String artifactId = dependency.getArtifactId();
    String version = dependency.getVersion();

    String name = ia.className;
    String fileName = ia.packageName.replace('.', '/') + "/" + ia.className + ".java";
    String tooltip = groupId + ":" + artifactId + ":" + version + "/" + fileName;

    try {
      MavenEmbedderManager embedderManager = MavenPlugin.getDefault().getMavenEmbedderManager();
      MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();
      Artifact artifact = embedder.createArtifactWithClassifier(groupId, artifactId, version, "java-source", "sources");

      IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
      List<ArtifactRepository> artifactRepositories = indexManager.getArtifactRepositories(null, null);
      
      embedder.resolve(artifact, artifactRepositories, embedder.getLocalRepository());

      final File file = artifact.getFile();
      if(file == null) {
        openDialog("Can't download sources for " + tooltip);
        return;
      }

      // that won't work if source archive have subfolders before actual source tree
      String url = "jar:" + file.toURL().toString() + "!/" + fileName;
      InputStream is = new URL(url).openStream();
      byte[] buff = readStream(is);

      openEditor(new MavenEditorStorageInput(name + ".java", tooltip, url, buff), name + ".java");

    } catch(AbstractArtifactResolutionException ex) {
      MavenLogger.log("Can't resolve artifact " + name, ex);
      openDialog("Can't resolve artifact " + name + "\n" + ex.toString());

    } catch(IOException ex) {
      MavenLogger.log("Can't open editor for " + name, ex);
      openDialog("Can't open editor for " + name + "\n" + ex.toString());
    }
  }

  public static void openEditor(String groupId, String artifactId, String version) {
    final String name = groupId + ":" + artifactId + ":" + version + ".pom";

    try {
      MavenPlugin plugin = MavenPlugin.getDefault();
      
      MavenProjectManager projectManager = plugin.getMavenProjectManager();
      IMavenProjectFacade projectFacade = projectManager.getMavenProject(groupId, artifactId, version);
      if(projectFacade!=null) {
        final IFile pomFile = projectFacade.getPom();
        openEditor(new FileEditorInput(pomFile), name);
        return;
      }
      
      MavenEmbedderManager embedderManager = plugin.getMavenEmbedderManager();
      MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();
      Artifact artifact = embedder.createArtifact(groupId, artifactId, version, null, "pom");

      IndexManager indexManager = plugin.getIndexManager();
      List<ArtifactRepository> artifactRepositories = indexManager.getArtifactRepositories(null, null);
      
      embedder.resolve(artifact, artifactRepositories, embedder.getLocalRepository());

      File file = artifact.getFile();
      if(file == null) {
        openDialog("Can't download " + name);
        return;
      }

      openEditor(new MavenEditorStorageInput(name, name, file.getAbsolutePath(), readStream(new FileInputStream(file))), name);

    } catch(AbstractArtifactResolutionException ex) {
      MavenLogger.log("Can't resolve artifact " + name, ex);
      openDialog("Can't resolve artifact " + name + "\n" + ex.toString());

    } catch(IOException ex) {
      MavenLogger.log("Can't open pom file for " + name, ex);
      openDialog("Can't open pom file for " + name + "\n" + ex.toString());
    }
  }

  public static void openEditor(final IEditorInput editorInput, final String name) {
    PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
      public void run() {
        IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
        IContentType contentType = contentTypeManager.findContentTypeFor(name);
        IEditorRegistry editorRegistry = PlatformUI.getWorkbench().getEditorRegistry();
        IEditorDescriptor editor = editorRegistry.getDefaultEditor(name, contentType);
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if(window != null) {
          IWorkbenchPage page = window.getActivePage();
          if(page != null) {
            try {
              page.openEditor(editorInput, editor.getId());
            } catch(PartInitException ex) {
              MessageDialog.openInformation(Display.getDefault().getActiveShell(), //
                  "Open Maven POM", "Can't open editor for " + name + "\n" + ex.toString());
            }
          }
        }
      }
    });
  }

  private static void openDialog(final String msg) {
    PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
      public void run() {
        MessageDialog.openInformation(Display.getDefault().getActiveShell(), //
            "Open Maven POM", msg);
      }
    });
  }

  private static byte[] readStream(InputStream is) throws IOException {
    byte[] b = new byte[is.available()];
    int len = 0;
    while(true) {
      int n = is.read(b, len, b.length - len);
      if(n == -1) {
        if(len < b.length) {
          byte[] c = new byte[len];
          System.arraycopy(b, 0, c, 0, len);
          b = c;
        }
        return b;
      }
      len += n;
      if(len == b.length) {
        byte[] c = new byte[b.length + 1000];
        System.arraycopy(b, 0, c, 0, len);
        b = c;
      }
    }
  }

  /**
   * Storage editor input implementation for Maven poms
   */
  public static class MavenEditorStorageInput implements IStorageEditorInput, IPathEditorInput {

    private final String name;

    private final String path;
    
    private final String tooltip;

    private final byte[] content;

    public MavenEditorStorageInput(String name, String tooltip, String path, byte[] content) {
      this.name = name;
      this.path = path;
      this.tooltip = tooltip;
      this.content = content;
    }

    // IStorageEditorInput

    public boolean exists() {
      return true;
    }

    public String getName() {
      return this.name;
    }

    public String getToolTipText() {
      return this.tooltip;
    }

    public IStorage getStorage() {
      return new MavenStorage(name, path, content);
    }

    public ImageDescriptor getImageDescriptor() {
      return null;
    }

    public IPersistableElement getPersistable() {
      return null;
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter(Class adapter) {
      return null;
    }

    // IPathEditorInput

    public IPath getPath() {
      return new Path(path);
    }

  }

  private static class MavenStorage implements IStorage {
    private String name;
    private final String path;
    private final byte[] content;


    public MavenStorage(String name, String path, byte[] content) {
      this.name = name;
      this.path = path;
      this.content = content;
    }

    public String getName() {
      return name;
    }

    public IPath getFullPath() {
      return path==null ? null : new Path(path);
    }

    public InputStream getContents() {
      return new ByteArrayInputStream(content);
    }

    public boolean isReadOnly() {
      return true;
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter(Class adapter) {
      return null;
    }
  }

}
