/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.ui.internal.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

import org.maven.ide.eclipse.MavenImages;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.actions.MaterializeAction;
import org.maven.ide.eclipse.actions.OpenPomAction;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.index.IndexListener;
import org.maven.ide.eclipse.index.IndexManager;
import org.maven.ide.eclipse.index.IndexedArtifact;
import org.maven.ide.eclipse.index.IndexedArtifactFile;
import org.maven.ide.eclipse.internal.index.IndexedArtifactGroup;
import org.maven.ide.eclipse.internal.index.NexusIndex;
import org.maven.ide.eclipse.internal.index.NexusIndexManager;
import org.maven.ide.eclipse.ui.internal.views.nodes.AbstractIndexedRepositoryNode;
import org.maven.ide.eclipse.ui.internal.views.nodes.IArtifactNode;
import org.maven.ide.eclipse.ui.internal.views.nodes.IndexedArtifactFileNode;
import org.maven.ide.eclipse.ui.internal.views.nodes.RepositoryNode;


/**
 * Maven repository view
 * 
 * @author dyocum
 */
public class MavenRepositoryView extends ViewPart {


  private IAction collapseAllAction;
  
  private IAction reloadSettings;
  
  private BaseSelectionListenerAction openPomAction;

  private BaseSelectionListenerAction updateAction;
  
  private BaseSelectionListenerAction rebuildAction;
  
  private BaseSelectionListenerAction enableAction;

  private BaseSelectionListenerAction copyUrlAction;
  
  //can't do this in indexer...yet

  //private BaseSelectionListenerAction deleteFromLocalAction;

  private BaseSelectionListenerAction materializeProjectAction;
  
  TreeViewer viewer;
  private RepositoryViewContentProvider contentProvider;

  private DrillDownAdapter drillDownAdapter;

  public void setFocus() {
    viewer.getControl().setFocus();
  }
  
  public void createPartControl(Composite parent) {
    viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    contentProvider = new RepositoryViewContentProvider();
    viewer.setContentProvider(contentProvider);
    viewer.setLabelProvider(new RepositoryViewLabelProvider(viewer.getTree().getFont()));
    
    viewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        
      }
    });
    viewer.setInput(getViewSite());
    drillDownAdapter = new DrillDownAdapter(viewer);

    makeActions();
    hookContextMenu();

    viewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        openPomAction.run();
      }
    });

    contributeToActionBars();

    IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
    indexManager.addIndexListener(new IndexListener() {

      public void indexAdded(String repositoryUrl) {
        refreshView();
      }

      public void indexChanged(String repositoryUrl) {
        refreshView();

      }

      public void indexRemoved(String repositoryUrl) {
        refreshView();
        
      }
      public void indexUpdating(String repositoryUrl){
        Display.getDefault().asyncExec(new Runnable(){
          public void run(){
           viewer.refresh(true); 
          }
        });
      }
    });
  }

  private void hookContextMenu() {
    MenuManager menuMgr = new MenuManager("#PopupMenu");
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        MavenRepositoryView.this.fillContextMenu(manager);
      }
    });

    Menu menu = menuMgr.createContextMenu(viewer.getControl());
    viewer.getControl().setMenu(menu);
    getSite().registerContextMenu(menuMgr, viewer);
  }

  private void contributeToActionBars() {
    IActionBars bars = getViewSite().getActionBars();
    fillLocalPullDown(bars.getMenuManager());
    fillLocalToolBar(bars.getToolBarManager());
  }

  private void fillLocalPullDown(IMenuManager manager) {
    manager.add(new Separator());
    manager.add(collapseAllAction);
    manager.add(reloadSettings);
  }

  protected List<AbstractIndexedRepositoryNode> getSelectedRepositoryNodes(List elements){
    ArrayList<AbstractIndexedRepositoryNode> list = new ArrayList<AbstractIndexedRepositoryNode>();
    if (elements != null) {
      for(int i=0;i<elements.size();i++){
        Object elem = elements.get(i);
        if(elem instanceof AbstractIndexedRepositoryNode) {
          list.add((AbstractIndexedRepositoryNode)elem);
        }
      }
    }
    return list;
  }
  protected List<IArtifactNode> getArtifactNodes(List elements){
    if(elements == null || elements.size() == 0){
      return null;
    }
    ArrayList<IArtifactNode> list = new ArrayList<IArtifactNode>();
    for(int i=0;i<elements.size();i++){
      Object elem = elements.get(i);
      if(elem instanceof IArtifactNode){
        IArtifactNode node = (IArtifactNode)elem;
        list.add(node);
      }
    }
    return list;
  }
  void fillContextMenu(IMenuManager manager) {
    manager.add(openPomAction);
    manager.add(copyUrlAction);
    manager.add(materializeProjectAction);
    manager.add(new Separator());
    manager.add(reloadSettings);
    manager.add(updateAction);
    manager.add(rebuildAction);
    manager.add(enableAction);
//    manager.add(deleteFromLocalAction);
    manager.add(new Separator());
    manager.add(collapseAllAction);
    manager.add(new Separator());
    drillDownAdapter.addNavigationActions(manager);
    manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
  }

  private void fillLocalToolBar(IToolBarManager manager) {
    manager.add(new Separator());
    manager.add(collapseAllAction);
    manager.add(reloadSettings);
    manager.add(new Separator());
    drillDownAdapter.addNavigationActions(manager);
  }

  private void makeActions() {
    collapseAllAction = new Action("Collapse All") {
      public void run() {
        viewer.collapseAll();
      }
    };
    collapseAllAction.setToolTipText("Collapse All");
    collapseAllAction.setImageDescriptor(MavenImages.COLLAPSE_ALL);
    reloadSettings = new Action("Reload settings.xml"){
      public void run(){
        String msg = "This will reload the settings.xml and rebuild the indexes for the repositories. Are you sure you want to reload the settings?";
        boolean res = MessageDialog.openConfirm(getViewSite().getShell(), //
            "Reload settings.xml", msg);
        if(res){
          Job job = new WorkspaceJob("Reloading settings.xml"){
            public IStatus runInWorkspace(IProgressMonitor monitor){
//              MavenPlugin.getDefault().reloadSettingsXml();
              return Status.OK_STATUS;
            }
          };
          job.schedule();
        }
      }
    };
    
    reloadSettings.setImageDescriptor(MavenImages.REFRESH);
//    deleteFromLocalAction = new BaseSelectionListenerAction("Delete from Repository") {
//      public void run() {
//        List<IArtifactNode> nodes = getArtifactNodes(getStructuredSelection().toList());
//        if(nodes != null){
//          for(IArtifactNode node : nodes){
//            String key = node.getDocumentKey();
//            System.out.println("key: "+key);
//            ((NexusIndexManager)MavenPlugin.getDefault().getIndexManager()).removeDocument("local", null, key);
//          }
//        }
//      }
//
//      protected boolean updateSelection(IStructuredSelection selection) {
//        List<IArtifactNode> nodes = getArtifactNodes(getStructuredSelection().toList());
//        return (nodes != null && nodes.size() > 0);
//      }
//    };
//    deleteFromLocalAction.setToolTipText("Delete the selected GAV from the local repository");
    //updateAction.setImageDescriptor(MavenImages.UPD_INDEX);

    
    updateAction = new BaseSelectionListenerAction("Update Index") {
      public void run() {
        List<AbstractIndexedRepositoryNode> nodes = getSelectedRepositoryNodes(getStructuredSelection().toList());
        for(AbstractIndexedRepositoryNode node : nodes) {
          if (node instanceof RepositoryNode) {
            updateIndex(((RepositoryNode) node).getRepositoryUrl(), false);
          }
        }
      }

      protected boolean updateSelection(IStructuredSelection selection) {
        int indexCount = 0;
        for (AbstractIndexedRepositoryNode node : getSelectedRepositoryNodes(selection.toList())) {
          if (node instanceof RepositoryNode && node.getIndex() != null) {
            indexCount ++;
          }
        }
        if(indexCount > 1){
          setText("Update Indexes");
        } else {
          setText("Update Index");
        }
        return indexCount > 0;
      }
    };
    updateAction.setToolTipText("Update repository index");
    updateAction.setImageDescriptor(MavenImages.UPD_INDEX);

    rebuildAction = new BaseSelectionListenerAction("Rebuild Index") {
      public void run() {
        List<AbstractIndexedRepositoryNode> nodes = getSelectedRepositoryNodes(getStructuredSelection().toList());
        if(nodes.size() > 0){
          if(nodes.size() == 1){
            NexusIndex index = nodes.get(0).getIndex();
            if (index != null) {
              String repositoryUrl = index.getRepositoryUrl();
              String msg = "Are you sure you want to rebuild the index '"+repositoryUrl+"'";
              boolean res = MessageDialog.openConfirm(getViewSite().getShell(), //
                  "Rebuild Index", msg);
              if(res) {
                // TODO request index deletion
                // TODO add deleted index to 
                updateIndex(repositoryUrl, true);
              }
            }
          } else {
            String msg = "Are you sure you want to rebuild the selected indexes?";
            boolean res = MessageDialog.openConfirm(getViewSite().getShell(), //
                "Rebuild Indexes", msg);
            if(res) {
              // TODO request index deletion
              // TODO add deleted index to 
              for(AbstractIndexedRepositoryNode node : nodes){
                NexusIndex index = node.getIndex();
                if (index != null) {
                  updateIndex(index.getRepositoryUrl(), true);
                }
              }
            }            
          }
        }
      }
      
      protected boolean updateSelection(IStructuredSelection selection) {
        int indexCount = 0;
        for (AbstractIndexedRepositoryNode node : getSelectedRepositoryNodes(selection.toList())) {
          if (node.getIndex() != null) {
            indexCount ++;
          }
        }
        if(indexCount > 1){
          setText("Rebuild Indexes");
        } else {
          setText("Rebuild Index");
        }
        return indexCount > 0;
      }
    };
    
    rebuildAction.setToolTipText("Force a rebuild of the maven index");
    rebuildAction.setImageDescriptor(MavenImages.REBUILD_INDEX);

    enableAction = new BaseSelectionListenerAction("Enable Index") {
      private RepositoryNode repositoryNode;
      public void run() {
        enableIndex(repositoryNode);
      }

      protected boolean updateSelection(IStructuredSelection selection) {
        List<AbstractIndexedRepositoryNode> nodes = getSelectedRepositoryNodes(getStructuredSelection().toList());
        RepositoryNode repositoryNode = null;
        for (AbstractIndexedRepositoryNode node : nodes) {
          if (node instanceof RepositoryNode) {
            if (repositoryNode != null) {
              return false;
            }
            repositoryNode = (RepositoryNode) node;
          }
        }
        if (repositoryNode == null) {
          return false;
        }
        if (repositoryNode.getImage() == null) {
          setText("Enable Index");
        } else {
          setText("Disable Index");
        }
        this.repositoryNode = repositoryNode;
        return true;
      }
    };

    enableAction.setToolTipText("Enable the index to be used for dependency resolution");
    enableAction.setImageDescriptor(MavenImages.REBUILD_INDEX);
    
    openPomAction = new BaseSelectionListenerAction("Open POM") {
      public void run() {
        ISelection selection = viewer.getSelection();
        Object element = ((IStructuredSelection) selection).getFirstElement();
        if(element instanceof IndexedArtifactFileNode) {
          IndexedArtifactFile f = ((IndexedArtifactFileNode) element).getIndexedArtifactFile();
          OpenPomAction.openEditor(f.group, f.artifact, f.version, null);
        }
      }

      protected boolean updateSelection(IStructuredSelection selection) {
        return selection.getFirstElement() instanceof IndexedArtifactFile;
      }
    };
    openPomAction.setToolTipText("Open Maven POM");
    openPomAction.setImageDescriptor(MavenImages.POM);

    copyUrlAction = new BaseSelectionListenerAction("Copy URL") {
      public void run() {
        Object element = getStructuredSelection().getFirstElement();
        String url = null;
        if(element instanceof RepositoryNode) {
          url = ((RepositoryNode) element).getRepositoryUrl();
        } else if(element instanceof IndexedArtifactGroup) {
          IndexedArtifactGroup group = (IndexedArtifactGroup) element;
          String repositoryUrl = group.getRepositoryUrl();
          if(!repositoryUrl.endsWith("/")) {
            repositoryUrl += "/";
          }
          url = repositoryUrl + group.getPrefix().replace('.', '/');
        } else if(element instanceof IndexedArtifact) {
          // 
        } else if(element instanceof IndexedArtifactFile) {
          // 
        }
        if(url != null) {
          Clipboard clipboard = new Clipboard(Display.getCurrent());
          clipboard.setContents(new String[] {url}, new Transfer[] {TextTransfer.getInstance()});
          clipboard.dispose();
        }
      }

      protected boolean updateSelection(IStructuredSelection selection) {
        Object element = selection.getFirstElement();
        return element instanceof RepositoryNode;
      }
    };
    copyUrlAction.setToolTipText("Copy URL to Clipboard");
    copyUrlAction.setImageDescriptor(MavenImages.COPY);
    
    materializeProjectAction = new BaseSelectionListenerAction("Materialize Projects") {
      public void run() {
        Object element = getStructuredSelection().getFirstElement();
        if(element instanceof IndexedArtifactFileNode){
          MaterializeAction action = new MaterializeAction();
          StructuredSelection sel = new StructuredSelection(new Object[]{((IndexedArtifactFileNode) element).getIndexedArtifactFile()});
          action.selectionChanged(this, sel);
          action.run(this);
        }
      }
      
      protected boolean updateSelection(IStructuredSelection selection) {
        return selection.getFirstElement() instanceof IndexedArtifactFileNode;
      }
    };
    materializeProjectAction.setImageDescriptor(MavenImages.IMPORT_PROJECT);

    viewer.addSelectionChangedListener(openPomAction);
    viewer.addSelectionChangedListener(updateAction);
    viewer.addSelectionChangedListener(enableAction);
//    viewer.addSelectionChangedListener(deleteFromLocalAction);
    viewer.addSelectionChangedListener(rebuildAction);
    viewer.addSelectionChangedListener(copyUrlAction);
    viewer.addSelectionChangedListener(materializeProjectAction);
  }

  /**
   * 
   */
  protected void enableIndex(RepositoryNode node) {
    String msg = "Are you sure you want to enable the index '" + node.getRepoName();

    boolean ok = MessageDialog.openConfirm(getViewSite().getShell(), "Enable Index", msg);

    if(ok) {
      NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();
      
      if(node.getIndex() == null) {
        // TODO select between short and full index
        indexManager.enableIndex(node.getRepositoryUrl(), true);
      } else {
        indexManager.disableIndex(node.getRepositoryUrl());
      }
    }
  }

  public void dispose() {
    viewer.removeSelectionChangedListener(materializeProjectAction);
    viewer.removeSelectionChangedListener(copyUrlAction);
    viewer.removeSelectionChangedListener(rebuildAction);
//    viewer.removeSelectionChangedListener(deleteFromLocalAction);
    viewer.removeSelectionChangedListener(enableAction);
    viewer.removeSelectionChangedListener(updateAction);
    viewer.removeSelectionChangedListener(openPomAction);

    super.dispose();
  }

//  void markIndexUpdating(final String indexName, final boolean isUpdating){
//    Display.getDefault().asyncExec(new Runnable() {
//      public void run() {
//        //TODO: mark the nodes as 'updating' when the update index is running
//        TreeItem[] items = viewer.getTree().getItems();
//        for(TreeItem item : items){
//          Object data = item.getData();
//          if(data instanceof IndexNode && ((IndexNode)data).getIndexName().equals(indexName)){
//            ((IndexNode)data).setIsUpdating(isUpdating);
//          }
//        }
//        viewer.refresh(true);
//      }
//      
//    });    
//  }
  
  void refreshView() {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        Object[] expandedElems = viewer.getExpandedElements();
        if (!viewer.getControl().isDisposed()) {
          viewer.setInput(getViewSite());
          if(expandedElems != null && expandedElems.length > 0){
            viewer.setExpandedElements(expandedElems);
          }
        }
      }
    });
  };

  protected void updateIndex(final String repositoryUrl, boolean force){
    try{
      IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
      indexManager.scheduleIndexUpdate(repositoryUrl, force);
    } catch(CoreException ce){
      MavenLogger.log(ce);
    }
  }

}
