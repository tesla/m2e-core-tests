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
import org.maven.ide.eclipse.internal.index.NexusIndexManager;
import org.maven.ide.eclipse.ui.internal.views.nodes.HiddenRepositoryNode;
import org.maven.ide.eclipse.ui.internal.views.nodes.IndexNode;
import org.maven.ide.eclipse.ui.internal.views.nodes.IndexedArtifactFileNode;


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

  private BaseSelectionListenerAction materializeProjectAction;
  
  private TreeViewer viewer;
  private RepositoryViewContentProvider contentProvider;

  private DrillDownAdapter drillDownAdapter;

  public void setFocus() {
    viewer.getControl().setFocus();
  }
  
  public void createPartControl(Composite parent) {
    viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    contentProvider = new RepositoryViewContentProvider();
    viewer.setContentProvider(contentProvider);
    viewer.setLabelProvider(new RepositoryViewLabelProvider());
    viewer.setSorter(new RepositoryViewerSorter());
    
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

      public void indexAdded(String indexName) {
        refreshView();
      }

      public void indexChanged(String indexName) {
        refreshView();

      }

      public void indexRemoved(String indexName) {
        refreshView();
        
      }
      public void indexUpdating(String indexName){

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

  protected List<IndexNode> getIndexElementsToUpdate(List elements){
    if(elements == null || elements.size() == 0){
      return null;
    }
    ArrayList<IndexNode> list = new ArrayList<IndexNode>();
    for(int i=0;i<elements.size();i++){
      Object elem = elements.get(i);
      if(elem instanceof IndexNode){
        IndexNode node = (IndexNode)elem;
        if(!node.isWorkspace()){
          list.add((IndexNode)elem);
        }
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
              MavenPlugin.getDefault().reloadSettingsXml();
              return Status.OK_STATUS;
            }
          };
          job.schedule();
        }
      }
    };
    reloadSettings.setImageDescriptor(MavenImages.REFRESH);
    updateAction = new BaseSelectionListenerAction("Update Index") {
      public void run() {
        List<IndexNode> infoElements = getIndexElementsToUpdate(getStructuredSelection().toList());
        for(IndexNode node : infoElements){
          updateIndex(node.getIndexName(), false);
        }
      }

      protected boolean updateSelection(IStructuredSelection selection) {
        List<IndexNode> elems = getIndexElementsToUpdate(selection.toList());
        if(elems != null && elems.size() > 1){
          setText("Update Indexes");
        } else {
          setText("Update Index");
        }
        return elems != null && elems.size() > 0;
      }
    };
    updateAction.setToolTipText("Update repository index");
    updateAction.setImageDescriptor(MavenImages.UPD_INDEX);

    rebuildAction = new BaseSelectionListenerAction("Rebuild Index") {
      public void run() {
        List<IndexNode> elemsToUpdate = getIndexElementsToUpdate(getStructuredSelection().toList());
        if(elemsToUpdate != null && elemsToUpdate.size() > 0){
          if(elemsToUpdate.size() == 1){
            IndexNode info = elemsToUpdate.get(0);
            String name = info.getName();
            String msg = "Are you sure you want to rebuild the index '"+name+"'";
            boolean res = MessageDialog.openConfirm(getViewSite().getShell(), //
                "Rebuild Index", msg);
            if(res) {
              // TODO request index deletion
              // TODO add deleted index to 
              updateIndex(info.getIndexName(), true);
            }
          } else {
            String msg = "Are you sure you want to rebuild the selected indexes?";
            boolean res = MessageDialog.openConfirm(getViewSite().getShell(), //
                "Rebuild Indexes", msg);
            if(res) {
              // TODO request index deletion
              // TODO add deleted index to 
              for(IndexNode info : elemsToUpdate){
                updateIndex(info.getIndexName(), true);
              }
            }            
          }
        }
      }
      
      protected boolean updateSelection(IStructuredSelection selection) {
        List<IndexNode> elems = getIndexElementsToUpdate(selection.toList());
        if(elems != null && elems.size() > 1){
          setText("Rebuild Indexes");
        } else {
          setText("Rebuild Index");
        }
        return elems != null && elems.size() > 0;
      }
    };
    
    rebuildAction.setToolTipText("Force a rebuild of the maven index");
    rebuildAction.setImageDescriptor(MavenImages.REBUILD_INDEX);

    enableAction = new BaseSelectionListenerAction("Enable Index") {
      public void run() {
        ISelection selection = viewer.getSelection();
        Object element = ((IStructuredSelection) selection).getFirstElement();
        if(element instanceof HiddenRepositoryNode) {
          HiddenRepositoryNode node = (HiddenRepositoryNode)element;
          String msg = "Are you sure you want to enable the index '"+node.getRepoName()+"'. This will allow the index to be used for dependency resolution, but it will NOT be used during a maven build.";
          boolean res = MessageDialog.openConfirm(getViewSite().getShell(), //
              "Enable Index", msg);
          if(res){
            NexusIndexManager im = (NexusIndexManager)MavenPlugin.getDefault().getIndexManager();
            try{
              im.addIndexForRemote(node.getRepoName(), node.getRepoUrl());
              im.scheduleIndexUpdate(node.getRepoName(), true, 1000);
            } catch(Exception e){
              MavenLogger.log("Unable to enable index "+node.getName(), e);
            }
          }
        }
      }

      protected boolean updateSelection(IStructuredSelection selection) {
        return (selection.getFirstElement() instanceof HiddenRepositoryNode);
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
        if(element instanceof IndexNode) {
          url = ((IndexNode) element).getRepositoryUrl();
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
        return element instanceof IndexNode;
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
    viewer.addSelectionChangedListener(rebuildAction);
    viewer.addSelectionChangedListener(copyUrlAction);
    viewer.addSelectionChangedListener(materializeProjectAction);
  }

  public void dispose() {
    viewer.removeSelectionChangedListener(materializeProjectAction);
    viewer.removeSelectionChangedListener(copyUrlAction);
    viewer.removeSelectionChangedListener(rebuildAction);
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

  protected void updateIndex(final String indexName, boolean force){
    try{
      IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();
      indexManager.scheduleIndexUpdate(indexName, force, 1L);
    } catch(CoreException ce){
      MavenLogger.log(ce);
    }
  }

}
